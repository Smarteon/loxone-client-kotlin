package cz.smarteon.loxkt

import cz.smarteon.loxkt.app.LoxoneApp
import cz.smarteon.loxkt.event.LoxoneEvent
import cz.smarteon.loxkt.ktor.KtorHttpLoxoneClient
import cz.smarteon.loxkt.ktor.KtorWebsocketLoxoneClient
import cz.smarteon.loxkt.state.LoxoneState
import cz.smarteon.loxkt.state.collectBatches
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * High-level entry point to a Loxone Miniserver.
 *
 * Wires up WebSocket and HTTP clients with token authentication, manages the connection lifecycle,
 * and keeps a live [state] store updated from WebSocket events.
 *
 * ### Kotlin / JVM / native usage
 * ```kotlin
 * // By endpoint (recommended — encodes local/public semantics):
 * val miniserver = LoxoneMiniserver(LoxoneEndpoint.local("192.168.1.100"), "user", "pass")
 * // By URL:
 * val miniserver = LoxoneMiniserver("https://192.168.1.100", "user", "pass")
 * miniserver.connect()
 * val app = miniserver.loadStructure()
 * miniserver.events.collect { event -> /* react to live events */ }
 * ```
 *
 * ### TypeScript / JS usage
 * Use the JS-exported wrapper, which appears in the generated `.d.mts` as `LoxoneMiniserver`:
 * ```ts
 * const miniserver = new LoxoneMiniserver("https://192.168.1.100", "user", "pass")
 * miniserver.onValueChanged((uuid, value) => console.log(uuid, value))
 * await miniserver.connect()
 * const app = await miniserver.loadStructure()
 * ```
 *
 * @param onEventBatch Internal hook invoked with every batch of events right after it was applied
 * to [state]; used by the JS wrapper to fire its value/text callbacks from the same loop.
 */
class LoxoneMiniserver internal constructor(
    val endpoint: LoxoneEndpoint,
    private val ws: KtorWebsocketLoxoneClient,
    private val http: KtorHttpLoxoneClient,
    private val onEventBatch: (List<LoxoneEvent>) -> Unit = {},
) {

    constructor(endpoint: LoxoneEndpoint, user: String, password: String)
        : this(endpoint, user, password, onEventBatch = {})

    constructor(url: String, user: String, password: String)
        : this(LoxoneEndpoint.fromUrl(url), user, password)

    internal constructor(
        endpoint: LoxoneEndpoint,
        user: String,
        password: String,
        onEventBatch: (List<LoxoneEvent>) -> Unit,
    ) : this(
        endpoint,
        LoxoneTokenAuthenticator(LoxoneProfile(endpoint, LoxoneCredentials(user, password))),
        onEventBatch,
    )

    private constructor(
        endpoint: LoxoneEndpoint,
        authenticator: LoxoneTokenAuthenticator,
        onEventBatch: (List<LoxoneEvent>) -> Unit,
    ) : this(
        endpoint,
        KtorWebsocketLoxoneClient(endpoint, authenticator),
        KtorHttpLoxoneClient(endpoint, LoxoneAuth.Token(authenticator)),
        onEventBatch,
    )

    /** Live state store — updated continuously after [connect]. */
    val state = LoxoneState()

    /**
     * Flow of raw events from the Miniserver WebSocket.
     * Use [state] or the JS callback API (`onValueChanged` / `onTextChanged`) instead of
     * collecting this directly from TypeScript — [SharedFlow] is not JS-exportable.
     */
    val events: SharedFlow<LoxoneEvent>
        get() = ws.events

    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val connectMutex = Mutex()

    @Volatile
    private var collectJob: Job? = null
    private var cachedApp: LoxoneApp? = null

    /**
     * Opens the WebSocket session, authenticates, and enables binary status updates.
     * Idempotent: repeated calls return immediately if already connected. Concurrent calls are
     * serialized, so a second caller never returns before the first one has finished connecting.
     *
     * Event collection is subscribed **before** binary status updates are enabled: the Miniserver
     * sends the full initial state right after `enablebinstatusupdate`, and [events] has no replay,
     * so anything emitted before the subscription exists would be lost.
     *
     * @throws IllegalStateException when event collection ends before it subscribed, e.g. because
     * [close] was called concurrently.
     */
    suspend fun connect() {
        connectMutex.withLock { connectLocked() }
    }

    private suspend fun connectLocked() {
        if (collectJob?.isActive == true) return

        val subscribed = CompletableDeferred<Unit>()
        val job = sessionScope.launch {
            ws.events
                .onSubscription { subscribed.complete(Unit) }
                .collectBatches { batch ->
                    state.updateAll(batch)
                    onEventBatch(batch)
                }
        }
        // Wrapped rather than passed through: a CancellationException from a concurrent close()
        // would otherwise be rethrown by connect() and mistaken for the caller's own cancellation.
        job.invokeOnCompletion { cause ->
            subscribed.completeExceptionally(
                IllegalStateException("Event collection ended before subscribing", cause)
            )
        }
        collectJob = job

        var enabled = false
        try {
            subscribed.await()
            ws.callForMsg(LoxoneCommands.App.enableBinStatusUpdate())
            enabled = true
        } finally {
            if (!enabled) {
                job.cancel()
                if (collectJob === job) collectJob = null
            }
        }
    }

    /** Downloads (and caches) the Miniserver structure file. */
    suspend fun loadStructure(): LoxoneApp =
        cachedApp ?: http.call(LoxoneCommands.App.get()).also { cachedApp = it }

    /** Closes both transports and cancels all background collection. */
    suspend fun close() {
        collectJob?.cancel()
        collectJob = null
        cachedApp = null
        ws.close()
        http.close()
    }
}
