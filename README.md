# Vinyl Remote

Android turntable-style remote controller for Spotify / YouTube Music sessions.

## 1. Project Summary

Vinyl Remote is a tactile UI experiment:
- Tonearm drag controls playback state (needle in/out)
- Platter animation and tonearm movement stay synced with media position
- Lyrics panel supports plain text and LRC line sync
- Notification/lockscreen/widget controls mirror playback + tonearm intent

This app does not stream audio directly.  
It reads and controls active external media sessions via Android MediaSession/Notification Listener.

## 2. Core Features

- Realistic turntable interaction
- Needle drag -> play/pause + seek mapping
- Platter spin-up/spin-down animation
- Tonearm 3-layer metallic lighting (base/shadow/highlight)
- Scratch SFX + haptic drop feedback
- LRC synced lyrics (offset tag support)
- Theme system: `SILVER`, `BLACK`, `BRONZE`
- Material texture upgrades
- Silver brushed metal / Black carbon-like / Bronze wood-grain deck texture
- Foldable presets
- Flip cover-style compact layout
- Flip open / phone / tablet adaptive layout
- External controls
- Notification + lockscreen transport actions
- Home screen widget actions

## 3. Tech Stack

- Kotlin
- Jetpack Compose (Material3)
- Android MediaSession / NotificationListenerService
- Coroutines + StateFlow
- Gradle (KTS)

## 4. Architecture

- `VinylViewModel`: playback state sync, needle/seek logic, lyrics fetch/cache, external controls publish
- `MainActivity` + Compose UI: turntable rendering, gestures, responsive layout presets
- `ExternalMediaSessionController`: finds and prioritizes active player sessions
- `LyricsProvider`: lyrics lookup (plain + synced LRC-aware parsing)
- `VinylExternalControls`: notification + widget rendering/actions

## 5. Run Locally

1. Open in Android Studio
2. Enable Notification Access for this app
3. Run:
   - `./gradlew :app:assembleDebug`
4. Install APK:
   - `app/build/outputs/apk/debug/app-debug.apk`

## 6. Portfolio Demo Checklist

- Needle rest -> needle in -> playback starts
- Seek slider updates tonearm position live
- LRC lyrics highlight follows track progression
- Theme switch (`SILVER/BLACK/BRONZE`) updates material look
- Foldable emulator: cover/open layout difference
- Notification + widget: prev/play-next + needle in/out control

## 7. Limitations

- Behavior depends on session metadata/actions exposed by each music app
- Some apps may not expose seek or lyric metadata
- Online lyrics availability can vary by track

## 8. Screenshots / Demo


```md
![Main UI](docs/screenshot-main.png)
![Flip Layout](docs/video.mp4)
```
