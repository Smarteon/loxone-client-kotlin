package cz.smarteon.loxone.message

import cz.smarteon.loxone.Codec.loxJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LoxoneMessageTest : StringSpec({

    "should deserialize" {
        loxJson.decodeFromString<LoxoneMsg>("""
            {"LL": { "control": "dev/cfg/api", "value": "SOMEVAL", "Code": "200"}}
        """) shouldBe LoxoneMsg("dev/cfg/api", "200", "SOMEVAL")
    }

})
