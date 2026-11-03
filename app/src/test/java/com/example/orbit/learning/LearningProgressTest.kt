package com.example.orbit.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningProgressTest {

    @Test
    fun dailyPicksOnlyApplyToTheDayTheyWereChosen() {
        val progress = LearningProgress(
            dailyPoemDay = 42L,
            dailyPoemId = "poem-x",
            dailyWordDay = 42L,
            dailyWordId = "word-x",
        )

        assertEquals("poem-x", progress.poemIdFor(42L))
        assertEquals("word-x", progress.wordIdFor(42L))
        assertNull(progress.poemIdFor(43L))
        assertNull(progress.wordIdFor(43L))
    }

    @Test
    fun blankProgressHasNoPicksOrBookmarks() {
        val progress = LearningProgress()

        assertNull(progress.poemIdFor(7L))
        assertNull(progress.wordIdFor(7L))
        assertTrue(progress.bookmarkedWordIds.isEmpty())
    }

    @Test
    fun emptySearchReturnsTheWholeBook() {
        assertEquals(
            LearningLibrary.poems,
            LearningBrowse.filterPoems(LearningLibrary.poems, ""),
        )
        assertEquals(
            LearningLibrary.poems,
            LearningBrowse.filterPoems(LearningLibrary.poems, "   "),
        )
        assertEquals(
            LearningLibrary.english,
            LearningBrowse.filterWords(
                LearningLibrary.english,
                WordShelfFilter.ALL,
                emptySet(),
            ),
        )
    }

    @Test
    fun poemSearchMatchesTitleAuthorAndLines() {
        val byTitle = LearningBrowse.filterPoems(LearningLibrary.poems, "静夜思")
        assertTrue(byTitle.any { it.title == "静夜思" })

        val byAuthor = LearningBrowse.filterPoems(LearningLibrary.poems, "李白")
        assertTrue(byAuthor.isNotEmpty())
        assertTrue(byAuthor.any { it.author == "李白" })
        // Titles that mention 李白 (for example 杜甫's 天末怀李白) match too.
        assertTrue(byAuthor.any { it.author == "杜甫" && "李白" in it.title })

        val byLine = LearningBrowse.filterPoems(LearningLibrary.poems, "床前明月光")
        assertTrue(byLine.any { it.title == "静夜思" })

        val byDynasty = LearningBrowse.filterPoems(LearningLibrary.poems, "唐")
        assertTrue(byDynasty.isNotEmpty())
    }

    @Test
    fun poemSearchRequiresEveryTokenToMatch() {
        val both = LearningBrowse.filterPoems(LearningLibrary.poems, "李白 静夜")
        assertTrue(both.any { it.title == "静夜思" })

        val none = LearningBrowse.filterPoems(LearningLibrary.poems, "李白 苏轼")
        assertTrue(none.isEmpty())

        // Surrounding and repeated whitespace asks the same question.
        assertEquals(
            both,
            LearningBrowse.filterPoems(LearningLibrary.poems, "  李白   静夜  "),
        )
    }

    @Test
    fun poemSearchIsCaseInsensitiveForLatinLetters() {
        val poem = Poem(
            id = "mixed",
            title = "Moonlight Sonata",
            author = "Li Bai",
            dynasty = "Tang",
            lines = listOf("Quiet Night Thought"),
            note = "",
            appreciation = "",
        )

        assertEquals(listOf(poem), LearningBrowse.filterPoems(listOf(poem), "MOONLIGHT"))
        assertEquals(listOf(poem), LearningBrowse.filterPoems(listOf(poem), "li bai"))
        assertEquals(listOf(poem), LearningBrowse.filterPoems(listOf(poem), "quiet thought"))
    }

    @Test
    fun poemSearchThatMatchesNothingReturnsEmpty() {
        assertTrue(
            LearningBrowse.filterPoems(LearningLibrary.poems, "不存在的诗句搭配")
                .isEmpty(),
        )
    }

    @Test
    fun bookmarkFilterKeepsOnlySavedWordsInShelfOrder() {
        val targets = LearningLibrary.english.take(3).map { it.id }.toSet()
        val bookmarked = LearningBrowse.filterWords(
            LearningLibrary.english,
            WordShelfFilter.BOOKMARKED,
            targets,
        )

        assertEquals(3, bookmarked.size)
        assertEquals(
            LearningLibrary.english.take(3).map { it.id },
            bookmarked.map { it.id },
        )
        assertTrue(
            LearningBrowse.filterWords(
                LearningLibrary.english,
                WordShelfFilter.BOOKMARKED,
                emptySet(),
            ).isEmpty(),
        )
    }

    @Test
    fun unknownBookmarkIdsDoNotCreateRows() {
        val filtered = LearningBrowse.filterWords(
            LearningLibrary.english,
            WordShelfFilter.BOOKMARKED,
            setOf("missing-word"),
        )
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun pinnedPoemOverridesTheDailyRotation() {
        val pinned = LearningLibrary.poems.last()
        val day = 9L

        assertEquals(pinned, LearningBrowse.resolvePoem(day, pinned.id))
        assertEquals(LearningLibrary.poemFor(day), LearningBrowse.resolvePoem(day, null))
    }

    @Test
    fun unknownPinnedPoemFallsBackToTheRotation() {
        assertEquals(
            LearningLibrary.poemFor(11L),
            LearningBrowse.resolvePoem(11L, "missing-poem"),
        )
    }

    @Test
    fun pinnedWordOverridesTheDailyRotation() {
        val pinned = LearningLibrary.english.last()
        val day = 21L

        assertEquals(pinned, LearningBrowse.resolveWord(day, pinned.id))
        assertEquals(LearningLibrary.englishFor(day), LearningBrowse.resolveWord(day, null))
    }

    @Test
    fun unknownPinnedWordFallsBackToTheRotation() {
        assertEquals(
            LearningLibrary.englishFor(3L),
            LearningBrowse.resolveWord(3L, "missing-word"),
        )
    }
}
