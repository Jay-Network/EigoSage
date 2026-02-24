package com.jworks.eigolens.data.repository

import android.util.Log
import android.util.LruCache
import com.jworks.eigolens.data.local.WordNetDao
import com.jworks.eigolens.data.local.entities.WordWithDefinitions
import com.jworks.eigolens.domain.models.Definition
import com.jworks.eigolens.domain.models.Meaning
import com.jworks.eigolens.domain.models.PartOfSpeech
import com.jworks.eigolens.domain.nlp.NlpProcessor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefinitionRepository @Inject constructor(
    private val wordNetDao: WordNetDao,
    private val nlpProcessor: NlpProcessor
) {
    companion object {
        private const val TAG = "DefRepo"
        private const val CACHE_SIZE = 200
    }

    private val memoryCache = LruCache<String, Definition>(CACHE_SIZE)

    suspend fun getDefinition(word: String): Result<Definition> {
        val normalizedWord = word.lowercase().trim()
        if (normalizedWord.isEmpty()) return Result.failure(IllegalArgumentException("Empty word"))

        // Tier 1: Memory cache (instant)
        memoryCache.get(normalizedWord)?.let { cached ->
            Log.d(TAG, "Cache hit: $normalizedWord")
            return Result.success(cached)
        }

        // Tier 2: Local WordNet database (10-30ms)
        try {
            val wordWithDefs = wordNetDao.lookupWithDefinitions(normalizedWord)
            if (wordWithDefs != null && wordWithDefs.definitions.isNotEmpty()) {
                val definition = wordWithDefs.toDomain()
                memoryCache.put(normalizedWord, definition)
                Log.d(TAG, "DB hit: $normalizedWord (${wordWithDefs.definitions.size} defs)")
                return Result.success(definition)
            }

            // Try lemma form (e.g., "running" -> "run")
            val lemma = nlpProcessor.lemmatize(normalizedWord)
            if (lemma != normalizedWord) {
                val lemmaWithDefs = wordNetDao.lookupWithDefinitions(lemma)
                if (lemmaWithDefs != null && lemmaWithDefs.definitions.isNotEmpty()) {
                    val definition = lemmaWithDefs.toDomain()
                    memoryCache.put(normalizedWord, definition)
                    Log.d(TAG, "Lemma hit: $normalizedWord -> $lemma (${lemmaWithDefs.definitions.size} defs)")
                    return Result.success(definition)
                }
            }

            Log.d(TAG, "Not found: $normalizedWord (lemma: $lemma)")
            return Result.failure(NoSuchElementException("Word not found: $normalizedWord"))
        } catch (e: Exception) {
            Log.e(TAG, "DB error for $normalizedWord", e)
            return Result.failure(e)
        }
    }

    suspend fun preloadCommonWords(count: Int = 100) {
        try {
            val topWords = wordNetDao.getTopFrequentWords(count)
            var loaded = 0
            for (wordEntry in topWords) {
                if (memoryCache.get(wordEntry.word) != null) continue
                val withDefs = wordNetDao.lookupWithDefinitions(wordEntry.word)
                if (withDefs != null && withDefs.definitions.isNotEmpty()) {
                    memoryCache.put(wordEntry.word, withDefs.toDomain())
                    loaded++
                }
            }
            Log.d(TAG, "Preloaded $loaded common words into cache")
        } catch (e: Exception) {
            Log.e(TAG, "Preload failed", e)
        }
    }

    fun getCacheStats(): CacheStats {
        return CacheStats(
            size = memoryCache.size(),
            maxSize = memoryCache.maxSize(),
            hitCount = memoryCache.hitCount(),
            missCount = memoryCache.missCount()
        )
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}

data class CacheStats(
    val size: Int,
    val maxSize: Int,
    val hitCount: Int,
    val missCount: Int
) {
    val hitRate: Float
        get() {
            val total = hitCount + missCount
            return if (total > 0) hitCount.toFloat() / total else 0f
        }
}

private fun WordWithDefinitions.toDomain(): Definition {
    return Definition(
        word = word.word,
        lemma = word.lemma,
        frequency = word.frequency?.takeIf { it != 999999 },
        meanings = definitions.map { def ->
            Meaning(
                partOfSpeech = PartOfSpeech.fromString(def.pos),
                definition = def.meaning,
                examples = def.example?.let { listOf(it) } ?: emptyList(),
                synonyms = def.synonyms?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
                antonyms = def.antonyms?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            )
        }
    )
}
