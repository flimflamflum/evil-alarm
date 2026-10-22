package com.evilalarm.app.data

import kotlinx.serialization.Serializable

/** What happened on one morning, for streaks and stats. */
@Serializable
data class DayStat(
    /** ISO date, e.g. "2026-10-21". */
    val date: String,
    /** The first alarm of the day started an app block. */
    val blocked: Boolean = false,
    /** That block was ended early with math. */
    val endedEarly: Boolean = false,
    /** Times a blocked app was opened during a block. */
    val attempts: Int = 0,
    /** From the first alarm ringing to it being turned off. */
    val secondsToDismiss: Int? = null,
    /** Wake-up checks that went unanswered, so the alarm rang again. */
    val fellBackAsleep: Int = 0,
)
