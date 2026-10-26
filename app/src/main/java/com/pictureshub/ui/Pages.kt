package com.pictureshub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pictureshub.data.Album
import com.pictureshub.data.Group
import com.pictureshub.data.MediaItem

@Composable
fun EmptyNote(text: String) {
    Text(text, style = lightText(16, muted = true), modifier = Modifier.padding(start = 20.dp, top = 8.dp))
}

/** Camera roll, videos and a single album: one group of pictures in a tile mosaic. */
@Composable
fun PhotoGridPage(title: String, items: List<MediaItem>, onOpen: (List<MediaItem>, Int) -> Unit) {
    PageScaffold(title) {
        if (items.isEmpty()) EmptyNote("nothing here yet")
        else MosaicList(listOf(Group(title, items)), showHeaders = false, onOpen)
    }
}

/** The same mosaic, broken up under month headings. */
@Composable
fun DatePage(months: List<Group>, onOpen: (List<MediaItem>, Int) -> Unit) {
    PageScaffold("date") {
        if (months.isEmpty()) EmptyNote("nothing here yet")
        else MosaicList(months, showHeaders = true, onOpen)
    }
}

@Composable
fun AlbumsPage(albums: List<Album>, onOpen: (Album) -> Unit) {
    PageScaffold("albums") {
        if (albums.isEmpty()) {
            EmptyNote("no albums yet")
            return@PageScaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                start = 12.dp, end = 12.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(albums, key = { it.id }) { album ->
                Box(Modifier.aspectRatio(1f).background(Accent).wpPress { onOpen(album) }) {
                    Thumb(album.cover, Modifier.fillMaxSize(), 480)
                    Box(Modifier.fillMaxSize().background(ScrimBottom))
                    Text(
                        album.name.lowercase(),
                        style = lightText(20, Color.White),
                        modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    )
                    Text(
                        album.items.size.toString(),
                        style = lightText(14, Color.White),
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsPage(liveBackground: Boolean, onLiveBackground: (Boolean) -> Unit) {
    PageScaffold("settings") {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                "hub background",
                style = lightText(22, Accent),
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            SettingOption(
                "phone theme",
                "plain black or white, following your phone's dark or light mode",
                selected = !liveBackground,
            ) { onLiveBackground(false) }
            SettingOption(
                "live background",
                "a random photo from your camera, picked each time you open the hub",
                selected = liveBackground,
            ) { onLiveBackground(true) }
        }
    }
}

@Composable
private fun SettingOption(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    val fg = LocalPalette.current.fg
    Row(
        Modifier
            .fillMaxWidth()
            .wpPress(onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .border(2.dp, if (selected) Accent else fg)
                .padding(5.dp)
                .background(if (selected) Accent else Color.Transparent)
        )
        Column(Modifier.padding(start = 16.dp)) {
            Text(title, style = lightText(22))
            Text(detail, style = lightText(14, muted = true))
        }
    }
}
