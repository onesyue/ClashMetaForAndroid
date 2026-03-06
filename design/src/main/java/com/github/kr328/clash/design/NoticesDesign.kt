package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.github.kr328.clash.design.databinding.DesignNoticesBinding
import com.github.kr328.clash.design.util.MarkdownRenderer
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoticesDesign(context: Context) : Design<Unit>(context) {

    data class Notice(
        val id: Int,
        val title: String,
        val content: String,
        val createdAt: Long
    )

    private val binding = DesignNoticesBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    init {
        binding.activityBarLayout.applyFrom(context)
    }

    fun showLoading() {
        binding.noticesLoading.visibility = View.VISIBLE
        binding.noticesEmpty.visibility = View.GONE
        binding.noticesScroll.visibility = View.GONE
    }

    fun showNotices(notices: List<Notice>) {
        binding.noticesLoading.visibility = View.GONE

        if (notices.isEmpty()) {
            binding.noticesEmpty.visibility = View.VISIBLE
            binding.noticesScroll.visibility = View.GONE
            return
        }

        binding.noticesContainer.removeAllViews()
        notices.forEach { notice ->
            binding.noticesContainer.addView(createNoticeCard(notice))
        }

        binding.noticesEmpty.visibility = View.GONE
        binding.noticesScroll.visibility = View.VISIBLE
    }

    private fun createNoticeCard(notice: Notice): View {
        val dp = context.resources.displayMetrics.density
        val card = MaterialCardView(context).apply {
            radius = 12 * dp
            cardElevation = 2 * dp
            strokeWidth = 0
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

        // Title
        inner.addView(android.widget.TextView(context).apply {
            text = notice.title
            textSize = 16f
            android.graphics.Typeface.DEFAULT_BOLD.also { setTypeface(it) }
            setTextColor(0xFF1A237E.toInt())
        })

        // Date
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(notice.createdAt * 1000))
        inner.addView(android.widget.TextView(context).apply {
            text = dateStr
            textSize = 12f
            setTextColor(0xFF9E9E9E.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * dp).toInt() }
        })

        // Divider
        inner.addView(View(context).apply {
            setBackgroundColor(0xFFEEEEEE.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { topMargin = (10 * dp).toInt(); bottomMargin = (10 * dp).toInt() }
        })

        // Content (Markdown)
        if (notice.content.isNotBlank()) {
            inner.addView(android.widget.TextView(context).apply {
                text = MarkdownRenderer.render(notice.content)
                textSize = 14f
                setTextColor(0xFF424242.toInt())
                setLineSpacing(0f, 1.5f)
            })
        }

        card.addView(inner)
        return card
    }
}
