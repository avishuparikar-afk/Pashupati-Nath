package com.pashuraksha

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scanLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var scanLinePosition = 0f // 0 to 1, representing top to bottom

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = ContextCompat.getColor(context, R.color.bioluminescent_green)

        scanLinePaint.style = Paint.Style.STROKE
        scanLinePaint.strokeWidth = 8f
        scanLinePaint.color = ContextCompat.getColor(context, R.color.bioluminescent_green)

        // Scanning line animation
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.addUpdateListener { animation ->
            scanLinePosition = animation.animatedValue as Float
            invalidate()
        }
        animator.duration = 2000 // 2 seconds for one sweep
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return

        // Draw corner brackets
        val cornerSize = 50f
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // Top-left
        canvas.drawLine(rect.left, rect.top + cornerSize, rect.left, rect.top, paint)
        canvas.drawLine(rect.left, rect.top, rect.left + cornerSize, rect.top, paint)
        // Top-right
        canvas.drawLine(rect.right - cornerSize, rect.top, rect.right, rect.top, paint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + cornerSize, paint)
        // Bottom-left
        canvas.drawLine(rect.left, rect.bottom - cornerSize, rect.left, rect.bottom, paint)
        canvas.drawLine(rect.left, rect.bottom, rect.left + cornerSize, rect.bottom, paint)
        // Bottom-right
        canvas.drawLine(rect.right - cornerSize, rect.bottom, rect.right, rect.bottom, paint)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - cornerSize, paint)

        // Draw scanning grid lines (simplified as horizontal lines for now)
        val gridLineCount = 10
        for (i in 0 until gridLineCount) {
            val y = height * (i.toFloat() / gridLineCount)
            paint.alpha = (70 * (1 - Math.abs(scanLinePosition - (i.toFloat() / gridLineCount)))).toInt() // Fade based on proximity to scan line
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
        }

        // Draw horizontal glowing scan line
        val scanY = height * scanLinePosition
        canvas.drawLine(0f, scanY, width.toFloat(), scanY, scanLinePaint)

        // TODO: Draw bounding boxes and health overlays for detected cattle
    }

    // Function to draw bounding boxes for detected objects
    fun drawBoundingBoxes(boxes: List<RectF>, labels: List<String>, healthScores: List<String>) {
        // This will be implemented later when ML Kit is integrated
        invalidate()
    }
}
