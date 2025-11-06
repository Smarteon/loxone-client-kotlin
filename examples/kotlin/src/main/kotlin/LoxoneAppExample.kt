package cz.smarteon.loxkt.examples

import cz.smarteon.loxkt.LoxoneClient
import cz.smarteon.loxkt.LoxoneCommands
import cz.smarteon.loxkt.app.*

/**
 * Example demonstrating how to work with Loxone apps.
 *
 * This example shows:
 * - Downloading and caching apps
 * - Working with rooms, categories, and controls
 * - Filtering and querying controls
 * - Accessing control states and details
 */
class LoxoneAppExample {

    /**
     * Download app with caching strategy
     */
    suspend fun downloadApp(client: LoxoneClient, cached: LoxoneApp?): LoxoneApp {
        // First check if cached version is still valid
        if (cached != null) {
            val version = client.call(LoxoneCommands.App.version())
            if (version.lastModified == cached.lastModified) {
                println("Using cached app")
                return cached
            }
        }

        // Download fresh app
        println("Downloading app...")
        val app = client.call(LoxoneCommands.App.get())
        println("App downloaded: ${app.lastModified}")

        // Cache the app here (implementation dependent)
        // saveToCache(app)

        return app
    }

    /**
     * Display miniserver information
     */
    fun displayMiniserverInfo(app: LoxoneApp) {
        val msInfo = app.msInfo

        println("=== Miniserver Information ===")
        println("Name: ${msInfo.msName}")
        println("Serial: ${msInfo.serialNr}")
        println("Project: ${msInfo.projectName}")
        println("Type: ${getMiniserverTypeName(msInfo.miniserverType)}")
        println("Temperature Unit: ${if (msInfo.tempUnit == 0) "°C" else "°F"}")
        println("Local URL: ${msInfo.localUrl}")
        println("Remote URL: ${msInfo.remoteUrl}")

        msInfo.currentUser?.let { user ->
            println("\n=== Current User ===")
            println("Username: ${user.name}")
            println("UUID: ${user.uuid}")
            println("Admin: ${user.isAdmin}")
        }
    }

    /**
     * List all rooms with their controls
     */
    fun listRoomsAndControls(app: LoxoneApp) {
        println("\n=== Rooms and Controls ===")

        val rooms = if (app.msInfo.sortByRating == true) {
            app.getRoomsSortedByRating()
        } else {
            app.rooms.values.toList()
        }

        rooms.forEach { room ->
            val controls = app.getControlsForRoom(room.uuid)
            println("\n${room.name} (${controls.size} controls)")

            controls.forEach { control ->
                if (control.type.isNotEmpty()) {
                    val category = control.cat?.let { app.getCategory(it)?.name } ?: "No category"
                    println("  - ${control.name} [${control.type}] - $category")
                }
            }
        }
    }

    /**
     * Find and display all light controls
     */
    fun listLightControls(app: LoxoneApp) {
        println("\n=== Light Controls ===")

        // Find lights by category
        val lightsCategory = app.cats.values.find { it.type == "lights" }

        if (lightsCategory != null) {
            val lightControls = app.getControlsForCategory(lightsCategory.uuid)
            println("Found ${lightControls.size} light controls in category '${lightsCategory.name}'")

            lightControls.forEach { control ->
                val room = app.getRoomName(control) ?: "No room"
                println("  - ${control.name} [${control.type}] in $room")

                // Display available states
                control.states?.forEach { (stateName, stateUuid) ->
                    println("    State: $stateName -> $stateUuid")
                }
            }
        }

        // Also find by type
        println("\n=== Switches ===")
        val switches = app.getControlsByType("Switch")
        switches.take(5).forEach { switch ->
            val room = app.getRoomName(switch) ?: "No room"
            println("  - ${switch.name} in $room")
            println("    Active state: ${switch.getState("active")}")
        }
    }

    /**
     * Find and display thermostats (Intelligent Room Controllers)
     */
    fun listThermostats(app: LoxoneApp) {
        println("\n=== Thermostats (IRC v2) ===")

        val ircs = app.getControlsByType("IRoomControllerV2")
        println("Found ${ircs.size} thermostats")

        ircs.forEach { irc ->
            val room = app.getRoomName(irc) ?: "No room"
            println("\n  ${irc.name} in $room")

            // Display main states
            println("    States:")
            println("      Current Temp: ${irc.getState("tempActual")}")
            println("      Target Temp: ${irc.getState("tempTarget")}")
            println("      Active Mode: ${irc.getState("activeMode")}")

            // Display sub-controls
            irc.subControls?.forEach { (name, subControl) ->
                println("    Sub-control: $name [${subControl.type}]")
            }
        }
    }

    /**
     * Display controls with statistics
     */
    fun listControlsWithStatistics(app: LoxoneApp) {
        println("\n=== Controls with Statistics ===")

        val controlsWithStats = app.controls.values.filter {
            it.statistic != null || it.statisticV2 != null
        }

        println("Found ${controlsWithStats.size} controls with statistics enabled")

        controlsWithStats.take(5).forEach { control ->
            println("\n  ${control.name} [${control.type}]")

            control.statistic?.let { stat ->
                println("    Frequency: ${stat.frequency}")
                stat.outputs.forEach { output ->
                    println("    Output: ${output.name} (${output.format})")
                }
            }

            control.statisticV2?.let { stat ->
                println("    Statistics V2 with ${stat.groups.size} groups")
                stat.groups.forEach { group ->
                    println("    Group ${group.id}: ${group.dataPoints.size} data points")
                }
            }
        }
    }

    /**
     * Send commands to controls
     */
    suspend fun controlExamples(client: LoxoneClient, app: LoxoneApp) {
        println("\n=== Control Examples ===")

        // Find a switch and turn it on
        val switch = app.getControlsByType("Switch").firstOrNull()
        if (switch != null) {
            println("Turning on: ${switch.name}")
            // client.call(Command("jdev/sps/io/${switch.uuidAction}/On"))
        }

        // Find a dimmer and set to 50%
        val dimmer = app.getControlsByType("Dimmer").firstOrNull()
        if (dimmer != null) {
            println("Setting dimmer ${dimmer.name} to 50%")
            // client.call(Command("jdev/sps/io/${dimmer.uuidAction}/50"))
        }

        // Find a jalousie and open it
        val jalousie = app.getControlsByType("Jalousie").firstOrNull()
        if (jalousie != null) {
            println("Opening jalousie: ${jalousie.name}")
            // client.call(Command("jdev/sps/io/${jalousie.uuidAction}/FullUp"))
        }
    }

    /**
     * Filter controls by restrictions
     */
    fun filterControlsByRestrictions(app: LoxoneApp) {
        println("\n=== Control Restrictions ===")

        // Get only user-accessible controls
        val userControls = app.getControlsFiltered(
            includeReferencedOnly = false,
            includeReadOnly = false,
            forExternal = true
        )

        println("User-accessible controls (external): ${userControls.size}")

        // Get read-only controls
        val readOnlyControls = app.controls.values.filter {
            it.isReadOnlyExternal || it.isReadOnlyInternal
        }

        println("Read-only controls: ${readOnlyControls.size}")

        // Get secured controls
        val securedControls = app.controls.values.filter { it.isSecured }
        println("Secured controls: ${securedControls.size}")
    }

    private fun getMiniserverTypeName(type: Int): String = when (type) {
        0 -> "Miniserver (Gen 1)"
        1 -> "Miniserver Go (Gen 1)"
        2 -> "Miniserver (Gen 2)"
        3 -> "Miniserver Go (Gen 2)"
        4 -> "Miniserver Compact"
        else -> "Unknown"
    }
}

/**
 * Example main function showing complete workflow
 */
suspend fun main() {
    // Initialize client (pseudo-code)
    // val client = LoxoneClient(...)

    // val example = LoxoneAppExample()
    // val app = example.downloadApp(client, null)

    // example.displayMiniserverInfo(app)
    // example.listRoomsAndControls(app)
    // example.listLightControls(app)
    // example.listThermostats(app)
    // example.listControlsWithStatistics(app)
    // example.filterControlsByRestrictions(app)
    // example.controlExamples(client, app)
}
