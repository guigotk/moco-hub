package com.guigo.mocohub.data

import android.content.Context
import com.guigo.mocohub.AppLinks
import com.guigo.mocohub.model.GameEvent
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object EventsRepository {
    private const val CACHE_PREFS = "events_cache"
    private const val KEY_EVENTS = "events_json"
    private const val KEY_UPDATED = "events_updated_at"
    private const val KEY_HISTORY = "events_history_json"

    data class ResultData(
        val events: List<GameEvent>,
        val fromCache: Boolean,
        val updatedAt: Long,
        val errorMessage: String? = null
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val EVENT_PATTERN = Pattern.compile(
        "(\\d{1,2}:\\d{2}\\s?[AP]M)\\s+([A-Za-zÀ-ÿ ]+?)\\s+in\\s+((?:\\d+h\\s*)?\\d+m)",
        Pattern.CASE_INSENSITIVE
    )

    fun fetchUpcomingEvents(context: Context): ResultData {
        return try {
            val text = fetchRawTextForDebug() ?: error("Resposta vazia")
            val events = parse(text)
            if (events.isEmpty()) error("Nenhum evento reconhecido")
            migratePassedEventsToHistory(context)
            saveCache(context, events)
            ResultData(events, false, System.currentTimeMillis())
        } catch (t: Throwable) {
            val cached = loadCache(context).filter { it.etaMillis > System.currentTimeMillis() - 5 * 60_000L }
            ResultData(cached, true, lastUpdated(context), t.message)
        }
    }

    fun fetchRawTextForDebug(): String? {
        val request = Request.Builder()
            .url(AppLinks.CELLSTRING_EVENTS)
            .header("User-Agent", "Mozilla/5.0 (Android) mo.co hub")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val html = response.body?.string() ?: return null
            return Jsoup.parse(html).text()
        }
    }

    private fun parse(pageText: String): List<GameEvent> {
        val now = System.currentTimeMillis()
        val out = mutableListOf<GameEvent>()
        val matcher = EVENT_PATTERN.matcher(pageText)
        while (matcher.find()) {
            val timeLabel = matcher.group(1)?.trim() ?: continue
            val name = matcher.group(2)?.trim() ?: continue
            val relative = matcher.group(3)?.trim() ?: continue
            val minutesFromNow = parseRelativeMinutes(relative)
            if (minutesFromNow < 0) continue
            out += GameEvent(name, timeLabel, now + minutesFromNow * 60_000L)
        }
        return out.distinctBy { it.name + it.timeLabel }
    }

    private fun parseRelativeMinutes(relative: String): Long {
        val h = Regex("(\\d+)h").find(relative)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val m = Regex("(\\d+)m").find(relative)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        if (h == 0L && m == 0L) return -1
        return h * 60 + m
    }

    private fun saveCache(context: Context, events: List<GameEvent>) {
        val arr = JSONArray()
        events.forEach { event ->
            arr.put(JSONObject().apply {
                put("name", event.name)
                put("timeLabel", event.timeLabel)
                put("etaMillis", event.etaMillis)
            })
        }
        context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_EVENTS, arr.toString())
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .apply()
    }

    fun loadCache(context: Context): List<GameEvent> = parseJson(
        context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).getString(KEY_EVENTS, "[]") ?: "[]"
    )

    fun lastUpdated(context: Context): Long =
        context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).getLong(KEY_UPDATED, 0L)

    fun loadHistory(context: Context): List<GameEvent> = parseJson(
        context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).getString(KEY_HISTORY, "[]") ?: "[]"
    ).sortedByDescending { it.etaMillis }.take(3)

    private fun migratePassedEventsToHistory(context: Context) {
        val now = System.currentTimeMillis()
        val passed = loadCache(context).filter { it.etaMillis < now }
        if (passed.isEmpty()) return
        val merged = (passed + loadHistory(context)).distinctBy { it.name + it.timeLabel + it.etaMillis }
            .sortedByDescending { it.etaMillis }.take(10)
        val arr = JSONArray()
        merged.forEach { event ->
            arr.put(JSONObject().apply {
                put("name", event.name); put("timeLabel", event.timeLabel); put("etaMillis", event.etaMillis)
            })
        }
        context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    private fun parseJson(raw: String): List<GameEvent> {
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(GameEvent(o.getString("name"), o.getString("timeLabel"), o.getLong("etaMillis")))
                }
            }
        }.getOrDefault(emptyList())
    }
}
