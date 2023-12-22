package cz.smarteon.loxone.message

import kotlin.reflect.KClass

interface LoxoneMsgVal

internal val loxoneMsgValDecoders: MutableMap<KClass<out LoxoneMsgVal>, (String) -> String> = mutableMapOf()
