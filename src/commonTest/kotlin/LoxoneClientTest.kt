package cz.smarteon.loxone

import cz.smarteon.loxone.message.ApiInfo
import cz.smarteon.loxone.message.EmptyLoxoneMsgVal
import cz.smarteon.loxone.message.LoxoneMsg
import cz.smarteon.loxone.message.SimpleLoxoneMsgCommand
import cz.smarteon.loxone.message.TestingLoxValues.API_INFO_MSG_VAL
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.kodein.mock.Mocker
import org.kodein.mock.UsesMocks

@UsesMocks(LoxoneClient::class)
class LoxoneClientTest : StringSpec({

    "should call for ApiInfo" {
        val client = mockClientCall(ApiInfo.command, LoxoneMsg("dev/cfg/api","200", API_INFO_MSG_VAL))

        client.callForMsg(ApiInfo.command).mac shouldBe "50:4F:94:11:12:5F"
    }

    "should call for empty val" {
        val command = SimpleLoxoneMsgCommand(listOf("empty"), EmptyLoxoneMsgVal::class)
        val client = mockClientCall(command, LoxoneMsg("empty", "200", ""))

        client.callForMsg(command) shouldBe EmptyLoxoneMsgVal
    }
})

private suspend fun mockClientCall(command: Command<LoxoneMsg>, value: LoxoneMsg): LoxoneClient {
    val mocker = Mocker()
    val client: LoxoneClient = MockLoxoneClient(mocker)
    mocker.everySuspending { client.call(command) } returns value
    return client
}
