package cz.smarteon.loxkt.event

/**
 * Represents a text state update event.
 * Text-States contain a UUID, an icon UUID, and variable-length text content.
 *
 * Binary structure:
 * - UUID: 128-bit (16 bytes)
 * - Icon UUID: 128-bit (16 bytes)
 * - Text length: 32-bit unsigned int (4 bytes, little endian)
 * - Text: variable length (padded to multiple of 4 bytes)
 *
 * @property uuid UUID of the state being updated
 * @property iconUuid UUID of the icon associated with this text (used by Status control)
 * @property text The text content
 */
data class TextEvent(
    override val uuid: String,
    val iconUuid: String,
    val text: String
) : LoxoneEvent {
    companion object {
        /** Minimum size of a text event header in bytes (2 UUIDs + length field) */
        const val HEADER_SIZE_BYTES = 36
    }
}
