package com.jworks.eigolens.data.repository

import com.jworks.eigolens.data.local.BookmarkDao
import com.jworks.eigolens.data.local.HistoryDao
import com.jworks.eigolens.data.local.entities.BookmarkedWordEntity
import com.jworks.eigolens.data.local.entities.LookupHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao
) {

    suspend fun recordLookup(
        word: String,
        scopeLevel: String,
        contextSnippet: String? = null,
        aiProvider: String? = null
    ) {
        historyDao.insertLookup(
            LookupHistoryEntity(
                word = word,
                scopeLevel = scopeLevel,
                timestamp = System.currentTimeMillis(),
                contextSnippet = contextSnippet?.take(200),
                aiProvider = aiProvider
            )
        )
    }

    suspend fun addBookmark(word: String, definition: String, contextSnippet: String? = null) {
        bookmarkDao.insertBookmark(
            BookmarkedWordEntity(
                word = word,
                definition = definition,
                contextSnippet = contextSnippet?.take(200),
                bookmarkedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeBookmark(word: String) {
        bookmarkDao.removeBookmark(word)
    }

    fun isBookmarked(word: String): Flow<Boolean> = bookmarkDao.isBookmarked(word)

    fun getRecentHistory(limit: Int = 100): Flow<List<LookupHistoryEntity>> =
        historyDao.getRecentLookups(limit)

    fun getAllBookmarks(): Flow<List<BookmarkedWordEntity>> = bookmarkDao.getAllBookmarks()

    fun getHistoryCount(): Flow<Int> = historyDao.getLookupCount()

    fun getBookmarkCount(): Flow<Int> = bookmarkDao.getBookmarkCount()

    suspend fun clearHistory() = historyDao.clearHistory()
}
