package cz.smarteon.loxone

import kotlin.reflect.KClass

interface LoxoneClient {

    val profile: LoxoneProfile

    suspend fun <RESPONSE : LoxoneResponse> call(command: Command<RESPONSE>): RESPONSE

    suspend fun callRaw(command: String): String

    fun close()

}
