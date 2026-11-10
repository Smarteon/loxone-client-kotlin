package cz.smarteon.loxkt

import cz.smarteon.loxkt.app.LoxoneApp
import cz.smarteon.loxkt.event.LoxoneEvent
import cz.smarteon.loxkt.event.TextEvent
import cz.smarteon.loxkt.event.ValueEvent
import cz.smarteon.loxkt.state.LoxoneState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.promise
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.Promise

/**
 * JS/TypeScript entry point to a Loxone Miniserver. Exported as `LoxoneMiniserver` in the
 * generated `.d.mts`.
 *
 * Wraps a [LoxoneMiniserver] (composition, not inheritance): `@JsExport` classes cannot declare
 * `suspend fun` members and [kotlinx.coroutines.flow.SharedFlow] is not exportable, so the
 * lifecycle is exposed as Promise-returning functions and live events as callbacks.
 *
 * Typical usage:
 * ```ts
 * const miniserver = new LoxoneMiniserver(LoxoneEndpoint.local("192.168.1.100"), "user", "pass")
 * // or from a URL string:
 * const miniserver = LoxoneMiniserver.fromUrl("https://192.168.1.100", "user", "pass")
 * const unsubscribe = miniserver.onValueChanged((uuid, value) => {
 *   console.log(`${uuid}: ${value}`)
 * })
 * await miniserver.connect()
 * const app = await miniserver.loadStructure()
 * // later:
 * unsubscribe()
 * await miniserver.close()
 * ```
 */
@JsExport
@JsName("LoxoneMiniserver")
class JsLoxoneMiniserver(endpoint: LoxoneEndpoint, user: String, password: String) {

    @JsName("fromUrl")
    constructor(url: String, user: String, password: String)
        : this(LoxoneEndpoint.fromUrl(url), user, password)

    private val valueListeners = mutableListOf<(String, Double) -> Unit>()
    private val textListeners = mutableListOf<(String, String) -> Unit>()

    private val delegate = LoxoneMiniserver(endpoint, user, password) { batch -> dispatch(batch) }

    // Only runs the Promise wrappers below; the delegate owns the session and its collection.
    private val promiseScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Live state store — updated continuously after [connect]. */
    val state: LoxoneState get() = delegate.state

    @JsName("connect")
    fun connectAsync(): Promise<Unit> = promiseScope.promise { delegate.connect() }

    @JsName("loadStructure")
    fun loadStructureAsync(): Promise<LoxoneApp> = promiseScope.promise { delegate.loadStructure() }

    @JsName("close")
    fun closeAsync(): Promise<Unit> = promiseScope.promise { delegate.close() }

    /**
     * Registers a callback invoked for every numeric state update.
     * @return An unsubscribe function — call it to remove the listener.
     */
    fun onValueChanged(cb: (uuid: String, value: Double) -> Unit): () -> Unit {
        valueListeners += cb
        return { valueListeners -= cb }
    }

    /**
     * Registers a callback invoked for every text state update.
     * @return An unsubscribe function — call it to remove the listener.
     */
    fun onTextChanged(cb: (uuid: String, text: String) -> Unit): () -> Unit {
        textListeners += cb
        return { textListeners -= cb }
    }

    // Called by the delegate after the batch was applied to state, so listeners reading
    // state from inside the callback already see the new values.
    private fun dispatch(batch: List<LoxoneEvent>) {
        for (event in batch) {
            when (event) {
                is ValueEvent -> valueListeners.forEach { it(event.uuid, event.value) }
                is TextEvent -> textListeners.forEach { it(event.uuid, event.text) }
                else -> Unit
            }
        }
    }
}
