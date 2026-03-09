package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.design.adapter.OrderAdapter
import com.github.kr328.clash.design.databinding.DesignOrdersBinding
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root

class OrdersDesign(context: Context) : Design<OrdersDesign.Request>(context) {

    sealed class Request {
        data class CancelOrder(val tradeNo: String) : Request()
        object Refresh : Request()
        object GoStore : Request()
    }

    data class Order(
        val tradeNo: String,
        val planName: String,
        val period: String,
        val totalAmount: Long,
        val discountAmount: Long,
        val surplusAmount: Long,
        val couponCode: String?,
        val status: Int,
        val createdAt: Long
    )

    private val binding = DesignOrdersBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    private val adapter = OrderAdapter { tradeNo ->
        AlertDialog.Builder(context)
            .setTitle(R.string.order_cancel_confirm_title)
            .setMessage(R.string.order_cancel_confirm_message)
            .setPositiveButton(R.string.ok) { _, _ ->
                requests.trySend(Request.CancelOrder(tradeNo))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    init {
        binding.activityBarLayout.applyFrom(context)
        binding.ordersGoStoreBtn.setOnClickListener {
            requests.trySend(Request.GoStore)
        }
        binding.ordersSwipeRefresh.setOnRefreshListener {
            requests.trySend(Request.Refresh)
        }
        binding.ordersSwipeRefresh.setColorSchemeColors(0xFF6E72FC.toInt())
        binding.ordersSwipeRefresh.setProgressBackgroundColorSchemeColor(0xFF1A2332.toInt())

        binding.ordersRecycler.layoutManager = LinearLayoutManager(context)
        binding.ordersRecycler.adapter = adapter
    }

    fun showLoading() {
        binding.ordersLoading.visibility = View.VISIBLE
        binding.ordersEmpty.visibility = View.GONE
        binding.ordersSwipeRefresh.visibility = View.GONE
    }

    fun showOrders(orders: List<Order>) {
        binding.ordersLoading.visibility = View.GONE
        binding.ordersSwipeRefresh.isRefreshing = false

        if (orders.isEmpty()) {
            binding.ordersEmpty.visibility = View.VISIBLE
            binding.ordersSwipeRefresh.visibility = View.GONE
            return
        }

        adapter.submitList(orders.map { order ->
            val (statusText, statusColor) = statusLabel(order.status)
            OrderAdapter.OrderItem(
                tradeNo = order.tradeNo,
                planName = order.planName,
                period = order.period,
                totalAmount = order.totalAmount,
                discountAmount = order.discountAmount,
                surplusAmount = order.surplusAmount,
                couponCode = order.couponCode,
                status = order.status,
                createdAt = order.createdAt,
                statusText = statusText,
                statusColor = statusColor,
                periodText = periodLabel(order.period)
            )
        })

        binding.ordersEmpty.visibility = View.GONE
        binding.ordersSwipeRefresh.visibility = View.VISIBLE
    }

    private fun statusLabel(status: Int): Pair<String, Int> = when (status) {
        0    -> context.getString(R.string.order_status_pending) to 0xFFFBBF24.toInt()
        1    -> context.getString(R.string.order_status_processing) to 0xFF6E72FC.toInt()
        2    -> context.getString(R.string.order_status_cancelled) to 0xFF8494A7.toInt()
        3    -> context.getString(R.string.order_status_completed) to 0xFF34D399.toInt()
        4    -> context.getString(R.string.order_status_discounted) to 0xFF9B8AFB.toInt()
        else -> context.getString(R.string.order_status_unknown) to 0xFF8494A7.toInt()
    }

    private fun periodLabel(period: String): String = when (period) {
        "monthly"         -> context.getString(R.string.period_monthly)
        "quarterly"       -> context.getString(R.string.period_quarterly)
        "half_yearly"     -> context.getString(R.string.period_half_yearly)
        "yearly"          -> context.getString(R.string.period_yearly)
        "onetime"         -> context.getString(R.string.period_onetime)
        "month_price"     -> context.getString(R.string.period_monthly)
        "quarter_price"   -> context.getString(R.string.period_quarterly)
        "half_year_price" -> context.getString(R.string.period_half_yearly)
        "year_price"      -> context.getString(R.string.period_yearly)
        "onetime_price"   -> context.getString(R.string.period_onetime)
        else              -> period
    }
}
