package com.evilalarm.app.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.evilalarm.app.R
import com.evilalarm.app.appScope
import com.evilalarm.app.repository
import kotlinx.coroutines.launch

/**
 * "Prove you're up": a few minutes after you turn the alarm off, a notification asks you to tap it.
 * Miss it and the alarm rings again (and the check repeats after that one too).
 */
object WakeCheck {
    const val CHECK_AFTER_MINUTES = 5
    const val ANSWER_WITHIN_MINUTES = 2

    // The test alarm uses short delays so it can be tried without waiting around.
    private const val TEST_CHECK_AFTER_SECONDS = 30
    private const val TEST_ANSWER_WITHIN_SECONDS = 60

    private const val ACTION_CHECK = "com.evilalarm.app.action.WAKE_CHECK"
    private const val ACTION_DEADLINE = "com.evilalarm.app.action.WAKE_DEADLINE"
    private const val ACTION_CONFIRM = "com.evilalarm.app.action.WAKE_CONFIRM"
    private const val EXTRA_TEST = "test"
    private const val NOTIFICATION_ID = 2

    fun schedule(context: Context, alarmId: Int, isTest: Boolean) {
        val delayMillis = if (isTest) TEST_CHECK_AFTER_SECONDS * 1_000L else CHECK_AFTER_MINUTES * 60_000L
        setExact(context, System.currentTimeMillis() + delayMillis, pending(context, ACTION_CHECK, alarmId, isTest))
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(pending(context, ACTION_CHECK, AlarmScheduler.TEST_ALARM_ID, false))
        am.cancel(pending(context, ACTION_DEADLINE, AlarmScheduler.TEST_ALARM_ID, false))
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    internal fun onCheck(context: Context, alarmId: Int, isTest: Boolean) {
        val answerMillis = if (isTest) TEST_ANSWER_WITHIN_SECONDS * 1_000L else ANSWER_WITHIN_MINUTES * 60_000L
        val confirm = pending(context, ACTION_CONFIRM, alarmId, isTest)
        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_WAKE_CHECK)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("Still awake?")
            .setContentText("Tap within ${if (isTest) "a minute" else "$ANSWER_WITHIN_MINUTES minutes"} or the alarm goes off again.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(confirm)
            .addAction(0, "I'm up", confirm)
            .setAutoCancel(true)
            .setTimeoutAfter(answerMillis)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        setExact(context, System.currentTimeMillis() + answerMillis, pending(context, ACTION_DEADLINE, alarmId, isTest))
    }

    internal fun onConfirm(context: Context) = cancel(context)

    internal fun onDeadline(context: Context, alarmId: Int, isTest: Boolean) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        if (!isTest) {
            val app = context.applicationContext
            app.appScope.launch { app.repository.recordFellBackAsleep() }
        }
        AlarmService.ring(context, alarmId)
    }

    private fun pending(context: Context, action: String, alarmId: Int, isTest: Boolean): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, WakeCheckReceiver::class.java)
                .setAction(action)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_TEST, isTest),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun setExact(context: Context, atMillis: Long, operation: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
        }
    }

    internal fun dispatch(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, AlarmScheduler.TEST_ALARM_ID)
        val isTest = intent.getBooleanExtra(EXTRA_TEST, false)
        when (intent.action) {
            ACTION_CHECK -> onCheck(context, alarmId, isTest)
            ACTION_CONFIRM -> onConfirm(context)
            ACTION_DEADLINE -> onDeadline(context, alarmId, isTest)
        }
    }
}

class WakeCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = WakeCheck.dispatch(context, intent)
}
