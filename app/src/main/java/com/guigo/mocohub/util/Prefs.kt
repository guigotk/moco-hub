package com.guigo.mocohub.util

import android.content.Context

object Prefs {
    private const val FILE = "moco_hub_prefs"
    private const val KEY_LEAD_TIMES = "lead_times_minutes"
    private const val KEY_NOTIFIED = "notified_event_keys"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_EVENT_PREFIX = "event_leads_"
    private const val KEY_FILTER = "event_filter"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun setLeadTimes(context: Context, minutes: Set<String>) =
        prefs(context).edit().putStringSet(KEY_LEAD_TIMES, minutes).apply()

    fun getLeadTimes(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_LEAD_TIMES, setOf("15", "10", "5", "0")) ?: setOf("15", "10", "5", "0")

    fun setLeadTimesForEvent(context: Context, typeKey: String, minutes: Set<String>) =
        prefs(context).edit().putStringSet(KEY_EVENT_PREFIX + typeKey, minutes).apply()

    fun getLeadTimesForEvent(context: Context, typeKey: String): Set<String> =
        prefs(context).getStringSet(KEY_EVENT_PREFIX + typeKey, null) ?: getLeadTimes(context)

    fun wasAlreadyNotified(context: Context, key: String): Boolean =
        prefs(context).getStringSet(KEY_NOTIFIED, emptySet())?.contains(key) == true

    fun markNotified(context: Context, key: String) {
        val set = (prefs(context).getStringSet(KEY_NOTIFIED, emptySet()) ?: emptySet()).toMutableSet()
        set.add(key)
        val trimmed = if (set.size > 300) set.toList().takeLast(200).toMutableSet() else set
        prefs(context).edit().putStringSet(KEY_NOTIFIED, trimmed).apply()
    }

    fun setThemeMode(context: Context, mode: String) =
        prefs(context).edit().putString(KEY_THEME, mode).apply()

    fun getThemeMode(context: Context): String =
        prefs(context).getString(KEY_THEME, "system") ?: "system"

    fun setEventFilter(context: Context, filter: String) =
        prefs(context).edit().putString(KEY_FILTER, filter).apply()

    fun getEventFilter(context: Context): String =
        prefs(context).getString(KEY_FILTER, "all") ?: "all"
}
