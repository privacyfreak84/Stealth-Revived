# Stealth Revived

A community revival of **Stealth**, the account-free Android Reddit client originally published at [cosmosapps/stealth](https://gitlab.com/cosmosapps/stealth).

Stealth Revived keeps the native Android client and its existing browsing, local profiles, subscriptions, history, saved items and media support, with a first-party Kotlin/resource overlay applied at build time.

## What's changed

- Rebranded as **Stealth Revived**, with a new launcher mark and application ID: `com.privacyfreak84.stealthrevived`.
- Material 3 Expressive UI, being rebuilt in Jetpack Compose. Progress is tracked in [the migration notes](docs/M3_EXPRESSIVE_MIGRATION.md).
- Tokyo Night is the default color scheme. Nord, Gruvbox, Dracula, Catppuccin Mocha and wallpaper colors (Material You, Android 12+) are selectable under **Settings → Color scheme**.
- An Expressive bottom navigation bar replaces the old floating bar and side drawer.
- Reddit requests originate on-device and go directly to Reddit's OAuth and JSON endpoints. The app does not require a public Redlib, Teddit or other frontend instance. See [the transport notes](docs/LOCAL_REDDIT_TRANSPORT.md).

## Build a debug APK

```sh
git clone --recurse-submodules https://github.com/privacyfreak84/Stealth-Revived.git
cd Stealth-Revived
./scripts/build.sh testDebugUnitTest assembleDebug
```

The script initialises the pinned Android source, applies `overlay/`, then runs Gradle. The debug APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`.

## Build a privately signed release

```sh
./scripts/build-release.sh
```

On first run the script creates a private release keystore in the ignored `android/keys/` directory, asks you to choose a password, and builds a signed release APK. Keep a secure backup of both `stealth-revived-release.jks` and `keystore.properties`. They are not committed to Git. The signing identity is yours, not a shared project key.

To use a password supplied by your own secure shell environment, set `STEALTH_KEYSTORE_PASSWORD` before running the script. You can also set `STEALTH_KEY_DNAME` to customise the certificate's display identity.

## Provenance and licensing

The Android source is pinned to `RohitSurwase/Stealth-App` commit `cb19d7a1929325cf5b43272c9a06f41adbbb860f`. Original notices are retained; see [UPSTREAM.md](UPSTREAM.md) and [COPYING](COPYING). Redlib is a separate AGPL-3.0-only project used as a technical reference; no Redlib source code is copied or embedded.
