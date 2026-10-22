package com.evilalarm.app.blocker

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.evilalarm.app.repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Watches which app is in the foreground (window changes only, never screen content) and covers
 * blocked apps with [BlockActivity] while a block is running.
 */
class BlockerService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var blockUntil = 0L
    private var blockedPackages: Set<String> = emptySet()
    private var foregroundPackage: String? = null
    private var ignoredPackages: Set<String> = emptySet()
    private var lastBlockedAt = 0L

    override fun onServiceConnected() {
        // Keyboards and the notification shade open over apps; they shouldn't count as switching away.
        ignoredPackages = setOf("com.android.systemui") +
            getSystemService(InputMethodManager::class.java).enabledInputMethodList.map { it.packageName }

        scope.launch {
            repository.blockUntil.collect {
                blockUntil = it
                enforce()
            }
        }
        scope.launch {
            repository.settings.collect {
                blockedPackages = it.blockedPackages
                enforce()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignoredPackages) return
        foregroundPackage = pkg
        enforce()
    }

    private fun enforce() {
        val pkg = foregroundPackage ?: return
        if (pkg == packageName || pkg !in blockedPackages) return
        if (System.currentTimeMillis() >= blockUntil) return
        // Opening an app fires a burst of window events; one block screen is enough.
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockedAt < 800) return
        lastBlockedAt = now
        startActivity(BlockActivity.intent(this, pkg))
        scope.launch { repository.recordBlockedAttempt() }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
