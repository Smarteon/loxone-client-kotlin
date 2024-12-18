@file:OptIn(ExperimentalSerializationApi::class)

package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.LoxoneResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlin.reflect.KClass

/**
 * Represents a message from Loxone Miniserver.
 * @property control control of the message, always without 'j' prefix,
 * even if it is a response to a JSON request (e.g. 'dev/cfg/api')
 * @property code status code of the message, e.g. '200' for OK
 * @property value value of the message in serialized form
 * - for instance in case of JSON it can be `"string"`, `12.3`, `true`, `{}`
 */
@Serializable
data class LoxoneMsg internal constructor(@SerialName("LL") private val content: Content) : LoxoneResponse {
    val control: String = content.control
    val code: String = content.code
    val value: String = content.value

    constructor(control: String, code: String, value: String) : this(Content(control, code, value))

    @Serializable
    data class Content(
        val control: String,
        @JsonNames("code", "Code") val code: String,
        @Serializable(with = ContentAsStringSerializer::class) val value: String,
    )

    fun valueForDecoding(cls: KClass<out LoxoneMsgVal>): String = loxoneMsgValDecoders[cls]?.invoke(value) ?: value

    companion object {
        const val CODE_OK = "200"
        const val CODE_AUTH_FAIL = "401"
        const val CODE_NOT_AUTHENTICATED = "400"
        const val CODE_NOT_FOUND = "404"
        const val CODE_AUTH_TOO_LONG = "420"
        const val CODE_UNAUTHORIZED = "500"
    }
}

internal class ContentAsStringSerializer : JsonTransformingSerializer<String>(String.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return JsonPrimitive(element.toString())
    }
}
