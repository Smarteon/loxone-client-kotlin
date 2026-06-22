package cz.smarteon.loxkt.app

import cz.smarteon.loxkt.state.DaytimerState
import cz.smarteon.loxkt.state.LoxoneState
import cz.smarteon.loxkt.state.StateValue
import cz.smarteon.loxkt.state.TextState
import cz.smarteon.loxkt.state.WeatherState
import cz.smarteon.loxkt.state.getDaytimer
import cz.smarteon.loxkt.state.getWeather

/**
 * Get a numeric value for a named state from the given [LoxoneState].
 *
 * @param state The state store to lookup values from
 * @param stateName Name of the state (e.g., "tempActual", "value", "position")
 * @return The numeric value or null if state not found or not a value state
 */
suspend fun Control.getValue(state: LoxoneState, stateName: String): Double? {
    val uuid = getState(stateName) ?: return null
    return state.getValue(uuid)
}

/**
 * Get a text state for a named state from the given [LoxoneState].
 *
 * @param state The state store to lookup values from
 * @param stateName Name of the state
 * @return The text state or null if state not found or not a text state
 */
suspend fun Control.getText(state: LoxoneState, stateName: String): TextState? {
    val uuid = getState(stateName) ?: return null
    return state.getText(uuid)
}

/**
 * Get a daytimer state for a named state from the given [LoxoneState].
 *
 * @param state The state store to lookup values from
 * @param stateName Name of the state
 * @return The daytimer state or null if state not found or not a daytimer state
 */
suspend fun Control.getDaytimer(state: LoxoneState, stateName: String): DaytimerState? {
    val uuid = getState(stateName) ?: return null
    return state.getDaytimer(uuid)
}

/**
 * Get a weather state for a named state from the given [LoxoneState].
 *
 * @param state The state store to lookup values from
 * @param stateName Name of the state
 * @return The weather state or null if state not found or not a weather state
 */
suspend fun Control.getWeather(state: LoxoneState, stateName: String): WeatherState? {
    val uuid = getState(stateName) ?: return null
    return state.getWeather(uuid)
}

/**
 * Get a raw state value for a named state from the given [LoxoneState].
 *
 * @param state The state store to lookup values from
 * @param stateName Name of the state
 * @return The state value or null if not found
 */
suspend fun Control.getStateValue(state: LoxoneState, stateName: String): StateValue? {
    val uuid = getState(stateName) ?: return null
    return state[uuid]
}

/**
 * Get all state values for this control from the given [LoxoneState].
 *
 * @param state The state store to lookup values from
 * @return Map of state name to state value for all states that have values
 */
suspend fun Control.getAllValues(state: LoxoneState): Map<String, StateValue> {
    val result = mutableMapOf<String, StateValue>()
    for ((name, uuid) in states.orEmpty()) {
        state[uuid]?.let { result[name] = it }
    }
    return result
}
