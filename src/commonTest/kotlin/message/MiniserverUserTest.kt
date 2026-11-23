package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.LoxoneCommands
import cz.smarteon.loxkt.MockLoxoneClient
import cz.smarteon.loxkt.callForMsg
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class MiniserverUserTest : ShouldSpec({

    val groups = """[{"name":"Host","uuid":"g-1","type":0,"userRights":0},
        |{"name":"Admins","uuid":"g-2","type":4,"userRights":4294967295},
        |{"name":"Config","uuid":"g-3","type":0,"userRights":4},
        |{"name":"Users","uuid":"g-4","type":0,"userRights":256},
        |{"name":"Administrators","uuid":"g-5","type":1,"userRights":0}]""".trimMargin()

    should("decode the group list whether the value is JSON or a string holding it") {
        listOf(groups, Json.encodeToString(String.serializer(), groups)).forEach { value ->
            val client = MockLoxoneClient().apply { stubMsg("getgrouplist", value) }
            val decoded = client.callForMsg(LoxoneCommands.Users.groups()).items
            decoded.map { it.name } shouldContainExactly listOf("Host", "Admins", "Config", "Users", "Administrators")
            decoded.map { it.isAdmin } shouldContainExactly listOf(false, true, true, false, true)
        }
    }

    should("decode the user list and tell time-limited users") {
        val users = """[{"name":"admin","uuid":"u-1","isAdmin":true,"userState":0},
            |{"name":"guest","uuid":"u-2","userState":2,"validUntil":500000000,"expirationAction":1,"userid":""},
            |{"name":"visitor","uuid":"u-3","userState":4,"validFrom":400000000,"validUntil":500000000},
            |{"name":"plain","uuid":"u-4"}]
            """.trimMargin()
        val client = MockLoxoneClient().apply { stubMsg("getuserlist2", users) }
        val decoded = client.callForMsg(LoxoneCommands.Users.list()).items
        decoded.map { it.uuid to it.name } shouldContainExactly
            listOf("u-1" to "admin", "u-2" to "guest", "u-3" to "visitor", "u-4" to "plain")
        decoded.map { it.isTimeLimited } shouldContainExactly listOf(false, true, true, false)
        decoded[1].validUntil shouldBe 500_000_000L
        decoded[1].expirationAction shouldBe MiniserverUser.EXPIRATION_DELETE
        decoded[2].validFrom shouldBe 400_000_000L
        decoded[3].userState shouldBe MiniserverUser.STATE_ENABLED
    }

    should("decode a user with its groups") {
        val user = """{"name":"guest","uuid":"u-2","userState":2,"validUntil":500000000,
            |"usergroups":[{"name":"Host","uuid":"g-1"}],"nfcTags":[],"keycodes":[]}""".trimMargin()
        val client = MockLoxoneClient().apply { stubMsg("getuser", Json.encodeToString(String.serializer(), user)) }
        client.callForMsg(LoxoneCommands.Users.get("u-2")).usergroups.map { it.uuid } shouldContainExactly listOf("g-1")
    }
})
