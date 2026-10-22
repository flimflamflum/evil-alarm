package com.evilalarm.app.ring

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evilalarm.app.data.Difficulty
import com.evilalarm.app.ui.CodeScanner
import com.evilalarm.app.ui.rememberCameraPermission
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassCard
import kotlin.math.sqrt

object ShakeChallenge {
    fun shakesFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 20
        Difficulty.MEDIUM -> 40
        Difficulty.HARD -> 70
    }

    /** A jolt this strong (in g, gravity included) counts as one shake. */
    const val THRESHOLD_G = 2.3f
    const val MIN_GAP_MS = 250L
}

/** Shake the phone [target] times. Calls [onUnavailable] on phones without an accelerometer. */
@Composable
fun ShakePanel(target: Int, onSolved: () -> Unit, onUnavailable: () -> Unit) {
    val context = LocalContext.current
    var shakes by remember { mutableIntStateOf(0) }
    val currentOnSolved by rememberUpdatedState(onSolved)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)

    DisposableEffect(Unit) {
        val sensors = context.getSystemService(SensorManager::class.java)
        val accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            currentOnUnavailable()
            return@DisposableEffect onDispose { }
        }
        var lastShakeAt = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                val now = SystemClock.elapsedRealtime()
                if (g > ShakeChallenge.THRESHOLD_G && now - lastShakeAt > ShakeChallenge.MIN_GAP_MS && shakes < target) {
                    lastShakeAt = now
                    shakes++
                    if (shakes == target) currentOnSolved()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensors.unregisterListener(listener) }
    }

    val progress by animateFloatAsState(shakes / target.toFloat(), label = "shake progress")
    GlassCard(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SHAKE IT", color = Glass.TextFaint, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text("${target - shakes}", color = Glass.TextPrimary, fontSize = 96.sp, fontWeight = FontWeight.Thin)
        Text("shakes to go", color = Glass.TextSecondary, fontSize = 16.sp)
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Glass.FillStrong),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Glass.AccentBrush),
            )
        }
    }
}

/**
 * Scan the code you registered in Settings. [onGiveUp] switches to the math fallback, for a broken
 * camera or a lost code.
 */
@Composable
fun ScanPanel(expectedCode: String, onSolved: () -> Unit, onGiveUp: () -> Unit) {
    val (cameraAllowed, askForCamera) = rememberCameraPermission()
    var torchOn by remember { mutableStateOf(false) }
    var wrongCode by remember { mutableStateOf(false) }
    var solved by remember { mutableStateOf(false) }

    GlassCard(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SCAN YOUR CODE", color = Glass.TextFaint, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        val shape = RoundedCornerShape(20.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(Glass.Fill)
                .border(1.dp, Glass.Border, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (cameraAllowed) {
                CodeScanner(
                    onCode = { code ->
                        if (solved) return@CodeScanner
                        if (code == expectedCode) {
                            solved = true
                            onSolved()
                        } else {
                            wrongCode = true
                        }
                    },
                    torchOn = torchOn,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            } else {
                Text(
                    "Camera access is off.\nTap to allow it.",
                    color = Glass.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable(onClick = askForCamera).padding(24.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (wrongCode) "That's not your code. Find the right one." else "Get up and go to where you put it.",
            color = if (wrongCode) Glass.Danger else Glass.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        if (cameraAllowed) {
            Text(
                if (torchOn) "Flashlight off" else "Flashlight on",
                color = Glass.Pink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { torchOn = !torchOn }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
    Text(
        "Can't scan it? Solve 5 math problems instead",
        color = Glass.TextFaint,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onGiveUp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
