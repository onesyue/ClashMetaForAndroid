package com.github.kr328.clash

import com.github.kr328.clash.design.OrdersDesign
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.xboard.XBoardApi
import com.github.kr328.clash.xboard.XBoardSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class OrdersActivity : BaseActivity<OrdersDesign>() {

    override suspend fun main() {
        val design = OrdersDesign(this)
        setContentDesign(design)

        loadOrders(design)

        while (isActive) {
            select<Unit> {
                events.onReceive { }
                design.requests.onReceive { request ->
                    when (request) {
                        is OrdersDesign.Request.CancelOrder -> {
                            val authData = XBoardSession.getAuthData(this@OrdersActivity) ?: return@onReceive
                            val baseUrl = XBoardSession.getBaseUrl(this@OrdersActivity)
                            launch(Dispatchers.IO) {
                                try {
                                    XBoardApi.cancelOrder(baseUrl, authData, request.tradeNo)
                                    withContext(Dispatchers.Main) {
                                        design.showToast(getString(R.string.order_cancelled), ToastDuration.Short)
                                        loadOrders(design)
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        design.showToast(
                                            e.message ?: getString(R.string.xboard_request_failed),
                                            ToastDuration.Long
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadOrders(design: OrdersDesign) {
        design.showLoading()
        launch(Dispatchers.IO) {
            try {
                val authData = XBoardSession.getAuthData(this@OrdersActivity)
                    ?: return@launch
                val baseUrl = XBoardSession.getBaseUrl(this@OrdersActivity)
                val orders = XBoardApi.getOrders(baseUrl, authData)
                val items = orders.map {
                    OrdersDesign.Order(
                        tradeNo     = it.tradeNo,
                        planName    = it.planName,
                        period      = it.period,
                        totalAmount = it.totalAmount,
                        status      = it.status,
                        createdAt   = it.createdAt
                    )
                }
                withContext(Dispatchers.Main) { design.showOrders(items) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { design.showOrders(emptyList()) }
            }
        }
    }
}
