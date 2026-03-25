package com.truth.vinylremote

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat as MediaStyleCompat

internal object VinylExternalControls {
    private const val CHANNEL_ID = "vinyl_remote_controls_v2"
    private const val CHANNEL_NAME = "Vinyl Remote Controls"
    private const val NOTIFICATION_ID = 8041

    private const val SNAPSHOT_PREFS = "vinyl_remote_widget_snapshot"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"
    private const val KEY_IS_PLAYING = "is_playing"
    private const val KEY_NEEDLE_ON_RECORD = "needle_on_record"
    private const val KEY_POSITION_MS = "position_ms"
    private const val KEY_DURATION_MS = "duration_ms"

    fun publish(context: Context, state: VinylUiState) {
        ensureChannel(context)
        saveSnapshot(context, state)

        val needleAction = if (state.isPlaying) {
            VinylControlActions.ACTION_NEEDLE_OUT
        } else {
            VinylControlActions.ACTION_NEEDLE_IN
        }
        val needleLabel = if (state.isPlaying) "Needle Out" else "Needle In"
        val needleIcon = if (state.isPlaying) {
            android.R.drawable.ic_menu_close_clear_cancel
        } else {
            android.R.drawable.ic_input_add
        }
        val playPauseLabel = if (state.isPlaying) "Pause" else "Play"
        val playPauseIcon = if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val summaryText = when {
            state.artist.isNotBlank() -> state.artist
            state.connectedPackage != null -> state.connectedPackage
            else -> "Control playback and tonearm from lock screen"
        }
        val bodyText = summaryText

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
            .setContentText(bodyText)
            .setSubText(summaryText)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                needleIcon,
                needleLabel,
                broadcastIntent(context, needleAction, 1)
            )
            .addAction(
                android.R.drawable.ic_media_rew,
                "-10s",
                broadcastIntent(context, VinylControlActions.ACTION_SEEK_BACK, 2)
            )
            .addAction(
                playPauseIcon,
                playPauseLabel,
                broadcastIntent(context, VinylControlActions.ACTION_TOGGLE_PLAY_PAUSE, 3)
            )
            .addAction(
                android.R.drawable.ic_media_ff,
                "+10s",
                broadcastIntent(context, VinylControlActions.ACTION_SEEK_FORWARD, 4)
            )
            .setStyle(
                MediaStyleCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()

        if (canPostNotifications(context)) {
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Notification permission may be revoked while app is running.
            }
        }
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
        val positionMs = prefs.getLong(KEY_POSITION_MS, 0L)
        val durationMs = prefs.getLong(KEY_DURATION_MS, 0L)
        return VinylUiState(
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
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
            .putLong(KEY_POSITION_MS, state.positionMs)
            .putLong(KEY_DURATION_MS, state.durationMs)
            .apply()
    }

    private fun updateWidget(context: Context, state: VinylUiState) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateWidgetSet(
            context = context,
            appWidgetManager = appWidgetManager,
            widgetComponent = ComponentName(context, VinylRemoteWidgetProvider::class.java),
            state = state
        )
        updateWidgetSet(
            context = context,
            appWidgetManager = appWidgetManager,
            widgetComponent = ComponentName(context, VinylTurntableWidgetProvider::class.java),
            state = state
        )
    }

    private fun updateWidgetSet(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetComponent: ComponentName,
        state: VinylUiState
    ) {
        val ids = appWidgetManager.getAppWidgetIds(widgetComponent)
        if (ids.isEmpty()) return

        val isTurntableWidget =
            widgetComponent.className == VinylTurntableWidgetProvider::class.java.name
        val baseRequestCode = if (isTurntableWidget) 5000 else 1000

        ids.forEach { appWidgetId ->
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val remoteViews = RemoteViews(
                context.packageName,
                widgetLayoutRes(options, isTurntableWidget)
            )
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
                R.id.widget_btn_extra,
                android.R.drawable.ic_menu_view
            )

            remoteViews.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                broadcastIntent(context, VinylControlActions.ACTION_PREV, baseRequestCode + 11 + appWidgetId * 10)
            )
            remoteViews.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                broadcastIntent(
                    context,
                    VinylControlActions.ACTION_TOGGLE_PLAY_PAUSE,
                    baseRequestCode + 12 + appWidgetId * 10
                )
            )
            remoteViews.setOnClickPendingIntent(
                R.id.widget_btn_next,
                broadcastIntent(context, VinylControlActions.ACTION_NEXT, baseRequestCode + 13 + appWidgetId * 10)
            )
            remoteViews.setOnClickPendingIntent(
                R.id.widget_btn_extra,
                openAppIntent(context, baseRequestCode + 14 + appWidgetId * 10)
            )
            remoteViews.setOnClickPendingIntent(
                R.id.widget_root,
                openAppIntent(context, baseRequestCode + 15 + appWidgetId * 10)
            )

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private fun widgetLayoutRes(options: Bundle, isTurntableWidget: Boolean): Int {
        if (isTurntableWidget) return R.layout.widget_vinyl_turntable
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        return if (minWidth >= 220 && minHeight >= 220) {
            R.layout.widget_vinyl_remote
        } else {
            R.layout.widget_vinyl_remote_compact
        }
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
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

    private fun openAppIntent(context: Context, requestCode: Int): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
