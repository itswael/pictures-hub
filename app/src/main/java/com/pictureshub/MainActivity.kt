package com.pictureshub

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pictureshub.data.MediaItem
import com.pictureshub.ui.AlbumsPage
import com.pictureshub.ui.Accent
import com.pictureshub.ui.DatePage
import com.pictureshub.ui.HubScreen
import com.pictureshub.ui.LocalPalette
import com.pictureshub.ui.SettingsPage
import com.pictureshub.ui.PhotoGridPage
import com.pictureshub.ui.PicturesTheme
import com.pictureshub.ui.ViewerScreen
import com.pictureshub.ui.lightText
import com.pictureshub.ui.wpPress

private enum class Access { Full, Partial, None }

private val PERMISSIONS = arrayOf(
    Manifest.permission.READ_MEDIA_IMAGES,
    Manifest.permission.READ_MEDIA_VIDEO,
    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
)

private fun accessLevel(context: Context): Access {
    fun granted(p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    return when {
        granted(Manifest.permission.READ_MEDIA_IMAGES) &&
            granted(Manifest.permission.READ_MEDIA_VIDEO) -> Access.Full
        granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> Access.Partial
        else -> Access.None
    }
}

class MainActivity : ComponentActivity() {
    private val vm: GalleryViewModel by viewModels()

    private val deleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) vm.onDeleted() else vm.pendingDeleteId = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { PicturesTheme { Root() } }
    }

    @Composable
    private fun Root() {
        val context = LocalContext.current
        var access by remember { mutableStateOf(accessLevel(context)) }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            access = accessLevel(context)
            vm.refresh()
        }

        LaunchedEffect(Unit) {
            if (access == Access.None) permissionLauncher.launch(PERMISSIONS)
        }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    access = accessLevel(context)
                    if (access != Access.None) vm.refresh()
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }

        val library by vm.library.collectAsState()
        val favoriteIds by vm.favoriteIds.collectAsState()
        val favorites = remember(library, favoriteIds) { library.all.filter { it.id in favoriteIds } }
        val pinnedIds by vm.pinnedIds.collectAsState()
        val pinned = remember(library, pinnedIds) { library.all.filter { it.id in pinnedIds } }
        val liveBackground by vm.liveBackground.collectAsState()

        // Light status/navigation icons whenever the screen behind them is dark.
        val barsDark = isSystemInDarkTheme() ||
            vm.viewer != null || (liveBackground && vm.screen == Screen.Hub)
        val view = LocalView.current
        SideEffect {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !barsDark
                isAppearanceLightNavigationBars = !barsDark
            }
        }

        BackHandler(enabled = vm.viewer != null || vm.screen != Screen.Hub) {
            val screen = vm.screen
            when {
                vm.viewer != null -> vm.viewer = null
                screen is Screen.AlbumDetail -> vm.screen = Screen.Albums
                else -> vm.screen = Screen.Hub
            }
        }

        Box(Modifier.fillMaxSize().background(LocalPalette.current.bg)) {
            if (access == Access.None) {
                PermissionPage { permissionLauncher.launch(PERMISSIONS) }
            } else {
                AnimatedContent(
                    targetState = vm.screen,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInHorizontally(tween(260)) { it / 10 }) togetherWith
                            fadeOut(tween(140))
                    },
                    label = "screen",
                ) { screen ->
                    when (screen) {
                        Screen.Hub -> HubScreen(
                            library = library,
                            favorites = favorites,
                            pinned = pinned,
                            partialAccess = access == Access.Partial,
                            liveBackground = liveBackground,
                            onNavigate = { vm.screen = it },
                            onOpen = vm::open,
                            onSelectMore = { permissionLauncher.launch(PERMISSIONS) },
                        )
                        Screen.CameraRoll -> PhotoGridPage("camera roll", library.camera, vm::open)
                        Screen.Videos -> PhotoGridPage("videos", library.videos, vm::open)
                        Screen.Date -> DatePage(library.months, vm::open)
                        Screen.Settings -> SettingsPage(liveBackground, vm::setLiveBackground)
                        Screen.Albums -> AlbumsPage(library.albums) {
                            vm.screen = Screen.AlbumDetail(it.id, it.name)
                        }
                        is Screen.AlbumDetail -> PhotoGridPage(
                            screen.name,
                            library.albums.firstOrNull { it.id == screen.id }?.items.orEmpty(),
                            vm::open,
                        )
                    }
                }

                AnimatedContent(
                    targetState = vm.viewer,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "viewer",
                ) { viewer ->
                    if (viewer != null) {
                        ViewerScreen(
                            state = viewer,
                            favorites = favoriteIds,
                            pinned = pinnedIds,
                            onToggleFavorite = { vm.toggleFavorite(it.id) },
                            onTogglePin = { vm.togglePin(it.id) },
                            onShare = ::share,
                            onDelete = ::requestDelete,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PermissionPage(onAllow: () -> Unit) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            Text("pictures", style = lightText(64))
            Text(
                "allow access to your photos and videos to see them here.",
                style = lightText(18, muted = true),
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )
            Text(
                "allow access",
                style = lightText(20, Color.White),
                modifier = Modifier
                    .background(Accent)
                    .wpPress(onAllow)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }

    private fun share(item: MediaItem) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = item.mime
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, null))
    }

    private fun requestDelete(item: MediaItem) {
        vm.pendingDeleteId = item.id
        val sender = MediaStore.createDeleteRequest(contentResolver, listOf(item.uri)).intentSender
        deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }
}
