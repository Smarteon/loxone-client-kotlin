package cz.smarteon.loxone.message


import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ApiInfoTest : StringSpec({

    "should sanitize for decoding" {
        ApiInfo.valueForDecoding("""
           {'snr': '50:4F:94:11:12:5F', 'version':'14.5.12.7', 'hasEventSlots':true, 'isInTrust':false, 'local':true}
        """.trimIndent()) shouldBe """
             {"snr": "50:4F:94:11:12:5F", "version":"14.5.12.7", "hasEventSlots":true, "isInTrust":false, "local":true}
        """.trimIndent()
    }

})
