package cz.smarteon.loxone.message

import cz.smarteon.loxone.LoxoneMsgCommand
import kotlin.reflect.KClass

data class SimpleLoxoneMsgCommand<out VAL : LoxoneMsgVal>(
    override val pathSegments: List<String>,
    override val valueType: KClass<out VAL>
) : LoxoneMsgCommand<VAL>

val apiInfoCmd = SimpleLoxoneMsgCommand(listOf("jdev", "cfg", "api"), ApiInfo::class)
