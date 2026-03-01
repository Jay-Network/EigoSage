package com.jworks.eigosage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jworks.eigosage.data.local.entities.WordEntry
import com.jworks.eigosage.data.local.entities.DefinitionEntry

@Database(
    entities = [WordEntry::class, DefinitionEntry::class],
    version = 2,
    exportSchema = false
)
abstract class WordNetDatabase : RoomDatabase() {
    abstract fun wordNetDao(): WordNetDao
}
