package dev.terraformist.somewhere

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.roundToInt

/** A line of dot-matrix text, used for the "One job." heading on the About card. */
class DotTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var text: String = ""
        set(value) {
            field = value
            contentDescription = value
            requestLayout()
            invalidate()
        }

    /** Pixels between dot centres. */
    var pitch: Float = 2.8f * resources.displayMetrics.density
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = (DotFont.LARGE.width(text) * pitch).roundToInt() + paddingLeft + paddingRight
        val h = (DotFont.LARGE.rows(text) * pitch).roundToInt() + paddingTop + paddingBottom
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        Art.dots(canvas, DotFont.LARGE, text, paddingLeft.toFloat(), paddingTop.toFloat(), pitch)
    }
}

/** The little dotted arrow with the red store dot, in the corner of the About card. */
class MiniArrowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Drawn in a 40-unit box, like the mockup.
    private val dots = arrayOf(20 to 12, 15 to 17, 20 to 17, 25 to 17, 20 to 22, 20 to 27, 20 to 32)

    override fun onDraw(canvas: Canvas) {
        val s = minOf(width, height) / 40f
        canvas.save()
        canvas.translate((width - 40 * s) / 2f, (height - 40 * s) / 2f)
        canvas.scale(s, s)
        paint.color = Art.WHITE
        for ((x, y) in dots) canvas.drawCircle(x.toFloat(), y.toFloat(), 2.2f, paint)
        paint.color = Art.RED
        canvas.drawCircle(20f, 4f, 3f, paint)
        canvas.restore()
    }
}
