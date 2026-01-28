package cz.smarteon.loxkt.event

/**
 * Represents a single weather entry.
 *
 * Binary structure (68 bytes):
 * - Timestamp: 32-bit int (4 bytes)
 * - Weather type: 32-bit int (4 bytes)
 * - Wind direction: 32-bit int (4 bytes)
 * - Solar radiation: 32-bit int (4 bytes)
 * - Relative humidity: 32-bit int (4 bytes)
 * - Temperature: 64-bit float (8 bytes)
 * - Perceived temperature: 64-bit float (8 bytes)
 * - Dew point: 64-bit float (8 bytes)
 * - Precipitation: 64-bit float (8 bytes)
 * - Wind speed: 64-bit float (8 bytes)
 * - Barometric pressure: 64-bit float (8 bytes)
 *
 * @property timestamp Timestamp of the weather data
 * @property weatherType Type of weather (encoded as int)
 * @property windDirection Wind direction in degrees
 * @property solarRadiation Solar radiation value
 * @property relativeHumidity Relative humidity percentage
 * @property temperature Temperature in Celsius
 * @property perceivedTemperature Feels-like temperature in Celsius
 * @property dewPoint Dew point in Celsius
 * @property precipitation Precipitation amount
 * @property windSpeed Wind speed
 * @property barometricPressure Barometric pressure in hPa
 */
data class WeatherEntry(
    val timestamp: Int,
    val weatherType: Int,
    val windDirection: Int,
    val solarRadiation: Int,
    val relativeHumidity: Int,
    val temperature: Double,
    val perceivedTemperature: Double,
    val dewPoint: Double,
    val precipitation: Double,
    val windSpeed: Double,
    val barometricPressure: Double
) {
    companion object {
        /** Size of a weather entry in bytes */
        const val SIZE_BYTES = 68
    }
}

/**
 * Represents a weather state update event.
 * Weather-States contain weather forecast data when a Weather-Abo is active.
 *
 * Binary structure:
 * - UUID: 128-bit (16 bytes)
 * - Last update: 32-bit unsigned int (4 bytes) - seconds since 2009 (UTC)
 * - Number of entries: 32-bit int (4 bytes)
 * - Entries: variable number of [WeatherEntry]
 *
 * @property uuid UUID of the weather state
 * @property lastUpdate Last update timestamp (seconds since 2009, UTC)
 * @property entries List of weather forecast entries
 */
data class WeatherEvent(
    override val uuid: String,
    val lastUpdate: UInt,
    val entries: List<WeatherEntry>
) : LoxoneEvent {
    companion object {
        /** Size of the weather event header in bytes (UUID + last update + entry count) */
        const val HEADER_SIZE_BYTES = 24
    }
}
