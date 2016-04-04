
package com.itszuvalex.itszulib.command

import java.util

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.util.PlayerUtils
import net.minecraft.command.{ICommand, ICommandSender, WrongUsageException}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{BlockPos, EnumChatFormatting}
import org.apache.logging.log4j.Level

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

abstract class CommandBase(val name: String, val aliases: ArrayBuffer[String]) extends ICommand {

  var subcmds = new mutable.HashMap[String, ICommand]

  def this(name: String) = this(name, ArrayBuffer[String](name))

  def addSubCommand(subcommand: ICommand): Boolean = {
    subcmds.put(subcommand.getCommandName, subcommand)
    true
  }

  override def compareTo(o: ICommand): Int = getCommandName.compareTo(o.getCommandName)

  override def getCommandName = name

  override def getCommandAliases: java.util.List[String] = aliases

  override def processCommand(icommandsender: ICommandSender, astring: Array[String]) {
    if (icommandsender.getEntityWorld.isRemote) {
      ItszuLib.logger.log(Level.WARN, "Not processing commands Client-side")
      return
    }
    if (astring.length > 0) {
      getSubCommand(astring(0)) match {
        case Some(sub) =>
          sub.processCommand(icommandsender, util.Arrays.copyOfRange(astring, 1, astring.length))
          return
        case None =>
      }
    }
    throw new WrongUsageException(getCommandUsage(icommandsender))
  }

  override def getCommandUsage(sender: ICommandSender): String = {
    sender match {
      case player: EntityPlayer =>
        val output = new StringBuilder
        output.append(EnumChatFormatting.YELLOW)
        output.append(getCommandName)
        PlayerUtils.sendMessageToPlayer(player, getModName, output.toString)
        output.setLength(0)
        output.append(EnumChatFormatting.BOLD).append("aliases").append(EnumChatFormatting.RESET)
        PlayerUtils.sendMessageToPlayer(player, getModName, output.toString)
        output.setLength(0)
        aliases.foreach(alias => PlayerUtils.sendMessageToPlayer(player, getModName, EnumChatFormatting.YELLOW + alias))
        output.append(EnumChatFormatting.BOLD).append("subcommands").append(EnumChatFormatting.RESET)
        PlayerUtils.sendMessageToPlayer(player, getModName, output.toString)
        output.setLength(0)
        subcmds.keySet.foreach { subcommand =>
          output.append(EnumChatFormatting.YELLOW)
          val com = getSubCommand(subcommand).get
          output.append(EnumChatFormatting.RED).append(subcommand).append(EnumChatFormatting.YELLOW)
          com.getCommandAliases.foreach { alias =>
            output.append(EnumChatFormatting.BLUE).append("|").append(EnumChatFormatting.GRAY)
            output.append(alias)
                                        }
          com match {
            case base: CommandBase => output.append(EnumChatFormatting.WHITE).append(" - ").append(base.getDescription).append(EnumChatFormatting.YELLOW)
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

  override def canCommandSenderUseCommand(icommandsender: ICommandSender) = true

  override def addTabCompletionOptions(sender: ICommandSender, args: Array[String], pos: BlockPos): util.List[String] = {
    if (args.length > 0) {
      getSubCommand(args(0)) match {
        case Some(sub) =>
          return sub.addTabCompletionOptions(sender, util.Arrays.copyOfRange(args, 1, args.length), pos)
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
          subc.getCommandAliases.foreach { alias =>
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
