package com.evilalarm.app.alarm

import com.evilalarm.app.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RingingAlarm(
    val alarmId: Int,
    val label: String,
    /** Settings as of when it started ringing: sound, how to turn it off, snooze length… */
    val settings: AppSettings,
    /** Length of the app block this ring started, or null if it didn't start one. */
    val blockMinutes: Int?,
    /** When ringing started, for "how long it took you to get up". */
    val startedAt: Long,
) {
    val isTest: Boolean get() = alarmId == AlarmScheduler.TEST_ALARM_ID
}

/** What is ringing right now, shared between [AlarmService] and the ring screen. */
object RingState {
    private val _ringing = MutableStateFlow<RingingAlarm?>(null)
    val ringing: StateFlow<RingingAlarm?> = _ringing.asStateFlow()

    internal fun set(alarm: RingingAlarm?) {
        _ringing.value = alarm
    }
}
