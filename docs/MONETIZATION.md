# Monetization setup

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
