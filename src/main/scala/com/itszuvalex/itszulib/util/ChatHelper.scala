package com.itszuvalex.itszulib.util

import com.mojang.realmsclient.gui.ChatFormatting

object ChatHelper {
  /*
  BLACK('0'),
  DARK_BLUE('1'),
  DARK_GREEN('2'),
  DARK_AQUA('3'),
  DARK_RED('4'),
  DARK_PURPLE('5'),
  GOLD('6'),
  GRAY('7'),
  DARK_GRAY('8'),
  BLUE('9'),
  GREEN('a'),
  AQUA('b'),
  RED('c'),
  LIGHT_PURPLE('d'),
  YELLOW('e'),
  WHITE('f'),
  OBFUSCATED('k', true),
  BOLD('l', true),
  STRIKETHROUGH('m', true),
  UNDERLINE('n', true),
  ITALIC('o', true),
  */

  def black(str: String): String = wrap(str, ChatFormatting.BLACK)

  def darkblue(str: String): String = wrap(str, ChatFormatting.DARK_BLUE)

  def wrap(str: String, chatFormatting: ChatFormatting): String = s"$chatFormatting$str${ChatFormatting.RESET}"

  def darkgreen(str: String): String = wrap(str, ChatFormatting.DARK_GREEN)

  def darkaqua(str: String): String = wrap(str, ChatFormatting.DARK_AQUA)

  def darkred(str: String): String = wrap(str, ChatFormatting.DARK_RED)

  def darkpurple(str: String): String = wrap(str, ChatFormatting.DARK_PURPLE)

  def gold(str: String): String = wrap(str, ChatFormatting.GOLD)

  def gray(str: String): String = wrap(str, ChatFormatting.GRAY)

  def darkgray(str: String): String = wrap(str, ChatFormatting.DARK_GRAY)

  def blue(str: String): String = wrap(str, ChatFormatting.BLUE)

  def green(str: String): String = wrap(str, ChatFormatting.GREEN)

  def aqua(str: String): String = wrap(str, ChatFormatting.AQUA)

  def red(str: String): String = wrap(str, ChatFormatting.RED)

  def lightpurple(str: String): String = wrap(str, ChatFormatting.LIGHT_PURPLE)

  def yellow(str: String): String = wrap(str, ChatFormatting.YELLOW)

  def white(str: String): String = wrap(str, ChatFormatting.WHITE)

  def obfuscated(str: String): String = wrap(str, ChatFormatting.OBFUSCATED)

  def bold(str: String): String = wrap(str, ChatFormatting.BOLD)

  def strikethrough(str: String): String = wrap(str, ChatFormatting.STRIKETHROUGH)

  def underline(str: String): String = wrap(str, ChatFormatting.UNDERLINE)

  def italic(str: String): String = wrap(str, ChatFormatting.ITALIC)

  // Reset will be added by innermost wrap
  def wrap(str: String, chatFormatting: List[ChatFormatting]): String = chatFormatting match {
    case Nil => str
    case head :: Nil => wrap(str, head)
    case head :: rest => s"$head${wrap(str, rest)}"
  }

}
