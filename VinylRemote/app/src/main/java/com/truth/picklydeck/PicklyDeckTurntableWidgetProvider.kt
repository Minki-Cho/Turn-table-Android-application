package com.truth.picklydeck

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class PicklyDeckTurntableWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        PicklyDeckExternalControls.refreshWidget(context.applicationContext)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PicklyDeckExternalControls.refreshWidget(context.applicationContext)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        PicklyDeckExternalControls.refreshWidget(context.applicationContext)
    }
}
