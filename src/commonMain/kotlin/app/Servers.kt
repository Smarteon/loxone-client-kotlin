package cz.smarteon.loxkt.app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Media server configuration (Loxone Music Server, Audioserver, or Casatunes).
 *
 * @property type Server type (0=Casatunes, 1=Music Server, 2=Audioserver/Compact)
 * @property subtype Server subtype for type 2 (0/missing=Audioserver, 1=Compact)
 * @property host IP and port for communication
 * @property mac MAC address of server (since 11.1)
 * @property localIP Local resolved IP address (Audioserver/Compact only)
 */
@Serializable
data class MediaServer(
    val type: Int,
    val subtype: Int? = null,
    val host: String,
    @SerialName("MAC") val mac: String? = null,
    val localIP: String? = null,
)

/**
 * Weather server configuration (Loxone Cloud Weather).
 *
 * @property states Weather state UUIDs
 * @property format Format specifiers for weather values
 * @property weatherTypeTexts User-friendly texts for weather types
 * @property weatherFieldTypes Possible weather field types (since v8)
 */
@Serializable
data class WeatherServer(
    val states: WeatherStates? = null,
    val format: WeatherFormat? = null,
    val weatherTypeTexts: Map<String, String>? = null,
    val weatherFieldTypes: Map<String, WeatherFieldType>? = null,
)

/**
 * Weather field type information.
 *
 * @property id Field type ID
 * @property name Field name
 * @property analog Whether the field is analog
 * @property unit Unit of measurement
 * @property format Format specifier
 */
@Serializable
data class WeatherFieldType(
    val id: Int? = null,
    val name: String? = null,
    val analog: Boolean? = null,
    val unit: String? = null,
    val format: String? = null,
)

/**
 * Weather state UUIDs.
 *
 * @property actual UUID for current weather data
 * @property forecast UUID for weather forecast (next 96 hours)
 */
@Serializable
data class WeatherStates(
    val actual: String? = null,
    val forecast: String? = null,
)

/**
 * Format specifiers for weather values.
 *
 * @property relativeHumidity Format for relative humidity
 * @property temperature Format for temperature
 * @property windSpeed Format for wind speed
 * @property precipitation Format for precipitation
 * @property barometricPressure Format for barometric pressure
 */
@Serializable
data class WeatherFormat(
    val relativeHumidity: String? = null,
    val temperature: String? = null,
    val windSpeed: String? = null,
    val precipitation: String? = null,
    val barometricPressure: String? = null,
)
