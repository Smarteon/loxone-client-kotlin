package cz.smarteon.loxkt

import cz.smarteon.loxkt.message.EmptyLoxoneMsgVal
import cz.smarteon.loxkt.message.Hashing
import cz.smarteon.loxkt.message.LoginCode
import cz.smarteon.loxkt.message.Token
import cz.smarteon.loxkt.message.TokenPermission
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.Json
import kotlin.time.Instant

class LoxoneCommandsTest : ShouldSpec({

    should("default commands to not encrypted") {
        // KEEP_ALIVE does not override Command.encrypted, exercising the interface default
        LoxoneCommands.KEEP_ALIVE.encrypted shouldBe false
    }

    should("create kill token command") {
        LoxoneCommands.Tokens.kill("hash", "user").asClue {
            it.pathSegments shouldBe listOf("jdev", "sys", "killtoken", "hash", "user")
            it.valueType shouldBe EmptyLoxoneMsgVal::class
            it.expectedCode shouldBe "401"
        }
    }

    should("create gettoken command") {
        LoxoneCommands.Tokens.get("hash", "user", TokenPermission.WEB).asClue {
            it.pathSegments shouldBe listOf(
                "jdev",
                "sys",
                "getjwt",
                "hash",
                "user",
                "2",
                "df184362-73fc-5d3e-ab0ec7c1c3e5bb2e",
                "loxoneKotlin"
            )
            it.valueType shouldBe Token::class
            it.authenticated shouldBe false
            it.expectedCode shouldBe "200"
            it.encrypted shouldBe true
        }
    }

    should("ask for combined permissions in one token") {
        val permission = TokenPermission.APP + TokenPermission.EXPERT_MODE + TokenPermission.SYS_WS +
            TokenPermission.USER_MANAGEMENT + TokenPermission.TRIGGER_UPDATE

        LoxoneCommands.Tokens.get("hash", "user", permission).pathSegments[5] shouldBe "35140"
        (TokenPermission.SYS_WS in TokenPermission(1988)) shouldBe true
        (TokenPermission.SYS_WS in TokenPermission(1732)) shouldBe false
    }

    should("create gettoken command signed with a token") {
        LoxoneCommands.Tokens.getWithToken("hash", "user", TokenPermission.WEB).asClue {
            it.pathSegments shouldBe LoxoneCommands.Tokens.get("hash", "user", TokenPermission.WEB).pathSegments
            it.query shouldBe mapOf("tokenHash" to "true")
            it.queryString() shouldBe "?tokenHash=true"
            it.authenticated shouldBe false
            it.encrypted shouldBe true
        }
    }

    should("create gettoken command with the second factor") {
        LoxoneCommands.Tokens.get("hash", "user", TokenPermission.APP, method = "totp", otp = "639c").asClue {
            it.pathSegments shouldBe LoxoneCommands.Tokens.get("hash", "user", TokenPermission.APP).pathSegments
            it.queryString() shouldBe "?method=totp&otp=639c"
            it.encrypted shouldBe true
        }
        LoxoneCommands.Tokens.get("hash", "user", TokenPermission.APP, method = "email").query shouldBe
            mapOf("method" to "email")
        LoxoneCommands.Tokens.get("hash", "user", TokenPermission.APP).query shouldBe emptyMap()
    }

    should("create otpresend command") {
        LoxoneCommands.Tokens.resendLoginCode("hash", "user").asClue {
            it.pathSegments shouldBe listOf("jdev", "sys", "otpresend", "hash", "user")
            it.valueType shouldBe LoginCode::class
            it.authenticated shouldBe false
        }
    }

    should("create authwithtoken command") {
        LoxoneCommands.Tokens.auth("hash", "user").asClue {
            it.pathSegments shouldBe listOf("authwithtoken", "hash", "user")
            it.valueType shouldBe Token::class
            it.authenticated shouldBe false
            it.expectedCode shouldBe "200"
            it.encrypted shouldBe true
        }
    }

    should("create refresh token command") {
        LoxoneCommands.Tokens.refresh("hash", "user").asClue {
            it.pathSegments shouldBe listOf("jdev", "sys", "refreshjwt", "hash", "user")
            it.valueType shouldBe Token::class
            it.authenticated shouldBe true
            it.expectedCode shouldBe "200"
        }
    }

    should("create user management read and delete commands") {
        LoxoneCommands.Users.list().pathSegments shouldBe listOf("jdev", "sps", "getuserlist2")
        LoxoneCommands.Users.groups().pathSegments shouldBe listOf("jdev", "sps", "getgrouplist")
        LoxoneCommands.Users.get("u-1").pathSegments shouldBe listOf("jdev", "sps", "getuser", "u-1")
        LoxoneCommands.Users.delete("u-1").asClue {
            it.pathSegments shouldBe listOf("jdev", "sps", "deleteuser", "u-1")
            it.authenticated shouldBe true
        }
    }

    should("create a time-limited user in its groups") {
        val until = Instant.fromEpochSeconds(LoxoneTime.LOXONE_EPOCH_BEGIN + 500_000_000L)
        LoxoneCommands.Users.create("host-loznice_1", listOf("g-1", "g-2"), until, deleteOnExpiry = true).asClue {
            it.pathSegments.dropLast(1) shouldBe listOf("jdev", "sps", "addoredituser")
            it.pathSegments.last() shouldNotContain " "
            Json.parseToJsonElement(it.pathSegments.last()) shouldBe Json.parseToJsonElement(
                """{"name":"host-loznice_1","changePassword":false,"userState":2,
                |"validUntil":500000000,"expirationAction":1,"usergroups":["g-1","g-2"]}
                """.trimMargin()
            )
        }
    }

    should("refuse user names the clients would encode differently") {
        listOf("Host ložnice", "a/b", "50%", "", "host@chata", "..", "x".repeat(65)).forEach { name ->
            shouldThrow<IllegalArgumentException> { LoxoneCommands.Users.create(name, emptyList()) }
        }
    }

    should("create a permanent user without validity fields") {
        val json = Json.parseToJsonElement(LoxoneCommands.Users.create("guest", emptyList()).pathSegments.last())
        json shouldBe Json.parseToJsonElement("""{"name":"guest","changePassword":false,"userState":0,"usergroups":[]}""")
    }

    should("send only the salted password hash, encrypted") {
        val salt = "31346632393637342D303239312D323837622D66666666613532346235633538306662"
        mapOf(
            "SHA1" to "75C4C904ECA6C7BC82B9D65D4C7FA94C3775CBCF",
            "SHA256" to "CB6EE76C8E5403AA303D353C4F2883700BEF489BDB8412D522C80507DF9394E4"
        ).forEach { (alg, hash) ->
            LoxoneCommands.Users.setPassword("u-1", "pass", Hashing(byteArrayOf(1), salt, alg)).asClue {
                it.pathSegments shouldBe listOf("jdev", "sps", "updateuserpwdh", "u-1", hash)
                it.encrypted shouldBe true
            }
        }
    }
})
