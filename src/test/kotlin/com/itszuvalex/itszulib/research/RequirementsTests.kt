package com.itszuvalex.itszulib.research

import com.itszuvalex.itszulib.team.Research
import net.minecraft.SharedConstants
import net.minecraft.nbt.NbtOps
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import com.itszuvalex.itszulib.TestableIItemStack
import com.itszuvalex.itszulib.api.adapters.IItemStack
import net.neoforged.neoforge.common.crafting.SizedIngredient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

private val TREE = Identifier.fromNamespaceAndPath("itszulib_test", "tree")
private val COMPUTATION = Identifier.fromNamespaceAndPath("itszulib_test", "computation")

private fun id(path: String) = Identifier.fromNamespaceAndPath("itszulib_test", path)

private fun tech(cost: Long = 10L, resources: Map<Identifier, Long> = emptyMap(), items: List<SizedIngredient> = emptyList(), rewards: List<ItemStackTemplate> = emptyList()) =
    Technology(TREE, emptyList(), cost, Items.BOOK, Optional.empty(), Optional.empty(), Optional.empty(), false, false, resources, items, rewards)

class RequirementsTests {
    companion object {
        @BeforeAll
        @JvmStatic
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    @Test
    fun Remaining_CountsPointsResourcesAndItems() {
        val techs = Technologies(mapOf(id("a") to tech(10L, mapOf(COMPUTATION to 500L), listOf(SizedIngredient.of(Items.IRON_INGOT, 8)))))
        val r = Research(emptySet(), mapOf(id("a") to 10L), requirements = mapOf(id("a") to mapOf(Technologies.resourceKey(COMPUTATION) to 200L, Technologies.itemKey(0) to 3L)))
        val left = techs.remaining(id("a"), r)!!
        Assertions.assertEquals(0L, left.points)
        Assertions.assertEquals(300L, left.resources[COMPUTATION])
        Assertions.assertEquals(5, left.items.single().second)
        Assertions.assertFalse(techs.requirementsMet(id("a"), r))
        val done = r.withRequirement(id("a"), Technologies.resourceKey(COMPUTATION), 500L).withRequirement(id("a"), Technologies.itemKey(0), 8L)
        Assertions.assertTrue(techs.requirementsMet(id("a"), done))
    }

    @Test
    fun Unlock_DropsRequirementProgress() {
        val r = Research(emptySet(), requirements = mapOf(id("a") to mapOf("item/0" to 2L))).unlock(id("a"))
        Assertions.assertTrue(r.requirements.isEmpty())
        Assertions.assertEquals(r, r.withRequirement(id("a"), "item/0", 5L), "unlocked ids keep none")
    }

    @Test
    fun Research_RequirementsAndClaims_Validated() {
        Assertions.assertThrows(IllegalArgumentException::class.java) { Research(emptySet(), requirements = mapOf(id("a") to mapOf("x" to 0L))) }
        Assertions.assertThrows(IllegalArgumentException::class.java) { Research(setOf(id("a")), requirements = mapOf(id("a") to mapOf("x" to 1L))) }
        Assertions.assertThrows(IllegalArgumentException::class.java) { Research(emptySet(), claimed = mapOf(id("a") to setOf(UUID.randomUUID()))) }
    }

    @Test
    fun Codec_RequirementsAndClaims_RoundTrip() {
        val player = UUID.randomUUID()
        val research = Research(setOf(id("a")), queue = listOf(id("b")), requirements = mapOf(id("b") to mapOf("item/0" to 4L)), claimed = mapOf(id("a") to setOf(player)))
        val tag = Research.CODEC.encodeStart(NbtOps.INSTANCE, research).getOrThrow()
        Assertions.assertEquals(research, Research.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow())
    }

    @Test
    fun Merge_MaxPerRequirement_UnionsClaims() {
        val p1 = UUID.randomUUID()
        val p2 = UUID.randomUUID()
        val team = Research(setOf(id("a")), requirements = mapOf(id("b") to mapOf("item/0" to 4L, "item/1" to 1L)), claimed = mapOf(id("a") to setOf(p1)))
        val joining = Research(setOf(id("a")), requirements = mapOf(id("b") to mapOf("item/0" to 2L, "item/2" to 7L)), claimed = mapOf(id("a") to setOf(p2)))
        val merged = Research.merge(team, joining)
        Assertions.assertEquals(mapOf("item/0" to 4L, "item/1" to 1L, "item/2" to 7L), merged.requirements[id("b")])
        Assertions.assertEquals(setOf(p1, p2), merged.claimed[id("a")])
    }

    /** A test stack of a real item id, for the default matcher (which compares ids). */
    private fun stackOf(item: net.minecraft.world.item.Item, count: Int): IItemStack {
        val fake = TestableIItemStack(1, count)
        val id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)
        return object : IItemStack by fake {
            override fun item(): Identifier = id
        }
    }

    @Test
    fun Deliver_TakesWhatIsNeededFromMatchingStacks_KeepsTheRest() {
        val techs = Technologies(mapOf(id("a") to tech(items = listOf(SizedIngredient.of(Items.IRON_INGOT, 4), SizedIngredient.of(Items.GOLD_INGOT, 2)))))
        val stacks = listOf(stackOf(Items.IRON_INGOT, 3), stackOf(Items.DIAMOND, 5), stackOf(Items.IRON_INGOT, 5), stackOf(Items.GOLD_INGOT, 1))
        val (research, taken) = techs.deliver(id("a"), Research.EMPTY, stacks)
        Assertions.assertEquals(5, taken)
        Assertions.assertEquals(listOf(0, 5, 4, 0), stacks.map { it.stackSize() })
        Assertions.assertEquals(4L, research.requirementOf(id("a"), Technologies.itemKey(0)))
        Assertions.assertEquals(1L, research.requirementOf(id("a"), Technologies.itemKey(1)))
        val (again, more) = techs.deliver(id("a"), research, listOf(stackOf(Items.IRON_INGOT, 9), stackOf(Items.GOLD_INGOT, 9)))
        Assertions.assertEquals(1, more, "only the gold still missing")
        Assertions.assertTrue(techs.requirementsMet(id("a"), again.withProgress(id("a"), 10L)))
    }

    @Test
    fun Deliver_CustomMatcher_DecidesWhatCounts() {
        val techs = Technologies(mapOf(id("a") to tech(items = listOf(SizedIngredient.of(Items.IRON_INGOT, 3)))))
        val wanted = TestableIItemStack(7, 2)
        val other = TestableIItemStack(8, 9)
        val (_, taken) = techs.deliver(id("a"), Research.EMPTY, listOf(other, wanted)) { _, stack -> stack.item() == TestableIItemStack(7).item() }
        Assertions.assertEquals(2, taken)
        Assertions.assertEquals(0, wanted.stackSize())
        Assertions.assertEquals(9, other.stackSize())
    }

    @Test
    fun Deliver_ResearchedOrUnknown_TakesNothing() {
        val techs = Technologies(mapOf(id("a") to tech(items = listOf(SizedIngredient.of(Items.IRON_INGOT, 3)))))
        val stack = stackOf(Items.IRON_INGOT, 3)
        Assertions.assertEquals(0, techs.deliver(id("a"), Research(setOf(id("a"))), listOf(stack)).second)
        Assertions.assertEquals(0, techs.deliver(id("missing"), Research.EMPTY, listOf(stack)).second)
        Assertions.assertEquals(3, stack.stackSize())
    }

    @Test
    fun UnclaimedRewards_OnlyResearchedWithRewardsNotClaimed() {
        val player = UUID.randomUUID()
        val techs = Technologies(mapOf(
            id("rewarding") to tech(rewards = listOf(ItemStackTemplate(Items.DIAMOND))),
            id("plain") to tech(),
            id("pending") to tech(rewards = listOf(ItemStackTemplate(Items.DIAMOND))),
        ))
        val research = Research(setOf(id("rewarding"), id("plain")))
        Assertions.assertEquals(listOf(id("rewarding")), techs.unclaimedRewards(research, player))
        val claimed = research.withClaimed(player, listOf(id("rewarding"), id("pending")))
        Assertions.assertEquals(emptyList<Identifier>(), techs.unclaimedRewards(claimed, player))
        Assertions.assertFalse(claimed.claimed.containsKey(id("pending")), "no claims for unresearched ids")
        Assertions.assertEquals(listOf(id("rewarding")), techs.unclaimedRewards(claimed, UUID.randomUUID()), "each player claims once")
    }

    @Test
    fun TechnologyCodec_ReadsResourcesItemsRewards() {
        val ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,
            net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY))
        val json = com.google.gson.JsonParser.parseString(
            """{"tree": "itszulib_test:tree", "cost": 5, "resources": {"itszulib_test:computation": 100},
               "items": [{"ingredient": "minecraft:iron_ingot", "count": 8}], "rewards": [{"id": "minecraft:diamond", "count": 2}]}""",
        )
        val tech = Technology.CODEC.parse(ops, json).getOrThrow()
        Assertions.assertEquals(mapOf(COMPUTATION to 100L), tech.resources)
        Assertions.assertEquals(8, tech.items.single().count())
        Assertions.assertTrue(Technologies.matchesItem(tech.items.single(), stackOf(Items.IRON_INGOT, 1)))
        Assertions.assertFalse(Technologies.matchesItem(tech.items.single(), stackOf(Items.GOLD_INGOT, 1)))
        Assertions.assertTrue(tech.rewards.single().item().value() === Items.DIAMOND && tech.rewards.single().count() == 2)
        val bad = com.google.gson.JsonParser.parseString("""{"tree": "itszulib_test:tree", "resources": {"itszulib_test:computation": 0}}""")
        Assertions.assertTrue(Technology.CODEC.parse(ops, bad).isError)
    }
}
