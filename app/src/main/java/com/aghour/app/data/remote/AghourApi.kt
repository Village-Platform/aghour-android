package com.aghour.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
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
}
