package cz.smarteon.loxkt.state

import cz.smarteon.loxkt.event.DaytimerEntry
import cz.smarteon.loxkt.event.DaytimerEvent
import cz.smarteon.loxkt.event.TextEvent
import cz.smarteon.loxkt.event.ValueEvent
import cz.smarteon.loxkt.event.WeatherEntry
import cz.smarteon.loxkt.event.WeatherEvent
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class LoxoneStateTest : ShouldSpec({

    context("LoxoneState") {
        context("value events") {
            should("store and retrieve value event") {
                val state = LoxoneState()
                val event = ValueEvent("uuid-1", 42.5)

                state.update(event)

                state["uuid-1"].shouldNotBeNull()
                state["uuid-1"].shouldBeInstanceOf<ValueState>()
                state.getValue("uuid-1") shouldBe 42.5
            }

            should("overwrite existing value") {
                val state = LoxoneState()

                state.update(ValueEvent("uuid-1", 10.0))
                state.update(ValueEvent("uuid-1", 20.0))

                state.getValue("uuid-1") shouldBe 20.0
            }

            should("return null for unknown UUID") {
                val state = LoxoneState()

                state.getValue("unknown-uuid").shouldBeNull()
                state["unknown-uuid"].shouldBeNull()
            }
        }

        context("text events") {
            should("store and retrieve text event") {
                val state = LoxoneState()
                val event = TextEvent("uuid-text", "icon-uuid", "Hello World")

                state.update(event)

                state["uuid-text"].shouldNotBeNull()
                state["uuid-text"].shouldBeInstanceOf<TextState>()
                val textState = state.getText("uuid-text")
                textState.shouldNotBeNull()
                textState.text shouldBe "Hello World"
                textState.iconUuid shouldBe "icon-uuid"
            }

            should("return null when accessing value as text") {
                val state = LoxoneState()
                state.update(ValueEvent("uuid-value", 42.0))

                state.getText("uuid-value").shouldBeNull()
            }
        }

        context("daytimer events") {
            should("store and retrieve daytimer event") {
                val state = LoxoneState()
                val entries = listOf(
                    DaytimerEntry(mode = 0, from = 480, to = 1020, needActivate = false, value = 21.0),
                    DaytimerEntry(mode = 1, from = 0, to = 480, needActivate = false, value = 18.0)
                )
                val event = DaytimerEvent("uuid-daytimer", 15.0, entries)

                state.update(event)

                state["uuid-daytimer"].shouldNotBeNull()
                state["uuid-daytimer"].shouldBeInstanceOf<DaytimerState>()
                val daytimerState = state.getDaytimer("uuid-daytimer")
                daytimerState.shouldNotBeNull()
                daytimerState.defaultValue shouldBe 15.0
                daytimerState.entries shouldHaveSize 2
                daytimerState.entries[0].from shouldBe 480
            }
        }

        context("weather events") {
            should("store and retrieve weather event") {
                val state = LoxoneState()
                val entries = listOf(
                    WeatherEntry(
                        timestamp = 12345,
                        weatherType = 1,
                        windDirection = 180,
                        solarRadiation = 500,
                        relativeHumidity = 65,
                        temperature = 22.5,
                        perceivedTemperature = 21.0,
                        dewPoint = 15.0,
                        precipitation = 0.0,
                        windSpeed = 10.5,
                        barometricPressure = 1013.25
                    )
                )
                val event = WeatherEvent("uuid-weather", 100000, entries)

                state.update(event)

                state["uuid-weather"].shouldNotBeNull()
                state["uuid-weather"].shouldBeInstanceOf<WeatherState>()
                val weatherState = state.getWeather("uuid-weather")
                weatherState.shouldNotBeNull()
                weatherState.lastUpdate shouldBe 100000
                weatherState.entries shouldHaveSize 1
                weatherState.entries[0].temperature shouldBe 22.5
            }
        }

        context("typed accessor") {
            should("return typed value when type matches") {
                val state = LoxoneState()
                state.update(ValueEvent("uuid-1", 42.0))

                val typed = state.getTyped<ValueState>("uuid-1")
                typed.shouldNotBeNull()
                typed.value shouldBe 42.0
            }

            should("return null when type does not match") {
                val state = LoxoneState()
                state.update(ValueEvent("uuid-1", 42.0))

                state.getTyped<TextState>("uuid-1").shouldBeNull()
            }
        }

        context("utility functions") {
            should("report correct size") {
                val state = LoxoneState()

                state.size() shouldBe 0

                state.update(ValueEvent("uuid-1", 1.0))
                state.size() shouldBe 1

                state.update(ValueEvent("uuid-2", 2.0))
                state.size() shouldBe 2

                state.update(ValueEvent("uuid-1", 3.0)) // overwrite
                state.size() shouldBe 2
            }

            should("return all UUIDs") {
                val state = LoxoneState()
                state.update(ValueEvent("uuid-1", 1.0))
                state.update(TextEvent("uuid-2", "icon", "text"))
                state.update(ValueEvent("uuid-3", 3.0))

                val uuids = state.getAllUuids()
                uuids shouldHaveSize 3
                uuids.toSet() shouldBe setOf("uuid-1", "uuid-2", "uuid-3")
            }

            should("check if contains UUID") {
                val state = LoxoneState()
                state.update(ValueEvent("uuid-1", 1.0))

                state.contains("uuid-1") shouldBe true
                state.contains("uuid-unknown") shouldBe false
            }

            should("clear all states") {
                val state = LoxoneState()
                state.update(ValueEvent("uuid-1", 1.0))
                state.update(ValueEvent("uuid-2", 2.0))

                state.clear()

                state.size() shouldBe 0
                state["uuid-1"].shouldBeNull()
            }
        }

        context("mixed event types") {
            should("store different event types with same prefix") {
                val state = LoxoneState()

                state.update(ValueEvent("control-value", 42.0))
                state.update(TextEvent("control-text", "icon", "Hello"))

                state.getValue("control-value") shouldBe 42.0
                state.getText("control-text")?.text shouldBe "Hello"
            }

            should("overwrite with different event type") {
                val state = LoxoneState()

                // First store a value event
                state.update(ValueEvent("uuid-1", 42.0))
                state.getValue("uuid-1") shouldBe 42.0

                // Then overwrite with text event
                state.update(TextEvent("uuid-1", "icon", "Now text"))
                state.getValue("uuid-1").shouldBeNull() // old accessor returns null
                state.getText("uuid-1")?.text shouldBe "Now text"
            }
        }
    }
})
