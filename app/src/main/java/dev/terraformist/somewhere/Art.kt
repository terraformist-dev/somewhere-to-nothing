package dev.terraformist.somewhere

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** What there is to show. */
enum class Mode { NO_PERMISSION, LOOKING, READY }

/**
 * Everything drawn on screen: the dotted compass, the dot-matrix numbers and the small
 * Space Mono labels. Shared by the widget and the full-screen compass so they always match.
 */
object Art {

    const val BLACK = 0xFF000000.toInt()
    const val WHITE = 0xFFF2F2F2.toInt()
    const val GREY = 0xFF9A9A9A.toInt()
    const val GHOST = 0xFF333333.toInt()
    const val RING = 0xFF3A3A3A.toInt()
    const val RED = 0xFFD71921.toInt()

    private var regular: Typeface? = null
    private var bold: Typeface? = null

    fun typeface(context: Context, isBold: Boolean): Typeface {
        val cached = if (isBold) bold else regular
        if (cached != null) return cached
        val loaded = try {
            Typeface.createFromAsset(context.assets, if (isBold) "SpaceMono-Bold.ttf" else "SpaceMono-Regular.ttf")
        } catch (e: RuntimeException) {
            Typeface.create(Typeface.MONOSPACE, if (isBold) Typeface.BOLD else Typeface.NORMAL)
        }
        if (isBold) bold = loaded else regular = loaded
        return loaded
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ---------- The compass ----------

    // The compass is designed in a 120-unit box: a ring of 48 dots, a dotted disc, a dotted arrow.
    private val ARROW = arrayOf(
        60 to 20, 50 to 30, 60 to 30, 70 to 30, 40 to 40, 60 to 40, 80 to 40,
        30 to 50, 60 to 50, 90 to 50, 60 to 60, 60 to 70, 60 to 80, 60 to 90, 60 to 100,
    )

    /**
     * @param arrow where the store is, in degrees clockwise from the top; null draws no arrow.
     * @param north if set, a small white dot on the ring marks north, at this angle from the top.
     * @param arrived draws the "you're here" state: a red dot in the middle, no arrow.
     */
    fun compass(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        arrow: Double?,
        north: Double? = null,
        arrived: Boolean = false,
    ) {
        canvas.save()
        canvas.translate(centerX - size / 2f, centerY - size / 2f)
        val scale = size / 120f
        canvas.scale(scale, scale)

        dotPaint.color = RING
        for (i in 0 until 48) {
            val a = Math.PI * 2 * i / 48
            canvas.drawCircle((60 + 54 * cos(a)).toFloat(), (60 + 54 * sin(a)).toFloat(), 1.6f, dotPaint)
        }
        if (north != null && !arrived) {
            val a = Math.toRadians(north)
            dotPaint.color = WHITE
            canvas.drawCircle((60 + 54 * sin(a)).toFloat(), (60 - 54 * cos(a)).toFloat(), 2.4f, dotPaint)
        }

        if (arrow != null && !arrived) canvas.rotate(arrow.toFloat(), 60f, 60f)

        dotPaint.color = GHOST
        for (gy in 0..12) for (gx in 0..12) {
            val x = gx * 10f
            val y = gy * 10f
            val dx = x - 60f
            val dy = y - 60f
            if (sqrt(dx * dx + dy * dy) <= 44f) canvas.drawCircle(x, y, 2f, dotPaint)
        }

        when {
            arrived -> {
                dotPaint.color = RED
                canvas.drawCircle(60f, 60f, 7f, dotPaint)
            }
            arrow != null -> {
                dotPaint.color = WHITE
                for ((x, y) in ARROW) canvas.drawCircle(x.toFloat(), y.toFloat(), 3.4f, dotPaint)
                dotPaint.color = RED
                canvas.drawCircle(60f, 6f, 5f, dotPaint)
            }
        }
        canvas.restore()
    }

    // ---------- Dot-matrix text ----------

    /** Draws [text] in dots, top-left at ([left], [top]), [pitch] pixels between dot centres. Returns its width. */
    fun dots(canvas: Canvas, font: DotFont, text: String, left: Float, top: Float, pitch: Float, color: Int = WHITE): Float {
        dotPaint.color = color
        val r = pitch * 0.42f
        font.forEachDot(text) { x, y ->
            canvas.drawCircle(left + x * pitch + pitch / 2f, top + y * pitch + pitch / 2f, r, dotPaint)
        }
        return font.width(text) * pitch
    }

    fun dotsWidth(font: DotFont, text: String, pitch: Float): Float = font.width(text) * pitch

    // ---------- Plain labels ----------

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = Rect()

    private fun setUpText(context: Context, size: Float, isBold: Boolean, color: Int) {
        textPaint.typeface = typeface(context, isBold)
        textPaint.textSize = size
        textPaint.color = color
        textPaint.letterSpacing = 0.08f
    }

    fun textWidth(context: Context, text: String, size: Float, isBold: Boolean = false): Float {
        setUpText(context, size, isBold, WHITE)
        return textPaint.measureText(text)
    }

    fun capHeight(context: Context, size: Float, isBold: Boolean = false): Float {
        setUpText(context, size, isBold, WHITE)
        textPaint.getTextBounds("H", 0, 1, bounds)
        return bounds.height().toFloat()
    }

    /** Draws [text] with its capital letters' top at [top]. Shrinks it to fit [maxWidth]. Returns the cap height. */
    fun label(
        context: Context,
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        size: Float,
        color: Int = GREY,
        isBold: Boolean = false,
        maxWidth: Float = Float.MAX_VALUE,
        centered: Boolean = false,
    ): Float {
        setUpText(context, size, isBold, color)
        var w = textPaint.measureText(text)
        if (w > maxWidth) {
            textPaint.textSize = size * maxWidth / w
            w = maxWidth
        }
        textPaint.getTextBounds("H", 0, 1, bounds)
        val cap = bounds.height().toFloat()
        val x = if (centered) left - w / 2f else left
        canvas.drawText(text, x, top + cap, textPaint)
        return cap
    }

    /** A small status dot: filled white when the store is open, an empty grey ring when it's closed. */
    fun statusDot(canvas: Canvas, centerX: Float, centerY: Float, size: Float, open: Boolean) {
        dotPaint.color = if (open) WHITE else GREY
        if (open) {
            canvas.drawCircle(centerX, centerY, size / 2f, dotPaint)
        } else {
            dotPaint.style = Paint.Style.STROKE
            dotPaint.strokeWidth = size * 0.2f
            canvas.drawCircle(centerX, centerY, size / 2f - dotPaint.strokeWidth / 2f, dotPaint)
            dotPaint.style = Paint.Style.FILL
        }
    }

    // ---------- Words ----------

    fun statusLine(reading: Reading, open: Boolean): String = when {
        reading.arrived -> if (open) "OPEN NOW" else "CLOSED NOW"
        reading.far -> "NEAREST STORE"
        else -> (if (open) "OPEN NOW · " else "CLOSED · ") + reading.bearingLabel
    }

    fun describe(reading: Reading?, mode: Mode): String = when {
        mode == Mode.NO_PERMISSION -> "Somewhere to Nothing. Location is off. Tap to start."
        reading == null -> "Somewhere to Nothing. Finding where you are."
        reading.arrived -> "You're at the Nothing store, ${reading.store.area.lowercase()}, ${reading.store.city.lowercase()}."
        else -> {
            val d = reading.distance
            val unit = if (d.unit == "KM") "kilometres" else "metres"
            "Nearest Nothing store: ${reading.store.area.lowercase()}, ${reading.store.city.lowercase()}, " +
                "${d.number} $unit, heading ${Geo.cardinal(reading.bearing)}."
        }
    }

    // ---------- The home-screen widget ----------

    /**
     * Draws the widget's contents at its real size in pixels. Wide widgets (4×2) put the
     * compass on the left and the details on the right; square ones (2×2) stack the
     * compass over the distance. On the widget the top of the compass is north, like a map,
     * because a widget can't follow the phone as it turns.
     */
    fun widget(context: Context, width: Int, height: Int, reading: Reading?, mode: Mode, open: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (width >= height * 1.6f) wide(context, canvas, width.toFloat(), height.toFloat(), reading, mode, open)
        else square(context, canvas, width.toFloat(), height.toFloat(), reading, mode)
        return bmp
    }

    private fun wide(context: Context, canvas: Canvas, w: Float, h: Float, reading: Reading?, mode: Mode, open: Boolean) {
        val u = h / 170f
        val pad = 20f * u
        val size = min(h - 2 * pad, w * 0.4f)
        compass(
            canvas, pad + size / 2f, h / 2f, size,
            arrow = reading?.bearing,
            north = 0.0,
            arrived = reading?.arrived == true,
        )

        val left = pad + size + 18f * u
        val room = w - left - pad
        val nameSize = 12f * u
        val statusSize = 11f * u

        if (reading == null) {
            val pitch = 6f * u
            val block = 7 * pitch + 12f * u + nameSize
            val top = (h - block) / 2f
            dots(canvas, DotFont.LARGE, "--", left, top, pitch)
            val words = if (mode == Mode.NO_PERMISSION) "TAP TO START" else "FINDING YOU"
            label(context, canvas, words, left, top + 7 * pitch + 12f * u, nameSize, maxWidth = room)
            return
        }

        val headlineTop: Float
        val headlineHeight: Float
        if (reading.arrived) {
            val pitch = min(4.2f * u, room / DotFont.LARGE.width("SOMEWHERE"))
            headlineHeight = 7 * pitch * 2 + pitch * 2
            headlineTop = (h - (headlineHeight + 10f * u + nameSize + 8f * u + statusSize)) / 2f
            dots(canvas, DotFont.LARGE, "YOU'RE", left, headlineTop, pitch)
            dots(canvas, DotFont.LARGE, "SOMEWHERE", left, headlineTop + 9 * pitch, pitch)
        } else {
            val d = reading.distance
            val unitSize = 14f * u
            val unitW = textWidth(context, d.unit, unitSize, isBold = true)
            val pitch = min(6f * u, (room - unitW - 6f * u) / DotFont.LARGE.width(d.number))
            headlineHeight = 7 * pitch
            headlineTop = (h - (headlineHeight + 12f * u + nameSize + 8f * u + statusSize)) / 2f
            val numberW = dots(canvas, DotFont.LARGE, d.number, left, headlineTop, pitch)
            setUpText(context, unitSize, true, WHITE)
            textPaint.getTextBounds("H", 0, 1, bounds)
            label(context, canvas, d.unit, left + numberW + 6f * u, headlineTop + headlineHeight - bounds.height(), unitSize, WHITE, isBold = true)
        }

        val nameTop = headlineTop + headlineHeight + (if (reading.arrived) 10f else 12f) * u
        label(context, canvas, reading.store.label, left, nameTop, nameSize, GREY, maxWidth = room)

        val statusTop = nameTop + nameSize + 8f * u
        val dotSize = 6f * u
        val showDot = !reading.far || reading.arrived
        var textLeft = left
        if (showDot) {
            setUpText(context, statusSize, false, WHITE)
            textPaint.getTextBounds("H", 0, 1, bounds)
            statusDot(canvas, left + dotSize / 2f, statusTop + bounds.height() / 2f, dotSize, open)
            textLeft += dotSize + 8f * u
        }
        label(context, canvas, statusLine(reading, open), textLeft, statusTop, statusSize, WHITE, maxWidth = w - pad - textLeft)
    }

    private fun square(context: Context, canvas: Canvas, w: Float, h: Float, reading: Reading?, mode: Mode) {
        val u = min(w, h) / 170f
        val pad = 14f * u
        val size = min(92f * u, h * 0.56f)
        val compassTop = pad + 2f * u
        compass(
            canvas, w / 2f, compassTop + size / 2f, size,
            arrow = reading?.bearing,
            north = 0.0,
            arrived = reading?.arrived == true,
        )

        val areaTop = compassTop + size
        val areaHeight = h - pad - areaTop
        val room = w - 2 * pad

        when {
            reading == null -> {
                val words = if (mode == Mode.NO_PERMISSION) "TAP TO START" else "FINDING YOU"
                val size2 = 11f * u
                label(context, canvas, words, w / 2f, areaTop + (areaHeight - size2 * 0.7f) / 2f, size2, GREY, maxWidth = room, centered = true)
            }
            reading.arrived -> {
                val pitch = min(3f * u, room / DotFont.LARGE.width("SOMEWHERE"))
                val block = 7 * pitch * 2 + 2 * pitch
                val top = areaTop + (areaHeight - block) / 2f
                val w1 = dotsWidth(DotFont.LARGE, "YOU'RE", pitch)
                val w2 = dotsWidth(DotFont.LARGE, "SOMEWHERE", pitch)
                dots(canvas, DotFont.LARGE, "YOU'RE", (w - w1) / 2f, top, pitch)
                dots(canvas, DotFont.LARGE, "SOMEWHERE", (w - w2) / 2f, top + 9 * pitch, pitch)
            }
            else -> {
                val d = reading.distance
                val unitSize = 12f * u
                val gap = 5f * u
                val unitW = textWidth(context, d.unit, unitSize, isBold = true)
                val pitch = min(4.2f * u, (room - unitW - gap) / DotFont.LARGE.width(d.number))
                val numberW = dotsWidth(DotFont.LARGE, d.number, pitch)
                val total = numberW + gap + unitW
                val left = (w - total) / 2f
                val top = areaTop + (areaHeight - 7 * pitch) / 2f
                dots(canvas, DotFont.LARGE, d.number, left, top, pitch)
                setUpText(context, unitSize, true, WHITE)
                textPaint.getTextBounds("H", 0, 1, bounds)
                label(context, canvas, d.unit, left + numberW + gap, top + 7 * pitch - bounds.height(), unitSize, WHITE, isBold = true)
            }
        }
    }
}
