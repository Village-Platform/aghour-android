package com.aghourservices.data.remote

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException
import java.io.IOException

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val message: String, val code: String? = null, val httpStatus: Int? = null) : ApiResult<Nothing>
}

/**
 * Where [safeApiCall] sends failures that are bugs rather than bad luck (a response the DTOs can't
 * decode). Set once by AppContainer, so the data layer never depends on Crashlytics directly.
 */
object ApiErrorReporter {
    @Volatile var report: (Throwable) -> Unit = {}
}

private const val NETWORK_ERROR = "تعذّر الاتصال بالخادم. تأكد من الإنترنت وحاول مرة أخرى."
private const val UNEXPECTED_ERROR = "حدث خطأ غير متوقع. حاول مرة أخرى."

/**
 * Runs an API call and turns every failure into a readable Arabic message.
 * The server already sends Arabic messages in { "error": { "message", "details" } }.
 */
suspend fun <T> safeApiCall(json: Json, block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        parseHttpError(json, e)
    } catch (e: IOException) {
        ApiResult.Failure(NETWORK_ERROR)
    } catch (e: SerializationException) {
        // Client and server disagree on a response shape: worth a non-fatal report.
        ApiErrorReporter.report(e)
        ApiResult.Failure(UNEXPECTED_ERROR)
    }

private fun parseHttpError(json: Json, e: HttpException): ApiResult.Failure {
    val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val body = raw?.let { runCatching { json.decodeFromString<ErrorEnvelope>(it).error }.getOrNull() }
        ?: return ApiResult.Failure(UNEXPECTED_ERROR, httpStatus = e.code())

    // Validation errors: show the first field message (already a full Arabic sentence from Rails).
    val firstDetail = body.details?.values
        ?.flatMap { value -> if (value is JsonArray) value.toList() else listOf(value) }
        ?.firstNotNullOfOrNull { (it as? JsonPrimitive)?.content }

    return ApiResult.Failure(firstDetail ?: body.message, code = body.code, httpStatus = e.code())
}
