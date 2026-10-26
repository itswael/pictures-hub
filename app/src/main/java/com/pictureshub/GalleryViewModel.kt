package com.pictureshub

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pictureshub.data.IdSet
import com.pictureshub.data.Library
import com.pictureshub.data.MediaItem
import com.pictureshub.data.MediaRepository
import com.pictureshub.data.Settings
import com.pictureshub.data.buildLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Hub : Screen
    data object CameraRoll : Screen
    data object Albums : Screen
    data object Date : Screen
    data object Videos : Screen
    data object Settings : Screen
    data class AlbumDetail(val id: Long, val name: String) : Screen
}

data class ViewerState(val items: List<MediaItem>, val index: Int)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MediaRepository(app.contentResolver)
    private val favorites = IdSet(app, "favorites")
    private val pins = IdSet(app, "pins")

    private val _library = MutableStateFlow(Library())
    val library: StateFlow<Library> = _library
    val favoriteIds: StateFlow<Set<Long>> = favorites.ids
    val pinnedIds: StateFlow<Set<Long>> = pins.ids

    private val settings = Settings(app)
    val liveBackground: StateFlow<Boolean> = settings.liveBackground
    fun setLiveBackground(value: Boolean) = settings.setLiveBackground(value)

    var screen: Screen by mutableStateOf(Screen.Hub)
    var viewer: ViewerState? by mutableStateOf(null)
    var pendingDeleteId: Long? = null

    private var loadJob: Job? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refresh(debounce = true)
    }

    init {
        app.contentResolver.registerContentObserver(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), true, observer,
        )
    }

    fun refresh(debounce: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (debounce) delay(300)
            val items = withContext(Dispatchers.IO) {
                try {
                    repo.load()
                } catch (_: SecurityException) {
                    emptyList()
                }
            }
            _library.value = buildLibrary(items)
        }
    }

    fun toggleFavorite(id: Long) = favorites.toggle(id)
    fun togglePin(id: Long) = pins.toggle(id)

    fun open(items: List<MediaItem>, index: Int) {
        viewer = ViewerState(items, index)
    }

    /** Called after the system confirmed deletion of [pendingDeleteId]. */
    fun onDeleted() {
        val id = pendingDeleteId ?: return
        pendingDeleteId = null
        val v = viewer
        if (v != null) {
            val pos = v.items.indexOfFirst { it.id == id }
            val rest = v.items.filterNot { it.id == id }
            viewer = if (rest.isEmpty()) null
            else ViewerState(rest, pos.coerceIn(0, rest.lastIndex))
        }
        refresh()
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(observer)
    }
}
