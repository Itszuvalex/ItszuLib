package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.wrappers.{Converter, IBlock, IWorld}
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos

import scala.collection.mutable

/**
  *
  * @param blocks Map of BlockPos to IBlocks.  BlockPos should be offsets from (0,0,0), where (0,0,0) would be the Multiblock Controller block
  */
class BlockPatternStatic(blocks: Map[BlockPos, IBlock], var rot: Rotation = Rotation.NONE) extends IBlockPattern {
  val blockMap = new mutable.HashMap[BlockPos, IBlock]()
  blockMap ++= blocks

  override def rotation: Rotation = rot

  override def canRotate: Boolean = true

  override def rotate(r: Rotation): Unit = {
    val contents = blockMap.toBuffer
    blockMap.clear()
    blockMap ++= contents.map(a => (a._1.rotate(r), a._2))
    rot = rot.add(r)
  }

  override def matches(world: IWorld, pos: BlockPos): Boolean = {
    blockMap.forall { pair =>
      world.getBlockState(pair._1.add(pos)).getBlock == Converter.BlockFromIBlock(pair._2)
    }
  }

  override def blocksInMatch(world: IWorld, pos: BlockPos): Iterable[BlockPos] = {
    blockMap.keys.map(_.add(pos))
  }

  override def copy(): IBlockPattern = new BlockPatternStatic(blockMap.toMap, rot)
}
