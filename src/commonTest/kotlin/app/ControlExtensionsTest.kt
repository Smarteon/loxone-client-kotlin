package cz.smarteon.loxkt.app

import cz.smarteon.loxkt.event.DaytimerEntry
import cz.smarteon.loxkt.event.DaytimerEvent
import cz.smarteon.loxkt.event.TextEvent
import cz.smarteon.loxkt.event.ValueEvent
import cz.smarteon.loxkt.event.WeatherEntry
import cz.smarteon.loxkt.event.WeatherEvent
import cz.smarteon.loxkt.state.LoxoneState
import cz.smarteon.loxkt.state.TextState
import cz.smarteon.loxkt.state.ValueState
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe

class ControlExtensionsTest : ShouldSpec({

    context("ControlExtensions") {

        val control = Control(
            name = "Test Control",
            type = "InfoOnlyAnalog",
            uuidAction = "control-uuid",
            states = mapOf(
                "value" to "uuid-value",
                "text" to "uuid-text",
                "daytimer" to "uuid-daytimer",
                "weather" to "uuid-weather",
                "missing" to "uuid-missing"
            )
        )

        should("getValue returns value when state exists") {
            val state = LoxoneState()
            state.update(ValueEvent("uuid-value", 123.45))

            control.getValue(state, "value") shouldBe 123.45
        }

        should("getValue returns null when state name doesn't exist on control") {
            val state = LoxoneState()
            state.update(ValueEvent("uuid-value", 123.45))

            control.getValue(state, "unknown") shouldBe null
        }

        should("getValue returns null when state value is missing in LoxoneState") {
            val state = LoxoneState()
            // uuid-value not updated in state

            control.getValue(state, "value") shouldBe null
        }

        should("getText returns text state when state exists") {
            val state = LoxoneState()
            state.update(TextEvent("uuid-text", "icon-uuid", "Hello"))

            val result = control.getText(state, "text")
            result?.text shouldBe "Hello"
        }

        should("getDaytimer returns daytimer state when state exists") {
            val state = LoxoneState()
            val entries = listOf(DaytimerEntry(mode = 0, from = 480, to = 1200, needActivate = false, value = 1.0))
            state.update(DaytimerEvent("uuid-daytimer", 0.0, entries))

            val result = control.getDaytimer(state, "daytimer")
            result?.entries?.first()?.from shouldBe 480
        }

        should("getWeather returns weather state when state exists") {
            val state = LoxoneState()
            val entries = listOf(WeatherEntry(timestamp = 1000, weatherType = 1, windDirection = 0, solarRadiation = 0, relativeHumidity = 0, temperature = 20.0, perceivedTemperature = 20.0, dewPoint = 10.0, precipitation = 0.0, windSpeed = 0.0, barometricPressure = 1000.0))
            state.update(WeatherEvent("uuid-weather", 2000u, entries))

            val result = control.getWeather(state, "weather")
            result?.lastUpdate shouldBe 2000u
        }

        should("getStateValue returns raw state value") {
            val state = LoxoneState()
            state.update(ValueEvent("uuid-value", 42.0))

            val result = control.getStateValue(state, "value")
            (result as? ValueState)?.value shouldBe 42.0
        }

        should("getAllValues returns all available states") {
            val state = LoxoneState()
            state.update(ValueEvent("uuid-value", 10.0))
            state.update(TextEvent("uuid-text", "icon", "Status"))
            // uuid-missing is not in state

            val result = control.getAllValues(state)

            result shouldHaveSize 2
            (result["value"] as ValueState).value shouldBe 10.0
            (result["text"] as TextState).text shouldBe "Status"
        }
    }
})

