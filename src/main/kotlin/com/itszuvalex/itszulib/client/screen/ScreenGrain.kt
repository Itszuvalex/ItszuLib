package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.ItszuLib
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.common.ModConfigSpec

/**
 * Per-pixel value noise over a panel, for a little grain: most pixels a shade lighter or darker, a few specks more so.
 * One [SIZE] x [SIZE] tile of noise is drawn white (lighter) or black (darker) with alpha, tiled over the panel and
 * scaled by the theme's [ScreenTheme.grain], so it shifts each pixel's value without changing its hue.
 */
object ScreenGrain {
    const val SIZE = 128

    /** About one pixel in this many is a speck. */
    const val SPECK_EVERY = 160

    private val TEXTURE: Identifier = Identifier.fromNamespaceAndPath(ItszuLib.ID, "screen_grain")
    private var registered = false

    /**
     * The noise at ([x], [y]) of the tile, in [-1, 1]: usually within ±0.25, ±0.6 to ±1 for specks. Deterministic.
     */
    @JvmStatic
    fun noise(x: Int, y: Int): Float {
        val h = hash(x * 73856093 xor y * 19349663)
        val unit = (h and 0xFFFF) / 65535f
        val sign = if ((h ushr 16) and 1 == 0) 1f else -1f
        return if ((h ushr 17) % SPECK_EVERY == 0) sign * (0.6f + 0.4f * unit) else sign * 0.25f * unit * unit
    }

    private fun hash(seed: Int): Int {
        var h = seed
        h = (h xor (h ushr 16)) * 0x45d9f3b
        h = (h xor (h ushr 16)) * 0x45d9f3b
        return (h xor (h ushr 16)) and 0x7FFFFFFF
    }

    /**
     * The tile's pixel at ([x], [y]) as ARGB: white for positive noise, black for negative, alpha from its size.
     */
    @JvmStatic
    fun pixel(x: Int, y: Int): Int {
        val n = noise(x, y)
        val alpha = (kotlin.math.abs(n) * 255).toInt().coerceIn(0, 255)
        return (alpha shl 24) or (if (n >= 0) 0xFFFFFF else 0)
    }

    private fun texture(): Identifier {
        if (!registered) {
            val image = NativeImage(SIZE, SIZE, false)
            for (y in 0 until SIZE) for (x in 0 until SIZE) image.setPixel(x, y, pixel(x, y))
            Minecraft.getInstance().textureManager.register(TEXTURE, DynamicTexture({ "ItszuLib screen grain" }, image))
            registered = true
        }
        return TEXTURE
    }

    /**
     * Grain over ([x], [y], [width], [height]) at [strength] (0 to 1), unless grain is turned off
     * ([ScreenThemeConfig.grain]).
     */
    @JvmStatic
    fun draw(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, strength: Float) {
        if (strength <= 0f || !ScreenThemeConfig.grain()) return
        val color = ((strength.coerceIn(0f, 1f) * 255).toInt() shl 24) or 0xFFFFFF
        val id = texture()
        var ty = 0
        while (ty < height) {
            var tx = 0
            val h = minOf(SIZE, height - ty)
            while (tx < width) {
                val w = minOf(SIZE, width - tx)
                graphics.blit(RenderPipelines.GUI_TEXTURED, id, x + tx, y + ty, 0f, 0f, w, h, SIZE, SIZE, color)
                tx += SIZE
            }
            ty += SIZE
        }
    }
}

/**
 * ItszuLib's client settings for screens (`config/itszulib-client.toml`, editable from the Mods screen): the theme to
 * draw every ItszuLib screen with (empty: each screen's own default), whether panels show grain, and the side
 * configuration view's background.
 */
object ScreenThemeConfig {
    private val builder = ModConfigSpec.Builder()

    private val THEME: ModConfigSpec.ConfigValue<String> = builder
        .comment("Screen theme for every ItszuLib-based screen, e.g. itszulib:light or itszulib:dark. Empty: each screen's own default.")
        .translation("itszulib.configuration.theme")
        .define("theme", "")

    private val GRAIN: ModConfigSpec.BooleanValue = builder
        .comment("Show a faint per-pixel grain on screen panels.")
        .translation("itszulib.configuration.grain")
        .define("grain", true)

    private val SIDE_CONFIG_LIGHT: ModConfigSpec.BooleanValue = builder
        .comment("Light background behind the 3D side configuration view (dark if false).")
        .translation("itszulib.configuration.side_config_light")
        .define("sideConfigLight", false)

    @JvmField
    val SPEC: ModConfigSpec = builder.build()

    /**
     * The player's chosen theme, or null for each screen's default (also before the config loads).
     */
    @JvmStatic
    fun chosen(): Identifier? = runCatching { THEME.get() }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let(Identifier::tryParse)

    @JvmStatic
    fun grain(): Boolean = runCatching { GRAIN.get() }.getOrDefault(true)

    @JvmStatic
    fun sideConfigLight(): Boolean = runCatching { SIDE_CONFIG_LIGHT.get() }.getOrDefault(false)

    /** Sets and saves the side configuration view's background (does nothing before the config loads). */
    @JvmStatic
    fun setSideConfigLight(light: Boolean) {
        runCatching {
            SIDE_CONFIG_LIGHT.set(light)
            SIDE_CONFIG_LIGHT.save()
        }
    }
}
