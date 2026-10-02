package com.itszuvalex.itszulib.client

import com.itszuvalex.itszulib.client.scene.BlockSceneGeometry
import com.itszuvalex.itszulib.client.scene.BlockSceneHit
import com.itszuvalex.itszulib.client.scene.BlockScenePickBox
import net.minecraft.core.Direction
import org.joml.Vector3f
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlockSceneGeometryTest {
    private val scale = 30f
    private val centre = BlockScenePickBox("centre", Vector3f(), 1f)

    private fun east(distance: Float = 1.35f) = BlockScenePickBox("east", Vector3f(distance, 0f, 0f), 0.5f)

    private fun south() = BlockScenePickBox("south", Vector3f(0f, 0f, 1.35f), 0.5f)

    @Test
    fun Pick_Unrotated_CentreIsSouthFace() {
        // Unrotated, +z (south) faces the viewer.
        assertEquals(BlockSceneHit("centre", Direction.SOUTH), BlockSceneGeometry.pick(BlockSceneGeometry.rotation(0f, 0f), 0.0, 0.0, scale, listOf(centre)))
    }

    @Test
    fun Pick_PitchedDown_UpperPointIsUpFace() {
        val rotation = BlockSceneGeometry.rotation(0f, 0.6f)
        // Screen y grows downwards, so the top face is above the centre.
        assertEquals(Direction.UP, BlockSceneGeometry.pick(rotation, 0.0, -0.55 * scale, scale, listOf(centre))?.face)
        assertEquals(Direction.SOUTH, BlockSceneGeometry.pick(rotation, 0.0, 0.3 * scale, scale, listOf(centre))?.face)
    }

    @Test
    fun Pick_OutsideEverything_IsNull() {
        assertNull(BlockSceneGeometry.pick(BlockSceneGeometry.rotation(0f, 0f), 2.0 * scale, 0.0, scale, listOf(centre)))
    }

    @Test
    fun Pick_OtherBox_SelectsIt() {
        val rotation = BlockSceneGeometry.rotation(0f, 0f)
        val x = 1.35 * scale
        assertNull(BlockSceneGeometry.pick(rotation, x, 0.0, scale, listOf(centre)))
        assertEquals("east", BlockSceneGeometry.pick(rotation, x, 0.0, scale, listOf(centre, east()))?.key)
    }

    @Test
    fun Pick_NearerBox_Wins() {
        // The south box sits between the viewer and the centre box's south face.
        val rotation = BlockSceneGeometry.rotation(0f, 0f)
        assertEquals("south", BlockSceneGeometry.pick(rotation, 0.0, 0.0, scale, listOf(centre, south()))?.key)
        assertEquals("centre", BlockSceneGeometry.pick(rotation, 0.4 * scale, 0.0, scale, listOf(centre, south()))?.key)
    }

    @Test
    fun DefaultYaw_TurnsFrontTowardsViewer() {
        for (front in Direction.Plane.HORIZONTAL) {
            val rotation = BlockSceneGeometry.rotation(BlockSceneGeometry.defaultYaw(front), 0f)
            assertEquals(front, BlockSceneGeometry.pick(rotation, 0.0, 0.0, scale, listOf(centre))?.face, "front $front")
        }
    }

    @Test
    fun FaceCorners_AllFaces_CounterClockwiseFromOutside() {
        for (face in Direction.entries) {
            val c = BlockSceneGeometry.faceCorners(face)
            val normal = Vector3f(c[1]).sub(c[0]).cross(Vector3f(c[2]).sub(c[1]))
            assertTrue(normal.dot(BlockSceneGeometry.unit(face)) > 0, "face $face")
        }
    }
}
