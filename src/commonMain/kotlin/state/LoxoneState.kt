package cz.smarteon.loxkt.state

import cz.smarteon.loxkt.event.DaytimerEvent
import cz.smarteon.loxkt.event.LoxoneEvent
import cz.smarteon.loxkt.event.TextEvent
import cz.smarteon.loxkt.event.ValueEvent
import cz.smarteon.loxkt.event.WeatherEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Stores the latest state value per UUID from Loxone events.
 *
 * All accessor methods are suspending functions due to the internal locking.
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
class LoxoneState {

    private val mutex = Mutex()

    private val states = mutableMapOf<String, StateValue>()

    /**
     * Get the raw state value for a UUID.
     *
     * @param uuid UUID of the state
     * @return The state value or null if not found
     */
    suspend operator fun get(uuid: String): StateValue? = mutex.withLock { states[uuid] }

    /**
     * Get the numeric value for a UUID.
     *
     * @param uuid UUID of the state
     * @return The value or null if not found or not a value state
     */
    suspend fun getValue(uuid: String): Double? = mutex.withLock { (states[uuid] as? ValueState)?.value }

    /**
     * Get the text state for a UUID.
     *
     * @param uuid UUID of the state
     * @return The text state or null if not found or not a text state
     */
    suspend fun getText(uuid: String): TextState? = mutex.withLock { states[uuid] as? TextState }

    /**
     * Get the daytimer state for a UUID.
     *
     * @param uuid UUID of the state
     * @return The daytimer state or null if not found or not a daytimer state
     */
    suspend fun getDaytimer(uuid: String): DaytimerState? = mutex.withLock { states[uuid] as? DaytimerState }

    /**
     * Get the weather state for a UUID.
     *
     * @param uuid UUID of the state
     * @return The weather state or null if not found or not a weather state
     */
    suspend fun getWeather(uuid: String): WeatherState? = mutex.withLock { states[uuid] as? WeatherState }

    /**
     * Get a typed state value for a UUID.
     *
     * @param T Type of state value expected
     * @param uuid UUID of the state
     * @return The typed state or null if not found or wrong type
     */
    suspend inline fun <reified T : StateValue> getTyped(uuid: String): T? = get(uuid) as? T

    /**
     * Get all stored state UUIDs.
     *
     * @return Set of all UUIDs currently in the state store
     */
    suspend fun getAllUuids(): Set<String> = mutex.withLock { states.keys.toSet() }

    /**
     * Get the number of states currently stored.
     *
     * @return Number of stored states
     */
    suspend fun size(): Int = mutex.withLock { states.size }

    /**
     * Check if a state exists for the given UUID.
     *
     * @param uuid UUID to check
     * @return true if a state exists
     */
    suspend fun contains(uuid: String): Boolean = mutex.withLock { states.containsKey(uuid) }

    /**
     * Update the state from a Loxone event.
     *
     * This is called internally by [collectFrom].
     *
     * @param event The event to process
     */
    internal suspend fun update(event: LoxoneEvent) {
        val state = when (event) {
            is ValueEvent -> ValueState(event.value)
            is TextEvent -> TextState(event.text, event.iconUuid)
            is DaytimerEvent -> DaytimerState(event.defaultValue, event.entries)
            is WeatherEvent -> WeatherState(event.lastUpdate, event.entries)
        }
        mutex.withLock { states[event.uuid] = state }
    }

    /**
     * Clear all stored states.
     */
    suspend fun clear() {
        mutex.withLock { states.clear() }
    }
}
