package com.evilalarm.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.evilalarm.app.alarm.SoundCatalog

/** Plays a sound once at media volume, so previewing in Settings doesn't blast at alarm volume. */
class SoundPreview(private val context: Context) {
    var playingId by mutableStateOf<String?>(null)
        private set
    private var player: MediaPlayer? = null

    fun toggle(id: String) {
        val wasPlaying = playingId == id
        stop()
        if (!wasPlaying) play(id)
    }

    fun stop() {
        player?.release()
        player = null
        playingId = null
    }

    private fun play(id: String) {
        val mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            SoundCatalog.setDataSource(context, mediaPlayer, id)
            mediaPlayer.setOnCompletionListener { stop() }
            mediaPlayer.prepare()
            mediaPlayer.start()
            player = mediaPlayer
            playingId = id
        } catch (e: Exception) {
            mediaPlayer.release()
        }
    }
}

@Composable
fun rememberSoundPreview(): SoundPreview {
    val context = LocalContext.current
    val preview = remember { SoundPreview(context) }
    LifecycleResumeEffect(preview) {
        onPauseOrDispose { preview.stop() }
    }
    return preview
}
