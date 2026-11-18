# Android Google Drive OAuth setup

Android Google Drive sign-in uses AppAuth with the system browser and the OAuth authorization code flow with PKCE. The app uses Google's public OAuth client ID and does not embed a client secret or depend on Google Play Services.

## Google Cloud setup

The production Android build uses this client ID:

```text
367084333033-n8de4dv4lg43g8rmotk6c0op179egapu.apps.googleusercontent.com
```

Configure the Google OAuth client for the ArchiveKeep Android application ID and the signing certificate fingerprints for the build being used. To find the local debug certificate fingerprint, run `./gradlew :app-android:signingReport`; register that fingerprint on the matching Android OAuth client in Google Cloud Console. In that client's advanced settings, enable **Custom URI scheme**; Google disables this redirect method by default. The redirect URI uses the reverse client-ID scheme and `/oauth2redirect` path, so the setting must be enabled on the exact client ID used by the build.

## Development client ID

To use a separate OAuth client for development, add this property to the root `local.properties` file, which should not be committed:

```properties
googleDriveClientId=YOUR_DEVELOPMENT_CLIENT_ID.apps.googleusercontent.com
```

The Android build uses the override both for browser authorization and for refreshing access tokens; builds without the override use the production client above. The development client must also have custom URI schemes enabled and be configured for the app's package and signing certificate. A package, certificate, redirect-scheme, or custom-URI-scheme mismatch can cause Google to reject the browser authorization request with `400: invalid_request`.