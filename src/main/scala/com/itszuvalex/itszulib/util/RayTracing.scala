package com.itszuvalex.itszulib.util

import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.{BlockPos, RayTraceResult, Vec3d}
import net.minecraft.world.IBlockAccess
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by Alex on 07.02.2017.
  */
@SideOnly(Side.CLIENT)
object RayTracing {

  /**
    * Determines a position which the player looks at, raytraced against blocks and a maximum range
    */
  def positionLookedAt(maxRange: Double, partialTicks: Float): Vec3d = {
    val player = Minecraft.getMinecraft.player
    player.rayTrace(maxRange, partialTicks) match {
      case hit if hit.typeOfHit == RayTraceResult.Type.BLOCK => offsetRayTraceResult(hit, false).hitVec
      case _ => player.getPositionEyes(partialTicks).add(player.getLookVec.scale(maxRange))
    }
  }

  //TODO continue
  def rayTraceFiltered(startVec: Vec3d, endVec: Vec3d, ignoredBlockRule: BlockFilterRule, validPlacementBlockRule: BlockFilterRule): RayTraceResult = {
    val world = Minecraft.getMinecraft.world
    val dir = endVec.subtract(startVec).normalize()

    var nextStart = startVec
    do {
      val res = world.rayTraceBlocks(startVec, endVec, false, false, true)
      if (res.typeOfHit != RayTraceResult.Type.BLOCK) return res

      nextStart = res.hitVec.add(dir.scale(.001))
    } while (nextStart.subtract(endVec).dotProduct(dir) > 0) // while we haven't reached/surpassed endVec

  }

  /**
    * Offset the given RayTraceResult's vector by offsetAmount away from the hit block face if a negative block face is hit (to distinguish between blocks).
    * @param offsetPositiveSides If true, also offsets when hitting positive block faces.
    */
  def offsetRayTraceResult(res: RayTraceResult, offsetPositiveSides: Boolean, offsetAmount: Double = .001): RayTraceResult = {
    if (res.typeOfHit == RayTraceResult.Type.BLOCK) {
      val offsetVec = res.sideHit match {
        case EnumFacing.DOWN => res.hitVec.subtract(0, offsetAmount, 0)
        case EnumFacing.UP if offsetPositiveSides => res.hitVec.addVector(0, offsetAmount, 0)
        case EnumFacing.NORTH => res.hitVec.subtract(0, 0, offsetAmount)
        case EnumFacing.SOUTH if offsetPositiveSides => res.hitVec.addVector(0, 0, offsetAmount)
        case EnumFacing.WEST => res.hitVec.subtract(offsetAmount, 0, 0)
        case EnumFacing.EAST if offsetPositiveSides => res.hitVec.addVector(offsetAmount, 0, 0)
        case _ => res.hitVec
      }
      new RayTraceResult(offsetVec, res.sideHit, res.getBlockPos)
    } else res
  }

  sealed trait BlockFilterRule {
    def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean
    def &&(other: BlockFilterRule): BlockFilterRule = CombinedAnd(Set(this, other))
    def ||(other: BlockFilterRule): BlockFilterRule = CombinedOr(Set(this, other))
  }
  case object Replaceable extends BlockFilterRule {
    override def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean = block.isReplaceable(world, pos)
  }
  case object Passable extends BlockFilterRule {
    override def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean = block.isPassable(world, pos)
  }
  case object Air extends BlockFilterRule {
    override def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean = block.isAir(world.getBlockState(pos), world, pos)
  }
  case object None extends BlockFilterRule {
    override def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean = false
  }
  case class Whitelist(list: Set[Block]) extends BlockFilterRule {
    override def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean = list.contains(block)
  }
  case class Blacklist(list: Set[Block]) extends BlockFilterRule {
    override def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean = !list.contains(block)
  }
  case class CombinedAnd(rules: Set[BlockFilterRule]) extends BlockFilterRule {
    override def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean = rules.forall(r => r.canPass(block, world, pos))
  }
  case class CombinedOr(rules: Set[BlockFilterRule]) extends BlockFilterRule {
    override def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean = {
      rules.foreach(r => if (r.canPass(block, world, pos)) return true)
      false
    }
  }

}
