package com.aghourservices.data.analytics

import com.aghourservices.data.remote.ArticleDto
import com.aghourservices.data.remote.FirmDto
import com.aghourservices.data.remote.SubscriptionPlanDto

/**
 * Tracking plan: every analytics event the app sends, in one place (docs/analytics.md lists them
 * for people reading the dashboard).
 *
 * - Names are snake_case, at most 40 characters. GA4 recommended names (login, sign_up, search,
 *   share) are used where one fits, so Firebase's built-in reports understand them.
 * - Params carry ids, enum keys, counts and business names. Never a user's name, phone or email:
 *   Google Analytics forbids personal data.
 * - Renaming an event or a param splits its history in the dashboard. Add new ones instead.
 */
sealed class AnalyticsEvent(val name: String, val params: Map<String, Any?> = emptyMap()) {

    // ---- Auth ----

    data object Login : AnalyticsEvent("login", mapOf("method" to "phone"))

    data object SignUp : AnalyticsEvent("sign_up", mapOf("method" to "phone"))

    /** The server refused a login or sign-up (wrong password, phone already used…). */
    data class AuthFailed(val flow: AuthFlow, val httpStatus: Int?) :
        AnalyticsEvent("auth_failed", mapOf("flow" to flow.key, "http_status" to httpStatus))

    data object Logout : AnalyticsEvent("logout")

    data object PasswordChanged : AnalyticsEvent("password_changed")

    data object AccountDeleted : AnalyticsEvent("account_deleted")

    /** First-launch welcome screen closed: "start" browses as a guest, "login" opens sign in. */
    data class WelcomeCompleted(val choice: String) : AnalyticsEvent("welcome_completed", mapOf("choice" to choice))

    // ---- Discovery ----

    /** A search that returned its first page; [resultsCount] = 0 shows what people miss. */
    data class Search(val term: String, val categoryId: Long?, val resultsCount: Int) : AnalyticsEvent(
        "search",
        mapOf("search_term" to scrubSearchTerm(term), "category_id" to categoryId, "results_count" to resultsCount),
    )

    data class SelectCategory(val categoryId: Long, val categoryName: String?, val source: Source) : AnalyticsEvent(
        "select_category",
        mapOf("category_id" to categoryId, "category_name" to categoryName, "source" to source.key),
    )

    /** A firm page finished loading. */
    data class ViewFirm(val firm: FirmDto) : AnalyticsEvent(
        "view_firm",
        mapOf(
            "firm_id" to firm.id,
            "firm_name" to firm.name,
            "category_id" to firm.category.id,
            "category_name" to firm.category.name,
        ),
    )

    /** Call / WhatsApp / map tapped on a firm page: the moment the app delivered value. */
    data class ContactFirm(val firm: FirmDto, val channel: ContactChannel) : AnalyticsEvent(
        "contact_firm",
        mapOf("firm_id" to firm.id, "category_id" to firm.category.id, "channel" to channel.key),
    )

    data class Share(val contentType: String, val itemId: Long) :
        AnalyticsEvent("share", mapOf("content_type" to contentType, "item_id" to itemId))

    // ---- News ----

    /** null kind = the "all" chip. */
    data class FilterNews(val kind: String?) : AnalyticsEvent("filter_news", mapOf("kind" to (kind ?: "all")))

    /** An article page finished loading. */
    data class ViewArticle(val article: ArticleDto) : AnalyticsEvent(
        "view_article",
        mapOf("article_id" to article.id, "article_kind" to article.kind, "article_title" to article.title),
    )

    // ---- Account ----

    data class ChangeTheme(val theme: String) : AnalyticsEvent("change_theme", mapOf("theme" to theme))

    data class OpenNotification(val kind: String) : AnalyticsEvent("open_notification", mapOf("kind" to kind))

    data object MarkAllNotificationsRead : AnalyticsEvent("mark_all_notifications_read")

    // ---- Plans & payments ----

    data class SelectPlan(val plan: SubscriptionPlanDto) : AnalyticsEvent(
        "select_plan",
        mapOf("plan_id" to plan.id, "plan_name" to plan.name, "value" to plan.priceCents / 100.0, "currency" to CURRENCY),
    )

    /**
     * Transfer reference sent. Not a GA4 "purchase": an admin still has to confirm the transfer, so
     * revenue belongs on the server when the subscription is approved.
     */
    data class SubmitPayment(val planId: Long, val method: String, val priceCents: Int?) : AnalyticsEvent(
        "submit_payment",
        mapOf("plan_id" to planId, "payment_method" to method, "value" to priceCents?.div(100.0), "currency" to CURRENCY),
    )

    // ---- Content management (firm owners and the super admin) ----

    data class ManageContent(val entity: Entity, val action: Action) :
        AnalyticsEvent("manage_content", mapOf("entity" to entity.key, "action" to action.key))

    enum class AuthFlow(val key: String) { Login("login"), SignUp("sign_up") }
    enum class Source(val key: String) { Home("home"), Explore("explore") }
    enum class ContactChannel(val key: String) { Call("call"), WhatsApp("whatsapp"), Map("map") }
    enum class Entity(val key: String) { Firm("firm"), Category("category"), Article("article"), Plan("plan") }
    enum class Action(val key: String) { Create("create"), Update("update"), Delete("delete"), Approve("approve"), Suspend("suspend") }

    private companion object {
        const val CURRENCY = "EGP"
        val PhoneLike = Regex("\\d{7,}")

        /** People sometimes type a phone number into search; never send one to Analytics. */
        fun scrubSearchTerm(term: String): String = term.trim().lowercase().replace(PhoneLike, "[number]")
    }
}
