package com.guigo.mocohub.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guigo.mocohub.R
import com.guigo.mocohub.notification.NotificationHelper
import com.guigo.mocohub.util.Prefs
import kotlin.math.abs

class EventAlertWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val key = inputData.getString("key") ?: return Result.failure()
        if (Prefs.wasAlreadyNotified(applicationContext, key)) return Result.success()
        val title = inputData.getString("title") ?: return Result.failure()
        val lead = inputData.getInt("lead", 0)
        val triggerAt = inputData.getLong("triggerAt", 0L)
        val now = System.currentTimeMillis()

        // WorkManager pode ser atrasado por Doze/otimização de bateria. Não exibimos
        // alertas antigos: uma notificação de 15 min não deve chegar junto da de 10 min.
        val tolerance = if (lead <= 0) 2 * 60_000L else 90_000L
        if (triggerAt > 0L && abs(now - triggerAt) > tolerance) return Result.success()

        val message = if (lead <= 0) applicationContext.getString(R.string.notification_started)
        else applicationContext.getString(R.string.notification_in_minutes, lead)
        NotificationHelper.show(applicationContext, key.hashCode(), title, message)
        Prefs.markNotified(applicationContext, key)
        return Result.success()
    }
}
