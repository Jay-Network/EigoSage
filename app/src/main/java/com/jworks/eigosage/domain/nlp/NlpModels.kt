package com.jworks.eigosage.domain.nlp

data class Token(
    val text: String,
    val normalized: String,
    val position: Int,
    val index: Int
)

data class TokenAnnotation(
    val token: Token,
    val lemma: String,
    val pos: PosTag,
    val entity: EntityType?,
    val isStopWord: Boolean
)

enum class PosTag(val label: String) {
    NOUN("noun"),
    VERB("verb"),
    ADJECTIVE("adj"),
    ADVERB("adv"),
    PRONOUN("pron"),
    PREPOSITION("prep"),
    CONJUNCTION("conj"),
    DETERMINER("det"),
    AUXILIARY("aux"),
    INTERJECTION("intj"),
    NUMBER("num"),
    UNKNOWN("unknown")
}

enum class EntityType {
    PERSON,
    LOCATION,
    ORGANIZATION,
    DATE,
    NUMBER,
    UNKNOWN
}

data class NamedEntity(
    val text: String,
    val type: EntityType,
    val confidence: Float
)

data class NlpResult(
    val tokens: List<TokenAnnotation>,
    val entities: List<NamedEntity>,
    val processingTimeMs: Long
) {
    fun lemmaFor(word: String): String? {
        val normalized = word.lowercase().trim()
        return tokens.firstOrNull { it.token.normalized == normalized }?.lemma
    }

    fun posFor(word: String): PosTag? {
        val normalized = word.lowercase().trim()
        return tokens.firstOrNull { it.token.normalized == normalized }?.pos
    }
}
