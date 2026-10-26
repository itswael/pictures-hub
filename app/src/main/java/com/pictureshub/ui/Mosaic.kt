package com.pictureshub.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pictureshub.data.Group
import com.pictureshub.data.MediaItem
import kotlin.random.Random

private val Gap = 3.dp
private const val Columns = 4

/** A tile's position and span, in grid cells, within a four-column strip. */
private class Cell(val x: Int, val y: Int, val w: Int, val h: Int)

/** A strip of [rows] rows that [cells] cover exactly, with no gaps. Hero strips hold one oversized tile. */
private class Template(val rows: Int, val hero: Boolean, val cells: List<Cell>)

private fun strip(rows: Int, hero: Boolean = false, vararg cells: IntArray) =
    Template(rows, hero, cells.map { Cell(it[0], it[1], it[2], it[3]) })

private fun c(x: Int, y: Int, w: Int = 1, h: Int = 1) = intArrayOf(x, y, w, h)

private val Templates = listOf(
    strip(2, true, c(0, 0, 2, 2), c(2, 0), c(3, 0), c(2, 1), c(3, 1)),
    strip(2, true, c(2, 0, 2, 2), c(0, 0), c(1, 0), c(0, 1), c(1, 1)),
    strip(3, true, c(0, 0, 3, 3), c(3, 0), c(3, 1), c(3, 2)),
    strip(3, true, c(1, 0, 3, 3), c(0, 0), c(0, 1), c(0, 2)),
    strip(2, false, c(0, 0, 2, 1), c(2, 0), c(3, 0), c(0, 1), c(1, 1), c(2, 1, 2, 1)),
    strip(2, false, c(0, 0, 1, 2), c(1, 0, 2, 1), c(3, 0, 1, 2), c(1, 1), c(2, 1)),
    strip(3, false, c(0, 0, 2, 2), c(2, 0, 2, 1), c(2, 1), c(3, 1), c(0, 2), c(1, 2), c(2, 2, 2, 1)),
    strip(1, false, c(0, 0), c(1, 0), c(2, 0), c(3, 0)),
    strip(1, false, c(0, 0, 2, 1), c(2, 0), c(3, 0)),
    strip(1, false, c(0, 0), c(1, 0), c(2, 0, 2, 1)),
    strip(1, false, c(0, 0, 2, 1), c(2, 0, 2, 1)),
)

/** Used only when a single picture is left over. */
private val SingleStrip = strip(2, true, c(0, 0, 4, 2))

private class Tile(val item: MediaItem, val index: Int, val cell: Cell)
private class Band(val key: Long, val rows: Int, val tiles: List<Tile>)
private class Section(val label: String, val bands: List<Band>)

/**
 * Splits [items] into strips with a mix of tile sizes. The choice is seeded by the first picture
 * of each strip, so the layout is the same every time the list is shown. A strip never leaves
 * exactly one picture behind, since no strip fits a lone picture without a gap.
 */
private fun buildBands(items: List<MediaItem>, firstIndex: Int): List<Band> {
    val bands = ArrayList<Band>()
    var i = 0
    var prev: Template? = null
    while (i < items.size) {
        val remaining = items.size - i
        val random = Random(items[i].id)
        val fits = Templates.filter { it.cells.size <= remaining && remaining - it.cells.size != 1 }
        val fresh = fits.filter { it !== prev && !(it.hero && prev?.hero == true) }
        val options = fresh.ifEmpty { fits }
        val template = if (options.isEmpty()) {
            SingleStrip
        } else {
            val wantHero = random.nextInt(100) < 28
            options.filter { it.hero == wantHero }.ifEmpty { options }.random(random)
        }
        val tiles = template.cells.mapIndexed { n, cell -> Tile(items[i + n], firstIndex + i + n, cell) }
        bands += Band(items[i].id, template.rows, tiles)
        i += template.cells.size
        prev = template
    }
    return bands
}

/**
 * Pictures laid out like Start-screen tiles: squares, wide and tall tiles and a few large ones,
 * packed edge to edge. Several [groups] can be shown under headings.
 */
@Composable
fun MosaicList(
    groups: List<Group>,
    showHeaders: Boolean,
    onOpen: (List<MediaItem>, Int) -> Unit,
) {
    val flat = remember(groups) { groups.flatMap { it.items } }
    val sections = remember(groups) {
        var offset = 0
        groups.map { g ->
            Section(g.label, buildBands(g.items, offset)).also { offset += g.items.size }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cell = (maxWidth - Gap * (Columns - 1)) / Columns
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Gap),
            contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            for (section in sections) {
                if (showHeaders) {
                    item(key = "h-${section.label}") {
                        Text(
                            section.label.lowercase(),
                            style = lightText(22, Accent),
                            modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 4.dp),
                        )
                    }
                }
                items(section.bands, key = { it.key }) { band ->
                    Box(Modifier.fillMaxWidth().height(cell * band.rows + Gap * (band.rows - 1))) {
                        for (tile in band.tiles) {
                            val c = tile.cell
                            val span = maxOf(c.w, c.h)
                            MediaCell(
                                tile.item,
                                Modifier
                                    .offset((cell + Gap) * c.x, (cell + Gap) * c.y)
                                    .size(cell * c.w + Gap * (c.w - 1), cell * c.h + Gap * (c.h - 1))
                                    .wpPress { onOpen(flat, tile.index) },
                                size = when {
                                    span >= 3 -> 900
                                    span == 2 -> 560
                                    else -> 320
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
