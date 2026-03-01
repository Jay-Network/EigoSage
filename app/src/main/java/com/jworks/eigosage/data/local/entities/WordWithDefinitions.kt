package com.jworks.eigosage.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

data class WordWithDefinitions(
    @Embedded val word: WordEntry,
    @Relation(
        parentColumn = "word_id",
        entityColumn = "word_id"
    )
    val definitions: List<DefinitionEntry>
)
