package com.github.kr328.clash.design

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.kr328.clash.design.databinding.DesignAccountBinding
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root

@SuppressLint("SetJavaScriptEnabled")
class AccountDesign(context: Context, private val baseUrl: String, initialPath: String = "", showSyncButton: Boolean = true) : Design<AccountDesign.Request>(context) {

    sealed class Request {
        data class SyncSubscription(val authData: String, val baseUrl: String) : Request()
        data class AuthDataChanged(val authData: String, val baseUrl: String) : Request()
    }

    private val binding = DesignAccountBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    var loading: Boolean
        get() = binding.loading
        set(value) {
            binding.loading = value
        }

    fun requestSync() {
        binding.webView.evaluateJavascript(
            "(function(){ return localStorage.getItem('auth_data') || ''; })()"
        ) { result ->
            try {
                val authData = result?.trim('"')?.takeIf { it.isNotBlank() && it != "null" }
                requests.trySend(
                    Request.SyncSubscription(
                        authData = authData ?: "",
                        baseUrl = baseUrl
                    )
                )
            } catch (_: Exception) { /* Design already destroyed */ }
        }
    }

    init {
        binding.self = this
        binding.loading = true

        if (!showSyncButton) {
            binding.syncButton.visibility = View.GONE
        }

        binding.activityBarLayout.applyFrom(context)

        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = false
                displayZoomControls = false
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    binding.loading = false
                    if (showSyncButton) {
                        binding.syncButton.visibility = View.VISIBLE
                    }
                    // Persist auth_data whenever page finishes loading
                    view.evaluateJavascript(
                        "(function(){ return localStorage.getItem('auth_data') || ''; })()"
                    ) { result ->
                        try {
                            val authData = result?.trim('"')
                                ?.takeIf { it.isNotBlank() && it != "null" }
                            if (authData != null) {
                                requests.trySend(Request.AuthDataChanged(authData, baseUrl))
                            }
                        } catch (_: Exception) { /* Design already destroyed */ }
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    binding.loading = true
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    if (newProgress >= 100) {
                        binding.loading = false
                    }
                }
            }

            loadUrl(baseUrl.trimEnd('/') + initialPath)
        }
    }
}
