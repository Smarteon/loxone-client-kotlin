package cz.smarteon.loxone.message


import cz.smarteon.loxone.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe

class ApiKeyInfoTest : StringSpec({

    "should sanitize for decoding" {
        ApiKeyInfo.command.valueType shouldBe ApiKeyInfo::class
        loxoneMsgValDecoders shouldContainKey ApiKeyInfo::class
        loxoneMsgValDecoders[ApiKeyInfo::class]!!.invoke("""
           "{'snr': '50:4F:94:A0:2E:7E', 'version':'14.5.12.7', 'key':'41384441353644413143443743364637333636413739313034423731413846364237453936384144', 'isInTrust':false, 'local':true,'address':'192.168.51.77','certTLD':'com', 'httpsStatus':1}"
        """.trimIndent()) shouldBe """
             {"snr": "50:4F:94:A0:2E:7E", "version":"14.5.12.7", "key":"41384441353644413143443743364637333636413739313034423731413846364237453936384144", "isInTrust":false, "local":true,"address":"192.168.51.77","certTLD":"com", "httpsStatus":1}
        """.trimIndent()
    }

    "should deserialize" {
        //language=JSON
        val apiKeyInfoJson = """{
          "snr": "50:4F:94:A0:2E:7E",
          "version": "14.5.12.7",
          "key": "41384441353644413143443743364637333636413739313034423731413846364237453936384144",
          "isInTrust": false,
          "local": true,
          "address": "192.168.51.77",
          "certTLD": "com",
          "httpsStatus": 1
        }""".trimIndent()

        Codec.loxJson.decodeFromString<ApiKeyInfo>(apiKeyInfoJson) shouldBe ApiKeyInfo(
            "50:4F:94:A0:2E:7E",
            "14.5.12.7",
            "41384441353644413143443743364637333636413739313034423731413846364237453936384144",
            "192.168.51.77",
            false,
            true,
            "com",
            1
        )
    }

})
