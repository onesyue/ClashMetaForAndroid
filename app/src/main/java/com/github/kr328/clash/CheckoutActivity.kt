package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.CheckoutDesign
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.xboard.XBoardApi
import com.github.kr328.clash.xboard.XBoardSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class CheckoutActivity : BaseActivity<CheckoutDesign>() {

    companion object {
        const val EXTRA_PLAN_ID       = "plan_id"
        const val EXTRA_PLAN_NAME     = "plan_name"
        const val EXTRA_PERIOD        = "period"
        const val EXTRA_PERIOD_LABEL  = "period_label"
        const val EXTRA_PRICE_CENTS   = "price_cents"
    }

    override suspend fun main() {
        val planId      = intent.getIntExtra(EXTRA_PLAN_ID, 0)
        val planName    = intent.getStringExtra(EXTRA_PLAN_NAME) ?: ""
        val period      = intent.getStringExtra(EXTRA_PERIOD) ?: ""
        val periodLabel = intent.getStringExtra(EXTRA_PERIOD_LABEL) ?: ""
        val priceCents  = intent.getLongExtra(EXTRA_PRICE_CENTS, 0)

        val design = CheckoutDesign(this)
        setContentDesign(design)

        design.setSummary(planName, periodLabel, priceCents)

        // Fetch payment methods in background
        launch(Dispatchers.IO) {
            try {
                val authData = XBoardSession.getAuthData(this@CheckoutActivity) ?: return@launch
                val baseUrl  = XBoardSession.getBaseUrl(this@CheckoutActivity)
                val methods  = XBoardApi.getPaymentMethods(baseUrl, authData)
                withContext(Dispatchers.Main) {
                    design.showPaymentMethods(
                        methods.map { CheckoutDesign.PaymentMethod(it.id, it.name) }
                    )
                }
            } catch (e: XBoardApi.AuthExpiredException) {
                withContext(Dispatchers.Main) { handleAuthExpired() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    design.showToast(
                        e.message ?: getString(R.string.xboard_request_failed),
                        ToastDuration.Long
                    )
                }
            }
        }

        while (isActive) {
            select<Unit> {
                events.onReceive { }
                design.requests.onReceive { request ->
                    when (request) {
                        is CheckoutDesign.Request.Pay ->
                            processPay(design, planId, period, request.methodId)
                    }
                }
            }
        }
    }

    private suspend fun processPay(
        design: CheckoutDesign,
        planId: Int,
        period: String,
        methodId: Int
    ) {
        design.setLoading(true)
        try {
            val authData = XBoardSession.getAuthData(this) ?: run {
                design.showToast(getString(R.string.not_logged_in), ToastDuration.Long)
                return
            }
            val baseUrl = XBoardSession.getBaseUrl(this)

            val tradeNo = XBoardApi.createOrder(baseUrl, authData, planId, period)
            val result  = XBoardApi.checkoutOrder(baseUrl, authData, tradeNo, methodId)

            when (result.type) {
                -1 -> {
                    // Free / balance paid — success
                    design.showToast(getString(R.string.checkout_success), ToastDuration.Short)
                    setResult(RESULT_OK)
                    finish()
                }
                0 -> {
                    // URL redirect
                    val uri = Uri.parse(result.data)
                    val scheme = uri.scheme ?: ""
                    if (scheme in listOf("alipay", "alipays", "weixin", "wxpay")) {
                        // Open payment app natively
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } else {
                        // Open payment page in in-app WebView
                        startActivity(
                            AccountActivity::class.intent.apply {
                                putExtra(AccountActivity.EXTRA_FULL_URL, result.data)
                            }
                        )
                    }
                }
                1 -> {
                    // HTML — open in WebView (pass as data URI or temp URL)
                    startActivity(
                        AccountActivity::class.intent.apply {
                            putExtra(AccountActivity.EXTRA_FULL_URL, result.data)
                        }
                    )
                }
            }
        } catch (e: XBoardApi.AuthExpiredException) {
            handleAuthExpired()
        } catch (e: Exception) {
            design.showToast(
                e.message ?: getString(R.string.xboard_request_failed),
                ToastDuration.Long
            )
        } finally {
            design.setLoading(false)
        }
    }

    private fun handleAuthExpired() {
        XBoardSession.clear(this)
        startActivity(
            XBoardLoginActivity::class.intent.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}
