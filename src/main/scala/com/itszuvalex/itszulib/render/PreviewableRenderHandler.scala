package com.itszuvalex.itszulib.render

import com.itszuvalex.itszulib.api.IPreviewable
import com.itszuvalex.itszulib.api.core.Loc4
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.util.{EnumFacing, MovingObjectPosition}
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
                var side = vec.sideHit
                val state = world.getBlockState(hitPos)
                val block = state.getBlock

                var dir = EnumFacing.DOWN
                if (block == Blocks.snow_layer && (block.getMetaFromState(world.getBlockState(hitPos)) & 7) < 1) {
                  side = EnumFacing.UP
                } else if (block != Blocks.vine && block != Blocks.tallgrass && block != Blocks.deadbush && !block.isReplaceable(world, hitPos)) {
                  dir = side
                }

                val bPos = hitPos.offset(dir)
                val px = player.prevPosX + (player.posX - player.prevPosX) * event.partialTicks
                val py = player.prevPosY + (player.posY - player.prevPosY) * event.partialTicks
                val pz = player.prevPosZ + (player.posZ - player.prevPosZ) * event.partialTicks

                renderer.renderAtLocation(stack, new Loc4(world, bPos), bPos.getX - px, bPos.getY - py, bPos.getZ - pz)
              case _ =>
            }
          case None =>
        }
      case _ =>
    }
  }

}
