package cz.smarteon.loxone

import cz.smarteon.loxone.Codec.loxJson
import cz.smarteon.loxone.message.EmptyLoxoneMsgVal
import cz.smarteon.loxone.message.LoxoneMsgVal

interface LoxoneClient {

    suspend fun <RESPONSE : LoxoneResponse> call(command: Command<RESPONSE>): RESPONSE

    suspend fun callRaw(command: String): String

    suspend fun close()
}

interface HttpLoxoneClient : LoxoneClient
interface WebsocketLoxoneClient : LoxoneClient

suspend inline fun <reified VAL : LoxoneMsgVal> LoxoneClient.callForMsg(command: LoxoneMsgCommand<VAL>): VAL {
    val msg = call(command)
    return if (msg.code == command.expectedCode) {
        if (VAL::class == EmptyLoxoneMsgVal::class) {
            EmptyLoxoneMsgVal as VAL
        } else {
            loxJson.decodeFromString<VAL>(msg.valueForDecoding(VAL::class))
        }
    } else {
        error("TODO")
    }
}
