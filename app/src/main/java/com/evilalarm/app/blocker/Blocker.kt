package com.evilalarm.app.blocker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

data class InstalledApp(val packageName: String, val label: String)

object Blocker {
    fun isServiceEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val ours = ComponentName(context, BlockerService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == ours }
    }

    fun accessibilitySettingsIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    /** Apps with a launcher icon, minus this one. Slow-ish: call off the main thread. */
    fun launchableApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val activities = pm.queryIntentActivities(launcherIntent, 0)
        return activities
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .filter { it.first != context.packageName }
            .map { InstalledApp(it.first, it.second) }
            .sortedBy { it.label.lowercase() }
    }

    fun appLabel(context: Context, packageName: String): String = installedLabel(context, packageName) ?: packageName

    /** The app's name, or null if it isn't installed. */
    fun installedLabel(context: Context, packageName: String): String? {
        val pm = context.packageManager
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}

/** Whether the accessibility service is on, re-checked every time the screen resumes. */
@Composable
fun rememberBlockerEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(Blocker.isServiceEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        enabled = Blocker.isServiceEnabled(context)
        onPauseOrDispose { }
    }
    return enabled
}
