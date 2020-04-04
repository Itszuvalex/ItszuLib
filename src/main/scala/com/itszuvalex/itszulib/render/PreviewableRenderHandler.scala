package com.itszuvalex.itszulib.render

import com.itszuvalex.itszulib.api.ItszuLibCapabilities
import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.wrappers.Converter
import com.itszuvalex.itszulib.util.PlayerUtils
import net.minecraft.client.Minecraft
import net.minecraft.util.math.BlockPos
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
    val player = Minecraft.getMinecraft.player
    player.getHeldEquipment.iterator().next() match {
      case null =>
      case stack if stack.isEmpty =>
      case stack if stack.hasCapability(ItszuLibCapabilities.ITEM_PREVIEWABLE, null) =>
        val prev = stack.getCapability(ItszuLibCapabilities.ITEM_PREVIEWABLE, null)
        PreviewableRendererRegistry.getRenderer(prev.renderID) match {
          case Some(renderer) =>
            val px       = player.prevPosX + (player.posX - player.prevPosX) * event.getPartialTicks
            val py       = player.prevPosY + (player.posY - player.prevPosY) * event.getPartialTicks
            val pz       = player.prevPosZ + (player.posZ - player.prevPosZ) * event.getPartialTicks
            val hitVec   = PlayerUtils.positionLookedAt(Minecraft.getMinecraft.playerController.getBlockReachDistance, event.getPartialTicks)
            val blockPos = new BlockPos(hitVec)
            if (prev.snapToBlockGrid)
              renderer.renderAtLocation(Converter.IItemStackFromItemStack(stack), Minecraft.getMinecraft.player, Loc4(player.getEntityWorld, blockPos), blockPos.getX - px, blockPos.getY - py, blockPos.getZ - pz)
            else
              renderer.renderAtLocation(Converter.IItemStackFromItemStack(stack), Minecraft.getMinecraft.player, Loc4(player.getEntityWorld, blockPos), hitVec.x - px, hitVec.y - py, hitVec.z - pz)
          case None =>
        }
      case _ =>
    }
  }

}
