package cz.smarteon.loxone

import cz.smarteon.loxone.message.MessageHeader
import cz.smarteon.loxone.message.MessageKind
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@OptIn(ExperimentalStdlibApi::class)
class CodecTest : StringSpec({

    "should decode hex" {
        Codec.hexToBytes("3235") shouldBe byteArrayOf(50, 53)
    }

    "should encode hex" {
        Codec.bytesToHex(byteArrayOf(50, 53)) shouldBe "3235"
    }

    "should read header" {
        val header = Codec.readHeader(Codec.hexToBytes("03000000ab000000"))
        header shouldBe MessageHeader(MessageKind.TEXT, false, 171)
    }

    "should write header" {
        val header = MessageHeader(MessageKind.TEXT, false, 171)
        Codec.writeHeader(header) shouldBe Codec.hexToBytes("03000000ab000000")
    }

    "should read KEEP_ALIVE header" {
        val header = Codec.readHeader(Codec.hexToBytes("0306000000000000"))
        header shouldBe MessageHeader.KEEP_ALIVE
    }
})
