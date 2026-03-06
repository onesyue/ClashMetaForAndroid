package com.github.kr328.clash.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object RemoteConfig {
    private const val OSS_URL =
        "https://f004.backblazeb2.com/file/yuetoto/yt-remote-config.json"
    private const val PREF_NAME = "yt_remote_config"
    private const val KEY_XBOARD_URL = "xboard_url"
    const val DEFAULT_XBOARD_URL = "https://my.yue.to"

    fun getXboardUrl(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_XBOARD_URL, DEFAULT_XBOARD_URL) ?: DEFAULT_XBOARD_URL

    suspend fun fetchAndCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            val text = URL(OSS_URL).openConnection().apply {
                connectTimeout = 5_000
                readTimeout = 5_000
            }.getInputStream().bufferedReader().readText()

            val url = JSONObject(text).optString("xboard_url", "").trim()
            if (url.isNotBlank()) {
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().putString(KEY_XBOARD_URL, url).apply()
            }
        } catch (_: Exception) {
            // 静默失败，使用缓存或默认值
        }
    }
}
