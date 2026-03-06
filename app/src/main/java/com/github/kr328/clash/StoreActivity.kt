package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.StoreDesign
import com.github.kr328.clash.remote.RemoteConfig
import com.github.kr328.clash.xboard.XBoardApi
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
                                AccountActivity::class.intent.apply {
                                    putExtra(AccountActivity.EXTRA_PATH, "/#/buy")
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
                val baseUrl = RemoteConfig.getXboardUrl(this@StoreActivity)
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
