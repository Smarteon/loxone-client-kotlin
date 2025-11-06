package cz.smarteon.loxkt.app

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class AppExtensionsTest : ShouldSpec({

    val testApp = LoxoneApp(
        lastModified = "2024-10-22 10:30:00",
        msInfo = MiniserverInfo(
            serialNr = "504F94123456",
            msName = "TestMiniserver",
            projectName = "TestProject",
            tempUnit = 0,
            miniserverType = 2,
            sortByRating = true
        ),
        rooms = mapOf(
            "room-1" to Room(uuid = "room-1", name = "Living Room", defaultRating = 5),
            "room-2" to Room(uuid = "room-2", name = "Kitchen", defaultRating = 3)
        ),
        cats = mapOf(
            "cat-1" to Category(uuid = "cat-1", name = "Lights", defaultRating = 4),
            "cat-2" to Category(uuid = "cat-2", name = "Climate", defaultRating = 5)
        ),
        controls = mapOf(
            "control-1" to Control(
                name = "Main Light",
                type = "Switch",
                uuidAction = "control-1",
                room = "room-1",
                cat = "cat-1",
                defaultRating = 5
            ),
            "control-2" to Control(
                name = "Kitchen Light",
                type = "Switch",
                uuidAction = "control-2",
                room = "room-2",
                cat = "cat-1",
                defaultRating = 3
            ),
            "control-3" to Control(
                name = "Thermostat",
                type = "IRoomControllerV2",
                uuidAction = "control-3",
                room = "room-1",
                cat = "cat-2",
                defaultRating = 4
            ),
            "control-4" to Control(
                name = "Internal Control",
                type = "Switch",
                uuidAction = "control-4",
                restrictions = 0b000001 // bit 0 set = referenced only internal
            ),
            "control-5" to Control(
                name = "Hidden Control",
                type = "",
                uuidAction = "control-5"
            )
        )
    )

    context("getControlsForRoom") {
        should("return controls for specific room") {
            val controls = testApp.getControlsForRoom("room-1")
            controls shouldHaveSize 2
            controls.map { it.name } shouldContain "Main Light"
            controls.map { it.name } shouldContain "Thermostat"
        }

        should("return empty list for room with no controls") {
            val controls = testApp.getControlsForRoom("nonexistent-room")
            controls shouldHaveSize 0
        }
    }

    context("getControlsForCategory") {
        should("return controls for specific category") {
            val controls = testApp.getControlsForCategory("cat-1")
            controls shouldHaveSize 2
            controls.map { it.name } shouldContain "Main Light"
            controls.map { it.name } shouldContain "Kitchen Light"
        }
    }

    context("getControlsByType") {
        should("return controls of specific type") {
            val switches = testApp.getControlsByType("Switch")
            switches shouldHaveSize 3 // Including internal control

            val ircs = testApp.getControlsByType("IRoomControllerV2")
            ircs shouldHaveSize 1
            ircs.first().name shouldBe "Thermostat"
        }
    }

    context("getControl") {
        should("return control by UUID") {
            val control = testApp.getControl("control-1")
            control?.name shouldBe "Main Light"
        }

        should("return null for nonexistent UUID") {
            val control = testApp.getControl("nonexistent")
            control shouldBe null
        }
    }

    context("getRoom") {
        should("return room by UUID") {
            val room = testApp.getRoom("room-1")
            room?.name shouldBe "Living Room"
        }
    }

    context("getCategory") {
        should("return category by UUID") {
            val category = testApp.getCategory("cat-1")
            category?.name shouldBe "Lights"
        }
    }

    context("getRoomName") {
        should("return room name for control") {
            val control = testApp.getControl("control-1")!!
            testApp.getRoomName(control) shouldBe "Living Room"
        }

        should("return null for control without room") {
            val control = testApp.getControl("control-4")!!
            testApp.getRoomName(control) shouldBe null
        }
    }

    context("getCategoryName") {
        should("return category name for control") {
            val control = testApp.getControl("control-1")!!
            testApp.getCategoryName(control) shouldBe "Lights"
        }
    }

    context("getControlsSortedByRating") {
        should("return controls sorted by rating when sortByRating is true") {
            val sorted = testApp.getControlsSortedByRating()
            sorted.first().name shouldBe "Main Light" // rating 5
            sorted[1].name shouldBe "Thermostat" // rating 4
        }
    }

    context("getRoomsSortedByRating") {
        should("return rooms sorted by rating when sortByRating is true") {
            val sorted = testApp.getRoomsSortedByRating()
            sorted.first().name shouldBe "Living Room" // rating 5
            sorted.last().name shouldBe "Kitchen" // rating 3
        }
    }

    context("getCategoriesSortedByRating") {
        should("return categories sorted by rating when sortByRating is true") {
            val sorted = testApp.getCategoriesSortedByRating()
            sorted.first().name shouldBe "Climate" // rating 5
            sorted.last().name shouldBe "Lights" // rating 4
        }
    }

    context("getVisibleControls") {
        should("return only controls with non-empty type") {
            val visible = testApp.getVisibleControls()
            visible shouldHaveSize 4
            visible.map { it.name } shouldContain "Main Light"
            visible.none { it.name == "Hidden Control" } shouldBe true
        }
    }

    context("getControlsFiltered") {
        should("filter out referenced only controls when includeReferencedOnly is false") {
            val filtered = testApp.getControlsFiltered(
                includeReferencedOnly = false,
                includeReadOnly = true,
                forExternal = false
            )
            filtered.none { it.name == "Internal Control" } shouldBe true
        }

        should("include all controls when both flags are true") {
            val filtered = testApp.getControlsFiltered(
                includeReferencedOnly = true,
                includeReadOnly = true,
                forExternal = false
            )
            filtered shouldHaveSize 5
        }
    }
})
