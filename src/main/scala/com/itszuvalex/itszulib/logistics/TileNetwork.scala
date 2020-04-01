package com.itszuvalex.itszulib.logistics

import java.util

import com.itszuvalex.itszulib.api.core.{IModule, Loc4}
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.logistics.INetwork.{Edge, NetworkExplorer}
import com.itszuvalex.itszulib.util.Debug

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection._

/**
  * Created by Christopher Harris (Itszuvalex) on 4/5/15.
  */
abstract class TileNetwork[C <: TileNetworkNode[C, N], N <: TileNetwork[C, N]](val id: Int) extends INetwork[C, N] {

  val nodeMap = new mutable.HashMap[Loc4, C]()

  val connectionMap: mutable.Map[C, mutable.HashSet[C]] = mutable.HashMap[C, mutable.HashSet[C]]()

  def networkModule: IModule[C]

  def canConnectLocs(a: Loc4, b: Loc4): Boolean = (a.getITileEntity().orNull, b.getITileEntity().orNull) match {
    case (null, _) => false
    case (_, null) => false
    case (nodeA: ITileEntity, nodeB: ITileEntity)
      if nodeA.hasModule(networkModule, null) && nodeB.hasModule(networkModule, null) =>
      canConnect(nodeA.getModule(networkModule, null), nodeB.getModule(networkModule, null))
    case _ => false
  }

  override def getConnections: util.Map[C, util.Set[C]] = connectionMap.synchronized {
    connectionMap.map { case (k, v) => k -> v.asJava }.asJava
  }

  /**
    * Removes all nodes in nodes from the network.
    * Use this method for mass-removal, for instance in chunk unloading instances, to prevent creating multiple sub-networks redundantly.
    *
    * @param nodes
    */
  override def removeNodes(nodes: util.Collection[C]): Unit = {
    //Map nodes to locations
    //Find all edges.  These are the set of locations that are connected to nodeLocs, that aren't nodeLocs themselves.
    val edges = nodes.flatMap(a => getConnections(a)).flatten.toSet -- nodes
    //Removal all edges that touch nodeLocs.
    nodeMap.synchronized {
      nodes.foreach { a =>
        (Set[C]() ++ getConnections(a).getOrElse(Set[C]())).foreach(removeConnectionBatch(a, _))
        nodeMap.remove(a.getLoc)
      }

      Debug.only {
        nodes.foreach(a => Debug.assert(!nodeMap.contains(a), "NodeMap should not have any of the nodes that have been removed."))
        nodes.foreach(a => Debug.assert(!connectionMap.contains(a), "ConnectionMap should not have any of the nodes that have been removed."))
        nodes.foreach(a => connectionMap.values.foreach(b => Debug.assert(!b.contains(a), "No loc in ConnectionMap should point to a node that has been removed.")))
      }
    }

    split(edges)

    if (size == 0) {
      clear()
      unregister()
    }
  }

  /**
    *
    * Called when a node is removed from the network.  Maps all out all sub-networks created by the split, creates and registers them, and informs nodes.
    *
    * @param edges All nodes that were connected to all nodes that were removed.
    */
  override def split(edges: util.Set[C]): Unit = {
    val workingSet = mutable.HashSet() ++= edges
    val networks   = mutable.ArrayBuffer[util.Collection[C]]()
    while (workingSet.nonEmpty) {
      val first = workingSet.head
      val nodes = NetworkExplorer.explore[C, N](first, this)
      networks += nodes
      workingSet --= nodes.intersect(workingSet)
    }

    /*
    Only split if we need to.
     */
    if (networks.size > 1) {
      //Get here, so we don't rebuild the java collection multiple times
      val edgeTuples = getEdges

      networks.foreach { nodes =>
        //val nodes   = collect.flatMap(_.getITileEntity()).withFilter(_.hasCapability(networkCapability, null)).map(_.getCapability(networkCapability, null)).asJavaCollection
        val edges   = edgeTuples.filter { e => nodes.contains(e.a)
          /*&& collect.contains(loc2)  Not necessary, as these are fully explored graphs.*/
        }.toSet
        val network = create(nodes, edges)
        network.onSplit(this.asInstanceOf[N])
        network.register()
      }
      clear()
      unregister()
    }
  }

  def removeConnectionBatch(a: C, b: C): Unit = {
    removeConnectionSilently(a, b)

    a.disconnect(b)
    b.disconnect(a)
  }

  def removeConnectionLocs(a: Loc4, b: Loc4): Unit = {
    (a.getITileEntity().flatMap(_.moduleOption(networkModule, null)), b.getITileEntity().flatMap(_.moduleOption(networkModule, null))) match {
      case (_, None) =>
      case (None, _) =>
      case (Some(a), Some(b)) =>
        removeConnectionBatch(a, b)
        split(Set(a, b))
    }
  }

  def getConnections(a: C): Option[mutable.HashSet[C]] =
    connectionMap.synchronized {
      connectionMap.get(a)
    }

  override def addNode(node: C): Unit = {
    if (!(canAddNode(node) && node.canAdd(this.asInstanceOf[N]))) return
    addNodeSilently(node)
    node.setNetwork(this.asInstanceOf[N])
    node.added(this.asInstanceOf[N])
    getNodes.withFilter { a => a.canConnect(node) && node.canConnect(a) }.foreach(n => addConnectionLocs(n.getLoc, node.getLoc))
  }

  def addConnectionLocs(a: Loc4, b: Loc4): Unit = {
    (a.getITileEntity().orNull, b.getITileEntity().orNull) match {
      case (null, _) =>
      case (_, null) =>
      case (nodeA: ITileEntity, nodeB: ITileEntity) if nodeA.hasModule(networkModule, null) && nodeB.hasModule(networkModule, null) =>
        val aCap = nodeA.getModule(networkModule, null)
        val bCap = nodeB.getModule(networkModule, null)

        addConnection(aCap, bCap)
      case _ =>
    }
  }

  override def addConnection(a: C, b: C): Unit = {
    addConnectionSilently(a, b)
    addConnectionInternal(a, b)
  }

  /**
    *
    * Called when a node is added to the network.  Sets ownership of all of its nodes to this one, takes over connections.
    *
    * @param iNetwork Network that this network is taking over.
    */
  override def takeover(iNetwork: N): Unit = {
    iNetwork.getNodes.foreach { n => addNodeSilently(n); n.setNetwork(this.asInstanceOf[N]) }
    iNetwork.getEdges.foreach { e => addConnectionSilently(e.a, e.b) }
    iNetwork.clear()
    iNetwork.unregister()
  }

  protected def addConnectionSilently(a: C, b: C): Unit =
    connectionMap.synchronized {
      connectionMap.getOrElseUpdate(a, mutable.HashSet[C]()) += b
      connectionMap.getOrElseUpdate(b, mutable.HashSet[C]()) += a
    }

  def addNodeSilently(node: C): Unit = {
    nodeMap(node.getLoc) = node
  }

  override def canAddNode(node: C): Boolean = true

  override def ID: Int = id

  override def size: Int = nodeMap.size

  override def clear(): Unit = {
    nodeMap.clear()
    connectionMap.clear()
  }

  override def refresh(): Unit = {
    getNodes.foreach(_.refresh())
  }

  override def getNodes: util.Collection[C] = nodeMap.values.asJavaCollection

  override def removeNode(node: C): Unit = removeNodes(List(node))

  override def register(): Unit = ManagerNetwork.instance.addNetwork(this)

  override def unregister(): Unit = ManagerNetwork.instance.removeNetwork(this)

  /**
    *
    * @param nodes Nodes to make a new network out of
    * @param edges Edges to include in the network.
    * @return Create a new network of this type from the given collection of nodes.
    */
  override def create(nodes: util.Collection[C], edges: util.Set[Edge[C, N]]): N = {
    val t = create()
    nodes.foreach(n => {t.addNodeSilently(n); n.setNetwork(t)})
    edges.foreach(e => t.addConnectionSilently(e.a, e.b))
    t
  }

  /**
    * Helper function for getting edges in an easy to parse manner.
    *
    * @return Tuple of all edge pairs.
    */
  override def getEdges: util.Set[Edge[C, N]] = {
    for {
      pairs <- getConnections.toIterable
      con <- pairs._2
      if pairs._1.getLoc.compareTo(con.getLoc) < 0

    } yield Edge[C, N](pairs._1, con)
  }.toSet.asJava

  protected def removeConnectionSilently(a: C, b: C): Unit =
    connectionMap.synchronized {
      val setA = connectionMap.getOrElse(a, return)
      setA -= b
      if (setA.isEmpty) connectionMap.remove(a)
      val setB = connectionMap.getOrElse(b, return)
      setB -= a
      if (setB.isEmpty) connectionMap.remove(b)
    }

  override def canConnect(a: C, b: C): Boolean = a.canConnect(b) && b.canConnect(a)

  private def addConnectionInternal(a: C, b: C): Unit = {
    if (a.getNetwork != b.getNetwork) {
      if (a.getNetwork == this) takeover(b.getNetwork)
      else takeover(a.getNetwork)
    }
    a.connect(b)
    b.connect(a)
  }

  override def removeConnection(a: C, b: C): Unit = {
    removeConnectionSilently(a, b)
    a.disconnect(b)
    b.disconnect(a)
    split(Set(a, b))
  }

}
