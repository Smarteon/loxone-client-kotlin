package cz.smarteon.loxkt.state

import cz.smarteon.loxkt.event.LoxoneEvent
import cz.smarteon.loxkt.event.TextEvent
import cz.smarteon.loxkt.event.ValueEvent
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf

class StateCollectorTest : ShouldSpec({

    context("collectFrom") {
        should("collect value events from flow") {
            val state = LoxoneState()
            val events = flowOf<LoxoneEvent>(
                ValueEvent("uuid-1", 42.0),
                ValueEvent("uuid-2", 100.0)
            )

            state.collectFrom(events)

            state.getValue("uuid-1") shouldBe 42.0
            state.getValue("uuid-2") shouldBe 100.0
        }

        should("collect mixed event types from flow") {
            val state = LoxoneState()
            val events = flowOf<LoxoneEvent>(
                ValueEvent("uuid-value", 42.0),
                TextEvent("uuid-text", "icon", "Hello")
            )

            state.collectFrom(events)

            state.getValue("uuid-value") shouldBe 42.0
            state.getText("uuid-text")?.text shouldBe "Hello"
        }

        should("update state when same UUID emits again") {
            val state = LoxoneState()
            val events = flowOf<LoxoneEvent>(
                ValueEvent("uuid-1", 10.0),
                ValueEvent("uuid-1", 20.0)
            )

            state.collectFrom(events)

            state.getValue("uuid-1") shouldBe 20.0
        }

        should("handle high-frequency events") {
            val state = LoxoneState()
            val eventList = (0 until 1000).map { i ->
                ValueEvent("uuid-$i", i.toDouble())
            }
            val events = flowOf<LoxoneEvent>(*eventList.toTypedArray())

            state.collectFrom(events)

            state.size() shouldBe 1000
            state.getValue("uuid-0") shouldBe 0.0
            state.getValue("uuid-999") shouldBe 999.0
        }
    }
})
