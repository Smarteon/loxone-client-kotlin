# Loxone App Parsing

This module provides comprehensive support for parsing and working with Loxone app (LoxAPP3.json).

## Overview

The app contains the complete static configuration of a Loxone Miniserver, including:
- **Controls**: All sensors, actuators, and complex blocks (switches, lights, thermostats, etc.)
- **Rooms**: Logical grouping by location
- **Categories**: Logical grouping by function (lights, climate, shading, etc.)
- **Miniserver Info**: Configuration and metadata
- **Global States**: System-wide state UUIDs
- **Weather Server**: Weather service configuration (if enabled)
- **Media Servers**: Audio/music server configurations (if present)

## Usage

### Downloading the App

```kotlin
// Download the complete app
val app = client.call(LoxoneCommands.App.get())

// Check version before downloading (to avoid unnecessary downloads)
val version = client.call(LoxoneCommands.App.version())
if (version.lastModified != cachedApp.lastModified) {
    // App has changed, download new version
    val newApp = client.call(LoxoneCommands.App.get())
    // Cache the new app
}
```

### Working with Rooms and Categories

```kotlin
// Get all rooms
val rooms = app.rooms.values

// Get a specific room
val livingRoom = app.getRoom("room-uuid")

// Get all controls in a room
val livingRoomControls = app.getControlsForRoom("room-uuid")

// Get all categories
val categories = app.cats.values

// Get controls by category
val lightControls = app.getControlsForCategory("lights-category-uuid")

// Get sorted rooms (if sorting is enabled)
val sortedRooms = app.getRoomsSortedByRating()
```

### Working with Controls

```kotlin
// Get all controls
val allControls = app.controls.values

// Get controls by type
val switches = app.getControlsByType("Switch")
val thermostats = app.getControlsByType("IRoomControllerV2")
val jalousies = app.getControlsByType("Jalousie")

// Get a specific control
val control = app.getControl("control-uuid")

// Access control properties
control?.let {
    println("Name: ${it.name}")
    println("Type: ${it.type}")
    println("Room: ${app.getRoomName(it)}")
    println("Category: ${app.getCategoryName(it)}")
    
    // Get state UUIDs
    val activeStateUuid = it.getState("active")
    val valueStateUuid = it.getState("value")
    
    // Access type-specific details (stored as flexible JSON)
    val details = it.details
    
    // Check for sub-controls (e.g., in Intelligent Room Controllers)
    it.subControls?.forEach { (name, subControl) ->
        println("Sub-control: $name - ${subControl.type}")
    }
}

// Get only visible controls (non-empty type)
val visibleControls = app.getVisibleControls()

// Filter controls by restrictions
val userAccessibleControls = app.getControlsFiltered(
    includeReferencedOnly = false,  // Exclude internal-only controls
    includeReadOnly = true,          // Include read-only controls
    forExternal = true               // Check external restrictions
)
```

### Generic Control Support

The `Control` class is designed to support **all** Loxone control types generically. Rather than creating specific classes for each control type (Switch, Jalousie, Dimmer, etc.), the implementation uses:

1. **Type String**: The `type` field identifies the control type
2. **Flexible Details**: The `details` field stores type-specific properties as JSON
3. **Dynamic States**: The `states` map provides access to all state UUIDs
4. **Sub-Controls**: Support for nested controls (common in complex blocks)

This approach allows library users to:
- Work with any control type, including future types not yet documented
- Access type-specific properties through the JSON details object
- Send commands to any control using the documentation
- Parse and use state updates for any control type

### Example: Working with Different Control Types

```kotlin
// Switch
val switch = app.getControlsByType("Switch").first()
val activeState = switch.getState("active")

// Dimmer
val dimmer = app.getControlsByType("Dimmer").first()
val positionState = dimmer.getState("position")

// Jalousie (Blinds)
val jalousie = app.getControlsByType("Jalousie").first()
val upState = jalousie.getState("up")
val downState = jalousie.getState("down")
val positionState = jalousie.getState("position")

// Intelligent Room Controller V2
val irc = app.getControlsByType("IRoomControllerV2").first()
val tempActual = irc.getState("tempActual")
val tempTarget = irc.getState("tempTarget")
val operatingMode = irc.getState("activeMode")

// Access sub-controls
irc.subControls?.forEach { (name, subControl) ->
    when (subControl.type) {
        "Temperature" -> println("Temperature sensor: ${subControl.name}")
        "Daytimer" -> println("Daytimer: ${subControl.name}")
    }
}
```

### Working with Statistics

```kotlin
control.statistic?.let { stats ->
    println("Recording frequency: ${stats.frequency}")
    stats.outputs.forEach { output ->
        println("Output: ${output.name}, Format: ${output.format}, UUID: ${output.uuid}")
    }
}

// For newer statisticV2 (energy flow monitors, meters)
control.statisticV2?.let { stats ->
    stats.groups.forEach { group ->
        println("Group ${group.id}, Mode: ${group.mode}")
        group.dataPoints.forEach { point ->
            println("  ${point.name}: ${point.uuid}")
        }
    }
}
```

### Checking Control Restrictions

```kotlin
control?.let {
    if (it.isReferencedOnlyInternal) {
        println("This control is for internal use only")
    }
    
    if (it.isReadOnlyInternal) {
        println("This control is read-only")
    }
    
    if (it.isSecured) {
        println("This control requires visualization password")
    }
    
    if (it.hasControlNotes) {
        // Fetch notes using: jdev/sps/io/{controlUUID}/controlnotes
    }
}
```

### Working with Miniserver Info

```kotlin
val msInfo = app.msInfo

println("Miniserver: ${msInfo.msName}")
println("Serial: ${msInfo.serialNr}")
println("Project: ${msInfo.projectName}")
println("Local URL: ${msInfo.localUrl}")
println("Temperature Unit: ${if (msInfo.tempUnit == 0) "°C" else "°F"}")

when (msInfo.miniserverType) {
    0 -> println("Miniserver Gen 1")
    1 -> println("Miniserver Go Gen 1")
    2 -> println("Miniserver Gen 2")
    3 -> println("Miniserver Go Gen 2")
    4 -> println("Miniserver Compact")
}

msInfo.currentUser?.let { user ->
    println("User: ${user.name}")
    println("Admin: ${user.isAdmin}")
    println("UUID: ${user.uuid}")
}
```

### Global States

```kotlin
app.globalStates?.let { states ->
    // These UUIDs can be used to subscribe to state updates
    val sunriseUuid = states.sunrise
    val sunsetUuid = states.sunset
    val notificationsUuid = states.notifications
    val timeUuid = states.miniserverTime
}
```

## Control Commands

To send commands to controls, use the control's `uuidAction` with the appropriate command path. Refer to the [Loxone documentation](../../docs/loxone/AppFile.md) for control-specific commands.

```kotlin
// Generic command pattern
val commandPath = "jdev/sps/io/${control.uuidAction}/<command>"

// Example: Turn on a switch
client.call(Command("jdev/sps/io/${switchControl.uuidAction}/On"))

// Example: Set dimmer position to 50%
client.call(Command("jdev/sps/io/${dimmerControl.uuidAction}/50"))

// Example: Move jalousie up
client.call(Command("jdev/sps/io/${jalousieControl.uuidAction}/FullUp"))
```

## Caching Strategy

The app can be quite large. Best practices:

1. **Cache locally**: Save the app after downloading
2. **Check version**: Use `LoxoneCommands.App.version()` to check if cached version is still valid
3. **Update on changes**: Only re-download when `lastModified` differs
4. **Handle modifications**: Subscribe to the `globalStates.modifications` UUID to get notified of structural changes

```kotlin
// Pseudo-code for caching strategy
fun getLoxoneApp(): LoxoneApp {
    val cached = loadFromCache()
    val version = client.call(LoxoneCommands.App.version())
    
    return if (cached != null && cached.lastModified == version.lastModified) {
        cached
    } else {
        val fresh = client.call(LoxoneCommands.App.get())
        saveToCache(fresh)
        fresh
    }
}
```

## Type-Specific Control Information

The Loxone app supports many control types. While this library provides a generic implementation, here are some common types and their typical states:

- **Switch**: `active`
- **Dimmer**: `position`, `min`, `max`, `step`
- **Jalousie**: `up`, `down`, `position`, `shade`, `shadePosition`
- **IRoomControllerV2**: `tempActual`, `tempTarget`, `activeMode`, `operatingMode`
- **ColorPickerV2**: `color`, `sequence`
- **Alarm**: `armed`, `level`, `startTime`
- **AudioZone**: `power`, `volume`, `input`
- **Meter**: `actual`, `total`

For complete documentation of all control types and their states, refer to the [App File documentation](../../docs/loxone/StructureFile.md).

## Data Classes

### Main Classes
- `LoxoneApp`: Root app object
- `MiniserverInfo`: Miniserver metadata and configuration
- `Room`: Room definition
- `Category`: Category definition
- `Control`: Generic control with flexible properties
- `SubControl`: Nested control within a parent control

### Supporting Classes
- `GlobalStates`: System-wide state UUIDs
- `OperatingMode`: Time-based operating modes
- `WeatherServer`: Weather service configuration
- `MediaServer`: Audio/music server configuration
- `Statistic`: Statistics recording configuration
- `StatisticV2`: New statistics format for energy monitoring

All classes use Kotlinx Serialization and support automatic JSON parsing.
