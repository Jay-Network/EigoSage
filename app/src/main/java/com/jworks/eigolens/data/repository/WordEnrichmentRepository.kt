package com.jworks.eigolens.data.repository

import android.util.Log
import android.util.LruCache
import com.jworks.eigolens.data.local.WordNetDao
import com.jworks.eigolens.domain.models.CefrLevel
import com.jworks.eigolens.domain.models.EnrichedWord
import com.jworks.eigolens.domain.models.OCRResult
import com.jworks.eigolens.domain.nlp.NlpProcessor
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
