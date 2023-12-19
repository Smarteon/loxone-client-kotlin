package cz.smarteon.loxone

import kotlin.reflect.KClass

interface LoxoneClient {

    val profile: LoxoneProfile

    suspend fun <R : LoxoneResponse> call(command: String, responseType: KClass<out R>): R

    // can't use @JvmOverloads on interface
    @Suppress("UNCHECKED_CAST")
    suspend fun <R : LoxoneResponse> call(command: String): R = call(command, RawLoxoneResponse::class as KClass<out R>)

    fun close()

}
