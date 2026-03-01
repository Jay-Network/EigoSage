package com.jworks.eigosage.domain.nlp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NlpModelsTest {

    @Test
    fun `NlpResult lemmaFor finds matching token`() {
        val tokens = listOf(
            TokenAnnotation(
                token = Token("Running", "running", 0, 0),
                lemma = "run",
                pos = PosTag.VERB,
                entity = null,
                isStopWord = false
            ),
            TokenAnnotation(
                token = Token("cats", "cats", 8, 1),
                lemma = "cat",
                pos = PosTag.NOUN,
                entity = null,
                isStopWord = false
            )
        )

        val result = NlpResult(tokens, emptyList(), 5)
        assertEquals("run", result.lemmaFor("running"))
        assertEquals("run", result.lemmaFor("Running"))
        assertEquals("cat", result.lemmaFor("cats"))
    }

    @Test
    fun `NlpResult lemmaFor returns null for missing word`() {
        val result = NlpResult(emptyList(), emptyList(), 0)
        assertNull(result.lemmaFor("nonexistent"))
    }

    @Test
    fun `NlpResult posFor finds matching token`() {
        val tokens = listOf(
            TokenAnnotation(
                token = Token("quickly", "quickly", 0, 0),
                lemma = "quick",
                pos = PosTag.ADVERB,
                entity = null,
                isStopWord = false
            )
        )

        val result = NlpResult(tokens, emptyList(), 1)
        assertEquals(PosTag.ADVERB, result.posFor("quickly"))
    }

    @Test
    fun `PosTag labels are correct`() {
        assertEquals("noun", PosTag.NOUN.label)
        assertEquals("verb", PosTag.VERB.label)
        assertEquals("adj", PosTag.ADJECTIVE.label)
        assertEquals("adv", PosTag.ADVERB.label)
        assertEquals("pron", PosTag.PRONOUN.label)
        assertEquals("prep", PosTag.PREPOSITION.label)
        assertEquals("conj", PosTag.CONJUNCTION.label)
        assertEquals("det", PosTag.DETERMINER.label)
        assertEquals("aux", PosTag.AUXILIARY.label)
        assertEquals("num", PosTag.NUMBER.label)
    }

    @Test
    fun `EntityType enum values exist`() {
        assertEquals(6, EntityType.entries.size)
        EntityType.PERSON
        EntityType.LOCATION
        EntityType.ORGANIZATION
        EntityType.DATE
        EntityType.NUMBER
        EntityType.UNKNOWN
    }

    @Test
    fun `NamedEntity holds data correctly`() {
        val entity = NamedEntity("New York", EntityType.LOCATION, 0.85f)
        assertEquals("New York", entity.text)
        assertEquals(EntityType.LOCATION, entity.type)
        assertEquals(0.85f, entity.confidence, 0.001f)
    }

    @Test
    fun `Token preserves original case`() {
        val token = Token("Hello", "hello", 0, 0)
        assertEquals("Hello", token.text)
        assertEquals("hello", token.normalized)
        assertEquals(0, token.position)
        assertEquals(0, token.index)
    }
}
