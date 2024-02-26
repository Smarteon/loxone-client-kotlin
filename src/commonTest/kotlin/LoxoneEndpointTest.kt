package cz.smarteon.loxone

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class LoxoneEndpointTest : WordSpec({

    "Loxone endpoint constructed" When {

        "with default values" should {
            LoxoneEndpoint("somehost").apply {
                "have host" { host shouldBe "somehost"}
                "have port" { port shouldBe 443 }
                "have useSsl" { useSsl shouldBe true }
                "have path" { path shouldBe "" }
            }
        }

        withData(
            nameFn = { (case, _) -> "with $case host" },
            "empty" to "",
            "contains port" to "host:123",
            "contains protocol" to "http://host"
        ) { (_, host) ->
            shouldThrow<IllegalArgumentException> {
                LoxoneEndpoint(host)
            }
        }
    }

    "FromUrl endpoint constructed" When {
        withData(
            nameFn = { (url, _) -> url },
            "http://some:7780/testPath" to LoxoneEndpoint("some", 7780, false, "/testPath"),
            "http://some/testPath" to LoxoneEndpoint("some", 80, false, "/testPath"),
            "https://some.smarteon.cz:7743/testPath" to LoxoneEndpoint("some.smarteon.cz", 7743, true, "/testPath"),
            "https://some.smarteon.cz/testPath" to LoxoneEndpoint("some.smarteon.cz", 443, true, "/testPath")
        ) {  (url, expected) ->
            LoxoneEndpoint.fromUrl(url) shouldBe expected
        }
    }

    "Local endpoint constructed" When {
        "with default values" should {
            LoxoneEndpoint.local("192.168.9.77").apply {
                "have host" { host shouldBe "192.168.9.77" }
                "have port" { port shouldBe 80 }
                "have useSsl" { useSsl shouldBe false }
                "have path" { path shouldBe "" }
            }
        }

        "with non-IP address" should {
            shouldThrow<IllegalArgumentException> {
                LoxoneEndpoint.local("somehost")
            }
        }
    }

    "Public domain endpoint constructed" When {
        "with default values" should {
            LoxoneEndpoint.public("test.smarteon.cz").apply {
                "have host" { host shouldBe "test.smarteon.cz" }
                "have port" { port shouldBe 443 }
                "have useSsl" { useSsl shouldBe true }
                "have path" { path shouldBe "" }
            }
        }

        "with non-IP address" should {
            shouldThrow<IllegalArgumentException> {
                LoxoneEndpoint.public("192.168.9.77")
            }
        }
    }
})
