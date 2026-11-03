package com.example.orbit.learning

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * The bundled learning library: the full 唐诗三百首 collection and a high
 * school 3500 word list, both loaded from packaged resources so the home cards
 * work offline. The AI assistant is only used to explain or quiz on an item.
 */
data class Poem(
    val id: String,
    val title: String,
    val author: String,
    val dynasty: String,
    val lines: List<String>,
    val note: String,
    val appreciation: String,
) {
    val firstLine: String get() = lines.firstOrNull().orEmpty()
}

data class EnglishWord(
    val id: String,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val example: String = "",
    val exampleZh: String = "",
    val tip: String = "",
)

enum class MathGrade(val label: String, val stage: String) {
    GRADE_7("初一", "七年级"),
    GRADE_8("初二", "八年级"),
    GRADE_9("初三", "九年级"),
    GRADE_10("高一", "必修"),
    GRADE_11("高二", "选择性必修"),
    GRADE_12("高三", "高考复习"),
}

data class MathTopic(
    val id: String,
    val grade: MathGrade,
    val title: String,
    val summary: String,
    val formula: String,
    val example: String,
    val answer: String,
    val tip: String,
)

data class Quote(
    val id: String,
    val text: String,
    val source: String,
)

enum class LearningKind {
    POEM,
    ENGLISH,
    MATH,
}

private object BundledLearningData {

    private val gson = Gson()

    val poems: List<Poem> by lazy {
        read("learning/poems.json", object : TypeToken<List<Poem>>() {}.type)
    }

    val words: List<EnglishWord> by lazy {
        read("learning/words.json", object : TypeToken<List<EnglishWord>>() {}.type)
    }

    private fun <T> read(name: String, type: java.lang.reflect.Type): T {
        val stream = BundledLearningData::class.java.classLoader
            ?.getResourceAsStream(name)
            ?: error("Bundled learning data is missing: $name")
        return stream.bufferedReader(Charsets.UTF_8).use { reader ->
            gson.fromJson(reader, type)
        }
    }
}

object LearningLibrary {

    /**
     * The full books live in `src/main/resources/learning/` so 309 poems and
     * 3,877 exam words do not turn this file into a million-line literal. Both
     * are loaded on first use and cached; [warmUp] lets the app hide the parse
     * behind startup.
     */
    val poems: List<Poem> get() = BundledLearningData.poems

    val english: List<EnglishWord> get() = BundledLearningData.words

    fun warmUp() {
        poems
        english
    }

    val math: List<MathTopic> = listOf(
        MathTopic(
            id = "rational-add-sub",
            grade = MathGrade.GRADE_7,
            title = "有理数的加减",
            summary = "同号相加取相同的符号，异号相加取绝对值较大数的符号，再用较大绝对值减较小绝对值。",
            formula = "a − b = a + (−b)",
            example = "计算：−7 + 3 − (−5)",
            answer = "−7 + 3 + 5 = 1",
            tip = "减法先变成加法，再按同号、异号两条法则定符号。",
        ),
        MathTopic(
            id = "linear-equation",
            grade = MathGrade.GRADE_7,
            title = "一元一次方程",
            summary = "去分母、去括号、移项、合并同类项、系数化为 1，逐步把方程化成 x = a。",
            formula = "ax + b = 0（a ≠ 0）⇒ x = −b / a",
            example = "解方程：3(x − 2) = 2x + 5",
            answer = "3x − 6 = 2x + 5，移项得 x = 11。",
            tip = "移项要变号；算出结果后代回原方程检验。",
        ),
        MathTopic(
            id = "linear-system",
            grade = MathGrade.GRADE_7,
            title = "二元一次方程组",
            summary = "用代入消元或加减消元，把两个未知数化成一个，再回代求另一个。",
            formula = "a₁x + b₁y = c₁；a₂x + b₂y = c₂",
            example = "解方程组：x + y = 7，x − y = 1",
            answer = "两式相加得 2x = 8，x = 4；回代得 y = 3。",
            tip = "系数相同用减法，系数相反用加法，先消去一个字母。",
        ),
        MathTopic(
            id = "pythagorean",
            grade = MathGrade.GRADE_8,
            title = "勾股定理",
            summary = "直角三角形两条直角边的平方和等于斜边的平方，已知任意两边可求第三边。",
            formula = "a² + b² = c²（c 为斜边）",
            example = "两条直角边分别为 6 和 8，求斜边。",
            answer = "c = √(6² + 8²) = √100 = 10。",
            tip = "先确认哪条边是斜边，只有斜边才单独在等号右边。",
        ),
        MathTopic(
            id = "linear-function",
            grade = MathGrade.GRADE_8,
            title = "一次函数",
            summary = "y = kx + b 的图像是一条直线，k 决定倾斜方向，b 是与 y 轴交点的纵坐标。",
            formula = "k = (y₂ − y₁) / (x₂ − x₁)",
            example = "直线经过 (1, 3) 和 (3, 7)，求解析式。",
            answer = "k = (7 − 3)/(3 − 1) = 2，代入得 b = 1，所以 y = 2x + 1。",
            tip = "两点定一线：先求斜率 k，再任取一点求 b。",
        ),
        MathTopic(
            id = "radical-simplify",
            grade = MathGrade.GRADE_8,
            title = "二次根式化简",
            summary = "把被开方数分解出完全平方因数，再合并同类二次根式。",
            formula = "√a · √b = √(ab)；√a / √b = √(a / b)（a ≥ 0，b > 0）",
            example = "化简：√48 − √27",
            answer = "√48 = 4√3，√27 = 3√3，所以结果为 √3。",
            tip = "先分解成 16×3、9×3 这样的完全平方因数，再开方。",
        ),
        MathTopic(
            id = "quadratic-equation",
            grade = MathGrade.GRADE_9,
            title = "一元二次方程",
            summary = "先化为一般式，能因式分解就分解，否则用求根公式；判别式决定根的个数。",
            formula = "x = (−b ± √(b² − 4ac)) / 2a",
            example = "解方程：2x² − 5x + 2 = 0",
            answer = "Δ = 25 − 16 = 9，x = (5 ± 3)/4，所以 x₁ = 2，x₂ = 1/2。",
            tip = "先算 Δ = b² − 4ac：Δ > 0 两个根，Δ = 0 一个根，Δ < 0 无实根。",
        ),
        MathTopic(
            id = "quadratic-function",
            grade = MathGrade.GRADE_9,
            title = "二次函数",
            summary = "配方后可直接读出顶点和对称轴，a 的正负决定开口方向。",
            formula = "y = a(x − h)² + k，顶点 (h, k)，h = −b / (2a)",
            example = "求 y = x² − 4x + 1 的顶点。",
            answer = "y = (x − 2)² − 3，顶点为 (2, −3)。",
            tip = "配方时先提 a，再补 (b/2a)² 并同时减去。",
        ),
        MathTopic(
            id = "trig-ratio",
            grade = MathGrade.GRADE_9,
            title = "锐角三角函数",
            summary = "直角三角形中，正弦是对边比斜边，余弦是邻边比斜边，正切是对边比邻边。",
            formula = "sin A = a/c；cos A = b/c；tan A = a/b",
            example = "∠C = 90°，∠A = 30°，斜边 c = 10，求 a。",
            answer = "a = c · sin 30° = 10 × 1/2 = 5。",
            tip = "先画图标出对边、邻边、斜边，再选择对应的函数。",
        ),
        MathTopic(
            id = "exponent-rules",
            grade = MathGrade.GRADE_10,
            title = "指数运算",
            summary = "同底数幂相乘指数相加，相除指数相减，幂的乘方指数相乘。",
            formula = "aᵐ · aⁿ = aᵐ⁺ⁿ；aᵐ / aⁿ = aᵐ⁻ⁿ；(aᵐ)ⁿ = aᵐⁿ",
            example = "化简：2³ × 2⁻⁵ ÷ 2⁻¹",
            answer = "2^(3 − 5 + 1) = 2⁻¹ = 1/2。",
            tip = "负指数表示倒数：a⁻ⁿ = 1/aⁿ（a ≠ 0）。",
        ),
        MathTopic(
            id = "log-rules",
            grade = MathGrade.GRADE_10,
            title = "对数运算",
            summary = "对数是指数的逆运算，真数必须大于 0，底数大于 0 且不等于 1。",
            formula = "logₐ(MN) = logₐM + logₐN；logₐ(M/N) = logₐM − logₐN",
            example = "计算：log₂ 8 + log₂ 4",
            answer = "3 + 2 = 5，也就是 log₂ 32。",
            tip = "先把同底对数合并，再化成指数形式求值。",
        ),
        MathTopic(
            id = "trig-identity",
            grade = MathGrade.GRADE_10,
            title = "三角恒等变换",
            summary = "和角公式是推导二倍角公式的基础，展开时注意中间的加减号。",
            formula = "sin(α ± β) = sinα cosβ ± cosα sinβ；cos2α = 1 − 2sin²α",
            example = "已知 sinα = 3/5 且 α 为锐角，求 sin2α。",
            answer = "cosα = 4/5，sin2α = 2 × 3/5 × 4/5 = 24/25。",
            tip = "先由平方关系求出另一个三角函数，再套二倍角公式。",
        ),
        MathTopic(
            id = "sequence-sum",
            grade = MathGrade.GRADE_11,
            title = "等差与等比数列",
            summary = "等差看公差 d，等比看公比 q；等比求和前先判断 q 是否等于 1。",
            formula = "Sₙ = n(a₁ + aₙ)/2；Sₙ = a₁(1 − qⁿ)/(1 − q)（q ≠ 1）",
            example = "等比数列首项 2、公比 3，求前 4 项和。",
            answer = "S₄ = 2(1 − 3⁴)/(1 − 3) = 80。",
            tip = "q = 1 时不能套公式，应直接算 n · a₁。",
        ),
        MathTopic(
            id = "derivative-tangent",
            grade = MathGrade.GRADE_11,
            title = "导数与切线",
            summary = "导数是瞬时变化率，某点切线的斜率等于函数在该点的导数值。",
            formula = "(xⁿ)' = n xⁿ⁻¹；y − y₀ = f'(x₀)(x − x₀)",
            example = "求 y = x³ − 2x 在 x = 1 处的切线斜率。",
            answer = "y' = 3x² − 2，代入 x = 1 得斜率为 1。",
            tip = "先求导，再代横坐标，最后用点斜式写切线方程。",
        ),
        MathTopic(
            id = "line-circle",
            grade = MathGrade.GRADE_11,
            title = "直线与圆",
            summary = "判断位置关系时，比较圆心到直线的距离 d 与半径 r 的大小。",
            formula = "d = |Ax₀ + By₀ + C| / √(A² + B²)",
            example = "圆 x² + y² = 5 与直线 x + 2y − 5 = 0 有几个交点？",
            answer = "d = |−5| / √5 = √5 = r，所以相切，只有 1 个交点。",
            tip = "d < r 相交，d = r 相切，d > r 相离。",
        ),
        MathTopic(
            id = "binomial",
            grade = MathGrade.GRADE_12,
            title = "二项式定理",
            summary = "展开式的通项用来求指定项或指定项的系数。",
            formula = "Tₖ₊₁ = C(n, k) · aⁿ⁻ᵏ · bᵏ",
            example = "求 (x + 2)⁵ 中 x³ 的系数。",
            answer = "由 n − k = 3 得 k = 2，系数为 C(5,2) × 2² = 40。",
            tip = "先由 a 的指数确定 k，再代入通项，别漏掉 b 的幂。",
        ),
        MathTopic(
            id = "classical-probability",
            grade = MathGrade.GRADE_12,
            title = "古典概型",
            summary = "基本事件有限且等可能时，概率等于有利结果数除以总结果数。",
            formula = "P(A) = m / n",
            example = "从 1 到 6 中随机取一个数，取到偶数的概率是多少？",
            answer = "偶数有 2、4、6 共 3 个，P = 3/6 = 1/2。",
            tip = "列举时按顺序写，确保不重复、不遗漏。",
        ),
        MathTopic(
            id = "derivative-extremum",
            grade = MathGrade.GRADE_12,
            title = "导数求极值",
            summary = "先求导并解 f'(x) = 0，再根据导数在驻点两侧的符号变化判断极大或极小。",
            formula = "f'(x) = 0 且左侧为正、右侧为负 ⇒ 极大值",
            example = "求 f(x) = x³ − 3x 的极值。",
            answer = "f'(x) = 3(x² − 1)，x = −1 时极大值 2，x = 1 时极小值 −2。",
            tip = "解出驻点后一定检查两侧导数符号，不能只看方程解。",
        ),
    )

    fun poemFor(epochDay: Long): Poem = poems[indexFor(epochDay, poems.size)]

    fun englishFor(epochDay: Long): EnglishWord =
        english[indexFor(epochDay + 3, english.size)]

    fun mathFor(epochDay: Long): MathTopic = math[indexFor(epochDay + 7, math.size)]

    fun poemById(id: String?): Poem? = poems.firstOrNull { it.id == id }

    fun englishById(id: String?): EnglishWord? = english.firstOrNull { it.id == id }

    fun mathById(id: String?): MathTopic? = math.firstOrNull { it.id == id }

    fun mathForGrade(grade: MathGrade): List<MathTopic> = math.filter { it.grade == grade }

    fun poemPrompt(poem: Poem): String =
        "请用适合中学生的方式讲解《${poem.title}》（${poem.author}，${poem.dynasty}）。" +
            "原文：${poem.lines.joinToString("")}" +
            "先逐句解释意思，再点出 1 个写作手法，然后出 2 道理解题，最后附上答案。"

    fun englishPrompt(word: EnglishWord): String {
        val phonetic = if (word.phonetic.isBlank()) "" else " ${word.phonetic}"
        return "请讲解英语单词 ${word.word}$phonetic：词义、常见搭配和一个例句，" +
            "再出 2 道填空题并附答案。"
    }

    fun mathPrompt(topic: MathTopic): String =
        "请用适合${topic.grade.label}学生的方式讲解「${topic.title}」。" +
            "核心要点：${topic.summary}" +
            "公式：${topic.formula}。" +
            "先解释公式里每个符号的含义和适用条件，再完整演算一个例子，" +
            "然后出 2 道同类练习并附答案，最后提醒一个常见错误。"

    private fun indexFor(epochDay: Long, size: Int): Int {
        if (size <= 0) return 0
        val remainder = epochDay % size
        return ((remainder + size) % size).toInt()
    }
}

/**
 * Offline quote of the day. The refresh action walks the list instead of
 * picking randomly, so a tap always shows something new and the order stays
 * stable across a session.
 */
object QuoteLibrary {

    val quotes: List<Quote> = listOf(
        Quote("q01", "学而不思则罔，思而不学则殆。", "《论语》"),
        Quote("q02", "知之者不如好之者，好之者不如乐之者。", "《论语》"),
        Quote("q03", "不积跬步，无以至千里。", "《荀子·劝学》"),
        Quote("q04", "锲而不舍，金石可镂。", "《荀子·劝学》"),
        Quote("q05", "业精于勤，荒于嬉。", "韩愈《进学解》"),
        Quote("q06", "读书破万卷，下笔如有神。", "杜甫"),
        Quote("q07", "纸上得来终觉浅，绝知此事要躬行。", "陆游"),
        Quote("q08", "问渠那得清如许？为有源头活水来。", "朱熹"),
        Quote("q09", "博学之，审问之，慎思之，明辨之，笃行之。", "《礼记·中庸》"),
        Quote("q10", "千里之行，始于足下。", "《老子》"),
        Quote("q11", "天行健，君子以自强不息。", "《周易》"),
        Quote("q12", "少年易老学难成，一寸光阴不可轻。", "朱熹"),
        Quote("q13", "黑发不知勤学早，白首方悔读书迟。", "颜真卿"),
        Quote("q14", "书山有路勤为径，学海无涯苦作舟。", "传统联语"),
        Quote("q15", "读万卷书，行万里路。", "董其昌"),
        Quote("q16", "温故而知新，可以为师矣。", "《论语》"),
        Quote("q17", "学如逆水行舟，不进则退。", "《增广贤文》"),
        Quote("q18", "非学无以广才，非志无以成学。", "诸葛亮《诫子书》"),
        Quote("q19", "盛年不重来，一日难再晨。", "陶渊明"),
        Quote("q20", "路漫漫其修远兮，吾将上下而求索。", "屈原《离骚》"),
        Quote("q21", "宝剑锋从磨砺出，梅花香自苦寒来。", "《警世贤文》"),
        Quote("q22", "熟读唐诗三百首，不会作诗也会吟。", "孙洙"),
        Quote("q23", "尽信书，则不如无书。", "《孟子》"),
        Quote("q24", "知之为知之，不知为不知，是知也。", "《论语》"),
    )

    fun forDay(epochDay: Long): Quote {
        val size = quotes.size
        val remainder = epochDay % size
        return quotes[((remainder + size) % size).toInt()]
    }

    fun next(after: Quote): Quote {
        val index = quotes.indexOfFirst { it.id == after.id }
        if (index < 0) return quotes.first()
        return quotes[(index + 1) % quotes.size]
    }
}
