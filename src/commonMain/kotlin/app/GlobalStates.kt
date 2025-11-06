package cz.smarteon.loxkt.app

import kotlinx.serialization.Serializable

/**
 * Global states that affect the whole Miniserver.
 * The UUIDs here can be used to lookup the corresponding state values that arrive
 * with all the other controls state updates through websocket.
 *
 * @property sunrise UUID for seconds since midnight when sun rises
 * @property sunset UUID for seconds since midnight when sun sets
 * @property favColorSequences UUID for array of favorite color sequences
 * @property favColors UUID for array of favorite colors
 * @property notifications UUID for push notifications
 * @property miniserverTime UUID for current Miniserver date/time
 * @property liveSearch UUID for device learning information
 * @property modifications UUID for structural changes made via API
 */
@Serializable
data class GlobalStates(
    val operatingMode: String? = null,
    val sunrise: String? = null,
    val sunset: String? = null,
    val pastTasks: String? = null,
    val plannedTasks: String? = null,
    val notifications: String? = null,
    val modifications: String? = null,
    val favColorSequences: String? = null,
    val favColors: String? = null,
    val miniserverTime: String? = null,
    val liveSearch: String? = null,
    val userSettings: String? = null,
    val userSettingsTs: String? = null,
    val cloudservice: String? = null,
    val trustVersion: String? = null,
    val propsVersion: String? = null,
    val hasInternet: String? = null,
)
