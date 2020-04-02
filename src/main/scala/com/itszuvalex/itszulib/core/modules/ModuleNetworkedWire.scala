package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.wrappers.{IBlock, IItemStack, ITileEntity, IWorld}
import com.itszuvalex.itszulib.logistics.{INetworkNode, TileNetwork}
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

abstract class ModuleNetworkedWire[T <: ModuleSidedBlockableConnectable[T] with INetworkNode[T, N], N <: TileNetwork[T, N]](tile: ITileEntity, tnfact: () => N) extends ModuleSidedBlockableConnectable[T] with INetworkNode[T, N] {
  var network: N = null.asInstanceOf[N]

  override def setNetwork(network: N): Unit = this.network = network

  override def getNetwork: N = network

  override def getLoc: Loc4 = new Loc4(tile)

  override def canConnect(loc: Loc4): Boolean = getLoc.isNeighbor(loc) && !EnumFacing.VALUES.view.filter(getLoc.getOffset(_) == loc).exists(isBlocked)

  override def refresh(): Unit = {}

  override def canAdd(iNetwork: N): Boolean = true

  override def added(iNetwork: N): Unit = {}

  override def removed(iNetwork: N): Unit = {}

  override def connect(node: Loc4, permanent: Boolean): Unit = if (permanent) {
    EnumFacing.VALUES.view.filter(getLoc.getOffset(_) == node).foreach(connect)
    tile.markDirtyForSave()
    tile.setUpdate()
  }

  override def disconnect(node: Loc4, permanent: Boolean): Unit = if (permanent) {
    EnumFacing.VALUES.view.filter(getLoc.getOffset(_) == node).foreach(disconnect)
    tile.markDirtyForSave()
    tile.setUpdate()
  }

  override def onNeighborChanged(world: IWorld, pos: BlockPos, state: IBlockState, changedBlock: IBlock, changedPos: BlockPos): Unit = {
    if (world.isRemote) return
    val loc  = getLoc
    val nloc = new Loc4(world, changedPos)
    EnumFacing.VALUES.withFilter(loc.getOffset(_) == nloc).foreach(checkFacingForConnection)
  }

  protected def checkFacingForConnection(f: EnumFacing): Unit = {
    val loc  = getLoc
    val floc = loc.getOffset(f)
    if (isConnected(f)) {
      floc.getITileEntity(true) match {
        case Some(a: ITileEntity) if shouldConnect(a) =>
        case _ => disconnect(floc, true)
      }
    }
    else {
      floc.getITileEntity(true) match {
        case Some(a: ITileEntity) if shouldConnect(a) =>
          connect(floc, true)
        case _ =>
      }
    }
  }

  protected def shouldConnect(a: ITileEntity): Boolean

  protected def getNetworkNode: T = this.asInstanceOf[T]

  protected def getNetworkNodeFromITE(ite: ITileEntity): T

  override def onBlockPlacedBy(iworld: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, istack: IItemStack): Unit = {
    if (iworld.isRemote) return

    val network = tnfact()
    network.addNode(getNetworkNode)
    network.register()

    EnumFacing.VALUES.foreach(checkFacingForConnection)
  }

  override def onBlockBreak(core: ITileEntity, state: IBlockState): Unit = {
    if (core.getIWorld.isRemote) return

    EnumFacing.VALUES.withFilter(isConnected).foreach { f =>
      new Loc4(core).getOffset(f).getITileEntity(true) match {
        case Some(a: ITileEntity) if shouldConnect(a) =>
          getNetworkNodeFromITE(a) match {
            case null =>
            case x => x.disconnect(new Loc4(core), true)
          }
        case _ =>
      }
    }
    // network.removeNode(getNetworkNode) // Unnecessary, as removing all connections to this node should also remove the node from the Network
  }

  override def onLoad(tile: ITileEntity): Unit = {
    if (tile.getIWorld.isRemote) return

    EnumFacing.VALUES.withFilter(isConnected).foreach { f =>
      val loc = new Loc4(tile).getOffset(f)
      loc.getITileEntity(false) match {
        case Some(i: ITileEntity) if shouldConnect(i) =>
          getNetworkNodeFromITE(i) match {
            case null =>
            case x => if (x.getNetwork != null) {
              if (getNetwork == null)
                x.getNetwork.addNode(getNetworkNode)
              else
                x.getNetwork.addConnectionNodes(x, getNetworkNode)
            }
          }
        case _ =>
      }
    }

    if (network == null) {
      val network = tnfact()
      network.addNode(getNetworkNode)
      network.register()
    }
  }

  override def invalidate(tile: ITileEntity): Unit = {
    if (tile.getIWorld.isRemote) return
    network.removeNode(getNetworkNode)
  }

  override def onChunkUnload(tile: ITileEntity): Unit = {
    if (tile.getIWorld.isRemote) return
    network.removeNode(getNetworkNode)
  }
}

