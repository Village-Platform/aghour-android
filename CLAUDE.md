# CLAUDE.md

@RTK.md
@MENTOR.md

Native Android client (Kotlin + Compose) for the أجهور الكبرى village services platform.
`README.md` documents the stack, screen-by-screen behaviour and design decisions — read it for
*what* the app does. This file is the *how to work in it* companion.

## Build & verify

```bash
./gradlew assembleDebug     # main check after any change (~3 s incremental)
./gradlew lintDebug         # Android lint
./gradlew installDebug      # push to a connected device/emulator
```

There are no unit or instrumentation tests yet (`app/src/test` / `app/src/androidTest` do not
exist), so `assembleDebug` is the only automated signal — always run it before reporting done.

Requires JDK 17+ and Android SDK 37 (`local.properties` → `sdk.dir`, not in git).

## Backend

The Rails API lives at `/home/mego/Workspace/village-platform` (`app/controllers/api/v1/`).
The README's `../aghour_services` path is stale. When an endpoint's shape is unclear, read the
controller there rather than guessing.

`BuildConfig.API_BASE_URL` comes from `gradle.properties` (`AGHOUR_API_URL_DEBUG` /
`AGHOUR_API_URL_RELEASE`) and **must end with `/api/v1/`**. Debug builds allow cleartext HTTP
(`app/src/debug/AndroidManifest.xml`); release builds are HTTPS-only.

## Build config

- AGP 9 ships built-in Kotlin support — do **not** add `org.jetbrains.kotlin.android`.
- All dependency versions go in `gradle/libs.versions.toml`; `settings.gradle.kts` sets
  `FAIL_ON_PROJECT_REPOS`, so repositories cannot be declared per-module.
- `minSdk 26`, `targetSdk 36`, `compileSdk 37`, Java 17.

## Architecture

Manual DI: `AppContainer` is built once in `AghourApp` and handed to Compose through
`LocalAppContainer`. Screens read it with `appContainer()`. No Hilt.

One screen = one `XxxUiState` data class + `XxxViewModel` exposing `StateFlow<XxxUiState>` + a
`@Composable` that renders that state. ViewModels are created inline from the container:

```kotlin
val container = appContainer()
val vm: HomeViewModel = viewModel { HomeViewModel(container.catalog) }
val state by vm.state.collectAsStateWithLifecycle()
```

State is mutated only via `_state.update { it.copy(...) }`.

### Networking

Every repository call wraps Retrofit in `safeApiCall(json) { ... }` and returns `ApiResult<T>`
(`Success` / `Failure(message, code, httpStatus)`). Never let a raw Retrofit call or an exception
escape a repository. `ApiResult.Failure.message` is already a user-ready sentence in the app language (from the
server's `{ error: { message, details } }` envelope, or a local fallback) — show it as-is.

DTOs live in `data/remote/Dtos.kt`: `@Serializable`, `@SerialName` for snake_case fields, wrapped
in the `ListResponse<T>` / `ItemResponse<T>` / `DataList<T>` envelopes. `Json` is configured with
`ignoreUnknownKeys`, so new server fields never break the client.

`AuthInterceptor` attaches the bearer token and `Accept-Language` (`ar` / `en`, from `AppLanguage`),
and clears the session on 401. Token + user + welcome flag persist in DataStore via `SessionStore`.

## Navigation

Routes are `@Serializable` objects/classes declared at the top of `ui/navigation/AppNavigation.kt`.
Use the helpers defined there — never raw `navigate()` / `popBackStack()`:

- `nav.go(Route)` — push, ignored while a transition is running
- `nav.back()` — pop, never empties the stack
- `nav.navigateToTab(Route)` — bottom-tab semantics (single copy, state saved/restored)

They all gate on the current entry being `RESUMED`, which is what stops double taps from pushing a
screen twice. Adding a screen means: route declaration + `composable<Route>` entry + screen file +
ViewModel.

## UI conventions

- **Arabic and English; direction follows the language.** Arabic is right-to-left, English
  left-to-right. The choice lives in Android (per-app language), read and set through
  `data/settings/AppLanguage.kt` — not in DataStore. `MainActivity` is an `AppCompatActivity` so the
  choice applies on Android 12 and older too. Use `start`/`end` (never `left`/`right`) and
  `Icons.AutoMirrored.*` for directional icons.
- **Text goes in `res/values/strings.xml` (Arabic, the default) and `res/values-en/strings.xml`** —
  add every string to both — and is read with `stringResource` / `pluralStringResource`. Counts are
  `<plurals>`: Arabic needs all six forms (zero, one, two, few, many, other). Dates, numbers and
  money go through `formatDate` / `relativeTime` / `egp` in `ui/components/Common.kt`, which format
  for the current language. The only text literals in Kotlin are the two fallbacks in `ApiResult.kt`
  (repositories have no `Context`).
- **Colors** come from the design tokens: `val c = Aghour.colors`, then `c.page`, `c.surface`,
  `c.surface2`, `c.ink`, `c.muted`, `c.line`, `c.gold`, `c.goldInk`, `c.brand`, `c.ok`, `c.bad`
  (`ui/theme/Theme.kt`). Light and dark palettes exist; the user picks Light / Dark / System in
  Settings (`SettingsStore`). Never hard-code a colour for page content. The fixed `Forest`, `Gold`,
  `Cream` vals are only for the welcome artwork and text drawn over photos.
- **Icons** are Material icons (`Icons.Outlined.*`, `material-icons-extended`), tinted with a token —
  never emoji. Notification icons map server keys in `Common.kt` (`notificationIcon`).
- **Category icons are images from the API** (`icon_url`, relative to the API host → resolve with
  `categoryIconUrl()` / `firmIconUrl()`), drawn by `IconTile(url)` with Coil; no image = the app logo.
- **Logos:** use `BrandMark` / `BrandLogo` — they switch between the green and cream artwork by theme.
- **Page shell:** `SheetLayout` for photo-header screens (firm, article), `ScreenTitle` for plain
  screens. Both set the status-bar icon colour via `StatusBarIcons`; screens without either
  (Home, Profile, Login) call `StatusBarIcons()` themselves.
- Reuse `ui/components/` before writing new widgets: `GoldButton`, `GhostButton`, `SurfaceCard`,
  `Badge`, `Segmented`, `SectionHeader`, `FilterChipsRow`, `SearchPill`, `FormField`, `ErrorBanner`,
  `MessageBox`, `SkeletonRows`, `FirmRow`, `NewsRow`, `IconTile`, `CheckLine`, `Chevron`, and the `Context.openDialer/openWhatsApp/
  openMap/shareText/copyToClipboard` intent helpers.
- Lists that page follow the Explore pattern: `canLoadMore` on the state, `loadMore()` on the VM,
  triggered from the scroll position.

## Don't touch

`app/build/`, `build/`, `.gradle/`, `local.properties` — all generated or machine-local.
