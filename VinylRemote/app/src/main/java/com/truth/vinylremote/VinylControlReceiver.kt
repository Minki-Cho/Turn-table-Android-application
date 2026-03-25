package com.truth.vinylremote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.session.PlaybackState
import android.os.SystemClock

class VinylControlReceiver : BroadcastReceiver() {
    companion object {
        private const val SEEK_DELTA_MS = 10_000L
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appContext = context.applicationContext
        val bridge = ExternalMediaSessionController(appContext)
        val controller = bridge.bestController()
        val controls = controller?.transportControls
        val playbackStateObj = controller?.playbackState
        val playbackState = playbackStateObj?.state
        val playbackActions = playbackStateObj?.actions ?: 0L
        val durationMs = controller?.metadata
            ?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION)
            ?.takeIf { it > 0L } ?: 0L
        val canSeek = playbackActions and PlaybackState.ACTION_SEEK_TO != 0L

        when (action) {
            VinylControlActions.ACTION_PREV -> controls?.skipToPrevious()
            VinylControlActions.ACTION_NEXT -> controls?.skipToNext()
            VinylControlActions.ACTION_SEEK_BACK -> {
                if (canSeek) {
                    val current = estimatedPositionMs(playbackStateObj, durationMs)
                    val target = (current - SEEK_DELTA_MS).coerceAtLeast(0L)
                    controls?.seekTo(target)
                }
            }
            VinylControlActions.ACTION_SEEK_FORWARD -> {
                if (canSeek) {
                    val current = estimatedPositionMs(playbackStateObj, durationMs)
                    val target = if (durationMs > 0L) {
                        (current + SEEK_DELTA_MS).coerceAtMost(durationMs)
                    } else {
                        current + SEEK_DELTA_MS
                    }
                    controls?.seekTo(target)
                }
            }
            VinylControlActions.ACTION_TOGGLE_PLAY_PAUSE -> {
                if (playbackState == PlaybackState.STATE_PLAYING ||
                    playbackState == PlaybackState.STATE_BUFFERING
                ) {
                    controls?.pause()
                } else {
                    controls?.play()
                }
            }
            VinylControlActions.ACTION_NEEDLE_IN -> {
                controls?.play()
                VinylControlActions.writeNeedleCommand(appContext, VinylControlActions.NEEDLE_IN)
            }
            VinylControlActions.ACTION_NEEDLE_OUT -> {
                controls?.pause()
                VinylControlActions.writeNeedleCommand(appContext, VinylControlActions.NEEDLE_OUT)
            }
            else -> return
        }

        val current = VinylExternalControls.loadSnapshot(appContext)
        val updated = when (action) {
            VinylControlActions.ACTION_NEEDLE_IN -> current.copy(
                isPlaying = true,
                needleProgress = 0.30f
            )
            VinylControlActions.ACTION_NEEDLE_OUT -> current.copy(
                isPlaying = false,
                needleProgress = 0.05f
            )
            VinylControlActions.ACTION_TOGGLE_PLAY_PAUSE -> current.copy(isPlaying = !current.isPlaying)
            VinylControlActions.ACTION_SEEK_BACK -> {
                val nextPos = (current.positionMs - SEEK_DELTA_MS).coerceAtLeast(0L)
                current.copy(
                    positionMs = nextPos,
                    needleProgress = resolveNeedleFromPosition(nextPos, current.durationMs, current.needleProgress)
                )
            }
            VinylControlActions.ACTION_SEEK_FORWARD -> {
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
        VinylExternalControls.publish(appContext, updated)
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
        return 0.30f + (0.98f - 0.30f) * fraction
    }
}
