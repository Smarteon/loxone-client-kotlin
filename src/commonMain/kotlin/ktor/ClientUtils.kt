package cz.smarteon.loxkt.ktor

import cz.smarteon.loxkt.LoxoneResponse
import cz.smarteon.loxkt.message.LoxoneMsg
import io.ktor.util.reflect.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

internal val KClass<out LoxoneResponse>.typeInfo: TypeInfo
    get() = when (this) {
        LoxoneMsg::class -> typeInfo<LoxoneMsg>()
        else -> error("Unknown LoxoneResponse")
    }

internal val KClass<out LoxoneResponse>.deserializer: KSerializer<out LoxoneResponse>
    get() = when (this) {
        LoxoneMsg::class -> serializer<LoxoneMsg>()
        else -> error("Unknown LoxoneResponse")
    }
