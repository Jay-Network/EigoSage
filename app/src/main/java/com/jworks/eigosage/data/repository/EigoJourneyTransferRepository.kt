package com.jworks.eigosage.data.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.jworks.eigosage.domain.models.EnrichedWord
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Sends difficult words from EigoSage to EigoJourney via J Coin's `eq-word-transfer` edge
 * function (contract: ~/1_jworks/A_ai/3_System/J_Coin/docs/api-spec.md section 12).
 *
 * Replaces a direct `eq_received_words` table upsert that stopped working once the old J Coin
 * Supabase project went NXDOMAIN and table REST was closed (only functions paths are routed
 * now) — see BUGS.md / MISTAKES.md. UNVERIFIED as of 2026-10-11: the function is still in
 * jayhub:110 review and not live yet, so this has not been exercised against a real response.
 */
@Singleton
class EigoJourneyTransferRepository @Inject constructor(
    @Named("jcoin") private val supabaseClient: SupabaseClient,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "EJTransfer"
        private const val FUNCTION = "eq-word-transfer"
        private const val MAX_WORDS = 200
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val androidId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    suspend fun sendWords(words: List<EnrichedWord>): Result<Int> {
        if (words.isEmpty()) return Result.success(0)

        return try {
            val deviceId = androidId
            val payloadWords = words
                .mapNotNull { word -> word.cefr?.let { cefr -> Triple(word.text, word.ipa, cefr.name) } }
                .distinctBy { it.first }
                .take(MAX_WORDS)

            if (payloadWords.isEmpty()) return Result.success(0)

            val response = supabaseClient.functions.invoke(
                function = FUNCTION,
                body = buildJsonObject {
                    put("action", "send")
                    put("device_id", deviceId)
                    putJsonArray("words") {
                        payloadWords.forEach { (word, ipa, cefrLevel) ->
                            add(buildJsonObject {
                                put("word", word)
                                ipa?.let { put("ipa", it) }
                                put("cefr_level", cefrLevel)
                            })
                        }
                    }
                }
            )

            val body = response.body<String>()
            val root = json.parseToJsonElement(body).jsonObject
            if (root["success"]?.jsonPrimitive?.booleanOrNull != true) {
                return Result.failure(RuntimeException("eq-word-transfer returned success=false: $body"))
            }

            val sent = root["data"]?.jsonObject?.get("sent")?.jsonPrimitive?.intOrNull ?: payloadWords.size
            val batchId = root["data"]?.jsonObject?.get("batch_id")?.jsonPrimitive?.content

            Log.d(TAG, "Sent $sent words to EigoJourney via $FUNCTION (batch=$batchId, device=$deviceId)")
            Result.success(sent)
        } catch (e: Exception) {
            Log.w(TAG, "Transfer failed", e)
            Result.failure(e)
        }
    }
}
