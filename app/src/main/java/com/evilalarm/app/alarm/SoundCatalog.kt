package com.evilalarm.app.alarm

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager

/** Lets the vibration pulse in time with a track. */
data class BeatSync(val bpm: Double, val beatsPerPulse: Int, val firstBeatMs: Int) {
    val pulseMs: Double get() = 60_000.0 / bpm * beatsPerPulse
}

data class AlarmSound(val id: String, val name: String, val beat: BeatSync? = null)

/**
 * Alarm sounds: every audio file in `assets/sounds/` plus the phone's default alarm.
 * Dropping an mp3 into that folder is all it takes to add a sound; [bundledInfo] can give it
 * a nicer name and a tempo for beat-synced vibration.
 */
object SoundCatalog {
    const val SYSTEM_DEFAULT_ID = "system:default"
    const val EVIL_ALARM_ID = "asset:evil_alarm.mp3"

    private const val ASSET_DIR = "sounds"
    private const val ASSET_PREFIX = "asset:"
    private val audioExtensions = setOf("mp3", "ogg", "m4a", "wav")

    private data class BundledInfo(val name: String, val beat: BeatSync?)

    private val bundledInfo = mapOf(
        // 200 BPM, 20 bars. The first ~25 ms is MP3 encoder silence, so beat one lands there.
        "evil_alarm.mp3" to BundledInfo("EVIL ALARM", BeatSync(bpm = 200.0, beatsPerPulse = 2, firstBeatMs = 25)),
    )

    /** Display names for tracks listed after the phone default, where the file name reads badly. */
    private val extraNames = mapOf(
        "cant_hold_us_x_bad_piggies.mp3" to "Can't Hold Us X Bad Piggies",
        "morning_flower_trap.mp3" to "Morning Flower Trap",
    )

    fun all(context: Context): List<AlarmSound> {
        val files = context.assets.list(ASSET_DIR).orEmpty()
            .filter { it.substringAfterLast('.').lowercase() in audioExtensions }
        // Named tracks first, then the phone's default, then anything else that was dropped in.
        val (named, others) = files.partition { it in bundledInfo }
        val namedSounds = named.map { file ->
            val info = bundledInfo.getValue(file)
            AlarmSound(ASSET_PREFIX + file, info.name, info.beat)
        }.sortedBy { it.name.lowercase() }
        val otherSounds = others.map { AlarmSound(ASSET_PREFIX + it, displayName(it)) }.sortedBy { it.name.lowercase() }
        return namedSounds + AlarmSound(SYSTEM_DEFAULT_ID, "Phone default alarm") + otherSounds
    }

    fun find(context: Context, id: String): AlarmSound? = all(context).find { it.id == id }

    /** Points [player] at sound [id]. Throws if the sound can't be opened. */
    fun setDataSource(context: Context, player: MediaPlayer, id: String) {
        if (id.startsWith(ASSET_PREFIX)) {
            context.assets.openFd("$ASSET_DIR/${id.removePrefix(ASSET_PREFIX)}").use { fd ->
                player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
        } else {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            player.setDataSource(context, uri)
        }
    }

    private fun displayName(fileName: String): String =
        extraNames[fileName] ?: fileName.substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .replaceFirstChar { it.uppercase() }
}
