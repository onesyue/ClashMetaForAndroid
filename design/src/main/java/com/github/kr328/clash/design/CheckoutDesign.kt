package com.github.kr328.clash.design

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.github.kr328.clash.design.databinding.DesignCheckoutBinding
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root

class CheckoutDesign(context: Context) : Design<CheckoutDesign.Request>(context) {

    sealed class Request {
        data class Pay(val methodId: Int) : Request()
        data class VerifyCoupon(val code: String) : Request()
    }

    data class PaymentMethod(val id: Int, val name: String)

    private val binding = DesignCheckoutBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    private var selectedMethodId: Int = -1
    private val radioGroup = RadioGroup(context)
    private val viewIdToMethodId = mutableMapOf<Int, Int>()

    private val dp = context.resources.displayMetrics.density
    private var couponEdit: EditText? = null
    private var couponBtn: com.google.android.material.button.MaterialButton? = null
    private var discountRow: View? = null
    private var actualRow: View? = null
    private var originalPriceCents: Long = 0

    init {
        binding.activityBarLayout.applyFrom(context)

        binding.checkoutPayBtn.setOnClickListener {
            if (selectedMethodId >= 0) {
                requests.trySend(Request.Pay(selectedMethodId))
            }
        }
    }

    fun setSummary(planName: String, periodLabel: String, priceCents: Long) {
        originalPriceCents = priceCents
        val container = binding.checkoutSummaryContainer
        container.removeAllViews()

        listOf(
            context.getString(R.string.checkout_plan_label) to planName,
            context.getString(R.string.checkout_period_label) to periodLabel,
            context.getString(R.string.checkout_amount_label) to
                if (priceCents == 0L) context.getString(R.string.checkout_free)
                else "¥%.2f".format(priceCents / 100.0)
        ).forEach { (label, value) ->
            container.addView(createSummaryRow(label, value))
        }

        // Coupon input
        container.addView(createCouponInput())
    }

    fun showPaymentMethods(methods: List<PaymentMethod>) {
        binding.methodsLoading.visibility = View.GONE
        val container = binding.checkoutMethodsContainer
        container.removeAllViews()

        radioGroup.removeAllViews()
        viewIdToMethodId.clear()
        radioGroup.orientation = RadioGroup.VERTICAL
        radioGroup.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        if (methods.isEmpty() && originalPriceCents == 0L) {
            // Free order with no payment methods needed
            selectedMethodId = 0
            binding.checkoutPayBtn.isEnabled = true
        } else if (methods.isEmpty()) {
            // Non-free order but no methods available — keep button disabled
            binding.checkoutPayBtn.isEnabled = false
        } else {
            methods.forEach { method ->
                val viewId = View.generateViewId()
                viewIdToMethodId[viewId] = method.id
                val rb = RadioButton(context).apply {
                    id = viewId
                    text = method.name
                    textSize = 14f
                    setTextColor(0xFFF1F5F9.toInt())
                    buttonTintList = ColorStateList.valueOf(0xFF6E72FC.toInt())
                }
                radioGroup.addView(rb)
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                selectedMethodId = viewIdToMethodId[checkedId] ?: -1
                binding.checkoutPayBtn.isEnabled = selectedMethodId >= 0
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

    fun setCouponLoading(loading: Boolean) {
        couponEdit?.isEnabled = !loading
        couponBtn?.isEnabled = !loading
        couponBtn?.text = if (loading)
            context.getString(R.string.coupon_verifying)
        else
            context.getString(R.string.coupon_verify)
    }

    fun applyCoupon(discountCents: Long) {
        val container = binding.checkoutSummaryContainer

        // Remove old discount/actual rows
        discountRow?.let { container.removeView(it) }
        actualRow?.let { container.removeView(it) }

        val discountStr = "-¥%.2f".format(discountCents / 100.0)
        discountRow = createSummaryRow(
            context.getString(R.string.checkout_discount_label), discountStr,
            valueColor = 0xFF34D399.toInt()
        )
        container.addView(discountRow)

        val actualCents = (originalPriceCents - discountCents).coerceAtLeast(0)
        val actualStr = "¥%.2f".format(actualCents / 100.0)
        actualRow = createSummaryRow(
            context.getString(R.string.checkout_actual_label), actualStr,
            valueColor = 0xFFF1F5F9.toInt(), bold = true
        )
        container.addView(actualRow)
    }

    fun clearCoupon() {
        val container = binding.checkoutSummaryContainer
        discountRow?.let { container.removeView(it) }
        actualRow?.let { container.removeView(it) }
        discountRow = null
        actualRow = null
    }

    private fun createCouponInput(): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * dp).toInt() }
        }

        val edit = EditText(context).apply {
            hint = context.getString(R.string.coupon_hint)
            textSize = 13f
            setTextColor(0xFFF1F5F9.toInt())
            setHintTextColor(0xFF8494A7.toInt())
            inputType = InputType.TYPE_CLASS_TEXT
            background = null
            setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        couponEdit = edit
        row.addView(edit)

        val btn = com.google.android.material.button.MaterialButton(context).apply {
            text = context.getString(R.string.coupon_verify)
            textSize = 12f
            val btnPad = (12 * dp).toInt()
            setPadding(btnPad, 0, btnPad, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (34 * dp).toInt()
            ).apply { marginStart = (8 * dp).toInt() }
            setOnClickListener {
                val code = edit.text?.toString()?.trim() ?: ""
                if (code.isNotBlank()) {
                    requests.trySend(Request.VerifyCoupon(code))
                }
            }
        }
        couponBtn = btn
        row.addView(btn)

        return row
    }

    private fun createSummaryRow(
        label: String,
        value: String,
        valueColor: Int = 0xFFF1F5F9.toInt(),
        bold: Boolean = false
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (4 * dp).toInt() }
        }
        row.addView(TextView(context).apply {
            text = label
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(context).apply {
            text = value
            textSize = 13f
            setTextColor(valueColor)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        })
        return row
    }
}
