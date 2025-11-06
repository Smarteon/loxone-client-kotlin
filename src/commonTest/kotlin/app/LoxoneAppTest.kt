package cz.smarteon.loxkt.app

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class LoxoneAppTest : ShouldSpec({

    val json = Json { ignoreUnknownKeys = false }

    context("LoxoneApp deserialization") {
        should("parse minimal app") {
            val jsonString = """
                {
                    "lastModified": "2024-10-22 10:30:00",
                    "msInfo": {
                        "serialNr": "504F94123456",
                        "msName": "TestMiniserver",
                        "projectName": "TestProject",
                        "tempUnit": 0,
                        "miniserverType": 2
                    },
                    "rooms": {},
                    "cats": {},
                    "controls": {}
                }
            """.trimIndent()

            val app = json.decodeFromString<LoxoneApp>(jsonString)

            app.lastModified shouldBe "2024-10-22 10:30:00"
            app.msInfo.serialNr shouldBe "504F94123456"
            app.msInfo.msName shouldBe "TestMiniserver"
            app.rooms shouldHaveSize 0
            app.cats shouldHaveSize 0
            app.controls shouldHaveSize 0
        }

        should("parse app with rooms") {
            val jsonString = """
                {
                    "lastModified": "2024-10-22 10:30:00",
                    "msInfo": {
                        "serialNr": "504F94123456",
                        "msName": "TestMiniserver",
                        "projectName": "TestProject",
                        "tempUnit": 0,
                        "miniserverType": 2
                    },
                    "rooms": {
                        "0f1d2e3c-4b5a-6789-0abc-def123456789": {
                            "uuid": "0f1d2e3c-4b5a-6789-0abc-def123456789",
                            "name": "Living Room",
                            "image": "livingroom.svg",
                            "defaultRating": 3
                        },
                        "1a2b3c4d-5e6f-7890-1abc-def987654321": {
                            "uuid": "1a2b3c4d-5e6f-7890-1abc-def987654321",
                            "name": "Kitchen",
                            "image": "kitchen.svg",
                            "defaultRating": 5
                        }
                    },
                    "cats": {},
                    "controls": {}
                }
            """.trimIndent()

            val app = json.decodeFromString<LoxoneApp>(jsonString)

            app.rooms shouldHaveSize 2
            val livingRoom = app.rooms["0f1d2e3c-4b5a-6789-0abc-def123456789"]
            livingRoom.shouldNotBeNull()
            livingRoom.name shouldBe "Living Room"
            livingRoom.defaultRating shouldBe 3
        }

        should("parse app with categories") {
            val jsonString = """
                {
                    "lastModified": "2024-10-22 10:30:00",
                    "msInfo": {
                        "serialNr": "504F94123456",
                        "msName": "TestMiniserver",
                        "projectName": "TestProject",
                        "tempUnit": 0,
                        "miniserverType": 2
                    },
                    "rooms": {},
                    "cats": {
                        "0f1d2e3c-4b5a-6789-0abc-aaa111222333": {
                            "uuid": "0f1d2e3c-4b5a-6789-0abc-aaa111222333",
                            "name": "Lights",
                            "type": "lights",
                            "color": "#FFD700",
                            "defaultRating": 4
                        },
                        "1a2b3c4d-5e6f-7890-1abc-bbb444555666": {
                            "uuid": "1a2b3c4d-5e6f-7890-1abc-bbb444555666",
                            "name": "Climate",
                            "type": "indoortemperature",
                            "color": "#00BFFF",
                            "defaultRating": 5
                        }
                    },
                    "controls": {}
                }
            """.trimIndent()

            val app = json.decodeFromString<LoxoneApp>(jsonString)

            app.cats shouldHaveSize 2
            val lightsCategory = app.cats["0f1d2e3c-4b5a-6789-0abc-aaa111222333"]
            lightsCategory.shouldNotBeNull()
            lightsCategory.name shouldBe "Lights"
            lightsCategory.type shouldBe "lights"
            lightsCategory.color shouldBe "#FFD700"
        }

        should("parse app with controls") {
            val jsonString = """
                {
                    "lastModified": "2024-10-22 10:30:00",
                    "msInfo": {
                        "serialNr": "504F94123456",
                        "msName": "TestMiniserver",
                        "projectName": "TestProject",
                        "tempUnit": 0,
                        "miniserverType": 2
                    },
                    "rooms": {
                        "room-uuid-1": {
                            "uuid": "room-uuid-1",
                            "name": "Living Room"
                        }
                    },
                    "cats": {
                        "cat-uuid-1": {
                            "uuid": "cat-uuid-1",
                            "name": "Lights"
                        }
                    },
                    "controls": {
                        "control-uuid-1": {
                            "name": "Main Light",
                            "type": "Switch",
                            "uuidAction": "control-uuid-1",
                            "room": "room-uuid-1",
                            "cat": "cat-uuid-1",
                            "defaultRating": 4,
                            "isSecured": false,
                            "states": {
                                "active": "state-uuid-1"
                            },
                            "details": {
                                "format": "%.1f"
                            }
                        }
                    }
                }
            """.trimIndent()

            val app = json.decodeFromString<LoxoneApp>(jsonString)

            app.controls shouldHaveSize 1
            val control = app.controls["control-uuid-1"]
            control.shouldNotBeNull()
            control.name shouldBe "Main Light"
            control.type shouldBe "Switch"
            control.room shouldBe "room-uuid-1"
            control.cat shouldBe "cat-uuid-1"
            control.states?.get("active") shouldBe "state-uuid-1"
        }

        should("parse control with sub-controls") {
            val jsonString = """
                {
                    "lastModified": "2024-10-22 10:30:00",
                    "msInfo": {
                        "serialNr": "504F94123456",
                        "msName": "TestMiniserver",
                        "projectName": "TestProject",
                        "tempUnit": 0,
                        "miniserverType": 2
                    },
                    "rooms": {},
                    "cats": {},
                    "controls": {
                        "control-uuid-1": {
                            "name": "IRC",
                            "type": "IRoomControllerV2",
                            "uuidAction": "control-uuid-1",
                            "states": {
                                "tempActual": "temp-state-uuid"
                            },
                            "subControls": {
                                "tempSensor": {
                                    "name": "Temperature Sensor",
                                    "type": "Temperature",
                                    "uuidAction": "temp-sensor-uuid",
                                    "states": {
                                        "value": "temp-value-uuid"
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent()

            val app = json.decodeFromString<LoxoneApp>(jsonString)

            val control = app.controls["control-uuid-1"]
            control.shouldNotBeNull()
            control.subControls?.shouldHaveSize(1)
            val subControl = control.subControls?.get("tempSensor")
            subControl.shouldNotBeNull()
            subControl.name shouldBe "Temperature Sensor"
            subControl.type shouldBe "Temperature"
        }

        should("parse control with statistics") {
            val jsonString = """
                {
                    "lastModified": "2024-10-22 10:30:00",
                    "msInfo": {
                        "serialNr": "504F94123456",
                        "msName": "TestMiniserver",
                        "projectName": "TestProject",
                        "tempUnit": 0,
                        "miniserverType": 2
                    },
                    "rooms": {},
                    "cats": {},
                    "controls": {
                        "control-uuid-1": {
                            "name": "Power Meter",
                            "type": "Meter",
                            "uuidAction": "control-uuid-1",
                            "statistic": {
                                "frequency": 11,
                                "outputs": [
                                    {
                                        "id": 0,
                                        "name": "Power",
                                        "format": "%.2f W",
                                        "uuid": "output-uuid-1",
                                        "visuType": 0
                                    }
                                ]
                            }
                        }
                    }
                }
            """.trimIndent()

            val app = json.decodeFromString<LoxoneApp>(jsonString)

            val control = app.controls["control-uuid-1"]
            control.shouldNotBeNull()
            control.statistic.shouldNotBeNull()
            control.statistic.frequency shouldBe 11
            control.statistic.outputs.shouldHaveSize(1)
            control.statistic.outputs.first().name shouldBe "Power"
        }
    }

    context("Control extensions") {
        should("get state by name") {
            val control = Control(
                name = "Test Control",
                type = "Switch",
                uuidAction = "control-uuid",
                states = mapOf("active" to "state-uuid-1", "value" to "state-uuid-2")
            )

            control.getState("active") shouldBe "state-uuid-1"
            control.getState("value") shouldBe "state-uuid-2"
            control.getState("nonexistent") shouldBe null
        }

        should("check restriction flags") {
            val control = Control(
                name = "Test Control",
                type = "Switch",
                uuidAction = "control-uuid",
                restrictions = 0b100001 // bits 0 and 5 set
            )

            control.hasRestriction(0) shouldBe true
            control.hasRestriction(1) shouldBe false
            control.hasRestriction(5) shouldBe true
            control.isReferencedOnlyInternal shouldBe true
            control.isReadOnlyInternal shouldBe false
            control.isReadOnlyExternal shouldBe true
        }

        should("get sub-control by name") {
            val subControl = SubControl(
                name = "Sub Control",
                type = "Temperature",
                uuidAction = "sub-uuid"
            )
            val control = Control(
                name = "Test Control",
                type = "IRC",
                uuidAction = "control-uuid",
                subControls = mapOf("temp" to subControl)
            )

            control.getSubControl("temp") shouldBe subControl
            control.getSubControl("nonexistent") shouldBe null
        }
    }
})
