package cz.smarteon.loxkt.example

import cz.smarteon.loxkt.LoxoneCommands
import cz.smarteon.loxkt.LoxoneCredentials
import cz.smarteon.loxkt.LoxoneEndpoint
import cz.smarteon.loxkt.LoxoneProfile
import cz.smarteon.loxkt.LoxoneTokenAuthenticator
import cz.smarteon.loxkt.app.getAllValues
import cz.smarteon.loxkt.app.getValue
import cz.smarteon.loxkt.callForMsg
import cz.smarteon.loxkt.event.DaytimerEvent
import cz.smarteon.loxkt.event.TextEvent
import cz.smarteon.loxkt.event.ValueEvent
import cz.smarteon.loxkt.event.WeatherEvent
import cz.smarteon.loxkt.ktor.KtorWebsocketLoxoneClient
import cz.smarteon.loxkt.state.LoxoneState
import cz.smarteon.loxkt.state.ValueState
import cz.smarteon.loxkt.state.collectFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Example demonstrating real-time events and state management with Loxone.
 *
 * This example shows:
 * 1. Connecting to Miniserver via WebSocket
 * 2. Enabling binary status updates to receive real-time events
 * 3. Using LoxoneState to track state values
 * 4. Downloading and using LoxoneApp structure
 * 5. Querying control states by name
 *
 * Usage:
 * ```
 * ./gradlew run --args="<miniserver-url> <username> <password>"
 * ```
 *
 * Example:
 * ```
 * ./gradlew run --args="http://192.168.1.100 admin mypassword"
 * ```
 */
fun main(args: Array<String>) = runBlocking {
    if (args.size < 3) {
        println("Usage: <miniserver-url> <username> <password>")
        println("Example: http://192.168.1.100 admin mypassword")
        return@runBlocking
    }

    val url = args[0]
    val username = args[1]
    val password = args[2]

    println("═══════════════════════════════════════════════════════")
    println("  Loxone Real-time Events & State Management Example")
    println("═══════════════════════════════════════════════════════")
    println()

    // Setup connection
    val endpoint = LoxoneEndpoint.fromUrl(url)
    val profile = LoxoneProfile(endpoint, LoxoneCredentials(username, password))
    val authenticator = LoxoneTokenAuthenticator(profile)
    val wsClient = KtorWebsocketLoxoneClient(endpoint, authenticator)

    try {
        println("Connecting to Miniserver at ${endpoint.host}...")

        // Create state store
        val state = LoxoneState()

        // Start collecting events into state (runs in background on IO dispatcher)
        val stateCollectorJob = launch(Dispatchers.IO) {
            state.collectFrom(wsClient.events)
        }

        // Enable binary status updates to start receiving events
        println("Enabling binary status updates...")
        wsClient.callForMsg(LoxoneCommands.App.enableBinStatusUpdate())
        println("Connected! Receiving events...")
        println()

        // Download app structure to get control definitions
        println("Downloading app structure...")
        val app = wsClient.call(LoxoneCommands.App.get())
        println("Loaded ${app.controls.size} controls, ${app.rooms.size} rooms")
        println()

        // Wait for initial state to arrive
        delay(2000)

        // Show current state count
        println("═══════════════════════════════════════════════════════")
        println("  Current State Summary")
        println("═══════════════════════════════════════════════════════")
        println("Total state values tracked: ${state.size()}")
        println()

        // Show some controls with their current values
        println("Sample controls with state values:")
        println()

        app.controls.values
            .filter { it.states?.isNotEmpty() == true }
            .take(5)
            .forEach { control ->
                val roomName = control.room?.let { app.rooms[it]?.name } ?: "Unknown"
                println("  ${control.name} (${control.type}) - Room: $roomName")

                // Get all state values for this control
                val allValues = control.getAllValues(state)
                if (allValues.isEmpty()) {
                    println("    └─ (no state values yet)")
                } else {
                    allValues.entries.take(3).forEach { (stateName, stateValue) ->
                        when (stateValue) {
                            is ValueState -> println("    └─ $stateName = ${stateValue.value}")
                            else -> println("    └─ $stateName = $stateValue")
                        }
                    }
                    if (allValues.size > 3) {
                        println("    └─ ... and ${allValues.size - 3} more states")
                    }
                }
                println()
            }

        // Demonstrate querying a specific control state
        println("═══════════════════════════════════════════════════════")
        println("  Query Examples")
        println("═══════════════════════════════════════════════════════")

        // Find first control with states
        val sampleControl = app.controls.values.firstOrNull { it.states?.isNotEmpty() == true }
        if (sampleControl != null) {
            val firstStateName = sampleControl.states!!.keys.first()
            val firstStateUuid = sampleControl.states!![firstStateName]!!

            println("Control: ${sampleControl.name}")
            println("State name: $firstStateName")
            println("State UUID: $firstStateUuid")
            println()

            // Method 1: Query by UUID
            val valueByUuid = state.getValue(firstStateUuid)
            println("  state.getValue(uuid) = $valueByUuid")

            // Method 2: Query via control extension
            val valueByControl = sampleControl.getValue(state, firstStateName)
            println("  control.getValue(state, \"$firstStateName\") = $valueByControl")

            // Method 3: Get raw state value
            val rawState = state[firstStateUuid]
            println("  state[uuid] = $rawState")
        }
        println()

        // Monitor real-time changes
        println("═══════════════════════════════════════════════════════")
        println("  Real-time Event Monitoring (15 seconds)")
        println("═══════════════════════════════════════════════════════")
        println("Watching for changes... (try switching a light or adjusting something!)")
        println()

        var eventCount = 0
        val monitorJob = launch {
            wsClient.events.collect { event ->
                eventCount++
                when (event) {
                    is ValueEvent -> {
                        val control = app.controls.values.find { it.states?.values?.contains(event.uuid) == true }
                        val stateName = control?.states?.entries?.find { it.value == event.uuid }?.key
                        if (control != null) {
                            println("  [Value] ${control.name}.$stateName = ${event.value}")
                        }
                    }
                    is TextEvent -> {
                        val control = app.controls.values.find { it.states?.values?.contains(event.uuid) == true }
                        if (control != null) {
                            println("  [Text] ${control.name} = \"${event.text}\"")
                        }
                    }
                    is DaytimerEvent -> {
                        println("  [Daytimer] ${event.uuid} - ${event.entries.size} entries")
                    }
                    is WeatherEvent -> {
                        println("  [Weather] ${event.uuid} - ${event.entries.size} forecast entries")
                    }
                }
            }
        }

        delay(15_000)
        monitorJob.cancel()

        println()
        println("═══════════════════════════════════════════════════════")
        println("  Summary")
        println("═══════════════════════════════════════════════════════")
        println("Events received: $eventCount")
        println("Final state count: ${state.size()}")
        println()

        stateCollectorJob.cancel()

    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        println("Closing connection...")
        wsClient.close()
        println("Done!")
    }
}
