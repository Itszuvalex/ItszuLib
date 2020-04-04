package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.wrappers.{IBlock, IItemStack, ITileEntity, IWorld}
import com.itszuvalex.itszulib.logistics.{IPersistedConnectableNetworkNode, TileNetwork}
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

abstract class ModuleNetworkedWire[T <: IPersistedConnectableNetworkNode[T,N], N <: TileNetwork[T, N]](tile: ITileEntity, tnfact: () => N) extends ModuleSidedBlockableConnectable[T] with IPersistedConnectableNetworkNode[T, N] {
  var network: N = null.asInstanceOf[N]

  override def setNetwork(network: N): Unit = this.network = network

  override def getNetwork: N = network

  override def getLoc: Loc4 = Loc4(tile)

  override def canConnect(loc: Loc4): Boolean = getLoc.isNeighbor(loc) && !EnumFacing.VALUES.view.filter(getLoc.getOffset(_) == loc).exists(isBlocked)

  override def refresh(): Unit = {}

  override def canAdd(iNetwork: N): Boolean = true

  override def onAdded(iNetwork: N): Unit = {}

  override def onRemoved(iNetwork: N): Unit = {}

  override def onConnect(node: Loc4): Unit = {}

  override def onDisconnect(node: Loc4): Unit = {}

  override def addPersistedConnection(node: Loc4): Unit = {
    val facing = EnumFacing.VALUES.view.filter(getLoc.getOffset(_) == node).head
    if (isConnected(facing)) return

    connect(facing)

    tile.markDirtyForSave()
    tile.setUpdate()
    node.getITileEntity(false) match {
      case None =>
      case Some(x) =>
        getNetworkNodeFromITE(x, facing.getOpposite) match {
          case None =>
          case Some(x) =>
            val network = if (x.getNetwork != null) x.getNetwork else getNetwork
            network.addConnection(getLoc, node)
            x.addPersistedConnection(getLoc)
        }
    }
  }

  override def removePersistedConnection(node: Loc4): Unit = {
    val facing = EnumFacing.VALUES.view.filter(getLoc.getOffset(_) == node).head
    if (!isConnected(facing)) return

    disconnect(facing)

    tile.markDirtyForSave()
    tile.setUpdate()

    node.getITileEntity() match {
      case None =>
      case Some(x) =>
        getNetworkNodeFromITE(x, facing.getOpposite) match {
          case None =>
          case Some(n) =>
            network.removeConnection(getLoc, node)
        }
    }
  }


  override def onNeighborChanged(world: IWorld, pos: BlockPos, state: IBlockState, changedBlock: IBlock, changedPos: BlockPos): Unit = {
    if (world.isRemote) return
    val loc  = getLoc
    val nloc = Loc4(world, changedPos)
    EnumFacing.VALUES.withFilter(loc.getOffset(_) == nloc).foreach(checkFacingForConnection)
  }

  protected def checkFacingForConnection(f: EnumFacing): Unit = {
    val loc  = getLoc
    val floc = loc.getOffset(f)
    if (isConnected(f)) {
      floc.getITileEntity(true) match {
        case Some(a: ITileEntity) if shouldConnect(a, f.getOpposite) =>
        case _ => removePersistedConnection(floc)
      }
    }
    else {
      floc.getITileEntity(true) match {
        case Some(a: ITileEntity) if shouldConnect(a, f.getOpposite) =>
          addPersistedConnection(floc)
        case _ =>
      }
    }
  }

  protected def shouldConnect(a: ITileEntity, f: EnumFacing): Boolean

  protected def getNetworkNode: T = this.asInstanceOf[T]

  protected def getNetworkNodeFromITE(ite: ITileEntity, f: EnumFacing): Option[T]

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
      Loc4(core).getOffset(f).getITileEntity(true) match {
        case Some(a: ITileEntity) if shouldConnect(a, f.getOpposite) =>
          getNetworkNodeFromITE(a, f.getOpposite) match {
            case None =>
            case Some(x) => x.removePersistedConnection(Loc4(core))
          }
        case _ =>
      }
    }
    // network.removeNode(getNetworkNode) // Unnecessary, as removing all connections to this node should also remove the node from the Network
  }

  override def onLoad(tile: ITileEntity): Unit = {
    if (tile.getIWorld.isRemote) return

    EnumFacing.VALUES.withFilter(isConnected).foreach { f =>
      val loc = Loc4(tile).getOffset(f)
      loc.getITileEntity(false) match {
        case None =>
        case Some(i: ITileEntity) if shouldConnect(i, f.getOpposite) =>
          getNetworkNodeFromITE(i, f.getOpposite) match {
            case None =>
            case Some(x) => if (x.getNetwork != null) {
              if (getNetwork == null)
                x.getNetwork.addNode(getNetworkNode)
              else
                x.getNetwork.addConnectionNodes(getNetworkNode, x)
            }
          }
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

