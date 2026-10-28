package com.aghourservices.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.aghourservices.BuildConfig
import com.aghourservices.R
import com.aghourservices.data.remote.CategoryDto
import com.aghourservices.data.remote.FirmDto
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.FormatStyle
import java.util.Locale
import okhttp3.HttpUrl.Companion.toHttpUrl

private fun parse(iso: String?): OffsetDateTime? = iso?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }

/** The language the screen is drawn in (follows the in-app language choice). */
@Composable
@ReadOnlyComposable
private fun uiLocale(): Locale = LocalConfiguration.current.locales[0]

/**
 * Rails sends ISO-8601 with offset, e.g. 2026-10-24T19:00:00.000+03:00.
 * Shown as "28 أكتوبر 2026" / "Oct 28, 2026", with the language's own digits.
 */
@Composable
fun formatDate(iso: String?): String {
    val locale = uiLocale()
    val time = parse(iso) ?: return ""
    return time.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).withDecimalStyle(DecimalStyle.of(locale)),
    )
}

/** "منذ ساعتين" / "2 hours ago"… falls back to the date after a month. Plural forms come from resources. */
@Composable
fun relativeTime(iso: String?): String {
    val time = parse(iso) ?: return ""
    val minutes = Duration.between(time, OffsetDateTime.now()).toMinutes()
    return when {
        minutes < 0 -> formatDate(iso)
        minutes < 1 -> stringResource(R.string.time_now)
        minutes < 60 -> minutes.toInt().let { pluralStringResource(R.plurals.minutes_ago, it, it) }
        minutes < 60 * 24 -> (minutes / 60).toInt().let { pluralStringResource(R.plurals.hours_ago, it, it) }
        else -> when (val d = (minutes / (60 * 24)).toInt()) {
            in 1..6 -> pluralStringResource(R.plurals.days_ago, d, d)
            in 7..29 -> (d / 7).let { pluralStringResource(R.plurals.weeks_ago, it, it) }
            else -> formatDate(iso)
        }
    }
}

/** 24900 piasters -> "٢٤٩ ج.م" / "EGP 249" */
@Composable
fun egp(cents: Int): String {
    val amount = NumberFormat.getNumberInstance(uiLocale()).apply { maximumFractionDigits = 2 }.format(cents / 100.0)
    return stringResource(R.string.egp_amount, amount)
}

@Composable
fun greeting(): String =
    stringResource(if (LocalTime.now().hour < 12) R.string.greeting_morning else R.string.greeting_evening)

/**
 * Absolute URL of a category image. The API sends `icon_url` relative to its host
 * (e.g. /api/v1/categories/3/icon?v=…), so it is resolved against the API base URL.
 * null when the category has no image: callers show the app logo instead.
 */
fun categoryIconUrl(iconUrl: String?): String? =
    iconUrl?.takeIf { it.isNotBlank() }?.let { BuildConfig.API_BASE_URL.toHttpUrl().resolve(it)?.toString() }

/** The firm's category image: from the firm's own category if the API sent it, else from the loaded list. */
fun firmIconUrl(firm: FirmDto, categories: List<CategoryDto>): String? =
    categoryIconUrl(firm.category.iconUrl ?: categories.firstOrNull { it.id == firm.category.id }?.iconUrl)

/** Icon for a notification row, from the server's `kind` (unknown kinds get a bell). */
fun notificationIcon(kind: String): ImageVector = when {
    "payment" in kind || "subscription" in kind -> Icons.Outlined.CreditCard
    "firm" in kind -> Icons.Outlined.Storefront
    "article" in kind || "news" in kind -> Icons.Outlined.Newspaper
    "invit" in kind -> Icons.Outlined.Handshake
    else -> Icons.Outlined.Notifications
}

@Composable
fun kindLabel(kind: String): String = when (kind) {
    "article" -> stringResource(R.string.kind_article)
    "announcement" -> stringResource(R.string.kind_announcement)
    else -> stringResource(R.string.kind_news)
}

fun kindTone(kind: String): BadgeTone = when (kind) {
    "announcement" -> BadgeTone.Gold
    "article" -> BadgeTone.Gray
    else -> BadgeTone.Green
}

@Composable
fun roleLabel(role: String): String = when (role) {
    "firm_admin" -> stringResource(R.string.role_firm_admin)
    "super_admin" -> stringResource(R.string.role_super_admin)
    else -> stringResource(R.string.role_user)
}

/** Subscription state -> label + badge tone (states come from the API: pending/rejected/scheduled/current/expired). */
@Composable
fun subscriptionState(state: String): Pair<String, BadgeTone> = when (state) {
    "current" -> stringResource(R.string.state_current) to BadgeTone.Green
    "scheduled" -> stringResource(R.string.state_scheduled) to BadgeTone.Gold
    "pending" -> stringResource(R.string.state_pending) to BadgeTone.Gold
    "rejected" -> stringResource(R.string.state_rejected) to BadgeTone.Red
    else -> stringResource(R.string.state_expired) to BadgeTone.Gray
}

@Composable
fun paymentMethodLabel(key: String): String = when (key) {
    "instapay" -> stringResource(R.string.payment_instapay)
    "vodafone_cash" -> stringResource(R.string.payment_vodafone_cash)
    else -> key
}
