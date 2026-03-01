package com.jworks.eigosage.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jworks.eigosage.data.local.entities.BookmarkedWordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(entity: BookmarkedWordEntity)

    @Query("DELETE FROM bookmarked_words WHERE word = :word")
    suspend fun removeBookmark(word: String)

    @Query("SELECT * FROM bookmarked_words ORDER BY bookmarked_at DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedWordEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_words WHERE word = :word LIMIT 1)")
    fun isBookmarked(word: String): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM bookmarked_words")
    fun getBookmarkCount(): Flow<Int>
}
