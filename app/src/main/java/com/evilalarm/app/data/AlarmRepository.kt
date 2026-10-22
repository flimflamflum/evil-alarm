package com.evilalarm.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "evil_alarm")

/** Alarms and settings, persisted in a single Preferences DataStore. */
class AlarmRepository(context: Context) {
    private val store = context.applicationContext.dataStore
    private val json = Json { ignoreUnknownKeys = true }
    private val alarmListSerializer = ListSerializer(Alarm.serializer())
    private val dayStatsSerializer = ListSerializer(DayStat.serializer())

    val alarms: Flow<List<Alarm>> = store.data.map { decodeAlarms(it) }

    val settings: Flow<AppSettings> = store.data.map { decodeSettings(it) }

    suspend fun alarm(id: Int): Alarm? = alarms.first().find { it.id == id }

    /** Inserts or replaces [alarm]; a [Alarm.NEW_ID] alarm gets a fresh id. Returns what was saved. */
    suspend fun upsert(alarm: Alarm): Alarm {
        var saved = alarm
        store.edit { prefs ->
            val list = decodeAlarms(prefs).toMutableList()
            if (saved.id == Alarm.NEW_ID) {
                saved = saved.copy(id = (list.maxOfOrNull { it.id } ?: 0) + 1)
            }
            val index = list.indexOfFirst { it.id == saved.id }
            if (index >= 0) list[index] = saved else list += saved
            prefs[ALARMS] = json.encodeToString(alarmListSerializer, list.sortedWith(compareBy({ it.hour }, { it.minute })))
        }
        return saved
    }

    suspend fun delete(id: Int) {
        store.edit { prefs ->
            prefs[ALARMS] = json.encodeToString(alarmListSerializer, decodeAlarms(prefs).filterNot { it.id == id })
        }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val next = transform(decodeSettings(prefs))
            prefs[SOUND] = next.soundId
            prefs[DIFFICULTY] = next.difficulty.name
            prefs[DISMISS_METHOD] = next.dismissMethod.name
            if (next.qrCode == null) prefs.remove(QR_CODE) else prefs[QR_CODE] = next.qrCode
            prefs[SNOOZE_MINUTES] = next.snoozeMinutes
            prefs[WAKE_CHECK] = next.wakeCheck
            prefs[BEDTIME_HOURS] = next.bedtimeHours
            prefs[HARD_MODE] = next.hardMode
            prefs[BLOCK_MINUTES] = next.blockMinutes
            prefs[BLOCKED_PACKAGES] = next.blockedPackages
        }
    }

    /** Epoch millis until which blocked apps stay blocked; 0 when no block has run. */
    val blockUntil: Flow<Long> = store.data.map { it[BLOCK_UNTIL] ?: 0L }

    suspend fun startBlock(minutes: Int) {
        store.edit { it[BLOCK_UNTIL] = System.currentTimeMillis() + minutes * 60_000L }
    }

    /** The date the most recent first-alarm-of-the-day block started, if any. */
    val lastDailyBlock: Flow<LocalDate?> = store.data.map { prefs ->
        prefs[LAST_DAILY_BLOCK]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }

    /**
     * Starts a block if no alarm has started one on [today], and returns whether it did. A test alarm
     * always starts one but doesn't use up the day. An already-running longer block is never shortened.
     */
    suspend fun startDailyBlock(minutes: Int, today: LocalDate, isTest: Boolean): Boolean {
        var started = false
        store.edit { prefs ->
            if (isTest || prefs[LAST_DAILY_BLOCK] != today.toString()) {
                if (!isTest) {
                    prefs[LAST_DAILY_BLOCK] = today.toString()
                    prefs.updateDay(today) { it.copy(blocked = true) }
                }
                val until = System.currentTimeMillis() + minutes * 60_000L
                prefs[BLOCK_UNTIL] = maxOf(prefs[BLOCK_UNTIL] ?: 0L, until)
                started = true
            }
        }
        return started
    }

    val setupDone: Flow<Boolean> = store.data.map { it[SETUP_DONE] ?: false }

    suspend fun markSetupDone() {
        store.edit { it[SETUP_DONE] = true }
    }

    /** Ends the running block early. If it was this morning's block, that breaks the streak. */
    suspend fun endBlock() {
        store.edit { prefs ->
            val today = LocalDate.now()
            val wasRunning = (prefs[BLOCK_UNTIL] ?: 0L) > System.currentTimeMillis()
            if (wasRunning && prefs[LAST_DAILY_BLOCK] == today.toString()) {
                prefs.updateDay(today) { it.copy(endedEarly = true) }
            }
            prefs.remove(BLOCK_UNTIL)
        }
    }

    /** One entry per day that had any activity, oldest first. */
    val dayStats: Flow<List<DayStat>> = store.data.map { decodeStats(it) }

    suspend fun recordBlockedAttempt() {
        store.edit { prefs -> prefs.updateDay(LocalDate.now()) { it.copy(attempts = it.attempts + 1) } }
    }

    /** Only the day's first dismissal counts: that's how long it took to get up. */
    suspend fun recordDismiss(secondsToDismiss: Int) {
        store.edit { prefs ->
            prefs.updateDay(LocalDate.now()) { if (it.secondsToDismiss == null) it.copy(secondsToDismiss = secondsToDismiss) else it }
        }
    }

    suspend fun recordFellBackAsleep() {
        store.edit { prefs -> prefs.updateDay(LocalDate.now()) { it.copy(fellBackAsleep = it.fellBackAsleep + 1) } }
    }

    private fun MutablePreferences.updateDay(day: LocalDate, transform: (DayStat) -> DayStat) {
        val key = day.toString()
        val days = decodeStats(this).toMutableList()
        val index = days.indexOfFirst { it.date == key }
        val updated = transform(if (index >= 0) days[index] else DayStat(key))
        if (index >= 0) days[index] = updated else days += updated
        // A few months is plenty for streaks and weekly numbers.
        this[DAY_STATS] = json.encodeToString(dayStatsSerializer, days.sortedBy { it.date }.takeLast(120))
    }

    private fun decodeStats(prefs: Preferences): List<DayStat> =
        prefs[DAY_STATS]?.let { runCatching { json.decodeFromString(dayStatsSerializer, it) }.getOrNull() }.orEmpty()

    private fun decodeAlarms(prefs: Preferences): List<Alarm> =
        prefs[ALARMS]?.let { runCatching { json.decodeFromString(alarmListSerializer, it) }.getOrNull() }.orEmpty()

    private fun decodeSettings(prefs: Preferences): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            soundId = prefs[SOUND] ?: defaults.soundId,
            difficulty = prefs[DIFFICULTY]?.let { name -> Difficulty.entries.find { it.name == name } } ?: defaults.difficulty,
            dismissMethod = prefs[DISMISS_METHOD]?.let { name -> DismissMethod.entries.find { it.name == name } }
                ?: defaults.dismissMethod,
            qrCode = prefs[QR_CODE],
            snoozeMinutes = prefs[SNOOZE_MINUTES] ?: defaults.snoozeMinutes,
            wakeCheck = prefs[WAKE_CHECK] ?: defaults.wakeCheck,
            bedtimeHours = prefs[BEDTIME_HOURS] ?: defaults.bedtimeHours,
            hardMode = prefs[HARD_MODE] ?: defaults.hardMode,
            blockMinutes = prefs[BLOCK_MINUTES] ?: defaults.blockMinutes,
            blockedPackages = prefs[BLOCKED_PACKAGES] ?: defaults.blockedPackages,
        )
    }

    private companion object {
        val ALARMS = stringPreferencesKey("alarms")
        val SOUND = stringPreferencesKey("sound")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        val HARD_MODE = booleanPreferencesKey("hard_mode")
        val BLOCK_MINUTES = intPreferencesKey("block_minutes")
        val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        val BLOCK_UNTIL = longPreferencesKey("block_until")
        val LAST_DAILY_BLOCK = stringPreferencesKey("last_daily_block")
        val SETUP_DONE = booleanPreferencesKey("setup_done")
        val DISMISS_METHOD = stringPreferencesKey("dismiss_method")
        val QR_CODE = stringPreferencesKey("qr_code")
        val WAKE_CHECK = booleanPreferencesKey("wake_check")
        val BEDTIME_HOURS = intPreferencesKey("bedtime_hours")
        val DAY_STATS = stringPreferencesKey("day_stats")
    }
}
