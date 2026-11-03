package com.example.orbit.learning

/**
 * Skip-the-word study state. A word is shown alone first, the learner decides
 * whether it is known, and the meaning is revealed only after that decision.
 * The whole layer is plain Kotlin so selection order and round accounting can
 * be tested without a device.
 */
data class WordStudyRecord(
    val knownCount: Int = 0,
    val unknownCount: Int = 0,
    val lastStudiedEpochDay: Long = 0L,
    /** Null until the word has been through at least one round. */
    val lastAnswerKnown: Boolean? = null,
) {
    val timesStudied: Int get() = knownCount + unknownCount
}

data class WordStudyProgress(
    val records: Map<String, WordStudyRecord> = emptyMap(),
    val dailyTarget: Int = WordStudyPlanner.DEFAULT_TARGET,
) {
    val learnedCount: Int get() = records.size

    /** Words whose latest answer was "不认识"; they lead the next auto round. */
    val reviewCount: Int get() = records.values.count { it.lastAnswerKnown == false }

    /** Unique words whose latest answer happened on [epochDay]. */
    fun studiedOn(epochDay: Long): Int =
        records.values.count { it.lastStudiedEpochDay == epochDay }

    fun knownOn(epochDay: Long): Int =
        records.values.count { it.lastStudiedEpochDay == epochDay && it.lastAnswerKnown == true }

    fun record(wordId: String, known: Boolean, epochDay: Long): WordStudyProgress {
        val previous = records[wordId] ?: WordStudyRecord()
        val updated = previous.copy(
            knownCount = previous.knownCount + if (known) 1 else 0,
            unknownCount = previous.unknownCount + if (known) 0 else 1,
            lastStudiedEpochDay = epochDay,
            lastAnswerKnown = known,
        )
        return copy(records = records + (wordId to updated))
    }

    fun withTarget(target: Int): WordStudyProgress =
        copy(dailyTarget = WordStudyPlanner.clampTarget(target))
}

/**
 * Chooses the words for the next round. Priority is "not seen yet", then words
 * whose latest answer was wrong, then the words seen longest ago. Within each
 * bucket the book order is kept so a round still feels like a walk through the
 * textbook rather than random sampling.
 */
object WordStudyPlanner {

    const val DEFAULT_TARGET = 10
    const val MIN_TARGET = 1
    const val MAX_TARGET = 50

    /** Quick choices offered next to the stepper on the learning page. */
    val PRESETS: List<Int> = listOf(5, 10, 20, 30)

    fun clampTarget(value: Int): Int = value.coerceIn(MIN_TARGET, MAX_TARGET)

    fun autoSelect(
        words: List<EnglishWord>,
        progress: WordStudyProgress,
        count: Int,
    ): List<EnglishWord> {
        val limit = count.coerceIn(0, MAX_TARGET)
        if (limit == 0) return emptyList()
        val unseen = words.filter { it.id !in progress.records }
        val wrong = words
            .filter { progress.records[it.id]?.lastAnswerKnown == false }
            .sortedBy { progress.records.getValue(it.id).lastStudiedEpochDay }
        val oldest = words
            .filter { progress.records[it.id]?.lastAnswerKnown == true }
            .sortedBy { progress.records.getValue(it.id).lastStudiedEpochDay }
        // Bucket order is the point of the rule; only within a bucket does the
        // book order survive (stable sorts above keep it).
        return (unseen + wrong + oldest)
            .distinctBy { it.id }
            .take(limit)
    }

    /**
     * Manual picks keep the book order so the session reads consistently; a
     * selection larger than the round cap is cut at [MAX_TARGET].
     */
    fun manualSelect(words: List<EnglishWord>, ids: Set<String>): List<EnglishWord> =
        words.filter { it.id in ids }.take(MAX_TARGET)
}

data class WordStudyAnswer(val wordId: String, val known: Boolean)

/**
 * One pass over a fixed list of words. A card must be revealed before it can be
 * answered, which keeps the self-assessment honest: the learner commits to
 * "认识 / 不认识" knowing the meaning is about to appear.
 */
data class WordStudySession(
    val words: List<EnglishWord>,
    val index: Int = 0,
    val revealed: Boolean = false,
    val answers: List<WordStudyAnswer> = emptyList(),
) {
    val current: EnglishWord? get() = words.getOrNull(index)
    val isComplete: Boolean get() = index >= words.size
    val answeredCount: Int get() = answers.size
    val knownCount: Int get() = answers.count { it.known }
    val unknownCount: Int get() = answers.size - knownCount

    val accuracyPercent: Int
        get() = if (answers.isEmpty()) 0
        else (knownCount * 100f / answers.size).roundToInt()

    fun reveal(): WordStudySession =
        if (current == null || revealed) this else copy(revealed = true)

    fun answer(known: Boolean): WordStudySession {
        val word = current ?: return this
        if (!revealed) return this
        return copy(
            index = index + 1,
            revealed = false,
            answers = answers + WordStudyAnswer(word.id, known),
        )
    }

    /** A new round over exactly the words answered "不认识", or null if none. */
    fun retryWrong(): WordStudySession? {
        val wrongIds = answers.filterNot { it.known }.map { it.wordId }.toSet()
        if (wrongIds.isEmpty()) return null
        return WordStudySession(words = words.filter { it.id in wrongIds })
    }
}

private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
