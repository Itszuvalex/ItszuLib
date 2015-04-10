package com.itszuvalex.itszucore.logistics

import java.util
import java.util.concurrent.ConcurrentHashMap

import com.itszuvalex.itszucore.api.core.Loc4
import com.itszuvalex.itszucore.logistics.TileNetwork._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection._
import scala.collection.immutable.HashSet

/**
 * Created by Christopher Harris (Itszuvalex) on 4/5/15.
 */
object TileNetwork {

  object NetworkExplorer {
    def explore[C <: INetworkNode[N], N <: TileNetwork[C, N]](start: Loc4, network: TileNetwork[C, N]): HashSet[Loc4] = {
      immutable.HashSet[Loc4]() ++ expandLoc(start, network, mutable.HashSet[Loc4]())
    }

    private def expandLoc[C <: INetworkNode[N], N <: TileNetwork[C, N]](node: Loc4, network: TileNetwork[C, N], explored: mutable.HashSet[Loc4]): mutable.HashSet[Loc4] = {
      node.getTileEntity() match {
        case Some(c) =>
          c match {
            case a: INetworkNode[N] if !explored.contains(a.getLoc) =>
              explored += node
              network.getConnections(a.getLoc).getOrElse(return explored).foreach(expandLoc(_, network, explored))
            case _ =>
          }
        case None =>
      }
      explored
    }

  }


}

abstract class TileNetwork[C <: INetworkNode[N], N <: TileNetwork[C, N]]() extends INetwork[C, N] {
  val id = ManagerNetwork.getNextID

  def nodeMap = new ConcurrentHashMap[Loc4, INetworkNode[N]]().asScala

  def connectionMap = mutable.HashMap[Loc4, mutable.HashSet[Loc4]]()

  override def addConnection(a: Loc4, b: Loc4): Unit = {
    addConnectionSilently(a, b)
    (a.getTileEntity().get, b.getTileEntity().get) match {
      case (nodeA: INetworkNode[N], nodeB: INetworkNode[N]) =>
        nodeA.connect(b)
        nodeB.connect(a)
      case _ =>
    }
  }

  private def addConnectionSilently(a: Loc4, b: Loc4): Unit =
    connectionMap.synchronized {
                                 connectionMap.getOrElseUpdate(a, mutable.HashSet[Loc4]()) += b
                                 connectionMap.getOrElseUpdate(b, mutable.HashSet[Loc4]()) += a

                               }


  override def canConnect(a: Loc4, b: Loc4): Boolean = (a.getTileEntity().get, b.getTileEntity().get) match {
    case (nodeA: INetworkNode[N], nodeB: INetworkNode[N]) => nodeA.canConnect(b) && nodeB.canConnect(a)
    case _ => false
  }

  override def getConnections: util.Map[Loc4, util.Set[Loc4]] = connectionMap.map { case (k, v) => k -> v.asJava }.asJava

  /**
   * Removes all nodes in nodes from the network.
   * Use this method for mass-removal, for instance in chunk unloading instances, to prevent creating multiple sub-networks redundantly.
   *
   * @param nodes
   */
  override def removeNodes(nodes: util.Collection[INetworkNode[N]]): Unit = {
    //Map nodes to locations
    val nodeLocs = HashSet() ++ nodes.map(_.getLoc)
    //Find all edges.  These are the set of locations that are connected to nodeLocs, that aren't nodeLocs themselves.
    val edges = nodes.flatMap(a => getConnections(a.getLoc)).flatten.toSet -- nodeLocs
    //Removal all edges that touch nodeLocs.
    nodeLocs.foreach { a =>
      getConnections(a).getOrElse(Set()).foreach(removeConnection(a, _))
                     }
    split(edges)
  }

  /**
   *
   * Called when a node is removed from the network.  Maps all out all sub-networks created by the split, creates and registers them, and informs nodes.
   *
   * @param edges All nodes that were connected to all nodes that were removed.
   */
  override def split(edges: util.Set[Loc4]): Unit = {
    val workingSet = mutable.HashSet() ++= edges
    val networks = mutable.ArrayBuffer[util.Collection[Loc4]]()
    while (workingSet.nonEmpty) {
      val first = workingSet.head
      val nodes = NetworkExplorer.explore[C, N](first, this)
      networks += nodes
      workingSet --= nodes.union(workingSet)
    }

    //Get here, so we don't rebuild the java collection multiple times
    val connections = getConnections

    networks.foreach { collect =>
      val nodes = collect.map(_.getTileEntity().get).collect { case a if a.isInstanceOf[INetworkNode[N]] => a.asInstanceOf[INetworkNode[N]] }.asJavaCollection
      val edges = mutable.HashMap[Loc4, mutable.HashSet[Loc4]]()
      connections.foreach { case (loc, set) if collect.contains(loc) => edges.getOrElseUpdate(loc, mutable.HashSet[Loc4]()) ++= connections.get(loc).filter(collect.contains(_)) }
      val network = create(nodes, edges.map { case (k, v) => k -> v.asJava }.asJava)
      network.onSplit(this)
      network.register()
                     }
    nodeMap.clear()
    connectionMap.clear()
    unregister()
  }

  /**
   *
   * Called when a node is added to the network.  Sets ownership of all of its nodes to this one, takes over connections.
   *
   * @param iNetwork Network that this network is taking over.
   */
  override def merge(iNetwork: INetwork[C, N]): Unit = {

  }

  override def removeConnection(a: Loc4, b: Loc4): Unit = {
    removeConnectionSilently(a, b)
    (a.getTileEntity().get, b.getTileEntity().get) match {
      case (nodeA: INetworkNode[N], nodeB: INetworkNode[N]) =>
        nodeA.disconnect(b)
        nodeB.disconnect(a)
      case _ =>
    }
  }

  private def removeConnectionSilently(a: Loc4, b: Loc4): Unit =
    connectionMap.synchronized {
                                 val setA = connectionMap.getOrElse(a, return)
                                 setA -= b
                                 if (setA.isEmpty) connectionMap.remove(a)
                                 val setB = connectionMap.getOrElse(b, return)
                                 setB -= a
                                 if (setB.isEmpty) connectionMap.remove(b)
                               }


  def getConnections(a: Loc4): Option[mutable.HashSet[Loc4]] =
    connectionMap.synchronized {
                                 connectionMap.get(a)
                               }

  override def addNode(node: INetworkNode[N]): Unit = {
    if (!(canAddNode(node) && node.canAdd(this))) return
    getNodes.filter { a => a.canConnect(node.getLoc) && node.canConnect(a.getLoc) }.foreach(n => addConnection(n.getLoc, node.getLoc))
    addNodeSilently(node)
    node.added(this)
  }

  def addNodeSilently(node: INetworkNode[N]): Unit = {
    nodeMap(node.getLoc) = node
  }

  override def ID = id

  override def size = nodeMap.size

  override def clear(): Unit = {
    nodeMap.clear()
  }

  override def refresh(): Unit = {
    getNodes.foreach(_.refresh())
  }

  override def removeNode(node: INetworkNode[N]) = removeNodes(List(node))

  override def getNodes = nodeMap.values.asJavaCollection

  override def canAddNode(node: INetworkNode[N]): Boolean = true

  override def register() = ManagerNetwork.addNetwork(this)

  override def unregister(): Unit = ManagerNetwork.removeNetwork(this)

  /**
   *
   * @param nodes Nodes to make a new network out of
   * @param edges Edges to include in the network.
   * @return Create a new network of this type from the given collection of nodes.
   */
  override def create(nodes: util.Collection[INetworkNode[N]], edges: util.Map[Loc4, util.Set[Loc4]]): N = {
    val t = create()
    nodes.foreach(n => {t.addNodeSilently(n); n.setNetwork(t)})
    edges.foreach { case (loc, set) => set.foreach(t.addConnectionSilently(loc, _)) }
    t
  }
}
