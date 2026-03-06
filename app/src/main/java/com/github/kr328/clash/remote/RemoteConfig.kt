package com.github.kr328.clash.remote

import android.content.Context

object RemoteConfig {
    /** API base URL — all native HTTP requests go here */
    const val DEFAULT_XBOARD_URL = "https://d7ccm19ki90mg.cloudfront.net"

    /** WebView URL — the website loaded inside AccountActivity */
    const val DEFAULT_WEBVIEW_URL = "https://my.yue.to"

    fun getXboardUrl(context: Context): String = DEFAULT_XBOARD_URL

    fun getWebviewUrl(context: Context): String = DEFAULT_WEBVIEW_URL
}
