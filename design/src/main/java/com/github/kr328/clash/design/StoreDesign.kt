package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.github.kr328.clash.design.databinding.DesignStoreBinding
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.google.android.material.card.MaterialCardView

class StoreDesign(context: Context) : Design<StoreDesign.Request>(context) {

    sealed class Request {
        data class BuyPlan(val planId: Int, val planName: String) : Request()
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
        val dp = context.resources.displayMetrics.density
        val card = MaterialCardView(context).apply {
            radius = 12 * dp
            cardElevation = 2 * dp
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

        // Plan name
        inner.addView(android.widget.TextView(context).apply {
            text = plan.name
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF1A237E.toInt())
        })

        // Traffic
        val trafficText = if (plan.transferGb == 0L)
            context.getString(R.string.unlimited_traffic)
        else
            context.getString(R.string.traffic_gb_format, plan.transferGb)
        inner.addView(android.widget.TextView(context).apply {
            text = trafficText
            textSize = 14f
            setTextColor(0xFF5C7CAB.toInt())
            val mt = (4 * dp).toInt()
            (layoutParams as? LinearLayout.LayoutParams)?.topMargin = mt
                ?: run { layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = mt } }
        })

        // Description
        if (plan.content.isNotBlank()) {
            inner.addView(android.widget.TextView(context).apply {
                text = plan.content
                textSize = 13f
                setTextColor(0xFF757575.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (8 * dp).toInt() }
            })
        }

        // Divider
        inner.addView(View(context).apply {
            setBackgroundColor(0xFFEEEEEE.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
            ).apply { topMargin = (12 * dp).toInt(); bottomMargin = (8 * dp).toInt() }
        })

        // Prices
        listOf(
            plan.monthPrice    to R.string.price_monthly,
            plan.quarterPrice  to R.string.price_quarterly,
            plan.halfYearPrice to R.string.price_half_year,
            plan.yearPrice     to R.string.price_yearly,
            plan.onetimePrice  to R.string.price_onetime
        ).forEach { (cents, strRes) ->
            if (cents != null && cents > 0) {
                inner.addView(android.widget.TextView(context).apply {
                    text = context.getString(strRes, "%.2f".format(cents / 100.0))
                    textSize = 14f
                    setTextColor(0xFF212121.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (4 * dp).toInt() }
                })
            }
        }

        // Buy button
        inner.addView(com.google.android.material.button.MaterialButton(context).apply {
            text = context.getString(R.string.buy_now)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * dp).toInt() }
            setOnClickListener {
                requests.trySend(Request.BuyPlan(plan.id, plan.name))
            }
        })

        card.addView(inner)
        return card
    }
}
