package com.jworks.eigosage.domain.nlp

import com.jworks.eigosage.data.local.WordNetDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnglishPosTagger @Inject constructor(
    private val wordNetDao: WordNetDao
) {
    // Function word dictionaries (closed-class words with known POS)
    private val determiners = setOf(
        "the", "a", "an", "this", "that", "these", "those",
        "my", "your", "his", "her", "its", "our", "their",
        "some", "any", "no", "every", "each", "all", "both",
        "few", "several", "many", "much", "enough", "either", "neither",
        "which", "what", "whose"
    )

    private val pronouns = setOf(
        "i", "me", "my", "mine", "myself",
        "you", "your", "yours", "yourself", "yourselves",
        "he", "him", "his", "himself",
        "she", "her", "hers", "herself",
        "it", "its", "itself",
        "we", "us", "our", "ours", "ourselves",
        "they", "them", "their", "theirs", "themselves",
        "who", "whom", "whose", "which", "that",
        "what", "whoever", "whatever", "whichever",
        "this", "that", "these", "those",
        "something", "anything", "nothing", "everything",
        "someone", "anyone", "no one", "everyone",
        "somebody", "anybody", "nobody", "everybody",
        "one", "ones", "none"
    )

    private val prepositions = setOf(
        "about", "above", "across", "after", "against", "along", "among",
        "around", "at", "before", "behind", "below", "beneath", "beside",
        "between", "beyond", "by", "despite", "down", "during", "except",
        "for", "from", "in", "inside", "into", "near", "of", "off",
        "on", "onto", "out", "outside", "over", "past", "since",
        "through", "throughout", "to", "toward", "towards", "under",
        "underneath", "until", "up", "upon", "with", "within", "without"
    )

    private val conjunctions = setOf(
        "and", "but", "or", "nor", "for", "yet", "so",
        "although", "because", "since", "unless", "while", "whereas",
        "if", "though", "even", "whether", "however", "therefore",
        "moreover", "furthermore", "nevertheless", "meanwhile",
        "otherwise", "instead", "besides", "accordingly", "thus"
    )

    private val auxiliaries = setOf(
        "be", "am", "is", "are", "was", "were", "been", "being",
        "have", "has", "had", "having",
        "do", "does", "did",
        "will", "would", "shall", "should",
        "may", "might", "can", "could", "must",
        "need", "dare", "ought"
    )

    private val interjections = setOf(
        "oh", "ah", "wow", "hey", "ouch", "oops", "yay", "ugh",
        "hmm", "huh", "yeah", "yes", "no", "okay", "ok", "well",
        "hello", "hi", "bye", "goodbye", "please", "thanks", "sorry"
    )

    val stopWords = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to",
        "for", "of", "with", "by", "from", "is", "am", "are", "was",
        "were", "be", "been", "being", "have", "has", "had", "do",
        "does", "did", "will", "would", "could", "should", "may",
        "might", "shall", "can", "must", "it", "its", "i", "me",
        "my", "we", "us", "our", "you", "your", "he", "him", "his",
        "she", "her", "they", "them", "their", "this", "that", "not"
    )

    // Suffix -> POS heuristics
    private val nounSuffixes = listOf(
        "tion", "sion", "ment", "ness", "ity", "ance", "ence",
        "dom", "ship", "hood", "ism", "ist", "er", "or",
        "ure", "age", "ery", "ary"
    )

    private val adjSuffixes = listOf(
        "ful", "less", "ous", "ious", "ive", "able", "ible",
        "al", "ial", "ical", "ant", "ent", "ish", "like", "esque"
    )

    private val verbSuffixes = listOf(
        "ize", "ise", "ify", "ate", "en"
    )

    private val advSuffixes = listOf(
        "ly", "ward", "wards", "wise"
    )

    fun tag(word: String): PosTag {
        val lower = word.lowercase().trim()
        if (lower.isEmpty()) return PosTag.UNKNOWN

        // 1. Check function word dictionaries (highest confidence)
        if (lower in determiners) return PosTag.DETERMINER
        if (lower in auxiliaries) return PosTag.AUXILIARY
        if (lower in prepositions) return PosTag.PREPOSITION
        if (lower in conjunctions) return PosTag.CONJUNCTION
        if (lower in pronouns) return PosTag.PRONOUN
        if (lower in interjections) return PosTag.INTERJECTION

        // 2. Check number patterns
        if (lower.all { it.isDigit() || it == ',' || it == '.' }) return PosTag.NUMBER

        // 3. Use suffix heuristics for open-class words
        return tagBySuffix(lower)
    }

    suspend fun tagWithValidation(word: String): PosTag {
        val lower = word.lowercase().trim()
        if (lower.isEmpty()) return PosTag.UNKNOWN

        // 1. Check function word dictionaries first
        val functionTag = tag(word)
        if (functionTag != PosTag.UNKNOWN) return functionTag

        // 2. Check WordNet for POS data
        val wordNetTag = getWordNetPos(lower)
        if (wordNetTag != null) return wordNetTag

        // 3. Fall back to suffix heuristics
        return tagBySuffix(lower)
    }

    fun isStopWord(word: String): Boolean = word.lowercase().trim() in stopWords

    private fun tagBySuffix(word: String): PosTag {
        // Check adverb first (-ly is very reliable)
        for (suffix in advSuffixes) {
            if (word.endsWith(suffix) && word.length > suffix.length + 2) return PosTag.ADVERB
        }

        // Check adjective suffixes
        for (suffix in adjSuffixes) {
            if (word.endsWith(suffix) && word.length > suffix.length + 2) return PosTag.ADJECTIVE
        }

        // Check noun suffixes
        for (suffix in nounSuffixes) {
            if (word.endsWith(suffix) && word.length > suffix.length + 1) return PosTag.NOUN
        }

        // Check verb suffixes
        for (suffix in verbSuffixes) {
            if (word.endsWith(suffix) && word.length > suffix.length + 1) return PosTag.VERB
        }

        return PosTag.UNKNOWN
    }

    private suspend fun getWordNetPos(word: String): PosTag? {
        val definitions = wordNetDao.getDefinitions(word)
        if (definitions.isEmpty()) return null

        // Count definitions per POS and return the most common
        val posCounts = definitions.groupingBy { it.pos }.eachCount()
        val dominantPos = posCounts.maxByOrNull { it.value }?.key ?: return null

        return when (dominantPos?.lowercase()) {
            "n" -> PosTag.NOUN
            "v" -> PosTag.VERB
            "a", "s" -> PosTag.ADJECTIVE
            "r" -> PosTag.ADVERB
            else -> null
        }
    }
}
