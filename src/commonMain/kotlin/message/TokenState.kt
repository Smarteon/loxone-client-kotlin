package cz.smarteon.loxkt.message

/**
 * Helper class for token state diagnostics.
 */
internal class TokenState(token: Token?) {

    private val secondsToExpire: Long? = if (token?.filled == true) token.secondsToExpireFromNow() else null

    /**
     * Check if the token expired or is close to expiry.
     * Is true if token is not set or is close to expire, false otherwise
     */
    val isExpired: Boolean = secondsToExpire == null || secondsToExpire <= MAX_SECONDS_TO_EXPIRE

    /**
     * Check if the token needs to be refreshed and is still valid in order to be refreshed.
     * When [.isExpired] returns true, this method will return false.
     * Is true if token is close to expiry but not yet too close, false otherwise
     */
    val needsRefresh: Boolean =
        secondsToExpire != null && REFRESH_THRESHOLD >= secondsToExpire && secondsToExpire > MAX_SECONDS_TO_EXPIRE

    /**
     * Number of seconds remaining until [.needsRefresh] become true.
     */
    val secondsToRefresh: Long =
        if (secondsToExpire == null || secondsToExpire < REFRESH_THRESHOLD) 0 else secondsToExpire - REFRESH_THRESHOLD

    /**
     * Check whether the token is still valid and doesn't need refresh.
     * Is true if the token is not expired and does not need refresh, false otherwise
     */
    val isUsable: Boolean = !isExpired && !needsRefresh

    companion object {
        private const val MAX_SECONDS_TO_EXPIRE = 60L
        private const val REFRESH_THRESHOLD = 300L
    }
}
