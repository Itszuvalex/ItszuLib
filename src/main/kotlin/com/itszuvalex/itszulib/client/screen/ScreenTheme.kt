package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.ItszuLib
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * The colours a [ComponentScreen] and ItszuLib's components draw with (ARGB).
 *
 * @param panel The screen's background; [panelLight] and [panelDark] its bevel (top/left and bottom/right), inside
 * a one-pixel [outline].
 * @param slot A slot's face; [slotShadow] and [slotLight] its inset edges (top/left and bottom/right).
 * @param slotOutput The ring around take-only (output) slots.
 * @param frame Gauge and widget frames; [well] the empty part of gauges and bars.
 * @param text Titles and labels; [textMuted] secondary text.
 * @param grain How strongly per-pixel value noise shows on panels (0 for none, about 0.05 for a faint grain).
 * @param button A button's face ([ThemedButton], side panel tabs); [buttonHover] while hovered. Both default to
 * colours derived from the panel.
 */
data class ScreenTheme(
    val panel: Int,
    val panelLight: Int,
    val panelDark: Int,
    val outline: Int,
    val slot: Int,
    val slotShadow: Int,
    val slotLight: Int,
    val slotOutput: Int,
    val frame: Int,
    val well: Int,
    val text: Int,
    val textMuted: Int,
    val progress: Int,
    val energy: Int,
    val grain: Float,
    val button: Int = panel,
    val buttonHover: Int = panelLight,
) {
    companion object {
        /** Vanilla's grey, with its bevelled panel and inset slots. */
        @JvmField
        val LIGHT = ScreenTheme(
            panel = 0xFFC6C6C6.toInt(), panelLight = 0xFFFFFFFF.toInt(), panelDark = 0xFF555555.toInt(), outline = 0xFF000000.toInt(),
            slot = 0xFF8B8B8B.toInt(), slotShadow = 0xFF373737.toInt(), slotLight = 0xFFFFFFFF.toInt(), slotOutput = 0xFFB07A2A.toInt(),
            frame = 0xFF8B8B8B.toInt(), well = 0xFF2B2B2B.toInt(), text = 0xFF404040.toInt(), textMuted = 0xFF6A6A6A.toInt(),
            progress = 0xFF55DD55.toInt(), energy = 0xFFCC3333.toInt(), grain = 0.04f,
            button = 0xFFB4B4B4.toInt(), buttonHover = 0xFFC8D0E0.toInt(),
        )

        /** Near-black panels with a cool grey bevel and dark inset slots. */
        @JvmField
        val DARK = ScreenTheme(
            panel = 0xFF1E1E1E.toInt(), panelLight = 0xFF3E4040.toInt(), panelDark = 0xFF141414.toInt(), outline = 0xFF000000.toInt(),
            slot = 0xFF292929.toInt(), slotShadow = 0xFF0F0F0F.toInt(), slotLight = 0xFF3E4040.toInt(), slotOutput = 0xFF3A8FA8.toInt(),
            frame = 0xFF3E4040.toInt(), well = 0xFF0F0F0F.toInt(), text = 0xFFD8D8D8.toInt(), textMuted = 0xFF8A8A8A.toInt(),
            progress = 0xFF55DD55.toInt(), energy = 0xFFCC3333.toInt(), grain = 0.05f,
            button = 0xFF2C2E2E.toInt(), buttonHover = 0xFF3A484C.toInt(),
        )

        /**
         * An ARGB colour as `"#RRGGBB"` (opaque), `"#AARRGGBB"`, or a number.
         */
        @JvmField
        val COLOR: Codec<Int> = Codec.withAlternative(
            Codec.STRING.comapFlatMap(::parseColor) { "#%08X".format(it) },
            Codec.INT,
        )

        @JvmStatic
        fun parseColor(text: String): DataResult<Int> {
            val hex = text.removePrefix("#")
            val value = hex.toLongOrNull(16)
            return when {
                !text.startsWith("#") || value == null -> DataResult.error { "Not a colour: $text (use #RRGGBB or #AARRGGBB)" }
                hex.length == 6 -> DataResult.success((0xFF000000L or value).toInt())
                hex.length == 8 -> DataResult.success(value.toInt())
                else -> DataResult.error { "Not a colour: $text (use #RRGGBB or #AARRGGBB)" }
            }
        }
    }
}

/**
 * A theme as written in a JSON file: an optional [parent] it starts from (default `itszulib:light`) and the colours it
 * changes. Field names are the [ScreenTheme] properties in snake case (`panel_light`, `slot_output`, ...).
 */
data class ThemeDefinition(val parent: Optional<Identifier>, val values: Map<String, Int>, val grain: Optional<Float>) {
    /**
     * The theme this defines, on top of [parent].
     */
    fun resolve(parent: ScreenTheme): ScreenTheme {
        fun c(key: String, base: Int) = values[key] ?: base
        return ScreenTheme(
            panel = c("panel", parent.panel), panelLight = c("panel_light", parent.panelLight), panelDark = c("panel_dark", parent.panelDark),
            outline = c("outline", parent.outline), slot = c("slot", parent.slot), slotShadow = c("slot_shadow", parent.slotShadow),
            slotLight = c("slot_light", parent.slotLight), slotOutput = c("slot_output", parent.slotOutput), frame = c("frame", parent.frame),
            well = c("well", parent.well), text = c("text", parent.text), textMuted = c("text_muted", parent.textMuted),
            progress = c("progress", parent.progress), energy = c("energy", parent.energy), grain = grain.orElse(parent.grain),
            button = c("button", parent.button), buttonHover = c("button_hover", parent.buttonHover),
        )
    }

    companion object {
        @JvmField
        val KEYS = listOf(
            "panel", "panel_light", "panel_dark", "outline", "slot", "slot_shadow", "slot_light", "slot_output",
            "frame", "well", "text", "text_muted", "progress", "energy", "button", "button_hover",
        )

        @JvmField
        val CODEC: Codec<ThemeDefinition> = RecordCodecBuilder.create { i ->
            i.group(
                Identifier.CODEC.optionalFieldOf("parent").forGetter(ThemeDefinition::parent),
                Codec.unboundedMap(Codec.STRING, ScreenTheme.COLOR).validate { map ->
                    val unknown = map.keys - KEYS.toSet()
                    if (unknown.isEmpty()) DataResult.success(map) else DataResult.error { "Unknown theme colours $unknown; known: $KEYS" }
                }.optionalFieldOf("colors", emptyMap()).forGetter(ThemeDefinition::values),
                Codec.floatRange(0f, 1f).optionalFieldOf("grain").forGetter(ThemeDefinition::grain),
            ).apply(i, ::ThemeDefinition)
        }
    }
}

/**
 * The known screen themes: [LIGHT] and [DARK], any registered in code ([register]), and JSON files in resource packs
 * and mods' assets at `assets/<namespace>/itszulib/themes/<path>.json` (id `<namespace>:<path>`, reloaded with
 * resources; a file replaces a theme of the same id). A screen draws with [resolve]: the player's chosen theme
 * ([ScreenThemeConfig]) if it exists, else the screen's own default.
 */
object ScreenThemes {
    private val LOGGER = com.mojang.logging.LogUtils.getLogger()

    @JvmField
    val LIGHT: Identifier = Identifier.fromNamespaceAndPath(ItszuLib.ID, "light")

    @JvmField
    val DARK: Identifier = Identifier.fromNamespaceAndPath(ItszuLib.ID, "dark")

    private val code = ConcurrentHashMap<Identifier, ScreenTheme>().apply {
        put(LIGHT, ScreenTheme.LIGHT)
        put(DARK, ScreenTheme.DARK)
    }

    @Volatile
    private var loaded: Map<Identifier, ScreenTheme> = emptyMap()

    /**
     * Registers a theme from code (replaced by a resource file of the same id).
     */
    @JvmStatic
    fun register(id: Identifier, theme: ScreenTheme) {
        code[id] = theme
    }

    @JvmStatic
    operator fun get(id: Identifier): ScreenTheme? = loaded[id] ?: code[id]

    @JvmStatic
    fun ids(): Set<Identifier> = (code.keys + loaded.keys).toSortedSet()

    /**
     * The theme a screen whose default is [screenDefault] draws with.
     */
    @JvmStatic
    fun resolve(screenDefault: Identifier): ScreenTheme =
        ScreenThemeConfig.chosen()?.let(::get) ?: get(screenDefault) ?: ScreenTheme.LIGHT

    /**
     * Resolves definitions (each on top of its parent, which may itself be a definition), ignoring cycles and unknown
     * parents (logged).
     */
    @JvmStatic
    fun resolveAll(definitions: Map<Identifier, ThemeDefinition>, base: (Identifier) -> ScreenTheme?): Map<Identifier, ScreenTheme> {
        val done = HashMap<Identifier, ScreenTheme>()
        fun visit(id: Identifier, seen: Set<Identifier>): ScreenTheme? {
            done[id]?.let { return it }
            val def = definitions[id] ?: return base(id)
            if (id in seen) {
                LOGGER.error("Screen theme {} has a parent cycle", id)
                return null
            }
            val parentId = def.parent.orElse(LIGHT)
            val parent = (if (parentId == id) base(id) else visit(parentId, seen + id)) ?: run {
                LOGGER.error("Screen theme {} has unknown parent {}", id, parentId)
                return null
            }
            return def.resolve(parent).also { done[id] = it }
        }
        definitions.keys.forEach { visit(it, emptySet()) }
        return done
    }

    /**
     * Loads theme files on resource reload (registered from ItszuLib's client setup).
     */
    class Loader : SimpleJsonResourceReloadListener<ThemeDefinition>(ThemeDefinition.CODEC, FileToIdConverter.json("itszulib/themes")) {
        override fun apply(prepared: Map<Identifier, ThemeDefinition>, manager: ResourceManager, profiler: ProfilerFiller) {
            loaded = resolveAll(prepared) { code[it] }
        }
    }

    @JvmField
    val LOADER_ID: Identifier = Identifier.fromNamespaceAndPath(ItszuLib.ID, "screen_themes")
}
