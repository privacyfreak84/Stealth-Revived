# Material 3 Expressive rebuild

Stealth Revived's UI was rebuilt from scratch in Jetpack Compose with Material 3
Expressive. The data layer (on-device Reddit transport, Room, repositories,
ViewModels) is unchanged.

## Design decisions

- **Navigation:** a single Expressive bottom bar (`ShortNavigationBar`). It hides while
  scrolling down and on detail screens, and re-tapping a tab scrolls back to the top. The
  side drawer, the "Navigation layout" setting and left-handed mode are gone.
- **Colors:** the five palettes (Tokyo Night, Nord, Gruvbox, Dracula, Catppuccin Mocha)
  are full Material 3 schemes, including the surface-container and fixed roles
  (`ui/theme/Palettes.kt`). "Wallpaper colors (Material You)" appears on Android 12+.
  The AMOLED night mode makes surfaces pure black.
- **Theme:** `MaterialExpressiveTheme` with `MotionScheme.expressive()` springs, the
  expressive shape scale and emphasized type (`ui/theme/StealthTheme.kt`).

## Expressive components in use

| Where | Components |
|---|---|
| App shell | `ShortNavigationBar`, spring-animated icons, `SnackbarHost` |
| Feeds | `PullToRefreshBox` with `PullToRefreshDefaults.LoadingIndicator`, `LoadingIndicator`, `ContainedLoadingIndicator`, `MediumFloatingActionButton` scroll-to-top |
| Post cards | connected `ToggleButton` group (score, comments), shape-morphing `FilledTonalIconToggleButton` (save), `MaterialShapes` avatars |
| Sorting | `ToggleButton` chips plus a connected button group for the time range |
| Lists and menus | `SegmentedListItem` with `ListItemDefaults.segmentedShapes` |
| App bars | `TopAppBar` with subtitle, `MediumFlexibleTopAppBar`, `LargeFlexibleTopAppBar` |
| Media viewer | `HorizontalFloatingToolbar` (vibrant), `LinearWavyProgressIndicator`, cookie-shaped play button |
| States | `MaterialShapes` badges for empty, error and success states |

## Code layout

- `ui/main/`: `StealthApp` (Navigation Compose graph, bar, global dialogs) and the bar.
- `ui/navigation/`: routes, deep-link routing for Reddit URLs, in-memory hand-off for
  posts and media.
- `ui/common/compose/`: shared pieces: post card and feed, sheets, link handling, states,
  rich text (the existing span renderer, embedded with `AndroidView`).
- One `*Screen.kt` per feature package, next to its existing ViewModel.

The overlay can only add or replace upstream files, so `overlay-removed.txt` lists the
upstream fragments, adapters, widgets and XML layouts that `scripts/build.sh` deletes
after copying the overlay.

## Toolchain

Gradle 9.6.1, AGP 9.5.1 with built-in Kotlin, Kotlin 2.3.21, compileSdk 37,
minSdk 24, Java 17. Compose BOM 2026.09.00 with `material3` 1.5.0-rc01. Versions live in
`overlay/buildSrc/src/main/kotlin/Dependencies.kt`.

- **minSdk 24 (Android 7.0).** material3 1.5 and Compose 1.12 no longer support
  Android 6.0.
- **KSP only.** Hilt, Room and Moshi all run on KSP2, and kapt and data binding are gone.
  Room generates Java (`room.generateKotlin=false`) because the entities use
  `@JvmOverloads` constructors. The four Moshi qualifier annotations no longer declare
  `@Retention`: Kotlin's default is already RUNTIME, and the explicit annotation crashes
  Moshi 1.15.2's KSP2 processor.
- **ShortNavigationBar quirk.** In material3 1.5, a bar given a fixed width shows only its
  first item. `StealthNavigationBar` wraps it in a `Box` and never sizes the bar itself.
