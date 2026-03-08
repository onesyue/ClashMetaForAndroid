package com.github.kr328.clash.design

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.design.databinding.DesignOrdersBinding
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.createGlassCard
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrdersDesign(context: Context) : Design<OrdersDesign.Request>(context) {

    sealed class Request {
        data class CancelOrder(val tradeNo: String) : Request()
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

    private val dp = context.resources.displayMetrics.density

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
        0    -> context.getString(R.string.order_status_pending) to 0xFFFBBF24.toInt()
        1    -> context.getString(R.string.order_status_processing) to 0xFF6366F1.toInt()
        2    -> context.getString(R.string.order_status_cancelled) to 0xFF64748B.toInt()
        3    -> context.getString(R.string.order_status_completed) to 0xFF10B981.toInt()
        4    -> context.getString(R.string.order_status_discounted) to 0xFF8B5CF6.toInt()
        else -> context.getString(R.string.order_status_unknown) to 0xFF64748B.toInt()
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

    private fun createOrderCard(order: Order): View {
        val card = context.createGlassCard()

        val inner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * dp).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // Header row: plan name + status badge
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val (statusText, statusColor) = statusLabel(order.status)
        headerRow.addView(TextView(context).apply {
            text = order.planName.ifBlank { context.getString(R.string.order_plan_unknown) }
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFFF1F5F9.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        headerRow.addView(TextView(context).apply {
            text = statusText
            textSize = 12f
            setTextColor(statusColor)
            setTypeface(typeface, Typeface.BOLD)
        })
        inner.addView(headerRow)

        // Period
        inner.addView(TextView(context).apply {
            text = periodLabel(order.period)
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * dp).toInt() }
        })

        // Coupon code (if applied)
        if (!order.couponCode.isNullOrBlank()) {
            inner.addView(TextView(context).apply {
                text = "🎫 ${order.couponCode}"
                textSize = 12f
                setTextColor(0xFF8B5CF6.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (2 * dp).toInt() }
            })
        }

        // Divider
        inner.addView(View(context).apply {
            setBackgroundColor(0x26FFFFFF)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
            ).apply { topMargin = (10 * dp).toInt(); bottomMargin = (10 * dp).toInt() }
        })

        // Discount / surplus info
        if (order.discountAmount > 0) {
            inner.addView(createInfoRow(
                context.getString(R.string.order_discount_format, "%.2f".format(order.discountAmount / 100.0)),
                0xFF10B981.toInt()
            ))
        }
        if (order.surplusAmount > 0) {
            inner.addView(createInfoRow(
                context.getString(R.string.order_surplus_format, "%.2f".format(order.surplusAmount / 100.0)),
                0xFF6366F1.toInt()
            ))
        }

        // Bottom row: date + amount
        val bottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(order.createdAt * 1000))
        bottomRow.addView(TextView(context).apply {
            text = dateStr
            textSize = 12f
            setTextColor(0xFF64748B.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        bottomRow.addView(TextView(context).apply {
            text = "¥%.2f".format(order.totalAmount / 100.0)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFFF1F5F9.toInt())
        })
        inner.addView(bottomRow)

        // Cancel button for pending orders
        if (order.status == 0) {
            inner.addView(com.google.android.material.button.MaterialButton(context).apply {
                text = context.getString(R.string.order_cancel_btn)
                textSize = 13f
                setTextColor(0xFFEF4444.toInt())
                strokeColor = ColorStateList.valueOf(0xFFEF4444.toInt())
                backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                val btnPad = (12 * dp).toInt()
                setPadding(btnPad, 0, btnPad, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (40 * dp).toInt()
                ).apply { topMargin = (8 * dp).toInt() }
                setOnClickListener {
                    AlertDialog.Builder(context)
                        .setTitle(R.string.order_cancel_confirm_title)
                        .setMessage(R.string.order_cancel_confirm_message)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            requests.trySend(Request.CancelOrder(order.tradeNo))
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            })
        }

        card.addView(inner)
        return card
    }

    private fun createInfoRow(text: String, color: Int): View {
        return TextView(context).apply {
            this.text = text
            textSize = 12f
            setTextColor(color)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (4 * dp).toInt() }
        }
    }
}
