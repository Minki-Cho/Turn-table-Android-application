package com.truth.vinylremote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.session.PlaybackState

class VinylControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appContext = context.applicationContext
        val bridge = ExternalMediaSessionController(appContext)
        val controller = bridge.bestController()
        val controls = controller?.transportControls
        val playbackState = controller?.playbackState?.state

        when (action) {
            VinylControlActions.ACTION_PREV -> controls?.skipToPrevious()
            VinylControlActions.ACTION_NEXT -> controls?.skipToNext()
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
            else -> current
        }
        VinylExternalControls.publish(appContext, updated)
    }
}
