package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers.IWorld
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.{AxisAlignedBB, BlockPos, RayTraceResult, Vec3d}

import scala.collection.JavaConversions._
import scala.collection.{SeqView, mutable}

abstract class NamedBoundingBoxCollection(var defaultBoundingBox: () => KeyedBoundingBox) {
  var renderBox: KeyedBoundingBox = defaultBoundingBox()

  def boxes(world: IWorld, pos: BlockPos): mutable.Buffer[KeyedBoundingBox]

  def getIntersectingBoxes(world: IWorld, pos: BlockPos, aabb: AxisAlignedBB): Iterable[AxisAlignedBB] = getIntersectingBoxesFull(world, pos, aabb).map(_.box)

  def getIntersectingBoxesFull(world: IWorld, pos: BlockPos, aabb: AxisAlignedBB): Iterable[KeyedBoundingBox] = boxes(world, pos).view.filter(a => aabb.intersects(a.box))

  def toList(world: IWorld, pos: BlockPos): java.util.List[AxisAlignedBB] = boxes(world, pos).map(_.box).toList

  def toListFull(world: IWorld, pos: BlockPos): java.util.List[KeyedBoundingBox] = boxes(world, pos).toList

  def rayTrace(pos: BlockPos, start: Vec3d, end: Vec3d, boundingBox: KeyedBoundingBox): RayTraceResult = {
    val vec3d         : Vec3d          = start.subtract(pos.getX.toDouble, pos.getY.toDouble, pos.getZ.toDouble)
    val vec3d1        : Vec3d          = end.subtract(pos.getX.toDouble, pos.getY.toDouble, pos.getZ.toDouble)
    val raytraceresult: RayTraceResult = boundingBox.box.calculateIntercept(vec3d, vec3d1)
    if (raytraceresult == null) return null
    val ret = new RayTraceResult(raytraceresult.hitVec.addVector(pos.getX.toDouble, pos.getY.toDouble, pos.getZ.toDouble), raytraceresult.sideHit, pos)
    ret.subHit = boundingBox.index
    ret
  }

  def collisionRayTrace(blockState: IBlockState, worldIn: IWorld, pos: BlockPos, start: Vec3d, end: Vec3d): RayTraceResult = {
    val results = rayTraceList(worldIn, pos, start, end)
    results.headOption.map { r =>
      renderBox = r._1
      r._2
    }.orNull
  }

  def collisionRayTraceBoxFull(blockState: IBlockState, worldIn: IWorld, pos: BlockPos, start: Vec3d, end: Vec3d): KeyedBoundingBox = {
    val results = rayTraceList(worldIn, pos, start, end)
    results.headOption.map { r =>
      renderBox = r._1
      renderBox
    }.orNull
  }

  private def rayTraceList(world: IWorld, pos: BlockPos, start: Vec3d, end: Vec3d): SeqView[(KeyedBoundingBox, RayTraceResult), Seq[_]] = {
    val results = boxes(world, pos).view.map(box => (box, rayTrace(pos, start, end, box))).filter(a => a._2 != null).sortWith { (a, b) =>
      start.squareDistanceTo(a._2.hitVec) < start.squareDistanceTo(b._2.hitVec)
    }
    results
  }

  def getSelectedBoundingBox(state: IBlockState, world: IWorld, pos: BlockPos): AxisAlignedBB = {
    renderBox.box.offset(pos)
  }

  def getSelectedBoundingBoxFull(state: IBlockState, world: IWorld, pos: BlockPos): KeyedBoundingBox = {
    renderBox
  }

  def getBoxFromPoint(worldIn: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Option[KeyedBoundingBox] = {
    val hit         = new Vec3d(hitX, hitY, hitZ)
    val playerInPos = playerIn.getPositionVector
    boxes(worldIn, pos).view.withFilter(_.box.grow(1.01).contains(hit)).sortWith { (a, b) =>
      playerInPos.squareDistanceTo(a.box.getCenter) < playerInPos.squareDistanceTo(b.box.getCenter)
    }.headOption
  }
}
