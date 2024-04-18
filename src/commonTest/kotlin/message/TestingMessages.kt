package cz.smarteon.loxone.message

import io.ktor.http.*

internal object TestingMessages {

    fun okMsg(control: String, jsonValue: String) =
        // language=JSON
        """{"LL": { "control": "$control", "value": $jsonValue, "Code": "200"}}"""

    fun htmlError(status: HttpStatusCode) =
        """
            <html><head><title>error</title></head><body><errorcode>${status.value}</errorcode> <errordetail>${status.description}</errordetail></body></html>
        """.trimIndent()

}

internal object TestingLoxValues {
    const val API_INFO =
        "{'snr': '50:4F:94:11:12:5F', 'version':'14.5.12.7', 'hasEventSlots':true, 'isInTrust':false, 'local':true}"
    const val API_INFO_MSG_VAL = "\"$API_INFO\""

    // language=JSON
    val HASHING = """{
          "key": "3235",
          "salt": "31346632393637342D303239312D323837622D66666666613532346235633538306662",
          "hashAlg": "SHA1"
        }""".trimIndent()

    fun token(validUntil: Long) =
        // language=JSON
        """
            {
              "token": "8E2AA590E996B321C0E17C3FA9F7A3C17BD376CC",
              "key": "444432",
              "validUntil": $validUntil,
              "tokenRights": 1666,
              "unsecurePass": false
            }
        """.trimIndent()

    fun tokenAuthResponse(validUntil: Long) =
        // language=JSON
        """
            {
              "validUntil": $validUntil,
              "tokenRights": 1666,
              "unsecurePass": false
            }
        """.trimIndent()

}
