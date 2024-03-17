package cz.smarteon.loxone.message

internal data class MessageHeader(
    val kind: MessageKind,
    val sizeEstimated: Boolean,
    val messageSize: Long
) {

    companion object {
        const val PAYLOAD_LENGTH = 8
        const val FIRST_BYTE: Byte = 0x03
        const val MSG_SIZE_POSITION = 4

        /**
         * Keep alive message header, received from miniserver as response to keep alive command.
         */
        val KEEP_ALIVE = MessageHeader(MessageKind.KEEP_ALIVE, false, 0)
    }
}

internal enum class MessageKind {
    TEXT, FILE, EVENT_VALUE, EVENT_TEXT, EVENT_DAYTIMER, OUT_OF_SERVICE, KEEP_ALIVE, EVENT_WEATHER
}
