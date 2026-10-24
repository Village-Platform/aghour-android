package com.aghour.app.data.repository

import com.aghour.app.data.remote.AghourApi
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.remote.CreateSubscriptionRequest
import com.aghour.app.data.remote.FirmDto
import com.aghour.app.data.remote.NewSubscription
import com.aghour.app.data.remote.NotificationDto
import com.aghour.app.data.remote.PaymentMethodDto
import com.aghour.app.data.remote.SubscriptionDto
import com.aghour.app.data.remote.SubscriptionPlanDto
import com.aghour.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json

/** Everything a firm owner needs: plans, paying for a subscription, their firms and notifications. */
class OwnerRepository(private val api: AghourApi, private val json: Json) {

    suspend fun plans(): ApiResult<List<SubscriptionPlanDto>> =
        safeApiCall(json) { api.plans().data.sortedBy { it.position } }

    suspend fun paymentMethods(): ApiResult<List<PaymentMethodDto>> =
        safeApiCall(json) { api.paymentMethods().data }

    suspend fun subscriptions(): ApiResult<List<SubscriptionDto>> =
        safeApiCall(json) { api.subscriptions().data }

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

    suspend fun myFirms(): ApiResult<List<FirmDto>> =
        safeApiCall(json) { api.firms(mine = true, perPage = 50).data }

    suspend fun notifications(): ApiResult<Pair<List<NotificationDto>, Int>> =
        safeApiCall(json) {
            val response = api.notifications()
            response.data to response.data.count { !it.read }
        }

    suspend fun markRead(id: Long): ApiResult<NotificationDto> =
        safeApiCall(json) { api.markNotificationRead(id).data }

    suspend fun markAllRead(): ApiResult<Unit> =
        safeApiCall(json) { api.markAllNotificationsRead(); Unit }
}
