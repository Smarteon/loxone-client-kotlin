package cz.smarteon.loxone.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*

internal actual val clientEngineFactory: HttpClientEngineFactory<*>
    get() = CIO
