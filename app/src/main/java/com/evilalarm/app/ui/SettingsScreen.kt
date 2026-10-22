package com.evilalarm.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evilalarm.app.alarm.AlarmSound
import com.evilalarm.app.alarm.Bedtime
import com.evilalarm.app.alarm.SoundCatalog
import com.evilalarm.app.alarm.WakeCheck
import com.evilalarm.app.data.AppSettings
import com.evilalarm.app.data.Difficulty
import com.evilalarm.app.data.DismissMethod
import com.evilalarm.app.math.MathChallenge
import com.evilalarm.app.ring.ShakeChallenge
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassButton
import com.evilalarm.app.ui.theme.GlassCard
import com.evilalarm.app.ui.theme.GlassIconButton
import com.evilalarm.app.ui.theme.GlassSegmented
import com.evilalarm.app.ui.theme.GlassSwitch
import com.evilalarm.app.ui.theme.ScreenHeader
import com.evilalarm.app.ui.theme.SectionTitle

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onTestAlarm: () -> Unit,
    onRunSetup: () -> Unit,
    onScanCode: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val sounds = remember { SoundCatalog.all(context) }
    val preview = rememberSoundPreview()

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenHeader("Settings", onBack)

        Column {
            SectionTitle("Alarm sound")
            GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp)) {
                sounds.forEach { sound ->
                    SoundRow(
                        sound = sound,
                        selected = sound.id == settings.soundId,
                        playing = preview.playingId == sound.id,
                        onSelect = { onChange { it.copy(soundId = sound.id) } },
                        onPreview = { preview.toggle(sound.id) },
                    )
                }
            }
        }

        Column {
            SectionTitle("Turn it off by")
            GlassCard(Modifier.fillMaxWidth()) {
                GlassSegmented(
                    options = DismissMethod.entries,
                    selected = settings.dismissMethod,
                    label = { it.label },
                    onSelect = { method ->
                        // Scanning needs a code first; registering one switches the method over.
                        if (method == DismissMethod.QR && settings.qrCode == null) onScanCode()
                        else onChange { it.copy(dismissMethod = method) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "DIFFICULTY",
                    color = Glass.TextFaint,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                GlassSegmented(
                    options = Difficulty.entries,
                    selected = settings.difficulty,
                    label = { it.label },
                    onSelect = { difficulty -> onChange { it.copy(difficulty = difficulty) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                val example = remember(settings.difficulty) { MathChallenge.question(settings.difficulty).text }
                Text(
                    when (settings.dismissMethod) {
                        DismissMethod.MATH ->
                            "Solve ${MathChallenge.DISMISS_QUESTIONS} problems like  $example  to turn the alarm off."
                        DismissMethod.SHAKE ->
                            "Shake your phone ${ShakeChallenge.shakesFor(settings.difficulty)} times to turn the alarm off."
                        DismissMethod.QR ->
                            "Scan your code to turn the alarm off, so put it somewhere you have to walk to. " +
                                "Can't scan it? Then it's ${MathChallenge.END_BLOCK_QUESTIONS} problems like  $example ."
                    },
                    color = Glass.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                if (settings.dismissMethod == DismissMethod.QR) {
                    Text(
                        "Scan a new code",
                        color = Glass.Pink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onScanCode)
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                    )
                }
            }
        }

        Column {
            SectionTitle("Wake-up check")
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Prove you're up", color = Glass.TextPrimary, fontSize = 16.sp)
                        Text(
                            "${WakeCheck.CHECK_AFTER_MINUTES} minutes after you turn the alarm off, tap the notification " +
                                "within ${WakeCheck.ANSWER_WITHIN_MINUTES} minutes or the alarm goes off again.",
                            color = Glass.TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    GlassSwitch(settings.wakeCheck, onCheckedChange = { on -> onChange { it.copy(wakeCheck = on) } })
                }
            }
        }

        Column {
            SectionTitle("Bedtime reminder")
            GlassCard(Modifier.fillMaxWidth()) {
                GlassSegmented(
                    options = listOf(0) + Bedtime.HOUR_OPTIONS,
                    selected = settings.bedtimeHours,
                    label = { if (it == 0) "Off" else "$it h" },
                    onSelect = { hours -> onChange { it.copy(bedtimeHours = hours) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (settings.bedtimeHours == 0) {
                        "No bedtime reminder."
                    } else {
                        "A nudge to put your phone down ${settings.bedtimeHours} hours before your first alarm of the day."
                    },
                    color = Glass.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        Column {
            SectionTitle("Snooze")
            GlassCard(Modifier.fillMaxWidth()) {
                GlassSegmented(
                    options = listOf(3, 5, 10),
                    selected = settings.snoozeMinutes,
                    label = { "$it min" },
                    onSelect = { minutes -> onChange { it.copy(snoozeMinutes = minutes) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Column {
            SectionTitle("Hard mode")
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hard mode", color = Glass.TextPrimary, fontSize = 16.sp)
                        Text(
                            "While apps are blocked, also lock the settings that could switch the blocker off. " +
                                "Takes effect once the app blocker is built.",
                            color = Glass.TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    GlassSwitch(settings.hardMode, onCheckedChange = { on -> onChange { it.copy(hardMode = on) } })
                }
            }
        }

        Column {
            SectionTitle("Permissions")
            PermissionsCard(onlyMissing = false)
        }

        GlassButton("Run setup again", onClick = onRunSetup, modifier = Modifier.fillMaxWidth())
        GlassButton("Test alarm in 10 seconds", onClick = onTestAlarm, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SoundRow(
    sound: AlarmSound,
    selected: Boolean,
    playing: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (selected) Glass.Pink else Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Glass.AccentBrush),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            sound.name,
            color = if (selected) Glass.TextPrimary else Glass.TextSecondary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        GlassIconButton(
            icon = if (playing) Icons.Rounded.Close else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) "Stop preview" else "Preview",
            onClick = onPreview,
            size = 36.dp,
        )
    }
}
