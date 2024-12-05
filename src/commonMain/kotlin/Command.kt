package cz.smarteon.loxkt

import cz.smarteon.loxkt.message.LoxoneMsg
import cz.smarteon.loxkt.message.LoxoneMsgVal
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

interface Command<out RESPONSE : LoxoneResponse> {

    val pathSegments: List<String>

    val responseType: KClass<out RESPONSE>

    val authenticated: Boolean
}

/**
 * Skeleton for commands which have no response from Loxone miniserver.
 */
abstract class NoResponseCommand @JvmOverloads constructor(
    override val pathSegments: List<String>,
    override val authenticated: Boolean = false
) : Command<Nothing> {

    @JvmOverloads
    constructor(vararg pathSegments: String, authenticated: Boolean = false) : this(
        pathSegments.toList(),
        authenticated
    )

    override val responseType
        get() = Nothing::class
}

interface LoxoneMsgCommand<out VALUE : LoxoneMsgVal> : Command<LoxoneMsg> {
    override val responseType
        get() = LoxoneMsg::class

    val expectedCode: String
        get() = LoxoneMsg.CODE_OK

    val valueType: KClass<out VALUE>
}

interface CommandSupplier<out R : LoxoneResponse, out C : Command<R>> {
    val command: C
}
