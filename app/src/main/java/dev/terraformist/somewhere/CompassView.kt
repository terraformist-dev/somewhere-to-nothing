package dev.terraformist.somewhere

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * The full-screen compass. Unlike the widget, the arrow turns with the phone: it points
 * at the store from wherever you're facing. A small white dot on the ring marks north.
 */
class CompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var reading: Reading? = null
    var mode: Mode = Mode.LOOKING
    var open: Boolean = false

    /** Which way the top of the screen faces (degrees from north), or null if unknown. */
    var heading: Double? = null

    fun update() {
        contentDescription = Art.describe(reading, mode)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Art.BLACK)
        val w = width.toFloat()
        val u = w / 390f
        val cx = w / 2f

        var y = 40f * u
        Art.label(context, canvas, "SOMEWHERE TO NOTHING", cx, y, 11f * u, Art.GREY, centered = true)
        y += 11f * u + 48f * u

        // Leave room at the bottom for the buttons.
        val size = min(300f * u, height - y - 320f * u).coerceAtLeast(160f * u)
        val r = reading
        val h = heading
        val arrow = r?.bearing?.let { if (h != null) it - h else it }
        val north = if (h != null) -h else 0.0
        Art.compass(canvas, cx, y + size / 2f, size, arrow, north, arrived = r?.arrived == true)
        y += size + 28f * u

        val room = w - 48f * u
        if (r == null) {
            val pitch = 11f * u
            Art.dots(canvas, DotFont.LARGE, "--", cx - Art.dotsWidth(DotFont.LARGE, "--", pitch) / 2f, y, pitch)
            y += 7 * pitch + 16f * u
            val words = if (mode == Mode.NO_PERMISSION) "LOCATION IS OFF" else "FINDING YOU"
            Art.label(context, canvas, words, cx, y, 13f * u, Art.GREY, centered = true)
            return
        }

        if (r.arrived) {
            val pitch = min(5.5f * u, room / DotFont.LARGE.width("SOMEWHERE"))
            val w1 = Art.dotsWidth(DotFont.LARGE, "YOU'RE", pitch)
            val w2 = Art.dotsWidth(DotFont.LARGE, "SOMEWHERE", pitch)
            Art.dots(canvas, DotFont.LARGE, "YOU'RE", cx - w1 / 2f, y, pitch)
            Art.dots(canvas, DotFont.LARGE, "SOMEWHERE", cx - w2 / 2f, y + 9 * pitch, pitch)
            y += 16 * pitch + 16f * u
        } else {
            val d = r.distance
            val unitSize = 18f * u
            val gap = 10f * u
            val unitW = Art.textWidth(context, d.unit, unitSize, isBold = true)
            val pitch = min(11f * u, (room - unitW - gap) / DotFont.LARGE.width(d.number))
            val numberW = Art.dotsWidth(DotFont.LARGE, d.number, pitch)
            val left = cx - (numberW + gap + unitW) / 2f
            Art.dots(canvas, DotFont.LARGE, d.number, left, y, pitch)
            val unitCap = unitSize * 0.7f
            Art.label(context, canvas, d.unit, left + numberW + gap, y + 7 * pitch - unitCap, unitSize, Art.WHITE, isBold = true)
            y += 7 * pitch + 16f * u
        }

        Art.label(context, canvas, r.store.label, cx, y, 13f * u, Art.GREY, maxWidth = room, centered = true)
        y += 13f * u + 14f * u

        // Bottom line: direction, then open or closed (opening hours don't matter from another continent).
        val parts = mutableListOf<String>()
        if (!r.arrived) parts += r.bearingLabel
        if (!r.far) parts += if (open) "OPEN NOW" else "CLOSED NOW"
        val line = parts.joinToString("  /  ")
        Art.label(context, canvas, line, cx, y, 12f * u, Art.WHITE, maxWidth = room, centered = true)
    }
}
