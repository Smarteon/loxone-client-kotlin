package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.LoxoneResponse
import cz.smarteon.loxone.message.LoxoneMsg
import io.ktor.util.reflect.*
import kotlin.reflect.KClass

internal val KClass<out LoxoneResponse>.typeInfo: TypeInfo
    get() = when (this) {
        LoxoneMsg::class -> typeInfo<LoxoneMsg>()
        else -> error("Unknown LoxoneResponse")
    }
