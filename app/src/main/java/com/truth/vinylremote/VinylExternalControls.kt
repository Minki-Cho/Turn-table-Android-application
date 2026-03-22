package com.truth.vinylremote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

internal object VinylExternalControls {
    private const val CHANNEL_ID = "vinyl_remote_controls"
    private const val CHANNEL_NAME = "Vinyl Remote Controls"
    private const val NOTIFICATION_ID = 8041

    private const val SNAPSHOT_PREFS = "vinyl_remote_widget_snapshot"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"
    private const val KEY_IS_PLAYING = "is_playing"
    private const val KEY_NEEDLE_ON_RECORD = "needle_on_record"

    fun publish(context: Context, state: VinylUiState) {
        ensureChannel(context)
        saveSnapshot(context, state)

        val toggleAction = if (state.isPlaying) {
            VinylControlActions.ACTION_NEEDLE_OUT
        } else {
            VinylControlActions.ACTION_NEEDLE_IN
        }
        val toggleLabel = if (state.isPlaying) "Needle Out" else "Needle In"
        val toggleIcon = if (state.isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val openApp = PendingIntent.getActivity(
            context,
            100,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(state.title.ifBlank { "Vinyl Remote" })
            .setContentText(
                when {
                    state.artist.isNotBlank() -> state.artist
                    state.connectedPackage != null -> state.connectedPackage
                    else -> "Control playback and tonearm from lock screen"
                }
            )
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_media_previous,
                "Prev",
                broadcastIntent(context, VinylControlActions.ACTION_PREV, 1)
            )
            .addAction(
                toggleIcon,
                toggleLabel,
                broadcastIntent(context, toggleAction, 2)
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                broadcastIntent(context, VinylControlActions.ACTION_NEXT, 3)
            )
            .addAction(
                if (state.isPlaying) android.R.drawable.ic_menu_close_clear_cancel else android.R.drawable.ic_input_add,
                if (state.isPlaying) "Needle Out" else "Needle In",
                broadcastIntent(
                    context,
                    if (state.isPlaying) {
                        VinylControlActions.ACTION_NEEDLE_OUT
                    } else {
                        VinylControlActions.ACTION_NEEDLE_IN
                    },
                    4
                )
            )
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        updateWidget(context, state)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun refreshWidget(context: Context) {
        updateWidget(context, loadSnapshot(context))
    }

    fun loadSnapshot(context: Context): VinylUiState {
        val prefs = context.getSharedPreferences(SNAPSHOT_PREFS, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, "Vinyl Remote").orEmpty()
        val artist = prefs.getString(KEY_ARTIST, "No active playback").orEmpty()
        val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)
        val needleOnRecord = prefs.getBoolean(KEY_NEEDLE_ON_RECORD, false)
        return VinylUiState(
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            needleProgress = if (needleOnRecord) 0.30f else 0.05f
        )
    }

    private fun saveSnapshot(context: Context, state: VinylUiState) {
        context.getSharedPreferences(SNAPSHOT_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TITLE, state.title)
            .putString(KEY_ARTIST, state.artist)
            .putBoolean(KEY_IS_PLAYING, state.isPlaying)
            .putBoolean(KEY_NEEDLE_ON_RECORD, state.needleProgress >= 0.30f)
            .apply()
    }

    private fun updateWidget(context: Context, state: VinylUiState) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, VinylRemoteWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(widgetComponent)
        if (ids.isEmpty()) return

        ids.forEach { appWidgetId ->
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_vinyl_remote)
            remoteViews.setTextViewText(
                R.id.widget_title,
                state.title.ifBlank { "Vinyl Remote" }
            )
            remoteViews.setTextViewText(
                R.id.widget_subtitle,
                state.artist.ifBlank { "No active playback" }
            )
            remoteViews.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
            remoteViews.setImageViewResource(
                R.id.widget_btn_needle,
                if (state.isPlaying) android.R.drawable.ic_menu_close_clear_cancel else android.R.drawable.ic_input_add
            )

            remoteViews.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                broadcastIntent(context, VinylControlActions.ACTION_PREV, 11 + appWidgetId * 10)
            )
            remoteViews.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                broadcastIntent(context, VinylControlActions.ACTION_TOGGLE_PLAY_PAUSE, 12 + appWidgetId * 10)
            )
            remoteViews.setOnClickPendingIntent(
                R.id.widget_btn_next,
                broadcastIntent(context, VinylControlActions.ACTION_NEXT, 13 + appWidgetId * 10)
            )
            remoteViews.setOnClickPendingIntent(
                R.id.widget_btn_needle,
                broadcastIntent(
                    context,
                    if (state.isPlaying) VinylControlActions.ACTION_NEEDLE_OUT else VinylControlActions.ACTION_NEEDLE_IN,
                    14 + appWidgetId * 10
                )
            )

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                description = "Playback and tonearm controls from notification/lock screen."
            }
        )
    }

    private fun broadcastIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            VinylControlActions.intent(context, action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
