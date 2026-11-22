# Picasso

Trace anything, freehand. Picasso shows the live camera under a translucent
reference image so you can trace it onto paper — with filters, proportion
guides, drawing steps, a compare flash, timelapse frames and recording.

**Design:** Ink `#171717` · Surface `#242424` · Paper `#F1EBDD` · Ochre `#E99A52`
— Manrope / Inter / JetBrains Mono, 20dp corners, minimal thin-line guides.
Figma: https://www.figma.com/design/ljeyvNGO9gyLMxrjECzr7Z

## Download

Grab the APK from [Releases](https://github.com/alekslime/Picasso/releases),
or from CI artifacts on any green build. A copy is also kept at
`apk/Picasso-debug-1.0.0.apk` in the repo.

## Build

```sh
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

JDK 17, Android SDK 36. 

## Release

```sh
git tag v1.1.0
git push origin v1.1.0   # GitHub Actions builds and publishes the Release
```

## Highlights

- Clean-camera trace deck with a full control panel one tap away
- Filters: original, grayscale, high contrast, outline, value study
- Guides: grid, thirds, golden ratio, diagonal (1dp thin lines)
- AR anchor (beta): overlay stays on the paper as the phone moves
- Timelapse frames, before/after gallery export, session recording
- Library with samples, favorites, search, sort, rename/delete
- "Continue last tracing" home-screen widget
