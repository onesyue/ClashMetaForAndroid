package com.github.kr328.clash.design.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.github.kr328.clash.design.R

class SpeedChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val maxPoints = 60
    private val downloadPoints = ArrayDeque<Long>(maxPoints)
    private val uploadPoints = ArrayDeque<Long>(maxPoints)

    private val downloadPath = Path()
    private val uploadPath = Path()
    private val downloadFillPath = Path()
    private val uploadFillPath = Path()

    private val downloadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, R.color.color_status_good)
    }

    private val uploadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, R.color.color_primary)
    }

    private val downloadFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val uploadFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.5f * resources.displayMetrics.density
        color = ContextCompat.getColor(context, R.color.color_divider)
    }

    fun addDataPoint(downloadSpeed: Long, uploadSpeed: Long) {
        if (downloadPoints.size >= maxPoints) downloadPoints.removeFirst()
        if (uploadPoints.size >= maxPoints) uploadPoints.removeFirst()
        downloadPoints.addLast(downloadSpeed)
        uploadPoints.addLast(uploadSpeed)
        invalidate()
    }

    fun reset() {
        downloadPoints.clear()
        uploadPoints.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (downloadPoints.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val pad = 2f * resources.displayMetrics.density

        // Draw grid lines
        for (i in 1..3) {
            val y = h * i / 4f
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        val maxVal = maxOf(
            downloadPoints.maxOrNull() ?: 1L,
            uploadPoints.maxOrNull() ?: 1L,
            1024L // minimum 1KB/s scale
        ).toFloat()

        // Build paths
        buildPaths(downloadPoints, downloadPath, downloadFillPath, w, h, pad, maxVal)
        buildPaths(uploadPoints, uploadPath, uploadFillPath, w, h, pad, maxVal)

        // Update gradient fills
        downloadFillPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            (downloadPaint.color and 0x00FFFFFF) or 0x40000000,
            (downloadPaint.color and 0x00FFFFFF) or 0x05000000,
            Shader.TileMode.CLAMP
        )
        uploadFillPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            (uploadPaint.color and 0x00FFFFFF) or 0x30000000,
            (uploadPaint.color and 0x00FFFFFF) or 0x05000000,
            Shader.TileMode.CLAMP
        )

        // Draw fills then strokes
        canvas.drawPath(downloadFillPath, downloadFillPaint)
        canvas.drawPath(uploadFillPath, uploadFillPaint)
        canvas.drawPath(downloadPath, downloadPaint)
        canvas.drawPath(uploadPath, uploadPaint)
    }

    private fun buildPaths(
        points: ArrayDeque<Long>,
        linePath: Path,
        fillPath: Path,
        w: Float, h: Float, pad: Float, maxVal: Float,
    ) {
        linePath.reset()
        fillPath.reset()
        if (points.size < 2) return

        val count = points.size
        val stepX = w / (maxPoints - 1).toFloat()
        val startX = w - (count - 1) * stepX

        var prevX = startX
        var prevY = h - pad - (points[0].toFloat() / maxVal) * (h - 2 * pad)
        linePath.moveTo(prevX, prevY)
        fillPath.moveTo(prevX, h)
        fillPath.lineTo(prevX, prevY)

        for (i in 1 until count) {
            val x = startX + i * stepX
            val y = h - pad - (points[i].toFloat() / maxVal) * (h - 2 * pad)
            // Smooth cubic bezier
            val cx = (prevX + x) / 2f
            linePath.cubicTo(cx, prevY, cx, y, x, y)
            fillPath.cubicTo(cx, prevY, cx, y, x, y)
            prevX = x
            prevY = y
        }

        fillPath.lineTo(prevX, h)
        fillPath.close()
    }
}
