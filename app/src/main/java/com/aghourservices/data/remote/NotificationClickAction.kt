package com.aghourservices.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

sealed interface NotificationClickAction {
    data class OpenFirm(val firmId: Long) : NotificationClickAction
    data class OpenArticle(val articleId: Long) : NotificationClickAction
    data class OpenCategory(val categoryId: Long) : NotificationClickAction
    data object OpenPlans : NotificationClickAction
    data object OpenPayments : NotificationClickAction
    data object OpenMyFirms : NotificationClickAction
    data object OpenNews : NotificationClickAction
    data object OpenNotifications : NotificationClickAction
}

/** Parses server destination (screen + id) or falls back to notification kind. */
fun parseNotificationAction(
    screen: String?,
    rawId: String?,
    kind: String?,
): NotificationClickAction? {
    val id = rawId?.toLongOrNull()

    if (screen != null && screen != "none") {
        return when (screen) {
            "article" -> id?.let { NotificationClickAction.OpenArticle(it) } ?: NotificationClickAction.OpenNews
            "firm" -> id?.let { NotificationClickAction.OpenFirm(it) } ?: NotificationClickAction.OpenMyFirms
            "category" -> id?.let { NotificationClickAction.OpenCategory(it) }
            "subscription" -> NotificationClickAction.OpenPayments
            else -> NotificationClickAction.OpenNotifications
        }
    }

    if (kind.isNullOrBlank()) return null

    return when {
        "expir" in kind || "renew" in kind -> NotificationClickAction.OpenPlans
        "payment" in kind || "subscription" in kind -> NotificationClickAction.OpenPayments
        "firm" in kind -> NotificationClickAction.OpenMyFirms
        "article" in kind || "news" in kind -> NotificationClickAction.OpenNews
        else -> NotificationClickAction.OpenNotifications
    }
}

fun notificationAction(n: NotificationDto): NotificationClickAction? {
    val data: JsonObject? = n.data
    val screen = data?.get("screen")?.jsonPrimitive?.contentOrNull
    val id = data?.get("id")?.jsonPrimitive?.contentOrNull
    return parseNotificationAction(screen, id, n.kind)
}
