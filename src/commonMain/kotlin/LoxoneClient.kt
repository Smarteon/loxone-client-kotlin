package cz.smarteon.loxone

import kotlin.reflect.KClass

interface LoxoneClient {

    val profile: LoxoneProfile

    suspend fun <R : LoxoneResponse> call(command: String, responseType: KClass<out R>): R

    suspend fun callRaw(command: String): String

    fun close()

}
