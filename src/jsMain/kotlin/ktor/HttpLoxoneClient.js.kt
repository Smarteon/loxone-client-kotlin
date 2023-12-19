package cz.smarteon.loxone.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*

internal actual val clientEngineFactory: HttpClientEngineFactory<*>
    get() = Js
