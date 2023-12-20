@file:OptIn(ExperimentalSerializationApi::class)

package cz.smarteon.loxone.message

import cz.smarteon.loxone.LoxoneResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlin.reflect.KClass


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
        val value: String,
    )

    fun valueForDecoding(cls: KClass<out LoxoneMsgVal>): String = when(cls) {
        ApiInfo::class -> ApiInfo.valueForDecoding(value)
        else -> value
    }

    companion object {
        const val CODE_OK = "200"
        const val CODE_AUTH_FAIL = "401"
        const val CODE_NOT_AUTHENTICATED = "400"
        const val CODE_NOT_FOUND = "404"
        const val CODE_AUTH_TOO_LONG = "420"
        const val CODE_UNAUTHORIZED = "500"
    }
}

interface LoxoneMsgVal
