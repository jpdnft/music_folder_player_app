# Music Folder Player

Music Folder Player is a simple free Android music player built with Kotlin, Jetpack Compose, Material 3, AndroidX Navigation, and Media3 ExoPlayer.

## Current status
This is a first working version focused on a real local-folder music workflow:
- choose a music folder
- browse folders and tracks
- play a selected track
- persist the current folder and playback state
- keep background playback service scaffolding active

## Features
- Folder-based music browsing
- Local playback using ExoPlayer
- Background service and media session support
- Material 3 navigation and screens
- Settings reset and recovery flow

## Tech stack
- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Media3 ExoPlayer
- DataStore preferences
- Storage Access Framework folder picking

## Build
From the project root:

```bash
C:\gradle\run_gradle_8_10.bat --no-daemon -p C:\jpd_music_player :app:compileDebugKotlin --console=plain
```

## Notes
This repo is now in a stable first-version state with the core app flow in place. It is intended as a working local music player foundation suitable for continued iteration and GitHub publication.
