package com.jworks.eigolens.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jworks.eigolens.data.local.entities.LookupHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLookup(entity: LookupHistoryEntity)

    @Query("SELECT * FROM lookup_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLookups(limit: Int = 100): Flow<List<LookupHistoryEntity>>

    @Query("SELECT COUNT(*) FROM lookup_history")
    fun getLookupCount(): Flow<Int>

    @Query("DELETE FROM lookup_history")
    suspend fun clearHistory()
}
