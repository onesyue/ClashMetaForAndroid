package com.github.kr328.clash.xboard

import android.content.Context
import com.github.kr328.clash.remote.RemoteConfig

/**
 * Persists the Xboard auth_data token and base URL so native API calls
 * can be made without requiring the WebView to be open.
 */
object XBoardSession {
    private const val PREF_NAME = "yt_xboard_session"
    private const val KEY_AUTH_DATA = "auth_data"
    private const val KEY_BASE_URL = "base_url"

    fun save(context: Context, authData: String, baseUrl: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AUTH_DATA, authData)
            .putString(KEY_BASE_URL, baseUrl)
            .apply()
    }

    fun getAuthData(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AUTH_DATA, null)?.takeIf { it.isNotBlank() }

    fun getBaseUrl(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, null)
            ?.takeIf { it.isNotBlank() }
            ?: RemoteConfig.getXboardUrl(context)

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
