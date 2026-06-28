package cz.smarteon.loxkt

import cz.smarteon.loxkt.app.LoxoneApp
import cz.smarteon.loxkt.event.LoxoneEvent
import cz.smarteon.loxkt.ktor.KtorHttpLoxoneClient
import cz.smarteon.loxkt.ktor.KtorWebsocketLoxoneClient
import cz.smarteon.loxkt.state.LoxoneState
import cz.smarteon.loxkt.state.collectFrom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

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
 */
open class LoxoneMiniserver(val endpoint: LoxoneEndpoint, user: String, password: String) {

    constructor(url: String, user: String, password: String)
        : this(LoxoneEndpoint.fromUrl(url), user, password)

    private val authenticator = LoxoneTokenAuthenticator(
        LoxoneProfile(endpoint, LoxoneCredentials(user, password))
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

    protected val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    protected var collectJob: Job? = null
    private var cachedApp: LoxoneApp? = null

    /**
     * Opens the WebSocket session, authenticates, and enables binary status updates.
     * Idempotent: repeated calls return immediately if already connected.
     */
    open suspend fun connect() {
        if (collectJob?.isActive == true) return
        ws.callForMsg(LoxoneCommands.App.enableBinStatusUpdate())
        startEventCollection()
    }

    /**
     * Starts collecting WebSocket events into [state]. Subclasses may override to add
     * additional per-event side effects (e.g. firing JS callbacks) in the same loop.
     * The launched [Job] should be stored in [collectJob] by the implementation.
     */
    protected open fun startEventCollection() {
        collectJob = sessionScope.launch {
            state.collectFrom(ws.events)
        }
    }

    /** Downloads (and caches) the Miniserver structure file. */
    open suspend fun loadStructure(): LoxoneApp =
        cachedApp ?: http.call(LoxoneCommands.App.get()).also { cachedApp = it }

    /** Closes both transports and cancels all background collection. */
    open suspend fun close() {
        collectJob?.cancel()
        collectJob = null
        cachedApp = null
        ws.close()
        http.close()
    }
}
