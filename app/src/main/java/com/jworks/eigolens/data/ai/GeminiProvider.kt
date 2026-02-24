package com.jworks.eigolens.data.ai

import android.util.Log
import com.jworks.eigolens.domain.ai.AiProvider
import com.jworks.eigolens.domain.ai.AiResponse
import com.jworks.eigolens.domain.ai.AnalysisContext
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
                        addJsonObject {
                            put("text", prompt)
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("maxOutputTokens", 1024)
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
            val tokensUsed = usageMetadata?.get("totalTokenCount")?.jsonPrimitive?.content?.toIntOrNull()

            Log.d(TAG, "Response in ${elapsed}ms, tokens: $tokensUsed")

            Result.success(
                AiResponse(
                    content = content,
                    provider = "Gemini",
                    model = model,
                    tokensUsed = tokensUsed,
                    processingTimeMs = elapsed
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Request failed", e)
            Result.failure(e)
        }
    }
}

// Extension for JsonArrayBuilder
private fun kotlinx.serialization.json.JsonArrayBuilder.addJsonObject(
    block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit
) {
    add(buildJsonObject(block))
}
