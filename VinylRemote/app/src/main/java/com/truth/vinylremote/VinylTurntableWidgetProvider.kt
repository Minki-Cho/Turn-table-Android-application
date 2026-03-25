package com.truth.vinylremote

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class VinylTurntableWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        VinylExternalControls.refreshWidget(context.applicationContext)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        VinylExternalControls.refreshWidget(context.applicationContext)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        VinylExternalControls.refreshWidget(context.applicationContext)
    }
}
