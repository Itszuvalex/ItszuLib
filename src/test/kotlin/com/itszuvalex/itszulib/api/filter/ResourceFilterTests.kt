package com.itszuvalex.itszulib.api.filter

import com.itszuvalex.itszulib.TestableIItemStack
import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.storage.IndexedItemStorage
import com.itszuvalex.itszulib.api.storage.ItemStorageArray
import com.itszuvalex.itszulib.api.storage.ItemStorageIndex
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import net.minecraft.world.item.ItemStack
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/** Words as things, "word#data" carrying data; a held item means nothing. */
private object Words : FilterKind<String> {
    override val empty = ""
    override val codec: Codec<String> = Codec.STRING
    override fun isEmpty(t: String) = t.isEmpty()
    override fun same(a: String, b: String, components: Boolean) = if (components) a == b else a.substringBefore('#') == b.substringBefore('#')
    override fun entryOf(t: String) = t
    override fun fromHeld(held: ItemStack): String? = null
    override fun equal(a: String, b: String) = a == b
    override fun hash(t: String) = t.hashCode()
}

class ResourceFilterTest {
    private fun words(vararg entries: String, mode: FilterMode = FilterMode.ALLOW, components: Boolean = true) =
        ResourceFilter(Words, 4, entries.toList(), mode, components)

    @Test
    fun Test_NothingListedPassesEverything() {
        Assertions.assertTrue(words().test("anything"))
        Assertions.assertTrue(words(mode = FilterMode.DENY).test("anything"), "an empty denylist keeps nothing out")
        Assertions.assertTrue(words().isEmpty)
    }

    @Test
    fun Test_AllowAndDeny() {
        val allow = words("iron", "gold")
        Assertions.assertTrue(allow.test("iron"))
        Assertions.assertFalse(allow.test("dirt"))
        val deny = allow.withMode(FilterMode.DENY)
        Assertions.assertFalse(deny.test("iron"))
        Assertions.assertTrue(deny.test("dirt"))
    }

    @Test
    fun Test_ComponentMatching() {
        val exact = words("sword#sharp")
        Assertions.assertTrue(exact.test("sword#sharp"))
        Assertions.assertFalse(exact.test("sword#dull"), "exact entries need the same data")
        Assertions.assertTrue(exact.withMatchComponents(false).test("sword#dull"), "or match the thing alone")
    }

    @Test
    fun Only_NamesTheItemsOfAnAllowlist() {
        Assertions.assertEquals(listOf("iron"), words("", "iron").only())
        Assertions.assertNull(words("iron", mode = FilterMode.DENY).only(), "a denylist does not say what passes")
        Assertions.assertNull(words().only(), "nor does an empty filter")
    }

    @Test
    fun Entries_PaddedToSizeAndChangedImmutably() {
        val f = words("a")
        Assertions.assertEquals(listOf("a", "", "", ""), f.entries)
        val g = f.withEntry(2, "b")
        Assertions.assertEquals(listOf("a", "", "b", ""), g.entries)
        Assertions.assertEquals(listOf("a", "", "", ""), f.entries, "the original is unchanged")
        Assertions.assertThrows(IllegalArgumentException::class.java) { f.withEntry(4, "x") }
    }

    @Test
    fun Apply_ModeComponentsAndCells() {
        val f = words("a")
        Assertions.assertEquals(FilterMode.DENY, f.apply(FilterActions.mode(), ItemStack.EMPTY)!!.mode)
        Assertions.assertFalse(f.apply(FilterActions.components(), ItemStack.EMPTY)!!.matchComponents)
        Assertions.assertNull(f.apply(FilterActions.set(1), ItemStack.EMPTY), "words take nothing from a hand")
        Assertions.assertNull(f.apply(FilterActions.set(9), ItemStack.EMPTY), "no such cell")
    }

    @Test
    fun Actions_PackOperationAndCell() {
        for (cell in listOf(0, 1, 8, 63)) {
            val a = FilterActions.set(cell)
            Assertions.assertEquals(FilterActions.SET, FilterActions.op(a))
            Assertions.assertEquals(cell, FilterActions.cell(a))
            Assertions.assertTrue(a < (1 shl FilterActions.BITS))
        }
        Assertions.assertEquals(FilterActions.MODE, FilterActions.op(FilterActions.mode()))
    }

    @Test
    fun Codec_RoundTrips() {
        val codec = ResourceFilter.codec(Words, 4)
        val f = words("iron", "", "gold#shiny", mode = FilterMode.DENY, components = false)
        val json = codec.encodeStart(JsonOps.INSTANCE, f).getOrThrow()
        Assertions.assertEquals(f, codec.parse(JsonOps.INSTANCE, json).getOrThrow())
        val empty = codec.encodeStart(JsonOps.INSTANCE, words()).getOrThrow()
        Assertions.assertFalse(empty.asJsonObject.has("entries"), "an empty filter saves no entries")
    }

    @Test
    fun ItemKind_MatchesItemsThroughTheSeam() {
        val f = ResourceFilter(FilterKinds.I_ITEM, 3, listOf(TestableIItemStack(1, 5)))
        Assertions.assertEquals(1, f.entries[0].stackSize(), "entries hold one")
        Assertions.assertTrue(f.test(TestableIItemStack(1, 64)))
        Assertions.assertFalse(f.test(TestableIItemStack(2, 1)))
    }
}

class ItemStorageIndexFilterTest {
    @Test
    fun Extract_TakesWhatTheFilterAllows() {
        val a = IndexedItemStorage(ItemStorageArray(4))
        val b = IndexedItemStorage(ItemStorageArray(4))
        a.setSlot(0, TestableIItemStack(1, 10))
        b.setSlot(2, TestableIItemStack(2, 7))
        val index = ItemStorageIndex().apply { add(a); add(b) }
        val allowTwo = ResourceFilter(FilterKinds.I_ITEM, 9, listOf(TestableIItemStack(2)))
        val taken = index.extract(allowTwo, 5)
        Assertions.assertEquals(TestableIItemStack(2).item(), taken.item())
        Assertions.assertEquals(5, taken.stackSize())
        val denyOne = allowTwo.withEntry(0, TestableIItemStack(1)).withMode(FilterMode.DENY)
        Assertions.assertEquals(TestableIItemStack(2).item(), index.extract(denyOne, 1).item(), "a denylist skips the denied item")
        val allowThree = ResourceFilter(FilterKinds.I_ITEM, 9, listOf(TestableIItemStack(3)))
        Assertions.assertTrue(index.extract(allowThree, 1).isEmpty(), "nothing allowed is held")
        Assertions.assertEquals(IItemStack.Empty, index.extract(allowThree, 1))
    }
}
