package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.Codec.loxJson
import cz.smarteon.loxkt.message.TestingLoxValues.HASHING
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class HashingTest : StringSpec({

    "should deserialize" {
        loxJson.decodeFromString<Hashing>(HASHING) shouldBe Hashing(
            byteArrayOf(50, 53),
            "31346632393637342D303239312D323837622D66666666613532346235633538306662",
            "SHA1"
        )
    }
})
