package com.aghour.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aghour.app.R
import com.aghour.app.data.remote.CategoryDto
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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

/** Symbols for the category icon keys used by the admin dashboard; falls back to the first letter. */
fun categorySymbol(category: CategoryDto): String = categorySymbol(category.icon, category.name)

fun categorySymbol(icon: String?, name: String): String = when (icon?.lowercase()) {
    "doctor", "doctors", "clinic" -> "🩺"
    "pharmacy" -> "💊"
    "restaurant", "food" -> "🍽️"
    "shop", "store", "market" -> "🛒"
    "craft", "crafts", "worker" -> "🛠️"
    "school", "education" -> "🎓"
    "bakery" -> "🥖"
    "car", "transport" -> "🚗"
    "mosque" -> "🕌"
    "sports", "gym" -> "⚽"
    "services", "government" -> "🏛️"
    else -> name.trim().take(1)
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
