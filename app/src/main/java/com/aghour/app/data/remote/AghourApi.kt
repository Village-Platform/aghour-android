package com.aghour.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Rails API under /api/v1 (see aghour_services README). Null query params are omitted. */
interface AghourApi {

    @GET("categories")
    suspend fun categories(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
    ): ListResponse<CategoryDto>

    @GET("categories/{id}/firms")
    suspend fun categoryFirms(
        @Path("id") categoryId: Long,
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): ListResponse<FirmDto>

    /** Visible firms, optionally filtered by name and/or category. mine=true -> the signed-in owner's firms. */
    @GET("firms")
    suspend fun firms(
        @Query("q") query: String? = null,
        @Query("category_id") categoryId: Long? = null,
        @Query("mine") mine: Boolean? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): ListResponse<FirmDto>

    @GET("firms/{id}")
    suspend fun firm(@Path("id") id: Long): ItemResponse<FirmDto>

    @GET("articles")
    suspend fun articles(
        @Query("kind") kind: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): ListResponse<ArticleDto>

    @GET("articles/{id}")
    suspend fun article(@Path("id") id: Long): ItemResponse<ArticleDto>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ItemResponse<AuthPayload>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): ItemResponse<AuthPayload>

    @GET("me")
    suspend fun me(): ItemResponse<UserDto>

    @PATCH("me")
    suspend fun updateMe(@Body body: UpdateMeRequest): ItemResponse<UserDto>

    /** 204 on success. Plain Unit (not Response<Unit>) so a 422 wrong password throws into safeApiCall. */
    @HTTP(method = "DELETE", path = "me", hasBody = true)
    suspend fun deleteMe(@Body body: DeleteMeRequest)

    // ---- Firm owners: plans, payment, subscriptions ----

    @GET("subscription_plans")
    suspend fun plans(): ListResponse<SubscriptionPlanDto>

    @GET("payment_methods")
    suspend fun paymentMethods(): DataList<PaymentMethodDto>

    @GET("subscriptions")
    suspend fun subscriptions(@Query("per_page") perPage: Int = 50): ListResponse<SubscriptionDto>

    @POST("subscriptions")
    suspend fun createSubscription(@Body body: CreateSubscriptionRequest): ItemResponse<SubscriptionDto>

    // ---- Notifications ----

    @GET("notifications")
    suspend fun notifications(@Query("page") page: Int = 1, @Query("per_page") perPage: Int = 30): ListResponse<NotificationDto>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): ItemResponse<NotificationDto>

    @PATCH("notifications/read_all")
    suspend fun markAllNotificationsRead(): retrofit2.Response<Unit>
}
