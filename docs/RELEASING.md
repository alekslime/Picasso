# Releasing Picasso

## Tag a release

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
4. Tag a release: the workflow uploads the base edition's bundle (`./gradlew bundleBaseRelease`) as an artifact —
   download it and drop it on the Console's release track.

The `pro` edition is for testing and sideloading only; don't upload it to Play.

Local release builds without the env vars stay unsigned (normal).
