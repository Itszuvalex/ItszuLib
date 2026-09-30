package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.utility.Loc4
import com.itszuvalex.itszulib.api.utility.Loc4Indirect
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.util.ProblemReporter
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import org.junit.jupiter.api.Assertions
import java.util.Optional

object MCAssert {
    fun assertIItemStackEmpty(stack: IItemStack) {
        if (stack === IItemStack.Empty) return
        Assertions.assertTrue(stack.isEmpty())
    }

    fun assertIItemStackNotEmpty(stack: IItemStack) {
        Assertions.assertNotSame(stack, IItemStack.Empty)
        Assertions.assertFalse(stack.isEmpty())
    }

    fun failVanillaClass(methodName: String): Nothing =
        Assertions.fail("Failed due to calling vanilla minecraft interface method: $methodName")
}

object TestableLoc4 {
    val DEFAULT_DIM: Identifier = Identifier.parse("test")
    val ORIGIN: Loc4 = Loc4Indirect(DEFAULT_DIM, BlockPos(0, 0, 0))
}

/**
 * Serializes with a registry-free tag round trip.
 */
object TestIO {
    fun write(block: (ValueOutput) -> Unit): CompoundTag {
        val output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING)
        block(output)
        return output.buildResult()
    }

    fun read(tag: CompoundTag): ValueInput = TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag)
}

class TestableLevel(private val dimension: Identifier) : ILevel {
    private val blockEntityMap = HashMap<BlockPos, IBlockEntity>()

    override fun isClientSide(): Boolean = false
    override fun dimension(): ResourceKey<Level> = MCAssert.failVanillaClass("dimension")
    override fun dimensionLocation(): Identifier = dimension
    override fun toMinecraft(): Level = MCAssert.failVanillaClass("toMinecraft")
    override fun isLoaded(pos: BlockPos): Boolean = true
    override fun getIBlockEntity(pos: BlockPos): IBlockEntity? = blockEntityMap[pos]
    override fun setIBlockEntity(entity: IBlockEntity) {
        blockEntityMap[entity.getBlockPos()] = entity
    }
    override fun setBlockEntity(entity: BlockEntity) = MCAssert.failVanillaClass("setBlockEntity")
}

class TestableIItemStack(
    var testItem: Int = -1,
    var testStack: Int = 0,
    var testDamage: Int = 0,
) : IItemStack {
    var testStackMax = 64
    var testDamageMax = 0
    var testNBT: CompoundTag? = CompoundTag()

    constructor(item: Int) : this(item, 1, 0)

    override fun item(): Identifier = Identifier.fromNamespaceAndPath(ItszuLib.ID, testItem.toString().replace('-', 'n'))
    override fun stackSize(): Int = testStack
    override fun setStackSize(size: Int) {
        testStack = size
    }
    override fun stackSizeMax(): Int = testStackMax
    override fun damage(): Int = testDamage
    override fun setDamage(damage: Int) {
        testDamage = damage
    }
    override fun damageMax(): Int = testDamageMax
    override fun components(): DataComponentPatch = DataComponentPatch.EMPTY
    override fun toMinecraft(): ItemStack = Assertions.fail("toMinecraft shouldn't be reached from test apis")
    override fun isEmpty(): Boolean = stackSize() <= 0
    override fun copy(): IItemStack = TestableIItemStack(testItem, testStack, testDamage).also { it.testNBT = testNBT?.copy() }
    override fun isItemEqual(other: IItemStack): Boolean {
        if (other !is TestableIItemStack) {
            Assertions.fail<Unit>("Tested TestableIItemStack#isItemEqual against non TestableIItemStack class: ${other.javaClass.typeName}")
            return false
        }
        if (testItem != other.testItem) return false
        if (testDamage != other.testDamage) return false
        val nbtNullOrEmpty = testNBT?.isEmpty ?: true
        val otherNbtNullOrEmpty = other.testNBT?.isEmpty ?: true
        return nbtNullOrEmpty == otherNbtNullOrEmpty
    }
    override fun <T : Any> getModule(module: IModule<T>, side: Direction?): T? = null

    companion object {
        const val ITEM_KEY = "Item"
        const val STACK_KEY = "Stack"
        const val DAMAGE_KEY = "Damage"
        const val NBT_KEY = "NBT"

        val CODEC: Codec<IItemStack> = RecordCodecBuilder.create<TestableIItemStack> { instance ->
            instance.group(
                Codec.INT.fieldOf(ITEM_KEY).forGetter { it.testItem },
                Codec.INT.fieldOf(STACK_KEY).forGetter { it.testStack },
                Codec.INT.fieldOf(DAMAGE_KEY).forGetter { it.testDamage },
                CompoundTag.CODEC.optionalFieldOf(NBT_KEY).forGetter { Optional.ofNullable(it.testNBT) },
            ).apply(instance) { item, stack, damage, nbt ->
                TestableIItemStack(item, stack, damage).also { it.testNBT = nbt.orElse(null) }
            }
        }.xmap({ it }, { it as? TestableIItemStack ?: TestableIItemStack() })

        fun overrideCodec() = IItemStack.CODEC.setOverrideValue(CODEC)

        fun resetCodec() = IItemStack.CODEC.revert()
    }
}
