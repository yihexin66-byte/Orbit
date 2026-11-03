# Orbit Learn 2.8：古诗搜索、每日挑选与单词收藏

2.8 在 2.7 的两本真实词库之上再包一层「管理」：古诗可以按诗名、作者、朝代或诗句搜索，任意一首都能设为今日；单词可以收藏，按「全部 / 收藏」筛选，任意一条也能设为今日。首页「今日学习」、书架 TODAY 行与书页里的「今日」标记读取同一份选择，改完立即同步。版本 `2.8.0`（versionCode 10），包名仍为 `com.example.orbit`。

本次不新增网络请求、不改 `/api/ai/*` 契约、不触碰 Coze 凭证、不改 Room schema 与云端快照。选择与收藏都是**本机、按账号隔离**的轻量状态，不进入云端同步；没有任何跨设备承诺。

## 1. 交互

### 1.1 古诗搜索

古诗书页标题下方新增搜索框：

- 匹配范围：诗名、作者、朝代与全部诗句，忽略大小写。
- 分词：按空白切分，要求每个词都出现在上述文本中。例如 `李白 静夜` 只保留同时满足两个词的条目；`李白` 也会命中标题里提到李白的杜甫诗（如《天末怀李白》）。
- 输入即筛选；有输入时搜索框下方显示「找到 N 首」。
- 右侧 `×` 清空并收起键盘；键盘的搜索键也会收起键盘。
- 没有结果时显示空状态卡片「没有匹配的诗句」与「清空搜索」入口，不留下空白列表。
- 搜索只影响当前书页；返回书架或切换书籍后查询清空，列表回到顶部。

### 1.2 设为今日

古诗与单词共用「设为今日」动作：

- 动作只在卡片展开且该条目还不是今日时出现，位置在正文之后、注释 / 赏析 / 记忆提示之前，避免长注释把按钮推到很远。
- 点击后写入「今天 + 条目 id」，卡片立即出现「今日」徽标，按钮消失；首页两张预览卡与书架 TODAY 行同步更新。
- 昨天选的诗不会带到今天：读取时同时校验 `epochDay`，日期不匹配就回退到原有的 `epochDay % size` 轮换。
- 没有「取消今日」；换一条即可，跨天自动回到轮换。数学讲义不参与手动挑选，仍按日轮换。

### 1.3 单词收藏与筛选

- 每张词卡右上角新增「收藏 / 已收藏」胶囊，点击即加入或移出收藏，不需要展开卡片。
- 收藏是开关语义（`Role.Checkbox`），读屏描述为「收藏单词 abandon」/「取消收藏单词 abandon」，状态描述为「已收藏 / 未收藏」。
- 书页顶部两个筛选 chip：「全部」与「收藏 · N」。选中态用荧光黄实底，未选中用轨道灰。
- 收藏筛选为空时显示「还没有收藏的单词」，并给出「全部」按钮返回完整词表。
- 切换筛选时列表回到顶部；收藏状态在「全部」视图里即时更新，不打断当前滚动位置。
- 收藏仅对单词开放；古诗目前只有搜索与今日挑选，没有收藏。

## 2. 状态与持久化

### 2.1 纯逻辑

`learning/LearningProgress.kt` 只包含可 JVM 测试的纯函数与数据：

```kotlin
data class LearningProgress(
    val dailyPoemDay: Long = 0L,
    val dailyPoemId: String? = null,
    val dailyWordDay: Long = 0L,
    val dailyWordId: String? = null,
    val bookmarkedWordIds: Set<String> = emptySet(),
) {
    fun poemIdFor(epochDay: Long): String? = dailyPoemId?.takeIf { dailyPoemDay == epochDay }
    fun wordIdFor(epochDay: Long): String? = dailyWordId?.takeIf { dailyWordDay == epochDay }
}

object LearningBrowse {
    fun filterPoems(poems: List<Poem>, query: String): List<Poem>
    fun filterWords(words: List<EnglishWord>, filter: WordShelfFilter, bookmarked: Set<String>): List<EnglishWord>
    fun resolvePoem(epochDay: Long, pinnedId: String?): Poem
    fun resolveWord(epochDay: Long, pinnedId: String?): EnglishWord
}
```

- `filterPoems`：空白查询直接返回原列表；否则把 `title + author + dynasty + lines` 拼成小写 haystack，所有 token 都必须命中。
- `filterWords`：`ALL` 返回原顺序；`BOOKMARKED` 只保留 id 在集合中的词，保持词表顺序；未知 id 不会凭空造出条目。
- `resolvePoem / resolveWord`：有效的 pinned id 优先；id 缺失或已不在词库时回退到每日轮换，避免旧数据把首页打空。

### 2.2 按账号保存

`data/LearningProgressStore.kt` 使用单独的 `SharedPreferences` 文件 `orbit_learning`，键按账号 id 命名：

```text
poem_day.<owner>       poem_id.<owner>
word_day.<owner>       word_id.<owner>
word_bookmarks.<owner>
```

- `owner` 是当前登录用户 id；未登录 / QA 无账号时使用 `local`，与 `AccountStore.NO_OWNER` 对齐。
- `LearningViewModel` 监听 `AccountStore.session`，切换账号即重新读取该账号的五个键，不同账号不会看到彼此的「今日」与收藏。
- 退出登录只清会话与 Room 缓存，不清 `orbit_learning`；同一账号重新登录后选择仍在。这是刻意的本机偏好语义，和主题 / 强调色一致。
- 这些数据不进入 Room，也不进入 `/api/sync` 快照。换设备、重装或清应用数据后不会恢复。

### 2.3 状态接线

```text
LearningProgressStore
        ↓ load / save
LearningViewModel ── progress: StateFlow<LearningProgress>
        ↓ LearningActions(onSetPoemToday / onSetWordToday / onToggleWordBookmark)
OrbitRoot → OrbitApp → OrbitHomeScreen
        ↓ dailyPoemId / dailyWordId / bookmarkedWordIds
TodayLearningSection + LearningSheet → BookPage → PoemCard / EnglishCard
```

- `OrbitHomeScreen` 用 `LearningProgress.poemIdFor(today)` / `wordIdFor(today)` 决定首页与书页的「今日」。
- 所有写入都带当前 `today`，由 `LearningActions.onSetPoemToday(today, id)` 统一传入，页面不自行计算日期。
- 收藏写入返回新的不可变集合，ViewModel 用 `StateFlow.update` 发布，Compose 以集合引用变化触发重组。

## 3. UI 细节与无障碍

- 搜索框是自绘 `BasicTextField`：胶囊底、聚焦时描边变为主色、`SolidColor` 光标、`ImeAction.Search`；测试标签 `learning_poem_search`，清空按钮 `learning_poem_search_clear`，读屏文案「清空搜索」。
- 搜索无结果与收藏为空都复用 `OrbitEmptyState`，带标题、说明与一个动作，不出现只有一行灰字的裸空态。
- 「设为今日」是 44dp 高的描边胶囊，主色文字；测试标签 `learning_set_today`。只展开时渲染，点击后立即消失。
- 收藏胶囊的点击区域固定 48dp 高、宽度至少 48dp；`已收藏` 三字完整显示，不被固定宽度裁切。
- 词卡头部的顺序是：词头 → 收藏胶囊 → 今日徽标 → 展开 / 收起；三个元素同时出现时词头仍可换行到两行。
- 筛选 chip 最小高度 40dp，视觉内边距 14dp；选中态同时改变底色与文字对比度，不只靠颜色描边。
- 搜索词或筛选变化时 `LazyColumn` 滚回第一项；单纯收藏 / 取消收藏不会打断滚动。
- 既有底部不抖动约束保持：sheet 仍禁用拖拽手势，列表末尾不与弹窗锚点抢事件。

## 4. 测试

### 4.1 JVM：127 项通过（新增 13 项）

新增 `LearningProgressTest`：

- 今日选择只在选定当天有效，次日返回 `null`。
- 默认状态没有选择、没有收藏。
- 空查询 / 纯空白返回完整词库。
- 按诗名、作者、诗句、朝代都能命中；多 token 必须全部命中；大小写不敏感；无结果返回空。
- 收藏筛选保持词表顺序、未知 id 不生成行、空集合返回空。
- 有效 pinned id 覆盖轮换；未知 pinned id 回退轮换（诗与词各一组）。

### 4.2 设备：56 项通过（Pixel 7 Pro / API 37，Android 17）

`LearningVisualReviewTest` 新增 `capturePoemSearchDailyPickAndWordBookmarks`，串起完整路径：

1. 从首页进入书架与古诗书，输入「李白」得到 32 首，截图；清空后输入「静夜思」得到 1 首。
2. 展开诗卡，滚动到「设为今日」并点击；断言 ViewModel 的 `poemIdFor(today)` 等于该诗 id，卡片出现今日徽标且按钮消失。
3. 返回书架断言 TODAY 行变为「古诗《静夜思》…」，关闭弹窗回到首页断言预览卡同步。
4. 进入英语书，收藏 `a`，断言筛选 chip 变为「收藏 · 1」且集合只含该 id。
5. 切到「收藏」筛选，展开并设为今日，断言 `wordIdFor(today)`。
6. 首页同时显示静夜思与 a，截图；`scenario.recreate()` 后再次滚动到今日学习，断言两条选择仍在，再打开书架确认 TODAY 行仍由磁盘上的选择重建，验证 SharedPreferences 读取。

设备测试开始时清空 `orbit_learning` 并调用 `LearningViewModel.resetLocalProgress()`，每个用例从空状态开始，截图不夹带上一条用例的收藏。

### 4.3 截图

10 张 2.8 截图位于 [`screenshots/phase16/`](screenshots/phase16/)：

| 文件 | 内容 |
| --- | --- |
| `01-poem-search.png` | 古诗书页搜索「李白」，结果 32 首 |
| `02-poem-set-today.png` | 搜索「静夜思」并展开，正文下的「设为今日」 |
| `03-poem-today.png` | 设为今日后的今日徽标，动作消失 |
| `04-shelf-poem-today.png` | 书架 TODAY 行同步为《静夜思》 |
| `05-home-poem-today.png` | 首页今日学习同步为《静夜思》 |
| `06-word-bookmarked.png` | 收藏 `a` 后的「已收藏」与「收藏 · 1」 |
| `07-word-bookmark-filter.png` | 切到收藏筛选，仅剩一条词卡 |
| `08-word-today.png` | 收藏词设为今日：已收藏 + 今日 + 收起 |
| `09-home-custom-picks.png` | 首页同时显示自定义的静夜思与单词 a |
| `10-shelf-picks-persisted.png` | Activity 重建后书架 TODAY 行仍是静夜思与 a |

首页截图包含系统状态栏；书架与书页截图直接捕获弹窗内容节点，不含状态栏与遮罩。全部使用 QA 离线数据，不含账号凭证。

## 5. 构建产物

- 文件：`artifacts/orbit-2.8.0-orbit-learn-debug.apk`
- 大小：15,733,613 bytes
- SHA-256：`6ef0c755e7d76cad0e30c43f761385134bc56e1f8e0524730547486436ab31f0`
- MD5：`24737571fde2c26d52bec8909c0d7e82`
- versionName `2.8.0` / versionCode 10 / minSdk 24 / targetSdk 37
- Base URL：`https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`
- 安装后冷启动进入登录页 / 已有 QA 会话进入首页的冒烟验证通过。

## 6. 限制

- 搜索是字面匹配，不做拼音、首字母、同义词或模糊纠错；不记录搜索历史，也没有热门推荐。
- 「今日」只能手动指定一条诗与一条词；没有「未来七天排期」，不能一次预约多天。
- 收藏只对英语单词开放；古诗没有收藏夹，数学没有搜索或挑选。
- 今日选择与收藏只在本机、按账号保存，不进入云端快照；换设备、重装或清数据后丢失。
- 没有「取消今日」按钮；再选一条或等第二天即可。日期只在读取时校验，跨天时若页面一直停留、未触发重组，UI 可能继续显示旧条目，回到前台刷新后恢复。
- 收藏为空时只能回「全部」，没有批量清空或导出收藏。
- 搜索期间的展开状态按条目 id 保留；清空搜索后原展开条目若仍在首屏会保持展开，否则需要重新点击。
- 「设为今日」位于展开区，必须先展开卡片才能操作；折叠态没有独立的快捷入口。
