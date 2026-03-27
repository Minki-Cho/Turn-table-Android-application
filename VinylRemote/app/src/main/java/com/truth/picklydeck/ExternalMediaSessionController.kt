package com.truth.picklydeck

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.core.app.NotificationManagerCompat

class ExternalMediaSessionController(private val context: Context) {
    private val mediaSessionManager =
        context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent =
        ComponentName(context, PicklyDeckNotificationListenerService::class.java)

    private val preferredPackages = listOf(
        "com.spotify.music",
        "com.google.android.apps.youtube.music"
    )

    fun hasNotificationAccess(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    fun bestController(): MediaController? {
        if (!hasNotificationAccess()) return null
        val sessions = try {
            mediaSessionManager.getActiveSessions(listenerComponent)
        } catch (_: SecurityException) {
            emptyList()
        }
        if (sessions.isEmpty()) return null

        return sessions.sortedWith(
            compareBy<MediaController> { !it.isLikelyPlaying() }
                .thenBy { preferredPackages.indexOrLarge(it.packageName) }
        ).firstOrNull()
    }

    private fun MediaController.isLikelyPlaying(): Boolean {
        val state = playbackState?.state
        return state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
    }

    private fun List<String>.indexOrLarge(value: String): Int {
        val index = indexOf(value)
        return if (index < 0) Int.MAX_VALUE else index
    }
}
