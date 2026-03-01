package com.jworks.eigosage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jworks.eigosage.data.local.entities.BookmarkedWordEntity
import com.jworks.eigosage.data.local.entities.LookupHistoryEntity

@Database(
    entities = [LookupHistoryEntity::class, BookmarkedWordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
}
