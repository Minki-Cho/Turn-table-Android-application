package com.truth.picklydeck

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.session.PlaybackState
import android.os.SystemClock

class PicklyDeckControlReceiver : BroadcastReceiver() {
    companion object {
        private const val SEEK_DELTA_MS = 10_000L
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appContext = context.applicationContext
        val bridge = ExternalMediaSessionController(appContext)
        val controller = bridge.bestController()
        val current = PicklyDeckExternalControls.loadSnapshot(appContext)
        val controls = controller?.transportControls
        val playbackStateObj = controller?.playbackState
        val playbackState = playbackStateObj?.state
        val playbackActions = playbackStateObj?.actions ?: 0L
        val durationMs = controller?.metadata
            ?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION)
            ?.takeIf { it > 0L } ?: 0L
        val canSeek = playbackActions and PlaybackState.ACTION_SEEK_TO != 0L

        when (action) {
            PicklyDeckControlActions.ACTION_PREV -> controls?.skipToPrevious()
            PicklyDeckControlActions.ACTION_NEXT -> controls?.skipToNext()
            PicklyDeckControlActions.ACTION_SEEK_BACK -> {
                if (canSeek) {
                    val currentPosition = estimatedPositionMs(playbackStateObj, durationMs)
                    val target = (currentPosition - SEEK_DELTA_MS).coerceAtLeast(0L)
                    controls?.seekTo(target)
                }
            }
            PicklyDeckControlActions.ACTION_SEEK_FORWARD -> {
                if (canSeek) {
                    val currentPosition = estimatedPositionMs(playbackStateObj, durationMs)
                    val target = if (durationMs > 0L) {
                        (currentPosition + SEEK_DELTA_MS).coerceAtMost(durationMs)
                    } else {
                        currentPosition + SEEK_DELTA_MS
                    }
                    controls?.seekTo(target)
                }
            }
            PicklyDeckControlActions.ACTION_TOGGLE_PLAY_PAUSE -> {
                if (playbackState == PlaybackState.STATE_PLAYING ||
                    playbackState == PlaybackState.STATE_BUFFERING
                ) {
                    controls?.pause()
                } else {
                    controls?.play()
                }
            }
            PicklyDeckControlActions.ACTION_NEEDLE_IN -> {
                controls?.play()
                PicklyDeckControlActions.writeNeedleCommand(appContext, PicklyDeckControlActions.NEEDLE_IN)
            }
            PicklyDeckControlActions.ACTION_NEEDLE_OUT -> {
                controls?.pause()
                PicklyDeckControlActions.writeNeedleCommand(appContext, PicklyDeckControlActions.NEEDLE_OUT)
            }
            else -> return
        }

        val fallback = when (action) {
            PicklyDeckControlActions.ACTION_TOGGLE_PLAY_PAUSE -> current.copy(isPlaying = !current.isPlaying)
            PicklyDeckControlActions.ACTION_SEEK_BACK -> {
                val nextPos = (current.positionMs - SEEK_DELTA_MS).coerceAtLeast(0L)
                current.copy(
                    positionMs = nextPos,
                    needleProgress = resolveNeedleFromPosition(nextPos, current.durationMs, current.needleProgress)
                )
            }
            PicklyDeckControlActions.ACTION_SEEK_FORWARD -> {
                val nextPos = if (current.durationMs > 0L) {
                    (current.positionMs + SEEK_DELTA_MS).coerceAtMost(current.durationMs)
                } else {
                    current.positionMs + SEEK_DELTA_MS
                }
                current.copy(
                    positionMs = nextPos,
                    needleProgress = resolveNeedleFromPosition(nextPos, current.durationMs, current.needleProgress)
                )
            }
            else -> current
        }
        val refreshed = snapshotFromController(bridge.bestController(), current)
        val published = when (action) {
            PicklyDeckControlActions.ACTION_SEEK_BACK,
            PicklyDeckControlActions.ACTION_SEEK_FORWARD -> {
                if (refreshed.positionMs == current.positionMs &&
                    refreshed.durationMs == current.durationMs
                ) {
                    fallback
                } else {
                    refreshed
                }
            }
            else -> refreshed
        }
        PicklyDeckExternalControls.publish(appContext, published)
    }

    private fun estimatedPositionMs(state: PlaybackState?, durationMs: Long): Long {
        if (state == null) return 0L
        val base = state.position.coerceAtLeast(0L)
        val playing = state.state == PlaybackState.STATE_PLAYING ||
            state.state == PlaybackState.STATE_BUFFERING
        val estimated = if (playing && state.lastPositionUpdateTime > 0L) {
            val elapsed = (SystemClock.elapsedRealtime() - state.lastPositionUpdateTime).coerceAtLeast(0L)
            val speed = if (state.playbackSpeed == 0f) 1f else state.playbackSpeed
            base + (elapsed * speed).toLong()
        } else {
            base
        }
        return if (durationMs > 0L) estimated.coerceIn(0L, durationMs) else estimated.coerceAtLeast(0L)
    }

    private fun resolveNeedleFromPosition(positionMs: Long, durationMs: Long, fallback: Float): Float {
        if (durationMs <= 0L) return fallback
        val fraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        return PlaybackMath.trackFractionToNeedle(fraction)
    }

    private fun snapshotFromController(
        controller: android.media.session.MediaController?,
        fallback: PicklyDeckUiState
    ): PicklyDeckUiState {
        if (controller == null) return fallback

        val state = controller.playbackState
        val metadata = controller.metadata
        val duration = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION)
            ?.takeIf { it > 0L } ?: 0L
        val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
            ?: fallback.title
        val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM)
            ?: fallback.artist
        val resolvedPosition = estimatedPositionMs(state, duration)
        val resolvedNeedle = if (duration > 0L) {
            PlaybackMath.trackFractionToNeedle(
                (resolvedPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            )
        } else {
            fallback.needleProgress
        }
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING ||
            state?.state == PlaybackState.STATE_BUFFERING

        return fallback.copy(
            connectedPackage = controller.packageName,
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            positionMs = resolvedPosition,
            durationMs = duration,
            playbackSpeed = state?.playbackSpeed ?: fallback.playbackSpeed,
            needleProgress = resolvedNeedle
        )
    }
}
