package cz.smarteon.loxone.message

/**
 * Represents [Token]'s permission type necessary for token acquire.
 *
 * @property[id] numeric representation as used in Loxone API
 */
@Suppress("MagicNumber")
enum class TokenPermission(val id: Int) {

    /**
     * ADMIN permission.
     */
    ADMIN(1),

    /**
     * WEB permission - short token validity (lasts for hours).
     */
    WEB(2),

    /**
     * APP permission - long token validity (lasts for weeks).
     */
    APP(4),

    /**
     * CONFIG permission - used to connect like Loxone Config application.
     */
    CONFIG(12)
}
