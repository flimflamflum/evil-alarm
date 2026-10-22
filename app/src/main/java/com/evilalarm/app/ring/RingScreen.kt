package com.evilalarm.app.ring

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evilalarm.app.alarm.RingingAlarm
import com.evilalarm.app.data.DismissMethod
import com.evilalarm.app.math.MathChallenge
import com.evilalarm.app.ui.MathPanel
import com.evilalarm.app.ui.currentTimeMillis
import com.evilalarm.app.ui.formatTime
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassBackground
import java.time.Instant
import java.time.ZoneId

@Composable
fun RingScreen(ringing: RingingAlarm?, onSolved: () -> Unit, onSnooze: () -> Unit) {
    val is24h = DateFormat.is24HourFormat(LocalContext.current)
    val now = Instant.ofEpochMilli(currentTimeMillis()).atZone(ZoneId.systemDefault())

    GlassBackground {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            val (time, amPm) = formatTime(now.hour, now.minute, is24h)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(time, color = Glass.TextPrimary, fontSize = 72.sp, fontWeight = FontWeight.Thin)
                if (amPm != null) {
                    Text(amPm, color = Glass.TextSecondary, fontSize = 18.sp, modifier = Modifier.padding(start = 6.dp, bottom = 16.dp))
                }
            }
            Text(
                ringing?.label?.ifBlank { null } ?: "Wake up.",
                color = Glass.TextSecondary,
                fontSize = 18.sp,
            )
            ringing?.blockMinutes?.let { minutes ->
                Spacer(Modifier.height(6.dp))
                Text("Your apps are locked for $minutes minutes.", color = Glass.Pink, fontSize = 14.sp)
            }

            Spacer(Modifier.height(32.dp))
            if (ringing != null) {
                // Keyed on the alarm so a new alarm firing mid-solve starts fresh.
                key(ringing.alarmId) {
                    DismissChallenge(ringing, onSolved)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Snooze · ${ringing.settings.snoozeMinutes} min",
                    color = Glass.TextFaint,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onSnooze)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

/** Whatever Settings says it takes to turn the alarm off, falling back to math if that can't work. */
@Composable
private fun DismissChallenge(ringing: RingingAlarm, onSolved: () -> Unit) {
    val settings = ringing.settings
    var fallback by remember { mutableStateOf(false) }
    val qrCode = settings.qrCode

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            fallback || (settings.dismissMethod == DismissMethod.QR && qrCode == null) -> MathPanel(
                settings.difficulty,
                // Bailing out of a scan costs more than plain math does.
                if (fallback && settings.dismissMethod == DismissMethod.QR) MathChallenge.END_BLOCK_QUESTIONS else MathChallenge.DISMISS_QUESTIONS,
                onSolved,
            )
            settings.dismissMethod == DismissMethod.QR && qrCode != null -> ScanPanel(
                expectedCode = qrCode,
                onSolved = onSolved,
                onGiveUp = { fallback = true },
            )
            settings.dismissMethod == DismissMethod.SHAKE -> ShakePanel(
                target = ShakeChallenge.shakesFor(settings.difficulty),
                onSolved = onSolved,
                onUnavailable = { fallback = true },
            )
            else -> MathPanel(settings.difficulty, MathChallenge.DISMISS_QUESTIONS, onSolved)
        }
    }
}
