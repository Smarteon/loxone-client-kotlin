package cz.smarteon.loxkt.app

import cz.smarteon.loxkt.Codec
import cz.smarteon.loxkt.LoxoneClient
import cz.smarteon.loxkt.LoxoneCommands
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private val DEFAULT_TIMEZONE: TimeZone get() = TimeZone.currentSystemDefault()

// Matches <S T="YYYY-MM-DD HH:MM:SS" V="float"/> entries in V1 statisticdata.xml
private val V1_XML_ENTRY = Regex("""<S T="(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})" V="([^"]+)"/>""")

/**
 * A single entry in a statistic time-series.
 *
 * For V1 statistics, [timestamp] is seconds since 1.1.2009 (Loxone epoch) in local Miniserver time.
 * For V2 statistics, [timestamp] is a Unix UTC timestamp.
 *
 * @property timestamp Timestamp of the recorded data point
 * @property values List of recorded values, one per output/data-point in the statistic configuration
 */
data class StatisticEntry(
    val timestamp: UInt,
    val values: List<Double>,
)

/**
 * Resolution for grouping V2 statistic data points.
 *
 * Used in [cz.smarteon.loxkt.app.fetchV2Raw], [cz.smarteon.loxkt.app.fetchV2Diff],
 * and [cz.smarteon.loxkt.app.fetchStatistics].
 * Ignored for V1 controls (which always return data as-recorded).
 */
enum class StatisticUnit(val value: String) {
    /** All available data points as recorded */
    ALL("all"),

    /** One data point per hour */
    HOUR("hour"),

    /** One data point per day */
    DAY("day"),

    /** One data point per month */
    MONTH("month"),

    /** One data point per year */
    YEAR("year"),
}

/**
 * Fetch historical statistic data for this control.
 *
 * Accepts a single `from`/`until` [Instant] range that works for both control generations:
 * - For **V2 controls** (`statisticV2` set): fetches preprocessed diff data at [unit] resolution.
 *   Timestamps in the result are Unix UTC.
 * - For **V1 controls** (`statistic` set): fetches all months overlapping the `from`/`until` range
 *   (determined in [miniserverTimeZone]), then filters the results to only entries within the range.
 *   Timestamps are seconds since Loxone epoch (1.1.2009) in local Miniserver time.
 *   The [unit] parameter is ignored for V1 — use [fetchV1Http] directly for a specific month or day.
 * - Returns empty list if the control has no statistics.
 *
 * @param client Loxone client
 * @param from Start of the time range (inclusive)
 * @param until End of the time range (inclusive)
 * @param unit Resolution for V2 data points. Defaults to [StatisticUnit.DAY]. Ignored for V1.
 * @param miniserverTimeZone The Miniserver's local timezone. Used to enumerate V1 months from the
 *   range and to interpret the wall-clock timestamps in the XML response.
 *   Defaults to [TimeZone.currentSystemDefault].
 */
suspend fun Control.fetchStatistics(
    client: LoxoneClient,
    from: Instant = Clock.System.now(),
    until: Instant = Clock.System.now(),
    unit: StatisticUnit = StatisticUnit.DAY,
    miniserverTimeZone: TimeZone = DEFAULT_TIMEZONE,
): List<StatisticEntry> = when {
    statisticV2 != null -> {
        val groupId = statisticV2.groups.firstOrNull()?.id ?: return emptyList()
        fetchV2Diff(client, from.epochSeconds, until.epochSeconds, groupId, unit)
    }
    statistic != null -> {
        val fromTs = Codec.instantToLoxoneTimestamp(from, miniserverTimeZone)
        val untilTs = Codec.instantToLoxoneTimestamp(until, miniserverTimeZone)
        v1Months(from, until, miniserverTimeZone)
            .flatMap { month -> fetchV1Http(client, month, miniserverTimeZone) }
            .filter { it.timestamp in fromTs..untilTs }
    }
    else -> emptyList()
}

/**
 * Fetch V1 statistic data for this control via XML.
 *
 * Only valid for controls with a [Control.statistic] configuration.
 * Uses `stats/statisticdata.xml/{controlUuid}/{date}` over HTTP.
 * Timestamps are seconds since Loxone epoch (1.1.2009) in local Miniserver time.
 *
 * @param client Loxone client
 * @param date Date in format `YYYYMM` (full month) or `YYYYMMDD` (single day)
 * @param miniserverTimeZone The Miniserver's local timezone, used to correctly interpret the XML timestamps.
 *   Defaults to [TimeZone.currentSystemDefault]. Pass the actual Miniserver timezone to avoid
 *   incorrect offsets when client and Miniserver are in different regions.
 * @return Entries for the period, or empty list if the control has no V1 stats or no data
 */
suspend fun Control.fetchV1Http(
    client: LoxoneClient,
    date: String,
    miniserverTimeZone: TimeZone = DEFAULT_TIMEZONE,
): List<StatisticEntry> {
    statistic ?: return emptyList()
    return parseV1Xml(
        client.callRaw(LoxoneCommands.Statistics.statisticDataXml(uuidAction, date)),
        miniserverTimeZone,
    )
}

/**
 * Fetch V2 raw statistic data for this control.
 *
 * Only valid for controls with a [Control.statisticV2] configuration.
 *
 * @param client Loxone client
 * @param from Start of the time range as Unix UTC timestamp (inclusive)
 * @param until End of the time range as Unix UTC timestamp (inclusive)
 * @param groupId Statistic group to request; defaults to the first group's id
 * @param unit Grouping resolution for the returned data points
 * @param outputName Optional output filter; if null all outputs of the group are returned
 */
@Suppress("LongParameterList")
suspend fun Control.fetchV2Raw(
    client: LoxoneClient,
    from: Long,
    until: Long,
    groupId: Int = statisticV2?.groups?.firstOrNull()?.id ?: 0,
    unit: StatisticUnit = StatisticUnit.ALL,
    outputName: String? = null,
): List<StatisticEntry> {
    statisticV2 ?: return emptyList()
    val path = LoxoneCommands.Statistics.getStatisticRaw(uuidAction, from to until, groupId, unit, outputName)
    return Codec.readStatisticEntries(client.callRawForData(path), if (outputName != null) 1 else null)
}

/**
 * Fetch V2 preprocessed (diff) statistic data for this control.
 *
 * Only valid for controls with a [Control.statisticV2] configuration. Returns one entry per
 * [unit] period where each value is the sum of differences recorded in that period.
 *
 * @param client Loxone client
 * @param from Start of the time range as Unix UTC timestamp (inclusive)
 * @param until End of the time range as Unix UTC timestamp (inclusive)
 * @param groupId Statistic group to request; defaults to the first group's id
 * @param unit Grouping resolution — each entry covers one period
 * @param outputName Optional output filter; if null all outputs of the group are returned
 */
@Suppress("LongParameterList")
suspend fun Control.fetchV2Diff(
    client: LoxoneClient,
    from: Long,
    until: Long,
    groupId: Int = statisticV2?.groups?.firstOrNull()?.id ?: 0,
    unit: StatisticUnit = StatisticUnit.DAY,
    outputName: String? = null,
): List<StatisticEntry> {
    statisticV2 ?: return emptyList()
    val path = LoxoneCommands.Statistics.getStatisticDiff(uuidAction, from to until, groupId, unit, outputName)
    return Codec.readStatisticEntries(client.callRawForData(path), if (outputName != null) 1 else null)
}

private fun parseV1Xml(xml: String, timeZone: TimeZone): List<StatisticEntry> =
    V1_XML_ENTRY.findAll(xml).map { m ->
        StatisticEntry(
            Codec.loxoneLocalDateTimeToTimestamp(m.groupValues[1], timeZone),
            listOf(m.groupValues[2].toDouble())
        )
    }.toList()

/** Returns all YYYYMM strings for months that overlap the [from]..[until] range. */
private fun v1Months(from: Instant, until: Instant, timeZone: TimeZone): List<String> {
    val startDate = from.toLocalDateTime(timeZone).date
    val endDate = until.toLocalDateTime(timeZone).date
    // Truncate both to the first day of their respective months for clean iteration
    var month = LocalDate(startDate.year, startDate.monthNumber, 1)
    val endMonth = LocalDate(endDate.year, endDate.monthNumber, 1)
    return buildList {
        while (month <= endMonth) {
            add("${month.year}${month.monthNumber.toString().padStart(2, '0')}")
            month = month.plus(1, DateTimeUnit.MONTH)
        }
    }
}
