package cz.smarteon.loxkt.state

import cz.smarteon.loxkt.event.LoxoneEvent
import kotlinx.coroutines.flow.Flow

/**
 * Collects events from a flow into this [LoxoneState].
 *
 * This function suspends and continuously processes events from the flow,
 * updating the state store with the latest values.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val wsClient = KtorWebsocketLoxoneClient(endpoint, authenticator)
 * val state = LoxoneState()
 *
 * scope.launch(Dispatchers.IO) {
 *     state.collectFrom(wsClient.events)
 * }
 *
 * // Safe to read from any context
 * val temp = state.getValue("uuid")
 *
 * // Enable binary status updates to start receiving events
 * wsClient.callForMsg(LoxoneCommands.App.enableBinStatusUpdate())
 * ```
 *
 * ## Buffering
 *
 * For high-frequency event streams, buffering should be configured on the upstream
 * [SharedFlow][kotlinx.coroutines.flow.SharedFlow]. The WebSocket client already
 * provides appropriate buffering.
 *
 * @param events Flow of Loxone events to collect
 */
suspend fun LoxoneState.collectFrom(events: Flow<LoxoneEvent>) {
    events.collect { event ->
        update(event)
    }
}
