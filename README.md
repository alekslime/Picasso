<p align="center">
  <img src="assets/readme-banner.png" alt="Picasso — Trace anything, freehand." width="100%">
</p>

<p align="center">
  <a href="https://github.com/alekslime/Picasso/releases"><img alt="Release" src="https://img.shields.io/github/v/release/alekslime/Picasso?include_prereleases&color=E99A52&labelColor=242424"></a>
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-E99A52?labelColor=242424&logo=android&logoColor=F1EBDD">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-E99A52?labelColor=242424&logo=kotlin&logoColor=F1EBDD">
  <a href="https://github.com/alekslime/Picasso/actions"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/alekslime/Picasso/build.yml?color=E99A52&labelColor=242424"></a>
</p>

# Picasso

**Trace anything, freehand.**

Picasso is an Android app that shows your live camera feed *under* a translucent reference image, so you can trace it onto real paper. Point your phone at your sketchbook, line the reference up, and draw. It's a modern take on the lightbox and the camera lucida, with filters, proportion guides, drawing steps, and time-lapse recording built in.

## Features

**Trace**
- Clean camera view with a full control panel one tap away
- Adjustable overlay opacity, so the reference fades in and out over the paper
- Overlay layers: add a reference, swap it with the base, trace over it, fade it, or rename it
- AR anchor *(beta)*: the overlay stays fixed on the paper as you move the phone
- **Training wheels:** the reference fades out as you improve, from the full image to values, outline, guides only, and finally nothing

**See better**
- Filters: original, grayscale, high contrast, outline, and value study
- Guides: grid, thirds, golden ratio, and diagonal, all drawn as 1dp thin lines
- Step-by-step drawing guides

**Keep your work**
- Time-lapse frames and session recording
- Before/after gallery export
- Library with samples, favorites, search, sort, and rename/delete
- "Continue last tracing" home-screen widget

## Download

Get the latest APK from [Releases](https://github.com/alekslime/Picasso/releases), or from the CI artifacts of any green build. A debug copy is also kept in the repo at [`apk/Picasso-debug-1.0.0.apk`](apk/Picasso-debug-1.0.0.apk).

> Requires Android 8.0 (API 26) or newer. The AR anchor feature also needs a device that supports ARCore.

## Build from source

You need JDK 17 and the Android SDK 36.

```sh
./gradlew testDebugUnitTest   # run unit tests
./gradlew assembleDebug       # builds app/build/outputs/apk/debug/app-debug.apk
```

## Tech stack

| | |
|---|---|
| Language & UI | Kotlin, Jetpack Compose, Material 3 |
| Camera | CameraX (preview, video recording) |
| AR | ARCore |
| Architecture | ViewModels, Hilt, Navigation Compose |
| Storage | Room, DataStore, Coil |
| Monetization | Play Billing, AdMob (with UMP consent), Google sign-in |

## Design

Picasso uses a small, quiet palette so the camera and the reference stay the focus.

| Ink | Surface | Paper | Ochre |
|:---:|:---:|:---:|:---:|
| `#171717` | `#242424` | `#F1EBDD` | `#E99A52` |

Type is set in Manrope, Inter, and JetBrains Mono, with 20dp corners and thin-line guides throughout. The full design is in [Figma](https://www.figma.com/design/ljeyvNGO9gyLMxrjECzr7Z).

## Privacy

Tracings stay on your device. See [PRIVACY.md](PRIVACY.md) for what is stored and what happens when you delete your account.

---

## Maintainer notes

<details>
<summary><strong>Releasing</strong></summary>

```sh
git tag v1.1.0
git push origin v1.1.0   # GitHub Actions builds and publishes the Release
```

</details>

<details>
<summary><strong>Play Store signing (one-time setup)</strong></summary>

Play takes a **signed `.aab`**, built automatically on tags once these repo secrets exist (Settings → Secrets → Actions). Nothing secret is committed.

1. Generate the upload key. Keep the passwords somewhere safe, such as a password manager, because losing them means a new app listing:
   ```sh
   keytool -genkeypair -v -keystore picasso-upload.jks -alias picasso \
     -keyalg RSA -keysize 2048 -validity 9125
   ```
2. Enroll in **Play App Signing** in Play Console (Setup → App signing). Google then holds the real signing key, and the upload key above can be reset at any time.
3. Add these secrets: `PICASSO_KEYSTORE_BASE64` (from `base64 -w0 picasso-upload.jks`), `PICASSO_KEYSTORE_PASSWORD`, `PICASSO_KEY_ALIAS` (`picasso`), and `PICASSO_KEY_PASSWORD`.
4. Tag a release. The workflow uploads `app-release.aab` as an artifact. Download it and drop it on the Console's release track.

Local release builds without these environment variables stay unsigned, which is normal.

</details>

<details>
<summary><strong>Monetization setup (ads, billing, sign-in)</strong></summary>

The app compiles and runs without any of this. Each backend degrades gracefully, with test ads, a "sign-in disabled" notice, and a "billing unavailable" notice. Fill these in before a store release:

1. **Google sign-in:** In Cloud Console go to APIs & Services → Credentials and create an **OAuth 2.0 Client ID of type Web application** (not Android). Paste it as `google_server_client_id` in `app/src/main/res/values/monetization.xml`.
2. **AdMob:** Create the app in the AdMob console, then a banner unit. Replace the demo `APPLICATION_ID` in `AndroidManifest.xml` and `admob_banner_id` in `monetization.xml`. Under AdMob → Privacy & messaging, configure the EEA consent message (the app shows it via UMP).
3. **Billing:** In Play Console go to Monetize → In-app products and create a one-time product with ID `picasso_pro` (or change `pro_product_id` in `monetization.xml` to match). Add license-test accounts under Setup → License testing, and test with those first.
4. **Account deletion** (a Play requirement, already in the app): Settings → Delete account erases the stored profile. [PRIVACY.md](PRIVACY.md) states what stays (on-device tracings) and why.

</details>
