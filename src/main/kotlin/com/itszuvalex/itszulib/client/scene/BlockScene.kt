package com.itszuvalex.itszulib.client.scene

import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.QuadInstance
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * Blocks drawn in 3D inside a screen: a NeoForge picture-in-picture element ([BlockSceneRenderer], registered by
 * ItszuLib's client setup). Positions are in blocks relative to the scene's centre, viewed through [rotation] (see
 * [BlockSceneGeometry] for the view space), [scale] GUI pixels per block. Each [BlockSceneBlock] is drawn from its
 * block model, so parts drawn by block entity renderers are missing; [overlays] are flat translucent faces drawn over
 * the blocks.
 */
class BlockSceneRenderState(
    val blocks: List<BlockSceneBlock>,
    val overlays: List<BlockSceneOverlay>,
    val rotation: Quaternionf,
    private val x0: Int,
    private val y0: Int,
    private val x1: Int,
    private val y1: Int,
    private val scale: Float,
    private val scissorArea: ScreenRectangle?,
) : PictureInPictureRenderState {
    private val bounds = PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea)

    override fun x0() = x0

    override fun y0() = y0

    override fun x1() = x1

    override fun y1() = y1

    override fun scale() = scale

    override fun scissorArea() = scissorArea

    override fun bounds() = bounds
}

/**
 * A block in a [BlockSceneRenderState]: its model parts and tint colours (collected on the render thread, see [of]),
 * centred at [centre] with edge [size].
 */
class BlockSceneBlock(val parts: List<BlockStateModelPart>, val tints: IntArray, val centre: Vector3f, val size: Float) {
    /**
     * True if the model has no quads (the block is drawn by its block entity renderer, or not at all).
     */
    val empty: Boolean = parts.all { part -> SIDES.all { part.getQuads(it).isEmpty() } }

    companion object {
        private val SIDES: List<Direction?> = Direction.entries + listOf(null)

        /**
         * The block at [pos] in [level] as it is there (model data and tints from the world).
         */
        @JvmStatic
        @JvmOverloads
        fun of(level: ClientLevel, pos: BlockPos, centre: Vector3f, size: Float = 1f, state: BlockState = level.getBlockState(pos)): BlockSceneBlock {
            val minecraft = Minecraft.getInstance()
            val parts = mutableListOf<BlockStateModelPart>()
            minecraft.modelManager.blockStateModelSet.get(state).collectParts(level, pos, state, RandomSource.create(pos.asLong()), parts)
            val tints = minecraft.blockColors.getTintSources(state).map { it.colorInWorld(state, level, pos) or OPAQUE }.toIntArray()
            return BlockSceneBlock(parts, tints, centre, size)
        }

        private const val OPAQUE = 0xFF000000.toInt()
    }
}

/**
 * A flat ARGB face of the cube of edge [size] centred at [centre], drawn over the blocks.
 */
class BlockSceneOverlay(val centre: Vector3f, val size: Float, val face: Direction, val color: Int)

/**
 * Draws [BlockSceneRenderState]s.
 */
class BlockSceneRenderer(bufferSource: MultiBufferSource.BufferSource) : PictureInPictureRenderer<BlockSceneRenderState>(bufferSource) {
    private val quad = QuadInstance()

    override fun getRenderStateClass(): Class<BlockSceneRenderState> = BlockSceneRenderState::class.java

    override fun getTextureLabel(): String = "itszulib_block_scene"

    override fun getTranslateY(height: Int, guiScale: Int): Float = height / 2f

    override fun renderToTexture(renderState: BlockSceneRenderState, poseStack: PoseStack) {
        Minecraft.getInstance().gameRenderer.lighting.setupFor(Lighting.Entry.ITEMS_3D)
        // The texture's pose has y down and z away from the viewer: half a turn about x from the scene's view space.
        poseStack.mulPose(Quaternionf().rotateX(Math.PI.toFloat()))
        poseStack.mulPose(renderState.rotation)
        quad.setLightCoords(FULL_BRIGHT)
        quad.setOverlayCoords(OverlayTexture.NO_OVERLAY)
        for (block in renderState.blocks) {
            poseStack.pushPose()
            poseStack.translate(block.centre.x, block.centre.y, block.centre.z)
            poseStack.scale(block.size, block.size, block.size)
            poseStack.translate(-0.5f, -0.5f, -0.5f)
            val pose = poseStack.last()
            for (part in block.parts) {
                for (side in SIDES) {
                    for (baked in part.getQuads(side)) {
                        val tint = baked.materialInfo().tintIndex()
                        quad.setColor(if (tint in block.tints.indices) block.tints[tint] else -1)
                        bufferSource.getBuffer(baked.materialInfo().itemRenderType()).putBakedQuad(pose, baked, quad)
                    }
                }
            }
            poseStack.popPose()
        }
        // The blocks' render types are batched until the end; draw them now so the overlays go on top.
        bufferSource.endBatch()
        for (block in renderState.blocks) {
            if (!block.empty) continue
            for (face in Direction.entries) face(poseStack, block.centre, block.size, face, EMPTY_BOX)
        }
        for (overlay in renderState.overlays) face(poseStack, overlay.centre, overlay.size, overlay.face, overlay.color)
    }

    private fun face(poseStack: PoseStack, centre: Vector3f, size: Float, face: Direction, color: Int) {
        val buffer = bufferSource.getBuffer(RenderTypes.textBackground())
        val pose = poseStack.last()
        for (corner in BlockSceneGeometry.faceCorners(face)) {
            corner.mul(size).add(centre)
            buffer.addVertex(pose, corner.x, corner.y, corner.z).setColor(color).setLight(FULL_BRIGHT)
        }
    }

    companion object {
        private const val FULL_BRIGHT = 0xF000F0

        /**
         * Blocks whose model has no quads (drawn by a block entity renderer, such as a chest) show as a faint box.
         */
        const val EMPTY_BOX = 0x40C0C0C0
        private val SIDES: List<Direction?> = Direction.entries + listOf(null)

        fun register(event: RegisterPictureInPictureRenderersEvent) {
            event.register(BlockSceneRenderState::class.java, ::BlockSceneRenderer)
        }
    }
}

/**
 * A box that can be clicked in a scene: the cube of edge [size] centred at [centre], identified by [key].
 */
class BlockScenePickBox<K>(val key: K, val centre: Vector3f, val size: Float)

/**
 * A pick result: the box hit and the face of it under the point.
 */
data class BlockSceneHit<K>(val key: K, val face: Direction)

/**
 * A scene's view space and picking, without client classes (so it can be unit tested). View space: x right, y up, z
 * towards the viewer, projected orthographically; screen y grows downwards.
 */
object BlockSceneGeometry {
    @JvmStatic
    fun rotation(yaw: Float, pitch: Float): Quaternionf = Quaternionf().rotateX(pitch).rotateY(yaw)

    /**
     * A yaw that turns [front] towards the viewer, a little to the left so a side shows too.
     */
    @JvmStatic
    fun defaultYaw(front: Direction): Float {
        val angle = kotlin.math.atan2(front.stepX.toFloat(), front.stepZ.toFloat())
        return -(Math.PI.toFloat() / 6) - angle
    }

    /**
     * The nearest face of [boxes] under a point [dx], [dy] GUI pixels from the scene's centre, or null. [scale] is
     * pixels per block.
     */
    @JvmStatic
    fun <K> pick(rotation: Quaternionf, dx: Double, dy: Double, scale: Float, boxes: List<BlockScenePickBox<K>>): BlockSceneHit<K>? {
        val px = (dx / scale).toFloat()
        val py = (-dy / scale).toFloat()
        var best: BlockSceneHit<K>? = null
        var bestDepth = Float.NEGATIVE_INFINITY
        for (box in boxes) {
            for (face in Direction.entries) {
                val depth = faceDepthAt(rotation, box.centre, box.size, face, px, py) ?: continue
                if (depth > bestDepth) {
                    bestDepth = depth
                    best = BlockSceneHit(box.key, face)
                }
            }
        }
        return best
    }

    /**
     * The depth (larger is nearer) at which the projected point ([px], [py]), in blocks, hits [face] of the cube of
     * edge [size] centred on [centre], or null if it misses or the face points away.
     */
    @JvmStatic
    fun faceDepthAt(rotation: Quaternionf, centre: Vector3f, size: Float, face: Direction, px: Float, py: Float): Float? {
        val normal = rotation.transform(unit(face))
        if (normal.z <= 1e-4f) return null
        val corners = faceCorners(face).map { rotation.transform(it.mul(size).add(centre)) }
        if (!insideConvex(corners, px, py)) return null
        val p0 = corners[0]
        return p0.z - (normal.x * (px - p0.x) + normal.y * (py - p0.y)) / normal.z
    }

    /**
     * The corners of a unit cube's [face], centred on the origin, counter-clockwise seen from outside the cube (so
     * culling render types keep the face).
     */
    @JvmStatic
    fun faceCorners(face: Direction): List<Vector3f> {
        val n = unit(face).mul(0.5f)
        val (u, v) = when (face.axis) {
            Direction.Axis.X -> Vector3f(0f, 0.5f, 0f) to Vector3f(0f, 0f, 0.5f)
            Direction.Axis.Y -> Vector3f(0.5f, 0f, 0f) to Vector3f(0f, 0f, 0.5f)
            Direction.Axis.Z -> Vector3f(0.5f, 0f, 0f) to Vector3f(0f, 0.5f, 0f)
        }
        val corners = listOf(
            Vector3f(n).sub(u).sub(v), Vector3f(n).add(u).sub(v), Vector3f(n).add(u).add(v), Vector3f(n).sub(u).add(v),
        )
        return if (Vector3f(u).cross(v).dot(n) > 0) corners else corners.reversed()
    }

    /**
     * The unit vector along [face].
     */
    @JvmStatic
    fun unit(face: Direction): Vector3f = Vector3f(face.stepX.toFloat(), face.stepY.toFloat(), face.stepZ.toFloat())

    private fun insideConvex(corners: List<Vector3f>, px: Float, py: Float): Boolean {
        var sign = 0
        for (i in corners.indices) {
            val a = corners[i]
            val b = corners[(i + 1) % corners.size]
            val cross = (b.x - a.x) * (py - a.y) - (b.y - a.y) * (px - a.x)
            val s = if (cross > 0) 1 else if (cross < 0) -1 else 0
            if (s == 0) continue
            if (sign == 0) sign = s else if (s != sign) return false
        }
        return true
    }
}

/**
 * A rotatable scene view in a screen area: drag to turn it, a press without a drag is a click. Keeps the view angles;
 * the owner supplies the blocks each frame ([submit]) and the clickable boxes ([pick]).
 */
class BlockSceneView(var scale: Float, var yaw: Float = 0f, var pitch: Float = DEFAULT_PITCH) {
    var x = 0
    var y = 0
    var width = 0
    var height = 0
    private var pressX = Double.NaN
    private var pressY = Double.NaN

    /**
     * True while the current press has turned into a drag.
     */
    var dragging = false
        private set

    fun rotation(): Quaternionf = BlockSceneGeometry.rotation(yaw, pitch)

    fun contains(mx: Double, my: Double) = mx >= x && mx < x + width && my >= y && my < y + height

    fun submit(graphics: GuiGraphicsExtractor, blocks: List<BlockSceneBlock>, overlays: List<BlockSceneOverlay>) {
        graphics.submitPictureInPictureRenderState(
            BlockSceneRenderState(blocks, overlays, rotation(), x, y, x + width, y + height, scale, graphics.peekScissorStack()),
        )
    }

    fun <K> pick(mx: Double, my: Double, boxes: List<BlockScenePickBox<K>>): BlockSceneHit<K>? {
        if (!contains(mx, my)) return null
        return BlockSceneGeometry.pick(rotation(), mx - (x + width / 2.0), my - (y + height / 2.0), scale, boxes)
    }

    /**
     * @return True if the press is in the view (the view then owns it until release).
     */
    fun press(mx: Double, my: Double): Boolean {
        if (!contains(mx, my)) return false
        pressX = mx
        pressY = my
        dragging = false
        return true
    }

    fun drag(mx: Double, my: Double, dx: Double, dy: Double): Boolean {
        if (pressX.isNaN()) return false
        if (!dragging && (mx - pressX) * (mx - pressX) + (my - pressY) * (my - pressY) < DRAG_THRESHOLD * DRAG_THRESHOLD) return true
        dragging = true
        yaw += (dx * DRAG_RADIANS).toFloat()
        pitch = (pitch + (dy * DRAG_RADIANS).toFloat()).coerceIn(-MAX_PITCH, MAX_PITCH)
        return true
    }

    /**
     * Ends a press.
     *
     * @return Null if no press was in the view; true if it was a click (not a drag), false otherwise.
     */
    fun release(): Boolean? {
        if (pressX.isNaN()) return null
        val click = !dragging
        pressX = Double.NaN
        pressY = Double.NaN
        dragging = false
        return click
    }

    companion object {
        const val DEFAULT_PITCH = 0.5f
        const val MAX_PITCH = 1.5f
        const val DRAG_RADIANS = 0.02
        const val DRAG_THRESHOLD = 3.0
    }
}
