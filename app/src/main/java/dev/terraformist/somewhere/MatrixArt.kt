package dev.terraformist.somewhere

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * What the 25×25 Glyph Matrix shows:
 *  - far away: a block of dots at the edge, on the side the store is, pulsing slowly;
 *  - within 2 km: a needle of dots from the centre out to that block;
 *  - the closer you get, the faster it pulses;
 *  - arrived: the whole outer ring pulses;
 *  - after a long-press of the Glyph Button: the distance in numbers for a few seconds.
 */
object MatrixArt {

    private const val C = GlyphShape.CENTER.toDouble()
    private const val TIP_RADIUS = 10.4
    private const val NEEDLE_END = 8.8

    /**
     * @param angle where the store is, degrees clockwise from the top of the matrix.
     * @param still true for a frame that will stay on the matrix for a while: no pulsing.
     */
    fun render(reading: Reading?, angle: Double, now: Long, showDistance: Boolean, still: Boolean, dim: Boolean): DotGrid {
        val grid = DotGrid(GlyphShape.SIZE, GlyphShape.SIZE)
        when {
            reading == null -> {
                // Don't know where we are yet: a lone centre dot, breathing.
                grid.set(12, 12, (255 * pulse(now, 2_400L, still)).toInt())
            }
            showDistance -> distance(grid, reading)
            reading.arrived -> arrived(grid, now, still)
            else -> pointer(grid, reading, angle, now, still)
        }
        if (dim) for (i in grid.levels.indices) grid.levels[i] = grid.levels[i] / 2
        return grid
    }

    // ---------- Switch-on ----------

    /** How long the switch-on lasts. After this, [render] takes over. */
    const val INTRO_MS = 1_050L
    private const val ALL_ON_MS = 120L
    private const val DISSOLVE_MS = 570L
    private const val BEAT_MS = 720L

    /**
     * Played once when you switch to this toy, so it's recognisable among the other Somewhere toys:
     * everything (the whole matrix lit) dissolves from the edges in to nothing (one centre dot),
     * which then shoots out to somewhere (the edge facing the store), where the pointer takes over.
     * If you've arrived, the dot opens into the arrival ring instead. No position yet: it stays a dot.
     */
    fun intro(reading: Reading?, angle: Double, elapsedMs: Long): DotGrid {
        val grid = DotGrid(GlyphShape.SIZE, GlyphShape.SIZE)
        val t = elapsedMs.coerceIn(0L, INTRO_MS)
        when {
            t < DISSOLVE_MS -> {
                for (y in 0 until GlyphShape.SIZE) for (x in 0 until GlyphShape.SIZE) {
                    if (GlyphShape.contains(x, y) && t < switchOffAt(x, y)) grid.set(x, y, 255)
                }
            }
            t >= BEAT_MS && reading != null -> {
                val p = smooth((t - BEAT_MS).toDouble() / (INTRO_MS - BEAT_MS))
                if (reading.arrived) {
                    val r = p * 11.6
                    for (y in 0 until GlyphShape.SIZE) for (x in 0 until GlyphShape.SIZE) {
                        if (kotlin.math.abs(radius(x, y) - r) <= 0.7) grid.set(x, y, 255)
                    }
                } else {
                    val a = Math.toRadians(angle)
                    // The dot, with a short fading tail behind it.
                    for ((back, level) in listOf(0.0 to 255, 1.0 to 140, 2.0 to 60)) {
                        val d = p * TIP_RADIUS - back
                        if (d < 0) continue
                        val x = Math.round(C + d * sin(a)).toInt()
                        val y = Math.round(C - d * cos(a)).toInt()
                        grid.lift(x, y, level)
                    }
                }
            }
        }
        grid.lift(12, 12, 255)
        return grid
    }

    /** When each dot goes dark in the dissolve: all on at first, then outer dots first, with a little scatter. */
    fun switchOffAt(x: Int, y: Int): Long {
        val outside = (radius(x, y) / 12.7).coerceIn(0.0, 1.0)
        val scatter = ((x * 37 + y * 61) % 11 - 5) * 14
        val span = (DISSOLVE_MS - ALL_ON_MS) * 0.85
        return maxOf(ALL_ON_MS, ALL_ON_MS + (span * (1 - outside)).toLong() + scatter)
    }

    private fun smooth(t: Double): Double {
        val c = t.coerceIn(0.0, 1.0)
        return c * c * (3 - 2 * c)
    }

    private fun distance(grid: DotGrid, reading: Reading) {
        val d = reading.distance
        val font = DotFont.SMALL
        grid.text(font, d.number, (GlyphShape.SIZE - font.width(d.number)) / 2, 6)
        grid.text(font, d.unit, (GlyphShape.SIZE - font.width(d.unit)) / 2, 14, level = 140)
    }

    private fun arrived(grid: DotGrid, now: Long, still: Boolean) {
        val level = (255 * pulse(now, 700L, still)).toInt()
        for (y in 0 until GlyphShape.SIZE) for (x in 0 until GlyphShape.SIZE) {
            val r = radius(x, y)
            if (r in 10.6..12.6) grid.set(x, y, level)
        }
        grid.set(12, 12, 255)
    }

    private fun pointer(grid: DotGrid, reading: Reading, angle: Double, now: Long, still: Boolean) {
        val a = Math.toRadians(angle)
        val dirX = sin(a)
        val dirY = -cos(a)
        val tipX = C + TIP_RADIUS * dirX
        val tipY = C + TIP_RADIUS * dirY
        val close = reading.meters <= Reading.CLOSE_METERS
        val tipLevel = (255 * pulse(now, periodFor(reading.meters), still)).toInt()

        for (y in 0 until GlyphShape.SIZE) for (x in 0 until GlyphShape.SIZE) {
            val dx = x - C
            val dy = y - C
            // The tip: dots around the point on the edge facing the store.
            val tx = x - tipX
            val ty = y - tipY
            if (sqrt(tx * tx + ty * ty) <= 1.3) grid.lift(x, y, tipLevel)

            // The needle: dots along the line from the centre, brighter further out.
            if (close) {
                val along = dx * dirX + dy * dirY
                val across = dx * dirY - dy * dirX
                if (along in 1.5..NEEDLE_END && kotlin.math.abs(across) <= 0.6) {
                    grid.lift(x, y, (70 + 130 * along / NEEDLE_END).toInt())
                }
            }
        }
        grid.lift(12, 12, if (close) 255 else 110)
    }

    private fun radius(x: Int, y: Int): Double {
        val dx = x - C
        val dy = y - C
        return sqrt(dx * dx + dy * dy)
    }

    /** Pulse length: 0.6 s at 100 m or closer, slowing to 3 s at 10,000 km. */
    fun periodFor(meters: Double): Long {
        val t = ((log10(meters.coerceAtLeast(1.0)) - 2.0) / 5.0).coerceIn(0.0, 1.0)
        return (600 + t * 2_400).toLong()
    }

    /** 0.3..1, smoothly, once per [period]. Always 1 for a still frame. */
    private fun pulse(now: Long, period: Long, still: Boolean): Double {
        if (still) return 1.0
        val phase = (now % period).toDouble() / period
        return 0.3 + 0.7 * (0.5 + 0.5 * cos(2 * PI * phase))
    }
}
