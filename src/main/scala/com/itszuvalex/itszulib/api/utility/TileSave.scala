package com.itszuvalex.itszulib.api.utility

import com.itszuvalex.itszulib.api.core.Loc4
import net.minecraft.block.state.IBlockState
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.fml.common.registry.GameRegistry

object TileSave {
  def apply(nBTTagCompound: NBTTagCompound) = loadFromNBT(nBTTagCompound)


  def loadFromNBT(compound: NBTTagCompound): TileSave = {
    new TileSave(compound.getInteger("dimension"),
                 compound.getInteger("posX"),
                 compound.getInteger("posY"),
                 compound.getInteger("posZ"),
                 compound.getString("modID"),
                 compound.getString("blockID"),
                 compound.getInteger("meta"),
                 if (compound.hasKey("nbt")) compound.getCompoundTag("nbt") else null)
  }
}

class TileSave(private var _dimensionID: Int, var pos: BlockPos, var modID: String,
               var blockID: String, var meta: Int, var te: NBTTagCompound) extends INBTSerializable[NBTTagCompound] {

  lazy val block = GameRegistry.findBlock(modID, blockID)

  def this(dim: Int, x: Int, y: Int, z: Int, modID: String, blockID: String, meta: Int, nbt: NBTTagCompound) =
    this(dim, new BlockPos(x, y, z), modID, blockID, meta, nbt)

  def this(dim: Int, x: Int, y: Int, z: Int, modID: String, blockID: String, state: IBlockState, nbt: NBTTagCompound) =
    this(dim, x, y, z, modID, blockID, state.getBlock.getMetaFromState(state), nbt)

  def this(dim: Int, pos: BlockPos, modID: String, blockID: String, state: IBlockState, nbt: NBTTagCompound) =
    this(dim, pos, modID, blockID, state.getBlock.getMetaFromState(state), nbt)

  def world = DimensionManager.getWorld(dimensionID)

  def world_=(world: World) = _dimensionID = world.provider.getDimension

  def this(dimensionID: Int, pos: BlockPos, state: IBlockState, te: NBTTagCompound) =
    this(dimensionID,
         pos,
         state.getBlock.getRegistryName.getResourceDomain,
         state.getBlock.getRegistryName.getResourcePath,
         state,
         te)

  def this(world: World, pos: BlockPos, te: NBTTagCompound) =
    this(world.provider.getDimension,
         pos,
         world.getBlockState(pos),
         te)

  def this(dimensionID: Int, pos: BlockPos, state: IBlockState, te: TileEntity) =
    this(dimensionID, pos, state, if (te != null) {
      val nbt = new NBTTagCompound
      te.writeToNBT(nbt)
      nbt
    } else {
      null
    })

  def this(world: World, pos: BlockPos, state: IBlockState, te: TileEntity) =
    this(world, pos, if (te != null) {
      val nbt = new NBTTagCompound
      te.writeToNBT(nbt)
      nbt
    } else {
      null
    })

  def this(world: World, pos: BlockPos) =
    this(world, pos, world.getBlockState(pos), world.getTileEntity(pos))

  def this(loc: Loc4) = this(loc.getWorld.get, loc.getPos)

  override def serializeNBT(): NBTTagCompound = {
    val compound = new NBTTagCompound
    compound.setInteger("dimension", dimensionID)
    compound.setInteger("posX", pos.getX)
    compound.setInteger("posY", pos.getY)
    compound.setInteger("posZ", pos.getZ)
    compound.setString("modID", modID)
    compound.setString("blockID", blockID)
    compound.setInteger("meta", meta)
    if (te != null) compound.setTag("nbt", te)
    compound
  }

  def dimensionID = _dimensionID

  def dimensionID_=(dim: Int) = _dimensionID = dim

  override def deserializeNBT(compound: NBTTagCompound): Unit = {
    _dimensionID = compound.getInteger("dimension")
    pos = new BlockPos(compound.getInteger("posX"),
                       compound.getInteger("posY"),
                       compound.getInteger("posZ"))
    modID = compound.getString("modID")
    blockID = compound.getString("blockID")
    meta = compound.getInteger("meta")
    te = if (compound.hasKey("nbt")) compound.getCompoundTag("nbt") else null
  }
}
