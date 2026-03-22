package com.truth.vinylremote

import android.app.Application
import android.content.Context
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.security.MessageDigest

data class VinylUiState(
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
    val albumArt: Bitmap? = null,
    val lyrics: String = ""
)

class VinylViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        // 0f~1f: tonearm progress from rest(white area) to inner groove.
        private const val NEEDLE_PAUSE_THRESHOLD = 0.20f
        private const val NEEDLE_PLAY_START = 0.30f
        private const val NEEDLE_PLAY_END = 0.98f
        private const val NEEDLE_SNAP_PAUSE = 0.05f
        private const val LYRICS_RETRY_INTERVAL_MS = 20_000L
        private const val LYRICS_FORCE_RETRY_INTERVAL_MS = 12_000L
        private const val LYRICS_FORCE_RETRY_AFTER_POSITION_MS = 12_000L
        private const val LYRICS_AUTO_RETRY_COUNT = 2
        private const val LYRICS_AUTO_RETRY_DELAY_MS = 650L
    }

    private val mediaController = ExternalMediaSessionController(application)
    private val lyricsProvider = LyricsProvider()
    private val onlineLyricsEnabled = BuildConfig.LYRICS_ONLINE_ENABLED
    private val lyricsPrefs =
        application.getSharedPreferences("vinyl_remote_lyrics", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        VinylUiState(hasNotificationAccess = mediaController.hasNotificationAccess())
    )
    val uiState: StateFlow<VinylUiState> = _uiState.asStateFlow()

    private var activeController: MediaController? = null
    private var sessionCallback: MediaController.Callback? = null
    private var needleIntentPlaying: Boolean? = null
    private var suppressAutoNeedleUntilMs: Long = 0L
    private var lyricsFetchJob: Job? = null
    private var lyricsFetchInFlightKey: String? = null
    private var lastPrefetchKey: String? = null
    private val externalLyricsCache = mutableMapOf<String, String>()
    private val manualLyricsCache = mutableMapOf<String, String>()
    private val lyricsFailedAtMs = mutableMapOf<String, Long>()
    private val lyricsForceRetryAtMs = mutableMapOf<String, Long>()
    private var lastExternalControlSignature = ""
    private var lastHandledNeedleCommandSeq = VinylControlActions.readNeedleCommand(application).seq

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

    fun retryLyricsLookup() {
        if (!onlineLyricsEnabled) return
        val current = _uiState.value
        val key = lyricsLookupKey(current.title, current.artist)
        externalLyricsCache.remove(key)
        lyricsFailedAtMs.remove(key)
        if (lyricsFetchInFlightKey == key) {
            lyricsFetchInFlightKey = null
            lyricsFetchJob?.cancel()
        }
        maybeFetchExternalLyrics(
            key = key,
            title = current.title,
            artist = current.artist,
            durationMs = current.durationMs
        )
    }

    fun saveManualLyrics(lyrics: String) {
        val current = _uiState.value
        val key = lyricsLookupKey(current.title, current.artist)
        val normalized = lyrics
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()
        if (normalized.isBlank()) {
            clearManualLyrics()
            return
        }
        manualLyricsCache[key] = normalized
        lyricsPrefs.edit().putString(manualLyricsStorageKey(key), normalized).apply()
        _uiState.update { it.copy(lyrics = normalized) }
    }

    fun clearManualLyrics() {
        val current = _uiState.value
        val key = lyricsLookupKey(current.title, current.artist)
        manualLyricsCache.remove(key)
        lyricsPrefs.edit().remove(manualLyricsStorageKey(key)).apply()
        _uiState.update { it.copy(lyrics = "") }
        if (onlineLyricsEnabled) {
            maybeFetchExternalLyrics(
                key = key,
                title = current.title,
                artist = current.artist,
                durationMs = current.durationMs
            )
        }
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
                    albumArt = null,
                    lyrics = ""
                )
            }
            lastExternalControlSignature = ""
            VinylExternalControls.cancel(getApplication())
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
                    artist = "Play music in Spotify or YouTube Music.",
                    isPlaying = false,
                    canSeek = false,
                    positionMs = 0L,
                    durationMs = 0L,
                    playbackSpeed = 1f,
                    albumArt = null,
                    lyrics = ""
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
        val displayDescription = metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)
        val lyricText = metadata?.getText("android.media.metadata.LYRIC")?.toString()
        val metadataLyrics = when {
            !lyricText.isNullOrBlank() -> lyricText
            !displayDescription.isNullOrBlank() -> displayDescription
            else -> ""
        }
        val resolvedTitle = displayTitle
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: "Unknown title"
        val resolvedArtist = displaySubtitle
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: ""
        val lyricsKey = lyricsLookupKey(resolvedTitle, resolvedArtist)
        val manualLyrics = resolveManualLyrics(lyricsKey)
        val fetchedLyrics = externalLyricsCache[lyricsKey].orEmpty()
        val lyrics = when {
            manualLyrics.isNotBlank() -> manualLyrics
            metadataLyrics.isNotBlank() -> metadataLyrics
            fetchedLyrics.isNotBlank() -> fetchedLyrics
            else -> ""
        }
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
        val externalNeedleCommand = VinylControlActions.readNeedleCommand(getApplication())
        var commandNeedleProgress: Float? = null
        var commandPlaying: Boolean? = null
        if (externalNeedleCommand.seq > lastHandledNeedleCommandSeq) {
            lastHandledNeedleCommandSeq = externalNeedleCommand.seq
            when (externalNeedleCommand.command) {
                VinylControlActions.NEEDLE_IN -> {
                    commandNeedleProgress = maxOf(autoNeedleProgress, NEEDLE_PLAY_START)
                    commandPlaying = true
                }
                VinylControlActions.NEEDLE_OUT -> {
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
                albumArt = albumArt,
                lyrics = lyrics
            )
        }
        publishExternalControls(_uiState.value)

        if (onlineLyricsEnabled && manualLyrics.isBlank() && metadataLyrics.isBlank()) {
            maybePrefetchLyrics(
                key = lyricsKey,
                title = resolvedTitle,
                artist = resolvedArtist,
                durationMs = duration
            )
            maybeFetchExternalLyrics(
                key = lyricsKey,
                title = resolvedTitle,
                artist = resolvedArtist,
                durationMs = duration
            )
            if (
                lyrics.isBlank() &&
                resolvedPosition >= LYRICS_FORCE_RETRY_AFTER_POSITION_MS
            ) {
                maybeFetchExternalLyrics(
                    key = lyricsKey,
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    durationMs = duration,
                    force = true
                )
            }
        }
    }

    private fun publishExternalControls(state: VinylUiState) {
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
        }
        if (signature == lastExternalControlSignature) return
        lastExternalControlSignature = signature
        VinylExternalControls.publish(getApplication(), state)
    }

    private fun resolvePositionMs(state: PlaybackState?, durationMs: Long): Long {
        if (state == null) return 0L
        val base = state.position.coerceAtLeast(0L)
        val now = SystemClock.elapsedRealtime()
        val isPlaying =
            state.state == PlaybackState.STATE_PLAYING || state.state == PlaybackState.STATE_BUFFERING
        if (!isPlaying) return clampPosition(base, durationMs)

        val lastUpdate = state.lastPositionUpdateTime
        if (lastUpdate <= 0L) return clampPosition(base, durationMs)
        val elapsed = (now - lastUpdate).coerceAtLeast(0L)
        val speed = if (state.playbackSpeed == 0f) 1f else state.playbackSpeed
        val estimated = base + (elapsed * speed).toLong()
        return clampPosition(estimated, durationMs)
    }

    private fun clampPosition(positionMs: Long, durationMs: Long): Long {
        if (durationMs <= 0L) return positionMs.coerceAtLeast(0L)
        return positionMs.coerceIn(0L, durationMs)
    }

    private fun needleToTrackFraction(needleProgress: Float): Float {
        val p = needleProgress.coerceIn(NEEDLE_PLAY_START, NEEDLE_PLAY_END)
        return (p - NEEDLE_PLAY_START) / (NEEDLE_PLAY_END - NEEDLE_PLAY_START)
    }

    private fun trackFractionToNeedle(trackFraction: Float): Float {
        val f = trackFraction.coerceIn(0f, 1f)
        return NEEDLE_PLAY_START + (NEEDLE_PLAY_END - NEEDLE_PLAY_START) * f
    }

    override fun onCleared() {
        lyricsFetchJob?.cancel()
        detachController()
        super.onCleared()
    }

    private fun maybeFetchExternalLyrics(
        key: String,
        title: String,
        artist: String,
        durationMs: Long,
        force: Boolean = false
    ) {
        if (title.isBlank()) return
        if (externalLyricsCache.containsKey(key)) return
        if (lyricsFetchInFlightKey == key) return
        val now = SystemClock.elapsedRealtime()
        if (!force) {
            val lastFailedAt = lyricsFailedAtMs[key] ?: 0L
            if (now - lastFailedAt < LYRICS_RETRY_INTERVAL_MS) return
        } else {
            val lastForceAt = lyricsForceRetryAtMs[key] ?: 0L
            if (now - lastForceAt < LYRICS_FORCE_RETRY_INTERVAL_MS) return
            lyricsForceRetryAtMs[key] = now
        }

        lyricsFetchInFlightKey = key
        lyricsFetchJob?.cancel()
        lyricsFetchJob = viewModelScope.launch {
            var fetched = ""
            for (attempt in 0..LYRICS_AUTO_RETRY_COUNT) {
                fetched = lyricsProvider.fetchLyrics(
                    title = title,
                    artist = artist,
                    durationMs = durationMs
                ).orEmpty()
                if (fetched.isNotBlank()) break
                if (attempt < LYRICS_AUTO_RETRY_COUNT) {
                    delay(LYRICS_AUTO_RETRY_DELAY_MS)
                }
            }

            if (fetched.isNotBlank()) {
                externalLyricsCache[key] = fetched
                lyricsFailedAtMs.remove(key)
                lyricsForceRetryAtMs.remove(key)
                val current = _uiState.value
                if (lyricsLookupKey(current.title, current.artist) == key && current.lyrics.isBlank()) {
                    _uiState.update { it.copy(lyrics = fetched) }
                }
            } else {
                lyricsFailedAtMs[key] = SystemClock.elapsedRealtime()
            }
            if (lyricsFetchInFlightKey == key) {
                lyricsFetchInFlightKey = null
            }
        }
    }

    private fun maybePrefetchLyrics(
        key: String,
        title: String,
        artist: String,
        durationMs: Long
    ) {
        if (lastPrefetchKey == key) return
        lastPrefetchKey = key
        maybeFetchExternalLyrics(
            key = key,
            title = title,
            artist = artist,
            durationMs = durationMs,
            force = true
        )
    }

    private fun lyricsLookupKey(title: String, artist: String): String {
        return "${title.trim().lowercase()}|${artist.trim().lowercase()}"
    }

    private fun resolveManualLyrics(key: String): String {
        val cached = manualLyricsCache[key]
        if (cached != null) return cached
        val fromPrefs = lyricsPrefs
            .getString(manualLyricsStorageKey(key), "")
            .orEmpty()
            .trim()
        if (fromPrefs.isNotBlank()) {
            manualLyricsCache[key] = fromPrefs
        }
        return fromPrefs
    }

    private fun manualLyricsStorageKey(lookupKey: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(lookupKey.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "m_$hash"
    }
}
