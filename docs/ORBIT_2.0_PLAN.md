# Orbit 2.0 产品与技术规划

状态：**待确认，尚未修改任何代码**  
日期：2026-10-25  
基线：当前 Orbit 1.0 工程（Room v1，37 个 JVM 测试 + 26 个设备测试通过）

本文回答七个问题：现有架构审查、新功能与旧功能关系、数据实体及关系、导航结构、实现难度与顺序、模块验收标准、数据库迁移与回归测试计划。第一期只做「目标星系、阶段任务、习惯打卡、成长统计」，不接 AI、不做专注计时和每日回顾。

---

## 0. 结论摘要

- **保留现有 Task 作为唯一执行单元**，在 Task 上增加可空的 `goalId` / `stageId`，不重做任务模型，也不把目标强行塞进现有星轨首页。
- **新增五个 Room 实体**：`goals`、`goal_stages`、`habits`、`habit_records`、`growth_events`。专注记录、每日回顾、成就解锁只做预留设计，第一期不建表。
- **目标进度、习惯连续天数、经验等级全部是纯函数派生**，不把进度写进数据库，不在 Composable 里拼统计。
- **首页星轨继续只表达今日任务**；目标用独立的「目标星系」Canvas 表达，避免手势、语义和 9 节点上限互相打架。
- **底部导航改为五个区域**：星轨 / 目标 / 规划 / 成长 / 我的。Planner 内部只有「任务 / 习惯」两个分段，不增加第二层底部导航。
- **数据库 v1 → v2 用手写 Migration + 重建 tasks 表**，不使用 destructive fallback；必须新增真实迁移测试和升级安装验证。
- **成长经验采用事件账本**：同一来源只记一次，撤销完成不清除已获得的经验；成就在第二期基于事件账本扩展。
- 第一期不包含：AI 计划助手、专注计时、每日回顾、成就墙、通知/提醒、云同步、导出导入、Release 签名。

需要确认的四个默认决策在第 8 节。

---

## 1. 现有架构审查

### 1.1 工程结构

当前是单模块 Android 原生项目，技术栈保持 Kotlin、Jetpack Compose、Material 3、Navigation Compose、Room、ViewModel、StateFlow。没有 Hilt/Koin，没有网络权限，没有 WorkManager，没有 Git 仓库和 CI。

| 层 | 当前实现 | 对升级的含义 |
| --- | --- | --- |
| 入口 | `MainActivity` → `OrbitRoot` | 所有 ViewModel 目前都在 Activity 级创建 |
| 状态 | `TaskViewModel` + `TaskUiState`；`OrbitViewModel` 管主题 | 任务与外观已经分离，新模块可以沿用这个边界 |
| 数据 | Room v1，仅 `tasks` 一张表 | 需要新增实体和第一次数据库迁移 |
| 聚合 | `TaskMetrics` / `TaskStatistics` 纯 Kotlin | 目标、习惯、成长可以复制同一套可测试模式 |
| 视觉 | `Glass.kt`、`OrbitPrimitives.kt`、`SpaceBackdrop`、浮动导航 | 可直接复用，不需要第二套设计系统 |
| 手势 | `TaskOrbit` + 纯几何 `OrbitLayout` | 首页星轨与任务强耦合，目标星系应新建轻量 Canvas |

### 1.2 Task 数据模型

`TaskEntity` 当前字段：

```text
id, title, description, priority, category, dueDate,
isCompleted, createdAt, completedAt
```

- `dueDate`：本地日历 epoch day，无日程时为 null。
- `createdAt` / `completedAt`：epoch 毫秒。
- 索引：`isCompleted`、`dueDate`。
- `Task` 是独立 UI model，Room 注解没有泄漏到 Compose。
- `TaskConverters` 以 enum name 持久化，未知值回退到稳定默认值，这一点应在所有新枚举上保持一致。

**结论**：现有 Task 没有目标/阶段关联，但也不需要新建“目标任务”类型。增加两个可空外键即可，历史任务保持无目标，现有排序、完成时间、统计和测试语义不变。

### 1.3 数据访问与状态流

- `TaskDao` 提供 `observeAll`、按完成状态观察、按 id 查询、插入/更新/删除、原子完成状态更新。
- `TaskRepository` 是唯一任务写入口，负责标题校验、描述清洗、完成时间写入/清空和不存在任务的异常。
- 当前仓库方法多为单表单语句，没有显式事务。目标删除、任务关联、习惯打卡 + 成长事件这类跨表操作必须引入 `withTransaction`。
- `TaskViewModel` 是 Activity 级单例，订阅一次 `observeAll()`，用 `TaskUiState` 派生今日任务、汇总、统计、筛选和选中节点。
- `OrbitViewModel` 只负责主题和强调色，使用 SharedPreferences。
- `OrbitRoot` 用 `LifecycleEventEffect(ON_RESUME)` 刷新“今天”，这是跨夜一致性的关键，新模块必须接入同一刷新节奏。

**结论**：不建议把所有新状态塞进 `TaskViewModel`。应按 Goal、Habit、Growth 建独立 ViewModel，但在 `OrbitRoot` 统一收集，沿用“单 Activity 状态源 + 纯派生”的模式。

### 1.4 UI 与导航

- 当前四个底部入口：`ORBIT`、`TASKS`、`STATS`、`SETTINGS`，路由分别为 `orbit`、`tasks`、`stats`、`settings`。
- `OrbitBottomBar` 直接用 `currentRoute == destination.route` 判断选中，不能识别嵌套路由。
- `OrbitApp` 的 `BackHandler` 对所有非 `orbit` 路由直接回首页；如果新增 `goals/{id}`，这个逻辑会跳过目标列表。
- `OrbitApp` 已有约 20 个回调参数，继续直接添加 Goal/Habit 回调会迅速失控，需要按领域分组。
- 首页 `TaskOrbit` 是 897 行的任务专用 Canvas：9 节点、双环、拖动、优先级节点、选中光晕、无障碍语义。它很适合“今日任务”，不适合同时承载目标和阶段。
- `StatsScreen` 完全基于 `TaskStatistics`：今日完成率、总览、近 7 天、分类、连续达成。可以整体搬进 Growth 区域并在顶部增加成长卡，不需要重写。
- `SettingsScreen` 已有主题、强调色、提醒诚实说明、数据隐私和关于。可以直接作为 Profile 的主体，补充数据概览即可。
- `TaskEditorSheet` 目前只有标题、描述、优先级、分类、截止日期；需要增加目标/阶段选择，而不是新建第二套任务编辑器。

**结论**：目标应使用独立的目标星系和阶段轨道；首页星轨不改职责。现有统计和设置是“搬家 + 增强”，不是重写。

### 1.5 现有测试与可复用保障

- 37 个 JVM 测试覆盖统计口径、排序、标题规则、UI 派生状态和轨道几何。
- 26 个设备测试覆盖 Room、偏好持久化、任务 CRUD、完成换环、筛选、删除、拖动、返回栈、触控目标和截图。
- `TaskUiFlowTest`、`OrbitInteractionTest`、`CoreEdgeCaseTest` 通过文本和语义定位，底部标签变更会影响 `navigateToTab("任务")`、`navigateToTab("统计")`、`navigateToTab("设置")`。
- `SettingsPersistenceTest` 通过可点击文本「设置」进入设置页。
- `ScreenshotGalleryTest` 会清空数据库并生成 8 张截图；新增页面后应扩展截图集合。
- 现有 `app/schemas/.../1.json` 已导出，是迁移测试的基线。目前没有 `room-testing` 依赖，也没有迁移测试。

**结论**：导航标签变更必须同步更新测试助手；内容标题可以暂时保留以减少语义改动，但不能用假入口过渡。

### 1.6 关键架构风险

| 风险 | 现状 | 规划中的处理 |
| --- | --- | --- |
| 数据库首次迁移 | 只有 v1，没有迁移经验 | 手写 v2、重建 tasks、迁移测试、真实升级安装验证 |
| 跨表写入 | Repository 没有事务 | 完成/打卡/删除目标统一走 `withTransaction` |
| 状态膨胀 | 单个 TaskViewModel 已较大 | 按 Goal/Habit/Growth 拆分，根节点统一收集 |
| 目标进度歧义 | 无现成模型 | 纯函数 `GoalMetrics` + 页面写明口径 |
| 首页过载 | 星轨已有任务、统计、详情、手势 | 首页只加轻量概览和习惯卡；目标星系独立 |
| 导航测试回归 | 标签被大量测试引用 | 先改导航壳并同步测试，再逐模块填充 |
| 性能 | 无真实模糊，星轨是自绘 | 目标星系用点击交互、有限动画，不做持续脉冲 |
| 无依赖注入 | ViewModel 直接 new Repository | 第一期继续手动构造，不引入 Hilt，避免扩大改动面 |

---

## 2. 新功能与旧功能的关系

### 2.1 保留与复用

| 旧能力 | 新位置 | 处理方式 |
| --- | --- | --- |
| 今日星轨、节点详情、拖动 | 星轨首页 | **原样保留**，只新增今日习惯卡和宇宙速览 |
| Task / TaskDao / TaskRepository | 规划 → 任务；目标详情 | 扩展 `goalId`、`stageId`，其余 CRUD 和完成语义不变 |
| TaskEditorSheet | 全局任务编辑器 | 增加目标/阶段选择；保留现有标题、描述、优先级、分类、日期 |
| TaskMetrics / TaskStatistics | 成长 | 保留现有任务统计卡和口径说明 |
| StatsScreen | 成长 | 搬入 Growth 区域，顶部新增成长概览 |
| SettingsScreen | 我的 | 搬入 Profile 区域，新增数据概览，不改变主题持久化 |
| Glass / OrbitPrimitives / SpaceBackdrop | 所有新页面 | 直接复用玻璃系统、卡片、标题、徽章、按钮 |
| OrbitLayout 的纯几何思路 | 目标星系 | 新建更简单的 `GoalGalaxyLayout`，不改造任务星轨 |
| 现有 63 个测试 | 全项目 | 保留并更新受导航标签影响的部分 |

### 2.2 新增

- 目标与阶段：`GoalEntity`、`GoalStageEntity`、GoalRepository、GoalViewModel、GoalsScreen、GoalDetailScreen、GoalEditorSheet、StageEditorSheet、GoalGalaxy。
- 习惯：`HabitEntity`、`HabitRecordEntity`、HabitRepository、HabitViewModel、Planner 习惯分段、HabitEditorSheet、12 周热力图、首页今日习惯卡。
- 成长：`GrowthEventEntity`、GrowthRepository、GrowthMetrics、成长概览卡、等级/XP；成就墙与解锁持久化留到第二期。
- 导航：五区域壳、嵌套目标详情路由、返回栈修正、根级编辑 Sheet 协调。
- 数据：v1 → v2 迁移、迁移测试、目标/习惯/成长 DAO 测试、UI 流程测试。

### 2.3 关系原则

1. **Task 仍是执行单元**：目标、阶段只是组织层；删除目标不能静默删除用户任务。
2. **Habit 不是 Task**：习惯是每日重复记录，不生成无限任务行；不要把打卡伪装成任务完成。
3. **Growth 不是独立数据源**：XP 来自真实的完成、打卡、阶段/目标达成事件，不手填、不造假。
4. **星轨负责“今天”，目标星系负责“长期”**：两种画布各有清晰职责，避免首页变成信息仪表盘。

---

## 3. 数据实体与关系设计

### 3.1 新增/修改实体

#### `goals` — 长期目标

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long PK | 自增主键 |
| `title` | String | 目标名称，规则上限 60 字 |
| `description` | String? | 可选说明，上限 280 字 |
| `tone` | `OrbitTone` | CYAN / VIOLET / ROSE / GOLD，名称持久化 |
| `status` | `GoalStatus` | ACTIVE / PAUSED / COMPLETED / ARCHIVED |
| `targetDate` | Long? | 目标日期，epoch day |
| `createdAt` | Long | 创建时间，epoch 毫秒 |
| `updatedAt` | Long | 更新时间 |
| `completedAt` | Long? | 完成时间 |
| `sortOrder` | Int | 稳定展示顺序；同值时按创建时间 |
| 索引 | — | `status`、`targetDate` |

进度不落库。完成/归档是显式动作；删除是独立操作。

#### `goal_stages` — 目标阶段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long PK | 自增主键 |
| `goalId` | Long FK | → `goals.id`，`ON DELETE CASCADE` |
| `title` | String | 阶段名，上限 60 字 |
| `position` | Int | 阶段顺序；第一期只支持新建时追加，不做拖拽排序 |
| `targetDate` | Long? | 阶段目标日期 |
| `isCompleted` | Boolean | 是否完成 |
| `completedAt` | Long? | 完成时间 |
| `createdAt` / `updatedAt` | Long | 时间戳 |
| 索引 | — | `goalId`、`(goalId, position)` |

“当前阶段”不需要持久化，按 `position` 取第一个未完成阶段。避免引入额外状态机。

#### `tasks` — 现有表增加关联

| 新字段 | 类型 | 说明 |
| --- | --- | --- |
| `goalId` | Long? FK | → `goals.id`，`ON DELETE SET NULL` |
| `stageId` | Long? FK | → `goal_stages.id`，`ON DELETE SET NULL` |
| 新索引 | — | `goalId`、`stageId` |

规则：

- `stageId != null` 时，`goalId` 必须等于该阶段的 `goalId`。
- 不允许只填 `stageId` 不填 `goalId`。
- 任务可以只关联目标、不关联阶段；也可以在阶段创建时通过“添加任务”预填两者。
- 任务的截止日期、完成时间、分类、优先级与目标无关，继续由现有逻辑维护。

#### `habits` — 每日习惯

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long PK | 自增主键 |
| `title` | String | 习惯名，上限 40 字 |
| `note` | String? | 可选说明 |
| `tone` | `OrbitTone` | 卡片强调色 |
| `createdAt` | Long | 创建时间；热力图/达标率的起点 |
| `archivedAt` | Long? | 归档时间；空值表示活跃 |
| `sortOrder` | Int | 稳定顺序 |
| 索引 | — | `archivedAt` |

第一期仅支持每日习惯。未来“每周 N 次”需要新增调度字段和迁移，不提前塞入未使用的枚举。

#### `habit_records` — 打卡记录

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long PK | 自增主键 |
| `habitId` | Long FK | → `habits.id`，`ON DELETE CASCADE` |
| `epochDay` | Long | 打卡当天的本地日历日 |
| `checkedAt` | Long | 实际打卡时间 |
| 唯一索引 | — | `(habitId, epochDay)`，一天最多一条 |
| 其他索引 | — | `epochDay` |

打卡日期以操作时的本地日历日为准，时区变化不会改写历史记录。

#### `growth_events` — 成长事件账本

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long PK | 自增主键 |
| `type` | `GrowthEventType` | TASK_COMPLETED / HABIT_CHECKED / STAGE_COMPLETED / GOAL_COMPLETED |
| `sourceKey` | String | 幂等键，见下 |
| `xp` | Int | 本次获得经验 |
| `occurredAt` | Long | 发生时间，epoch 毫秒 |
| `epochDay` | Long | 本地日历日，用于周统计/热力图 |
| 唯一索引 | — | `sourceKey` |
| 其他索引 | — | `type`、`epochDay` |

`sourceKey`：

- `task:{taskId}:{createdAt}`（避免 id 重用导致误判）
- `habit:{habitId}:{epochDay}`
- `stage:{stageId}`
- `goal:{goalId}`

账本只追加，不因为撤销完成而删除。同一来源再次完成不会重复获得经验。

### 3.2 关系图

```text
goals 1 ───────< goal_stages        (CASCADE)
  │                   │
  │ 0..*              │ 0..*
  └──────────────< tasks               (SET NULL)
                      │
habits 1 ──────< habit_records        (CASCADE)
                      │
growth_events         │  logical sourceKey (no FK)
  ├── task:{id}:{createdAt}
  ├── habit:{id}:{epochDay}
  ├── stage:{id}
  └── goal:{id}
```

| 关系 | 基数 | 删除行为 | 说明 |
| --- | --- | --- | --- |
| Goal → Stage | 1 : N | CASCADE | 删除目标时阶段一起删 |
| Goal → Task | 1 : N（可选） | SET NULL | 删除目标时任务保留，解除关联 |
| Stage → Task | 1 : N（可选） | SET NULL | 删除阶段时任务保留，回到目标级/无阶段 |
| Habit → HabitRecord | 1 : N | CASCADE | 删除习惯时打卡记录一起删 |
| GrowthEvent → 来源 | 逻辑关联 | 不设 FK | 来源删除后事件保留，作为历史 |

### 3.3 删除与归档规则

- **目标完成**：状态改为 COMPLETED，记录 `completedAt`；阶段和任务保持原样。
- **目标归档**：状态改为 ARCHIVED，从目标星系和默认列表隐藏；可在“已归档”中恢复。
- **目标删除**：弹窗明确显示“将删除 N 个阶段；M 个任务会保留并解除目标关联”。确认后只删除目标和阶段；任务不删。
- **阶段删除**：关联任务保留，`stageId` 置空，`goalId` 不变；任务在目标详情中进入“未分配阶段”。
- **任务删除**：不影响目标/阶段；成长事件保留。
- **习惯归档**：保留全部打卡记录，从今日列表隐藏；可恢复。
- **习惯删除**：删除打卡记录；成长事件保留。删除需二次确认。
- 不设置“回收站”或级联删任务，第一期保持可解释。

### 3.4 派生指标与统一口径

#### 目标进度

```text
阶段进度 =
  阶段已标记完成                     -> 1.0
  阶段未完成且有关联任务             -> 已完成任务数 / 关联任务数
  阶段未完成且无关联任务             -> 0.0

目标进度 =
  目标有阶段                         -> 所有阶段进度的算术平均
  目标无阶段                         -> 目标关联任务的完成率（无任务则为 0）
```

- 完成的目标显示 100%，但 `completedAt` 仍以手动完成为准。
- 归档目标不计入目标星系的活跃进度。
- 当目标有阶段时，“目标级任务”和“未分配阶段任务”单独展示，不重复计入阶段平均值；页面必须写明这一点。
- 所有函数只接收已持久化列表，可在 JVM 测试，不依赖当前时间或 Android。

#### 习惯连续天数与热力图

- “今天已打卡”时从今天向前数；今天尚未打卡但昨天已打卡时，保留到昨天为止的连续记录；否则为 0。与现有任务连续达成口径一致。
- 热力图固定展示最近 12 个自然周（84 天），按周一开头，空格表示无记录，未来日期不显示为完成。
- 习惯达标率的分母从 `max(习惯创建日, 窗口起始日)` 开始算，避免新习惯被历史空格惩罚。
- 打卡/取消打卡都只影响当前日记录；成长事件按账本规则处理。

#### 成长经验与等级

| 事件 | XP |
| --- | --- |
| 完成任务 | 10 |
| 完成任务（高优先级） | +5（合计 15） |
| 习惯打卡 | 8 |
| 阶段完成 | 40 |
| 目标完成 | 120 |

- 累计经验 = `SUM(growth_events.xp)`。
- 等级阈值：`threshold(L) = 50 × L × (L - 1)`；Lv.1 = 0，Lv.2 = 100，Lv.3 = 300，Lv.4 = 600。
- 下一级进度 = `(xp - threshold(level)) / (threshold(level + 1) - threshold(level))`，边界钳制到 0..1。
- 同一 `sourceKey` 只发一次经验；撤销完成/取消打卡不扣经验，避免刷分，也让成长记录稳定。
- 第一期只展示等级、经验、本周新增经验；成就墙、徽章和等级称号留到第二期。
- 升级后不回填历史任务经验；成长从第一期第一次达成开始记录，页面会注明。这个取舍避免在 SQL 迁移里重复计算本地时区日。

### 3.5 DAO 与 Repository 职责

| 组件 | 职责 |
| --- | --- |
| `GoalDao` | 目标 CRUD、按状态/顺序观察、按 id 查询 |
| `GoalStageDao` | 阶段 CRUD、按目标/顺序观察、标记完成、计算最大 position |
| `HabitDao` | 习惯 CRUD、活跃/归档观察 |
| `HabitRecordDao` | 按窗口观察、按习惯观察、唯一键插入/删除 |
| `GrowthDao` | 事件插入（IGNORE）、总经验查询、按日/类型观察 |
| `TaskDao` | 保持现有能力，新增按 `goalId` / `stageId` 查询和关联清空 |
| `GoalRepository` | 目标/阶段写操作、事务、删除确认统计、关联校验 |
| `HabitRepository` | 打卡/取消打卡 + 成长事件事务、归档、删除 |
| `TaskRepository` | 保持单入口；完成时在事务中写入成长事件；增加目标/阶段校验 |
| `GrowthRepository` | 只读聚合 + 事件幂等写入，不直接操作 UI 状态 |
| `GoalMetrics` / `HabitMetrics` / `GrowthRules` | 纯函数：进度、连续天数、等级、窗口统计 |

不建议在第一期引入 Hilt。Repository 由 ViewModel 在构造时创建，测试通过 in-memory Room 构造同一套对象。

### 3.6 预留实体（第一期不建表）

- `focus_sessions`：id、taskId?（SET_NULL）、goalId?（SET_NULL）、startedAt、endedAt?、plannedMinutes、actualSeconds、status、note。用于第二期任务时间轴和预计/实际耗时。
- `daily_reviews`：id、epochDay（唯一）、journal、wins、blockers、tomorrowPlan、mood?、createdAt、updatedAt。用于第三期每日回顾。
- `achievement_unlocks`：achievementId（PK）、unlockedAt、sourceEventId?。成就定义放代码常量表，解锁时间落库；第二期实现。
- `tasks` 的 `estimatedMinutes`、`startTime` 等时间轴字段也在第二期迁移中增加，不提前塞进 v2。

---

## 4. 导航与界面规划

### 4.1 五个主要区域

| 区域 | 路由 | 底部标签 | 内容 | 复用/新增 |
| --- | --- | --- | --- | --- |
| Orbit | `orbit` | 星轨 | 现有星轨、今日汇总、今日习惯、宇宙速览 | 保留 + 小增量 |
| Goals | `goals` + `goals/{goalId}` | 目标 | 目标星系、目标列表、目标详情/阶段 | 新增 |
| Planner | `planner` | 规划 | 分段：任务 / 习惯；专注在第二期加入 | 复用任务页主体 + 新增习惯 |
| Growth | `growth` | 成长 | 成长概览 + 现有统计卡 + 习惯一致性 | 复用 StatsScreen + 新增成长卡 |
| Profile | `profile` | 我的 | 设置、外观、数据概览、关于 | 复用 SettingsScreen + 数据卡 |

编辑器仍作为根级 BottomSheet：任务编辑器、目标编辑器、阶段编辑器、习惯编辑器。任何时刻最多一个编辑 Sheet，避免叠加。

### 4.2 页面结构与星轨表达

#### 星轨首页

保持现有顺序：问候 → 星轨 Canvas → 轨道提示 → 统计卡 → 主按钮 → 今日任务 → 本地说明。

新增两个轻量模块，不挤压星轨：

1. **宇宙速览**：一行三枚玻璃小卡/胶囊，显示活跃目标数、今日习惯完成数、当前等级；分别跳转目标、规划习惯、成长。
2. **今日习惯**：一张紧凑玻璃卡，最多显示 3 个活跃习惯，每行有 48dp 打卡圆钮、连续天数和 7 日小点；“查看全部”去规划。没有习惯时只显示一句引导，不强制占大块空间。

星轨节点仍只对应今日任务，不混入目标或习惯。这样保留现有拖动、9 节点上限、无障碍语义和测试基线。

#### 目标星系

- 顶部：`ORBIT / GOALS`、标题「目标」、创建按钮。
- Hero：`GoalGalaxy` Canvas。中心是个人核心，最多 7 个活跃目标沿椭圆轨道分布；每个节点显示目标色、进度弧和阶段数。点击打开目标详情。**第一期不做拖动**，位置按 `sortOrder + targetDate` 确定性计算。
- 下方：活跃目标玻璃卡列表（进度条、阶段数、任务数、目标日期），再下面是已完成/已归档折叠区。
- 空状态：中心出现幽灵目标节点 + 「建立第一个长期目标」。

#### 目标详情

- Hero：目标行星 + 环形进度 + 目标日期/状态 + 完成/暂停/归档/删除入口。
- 阶段轨道：竖向时间线 + 阶段节点（完成实心、当前高亮、未来描边），不使用第二个可拖动 Canvas。每个阶段显示任务完成数/总数。
- 阶段操作：添加、编辑、标记完成、删除；删除时提示任务会保留。
- 任务区分组：按阶段列出关联任务；阶段下可“添加任务”并自动预填目标/阶段；上方单独显示“未分配阶段”的目标级任务。
- 目标删除确认展示阶段数和任务数，明确“任务会保留，只解除关联”。

#### 规划

- 顶部标题「规划」，内部两枚分段：**任务 / 习惯**。
- 任务分段：复用现有筛选、任务卡片、添加卡和本地说明；外层标题不再重复两套。
- 习惯分段：今天待打卡列表、每个习惯的连续天数、7 日点阵、12 周热力图、归档区、添加入口。
- 专注在第二期成为第三个分段；第一期不显示假入口。

#### 成长

- 顶部成长概览：等级徽章、XP 进度条、下一级所需、今日/本周新增经验。
- 保留现有统计卡：今日完成率、任务总览、近 7 天、分类分布、连续达成。
- 新增习惯一致性卡：今日打卡数、7 日达标率、最长连续。
- 新增目标进度摘要：活跃目标数、完成阶段数、平均目标进度。
- 空状态判断改为“任务、习惯、成长事件都为空”，不能因为任务为空就把整个成长页隐藏。
- 统计口径说明保留并补充目标/习惯/经验定义，避免同一词在不同页面含义不同。

#### 我的

- 复用现有设置页全部内容：外观、提醒诚实说明、数据与隐私、关于。
- 新增数据概览卡：任务数、目标数、习惯数、打卡记录数（全部来自 Room）。
- 第二期再增加导出/导入；第一期不显示不可用按钮。

### 4.3 导航实现要点

- `OrbitDestination` 扩展为五项，标签建议：**星轨 / 目标 / 规划 / 成长 / 我的**。
- `OrbitGlyph` 增加 `GOALS`、`PLANNER`、`GROWTH`、`PROFILE` 或复用对应图形，保留现有自绘风格。
- 底部选中状态支持前缀匹配：`goals/{id}` 仍高亮“目标”；不能继续用严格相等。
- 返回逻辑拆分：
  - 目标详情：由 NavHost 正常 pop 回目标列表。
  - 顶层非星轨页：回到星轨（现有行为）。
  - 星轨选中任务：先关闭节点详情。
- 保留 `saveState / restoreState / launchSingleTop`；每个主区域独立保存滚动位置。
- 根级编辑 Sheet 需要统一协调：任务、目标、阶段、习惯同一时间只开一个；Snackbar 反馈从任务专用类型抽象为通用消息，避免三套 Snackbar。
- `OrbitApp` 参数过多，实施时按 `OrbitActions` / 领域状态分组，而不是再平铺十个回调。
- 360dp 宽度下五个 54dp 项理论可容纳，但必须实机检查标签不换行、不截断；标签保持两字。

---

## 5. 实现难度与开发顺序

难度：S 小、M 中、L 大。每个里程碑结束都必须能编译、跑通对应测试，不允许把半个数据库状态留在主分支。

| 顺序 | 里程碑 | 内容 | 难度 | 依赖 | 完成标志 |
| --- | --- | --- | --- | --- | --- |
| M0 | 领域与数据库地基 | 新实体/枚举/转换器/DAO；v2 迁移；时间/连续天数/等级纯函数；迁移测试 | M | 无 | v1 测试数据库可升级；领域单测通过 |
| M1 | 目标与五区域壳 | Goal CRUD、目标列表、目标编辑 Sheet、目标星系 Canvas、五标签导航 | L | M0 | 可创建/编辑/归档目标；目标星系可打开详情 |
| M2 | 阶段与任务关联 | 阶段 CRUD、目标详情、任务编辑器目标/阶段选择、进度计算、删除规则 | M | M1 | 阶段任务端到端；删除目标保留任务 |
| M3 | 习惯 | Habit 数据、打卡事务、规划习惯分段、热力图、首页今日习惯卡 | L | M0（可与 M1/M2 并行） | 打卡/取消/连续天数/热力图真实 |
| M4 | 成长与 Profile | 成长事件写入、XP/等级、成长页概览、Profile 数据概览、Snackbar 统一 | M | M1–M3 | 完成任务/打卡/阶段完成即时反映到成长页 |
| M5 | 回归与交付 | 全量测试、360/411 深浅色、迁移升级、截图、README、APK | M | M0–M4 | 旧 63 项测试 + 新测试全绿，截图更新 |

**建议实际开发顺序**：M0 → M1 → M2 → M3 → M4 → M5。M3 与 M2 无硬依赖，如果时间紧可以并行，但合并时要先解决导航壳和测试助手的冲突。

### 第二期

- 专注计时 + 任务时间轴 + 预计/实际耗时（新增 FocusSession、Task 时间字段，迁移 v3）。
- 成就墙 + 解锁持久化（基于 growth_events，新增 achievement_unlocks，迁移 v3/v4）。
- 目标阶段拖拽排序、任务批量归档、更多目标/习惯筛选。

### 第三期

- 每日回顾（DailyReview，迁移 v4）、周/月复盘。
- 数据导出/导入、备份说明。
- AI 计划助手。

### AI 助手边界

- 第一期不添加网络权限、不接任何外部 API、不在设置页放假入口。
- 未来接口设计为 `PlanSuggestionService`，输入结构化目标/阶段/约束，输出 `GoalPlanDraft`；UI 只负责预览和确认。
- 默认本地规则/模板先行；接远程模型必须显式开关、明确数据范围、失败可回退，不自动上传用户任务。

---

## 6. 各模块验收标准

### 6.1 数据库与迁移

- [ ] 数据库版本从 1 升到 2，新增五张表，`tasks` 增加 `goalId`/`stageId` 与索引。
- [ ] 用真实 v1 schema 构造数据库，迁移后原有任务的标题、描述、优先级、分类、日期、完成状态、时间戳完全一致。
- [ ] 不启用 `fallbackToDestructiveMigration`；迁移失败应显式失败而不是清空数据。
- [ ] 删除目标：阶段级联删除，任务保留且两个外键为 null。
- [ ] 删除阶段：任务保留，`stageId` 为 null，`goalId` 不变。
- [ ] 删除习惯：打卡记录级联删除，成长事件保留。
- [ ] 所有迁移和删除行为有设备测试。

### 6.2 目标星系

- [ ] 可创建目标（标题、说明、目标日期、色调）；空白标题被拦截；保存后重启仍在。
- [ ] 星轨首页不受目标数据影响；目标星系最多显示 7 个活跃目标并有溢出说明。
- [ ] 目标节点点击进入详情；暂停/完成/归档目标从星系移除或进入对应分组。
- [ ] 目标进度严格符合 3.4 公式；阶段增删、任务完成、目标级任务变化后即时更新。
- [ ] 无阶段、无任务、全部完成、部分完成、归档均有明确状态和文案。
- [ ] 目标删除确认显示正确阶段/任务数量，确认后任务仍在规划中。

### 6.3 阶段与任务关联

- [ ] 目标详情可新增、编辑、完成、删除阶段；顺序稳定，不因刷新重排。
- [ ] 任务编辑器可选择目标和阶段；选择阶段时目标自动一致；非法组合在仓库层被拒绝。
- [ ] 阶段详情可添加任务、查看关联任务、完成任务；任务完成后目标进度变化。
- [ ] 删除阶段后任务进入“未分配阶段”，不会消失。
- [ ] 目标任务仍出现在规划任务列表和今日星轨（若截止今天），不被目标页“私藏”。
- [ ] 任务卡片/详情能显示目标/阶段标签，但不破坏现有无目标任务的布局。

### 6.4 习惯

- [ ] 可创建、编辑、归档每日习惯；空白名称被拦截。
- [ ] 今天可打卡/取消；同一天重复打卡只产生一条记录；跨天刷新后“今天”正确推进。
- [ ] 连续天数符合“今天未打卡但昨天打过仍保留”规则；空记录为 0。
- [ ] 12 周热力图只使用真实记录；未来日期不显示完成；周一开头。
- [ ] 首页今日习惯卡与规划页状态同步，打卡按钮不小于 48dp。
- [ ] 归档习惯保留记录并从今日列表隐藏；删除后记录清除。

### 6.5 成长

- [ ] 完成任务、习惯打卡、阶段完成、目标完成分别产生一次成长事件；重复完成/取消再完成不会重复发放。
- [ ] XP/等级边界符合公式；Lv.2/3/4 阈值测试通过。
- [ ] 成长页显示等级、总 XP、下一级进度、本周新增；数据全部来自 Room。
- [ ] 任务为空但习惯有记录时，成长页仍展示成长与习惯数据，不落入错误空态。
- [ ] 没有任何事件时给出真实空态，不显示假等级或假图表。
- [ ] 口径说明能解释经验、目标进度、习惯连续天数。

### 6.6 导航与 Profile

- [ ] 五个底部标签在 360dp / 411dp 不截断、不换行，触控高度不小于 48dp。
- [ ] 目标详情返回目标列表；其他顶层页返回星轨；星轨详情维持原有“先关详情”行为。
- [ ] 任务编辑、目标编辑、习惯编辑不会同时叠加；返回可关闭当前 Sheet。
- [ ] 现有主题/强调色持久化、提醒“暂未接入”、无通知权限、无通知渠道仍成立。
- [ ] Profile 数据概览数字与数据库一致；没有未实现的导出按钮。
- [ ] 深空/晨光、空/满、长标题下均无遮挡；玻璃卡片复用现有系统。

### 6.7 兼容性

- [ ] 原有任务 CRUD、完成换环、拖动、筛选、统计、删除确认、返回栈全部通过。
- [ ] 没有网络权限、没有云同步、没有提醒调度；仍在本地 Room/SharedPreferences。
- [ ] 所有关键业务逻辑有 JVM 或设备测试，不在 Composable 里临时算统计。

---

## 7. 数据库迁移与回归测试计划

### 7.1 v1 → v2 迁移

当前 `OrbitDatabase` 为 version 1，schema 1 已导出。v2 建议：

1. 新增 `goals`、`goal_stages`、`habits`、`habit_records`、`growth_events` 及索引。
2. **重建 `tasks` 表**而不是简单 `ALTER TABLE ADD COLUMN`：
   - 创建 `tasks_new`，包含原字段 + `goalId`/`stageId` 和两个外键（`SET NULL`）；
   - `INSERT INTO tasks_new SELECT 原字段, NULL, NULL FROM tasks;`
   - 删除旧表、重命名、重建 `isCompleted` / `dueDate` / `goalId` / `stageId` 索引。
   - 这样 Room 期望的外键定义与迁移后的实际 schema 完全一致，避免列级 ALTER 带来的验证差异。
3. `Room.databaseBuilder(...).addMigrations(MIGRATION_1_2)`。
4. **不添加** `fallbackToDestructiveMigration()`；缺失迁移就抛出，保护数据。
5. 迁移在 Room 的事务内完成；表很小，重建成本可接受。
6. 迁移后生成并提交 `schemas/.../2.json`。
7. 不回填历史成长事件；升级后的第一次达成开始记录，UI 明确说明。
8. 版本号建议升到 `versionCode 2` / `versionName 2.0.0`（第一期交付时）。

`MIGRATION_1_2` 的测试步骤：

- 用 `MigrationTestHelper` 打开 v1、插入 2–3 条覆盖不同优先级/完成状态的任务。
- 执行迁移到 v2。
- 校验任务行数、全部字段、完成时间、索引。
- 校验新表存在且外键行为正确：插入目标/阶段/任务 → 删除目标 → 任务仍在、外键为空、阶段消失。
- 插入重复习惯记录 → 唯一键生效。
- 删除习惯 → 记录消失，成长事件仍在。
- 关闭并重新打开文件数据库，确认数据仍在。

### 7.2 测试分层

| 层级 | 新增/修改 | 说明 |
| --- | --- | --- |
| JVM 纯函数 | `GoalMetricsTest`、`HabitMetricsTest`、`GrowthRulesTest`、`TaskLinkRulesTest` | 进度、连续天数、等级、阶段归属校验；不依赖 Android |
| DAO/迁移 | `MigrationTest`、`GoalDaoTest`、`HabitDaoTest`、`GrowthDaoTest` | 真实 Room、外键、唯一键、事务 |
| UI 流程 | `GoalUiFlowTest`、`HabitUiFlowTest`、`GrowthUiFlowTest`、`NavigationShellTest` | 创建/编辑/完成/删除/空态/持久化/返回栈 |
| 回归 | 原有 37 JVM + 26 设备测试 | 更新导航标签助手，保留语义和断言 |
| 截图 | `ScreenshotGalleryTest` | 增加目标星系、目标详情、习惯热力图、成长页、Profile |
| 手动 | v1 APK → v2 APK 覆盖安装 | 真实升级路径，不清数据 |

新增依赖：`androidx.room:room-testing`（instrumentation），版本与当前 Room 2.8.5 对齐。

### 7.3 现有测试需要改的地方

- `navigateToTab("任务")` → `navigateToTab("规划")`；进入后仍可看到任务内容。
- `navigateToTab("统计")` → `navigateToTab("成长")`。
- `navigateToTab("设置")` → `navigateToTab("我的")`。
- 内容标题可以保留「任务」「统计」「设置」以减少断言变化，但外层头部和导航标签必须统一。
- `ScreenshotGalleryTest` 的导航和截图数量更新。
- `TaskRepository` 构造参数变化会影响 `TaskDaoTest`；改为传入完整数据库/成长 DAO。
- `Task` 新增字段必须提供默认值 null，避免现有预览和测试构造失败。
- 原有节点语义、完成率描述、删除确认、主题持久化断言不得被削弱。

### 7.4 回归验收

- `:app:testDebugUnitTest` 全部通过。
- `:app:connectedDebugAndroidTest` 全部通过。
- 覆盖安装验证：v1 创建任务 → 安装 v2 → 任务仍在、新表为空、首页星轨正常。
- 目标删除/阶段删除/习惯删除的外键行为逐项验证。
- 360dp 与 411dp、深空与晨光、空/满、长标题、五标签导航手动检查。
- `ScreenshotGalleryTest` 重新生成截图，README 更新产品结构、数据模型、迁移说明、测试数量和新限制。
- 性能观察：目标星系首帧、滚动、空闲帧；不新增持续动画或真实模糊，不把 Debug 模拟器数字当 Release 基准。

### 7.5 发布与回滚

- 第一期只出 Debug APK；不配置 Release 签名。
- 不支持降级安装（Room 默认拒绝旧版本打开新数据库）；文档写明。
- 迁移失败不自动清库；保留原数据库文件，用户可反馈后修复。导出/备份在第三期提供。
- 提交 schema 1/2 与迁移测试，后续每升一版都增量添加 Migration。

---

## 8. 需要确认的默认决策

| 决策 | 推荐方案 | 备选/影响 |
| --- | --- | --- |
| 五个底部标签 | 星轨 / 目标 / 规划 / 成长 / 我的 | 若保留「任务/统计/设置」，语义会更偏工具，习惯和成长入口不明显 |
| 目标进度 | 阶段进度平均值；阶段有任务时按任务完成率，无任务按手动完成状态 | 也可只按阶段完成数，但任务完成不会推动目标进度 |
| 目标删除 | 阶段级联删除，任务保留并解除关联 | 不提供“删除目标同时删任务”；避免不可逆批量误删 |
| 成长经验 | 事件账本，同一来源只奖励一次，撤销不扣经验 | 若改为实时派生，重开任务会掉等级，测试和口径更复杂 |
| 第一期成就 | 只做等级/XP/统计，不做成就墙 | 成就墙需要解锁表和规则目录，放第二期 |
| 习惯频率 | 第一期仅每日习惯 | 每周 N 次需要调度字段和热力图口径，放后续迁移 |

如果以上默认方案没有异议，下一步从 **M0：数据库 v2 + 领域纯函数 + 迁移测试** 开始；M0 完成并跑通测试后再进入目标星系 UI。任何一项要改，先在这一步确认，避免后续返工。
