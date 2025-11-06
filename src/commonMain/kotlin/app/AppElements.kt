package cz.smarteon.loxkt.app

import kotlinx.serialization.Serializable

/**
 * Represents a room used to group controls by location.
 *
 * @property uuid Unique identifier for this room
 * @property name Name of the room
 * @property image Icon for this room
 * @property defaultRating Rating used for sorting (0-5 stars)
 * @property default Whether this is the default room
 * @property type Type of the room
 * @property color Color for the room in the UI
 * @property isFavorite Whether the room is marked as favorite
 */
@Serializable
data class Room(
    val uuid: String,
    val name: String,
    val image: String? = null,
    val defaultRating: Int? = null,
    val default: Boolean? = null,
    val type: Int? = null,
    val color: String? = null,
    val isFavorite: Boolean? = null,
)

/**
 * Represents a category used to group controls logically.
 *
 * @property uuid Unique identifier for this category
 * @property name Name of the category
 * @property image Icon for this category
 * @property isFavorite Whether this category is marked as favorite
 * @property default Whether this is the default category
 * @property defaultRating Rating used for sorting (0-5 stars)
 * @property type Semantic type of the category (lights, indoortemperature, shading, media)
 * @property color Color for the category in the UI
 */
@Serializable
data class Category(
    val uuid: String,
    val name: String,
    val image: String? = null,
    val isFavorite: Boolean? = null,
    val default: Boolean? = null,
    val defaultRating: Int? = null,
    val type: String? = null,
    val color: String? = null,
)
