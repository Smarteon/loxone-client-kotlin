package cz.smarteon.loxkt.event

import cz.smarteon.loxkt.Codec
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldBe

/**
 * Tests for [Codec.readWeatherEvents].
 *
 * Weather events have variable size:
 * - UUID: 16 bytes (little endian)
 * - Last update: 4 bytes (uint32, little endian) - seconds since 2009 UTC
 * - Entry count: 4 bytes (int32, little endian)
 * - Entries: 68 bytes each:
 *   - Timestamp: 4 bytes (int32)
 *   - Weather type: 4 bytes (int32)
 *   - Wind direction: 4 bytes (int32)
 *   - Solar radiation: 4 bytes (int32)
 *   - Relative humidity: 4 bytes (int32)
 *   - Temperature: 8 bytes (double)
 *   - Perceived temperature: 8 bytes (double)
 *   - Dew point: 8 bytes (double)
 *   - Precipitation: 8 bytes (double)
 *   - Wind speed: 8 bytes (double)
 *   - Barometric pressure: 8 bytes (double)
 *
 * Minimum size: 24 bytes (header only, no entries)
 */
@OptIn(ExperimentalStdlibApi::class)
class WeatherEventTest : ShouldSpec({

    context("readWeatherEvents") {
        should("return empty list for empty input") {
            Codec.readWeatherEvents(ByteArray(0)).shouldBeEmpty()
        }

        should("parse weather event with no entries") {
            // UUID: 11111111-2222-3333-4444444444444444
            // Last update: 500000000 (0x1DCD6500)
            // Entry count: 0
            val uuid = "11111111" + "2222" + "3333" + "4444444444444444"
            val lastUpdate = "0065cd1d" // 500000000 little endian
            val entryCount = "00000000"
            val bytes = (uuid + lastUpdate + entryCount).hexToByteArray()

            val events = Codec.readWeatherEvents(bytes)

            events shouldHaveSize 1
            events[0].uuid shouldBe "11111111-2222-3333-4444444444444444"
            events[0].lastUpdate shouldBe 500000000u
            events[0].entries.shouldBeEmpty()
        }

        should("parse weather event with single entry") {
            // UUID: aaaaaaaa-bbbb-cccc-dddddddddddddddd
            // Last update: 600000000 (0x23C34600)
            // Entry count: 1
            // Entry:
            //   timestamp=1000, weatherType=1, windDirection=180,
            //   solarRadiation=500, relativeHumidity=65,
            //   temperature=22.5, perceivedTemperature=21.0,
            //   dewPoint=15.0, precipitation=0.0,
            //   windSpeed=5.5, barometricPressure=1013.25
            val uuid = "aaaaaaaa" + "bbbb" + "cccc" + "dddddddddddddddd"
            val lastUpdate = "0046c323" // 600000000 little endian
            val entryCount = "01000000" // 1

            // Entry data (68 bytes)
            val timestamp = "e8030000" // 1000 little endian
            val weatherType = "01000000" // 1
            val windDirection = "b4000000" // 180
            val solarRadiation = "f4010000" // 500
            val relativeHumidity = "41000000" // 65
            val temperature = "0000000000803640" // 22.5
            val perceivedTemp = "0000000000003540" // 21.0
            val dewPoint = "0000000000002e40" // 15.0
            val precipitation = "0000000000000000" // 0.0
            val windSpeed = "0000000000001640" // 5.5
            val pressure = "0000000000aa8f40" // 1013.25

            val bytes = (uuid + lastUpdate + entryCount +
                         timestamp + weatherType + windDirection + solarRadiation + relativeHumidity +
                         temperature + perceivedTemp + dewPoint + precipitation + windSpeed + pressure
            ).hexToByteArray()

            val events = Codec.readWeatherEvents(bytes)

            events shouldHaveSize 1
            events[0].uuid shouldBe "aaaaaaaa-bbbb-cccc-dddddddddddddddd"
            events[0].lastUpdate shouldBe 600000000u
            events[0].entries shouldHaveSize 1

            val entry = events[0].entries[0]
            entry.timestamp shouldBe 1000
            entry.weatherType shouldBe 1
            entry.windDirection shouldBe 180
            entry.solarRadiation shouldBe 500
            entry.relativeHumidity shouldBe 65
            entry.temperature shouldBeExactly 22.5
            entry.perceivedTemperature shouldBeExactly 21.0
            entry.dewPoint shouldBeExactly 15.0
            entry.precipitation shouldBeExactly 0.0
            entry.windSpeed shouldBeExactly 5.5
            entry.barometricPressure shouldBeExactly 1013.25
        }

        should("parse weather event with multiple entries") {
            // UUID: 12345678-1234-5678-0000000000000000
            // Last update: 700000000
            // 2 entries
            val uuid = "78563412" + "3412" + "7856" + "0000000000000000"
            val lastUpdate = "00c5c829" // 700000000 little endian
            val entryCount = "02000000" // 2

            // Entry 1: timestamp=2000, simple values
            val entry1 = "d0070000" + // timestamp 2000
                         "02000000" + // weatherType 2
                         "5a000000" + // windDirection 90
                         "c8000000" + // solarRadiation 200
                         "32000000" + // relativeHumidity 50
                         "0000000000003440" + // temperature 20.0
                         "0000000000003440" + // perceivedTemperature 20.0
                         "0000000000002840" + // dewPoint 12.0
                         "0000000000000000" + // precipitation 0.0
                         "0000000000002040" + // windSpeed 8.0
                         "00000000008a8f40"   // barometricPressure 1010.5

            // Entry 2: timestamp=3000, different values
            val entry2 = "b80b0000" + // timestamp 3000
                         "03000000" + // weatherType 3
                         "0e010000" + // windDirection 270
                         "2c010000" + // solarRadiation 300
                         "46000000" + // relativeHumidity 70
                         "0000000000003940" + // temperature 25.0
                         "0000000000003b40" + // perceivedTemperature 27.0
                         "0000000000003240" + // dewPoint 18.0
                         "9a9999999999a93f" + // precipitation 0.05
                         "0000000000001040" + // windSpeed 4.0
                         "0000000000948f40"   // barometricPressure 1012.5

            val bytes = (uuid + lastUpdate + entryCount + entry1 + entry2).hexToByteArray()

            val events = Codec.readWeatherEvents(bytes)

            events shouldHaveSize 1
            events[0].entries shouldHaveSize 2

            events[0].entries[0].timestamp shouldBe 2000
            events[0].entries[0].weatherType shouldBe 2
            events[0].entries[0].windDirection shouldBe 90
            events[0].entries[1].timestamp shouldBe 3000
            events[0].entries[1].weatherType shouldBe 3
            events[0].entries[1].windDirection shouldBe 270
        }

        should("handle truncated header gracefully") {
            // Only 20 bytes - less than minimum header size of 24
            val partialHeader = "11111111" + "2222" + "3333" + "4444444444444444" + "0000"
            val bytes = partialHeader.hexToByteArray()

            val events = Codec.readWeatherEvents(bytes)

            events.shouldBeEmpty()
        }

        should("handle truncated entries gracefully") {
            // Header says 2 entries but only space for 1
            val uuid = "11111111" + "2222" + "3333" + "4444444444444444"
            val lastUpdate = "0065cd1d"
            val entryCount = "02000000" // Claims 2 entries

            // Only 1 complete entry (68 bytes)
            val entry1 = "e8030000" + "01000000" + "b4000000" + "f4010000" + "41000000" +
                         "0000000000803640" + "0000000000003540" + "0000000000002e40" +
                         "0000000000000000" + "0000000000001640" + "0000000000a48f40"

            val bytes = (uuid + lastUpdate + entryCount + entry1).hexToByteArray()

            val events = Codec.readWeatherEvents(bytes)

            events.shouldBeEmpty()
        }

        should("handle negative entry count gracefully") {
            // Header with negative entry count (-1 = 0xFFFFFFFF)
            val uuid = "11111111" + "2222" + "3333" + "4444444444444444"
            val lastUpdate = "0065cd1d"
            val entryCount = "ffffffff" // -1 in little endian signed int

            val bytes = (uuid + lastUpdate + entryCount).hexToByteArray()

            val events = Codec.readWeatherEvents(bytes)

            events.shouldBeEmpty()
        }

        should("parse multiple weather events") {
            // Two complete events, each with 0 entries
            val event1Uuid = "11111111" + "1111" + "1111" + "1111111111111111"
            val event1Update = "01000000"
            val event1Count = "00000000"

            val event2Uuid = "22222222" + "2222" + "2222" + "2222222222222222"
            val event2Update = "02000000"
            val event2Count = "00000000"

            val bytes = (event1Uuid + event1Update + event1Count +
                         event2Uuid + event2Update + event2Count).hexToByteArray()

            val events = Codec.readWeatherEvents(bytes)

            events shouldHaveSize 2
            events[0].uuid shouldBe "11111111-1111-1111-1111111111111111"
            events[0].lastUpdate shouldBe 1u
            events[1].uuid shouldBe "22222222-2222-2222-2222222222222222"
            events[1].lastUpdate shouldBe 2u
        }
    }
})
