package com.itszuvalex.itszulib.logistics

import java.util
import java.util.concurrent.ConcurrentHashMap

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.logistics.TileNetwork.NetworkExplorer

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
              network.getConnections(a.getLoc).getOrElse(Set()).foreach(expandLoc(_, network, explored))
            case _ =>
          }
        case None =>
      }

      explored
    }

  }

}

abstract class TileNetwork[C <: INetworkNode[N], N <: TileNetwork[C, N]](val id: Int) extends INetwork[C, N] {

  val nodeMap = new ConcurrentHashMap[Loc4, INetworkNode[N]]().asScala

  val connectionMap = mutable.HashMap[Loc4, mutable.HashSet[Loc4]]()

  override def canConnect(a: Loc4, b: Loc4): Boolean = (a.getTileEntity().orNull, b.getTileEntity().orNull) match {
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
      nodeMap.remove(a)
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
      workingSet --= nodes.intersect(workingSet)
    }

    /*
    Only split if we need to.
     */
    if (networks.size > 1) {
      //Get here, so we don't rebuild the java collection multiple times
      val edgeTuples = getEdges

      networks.foreach { collect =>
        val nodes = collect.flatMap(_.getTileEntity()).collect { case a: INetworkNode[N] => a.asInstanceOf[INetworkNode[N]] }.asJavaCollection
        val edges = edgeTuples.filter { case (loc1, loc2) => collect.contains(loc1)
                                        /*&& collect.contains(loc2)  Not necessary, as these are fully explored graphs.*/
                                      }.toSet
        val network = create(nodes, edges)
        network.onSplit(this)
        network.register()
                       }
      clear()
      unregister()
    }
  }

  override def removeConnection(a: Loc4, b: Loc4): Unit = {
    removeConnectionSilently(a, b)
    (a.getTileEntity().orNull, b.getTileEntity().orNull) match {
      case (nodeA: INetworkNode[N], nodeB: INetworkNode[N]) =>
        nodeA.disconnect(b)
        nodeB.disconnect(a)
        split(Set(a, b))
      case _ =>
    }
  }

  def getConnections(a: Loc4): Option[mutable.HashSet[Loc4]] =
    connectionMap.synchronized {
                                 connectionMap.get(a)
                               }

  override def addNode(node: INetworkNode[N]): Unit = {
    if (!(canAddNode(node) && node.canAdd(this))) return
    getNodes.filter { a => a.canConnect(node.getLoc) && node.canConnect(a.getLoc) }.foreach(n => addConnection(n.getLoc, node.getLoc))
    addNodeSilently(node)
    node.setNetwork(this.asInstanceOf[N])
    node.added(this)
  }

  override def addConnection(a: Loc4, b: Loc4): Unit = {
    addConnectionSilently(a, b)
    (a.getTileEntity().orNull, b.getTileEntity().orNull) match {
      case (nodeA: INetworkNode[N], nodeB: INetworkNode[N]) =>
        if (nodeA.getNetwork != nodeB.getNetwork) {
          if (nodeA.getNetwork == this) takeover(nodeB.getNetwork)
          else takeover(nodeA.getNetwork)
        }
        nodeA.connect(b)
        nodeB.connect(a)
      case _ =>
    }
  }

  /**
    *
    * Called when a node is added to the network.  Sets ownership of all of its nodes to this one, takes over connections.
    *
    * @param iNetwork Network that this network is taking over.
    */
  override def takeover(iNetwork: INetwork[C, N]): Unit = {
    iNetwork.getNodes.foreach { n => addNodeSilently(n); n.setNetwork(this.asInstanceOf[N]) }
    iNetwork.getEdges.foreach { case (loc1, loc2) => addConnectionSilently(loc1, loc2) }
    iNetwork.clear()
    iNetwork.unregister()
  }

  protected def addConnectionSilently(a: Loc4, b: Loc4): Unit =
    connectionMap.synchronized {
                                 connectionMap.getOrElseUpdate(a, mutable.HashSet[Loc4]()) += b
                                 connectionMap.getOrElseUpdate(b, mutable.HashSet[Loc4]()) += a

                               }

  def addNodeSilently(node: INetworkNode[N]): Unit = {
    nodeMap(node.getLoc) = node
  }

  override def canAddNode(node: INetworkNode[N]): Boolean = true

  override def ID = id

  override def size = nodeMap.size

  override def clear(): Unit = {
    nodeMap.clear()
    connectionMap.clear()
  }

  override def refresh(): Unit = {
    getNodes.foreach(_.refresh())
  }

  override def getNodes = nodeMap.values.asJavaCollection

  override def removeNode(node: INetworkNode[N]) = removeNodes(List(node))

  override def register(): Unit = ManagerNetwork.addNetwork(this)

  override def unregister(): Unit = ManagerNetwork.removeNetwork(this)

  /**
    *
    * @param nodes Nodes to make a new network out of
    * @param edges Edges to include in the network.
    * @return Create a new network of this type from the given collection of nodes.
    */
  override def create(nodes: util.Collection[INetworkNode[N]], edges: util.Set[(Loc4, Loc4)]): N = {
    val t = create()
    nodes.foreach(n => {t.addNodeSilently(n); n.setNetwork(t)})
    edges.foreach(a => t.addConnectionSilently(a._1, a._2))
    t
  }

  /**
    * Helper function for getting edges in an easy to parse manner.
    *
    * @return Tuple of all edge pairs.
    */
  override def getEdges: util.Set[(Loc4, Loc4)] = {
                                                    for {
                                                      pairs <- getConnections.toIterable
                                                      con <- pairs._2
                                                      if pairs._1.compareTo(con) < 0

                                                    } yield (pairs._1, con)
                                                  }.toSet.asJava

  protected def removeConnectionSilently(a: Loc4, b: Loc4): Unit =
    connectionMap.synchronized {
                                 val setA = connectionMap.getOrElse(a, return)
                                 setA -= b
                                 if (setA.isEmpty) connectionMap.remove(a)
                                 val setB = connectionMap.getOrElse(b, return)
                                 setB -= a
                                 if (setB.isEmpty) connectionMap.remove(b)
                               }
}
