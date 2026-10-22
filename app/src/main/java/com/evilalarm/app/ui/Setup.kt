package com.evilalarm.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.evilalarm.app.alarm.AlarmScheduler
import com.evilalarm.app.alarm.Notifications
import com.evilalarm.app.blocker.Blocker

enum class RequirementKey { NOTIFICATIONS, FULL_SCREEN, EXACT_ALARMS, APP_BLOCKER, BATTERY }

/** Something the phone has to allow before Evil Alarm can do its job. */
data class Requirement(
    val key: RequirementKey,
    val title: String,
    /** One line, for the checklist. */
    val summary: String,
    /** A few sentences, for the setup walkthrough. */
    val detail: String,
    val fixLabel: String,
    val granted: Boolean,
    val fix: Intent,
)

/** Every requirement in setup order, re-checked each time the screen resumes (e.g. back from Settings). */
@Composable
fun rememberRequirements(): List<Requirement> {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    return remember(refresh) { requirements(context) }
}

@SuppressLint("BatteryLife")
fun requirements(context: Context): List<Requirement> {
    val pkg = context.packageName
    val pkgUri = "package:$pkg".toUri()
    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)
    val power = context.getSystemService(PowerManager::class.java)
    return listOf(
        Requirement(
            key = RequirementKey.NOTIFICATIONS,
            title = "Notifications",
            summary = "Needed to show the alarm screen",
            detail = "When your alarm goes off, Evil Alarm brings up its ring screen through a notification. " +
                "Without this, it can ring with nothing on screen to turn it off.",
            fixLabel = "Allow notifications",
            granted = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            fix = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg),
        ),
        Requirement(
            key = RequirementKey.FULL_SCREEN,
            title = "Full-screen alarms",
            summary = "Lets the alarm take over the lock screen",
            detail = "Lets the ring screen take over your whole phone, even when it's locked, " +
                "instead of hiding in a small banner you can swipe away.",
            fixLabel = "Allow full-screen alarms",
            granted = Notifications.canPostFullScreen(context),
            fix = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkgUri)
            } else {
                appDetails
            },
        ),
        Requirement(
            key = RequirementKey.EXACT_ALARMS,
            title = "Exact alarms",
            summary = "Rings on the exact minute",
            detail = "Android delays ordinary alarms to save battery. This makes sure Evil Alarm goes off on the exact minute you set.",
            fixLabel = "Allow exact alarms",
            granted = AlarmScheduler.canScheduleExact(context),
            fix = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri)
            } else {
                appDetails
            },
        ),
        Requirement(
            key = RequirementKey.APP_BLOCKER,
            title = "App blocker",
            summary = "Accessibility access, so it can lock apps after your alarm",
            detail = "Your first alarm of the day locks Instagram, TikTok and anything else you pick. " +
                "To know which app is open, Evil Alarm needs Accessibility access. " +
                "It's only told when you switch apps and never reads what's on your screen.",
            fixLabel = "Open Accessibility settings",
            granted = Blocker.isServiceEnabled(context),
            fix = Blocker.accessibilitySettingsIntent(),
        ),
        Requirement(
            key = RequirementKey.BATTERY,
            title = "Unrestricted battery",
            summary = "Stops the system from killing the alarm",
            detail = "Some phones shut down background apps overnight to save battery, which can silence an alarm. " +
                "This keeps Evil Alarm awake until it's needed.",
            fixLabel = "Allow unrestricted battery",
            granted = power.isIgnoringBatteryOptimizations(pkg),
            fix = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri),
        ),
    )
}

/** Extra steps some phone makers need on top of Android's battery setting. */
fun manufacturerBatteryTip(): String? = when (Build.MANUFACTURER.lowercase()) {
    "samsung" -> "Samsung phones have a second list: Settings → Battery → Background usage limits. " +
        "Make sure Evil Alarm isn't under Sleeping apps or Deep sleeping apps."
    "xiaomi", "redmi", "poco" -> "On Xiaomi phones, also open Evil Alarm's App info: turn on Autostart, " +
        "and set Battery saver to No restrictions."
    "oneplus", "oppo", "realme" -> "On this phone, also open Evil Alarm's App info → Battery and turn on Allow background activity."
    "huawei", "honor" -> "Also open Settings → Battery → App launch, find Evil Alarm, switch it to Manage manually and turn everything on."
    "vivo" -> "Also open Settings → Battery → Background power consumption and allow Evil Alarm."
    else -> null
}

/** Opens [intent], falling back to the app's info page if this phone doesn't have that screen. */
fun openSettings(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent) }
        .onFailure {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
        }
}

/** Android 13+ can show the notification prompt in-app instead of sending you to Settings. */
val canAskForNotifications: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

const val NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS
