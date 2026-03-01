package com.jworks.eigosage.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.jworks.eigosage.domain.ai.AiProvider
import com.jworks.eigosage.domain.ai.AiResponse
import com.jworks.eigosage.domain.ai.AnalysisContext
import io.ktor.client.HttpClient
import io.ktor.client.request.header
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

class ClaudeProvider(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL
) : AiProvider {

    companion object {
        private const val TAG = "ClaudeProvider"
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val API_VERSION = "2023-06-01"
        const val DEFAULT_MODEL = "claude-haiku-4-5-20251001"
    }

    override val name: String = "Claude"

    override val isAvailable: Boolean
        get() = apiKey.isNotBlank()

    override suspend fun analyze(context: AnalysisContext): Result<AiResponse> {
        if (!isAvailable) return Result.failure(IllegalStateException("Claude API key not configured"))

        val startTime = System.currentTimeMillis()
        val prompt = AiPrompts.buildPrompt(context)

        val requestBody = buildJsonObject {
            put("model", model)
            put("max_tokens", 4096)
            put("system", AiPrompts.SYSTEM_PROMPT)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    if (context.croppedImage != null) {
                        // Vision: send cropped image + text prompt
                        putJsonArray("content") {
                            addJsonObject {
                                put("type", "image")
                                putJsonObject("source") {
                                    put("type", "base64")
                                    put("media_type", "image/jpeg")
                                    put("data", bitmapToBase64(context.croppedImage))
                                }
                            }
                            addJsonObject {
                                put("type", "text")
                                put("text", prompt)
                            }
                        }
                    } else {
                        put("content", prompt)
                    }
                }
            }
        }.toString()

        return try {
            val response = httpClient.post(API_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", apiKey)
                header("anthropic-version", API_VERSION)
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            val elapsed = System.currentTimeMillis() - startTime

            if (response.status.value != 200) {
                Log.e(TAG, "API error ${response.status.value}: $responseText")
                return Result.failure(RuntimeException("Claude API error: ${response.status.value}"))
            }

            val json = Json.parseToJsonElement(responseText).jsonObject
            val content = json["content"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: return Result.failure(RuntimeException("Empty response from Claude"))

            val usage = json["usage"]?.jsonObject
            val inputTokens = usage?.get("input_tokens")?.jsonPrimitive?.content?.toIntOrNull()
            val outputTokens = usage?.get("output_tokens")?.jsonPrimitive?.content?.toIntOrNull()
            val tokensUsed = if (inputTokens != null || outputTokens != null) {
                (inputTokens ?: 0) + (outputTokens ?: 0)
            } else null

            Log.d(TAG, "Response in ${elapsed}ms, tokens: $tokensUsed (in=$inputTokens, out=$outputTokens)")

            Result.success(
                AiResponse(
                    content = content,
                    provider = "Claude",
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
