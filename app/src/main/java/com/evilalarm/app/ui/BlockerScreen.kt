package com.evilalarm.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evilalarm.app.blocker.Blocker
import com.evilalarm.app.blocker.rememberBlockerEnabled
import com.evilalarm.app.data.AppSettings
import com.evilalarm.app.math.MathChallenge
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassButton
import com.evilalarm.app.ui.theme.GlassCard
import com.evilalarm.app.ui.theme.GlassSegmented
import com.evilalarm.app.ui.theme.ScreenHeader
import com.evilalarm.app.ui.theme.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun BlockerScreen(
    settings: AppSettings,
    blockUntil: Long,
    lastDailyBlock: LocalDate?,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onStartBlock: () -> Unit,
    onEndBlock: () -> Unit,
    onPickApps: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val enabled = rememberBlockerEnabled()
    val now = currentTimeMillis()
    val active = blockUntil > now
    var solving by remember { mutableStateOf(false) }
    LaunchedEffect(active) { if (!active) solving = false }
    val blockedLabels by rememberInstalledLabels(settings.blockedPackages)

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenHeader("App blocker", onBack)

        if (!enabled) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Turn on the blocker", color = Glass.TextPrimary, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Android needs you to switch Evil Alarm on under Accessibility. It's only told which app is open, never what's on your screen.",
                    color = Glass.TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(16.dp))
                GlassButton(
                    "Open Accessibility settings",
                    onClick = { context.startActivity(Blocker.accessibilitySettingsIntent()) },
                    accent = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Look for \"Evil Alarm app blocker\" (sometimes under Installed or Downloaded apps). " +
                        "If the switch is greyed out: open Evil Alarm's App info, tap ⋮ → Allow restricted settings, then try again.",
                    color = Glass.TextFaint,
                    fontSize = 12.sp,
                )
            }
        }

        GlassCard(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (active) {
                SectionTitle("Blocking now")
                Text(formatCountdown(blockUntil - now), color = Glass.TextPrimary, fontSize = 56.sp, fontWeight = FontWeight.Thin)
                Spacer(Modifier.height(12.dp))
                if (!solving) {
                    GlassButton(
                        "End block early · solve ${MathChallenge.END_BLOCK_QUESTIONS}",
                        onClick = { solving = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                val usedToday = lastDailyBlock == LocalDate.now()
                Text(
                    if (usedToday) "Today's block is done" else "Waiting for your alarm",
                    color = Glass.TextPrimary,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (usedToday) {
                        "Tomorrow's first alarm starts the next ${settings.blockMinutes}-minute block."
                    } else {
                        "Your first alarm today locks these apps for ${settings.blockMinutes} minutes the moment it rings."
                    },
                    color = Glass.TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(16.dp))
                GlassButton(
                    "Start a ${settings.blockMinutes}-minute block now",
                    onClick = onStartBlock,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (active && solving) {
            MathPanel(
                settings.difficulty,
                MathChallenge.END_BLOCK_QUESTIONS,
                onSolved = {
                    solving = false
                    onEndBlock()
                },
            )
        }

        Column {
            SectionTitle("Block length")
            GlassCard(Modifier.fillMaxWidth()) {
                GlassSegmented(
                    options = listOf(15, 30, 45, 60),
                    selected = settings.blockMinutes,
                    label = { "$it min" },
                    onSelect = { minutes -> onChange { it.copy(blockMinutes = minutes) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Column {
            SectionTitle("Blocked apps")
            GlassCard(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                onClick = onPickApps,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (blockedLabels.size) {
                                0 -> "No apps picked"
                                1 -> "1 app"
                                else -> "${blockedLabels.size} apps"
                            },
                            color = Glass.TextPrimary,
                            fontSize = 16.sp,
                        )
                        if (blockedLabels.isNotEmpty()) {
                            Text(blockedLabels.joinToString(", "), color = Glass.TextSecondary, fontSize = 13.sp, maxLines = 2)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Edit", color = Glass.Pink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Names of the [packages] that are actually installed, sorted. */
@Composable
fun rememberInstalledLabels(packages: Set<String>): State<List<String>> {
    val context = LocalContext.current
    return produceState(initialValue = emptyList(), packages) {
        value = withContext(Dispatchers.IO) {
            packages.mapNotNull { Blocker.installedLabel(context, it) }.sortedBy { it.lowercase() }
        }
    }
}
