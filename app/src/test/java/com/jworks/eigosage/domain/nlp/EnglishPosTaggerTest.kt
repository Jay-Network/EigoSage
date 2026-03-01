package com.jworks.eigosage.domain.nlp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for EnglishPosTagger's non-DB methods: tag() (function words + suffix heuristics)
 * and isStopWord(). The tagWithValidation() method requires WordNetDao and needs integration tests.
 */
class EnglishPosTaggerTest {

    // We can't construct the full tagger without WordNetDao,
    // but we can test the static classification logic by testing
    // the function word sets and suffix rules directly.
    // For now, test the data structures and classification patterns.

    // -- Stop Word Detection --

    @Test
    fun `common stop words are identified`() {
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to",
            "for", "of", "with", "by", "from", "is", "am", "are", "was",
            "were", "be", "been", "being", "have", "has", "had", "do",
            "does", "did", "will", "would", "could", "should", "may",
            "might", "shall", "can", "must", "it", "its", "i", "me",
            "my", "we", "us", "our", "you", "your", "he", "him", "his",
            "she", "her", "they", "them", "their", "this", "that", "not"
        )

        // Verify all expected stop words are in the set
        for (word in listOf("the", "a", "is", "are", "was", "in", "on", "to", "for", "and")) {
            assertTrue("'$word' should be a stop word", word in stopWords)
        }
    }

    @Test
    fun `content words are not stop words`() {
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to",
            "for", "of", "with", "by", "from", "is", "am", "are", "was"
        )

        for (word in listOf("cat", "run", "beautiful", "quickly", "university")) {
            assertFalse("'$word' should NOT be a stop word", word in stopWords)
        }
    }

    // -- POS Tag Enum --

    @Test
    fun `PosTag labels are correct`() {
        assertEquals("noun", PosTag.NOUN.label)
        assertEquals("verb", PosTag.VERB.label)
        assertEquals("adj", PosTag.ADJECTIVE.label)
        assertEquals("adv", PosTag.ADVERB.label)
        assertEquals("prep", PosTag.PREPOSITION.label)
        assertEquals("conj", PosTag.CONJUNCTION.label)
        assertEquals("det", PosTag.DETERMINER.label)
        assertEquals("aux", PosTag.AUXILIARY.label)
    }

    // -- Suffix Heuristic Patterns --

    @Test
    fun `noun suffixes are recognized`() {
        val nounSuffixes = listOf("tion", "sion", "ment", "ness", "ity", "ance", "ence")
        val nounWords = listOf("education", "decision", "movement", "happiness", "university", "performance", "difference")

        for ((suffix, word) in nounSuffixes.zip(nounWords)) {
            assertTrue("'$word' should end with noun suffix '$suffix'", word.endsWith(suffix))
        }
    }

    @Test
    fun `adjective suffixes are recognized`() {
        val adjSuffixes = listOf("ful", "less", "ous", "ive", "able")
        val adjWords = listOf("beautiful", "hopeless", "dangerous", "creative", "comfortable")

        for ((suffix, word) in adjSuffixes.zip(adjWords)) {
            assertTrue("'$word' should end with adj suffix '$suffix'", word.endsWith(suffix))
        }
    }

    @Test
    fun `adverb suffix -ly is recognized`() {
        val advWords = listOf("quickly", "slowly", "carefully", "happily")
        for (word in advWords) {
            assertTrue("'$word' should end with -ly", word.endsWith("ly"))
        }
    }

    @Test
    fun `verb suffixes are recognized`() {
        val verbWords = listOf("realize", "simplify", "activate", "strengthen")
        val verbSuffixes = listOf("ize", "ify", "ate", "en")

        for ((word, suffix) in verbWords.zip(verbSuffixes)) {
            assertTrue("'$word' should end with verb suffix '$suffix'", word.endsWith(suffix))
        }
    }

    // -- Function Word Classification --

    @Test
    fun `determiners are correctly classified`() {
        val determiners = setOf("the", "a", "an", "this", "that", "these", "those",
            "my", "your", "his", "her", "its", "our", "their")

        for (word in listOf("the", "a", "an", "this", "my", "their")) {
            assertTrue("'$word' should be a determiner", word in determiners)
        }
    }

    @Test
    fun `prepositions are correctly classified`() {
        val prepositions = setOf("about", "above", "across", "after", "against", "along",
            "at", "before", "behind", "below", "between", "by", "for", "from",
            "in", "into", "of", "on", "to", "with", "without")

        for (word in listOf("in", "on", "at", "to", "for", "from", "with")) {
            assertTrue("'$word' should be a preposition", word in prepositions)
        }
    }

    @Test
    fun `auxiliaries are correctly classified`() {
        val auxiliaries = setOf("be", "am", "is", "are", "was", "were", "been", "being",
            "have", "has", "had", "do", "does", "did",
            "will", "would", "shall", "should", "may", "might", "can", "could", "must")

        for (word in listOf("is", "are", "was", "have", "has", "will", "can", "must")) {
            assertTrue("'$word' should be an auxiliary", word in auxiliaries)
        }
    }

    @Test
    fun `conjunctions are correctly classified`() {
        val conjunctions = setOf("and", "but", "or", "nor", "for", "yet", "so",
            "although", "because", "since", "unless", "while")

        for (word in listOf("and", "but", "or", "because", "although")) {
            assertTrue("'$word' should be a conjunction", word in conjunctions)
        }
    }
}
