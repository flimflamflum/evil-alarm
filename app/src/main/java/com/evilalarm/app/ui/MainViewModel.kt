package com.evilalarm.app.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evilalarm.app.alarm.AlarmScheduler
import com.evilalarm.app.alarm.Bedtime
import com.evilalarm.app.data.Alarm
import com.evilalarm.app.data.AppSettings
import com.evilalarm.app.data.DismissMethod
import com.evilalarm.app.data.Stats
import com.evilalarm.app.data.StatsSummary
import com.evilalarm.app.repository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<Application>()
    private val repository = application.repository

    /** Null until the first load, so the list doesn't flash its empty state. */
    val alarms: StateFlow<List<Alarm>?> =
        repository.alarms.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun save(alarm: Alarm) {
        viewModelScope.launch {
            val saved = repository.upsert(alarm)
            AlarmScheduler.schedule(app, saved)
            Bedtime.reschedule(app)
            if (saved.enabled) {
                toast("Rings in ${formatUntil(Instant.now(), saved.nextTrigger().toInstant())}")
            }
        }
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) = save(alarm.copy(enabled = enabled))

    fun delete(alarm: Alarm) {
        viewModelScope.launch {
            AlarmScheduler.cancel(app, alarm.id)
            repository.delete(alarm.id)
            Bedtime.reschedule(app)
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            repository.updateSettings(transform)
            // Cheap, and covers the bedtime setting changing.
            Bedtime.reschedule(app)
        }
    }

    /** Registers the code to scan at alarm time and switches to scanning. */
    fun saveWakeUpCode(code: String) {
        updateSettings { it.copy(qrCode = code, dismissMethod = DismissMethod.QR) }
        toast("Code saved. Now you'll have to go scan it.")
    }

    val blockUntil: StateFlow<Long> =
        repository.blockUntil.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val lastDailyBlock: StateFlow<LocalDate?> =
        repository.lastDailyBlock.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Null until loaded, so first launch can go straight to setup without flashing the alarm list. */
    val setupDone: StateFlow<Boolean?> =
        repository.setupDone.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun markSetupDone() {
        viewModelScope.launch { repository.markSetupDone() }
    }

    val stats: StateFlow<StatsSummary> =
        repository.dayStats.map { Stats.summarize(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats.summarize(emptyList()))

    fun startBlockNow() {
        viewModelScope.launch { repository.startBlock(repository.settings.first().blockMinutes) }
    }

    fun endBlock() {
        viewModelScope.launch { repository.endBlock() }
    }

    fun setAppBlocked(packageName: String, blocked: Boolean) = updateSettings {
        it.copy(blockedPackages = if (blocked) it.blockedPackages + packageName else it.blockedPackages - packageName)
    }

    fun testAlarm() {
        AlarmScheduler.scheduleTest(app, seconds = 10)
        toast("Test alarm rings in 10 seconds and starts a test block. Lock your phone.")
    }

    private fun toast(text: String) = Toast.makeText(app, text, Toast.LENGTH_SHORT).show()
}
