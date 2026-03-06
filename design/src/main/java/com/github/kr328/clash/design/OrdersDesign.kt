package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.github.kr328.clash.design.databinding.DesignOrdersBinding
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrdersDesign(context: Context) : Design<Unit>(context) {

    data class Order(
        val tradeNo: String,
        val planName: String,
        val period: String,
        val totalAmount: Long,
        val status: Int,
        val createdAt: Long
    )

    private val binding = DesignOrdersBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    init {
        binding.activityBarLayout.applyFrom(context)
    }

    fun showLoading() {
        binding.ordersLoading.visibility = View.VISIBLE
        binding.ordersEmpty.visibility = View.GONE
        binding.ordersScroll.visibility = View.GONE
    }

    fun showOrders(orders: List<Order>) {
        binding.ordersLoading.visibility = View.GONE

        if (orders.isEmpty()) {
            binding.ordersEmpty.visibility = View.VISIBLE
            binding.ordersScroll.visibility = View.GONE
            return
        }

        binding.ordersContainer.removeAllViews()
        orders.forEach { order ->
            binding.ordersContainer.addView(createOrderCard(order))
        }

        binding.ordersEmpty.visibility = View.GONE
        binding.ordersScroll.visibility = View.VISIBLE
    }

    private fun statusLabel(status: Int): Pair<String, Int> = when (status) {
        0    -> context.getString(R.string.order_status_pending) to 0xFFF57F17.toInt()
        1    -> context.getString(R.string.order_status_processing) to 0xFF1565C0.toInt()
        2    -> context.getString(R.string.order_status_cancelled) to 0xFF757575.toInt()
        3    -> context.getString(R.string.order_status_completed) to 0xFF2E7D32.toInt()
        4    -> context.getString(R.string.order_status_discounted) to 0xFF6A1B9A.toInt()
        else -> context.getString(R.string.order_status_unknown) to 0xFF757575.toInt()
    }

    private fun periodLabel(period: String): String = when (period) {
        "monthly"    -> context.getString(R.string.period_monthly)
        "quarterly"  -> context.getString(R.string.period_quarterly)
        "half_yearly"-> context.getString(R.string.period_half_yearly)
        "yearly"     -> context.getString(R.string.period_yearly)
        "onetime"    -> context.getString(R.string.period_onetime)
        else         -> period
    }

    private fun createOrderCard(order: Order): View {
        val dp = context.resources.displayMetrics.density
        val card = MaterialCardView(context).apply {
            radius = 12 * dp
            cardElevation = 2 * dp
            strokeWidth = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * dp).toInt() }
        }

        val inner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * dp).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // Header row: plan name + status badge
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val (statusText, statusColor) = statusLabel(order.status)
        headerRow.addView(android.widget.TextView(context).apply {
            text = order.planName.ifBlank { context.getString(R.string.order_plan_unknown) }
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF1A237E.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        headerRow.addView(android.widget.TextView(context).apply {
            text = statusText
            textSize = 12f
            setTextColor(statusColor)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        inner.addView(headerRow)

        // Period
        inner.addView(android.widget.TextView(context).apply {
            text = periodLabel(order.period)
            textSize = 13f
            setTextColor(0xFF616161.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * dp).toInt() }
        })

        // Divider
        inner.addView(View(context).apply {
            setBackgroundColor(0xFFEEEEEE.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { topMargin = (10 * dp).toInt(); bottomMargin = (10 * dp).toInt() }
        })

        // Bottom row: date + amount
        val bottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(order.createdAt * 1000))
        bottomRow.addView(android.widget.TextView(context).apply {
            text = dateStr
            textSize = 12f
            setTextColor(0xFF9E9E9E.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        bottomRow.addView(android.widget.TextView(context).apply {
            text = "¥%.2f".format(order.totalAmount / 100.0)
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF1A237E.toInt())
        })
        inner.addView(bottomRow)

        card.addView(inner)
        return card
    }
}
