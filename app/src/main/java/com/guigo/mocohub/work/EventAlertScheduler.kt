package com.guigo.mocohub.work

import android.content.Context
import androidx.work.*
import com.guigo.mocohub.model.GameEvent
import com.guigo.mocohub.util.Prefs
import java.util.concurrent.TimeUnit

object EventAlertScheduler {
    private val supportedLeads = listOf(15, 10, 5, 0)

    fun scheduleUpcoming(context: Context, events: List<GameEvent>) {
        val wm = WorkManager.getInstance(context)
        val now = System.currentTimeMillis()
        events.forEach { event ->
            val enabled = Prefs.getLeadTimesForEvent(context, event.typeKey).mapNotNull { it.toIntOrNull() }.toSet()
            supportedLeads.forEach { lead ->
                val workName = uniqueName(event, lead)
                if (lead !in enabled) {
                    wm.cancelUniqueWork(workName)
                    return@forEach
                }
                val triggerAt = event.etaMillis - lead * 60_000L
                val delay = triggerAt - now
                if (delay <= 0L) return@forEach
                val key = "${event.typeKey}_${event.timeLabel}_$lead"
                val req = OneTimeWorkRequestBuilder<EventAlertWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf("key" to key, "title" to event.name, "lead" to lead, "triggerAt" to triggerAt))
                    .build()
                wm.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, req)
            }
        }
    }

    private fun uniqueName(event: GameEvent, lead: Int) = "event_alert_${event.typeKey}_${event.timeLabel.replace(":", "").replace(" ", "")}_$lead"
}
