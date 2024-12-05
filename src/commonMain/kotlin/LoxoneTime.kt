package cz.smarteon.loxkt

/**
 * Utilities supporting Loxone miniserver specific time handling.
 */
object LoxoneTime {

    /**
     * Unix epoch seconds representing the beginning of Loxone epoch.
     */
    const val LOXONE_EPOCH_BEGIN = 1230768000

    /**
     * Converts the given loxone epoch seconds to unix epoch.
     * @param loxSeconds seconds since loxone epoch begin
     * @return unix epoch seconds of given loxone seconds
     */
    fun getUnixEpochSeconds(loxSeconds: Long): Long {
        return LOXONE_EPOCH_BEGIN + loxSeconds
    }
}
