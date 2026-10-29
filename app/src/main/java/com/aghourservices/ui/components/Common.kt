package com.aghourservices.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.aghourservices.BuildConfig
import com.aghourservices.R
import com.aghourservices.data.remote.CategoryDto
import com.aghourservices.data.remote.FirmDto
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import okhttp3.HttpUrl.Companion.toHttpUrl

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

private fun parse(iso: String?): OffsetDateTime? = iso?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }

/** Rails sends ISO-8601 with offset, e.g. 2026-10-24T19:00:00.000+03:00 */
fun formatDate(iso: String?): String = parse(iso)?.format(DateFormat) ?: ""

/** "منذ ساعتين", "منذ يوم", "منذ ٣ أيام"… falls back to the date after a month. */
fun relativeTime(iso: String?): String {
    val time = parse(iso) ?: return ""
    val minutes = Duration.between(time, OffsetDateTime.now()).toMinutes()
    return when {
        minutes < 0 -> formatDate(iso)
        minutes < 1 -> "الآن"
        minutes < 60 -> if (minutes == 1L) "منذ دقيقة" else "منذ $minutes دقيقة"
        minutes < 60 * 24 -> when (val h = minutes / 60) {
            1L -> "منذ ساعة"
            2L -> "منذ ساعتين"
            in 3..10 -> "منذ $h ساعات"
            else -> "منذ $h ساعة"
        }
        else -> when (val d = minutes / (60 * 24)) {
            1L -> "منذ يوم"
            2L -> "منذ يومين"
            in 3..6 -> "منذ $d أيام"
            in 7..13 -> "منذ أسبوع"
            in 14..29 -> "منذ ${d / 7} أسابيع"
            else -> formatDate(iso)
        }
    }
}

/** 24900 piasters -> "249 ج.م" */
fun egp(cents: Int): String {
    val pounds = cents / 100.0
    val text = if (pounds % 1.0 == 0.0) pounds.toLong().toString() else "%.2f".format(pounds)
    return "$text ج.م"
}

fun greeting(): String = if (LocalTime.now().hour < 12) "صباح الخير" else "مساء الخير"

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

/** Label for elevated roles only; plain users (and any role this app version doesn't know) get no badge. */
@Composable
fun roleLabel(role: String): String? = when (role) {
    "super_admin" -> stringResource(R.string.role_super_admin)
    else -> null
}

/** Firm status from the API (pending / active / suspended); an unknown one is shown as sent. */
@Composable
fun firmStatusLabel(status: String): String = when (status) {
    "active" -> stringResource(R.string.firm_status_active)
    "pending" -> stringResource(R.string.firm_status_pending)
    "suspended" -> stringResource(R.string.firm_status_suspended)
    else -> status
}

/** Subscription state -> label + badge tone (states come from the API: pending/rejected/scheduled/current/expired). */
fun subscriptionState(state: String): Pair<String, BadgeTone> = when (state) {
    "current" -> "ساري" to BadgeTone.Green
    "scheduled" -> "مجدول" to BadgeTone.Gold
    "pending" -> "قيد المراجعة" to BadgeTone.Gold
    "rejected" -> "مرفوض" to BadgeTone.Red
    else -> "منتهي" to BadgeTone.Gray
}

fun paymentMethodLabel(key: String): String = when (key) {
    "instapay" -> "إنستاباي"
    "vodafone_cash" -> "فودافون كاش"
    else -> key
}
