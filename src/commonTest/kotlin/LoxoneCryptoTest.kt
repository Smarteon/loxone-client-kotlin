package cz.smarteon.loxkt

import cz.smarteon.loxkt.message.Hashing
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class LoxoneCryptoTest : FreeSpec({

    "should do hashing" - {
        withData(
            "SHA1" to "cf72cfeeb893053157bcaee60eaf8a141a34b765",
            "SHA256" to "49ea4ce1af1301f478b365fc88c13eba4883bff2d10f4eafcc4613e7b2eed1c0"
        ) { (alg, result) ->
            LoxoneCrypto.loxoneHashing(
                "pass",
                Hashing(
                    Codec.hexToBytes("32353930373135363644424636333638334131414441394243393939343834303141324531413045"),
                    "31346632393637342D303239312D323837622D66666666613532346235633538306662",
                    alg
                ),
                "op",
                "user"
            ) shouldBe result
        }
    }
})
