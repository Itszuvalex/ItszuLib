package com.itszuvalex.itszulib.util

import net.minecraft.util.ResourceLocation

class ResourceMapper(val root: String) {

  def Sound(loc: String): ResourceLocation = Mod(loc)

  def Mod(loc: String) = new ResourceLocation(root, loc)

  def TexBlock(name: String): ResourceLocation = Texture("blocks/" + name)

  def TexGui(name: String): ResourceLocation = Texture("guis/" + name)

  def TexItem(name: String): ResourceLocation = Texture("items/" + name)

  def Texture(name: String): ResourceLocation = Mod("textures/" + name)

  def Particle(name: String): ResourceLocation = Texture("particles/" + name)

  def CustomModelBlock(name: String): ResourceLocation = Mod("block/" + name)

  def CustomModelBlockTex(name: String): ResourceLocation = Mod("models/block/" + name)

  def ModelItem(name: String): ResourceLocation = Mod("item/" + name)
}
