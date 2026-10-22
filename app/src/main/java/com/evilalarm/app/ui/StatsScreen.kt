package com.evilalarm.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evilalarm.app.data.DayStat
import com.evilalarm.app.data.StatsSummary
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassCard
import com.evilalarm.app.ui.theme.ScreenHeader
import com.evilalarm.app.ui.theme.SectionTitle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(stats: StatsSummary, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenHeader("Stats", onBack)

        GlassCard(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${stats.currentStreak}", color = Glass.TextPrimary, fontSize = 96.sp, fontWeight = FontWeight.Thin)
            Text(
                if (stats.currentStreak == 1) "morning in a row" else "mornings in a row",
                color = Glass.TextPrimary,
                fontSize = 18.sp,
            )
            Text("without ending the block early", color = Glass.TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text("Best: ${stats.bestStreak}", color = Glass.TextFaint, fontSize = 13.sp)
        }

        Column {
            SectionTitle("Last 7 days")
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    stats.week.forEach { (date, day) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                color = Glass.TextSecondary,
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            DayDot(day)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Legend(Glass.Success, "Held")
                    Legend(Glass.Pink, "Ended early")
                    Legend(Glass.FillStrong, "No block")
                }
            }
        }

        Column {
            SectionTitle("This week")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        value = "${stats.weekAttempts}",
                        label = "tries to open blocked apps",
                        detail = "${stats.todayAttempts} today",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = stats.weekAverageSecondsToDismiss?.let(::formatSeconds) ?: "–",
                        label = "average time to get up",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        value = "${stats.weekFellBackAsleep}",
                        label = "times you fell back asleep",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = "${stats.weekEndedEarly}",
                        label = "blocks ended early",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DayDot(day: DayStat?) {
    val color = when {
        day == null || !day.blocked -> Glass.FillStrong
        day.endedEarly -> Glass.Pink
        else -> Glass.Success
    }
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(6.dp))
        Text(label, color = Glass.TextFaint, fontSize = 12.sp)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier, detail: String? = null) {
    GlassCard(modifier, contentPadding = PaddingValues(16.dp)) {
        Text(value, color = Glass.TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Light)
        Text(label, color = Glass.TextSecondary, fontSize = 13.sp)
        if (detail != null) {
            Spacer(Modifier.height(4.dp))
            Text(detail, color = Glass.TextFaint, fontSize = 12.sp)
        }
    }
}

/** "45 s" or "2 m 05 s". */
fun formatSeconds(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return if (minutes == 0) "$rest s" else "$minutes m ${rest.toString().padStart(2, '0')} s"
}
