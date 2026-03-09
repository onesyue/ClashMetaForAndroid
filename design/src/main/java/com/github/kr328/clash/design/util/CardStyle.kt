package com.github.kr328.clash.design.util

import android.content.Context
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView

/**
 * Create a MaterialCardView styled with the Liquid Glass effect.
 *
 * Background: #801A2332, Stroke: #30FFFFFF 0.5dp, Corner: 16dp, Elevation: 0
 */
fun Context.createGlassCard(bottomMarginDp: Int = 12): MaterialCardView {
    val dp = resources.displayMetrics.density
    return MaterialCardView(this).apply {
        radius = 16 * dp
        cardElevation = 0f
        setCardBackgroundColor(0x801A2332.toInt())
        strokeWidth = (0.5f * dp).toInt().coerceAtLeast(1)
        strokeColor = 0x30FFFFFF
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { this.bottomMargin = (bottomMarginDp * dp).toInt() }
    }
}
