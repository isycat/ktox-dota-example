import com.isycat.dota.panorama.*
import com.isycat.dotaaddon.panorama.unitCardWrapperCardPanel
import com.isycat.dotaaddon.panorama.unitCardWrapperCardPanel2

root {
    scripts {
        include(src = "file://{resources}/scripts/custom_game/main-bundle.js")
    }

    styles {
        include(src = "s2r://panorama/styles/dotastyles.css")
        include(src = "file://{resources}/styles/custom_game/main-bundle.css")
    }

    snippets {
        snippet(name = "UnitCardWrapper") {
            unitCardWrapperCardPanel() // kotlin in src
            unitCardWrapperCardPanel2() // kts in /layout
        }

        snippet(name = "UnitCardContents") {
            Panel {
                Panel(id = "CardBack")
                Label(id = "CardTypeBG", classes = "ShopItemBG")
                Label(id = "CardFoil", hittest = false)
                Panel(id = "CardRarityIconWrapper", hittest = false) {
                    Label(id = "CardRarityIcon", hittest = false)
                    Label(id = "CardRarityIcon2", hittest = false)
                }
                Panel(id = "CardHeader") {
                    Label(id = "CardLevel")
                    Panel(id = "CardTitleWrapper") {
                        Label(id = "CardSuperTitle")
                        Label(id = "CardTitle")
                    }
                }
                Panel(id = "CardDetails") {
                    Label(id = "CardDescription")
                }
                Panel(id = "CardCostContainer") {
                    Label(id = "CardCostIcon")
                    Label(id = "CardCost")
                }
                Panel(id = "CardTypes")
                Panel(id = "TradeIcon")
                Panel(id = "XpButton", hittest = true) {
                    Panel(id = "XpButtonInner", hittest = true)
                    Panel(id = "XpButtonCornerInner") {
                        Label(id = "XpButtonLabel", hittest = false, text = "XP")
                        Panel(id = "XpButtonImage", hittest = false)
                    }
                }
                Panel(id = "MasterBallButton") {
                    Panel(id = "MasterBallButtonInner")
                    Panel(id = "MasterBallButtonCornerInner") {
                        Panel(id = "MasterBallButtonImage", hittest = false)
                    }
                }
                Panel(id = "ShopItemPortraitBG")
                Label(id = "CardImage")
                DOTAScenePanel(
                    id = "ShopItemPortrait",
                    antialias = false,
                    hittest = false,
                    light = "light",
                    particleonly = false,
                )
            }
        }

        snippet(name = "AvatarAbilityButton") {
            Panel {
                Label(id = "BindingLabel")
                Panel(id = "CardCostContainer") {
                    Label(id = "CardCostIcon")
                    Label(id = "CardCost")
                }
                DOTAAbilityImage(id = "AbilityImage", hittest = true)
            }
        }

        snippet(name = "WideAbilityPanel") {
            Panel {
                Label(id = "OptionLabel")
                DOTAAbilityImage(id = "OptionBGImage")
            }
        }

        snippet(name = "TradeWindow") {
            Panel(classes = "TradeWindow Dialog") {
                Panel(style = "width: 100%; vertical-align: top;") {
                    Label(id = "TradeTitle", text = "Trade")
                    Label(id = "CancelTradeButton", classes = "DialogButton Red Icon", text = "×")
                }
                Panel(id = "TradeUnitPanel") {
                    Panel(id = "TradeUnitSelector")
                }
                Panel(id = "TradeSummary") {
                    Label(text = "Do you want to trade your")
                    Label(id = "TradeSummaryFirstUnit", text = "")
                    Label(text = "for")
                    Label(id = "TradeSummarySecondUnit", text = "")
                }
                Panel(style = "width: 100%; vertical-align: bottom;") {
                    Label(id = "ConfirmTradeButton", classes = "DialogButton", text = "Confirm")
                }
            }
        }

        snippet(name = "TradeUnitOption") {
            Panel(classes = "TradeUnitOption") {
                DOTAScenePanel(
                    id = "TradeUnitOptionPortrait",
                    hittest = false,
                    light = "light",
                    antialias = false,
                    particleonly = false,
                )
            }
        }

        snippet(name = "UnitFrame") {
            Panel(classes = "UnitFrame", disablefocusonmousedown = true) {
                Panel(
                    hittest = true,
                    id = "UnitFrameCore",
                    classes = "UnitFrameCore",
                    disablefocusonmousedown = true,
                ) {
                    Label(id = "UnitFrameLevel", text = "0")
                    Label(id = "UnitFrameName", text = "")
                    Panel(id = "UnitFrameBG", classes = "TypeBG")
                    Panel(id = "UnitFramePortraitBG")
                    Panel(id = "UnitTypes")
                    DOTAScenePanel(
                        id = "UnitFramePortrait",
                        antialias = true,
                        hittest = false,
                        light = "light",
                        particleonly = false,
                    )
                    Panel(id = "MovePanel") {
                        Panel(id = "MoveSelector1", classes = "MoveSelector", hittest = true)
                        Panel(id = "MoveSelector2", classes = "MoveSelector", hittest = true)
                        Panel(id = "MoveSelector3", classes = "MoveSelector", hittest = true)
                        Panel(id = "MoveSelector4", classes = "MoveSelector", hittest = true)
                    }
                }
                Panel(id = "SidePane") {
                    Panel(id = "SpecializationPane")
                    Panel(id = "ItemPane") {
                        Panel(id = "ItemSelector", classes = "MoveSelector", hittest = true)
                    }
                }
            }
        }
    }

    Panel(hittest = false, classes = "MainHud") {
        Panel(id = "FloatingTextRoot")
        Panel(id = "x3DUIRoot") {
            Panel(id = "BenchSlot3d0", classes = "BenchSlot3d") { Label(id = "BenchSlot3dLabel") }
            Panel(id = "BenchSlot3d1", classes = "BenchSlot3d") { Label(id = "BenchSlot3dLabel") }
            Panel(id = "BenchSlot3d2", classes = "BenchSlot3d") { Label(id = "BenchSlot3dLabel") }
            Panel(id = "BenchSlot3d3", classes = "BenchSlot3d") { Label(id = "BenchSlot3dLabel") }
            Panel(id = "BenchSlot3d4", classes = "BenchSlot3d") { Label(id = "BenchSlot3dLabel") }
            Panel(id = "BenchSlot3d5", classes = "BenchSlot3d") { Label(id = "BenchSlot3dLabel") }
            Panel(id = "AddBenchSlotButton", classes = "BenchSlot3d") {
                Label(id = "BenchSlot3dLabel", text = "+")
                Label(id = "BenchSlot3dIcon")
            }
        }

        Panel(hittest = false, id = "Vignette")
        Panel(hittest = false, id = "VV1")
        Panel(hittest = false, id = "VV2")

        Panel(hittest = false, id = "RootPanel") {
            Panel(
                id = "PopupLayer",
                hittest = false,
                style = "x:0; y:0; width: 100%; height: 100%;",
                onload = "",
            )

            Panel(id = "Announcer") {
                Label(id = "AnnouncerText")
            }

            Panel(id = "UnitFrames", hittest = false, classes = "UnitFramesContainer") {
                Panel(id = "UnitFrame1")
                Panel(id = "UnitFrame2")
                Panel(id = "UnitFrame3")
                Panel(id = "UnitFrame4")
                Panel(id = "UnitFrame5")
                Panel(id = "UnitFrame6")
            }

            Panel(id = "TopLeftShadow")

            Panel(id = "BottomHud", style = "flow-children: down;") {
                Panel(id = "DexTop")
                Panel(id = "SelectedUnitLevelContainer") {
                    Label(id = "SelectedUnitLevel", text = "0")
                }
                Panel(id = "DexContents", style = "flow-children: down;") {
                    Panel(id = "SelectedUnitTitle") {
                        Label(id = "SelectedUnitName", text = "")
                        Panel(id = "SelectedUnitTypes", style = "flow-children: right;")
                    }
                    Panel(id = "SelectedUnit") {
                        Panel(id = "SelectedUnitLeft") {
                            Panel(id = "SelectedUnitStatsLeft") {
                                Panel(classes = "SelectedUnitStatRow") {
                                    Label(classes = "SelectedUnitStatLabel", text = "HP: ")
                                    Label(
                                        id = "SelectedUnitStats1",
                                        classes = "SelectedUnitStatValueLabel",
                                        text = "",
                                    )
                                }
                                Panel(classes = "SelectedUnitStatRow") {
                                    Label(classes = "SelectedUnitStatLabel", text = "Speed: ")
                                    Label(
                                        id = "SelectedUnitStats2",
                                        classes = "SelectedUnitStatValueLabel",
                                        text = "",
                                    )
                                }
                                Panel(classes = "SelectedUnitStatRow") {
                                    Label(classes = "SelectedUnitStatLabel", text = "Atk: ")
                                    Label(
                                        id = "SelectedUnitStats3",
                                        classes = "SelectedUnitStatValueLabel",
                                        text = "",
                                    )
                                }
                                Panel(classes = "SelectedUnitStatRow") {
                                    Label(classes = "SelectedUnitStatLabel", text = "Sp.Atk: ")
                                    Label(
                                        id = "SelectedUnitStats4",
                                        classes = "SelectedUnitStatValueLabel",
                                        text = "",
                                    )
                                }
                                Panel(classes = "SelectedUnitStatRow") {
                                    Label(classes = "SelectedUnitStatLabel", text = "Def: ")
                                    Label(
                                        id = "SelectedUnitStats5",
                                        classes = "SelectedUnitStatValueLabel",
                                        text = "",
                                    )
                                }
                                Panel(classes = "SelectedUnitStatRow") {
                                    Label(classes = "SelectedUnitStatLabel", text = "Sp.Def: ")
                                    Label(
                                        id = "SelectedUnitStats6",
                                        classes = "SelectedUnitStatValueLabel",
                                        text = "",
                                    )
                                }
                            }
                        }
                        Panel(id = "SelectedUnitRight") {
                            Panel(id = "SelectedUnitPortraitSection") {
                                DOTAScenePanel(
                                    id = "SelectedUnitPortrait",
                                    hittest = false,
                                    light = "light",
                                    antialias = false,
                                    particleonly = false,
                                )
                                Panel(id = "SelectedUnitExtra")
                            }
                        }
                    }
                    Panel(id = "SelectedUnitAbilityPanel", style = "flow-children: right;") {
                        Panel(id = "SelectedUnitItem")
                        Panel(id = "SelectedUnitAbilities")
                    }
                    Panel(id = "DexBottom")
                }
            }

            Panel(id = "Inventory", hittest = false) {
                Button(id = "CardDeleteButton")
                Panel(id = "InventoryHand", hittest = false)
            }

            Panel(id = "BottomAbilitiesContainer")

            Panel(id = "ShopPanel", classes = "Hidden", hittest = false) {
                Panel(id = "ShopPanelInner") {
                    Panel(id = "CurrentGoldContainer") {
                        Label(id = "GoldIcon")
                        Label(id = "GoldLabel")
                    }
                    Panel(id = "UpgradePoints") {
                        Label(id = "UpgradePointsIcon", text = "»")
                        Label(id = "UpgradePointsLabel", text = "0")
                    }
                    Panel(id = "ShopItemContainerGlow")
                    Panel(id = "ShopItemContainer")
                    Panel(id = "ShopAbilitiesContainer")

                    Label(
                        id = "ShopCloseButton",
                        classes = "DialogButton Red Icon",
                        text = "×",
                    )

                    Panel(id = "UpgradePanel", onfocus = "") {
                        Panel(id = "UpgradeContentWrapper", onfocus = "") {
                            Panel(id = "UpgradeBackground")
                            Panel(id = "UpgradeContainer", onfocus = "DropInputFocus()")
                        }
                    }

                    Panel(id = "ShopLockButton")
                }

                Panel(classes = "UnitCard", id = "UnitCard1", hittest = false) {
                    Panel(classes = "CardTransformWrapper", id = "CardTransformWrapper1", hittest = false) {
                        Button(classes = "ShopItem", id = "ShopItem1")
                    }
                }
                Panel(classes = "UnitCard", id = "UnitCard2", hittest = false) {
                    Panel(classes = "CardTransformWrapper", id = "CardTransformWrapper2", hittest = false) {
                        Button(classes = "ShopItem", id = "ShopItem2")
                    }
                }
                Panel(classes = "UnitCard", id = "UnitCard3", hittest = false) {
                    Panel(classes = "CardTransformWrapper", id = "CardTransformWrapper3", hittest = false) {
                        Button(classes = "ShopItem", id = "ShopItem3")
                    }
                }
                Panel(classes = "UnitCard", id = "UnitCard4", hittest = false) {
                    Panel(classes = "CardTransformWrapper", id = "CardTransformWrapper4", hittest = false) {
                        Button(classes = "ShopItem", id = "ShopItem4")
                    }
                }
                Panel(classes = "UnitCard", id = "UnitCard5", hittest = false) {
                    Panel(classes = "CardTransformWrapper", id = "CardTransformWrapper5", hittest = false) {
                        Button(classes = "ShopItem", id = "ShopItem5")
                    }
                }
            }

            Panel(id = "GameStatusPanel") {
                Panel(id = "TimerPanel") {
                    ProgressBar(id = "TimerProgressBar", min = 0, max = 100, value = 100)
                    Label(id = "TimerLabel", text = "")
                }
                Panel(id = "GameStatusBar") {
                    Label(id = "RoundLabel", text = "Round 0")
                    Panel(id = "GameState") {
                        Panel(id = "GameStateWrapper") {
                            Label(id = "GameStateText", text = "")
                            Label(id = "GameStateIcon", text = "")
                        }
                    }
                    Label(id = "PocketBallIcon")
                    Label(id = "PocketUnitCount", text = "0")
                    Label(id = "PocketBenchSize", text = "/2")
                    Panel(id = "AvatarAbilities")
                }
                Panel(id = "HpBar") {
                    Label(id = "HpLabel", text = "100")
                    ProgressBar(id = "HpProgressBar", min = 0, max = 100, value = 100)
                }
            }

            Label(id = "BigTimerLabel", text = "")

            Panel(id = "EventLog", hittest = false) {
                Panel(id = "Events", onfocus = "DropInputFocus()")
            }
        }
    }
}
