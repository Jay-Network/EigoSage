package com.jworks.eigosage.domain.analysis

import com.jworks.eigosage.domain.models.DifficultyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReadabilityCalculatorTest {

    private lateinit var calculator: ReadabilityCalculator

    @Before
    fun setup() {
        calculator = ReadabilityCalculator()
    }

    // -- Syllable Counting --

    @Test
    fun `syllable count for simple words`() {
        assertEquals(1, calculator.countSyllables("cat"))
        assertEquals(1, calculator.countSyllables("dog"))
        assertEquals(1, calculator.countSyllables("run"))
        assertEquals(1, calculator.countSyllables("the"))
    }

    @Test
    fun `syllable count for two-syllable words`() {
        assertEquals(2, calculator.countSyllables("happy"))
        assertEquals(2, calculator.countSyllables("water"))
        assertEquals(2, calculator.countSyllables("table"))
        assertEquals(2, calculator.countSyllables("paper"))
    }

    @Test
    fun `syllable count for multi-syllable words`() {
        assertEquals(3, calculator.countSyllables("beautiful"))
        assertEquals(3, calculator.countSyllables("library"))
        assertEquals(4, calculator.countSyllables("university"))
        assertEquals(3, calculator.countSyllables("computer"))
    }

    @Test
    fun `syllable count handles silent e`() {
        assertEquals(1, calculator.countSyllables("make"))
        assertEquals(1, calculator.countSyllables("like"))
        assertEquals(1, calculator.countSyllables("take"))
        assertEquals(1, calculator.countSyllables("home"))
    }

    @Test
    fun `syllable count minimum is 1`() {
        assertEquals(1, calculator.countSyllables("a"))
        assertEquals(1, calculator.countSyllables("I"))
        assertEquals(1, calculator.countSyllables(""))
    }

    // -- Text Statistics --

    @Test
    fun `analyzeText counts words correctly`() {
        val stats = calculator.analyzeText("The quick brown fox jumps over the lazy dog.")
        assertEquals(9, stats.totalWords)
    }

    @Test
    fun `analyzeText counts sentences correctly`() {
        val stats = calculator.analyzeText("Hello world. This is a test. How are you?")
        assertEquals(3, stats.totalSentences)
    }

    @Test
    fun `analyzeText handles single sentence`() {
        val stats = calculator.analyzeText("This is a single sentence.")
        assertEquals(1, stats.totalSentences)
        assertEquals(5, stats.totalWords)
    }

    @Test
    fun `analyzeText calculates averages`() {
        val stats = calculator.analyzeText("The cat sat. The dog ran.")
        assertEquals(2, stats.totalSentences)
        assertEquals(6, stats.totalWords)
        assertEquals(3.0, stats.averageWordsPerSentence, 0.01)
    }

    @Test
    fun `analyzeText counts polysyllabic words`() {
        val stats = calculator.analyzeText("The university provides education and beautiful experiences.")
        assertTrue(stats.polysyllableCount >= 2) // university, education, beautiful, experiences
    }

    // -- Full Readability Calculation --

    @Test
    fun `calculate returns null for very short text`() {
        assertNull(calculator.calculate("Hi."))
        assertNull(calculator.calculate(""))
    }

    @Test
    fun `calculate returns metrics for valid text`() {
        val text = "The cat sat on the mat. The dog ran in the park. It was a sunny day."
        val metrics = calculator.calculate(text)
        assertNotNull(metrics)
        metrics!!

        // Simple text should have low grade level
        assertTrue("Grade should be low for simple text: ${metrics.fleschKincaidGrade}",
            metrics.fleschKincaidGrade < 8.0)

        // Simple text should have high reading ease
        assertTrue("Reading ease should be high for simple text: ${metrics.fleschReadingEase}",
            metrics.fleschReadingEase > 60.0)
    }

    @Test
    fun `calculate produces higher grade for complex text`() {
        val simpleText = "The cat sat. The dog ran. It was fun."
        val complexText = "The philosophical implications of contemporary neuroscience " +
            "demonstrate unprecedented opportunities for understanding consciousness. " +
            "Furthermore, epistemological considerations fundamentally transform our " +
            "comprehension of phenomenological experiences."

        val simpleMetrics = calculator.calculate(simpleText)!!
        val complexMetrics = calculator.calculate(complexText)!!

        assertTrue("Complex text should have higher grade than simple text",
            complexMetrics.averageGrade > simpleMetrics.averageGrade)
    }

    @Test
    fun `flesch reading ease is between 0 and 100`() {
        val text = "The cat sat on the mat. It was a good day. The sun was bright."
        val metrics = calculator.calculate(text)!!

        assertTrue(metrics.fleschReadingEase >= 0.0)
        assertTrue(metrics.fleschReadingEase <= 100.0)
    }

    // -- Difficulty Levels --

    @Test
    fun `difficulty level classification`() {
        assertEquals(DifficultyLevel.VERY_EASY, DifficultyLevel.fromGrade(3.0))
        assertEquals(DifficultyLevel.EASY, DifficultyLevel.fromGrade(6.0))
        assertEquals(DifficultyLevel.MODERATE, DifficultyLevel.fromGrade(9.0))
        assertEquals(DifficultyLevel.DIFFICULT, DifficultyLevel.fromGrade(12.0))
        assertEquals(DifficultyLevel.VERY_DIFFICULT, DifficultyLevel.fromGrade(16.0))
    }

    @Test
    fun `difficulty level boundary values`() {
        assertEquals(DifficultyLevel.VERY_EASY, DifficultyLevel.fromGrade(5.0))
        assertEquals(DifficultyLevel.EASY, DifficultyLevel.fromGrade(7.0))
        assertEquals(DifficultyLevel.MODERATE, DifficultyLevel.fromGrade(10.0))
        assertEquals(DifficultyLevel.DIFFICULT, DifficultyLevel.fromGrade(13.0))
    }

    // -- All Four Formulas Produce Values --

    @Test
    fun `all four formulas produce non-zero values`() {
        val text = "Education is the most powerful weapon which you can use to change the world. " +
            "Knowledge is power. Information is liberating. Education is the premise of progress."
        val metrics = calculator.calculate(text)!!

        // All formulas should produce some value (not exactly 0)
        assertTrue("FK grade should be non-zero", metrics.fleschKincaidGrade != 0.0)
        assertTrue("FRE should be non-zero", metrics.fleschReadingEase != 0.0)
        assertTrue("SMOG should be non-zero", metrics.smogIndex != 0.0)
        assertTrue("CLI should be non-zero", metrics.colemanLiauIndex != 0.0)
    }

    @Test
    fun `target audience matches grade level`() {
        val text = "The cat sat on the mat. The dog ran fast. It was fun."
        val metrics = calculator.calculate(text)!!
        assertNotNull(metrics.targetAudience)
        assertTrue(metrics.targetAudience.isNotEmpty())
    }

    @Test
    fun `statistics are populated`() {
        val text = "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs."
        val metrics = calculator.calculate(text)!!

        assertTrue(metrics.statistics.totalWords > 0)
        assertTrue(metrics.statistics.totalSentences > 0)
        assertTrue(metrics.statistics.totalSyllables > 0)
        assertTrue(metrics.statistics.totalCharacters > 0)
        assertTrue(metrics.statistics.averageWordsPerSentence > 0)
        assertTrue(metrics.statistics.averageSyllablesPerWord > 0)
    }
}
