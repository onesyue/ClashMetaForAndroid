package com.github.kr328.clash.xboard

import android.content.Context
import android.content.SharedPreferences
import com.github.kr328.clash.remote.RemoteConfig
import com.github.kr328.clash.util.SecurePrefs

/**
 * Persists the Xboard auth_data token and base URL using encrypted storage
 * so native API calls can be made without requiring the WebView to be open.
 */
object XBoardSession {
    private const val LEGACY_PREF_NAME = "yt_xboard_session"
    private const val KEY_AUTH_DATA = "auth_data"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_MIGRATED = "session_migrated"

    private fun prefs(context: Context): SharedPreferences = SecurePrefs.get(context)

    /**
     * Migrate plaintext tokens from legacy SharedPreferences to encrypted storage.
     * Called once on app startup; deletes legacy data after migration.
     */
    fun migrateIfNeeded(context: Context) {
        val secure = prefs(context)
        if (secure.getBoolean(KEY_MIGRATED, false)) return

        val legacy = context.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE)
        val authData = legacy.getString(KEY_AUTH_DATA, null)
        val baseUrl = legacy.getString(KEY_BASE_URL, null)

        secure.edit().apply {
            if (!authData.isNullOrBlank()) putString(KEY_AUTH_DATA, authData)
            if (!baseUrl.isNullOrBlank()) putString(KEY_BASE_URL, baseUrl)
            putBoolean(KEY_MIGRATED, true)
            apply()
        }

        // Wipe legacy plaintext data
        legacy.edit().clear().apply()
    }

    fun save(context: Context, authData: String, baseUrl: String) {
        prefs(context).edit()
            .putString(KEY_AUTH_DATA, authData)
            .putString(KEY_BASE_URL, baseUrl)
            .apply()
    }

    fun getAuthData(context: Context): String? =
        prefs(context).getString(KEY_AUTH_DATA, null)?.takeIf { it.isNotBlank() }

    fun getBaseUrl(context: Context): String =
        prefs(context).getString(KEY_BASE_URL, null)
            ?.takeIf { it.isNotBlank() }
            ?: RemoteConfig.getXboardUrl(context)

    fun clear(context: Context) {
        prefs(context).edit()
            .remove(KEY_AUTH_DATA)
            .remove(KEY_BASE_URL)
            .apply()
    }
}
