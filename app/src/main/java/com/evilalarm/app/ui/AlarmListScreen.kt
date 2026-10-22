package com.evilalarm.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evilalarm.app.blocker.rememberBlockerEnabled
import com.evilalarm.app.data.Alarm
import com.evilalarm.app.data.StatsSummary
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassCard
import com.evilalarm.app.ui.theme.GlassIconButton
import com.evilalarm.app.ui.theme.GlassSwitch
import kotlinx.coroutines.delay
import java.time.Instant

@Composable
fun AlarmListScreen(
    alarms: List<Alarm>?,
    blockUntil: Long,
    blockedPackages: Set<String>,
    stats: StatsSummary,
    onAdd: () -> Unit,
    onEdit: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onOpenBlocker: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val is24h = DateFormat.is24HourFormat(LocalContext.current)
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = Instant.now()
        }
    }
    val nextRing = alarms.orEmpty().filter { it.enabled }.minOfOrNull { it.nextTrigger().toInstant() }
    val bars = WindowInsets.systemBars.asPaddingValues()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = bars.calculateTopPadding() + 16.dp,
                bottom = bars.calculateBottomPadding() + 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Evil Alarm", color = Glass.TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Light)
                        Text(
                            nextRing?.let { "Next alarm in ${formatUntil(now, it)}" } ?: "No alarms set",
                            color = Glass.TextSecondary,
                            fontSize = 14.sp,
                        )
                    }
                    GlassIconButton(Icons.Rounded.Settings, "Settings", onOpenSettings)
                }
            }

            item { PermissionsCard(onlyMissing = true) }

            item { BlockerCard(blockUntil, blockedPackages, onOpenBlocker) }

            item {
                ShortcutCard(
                    title = "Streak",
                    status = when (stats.currentStreak) {
                        0 -> "No streak yet · best ${stats.bestStreak}"
                        1 -> "1 morning · best ${stats.bestStreak}"
                        else -> "${stats.currentStreak} mornings · best ${stats.bestStreak}"
                    },
                    onClick = onOpenStats,
                )
            }

            if (alarms != null && alarms.isEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No alarms yet", color = Glass.TextPrimary, fontSize = 18.sp)
                        Text("Tap + to set one. It will not be gentle.", color = Glass.TextSecondary, fontSize = 14.sp)
                    }
                }
            }

            items(alarms.orEmpty(), key = { it.id }) { alarm ->
                AlarmRow(
                    alarm = alarm,
                    is24h = is24h,
                    onClick = { onEdit(alarm) },
                    onToggle = { onToggle(alarm, it) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        GlassIconButton(
            icon = Icons.Rounded.Add,
            contentDescription = "Add alarm",
            onClick = onAdd,
            size = 64.dp,
            accent = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun BlockerCard(blockUntil: Long, blockedPackages: Set<String>, onClick: () -> Unit) {
    val enabled = rememberBlockerEnabled()
    val now = currentTimeMillis()
    val blockedCount = rememberInstalledLabels(blockedPackages).value.size
    val blocking = enabled && blockUntil > now
    val status = when {
        !enabled -> "Off · tap to set up"
        blocking -> "Blocking · ${formatCountdown(blockUntil - now)} left"
        else -> "Ready · $blockedCount ${if (blockedCount == 1) "app" else "apps"}"
    }
    ShortcutCard("App blocker", status, onClick, highlight = blocking)
}

@Composable
private fun ShortcutCard(title: String, status: String, onClick: () -> Unit, highlight: Boolean = false) {
    GlassCard(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Glass.TextPrimary, fontSize = 16.sp)
                Text(status, color = if (highlight) Glass.Pink else Glass.TextSecondary, fontSize = 13.sp)
            }
            Text("›", color = Glass.TextFaint, fontSize = 26.sp)
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    is24h: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (time, amPm) = formatTime(alarm.hour, alarm.minute, is24h)
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (alarm.enabled) 1f else 0.55f),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(time, color = Glass.TextPrimary, fontSize = 44.sp, fontWeight = FontWeight.Light)
                    if (amPm != null) {
                        Text(
                            amPm,
                            color = Glass.TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
                        )
                    }
                }
                Text(
                    listOfNotNull(alarm.label.ifBlank { null }, formatDays(alarm.days)).joinToString("  ·  "),
                    color = Glass.TextSecondary,
                    fontSize = 14.sp,
                )
            }
            GlassSwitch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}
