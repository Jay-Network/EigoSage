package com.jworks.eigosage.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarked_words",
    indices = [Index(value = ["word"], unique = true)]
)
data class BookmarkedWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val definition: String,
    @ColumnInfo(name = "context_snippet") val contextSnippet: String? = null,
    @ColumnInfo(name = "bookmarked_at") val bookmarkedAt: Long
)
