package com.github.kr328.clash.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple offline cache for API responses using SharedPreferences.
 * Stores JSON strings with timestamps for staleness checks.
 */
object OfflineCache {

    private const val PREFS_NAME = "yt_offline_cache"
    private const val SUFFIX_TIME = "_time"
    private const val DEFAULT_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun put(context: Context, key: String, json: String) {
        prefs(context).edit()
            .putString(key, json)
            .putLong(key + SUFFIX_TIME, System.currentTimeMillis())
            .apply()
    }

    fun get(context: Context, key: String): String? {
        return prefs(context).getString(key, null)
    }

    fun isFresh(context: Context, key: String, ttlMs: Long = DEFAULT_TTL_MS): Boolean {
        val time = prefs(context).getLong(key + SUFFIX_TIME, 0)
        return System.currentTimeMillis() - time < ttlMs
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    // Cache keys
    const val KEY_USER_INFO = "user_info"
    const val KEY_STORE_PLANS = "store_plans"
    const val KEY_ORDERS = "orders"
    const val KEY_NOTICES = "notices"
}
