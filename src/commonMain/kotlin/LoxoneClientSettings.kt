package cz.smarteon.loxkt

import cz.smarteon.loxkt.message.TokenPermission
import kotlin.jvm.JvmOverloads

data class LoxoneClientSettings @JvmOverloads constructor(
    val tokenPermission: TokenPermission = TokenPermission.APP,
    val clientId: String = DEFAULT_CLIENT_ID,
    val clientInfo: String = DEFAULT_CLIENT_INFO,
    val killTokenOnClose: Boolean = true
) {
    companion object {
        const val DEFAULT_CLIENT_ID = "df184362-73fc-5d3e-ab0ec7c1c3e5bb2e"
        const val DEFAULT_CLIENT_INFO = "loxoneKotlin"
    }
}
