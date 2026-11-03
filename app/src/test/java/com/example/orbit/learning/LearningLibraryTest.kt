package com.example.orbit.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningLibraryTest {

    @Test
    fun librarySizesCoverTheShelfCounts() {
        assertEquals(309, LearningLibrary.poems.size)
        assertEquals(3877, LearningLibrary.english.size)
        assertEquals(18, LearningLibrary.math.size)
        assertEquals(24, QuoteLibrary.quotes.size)
    }

    @Test
    fun bundledBooksLoadRealContent() {
        assertTrue(LearningLibrary.poems.any { it.title == "静夜思" && it.author == "李白" })
        assertTrue(LearningLibrary.poems.any { it.title == "长恨歌" })
        assertTrue(LearningLibrary.english.any { it.word == "abandon" })

        LearningLibrary.poems.forEach { poem ->
            assertTrue("poem ${poem.id} needs a title", poem.title.isNotBlank())
            assertTrue("poem ${poem.id} needs an author", poem.author.isNotBlank())
            assertTrue("poem ${poem.id} needs lines", poem.lines.isNotEmpty())
            assertTrue("poem ${poem.id} needs a first line", poem.firstLine.isNotBlank())
        }
        LearningLibrary.english.forEach { word ->
            assertTrue("word ${word.id} needs a headword", word.word.isNotBlank())
            assertTrue("word ${word.id} needs a meaning", word.meaning.isNotBlank())
        }
    }

    @Test
    fun idsAreUniqueWithinEveryCollection() {
        assertEquals(
            LearningLibrary.poems.size,
            LearningLibrary.poems.map { it.id }.distinct().size,
        )
        assertEquals(
            LearningLibrary.english.size,
            LearningLibrary.english.map { it.id }.distinct().size,
        )
        assertEquals(
            LearningLibrary.math.size,
            LearningLibrary.math.map { it.id }.distinct().size,
        )
        assertEquals(
            QuoteLibrary.quotes.size,
            QuoteLibrary.quotes.map { it.id }.distinct().size,
        )
    }

    @Test
    fun everyGradeHasThreeMathTopics() {
        MathGrade.entries.forEach { grade ->
            assertEquals(
                "${grade.label} should have exactly three topics",
                3,
                LearningLibrary.mathForGrade(grade).size,
            )
        }
        assertEquals(
            LearningLibrary.math.size,
            MathGrade.entries.sumOf { LearningLibrary.mathForGrade(it).size },
        )
    }

    @Test
    fun gradeFilterOnlyReturnsMatchingTopics() {
        LearningLibrary.mathForGrade(MathGrade.GRADE_10).forEach { topic ->
            assertEquals(MathGrade.GRADE_10, topic.grade)
        }
        assertEquals(
            listOf("exponent-rules", "log-rules", "trig-identity"),
            LearningLibrary.mathForGrade(MathGrade.GRADE_10).map { it.id },
        )
    }

    @Test
    fun dailyRotationIsDeterministicAndInRange() {
        listOf(-1L, 0L, 1L, 17L, 400L).forEach { day ->
            assertEquals(
                LearningLibrary.poemFor(day),
                LearningLibrary.poemFor(day),
            )
            assertEquals(
                LearningLibrary.englishFor(day),
                LearningLibrary.englishFor(day),
            )
            assertEquals(
                LearningLibrary.mathFor(day),
                LearningLibrary.mathFor(day),
            )
        }

        val poemCycle = (0L until LearningLibrary.poems.size.toLong())
            .map { LearningLibrary.poemFor(it).id }
            .distinct()
        assertEquals(LearningLibrary.poems.size, poemCycle.size)
        val wordCycle = (0L until LearningLibrary.english.size.toLong())
            .map { LearningLibrary.englishFor(it).id }
            .distinct()
        assertEquals(LearningLibrary.english.size, wordCycle.size)
        val mathCycle = (0L..17L).map { LearningLibrary.mathFor(it).id }.distinct()
        assertEquals(LearningLibrary.math.size, mathCycle.size)
    }

    @Test
    fun idLookupsRoundTripAndRejectUnknownIds() {
        LearningLibrary.poems.forEach { assertEquals(it, LearningLibrary.poemById(it.id)) }
        LearningLibrary.english.forEach { assertEquals(it, LearningLibrary.englishById(it.id)) }
        LearningLibrary.math.forEach { assertEquals(it, LearningLibrary.mathById(it.id)) }

        assertNull(LearningLibrary.poemById("missing"))
        assertNull(LearningLibrary.englishById(null))
        assertNull(LearningLibrary.mathById("missing"))
    }

    @Test
    fun promptsCarryTheContentNeededByTheAssistant() {
        val poem = LearningLibrary.poems.first()
        val poemPrompt = LearningLibrary.poemPrompt(poem)
        assertTrue(poemPrompt.contains(poem.title))
        assertTrue(poemPrompt.contains(poem.firstLine))
        assertTrue(poemPrompt.contains("理解题"))

        val word = LearningLibrary.english.first()
        val wordPrompt = LearningLibrary.englishPrompt(word)
        assertTrue(wordPrompt.contains(word.word))
        assertTrue(wordPrompt.contains(word.phonetic))
        assertTrue(wordPrompt.contains("填空题"))

        val topic = LearningLibrary.mathForGrade(MathGrade.GRADE_11).first()
        val mathPrompt = LearningLibrary.mathPrompt(topic)
        assertTrue(mathPrompt.contains(topic.title))
        assertTrue(mathPrompt.contains(topic.grade.label))
        assertTrue(mathPrompt.contains(topic.formula))
    }

    @Test
    fun quoteForDayCyclesEveryQuote() {
        val seen = (0L..23L).map { QuoteLibrary.forDay(it).id }
        assertEquals(QuoteLibrary.quotes.size, seen.distinct().size)

        assertEquals(QuoteLibrary.forDay(3L), QuoteLibrary.forDay(27L))

        var quote = QuoteLibrary.forDay(0L)
        val walked = mutableSetOf(quote.id)
        repeat(QuoteLibrary.quotes.size - 1) {
            quote = QuoteLibrary.next(quote)
            walked += quote.id
        }
        assertEquals(QuoteLibrary.quotes.size, walked.size)
        assertEquals(
            QuoteLibrary.forDay(0L),
            QuoteLibrary.next(QuoteLibrary.quotes.last()),
        )
    }

    @Test
    fun unknownQuoteFallsBackToTheFirstEntry() {
        assertEquals(
            QuoteLibrary.quotes.first(),
            QuoteLibrary.next(Quote("missing", "x", "y")),
        )
        assertNotNull(QuoteLibrary.forDay(Long.MIN_VALUE))
    }
}
