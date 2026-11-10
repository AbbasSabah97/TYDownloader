# Sonic MP3 — Android

## Version 0.2.0
- Kotlin + Jetpack Compose + Material 3; Android 11+.
- Real MP3 encoding using the FFmpegKit maintained Android build with LAME, selectable 128/192/256/320 kbps.
- Extracts the first audio stream from supported local video/audio files.
- Saves converted files to Music/Sonic MP3.
- MediaStore-based music library and in-app playback using AndroidX Media3 ExoPlayer.
- Background conversion and direct downloads with WorkManager.
- Progress display, cancellation, conversion restart, and direct-download resume when the HTTP server supports Range requests.
- Direct downloads saved to Downloads/Sonic MP3.
- Arabic/English UI and dark appearance.

## GitHub Actions APK
Every push to main triggers Sonic MP3 Android CI. Open Actions, select the latest successful run, and download the Sonic-MP3-debug-apk artifact.

## Local build
Requires JDK 17, Android SDK 35 and Gradle 8.9:
```bash
gradle --no-daemon assembleDebug
```

## Behavior notes
- Conversion cancellation stops the encoder. Restarting a cancelled conversion begins again from the start.
- Direct-download resume continues from the saved partial file only when the server responds with HTTP 206 Partial Content. If the server ignores Range, the transfer restarts safely.
- Playback is currently available while Sonic MP3 is open. Background playback with a media notification is a later enhancement.
- Direct download accepts file URLs only. It does not extract streams from YouTube or bypass platform restrictions.
- 320 kbps is an encoding target; it cannot restore audio detail that is absent from the source.

## Third-party licensing
The FFmpegKit maintained full artifact is distributed under LGPL-3.0 and includes LAME. Review bundled third-party notices and comply with LGPL and all included component licenses before distributing the APK. No signing key is committed to this repository.
