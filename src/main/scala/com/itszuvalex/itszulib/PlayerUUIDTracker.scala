package com.itszuvalex.itszulib

import java.io.File
import java.util.UUID

import com.itszuvalex.itszulib.xml.XMLLoaderWriter
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent

import scala.collection.mutable

/**
  * Created by Christopher Harris (Itszuvalex) on 3/29/15.
  */
object PlayerUUIDTracker {
  val UUIDToUsername = new mutable.HashMap[UUID, String]()
  val UsernameToUUID = new mutable.HashMap[String, UUID]()
  private var xml: XMLLoaderWriter = null

  def init(): Unit = {
    FMLCommonHandler.instance().bus().register(this)
  }

  def setFile(file: File): Unit = {
    xml = new XMLLoaderWriter(file)
    load()
  }

  def load(): Unit = {
    UUIDToUsername.clear()
    xml.load()
    (xml.xml \ "Mapping").foreach(node => try addMapping(UUID.fromString(node \@ "uuid"), node \@ "username", doSave = false) catch {case _: Throwable =>})
  }

  def addMapping(uuid: UUID, username: String, doSave: Boolean = true): Unit = {
    if (UUIDToUsername.get(uuid).orNull != username) {
      UUIDToUsername(uuid) = username
      UsernameToUUID(username) = uuid
      if (doSave) save()
    }
  }

  def save(): Unit = {
    xml.xml = <xml>
      {for (mapping <- UUIDToUsername) yield <Mapping uuid={mapping._1.toString} username={mapping._2}/>}
    </xml>
    xml.save()
  }

  def getUsername(uuid: UUID): String = UUIDToUsername.getOrElse(uuid, "")

  def getUUID(string: String): UUID = UsernameToUUID.getOrElse(string, null)

  @SubscribeEvent
  def onPlayerLogin(event: PlayerLoggedInEvent): Unit = {
    addMapping(event.player.getUniqueID, event.player.getName)
  }
}
