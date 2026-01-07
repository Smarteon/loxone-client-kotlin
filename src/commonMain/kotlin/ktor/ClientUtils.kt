package cz.smarteon.loxkt.ktor

import cz.smarteon.loxkt.LoxoneResponse
import cz.smarteon.loxkt.app.LoxoneApp
import cz.smarteon.loxkt.app.LoxoneAppVersion
import cz.smarteon.loxkt.message.LoxoneMsg
import io.ktor.util.reflect.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * Registry of known [LoxoneResponse] types with their [TypeInfo] for Ktor deserialization.
 * Add new response types here when they are introduced.
 */
private val responseTypeInfoRegistry: Map<KClass<out LoxoneResponse>, TypeInfo> = mapOf(
    LoxoneMsg::class to typeInfo<LoxoneMsg>(),
    LoxoneApp::class to typeInfo<LoxoneApp>(),
    LoxoneAppVersion::class to typeInfo<LoxoneAppVersion>(),
)

/**
 * Registry of known [LoxoneResponse] types with their [KSerializer] for kotlinx.serialization.
 * Add new response types here when they are introduced.
 */
private val responseSerializerRegistry: Map<KClass<out LoxoneResponse>, KSerializer<out LoxoneResponse>> = mapOf(
    LoxoneMsg::class to serializer<LoxoneMsg>(),
    LoxoneApp::class to serializer<LoxoneApp>(),
    LoxoneAppVersion::class to serializer<LoxoneAppVersion>(),
)

internal val KClass<out LoxoneResponse>.typeInfo: TypeInfo
    get() = responseTypeInfoRegistry[this]
        ?: error("Unknown LoxoneResponse type: $this. Register it in ClientUtils.kt responseTypeInfoRegistry.")

internal val KClass<out LoxoneResponse>.deserializer: KSerializer<out LoxoneResponse>
    get() = responseSerializerRegistry[this]
        ?: error("Unknown LoxoneResponse type: $this. Register it in ClientUtils.kt responseSerializerRegistry.")
