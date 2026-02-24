package com.jworks.eigolens.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [
        Index(value = ["word"], unique = true, name = "idx_words_word"),
        Index(value = ["lemma"], name = "idx_words_lemma"),
        Index(value = ["frequency"], name = "idx_words_frequency")
    ]
)
data class WordEntry(
    @PrimaryKey
    @ColumnInfo(name = "word_id")
    val wordId: Int,

    @ColumnInfo(name = "word")
    val word: String,

    @ColumnInfo(name = "lemma")
    val lemma: String,

    @ColumnInfo(name = "frequency", defaultValue = "999999")
    val frequency: Int? = 999999
)

@Entity(
    tableName = "definitions",
    indices = [
        Index(value = ["word_id"], name = "idx_definitions_word_id")
    ],
    foreignKeys = [
        ForeignKey(
            entity = WordEntry::class,
            parentColumns = ["word_id"],
            childColumns = ["word_id"]
        )
    ]
)
data class DefinitionEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "def_id")
    val defId: Int,

    @ColumnInfo(name = "word_id")
    val wordId: Int,

    @ColumnInfo(name = "pos")
    val pos: String?,

    @ColumnInfo(name = "meaning")
    val meaning: String,

    @ColumnInfo(name = "example")
    val example: String?,

    @ColumnInfo(name = "synonyms")
    val synonyms: String?,

    @ColumnInfo(name = "antonyms")
    val antonyms: String?
)
