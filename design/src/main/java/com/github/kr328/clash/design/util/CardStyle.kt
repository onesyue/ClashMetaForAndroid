package com.github.kr328.clash.design.util

import android.content.Context
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.github.kr328.clash.design.R
import com.google.android.material.card.MaterialCardView

/**
 * Create a MaterialCardView styled with the Liquid Glass effect.
 * Automatically adapts to light/dark mode via color resources.
 */
fun Context.createGlassCard(bottomMarginDp: Int = 12): MaterialCardView {
    val dp = resources.displayMetrics.density
    return MaterialCardView(this).apply {
        radius = 16 * dp
        cardElevation = 0f
        setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_card_glass))
        strokeWidth = (0.5f * dp).toInt().coerceAtLeast(1)
        strokeColor = ContextCompat.getColor(context, R.color.color_glass_border)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { this.bottomMargin = (bottomMarginDp * dp).toInt() }
    }
}
