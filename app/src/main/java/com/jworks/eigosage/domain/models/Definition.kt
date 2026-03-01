package com.jworks.eigosage.domain.models

data class Definition(
    val word: String,
    val lemma: String,
    val frequency: Int?,
    val meanings: List<Meaning>,
    val phonetic: String? = null,
    val cefrLevel: String? = null
)

data class Meaning(
    val partOfSpeech: PartOfSpeech,
    val definition: String,
    val examples: List<String>,
    val synonyms: List<String>,
    val antonyms: List<String>
)

enum class PartOfSpeech(val label: String) {
    NOUN("noun"),
    VERB("verb"),
    ADJECTIVE("adj"),
    ADVERB("adv"),
    PREPOSITION("prep"),
    CONJUNCTION("conj"),
    INTERJECTION("intj"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(pos: String?): PartOfSpeech {
            if (pos == null) return UNKNOWN
            return when (pos.lowercase().trim()) {
                "noun", "n" -> NOUN
                "verb", "v" -> VERB
                "adjective", "adj", "a", "satellite", "s" -> ADJECTIVE
                "adverb", "adv", "r" -> ADVERB
                "preposition", "prep" -> PREPOSITION
                "conjunction", "conj" -> CONJUNCTION
                "interjection", "intj" -> INTERJECTION
                else -> UNKNOWN
            }
        }
    }
}
