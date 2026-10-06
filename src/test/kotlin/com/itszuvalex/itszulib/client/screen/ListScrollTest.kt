package com.itszuvalex.itszulib.client.screen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ListScrollTest {
    @Test
    fun Max_FewerRowsThanFit_Zero() {
        assertEquals(0, ListScroll.max(4, 6))
        assertEquals(0, ListScroll.max(6, 6))
        assertEquals(9, ListScroll.max(15, 6))
    }

    @Test
    fun NeedsBar_OnlyWhenMoreRowsThanFit() {
        assertFalse(ListScroll.needsBar(6, 6))
        assertTrue(ListScroll.needsBar(7, 6))
    }

    @Test
    fun Clamp_KeepsTheScrollInsideTheList() {
        assertEquals(0, ListScroll.clamp(-3, 15, 6))
        assertEquals(9, ListScroll.clamp(20, 15, 6))
        assertEquals(0, ListScroll.clamp(5, 3, 6))
    }

    @Test
    fun ThumbHeight_InProportionButGrabbable() {
        assertEquals(66, ListScroll.thumbHeight(66, 4, 6), "no bar needed: the whole track")
        assertEquals(26, ListScroll.thumbHeight(66, 15, 6))
        assertEquals(10, ListScroll.thumbHeight(66, 200, 6), "never under the minimum")
        assertEquals(5, ListScroll.thumbHeight(5, 200, 6), "or over a short track")
    }

    @Test
    fun ThumbTop_FollowsTheScroll() {
        val thumb = ListScroll.thumbHeight(66, 15, 6)
        assertEquals(0, ListScroll.thumbTop(66, thumb, 0, 15, 6))
        assertEquals(66 - thumb, ListScroll.thumbTop(66, thumb, 9, 15, 6))
        assertEquals(0, ListScroll.thumbTop(66, 66, 0, 4, 6))
    }

    @Test
    fun ScrollForThumbTop_IsTheInverse() {
        val thumb = ListScroll.thumbHeight(66, 15, 6)
        for (scroll in 0..9) {
            assertEquals(scroll, ListScroll.scrollForThumbTop(66, thumb, ListScroll.thumbTop(66, thumb, scroll, 15, 6), 15, 6))
        }
        assertEquals(9, ListScroll.scrollForThumbTop(66, thumb, 500, 15, 6), "dragged past the end")
        assertEquals(0, ListScroll.scrollForThumbTop(66, thumb, -20, 15, 6), "dragged past the start")
    }

    @Test
    fun Reveal_ScrollsOnlyAsFarAsNeeded() {
        assertEquals(0, ListScroll.reveal(3, 0, 15, 6), "already showing")
        assertEquals(4, ListScroll.reveal(9, 0, 15, 6), "below: it ends up the last row shown")
        assertEquals(2, ListScroll.reveal(2, 5, 15, 6), "above: it ends up the first row shown")
        assertEquals(9, ListScroll.reveal(14, 0, 15, 6), "at the end")
        assertEquals(0, ListScroll.reveal(1, 0, 3, 6), "short list")
    }

    @Test
    fun Page_MovesByTheRowsShownAndStopsAtTheEnds() {
        assertEquals(6, ListScroll.page(0, 1, 15, 6))
        assertEquals(9, ListScroll.page(6, 1, 15, 6))
        assertEquals(0, ListScroll.page(3, -1, 15, 6))
    }

    @Test
    fun Ellipsize_FitsOrCutsWithDots() {
        val width = { s: String -> s.length * 5 }
        assertEquals("short", ListScroll.ellipsize("short", 100, width))
        val cut = ListScroll.ellipsize("Workshop north-east line 07", 80, width)
        assertTrue(width(cut) <= 80, cut)
        assertTrue(cut.startsWith("Wor") && cut.endsWith("07") && "..." in cut, "both ends stay: $cut")
        assertTrue(ListScroll.ellipsize("Workshop north-east line 07", 80, width) != ListScroll.ellipsize("Workshop north-east line 08", 80, width), "names that differ at the end stay different")
        assertEquals("...", ListScroll.ellipsize("anything long", 15, width))
    }
}
