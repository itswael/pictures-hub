package com.pictureshub.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val taken: Long,
    val added: Long,
    val bucketId: Long,
    val bucket: String,
    val folder: String,
    val mime: String,
    val durationMs: Long,
)

data class Album(val id: Long, val name: String, val items: List<MediaItem>) {
    val cover: MediaItem get() = items.first()
}

data class Group(val label: String, val items: List<MediaItem>)

data class Library(
    val loaded: Boolean = false,
    val all: List<MediaItem> = emptyList(),
    val camera: List<MediaItem> = emptyList(),
    val videos: List<MediaItem> = emptyList(),
    val albums: List<Album> = emptyList(),
    val news: List<Group> = emptyList(),
    val months: List<Group> = emptyList(),
)

private fun isCameraFolder(folder: String) = folder.startsWith("DCIM/Camera", ignoreCase = true)

fun buildLibrary(all: List<MediaItem>): Library {
    val zone = ZoneId.systemDefault()
    val camera = all.filter { isCameraFolder(it.folder) }.ifEmpty { all }
    val videos = all.filter { it.isVideo }

    val albums = all.groupBy { it.bucketId }
        .map { (id, list) -> Album(id, list.first().bucket.ifBlank { "unknown" }, list) }
        .sortedWith(
            compareByDescending<Album> { isCameraFolder(it.items.first().folder) }
                .thenByDescending { it.items.first().taken }
        )

    val today = LocalDate.now(zone)
    val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM")
    val news = all
        .groupBy { Instant.ofEpochMilli(it.added).atZone(zone).toLocalDate() }
        .toSortedMap(reverseOrder())
        .entries.take(14)
        .map { (day, list) ->
            val label = when (day) {
                today -> "today"
                today.minusDays(1) -> "yesterday"
                else -> day.format(dayFormat)
            }
            Group(label, list)
        }

    val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy")
    val months = all
        .groupBy { YearMonth.from(Instant.ofEpochMilli(it.taken).atZone(zone)) }
        .map { (ym, list) -> Group(ym.format(monthFormat), list) }

    return Library(true, all, camera, videos, albums, news, months)
}

class MediaRepository(private val resolver: ContentResolver) {

    fun load(): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        val projection = arrayOf(
            FileColumns._ID,
            FileColumns.MEDIA_TYPE,
            MediaColumns.DATE_TAKEN,
            MediaColumns.DATE_ADDED,
            MediaColumns.BUCKET_ID,
            MediaColumns.BUCKET_DISPLAY_NAME,
            MediaColumns.RELATIVE_PATH,
            MediaColumns.MIME_TYPE,
            MediaColumns.DURATION,
        )
        val selection = "${FileColumns.MEDIA_TYPE} IN (?, ?)"
        val args = arrayOf(
            FileColumns.MEDIA_TYPE_IMAGE.toString(),
            FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
        resolver.query(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            projection, selection, args, null,
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(FileColumns._ID)
            val iType = c.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val iTaken = c.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
            val iAdded = c.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
            val iBucketId = c.getColumnIndexOrThrow(MediaColumns.BUCKET_ID)
            val iBucket = c.getColumnIndexOrThrow(MediaColumns.BUCKET_DISPLAY_NAME)
            val iPath = c.getColumnIndexOrThrow(MediaColumns.RELATIVE_PATH)
            val iMime = c.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
            val iDuration = c.getColumnIndexOrThrow(MediaColumns.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(iId)
                val video = c.getInt(iType) == FileColumns.MEDIA_TYPE_VIDEO
                val addedMs = c.getLong(iAdded) * 1000L
                val takenMs = c.getLong(iTaken).takeIf { it > 0 } ?: addedMs
                val base = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                out += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(base, id),
                    isVideo = video,
                    taken = takenMs,
                    added = addedMs,
                    bucketId = c.getLong(iBucketId),
                    bucket = c.getString(iBucket).orEmpty(),
                    folder = c.getString(iPath).orEmpty(),
                    mime = c.getString(iMime) ?: if (video) "video/*" else "image/*",
                    durationMs = c.getLong(iDuration),
                )
            }
        }
        out.sortByDescending { it.taken }
        return out
    }
}

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _liveBackground = MutableStateFlow(prefs.getBoolean(LIVE, false))

    /** False follows the phone's light/dark theme; true shows a random photo behind the hub. */
    val liveBackground: StateFlow<Boolean> = _liveBackground

    fun setLiveBackground(value: Boolean) {
        _liveBackground.value = value
        prefs.edit().putBoolean(LIVE, value).apply()
    }

    private companion object {
        const val LIVE = "live_background"
    }
}

/** A persisted set of media ids; backs both favorites and pins. */
class IdSet(context: Context, name: String) {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val _ids = MutableStateFlow(
        prefs.getStringSet(KEY, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
    )
    val ids: StateFlow<Set<Long>> = _ids

    fun toggle(id: Long) {
        val next = _ids.value.let { if (id in it) it - id else it + id }
        _ids.value = next
        prefs.edit().putStringSet(KEY, next.map { it.toString() }.toSet()).apply()
    }

    private companion object {
        const val KEY = "ids"
    }
}
