package com.evilalarm.app.data

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    /** ISO days of week (1 = Monday … 7 = Sunday). Empty means the alarm rings once. */
    val days: Set<Int> = emptySet(),
    val enabled: Boolean = true,
    val label: String = "",
) {
    val isRepeating: Boolean get() = days.isNotEmpty()

    /** The next moment strictly after [from] at which this alarm should ring. */
    fun nextTrigger(from: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        var candidate = from.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!candidate.isAfter(from)) candidate = candidate.plusDays(1)
        if (isRepeating) {
            while (candidate.dayOfWeek.value !in days) candidate = candidate.plusDays(1)
        }
        return candidate
    }

    companion object {
        /** Id used for an alarm that has not been saved yet. */
        const val NEW_ID = 0
    }
}
