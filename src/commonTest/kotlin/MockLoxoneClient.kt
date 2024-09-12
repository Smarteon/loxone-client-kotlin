package cz.smarteon.loxone

internal class MockLoxoneClient : WebsocketLoxoneClient {

    private val stubbedCalls = mutableListOf<StubbedCall<*>>()

    fun <RESPONSE : LoxoneResponse> stubCall(response: RESPONSE, commandMatcher: (Command<*>) -> Boolean): CallVerification {
        val verification = CallVerification()
        stubbedCalls.add(StubbedCall(commandMatcher, response, verification))
        return verification
    }

    override suspend fun <RESPONSE : LoxoneResponse> call(command: Command<RESPONSE>): RESPONSE {
        stubbedCalls.firstOrNull { it.commandMatcher(command) }?.let {
            it.verification.matches.add(command)

            @Suppress("UNCHECKED_CAST")
            return it.response as RESPONSE
        } ?: error("No stubbed call for $command")
    }

    override suspend fun callRaw(command: String): String {
        TODO("Not yet implemented")
    }

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
