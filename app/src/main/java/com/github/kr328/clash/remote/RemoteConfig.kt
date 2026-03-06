package com.github.kr328.clash.remote

import android.content.Context

object RemoteConfig {
    const val DEFAULT_XBOARD_URL = "https://my.yue.to"

    // URL 直接硬编码，不再做运行时网络请求
    fun getXboardUrl(context: Context): String = DEFAULT_XBOARD_URL
}
