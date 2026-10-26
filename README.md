# Pictures Hub

An Android gallery inspired by the Windows Phone **Pictures Hub** found on Lumia devices: a horizontally scrolling panorama with a large lowercase title, live tiles that keep swapping photos, and flat, edge-to-edge Metro-style pages.

Requires **Android 14 (API 34)** or newer.

## Features

**Hub (panorama)**
- Swipe through four pages: **what's new**, **favorites**, **pinned** and **albums & more**. The oversized "pictures" title drifts slower than the pages for a parallax effect.
- **what's new**: recent additions grouped by day.
- **favorites**: photos you hearted, shown as a tile mosaic.
- **pinned**: documents and IDs you want close at hand, shown whole on cards instead of cropped.
- **albums & more**: live tiles for camera roll, albums, date and videos. Each tile rotates through photos from its set.
- **Settings** is a link on the heading line of the last page.

**Pages**
- Camera roll, videos, date and every album use a packed **tile mosaic**: squares, wide, tall and a few large tiles, edge to edge with no gaps. The layout is the same every time you open a page.
- The date page groups photos under month headings.

**Viewer**
- Swipe between photos, pinch or double-tap to zoom, tap to hide the controls.
- **Videos play in place** with seek and pause controls.
- App bar: favorite, pin, share, delete (the system asks you to confirm deletes).

**Background**
- **Phone theme** (default): black in dark mode, white in light mode, with text and cards following.
- **Live background**: one random camera photo behind the hub, chosen each time you enter it (on app start, or when coming back from another page).

**Android 14 photo access**
- Handles full access and the "select photos" choice. With partial access it shows what you allowed and offers a link to choose more.

## Differences from the original

| Original | Here |
|---|---|
| "people" section (Facebook galleries) | Replaced by **videos** |
| "what's new" social feed | Recent additions grouped by day |
| Segoe WP font | System light font |
| Custom albums were not possible | Albums are your device folders |

Favorites and pins are stored inside the app, not in the phone's gallery data.

## Build

Requirements: JDK 17 to 23 (Gradle 8.13 does not run on newer JDKs), Android SDK with platform 35.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

In Android Studio, set **Settings > Build Tools > Gradle > Gradle JDK** to a JDK 17 to 23. Android Studio's bundled runtime may be newer than Gradle supports.

## Project layout

```
app/src/main/java/com/pictureshub/
  MainActivity.kt        permissions, navigation, system bars, share and delete
  GalleryViewModel.kt    screen state, library loading, favorites, pins, settings
  data/Media.kt          MediaStore query, grouping into albums/months/days, persistence
  ui/Common.kt           palettes, thumbnails, press tilt, page scaffold
  ui/Hub.kt              panorama pages and live tiles
  ui/Mosaic.kt           tile packing and mosaic list
  ui/Pages.kt            camera roll, date, albums and settings pages
  ui/Viewer.kt           full-screen viewer, zoom, video playback
LUMIA_PHOTOS_SPEC.md     research notes on the original Pictures Hub
```

## Built with

Kotlin, Jetpack Compose, [Coil](https://coil-kt.github.io/coil/) for images and video frames, and [Media3 ExoPlayer](https://developer.android.com/media/media3) for playback.
