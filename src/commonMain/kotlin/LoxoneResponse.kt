package cz.smarteon.loxone

import kotlin.jvm.JvmInline

interface LoxoneResponse

@JvmInline
value class RawLoxoneResponse(private val v: String) : LoxoneResponse

sealed interface LoxoneResponseType<out R : LoxoneResponse> {
    data object Raw : LoxoneResponseType<RawLoxoneResponse>
}
