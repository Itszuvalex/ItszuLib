package com.itszuvalex.itszulib.render

import com.itszuvalex.itszulib.api.IPreviewable
import com.itszuvalex.itszulib.api.core.Loc4
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.RayTraceResult
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
    player.getHeldEquipment.iterator().next() match {
      case null =>
      case stack if stack.getItem != null && stack.getItem.isInstanceOf[IPreviewable] =>
        val prev = stack.getItem.asInstanceOf[IPreviewable]
        PreviewableRendererRegistry.getRenderer(prev.renderID) match {
          case Some(renderer) =>
            Minecraft.getMinecraft.objectMouseOver match {
              case null =>
              case vec if vec.typeOfHit == RayTraceResult.Type.BLOCK =>
                val world = player.getEntityWorld
                val hitPos = vec.getBlockPos
                val state = world.getBlockState(hitPos)
                val block = state.getBlock

                val px = player.prevPosX + (player.posX - player.prevPosX) * event.getPartialTicks
                val py = player.prevPosY + (player.posY - player.prevPosY) * event.getPartialTicks
                val pz = player.prevPosZ + (player.posZ - player.prevPosZ) * event.getPartialTicks

                if (block.isReplaceable(world, hitPos)) {
                  renderer.renderAtLocation(stack, new Loc4(world, hitPos), hitPos.getX - px, hitPos.getY - py, hitPos.getZ - pz)
                } else {
                  val bPos = hitPos.offset(vec.sideHit)
                  renderer.renderAtLocation(stack, new Loc4(world, bPos), bPos.getX - px, bPos.getY - py, bPos.getZ - pz)
                }

              case _ =>
            }
          case None =>
        }
      case _ =>
    }
  }

}
