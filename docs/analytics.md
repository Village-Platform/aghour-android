# Analytics tracking plan

Every event the Android app sends to Google Analytics for Firebase. The source of truth is
`app/src/main/java/com/aghourservices/data/analytics/AnalyticsEvent.kt`; keep this file in step with it.

| Build | Firebase project | Package |
|---|---|---|
| debug | `aghour-dev` | `com.aghourservices.dev` |
| release | `aghour-app` | `com.aghourservices` |

## Rules

- **Add, don't rename.** Renaming an event or a param splits its history in the dashboard.
- **No personal data.** Ids, enum keys, counts and business names only. Never a user's name, phone or
  email. Search terms have digit runs of 7+ replaced by `[number]` (people type phone numbers).
- **Limits (GA4):** event names ≤ 40 chars, ≤ 25 params per event, string values ≤ 100 chars
  (longer values are cut by `FirebaseAnalyticsService`).
- **Where events fire:** results in ViewModels, after the server answered (`login` = the login
  succeeded); pure UI taps (call, share) in the composable; screens in one place (`AppNavigation`).
- Custom params must be registered as **custom dimensions** in the GA4 console (Admin → Custom
  definitions) before they appear in reports. DebugView shows them without that.

## User identity

| What | Value | Set in |
|---|---|---|
| User ID | numeric `users.id` while signed in, cleared on logout | `AppContainer` |
| User property `user_role` | `guest`, `user`, `firm_admin`, `super_admin` | `AppContainer` |

## Screens

`screen_view` with `screen_name` = the route name without `Route`, logged on every navigation:
`Welcome`, `Home`, `Explore`, `News`, `Account`, `Login`, `Register`, `Firm`, `Article`, `Plans`,
`Pay`, `Payments`, `Notifications`, `ChangePassword`, `DeleteAccount`, `MyFirms`, `Settings`,
`NewFirm`, `EditFirm`, `NewArticle`, `EditArticle`, `AdminCategories`, `NewCategory`,
`EditCategory`, `AdminPlans`, `NewPlan`, `EditPlan`.

## Events

| Event | When | Params |
|---|---|---|
| `welcome_completed` | First-launch welcome closed | `choice`: `start` / `login` |
| `login` | Login succeeded | `method`: `phone` |
| `sign_up` | Account created | `method`: `phone` |
| `auth_failed` | Server refused a login or sign-up | `flow`: `login` / `sign_up`, `http_status` |
| `logout` | Logout confirmed | — |
| `password_changed` | Password change succeeded | — |
| `account_deleted` | Account deletion succeeded | — |
| `search` | A search's first page returned (once per term + category) | `search_term`, `category_id`, `results_count` |
| `select_category` | Category tapped | `category_id`, `category_name`, `source`: `home` / `explore` |
| `view_firm` | Firm page loaded (once per visit) | `firm_id`, `firm_name`, `category_id`, `category_name` |
| `contact_firm` | Call / WhatsApp / map tapped on a firm | `firm_id`, `category_id`, `channel`: `call` / `whatsapp` / `map` |
| `share` | Firm or article shared | `content_type`: `firm` / `article`, `item_id` |
| `filter_news` | News kind chip tapped | `kind`: `all` / `news` / `announcement` / `article` |
| `view_article` | Article page loaded (once per visit) | `article_id`, `article_kind`, `article_title` |
| `change_theme` | Theme changed in Settings | `theme`: `light` / `dark` / `system` |
| `open_notification` | Notification row tapped | `kind` (server key) |
| `mark_all_notifications_read` | "Mark all read" tapped | — |
| `select_plan` | Subscribe tapped on a plan | `plan_id`, `plan_name`, `value`, `currency`: `EGP` |
| `submit_payment` | Transfer reference sent | `plan_id`, `payment_method`, `value`, `currency` |
| `manage_content` | Owner / admin write succeeded | `entity`: `firm` / `category` / `article` / `plan`, `action`: `create` / `update` / `delete` / `approve` / `suspend` |

`submit_payment` is not a GA4 `purchase`: an admin still has to confirm the transfer. Revenue
should be sent from the server when the subscription is approved.

## Checking events locally

```bash
adb shell setprop debug.firebase.analytics.app com.aghourservices.dev
adb shell setprop log.tag.FA VERBOSE
adb logcat -s FA FA-SVC
```

Then open Firebase console → `aghour-dev` → Analytics → DebugView. Turn it off again with
`adb shell setprop debug.firebase.analytics.app .none.`
