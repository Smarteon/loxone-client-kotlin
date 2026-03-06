package cz.smarteon.loxkt

import cz.smarteon.loxkt.event.LoxoneEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

internal class MockLoxoneClient : WebsocketLoxoneClient {

    private val stubbedCalls = mutableListOf<StubbedCall<*>>()
    private val stubbedRawData = mutableMapOf<String, ByteArray>()
    private val stubbedRawStrings = mutableMapOf<String, String>()
    private val _events = MutableSharedFlow<LoxoneEvent>()
    override val events: SharedFlow<LoxoneEvent> = _events

    fun <RESPONSE : LoxoneResponse> stubCall(response: RESPONSE, commandMatcher: (Command<*>) -> Boolean): CallVerification {
        val verification = CallVerification()
        stubbedCalls.add(StubbedCall(commandMatcher, response, verification))
        return verification
    }

    fun stubRawData(command: String, data: ByteArray) {
        stubbedRawData[command] = data
    }

    fun stubRawString(command: String, data: String) {
        stubbedRawStrings[command] = data
    }

    override suspend fun <RESPONSE : LoxoneResponse> call(command: Command<RESPONSE>): RESPONSE {
        stubbedCalls.firstOrNull { it.commandMatcher(command) }?.let {
            it.verification.matches.add(command)

            @Suppress("UNCHECKED_CAST")
            return it.response as RESPONSE
        } ?: error("No stubbed call for $command")
    }

    override suspend fun callRaw(command: String): String =
        stubbedRawStrings[command] ?: error("No stubbed raw string for command: $command")

    override suspend fun callRawForData(command: String): ByteArray =
        stubbedRawData[command] ?: error("No stubbed raw data for command: $command")

    override suspend fun close() {
        TODO("Not yet implemented")
    }
}

internal data class StubbedCall<RESPONSE : LoxoneResponse>(
    val commandMatcher: (Command<*>) -> Boolean,
    val response: RESPONSE,
    val verification: CallVerification,
)
internal class CallVerification(
    val matches: MutableList<Command<*>> = mutableListOf(),
)
