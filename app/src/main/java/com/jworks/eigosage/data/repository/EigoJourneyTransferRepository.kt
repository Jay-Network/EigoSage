package com.jworks.eigosage.data.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.jworks.eigosage.domain.models.EnrichedWord
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @SerialName("source_app") val sourceApp: String = "eigosage",
    @SerialName("sender_device_id") val senderDeviceId: String,
    @SerialName("target_device_id") val targetDeviceId: String,
    @SerialName("target_app") val targetApp: String = "eigoquest"
)

@Singleton
class EigoQuestTransferRepository @Inject constructor(
    @Named("jcoin") private val supabaseClient: SupabaseClient,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "EQTransfer"
        private const val TABLE = "eq_received_words"
    }

    private val androidId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    suspend fun sendWords(words: List<EnrichedWord>): Result<Int> {
        if (words.isEmpty()) return Result.success(0)

        return try {
            val batchId = UUID.randomUUID().toString()
            val deviceId = androidId

            val rows = words.mapNotNull { word ->
                val cefr = word.cefr ?: return@mapNotNull null
                EigoQuestWordRow(
                    batchId = batchId,
                    word = word.text,
                    ipa = word.ipa,
                    cefrLevel = cefr.name,
                    senderDeviceId = deviceId,
                    targetDeviceId = deviceId, // same device
                    targetApp = "eigoquest"
                )
            }.distinctBy { it.word }

            if (rows.isEmpty()) return Result.success(0)

            supabaseClient.postgrest[TABLE].upsert(
                rows,
                onConflict = "word,sender_device_id"
            )

            Log.d(TAG, "Sent ${rows.size} words to EigoQuest (batch=$batchId, device=$deviceId)")
            Result.success(rows.size)
        } catch (e: Exception) {
            Log.w(TAG, "Transfer failed", e)
            Result.failure(e)
        }
    }
}
