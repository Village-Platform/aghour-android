package com.aghour.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.aghour.app.R
import com.aghour.app.data.remote.CategoryDto
import com.aghour.app.data.remote.FirmDto
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

/** Material icon for the category icon keys used by the admin dashboard; unknown keys get a generic icon. */
fun categoryIcon(category: CategoryDto): ImageVector = categoryIcon(category.icon)

fun categoryIcon(icon: String?): ImageVector = when (icon?.lowercase()) {
    "doctor", "doctors", "clinic" -> Icons.Outlined.LocalHospital
    "pharmacy" -> Icons.Outlined.LocalPharmacy
    "restaurant", "food" -> Icons.Outlined.Restaurant
    "shop", "store", "market" -> Icons.Outlined.Storefront
    "craft", "crafts", "worker" -> Icons.Outlined.Handyman
    "school", "education" -> Icons.Outlined.School
    "bakery" -> Icons.Outlined.BakeryDining
    "car", "transport" -> Icons.Outlined.DirectionsCar
    "mosque" -> Icons.Outlined.Mosque
    "sports", "gym" -> Icons.Outlined.SportsSoccer
    "services", "government" -> Icons.Outlined.AccountBalance
    else -> Icons.Outlined.Category
}

/** The firm's category icon, looked up in an already-loaded category list (null = unknown, show the logo). */
fun firmIcon(firm: FirmDto, categories: List<CategoryDto>): ImageVector? =
    categories.firstOrNull { it.id == firm.category.id }?.let { categoryIcon(it) }

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
