package cz.smarteon.loxkt.app

import cz.smarteon.loxkt.LoxoneResponse
import kotlinx.serialization.Serializable

/**
 * Represents the complete Loxone app (LoxAPP3.json).
 * This file contains the static configuration of a Miniserver including controls, rooms, categories, and metadata.
 *
 * @property lastModified Timestamp when the app was last modified
 * @property msInfo Information about the Miniserver
 * @property globalStates Global state UUIDs that affect the whole Miniserver
 * @property operatingModes Operating modes configuration
 * @property rooms Map of room UUID to room definition
 * @property cats Map of category UUID to category definition
 * @property controls Map of control UUID to control definition
 * @property weatherServer Weather server configuration (if configured)
 * @property mediaServer Map of media server UUID to media server definition
 */
@Serializable
data class LoxoneApp(
    val lastModified: String,
    val msInfo: MiniserverInfo,
    val globalStates: GlobalStates? = null,
    val operatingModes: Map<String, String>? = null,
    val rooms: Map<String, Room>,
    val cats: Map<String, Category>,
    val controls: Map<String, Control>,
    val weatherServer: WeatherServer? = null,
    val mediaServer: Map<String, MediaServer>? = null,
) : LoxoneResponse
