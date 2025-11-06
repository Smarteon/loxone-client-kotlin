@file:Suppress("TooManyFunctions")

package cz.smarteon.loxkt.app

/**
 * Extension functions and utilities for working with Loxone app.
 */

/**
 * Get all controls sorted by rating (if sortByRating is enabled).
 *
 * @return List of controls sorted by defaultRating descending
 */
fun LoxoneApp.getControlsSortedByRating(): List<Control> =
    if (msInfo.sortByRating == true) {
        controls.values.sortedByDescending { it.defaultRating ?: 0 }
    } else {
        controls.values.toList()
    }

/**
 * Get all rooms sorted by rating (if sortByRating is enabled).
 *
 * @return List of rooms sorted by defaultRating descending
 */
fun LoxoneApp.getRoomsSortedByRating(): List<Room> =
    if (msInfo.sortByRating == true) {
        rooms.values.sortedByDescending { it.defaultRating ?: 0 }
    } else {
        rooms.values.toList()
    }

/**
 * Get all categories sorted by rating (if sortByRating is enabled).
 *
 * @return List of categories sorted by defaultRating descending
 */
fun LoxoneApp.getCategoriesSortedByRating(): List<Category> =
    if (msInfo.sortByRating == true) {
        cats.values.sortedByDescending { it.defaultRating ?: 0 }
    } else {
        cats.values.toList()
    }

/**
 * Get all visible controls (controls with non-empty type).
 *
 * @return List of visible controls
 */
fun LoxoneApp.getVisibleControls(): List<Control> =
    controls.values.filter { it.type.isNotEmpty() }

/**
 * Get controls filtered by restrictions.
 *
 * @param includeReferencedOnly Whether to include controls marked as referenced only
 * @param includeReadOnly Whether to include controls marked as read only
 * @param forExternal Whether to check external restrictions (true) or internal (false)
 * @return List of filtered controls
 */
fun LoxoneApp.getControlsFiltered(
    includeReferencedOnly: Boolean = true,
    includeReadOnly: Boolean = true,
    forExternal: Boolean = false
): List<Control> = controls.values.filter { control ->
    val referencedBit =
        if (forExternal) RESTRICTION_BIT_REFERENCED_ONLY_EXTERNAL else RESTRICTION_BIT_REFERENCED_ONLY_INTERNAL
    val readOnlyBit = if (forExternal) RESTRICTION_BIT_READ_ONLY_EXTERNAL else RESTRICTION_BIT_READ_ONLY_INTERNAL

    val isReferencedOnly = control.hasRestriction(referencedBit)
    val isReadOnly = control.hasRestriction(readOnlyBit)

    (includeReferencedOnly || !isReferencedOnly) && (includeReadOnly || !isReadOnly)
}

/**
 * Find a room by its UUID.
 *
 * @param uuid UUID of the room
 * @return Room or null if not found
 */
fun LoxoneApp.getRoom(uuid: String): Room? = rooms[uuid]

/**
 * Find a category by its UUID.
 *
 * @param uuid UUID of the category
 * @return Category or null if not found
 */
fun LoxoneApp.getCategory(uuid: String): Category? = cats[uuid]

/**
 * Get the room name for a control.
 *
 * @param control The control
 * @return Room name or null if control has no room or room not found
 */
fun LoxoneApp.getRoomName(control: Control): String? = control.room?.let { getRoom(it)?.name }

/**
 * Get the category name for a control.
 *
 * @param control The control
 * @return Category name or null if control has no category or category not found
 */
fun LoxoneApp.getCategoryName(control: Control): String? = control.cat?.let { getCategory(it)?.name }

/**
 * Get all controls for a specific room.
 *
 * @param roomUuid UUID of the room
 * @return List of controls in the specified room
 */
fun LoxoneApp.getControlsForRoom(roomUuid: String): List<Control> =
    controls.values.filter { it.room == roomUuid }

/**
 * Get all controls for a specific category.
 *
 * @param categoryUuid UUID of the category
 * @return List of controls in the specified category
 */
fun LoxoneApp.getControlsForCategory(categoryUuid: String): List<Control> =
    controls.values.filter { it.cat == categoryUuid }

/**
 * Get controls by type.
 *
 * @param type Type identifier (e.g., "Switch", "Jalousie", "Dimmer")
 * @return List of controls matching the type
 */
fun LoxoneApp.getControlsByType(type: String): List<Control> =
    controls.values.filter { it.type == type }

/**
 * Find a control by its UUID.
 *
 * @param uuid UUID of the control
 * @return Control or null if not found
 */
fun LoxoneApp.getControl(uuid: String): Control? =
    controls[uuid]
