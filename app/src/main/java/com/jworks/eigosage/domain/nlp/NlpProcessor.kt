package com.jworks.eigosage.domain.nlp

import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NlpProcessor @Inject constructor(
    private val lemmatizer: EnglishLemmatizer,
    private val posTagger: EnglishPosTagger,
    private val nerDetector: EnglishNerDetector
) {
    companion object {
        private const val LEMMA_CACHE_SIZE = 500
        private const val RESULT_CACHE_SIZE = 50
    }

    private val lemmaCache = LruCache<String, String>(LEMMA_CACHE_SIZE)
    private val resultCache = LruCache<String, NlpResult>(RESULT_CACHE_SIZE)

    suspend fun lemmatize(word: String): String {
        val key = word.lowercase().trim()
        if (key.isEmpty()) return word

        lemmaCache.get(key)?.let { return it }

        val lemma = lemmatizer.lemmatize(key)
        lemmaCache.put(key, lemma)
        return lemma
    }

    suspend fun analyze(text: String): NlpResult {
        if (text.isBlank()) return NlpResult(emptyList(), emptyList(), 0)

        // Check result cache (use first 100 chars as key to handle long texts)
        val cacheKey = text.take(100)
        resultCache.get(cacheKey)?.let { return it }

        val startTime = System.nanoTime()

        // Tokenize
        val tokens = tokenize(text)

        // NER detection (on full text, before lowercasing)
        val entities = nerDetector.detect(text)

        // Build entity text set for quick lookup
        val entityTexts = entities.map { it.text.lowercase() }.toSet()

        // Annotate each token
        val annotations = tokens.map { token ->
            val lemma = lemmatize(token.text)
            val pos = posTagger.tagWithValidation(token.normalized)
            val entityType = if (token.text[0].isUpperCase() && token.normalized in entityTexts) {
                entities.firstOrNull {
                    it.text.lowercase().contains(token.normalized)
                }?.type
            } else null
            val isStopWord = posTagger.isStopWord(token.normalized)

            TokenAnnotation(
                token = token,
                lemma = lemma,
                pos = pos,
                entity = entityType,
                isStopWord = isStopWord
            )
        }

        val processingTimeMs = (System.nanoTime() - startTime) / 1_000_000

        val result = NlpResult(annotations, entities, processingTimeMs)
        resultCache.put(cacheKey, result)
        return result
    }

    private fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val wordPattern = Regex("""\b[\w'-]+\b""")
        var index = 0

        for (match in wordPattern.findAll(text)) {
            val word = match.value
            // Skip pure punctuation or single quotes
            if (word.all { it == '\'' || it == '-' }) continue

            tokens.add(
                Token(
                    text = word,
                    normalized = word.lowercase().trim(),
                    position = match.range.first,
                    index = index++
                )
            )
        }
        return tokens
    }
}
