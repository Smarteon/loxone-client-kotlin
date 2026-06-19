package cz.smarteon.loxkt.js

import cz.smarteon.loxkt.LoxoneAuth
import cz.smarteon.loxkt.LoxoneCommands
import cz.smarteon.loxkt.LoxoneCredentials
import cz.smarteon.loxkt.LoxoneEndpoint
import cz.smarteon.loxkt.LoxoneProfile
import cz.smarteon.loxkt.LoxoneTokenAuthenticator
import cz.smarteon.loxkt.app.LoxoneApp
import cz.smarteon.loxkt.callForMsg
import cz.smarteon.loxkt.event.LoxoneEvent
import cz.smarteon.loxkt.event.TextEvent
import cz.smarteon.loxkt.event.ValueEvent
import cz.smarteon.loxkt.ktor.KtorHttpLoxoneClient
import cz.smarteon.loxkt.ktor.KtorWebsocketLoxoneClient
import cz.smarteon.loxkt.state.LoxoneState
import cz.smarteon.loxkt.state.TextState
import cz.smarteon.loxkt.state.ValueState
import cz.smarteon.loxkt.state.collectFrom
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * JS-friendly representation of a single named state of a [LoxControl].
 *
 * @property name State name as defined in the Miniserver structure (e.g. `actual`, `value`).
 * @property uuid UUID used to look up the live value via [LoxoneJsClient.getValue]/[LoxoneJsClient.getText].
 */
@JsExport
class LoxState(val name: String, val uuid: String)

/**
 * JS-friendly representation of a control from the Miniserver structure (`LoxAPP3.json`).
 *
 * @property name Display name of the control.
 * @property type Control type identifier (e.g. `Switch`, `InfoOnlyAnalog`). Empty string means not visualized.
 * @property room Resolved room name, or `null` if the control has no room.
 * @property category Resolved category name, or `null` if the control has no category.
 * @property states All named states of the control with their UUIDs.
 */
@JsExport
class LoxControl(
    val name: String,
    val type: String,
    val room: String?,
    val category: String?,
    val states: Array<LoxState>
)

/**
 * Current live value of a single state, keyed by [uuid].
 * Exactly one of [value] / [text] is set depending on the state kind.
 *
 * @property uuid UUID of the state.
 * @property value Numeric value for analog states, or `null`.
 * @property text Text value for text states, or `null`.
 */
@JsExport
class LoxStateValue(
    val uuid: String,
    val value: Double?,
    val text: String?
)

/**
 * Browser/JS facade over the Loxone client.
 *
 * Connects to a Miniserver over WebSocket for live events and over HTTP for the structure file,
 * exposing a small Promise/callback based API suitable for TypeScript/JavaScript consumers.
 *
 * Typical usage:
 * ```js
 * const client = new LoxoneJsClient(url, user, password)
 * client.onValueChanged((uuid, value) => { ... })
 * await client.connect()
 * const controls = await client.loadStructure()
 * const values = await client.getValues()
 * ```
 *
 * @param url Base Miniserver URL (already resolved host, e.g. `https://host:port`), without a trailing command path.
 * @param user Loxone user name.
 * @param password Loxone password.
 */
@JsExport
class LoxoneJsClient(url: String, user: String, password: String) {

    private val scope = MainScope()
    private val endpoint = LoxoneEndpoint.fromUrl(url)
    private val authenticator =
        LoxoneTokenAuthenticator(LoxoneProfile(endpoint, LoxoneCredentials(user, password)))
    private val ws = KtorWebsocketLoxoneClient(endpoint, authenticator)
    private val http = KtorHttpLoxoneClient(endpoint, LoxoneAuth.Token(authenticator))
    private val state = LoxoneState()

    private var cachedApp: LoxoneApp? = null
    private var connectPromise: Promise<Unit>? = null

    private var onValue: ((String, Double) -> Unit)? = null
    private var onText: ((String, String) -> Unit)? = null

    /**
     * Opens the WebSocket session, authenticates and enables binary status updates so that
     * live events start flowing into the internal state store and registered callbacks.
     *
     * Idempotent: repeated calls return the same in-flight/completed promise and never open
     * a second session (concurrent calls on a single session are not supported by the protocol).
     */
    fun connect(): Promise<Unit> = connectPromise ?: scope.promise {
        scope.launch { state.collectFrom(ws.events) }

        ws.events
            .onEach { ev: LoxoneEvent ->
                when (ev) {
                    is ValueEvent -> onValue?.invoke(ev.uuid, ev.value)
                    is TextEvent -> onText?.invoke(ev.uuid, ev.text)
                    else -> Unit
                }
            }
            .launchIn(scope)

        ws.callForMsg(LoxoneCommands.App.enableBinStatusUpdate())
        Unit
    }.also { connectPromise = it }

    /**
     * Downloads (and caches) the Miniserver structure and returns its controls in a flat,
     * JS-friendly form with rooms and categories resolved to names.
     */
    fun loadStructure(): Promise<Array<LoxControl>> = scope.promise {
        val app = ensureApp()
        app.controls.values.map { c ->
            LoxControl(
                name = c.name,
                type = c.type,
                room = c.room?.let { app.rooms[it]?.name },
                category = c.cat?.let { app.cats[it]?.name },
                states = (c.states ?: emptyMap())
                    .map { (n, u) -> LoxState(n, u) }
                    .toTypedArray()
            )
        }.toTypedArray()
    }

    /**
     * Returns a snapshot of all currently known state values keyed by UUID.
     * Only value and text states are included; states with no received value yet are omitted.
     */
    fun getValues(): Promise<Array<LoxStateValue>> = scope.promise {
        state.getAllUuids().mapNotNull { uuid ->
            when (val sv = state[uuid]) {
                is ValueState -> LoxStateValue(uuid, sv.value, null)
                is TextState -> LoxStateValue(uuid, null, sv.text)
                else -> null
            }
        }.toTypedArray()
    }

    /** Returns the current numeric value for [uuid], or `null` if absent or not a value state. */
    fun getValue(uuid: String): Promise<Double?> = scope.promise { state.getValue(uuid) }

    /** Returns the current text for [uuid], or `null` if absent or not a text state. */
    fun getText(uuid: String): Promise<String?> = scope.promise { state.getText(uuid)?.text }

    /** Registers a callback invoked for every numeric state update. Register before [connect]. */
    fun onValueChanged(cb: (uuid: String, value: Double) -> Unit) {
        onValue = cb
    }

    /** Registers a callback invoked for every text state update. Register before [connect]. */
    fun onTextChanged(cb: (uuid: String, text: String) -> Unit) {
        onText = cb
    }

    /** Closes both transports and cancels all background work. */
    fun close(): Promise<Unit> = scope.promise {
        ws.close()
        http.close()
    }

    private suspend fun ensureApp(): LoxoneApp =
        cachedApp ?: http.call(LoxoneCommands.App.get()).also { cachedApp = it }
}
