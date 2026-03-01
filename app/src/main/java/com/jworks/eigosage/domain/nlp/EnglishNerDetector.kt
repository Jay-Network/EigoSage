package com.jworks.eigosage.domain.nlp

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnglishNerDetector @Inject constructor() {

    private val personTitles = setOf(
        "mr", "mrs", "ms", "dr", "prof", "sir", "lady", "lord",
        "president", "senator", "governor", "mayor", "captain",
        "general", "colonel", "sergeant", "officer", "detective"
    )

    private val locationIndicators = setOf(
        "street", "st", "avenue", "ave", "road", "rd", "boulevard", "blvd",
        "drive", "dr", "lane", "ln", "court", "ct", "place", "pl",
        "city", "town", "village", "county", "state", "country",
        "river", "lake", "mountain", "mt", "island", "ocean", "sea",
        "north", "south", "east", "west"
    )

    private val orgSuffixes = setOf(
        "inc", "corp", "ltd", "llc", "co", "company", "group",
        "foundation", "institute", "university", "college", "school",
        "hospital", "association", "society", "organization", "bank",
        "airlines", "airways"
    )

    private val datePatterns = listOf(
        Regex("""\b\d{1,2}/\d{1,2}/\d{2,4}\b"""),
        Regex("""\b\d{4}-\d{2}-\d{2}\b"""),
        Regex("""\b(?:january|february|march|april|may|june|july|august|september|october|november|december)\s+\d{1,2}(?:,?\s+\d{4})?\b""", RegexOption.IGNORE_CASE),
        Regex("""\b\d{1,2}\s+(?:january|february|march|april|may|june|july|august|september|october|november|december)(?:\s+\d{4})?\b""", RegexOption.IGNORE_CASE)
    )

    private val numberPattern = Regex("""\b\d[\d,.]*\b""")

    private val months = setOf(
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december"
    )

    private val commonNonEntities = setOf(
        "i", "the", "a", "an", "this", "that", "these", "those",
        "my", "your", "his", "her", "its", "our", "their",
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
        "english", "french", "spanish", "german", "chinese", "japanese"
    )

    fun detect(text: String): List<NamedEntity> {
        val entities = mutableListOf<NamedEntity>()

        // Detect dates first (higher priority)
        for (pattern in datePatterns) {
            for (match in pattern.findAll(text)) {
                entities.add(NamedEntity(match.value, EntityType.DATE, 0.9f))
            }
        }

        // Detect numbers
        for (match in numberPattern.findAll(text)) {
            val value = match.value
            // Skip if already part of a date entity
            if (entities.any { it.text.contains(value) }) continue
            entities.add(NamedEntity(value, EntityType.NUMBER, 0.8f))
        }

        // Detect capitalized entities (Title Case mid-sentence)
        detectCapitalizedEntities(text, entities)

        return entities
    }

    fun classifyEntity(word: String, context: List<String>): EntityType? {
        val lower = word.lowercase()
        if (lower in commonNonEntities) return null

        // Check if preceded by a person title
        val prevWords = context.takeLast(2).map { it.lowercase() }
        if (prevWords.any { it.removeSuffix(".") in personTitles }) {
            return EntityType.PERSON
        }

        // Check if followed by location indicator
        // (context here represents surrounding words; caller passes following words)
        if (context.any { it.lowercase().removeSuffix(".") in locationIndicators }) {
            return EntityType.LOCATION
        }

        // Check if word itself or neighbors suggest organization
        if (context.any { it.lowercase().removeSuffix(".") in orgSuffixes }) {
            return EntityType.ORGANIZATION
        }

        // Default: if capitalized mid-sentence, likely a proper noun
        return EntityType.UNKNOWN
    }

    private fun detectCapitalizedEntities(text: String, entities: MutableList<NamedEntity>) {
        val sentences = text.split(Regex("""[.!?]+\s*"""))
        for (sentence in sentences) {
            val words = sentence.trim().split(Regex("""\s+"""))
            if (words.size < 2) continue

            // Skip the first word (sentence-initial capitalization)
            for (i in 1 until words.size) {
                val word = words[i].trimEnd(',', ';', ':', '"', '\'', ')', ']')
                if (word.isEmpty()) continue

                // Title Case: first letter uppercase, rest lowercase/mixed
                if (word[0].isUpperCase() && word.length > 1) {
                    val lower = word.lowercase()
                    if (lower in commonNonEntities) continue
                    if (lower in months) continue
                    // Skip ALL_CAPS words (likely acronyms, handled differently)
                    if (word.all { it.isUpperCase() } && word.length <= 4) continue

                    // Collect consecutive capitalized words (multi-word entities)
                    val entityWords = mutableListOf(word)
                    var j = i + 1
                    while (j < words.size) {
                        val next = words[j].trimEnd(',', ';', ':', '"', '\'', ')', ']')
                        if (next.isNotEmpty() && next[0].isUpperCase()) {
                            entityWords.add(next)
                            j++
                        } else break
                    }

                    val entityText = entityWords.joinToString(" ")
                    // Skip if already detected
                    if (entities.any { it.text == entityText }) continue

                    val following = if (j < words.size) words.subList(j, minOf(j + 2, words.size)) else emptyList()
                    val type = classifyEntity(entityText, following) ?: continue
                    entities.add(NamedEntity(entityText, type, 0.6f))
                }
            }
        }
    }
}
