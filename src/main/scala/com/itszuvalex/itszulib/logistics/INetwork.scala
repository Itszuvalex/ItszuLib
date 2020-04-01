package com.itszuvalex.itszulib.logistics

import java.util

import com.itszuvalex.itszulib.logistics.INetwork.Edge

import scala.collection.immutable.HashSet
import scala.collection.{Set, immutable, mutable}

object INetwork {

  object NetworkExplorer {
    def explore[C <: INetworkNode[C, N], N <: INetwork[C, N]](start: C, network: INetwork[C, N]): HashSet[C] = {
      immutable.HashSet[C]() ++ expandNode(start, network, mutable.HashSet[C]())
    }

    private def expandNode[C <: INetworkNode[C, N], N <: INetwork[C, N]](node: C, network: INetwork[C, N], explored: mutable.HashSet[C]): mutable.HashSet[C] = {
      if (!explored.contains(node)) {
        explored += node
        network.getConnections(node).getOrElse(Set()).foreach(expandNode(_, network, explored))
      }

      explored
    }
  }

  case class Edge[C <: INetworkNode[C, N], N <: INetwork[C, N]](a: C, b: C)

}

/**
  * Created by Christopher on 4/5/2015.
  */

trait INetwork[C <: INetworkNode[C, N], N <: INetwork[C, N]] {

  /**
    *
    * @return Network Identifier.  This should be unique.
    */
  def ID: Int

  /**
    *
    * @return Create an empty new network of this type.
    */
  def create(): N

  /**
    *
    * @param nodes Nodes to make a new network out of
    * @param edges Edges to include in the network.
    * @return Create a new network of this type from the given collection of nodes.
    */
  def create(nodes: util.Collection[C], edges: util.Set[Edge[C, N]]): N

  /**
    *
    * @return All nodes in this network.
    */
  def getNodes: util.Collection[C]

  /**
    * Helper function for getting connections in an easy to parse manner.
    * Connections, due to how its formatted.
    *
    * @return All connections, mapped by location.
    */
  def getConnections: util.Map[C, util.Set[C]]

  def getConnections(node: C): Option[mutable.HashSet[C]]

  /**
    * Helper function for getting edges in an easy to parse manner.
    *
    * @return Tuple of all edge pairs.
    */
  def getEdges: util.Set[Edge[C, N]]

  def canConnect(a: C, b: C): Boolean

  def addConnection(a: C, b: C): Unit

  def removeConnection(a: C, b: C): Unit

  /**
    *
    * Adds a node to the network.  Informs the node of its being added.  Informs all other nodes that this node is added.
    *
    * @param node Node to add.
    */
  def addNode(node: C): Unit

  /**
    *
    * @param node Node to be added.
    * @return true if this node can be added to the network.
    */
  def canAddNode(node: C): Boolean

  /**
    * Removes a node from the network.  Informs the node of its being removed.  Informs all other nodes that this node is being removed.
    *
    * @param node
    */
  def removeNode(node: C): Unit

  /**
    * Removes all nodes in nodes from the network.
    * Use this method for mass-removal, for instance in chunk unloading instances, to prevent creating multiple sub-networks redundantly.
    *
    * @param nodes
    */
  def removeNodes(nodes: util.Collection[C]): Unit

  /**
    * Simply remove all nodes from the network.  Does not inform them.
    */
  def clear(): Unit

  /**
    * Orders all nodes to refresh.
    */
  def refresh(): Unit

  /**
    * Register this network with the Network Manager.  Starts tick updates.
    */
  def register(): Unit

  /**
    * Unregister this network from the Network Manager.  Stops tick updates.
    */
  def unregister(): Unit

  /**
    *
    * @return Number of nodes in the network.
    */
  def size: Int

  /**
    * Called when a tick starts.
    */
  def onTickStart(): Unit

  /**
    * Called when a tick ends.
    */
  def onTickEnd(): Unit

  /**
    *
    * Called when a node is removed from the network.  Maps all out all sub-networks created by the split, creates and registers them, and informs nodes.
    *
    * @param edges All nodes that were connected to all nodes that were removed.
    */
  def split(edges: util.Set[C]): Unit

  /**
    *
    * Called when a node is added to the network.  Sets ownership of all of its nodes to this one, takes over connections.
    *
    * @param iNetwork Network that this network is taking over.
    */
  def takeover(iNetwork: N): Unit

  /**
    * Called on networks by another network, when that network is incorporating this network.
    *
    * @param iNetwork Network that is taking over this network.
    */
  def onTakeover(iNetwork: N): Unit

  /**
    * Called on sub networks by a main network, when that network is splitting apart.
    *
    * @param iNetwork Network that will split into this sub network.
    */
  def onSplit(iNetwork: N): Unit

}
