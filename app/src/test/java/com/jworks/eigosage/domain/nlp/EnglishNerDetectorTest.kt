package com.jworks.eigosage.domain.nlp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EnglishNerDetectorTest {

    private lateinit var detector: EnglishNerDetector

    @Before
    fun setup() {
        detector = EnglishNerDetector()
    }

    // -- Date Detection --

    @Test
    fun `detects date in slash format`() {
        val entities = detector.detect("The meeting is on 12/25/2025.")
        assertTrue(entities.any { it.type == EntityType.DATE && it.text.contains("12/25/2025") })
    }

    @Test
    fun `detects date in ISO format`() {
        val entities = detector.detect("The deadline is 2025-01-15.")
        assertTrue(entities.any { it.type == EntityType.DATE && it.text.contains("2025-01-15") })
    }

    @Test
    fun `detects date with month name`() {
        val entities = detector.detect("She was born on January 5, 2000.")
        assertTrue(entities.any { it.type == EntityType.DATE })
    }

    // -- Number Detection --

    @Test
    fun `detects plain numbers`() {
        val entities = detector.detect("There are 42 students in the class.")
        assertTrue(entities.any { it.type == EntityType.NUMBER && it.text == "42" })
    }

    @Test
    fun `detects numbers with commas`() {
        val entities = detector.detect("The population is 1,000,000.")
        assertTrue(entities.any { it.type == EntityType.NUMBER })
    }

    // -- Named Entity Detection (Mid-sentence Capitalization) --

    @Test
    fun `detects capitalized names mid-sentence`() {
        val entities = detector.detect("I saw John walking down the street.")
        assertTrue("Should detect 'John' as an entity",
            entities.any { it.text == "John" })
    }

    @Test
    fun `detects multi-word entity`() {
        val entities = detector.detect("I visited New York last summer.")
        assertTrue("Should detect 'New York' as an entity",
            entities.any { it.text == "New York" })
    }

    @Test
    fun `skips sentence-initial capitalization`() {
        val entities = detector.detect("The cat sat on the mat.")
        // "The" is sentence-initial, should not be detected as entity
        assertTrue("Should not detect sentence-initial 'The' as entity",
            entities.none { it.text == "The" })
    }

    @Test
    fun `skips common non-entity words`() {
        val entities = detector.detect("I think English is hard to learn.")
        // "I" and "English" are in commonNonEntities
        assertTrue("Should not detect 'I' as entity",
            entities.none { it.text == "I" })
    }

    // -- Entity Classification --

    @Test
    fun `classifyEntity returns PERSON for person title context`() {
        val type = detector.classifyEntity("Smith", listOf("Dr."))
        assertEquals(EntityType.PERSON, type)
    }

    @Test
    fun `classifyEntity returns LOCATION for location indicator context`() {
        val type = detector.classifyEntity("Main", listOf("Street"))
        assertEquals(EntityType.LOCATION, type)
    }

    @Test
    fun `classifyEntity returns ORGANIZATION for org suffix context`() {
        val type = detector.classifyEntity("Apple", listOf("Inc"))
        assertEquals(EntityType.ORGANIZATION, type)
    }

    @Test
    fun `classifyEntity returns null for common non-entities`() {
        val type = detector.classifyEntity("the", emptyList())
        assertEquals(null, type)
    }

    // -- Edge Cases --

    @Test
    fun `empty text returns no entities`() {
        val entities = detector.detect("")
        assertTrue(entities.isEmpty())
    }

    @Test
    fun `all lowercase text returns only numbers and dates`() {
        val entities = detector.detect("the cat sat on the mat at 3pm.")
        // Should only find number "3", no named entities
        assertTrue(entities.all { it.type == EntityType.NUMBER || it.type == EntityType.DATE })
    }

    @Test
    fun `detect returns entities with confidence`() {
        val entities = detector.detect("I met Dr. Smith in New York on 01/15/2025.")
        for (entity in entities) {
            assertTrue("Confidence should be positive", entity.confidence > 0f)
            assertTrue("Confidence should be <= 1.0", entity.confidence <= 1f)
        }
    }
}
