package com.evilalarm.app.blocker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evilalarm.app.data.Difficulty
import com.evilalarm.app.math.MathChallenge
import com.evilalarm.app.ui.MathPanel
import com.evilalarm.app.ui.currentTimeMillis
import com.evilalarm.app.ui.formatCountdown
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassBackground
import com.evilalarm.app.ui.theme.GlassButton

@Composable
fun BlockScreen(
    appLabel: String,
    blockUntil: Long,
    attemptsToday: Int,
    difficulty: Difficulty,
    onGoHome: () -> Unit,
    onEndBlock: () -> Unit,
    onContinue: () -> Unit,
) {
    val remaining = blockUntil - currentTimeMillis()
    var solving by remember { mutableStateOf(false) }

    GlassBackground {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(if (solving) 16.dp else 96.dp))

            if (remaining <= 0) {
                Text("Block's over.", color = Glass.TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(8.dp))
                Text("$appLabel is all yours again.", color = Glass.TextSecondary, fontSize = 16.sp)
                Spacer(Modifier.height(40.dp))
                GlassButton("Continue", onClick = onContinue, accent = true, modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    "${appLabel.uppercase()} IS BLOCKED",
                    color = Glass.TextFaint,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(formatCountdown(remaining), color = Glass.TextPrimary, fontSize = 80.sp, fontWeight = FontWeight.Thin)
                Text("You're awake now. Go do something.", color = Glass.TextSecondary, fontSize = 16.sp)
                if (attemptsToday > 1) {
                    Spacer(Modifier.height(6.dp))
                    Text("That's attempt #$attemptsToday today.", color = Glass.Pink, fontSize = 14.sp)
                }

                if (solving) {
                    Spacer(Modifier.height(24.dp))
                    MathPanel(difficulty, MathChallenge.END_BLOCK_QUESTIONS, onSolved = onEndBlock)
                    Spacer(Modifier.height(12.dp))
                    QuietButton("Never mind") { solving = false }
                } else {
                    Spacer(Modifier.height(56.dp))
                    GlassButton("Go home", onClick = onGoHome, accent = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    QuietButton("End block early · solve ${MathChallenge.END_BLOCK_QUESTIONS}") { solving = true }
                }
            }
        }
    }
}

@Composable
private fun QuietButton(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Glass.TextFaint,
        fontSize = 15.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}
