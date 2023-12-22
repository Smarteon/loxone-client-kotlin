package cz.smarteon.loxone.message

import cz.smarteon.loxone.LoxoneMsgCommand
import kotlin.reflect.KClass

data class SimpleLoxoneMsgCommand<out VAL : LoxoneMsgVal>(
    override val pathSegments: List<String>,
    override val valueType: KClass<out VAL>
) : LoxoneMsgCommand<VAL>

internal inline fun <reified VAL : LoxoneMsgVal> cfgCommand(path: String): LoxoneMsgCommand<VAL>
    = SimpleLoxoneMsgCommand(listOf("jdev", "cfg", path), VAL::class)
