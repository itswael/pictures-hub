package com.pictureshub.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pictureshub.ViewerState
import com.pictureshub.data.MediaItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private typealias Media3Item = androidx.media3.common.MediaItem

/** Material "push pin"; the core icon set has no pin. */
private val PinIcon: ImageVector = ImageVector.Builder(
    name = "Pin",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = PathParser().parsePathString(
        "M16,9V4l1,0c0.55,0 1,-0.45 1,-1v0c0,-0.55 -0.45,-1 -1,-1H7C6.45,2 6,2.45 6,3v0" +
            "c0,0.55 0.45,1 1,1l1,0v5c0,1.66 -1.34,3 -3,3h0v2h5.97v7l1,1l1,-1v-7H19v-2h0" +
            "C17.34,12 16,10.66 16,9z"
    ).toNodes(),
    fill = SolidColor(Color.Black),
).build()

private val DateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)

@Composable
fun ViewerScreen(
    state: ViewerState,
    favorites: Set<Long>,
    pinned: Set<Long>,
    onToggleFavorite: (MediaItem) -> Unit,
    onTogglePin: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit,
) {
    val items = state.items
    val pager = rememberPagerState(initialPage = state.index) { items.size }
    var chrome by remember { mutableStateOf(true) }
    var videoPlaying by remember { mutableStateOf(false) }
    val current = items[pager.currentPage.coerceIn(0, items.lastIndex)]
    // The video controls take the bottom of the screen, so our bars step aside while one plays.
    val showBars = chrome && !videoPlaying

    CompositionLocalProvider(LocalPalette provides DarkPalette) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pager,
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1,
            key = { items[it].id },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ViewerPage(
                item = items[page],
                isCurrent = pager.currentPage == page,
                onTap = { chrome = !chrome },
                onPlayingChange = { videoPlaying = it },
            )
        }

        AnimatedVisibility(
            showBars, Modifier.align(Alignment.TopStart), enter = fadeIn(), exit = fadeOut(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "${pager.currentPage + 1} / ${items.size}",
                    style = lightText(14, muted = true),
                )
                Text(
                    DateFormat.format(Instant.ofEpochMilli(current.taken).atZone(ZoneId.systemDefault())),
                    style = lightText(20),
                )
            }
        }

        AnimatedVisibility(
            showBars, Modifier.align(Alignment.BottomStart), enter = fadeIn(), exit = fadeOut(),
        ) {
            val isFavorite = current.id in favorites
            val isPinned = current.id in pinned
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .navigationBarsPadding()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CircleButton(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    48, tint = if (isFavorite) Accent else Color.White,
                ) { onToggleFavorite(current) }
                CircleButton(PinIcon, 48, tint = if (isPinned) Accent else Color.White) {
                    onTogglePin(current)
                }
                CircleButton(Icons.Filled.Share, 48) { onShare(current) }
                CircleButton(Icons.Filled.Delete, 48) { onDelete(current) }
            }
        }
    }
    }
}

/**
 * One swipeable page. A photo is zoomable. A video shows its first frame with a play button,
 * and plays in place once that is tapped; swiping to another page stops it.
 */
@Composable
private fun ViewerPage(
    item: MediaItem,
    isCurrent: Boolean,
    onTap: () -> Unit,
    onPlayingChange: (Boolean) -> Unit,
) {
    var playing by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(isCurrent) { if (!isCurrent) playing = false }
    LaunchedEffect(playing, isCurrent) { if (isCurrent) onPlayingChange(playing) }

    Box(Modifier.fillMaxSize()) {
        if (item.isVideo && playing) {
            VideoPlayer(item.uri)
        } else {
            ZoomableImage(item, onTap)
            if (item.isVideo) {
                CircleButton(Icons.Filled.PlayArrow, 72, Modifier.align(Alignment.Center)) {
                    playing = true
                }
            }
        }
    }
}

@Composable
private fun VideoPlayer(uri: Uri) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player.pause()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.release()
        }
    }
    AndroidView(
        factory = { PlayerView(it).apply { this.player = player } },
        modifier = Modifier.fillMaxSize(),
    )
}

/** The outlined-circle icon button used in Windows Phone app bars. */
@Composable
private fun CircleButton(
    icon: ImageVector,
    size: Int,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .border(2.dp, tint, CircleShape)
            .wpPress(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size((size / 2).dp))
    }
}

/** Pinch to zoom, drag to pan once zoomed, double-tap to toggle. Single-finger swipes at 1x fall through to the pager. */
@Composable
private fun ZoomableImage(item: MediaItem, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current
    val request = remember(item.id) {
        ImageRequest.Builder(context).data(item.uri).size(2400).build()
    }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1 || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, 6f)
                            val maxX = size.width * (newScale - 1f) / 2f
                            val maxY = size.height * (newScale - 1f) / 2f
                            offset = if (newScale == 1f) Offset.Zero else {
                                val moved = offset + event.calculatePan()
                                Offset(moved.x.coerceIn(-maxX, maxX), moved.y.coerceIn(-maxY, maxY))
                            }
                            scale = newScale
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}
