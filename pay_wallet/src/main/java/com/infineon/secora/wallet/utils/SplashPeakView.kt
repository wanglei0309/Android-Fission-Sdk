package com.infineon.secora.wallet.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.widget.FrameLayout

class SplashPeakView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val path = Path()

    private val peakHeightDp = 36f

    private val peakHeightPx: Float
        get() = peakHeightDp * resources.displayMetrics.density

    init {
        // IMPORTANT:
        // Do not give this View a rectangular background.
        setBackgroundColor(Color.TRANSPARENT)

        // We are drawing the white shape ourselves.
        setWillNotDraw(false)

        clipChildren = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawPeakShape(canvas)
    }

    private fun drawPeakShape(canvas: Canvas) {

        val peakHeight = peakHeightPx
        val centerX = width / 2f

        path.reset()

        /*
         *                  CENTER
         *                    ▲
         *                   / \
         *                  /   \
         *                 /     \
         * LEFT ----------       ---------- RIGHT
         *
         * White area is everything BELOW this shape.
         */

        path.moveTo(0f, peakHeight)

        // Left -> center peak
        path.lineTo(centerX, 0f)

        // Center peak -> right
        path.lineTo(width.toFloat(), peakHeight)

        // Right side down
        path.lineTo(width.toFloat(), height.toFloat())

        // Bottom
        path.lineTo(0f, height.toFloat())

        path.close()

        canvas.drawPath(path, paint)
    }

    override fun dispatchDraw(canvas: Canvas) {

        val peakHeight = peakHeightPx
        val centerX = width / 2f

        path.reset()

        path.moveTo(0f, peakHeight)
        path.lineTo(centerX, 0f)
        path.lineTo(width.toFloat(), peakHeight)
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()

        canvas.save()

        // Keep child views inside the white triangular panel.
        canvas.clipPath(path)

        super.dispatchDraw(canvas)

        canvas.restore()
    }
}