# Upstream and source provenance

## Stealth Android application

- Original project: [cosmosapps/stealth on GitLab](https://gitlab.com/cosmosapps/stealth)
- Selected publicly accessible source baseline: [RohitSurwase/Stealth-App](https://github.com/RohitSurwase/Stealth-App)
- Pinned commit: `cb19d7a1929325cf5b43272c9a06f41adbbb860f`
- Commit date: 2026-10-22
- License: GNU GPL version 3; the upstream `COPYING` file is retained.

The original GitLab repository could not be fetched in this environment. Rather than reconstruct unseen source, this revival pins the publicly accessible Stealth-derived GitHub tree. This is a documented provenance choice, not a claim that this commit is identical to the current GitLab HEAD.

The Android repository is a Git submodule pinned to a commit SHA, not a moving branch. Clone with `git clone --recurse-submodules` or initialise it with `git submodule update --init --recursive`.

## Redlib as a technical reference

[redlib-org/redlib](https://github.com/redlib-org/redlib) is an independent Rust Reddit frontend licensed AGPL-3.0-only. It informed the direct-to-Reddit approach. No Redlib source code is copied or embedded here; the Android transport uses Stealth's Kotlin, Retrofit, OkHttp and Moshi code.
