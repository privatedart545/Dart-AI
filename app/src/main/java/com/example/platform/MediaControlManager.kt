package com.example.platform

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent

data class MediaTrackInfo(
    val appName: String,
    val packageName: String,
    val title: String,
    val artist: String,
    val album: String,
    val isPlaying: Boolean,
    val durationMs: Long,
    val positionMs: Long
)

class MediaControlManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun getActiveControllers(): List<MediaController> {
        return DevAiNotificationListenerService.getActiveMediaControllers(context)
    }

    private fun getPrimaryController(): MediaController? {
        val controllers = getActiveControllers()
        if (controllers.isEmpty()) return null

        // Prefer controller that is currently playing
        val playingController = controllers.firstOrNull { controller ->
            val state = controller.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_FAST_FORWARDING || state == PlaybackState.STATE_REWINDING
        }
        return playingController ?: controllers.firstOrNull()
    }

    fun getCurrentMediaTrack(): MediaTrackInfo? {
        val controller = getPrimaryController() ?: return null
        val metadata = controller.metadata
        val state = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown Track"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: "Unknown Artist"
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = state?.position ?: 0L
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING

        val packageName = controller.packageName ?: ""
        val appName = try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            when {
                packageName.contains("spotify") -> "Spotify"
                packageName.contains("youtube.music") -> "YouTube Music"
                packageName.contains("youtube") -> "YouTube"
                packageName.contains("pocketcasts") -> "Pocket Casts"
                packageName.contains("audible") -> "Audible"
                packageName.contains("chrome") -> "Google Chrome"
                else -> packageName.substringAfterLast('.')
            }
        }

        return MediaTrackInfo(
            appName = appName,
            packageName = packageName,
            title = title,
            artist = artist,
            album = album,
            isPlaying = isPlaying,
            durationMs = duration,
            positionMs = position
        )
    }

    fun getMediaStatusSummary(): String {
        val track = getCurrentMediaTrack()
        if (track != null) {
            val playStateIcon = if (track.isPlaying) "▶️ Playing" else "⏸️ Paused"
            val posSec = track.positionMs / 1000
            val durSec = track.durationMs / 1000
            val timeStr = if (durSec > 0) " (${posSec / 60}:${String.format("%02d", posSec % 60)} / ${durSec / 60}:${String.format("%02d", durSec % 60)})" else ""

            return """### 🎵 Dart AI System Media Controller
- **Player App**: **${track.appName}** (`${track.packageName}`)
- **Title**: **${track.title}**
- **Artist**: **${track.artist}**
- **Album**: ${track.album.ifEmpty { "Single / Live Stream" }}
- **Status**: **$playStateIcon**$timeStr
- **Volume Level**: **${getMediaVolumePercent()}%**"""
        }

        val isMusicActive = audioManager.isMusicActive
        return if (isMusicActive) {
            "Audio is currently active on the device in background (Media volume: ${getMediaVolumePercent()}%). Dart AI can play, pause, skip, or adjust volume."
        } else {
            "No active media playback session detected on the device. (Media volume: ${getMediaVolumePercent()}%). You can ask Dart AI to play music or launch Spotify/YouTube."
        }
    }

    fun mediaPlay(): String {
        val controller = getPrimaryController()
        return if (controller != null) {
            controller.transportControls.play()
            val track = getCurrentMediaTrack()
            "Resumed playback of \"${track?.title ?: "track"}\" on ${track?.appName ?: "active player"}."
        } else {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
            "Sent system Media Play signal."
        }
    }

    fun mediaPause(): String {
        val controller = getPrimaryController()
        return if (controller != null) {
            controller.transportControls.pause()
            val track = getCurrentMediaTrack()
            "Paused playback of \"${track?.title ?: "track"}\" on ${track?.appName ?: "active player"}."
        } else {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
            "Sent system Media Pause signal."
        }
    }

    fun mediaPlayPause(): String {
        val controller = getPrimaryController()
        return if (controller != null) {
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                controller.transportControls.pause()
                "Paused playback on ${controller.packageName}."
            } else {
                controller.transportControls.play()
                "Resumed playback on ${controller.packageName}."
            }
        } else {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            "Toggled system-wide Media Play/Pause."
        }
    }

    fun mediaNext(): String {
        val controller = getPrimaryController()
        return if (controller != null) {
            controller.transportControls.skipToNext()
            "Skipped to next track on ${controller.packageName}."
        } else {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            "Sent system Skip Next Track command."
        }
    }

    fun mediaPrevious(): String {
        val controller = getPrimaryController()
        return if (controller != null) {
            controller.transportControls.skipToPrevious()
            "Skipped to previous track on ${controller.packageName}."
        } else {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            "Sent system Previous Track command."
        }
    }

    fun setMediaVolume(percent: Int): String {
        val clamped = percent.coerceIn(0, 100)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetIndex = ((clamped / 100.0) * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, AudioManager.FLAG_SHOW_UI)
        return "Set media music volume to $clamped% (Level $targetIndex of $maxVolume)."
    }

    fun getMediaVolumePercent(): Int {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) ((current.toFloat() / maxVolume.toFloat()) * 100).toInt() else 0
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val eventTime = SystemClock.uptimeMillis()
        val down = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val up = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)
        audioManager.dispatchMediaKeyEvent(down)
        audioManager.dispatchMediaKeyEvent(up)
    }

    fun openNotificationListenerSettings(): Result<String> {
        return try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success("Opened Notification & Media Session Access Settings.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
