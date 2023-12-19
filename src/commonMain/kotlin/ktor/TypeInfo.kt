package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.LoxoneResponse
import io.ktor.util.reflect.*
import kotlin.reflect.KClass

internal val KClass<out LoxoneResponse>.typeInfo: TypeInfo
    get() = when (this) {
        else -> error("Unknown LoxoneResponse")
    }
