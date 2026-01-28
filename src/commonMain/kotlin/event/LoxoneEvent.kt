package cz.smarteon.loxkt.event

/**
 * Base interface for all Loxone WebSocket events.
 * Events are parsed from binary event tables received through the WebSocket connection.
 *
 * @see ValueEvent
 * @see TextEvent
 * @see DaytimerEvent
 * @see WeatherEvent
 */
sealed interface LoxoneEvent {
    /**
     * UUID of the state this event relates to.
     * Can be matched to control states in [cz.smarteon.loxkt.app.Control.states].
     */
    val uuid: String
}
