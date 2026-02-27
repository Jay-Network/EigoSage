package com.jworks.eigolens.data.repository

import android.util.Log
import com.jworks.eigolens.data.jcoin.DeviceAuthRepository
import com.jworks.eigolens.domain.models.EnrichedWord
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Serializable
data class EigoQuestWordRow(
    @SerialName("batch_id") val batchId: String,
    val word: String,
    val ipa: String? = null,
    @SerialName("cefr_level") val cefrLevel: String = "B1",
    @SerialName("source_app") val sourceApp: String = "eigolens",
    @SerialName("sender_user_id") val senderUserId: String
)

@Singleton
class EigoQuestTransferRepository @Inject constructor(
    @Named("jcoin") private val supabaseClient: SupabaseClient,
    private val deviceAuthRepository: DeviceAuthRepository
) {
    companion object {
        private const val TAG = "EQTransfer"
        private const val TABLE = "eq_received_words"
    }

    suspend fun sendWords(words: List<EnrichedWord>): Result<Int> {
        if (words.isEmpty()) return Result.success(0)

        return try {
            val userId = deviceAuthRepository.getDeviceId()
            val batchId = UUID.randomUUID().toString()

            val rows = words.mapNotNull { word ->
                // Only send words that have CEFR level
                val cefr = word.cefr ?: return@mapNotNull null
                EigoQuestWordRow(
                    batchId = batchId,
                    word = word.text,
                    ipa = word.ipa,
                    cefrLevel = cefr.name,
                    senderUserId = userId
                )
            }.distinctBy { it.word } // dedupe within batch

            if (rows.isEmpty()) return Result.success(0)

            supabaseClient.postgrest[TABLE].upsert(
                rows,
                onConflict = "word,sender_user_id"
            )

            Log.d(TAG, "Sent ${rows.size} words to EigoQuest (batch=$batchId)")
            Result.success(rows.size)
        } catch (e: Exception) {
            Log.w(TAG, "Transfer failed", e)
            Result.failure(e)
        }
    }
}
