package com.jworks.eigolens.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jworks.eigolens.data.local.entities.WordEntry
import com.jworks.eigolens.data.local.entities.DefinitionEntry

@Database(
    entities = [WordEntry::class, DefinitionEntry::class],
    version = 2,
    exportSchema = false
)
abstract class WordNetDatabase : RoomDatabase() {
    abstract fun wordNetDao(): WordNetDao
}
