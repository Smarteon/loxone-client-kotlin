package cz.smarteon.loxkt

import cz.smarteon.loxkt.Codec.writeHeader
import cz.smarteon.loxkt.ktor.KtorHttpLoxoneClient
import cz.smarteon.loxkt.ktor.KtorWebsocketLoxoneClient
import cz.smarteon.loxkt.ktor.sendLoxoneResponse
import cz.smarteon.loxkt.ktor.startTestWebSocketServer
import cz.smarteon.loxkt.message.MessageHeader
import cz.smarteon.loxkt.message.MessageKind
import cz.smarteon.loxkt.message.TestingMessages.okMsg
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val ENABLE_STATUS_UPDATE = "jdev/sps/enablebinstatusupdate"
private const val VALUE_UUID = "0eed7ea0-0013-0ad8-ffff504f94185cc3"

// Single value event (see ValueEventTest): VALUE_UUID = 23.5
@OptIn(ExperimentalStdlibApi::class)
private val VALUE_EVENT = "a07eed0e1300d80affff504f94185cc30000000000803740".hexToByteArray()

private fun enableStatusUpdateResponse() = okMsg("dev/sps/enablebinstatusupdate", "\"1\"")

private fun testedMiniserver(client: HttpClient) = LoxoneMiniserver(
    LoxoneEndpoint.local("127.0.0.1"),
    KtorWebsocketLoxoneClient(client),
    KtorHttpLoxoneClient(LoxoneEndpoint.local("127.0.0.1")),
)

class LoxoneMiniserverIT : StringSpec({

    "should not lose the initial state pushed right after enablebinstatusupdate" {
        val ctx = startTestWebSocketServer(
            ENABLE_STATUS_UPDATE to { _ ->
                // Pushed before the response on purpose: with the old ordering (enable first, start
                // collecting after it returned) these events were always emitted before anyone
                // subscribed to the replay-less events flow, and got dropped.
                val header = MessageHeader(MessageKind.EVENT_VALUE, false, VALUE_EVENT.size.toLong())
                send(Frame.Binary(true, writeHeader(header)))
                send(Frame.Binary(true, VALUE_EVENT))
                sendLoxoneResponse(enableStatusUpdateResponse())
            }
        )
        val miniserver = testedMiniserver(ctx.testedClient)

        miniserver.connect()

        withTimeout(5.seconds) {
            while (miniserver.state.getValue(VALUE_UUID) == null) delay(10.milliseconds)
        }
        miniserver.state.getValue(VALUE_UUID) shouldBe 23.5

        miniserver.close()
    }

    "concurrent connect should not return before the first one has enabled status updates" {
        val release = CompletableDeferred<Unit>()
        val ctx = startTestWebSocketServer(
            ENABLE_STATUS_UPDATE to { _ ->
                release.await()
                sendLoxoneResponse(enableStatusUpdateResponse())
            }
        )
        val miniserver = testedMiniserver(ctx.testedClient)

        coroutineScope {
            val first = async { miniserver.connect() }
            ctx.received.receive() shouldBe ENABLE_STATUS_UPDATE

            val second = async { miniserver.connect() }
            delay(200.milliseconds)
            second.isCompleted shouldBe false

            release.complete(Unit)
            first.await()
            second.await()
        }

        // the second call found the session connected and did not enable status updates again
        ctx.received.tryReceive().getOrNull() shouldBe null

        miniserver.close()
    }
})
