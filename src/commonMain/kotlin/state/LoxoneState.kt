package cz.smarteon.loxkt.state

import cz.smarteon.loxkt.event.DaytimerEvent
import cz.smarteon.loxkt.event.LoxoneEvent
import cz.smarteon.loxkt.event.TextEvent
import cz.smarteon.loxkt.event.ValueEvent
import cz.smarteon.loxkt.event.WeatherEvent
import kotlin.concurrent.Volatile
import kotlin.js.JsExport

/**
 * Stores the latest state value per UUID from Loxone events.
 *
 * Reads are plain, non-suspending and lock-free. The backing map is replaced atomically on each
 * update (copy-on-write), so readers always observe a consistent snapshot.
 *
 * This type is exported to JavaScript: a JS/TS consumer can read live values directly
 * (e.g. from a React dashboard) via [getValue]/[getText]/[get] without any wrapper.
 *
 * ## Threading / single-writer assumption
 *
 * Updates are **not** synchronized against each other. This is safe because the store has a
 * single writer: [collectFrom] is expected to be launched exactly once per instance. The
 * [Volatile] annotation guarantees cross-thread visibility of the latest snapshot on the JVM.
 *
 * @see collectFrom
 * @see StateValue
 */
@JsExport
class LoxoneState {

    @Volatile
    private var states: Map<String, StateValue> = emptyMap()

    operator fun get(uuid: String): StateValue? = states[uuid]

    fun getValue(uuid: String): Double? = (states[uuid] as? ValueState)?.value

    fun getText(uuid: String): TextState? = states[uuid] as? TextState

    fun getDaytimer(uuid: String): DaytimerState? = states[uuid] as? DaytimerState

    fun getWeather(uuid: String): WeatherState? = states[uuid] as? WeatherState

    fun getAllUuids(): Array<String> = states.keys.toTypedArray()

    fun size(): Int = states.size

    fun contains(uuid: String): Boolean = states.containsKey(uuid)

    internal fun update(event: LoxoneEvent) {
        val state = when (event) {
            is ValueEvent -> ValueState(event.value)
            is TextEvent -> TextState(event.text, event.iconUuid)
            is DaytimerEvent -> DaytimerState(event.defaultValue, event.entries)
            is WeatherEvent -> WeatherState(event.lastUpdate, event.entries)
        }
        states = states + (event.uuid to state)
    }

    fun clear() {
        states = emptyMap()
    }
}

/**
 * Returns a typed state value, or null if the UUID is absent or has a different type.
 * Kotlin-only: reified generics are not JS-exportable.
 */
inline fun <reified T : StateValue> LoxoneState.getTyped(uuid: String): T? = this[uuid] as? T
