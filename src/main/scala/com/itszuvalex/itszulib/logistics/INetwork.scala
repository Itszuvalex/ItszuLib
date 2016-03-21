package com.itszuvalex.itszulib.logistics

import java.util

import com.itszuvalex.itszulib.api.core.Loc4

/**
  * Created by Christopher on 4/5/2015.
  */

trait INetwork[C <: INetworkNode[N], N <: INetwork[C, N]] {

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
  def create(nodes: util.Collection[INetworkNode[N]], edges: util.Set[(Loc4, Loc4)]): N

  /**
    *
    * @return All nodes in this network.
    */
  def getNodes: util.Collection[INetworkNode[N]]

  /**
    * Helper function for getting connections in an easy to parse manner.
    * Connections, due to how its formatted.
    *
    * @return All connections, mapped by location.
    */
  def getConnections: util.Map[Loc4, util.Set[Loc4]]

  /**
    * Helper function for getting edges in an easy to parse manner.
    *
    * @return Tuple of all edge pairs.
    */
  def getEdges: util.Set[(Loc4, Loc4)]

  def canConnect(a: Loc4, b: Loc4): Boolean

  def addConnection(a: Loc4, b: Loc4): Unit

  def removeConnection(a: Loc4, b: Loc4): Unit

  /**
    *
    * Adds a node to the network.  Informs the node of its being added.  Informs all other nodes that this node is added.
    *
    * @param node Node to add.
    */
  def addNode(node: INetworkNode[N]): Unit

  /**
    *
    * @param node Node to be added.
    * @return true if this node can be added to the network.
    */
  def canAddNode(node: INetworkNode[N]): Boolean

  /**
    * Removes a node from the network.  Informs the node of its being removed.  Informs all other nodes that this node is being removed.
    *
    * @param node
    */
  def removeNode(node: INetworkNode[N]): Unit

  /**
    * Removes all nodes in nodes from the network.
    * Use this method for mass-removal, for instance in chunk unloading instances, to prevent creating multiple sub-networks redundantly.
    *
    * @param nodes
    */
  def removeNodes(nodes: util.Collection[INetworkNode[N]]): Unit

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
  def split(edges: util.Set[Loc4]): Unit

  /**
    *
    * Called when a node is added to the network.  Sets ownership of all of its nodes to this one, takes over connections.
    *
    * @param iNetwork Network that this network is taking over.
    */
  def takeover(iNetwork: INetwork[C, N]): Unit

  /**
    * Called on networks by another network, when that network is incorporating this network.
    *
    * @param iNetwork Network that is taking over this network.
    */
  def onTakeover(iNetwork: INetwork[C, N]): Unit

  /**
    * Called on sub networks by a main network, when that network is splitting apart.
    *
    * @param iNetwork Network that will split into this sub network.
    */
  def onSplit(iNetwork: INetwork[C, N]): Unit

}
