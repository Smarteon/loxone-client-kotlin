package cz.smarteon.loxkt

import cz.smarteon.loxkt.message.PublicKey
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class PublicKeyRepositoryTest : ShouldSpec({

    val testProfile = LoxoneProfile(
        LoxoneEndpoint.local("192.168.1.100"),
        LoxoneCredentials("user", "password")
    )

    val testPublicKey = PublicKey(
        """
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
    )

    context("InMemoryPublicKeyRepository") {
        should("return null for non-existent public key") {
            val repository = InMemoryPublicKeyRepository()
            
            repository.getPublicKey(testProfile).shouldBeNull()
        }

        should("store and retrieve public key") {
            val repository = InMemoryPublicKeyRepository()
            
            repository.putPublicKey(testProfile, testPublicKey)
            val retrieved = repository.getPublicKey(testProfile)
            
            retrieved.shouldNotBeNull()
            retrieved.publicKey shouldBe testPublicKey.publicKey
        }

        should("overwrite existing public key") {
            val repository = InMemoryPublicKeyRepository()
            val newPublicKey = PublicKey("different-key")
            
            repository.putPublicKey(testProfile, testPublicKey)
            repository.putPublicKey(testProfile, newPublicKey)
            val retrieved = repository.getPublicKey(testProfile)
            
            retrieved.shouldNotBeNull()
            retrieved.publicKey shouldBe "different-key"
        }

        should("remove public key") {
            val repository = InMemoryPublicKeyRepository()
            
            repository.putPublicKey(testProfile, testPublicKey)
            repository.removePublicKey(testProfile)
            val retrieved = repository.getPublicKey(testProfile)
            
            retrieved.shouldBeNull()
        }

        should("handle multiple profiles") {
            val repository = InMemoryPublicKeyRepository()
            val profile2 = LoxoneProfile(
                LoxoneEndpoint.local("192.168.1.101"),
                LoxoneCredentials("user2", "password2")
            )
            val publicKey2 = PublicKey("different-key-for-profile2")
            
            repository.putPublicKey(testProfile, testPublicKey)
            repository.putPublicKey(profile2, publicKey2)
            
            repository.getPublicKey(testProfile)?.publicKey shouldBe testPublicKey.publicKey
            repository.getPublicKey(profile2)?.publicKey shouldBe publicKey2.publicKey
        }
    }
})
