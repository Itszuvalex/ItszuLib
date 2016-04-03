package com.itszuvalex.itszulib.render

import com.itszuvalex.itszulib.api.IPreviewable
import net.minecraft.client.Minecraft
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by Christopher Harris (Itszuvalex) on 8/26/15.
  */
@SideOnly(Side.CLIENT)
class PreviewableRenderHandler {

  @SubscribeEvent
  def render(event: RenderWorldLastEvent): Unit = {
    val player = Minecraft.getMinecraft.thePlayer
    player.getCurrentEquippedItem match {
      case null =>
      case stack if stack.getItem != null && stack.getItem.isInstanceOf[IPreviewable] =>
        val prev = stack.getItem.asInstanceOf[IPreviewable]
        PreviewableRendererRegistry.getRenderer(prev.renderID) match {
          case Some(renderer) =>
            Minecraft.getMinecraft.objectMouseOver match {
              case null =>
              case vec if vec.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK =>
                val world = player.getEntityWorld
                val hitPos = vec.getBlockPos
                val state = world.getBlockState(hitPos)
                val block = state.getBlock

                var hitOffsetX = 0
                var hitOffsetY = 0
                var hitOffsetZ = 0

                if (!block.isReplaceable(world, hitPos)) {
                  val side = vec.sideHit
                  hitOffsetX += side.getFrontOffsetX
                  hitOffsetY += side.getFrontOffsetY
                  hitOffsetZ += side.getFrontOffsetZ
                }

                val bx = hitPos.getX + hitOffsetX
                val by = hitPos.getY + hitOffsetY
                val bz = hitPos.getZ + hitOffsetZ
                val px = player.prevPosX + (player.posX - player.prevPosX) * event.partialTicks
                val py = player.prevPosY + (player.posY - player.prevPosY) * event.partialTicks
                val pz = player.prevPosZ + (player.posZ - player.prevPosZ) * event.partialTicks

                renderer.renderAtLocation(stack, world, bx, by, bz,
                                          bx - px, by - py, bz - pz)
              case _ =>
            }
          case None =>
        }
      case _ =>
    }
  }

}
