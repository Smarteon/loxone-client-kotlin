package cz.smarteon.loxkt.event

/**
 * Represents a value state update event.
 * Value-States are the simplest form of state update, containing a UUID and a double value.
 *
 * Binary structure (24 bytes):
 * - UUID: 128-bit (16 bytes)
 * - Value: 64-bit float (8 bytes, little endian)
 *
 * @property uuid UUID of the state being updated
 * @property value The new value as a double
 */
data class ValueEvent(
    override val uuid: String,
    val value: Double
) : LoxoneEvent {
    companion object {
        /** Size of a single value event in bytes */
        const val SIZE_BYTES = 24
    }
}
