package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.LoxoneMsgCommand
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

data class SimpleLoxoneMsgCommand<out VAL : LoxoneMsgVal> @JvmOverloads constructor(
    override val pathSegments: List<String>,
    override val valueType: KClass<out VAL>,
    override val authenticated: Boolean = true,
    override val expectedCode: String = LoxoneMsg.CODE_OK
) : LoxoneMsgCommand<VAL>

internal inline fun <reified VAL : LoxoneMsgVal> cfgCommand(path: String): LoxoneMsgCommand<VAL> =
    SimpleLoxoneMsgCommand(listOf("jdev", "cfg", path), VAL::class, false)

internal inline fun <reified VAL : LoxoneMsgVal> sysCommand(
    vararg paths: String,
    authenticated: Boolean = true,
    expectedCode: String = LoxoneMsg.CODE_OK
): LoxoneMsgCommand<VAL> =
    SimpleLoxoneMsgCommand(listOf("jdev", "sys") + paths, VAL::class, authenticated, expectedCode)
