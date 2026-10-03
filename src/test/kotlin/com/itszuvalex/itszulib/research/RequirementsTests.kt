package com.itszuvalex.itszulib.research

import com.itszuvalex.itszulib.team.Research
import net.minecraft.SharedConstants
import net.minecraft.nbt.NbtOps
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.Items
import net.neoforged.neoforge.common.crafting.SizedIngredient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

private val TREE = Identifier.fromNamespaceAndPath("itszulib_test", "tree")
private val COMPUTATION = Identifier.fromNamespaceAndPath("itszulib_test", "computation")

private fun id(path: String) = Identifier.fromNamespaceAndPath("itszulib_test", path)

private fun tech(cost: Long = 10L, resources: Map<Identifier, Long> = emptyMap(), items: List<SizedIngredient> = emptyList()) =
    Technology(TREE, emptyList(), cost, Items.BOOK, Optional.empty(), Optional.empty(), Optional.empty(), false, false, resources, items)

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
}
