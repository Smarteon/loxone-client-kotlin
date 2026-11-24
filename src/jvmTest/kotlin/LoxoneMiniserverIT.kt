package cz.smarteon.loxkt

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LoxoneMiniserverIT : StringSpec({

    "close should abort a connect in progress" {
        // Not routable: the WebSocket neither opens nor fails right away, like over a dead network.
        val miniserver = LoxoneMiniserver(LoxoneEndpoint.local("10.255.255.1"), "user", "pass")

        val attempt = async { runCatching { miniserver.connect() } }
        delay(200.milliseconds)

        withTimeout(2.seconds) { miniserver.close() }

        // ended by close(), not left to run into its own timeouts
        withTimeout(2.seconds) { attempt.await() }.isFailure shouldBe true
    }
})
