package com.github.kr328.clash.design.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.github.kr328.clash.design.R

class TrafficRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val dp = resources.displayMetrics.density
    private val strokeW = 8f * dp

    var percent: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f)
            invalidate()
        }

    var centerText: String = "0%"
        set(value) {
            field = value
            invalidate()
        }

    var subText: String = ""
        set(value) {
            field = value
            invalidate()
        }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeW
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.color_divider)
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeW
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        color = ContextCompat.getColor(context, R.color.color_text_primary)
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.color_text_tertiary)
    }

    private val colorGood = ContextCompat.getColor(context, R.color.color_status_good)
    private val colorWarn = ContextCompat.getColor(context, R.color.color_status_warn)
    private val colorBad = ContextCompat.getColor(context, R.color.color_status_bad)

    private val rectF = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) - strokeW
        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Background ring
        canvas.drawArc(rectF, -90f, 360f, false, bgPaint)

        // Progress arc
        val sweepAngle = percent / 100f * 360f
        arcPaint.color = when {
            percent < 80f -> colorGood
            percent < 95f -> colorWarn
            else -> colorBad
        }
        // Gradient shader along the arc
        arcPaint.shader = SweepGradient(
            cx, cy,
            intArrayOf(arcPaint.color and 0x00FFFFFF or 0x80000000.toInt(), arcPaint.color),
            floatArrayOf(0f, percent / 100f)
        ).also {
            val matrix = Matrix()
            matrix.setRotate(-90f, cx, cy)
            it.setLocalMatrix(matrix)
        }
        if (sweepAngle > 0f) {
            canvas.drawArc(rectF, -90f, sweepAngle, false, arcPaint)
        }

        // Center text
        textPaint.textSize = radius * 0.35f
        val textY = cy + textPaint.textSize * 0.35f - (if (subText.isNotEmpty()) dp * 6 else 0f)
        canvas.drawText(centerText, cx, textY, textPaint)

        // Sub text
        if (subText.isNotEmpty()) {
            subTextPaint.textSize = radius * 0.16f
            canvas.drawText(subText, cx, textY + textPaint.textSize * 0.9f, subTextPaint)
        }
    }
}
