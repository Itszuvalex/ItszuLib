package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.client.ScreenMath
import com.itszuvalex.itszulib.research.TechTree
import com.itszuvalex.itszulib.research.TechTreeLayout
import com.itszuvalex.itszulib.research.Technologies
import com.itszuvalex.itszulib.research.TechnologyState
import com.itszuvalex.itszulib.team.Research
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

/**
 * Where a [TechTreeView] draws a layout: cells of [CELL_WIDTH] x [CELL_HEIGHT] pixels, each node a [NODE] pixel
 * square at its cell's top left, the whole layout inset by [PAD] and shifted by the pan. Pure, for tests.
 */
object TechTreeGeometry {
    const val CELL_WIDTH = 40
    const val CELL_HEIGHT = 28
    const val NODE = 20
    const val PAD = 8

    /** The node's top left, relative to the view's top left. */
    @JvmStatic
    fun nodeX(point: TechTreeLayout.Point, panX: Double): Int = (PAD + point.x * CELL_WIDTH - panX).roundToInt()

    @JvmStatic
    fun nodeY(point: TechTreeLayout.Point, panY: Double): Int = (PAD + point.y * CELL_HEIGHT - panY).roundToInt()

    /** The layout's size in pixels, padding included. */
    @JvmStatic
    fun contentWidth(layout: TechTreeLayout.Result): Int = if (layout.positions.isEmpty()) 0 else ((layout.width - 1) * CELL_WIDTH).roundToInt() + NODE + 2 * PAD

    @JvmStatic
    fun contentHeight(layout: TechTreeLayout.Result): Int = if (layout.positions.isEmpty()) 0 else ((layout.height - 1) * CELL_HEIGHT).roundToInt() + NODE + 2 * PAD

    /**
     * The pan that keeps [content] pixels in a [view] pixel window: centred if it fits, else clamped to its ends.
     */
    @JvmStatic
    fun clampPan(pan: Double, content: Int, view: Int): Double = if (content <= view) (content - view) / 2.0 else pan.coerceIn(0.0, (content - view).toDouble())

    /**
     * The technology whose node is at ([mx], [my]) relative to the view's top left, among [visible].
     */
    @JvmStatic
    fun nodeAt(layout: TechTreeLayout.Result, visible: Set<Identifier>, panX: Double, panY: Double, mx: Double, my: Double): Identifier? =
        layout.positions.entries.firstOrNull { (id, p) ->
            id in visible && mx >= nodeX(p, panX) && mx < nodeX(p, panX) + NODE && my >= nodeY(p, panY) && my < nodeY(p, panY) + NODE
        }?.key
}

/**
 * Shows one tech tree ([TechTree]) laid out by [TechTreeLayout], in a [NodeTreeView]: each technology as its icon in a
 * frame coloured by its state for the team (green researched, yellow available, grey locked; hidden ones are not
 * drawn), progress under it, its place in the team's research queue ([Research.queue]) as a badge, links to its
 * prerequisites, and a tooltip with its name, description, progress, other requirements (resources, items to hand in),
 * rewards, queue place and missing prerequisites. Drag to pan, scroll to pan up and down (with shift, sideways);
 * clicking a technology calls [onSelect], right-clicking it [onAlternate]. It opens centred on [selected], if any.
 *
 * @param research The team's research; by default the local player's (synced team data).
 * @param selected Drawn with a white frame, e.g. what a machine is researching.
 */
class TechTreeView @JvmOverloads constructor(
    width: Int,
    height: Int,
    tree: Identifier,
    research: () -> Research = { Minecraft.getInstance().player?.let(TechTree::research) ?: Research.EMPTY },
    selected: () -> Identifier? = { null },
    onSelect: (Identifier) -> Unit = {},
    onAlternate: (Identifier) -> Unit = {},
) : NodeTreeView(width, height, { model(tree, research()) }, selected, onSelect, onAlternate) {
    companion object {
        private fun technologies(): Technologies = Minecraft.getInstance().level?.registryAccess()?.let(TechTree::of) ?: Technologies.EMPTY

        /** Tree [tree] for a team with [research], as a [NodeTreeModel]. */
        @JvmStatic
        fun model(tree: Identifier, research: Research): NodeTreeModel {
            val techs = technologies()
            return object : NodeTreeModel {
                override val layout = techs.layout(tree)

                override fun look(id: Identifier): NodeLook? {
                    val tech = techs[id] ?: return null
                    val state = when (techs.state(id, research) ?: TechnologyState.HIDDEN) {
                        TechnologyState.HIDDEN -> return null
                        TechnologyState.RESEARCHED -> NodeState.DONE
                        TechnologyState.AVAILABLE -> NodeState.AVAILABLE
                        TechnologyState.LOCKED -> NodeState.LOCKED
                    }
                    val progress = if (tech.cost > 0) (research.progressOf(id).toDouble() / tech.cost).toFloat() else 0f
                    val queued = research.queuePosition(id).takeIf { it >= 0 }?.let { (it + 1).toString() }
                    return NodeLook(tech.iconStack(), state, progress, queued)
                }

                override fun tooltip(id: Identifier): List<Component> = tooltip(techs, id, techs.state(id, research) ?: TechnologyState.HIDDEN, research)
            }
        }

        private fun tooltip(techs: Technologies, id: Identifier, state: TechnologyState, research: Research): List<Component> {
            val tech = techs[id] ?: return emptyList()
            val lines = ArrayList<Component>()
            lines += tech.displayName(id).copy().withStyle(
                when (state) {
                    TechnologyState.RESEARCHED -> ChatFormatting.GREEN
                    TechnologyState.AVAILABLE -> ChatFormatting.YELLOW
                    else -> ChatFormatting.GRAY
                },
            )
            lines += tech.displayDescription(id).copy().withStyle(ChatFormatting.GRAY)
            when (state) {
                TechnologyState.RESEARCHED -> lines += Component.translatable("gui.itszulib.research.researched").withStyle(ChatFormatting.GREEN)
                TechnologyState.AVAILABLE -> lines += Component.translatable(
                    "gui.itszulib.research.progress",
                    ScreenMath.formatAmount(research.progressOf(id)),
                    ScreenMath.formatAmount(tech.cost),
                ).withStyle(ChatFormatting.YELLOW)
                else -> {}
            }
            if (state != TechnologyState.RESEARCHED) {
                for ((resource, amount) in tech.resources) {
                    val have = amount - (techs.remaining(id, research)?.resources?.get(resource) ?: amount)
                    lines += Component.translatable("gui.itszulib.research.resource", Technologies.resourceName(resource), ScreenMath.formatAmount(have), ScreenMath.formatAmount(amount))
                        .withStyle(if (have >= amount) ChatFormatting.GREEN else ChatFormatting.YELLOW)
                }
                techs.remaining(id, research)?.items?.forEach { (item, left) ->
                    val name = item.ingredient().items().findFirst().map { net.minecraft.world.item.ItemStack(it).hoverName }.orElse(Component.literal("?"))
                    lines += Component.translatable("gui.itszulib.research.item", name, item.count() - left, item.count())
                        .withStyle(if (left <= 0) ChatFormatting.GREEN else ChatFormatting.YELLOW)
                }
            }
            if (tech.rewards.isNotEmpty()) {
                lines += Component.translatable("gui.itszulib.research.rewards").withStyle(ChatFormatting.LIGHT_PURPLE)
                for (reward in tech.rewards) lines += Component.literal("  ${reward.count()} × ").append(reward.create().hoverName).withStyle(ChatFormatting.LIGHT_PURPLE)
            }
            research.queuePosition(id).takeIf { it >= 0 }?.let {
                lines += Component.translatable("gui.itszulib.research.queued", it + 1).withStyle(ChatFormatting.AQUA)
            }
            if (state == TechnologyState.LOCKED || state == TechnologyState.HIDDEN) {
                lines += Component.translatable("gui.itszulib.research.requires").withStyle(ChatFormatting.RED)
                for (p in techs.missingPrerequisites(id, research)) {
                    lines += Component.literal("  ").append(techs[p]?.displayName(p) ?: Component.literal(p.toString())).withStyle(ChatFormatting.RED)
                }
            }
            return lines
        }
    }
}
