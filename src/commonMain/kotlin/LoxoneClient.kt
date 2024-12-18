package cz.smarteon.loxkt

import cz.smarteon.loxkt.Codec.loxJson
import cz.smarteon.loxkt.message.EmptyLoxoneMsgVal
import cz.smarteon.loxkt.message.LoxoneMsgVal

interface LoxoneClient {

    suspend fun <RESPONSE : LoxoneResponse> call(command: Command<RESPONSE>): RESPONSE

    suspend fun callRaw(command: String): String

    /**
     * Call given [command] and return response as raw data.
     * Can be used to retrieve files, icons, etc.
     *
     * @param command Command to call.
     */
    suspend fun callRawForData(command: String): ByteArray

    suspend fun close()
}

/**
 * Loxone client using HTTP for communication.
 */
interface HttpLoxoneClient : LoxoneClient {
    /**
     * Sends given [payload] to [command]'s endpoint using HTTP POST to Loxone and returns
     * the response body as text.
     *
     * @param command Command to call.
     * @param payload Payload to send.
     * @return Response body as text.
     */
    suspend fun postRaw(command: String, payload: ByteArray): String
}

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
