package com.evilalarm.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/** Wall-clock time that recomposes on every [tickMillis] boundary. */
@Composable
fun currentTimeMillis(tickMillis: Long = 1_000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(tickMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(tickMillis - now % tickMillis)
        }
    }
    return now
}

/** "27:41", or "1:05:00" past an hour; rounded up so it never shows 0:00 early. */
fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis + 999).coerceAtLeast(0) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds % 3_600 / 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$seconds" else "$minutes:$seconds"
}

/** Returns the clock text plus "AM"/"PM" (null in 24-hour mode). */
fun formatTime(hour: Int, minute: Int, is24h: Boolean): Pair<String, String?> {
    val mm = minute.toString().padStart(2, '0')
    if (is24h) return "${hour.toString().padStart(2, '0')}:$mm" to null
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "$h12:$mm" to if (hour < 12) "AM" else "PM"
}

private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun formatDays(days: Set<Int>): String = when {
    days.isEmpty() -> "Once"
    days.size == 7 -> "Every day"
    days == setOf(1, 2, 3, 4, 5) -> "Weekdays"
    days == setOf(6, 7) -> "Weekends"
    else -> days.sorted().joinToString(", ") { dayNames[it - 1] }
}

/** "7 h 32 min", rounded up to the minute. */
fun formatUntil(from: Instant, to: Instant): String {
    val totalMinutes = (Duration.between(from, to).seconds + 59).coerceAtLeast(0) / 60
    val days = totalMinutes / (24 * 60)
    val hours = totalMinutes % (24 * 60) / 60
    val minutes = totalMinutes % 60
    return buildList {
        if (days > 0) add("$days d")
        if (hours > 0) add("$hours h")
        if (minutes > 0 || isEmpty()) add("$minutes min")
    }.joinToString(" ")
}
