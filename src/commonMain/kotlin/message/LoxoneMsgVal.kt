package cz.smarteon.loxkt.message

import kotlin.reflect.KClass

interface LoxoneMsgVal

object EmptyLoxoneMsgVal : LoxoneMsgVal

internal val loxoneMsgValDecoders: MutableMap<KClass<out LoxoneMsgVal>, (String) -> String> = mutableMapOf()
