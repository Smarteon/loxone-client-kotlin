package cz.smarteon.loxkt.event

import kotlin.js.JsExport

private const val MINUTES_PER_HOUR = 60
private const val TIME_FORMAT_WIDTH = 2

/**
 * Represents a single daytimer entry.
 *
 * Binary structure (24 bytes):
 * - Mode: 32-bit int (4 bytes) - operating mode number
 * - From: 32-bit int (4 bytes) - start time in minutes since midnight
 * - To: 32-bit int (4 bytes) - end time in minutes since midnight
 * - NeedActivate: 32-bit int (4 bytes) - whether trigger is needed
 * - Value: 64-bit float (8 bytes) - value for analog daytimer
 *
 * For digital daytimers: presence of entry means "on", absence means "off".
 * For analog daytimers: each entry has an associated value.
 *
 * @property mode Operating mode number
 * @property from Start time in minutes since midnight (0-1439)
 * @property to End time in minutes since midnight (0-1439)
 * @property needActivate Whether this entry needs activation/trigger
 * @property value Value for analog daytimer
 */
@JsExport
data class DaytimerEntry(
    val mode: Int,
    val from: Int,
    val to: Int,
    val needActivate: Boolean,
    val value: Double
) {

    /**
     * Get the start time as a formatted string (HH:MM).
     */
    fun fromTimeString() = minutesFromMidnightToString(from)

    /**
     * Get the end time as a formatted string (HH:MM).
     */
    fun toTimeString() = minutesFromMidnightToString(to)

    private fun minutesFromMidnightToString(minutes: Int): String {
        val hours = minutes / MINUTES_PER_HOUR
        val mins = minutes % MINUTES_PER_HOUR
        val hoursStr = hours.toString().padStart(TIME_FORMAT_WIDTH, '0')
        val minsStr = mins.toString().padStart(TIME_FORMAT_WIDTH, '0')
        return "$hoursStr:$minsStr"
    }

    companion object {
        /** Size of a daytimer entry in bytes */
        const val SIZE_BYTES = 24
    }
}

/**
 * Represents a daytimer state update event.
 * Daytimer-States contain schedule entries for time-based automation.
 *
 * Binary structure:
 * - UUID: 128-bit (16 bytes)
 * - Default value: 64-bit float (8 bytes)
 * - Number of entries: 32-bit int (4 bytes)
 * - Entries: variable number of [DaytimerEntry]
 *
 * @property uuid UUID of the daytimer state being updated
 * @property defaultValue Default value when no entry is active
 * @property entries List of daytimer entries
 */
data class DaytimerEvent(
    override val uuid: String,
    val defaultValue: Double,
    val entries: List<DaytimerEntry>
) : LoxoneEvent {
    companion object {
        /** Size of the daytimer event header in bytes (UUID + default value + entry count) */
        const val HEADER_SIZE_BYTES = 28
    }
}
