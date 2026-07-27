package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters

import org.slf4j.LoggerFactory

/**
 * Parsing and change-detection for the `last_ride_1`..`last_ride_5` properties (codes 0x0601-0x0605).
 * Each is a fixed-width 32-digit ASCII string packing *two* 16-digit ride records back to back
 * (oldest first, matching the overall oldest to newest convention of the 5 codes themselves), and
 * neither record carries a timestamp. Each record contains *four* 4-digit fields. Each field is a
 * decimal number, multiplied by 10:
 *
 * ```
 * [0:4]    duration   (minutes)
 * [4:8]    distance   (km)
 * [8:12]   avg speed  (km/h)
 * [12:16]  unknown
 * ```
 *
 * `last_ride_1` is the oldest of the 5 slots and `last_ride_5` the newest; as new rides happen the
 * window shifts and the oldest ride record eventually falls off entirely.
 */
object XiaomiScooterRideHistory {
    private val LOG = LoggerFactory.getLogger(XiaomiScooterRideHistory::class.java)

    private const val FIELD_WIDTH = 4
    private const val RECORD_WIDTH = FIELD_WIDTH * 4
    private const val ENTRY_WIDTH = RECORD_WIDTH * 2

    data class Ride(
        val durationMinutes: Float,
        val distanceKm: Float,
        val avgSpeedKmh: Float,
        val unknown4: Float,
    ) {
        fun isEmpty(): Boolean {
            return durationMinutes == 0f &&
                    distanceKm == 0f &&
                    avgSpeedKmh == 0f
        }
    }

    /** Decodes a single 16-digit ride record. */
    fun decodeRecord(raw: String): Ride? {
        if (raw.length != RECORD_WIDTH || !raw.all { it.isDigit() }) {
            LOG.error("Failed to parse scooter ride record, invalid format: {}", raw)
            return null
        }
        fun field(index: Int) = raw.substring(index * FIELD_WIDTH, (index + 1) * FIELD_WIDTH).toInt() / 10f
        return Ride(
            durationMinutes = field(0),
            distanceKm = field(1),
            avgSpeedKmh = field(2),
            unknown4 = field(3),
        )
    }

    /** Splits one `last_ride_N` wire value (32 digits) into its two packed 16-digit ride records, oldest first. */
    fun splitEntry(raw: String): List<String> {
        if (raw.length != ENTRY_WIDTH || !raw.all { it.isDigit() }) {
            LOG.error("Failed to parse scooter ride entry, invalid format: {}", raw)
            return emptyList()
        }
        return listOf(raw.substring(0, RECORD_WIDTH), raw.substring(RECORD_WIDTH, ENTRY_WIDTH))
    }

    /**
     * Flattens the wire values of the 5 `last_ride_N` codes (oldest to newest code order) into the
     * individual raw 16-digit ride records they pack, oldest ride first overall.
     */
    fun flatten(entries: List<String>): List<String> = entries.flatMap { splitEntry(it) }

    /**
     * Returns the entries of [current] (oldest to newest) that aren't already accounted for in
     * [previouslySeen] -- the raw records of every ride already imported, in any order.
     *
     * Compares by raw-string equality with multiplicity: each entry in [previouslySeen] can match
     * at most one entry in [current], rather than a single seen value silently swallowing
     * every later occurrence. This way two genuinely different rides that happen to share identical
     * rounded stats are still both imported, while an already-imported ride re-appearing (shifted
     * towards the older end, as new rides push it down) is correctly recognized and skipped.
     */
    fun newRidesSince(previouslySeen: List<String>, current: List<String>): List<String> {
        val remaining = previouslySeen.groupingBy { it }.eachCount().toMutableMap()
        return current.filter { raw ->
            val count = remaining[raw] ?: 0
            if (count > 0) {
                remaining[raw] = count - 1
                false
            } else {
                true
            }
        }
    }
}
