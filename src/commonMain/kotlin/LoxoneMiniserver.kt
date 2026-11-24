package cz.smarteon.loxkt

import cz.smarteon.loxkt.app.LoxoneApp
import cz.smarteon.loxkt.event.LoxoneEvent
import cz.smarteon.loxkt.ktor.KtorHttpLoxoneClient
import cz.smarteon.loxkt.ktor.KtorWebsocketLoxoneClient
import cz.smarteon.loxkt.state.LoxoneState
import cz.smarteon.loxkt.state.collectFrom
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlin.jvm.JvmOverloads

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
 * Use the JS-exported subclass, which appears in the generated `.d.mts` as `LoxoneMiniserver`:
 * ```ts
 * const miniserver = new LoxoneMiniserver("https://192.168.1.100", "user", "pass")
 * miniserver.onValueChanged((uuid, value) => console.log(uuid, value))
 * await miniserver.connect()
 * const app = await miniserver.loadStructure()
 * ```
 *
 * @param settings Token and client settings, e.g. the token permission (which sets its lifespan).
 */
open class LoxoneMiniserver @JvmOverloads constructor(
    val endpoint: LoxoneEndpoint,
    user: String,
    password: String,
    settings: LoxoneClientSettings = LoxoneClientSettings(),
) {

    @JvmOverloads
    constructor(url: String, user: String, password: String, settings: LoxoneClientSettings = LoxoneClientSettings())
        : this(LoxoneEndpoint.fromUrl(url), user, password, settings)

    private val authenticator = LoxoneTokenAuthenticator(
        LoxoneProfile(endpoint, LoxoneCredentials(user, password)),
        settings = settings,
    )

    protected val ws = KtorWebsocketLoxoneClient(endpoint, authenticator)
    protected val http = KtorHttpLoxoneClient(endpoint, LoxoneAuth.Token(authenticator))

    /** Live state store — updated continuously after [connect]. */
    open val state = LoxoneState()

    /**
     * Flow of raw events from the Miniserver WebSocket.
     * Use [state] or the JS callback API (`onValueChanged` / `onTextChanged`) instead of
     * collecting this directly from TypeScript — [SharedFlow] is not JS-exportable.
     */
    val events: SharedFlow<LoxoneEvent>
        get() = ws.events

    /**
     * Emits when the connection is lost without [close] having been called (Miniserver restart,
     * network drop, missed keepalive). Live updates have stopped by then and this instance can't
     * resume them: close it and create a new [LoxoneMiniserver]. May emit more than once per loss.
     */
    val disconnects: SharedFlow<Unit>
        get() = ws.sessionLost

    protected val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    protected var collectJob: Job? = null
    private var cachedApp: LoxoneApp? = null

    /** The connect in progress, so [close] can abort it. */
    private var connecting: Deferred<Unit>? = null

    /**
     * Opens the WebSocket session, authenticates, and enables binary status updates.
     * Idempotent: repeated calls return immediately if already connected.
     *
     * Event collection is subscribed **before** binary status updates are enabled: the Miniserver
     * sends the full initial state right after `enablebinstatusupdate`, and [events] has no replay,
     * so anything emitted before the subscription exists would be lost.
     *
     * The work runs in this instance's scope rather than the caller's, so [close] aborts it — a
     * caller that gives up on a slow connect and closes this instance would otherwise leave it
     * running, and the socket it eventually opens would never be closed. Cancelling the caller
     * aborts it too.
     */
    open suspend fun connect() {
        if (collectJob?.isActive == true) return

        val attempt = sessionScope.async { openSession() }
        connecting = attempt
        try {
            attempt.await()
        } finally {
            attempt.cancel()
            if (connecting === attempt) connecting = null
        }
    }

    private suspend fun openSession() {
        val subscribed = CompletableDeferred<Unit>()
        startEventCollection(ws.events.onSubscription { subscribed.complete(Unit) })
        collectJob?.invokeOnCompletion { cause ->
            subscribed.completeExceptionally(
                cause ?: IllegalStateException("Event collection ended before subscribing")
            )
        }

        var enabled = false
        try {
            subscribed.await()
            ws.callForMsg(LoxoneCommands.App.enableBinStatusUpdate())
            enabled = true
        } finally {
            if (!enabled) {
                collectJob?.cancel()
                collectJob = null
            }
        }
    }

    /**
     * Starts collecting [events] into [state]. Subclasses may override to add
     * additional per-event side effects (e.g. firing JS callbacks) in the same loop.
     * The launched [Job] should be stored in [collectJob] by the implementation, and it must
     * collect [events] itself (not [ws] events directly) so [connect] knows when it subscribed.
     */
    protected open fun startEventCollection(events: Flow<LoxoneEvent>) {
        collectJob = sessionScope.launch {
            state.collectFrom(events)
        }
    }

    /** Downloads (and caches) the Miniserver structure file. */
    open suspend fun loadStructure(): LoxoneApp =
        cachedApp ?: http.call(LoxoneCommands.App.get()).also { cachedApp = it }

    /**
     * Aborts a connect in progress, closes both transports and cancels all background work.
     * The instance can't be connected again afterwards; create a new one.
     */
    open suspend fun close() {
        // Joined, so the transports below are closed only once the attempt has stopped touching them.
        connecting?.cancelAndJoin()
        connecting = null
        collectJob?.cancel()
        collectJob = null
        cachedApp = null
        ws.close()
        http.close()
        sessionScope.cancel("Miniserver closed")
    }
}
