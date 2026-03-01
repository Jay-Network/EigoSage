package com.jworks.eigosage.data.repository

import android.util.Log
import android.util.LruCache
import com.jworks.eigosage.data.local.WordNetDao
import com.jworks.eigosage.domain.models.CefrLevel
import com.jworks.eigosage.domain.models.EnrichedWord
import com.jworks.eigosage.domain.models.OCRResult
import com.jworks.eigosage.domain.nlp.NlpProcessor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordEnrichmentRepository @Inject constructor(
    private val wordNetDao: WordNetDao,
    private val nlpProcessor: NlpProcessor
) {
    companion object {
        private const val TAG = "WordEnrich"
        private const val CACHE_SIZE = 500
    }

    private data class WordMeta(
        val ipa: String?,
        val cefr: CefrLevel?,
        val briefDef: String?
    )

    private val cache = LruCache<String, WordMeta>(CACHE_SIZE)

    /**
     * Lightweight enrichment for live camera frames.
     * Only fetches IPA + CEFR (no brief definitions, no lemma fallback).
     * Uses cache first, single batch query for uncached words.
     */
    suspend fun enrichOcrWordsLive(ocrResult: OCRResult): List<EnrichedWord> {
        if (ocrResult.texts.isEmpty()) return emptyList()

        data class WordWithBounds(val text: String, val bounds: android.graphics.Rect?)
        val wordsWithBounds = mutableListOf<WordWithBounds>()

        for (detected in ocrResult.texts) {
            for (element in detected.elements) {
                if (!element.isWord) continue
                val clean = element.text.replace(Regex("[^a-zA-Z'-]"), "").trim().lowercase()
                if (clean.length >= 2) {
                    wordsWithBounds.add(WordWithBounds(clean, element.bounds))
                }
            }
        }

        val uniqueWords = wordsWithBounds.map { it.text }.distinct()
        val uncached = uniqueWords.filter { cache.get(it) == null }

        if (uncached.isNotEmpty()) {
            // Single batch query — no per-word getFirstMeaning, no lemmatization
            val entries = wordNetDao.getWordMetadataBatch(uncached)
            val entryMap = entries.associateBy { it.word }

            for (word in uncached) {
                val entry = entryMap[word]
                val meta = if (entry != null) {
                    WordMeta(
                        ipa = entry.phonetic,
                        cefr = CefrLevel.fromString(entry.cefrLevel),
                        briefDef = null // skip for live — not displayed
                    )
                } else {
                    WordMeta(null, null, null)
                }
                cache.put(word, meta)
            }
        }

        return wordsWithBounds.map { (text, bounds) ->
            val meta = cache.get(text) ?: WordMeta(null, null, null)
            EnrichedWord(
                text = text,
                bounds = bounds,
                ipa = meta.ipa,
                cefr = meta.cefr,
                briefDefinition = null
            )
        }
    }

    suspend fun enrichOcrWords(ocrResult: OCRResult): List<EnrichedWord> {
        val startTime = System.currentTimeMillis()

        // Collect unique words from OCR with their bounds
        data class WordWithBounds(val text: String, val bounds: android.graphics.Rect?)
        val wordsWithBounds = mutableListOf<WordWithBounds>()

        for (detected in ocrResult.texts) {
            for (element in detected.elements) {
                if (!element.isWord) continue
                val clean = element.text.replace(Regex("[^a-zA-Z'-]"), "").trim().lowercase()
                if (clean.length >= 2) {
                    wordsWithBounds.add(WordWithBounds(clean, element.bounds))
                }
            }
        }

        // Unique words to look up (not in cache)
        val uniqueWords = wordsWithBounds.map { it.text }.distinct()
        val uncached = uniqueWords.filter { cache.get(it) == null }

        if (uncached.isNotEmpty()) {
            // Also try lemma forms for better matching
            val lemmaMap = mutableMapOf<String, String>() // word -> lemma
            for (word in uncached) {
                val lemma = nlpProcessor.lemmatize(word)
                if (lemma != word) {
                    lemmaMap[word] = lemma
                }
            }

            val allLookupWords = (uncached + lemmaMap.values).distinct()

            // Batch query (Room handles chunking for >999 params)
            val entries = wordNetDao.getWordMetadataBatch(allLookupWords)
            val entryMap = entries.associateBy { it.word }

            // Fetch brief definitions for words found
            val briefDefs = mutableMapOf<String, String>()
            for (entry in entries) {
                val meaning = wordNetDao.getFirstMeaning(entry.word)
                if (meaning != null) {
                    briefDefs[entry.word] = meaning.take(80)
                }
            }

            // Populate cache
            for (word in uncached) {
                val entry = entryMap[word] ?: entryMap[lemmaMap[word]]
                val meta = if (entry != null) {
                    WordMeta(
                        ipa = entry.phonetic,
                        cefr = CefrLevel.fromString(entry.cefrLevel),
                        briefDef = briefDefs[entry.word] ?: briefDefs[lemmaMap[word]]
                    )
                } else {
                    WordMeta(null, null, null)
                }
                cache.put(word, meta)
            }
        }

        // Build enriched list
        val result = wordsWithBounds.map { (text, bounds) ->
            val meta = cache.get(text) ?: WordMeta(null, null, null)
            EnrichedWord(
                text = text,
                bounds = bounds,
                ipa = meta.ipa,
                cefr = meta.cefr,
                briefDefinition = meta.briefDef
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Enriched ${result.size} words (${uniqueWords.size} unique) in ${elapsed}ms")
        return result
    }
}
