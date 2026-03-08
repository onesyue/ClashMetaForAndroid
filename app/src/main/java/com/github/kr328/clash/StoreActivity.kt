package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.StoreDesign
import com.github.kr328.clash.xboard.XBoardApi
import com.github.kr328.clash.xboard.XBoardSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class StoreActivity : BaseActivity<StoreDesign>() {

    override suspend fun main() {
        val design = StoreDesign(this)
        setContentDesign(design)

        loadPlans(design)

        while (isActive) {
            select<Unit> {
                events.onReceive { }
                design.requests.onReceive { request ->
                    when (request) {
                        is StoreDesign.Request.BuyPlan -> {
                            startActivity(
                                CheckoutActivity::class.intent.apply {
                                    putExtra(CheckoutActivity.EXTRA_PLAN_ID,      request.planId)
                                    putExtra(CheckoutActivity.EXTRA_PLAN_NAME,    request.planName)
                                    putExtra(CheckoutActivity.EXTRA_PERIOD,       request.period)
                                    putExtra(CheckoutActivity.EXTRA_PERIOD_LABEL, request.periodLabel)
                                    putExtra(CheckoutActivity.EXTRA_PRICE_CENTS,  request.priceCents)
                                }
                            )
                        }
                        is StoreDesign.Request.Retry -> {
                            loadPlans(design)
                        }
                    }
                }
            }
        }
    }

    private fun loadPlans(design: StoreDesign) {
        design.showLoading()
        launch(Dispatchers.IO) {
            try {
                val baseUrl = XBoardSession.getBaseUrl(this@StoreActivity)
                val apiPlans = XBoardApi.getPlans(baseUrl)
                val plans = apiPlans.map { p ->
                    StoreDesign.Plan(
                        id            = p.id,
                        name          = p.name,
                        content       = p.content,
                        transferGb    = p.transferGb,
                        monthPrice    = p.monthPrice,
                        quarterPrice  = p.quarterPrice,
                        halfYearPrice = p.halfYearPrice,
                        yearPrice     = p.yearPrice,
                        onetimePrice  = p.onetimePrice
                    )
                }
                withContext(Dispatchers.Main) { design.showPlans(plans) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { design.showError() }
            }
        }
    }
}
