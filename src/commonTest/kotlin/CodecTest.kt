package cz.smarteon.loxone

import cz.smarteon.loxone.Codec.loxJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.builtins.ByteArraySerializer

class CodecTest : StringSpec({

    "should decode hex" {
        println(loxJson.encodeToString(ByteArraySerializer(), byteArrayOf(50, 53)))
        Codec.hexToBytes("3235") shouldBe byteArrayOf(50, 53)
    }
})
