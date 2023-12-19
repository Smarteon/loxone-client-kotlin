package cz.smarteon.loxone.message


import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.UseSerializers

class ApiInfoTest : StringSpec({

    "should deserialize from message value" {
        ApiInfo.fromMessageValue("""
           {'snr': '50:4F:94:11:12:5F', 'version':'14.5.12.7', 'hasEventSlots':true, 'isInTrust':false, 'local':true}
        """) shouldBe ApiInfo("50:4F:94:11:12:5F", "14.5.12.7", true, false, true)
    }

})
