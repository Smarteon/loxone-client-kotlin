package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.LoxoneResponse
import cz.smarteon.loxone.message.LoxoneMessage
import io.ktor.util.reflect.*
import kotlin.reflect.KClass

internal val KClass<out LoxoneResponse>.typeInfo: TypeInfo
    get() = when (this) {
        LoxoneMessage::class -> typeInfo<LoxoneMessage>()
        else -> error("Unknown LoxoneResponse")
    }
