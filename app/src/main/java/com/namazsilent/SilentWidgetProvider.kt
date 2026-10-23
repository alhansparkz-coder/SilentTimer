package com.namazsilent

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.RemoteViews
import java.util.Date

class SilentWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        // Safety net: if a timer should already have ended, restore now.
        if (SilenceController.isActive(context) && SilenceController.endAt(context) <= System.currentTimeMillis()) {
            SilenceController.restore(context)
            return
        }
        for (id in appWidgetIds) update(context, manager, id)
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SilentWidgetProvider::class.java))
            for (id in ids) update(context, manager, id)
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_silent)

            if (SilenceController.isActive(context)) {
                val until = DateFormat.getTimeFormat(context).format(Date(SilenceController.endAt(context)))
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_active)
                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_sound_off)
                views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title_active))
                views.setTextViewText(R.id.widget_subtitle, context.getString(R.string.widget_subtitle_active, until))
                views.setOnClickPendingIntent(R.id.widget_root, SilenceController.restoreNowIntent(context))
            } else {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_idle)
                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_sound_on)
                views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title_idle))
                views.setTextViewText(R.id.widget_subtitle, context.getString(R.string.widget_subtitle_idle))
                val open = PendingIntent.getActivity(
                    context, 3,
                    Intent(context, DurationPickerActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, open)
            }
            manager.updateAppWidget(id, views)
        }
    }
}
