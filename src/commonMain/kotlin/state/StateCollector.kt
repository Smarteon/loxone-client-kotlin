package cz.smarteon.loxkt.state

import cz.smarteon.loxkt.event.LoxoneEvent
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.produceIn

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
suspend fun LoxoneState.collectFrom(events: Flow<LoxoneEvent>): Unit = coroutineScope {
    val channel = events.produceIn(this)
    try {
        for (first in channel) {
            updateAll(channel.drainBatch(first))
        }
    } finally {
        channel.cancel()
    }
}

/**
 * Caps how many events a single [drainBatch] call accumulates. Without a cap, a sustained
 * high-frequency stream could keep [ReceiveChannel.tryReceive] succeeding indefinitely, so the
 * batch would never be handed off - starving consumers and, on single-threaded targets like JS,
 * monopolizing the dispatcher since [ReceiveChannel.tryReceive] never suspends.
 */
internal const val MAX_EVENT_BATCH_SIZE = 256

/**
 * Returns [first] together with up to [MAX_EVENT_BATCH_SIZE] - 1 further events already buffered
 * on the channel, draining them without suspending.
 */
internal fun ReceiveChannel<LoxoneEvent>.drainBatch(first: LoxoneEvent): List<LoxoneEvent> {
    val batch = ArrayList<LoxoneEvent>()
    batch.add(first)
    while (batch.size < MAX_EVENT_BATCH_SIZE) {
        batch.add(tryReceive().getOrNull() ?: break)
    }
    return batch
}
