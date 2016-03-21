package com.itszuvalex.itszulib.render

import com.itszuvalex.itszulib.api.IPreviewable
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.util.ForgeDirection

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
                val hitX = vec.blockX
                val hitY = vec.blockY
                val hitZ = vec.blockZ
                var side = vec.sideHit
                val block = world.getBlock(hitX, hitY, hitZ)

                var dir = ForgeDirection.UNKNOWN
                if (block == Blocks.snow_layer && (world.getBlockMetadata(hitX, hitY, hitZ) & 7) < 1) {
                  side = 1
                } else if (block != Blocks.vine && block != Blocks.tallgrass && block != Blocks.deadbush
                           && !block.isReplaceable(world, hitX, hitY, hitZ)) {
                  dir = ForgeDirection.getOrientation(side)
                }

                val bx = hitX + dir.offsetX
                val by = hitY + dir.offsetY
                val bz = hitZ + dir.offsetZ
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
