package com.example.orbit.learning

/**
 * Local learning choices for one account: which poem / word was picked for a
 * given day, plus the bookmarked words. Both are deliberately local-only; the
 * bundled books themselves never change.
 */
data class LearningProgress(
    val dailyPoemDay: Long = 0L,
    val dailyPoemId: String? = null,
    val dailyWordDay: Long = 0L,
    val dailyWordId: String? = null,
    val bookmarkedWordIds: Set<String> = emptySet(),
) {
    /** The pinned poem is valid only for the day it was chosen. */
    fun poemIdFor(epochDay: Long): String? =
        dailyPoemId?.takeIf { dailyPoemDay == epochDay }

    fun wordIdFor(epochDay: Long): String? =
        dailyWordId?.takeIf { dailyWordDay == epochDay }
}

enum class WordShelfFilter {
    ALL,
    BOOKMARKED,
}

/**
 * Pure read-only helpers used by the book pages. Kept outside Compose so the
 * search and filter semantics can be tested on the JVM.
 */
object LearningBrowse {

    fun filterPoems(poems: List<Poem>, query: String): List<Poem> {
        val tokens = query.trim().lowercase().split(WHITESPACE)
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return poems
        return poems.filter { poem ->
            val haystack = buildString {
                append(poem.title)
                append(' ')
                append(poem.author)
                append(' ')
                append(poem.dynasty)
                append(' ')
                append(poem.lines.joinToString(" "))
            }.lowercase()
            tokens.all { haystack.contains(it) }
        }
    }

    fun filterWords(
        words: List<EnglishWord>,
        filter: WordShelfFilter,
        bookmarkedWordIds: Set<String>,
    ): List<EnglishWord> = when (filter) {
        WordShelfFilter.ALL -> words
        WordShelfFilter.BOOKMARKED -> words.filter { it.id in bookmarkedWordIds }
    }

    /** Search across spelling, phonetic and Chinese meaning for manual picks. */
    fun searchWords(words: List<EnglishWord>, query: String): List<EnglishWord> {
        val tokens = query.trim().lowercase().split(WHITESPACE)
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return words
        return words.filter { word ->
            val haystack = buildString {
                append(word.word)
                append(' ')
                append(word.phonetic)
                append(' ')
                append(word.meaning)
                append(' ')
                append(word.example)
                append(' ')
                append(word.exampleZh)
            }.lowercase()
            tokens.all { haystack.contains(it) }
        }
    }

    /**
     * A pinned choice wins only when its id still exists in the bundled book;
     * otherwise the normal date rotation is used instead of an empty card.
     */
    fun resolvePoem(epochDay: Long, pinnedId: String?): Poem =
        pinnedId?.let(LearningLibrary::poemById) ?: LearningLibrary.poemFor(epochDay)

    fun resolveWord(epochDay: Long, pinnedId: String?): EnglishWord =
        pinnedId?.let(LearningLibrary::englishById) ?: LearningLibrary.englishFor(epochDay)

    private val WHITESPACE = Regex("\\s+")
}
