package cz.smarteon.loxone.message


import cz.smarteon.loxone.message.TestingLoxValues.API_INFO
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe

class ApiInfoTest : StringSpec({

    "should sanitize for decoding" {
        ApiInfo.command.valueType shouldBe ApiInfo::class
        loxoneMsgValDecoders shouldContainKey ApiInfo::class
        loxoneMsgValDecoders[ApiInfo::class]!!.invoke(API_INFO) shouldBe """
             {"snr": "50:4F:94:11:12:5F", "version":"14.5.12.7", "hasEventSlots":true, "isInTrust":false, "local":true}
        """.trimIndent()
    }

})
