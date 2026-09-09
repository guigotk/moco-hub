package com.guigo.mocohub

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.guigo.mocohub.util.Prefs

class MocoHubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        applyTheme(Prefs.getThemeMode(this))
    }

    companion object {
        fun applyTheme(mode: String) {
            val nightMode = when (mode) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }
}
