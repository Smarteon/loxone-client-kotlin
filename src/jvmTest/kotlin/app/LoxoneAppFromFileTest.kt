package cz.smarteon.loxkt.app

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/**
 * JVM-specific tests for loading LoxoneApp from file resources.
 */
class LoxoneAppFromFileTest : ShouldSpec({

    val json = Json { ignoreUnknownKeys = true }

    context("Loading app from resources") {
        should("load and parse app from JSON resource") {
            val resourceStream = this::class.java.classLoader.getResourceAsStream("app/sample-app.json")
            resourceStream.shouldNotBeNull()

            val jsonContent = resourceStream.bufferedReader().use { it.readText() }
            val app = json.decodeFromString<LoxoneApp>(jsonContent)

            app.shouldNotBeNull()
            app.lastModified shouldBe "2025-10-23 15:35:16"

            // Verify msInfo
            app.msInfo.shouldNotBeNull()
            app.msInfo.serialNr shouldBe "504F94SAMPLE"
            app.msInfo.msName shouldBe "Sample Miniserver"
            app.msInfo.projectName shouldBe "Sample Project"
            app.msInfo.miniserverType shouldBe 2
            app.msInfo.location shouldBe "Sample City"
            app.msInfo.currentUser.shouldNotBeNull()
            app.msInfo.currentUser.isAdmin shouldBe true

            // Verify rooms exist and contain expected data
            app.rooms.shouldNotBeNull()
            val livingRoom = app.rooms["1c8b2764-0061-373e-fffffd5438c1b69c"]
            livingRoom.shouldNotBeNull()
            livingRoom.name shouldBe "Obývací pokoj"
            livingRoom.defaultRating shouldBe 1
            livingRoom.isFavorite shouldBe true

            val kitchen = app.rooms["1c8b2764-0061-3742-fffffd5438c1b69c"]
            kitchen.shouldNotBeNull()
            kitchen.name shouldBe "Kuchyně"
            kitchen.defaultRating shouldBe 1
            kitchen.isFavorite shouldBe true

            // Verify categories exist
            app.cats.shouldNotBeNull()
            val lightsCategory = app.cats["1c8b2764-0054-365b-fffffd5438c1b69c"]
            lightsCategory.shouldNotBeNull()
            lightsCategory.name shouldBe "Osvětlení"
            lightsCategory.type shouldBe "lights"
            lightsCategory.color shouldBe "#FEE502"
            lightsCategory.isFavorite shouldBe true

            val shadingCategory = app.cats["1c8b2764-0054-365d-fffffd5438c1b69c"]
            shadingCategory.shouldNotBeNull()
            shadingCategory.name shouldBe "Stínění"
            shadingCategory.type shouldBe "shading"

            // Verify controls exist
            app.controls.shouldNotBeNull()
            val switchControl = app.controls["933a5d30-9e71-4f42-a8d07c50f0a6839d"]
            switchControl.shouldNotBeNull()
            switchControl.name shouldBe "Notifikace povoleny"
            switchControl.type shouldBe "Switch"

            // Verify global states
            app.globalStates.shouldNotBeNull()
            app.globalStates.sunrise.shouldNotBeNull()
            app.globalStates.sunset.shouldNotBeNull()
        }

        should("validate extension functions work with loaded app") {
            val resourceStream = this::class.java.classLoader.getResourceAsStream("app/sample-app.json")
            resourceStream.shouldNotBeNull()

            val jsonContent = resourceStream.bufferedReader().use { it.readText() }
            val app = json.decodeFromString<LoxoneApp>(jsonContent)

            // Test getRoom
            val room = app.getRoom("1c8b2764-0061-373e-fffffd5438c1b69c")
            room.shouldNotBeNull()
            room.name shouldBe "Obývací pokoj"

            // Test getCategory
            val category = app.getCategory("1c8b2764-0054-365b-fffffd5438c1b69c")
            category.shouldNotBeNull()
            category.name shouldBe "Osvětlení"

            // Test getControl
            val control = app.getControl("933a5d30-9e71-4f42-a8d07c50f0a6839d")
            control.shouldNotBeNull()
            control.name shouldBe "Notifikace povoleny"

            // Test getControlsByType
            val switches = app.getControlsByType("Switch")
            switches.shouldNotBeNull()
            switches.any { it.name == "Notifikace povoleny" } shouldBe true

            // Test getControlsForCategory
            val lightControls = app.getControlsForCategory("1c8b2764-0054-365b-fffffd5438c1b69c")
            lightControls.shouldNotBeNull()

            // Test getVisibleControls (controls with non-empty type)
            val visibleControls = app.getVisibleControls()
            visibleControls.shouldNotBeNull()
            visibleControls.all { it.type.isNotEmpty() } shouldBe true
        }
    }
})
