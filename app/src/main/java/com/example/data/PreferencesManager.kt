package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("timeline_prefs", Context.MODE_PRIVATE)

    // Single Earthy Sage theme - theme name is now informational only
    private val _themeName = MutableStateFlow("Earthy Sage")
    val themeName: StateFlow<String> = _themeName.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "LIGHT") ?: "LIGHT")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _use24HourFormat = MutableStateFlow(prefs.getBoolean(KEY_24_HOUR, false))
    val use24HourFormat: StateFlow<Boolean> = _use24HourFormat.asStateFlow()

    private val _defaultReminderMinutes = MutableStateFlow(prefs.getInt(KEY_DEFAULT_REMINDER, -1))
    val defaultReminderMinutes: StateFlow<Int> = _defaultReminderMinutes.asStateFlow()

    private val _defaultForwardIncomplete = MutableStateFlow(prefs.getBoolean(KEY_DEFAULT_FORWARD, true))
    val defaultForwardIncomplete: StateFlow<Boolean> = _defaultForwardIncomplete.asStateFlow()

    fun setThemeName(name: String) {
        // Theme name is now fixed to Earthy Sage
        prefs.edit().putString(KEY_THEME_NAME, "Earthy Sage").apply()
        _themeName.value = "Earthy Sage"
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setUse24HourFormat(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_24_HOUR, enabled).apply()
        _use24HourFormat.value = enabled
    }

    fun setDefaultReminderMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DEFAULT_REMINDER, minutes).apply()
        _defaultReminderMinutes.value = minutes
    }

    fun setDefaultForwardIncomplete(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEFAULT_FORWARD, enabled).apply()
        _defaultForwardIncomplete.value = enabled
    }

    companion object {
        private const val KEY_THEME_NAME = "theme_name"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_24_HOUR = "use_24_hour"
        private const val KEY_DEFAULT_REMINDER = "default_reminder"
        private const val KEY_DEFAULT_FORWARD = "default_forward"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
