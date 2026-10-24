# Aghour Android — أجهور الكبرى

Native Android app for the village services platform. It talks to the Rails API in
`../aghour_services` (`/api/v1`).

## Stack

| Part | Choice |
|------|--------|
| Language / UI | Kotlin 2.4.21, Jetpack Compose (Material 3), right-to-left Arabic |
| Build | Android Gradle Plugin 9.4.0 (built-in Kotlin), Gradle 9.8.2, JDK 17+ |
| SDK | minSdk 26, targetSdk 36 (Google Play requirement), compileSdk 37 |
| Networking | Retrofit 3 + OkHttp + kotlinx.serialization |
| Navigation | Navigation Compose with type-safe routes |
| Session | DataStore (JWT + signed-in user) |
| DI | Manual `AppContainer` (simple; can move to Hilt later) |

Versions live in `gradle/libs.versions.toml`.

## Run it

1. Start the backend: `cd ../aghour_services && bin/rails s -b 0.0.0.0`
2. Open this folder in Android Studio (File → Open) and let Gradle sync.
   If it asks to install SDK Platform 37 or build tools, accept.
3. Pick the API address in `gradle.properties`:
   - Emulator: `AGHOUR_API_URL_DEBUG=http://10.0.2.2:3000/api/v1/` (default)
   - Real phone on the same Wi-Fi: `http://<your-computer-IP>:3000/api/v1/`
     (find the IP with `hostname -I`)
4. Run the `app` configuration.

Debug builds allow plain HTTP (local Rails). Release builds are HTTPS-only.

## Structure

```
app/src/main/java/com/aghour/app/
├── AghourApp.kt / MainActivity.kt   app start, splash, theme
├── di/AppContainer.kt               creates the API client, session and repositories
├── data/
│   ├── remote/   AghourApi (endpoints), Dtos (JSON models), ApiResult (Arabic error messages), AuthInterceptor
│   ├── session/  SessionStore (token + user in DataStore)
│   └── repository/ CatalogRepository (categories, firms, news), AuthRepository (login, register, me)
└── ui/
    ├── theme/       green & gold Material theme, forced right-to-left
    ├── navigation/  routes + bottom bar (الرئيسية، الأخبار، حسابي)
    ├── home/        banner, categories, latest news
    ├── firms/       firms of a category (search + paging), firm details (call, WhatsApp, map)
    ├── news/        news list and article
    └── account/     profile, login, register
```

Every screen = a `ViewModel` (state as `StateFlow`) + a `@Composable` that draws that state.

## Screens in this version

- Home: village banner, categories grid, latest 5 news.
- Firms by category: search by name, loads more while scrolling.
- Firm details: call, WhatsApp (Egyptian numbers converted to +20), open in maps.
- News list and article page.
- Account: login / register (phone + password), profile with role, logout.

## Next steps

1. Firm owner area: subscription plans, payment request with screenshot, my firms (add / edit).
2. Notifications screen (API already exists), then push notifications with FCM.
3. Images for firms, categories and news (after the backend adds them).
4. Bundle the Cairo font, enable R8 for release, app signing.
