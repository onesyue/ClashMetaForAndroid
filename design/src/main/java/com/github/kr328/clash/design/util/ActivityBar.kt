package com.github.kr328.clash.design.util

import android.app.Activity
import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.view.ActivityBarLayout

fun ActivityBarLayout.applyFrom(context: Context) {
    if (context is Activity) {
        findViewById<ImageView>(R.id.activity_bar_close_view)?.apply {
            setOnClickListener {
                context.onBackPressed()
            }
        }
        findViewById<TextView>(R.id.activity_bar_title_view)?.apply {
            text = context.title
        }
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        v.setPadding(v.paddingLeft, top, v.paddingRight, v.paddingBottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}