package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.LoxoneResponse
import cz.smarteon.loxone.RawLoxoneResponse
import io.ktor.util.reflect.*
import kotlin.reflect.KClass

internal val KClass<out LoxoneResponse>.typeInfo: TypeInfo
    get() = when (this) {
        RawLoxoneResponse::class -> typeInfo<RawLoxoneResponse>()
        else -> error("Unknown LoxoneResponse")
    }
