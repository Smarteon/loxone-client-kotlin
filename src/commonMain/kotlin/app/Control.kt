package cz.smarteon.loxkt.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Control restriction bit positions
const val RESTRICTION_BIT_REFERENCED_ONLY_INTERNAL = 0
const val RESTRICTION_BIT_READ_ONLY_INTERNAL = 1
const val RESTRICTION_BIT_REFERENCED_ONLY_EXTERNAL = 4
const val RESTRICTION_BIT_READ_ONLY_EXTERNAL = 5

/**
 * Represents a control in the Loxone app.
 * Controls cover both actuators and sensors, from simple I/O to complex blocks like intelligent room controllers.
 *
 * This is a generic implementation that supports all control types through flexible JSON parsing.
 * Library users can access type-specific properties through the [details] and [subControls] fields.
 *
 * @property name Name of the control
 * @property type Type identifier (Jalousie, Daytimer, Switch, etc.). Empty string means not visualized.
 * @property uuidAction Unique identifier for this control
 * @property defaultRating Rating for sorting (0-5 stars)
 * @property isFavorite Whether the control is marked as favorite
 * @property isSecured Whether visualization password is required
 * @property room UUID of the room this control belongs to
 * @property cat UUID of the category this control belongs to
 * @property states Map of state name to state UUID
 * @property details Additional type-specific details as flexible JSON
 * @property subControls Map of sub-control name to sub-control definition
 * @property statistic Statistics configuration if enabled
 * @property statisticV2 New statistics configuration (for energy flow monitor and meters)
 * @property securedDetails Whether sensitive information is available
 * @property restrictions Bitmap of restrictions (referenced only, read only, etc.)
 * @property hasControlNotes Whether there are notes/help texts for this control (since 11.0)
 * @property preset Preset information if the control uses a preset (since 11.3)
 * @property links Array of linked control UUIDs (since 11.3)
 */
@Serializable
data class Control(
    val name: String,
    val type: String,
    val uuidAction: String,
    val defaultRating: Int? = null,
    val isFavorite: Boolean = false,
    val isSecured: Boolean = false,
    val configState: ConfigState? = null,
    val defaultIcon: String? = null,
    val room: String? = null,
    val cat: String? = null,
    val states: Map<String, String>? = null,
    val details: JsonElement? = null,
    val subControls: Map<String, SubControl>? = null,
    val statistic: Statistic? = null,
    val statisticV2: StatisticV2? = null,
    val securedDetails: Boolean? = null,
    val restrictions: Int? = null,
    val hasControlNotes: Boolean? = null,
    val preset: Preset? = null,
    val links: List<String>? = null,
) {
    /**
     * Get a state UUID by name.
     * @param stateName Name of the state
     * @return State UUID or null if not found
     */
    fun getState(stateName: String): String? = states?.get(stateName)

    /**
     * Get a sub-control by name.
     * @param subControlName Name of the sub-control
     * @return Sub-control or null if not found
     */
    fun getSubControl(subControlName: String): SubControl? = subControls?.get(subControlName)

    /**
     * Check if the control has a specific restriction flag set.
     * @param bit Restriction bit to check (0-5)
     * @return true if the restriction bit is set
     */
    fun hasRestriction(bit: Int): Boolean = restrictions?.let { (it and (1 shl bit)) != 0 } ?: false

    /**
     * Check if control is referenced only (internal).
     */
    val isReferencedOnlyInternal: Boolean get() = hasRestriction(RESTRICTION_BIT_REFERENCED_ONLY_INTERNAL)

    /**
     * Check if control is read only (internal).
     */
    val isReadOnlyInternal: Boolean get() = hasRestriction(RESTRICTION_BIT_READ_ONLY_INTERNAL)

    /**
     * Check if control is referenced only (external).
     */
    val isReferencedOnlyExternal: Boolean get() = hasRestriction(RESTRICTION_BIT_REFERENCED_ONLY_EXTERNAL)

    /**
     * Check if control is read only (external).
     */
    val isReadOnlyExternal: Boolean get() = hasRestriction(RESTRICTION_BIT_READ_ONLY_EXTERNAL)
}

/**
 * Represents a sub-control within a control.
 *
 * @property name Name of the sub-control
 * @property type Type of the sub-control
 * @property uuidAction UUID for this sub-control
 * @property defaultRating Rating for sorting (0-5 stars)
 * @property isFavorite Whether the sub-control is marked as favorite
 * @property isSecured Whether visualization password is required
 * @property restrictions Additional restrictions as flexible JSON
 * @property states Map of state name to state UUID
 * @property details Additional type-specific details
 */
@Serializable
data class SubControl(
    val name: String,
    val type: String,
    val uuidAction: String,
    val defaultRating: Int? = null,
    val isFavorite: Boolean = false,
    val isSecured: Boolean = false,
    val restrictions: Int? = null,
    val states: Map<String, String>? = null,
    val details: JsonElement? = null,
)

/**
 * Preset information for a control.
 *
 * @property uuid UUID of the preset
 * @property name Name of the preset
 */
@Serializable
data class Preset(
    val uuid: String,
    val name: String,
)

/**
 * Configuration state for a control.
 *
 * @property isConfigured Whether the control is configured
 */
@Serializable
data class ConfigState(
    val isConfigured: Boolean? = false,
)

/**
 * Statistics configuration for a control.
 *
 * @property frequency How often the statistic is written
 * @property outputs Array of outputs for which statistic data is recorded
 */
@Serializable
data class Statistic(
    val frequency: Int,
    val outputs: List<StatisticOutput>,
)

/**
 * Output configuration for statistics.
 *
 * @property id Index of the datapoint row used (0-6)
 * @property name Name of the output
 * @property format Format specifier for analog values
 * @property uuid UUID of the output
 * @property visuType Visualization type (0=line chart, 1=digital, 2=bar chart)
 */
@Serializable
data class StatisticOutput(
    val id: Int,
    val name: String,
    val format: String? = null,
    val uuid: String,
    val visuType: Int,
)

/**
 * New statistics configuration (V2) for energy flow monitor and meters.
 *
 * @property groups List of statistic groups with different recording frequencies
 */
@Serializable
data class StatisticV2(
    val groups: List<StatisticGroup>,
)

/**
 * Group of statistics with the same recording frequency.
 *
 * @property id Group ID used for getStatistics requests
 * @property mode Recording mode (0=none, 1=every change max 1/min, 7=every change, 8-12=intervals)
 * @property accumulated Whether this is an accumulated meter value
 * @property dataPoints List of data points in this group
 */
@Serializable
data class StatisticGroup(
    val id: Int,
    val mode: Int,
    val accumulated: Boolean? = null,
    val dataPoints: List<StatisticDataPoint>,
)

/**
 * Data point in a statistic group.
 *
 * @property name Name of the data point
 * @property uuid UUID of the data point
 * @property format Format specifier
 * @property type Type identifier
 */
@Serializable
data class StatisticDataPoint(
    val name: String,
    val uuid: String,
    val format: String? = null,
    val type: String? = null,
)
