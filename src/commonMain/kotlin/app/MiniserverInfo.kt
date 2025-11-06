package cz.smarteon.loxkt.app

import kotlinx.serialization.Serializable

/**
 * Information about the Miniserver and its configuration.
 *
 * @property serialNr Serial number of the Miniserver
 * @property msName Name of the Miniserver as specified in the configuration
 * @property projectName Name of the configuration document
 * @property localUrl IP and port for local network connection
 * @property remoteUrl URL/IP and port for global access
 * @property hostname Miniserver hostname including domain (DHCP only, since v13.3)
 * @property tempUnit Temperature unit (0 = °C, 1 = °F)
 * @property currency Currency symbol (€, $, etc.)
 * @property squareMeasure Unit of area for rooms
 * @property location Address/location of the Miniserver
 * @property latitude Latitude coordinate (since 12.16.14)
 * @property longitude Longitude coordinate (since 12.16.14)
 * @property altitude Altitude (since 12.16.14)
 * @property languageCode Language code for the Miniserver UI (e.g., "CSY", "DEU", "ENG")
 * @property heatPeriodStart Start of heating period (DEPRECATED)
 * @property heatPeriodEnd End of heating period (DEPRECATED)
 * @property coolPeriodStart Start of cooling period (DEPRECATED)
 * @property coolPeriodEnd End of cooling period (DEPRECATED)
 * @property catTitle Top level name for categories
 * @property roomTitle Top level name for rooms
 * @property miniserverType Type of Miniserver (0=Gen1, 1=Go Gen1, 2=Gen2, 3=Go Gen2, 4=Compact)
 * @property sortByRating Whether controls should be sorted by rating
 * @property currentUser Information about the current user
 */
@Serializable
data class MiniserverInfo(
    val serialNr: String,
    val msName: String,
    val projectName: String,
    val localUrl: String? = null,
    val remoteUrl: String? = null,
    val hostname: String? = null,
    val tempUnit: Int,
    val currency: String? = null,
    val squareMeasure: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val languageCode: String? = null,
    val heatPeriodStart: String? = null,
    val heatPeriodEnd: String? = null,
    val coolPeriodStart: String? = null,
    val coolPeriodEnd: String? = null,
    val catTitle: String? = null,
    val roomTitle: String? = null,
    val miniserverType: Int,
    val sortByRating: Boolean? = null,
    val currentUser: CurrentUser? = null,
)

/**
 * Information about the current authenticated user.
 *
 * @property name Username
 * @property uuid User UUID
 * @property isAdmin Whether the user has admin rights
 * @property changePassword Whether the user can change password via WebService
 * @property userRights Permissions available for this user
 */
@Serializable
data class CurrentUser(
    val name: String,
    val uuid: String,
    val isAdmin: Boolean,
    val changePassword: Boolean,
    val userRights: Long? = null,
)
