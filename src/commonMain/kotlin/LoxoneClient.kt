package cz.smarteon.loxone

import cz.smarteon.loxone.Codec.loxJson
import cz.smarteon.loxone.message.LoxoneMsg.Companion.CODE_OK
import cz.smarteon.loxone.message.LoxoneMsgVal

interface LoxoneClient {

    suspend fun <RESPONSE : LoxoneResponse> call(command: Command<RESPONSE>): RESPONSE

    suspend fun callRaw(command: String): String

    suspend fun close()

}

suspend inline fun <reified VAL : LoxoneMsgVal> LoxoneClient.callForMsg(command: LoxoneMsgCommand<VAL>): VAL {
    val msg = call(command)
    return when (msg.code) {
        CODE_OK -> loxJson.decodeFromString<VAL>(msg.valueForDecoding(VAL::class))
        else -> error("TODO")
    }
}
