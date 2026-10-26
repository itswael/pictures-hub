# Windows Phone "Pictures Hub" — Reference Spec

Purpose: describe the Pictures Hub from Windows Phone 7 / 7.5 (Mango) / 8 and Lumia devices so it can be recreated as a web or app gallery.

**Confidence key:** [S] = described in reviews/sources found while researching; [R] = recalled from the app, not verified online. Treat [R] items as a design brief, not documentation.

Naming: **Pictures Hub** on WP7 / 7.5, renamed **Photos hub** in WP8, replaced by a separate **Photos** app in WP8.1 and Windows 10 Mobile. GSMArena's WP8 reviews call it "pretty much the same as we left it in Windows Phone 7.5". [S]

---

## 1. The structure: a Panorama

The hub is a **Panorama control**: one wide canvas, wider than the screen, that you swipe horizontally. The big lowercase title ("pictures") sits behind the content and scrolls slower than the sections (parallax), and the next section's heading peeks in at the right edge to invite a swipe. [R]

### Section order
| # | Section | What it holds |
|---|---|---|
| 1 | **main view** (hub home) | Four entry tiles: **camera roll**, **albums**, **date**, **people**. [S] |
| 2 | **what's new** | Feed of new galleries from Facebook friends and liked pages (later SkyDrive). [S] |
| 3 | **favorites** | Tiled view of photos you've marked as favorites. [S] |

(WP7.5 reviews describe "a flick left shows all pictures sorted by date" and "swipe left reveals what's new", so the exact swipe order differs slightly between reviews and OS versions. [S] I treat main → what's new → favorites as the base order.)

---

## 2. The signature look: hub background

- The whole panorama background is **one full-bleed photo** behind the sections. [S]
- WP7: it was the last photo you viewed. Mango (7.5): you can **pin one photo or have it shuffle**. Favorites are preferred when the system picks. [S]
- Section content sits on top as **flat tiles** with a translucent dark overlay where text needs contrast. [R]
- Result: every hub looked different, and your own pictures were the UI. This is the "live, beautifully placed" feeling.

---

## 3. Section details

### 3.1 Main view tiles
- Four large square-ish tiles in a 2×2 arrangement: **camera roll, albums, date, people**. [S for names; R for 2×2]
- Each tile shows a **thumbnail from its contents with the label at the bottom-left** in small lowercase Segoe. [R]
- Tiles animate: images swap/flip on a staggered timer so the page looks alive. [R]

### 3.2 Camera roll
- All photos and videos taken on the device in a **fixed 4×5 grid** of square thumbnails, no layout option. [S]
- Scrolls vertically within its page; videos carry a play glyph. [R]

### 3.3 Albums
- List/grid of albums: **Camera Roll, Saved Pictures, preloaded system pictures, and Facebook albums**. [S]
- You **couldn't assign camera-roll photos to custom albums**. Albums came from the device or accounts. [S]
- Each entry: cover thumbnail + album name + count. [R]

### 3.4 Date
- All pictures sorted by date, grouped under month/day headers, thumbnails in a tight grid. [S for sorting, R for headers]

### 3.5 People
- Browse photo galleries by contact or group, mainly Facebook galleries. Added in Mango. Reviews note it's empty if you don't use Facebook. [S]

### 3.6 What's new
- Vertical feed of cards: friend's name/avatar, a few thumbnails from a newly posted gallery, comment count. Tap to open the gallery, with comments and like. [R]

### 3.7 Favorites
- Photos you marked, shown as a tile mosaic. Used first for hub backgrounds. [S]

---

## 4. Viewer (single photo)

- Full-screen on black, chrome hidden. Swipe left/right moves through the **current set** (album, date, favorites). [R]
- Pinch and double-tap to zoom, drag to pan. [R]
- Press-and-hold on a photo opens a context menu: **add to favorites, delete, share, use as wallpaper/lock screen, upload to SkyDrive/Facebook**. [R]
- Bottom app bar with 3–4 outlined-circle icons + `…` overflow. [R]
- Slideshow available from the overflow menu. [R]

---

## 5. Visual language (Metro)

- **Grid:** 25 px unit squares with 12 px gutters; large tile = 6×6 squares. [S]
- **Typeface:** Segoe WP, light weight for the huge panorama title, lowercase headers. [S for font, R for weight/casing]
- **Flat design:** no gloss, no shadows, square corners, content edge-to-edge. [R]
- **Accent color:** user-chosen theme color for selected states and highlights; dark theme default. [R]
- **Motion:** turnstile / slide-in on page entry, tiles tilt slightly when pressed, thumbnails fade in on load, panorama title moves slower than content. [R]
- **Live tile on Start:** flips through recent or favorite photos; wide tile shows a large image. [S for tile types, R for behavior]

---

## 6. Feature checklist

- Panorama navigation with parallax title and peeking next section
- Full-bleed hub background (pinned / shuffle)
- Camera roll fixed 4×5 grid · albums · date · people · what's new · favorites
- Facebook and SkyDrive integration (browse, comment, like, upload)
- Favorites, multi-select, delete, share, wallpaper / lock-screen
- Pinch-zoom viewer with swipe between photos
- Live Start tile

Lumia-era additions (WP8): Photos Extras and Lenses, Creative Studio, Smart Shoot, Cinemagraph, Refocus, Storyteller, Rich Capture. [R, partly S for Storyteller]

---

## 7. Suggested web recreation (proposed, not from MS docs)

| Concern | Approach |
|---|---|
| Shell | Horizontal scroll-snap container (`scroll-snap-type: x mandatory`), sections ≈ 85% viewport width so the next one peeks |
| Parallax title | Giant lowercase "pictures" in fixed layer, `translateX` at ~0.3× scroll speed |
| Background | Full-bleed photo (pinned or shuffled every N s with crossfade), dark overlay behind text |
| Main tiles | 2×2 grid, 12 px gutters, thumbnails rotate on staggered timers |
| Camera roll | 4-column square grid, lazy-loaded thumbs, play badge for video |
| Date | Sticky month headers, same grid |
| Albums | Folders from the chosen directory as cover tiles |
| Favorites | Heart toggle stored locally, mosaic of 1×1 / 2×2 tiles |
| What's new | Optional: recently added items grouped by day as feed cards |
| Viewer | Fullscreen overlay, swipe, pinch-zoom, long-press menu |
| Theme | Dark, accent CSS variable, Segoe UI stack, 0 radius, 150–250 ms ease-out turnstile |

---

## 8. Sources

- Nokia Lumia 800 review, Pictures Hub: https://allaboutwindowsphone.com/reviews/item/14938_Lumia_800_Part_6-Multimedia_Hu.php
- GSMArena Lumia 900 review (Pictures hub, Mango): https://www.gsmarena.com/nokia_lumia_900-review-782p5.php
- GSMArena Lumia 920 review (Photos hub, WP8): https://m.gsmarena.com/nokia_lumia_920-review-858p6.php
- GSMArena Lumia 928 review: https://m.gsmarena.com/nokia_lumia_928-review-937p5.php
- Windows Phone 8.1 Start backgrounds (Photo Hub, crop gesture): https://blogs.windows.com/devices/2014/04/29/deep-dive-windows-phone-8-1-start-screen-backgrounds/
- Metro grid and tile sizes: https://assets.blinkux.com/metro/winphone.html , https://hub.packtpub.com/windows-phone-8-principles-uiux-and-bindings
- Later Photos app and Storyteller: https://windowscentral.com/photo-add-ins-microsofts-photography-plan-windows-10-mobile , https://windowscentral.com/lumia-storyteller-picks-update-windows-phone
