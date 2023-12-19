package cz.smarteon.loxone.message

import cz.smarteon.loxone.Codec.JSON
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LoxoneMessageTest : StringSpec({

    "should deserialize" {
        JSON.decodeFromString<LoxoneMessage>("""
            {"LL": { "control": "dev/cfg/api", "value": "SOMEVAL", "Code": "200"}}
        """) shouldBe LoxoneMessage("dev/cfg/api", "200", "SOMEVAL")
    }

})
