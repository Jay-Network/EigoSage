package com.jworks.eigolens.data.jcoin

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class JCoinBalance(
    @SerialName("balance_coins") val balance: Int = 0,
    @SerialName("lifetime_earned") val lifetimeEarned: Long = 0,
    val tier: String = "bronze",
    @SerialName("earning_multiplier") val earningMultiplier: Double = 1.0,
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("monthly_earning") val monthlyEarning: MonthlyEarning? = null
)

@Serializable
data class MonthlyEarning(
    val source: String = "",
    val earned: Int = 0,
    val cap: Int = 200,
    val remaining: Int = 200
)

@Serializable
data class JCoinEarnResponse(
    @SerialName("transaction_id") val transactionId: String = "",
    @SerialName("amount_earned_coins") val coinsAwarded: Double = 0.0,
    @SerialName("balance_after_coins") val newBalance: Double = 0.0,
    @SerialName("tier_multiplier") val tierMultiplier: Double = 1.0,
    @SerialName("cap_info") val capInfo: CapInfo? = null,
    @SerialName("badges_earned") val badgesEarned: List<String> = emptyList(),
    @SerialName("streak_updated") val streakUpdated: String? = null
)

@Serializable
data class CapInfo(
    val source: String = "",
    @SerialName("earned_this_month") val earnedThisMonth: Int = 0,
    val cap: Int = 200,
    val remaining: Int = 200,
    @SerialName("partial_award") val partialAward: Boolean = false
)

@Singleton
class JCoinClient @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val TAG = "JCoinClient"
        private const val SOURCE_BUSINESS = "eigolens"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun unwrapData(body: String): kotlinx.serialization.json.JsonElement {
        val root = json.parseToJsonElement(body).jsonObject
        return root["data"] ?: throw Exception("Response missing 'data' field: $body")
    }

    private fun buildHeaders(accessToken: String?): Headers = Headers.build {
        if (accessToken != null) {
            append(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        append("X-Auth-Mode", "jwt_anonymous")
    }

    suspend fun getBalance(accessToken: String? = null): Result<JCoinBalance> {
        return try {
            val response = supabaseClient.functions.invoke(
                function = "jcoin-unified-balance",
                headers = buildHeaders(accessToken),
                body = buildJsonObject {
                    put("source_business", SOURCE_BUSINESS)
                }
            )
            val body = response.body<String>()
            val data = unwrapData(body)
            Result.success(json.decodeFromString<JCoinBalance>(data.toString()))
        } catch (e: Exception) {
            Log.w(TAG, "Balance fetch failed", e)
            Result.failure(e)
        }
    }

    suspend fun earn(
        accessToken: String? = null,
        sourceType: String,
        baseAmount: Int,
        metadata: Map<String, String> = emptyMap()
    ): Result<JCoinEarnResponse> {
        return try {
            val response = supabaseClient.functions.invoke(
                function = "jcoin-unified-earn",
                headers = buildHeaders(accessToken),
                body = buildJsonObject {
                    put("source_business", SOURCE_BUSINESS)
                    put("source_type", sourceType)
                    put("base_amount", baseAmount)
                    put("metadata", buildJsonObject {
                        metadata.forEach { (k, v) -> put(k, v) }
                    })
                }
            )
            val body = response.body<String>()
            val data = unwrapData(body)
            Result.success(json.decodeFromString<JCoinEarnResponse>(data.toString()))
        } catch (e: Exception) {
            val msg = e.message ?: ""
            when {
                msg.contains("MONTHLY_CAP_REACHED") ->
                    Log.d(TAG, "Monthly earn cap reached for source_type=$sourceType")
                else ->
                    Log.w(TAG, "Earn failed for source_type=$sourceType", e)
            }
            Result.failure(e)
        }
    }
}
