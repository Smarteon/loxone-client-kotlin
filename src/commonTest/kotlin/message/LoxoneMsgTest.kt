package cz.smarteon.loxone.message

import cz.smarteon.loxone.Codec.loxJson
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class LoxoneMsgTest : FreeSpec({


    "should deserialize" - {
        withData("\"string\"", "12.3", "true", "{}") { value ->
            loxJson.decodeFromString<LoxoneMsg>(
                //language=JSON
                """{"LL": { "control": "dev/cfg/api", "value": ${value}, "Code": "200"}}"""
            ) shouldBe LoxoneMsg("dev/cfg/api", "200", value)
        }
    }

})
