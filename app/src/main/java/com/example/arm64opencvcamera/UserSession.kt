package com.example.arm64opencvcamera

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * Manages user credentials, session state, and dynamic time-based greetings.
 */
class UserSession(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "arogyalens_user_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ABHA_ID = "user_abha_id"
        private const val KEY_REMEMBER_ME = "remember_me"

        const val DEFAULT_USER_NAME = "Utkarsh"
        const val DEFAULT_USER_PHONE = "+91 98765 43210"
        const val DEFAULT_ABHA_ID = "91-8472-1928-3019"
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userPhone: String
        get() = prefs.getString(KEY_USER_PHONE, DEFAULT_USER_PHONE) ?: DEFAULT_USER_PHONE
        set(value) = prefs.edit().putString(KEY_USER_PHONE, value).apply()

    var userAbhaId: String
        get() = prefs.getString(KEY_USER_ABHA_ID, DEFAULT_ABHA_ID) ?: DEFAULT_ABHA_ID
        set(value) = prefs.edit().putString(KEY_USER_ABHA_ID, value).apply()

    var isRememberMe: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_ME, true)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_ME, value).apply()

    fun saveUser(name: String, phone: String, rememberMe: Boolean = true) {
        val cleanName = name.trim().ifBlank { DEFAULT_USER_NAME }
        val cleanPhone = phone.trim().ifBlank { DEFAULT_USER_PHONE }
        // Generate pseudo-ABHA ID from phone if needed
        val last4 = if (cleanPhone.length >= 4) cleanPhone.takeLast(4) else "3019"
        val generatedAbha = "91-8472-1928-$last4"

        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_NAME, cleanName)
            .putString(KEY_USER_PHONE, cleanPhone)
            .putString(KEY_USER_ABHA_ID, generatedAbha)
            .putBoolean(KEY_REMEMBER_ME, rememberMe)
            .apply()
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }

    /**
     * Returns dynamic greeting data based on current system hour:
     * - Morning (04:00 - 11:59): "Good morning, {Name} 🌅"
     * - Afternoon (12:00 - 16:59): "Good afternoon, {Name} ☀️"
     * - Evening (17:00 - 20:59): "Good evening, {Name} 🌇"
     * - Night (21:00 - 03:59): "Good night, {Name} 🌙"
     */
    data class GreetingInfo(
        val title: String,
        val subtitle: String,
        val timeTag: String
    )

    fun getDynamicGreeting(): GreetingInfo {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val name = userName.trim().split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: DEFAULT_USER_NAME

        return when (hour) {
            in 4..11 -> GreetingInfo(
                title = "Good morning, $name 👋",
                subtitle = "How can we help you today?",
                timeTag = "Morning Care"
            )
            in 12..16 -> GreetingInfo(
                title = "Good afternoon, $name ☀️",
                subtitle = "Check your health vitals & triage records",
                timeTag = "Afternoon Care"
            )
            in 17..20 -> GreetingInfo(
                title = "Good evening, $name 🌇",
                subtitle = "Review your daily health summary",
                timeTag = "Evening Care"
            )
            else -> GreetingInfo(
                title = "Good night, $name 🌙",
                subtitle = "Emergency triage & PHC assistance is active 24/7",
                timeTag = "Night Assistance"
            )
        }
    }
}
