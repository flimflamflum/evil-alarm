package com.evilalarm.app.data

import java.time.LocalDate
import kotlin.math.roundToInt

data class StatsSummary(
    /** Blocked mornings in a row that weren't ended early. Days without a block don't break it. */
    val currentStreak: Int,
    val bestStreak: Int,
    /** The last 7 days, oldest first, with whatever happened on each. */
    val week: List<Pair<LocalDate, DayStat?>>,
    val todayAttempts: Int,
    val weekAttempts: Int,
    val weekAverageSecondsToDismiss: Int?,
    val weekFellBackAsleep: Int,
    val weekEndedEarly: Int,
)

object Stats {
    fun summarize(days: List<DayStat>, today: LocalDate = LocalDate.now()): StatsSummary {
        val byDate = days.associateBy { it.date }

        var run = 0
        var best = 0
        days.filter { it.blocked }.sortedBy { it.date }.forEach { day ->
            if (day.endedEarly) {
                run = 0
            } else {
                run++
                best = maxOf(best, run)
            }
        }

        val week = (6 downTo 0).map { today.minusDays(it.toLong()) }.map { it to byDate[it.toString()] }
        val weekDays = week.mapNotNull { it.second }
        return StatsSummary(
            currentStreak = run,
            bestStreak = best,
            week = week,
            todayAttempts = byDate[today.toString()]?.attempts ?: 0,
            weekAttempts = weekDays.sumOf { it.attempts },
            weekAverageSecondsToDismiss = weekDays.mapNotNull { it.secondsToDismiss }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.roundToInt(),
            weekFellBackAsleep = weekDays.sumOf { it.fellBackAsleep },
            weekEndedEarly = weekDays.count { it.endedEarly },
        )
    }
}
