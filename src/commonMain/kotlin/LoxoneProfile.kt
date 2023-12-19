package cz.smarteon.loxone

import kotlin.jvm.JvmOverloads

data class LoxoneEndpoint @JvmOverloads constructor(
    val host: String,
    val port: Int = 80,
    val useSsl: Boolean = true,
    val path: String = ""
)

data class LoxoneCredentials @JvmOverloads constructor(
    val username: String,
    val password: String,
    val visuPassword: String? = null
)

data class LoxoneProfile @JvmOverloads constructor(
    val endpoint: LoxoneEndpoint,
    val credentials: LoxoneCredentials? = null
)
