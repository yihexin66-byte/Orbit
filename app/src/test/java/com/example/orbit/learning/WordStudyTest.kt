package com.example.orbit.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordStudyTest {

    private fun word(id: String) = EnglishWord(
        id = id,
        word = id,
        phonetic = "/$id/",
        meaning = "meaning-$id",
    )

    private val words = listOf(word("a"), word("b"), word("c"), word("d"), word("e"))

    @Test
    fun autoSelectionTakesUnseenWordsFirstInBookOrder() {
        val progress = WordStudyProgress(
            records = mapOf(
                "a" to WordStudyRecord(lastAnswerKnown = true, lastStudiedEpochDay = 1L),
                "b" to WordStudyRecord(lastAnswerKnown = false, lastStudiedEpochDay = 2L),
            ),
        )

        val picked = WordStudyPlanner.autoSelect(words, progress, count = 3)

        assertEquals(listOf("c", "d", "e"), picked.map { it.id })
    }

    @Test
    fun autoSelectionFallsBackToWrongWordsThenOldestStudied() {
        val progress = WordStudyProgress(
            records = mapOf(
                "a" to WordStudyRecord(lastAnswerKnown = true, lastStudiedEpochDay = 10L),
                "b" to WordStudyRecord(lastAnswerKnown = false, lastStudiedEpochDay = 4L),
                "c" to WordStudyRecord(lastAnswerKnown = true, lastStudiedEpochDay = 2L),
                "d" to WordStudyRecord(lastAnswerKnown = true, lastStudiedEpochDay = 7L),
                "e" to WordStudyRecord(lastAnswerKnown = false, lastStudiedEpochDay = 1L),
            ),
        )

        val picked = WordStudyPlanner.autoSelect(words, progress, count = 4)

        // Wrong bucket: e (day 1) then b (day 4); the rest is oldest-first.
        assertEquals(listOf("e", "b", "c", "d"), picked.map { it.id })
    }

    @Test
    fun autoSelectionNeverRepeatsAWordAndRespectsTheRoundCap() {
        val progress = WordStudyProgress(
            records = mapOf(
                "a" to WordStudyRecord(lastAnswerKnown = false, lastStudiedEpochDay = 1L),
                "b" to WordStudyRecord(lastAnswerKnown = false, lastStudiedEpochDay = 1L),
            ),
        )

        val picked = WordStudyPlanner.autoSelect(words, progress, count = 99)

        assertEquals(words.map { it.id }.toSet(), picked.map { it.id }.toSet())
        assertEquals(picked.size, picked.distinctBy { it.id }.size)
    }

    @Test
    fun autoSelectionStopsWhenTheLibraryIsExhausted() {
        val twoWords = listOf(word("a"), word("b"))
        val picked = WordStudyPlanner.autoSelect(
            words = twoWords,
            progress = WordStudyProgress(),
            count = 10,
        )

        assertEquals(listOf("a", "b"), picked.map { it.id })
        assertTrue(WordStudyPlanner.autoSelect(words, WordStudyProgress(), 0).isEmpty())
    }

    @Test
    fun manualSelectionKeepsBookOrderAndDropsUnknownIds() {
        val picked = WordStudyPlanner.manualSelect(
            words = words,
            ids = setOf("d", "a", "missing"),
        )

        assertEquals(listOf("a", "d"), picked.map { it.id })
    }

    @Test
    fun manualSelectionIsCappedAtFifty() {
        val many = (1..60).map { word("w$it") }
        val picked = WordStudyPlanner.manualSelect(many, many.map { it.id }.toSet())

        assertEquals(50, picked.size)
        assertEquals("w1", picked.first().id)
        assertEquals("w50", picked.last().id)
    }

    @Test
    fun targetIsClampedToTheSupportedRange() {
        assertEquals(1, WordStudyPlanner.clampTarget(0))
        assertEquals(50, WordStudyPlanner.clampTarget(51))
        assertEquals(10, WordStudyPlanner.clampTarget(10))
        assertEquals(10, WordStudyProgress().dailyTarget)
        assertEquals(30, WordStudyProgress().withTarget(30).dailyTarget)
        assertEquals(50, WordStudyProgress().withTarget(300).dailyTarget)
    }

    @Test
    fun aCardMustBeRevealedBeforeItCanBeAnswered() {
        val session = WordStudySession(words = listOf(word("a"), word("b")))

        assertEquals("a", session.current?.id)
        assertFalse(session.revealed)

        val unchanged = session.answer(known = true)
        assertEquals(session, unchanged)

        val revealed = session.reveal()
        assertTrue(revealed.revealed)
        val answered = revealed.answer(known = true)
        assertEquals(1, answered.index)
        assertFalse(answered.revealed)
        assertEquals("b", answered.current?.id)
    }

    @Test
    fun roundReportsAccuracyAndRetriesOnlyWrongWords() {
        val session = WordStudySession(words = words.take(3))
            .reveal().answer(known = true)
            .reveal().answer(known = false)
            .reveal().answer(known = true)

        assertTrue(session.isComplete)
        assertNull(session.current)
        assertEquals(3, session.answeredCount)
        assertEquals(2, session.knownCount)
        assertEquals(1, session.unknownCount)
        assertEquals(67, session.accuracyPercent)

        val retry = session.retryWrong()
        assertEquals(listOf("b"), retry?.words?.map { it.id })
        assertFalse(retry!!.isComplete)
    }

    @Test
    fun anAllKnownRoundHasNoRetry() {
        val session = WordStudySession(words = listOf(word("a")))
            .reveal().answer(known = true)

        assertNull(session.retryWrong())
    }

    @Test
    fun wordSearchMatchesSpellingAndMeaning() {
        val bySpelling = LearningBrowse.searchWords(LearningLibrary.english, "abandon")
        assertTrue(bySpelling.any { it.word.equals("abandon", ignoreCase = true) })

        val sample = LearningLibrary.english.first()
        val byMeaning = LearningBrowse.searchWords(LearningLibrary.english, sample.meaning)
        assertTrue(byMeaning.any { it.id == sample.id })

        assertEquals(
            LearningLibrary.english,
            LearningBrowse.searchWords(LearningLibrary.english, "   "),
        )
        assertTrue(LearningBrowse.searchWords(LearningLibrary.english, "zzzz-no-word").isEmpty())
    }

    @Test
    fun recordsUpdateCountsAndDailyTotals() {
        val progress = WordStudyProgress()
            .record("a", known = false, epochDay = 100L)
            .record("a", known = true, epochDay = 100L)
            .record("b", known = false, epochDay = 100L)

        assertEquals(2, progress.learnedCount)
        assertEquals(1, progress.reviewCount)
        assertEquals(2, progress.studiedOn(100L))
        assertEquals(1, progress.knownOn(100L))
        assertEquals(0, progress.studiedOn(101L))

        val recordA = progress.records.getValue("a")
        assertEquals(1, recordA.knownCount)
        assertEquals(1, recordA.unknownCount)
        assertEquals(2, recordA.timesStudied)
        assertEquals(true, recordA.lastAnswerKnown)
        assertEquals(100L, recordA.lastStudiedEpochDay)
    }
}
