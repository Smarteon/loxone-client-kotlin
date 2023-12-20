@file:OptIn(ExperimentalSerializationApi::class)

package cz.smarteon.loxone.message

import cz.smarteon.loxone.LoxoneResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames


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
}

interface LoxoneMsgVal
