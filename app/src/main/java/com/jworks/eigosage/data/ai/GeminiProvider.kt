package com.jworks.eigosage.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.jworks.eigosage.domain.ai.AiProvider
import com.jworks.eigosage.domain.ai.AiResponse
import com.jworks.eigosage.domain.ai.AnalysisContext
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream

class GeminiProvider(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL
) : AiProvider {

    companion object {
        private const val TAG = "GeminiProvider"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }

    override val name: String = "Gemini"

    override val isAvailable: Boolean
        get() = apiKey.isNotBlank()

    override suspend fun analyze(context: AnalysisContext): Result<AiResponse> {
        if (!isAvailable) return Result.failure(IllegalStateException("Gemini API key not configured"))

        val startTime = System.currentTimeMillis()
        val prompt = "${AiPrompts.SYSTEM_PROMPT}\n\n${AiPrompts.buildPrompt(context)}"
        val url = "$BASE_URL/$model:generateContent?key=$apiKey"

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        if (context.croppedImage != null) {
                            // Vision: send cropped image
                            addJsonObject {
                                putJsonObject("inline_data") {
                                    put("mime_type", "image/jpeg")
                                    put("data", bitmapToBase64(context.croppedImage))
                                }
                            }
                        }
                        addJsonObject {
                            put("text", prompt)
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("maxOutputTokens", 4096)
                put("temperature", 0.3)
            }
        }.toString()

        return try {
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            val elapsed = System.currentTimeMillis() - startTime

            if (response.status.value != 200) {
                Log.e(TAG, "API error ${response.status.value}: $responseText")
                return Result.failure(RuntimeException("Gemini API error: ${response.status.value}"))
            }

            val json = Json.parseToJsonElement(responseText).jsonObject
            val content = json["candidates"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: return Result.failure(RuntimeException("Empty response from Gemini"))

            val usageMetadata = json["usageMetadata"]?.jsonObject
            val inputTokens = usageMetadata?.get("promptTokenCount")?.jsonPrimitive?.content?.toIntOrNull()
            val outputTokens = usageMetadata?.get("candidatesTokenCount")?.jsonPrimitive?.content?.toIntOrNull()
            val tokensUsed = usageMetadata?.get("totalTokenCount")?.jsonPrimitive?.content?.toIntOrNull()

            Log.d(TAG, "Response in ${elapsed}ms, tokens: $tokensUsed (in=$inputTokens, out=$outputTokens)")

            Result.success(
                AiResponse(
                    content = content,
                    provider = "Gemini",
                    model = model,
                    tokensUsed = tokensUsed,
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    processingTimeMs = elapsed
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Request failed", e)
            Result.failure(e)
        }
    }
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

// Extension for JsonArrayBuilder
private fun kotlinx.serialization.json.JsonArrayBuilder.addJsonObject(
    block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit
) {
    add(buildJsonObject(block))
}
