package cz.smarteon.loxkt

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

class LoxoneCryptoRsaTest : ShouldSpec({

    val testPublicKeyPem = """
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

    val testPrivateKeyPem = """
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

    context("RSA encryption") {
        should("encrypt data with public key") {
            val testData = "Hello, Loxone!"
            
            val encrypted = LoxoneCrypto.rsaEncrypt(testData, testPublicKeyPem)
            
            encrypted.shouldNotBeEmpty()
            encrypted shouldNotBe testData
        }

        should("produce Base64 encoded output") {
            val testData = "test:message"
            
            val encrypted = LoxoneCrypto.rsaEncrypt(testData, testPublicKeyPem)
            
            // Should be valid Base64
            kotlin.runCatching {
                Base64.getDecoder().decode(encrypted)
            }.isSuccess shouldBe true
        }

        should("encrypt and decrypt roundtrip") {
            val testData = "AES256key:InitVector16"
            
            val encrypted = LoxoneCrypto.rsaEncrypt(testData, testPublicKeyPem)
            val decrypted = decryptWithPrivateKey(encrypted, testPrivateKeyPem)
            
            decrypted shouldBe testData
        }

        should("produce different output for same input") {
            // Due to PKCS1 padding with random padding, same input should produce different encrypted output
            val testData = "test"
            
            val encrypted1 = LoxoneCrypto.rsaEncrypt(testData, testPublicKeyPem)
            val encrypted2 = LoxoneCrypto.rsaEncrypt(testData, testPublicKeyPem)
            
            // Both should decrypt to same value but be different encrypted
            encrypted1 shouldNotBe encrypted2
            decryptWithPrivateKey(encrypted1, testPrivateKeyPem) shouldBe testData
            decryptWithPrivateKey(encrypted2, testPrivateKeyPem) shouldBe testData
        }

        should("encrypt session key in Loxone format") {
            val aesKey = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
            val aesIv = "FEDCBA9876543210FEDCBA9876543210"
            val sessionKey = "$aesKey:$aesIv"
            
            val encrypted = LoxoneCrypto.rsaEncrypt(sessionKey, testPublicKeyPem)
            val decrypted = decryptWithPrivateKey(encrypted, testPrivateKeyPem)
            
            decrypted shouldBe sessionKey
        }
    }
})

/**
 * Helper function to decrypt RSA encrypted data with private key for testing.
 */
private fun decryptWithPrivateKey(base64Encrypted: String, privateKeyPem: String): String {
    // Parse private key
    val privateKeyContent = privateKeyPem
        .replace("-----BEGIN RSA PRIVATE KEY-----", "")
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END RSA PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("\\s".toRegex(), "")
    
    val keyBytes = Base64.getDecoder().decode(privateKeyContent)
    
    // Parse as PKCS8
    val keyFactory = KeyFactory.getInstance("RSA")
    val keySpec = PKCS8EncodedKeySpec(keyBytes)
    val privateKey = keyFactory.generatePrivate(keySpec)
    
    // Decrypt
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    cipher.init(Cipher.DECRYPT_MODE, privateKey)
    val encrypted = Base64.getDecoder().decode(base64Encrypted)
    val decrypted = cipher.doFinal(encrypted)
    
    return String(decrypted, Charsets.UTF_8)
}
