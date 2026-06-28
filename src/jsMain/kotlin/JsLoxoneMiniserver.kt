package cz.smarteon.loxkt

import cz.smarteon.loxkt.app.LoxoneApp
import cz.smarteon.loxkt.event.TextEvent
import cz.smarteon.loxkt.event.ValueEvent
import cz.smarteon.loxkt.state.LoxoneState
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.Promise

/**
 * JS/TypeScript entry point to a Loxone Miniserver. Exported as `LoxoneMiniserver` in the
 * generated `.d.mts`.
 *
 * All lifecycle methods are exposed as Promise-returning functions (no suspend) so TypeScript
 * sees them directly. Suspend functions from the Kotlin base class are wrapped here explicitly
 * because Kotlin/JS does not include inherited members of non-exported base classes in the
 * generated TypeScript declarations.
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
class JsLoxoneMiniserver(endpoint: LoxoneEndpoint, user: String, password: String)
    : LoxoneMiniserver(endpoint, user, password) {

    @JsName("fromUrl")
    constructor(url: String, user: String, password: String)
        : this(LoxoneEndpoint.fromUrl(url), user, password)

    // Expose state and lifecycle methods explicitly so they appear in the TypeScript declaration.
    // Kotlin/JS does not include inherited members of non-exported base classes in .d.mts.
    // For suspend funs: @JsName renames wrappers to idiomatic JS names; Kotlin-level names avoid
    //   conflicts with the inherited suspend methods (distinguished by continuation argument).
    // For `state`: a fresh @JsName alias is needed since override properties are also excluded.
    @JsName("state")
    val jsState: LoxoneState get() = super.state

    @JsName("connect")
    fun connectAsync(): Promise<Unit> = sessionScope.promise { super.connect() }

    @JsName("loadStructure")
    fun loadStructureAsync(): Promise<LoxoneApp> = sessionScope.promise { super.loadStructure() }

    @JsName("close")
    fun closeAsync(): Promise<Unit> = sessionScope.promise { super.close() }

    private val valueListeners = mutableListOf<(String, Double) -> Unit>()
    private val textListeners = mutableListOf<(String, String) -> Unit>()

    override fun startEventCollection() {
        collectJob = sessionScope.launch {
            ws.events.collect { event ->
                state.update(event)
                when (event) {
                    is ValueEvent -> valueListeners.forEach { it(event.uuid, event.value) }
                    is TextEvent -> textListeners.forEach { it(event.uuid, event.text) }
                    else -> Unit
                }
            }
        }
    }

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
}
