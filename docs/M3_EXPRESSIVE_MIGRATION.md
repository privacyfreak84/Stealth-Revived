# Material 3 Expressive rebuild

The UI is being rebuilt from scratch in Jetpack Compose with Material 3 Expressive.
This file tracks the work so it can continue across sessions. Work happens on the
`m3-expressive` branch.

## Decisions

- **Toolkit:** Jetpack Compose. The data layer (Reddit transport, Room, repositories,
  ViewModels) is kept.
- **Navigation:** a Material 3 Expressive bottom bar (`ShortNavigationBar`) only. The side
  drawer and the "Navigation layout" setting are removed. So is left-handed mode, which
  only placed the old floating bar.
- **Colors:** the five palettes (Tokyo Night, Nord, Gruvbox, Dracula, Catppuccin Mocha)
  are full Material 3 schemes, including the surface-container and fixed roles. A sixth
  option, "Wallpaper colors (Material You)", appears on Android 12 and later. The AMOLED
  night mode turns surfaces pure black.
- **Theme:** `MaterialExpressiveTheme` with `MotionScheme.expressive()` (spring motion),
  the expressive shape scale and the emphasized type styles. See `ui/theme/`.

## Toolchain

Gradle 8.14.3, AGP 8.13.2, Kotlin 2.2.21, compileSdk 36 (targetSdk unchanged at 33),
Java 17. Versions live in `overlay/buildSrc/src/main/kotlin/Dependencies.kt`.

- Compose BOM `2026.06.01` with `material3:1.5.0-alpha18`. Later 1.5 builds and
  Compose 1.12 require AGP 9.1 and compileSdk 37. Moving to them means migrating to
  AGP 9 (built-in Kotlin, `legacy-kapt`) first.
- Moshi codegen runs on KSP with `ksp.useKSP2=false`. Moshi 1.15.2's kapt processor
  cannot read Kotlin 2.2 metadata, and its KSP2 path crashes on `@JsonClass`.
- Hilt, Room and data binding still use kapt. Data binding goes away when the last
  XML screen is deleted. Then everything can move to KSP.

## Migration strategy

The fragment navigation graph stays the backbone until every screen is rebuilt. Each
fragment is replaced by a thin host whose view is a `ComposeView` running the new
screen. When a screen is rebuilt, delete its XML layouts and adapters. Every commit
then builds and runs. When no XML screens remain, switch to Navigation Compose and
delete the fragment hosts.

Upstream files that need deleting are listed in `overlay-removed.txt` (add it, and
teach `scripts/build.sh` to delete those paths after copying the overlay), because the
overlay can only add or replace files.

## Status

Done:
- [x] Toolchain upgrade
- [x] Expressive theme for Compose (`ui/theme/StealthTheme.kt`, `ui/theme/Palettes.kt`)
- [x] Legacy View theme on `Theme.Material3Expressive.DayNight.NoActionBar`, with surface
      container roles added to all palettes and dynamic color applied when selected
- [x] App shell: Expressive bottom bar in Compose (`ui/main/StealthNavigationBar.kt`).
      It hides on scroll and on detail screens, the selected icon fills and springs,
      and reselecting Home scrolls to top
- [x] Side drawer and navigation-layout setting removed; color scheme picker in Settings

Screens still on the old XML UI, roughly in priority order:
- [ ] Home feed: `postlist/PostListFragment` with the post item layouts. Use
      `LoadingIndicator`, an expressive pull-to-refresh, `ButtonGroup` for post
      actions, and segmented feed sort chips
- [ ] Post details and comments: `postdetails/PostDetailsFragment`
- [ ] Subscriptions: `subscriptions/SubscriptionsFragment`. Use expressive segmented list items
- [ ] Profile: `profile/ProfileFragment`, `profile/ProfileSavedFragment`
- [ ] Settings: `preferences/PreferencesFragment`, plus `redditsource`,
      `privacyenhancer`, `about`, `backup/*`, `policydisclaimer`
- [ ] Search: `search/*`
- [ ] Subreddit and user pages: `subreddit/*`, `user/*`. Use a large flexible top app bar
- [ ] Media viewer: `mediaviewer/MediaViewerFragment`. Use a `FloatingToolbar` for controls
      and a wavy progress indicator for video
- [ ] Bottom-sheet menus: `sort`, `postmenu`, `commentmenu`, `linkmenu`, `profilemanager`
- [ ] Delete `common/` widgets, adapters and layouts no longer referenced
- [ ] Switch to Navigation Compose; drop data binding and kapt
