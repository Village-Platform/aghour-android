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
├── AghourApp.kt / MainActivity.kt   app start, splash (kept until the welcome flag is read)
├── di/AppContainer.kt               API client, session, repositories, Explore category filter
├── data/
│   ├── remote/      AghourApi (endpoints), Dtos, ApiResult (Arabic error messages), AuthInterceptor
│   ├── session/     SessionStore (token, user, welcome flag in DataStore)
│   └── repository/  CatalogRepository (categories, firms, news), AuthRepository (login, register,
│                    me, password), OwnerRepository (plans, payment, subscriptions, my firms, notifications)
└── ui/
    ├── theme/       palette from the designs (forest, gold, cream, sand), Cairo font, right-to-left
    ├── components/  SheetLayout, ScreenTitle, chips, badges, buttons, form fields, cards, skeletons, intents
    ├── navigation/  type-safe routes, bottom tabs, safe navigation helpers
    ├── welcome/     first-launch screen (splash artwork + "ابدأ الآن")
    ├── home/        greeting, search, category tiles, cover banner, featured firms, latest news
    ├── explore/     all firms: search + category chips + paging
    ├── firms/       firm details (photo header, call / location / WhatsApp, hours, about)
    ├── news/        news with kind chips (featured first item) and article page
    ├── account/     profile (subscription progress, menu), login, register
    └── owner/       plans, payment (InstaPay / Vodafone Cash), payments history, notifications,
                     my firms, change password
```

Every screen = a `ViewModel` (state as `StateFlow`) + a `@Composable` that draws that state.

## Design

The UI follows `ajhour_screens.html` (the screen designs):

- Palette: forest `#0B3727`, gold `#C28D40`, cream `#FAF5EC`, sand `#E6DCC6` (`ui/theme/Theme.kt`).
- Font: Cairo, bundled as one variable font (`res/font/cairo.ttf`, SIL Open Font License in `licenses/`).
- Dark screens (Home, Profile, Login, details) use `SheetLayout`: forest or photo header, then a cream
  sheet with rounded top corners. Light screens (Explore, News, Plans, Pay) use `ScreenTitle`.
  The status-bar icon color follows the screen.
- Logos come from the design file (`logo_cream` / `logo_green`, `mark_cream` / `mark_green`).
  The welcome artwork and the home cover banner are made from the high-resolution originals in
  `village-platform/public/brand`. Launcher and splash icons use the cream mark on forest.

## Navigation

- Bottom tabs: الرئيسية، استكشف، الأخبار، حسابي. Each tab keeps its state; Back from a tab returns to Home.
- Welcome is shown once on first launch, on top of the app (not in the back stack).
- Login/Register return to the screen that asked for them (e.g. Plans → Login → back to Plans).
- `nav.go()` / `nav.back()` ignore taps while a screen is still animating, so double taps cannot
  open a screen twice and fast Back presses cannot leave an empty page.
- Choosing a category on Home opens the Explore tab with that category selected.

## Screens

| Screen | What it does |
|--------|--------------|
| Welcome | First launch: start, or go straight to login |
| Login / Register | Phone + password; checks the phone (11 digits, 01…), password length and match |
| Home | Greeting, search, categories, cover banner (→ plans), featured firms, latest news, pull-to-refresh |
| Explore | Search while typing + category chips, results count, paging |
| Firm | Photo header, share, call / location / WhatsApp tiles, hours, phone, address, about |
| News | Kind chips (all, news, announcements, articles), featured first item, paging |
| Plans | Plans from the API, middle one highlighted; guests are asked to log in first |
| Pay | InstaPay / Vodafone Cash account with copy button, transfer reference → sends the request |
| Profile | Subscription progress (days left) or "subscribe now", menu, logout with confirmation |
| Payments / Notifications / My firms / Change password | From the profile menu |

## Next steps

1. Owners: add / edit their firm from the app, attach the payment screenshot.
2. Push notifications with FCM (the in-app notifications screen already works).
3. Real photos for firms, categories and news (after the backend adds image uploads).
4. Ratings and reviews (shown in the designs; needs a backend model first).
5. Enable R8 for release, app signing, dark theme.
