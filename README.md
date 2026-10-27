# Aghour Android — أجهور الكبرى

Native Android app for the village services platform. It talks to the Rails API in
`../village-platform` (`/api/v1`).

## Stack

| Part | Choice |
|------|--------|
| Language / UI | Kotlin 2.4.21, Jetpack Compose (Material 3), right-to-left Arabic |
| Build | Android Gradle Plugin 9.4.1 (built-in Kotlin), Gradle 9.8.2, JDK 17+ |
| SDK | minSdk 26, targetSdk 36 (Google Play requirement), compileSdk 37 |
| Networking | Retrofit 3 + OkHttp + kotlinx.serialization |
| Navigation | Navigation Compose with type-safe routes |
| Session | DataStore (JWT + signed-in user) |
| DI | Manual `AppContainer` (simple; can move to Hilt later) |

Versions live in `gradle/libs.versions.toml`.

## Run it

1. Start the backend: `cd ../village-platform && bin/rails s -b 0.0.0.0`
   (defaults to port 3000; set `PORT` to use another one)
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

The UI follows the screen designs artifact "أجهور الكبرى — تصميم الشاشات" (light / dark):

- Design tokens in `ui/theme/Theme.kt` (`Aghour.colors`), one palette per theme:
  light = white page, black text, grey fields, gold `#C28D40` actions;
  dark = GitHub Dark greys (page `#0D1117`, cards `#161B22`, borders `#30363D`, text `#E6EDF3`), gold `#D9A458` actions.
- Theme: follows the phone's dark mode by default ("حسب الجهاز"); Light or Dark can be forced in
  Settings (Profile → المظهر). Saved on the device in `SettingsStore` (survives logout).
- Font: Cairo, bundled as one variable font (`res/font/cairo.ttf`, SIL Open Font License in `licenses/`).
- Welcome screen text and "ابدأ الآن" button use Tajawal (`res/font/tajawal_*.ttf`, SIL Open Font License).
- Detail screens (firm, article) use `SheetLayout`: photo header, then a sheet with rounded top
  corners. Other screens are plain pages with `ScreenTitle` or their own header (Home, Profile, Login).
  The status-bar icon color follows the screen and the theme.
- Logos and icons are generated from the high-resolution original `../village-platform/public/brand/logo.png`
  (1254 px): faint edge noise removed, the symbol separated from the name, and the green parts recolored
  to cream for dark backgrounds — `logo_cream` / `logo_green` (640 px), `mark_cream` / `mark_green` (512 px).
- App icon: adaptive icon with the cream symbol on forest (`ic_launcher_foreground`, inside the 66dp safe
  zone), a white `ic_launcher_monochrome` layer for Android 13+ themed icons, and a 512 px
  `app/src/main/ic_launcher-playstore.png` for the Play Store listing. The splash uses the same symbol.
- The welcome artwork and the home cover banner are made from the originals in `../village-platform/public/brand`.

## Navigation

- Bottom tabs: الرئيسية، استكشف، الأخبار، حسابي. Each tab keeps its state; Back from a tab returns to Home.
- Welcome is shown once on first launch, on top of the app (not in the back stack).
- Login/Register return to the screen that asked for them (e.g. Plans → Login → back to Plans).
- `nav.go()` / `nav.back()` ignore taps while a screen is still animating, so double taps cannot
  open a screen twice and fast Back presses cannot leave an empty page.
- Choosing a category on Home opens the Explore tab with that category selected.
- Motion: tabs cross-fade; pushed screens slide a short distance from the reading side (right-to-left
  aware) with a fade, and slide back on Back (320 ms, ease-out). Welcome cross-fades into the app.
- Predictive back is enabled (`android:enableOnBackInvokedCallback`), so the back gesture previews
  the previous screen on Android 14+.

## Screens

| Screen | What it does |
|--------|--------------|
| Welcome | First launch: start, or go straight to login |
| Login / Register | Phone + password; checks the phone (11 digits, 01…), password length and match |
| Home | Greeting, notifications bell with unread count, search, categories, cover banner (→ plans), featured firms, latest news, pull-to-refresh |
| Explore | Search while typing + category chips, results count, paging |
| Firm | Photo header, share, call / location / WhatsApp tiles, hours, phone, address, about |
| News | Kind chips (all, news, announcements, articles), featured first item, paging |
| Plans | Plans from the API, middle one highlighted; guests are asked to log in first |
| Pay | InstaPay / Vodafone Cash account with copy button, transfer reference → sends the request |
| Profile | Subscription progress (days left) or "subscribe now", menu, logout with confirmation |
| Notifications | All / unread chips, unread rows tinted gold, mark one or all as read |
| Settings | Appearance: light / dark / follow the phone |
| Payments / My firms / Change password | From the profile menu |

## Next steps

1. Owners: add / edit their firm from the app, attach the payment screenshot.
2. Push notifications with FCM (the in-app notifications screen already works).
3. Real photos for firms, categories and news (after the backend adds image uploads).
4. Ratings and reviews (shown in the designs; needs a backend model first).
5. Enable R8 for release, app signing.
6. Notification preferences (the designs' Settings toggles) — needs a server-side preferences endpoint first.
