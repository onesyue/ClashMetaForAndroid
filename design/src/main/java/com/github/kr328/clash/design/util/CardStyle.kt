package com.github.kr328.clash.design.util

import android.content.Context
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView

/**
 * Create a MaterialCardView styled for the dark glass-effect theme.
 *
 * Background: #CC1E293B, Stroke: #26FFFFFF 1dp, Corner: 16dp, Elevation: 0
 */
fun Context.createGlassCard(bottomMarginDp: Int = 12): MaterialCardView {
    val dp = resources.displayMetrics.density
    return MaterialCardView(this).apply {
        radius = 16 * dp
        cardElevation = 0f
        setCardBackgroundColor(0xCC1E293B.toInt())
        strokeWidth = (1 * dp).toInt()
        strokeColor = 0x26FFFFFF
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { this.bottomMargin = (bottomMarginDp * dp).toInt() }
    }
}
