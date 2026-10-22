package com.evilalarm.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evilalarm.app.data.Difficulty
import com.evilalarm.app.math.MathChallenge
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassCard
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** A run of [questions] math problems on a glass keypad; [onSolved] fires after the last one. */
@Composable
fun MathPanel(
    difficulty: Difficulty,
    questions: Int,
    onSolved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var solved by remember { mutableIntStateOf(0) }
    var question by remember { mutableStateOf(MathChallenge.question(difficulty)) }
    var input by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (input.isEmpty()) return
        if (input.toIntOrNull() == question.answer) {
            solved++
            if (solved >= questions) {
                onSolved()
                return
            }
            wrong = false
        } else {
            // A wrong answer burns the question: no brute-forcing your way out.
            wrong = true
            scope.launch { shake.animateTo(0f, spring(dampingRatio = 0.2f, stiffness = 1_200f), initialVelocity = 2_500f) }
        }
        question = MathChallenge.question(difficulty)
        input = ""
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shake.value.roundToInt(), 0) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(questions) { i ->
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (i < solved) Glass.AccentBrush else SolidColor(Glass.FillStrong)),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(question.text, color = Glass.TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Glass.Fill)
                .border(
                    1.dp,
                    if (wrong) SolidColor(Glass.Danger.copy(alpha = 0.7f)) else Glass.Border,
                    RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                input.ifEmpty { if (wrong) "Wrong. New one." else "?" },
                color = when {
                    input.isNotEmpty() -> Glass.TextPrimary
                    wrong -> Glass.Danger
                    else -> Glass.TextFaint
                },
                fontSize = if (input.isEmpty() && wrong) 16.sp else 28.sp,
            )
        }
        Spacer(Modifier.height(16.dp))
        Keypad(
            onDigit = { if (input.length < 6) input += it },
            onBackspace = { input = input.dropLast(1) },
            onSubmit = ::submit,
        )
    }
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit, onSubmit: () -> Unit) {
    val rows = listOf("123", "456", "789", "⌫0✓")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    val isSubmit = key == '✓'
                    Box(
                        Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSubmit) Glass.AccentBrush else SolidColor(Glass.FillStrong))
                            .border(1.dp, Glass.Border, RoundedCornerShape(18.dp))
                            .clickable {
                                when (key) {
                                    '⌫' -> onBackspace()
                                    '✓' -> onSubmit()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key.toString(), color = Glass.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
    }
}
