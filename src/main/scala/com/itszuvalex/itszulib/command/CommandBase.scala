
package com.itszuvalex.itszulib.command

import java.util

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.util.PlayerUtils
import net.minecraft.command.{ICommand, ICommandSender, WrongUsageException}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import org.apache.logging.log4j.Level

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

abstract class CommandBase(val name: String, val aliases: ArrayBuffer[String]) extends ICommand {

  var subcmds = new mutable.HashMap[String, ICommand]

  def this(name: String) = this(name, ArrayBuffer[String](name))

  def addSubCommand(subcommand: ICommand): Boolean = {
    subcmds.put(subcommand.getName, subcommand)
    true
  }

  override def compareTo(o: ICommand): Int = getName.compareTo(o.getName)

  override def getName: String = name

  override def getAliases: java.util.List[String] = aliases

  override def execute(server: MinecraftServer, sender: ICommandSender, args: Array[String]): Unit = {
    if (sender.getEntityWorld.isRemote) {
      ItszuLib.logger.log(Level.WARN, "Not processing commands Client-side")
      return
    }
    if (args.length > 0) {
      getSubCommand(args(0)) match {
        case Some(sub) =>
          sub.execute(server, sender, util.Arrays.copyOfRange(args, 1, args.length))
          return
        case None =>
      }
    }
    throw new WrongUsageException(getUsage(sender))
  }

  override def getUsage(sender: ICommandSender): String = {
    sender match {
      case player: EntityPlayer =>
        val output = new StringBuilder
        output.append(TextFormatting.YELLOW)
        output.append(getName)
        PlayerUtils.sendMessageToPlayer(player, getModName, output.toString)
        output.setLength(0)
        output.append(TextFormatting.BOLD).append("aliases").append(TextFormatting.RESET)
        PlayerUtils.sendMessageToPlayer(player, getModName, output.toString)
        output.setLength(0)
        aliases.foreach(alias => PlayerUtils.sendMessageToPlayer(player, getModName, TextFormatting.YELLOW + alias))
        output.append(TextFormatting.BOLD).append("subcommands").append(TextFormatting.RESET)
        PlayerUtils.sendMessageToPlayer(player, getModName, output.toString)
        output.setLength(0)
        subcmds.keySet.foreach { subcommand =>
          output.append(TextFormatting.YELLOW)
          val com = getSubCommand(subcommand).get
          output.append(TextFormatting.RED).append(subcommand).append(TextFormatting.YELLOW)
          com.getAliases.foreach { alias =>
            output.append(TextFormatting.BLUE).append("|").append(TextFormatting.GRAY)
            output.append(alias)
          }
          com match {
            case base: CommandBase => output.append(TextFormatting.WHITE).append(" - ").append(base.getDescription).append(TextFormatting.YELLOW)
            case _ =>
          }
          PlayerUtils.sendMessageToPlayer(player, getModName, output.toString)
          output.setLength(0)
        }
      case _ =>
    }
    ""
  }

  def getModName: String = ItszuLib.ID

  def getDescription = ""

  override def checkPermission(server: MinecraftServer, sender: ICommandSender): Boolean = true

  override def getTabCompletions(server: MinecraftServer, sender: ICommandSender, args: Array[String], pos: BlockPos): util.List[String] = {
    if (args.length > 0) {
      getSubCommand(args(0)) match {
        case Some(sub) =>
          return sub.getTabCompletions(server, sender, util.Arrays.copyOfRange(args, 1, args.length), pos)
        case None =>
      }
    }
    new util.ArrayList[String](subcmds.keySet)
  }

  private def getSubCommand(name: String): Option[ICommand] = {
    subcmds.get(name) match {
      case Some(a) => Some(a)
      case None =>
        subcmds.values.foreach { subc =>
          subc.getAliases.foreach { alias =>
            if (alias.compareToIgnoreCase(name) == 0) {
              return Some(subc)
            }
          }
        }
        None
    }
  }

  override def isUsernameIndex(astring: Array[String], i: Int): Boolean = {
    if (astring.length > 0) {
      getSubCommand(astring(0)) match {
        case Some(sub) =>
          return sub.isUsernameIndex(util.Arrays.copyOfRange(astring, 1, astring.length), i - 1)
        case None =>
      }
    }
    false
  }
}
