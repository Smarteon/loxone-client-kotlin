package cz.smarteon.loxkt.app

import cz.smarteon.loxkt.Command
import cz.smarteon.loxkt.LoxoneResponse
import kotlin.reflect.KClass

/**
 * Command to download the Loxone app (LoxAPP3.json).
 * This file contains the complete static configuration of the Miniserver.
 *
 * The app file can be quite large and should be cached locally.
 * Use the [lastModified] field to check if the cached version is still valid.
 *
 * @see LoxoneAppVersionCommand to check the version before downloading
 */
object LoxoneAppCommand : Command<LoxoneApp> {
    override val pathSegments = listOf("data", "LoxAPP3.json")
    override val responseType: KClass<out LoxoneApp> = LoxoneApp::class
    override val authenticated = true
}

/**
 * Command to check the version/last modified date of the app.
 * Returns the lastModified timestamp without downloading the entire app file.
 *
 * Use this to check if your cached app is still up to date.
 * Compare the returned value with the [LoxoneApp.lastModified] from your cache.
 */
object LoxoneAppVersionCommand : Command<LoxoneAppVersion> {
    override val pathSegments = listOf("jdev", "sps", "LoxAPPversion3")
    override val responseType: KClass<out LoxoneAppVersion> = LoxoneAppVersion::class
    override val authenticated = true
}

/**
 * Response containing the app version information.
 *
 * @property lastModified Timestamp when the app was last modified
 */
data class LoxoneAppVersion(val lastModified: String) : LoxoneResponse
