package com.github.kr328.clash.design.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.OrdersDesign
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private val onCancel: (String) -> Unit
) : ListAdapter<OrderAdapter.OrderItem, OrderAdapter.ViewHolder>(DIFF) {

    data class OrderItem(
        val tradeNo: String,
        val planName: String,
        val period: String,
        val totalAmount: Long,
        val discountAmount: Long,
        val surplusAmount: Long,
        val couponCode: String?,
        val status: Int,
        val createdAt: Long,
        val statusText: String,
        val statusColor: Int,
        val periodText: String
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val planName: TextView = view.findViewById(R.id.order_plan_name)
        val status: TextView = view.findViewById(R.id.order_status)
        val period: TextView = view.findViewById(R.id.order_period)
        val coupon: TextView = view.findViewById(R.id.order_coupon)
        val discount: TextView = view.findViewById(R.id.order_discount)
        val surplus: TextView = view.findViewById(R.id.order_surplus)
        val date: TextView = view.findViewById(R.id.order_date)
        val amount: TextView = view.findViewById(R.id.order_amount)
        val cancelBtn: MaterialButton = view.findViewById(R.id.order_cancel_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        holder.planName.text = item.planName.ifBlank {
            context.getString(R.string.order_plan_unknown)
        }
        holder.status.text = item.statusText
        holder.status.setTextColor(item.statusColor)
        holder.period.text = item.periodText

        // Coupon
        if (!item.couponCode.isNullOrBlank()) {
            holder.coupon.text = "\uD83C\uDFAB ${item.couponCode}"
            holder.coupon.visibility = View.VISIBLE
        } else {
            holder.coupon.visibility = View.GONE
        }

        // Discount
        if (item.discountAmount > 0) {
            holder.discount.text = context.getString(
                R.string.order_discount_format,
                "%.2f".format(item.discountAmount / 100.0)
            )
            holder.discount.visibility = View.VISIBLE
        } else {
            holder.discount.visibility = View.GONE
        }

        // Surplus
        if (item.surplusAmount > 0) {
            holder.surplus.text = context.getString(
                R.string.order_surplus_format,
                "%.2f".format(item.surplusAmount / 100.0)
            )
            holder.surplus.visibility = View.VISIBLE
        } else {
            holder.surplus.visibility = View.GONE
        }

        // Date and amount
        holder.date.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(item.createdAt * 1000))
        holder.amount.text = "¥%.2f".format(item.totalAmount / 100.0)

        // Cancel button
        if (item.status == 0) {
            holder.cancelBtn.visibility = View.VISIBLE
            holder.cancelBtn.setOnClickListener {
                onCancel(item.tradeNo)
            }
        } else {
            holder.cancelBtn.visibility = View.GONE
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OrderItem>() {
            override fun areItemsTheSame(a: OrderItem, b: OrderItem) = a.tradeNo == b.tradeNo
            override fun areContentsTheSame(a: OrderItem, b: OrderItem) = a == b
        }
    }
}
