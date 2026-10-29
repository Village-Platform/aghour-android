# AGENTS.md

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
escape a repository. `ApiResult.Failure.message` is already a user-ready Arabic sentence (from the
server's `{ error: { message, details } }` envelope, or a local fallback) — show it as-is.

DTOs live in `data/remote/Dtos.kt`: `@Serializable`, `@SerialName` for snake_case fields, wrapped
in the `ListResponse<T>` / `ItemResponse<T>` / `DataList<T>` envelopes. `Json` is configured with
`ignoreUnknownKeys`, so new server fields never break the client.

`AuthInterceptor` attaches the bearer token and `Accept-Language: ar`, and clears the session on
401. Token + user + welcome flag persist in DataStore via `SessionStore`.

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

- **RTL always.** `AghourTheme` forces `LayoutDirection.Rtl` regardless of phone language. Use
  `start`/`end` (never `left`/`right`) and `Icons.AutoMirrored.*` for directional icons.
- **Arabic text goes in `res/values/strings.xml`** and is read with `stringResource`. The only
  literals in Kotlin are the formatting/mapping helpers in `ui/components/Common.kt`
  (relative time, category symbols, status labels) and the fallbacks in `ApiResult.kt`.
- **Colors** are the named palette vals in `ui/theme/Theme.kt` (`Forest`, `Gold`, `Cream`, `Sand`,
  `Ink`, `Muted`) or `MaterialTheme.colorScheme`. Light theme only — there is no dark scheme yet.
- **Page shell:** `SheetLayout` for dark-header screens (Home, Profile, details), `ScreenTitle` for
  cream screens (Explore, News, Plans, Pay). Both set the status-bar icon colour themselves via
  `StatusBarIcons` — don't set it separately.
- Reuse `ui/components/` before writing new widgets: `GoldButton`, `ForestButton`, `SandCard`,
  `Badge`, `SectionHeader`, `FilterChipsRow`, `SearchPill`, `FormField`, `ErrorBanner`,
  `MessageBox`, `SkeletonRows`, `FirmRow`, `NewsRow`, and the `Context.openDialer/openWhatsApp/
  openMap/shareText/copyToClipboard` intent helpers.
- Lists that page follow the Explore pattern: `canLoadMore` on the state, `loadMore()` on the VM,
  triggered from the scroll position.

## Don't touch

`app/build/`, `build/`, `.gradle/`, `local.properties` — all generated or machine-local.
