package cz.smarteon.loxone

import cz.smarteon.loxone.message.LoxoneMsg
import cz.smarteon.loxone.message.LoxoneMsgVal
import kotlin.reflect.KClass

interface Command<out RESPONSE : LoxoneResponse> {

    val pathSegments: List<String>

    val responseType: KClass<out RESPONSE>
}

interface LoxoneMsgCommand<out VALUE : LoxoneMsgVal> : Command<LoxoneMsg> {
    override val responseType
        get() = LoxoneMsg::class

    val valueType: KClass<out VALUE>
}
