package cz.smarteon.loxkt.state

import cz.smarteon.loxkt.event.DaytimerEntry
import cz.smarteon.loxkt.event.WeatherEntry
import kotlin.js.JsExport

/**
 * Sealed interface for storing typed event payloads in [LoxoneState].
 *
 * Each variant corresponds to one of the four event types from the Loxone WebSocket protocol.
 *
 * @see ValueState
 * @see TextState
 * @see DaytimerState
 * @see WeatherState
 */
@JsExport
sealed interface StateValue

/**
 * State value from a [cz.smarteon.loxkt.event.ValueEvent].
 *
 * @property value The numeric value
 */
@JsExport
data class ValueState(val value: Double) : StateValue

/**
 * State value from a [cz.smarteon.loxkt.event.TextEvent].
 *
 * @property text The text content
 * @property iconUuid UUID of the associated icon
 */
@JsExport
data class TextState(
    val text: String,
    val iconUuid: String
) : StateValue

/**
 * State value from a [cz.smarteon.loxkt.event.DaytimerEvent].
 *
 * @property defaultValue Default value when no entry is active
 * @property entries List of daytimer entries
 */
@JsExport
data class DaytimerState(
    val defaultValue: Double,
    val entries: List<DaytimerEntry>
) : StateValue {
    val entriesArray: Array<DaytimerEntry> get() = entries.toTypedArray()
}

/**
 * State value from a [cz.smarteon.loxkt.event.WeatherEvent].
 *
 * @property lastUpdate Last update timestamp (seconds since Loxone epoch 2009, UTC)
 * @property entries List of weather forecast entries
 */
@JsExport
data class WeatherState(
    val lastUpdate: Int,
    val entries: List<WeatherEntry>
) : StateValue {
    val entriesArray: Array<WeatherEntry> get() = entries.toTypedArray()
}
