package com.jworks.eigolens.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
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
import javax.inject.Inject
import javax.inject.Singleton

data class ContextualInsight(
    val meaning: String,
    val partOfSpeech: String,
    val note: String? = null
)

/**
 * Uses Gemini Vision to extract text from an image more accurately than ML Kit.
 * Returns corrected text as a list of lines.
 */
@Singleton
class GeminiOcrCorrector @Inject constructor(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    companion object {
        private const val TAG = "GeminiOCR"
        private const val MODEL = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    val isAvailable: Boolean get() = apiKey.isNotBlank()

    suspend fun extractText(bitmap: Bitmap): Result<List<String>> {
        if (!isAvailable) return Result.failure(IllegalStateException("Gemini API key not set"))

        val startTime = System.currentTimeMillis()
        val base64Image = bitmapToBase64(bitmap)

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", EXTRACT_PROMPT)
                        })
                        add(buildJsonObject {
                            putJsonObject("inline_data") {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            }
                        })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("maxOutputTokens", 2048)
                put("temperature", 0.1)
            }
        }.toString()

        val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"

        return try {
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            val elapsed = System.currentTimeMillis() - startTime

            if (response.status.value != 200) {
                Log.e(TAG, "API error ${response.status.value}: $responseText")
                return Result.failure(RuntimeException("Gemini Vision error: ${response.status.value}"))
            }

            val json = Json.parseToJsonElement(responseText).jsonObject
            val content = json["candidates"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: return Result.failure(RuntimeException("Empty response from Gemini Vision"))

            val lines = content.lines().filter { it.isNotBlank() }
            Log.d(TAG, "Extracted ${lines.size} lines in ${elapsed}ms")

            Result.success(lines)
        } catch (e: Exception) {
            Log.e(TAG, "Vision OCR failed", e)
            Result.failure(e)
        }
    }

    /**
     * Given a tapped word and the full surrounding text, returns a contextual insight:
     * the most relevant meaning, usage note, and which part of speech applies here.
     */
    suspend fun getContextualInsight(word: String, surroundingText: String): Result<ContextualInsight> {
        if (!isAvailable) return Result.failure(IllegalStateException("Gemini API key not set"))

        val startTime = System.currentTimeMillis()
        val prompt = """Word: "$word"
Context: "$surroundingText"

For the word "$word" as used in the context above, provide:
1. MEANING: The specific meaning in this context (one clear sentence)
2. POS: The part of speech as used here (noun/verb/adj/adv/prep/conj)
3. NOTE: A brief usage or grammar note relevant to this context (optional, one sentence)

Format your response EXACTLY as:
MEANING: <meaning>
POS: <part of speech>
NOTE: <note>"""

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", prompt)
                        })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("maxOutputTokens", 256)
                put("temperature", 0.1)
            }
        }.toString()

        val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"

        return try {
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            val elapsed = System.currentTimeMillis() - startTime

            if (response.status.value != 200) {
                Log.e(TAG, "Insight API error ${response.status.value}: $responseText")
                return Result.failure(RuntimeException("Gemini error: ${response.status.value}"))
            }

            val json = Json.parseToJsonElement(responseText).jsonObject
            val content = json["candidates"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: return Result.failure(RuntimeException("Empty insight response"))

            val insight = parseInsight(content)
            Log.d(TAG, "Contextual insight for '$word' in ${elapsed}ms: ${insight.meaning}")

            Result.success(insight)
        } catch (e: Exception) {
            Log.e(TAG, "Contextual insight failed", e)
            Result.failure(e)
        }
    }

    private fun parseInsight(text: String): ContextualInsight {
        val lines = text.lines()
        var meaning = ""
        var pos = ""
        var note: String? = null

        for (line in lines) {
            when {
                line.startsWith("MEANING:", ignoreCase = true) ->
                    meaning = line.substringAfter(":").trim()
                line.startsWith("POS:", ignoreCase = true) ->
                    pos = line.substringAfter(":").trim()
                line.startsWith("NOTE:", ignoreCase = true) -> {
                    val n = line.substringAfter(":").trim()
                    if (n.isNotBlank()) note = n
                }
            }
        }

        return ContextualInsight(
            meaning = meaning.ifBlank { text.lines().firstOrNull()?.trim() ?: text },
            partOfSpeech = pos,
            note = note
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

private const val EXTRACT_PROMPT = """Extract ALL text from this image with perfect accuracy.
Rules:
- One line of text per output line
- Preserve the reading order (top to bottom, left to right)
- Use surrounding context to resolve ambiguous characters (e.g. "rn" vs "m", "l" vs "1", "O" vs "0")
- If a word looks misspelled but context suggests the correct spelling, output the CORRECT spelling
- Preserve original punctuation and capitalization
- Do NOT skip any text, even small or partial words
- Do NOT add any commentary, headings, labels, or markdown formatting
- Output ONLY the extracted text, nothing else"""
