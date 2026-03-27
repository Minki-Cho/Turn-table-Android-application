package com.truth.picklydeck

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.provider.Settings
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PicklyDeckUiState(
    val hasNotificationAccess: Boolean = false,
    val connectedPackage: String? = null,
    val title: String = "Looking for active player",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val canSeek: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val needleProgress: Float = 0f,
    val albumArt: Bitmap? = null
)

class PicklyDeckViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        // 0f~1f: tonearm progress from rest(white area) to inner groove.
        private const val NEEDLE_PAUSE_THRESHOLD = 0.20f
        private const val NEEDLE_PLAY_START = 0.30f
        private const val NEEDLE_PLAY_END = 0.98f
        private const val NEEDLE_SNAP_PAUSE = 0.05f
    }

    private val mediaController = ExternalMediaSessionController(application)

    private val _uiState = MutableStateFlow(
        PicklyDeckUiState(hasNotificationAccess = mediaController.hasNotificationAccess())
    )
    val uiState: StateFlow<PicklyDeckUiState> = _uiState.asStateFlow()

    private var activeController: MediaController? = null
    private var sessionCallback: MediaController.Callback? = null
    private var needleIntentPlaying: Boolean? = null
    private var suppressAutoNeedleUntilMs: Long = 0L
    private var lastExternalControlSignature = ""
    private var lastHandledNeedleCommandSeq = PicklyDeckControlActions.readNeedleCommand(application).seq

    init {
        viewModelScope.launch {
            while (isActive) {
                refreshState()
                delay(220L)
            }
        }
    }

    fun openNotificationAccessSettings() {
        val app = getApplication<Application>()
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(intent)
    }

    fun setNeedleProgress(progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        _uiState.update { it.copy(needleProgress = p) }
        suppressAutoNeedleUntilMs = SystemClock.elapsedRealtime() + 1200L

        // During drag, apply light hysteresis so play/pause doesn't flicker.
        when {
            p >= NEEDLE_PLAY_START -> {
                if (needleIntentPlaying != true) {
                    play()
                    needleIntentPlaying = true
                }
            }
            p <= NEEDLE_PAUSE_THRESHOLD -> {
                if (needleIntentPlaying != false) {
                    pause()
                    needleIntentPlaying = false
                }
            }
        }
    }

    fun snapNeedleAndControlPlayback() {
        val current = _uiState.value.needleProgress
        val shouldPlay = current >= NEEDLE_PLAY_START
        val snapped = if (shouldPlay) {
            current.coerceIn(NEEDLE_PLAY_START, NEEDLE_PLAY_END)
        } else {
            NEEDLE_SNAP_PAUSE
        }
        _uiState.update {
            it.copy(needleProgress = snapped)
        }
        if (shouldPlay) {
            seekToFraction(needleToTrackFraction(snapped))
            play()
            needleIntentPlaying = true
        } else {
            pause()
            needleIntentPlaying = false
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) pause() else play()
    }

    fun play() {
        activeController?.transportControls?.play()
        val current = _uiState.value
        val duration = current.durationMs
        val positionFraction = if (duration > 0L) {
            (current.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val targetNeedle = maxOf(NEEDLE_PLAY_START, trackFractionToNeedle(positionFraction))
        _uiState.update {
            it.copy(
                isPlaying = true,
                needleProgress = targetNeedle
            )
        }
        needleIntentPlaying = true
    }

    fun pause() {
        activeController?.transportControls?.pause()
        _uiState.update { it.copy(isPlaying = false) }
        needleIntentPlaying = false
    }

    fun skipNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun previewSeekFraction(fraction: Float) {
        val duration = _uiState.value.durationMs
        if (duration <= 0L) return
        val f = fraction.coerceIn(0f, 1f)
        val target = (duration * f).toLong()
        suppressAutoNeedleUntilMs = SystemClock.elapsedRealtime() + 1500L
        _uiState.update {
            it.copy(
                positionMs = target,
                needleProgress = trackFractionToNeedle(f)
            )
        }
    }

    fun seekToFraction(fraction: Float) {
        val duration = _uiState.value.durationMs
        if (duration <= 0L) return
        val f = fraction.coerceIn(0f, 1f)
        previewSeekFraction(f)
        val target = (duration * f).toLong()
        activeController?.transportControls?.seekTo(target)
    }

    private fun refreshState() {
        val hasAccess = mediaController.hasNotificationAccess()
        if (!hasAccess) {
            detachController()
            _uiState.update {
                it.copy(
                    hasNotificationAccess = false,
                    connectedPackage = null,
                    title = "Notification access required",
                    artist = "Enable this app in notification access settings.",
                    isPlaying = false,
                    canSeek = false,
                    positionMs = 0L,
                    durationMs = 0L,
                    playbackSpeed = 1f,
                    albumArt = null
                )
            }
            lastExternalControlSignature = ""
            PicklyDeckExternalControls.cancel(getApplication())
            return
        }

        val newController = mediaController.bestController()
        if (newController?.sessionToken != activeController?.sessionToken) {
            attachController(newController)
        }
        publishPlayback(newController)
    }

    private fun attachController(controller: MediaController?) {
        detachController()
        activeController = controller
        if (controller == null) return

        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                publishPlayback(activeController)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                publishPlayback(activeController)
            }
        }
        sessionCallback = callback
        controller.registerCallback(callback)
    }

    private fun detachController() {
        val callback = sessionCallback
        if (activeController != null && callback != null) {
            activeController?.unregisterCallback(callback)
        }
        sessionCallback = null
        activeController = null
    }

    private fun publishPlayback(controller: MediaController?) {
        if (controller == null) {
            _uiState.update {
                it.copy(
                    hasNotificationAccess = true,
                    connectedPackage = null,
                    title = "No active playback",
                    artist = "Play music in your media app (Spotify, YouTube Music, Apple Music, etc.).",
                    isPlaying = false,
                    canSeek = false,
                    positionMs = 0L,
                    durationMs = 0L,
                    playbackSpeed = 1f,
                    albumArt = null
                )
            }
            publishExternalControls(_uiState.value)
            return
        }

        val state = controller.playbackState
        val metadata = controller.metadata
        val actions = state?.actions ?: 0L
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
            ?.takeIf { it > 0L } ?: 0L
        val displayTitle = metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val displaySubtitle = metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        val resolvedTitle = displayTitle
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: "Unknown title"
        val resolvedArtist = displaySubtitle
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: ""
        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val resolvedPosition = resolvePositionMs(state, duration)
        val shouldAutoNeedle =
            duration > 0L && SystemClock.elapsedRealtime() >= suppressAutoNeedleUntilMs
        val autoNeedleProgress = if (shouldAutoNeedle) {
            trackFractionToNeedle(resolvedPosition.toFloat() / duration.toFloat())
        } else {
            _uiState.value.needleProgress
        }
        val externalNeedleCommand = PicklyDeckControlActions.readNeedleCommand(getApplication())
        var commandNeedleProgress: Float? = null
        var commandPlaying: Boolean? = null
        if (externalNeedleCommand.seq > lastHandledNeedleCommandSeq) {
            lastHandledNeedleCommandSeq = externalNeedleCommand.seq
            when (externalNeedleCommand.command) {
                PicklyDeckControlActions.NEEDLE_IN -> {
                    commandNeedleProgress = maxOf(autoNeedleProgress, NEEDLE_PLAY_START)
                    commandPlaying = true
                }
                PicklyDeckControlActions.NEEDLE_OUT -> {
                    commandNeedleProgress = NEEDLE_SNAP_PAUSE
                    commandPlaying = false
                }
            }
            suppressAutoNeedleUntilMs = SystemClock.elapsedRealtime() + 900L
        }
        val isPlayingState = state?.state == PlaybackState.STATE_PLAYING ||
            state?.state == PlaybackState.STATE_BUFFERING

        _uiState.update {
            it.copy(
                hasNotificationAccess = true,
                connectedPackage = controller.packageName,
                title = resolvedTitle,
                artist = resolvedArtist,
                isPlaying = commandPlaying ?: isPlayingState,
                canSeek = actions and PlaybackState.ACTION_SEEK_TO != 0L && duration > 0L,
                positionMs = resolvedPosition,
                durationMs = duration,
                playbackSpeed = state?.playbackSpeed ?: 1f,
                needleProgress = commandNeedleProgress ?: autoNeedleProgress,
                albumArt = albumArt
            )
        }
        publishExternalControls(_uiState.value)
    }

    private fun publishExternalControls(state: PicklyDeckUiState) {
        val signature = buildString {
            append(state.connectedPackage.orEmpty())
            append('|')
            append(state.title)
            append('|')
            append(state.artist)
            append('|')
            append(state.isPlaying)
            append('|')
            append(state.needleProgress >= NEEDLE_PLAY_START)
            append('|')
            append(state.positionMs / 1000L)
            append('|')
            append(state.durationMs / 1000L)
        }
        if (signature == lastExternalControlSignature) return
        lastExternalControlSignature = signature
        PicklyDeckExternalControls.publish(getApplication(), state)
    }

    private fun resolvePositionMs(state: PlaybackState?, durationMs: Long): Long {
        if (state == null) return 0L
        val now = SystemClock.elapsedRealtime()
        val isPlaying =
            state.state == PlaybackState.STATE_PLAYING || state.state == PlaybackState.STATE_BUFFERING
        return PlaybackMath.estimatePositionMs(
            basePositionMs = state.position,
            lastUpdateElapsedRealtimeMs = state.lastPositionUpdateTime,
            nowElapsedRealtimeMs = now,
            playbackSpeed = state.playbackSpeed,
            isPlaying = isPlaying,
            durationMs = durationMs
        )
    }

    private fun clampPosition(positionMs: Long, durationMs: Long): Long {
        return PlaybackMath.clampPosition(positionMs, durationMs)
    }

    private fun needleToTrackFraction(needleProgress: Float): Float {
        return PlaybackMath.needleToTrackFraction(needleProgress)
    }

    private fun trackFractionToNeedle(trackFraction: Float): Float {
        return PlaybackMath.trackFractionToNeedle(trackFraction)
    }

    override fun onCleared() {
        detachController()
        super.onCleared()
    }
}
