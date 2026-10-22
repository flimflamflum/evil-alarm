package com.evilalarm.app.ring

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evilalarm.app.alarm.AlarmService
import com.evilalarm.app.alarm.RingState
import com.evilalarm.app.alarm.WakeCheck
import com.evilalarm.app.ui.theme.EvilAlarmTheme
import kotlinx.coroutines.delay

/** Shown over the lock screen while an alarm rings. The only way out is the math (or snooze). */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        setContent {
            EvilAlarmTheme {
                BackHandler { /* Back does nothing: solve the math. */ }

                val ringing by RingState.ringing.collectAsStateWithLifecycle()
                var seenRinging by remember { mutableStateOf(false) }
                LaunchedEffect(ringing) {
                    when {
                        ringing != null -> seenRinging = true
                        seenRinging -> finish()
                        else -> {
                            // The notification can open us a moment before the service publishes state.
                            delay(3_000)
                            if (RingState.ringing.value == null) finish()
                        }
                    }
                }

                RingScreen(
                    ringing = ringing,
                    onSolved = {
                        val solved = RingState.ringing.value
                        AlarmService.dismiss(this)
                        if (solved != null && solved.settings.wakeCheck) {
                            val wait = if (solved.isTest) "30 seconds" else "${WakeCheck.CHECK_AFTER_MINUTES} minutes"
                            Toast.makeText(this, "Nice. Checking you're still up in $wait.", Toast.LENGTH_LONG).show()
                        }
                        finish()
                    },
                    onSnooze = {
                        AlarmService.snooze(this)
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, RingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}
