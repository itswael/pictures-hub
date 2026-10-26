package com.pictureshub.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pictureshub.Screen
import com.pictureshub.data.Group
import com.pictureshub.data.Library
import com.pictureshub.data.MediaItem
import kotlinx.coroutines.delay

private val PageGap = 12.dp
private val TileGap = 3.dp

@Composable
fun HubScreen(
    library: Library,
    favorites: List<MediaItem>,
    pinned: List<MediaItem>,
    partialAccess: Boolean,
    liveBackground: Boolean,
    onNavigate: (Screen) -> Unit,
    onOpen: (List<MediaItem>, Int) -> Unit,
    onSelectMore: () -> Unit,
) {
    // Over a photo the hub always uses light text; otherwise it follows the phone's theme.
    val palette = if (liveBackground) PhotoPalette else LocalPalette.current
    CompositionLocalProvider(LocalPalette provides palette) {
        BoxWithConstraints(Modifier.fillMaxSize().background(palette.bg)) {
            val pageWidth = maxWidth * 0.86f
            val pagerState = rememberPagerState { 4 }
            val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            if (liveBackground) LiveBackdrop(library)

            // The giant title lives behind the pages and drifts slower than they do.
            Text(
                "pictures",
                style = lightText(88),
                softWrap = false,
                modifier = Modifier
                    .padding(top = top)
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .graphicsLayer {
                        val progress = pagerState.currentPage + pagerState.currentPageOffsetFraction
                        translationX = 16.dp.toPx() - progress * pageWidth.toPx() * 0.45f
                    },
            )

            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(pageWidth),
                pageSpacing = PageGap,
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp,
                    top = top + 112.dp, bottom = bottom + 8.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                Column(Modifier.fillMaxHeight()) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            listOf("what's new", "favorites", "pinned", "albums & more")[page],
                            style = lightText(30),
                            modifier = Modifier.weight(1f),
                        )
                        // Settings live at the end of the scroll, on the heading line, so they
                        // take no room from the page and don't show on the other pages.
                        if (page == 3) SettingsLink { onNavigate(Screen.Settings) }
                    }
                    when (page) {
                        0 -> WhatsNew(library.news, onOpen)
                        1 -> FavoriteTiles(favorites, onOpen)
                        2 -> PinnedTiles(pinned, onOpen)
                        else -> MainTiles(library, pageWidth, partialAccess, onNavigate, onSelectMore)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsLink(onClick: () -> Unit) {
    Row(
        Modifier.wpPress(onClick).padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Settings, null,
            tint = LocalPalette.current.muted,
            modifier = Modifier.size(22.dp),
        )
        Text(
            "settings",
            style = lightText(16, muted = true),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/**
 * One random camera photo, chosen when the hub is entered (app start, or coming back from
 * another page) and kept until it is left again. Screenshots and downloads are skipped.
 */
@Composable
private fun LiveBackdrop(library: Library) {
    val pool = remember(library) {
        library.camera.filter { !it.isVideo }.ifEmpty { library.all.filter { !it.isVideo } }
    }
    var picked by remember { mutableStateOf<MediaItem?>(null) }
    LaunchedEffect(pool.isNotEmpty()) {
        if (picked == null && pool.isNotEmpty()) picked = pool.random()
    }
    Crossfade(picked, animationSpec = tween(900), label = "backdrop") { item ->
        if (item != null) Thumb(item, Modifier.fillMaxSize(), 1080)
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
}

@Composable
private fun MainTiles(
    library: Library,
    pageWidth: Dp,
    partialAccess: Boolean,
    onNavigate: (Screen) -> Unit,
    onSelectMore: () -> Unit,
) {
    val tile = (pageWidth - PageGap) / 2
    val covers = remember(library) { library.albums.map { it.cover } }
    val dated = remember(library) { library.all.drop(2) }
    Column(verticalArrangement = Arrangement.spacedBy(PageGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(PageGap)) {
            LiveTile("camera roll", library.camera, tile, 0) { onNavigate(Screen.CameraRoll) }
            LiveTile("albums", covers, tile, 1) { onNavigate(Screen.Albums) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PageGap)) {
            LiveTile("date", dated, tile, 2) { onNavigate(Screen.Date) }
            LiveTile("videos", library.videos, tile, 3) { onNavigate(Screen.Videos) }
        }
        if (partialAccess) {
            Text(
                "showing only the photos you allowed.\ntap to choose more",
                style = lightText(14, muted = true),
                modifier = Modifier
                    .wpPress(onSelectMore)
                    .padding(vertical = 8.dp),
            )
        }
    }
}

/** Start-screen style tile: a square that keeps swapping to the next photo in its set. */
@Composable
private fun LiveTile(
    label: String,
    items: List<MediaItem>,
    size: Dp,
    phase: Int,
    onClick: () -> Unit,
) {
    val rotation = minOf(items.size, 8)
    var index by mutableIntStateOf(0)
    LaunchedEffect(rotation) {
        index = 0
        if (rotation > 1) {
            delay(1500L + phase * 1100L)
            while (true) {
                index = (index + 1) % rotation
                delay(5200)
            }
        }
    }
    Box(Modifier.size(size).background(Accent).wpPress(onClick)) {
        val current = if (rotation == 0) null else items[index % rotation]
        Crossfade(current, animationSpec = tween(700), label = "tile") { item ->
            if (item != null) Thumb(item, Modifier.fillMaxSize(), 400)
        }
        Box(Modifier.fillMaxSize().background(ScrimBottom))
        Text(
            label,
            style = lightText(18, Color.White),
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        )
        if (items.isNotEmpty()) {
            Text(
                items.size.toString(),
                style = lightText(14, Color.White),
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
    }
}

/** Stand-in for the social "what's new" feed: recent additions grouped by day. */
@Composable
private fun WhatsNew(groups: List<Group>, onOpen: (List<MediaItem>, Int) -> Unit) {
    if (groups.isEmpty()) {
        Text("nothing new yet", style = lightText(16, muted = true))
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        items(groups, key = { it.label }) { group ->
            Column(Modifier.background(LocalPalette.current.card).padding(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Text(group.label, style = lightText(18), modifier = Modifier.weight(1f))
                    Text("${group.items.size} new", style = lightText(14, Accent))
                }
                NewsMosaic(group.items, onOpen)
            }
        }
    }
}

@Composable
private fun NewsMosaic(items: List<MediaItem>, onOpen: (List<MediaItem>, Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(TileGap),
    ) {
        MediaCell(
            items[0],
            Modifier.weight(2f).fillMaxHeight().wpPress { onOpen(items, 0) },
            500,
        )
        if (items.size > 1) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(TileGap)) {
                for (i in 1..minOf(2, items.lastIndex)) {
                    MediaCell(items[i], Modifier.weight(1f).fillMaxWidth().wpPress { onOpen(items, i) })
                }
            }
        }
    }
}

/** Documents and IDs: shown whole on cards, never cropped, so they can be read at a glance. */
@Composable
private fun PinnedTiles(items: List<MediaItem>, onOpen: (List<MediaItem>, Int) -> Unit) {
    if (items.isEmpty()) {
        Text(
            "pin documents and IDs from the\npicture viewer to keep them here",
            style = lightText(16, muted = true),
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(TileGap * 2),
        verticalArrangement = Arrangement.spacedBy(TileGap * 2),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        itemsIndexed(items, key = { _, it -> it.id }) { i, item ->
            Box(
                Modifier
                    .aspectRatio(1.3f)
                    .background(LocalPalette.current.card)
                    .wpPress { onOpen(items, i) }
            ) {
                Thumb(item, Modifier.fillMaxSize(), 480, ContentScale.Fit)
            }
        }
    }
}

@Composable
private fun FavoriteTiles(items: List<MediaItem>, onOpen: (List<MediaItem>, Int) -> Unit) {
    if (items.isEmpty()) {
        Text("tap the heart on a picture\nto keep it here", style = lightText(16, muted = true))
        return
    }
    // A ten-item cycle of two wide tiles fills four three-column rows with no gaps.
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(TileGap),
        verticalArrangement = Arrangement.spacedBy(TileGap),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        itemsIndexed(
            items,
            key = { _, it -> it.id },
            span = { i, _ -> GridItemSpan(if (i % 10 == 0 || i % 10 == 6) 2 else 1) },
        ) { i, item ->
            val wide = i % 10 == 0 || i % 10 == 6
            MediaCell(
                item,
                Modifier.aspectRatio(if (wide) 2f else 1f).wpPress { onOpen(items, i) },
                if (wide) 500 else 320,
            )
        }
    }
}
