package com.example.moonshineproject.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SentinelSettings(
    val afkStillnessSeconds: Int = 20,
    val reminderMinutes: Int = 30,
    val thresholdMarginDb: Int = 15,
    val calibrationSeconds: Int = 7,
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0
)

class AppPreferences private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<SentinelSettings> = _settingsFlow

    fun loadSettings(): SentinelSettings {
        return SentinelSettings(
            afkStillnessSeconds = prefs.getInt(KEY_AFK_SECONDS, 20),
            reminderMinutes = prefs.getInt(KEY_REMINDER_MINUTES, 30),
            thresholdMarginDb = prefs.getInt(KEY_THRESHOLD_MARGIN_DB, 15),
            calibrationSeconds = prefs.getInt(KEY_CALIBRATION_SECONDS, 7),
            bedtimeHour = prefs.getInt(KEY_BEDTIME_HOUR, 23),
            bedtimeMinute = prefs.getInt(KEY_BEDTIME_MINUTE, 0)
        )
    }

    fun saveSettings(settings: SentinelSettings) {
        prefs.edit()
            .putInt(KEY_AFK_SECONDS, settings.afkStillnessSeconds)
            .putInt(KEY_REMINDER_MINUTES, settings.reminderMinutes)
            .putInt(KEY_THRESHOLD_MARGIN_DB, settings.thresholdMarginDb)
            .putInt(KEY_CALIBRATION_SECONDS, settings.calibrationSeconds)
            .putInt(KEY_BEDTIME_HOUR, settings.bedtimeHour)
            .putInt(KEY_BEDTIME_MINUTE, settings.bedtimeMinute)
            .apply()
        _settingsFlow.value = settings
    }

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var sentinelExplicitlyActive: Boolean
        get() = prefs.getBoolean(KEY_SENTINEL_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_SENTINEL_ACTIVE, value).apply()

    companion object {
        private const val PREFS_NAME = "moonshine_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_SENTINEL_ACTIVE = "sentinel_active"

        private const val KEY_AFK_SECONDS = "afk_seconds"
        private const val KEY_REMINDER_MINUTES = "reminder_minutes"
        private const val KEY_THRESHOLD_MARGIN_DB = "threshold_margin_db"
        private const val KEY_CALIBRATION_SECONDS = "calibration_seconds"
        private const val KEY_BEDTIME_HOUR = "bedtime_hour"
        private const val KEY_BEDTIME_MINUTE = "bedtime_minute"

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferences(context).also { INSTANCE = it }
            }
        }
    }
}
