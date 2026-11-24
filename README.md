![Picasso](assets/readme-banner.png)

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

## Play Store release signing (one-time setup)

Play takes a **signed `.aab`**, built automatically on tags once these repo
secrets exist (Settings → Secrets → Actions). Nothing secret is committed.

1. Generate the upload key (keep the passwords somewhere safe, e.g. a
   password manager — losing them means a new app listing):
   ```sh
   keytool -genkeypair -v -keystore picasso-upload.jks -alias picasso \
     -keyalg RSA -keysize 2048 -validity 9125
   ```
2. Enroll in **Play App Signing** in Play Console (Setup → App signing) —
   Google then holds the real signing key and the upload key above can be
   reset any time.
3. Add the secrets: `PICASSO_KEYSTORE_BASE64` (`base64 -w0
   picasso-upload.jks`), `PICASSO_KEYSTORE_PASSWORD`, `PICASSO_KEY_ALIAS`
   (`picasso`), `PICASSO_KEY_PASSWORD`.
4. Tag a release: the workflow uploads `app-release.aab` as an artifact —
   download it and drop it on the Console's release track.

Local release builds without the env vars stay unsigned (normal).

## Monetization setup (ads, billing, login)

The app compiles and runs without any of this — each backend degrades
gracefully (test ads, sign-in disabled notice, billing unavailable notice).
Fill these in before a store release:

1. **Google sign-in**: Cloud Console → APIs & Services → Credentials →
   create an **OAuth 2.0 Client ID of type Web application** (not Android).
   Paste it as `google_server_client_id` in
   `app/src/main/res/values/monetization.xml`.
2. **AdMob**: create the app in the AdMob console, then a banner unit.
   Replace the demo `APPLICATION_ID` in `AndroidManifest.xml` and
   `admob_banner_id` in `monetization.xml`. Under AdMob → Privacy &
   messaging, configure the EEA consent message (the app shows it via UMP).
3. **Billing**: Play Console → Monetize → In-app products → create a
   one-time product with ID `picasso_pro` (or change `pro_product_id` in
   `monetization.xml` to match). Add license-test accounts under Setup →
   License testing and test with those accounts first.
4. **Account deletion** (Play requirement, already in-app): Settings →
   Delete account erases the stored profile; the policy above states what
   stays (on-device tracings) and why.

## Highlights

- Clean-camera trace deck with a full control panel one tap away
- Filters: original, grayscale, high contrast, outline, value study
- Guides: grid, thirds, golden ratio, diagonal (1dp thin lines)
- AR anchor (beta): overlay stays on the paper as the phone moves
- **Training wheels:** the reference fades out as you improve, from the full image to values, outline, guides only, and finally nothing
- Overlay layers: add a reference, swap it with the base or trace over it, fade it or rename it
- Timelapse frames, before/after gallery export, session recording
- Library with samples, favorites, search, sort, rename/delete
- "Continue last tracing" home-screen widget
