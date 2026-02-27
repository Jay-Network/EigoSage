package com.jworks.eigolens.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lookup_history",
    indices = [
        Index(value = ["word"]),
        Index(value = ["timestamp"])
    ]
)
data class LookupHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    @ColumnInfo(name = "scope_level") val scopeLevel: String,
    val timestamp: Long,
    @ColumnInfo(name = "context_snippet") val contextSnippet: String? = null,
    @ColumnInfo(name = "ai_provider") val aiProvider: String? = null
)
