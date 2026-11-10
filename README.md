# Stealth Revived

A community revival of **Stealth**, the account-free Android Reddit client originally published at [cosmosapps/stealth](https://gitlab.com/cosmosapps/stealth).

The Android app is pinned as the `android/` Git submodule, with a small first-party Kotlin overlay applied at build time. The existing native UI, models, local profiles, subscriptions, history, saved items and media support are retained.

## Local Reddit transport

Reddit requests originate on-device and go directly to Reddit's OAuth and JSON endpoints. The app no longer needs a public Redlib, Teddit or other frontend instance. Redlib's direct-to-Reddit design is a technical reference; no Redlib Rust source is copied or embedded. See [the transport notes](docs/LOCAL_REDDIT_TRANSPORT.md).

## Build

```sh
git clone --recurse-submodules https://github.com/privacyfreak84/Stealth-Revived.git
cd Stealth-Revived
./scripts/build.sh testDebugUnitTest assembleDebug
```

The script initialises the pinned submodule, applies `overlay/` to the Android project, then runs Gradle. With no task argument it runs `assembleDebug`.

APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

## Provenance and licensing

The Android source is pinned to `RohitSurwase/Stealth-App` commit `cb19d7a1929325cf5b43272c9a06f41adbbb860f` (2026-10-22), a public GPLv3 Stealth-derived fork. Original notices are retained; see [UPSTREAM.md](UPSTREAM.md) and [COPYING](COPYING). Redlib is a separate AGPL-3.0-only project used as a design reference.
