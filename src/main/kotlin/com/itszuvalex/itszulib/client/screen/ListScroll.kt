package com.itszuvalex.itszulib.client.screen

/**
 * The arithmetic of a scrolling list of rows with a scrollbar, apart from drawing it, so it is unit-tested: [count] rows
 * of which [rows] show at once, scrolled by [scroll] (the index of the first row shown).
 */
object ListScroll {
    /** The most a list can scroll. */
    @JvmStatic
    fun max(count: Int, rows: Int): Int = (count - rows).coerceAtLeast(0)

    /** Whether the list needs a scrollbar. */
    @JvmStatic
    fun needsBar(count: Int, rows: Int): Boolean = count > rows

    /** [scroll] kept inside the list. */
    @JvmStatic
    fun clamp(scroll: Int, count: Int, rows: Int): Int = scroll.coerceIn(0, max(count, rows))

    /**
     * The height of the scrollbar's thumb on a track [trackHeight] tall: in proportion to how much of the list shows, never
     * under [minimum] (so it can be grabbed) or over the track.
     */
    @JvmStatic
    @JvmOverloads
    fun thumbHeight(trackHeight: Int, count: Int, rows: Int, minimum: Int = 10): Int =
        if (!needsBar(count, rows)) trackHeight else (trackHeight * rows / count).coerceIn(minimum.coerceAtMost(trackHeight), trackHeight)

    /** Where the thumb's top is on the track, 0 at the top, for [scroll]. */
    @JvmStatic
    fun thumbTop(trackHeight: Int, thumbHeight: Int, scroll: Int, count: Int, rows: Int): Int {
        val max = max(count, rows)
        return if (max == 0) 0 else (trackHeight - thumbHeight) * clamp(scroll, count, rows) / max
    }

    /** The scroll that puts the thumb's top at [top] on the track (dragging the thumb). */
    @JvmStatic
    fun scrollForThumbTop(trackHeight: Int, thumbHeight: Int, top: Int, count: Int, rows: Int): Int {
        val travel = trackHeight - thumbHeight
        if (travel <= 0) return 0
        return clamp(Math.round(top.coerceIn(0, travel).toDouble() * max(count, rows) / travel).toInt(), count, rows)
    }

    /** The scroll that shows row [index] with the least movement from [scroll]. */
    @JvmStatic
    fun reveal(index: Int, scroll: Int, count: Int, rows: Int): Int = when {
        index < scroll -> clamp(index, count, rows)
        index >= scroll + rows -> clamp(index - rows + 1, count, rows)
        else -> clamp(scroll, count, rows)
    }

    /** [scroll] moved a page ([rows] rows) up (-1) or down (1). */
    @JvmStatic
    fun page(scroll: Int, direction: Int, count: Int, rows: Int): Int = clamp(scroll + direction * rows, count, rows)

    /**
     * [text] cut to fit [width] with an ellipsis in the middle, measuring with [measure] (a font's width); unchanged if it
     * fits. Both ends stay, since names that are long usually differ at the end (`... line 07`, `... line 08`).
     */
    @JvmStatic
    fun ellipsize(text: String, width: Int, measure: (String) -> Int): String {
        if (measure(text) <= width) return text
        val dots = "..."
        var head = (text.length + 1) / 2
        var tail = text.length - head
        fun cut() = text.substring(0, head).trimEnd() + dots + text.substring(text.length - tail).trimStart()
        while ((head > 0 || tail > 0) && measure(cut()) > width) {
            if (head > tail) head-- else tail--
        }
        return cut()
    }
}
