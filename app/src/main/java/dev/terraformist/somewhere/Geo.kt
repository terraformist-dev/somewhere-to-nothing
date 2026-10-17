package dev.terraformist.somewhere

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/** A distance split into the dotted number and its unit: "1.2" + "KM", "340" + "M", "5,570" + "KM". */
data class Distance(val number: String, val unit: String)

/** Where the nearest store is, from where you are. No Android code, so it's easy to check. */
data class Reading(
    val store: Store,
    val meters: Double,
    /** Compass direction to the store, in degrees clockwise from true north. */
    val bearing: Double,
) {
    val arrived: Boolean get() = meters <= ARRIVED_METERS
    val far: Boolean get() = meters > FAR_METERS
    val distance: Distance get() = Geo.format(meters)

    /** "38° NE" */
    val bearingLabel: String get() = "${bearing.roundToInt() % 360}° ${Geo.cardinal(bearing)}"

    companion object {
        /** Within this, you've arrived. */
        const val ARRIVED_METERS = 50.0

        /** Beyond this, opening hours and the compass direction stop mattering much. */
        const val FAR_METERS = 100_000.0

        /** Within this, the Glyph Matrix draws a needle from the centre. */
        const val CLOSE_METERS = 2_000.0
    }
}

object Geo {

    private const val EARTH_RADIUS_M = 6_371_000.0

    fun nearest(latitude: Double, longitude: Double, stores: List<Store> = Stores.ALL): Reading =
        stores.map { store ->
            Reading(
                store = store,
                meters = distanceMeters(latitude, longitude, store.latitude, store.longitude),
                bearing = bearingDegrees(latitude, longitude, store.latitude, store.longitude),
            )
        }.minBy { it.meters }

    /** Great-circle distance in metres. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = Math.toRadians(lat2 - lat1)
        val dl = Math.toRadians(lon2 - lon1)
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Starting direction of the shortest path, degrees clockwise from true north (0..360). */
    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dl = Math.toRadians(lon2 - lon1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Metres under 1 km (to the nearest 10), one decimal under 10 km, whole kilometres with commas after that. */
    fun format(meters: Double): Distance = when {
        meters < 995 -> Distance((((meters / 10).roundToInt()) * 10).toString(), "M")
        meters < 9_950 -> Distance(String.format(Locale.US, "%.1f", meters / 1000), "KM")
        else -> Distance(NumberFormat.getIntegerInstance(Locale.US).format((meters / 1000).roundToLong()), "KM")
    }

    fun cardinal(degrees: Double): String {
        val names = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return names[(((degrees % 360) + 360) % 360 / 45).roundToInt() % 8]
    }

    /** The smallest signed difference a - b, in degrees (-180..180). */
    fun angleDiff(a: Double, b: Double): Double = ((a - b) % 360 + 540) % 360 - 180
}
