package cz.smarteon.loxkt.app

import com.ditchoom.buffer.ByteOrder
import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.allocate
import cz.smarteon.loxkt.LoxoneCommands
import cz.smarteon.loxkt.MockLoxoneClient
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

class ControlStatisticExtensionsTest : ShouldSpec({

    fun buildBinaryData(vararg entries: Pair<UInt, List<Double>>): ByteArray {
        val outputCount = entries.firstOrNull()?.second?.size ?: 1
        val entrySize = 4 + outputCount * 8
        val buffer = PlatformBuffer.allocate(entries.size * entrySize, byteOrder = ByteOrder.LITTLE_ENDIAN)
        entries.forEach { (ts, values) ->
            buffer.writeUInt(ts)
            values.forEach { buffer.writeDouble(it) }
        }
        buffer.resetForRead()
        return buffer.readByteArray(entries.size * entrySize)
    }

    // August 2023: 2023-08-01 00:00:00 UTC = 1690848000, 2023-08-31 23:59:59 UTC = 1693526399
    val fromEpoch = 1690848000L
    val untilEpoch = 1693526399L
    val from = Instant.fromEpochSeconds(fromEpoch)
    val until = Instant.fromEpochSeconds(untilEpoch)

    context("fetchStatistics auto-detect") {

        val client = MockLoxoneClient()

        should("fetch V2 diff with DAY unit by default for V2 controls") {
            val control = Control(
                name = "Energy Meter", type = "Meter", uuidAction = "meter-uuid",
                statisticV2 = StatisticV2(groups = listOf(
                    StatisticGroup(id = 1, mode = 1, dataPoints = listOf(StatisticDataPoint(title = "Total", output = "total")))
                ))
            )
            val path = LoxoneCommands.Statistics.getStatisticDiff("meter-uuid", fromEpoch to untilEpoch, groupId = 1, unit = StatisticUnit.DAY)
            client.stubRawData(path, buildBinaryData(1690848000u to listOf(12.5), 1690934400u to listOf(11.0)))

            val result = control.fetchStatistics(client, from, until)

            result shouldHaveSize 2
            result[0].values[0] shouldBe 12.5
        }

        should("pass custom unit through for V2") {
            val control = Control(
                name = "Meter", type = "Meter", uuidAction = "meter-uuid",
                statisticV2 = StatisticV2(groups = listOf(StatisticGroup(id = 1, mode = 1)))
            )
            val path = LoxoneCommands.Statistics.getStatisticDiff("meter-uuid", fromEpoch to untilEpoch, groupId = 1, unit = StatisticUnit.MONTH)
            client.stubRawData(path, buildBinaryData(1690848000u to listOf(300.0)))

            control.fetchStatistics(client, from, until, unit = StatisticUnit.MONTH) shouldHaveSize 1
        }

        should("fetch V1 XML for a full month and return all entries") {
            val control = Control(
                name = "CO2", type = "InfoOnlyAnalog", uuidAction = "sensor-uuid",
                statistic = Statistic(frequency = 11, outputs = listOf(
                    StatisticOutput(id = 0, name = "CO2", format = "%.0f", uuid = "out-uuid", visuType = 0)
                ))
            )
            val xml = """<?xml version="1.0" encoding="utf-8"?>
<Statistics UUID="sensor-uuid">
<S T="2023-08-01 00:00:00" V="450.000"/>
<S T="2023-08-31 23:00:00" V="460.000"/>
</Statistics>"""
            // from/until span the whole of August 2023 in UTC → single fetch for "202308"
            client.stubRawString(LoxoneCommands.Statistics.statisticDataXml("sensor-uuid", "202308"), xml)

            val result = control.fetchStatistics(client, from, until, miniserverTimeZone = TimeZone.UTC)

            result shouldHaveSize 2
            result[0].values[0] shouldBe 450.0
            result[1].values[0] shouldBe 460.0
        }

        should("filter V1 entries to only those within the requested range") {
            val control = Control(
                name = "CO2", type = "InfoOnlyAnalog", uuidAction = "sensor-uuid",
                statistic = Statistic(frequency = 11, outputs = listOf(
                    StatisticOutput(id = 0, name = "CO2", format = "%.0f", uuid = "out-uuid", visuType = 0)
                ))
            )
            // 2023-08-15 00:00:00 UTC = 1692057600
            // 2023-08-28 23:00:00 UTC = 2023-08-28 00:00:00 (1693180800) + 23h (82800) = 1693263600
            val twoWeekFrom = Instant.fromEpochSeconds(1692057600L)
            val twoWeekUntil = Instant.fromEpochSeconds(1693263600L)
            val xml = """<?xml version="1.0" encoding="utf-8"?>
<Statistics UUID="sensor-uuid">
<S T="2023-08-01 00:00:00" V="100.000"/>
<S T="2023-08-15 00:00:00" V="200.000"/>
<S T="2023-08-28 23:00:00" V="300.000"/>
<S T="2023-08-31 00:00:00" V="400.000"/>
</Statistics>"""
            client.stubRawString(LoxoneCommands.Statistics.statisticDataXml("sensor-uuid", "202308"), xml)

            val result = control.fetchStatistics(client, twoWeekFrom, twoWeekUntil, miniserverTimeZone = TimeZone.UTC)

            // Only the two entries within [Aug 15 .. Aug 28 23:00] should survive
            result shouldHaveSize 2
            result[0].values[0] shouldBe 200.0
            result[1].values[0] shouldBe 300.0
        }

        should("fetch multiple months and filter when range spans a month boundary") {
            val control = Control(
                name = "CO2", type = "InfoOnlyAnalog", uuidAction = "sensor-uuid",
                statistic = Statistic(frequency = 11, outputs = listOf(
                    StatisticOutput(id = 0, name = "CO2", format = "%.0f", uuid = "out-uuid", visuType = 0)
                ))
            )
            // Range: 2023-08-15 00:00:00 UTC to 2023-09-15 00:00:00 UTC
            val multiFrom = Instant.fromEpochSeconds(1692057600L)  // 2023-08-15 00:00:00 UTC
            val multiUntil = Instant.fromEpochSeconds(1694736000L) // 2023-09-15 00:00:00 UTC
            val xmlAug = """<?xml version="1.0" encoding="utf-8"?>
<Statistics UUID="sensor-uuid">
<S T="2023-08-01 00:00:00" V="10.000"/>
<S T="2023-08-15 00:00:00" V="20.000"/>
<S T="2023-08-31 23:00:00" V="30.000"/>
</Statistics>"""
            val xmlSep = """<?xml version="1.0" encoding="utf-8"?>
<Statistics UUID="sensor-uuid">
<S T="2023-09-01 00:00:00" V="40.000"/>
<S T="2023-09-15 00:00:00" V="50.000"/>
<S T="2023-09-30 00:00:00" V="60.000"/>
</Statistics>"""
            client.stubRawString(LoxoneCommands.Statistics.statisticDataXml("sensor-uuid", "202308"), xmlAug)
            client.stubRawString(LoxoneCommands.Statistics.statisticDataXml("sensor-uuid", "202309"), xmlSep)

            val result = control.fetchStatistics(client, multiFrom, multiUntil, miniserverTimeZone = TimeZone.UTC)

            // Aug 1 and Sep 30 entries fall outside the range; the four in the middle remain
            result shouldHaveSize 4
            result.map { it.values[0] } shouldBe listOf(20.0, 30.0, 40.0, 50.0)
        }

        should("return empty list for controls with no statistics") {
            val control = Control(name = "Switch", type = "Switch", uuidAction = "sw-uuid")
            control.fetchStatistics(client, from, until).shouldBeEmpty()
        }
    }

    context("fetchV1Http") {

        val client = MockLoxoneClient()

        should("parse S entries from XML response") {
            val control = Control(
                name = "Temp", type = "InfoOnlyAnalog", uuidAction = "ctrl-uuid",
                statistic = Statistic(frequency = 6, outputs = listOf(
                    StatisticOutput(id = 0, name = "temp", format = "%.1f", uuid = "out-uuid", visuType = 0)
                ))
            )
            val xml = """<?xml version="1.0" encoding="utf-8"?>
<Statistics UUID="ctrl-uuid">
<S T="2023-08-01 00:00:00" V="21.500"/>
<S T="2023-08-01 01:00:00" V="22.000"/>
</Statistics>"""
            client.stubRawString(LoxoneCommands.Statistics.statisticDataXml("ctrl-uuid", "202308"), xml)

            val result = control.fetchV1Http(client, "202308")

            result shouldHaveSize 2
            result[0].values[0] shouldBe 21.5
            result[1].values[0] shouldBe 22.0
            result[1].timestamp shouldBe result[0].timestamp + 3600u
        }

        should("return empty list for empty XML") {
            val control = Control(
                name = "Temp", type = "InfoOnlyAnalog", uuidAction = "ctrl-uuid",
                statistic = Statistic(frequency = 6, outputs = emptyList())
            )
            val xml = """<?xml version="1.0" encoding="utf-8"?>
<Statistics UUID="ctrl-uuid">
</Statistics>"""
            client.stubRawString(LoxoneCommands.Statistics.statisticDataXml("ctrl-uuid", "202308"), xml)

            control.fetchV1Http(client, "202308").shouldBeEmpty()
        }

        should("return empty list when control has no V1 statistic") {
            val control = Control(name = "Meter", type = "Meter", uuidAction = "m-uuid",
                statisticV2 = StatisticV2(groups = listOf(StatisticGroup(id = 1, mode = 1))))
            control.fetchV1Http(client, "202308").shouldBeEmpty()
        }
    }

    context("fetchV2Raw") {

        val client = MockLoxoneClient()

        should("return entries from raw binary response") {
            val control = Control(
                name = "Meter", type = "Meter", uuidAction = "meter-uuid",
                statisticV2 = StatisticV2(groups = listOf(StatisticGroup(id = 1, mode = 1)))
            )
            val path = LoxoneCommands.Statistics.getStatisticRaw("meter-uuid", fromEpoch to untilEpoch, groupId = 1, unit = StatisticUnit.ALL)
            client.stubRawData(path, buildBinaryData(1690848000u to listOf(5.5), 1690934400u to listOf(6.2)))

            val result = control.fetchV2Raw(client, fromEpoch, untilEpoch)

            result shouldHaveSize 2
            result[0] shouldBe StatisticEntry(1690848000u, listOf(5.5))
        }

        should("return empty list when control has no V2 statistic") {
            val control = Control(name = "Sensor", type = "InfoOnlyAnalog", uuidAction = "s-uuid",
                statistic = Statistic(frequency = 1, outputs = emptyList()))
            control.fetchV2Raw(client, fromEpoch, untilEpoch).shouldBeEmpty()
        }
    }

    context("fetchV2Diff") {

        val client = MockLoxoneClient()

        should("return diff entries per day") {
            val control = Control(
                name = "Battery", type = "PowerUnit", uuidAction = "bat-uuid",
                statisticV2 = StatisticV2(groups = listOf(
                    StatisticGroup(id = 2, mode = 7, dataPoints = listOf(StatisticDataPoint(output = "totalNeg")))
                ))
            )
            val path = LoxoneCommands.Statistics.getStatisticDiff("bat-uuid", fromEpoch to untilEpoch, groupId = 2, unit = StatisticUnit.DAY)
            client.stubRawData(path, buildBinaryData(1690848000u to listOf(6.5), 1690934400u to listOf(7.0)))

            val result = control.fetchV2Diff(client, fromEpoch, untilEpoch)

            result shouldHaveSize 2
            result[0].values[0] shouldBe 6.5
        }
    }
})
