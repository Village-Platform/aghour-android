package com.aghourservices.data.repository

import com.aghourservices.data.remote.AghourApi
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.remote.CreateSubscriptionRequest
import com.aghourservices.data.remote.FirmDto
import com.aghourservices.data.remote.ListResponse
import com.aghourservices.data.remote.NewSubscription
import com.aghourservices.data.remote.NotificationDto
import com.aghourservices.data.remote.PaymentMethodDto
import com.aghourservices.data.remote.SubscriptionDto
import com.aghourservices.data.remote.SubscriptionPlanDto
import com.aghourservices.data.remote.safeApiCall
import kotlinx.serialization.json.Json

/** Everything a firm owner needs: plans, paying for a subscription, their firms and notifications. */
class OwnerRepository(private val api: AghourApi, private val json: Json) {

    suspend fun plans(): ApiResult<List<SubscriptionPlanDto>> =
        safeApiCall(json) { api.plans().data.sortedBy { it.position } }

    suspend fun paymentMethods(): ApiResult<List<PaymentMethodDto>> =
        safeApiCall(json) { api.paymentMethods().data }

    suspend fun subscriptions(page: Int = 1, perPage: Int = 20): ApiResult<ListResponse<SubscriptionDto>> =
        safeApiCall(json) { api.subscriptions(page = page, perPage = perPage) }

    suspend fun subscribe(planId: Long, method: String, reference: String, payerPhone: String?): ApiResult<SubscriptionDto> =
        safeApiCall(json) {
            api.createSubscription(
                CreateSubscriptionRequest(
                    NewSubscription(
                        planId = planId,
                        paymentMethod = method,
                        transactionReference = reference.trim(),
                        payerPhone = payerPhone?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() },
                    ),
                ),
            ).data
        }

    suspend fun myFirms(page: Int = 1, perPage: Int = 20): ApiResult<ListResponse<FirmDto>> =
        safeApiCall(json) { api.firms(mine = true, page = page, perPage = perPage) }

    suspend fun notifications(page: Int = 1, perPage: Int = 20): ApiResult<ListResponse<NotificationDto>> =
        safeApiCall(json) { api.notifications(page = page, perPage = perPage) }

    suspend fun markRead(id: Long): ApiResult<NotificationDto> =
        safeApiCall(json) { api.markNotificationRead(id).data }

    suspend fun markAllRead(): ApiResult<Unit> =
        safeApiCall(json) { api.markAllNotificationsRead(); Unit }
}
