package com.github.kr328.clash.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Offline cache for API responses using encrypted SharedPreferences.
 * Stores JSON strings with timestamps for staleness checks.
 */
object OfflineCache {

    private const val LEGACY_PREFS_NAME = "yt_offline_cache"
    private const val SUFFIX_TIME = "_time"
    private const val KEY_CACHE_MIGRATED = "cache_migrated"
    private const val DEFAULT_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours

    private fun prefs(context: Context): SharedPreferences = SecurePrefs.get(context)

    /**
     * Migrate legacy plaintext cache to encrypted storage.
     */
    fun migrateIfNeeded(context: Context) {
        val secure = prefs(context)
        if (secure.getBoolean(KEY_CACHE_MIGRATED, false)) return

        val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val allEntries = legacy.all
        if (allEntries.isNotEmpty()) {
            secure.edit().apply {
                for ((key, value) in allEntries) {
                    when (value) {
                        is String -> putString(key, value)
                        is Long -> putLong(key, value)
                    }
                }
                putBoolean(KEY_CACHE_MIGRATED, true)
                apply()
            }
            legacy.edit().clear().apply()
        } else {
            secure.edit().putBoolean(KEY_CACHE_MIGRATED, true).apply()
        }
    }

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
        val secure = prefs(context)
        secure.edit().apply {
            // Only clear cache keys, preserve migration flags
            for (key in secure.all.keys) {
                if (key != KEY_CACHE_MIGRATED && !key.startsWith("session_")) {
                    remove(key)
                }
            }
            apply()
        }
    }

    // Cache keys
    const val KEY_USER_INFO = "user_info"
    const val KEY_STORE_PLANS = "store_plans"
    const val KEY_ORDERS = "orders"
    const val KEY_NOTICES = "notices"
}
