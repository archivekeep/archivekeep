# Desktop Google Drive OAuth setup

The desktop build uses the production Google Drive OAuth client secret by default. To use a different secret, run `./bin/encode-google-drive-client-secret.sh` and provide the replacement when prompted; the script prints a Base64-encoded encrypted value without passing the secret to a subprocess argument.

Pass the resulting value as a Gradle property:

```sh
./gradlew :app-desktop:run -PgoogleDriveClientSecret=BASE64_ENCRYPTED_VALUE
```

Alternatively, set `GOOGLE_DRIVE_CLIENT_SECRET` in the build environment. The Gradle property takes precedence when both are set. Overrides must use the encrypted Base64 format produced by the script.

This is obfuscation, not secure secret storage: the decryption key is included in the build, and the decrypted secret is packaged with the desktop application. Anyone who can inspect the build output or application can recover it; desktop OAuth clients should not rely on this secret for confidentiality.

Google's [OAuth 2.0 documentation for installed applications](https://developers.google.com/identity/protocols/oauth2#installed) states:

> The process results in a client ID and, in some cases, a client secret, which you embed in the source code of your application. (In this context, the client secret is obviously not treated as a secret.)

Google's [OAuth 2.0 documentation for desktop apps](https://developers.google.com/identity/protocols/oauth2/native-app) also explains:

> Desktop applications are installed locally on a user's machine (such as standalone desktop software, developer CLI tools, background sync utilities, or scripts). Because these apps are distributed to individual devices, it is assumed that these apps cannot keep secrets (they are public clients). They can access Google APIs while the user is actively interacting with the app or when the app is running in the background.