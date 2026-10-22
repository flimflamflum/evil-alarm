package com.evilalarm.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.evilalarm.app.R
import com.evilalarm.app.ring.RingActivity

object Notifications {
    const val RINGING_ID = 1
    const val CHANNEL_WAKE_CHECK = "wake_check"
    const val CHANNEL_BEDTIME = "bedtime"
    private const val CHANNEL_RINGING = "ringing"

    fun createChannels(context: Context) {
        val ringing = NotificationChannel(CHANNEL_RINGING, "Ringing alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Shown while an alarm is ringing"
            // The service plays the sound and vibration itself.
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        val wakeCheck = NotificationChannel(CHANNEL_WAKE_CHECK, "Wake-up check", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Asks you to confirm you're awake a few minutes after the alarm"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        val bedtime = NotificationChannel(CHANNEL_BEDTIME, "Bedtime reminder", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Reminds you to go to sleep before your alarm"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(ringing, wakeCheck, bedtime))
    }

    /** The foreground notification; its full-screen intent opens the ring screen over the lock screen. */
    fun ringing(context: Context): Notification {
        val openRingScreen = PendingIntent.getActivity(
            context,
            0,
            RingActivity.intent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("Wake up.")
            .setContentText("Solve the math to make it stop.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openRingScreen)
            .setFullScreenIntent(openRingScreen, true)
            .build()
    }

    fun canPostFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
}
