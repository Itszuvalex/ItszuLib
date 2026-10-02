package com.itszuvalex.itszulib.client.screen

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.resources.Identifier
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private fun id(path: String) = Identifier.fromNamespaceAndPath("itszulib_test", path)

private fun definition(json: String): ThemeDefinition =
    ThemeDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow()

class ScreenThemeTests {
    @Test
    fun ParseColor_RgbIsOpaque_ArgbKept_OthersRejected() {
        Assertions.assertEquals(0xFF1E1E1E.toInt(), ScreenTheme.parseColor("#1E1E1E").getOrThrow())
        Assertions.assertEquals(0x801E1E1E.toInt(), ScreenTheme.parseColor("#801e1e1e").getOrThrow())
        for (bad in listOf("1E1E1E", "#12345", "#GGGGGG", "red")) Assertions.assertTrue(ScreenTheme.parseColor(bad).isError, bad)
    }

    @Test
    fun Definition_OverridesOnlyWhatItNames() {
        val def = definition("""{"parent": "itszulib:dark", "colors": {"text": "#FF0000", "slot_output": "#00FF00"}, "grain": 0}""")
        val theme = def.resolve(ScreenTheme.DARK)
        Assertions.assertEquals(0xFFFF0000.toInt(), theme.text)
        Assertions.assertEquals(0xFF00FF00.toInt(), theme.slotOutput)
        Assertions.assertEquals(ScreenTheme.DARK.panel, theme.panel)
        Assertions.assertEquals(0f, theme.grain)
    }

    @Test
    fun Definition_ButtonColours_OverriddenOrInherited() {
        val theme = definition("""{"colors": {"button_hover": "#123456"}}""").resolve(ScreenTheme.DARK)
        Assertions.assertEquals(0xFF123456.toInt(), theme.buttonHover)
        Assertions.assertEquals(ScreenTheme.DARK.button, theme.button)
    }

    @Test
    fun Theme_WithoutButtonColours_DerivesThemFromThePanel() {
        val theme = ScreenTheme(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0f)
        Assertions.assertEquals(theme.panel, theme.button)
        Assertions.assertEquals(theme.panelLight, theme.buttonHover)
    }

    @Test
    fun Definition_UnknownColourName_IsRejected() {
        val result = ThemeDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""{"colors": {"panle": "#000000"}}"""))
        Assertions.assertTrue(result.isError)
    }

    @Test
    fun ResolveAll_ChainsParents_DropsCyclesAndUnknownParents() {
        val defs = mapOf(
            id("base") to definition("""{"parent": "itszulib:dark", "colors": {"panel": "#101010"}}"""),
            id("child") to definition("""{"parent": "itszulib_test:base", "colors": {"text": "#FFFFFF"}}"""),
            id("loop_a") to definition("""{"parent": "itszulib_test:loop_b"}"""),
            id("loop_b") to definition("""{"parent": "itszulib_test:loop_a"}"""),
            id("orphan") to definition("""{"parent": "itszulib_test:missing"}"""),
            id("plain") to definition("""{}"""),
        )
        val builtIn = mapOf(ScreenThemes.LIGHT to ScreenTheme.LIGHT, ScreenThemes.DARK to ScreenTheme.DARK)
        val themes = ScreenThemes.resolveAll(defs) { builtIn[it] }
        Assertions.assertEquals(0xFF101010.toInt(), themes.getValue(id("child")).panel, "inherits from its parent's file")
        Assertions.assertEquals(0xFFFFFFFF.toInt(), themes.getValue(id("child")).text)
        Assertions.assertEquals(ScreenTheme.DARK.slot, themes.getValue(id("child")).slot, "and from the built-in below it")
        Assertions.assertEquals(ScreenTheme.LIGHT, themes.getValue(id("plain")), "no parent: light")
        Assertions.assertFalse(id("loop_a") in themes || id("loop_b") in themes || id("orphan") in themes)
    }

    @Test
    fun ResolveAll_FileMayReplaceABuiltInById() {
        val themes = ScreenThemes.resolveAll(mapOf(ScreenThemes.DARK to definition("""{"parent": "itszulib:dark", "colors": {"text": "#00FFFF"}}"""))) {
            if (it == ScreenThemes.DARK) ScreenTheme.DARK else null
        }
        Assertions.assertEquals(0xFF00FFFF.toInt(), themes.getValue(ScreenThemes.DARK).text)
        Assertions.assertEquals(ScreenTheme.DARK.panel, themes.getValue(ScreenThemes.DARK).panel)
    }

    @Test
    fun Grain_SmallMostlyWithRareSpecks_Deterministic() {
        val values = (0 until ScreenGrain.SIZE).flatMap { y -> (0 until ScreenGrain.SIZE).map { x -> ScreenGrain.noise(x, y) } }
        Assertions.assertTrue(values.all { it in -1f..1f })
        val specks = values.count { kotlin.math.abs(it) >= 0.6f }
        val expected = values.size / ScreenGrain.SPECK_EVERY
        Assertions.assertTrue(specks in expected / 2..expected * 2, "specks $specks, expected about $expected")
        Assertions.assertTrue(values.count { it > 0 } in values.size * 4 / 10..values.size * 6 / 10, "lighter and darker about evenly")
        Assertions.assertEquals(ScreenGrain.noise(5, 9), ScreenGrain.noise(5, 9))
        Assertions.assertEquals(0, ScreenGrain.pixel(5, 9) and 0x00FFFFFF.let { if (ScreenGrain.noise(5, 9) >= 0) 0 else it })
    }
}
