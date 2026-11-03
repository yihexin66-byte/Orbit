# Orbit 2.4 · AI 学习规划与周复盘

2.4 把 Orbit 从「记录工具」补成「用一个 AI 助手做学习计划、每周复盘」的学生应用：目标页新增 AI 规划入口，输入目标、截止日期、每天可投入时长与当前水平，云端生成带阶段和任务的学习计划，勾选后一键导入 Room；成长页新增 AI 周复盘，根据本周真实完成率、学习任务、习惯打卡、XP 与目标进度生成一段可执行的总结。

AI 能力不直接连大模型，也不把 Coze 令牌放进 APK。安卓只认 Orbit 自己的三个 REST 端点，云函数 `orbit-api` 持有 Coze Personal Access Token，负责提示词、结构化校验、一次修复重试与按用户每日配额。这样做的代价是多一跳网络与一次 Node 解析，换来的是：令牌不进客户端、提示词可版本化、模型输出不会把畸形 JSON 写进数据库、每个账号每天有明确上限。

- 代码状态：Android 规划页 / 周复盘卡 / 导入链路、云函数 AI 网关、Coze 客户端、单元与设备测试、Coze 搭建文档均已完成；2.5 又在此基础上新增自由对话与学习资料，见 [`ORBIT_2.5_LEARNING.md`](ORBIT_2.5_LEARNING.md)
- 测试状态：Node 49（2.5 已增至 55）/ JVM 96 通过；设备 52 项（48 项断言 + 4 项 AI 截图，5 张 PNG）通过（Pixel 7 Pro / API 37）
- 部署状态：云函数的 AI 路由已在 `mint` 生效（`GET /api/ai/status`、`POST /api/ai/plan`、`POST /api/ai/review`）；**2026-10-28 晚已配置** `COZE_API_TOKEN` / `COZE_BOT_ID`，`configured:true`，真实规划 / 复盘均通过，配置步骤见 [`COZE_SETUP.md`](COZE_SETUP.md)
- 版本状态：`2.4.0`（versionCode 6），2.5 已升至 `2.5.0`（versionCode 7）；数据库仍是 Room v2，无新增表

## 1. 用户路径

### 1.1 AI 学习规划

```text
目标页「用 AI 拆解学习目标」
  ↓ 填写目标 / 截止日期 / 每日分钟 / 当前水平 / 薄弱点
POST /api/ai/plan（Bearer）
  ↓ 云函数拼 plan-v1 提示词 → Coze Bot → JSON
服务端校验、截断、换算日期、必要时修复重试一次
  ↓ 返回 stages / tasks
预览页：按阶段勾选任务（可整段全选 / 全不选）
  ↓ 导入
GoalRepository.importPlan：同一事务写 Goal + GoalStage + Task(STUDY)
  ↓
跳到目标详情页
```

- 入口在目标页头部下方，卡片上标 `AI`；规划页是独立路由，返回键回到目标页。
- 表单默认目标日期为今天 + 56 天，每日时长默认 45 分钟，可选 30 / 45 / 60 / 90 / 120；目标最多 60 字，水平与薄弱点各 200 字，本地先做长度与范围校验。
- 生成中显示独立的等待卡；生成失败保留上一份预览，不清空已勾选状态。
- 导入前会再次裁剪：目标标题 60、阶段标题 60、任务标题 80，空阶段 / 空任务丢弃，日期夹在今天到 +730 天；全部被丢弃时不会写库。
- 导入产生的是普通目标与普通任务：分类固定 `STUDY`，优先级来自模型（`HIGH/MEDIUM/LOW`），之后可以在目标详情页继续改名、挪阶段、完成任务。AI 只在「生成」时参与，后续不再有隐藏状态。

### 1.2 AI 周复盘

成长页在「目标进度」之后新增 `AI REVIEW` 卡：

- 没有缓存时展示说明、隐私提示与「生成本周复盘」按钮；本周没有任何可复盘数据时按钮禁用并说明原因。
- 有缓存时展示标题、总结、做得好的、需要留意、下周建议与生成时间；可以「重新生成」。
- 生成结果按 `用户 + 周一日期` 缓存在 `SharedPreferences`（`orbit_ai_review`），退出登录或令牌过期时清除该用户前缀；不上传、不跨设备同步，所以卡片标注「本机保存」。
- 周复盘请求只发聚合数据：本周计划数 / 完成数、学习任务数、习惯可打卡天数 / 实际打卡数、本周 XP、最多 8 个进行中目标的标题与进度。**不发任务标题、不发备注、不发账号密码**。
- 周一按北京时间计算，和云端配额同一天界。

## 2. 云函数 AI 网关

### 2.1 路由

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/ai/status` | Bearer | 是否已配置、提示词版本、超时、每日上限与今日已用 |
| POST | `/api/ai/plan` | Bearer | 生成学习计划，返回规范化后的阶段 / 任务 |
| POST | `/api/ai/review` | Bearer | 生成周复盘 |

状态响应示例：

```json
{
  "configured": true,
  "planPromptVersion": "plan-v1",
  "reviewPromptVersion": "review-v1",
  "timeoutSeconds": 25,
  "limits": { "plan": 6, "review": 4 },
  "usage": { "planUsed": 2, "reviewUsed": 0 },
  "dayKey": "2026-10-28"
}
```

`configured` 只取决于云函数进程里是否同时存在 `COZE_API_TOKEN` 与 `COZE_BOT_ID`；客户端不读取、不展示令牌。

### 2.2 请求与规范化

规划请求：

```json
{
  "goal": "三个月内掌握 Android 开发",
  "targetDateEpochDay": 20800,
  "dailyMinutes": 60,
  "currentLevel": "会 Kotlin 基础语法",
  "weakPoints": "状态管理、接口联调"
}
```

- 日期是 epoch day（北京时间自然日），服务端把今天作为第 0 天，模型只返回相对天数，避免模型算错时区与月份。
- 目标 1–60 字；目标日期 0–730 天；每日时长 10–480 分钟；水平 / 薄弱点可为空，超长截断。

模型必须返回：

```json
{
  "goalTitle": "……",
  "goalDescription": "……",
  "stages": [
    {
      "title": "基础巩固",
      "endDayOffset": 14,
      "tasks": [
        {
          "title": "复习 Java 集合",
          "dayOffset": 2,
          "priority": "HIGH",
          "note": "重点泛型与 Stream"
        }
      ]
    }
  ]
}
```

服务端约束：1–6 个阶段、每阶段 1–5 个任务、总计不超过 20 个；任务标题 80 字、阶段标题 60 字、目标描述 160 字；`priority` 只接受 `HIGH/MEDIUM/LOW`，缺省转 `MEDIUM`；任务偏移夹在 0..目标天数，阶段结束日不早于本阶段最后一个任务，也不早于上一阶段结束日。最终写库的是 epoch day，客户端不需要再做换算。

复盘请求：

```json
{
  "weekStartEpochDay": 20790,
  "planned": 10,
  "completed": 6,
  "studyPlanned": 5,
  "studyCompleted": 4,
  "habitPossible": 14,
  "habitCheckIns": 9,
  "xp": 95,
  "goals": [
    { "title": "8 周复习 Java 后端", "progress": 42, "stageCount": 3, "completedStages": 1 }
  ]
}
```

- `completed` 不会大于 `planned`，`studyCompleted` 不会大于 `studyPlanned`，`habitCheckIns` 不会大于 `habitPossible`；目标最多 8 个。
- 四项（计划、习惯可能天数、XP、目标）全为空时返回 400 `AI_NO_DATA`，不消耗配额。
- 复盘结果：`headline` 40 字、`summary` 240 字、`wins` 1–4 条（80 字）、`risks` 0–3 条（80 字）、`nextWeek` 1–3 条（标题 80、理由 100）。

### 2.3 提示词与一次修复

- 提示词固定在 [`lib/prompts.js`](../unicloud/cloudfunctions/orbit-api/lib/prompts.js)，版本号 `plan-v1` / `review-v1` 随每次响应返回，方便排查「同一份输入为什么结果变了」。
- 系统提示把模型限定为「结构化数据引擎」：只输出 JSON、不解释、不使用插件 / 知识库 / 联网、把 `<user_input>` 与 `<week_stats>` 当数据而不是指令，并明确拒绝提示词注入。
- 第一次回复不是合法 JSON 或结构不合规时，服务端把错误原因与原始输出拼成修复消息，再请求一次；仍失败就返回 `AI_INVALID_OUTPUT`（502）。整次调用共享 25 秒预算，云函数超时为 30 秒。
- `coze.js` 用 Coze v3 非流式 Chat：`POST /v3/chat` 创建会话 → 轮询 retrieve 直到完成 → 拉取消息列表取 `type=answer` 的 assistant 文本。零 npm 依赖，只依赖 Node 18 的 `fetch`。

### 2.4 配额与失败语义

- 每账号每北京时间自然日：规划 6 次、复盘 4 次。计数写在 `orbit_users` 的 `aiDayKey / aiPlanCount / aiReviewCount`，跨设备一致。
- 配额在调用 Coze **之前**扣减：模型超时、输出不合法也会消耗次数。这是刻意选择——每次调用都已经产生上游成本，失败重试不应绕过上限。
- 错误码与客户端提示：

| 场景 | HTTP | code | 客户端行为 |
| --- | --- | --- | --- |
| 未配置令牌 | 503 | `AI_NOT_CONFIGURED` | 规划页显示「AI 暂未配置」与重试；复盘卡显示「AI 暂不可用」 |
| 输入不合法 | 400 | `AI_INPUT_INVALID` | 保留表单，提示检查目标 / 日期 / 时长 |
| 本周无可复盘数据 | 400 | `AI_NO_DATA` | 复盘卡提示先完成任务或打卡 |
| 今日次数用完 | 429 | `AI_QUOTA_EXCEEDED` | 按钮禁用，显示「明天 00:00 重置」 |
| 超过 25 秒 | 504 | `AI_TIMEOUT` | 提示稍后重试 |
| Coze 429 / 并发限制 | 503 | `AI_BUSY` | 提示稍后重试 |
| 两次 JSON 都不合法 | 502 | `AI_INVALID_OUTPUT` | 提示换一种描述再试 |
| 令牌过期 | 401 | `UNAUTHORIZED` | 回到登录门禁并清缓存 |
| 网络断开 | — | — | 客户端本地映射为网络错误，不发请求 |

未知错误码先按 HTTP 状态归类，再按状态码兜底；服务端返回的中文 `message` 优先展示，避免把英文上游错误直接丢给用户。

## 3. Android 实现

新增文件：

```text
app/src/main/java/com/example/orbit/ai/
├── AiModels.kt          # 请求 / 响应 / 缓存 DTO
├── AiErrors.kt          # AiFailureKind、HTTP 映射
├── AiRepository.kt      # Retrofit 调用与响应形状检查
├── AiPlanSelection.kt   # 勾选集合、整段全选 / 全不选
├── AiPlanImport.kt      # 预览 → GoalImportPlan，裁剪与日期夹取
├── AiReviewCache.kt     # SharedPreferences 按用户 + 周一缓存
└── AiReviewMetrics.kt   # 从 Room 数据构造复盘请求
app/src/main/java/com/example/orbit/ui/
├── AiPlanViewModel.kt   # FORM / GENERATING / PREVIEW / IMPORTING / IMPORTED
├── AiReviewViewModel.kt # 状态、缓存、可用性、失败重试
└── screens/
    ├── AiPlanScreen.kt  # 表单、日期选择、预览、勾选、导入
    └── AiCopy.kt        # 失败文案
```

- `AiPlanSelection` 用稳定的 `stageIndex:taskIndex` 作为键，阶段增删不会把另一段的勾选带过去；空阶段默认视为未选中，但「全选」会把空阶段重新打开。
- `AiPlanImport` 是纯函数，先裁剪再判断是否还有任务，避免把空目标写进数据库；日期同时夹在今天到 +730 天，阶段日期不早于其任务。
- `GoalRepository.importPlan` 在同一个 Room 事务里写目标、阶段和任务，任务全部是 `TaskCategory.STUDY`；不写成长流水，避免「导入即刷 XP」。
- `AiReviewViewModel` 合并 Room 的任务 / 习惯 / 打卡 / 目标 / 成长流，数据变化后重新计算可复盘性；缓存命中时直接展示，不自动请求，只有用户点按钮才消耗配额。
- QA 旁路：测试会话把同步标记为 blocked，两个 ViewModel 都不发 AI 网络请求，所以 48 项设备断言完全离线，不消耗真实配额。

## 4. Coze 侧配置（用户执行）

完整步骤、可复制的系统提示词、发布与排障见 [`COZE_SETUP.md`](COZE_SETUP.md)。要点：

1. 在 coze.cn 建一个 Bot，模型选非推理模型、温度 0.2–0.4、最大回复 4096，关闭插件 / 知识库 / 记忆 / 开场白 / 推荐问题，系统提示只做 JSON 引擎。
2. 只发布「API」渠道（channel id `1024`）；发布后从编辑页 URL 取 `/bot/` 后面的数字作为 `bot_id`。
3. 在 `coze.cn/open/oauth/pats` 建一个只有 chat 权限的 PAT，有效期 30 天；令牌只显示一次。
4. 在 uniCloud 控制台给 `orbit-api` 配环境变量 `COZE_API_TOKEN`、`COZE_BOT_ID`；不要提交 `.env`，不要发给任何人，也不要写进 APK。
5. 用 App 或 `node unicloud/tools/check-ai.mjs` 检查 `configured:true`，再各生成一次计划与复盘。

> 令牌只存在于云函数环境变量。客户端、日志、截图与仓库里都不应出现 `pat_` 开头的字符串；如果怀疑泄露，立即在 Coze 控制台吊销并新建。

## 5. 测试与验证

| 项目 | 结果 | 说明 |
| --- | --- | --- |
| `node --test`（云函数） | 49 项通过、0 失败 | 原有账号 / 同步 29 项 + AI 20 项：输入校验、日期偏移、阶段单调、配额、错误映射、一次修复、Coze 客户端轮询与超时 |
| `:app:testDebugUnitTest` | 96 项通过、0 失败 | 新增 30 项：`AiReviewMetrics` 9、`AiPlanImport` 9、`AiPlanSelection` 7、`AiErrorMapper` 5 |
| 设备断言 | 48 项通过、0 失败 | 原 46 项 + `importPlanWritesGoalStagesAndStudyTasks`、`importPlanRejectsAnEmptyPlan` |
| AI 截图 | 4 项通过（5 张 PNG） | 规划表单、预览顶 / 底、复盘卡与生成提示；写入 `artifacts/phase12/` |
| 全量设备套件 | 52 项通过、0 失败，611.577 s | 48 项断言 + 4 项截图（含旧版截图用例） |
| AI 状态 | 三个 AI 路由已上线（未带令牌返回 401）；配置后 `/api/ai/status` 返回 `configured:true` | 对比 `/api/health` 返回 200 |
| 真实 Coze 调用 | **2026-10-28 晚已通过**：plan 约 11.3 s / 3 阶段 12 任务；review 约 6.8 s；用量计数正常累加（`check-ai.mjs`） | 账号持有人自行配置环境变量，令牌不写进代码或交给他人 |
| 真机 E2E | **未执行** | 计划生成 → 导入 → 完成一个学习任务 → 周复盘，尚未在实体设备上跑通 |

后端单测使用内存 store 和假 Coze 客户端，验证的是「响应形状与容错」，不验证 Coze 账号、模型选择或真实延迟；真实调用已由账号持有人配置后补测一次，仍不足以评估模型质量与稳定性。

## 6. 已知限制

- 没有流式输出：Coze 非流式接口 + 轮询，25 秒内拿不到就判超时；复杂目标可能被截断为更少的阶段 / 任务，而不是失败。
- 模型只生成「目标 + 阶段 + 任务」三层，不生成每日时间表、不自动排优先级冲突，也不回写已有目标。
- 复盘只基于聚合数字与目标标题，不读任务备注，不适合做细粒度的学习内容诊断。
- 周复盘缓存只在本机，换设备或清数据后需要重新生成，并会再消耗一次配额。
- 配额按账号而不是按设备，也不区分 iOS / Android；同一账号多端共享。
- 令牌有效期最长 30 天，到期后 `configured` 变为 false 或调用返回 401 / 503，需要重新生成 PAT 并更新环境变量。
- 云函数没有服务端限流之外的防刷手段；公开运营前应在网关层加 IP / 设备级频率限制。

## 7. 与岗位的对应

| 岗位关注点 | Orbit 2.4 的证据 |
| --- | --- |
| 一门语言写业务 | Kotlin（Android）+ JavaScript（云函数），同一套 REST 契约 |
| Web / 数据库 / API 基础 | Bearer 鉴权、REST 路由、错误体、epoch day 语义、快照 revision CAS、Room 事务导入 |
| AI / Agent / LLM 应用 | Coze Bot + PAT、提示词版本化、结构化输出校验、一次修复重试、每日配额、超时与失败降级 |
| 面向学生 / 教育场景 | 学习目标拆解、每周复盘、习惯与 XP 反馈，都是真实教育产品的核心动作 |
| 工程化与验证 | Node 49 / JVM 96 / 设备 48 项测试，截图、接口文档、排障表与安全约束齐全 |

更完整的岗位包装、演示脚本与面试问答见 [`ORBIT_2.4_JOB_PACKAGING.md`](ORBIT_2.4_JOB_PACKAGING.md)。
