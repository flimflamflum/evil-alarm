package com.evilalarm.app.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import com.evilalarm.app.R
import com.evilalarm.app.data.Alarm
import com.evilalarm.app.repository
import com.evilalarm.app.ui.MainActivity
import com.evilalarm.app.ui.formatTime
import com.evilalarm.app.ui.formatUntil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** A nudge to go to sleep, a set number of hours before the day's first alarm. */
object Bedtime {
    val HOUR_OPTIONS = listOf(7, 8, 9, 10)

    private const val REQUEST_CODE = 4_000_000
    private const val NOTIFICATION_ID = 3

    /** Arms the reminder for the next first-alarm-of-the-day whose reminder time is still ahead. */
    suspend fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val operation = pending(context)
        am.cancel(operation)

        val hours = context.repository.settings.first().bedtimeHours
        if (hours <= 0) return
        val now = ZonedDateTime.now()
        val remindAt = firstAlarmsOfDay(context.repository.alarms.first(), now)
            .map { it.minusHours(hours.toLong()) }
            .firstOrNull { it.isAfter(now) }
            ?: return
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt.toInstant().toEpochMilli(), operation)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt.toInstant().toEpochMilli(), operation)
        }
    }

    /**
     * The earliest alarm of each upcoming day, soonest first. Later alarms on the same morning don't
     * get their own reminder.
     */
    fun firstAlarmsOfDay(alarms: List<Alarm>, now: ZonedDateTime): List<ZonedDateTime> =
        alarms.filter { it.enabled }
            .flatMap { alarm ->
                generateSequence(alarm.nextTrigger(now)) { if (alarm.isRepeating) alarm.nextTrigger(it) else null }
                    .take(8)
                    .toList()
            }
            .groupBy { it.toLocalDate() }
            .values
            .map { sameDay -> sameDay.minBy { it.toInstant() } }
            .sortedBy { it.toInstant() }

    internal suspend fun remind(context: Context) {
        val now = ZonedDateTime.now()
        val nextAlarm = firstAlarmsOfDay(context.repository.alarms.first(), now).firstOrNull()
        if (nextAlarm != null) {
            val (time, amPm) = formatTime(nextAlarm.hour, nextAlarm.minute, DateFormat.is24HourFormat(context))
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_BEDTIME)
                .setSmallIcon(R.drawable.ic_stat_alarm)
                .setContentTitle("Bedtime. Put the phone down.")
                .setContentText(
                    "Your alarm goes off at $time${amPm?.let { " $it" }.orEmpty()}, " +
                        "in ${formatUntil(now.toInstant(), nextAlarm.toInstant())}.",
                )
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        }
        reschedule(context)
    }

    private fun pending(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, BedtimeReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

class BedtimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Bedtime.remind(context)
            } finally {
                pending.finish()
            }
        }
    }
}
