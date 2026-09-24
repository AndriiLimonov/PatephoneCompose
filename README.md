# 🎵 Patephone

> **A minimal, modern Android music player built with Jetpack Compose**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)

---

## ✨ What is Patephone?

Patephone is a **clean, no-fuss local music player** for Android. You point it at your music folder, and it just works — no accounts, no cloud, no bloat. Just your files, your way.


---

## 🚀 Features

### 🎧 Core Playback
- **Local file playback** — MP3 & FLAC, right from your device storage
- **MediaSession integration** — system-level media controls and lock screen notifications
- **Background playback** — keep the vibes going even when the screen is off

### 🎨 Smart Artwork
- **Folder-level cover detection** — automatically finds `cover.jpg`, `album.jpg`, `artwork.jpg`, etc.
- **Embedded metadata art** — extract album art directly from file metadata (toggleable)
- **Fallback handling** — graceful degradation when no artwork is found

### 🎛️ Playback Controls
- Play / Pause, Skip Next / Previous
- **Wavy seek bar** — because sliders should have personality
- **Shuffle mode** — with a custom `BetterShuffleOrder` that keeps the current song from being replayed too soon
- **Repeat modes** — Off → All → One → Off cycling

### ⚙️ Settings
- Toggle between folder artwork and metadata-embedded artwork
- Recursive import (coming soon — subdirectories, we're coming for you)

---

## 🛠️ Tech Stack

| Layer                | Technology                         |
|----------------------|------------------------------------|
| **UI**               | Jetpack Compose + Material 3       |
| **Language**         | Kotlin 2.2.10                      |
| **Media Playback**   | AndroidX Media3 (ExoPlayer 1.10.0) |
| **FLAC Support**     | `media3-decode-flac`               |
| **Image Loading**    | Coil Compose 2.5.0                 |
| **State Management** | Coroutines + Flow                  |
| **Preferences**      | DataStore Preferences              |
| **Slider**           | Wavy Slider                        |
| **Document Access**  | AndroidX DocumentFile (SAF)        |

---

## 📐 Architecture


| level 1 | level 2 | level 3 | file / folder |
|---|---|---|---|
| core | | | folder |
| | App.kt | | file |
| | data | | folder |
| | | PlayerState.kt | file |
| | | Song.kt | file |
| | player | | folder |
| | | BetterShuffleOrder.kt | file |
| | | MediaItemBuilder.kt | file |
| | | MusicServiceConnection.kt | file |
| | | PlayerAction.kt | file |
| | | UpdatedService.kt | file |
| | ui | | folder |
| | | theme | folder |
| | | | Color.kt (file) |
| | | | Theme.kt (file) |
| | | | Type.kt (file) |
| features | | | folder |
| | main | | folder |
| | | MainActivity.kt | file |
| | | MainViewModel.kt | file |
| | settings | | folder |
| | | SettingsActivity.kt | file |
| | | SettingsManager.kt | file |

**MVVM + Service** pattern. The app connects to a `MediaSessionService` via `MusicServiceConnection`, exposing player state through `StateFlow`.

---

## 📸 Screenshots

![Patephone Screenshot](https://github.com/user-attachments/assets/7d9c261e-226c-481a-a74b-5c61e210ed81)

---

## 🧪 Testing

Tested on **Android** and **GrapheneOS**.

---

## 📦 Getting Started

### Prerequisites
- Android Studio (latest stable)
- JDK 11+
- Android SDK 24+ (target SDK 36)

### Build & Run

```bash
./gradlew assembleDebug
```

### Project Structure

```
app/src/main/java/com/andrii/patephone/
├── main/                  # MainActivity, App, MainViewModel
├── action/                # MusicServiceConnection, PlayerAction
├── settings/              # SettingsActivity, SettingsManager
├── ui/theme/              # Colors, Theme, Typography
├── BetterShuffleOrder.kt  # Custom shuffle logic
├── MediaItemBuilder.kt    # Metadata extraction & MediaItem creation
├── Song.kt                # Data models
├── UpdatedService.kt      # MediaSessionService + ExoPlayer
└── UriDiskCache.kt        # URI caching (WIP)
```

---

## 🚧 Roadmap

- [ ] **Favorites** — save and manage your favorite tracks
- [ ] **Recursive import** — scan subdirectories for music
- [ ] **Now Playing screen** — full-screen artwork experience
- [ ] **Search** — find tracks by title or artist
- [ ] **Playlists** — create, edit, and organize

---

## 🤝 Contributing

Contributions are welcome! Feel free to open issues or submit pull requests.

---

## 📄 License

This project is open source.

---

## 🙏 Acknowledgements

- [AndroidX Media3](https://developer.android.com/media/media3) — for rock-solid playback
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — for making UI fun again
- [Coil](https://coil-kt.github.io/coil/) — for image loading
- [Wavy Slider](https://github.com/mahozad/wavy-slider) — for the cool slider

---

**Made with ❤️ by [Andrey Limonov](https://github.com/AndriiLimonov)**
