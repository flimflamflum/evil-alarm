package com.evilalarm.app.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.evilalarm.app.appScope
import com.evilalarm.app.repository
import com.evilalarm.app.ring.RingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Foreground service that makes the noise: alarm volume pinned at max, the chosen sound on loop,
 * and vibration, until the ring screen sends [dismiss] or [snooze].
 */
class AlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var tone: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var volumeJob: Job? = null
    private var vibrationJob: Job? = null
    private var savedVolume: Int? = null
    private var noiseStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> ring(intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, AlarmScheduler.TEST_ALARM_ID))
            ACTION_SNOOZE -> snooze()
            ACTION_DISMISS -> dismiss()
            else -> if (RingState.ringing.value == null) stopSelf()
        }
        // If the system kills us mid-ring, redeliver the ring intent so the alarm comes back.
        return START_REDELIVER_INTENT
    }

    private fun ring(alarmId: Int) {
        // Must happen within seconds of startForegroundService(), before any disk I/O.
        ServiceCompat.startForeground(
            this,
            Notifications.RINGING_ID,
            Notifications.ringing(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        // A second alarm firing mid-ring just takes over the screen; the noise keeps going.
        val firstRing = !noiseStarted
        noiseStarted = true
        if (firstRing) acquireWakeLock()
        // Ringing again answers any pending wake-up check.
        WakeCheck.cancel(this)

        scope.launch {
            val alarm = repository.alarm(alarmId)
            val settings = repository.settings.first()
            // The day's first alarm locks the blocked apps the moment it rings.
            val blockStarted = repository.startDailyBlock(
                minutes = settings.blockMinutes,
                today = LocalDate.now(),
                isTest = alarmId == AlarmScheduler.TEST_ALARM_ID,
            )
            val previous = RingState.ringing.value
            RingState.set(
                RingingAlarm(
                    alarmId = alarmId,
                    label = alarm?.label.orEmpty(),
                    settings = settings,
                    blockMinutes = if (blockStarted) settings.blockMinutes else previous?.blockMinutes,
                    startedAt = previous?.startedAt ?: System.currentTimeMillis(),
                ),
            )

            // Re-arm repeating alarms for their next day; one-off alarms switch themselves off.
            if (alarm != null) {
                if (alarm.isRepeating) AlarmScheduler.schedule(this@AlarmService, alarm)
                else repository.upsert(alarm.copy(enabled = false))
            }
            Bedtime.reschedule(this@AlarmService)

            if (firstRing) startNoise(settings.soundId)
            // Works when the app is in the foreground; otherwise the full-screen notification takes over.
            runCatching { startActivity(RingActivity.intent(this@AlarmService)) }
        }
    }

    private fun snooze() {
        RingState.ringing.value?.let { AlarmScheduler.scheduleSnooze(this, it.alarmId, it.settings.snoozeMinutes) }
        stopRinging()
    }

    private fun dismiss() {
        val ringing = RingState.ringing.value
        if (ringing != null) {
            if (ringing.settings.wakeCheck) WakeCheck.schedule(this, ringing.alarmId, ringing.isTest)
            if (!ringing.isTest) {
                val seconds = ((System.currentTimeMillis() - ringing.startedAt) / 1_000).toInt()
                // The service is about to stop, so write from the app's scope.
                val app = applicationContext
                app.appScope.launch { app.repository.recordDismiss(seconds) }
            }
        }
        stopRinging()
    }

    private fun stopRinging() {
        RingState.set(null)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopNoise()
        RingState.set(null)
        scope.cancel()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EvilAlarm:ringing")
            .apply { acquire(60 * 60 * 1000L) }
    }

    private fun startNoise(soundId: String) {
        pinVolumeAtMax()
        val chosen = createPlayer(soundId)
        player = chosen ?: createPlayer(SoundCatalog.SYSTEM_DEFAULT_ID)
        if (player == null) {
            // Last resort so the alarm is never silent.
            tone = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
                .apply { startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK) }
        }
        val beat = SoundCatalog.find(this, soundId)?.beat
        if (chosen != null && beat != null) startBeatVibration(chosen, beat) else startVibration()
    }

    /** Maxes the alarm stream and keeps putting it back if anyone turns it down. */
    private fun pinVolumeAtMax() {
        val audio = getSystemService(AudioManager::class.java)
        savedVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        volumeJob = scope.launch {
            while (isActive) {
                if (audio.getStreamVolume(AudioManager.STREAM_ALARM) < max) {
                    runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, max, 0) }
                }
                delay(500)
            }
        }
    }

    private fun createPlayer(soundId: String): MediaPlayer? {
        val mediaPlayer = MediaPlayer()
        return try {
            mediaPlayer.setAudioAttributes(alarmAudioAttributes)
            SoundCatalog.setDataSource(this, mediaPlayer, soundId)
            mediaPlayer.isLooping = true
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't play sound $soundId", e)
            mediaPlayer.release()
            null
        }
    }

    private fun startVibration() {
        val v = systemVibrator()
        v.vibrateAlarm(VibrationEffect.createWaveform(longArrayOf(0, 800, 400), 0))
        vibrator = v
    }

    /**
     * Pulses on every [BeatSync.beatsPerPulse] beats, timed from the player's actual position so it
     * stays locked to the music across loops instead of drifting.
     */
    private fun startBeatVibration(player: MediaPlayer, beat: BeatSync) {
        val v = systemVibrator()
        vibrator = v
        val pulse = VibrationEffect.createOneShot(
            BEAT_PULSE_MS,
            if (v.hasAmplitudeControl()) 255 else VibrationEffect.DEFAULT_AMPLITUDE,
        )
        vibrationJob = scope.launch(Dispatchers.Default) {
            val duration = runCatching { player.duration }.getOrDefault(0)
            var lastPulse = -1L
            var lastPosition = 0
            while (isActive) {
                val position = runCatching { player.currentPosition }.getOrNull() ?: break
                if (position < lastPosition) lastPulse = -1L // the track looped
                lastPosition = position
                val sinceFirstBeat = position - beat.firstBeatMs
                if (sinceFirstBeat >= 0) {
                    val index = (sinceFirstBeat / beat.pulseMs).toLong()
                    if (index != lastPulse) {
                        lastPulse = index
                        // Skip a pulse that would land in the last moments before the loop restarts,
                        // otherwise it doubles up with the pulse on beat one.
                        val nearLoopEnd = duration > 0 && duration - position < beat.pulseMs / 2
                        if (!nearLoopEnd) v.vibrateAlarm(pulse)
                    }
                }
                delay(10)
            }
        }
    }

    private fun systemVibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }

    private fun Vibrator.vibrateAlarm(effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION") vibrate(effect, alarmAudioAttributes)
        }
    }

    private fun stopNoise() {
        volumeJob?.cancel()
        vibrationJob?.cancel()
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        tone?.release()
        tone = null
        vibrator?.cancel()
        vibrator = null
        savedVolume?.let { volume ->
            runCatching { getSystemService(AudioManager::class.java).setStreamVolume(AudioManager.STREAM_ALARM, volume, 0) }
        }
        savedVolume = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        noiseStarted = false
    }

    companion object {
        private const val TAG = "AlarmService"
        private const val BEAT_PULSE_MS = 140L
        private const val ACTION_RING = "com.evilalarm.app.action.RING"
        private const val ACTION_SNOOZE = "com.evilalarm.app.action.SNOOZE"
        private const val ACTION_DISMISS = "com.evilalarm.app.action.DISMISS"

        private val alarmAudioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        fun ring(context: Context, alarmId: Int) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmService::class.java)
                    .setAction(ACTION_RING)
                    .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId),
            )
        }

        fun snooze(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_SNOOZE))
        }

        fun dismiss(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_DISMISS))
        }
    }
}
