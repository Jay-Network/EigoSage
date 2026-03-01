package com.jworks.eigosage.data.ai

import android.util.Log
import com.jworks.eigosage.domain.ai.AiResponse
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

class GeminiChatClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL
) {
    companion object {
        private const val TAG = "GeminiChatClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val DEFAULT_MODEL = "gemini-2.5-flash"

        private const val SYSTEM_PROMPT = """You are an English language tutor helping a user understand text they captured with EigoSage (an English reading assistant app). Be concise, helpful, and friendly. Use simple English when possible. If asked to translate, provide the translation along with brief notes on nuance. Format responses with markdown bold for key terms and bullet points for lists."""
    }

    val isAvailable: Boolean get() = apiKey.isNotBlank()

    suspend fun send(
        messages: List<Pair<String, String>> // (role, content) — "user" or "model"
    ): Result<AiResponse> {
        if (!isAvailable) return Result.failure(IllegalStateException("Gemini API key not configured"))

        val startTime = System.currentTimeMillis()
        val url = "$BASE_URL/$model:generateContent?key=$apiKey"

        val requestBody = buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") {
                    add(buildJsonObject { put("text", SYSTEM_PROMPT) })
                }
            }
            putJsonArray("contents") {
                for ((role, content) in messages) {
                    add(buildJsonObject {
                        put("role", role)
                        putJsonArray("parts") {
                            add(buildJsonObject { put("text", content) })
                        }
                    })
                }
            }
            putJsonObject("generationConfig") {
                put("maxOutputTokens", 1024)
                put("temperature", 0.5)
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

            Log.d(TAG, "Chat response in ${elapsed}ms, tokens: $tokensUsed (in=$inputTokens, out=$outputTokens)")

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
            Log.e(TAG, "Chat request failed", e)
            Result.failure(e)
        }
    }
}
