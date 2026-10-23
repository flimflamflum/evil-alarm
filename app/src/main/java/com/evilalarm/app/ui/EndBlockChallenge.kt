package com.evilalarm.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.evilalarm.app.data.AppSettings
import com.evilalarm.app.data.BlockerUnlockMethod
import com.evilalarm.app.data.Difficulty
import com.evilalarm.app.math.MathChallenge
import com.evilalarm.app.ring.ScanPanel

/**
 * Whatever the blocker settings say it takes to end a block early. Giving up on a scan (or having
 * no code to scan) means [MathChallenge.END_BLOCK_SCAN_FALLBACK_QUESTIONS] hard problems instead.
 */
@Composable
fun EndBlockChallenge(settings: AppSettings, onSolved: () -> Unit, modifier: Modifier = Modifier) {
    var fallback by remember { mutableStateOf(false) }
    val code = settings.blockerCode

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            settings.blockerUnlock == BlockerUnlockMethod.MATH ->
                MathPanel(settings.blockerDifficulty, MathChallenge.END_BLOCK_QUESTIONS, onSolved)
            !fallback && code != null -> ScanPanel(
                expectedCode = code,
                fallbackQuestions = MathChallenge.END_BLOCK_SCAN_FALLBACK_QUESTIONS,
                onSolved = onSolved,
                onGiveUp = { fallback = true },
            )
            else -> MathPanel(Difficulty.HARD, MathChallenge.END_BLOCK_SCAN_FALLBACK_QUESTIONS, onSolved)
        }
    }
}

/** Label for the button that opens [EndBlockChallenge]. */
fun endBlockLabel(settings: AppSettings): String = when {
    settings.blockerUnlock == BlockerUnlockMethod.MATH -> "End block early · solve ${MathChallenge.END_BLOCK_QUESTIONS}"
    settings.blockerCode != null -> "End block early · scan your code"
    else -> "End block early · solve ${MathChallenge.END_BLOCK_SCAN_FALLBACK_QUESTIONS}"
}
