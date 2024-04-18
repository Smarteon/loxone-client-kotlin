package cz.smarteon.loxone

import cz.smarteon.loxone.message.LoxoneMsgVal
import org.kodein.mock.ArgConstraint
import org.kodein.mock.ArgConstraintsBuilder

internal fun <V : LoxoneMsgVal> ArgConstraintsBuilder.isLoxMsgCmdContaining(vararg segments: String, capture: MutableList<LoxoneMsgCommand<V>>? = null): LoxoneMsgCommand<V> =
    isValid(ArgConstraint(capture, { "isLoxMsgCmdContaining $segments" }) {
        if (it.pathSegments.containsAll(segments.toList())) {
            ArgConstraint.Result.Success
        } else {
            ArgConstraint.Result.Failure { "Expected command containing $segments, but was ${it.pathSegments}" }
        }
    })
