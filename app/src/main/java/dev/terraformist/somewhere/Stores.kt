package dev.terraformist.somewhere

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/**
 * One physical Nothing store.
 *
 * @param hours opening and closing time for each day, in minutes after midnight, local to the store.
 *   A day that's missing means closed all day.
 */
class Store(
    val area: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val zone: ZoneId,
    val hours: Map<DayOfWeek, IntRange>,
) {
    /** "SOHO · LONDON" */
    val label: String get() = "$area · $city"

    fun isOpen(at: Instant = Instant.now()): Boolean {
        val local = at.atZone(zone)
        val todays = hours[local.dayOfWeek] ?: return false
        val minute = local.hour * 60 + local.minute
        return minute >= todays.first && minute < todays.last
    }
}

/**
 * Every Nothing-owned store. Partner shops (Best Buy, Kith, pop-ups) don't count.
 *
 * To add a store when one opens (New York and Tokyo are announced), add an entry here.
 * Coordinates and hours come from the store's Google Maps listing.
 */
object Stores {

    private fun everyDay(open: Int, close: Int): Map<DayOfWeek, IntRange> =
        DayOfWeek.entries.associateWith { open..close }

    private fun hm(hours: Int, minutes: Int = 0) = hours * 60 + minutes

    val ALL: List<Store> = listOf(
        // 4 Peter Street, London W1F 0AD. Opened 2022.
        Store(
            area = "SOHO",
            city = "LONDON",
            latitude = 51.5129014,
            longitude = -0.1340202,
            zone = ZoneId.of("Europe/London"),
            hours = DayOfWeek.entries.associateWith { day ->
                if (day == DayOfWeek.SUNDAY) hm(12)..hm(18) else hm(11)..hm(18)
            },
        ),
        // 660/1, 100 Feet Road, Indiranagar, Bengaluru 560038. Opened 14 February 2026.
        Store(
            area = "INDIRANAGAR",
            city = "BENGALURU",
            latitude = 12.976177,
            longitude = 77.6407718,
            zone = ZoneId.of("Asia/Kolkata"),
            hours = everyDay(hm(11), hm(20, 30)),
        ),
    )
}
