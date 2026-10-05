package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TimezoneItem(
    val zoneId: String,
    val displayName: String,
    val offsetString: String,
    val flagOrLabel: String
)

object TimezoneManager {

    private const val PREFS_NAME = "assistant_timezone_prefs"
    private const val KEY_ZONE_ID = "selected_zone_id"
    private const val KEY_AUTO_DETECT = "auto_detect_timezone"

    private var prefs: SharedPreferences? = null

    private val _userZoneIdFlow = MutableStateFlow<ZoneId>(ZoneId.systemDefault())
    val userZoneIdFlow: StateFlow<ZoneId> = _userZoneIdFlow.asStateFlow()

    private val _isAutoDetectFlow = MutableStateFlow(true)
    val isAutoDetectFlow: StateFlow<Boolean> = _isAutoDetectFlow.asStateFlow()

    val popularTimezones = listOf(
        TimezoneItem("Asia/Kolkata", "India (IST)", "UTC+05:30", "🇮🇳"),
        TimezoneItem("America/New_York", "US Eastern (ET)", "UTC-04:00", "🇺🇸"),
        TimezoneItem("America/Chicago", "US Central (CT)", "UTC-05:00", "🇺🇸"),
        TimezoneItem("America/Denver", "US Mountain (MT)", "UTC-06:00", "🇺🇸"),
        TimezoneItem("America/Los_Angeles", "US Pacific (PT)", "UTC-07:00", "🇺🇸"),
        TimezoneItem("Europe/London", "London (GMT/BST)", "UTC+01:00", "🇬🇧"),
        TimezoneItem("Europe/Paris", "Central Europe (CET)", "UTC+02:00", "🇪🇺"),
        TimezoneItem("Asia/Dubai", "Dubai / Gulf (GST)", "UTC+04:00", "🇦🇪"),
        TimezoneItem("Asia/Singapore", "Singapore (SGT)", "UTC+08:00", "🇸🇬"),
        TimezoneItem("Asia/Tokyo", "Japan (JST)", "UTC+09:00", "🇯🇵"),
        TimezoneItem("Australia/Sydney", "Sydney (AEST)", "UTC+10:00", "🇦🇺")
    )

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        val isAuto = sp.getBoolean(KEY_AUTO_DETECT, true)
        _isAutoDetectFlow.value = isAuto

        val savedZone = sp.getString(KEY_ZONE_ID, null)
        val resolvedZone = if (!isAuto && !savedZone.isNullOrBlank()) {
            try {
                ZoneId.of(savedZone)
            } catch (e: Exception) {
                ZoneId.systemDefault()
            }
        } else {
            ZoneId.systemDefault()
        }

        _userZoneIdFlow.value = resolvedZone
    }

    fun getUserZoneId(): ZoneId {
        return _userZoneIdFlow.value
    }

    fun setTimezone(zoneId: ZoneId, autoDetect: Boolean = false) {
        _userZoneIdFlow.value = zoneId
        _isAutoDetectFlow.value = autoDetect

        prefs?.edit()
            ?.putString(KEY_ZONE_ID, zoneId.id)
            ?.putBoolean(KEY_AUTO_DETECT, autoDetect)
            ?.apply()
    }

    fun setAutoDetect(autoDetect: Boolean) {
        _isAutoDetectFlow.value = autoDetect
        if (autoDetect) {
            val systemZone = ZoneId.systemDefault()
            _userZoneIdFlow.value = systemZone
            prefs?.edit()
                ?.putBoolean(KEY_AUTO_DETECT, true)
                ?.putString(KEY_ZONE_ID, systemZone.id)
                ?.apply()
        } else {
            prefs?.edit()
                ?.putBoolean(KEY_AUTO_DETECT, false)
                ?.apply()
        }
    }

    fun getShortLabel(zoneId: ZoneId): String {
        return when (zoneId.id) {
            "Asia/Kolkata", "Asia/Calcutta" -> "IST (GMT+5:30)"
            "America/New_York" -> "ET (GMT-4)"
            "America/Los_Angeles" -> "PT (GMT-7)"
            "America/Chicago" -> "CT (GMT-5)"
            "Europe/London" -> "GMT/BST"
            "Europe/Paris" -> "CET"
            else -> {
                val zdt = ZonedDateTime.now(zoneId)
                val offset = zdt.format(DateTimeFormatter.ofPattern("xxx", Locale.US))
                "${zoneId.id.substringAfterLast('/')} ($offset)"
            }
        }
    }

    fun getFormattedCurrentTime(zoneId: ZoneId): String {
        val zdt = ZonedDateTime.now(zoneId)
        return zdt.format(DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a", Locale.US))
    }
}
