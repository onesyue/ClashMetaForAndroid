package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.github.kr328.clash.design.databinding.DesignCheckoutBinding
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root

class CheckoutDesign(context: Context) : Design<CheckoutDesign.Request>(context) {

    sealed class Request {
        data class Pay(val methodId: Int) : Request()
    }

    data class PaymentMethod(val id: Int, val name: String)

    private val binding = DesignCheckoutBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    private var selectedMethodId: Int = -1
    private val radioGroup = RadioGroup(context)

    init {
        binding.activityBarLayout.applyFrom(context)

        binding.checkoutPayBtn.setOnClickListener {
            if (selectedMethodId >= 0) {
                requests.trySend(Request.Pay(selectedMethodId))
            }
        }
    }

    fun setSummary(planName: String, periodLabel: String, priceCents: Long) {
        val dp = context.resources.displayMetrics.density
        val container = binding.checkoutSummaryContainer
        container.removeAllViews()

        listOf(
            context.getString(R.string.checkout_plan_label) to planName,
            context.getString(R.string.checkout_period_label) to periodLabel,
            context.getString(R.string.checkout_amount_label) to
                if (priceCents == 0L) context.getString(R.string.checkout_free)
                else "¥%.2f".format(priceCents / 100.0)
        ).forEach { (label, value) ->
            container.addView(createSummaryRow(label, value, dp))
        }
    }

    fun showPaymentMethods(methods: List<PaymentMethod>) {
        binding.methodsLoading.visibility = View.GONE
        val container = binding.checkoutMethodsContainer
        container.removeAllViews()

        radioGroup.removeAllViews()
        radioGroup.orientation = RadioGroup.VERTICAL
        radioGroup.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        if (methods.isEmpty()) {
            // If no external methods, allow free checkout
            selectedMethodId = 0
            binding.checkoutPayBtn.isEnabled = true
        } else {
            methods.forEach { method ->
                val rb = RadioButton(context).apply {
                    id = method.id
                    text = method.name
                    textSize = 14f
                }
                radioGroup.addView(rb)
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                selectedMethodId = checkedId
                binding.checkoutPayBtn.isEnabled = true
            }

            container.addView(radioGroup)
        }

        container.visibility = View.VISIBLE
    }

    fun setLoading(loading: Boolean) {
        binding.checkoutPayBtn.isEnabled = !loading && selectedMethodId >= 0
        binding.checkoutPayBtn.text = if (loading)
            context.getString(R.string.checkout_creating_order)
        else
            context.getString(R.string.checkout_confirm_pay)
    }

    private fun createSummaryRow(label: String, value: String, dp: Float): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (4 * dp).toInt() }
        }
        row.addView(android.widget.TextView(context).apply {
            text = label
            textSize = 13f
            setTextColor(0xFF757575.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(android.widget.TextView(context).apply {
            text = value
            textSize = 13f
            setTextColor(0xFF212121.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        return row
    }
}
