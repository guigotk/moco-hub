package com.guigo.mocohub.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guigo.mocohub.R
import com.guigo.mocohub.data.EventsRepository
import com.guigo.mocohub.notification.NotificationHelper
import com.guigo.mocohub.util.Prefs
import com.guigo.mocohub.widget.NextEventWidget
import kotlin.math.abs

class EventsCheckWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val result = EventsRepository.fetchUpcomingEvents(applicationContext)
            val now = System.currentTimeMillis()
            for (event in result.events) {
                val leadTimes = Prefs.getLeadTimesForEvent(applicationContext, event.typeKey).mapNotNull { it.toIntOrNull() }
                for (lead in leadTimes) {
                    val triggerAt = event.etaMillis - lead * 60_000L
                    val tolerance = if (lead <= 0) 2 * 60_000L else 90_000L
                    if (abs(now - triggerAt) > tolerance) continue
                    val key = "${event.typeKey}_${event.timeLabel}_$lead"
                    if (Prefs.wasAlreadyNotified(applicationContext, key)) continue
                    val message = if (lead <= 0) applicationContext.getString(R.string.notification_started)
                    else applicationContext.getString(R.string.notification_in_minutes, lead)
                    NotificationHelper.show(applicationContext, key.hashCode(), event.name, message)
                    Prefs.markNotified(applicationContext, key)
                }
            }
            EventAlertScheduler.scheduleUpcoming(applicationContext, result.events)
            NextEventWidget.updateAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object { const val UNIQUE_WORK_NAME = "moco_events_check" }
}
