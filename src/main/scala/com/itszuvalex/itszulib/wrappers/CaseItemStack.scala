package com.itszuvalex.itszulib.wrappers

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.implicits.IDImplicits._
import com.itszuvalex.itszulib.implicits.NBTHelpers.NBTAdditions._
import com.itszuvalex.itszulib.implicits.NBTHelpers.NBTLiterals._
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 4/17/2016.
  */
object CaseItemStack {
  val ITEM_ID_KEY = "id"
  val AMOUNT_KEY  = "amount"
  val DAMAGE_KEY  = "damage"
  val NBT_KEY     = "nbt"

  val nbtSerializer = new Overridable((c: CaseItemStack) => {
    NBTCompound(
                 ITEM_ID_KEY -> c.itemID,
                 AMOUNT_KEY -> c.stackSize,
                 DAMAGE_KEY -> c.damage,
                 NBT_KEY -> c.nbt
               )
  })

  val nbtDeserializer = new Overridable((n: NBTTagCompound) => {
    CaseItemStack(
                   Item.getItemById(n.Int(ITEM_ID_KEY)),
                   n.Int(AMOUNT_KEY),
                   n.Int(DAMAGE_KEY),
                   n.Compound(NBT_KEY)
                 )
  }
                                       )

  val nbtSelfModifyingDeserializer = new Overridable((c: CaseItemStack, n: NBTTagCompound) => {
    c.item = Item.getItemById(n.Int(ITEM_ID_KEY))
    c.stackSize = n.Int(AMOUNT_KEY)
    c.damage = n.Int(DAMAGE_KEY)
    c.nbt = n.Compound(NBT_KEY)
  })

  implicit def fromMinecraft(stack: ItemStack) : CaseItemStack = CaseItemStack(stack.getItem, stack.stackSize, stack.getItemDamage, stack.getTagCompound)
}

case class CaseItemStack(private var _item: Item, private var _amount: Int, private var _damage: Int, private var _nbt: NBTTagCompound) extends IItemStack {
  def this(item: Item, amount: Int, damage: Int) = this(item, amount, damage, null)

  def this(item: Item, amount: Int) = this(item, amount, 0)

  def this(item: Item) = this(item, 0)

  override def itemID: Int = item.itemID

  override def stackSize_=(size: Int): Unit = _amount = size

  override def damage: Int = _damage

  override def damage_=(dam: Int): Unit = _damage = dam

  override def stackSize: Int = _amount

  override def stackSizeMax: Int = item.getMaxDamage(asMinecraft)

  override def nbt_=(nbt: NBTTagCompound): Unit = _nbt = nbt

  override def damageMaximum: Int = item.getMaxDamage(asMinecraft)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = CaseItemStack.nbtSelfModifyingDeserializer.apply(this, nbt)

  override def serializeNBT(): NBTTagCompound = CaseItemStack.nbtSerializer.apply(this)

  override def item: Item = _item

  override def nbt: NBTTagCompound = _nbt

  override def item_=(i: Item): Unit = _item = i

  implicit def asMinecraft: ItemStack = new ItemStack(item, stackSize, damage, nbt)

}
