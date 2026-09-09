package com.guigo.mocohub.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.guigo.mocohub.MainActivity
import com.guigo.mocohub.R
import com.guigo.mocohub.data.EventsRepository
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NextEventWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NextEventWidget::class.java))
            ids.forEach { update(context, manager, it) }
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_next_event)
            val openIntent = Intent(context, MainActivity::class.java).putExtra("open_events", true)
            val pending = PendingIntent.getActivity(context, id, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widgetRoot, pending)

            val next = EventsRepository.loadCache(context)
                .filter { it.etaMillis > System.currentTimeMillis() }
                .minByOrNull { it.etaMillis }

            if (next == null) {
                views.setImageViewResource(R.id.widgetEventIcon, R.drawable.ic_event_chaos)
                views.setTextViewText(R.id.widgetEventName, "Abra o mo.co hub")
                views.setTextViewText(R.id.widgetEventEta, "Atualize a agenda de eventos")
                views.setTextViewText(R.id.widgetFooter, "Toque para abrir")
            } else {
                val mins = TimeUnit.MILLISECONDS.toMinutes((next.etaMillis - System.currentTimeMillis()).coerceAtLeast(0))
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(next.etaMillis))
                views.setImageViewResource(R.id.widgetEventIcon, if (next.typeKey == "overcharged") R.drawable.ic_event_overcharged else R.drawable.ic_event_chaos)
                views.setTextViewText(R.id.widgetEventName, next.name)
                views.setTextViewText(R.id.widgetEventEta, "em $mins min • $time")
                views.setTextViewText(R.id.widgetFooter, "Toque para ver detalhes e alertas")
            }
            manager.updateAppWidget(id, views)
        }
    }
}
