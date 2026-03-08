package com.github.kr328.clash.design

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kr328.clash.design.databinding.DesignStoreBinding
import com.github.kr328.clash.design.util.MarkdownRenderer
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.createGlassCard
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root

class StoreDesign(context: Context) : Design<StoreDesign.Request>(context) {

    sealed class Request {
        data class BuyPlan(
            val planId: Int,
            val planName: String,
            val period: String,
            val periodLabel: String,
            val priceCents: Long
        ) : Request()
        object Retry : Request()
    }

    data class Plan(
        val id: Int,
        val name: String,
        val content: String,
        val transferGb: Long,
        val monthPrice: Long?,
        val quarterPrice: Long?,
        val halfYearPrice: Long?,
        val yearPrice: Long?,
        val onetimePrice: Long?
    )

    private val binding = DesignStoreBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    private val dp = context.resources.displayMetrics.density

    init {
        binding.activityBarLayout.applyFrom(context)
        binding.storeRetryBtn.setOnClickListener {
            requests.trySend(Request.Retry)
        }
    }

    fun showLoading() {
        binding.storeLoading.visibility = View.VISIBLE
        binding.storeErrorView.visibility = View.GONE
        binding.storeEmptyView.visibility = View.GONE
        binding.storeScroll.visibility = View.GONE
    }

    fun showPlans(plans: List<Plan>) {
        binding.storeLoading.visibility = View.GONE
        binding.storeErrorView.visibility = View.GONE

        if (plans.isEmpty()) {
            binding.storeEmptyView.visibility = View.VISIBLE
            binding.storeScroll.visibility = View.GONE
            return
        }

        binding.storePlansContainer.removeAllViews()
        plans.forEach { plan ->
            binding.storePlansContainer.addView(createPlanCard(plan))
        }

        binding.storeEmptyView.visibility = View.GONE
        binding.storeScroll.visibility = View.VISIBLE
    }

    fun showError() {
        binding.storeLoading.visibility = View.GONE
        binding.storeErrorView.visibility = View.VISIBLE
        binding.storeEmptyView.visibility = View.GONE
        binding.storeScroll.visibility = View.GONE
    }

    private fun createPlanCard(plan: Plan): View {
        val card = context.createGlassCard()

        val inner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * dp).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // Plan name
        inner.addView(TextView(context).apply {
            text = plan.name
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFFF1F5F9.toInt())
        })

        // Traffic
        val trafficText = if (plan.transferGb == 0L)
            context.getString(R.string.unlimited_traffic)
        else
            context.getString(R.string.traffic_gb_format, plan.transferGb)
        inner.addView(TextView(context).apply {
            text = trafficText
            textSize = 14f
            setTextColor(0xFF94A3B8.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * dp).toInt() }
        })

        // Description (Markdown)
        if (plan.content.isNotBlank()) {
            inner.addView(TextView(context).apply {
                text = MarkdownRenderer.render(plan.content)
                textSize = 13f
                setTextColor(0xFFCBD5E1.toInt())
                setLineSpacing(0f, 1.3f)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (8 * dp).toInt() }
            })
        }

        // Divider
        inner.addView(View(context).apply {
            setBackgroundColor(0x26FFFFFF)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
            ).apply { topMargin = (12 * dp).toInt(); bottomMargin = (8 * dp).toInt() }
        })

        // Period rows
        listOf(
            Triple("month_price",     context.getString(R.string.period_monthly),     plan.monthPrice),
            Triple("quarter_price",   context.getString(R.string.period_quarterly),   plan.quarterPrice),
            Triple("half_year_price", context.getString(R.string.period_half_yearly), plan.halfYearPrice),
            Triple("year_price",      context.getString(R.string.period_yearly),      plan.yearPrice),
            Triple("onetime_price",   context.getString(R.string.period_onetime),     plan.onetimePrice)
        ).forEach { (period, label, cents) ->
            if (cents != null && cents > 0) {
                inner.addView(createPeriodRow(plan, period, label, cents))
            }
        }

        card.addView(inner)
        return card
    }

    private fun createPeriodRow(
        plan: Plan,
        period: String,
        label: String,
        priceCents: Long
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (6 * dp).toInt() }
        }

        // Period label
        row.addView(TextView(context).apply {
            text = label
            textSize = 14f
            setTextColor(0xFF94A3B8.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        // Price
        row.addView(TextView(context).apply {
            text = "¥%.2f".format(priceCents / 100.0)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFFF1F5F9.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (12 * dp).toInt() }
        })

        // Buy button
        row.addView(com.google.android.material.button.MaterialButton(context).apply {
            text = context.getString(R.string.buy_now)
            textSize = 12f
            val btnPad = (10 * dp).toInt()
            setPadding(btnPad, 0, btnPad, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (32 * dp).toInt()
            )
            setOnClickListener {
                requests.trySend(
                    Request.BuyPlan(plan.id, plan.name, period, label, priceCents)
                )
            }
        })

        return row
    }
}
