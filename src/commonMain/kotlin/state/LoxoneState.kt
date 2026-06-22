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
 * Reads are plain, non-suspending and lock-free. The backing map is immutable and replaced
 * atomically on each update (copy-on-write), so readers always observe a consistent snapshot.
 *
 * This type is exported to JavaScript: a JS/TS consumer can read live values directly
 * (e.g. from a React dashboard) via [getValue]/[getText]/[get] without any wrapper. The
 * less common [getDaytimer]/[getWeather]/[getTyped] accessors live as Kotlin-only extension
 * functions because their payload types are not JS-exportable.
 *
 * ## Threading / single-writer assumption
 *
 * Updates are **not** synchronized against each other. This is safe because the store has a
 * single writer: [collectFrom] is expected to be launched exactly once per instance. Driving
 * a single instance from two concurrent collectors (or racing [clear] against an active
 * collector) can lose updates and is unsupported. On Kotlin/JS this is moot (single threaded);
 * the [Volatile] annotation guarantees cross-thread visibility of the latest snapshot on the JVM.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val state = LoxoneState()
 *
 * scope.launch(Dispatchers.IO) {
 *     state.collectFrom(wsClient.events)
 * }
 *
 * // Safe to call from any coroutine context (e.g., Main thread)
 * val temperature = state.getValue("uuid-of-temp-sensor")
 * ```
 *
 * @see collectFrom
 * @see StateValue
 */
@JsExport
class LoxoneState {

    @Volatile
    private var states: Map<String, StateValue> = emptyMap()

    /**
     * Get the raw state value for a UUID.
     *
     * @param uuid UUID of the state
     * @return The state value or null if not found
     */
    operator fun get(uuid: String): StateValue? = states[uuid]

    /**
     * Get the numeric value for a UUID.
     *
     * @param uuid UUID of the state
     * @return The value or null if not found or not a value state
     */
    fun getValue(uuid: String): Double? = (states[uuid] as? ValueState)?.value

    /**
     * Get the text state for a UUID.
     *
     * @param uuid UUID of the state
     * @return The text state or null if not found or not a text state
     */
    fun getText(uuid: String): TextState? = states[uuid] as? TextState

    /**
     * Get all stored state UUIDs.
     *
     * @return Array of all UUIDs currently in the state store
     */
    fun getAllUuids(): Array<String> = states.keys.toTypedArray()

    /**
     * Get the number of states currently stored.
     *
     * @return Number of stored states
     */
    fun size(): Int = states.size

    /**
     * Check if a state exists for the given UUID.
     *
     * @param uuid UUID to check
     * @return true if a state exists
     */
    fun contains(uuid: String): Boolean = states.containsKey(uuid)

    /**
     * Update the state from a Loxone event.
     *
     * This is called internally by [collectFrom]. See the single-writer note on [LoxoneState].
     *
     * @param event The event to process
     */
    internal fun update(event: LoxoneEvent) {
        val state = when (event) {
            is ValueEvent -> ValueState(event.value)
            is TextEvent -> TextState(event.text, event.iconUuid)
            is DaytimerEvent -> DaytimerState(event.defaultValue, event.entries)
            is WeatherEvent -> WeatherState(event.lastUpdate, event.entries)
        }
        states = states + (event.uuid to state)
    }

    /**
     * Clear all stored states.
     */
    fun clear() {
        states = emptyMap()
    }
}

/**
 * Get the daytimer state for a UUID.
 *
 * Kotlin-only accessor: [DaytimerState] is not exported to JS, so this is kept off the exported
 * [LoxoneState] surface as an extension function.
 *
 * @param uuid UUID of the state
 * @return The daytimer state or null if not found or not a daytimer state
 */
fun LoxoneState.getDaytimer(uuid: String): DaytimerState? = this[uuid] as? DaytimerState

/**
 * Get the weather state for a UUID.
 *
 * Kotlin-only accessor: [WeatherState] is not exported to JS, so this is kept off the exported
 * [LoxoneState] surface as an extension function.
 *
 * @param uuid UUID of the state
 * @return The weather state or null if not found or not a weather state
 */
fun LoxoneState.getWeather(uuid: String): WeatherState? = this[uuid] as? WeatherState

/**
 * Get a typed state value for a UUID.
 *
 * Kotlin-only accessor: reified generics are not JS-exportable, so this is kept off the exported
 * [LoxoneState] surface as an inline extension function.
 *
 * @param T Type of state value expected
 * @param uuid UUID of the state
 * @return The typed state or null if not found or wrong type
 */
inline fun <reified T : StateValue> LoxoneState.getTyped(uuid: String): T? = this[uuid] as? T
