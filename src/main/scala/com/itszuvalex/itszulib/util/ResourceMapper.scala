package com.itszuvalex.itszulib.util

import java.io.File

import net.minecraft.util.ResourceLocation

class ResourceMapper(val root: String) {

  def Sound(loc: String): ResourceLocation = Mod(loc)

  def Mod(loc: String) = new ResourceLocation(root, loc)

  def TexBlock(name: String): ResourceLocation = Texture("blocks" + File.separator + name)

  def TexGui(name: String): ResourceLocation = Texture("guis" + File.separator + name)

  def TexItem(name: String): ResourceLocation = Texture("items" + File.separator + name)

  def Texture(name: String): ResourceLocation = Mod("textures" + File.separator + name)

  def Particle(name: String): ResourceLocation = Texture("particles" + File.separator + name)

  def CustomModelBlock(name: String): ResourceLocation = Mod("block" + File.separator + name)

  def CustomModelBlockTex(name: String): ResourceLocation = Mod("models" + File.separator + "block" + File.separator + name)

  def ModelItem(name: String): ResourceLocation = Mod("item" + File.separator + name)
}
