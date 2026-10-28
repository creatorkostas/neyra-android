package dev.neyra.companion

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class QuickCaptureWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val view = RemoteViews(context.packageName, R.layout.quick_capture_widget)
            listOf(R.id.widget_note to "note", R.id.widget_voice to "record").forEach { (button, action) ->
                val intent = Intent(context, MainActivity::class.java).setAction("dev.neyra.$action").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                view.setOnClickPendingIntent(button, PendingIntent.getActivity(context, id * 2 + if (action == "note") 0 else 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            manager.updateAppWidget(id, view)
        }
    }
}
