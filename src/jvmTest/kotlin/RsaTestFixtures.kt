package cz.smarteon.loxkt

import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

internal object RsaTestFixtures {

    val TEST_PUBLIC_KEY_PEM = """
        -----BEGIN PUBLIC KEY-----
        MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAr3wViebzGQCZbK2FrW6c
        bpiSbHFXyfGRCnnrGqRiflpK0YE29ODWdla0B9rXYQTbHTI2xbXdGTE1o/Ji9z8u
        7IkNjLE6nVAdCNveITAGeU1hOT72o1jKYTPRO3ABT2A/PGQvRAhohJ/qOqaK+nqm
        i2YdzZpozON6EijMb90pMz2KPCb6QAyBrlwf0HC1PCyaXRc1AeZs79y/gT+AcGys
        9lq817df8bBA9E19ZipQGuMfU0UhvudygTBHIp32tdfGbNTfu0GEm3baSxyZIiQG
        xoE+kb6vevhq7qZdBcb+fidcbFJpdt3QjQymlKA16CoLDNXAvtVD8iQARfGpZJ4q
        WwIDAQAB
        -----END PUBLIC KEY-----
    """.trimIndent()

    val TEST_PRIVATE_KEY_PEM = """
        -----BEGIN PRIVATE KEY-----
        MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCvfBWJ5vMZAJls
        rYWtbpxumJJscVfJ8ZEKeesapGJ+WkrRgTb04NZ2VrQH2tdhBNsdMjbFtd0ZMTWj
        8mL3Py7siQ2MsTqdUB0I294hMAZ5TWE5PvajWMphM9E7cAFPYD88ZC9ECGiEn+o6
        por6eqaLZh3NmmjM43oSKMxv3SkzPYo8JvpADIGuXB/QcLU8LJpdFzUB5mzv3L+B
        P4BwbKz2WrzXt1/xsED0TX1mKlAa4x9TRSG+53KBMEcinfa118Zs1N+7QYSbdtpL
        HJkiJAbGgT6Rvq96+Grupl0Fxv5+J1xsUml23dCNDKaUoDXoKgsM1cC+1UPyJABF
        8alknipbAgMBAAECggEATp9O/R6CqyCIHNdvrYEByFFGRRzRAtLes21lRuYAlPC6
        VbxJVZSIsbNt7JkBZ1/oFeAiBxnQqhFQoZge+/yMdwB+mBrtkn7Ky2XK016zf9SJ
        6z6I/S1yWvN+2lz30Urjehi+zoHf5g/JvyZP3SJnjMwfLTdxnd6LGii6U6Ioa+J1
        39Iz5H35wf3eU5XGebiIh3zjrDQbstK/t3ru/rAY8l4VCaTrm8IhwRo8u/5PSyke
        j/UTEO7owVch8m0anPsCXmWcdlcW98UvBGyZDxz7qTlHjZUGMQgcd5RRNTs4eoGB
        s3BdM4OgzNC9qJ0pEFrFL1kzh/ppWdh8o+chCfabaQKBgQD3dzdeJPtpA5t8IEcC
        fNER4qUZl0Q9qI6yVIUZQOTXma/O8eV580S1Pk/Raitipm6H5I1w1ALiHQM2lars
        OPPTmB4JQHKxO9M/gDJVdB0GoOZqTwWmyJxiA9Cf/9/7c5snmAClq0v/auVljRaL
        zZnS9Tis5BBZHa8kWytXKauxSQKBgQC1iV/JLMMomJ791aStj1k2N9T6oSGOPOC9
        2vAG7UbxrikEwGeGXvjg6VJev6QRiGHvpqjF+i5diJTf4x6HbddO0lxAtAgyHZKS
        AxaKvSq1p0FcW/eCVFgba8AEkCmiHVCJ7qCRE2Y4no8JQOGvmchFSoo5gRlPfh82
        Q7uMxUDigwKBgQChFyQdzukyNTz0EnbnMaVPhUCAZi3wDVfG0qpKBCp0BwGhL2p4
        dlnVuhhvdDOF5l2xbKB+QCUYWFaNI+S+HVzr8uwqjZ+brBwaDDO32PxEIl2b+pDt
        P049p8oZPZHquBjaL2LMdbPlMwrdjniMzWxDHYqlUVkrCd3HRunxtZiksQKBgCn3
        Hct1q4/A6FApiS4OC0N7WKKviQBGlnWNHRuc0l+gMR9GEyh+3+2uQjpg9t6OtoUd
        87oAgaNhpXi0GiSYgcNY4babZ6GeMHnMePONk0f26Ccfo3HfaZa9K+BiKx2sxSd9
        oGSpJWJFVS+AbiuX0zIhbx6n91/m+fQjaEG8f6ldAoGAAzsuJ82mu7nTuESgJAal
        Tv7T8Gvnkm9tSUNnfJTPzBMgS+sKqjywtQhXWT88kEHS9hQL0MLC3kz8Yp5QJwz+
        SJz7itfEEe50bnf8e3QDQd3W4a9xkfccKytUXdrubbdMaXtI57pqNlX0CKlkedgW
        30nOv9WQaIo21Nqw+55ddsk=
        -----END PRIVATE KEY-----
    """.trimIndent()

    fun decryptWithPrivateKey(base64Encrypted: String, privateKeyPem: String = TEST_PRIVATE_KEY_PEM): String {
        val pemBody = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(pemBody)))
        return Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
            init(Cipher.DECRYPT_MODE, privateKey)
            String(doFinal(Base64.getDecoder().decode(base64Encrypted)))
        }
    }
}
