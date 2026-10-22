package com.evilalarm.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.evilalarm.app.data.Alarm
import com.evilalarm.app.repository
import com.evilalarm.app.ui.MainActivity
import kotlinx.coroutines.flow.first

/**
 * Schedules alarms with [AlarmManager.setAlarmClock], which fires exactly on time even in Doze
 * and shows the alarm icon in the status bar.
 */
object AlarmScheduler {
    const val EXTRA_ALARM_ID = "alarm_id"

    /** Alarm id used by "Test alarm"; it has no saved alarm behind it. */
    const val TEST_ALARM_ID = -1

    private const val SNOOZE_REQUEST_OFFSET = 1_000_000
    private const val TEST_REQUEST_CODE = 2_000_000
    private const val TAG = "AlarmScheduler"

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager(context).canScheduleExactAlarms()

    fun schedule(context: Context, alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(context, alarm.id)
            return
        }
        setAlarmClock(context, alarm.id, alarm.id, alarm.nextTrigger().toInstant().toEpochMilli())
    }

    fun scheduleSnooze(context: Context, alarmId: Int, minutes: Int) {
        setAlarmClock(context, alarmId + SNOOZE_REQUEST_OFFSET, alarmId, System.currentTimeMillis() + minutes * 60_000L)
    }

    fun scheduleTest(context: Context, seconds: Int) {
        setAlarmClock(context, TEST_REQUEST_CODE, TEST_ALARM_ID, System.currentTimeMillis() + seconds * 1_000L)
    }

    fun cancel(context: Context, alarmId: Int) {
        val am = alarmManager(context)
        am.cancel(operation(context, alarmId, alarmId))
        am.cancel(operation(context, alarmId + SNOOZE_REQUEST_OFFSET, alarmId))
    }

    suspend fun rescheduleAll(context: Context) {
        context.repository.alarms.first().forEach { schedule(context, it) }
        Bedtime.reschedule(context)
    }

    private fun setAlarmClock(context: Context, requestCode: Int, alarmId: Int, triggerAtMillis: Long) {
        val showIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        try {
            alarmManager(context).setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                operation(context, requestCode, alarmId),
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission missing; alarm $alarmId not scheduled", e)
        }
    }

    private fun operation(context: Context, requestCode: Int, alarmId: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)
}
