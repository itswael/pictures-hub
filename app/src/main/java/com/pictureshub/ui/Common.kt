package com.pictureshub.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pictureshub.data.MediaItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon

val Accent = Color(0xFF1BA1E2)
val Muted = Color(0xFFB4B4B4)
val Placeholder = Color(0xFF1E1E1E)

data class Palette(
    val bg: Color,
    val fg: Color,
    val muted: Color,
    val card: Color,
    val dark: Boolean,
)

val DarkPalette = Palette(Color.Black, Color.White, Muted, Color(0xFF1B1B1B), true)
val LightPalette = Palette(Color.White, Color.Black, Color(0xFF6B6B6B), Color(0xFFEDEDED), false)

/** Text sits directly on a photo, so it stays light and the cards go translucent. */
val PhotoPalette = DarkPalette.copy(card = Color.Black.copy(alpha = 0.45f))

val LocalPalette = staticCompositionLocalOf { DarkPalette }

/** Light-weight text in the palette's foreground, or its muted tone, unless [color] is given. */
@Composable
fun lightText(size: Int, color: Color = Color.Unspecified, muted: Boolean = false): TextStyle {
    val palette = LocalPalette.current
    return TextStyle(
        fontSize = size.sp,
        fontWeight = FontWeight.Light,
        color = when {
            color != Color.Unspecified -> color
            muted -> palette.muted
            else -> palette.fg
        },
    )
}

@Composable
fun PicturesTheme(content: @Composable () -> Unit) {
    val palette = if (isSystemInDarkTheme()) DarkPalette else LightPalette
    val scheme = if (palette.dark) {
        darkColorScheme(primary = Accent, background = palette.bg, surface = palette.bg)
    } else {
        lightColorScheme(primary = Accent, background = palette.bg, surface = palette.bg)
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Square-cropped thumbnail, decoded near [size] px so large grids stay light. */
@Composable
fun Thumb(
    item: MediaItem,
    modifier: Modifier = Modifier,
    size: Int = 320,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val request = remember(item.id, size) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(size)
            .crossfade(180)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier.background(Placeholder),
    )
}

/** Thumbnail plus the play glyph and duration that videos carry in the grid. */
@Composable
fun MediaCell(item: MediaItem, modifier: Modifier = Modifier, size: Int = 320) {
    Box(modifier) {
        Thumb(item, Modifier.fillMaxSize(), size)
        if (item.isVideo) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Filled.PlayArrow, null,
                    tint = Color.White,
                    modifier = Modifier.padding(2.dp),
                )
            }
            Text(
                formatDuration(item.durationMs),
                style = lightText(11, Color.White),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Windows Phone press feedback: the element tilts toward the touch point and shrinks slightly. */
fun Modifier.wpPress(onClick: () -> Unit): Modifier = composed {
    var press by remember { mutableStateOf<Offset?>(null) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    val amount by animateFloatAsState(if (press != null) 1f else 0f, tween(100), label = "press")
    val last = remember { mutableStateOf(Offset.Zero) }
    val click by rememberUpdatedState(onClick)
    this
        .onSizeChanged { box = it }
        .graphicsLayer {
            val p = press ?: last.value
            val nx = if (box.width > 0) p.x / box.width - 0.5f else 0f
            val ny = if (box.height > 0) p.y / box.height - 0.5f else 0f
            rotationY = nx * 16f * amount
            rotationX = -ny * 16f * amount
            val s = 1f - 0.05f * amount
            scaleX = s
            scaleY = s
            cameraDistance = 12f * density
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    last.value = it
                    press = it
                    tryAwaitRelease()
                    press = null
                },
                onTap = { click() },
            )
        }
}

/** Standard sub-page: small app title, large lowercase page name, content below. */
@Composable
fun PageScaffold(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(LocalPalette.current.bg)
            .statusBarsPadding()
    ) {
        Text(
            "PICTURES",
            style = lightText(12, muted = true).copy(letterSpacing = 1.5.sp),
            modifier = Modifier.padding(start = 20.dp, top = 14.dp),
        )
        Text(
            title.lowercase(),
            style = lightText(44),
            modifier = Modifier.padding(start = 18.dp, bottom = 10.dp),
        )
        content()
    }
}

val ScrimBottom = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))
