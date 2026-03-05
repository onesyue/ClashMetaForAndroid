package com.github.kr328.clash.design

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.kr328.clash.design.databinding.DesignAccountBinding
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root

class AccountDesign(context: Context) : Design<AccountDesign.Request>(context) {

    sealed class Request {
        data class SyncSubscription(val authData: String, val baseUrl: String) : Request()
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
            val authData = result?.trim('"')?.takeIf { it.isNotBlank() && it != "null" }
            if (authData != null) {
                requests.trySend(
                    Request.SyncSubscription(
                        authData = authData,
                        baseUrl = context.getString(R.string.xboard_default_url)
                    )
                )
            } else {
                requests.trySend(
                    Request.SyncSubscription(
                        authData = "",
                        baseUrl = context.getString(R.string.xboard_default_url)
                    )
                )
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    init {
        binding.self = this
        binding.loading = true

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
                    // Stay within the WebView for all navigation
                    return false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    binding.loading = false
                    // Check if user is logged in (auth_data exists in localStorage)
                    view.evaluateJavascript(
                        "(function(){ return localStorage.getItem('auth_data') || ''; })()"
                    ) { result ->
                        val hasAuth = result?.trim('"')?.isNotBlank() == true &&
                            result.trim('"') != "null"
                        binding.syncButton.visibility = if (hasAuth) View.VISIBLE else View.VISIBLE
                        // Always show the FAB — let server response tell if it works
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

            loadUrl(context.getString(R.string.xboard_default_url))
        }
    }
}
