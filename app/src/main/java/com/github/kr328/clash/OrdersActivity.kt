package com.github.kr328.clash

import com.github.kr328.clash.design.OrdersDesign
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
