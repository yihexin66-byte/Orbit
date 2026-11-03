# Orbit Learn · 星轨

一个把任务、目标、习惯、学习资料与成长记录放进同一片「个人数字宇宙」的 AI 学习应用。星轨是今天的巡检入口，目标页把长期目标、任务与每日习惯收进同一页，学习页承载完整的今日学习与背单词，成长页只负责如实记账，我的页负责账号、头像与云同步；五个标签加一枚常驻 AI 助手，共同组成 Orbit Learn 2.9。

项目定位是个人作品集与 Android 工程练习，不是待上架的商业产品。设计上刻意保持小体量，但对数据一致性、空状态、错误恢复、主题持久化、数据库迁移、手势细节、账号鉴权与多设备同步做了完整处理。2.2 把入口改成强制登录：没有会话时只能看到独立的登录 / 注册页，任务、目标、习惯与成长流水以账号为准，Room 只承担离线缓存；首页招呼语读取用户自定义昵称，星轨新增「拖入核心点亮」的完成手势，相册与相机仍走真实系统权限弹窗。2.3 把后端迁到 uniCloud 支付宝云：云函数 `orbit-api` 与三个集合已部署到 `mint` 空间，默认 APK 已指向该空间。2.4 加入 AI 学习规划与周复盘：目标页可按目标、截止日期、每日时长生成阶段与任务，勾选后以单个 Room 事务导入；成长页可按本周真实数据生成复盘。2.5 改名 Orbit Learn，在首页右下角加入 AI 学习助手：点开即是一个可自由追问的对话框，也能一键生成计划、周复盘或打开学习资料；首页新增「今日学习」卡片，内置 8 首古诗与 8 个英语单词，任何一条都能交给 AI 逐句讲解。AI 走云函数转发 Coze Bot，令牌只存在服务端环境变量，客户端只认 Orbit 自己的四个 `/api/ai/*` 端点。2.6 修掉学习列表滑到底部的抖动，把资料整理成三本书（12 首古诗 / 18 个英语词 / 18 个数学知识点），数学可按初一至高三选年级；助手球可拖到内容区任意位置，星轨节点可拖到另一条环，首页新增可刷新的每日一句。2.7 把两本书换成真实词库：古诗对接《唐诗三百首》309 首、英语对接高中 3500 词（去重后 3,877 条），随 APK 以 JSON 资源打包、后台预加载，卡片按长诗折叠与空字段降级处理；助手球离开默认位置后自动半透明（0.55），贴近屏幕边缘再降到 0.32，按住或拖动时恢复常亮。2.8 给两本书再加一层管理：古诗可按诗名、作者、朝代或诗句搜索，任意一首都能设为今日；单词可收藏、按「全部 / 收藏」筛选，任意一条也能设为今日。选择按账号保存在本机，首页、书架与书页同步，跨天自动回到每日轮换。2.9 把「目标」与「规划」合并成一页三段（目标 / 任务 / 习惯），空出底部标签给新增的「学习」页：首页「今日学习」收成三行预览，完整古诗、单词、数学与背单词入口都搬进学习页；背单词按「未学优先 → 错词 → 最久没学」自动选词，数量可在 5 / 10 / 20 / 30 与 1–50 之间自定义，也可以手动挑词（上限 50），每轮翻卡后自评「认识 / 不认识」，结算正确率并可重练错词。2.9.1 给五个主页面补上跟手的横滑翻页：页面贴着手指平移，横向划满屏宽 20%（不低于 56dp）松手就滑到下一个标签，划回上一侧同理，只看距离不看速度；在卡片上起手也一样翻页（划出 24dp 后由翻页认领手势，卡片自己的点击随之取消），两端与嵌套路由只做橡皮筋、不翻页，目标页三段仍按点击切换，五个页面共用同一层背景，所以滑动过程中不会露出接缝。迁移与设计见 [`docs/ORBIT_2.3_UNICLOUD.md`](docs/ORBIT_2.3_UNICLOUD.md)、[`docs/ORBIT_2.4_AI.md`](docs/ORBIT_2.4_AI.md)、[`docs/ORBIT_2.5_LEARNING.md`](docs/ORBIT_2.5_LEARNING.md)、[`docs/ORBIT_2.6_LIBRARY.md`](docs/ORBIT_2.6_LIBRARY.md)、[`docs/ORBIT_2.7_LIBRARY.md`](docs/ORBIT_2.7_LIBRARY.md)、[`docs/ORBIT_2.8_LIBRARY.md`](docs/ORBIT_2.8_LIBRARY.md) 与 [`docs/ORBIT_2.9_LEARNING.md`](docs/ORBIT_2.9_LEARNING.md)。

## 界面

> 仓库只随包 2.9.1 一版素材：[`docs/screenshots/phase18/`](docs/screenshots/phase18/)、[`artifacts/phase18/`](artifacts/phase18/) 与 [`artifacts/orbit-2.9.1-tab-swipe-debug.apk`](artifacts/orbit-2.9.1-tab-swipe-debug.apk)。下面提到的 phase6–17 截图目录、历史 APK 与项目压缩包是本地交付物，体积合计约 250MB，没有入库，因此那些链接在 GitHub 上会 404，功能说明本身仍然完整可读。

### 2.9.1 横滑切换页面

内容区可以直接左右划着翻页，而且页面跟着手指走：横向划出 8dp 之后当前页就按 1:1 贴在指下，划到哪儿就看到哪儿；松手时横向位移不少于屏宽 20%（同时不低于 56dp）就翻页，不足则弹回原位。提交只看距离不看速度，所以慢慢把整屏划过去和快速一划结果一样。顺序就是底部栏的星轨 → 目标 → 学习 → 成长 → 我的，到头不循环：靠外的一侧改走更轻的橡皮筋（位移打 0.28 折、最多屏宽的 10%），用手感说明「这头没有页了」；`goals/{goalId}` 目标详情与 AI 规划这类嵌套路由同样只橡皮筋、不翻页，填到一半的表单不会被一次误划丢掉，系统侧边返回手势照常可用。目标页的「目标 / 任务 / 习惯」三段仍按点击切换，滑动只负责这五个主页面，避免同一个手势在不同位置含义不同。判定放在内容区的父层，星轨节点拖拽、阶段进度条、可拖动的助手球与横向书架这些真正要指针的子控件会先在 Main pass 拿到事件，只要在 24dp 内消费掉横向位移就仍然归它们管，页面随即停住、松手弹回。卡片是另一种情况：它的点击只靠「指针离开我多远」来取消，而页面是跟着手指整体平移的，卡片相对指针始终没动，所以在卡片上起手横滑会既翻页又点开卡片；现在横滑累计超过 24dp 后，翻页改在 Initial pass（父层先于子层）消费事件，卡片看到手势被取走就取消点击，松手也不会补上，而 24dp 之内照常放行，普通点按与更早认领手势的拖拽都不受影响。翻页是一段真正的平移：旧页顺着手指继续滑出屏幕，新页在屏幕外完成挂载后从对侧滑入，去程时长按已经划过的距离缩短（180ms 收起到 70ms，回程 230ms）。五个页面共用同一层 `SpaceBackdrop` 平铺背景且自身不画底色，所以滑开时露出来的那一条和页面背后完全一致，不会出现割裂的接缝；横滑期间 `NavHost` 的淡入淡出被让开，免得新页带着半透明叠在旧页上，点底部栏切换的淡入淡出照旧，底部栏也依旧是权威入口。16 张 2.9.1 截图位于 [`docs/screenshots/phase18/`](docs/screenshots/phase18/)，其中按住手指不放拍下的中途帧记录了 350px / 600px 的跟手位移、两端与目标详情的橡皮筋，以及在卡片上起手横滑最后落到学习页的完整过程；全部使用离线 QA 数据，不含账号凭证。

### 2.9 目标合并、学习页与翻卡背单词

底部现在是星轨 / 目标 / 学习 / 成长 / 我的：目标与规划合成一页，页内用「目标 / 任务 / 习惯」三段切换，默认落在目标，再次点底部「目标」也会回到目标分段。学习页顶部是完整的今日古诗、今日单词与今日数学，往下是背单词卡和同一套三本书书架；首页只保留三行摘要，不够划的问题因此消失。背单词卡显示今日已学 / 累计词数 / 待复习，先选数量再点「开始学习」，也可以「自己选词」按单词或释义搜索并挑到 50 个；每张卡先只给单词与音标，翻面后才出现释义、例句、译文与记忆提示，然后选「认识 / 不认识」，一轮结束给出正确率与错词数，可以只重练错词、换一轮或收工。14 张 2.9 截图位于 [`docs/screenshots/phase17/`](docs/screenshots/phase17/)，全部使用离线 QA 数据，不含账号凭证。

### 2.8 古诗搜索、每日挑选与单词收藏

古诗书页顶部是一条搜索框：输入诗名、作者、朝代或诗句片段，结果实时收窄并按「找到 N 首」计数，搜不到时有专门的空状态与「清空搜索」；任意一首诗展开后都能点「设为今日」。英语书页可以收藏单词（词卡右上角的书签标签），再按「全部 / 收藏 · N」筛选，收藏为空时给出引导；每条词也能设为今日。今日选择按账号保存在本机，首页、书架与书页立刻同步，跨天自动回到原有的每日轮换。10 张 2.8 截图位于 [`docs/screenshots/phase16/`](docs/screenshots/phase16/)。全部使用离线 QA 数据，不含账号凭证。

### 2.7 唐诗三百首、高中 3500 词与可隐藏的助手球

13 张 2.7 截图位于 [`docs/screenshots/phase15/`](docs/screenshots/phase15/)。首页截图包含系统状态栏；书架与书页截图直接捕获弹窗内容节点，不含状态栏与遮罩；全部使用离线 QA 数据，不含账号凭证。

### 2.6 学习书架、数学讲义与可拖动交互

12 张 2.6 截图位于 [`docs/screenshots/phase14/`](docs/screenshots/phase14/)。首页截图包含系统状态栏；书架与书页截图直接捕获弹窗内容节点，不含状态栏与遮罩；全部使用离线 QA 数据，不含账号凭证。

### 2.5 Orbit Learn：AI 助手与学习资料

9 张 2.5 截图位于 [`docs/screenshots/phase13/`](docs/screenshots/phase13/)，全部来自 Pixel 7 Pro / API 37 模拟器的真实截屏；其中第 9 张是英语列表（`08b-english-list.png`）。AI 对话与额度是登录临时测试账号后对已部署云函数的真实请求，不包含真实账号资料或任何令牌。

### 2.4 AI 学习规划与周复盘

5 张 2.4 截图位于 [`docs/screenshots/phase12/`](docs/screenshots/phase12/)，来自 Pixel 7 Pro / API 37 模拟器的 Compose 根节点；截图使用固定演示数据，不包含真实账号或 AI 令牌。

### 2.3 文案收尾与 uniCloud 客户端

22 张 2.3 截图位于 [`docs/screenshots/phase11/`](docs/screenshots/phase11/)，其中 20 张是深色 / 晨光两套主题下的五标签回归图，另 2 张为登录页与关于页专项；全部来自 Pixel 7 Pro / API 37 模拟器真实截屏。2.3 默认 APK 的 Base URL 为 `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`。

### 2.2 强制登录、昵称与星轨完成

13 张 2.2 截图位于 [`docs/screenshots/phase10/`](docs/screenshots/phase10/)，全部来自 Pixel 7 Pro / API 37 模拟器真实截屏，包含独立登录页、系统权限弹窗、昵称招呼、拖入核心完成、退出与重新登录恢复，以及撤销 `ACCESS_LOCAL_NETWORK` 后经 Cloudflare 公网 HTTPS 登录、首页同步恢复；账号 `nova_1027` 的昵称、头像与任务均已写入 MySQL。

### 2.1 账号、云同步与头像

> 这一组记录 2.1 时期的「我的页内登录」；2.2 已把入口换成独立登录页，界面与截图保留作版本对照。

### 2.0 五个标签

完整 20 张截图位于 [`docs/screenshots/phase8/`](docs/screenshots/phase8/)，覆盖五个标签、目标详情、习惯热力图、归档、设置与深色 / 浅色双主题；另有 3 张首页专项评审图（`v2-*.png`）：焦点卡深色 / 浅色，以及首页下半屏的任务与习惯衔接。

2.1 的账号、登录注册、Android 17 本地网络权限和云端同步首页截图位于 [`docs/screenshots/phase9/`](docs/screenshots/phase9/)，均为模拟器真实截屏，包含系统权限弹窗；[`05-account-avatar-camera.png`](docs/screenshots/phase9/05-account-avatar-camera.png) 是相机拍照上传后的头像效果，`01` 使用相册选择的星球头像。

## 产品结构

| 标签 | 角色 | 主要能力 |
| --- | --- | --- |
| 星轨 | 今天的巡检入口 | 双环星图、节点详情、拖动换槽与换环、拖入核心完成、今日学习三行预览与学习习惯 |
| 目标 | 长期方向 + 执行面 | 「目标 / 任务 / 习惯」三个分段；目标星系、阶段拆解、任务筛选与编辑、每日打卡、热力图、归档 |
| 学习 | 完整的学习面 | 今日古诗 / 单词 / 数学全卡、翻卡背单词与今日目标、三本书的书架与书页 |
| 成长 | 如实记账 | 经验与等级、任务统计、习惯一致性、目标进度 |
| 我的 | 舱内设置 | 账号资料、昵称与头像、云同步状态、主题与强调色、数据概览、数据与隐私、关于 |

一句话概括它们的关系：任务是原子操作，目标把任务组织成长期方向，习惯负责日复一日的节奏，学习页提供每天的输入，成长只负责把已经发生的事记下来。

## 功能

### 星轨首页

- 今日日期、问候语与「计划 / 已完成 / 待完成」概览；问候语读取账号昵称，未设置时回退用户名；日期跨天或从后台回前台时自动刷新。
- 自绘 Canvas 双环星图：12 槽位、15° 错位、最多显示 9 个节点；待完成在内圈，已完成在外圈。
- 节点位置稳定：新增或删除任务不会让无关节点重新洗牌；超过 9 项时给出溢出提示并引导去目标页的任务分段。
- 点击节点打开浮动聚焦面板，可完成 / 重新打开、编辑或关闭；节点带 56dp 命中区域，视觉直径仍是 42dp。
- 长按拖动节点：自由角度 + 半径跟随手指，吸附到最近空槽位并带震动反馈；拖动只改变展示位置，不写数据库。2.6 起释放点越过两环中点（0.32 倍轨道宽度）会落到另一条环：目标环出现虚线高亮、跨环时补一次触感，手动选择的环优先于「待完成在内、已完成在外」的默认规则，并随本次进程保留。
- 拖入中心核心（半径 20%）直接完成今天的任务：节点缩小让位、核心脉冲、粒子迸发、星座连线与一次性彗星串联成一次完成动画；已完成节点不能再次拖入，拖动半径被限制在轨道外缘 46%。
- 今日习惯卡片显示打卡比例、连续天数与快捷打卡入口，可跳转目标页的习惯分段管理全部习惯；习惯区标题为「学习习惯」，下方固定一张「每日一句」卡片，24 条名句按天轮换、可点「换一句」顺序切换。
- 2.9 起「今日学习」收成三行紧凑预览：今日古诗、今日单词与今日数学各一行，只显示标题与一行摘要；点任意一行或「进入学习 →」都会打开学习页顶部，完整诗正文、单词释义与例句、数学公式以及背单词入口都在那里，首页不再需要一路划到底。
- 右下角 58dp 圆形荧光黄 FAB 是 AI 助手的常驻入口；2.6 起可在内容区内自由拖动，位置会被记住，四周留 18dp，轻触仍打开助手。2.7 起离开默认位置后自动降到 0.55 透明度，贴近屏幕边缘再降到 0.32，按住或拖动时恢复常亮，松手后按新位置重新判断。
- 空轨道、加载失败、今日无任务、全部完成都有独立文案与操作。

### 目标（2.9 起合并规划）

目标页顶部是「目标 / 任务 / 习惯」三段控件，默认落在目标；分段状态在 Activity 重建后保留，再次点击底部「目标」标签会回到目标分段。原来的规划页不再是独立标签，任务与习惯在这里继续工作。

**目标**

- **目标星系**：进行中（含暂停）的目标以自绘 Canvas 星系呈现，最多 7 颗；节点显示标题缩写、进度百分比与进度环，暂停目标降低透明度；点击进入详情，溢出目标留在下方列表。
- **目标列表**：全部目标按状态分组，再按排序与创建时间排列，展示进度、阶段完成数、关联任务完成数、目标日期与状态。
- **新建 / 编辑**：标题、描述、色调、状态与目标日期；目标日期只做提示，不参与进度计算。
- **目标详情**：阶段列表、阶段内任务、目标级任务；支持添加 / 编辑 / 删除阶段，给阶段或目标添加任务，直接完成任务。
- **阶段进度**：手动完成优先；否则按关联任务完成率计算；没有任务时为 0。
- **目标进度**：所有阶段进度的平均值；没有阶段时按目标级关联任务完成率计算；目标级任务不计入阶段平均。
- **状态流转**：进行中、暂停、达成、归档之间通过显式操作切换；达成时追加一次经验，重新激活不会扣回。
- **删除语义**：删除目标会级联删除阶段，但保留任务并解除 `goalId` / `stageId` 关联；删除阶段同样保留任务，只解除阶段关联。删除确认框会给出受影响的阶段数与任务数。
- **AI 规划入口**：目标页头部下方有带 `AI` 标记的入口卡，进入独立的 AI 规划路由；返回键回到目标页。

**任务**

- 全部 / 待完成 / 已完成三档筛选；待完成按日期、优先级、创建时间排序，已完成按完成时间倒序。
- 新建、编辑、删除；标题最多 80 字、描述最多 280 字，空白标题会被拦下。
- 完成状态原子更新：完成时记录当前时间，重新打开时清空完成时间；快速连点有在途去重。
- 可选关联目标与阶段；选择阶段时目标自动一致，已关联的归档目标仍会在编辑器中可见。
- 卡片显示「目标 · …」「阶段 · …」标签；保存、删除、状态切换都有 Snackbar 反馈。
- 空列表、加载失败、长标题和窄屏都有专门布局。

**习惯**

- 每日打卡：左侧圆环切换今日状态；同一天只有一条记录，由唯一索引兜底，重复点击不会重复发放经验。
- 每张习惯卡展示连续天数、近 7 天圆点、近 12 周热力图、坚持率与累计打卡数。
- 连续天数规则：今天已打卡则从今天起算；今天尚未打卡但昨天已打卡时，保留到昨天为止，不立即断签。
- 12 周热力图按周一开始的固定网格绘制，今天与未来日期有不同视觉状态。
- 支持新建、编辑名称 / 备注 / 色调、归档、恢复与删除；归档保留全部历史，归档习惯不能继续打卡；删除习惯会级联删除打卡记录。
- 习惯分段顶部汇总今日打卡数量与完成比例；首页习惯卡片与其共享同一份状态。
- 只支持每日习惯，不提供每周次数、提醒或补卡日历。

### 成长

- **成长总览**：等级环、总经验、今日新增、本周新增、距下一级所需经验与等级进度条。
- **任务统计**：今日完成率、任务总览、近 7 天柱状图、分类分布、连续达成；统计口径见「数据与计算规则」。
- **习惯一致性**：今日打卡、7 日达标率、最长连续；今日与 7 日只统计进行中的习惯，归档习惯只保留历史；没有进行中习惯时展示单独空态。
- **目标进度**：进行中目标数、累计完成阶段数、进行中目标平均进度；没有进行中目标时展示单独空态。
- **空态判断**：只有任务、目标、习惯与经验记录全部为空时才显示整页空态；仅有习惯数据时，成长页仍展示等级、任务统计的 0 值与习惯一致性，不会误判为「没有数据」。
- 加载失败时提供重试，已保存的数据不会被清空。

### AI 学习规划

- 目标页进入独立的 AI 规划路由：目标最多 60 字，目标日期默认今天 + 56 天（可选到 +730 天），每日时长 10–480 分钟、默认 45，可填当前水平与薄弱点（各 200 字）。
- 生成期间显示等待卡；结果按阶段与任务预览，可按任务勾选或整段全选 / 全不选，取消勾选的任务不会写库。
- 导入在单个 Room 事务里写目标、阶段与任务：分类固定 `STUDY`，优先级来自模型（`HIGH / MEDIUM / LOW`），日期夹在今天到 +730 天，空阶段 / 空任务丢弃；导入成功后跳到目标详情页。
- AI 只生成一次草稿，导入后就是普通目标与任务，可改名、挪阶段、完成或删除；不写成长流水，避免导入即刷 XP。
- 未配置、未登录、今日额度用完、超时、上游繁忙、输出不合法分别有独立文案；失败时保留上一份预览与勾选状态。
- 每账号每北京时间自然日可生成 6 次，额度在调用模型前扣减；表单上方显示今日剩余次数。

### AI 周复盘

- 成长页「目标进度」之后是 `AI REVIEW` 卡：无缓存时展示说明、隐私提示与生成按钮；有缓存时展示标题、总结、做得好的、需要留意、下周建议和生成时间。
- 只上报聚合数据：本周计划 / 完成数、学习任务数、习惯可能天数 / 打卡数、XP、最多 8 个进行中目标的标题与进度；不上传任务标题与备注。
- 结果按「用户 + 周一」缓存在本机 `SharedPreferences`，退出登录或令牌过期时清除；卡片明确标注「本机保存」。
- 本周没有任何可复盘数据时按钮禁用；未配置、离线、超时、繁忙、额度用尽都有独立状态与重试入口。
- 每账号每日可生成 4 次；只有用户点按钮才会请求，缓存命中不自动刷新。

### AI 学习助手（2.5 / 2.6 可拖动 / 2.7 可隐藏）

- 首页 FAB 打开 `ModalBottomSheet`：顶部显示今日剩余对话次数，下面是欢迎卡、三个快捷动作（生成学习计划 / 生成周复盘 / 学习资料）、能力清单与建议问法；有对话后改为气泡列表，并保留快捷动作行。2.6 起这枚 58dp 助手球可在内容区内拖动，位置用 `rememberSaveable` 保存，四周留 18dp 且默认仍在右下角；拖动放大并加深阴影，松手回落，轻触仍打开对话。2.7 起离开默认角落（12dp 容差）后常驻 0.55 透明度，距任意屏幕边缘 36dp 以内进一步降到 0.32；触摸、拖动或动画期间始终 1.0，默认角落即使贴近边缘也保持常亮，避免主入口一开始就变淡。
- 对话走 `POST /api/ai/chat`，与规划、复盘共用同一个 Coze Bot；可以自由追问，也可以从古诗 / 英语卡片带一句预设问题进来。
- 只发送聚合数字（今日计划 / 完成、待完成数、习惯打卡、XP）与最多 5 个目标标题（每个 ≤60 字）；任务正文、习惯名称、昵称都不进请求。请求体与回复都不落盘，关闭进程即失去对话记录。
- 每账号每北京时间自然日可对话 20 次，额度在调用模型前扣减；`/api/ai/status` 一次返回三类额度。额度用完、未配置、超时、上游繁忙、输出异常分别有独立文案与重试入口。
- 上游失败时保留用户刚发的问题并显示可重试卡片；「清空」只在没有请求在途时可用。长回复到达后列表会自动滚到底部，不会被输入栏截断。
- 这个对话框是「问答 + 快捷入口」，不是流式输出：一次请求返回整段回复，等待期间显示打字气泡。

### 学习（2.9）

- 新增「学习」标签，顶部是完整的今日学习：古诗全文（默认折四行，可展开）、单词释义 / 例句 / 译文 / 记忆提示、数学公式与例题；内容、展开与 AI 讲解都和书架里的同名卡片一致。
- 背单词卡固定在学习页今日学习之后：今日已学 / 累计词数 / 待复习三个数字；数量有 5 / 10 / 20 / 30 四个预设，也可用步进器在 1–50 之间调整（默认 10），选择按账号记住。
- 「开始学习」按自动选词开局：先没学过的词，再上次答错的词，再最久没学的词；同一优先级里保持词库顺序，每轮最多 50。
- 「自己选词」打开词库选择器：可按单词或释义搜索，点选 / 取消并显示「已选 N / 50」，超过 50 后不再接受；开始后按书序出词。
- 一轮流程：卡片先只给单词与音标，点「显示释义」翻面后才出现释义、例句、译文与记忆提示，此时才可选「认识 / 不认识」。没翻面不能作答，避免先看答案再自评。
- 结算页给出本轮正确率、认识与不认识数量；有错词时「重练错词 N」只重练错误项，也可以「下一轮」或「完成」。
- 进度只记录每个词的最后一次作答：认识 / 不认识次数、作答自然日与最近结果。「今日已学」按最近一次作答落在今天的唯一词数计算，同一词重练不会重复计数；达到目标数后学习卡标出「今日目标已完成」。
- 背单词记录按账号保存在 `orbit_learning` 的 `word_study.<owner>`（Gson JSON），进程重启、Activity 重建与切回账号都会恢复；不写 Room、不上云，也不发经验，成长页数字不受影响。
- 「待复习」是最近一次答「不认识」的词数，只用来决定下一轮选词顺序；不做间隔重复、拼写题、选择题或发音。

### 学习资料（2.6 – 2.9）

- 资料从两列卡片改为**书架 + 书页**：三本书分别是唐诗三百首（309 篇）、高中 3500 词（去重后 3,877 条）与数学讲义（18 个知识点）。书架底部用一行 TODAY 卡串起今天的三样内容。
- 2.9 起首页只剩三行预览，完整的古诗 / 英语 / 数学卡都在学习页：顶部依次是今日古诗、今日单词与今日数学，再往下是背单词卡与三本书的书架；首页点任意一行都会打开学习页顶部。
- 古诗卡展开前只显示前 4 行并在末尾省略（避免《长恨歌》这类长诗铺满整屏），展开后显示全文、注释与赏析；英语卡展开显示中文释义、例句（斜体）、译文与记忆提示，缺少例句时给出空态提示与 AI 入口；数学卡显示等宽公式、摘要，展开后补充例题、答案与易错提醒。展开一次只保留一张；切换书籍或年级会回到列表顶部。
- 两份词库以 `app/src/main/resources/learning/poems.json`（1.4 MB）与 `words.json`（0.5 MB）随 APK 打包，首次访问时按需解析，`OrbitApplication` 启动后台线程预热；JVM 测试与设备测试读取同一份资源。词库保留原始词条的短语与变形（因此总数高于 3,500），42 个词条缺音标、3 篇古诗缺注释、11 篇缺赏析时对应区块直接省略而不是留空白标题。
- 数学讲义覆盖初一至高三，每个年级 3 个知识点，年级用横向 chip 选择并以 `rememberSaveable` 保存。从书架打开时自动展开当前年级的今日话题；学习页的今日数学卡也直接展示今日话题。
- 「让 AI 讲讲」把条目原文 / 公式与讲解要求拼成 prompt，关闭资料弹窗并打开助手发送；回答仍受每日额度限制。
- 2.8 给古诗书页加搜索：顶部 46dp 搜索框按空白分词，每个词都要命中诗名、作者、朝代或正文（不区分大小写），结果实时过滤并按「找到 N 首」计数；搜不到时给出空状态与「清空搜索」。搜索词用 `rememberSaveable` 保留，切换书籍再回来不丢。
- 古诗与英语词条都能「设为今日」：按钮在展开后的卡片里，古诗紧跟正文、词条在 AI 入口之前；当天卡片标出「今日」，重复点击不会重复写入。数学仍走年级与每日轮换。
- 英语书页支持收藏：词卡右上角是 48dp 书签标签，收藏 / 取消都有内容描述与选中态；书页顶部「全部 / 收藏 · N」切换筛选，收藏为空时给出引导。书签不改动词库顺序，筛选结果保持原顺序。
- 今日选择与收藏按账号保存在 `orbit_learning`（`SharedPreferences`），键名带 owner 后缀，未登录回退 `local`；首页三行预览、学习页今日卡、书架 TODAY 卡与书页今日标记读同一份状态。进程重启、Activity 重建和切换回账号都能恢复；跨天时今日选择失效，自动回到 epoch day 轮换。卸载重装或换设备会丢失这些本机偏好，云端账号数据不受影响。
- 今日卡片的选择按 epoch day 轮换，同一天内保持稳定，跨天自动换一条；若当天设过「今日」，则优先使用该选择。
- 2.6 修复真机「滑到底部疯狂跳动」：弹窗禁用整张 sheet 的拖拽，资料卡移除 `animateContentSize`，列表末尾不再与弹窗锚点抢手势；设备测试用相隔 1.2 秒的两帧逐像素相同来验证古诗与英语两本书的底部。
- 学习习惯区下方新增「每日一句」：24 条学习名句按 epoch day 轮换，「换一句」顺序取下一条；习惯加载中或失败时仍然显示。

### 我的

- **强制登录**：应用启动后由 `OrbitRoot` 判定会话；没有会话只渲染独立登录 / 注册页，看不到星轨、学习或设置。用户名 `[A-Za-z0-9_]{3,24}`，密码 6–64 位，密码框内可切换「显示 / 隐藏」（注册的确认密码独立切换），密码在 Spring 以 BCrypt、在 uniCloud 以 scrypt 哈希保存。登录成功先清掉不属于该账号的本机缓存，再阻塞等待云端快照落地，之后才进入主界面。
- **资料**：登录后可修改昵称（1–24 字符），点击铅笔即可就地编辑；首页招呼语立即使用昵称，未设置时回退用户名。可从相册选择图片或调用系统相机拍照作为头像。两者都走 Android 运行时权限弹窗，拒绝后给出说明与「去设置」入口。
- **头像链路**：相册使用系统照片选择器，相机使用 `FileProvider` 输出到应用缓存；图片按 EXIF 方向校正，最长边缩到 768、JPEG 质量 82。Spring 以 multipart 上传，uniCloud 以 JSON base64 上传（解码后 ≤1.4 MB）；服务端校验 JPG / PNG / WEBP，保存后由 Coil 加载；失败时保留首字母占位。
- **同步**：账号卡展示「云端已是最新 / 正在同步 / 离线 / 冲突」和上次同步时间，提供「立即同步」。Room 六张表发生写入后 1.2 秒防抖推送；登录、手动同步和已有会话的冷启动会拉取。
- **归属隔离**：本机 Room 缓存记录 `localOwnerUserId`。首次登录、切换账号或本机缓存属于其他账号时，一律用该账号的云端快照覆盖本机，空快照也会清空；缓存不属于当前账号时不会上传。退出登录立即清会话，再清空 Room 缓存与归属，云端资料保留，重新登录即可恢复。账号卡上的退出登录是整行红色描边按钮，确认后回到登录页，可直接换号。
- **主题**：深空（始终深色）/ 晨光（浅色）/ 跟随系统。
- **强调色**：极光青、星云紫、玫瑰星、日冕金；只作为目标、习惯与图表分类的辅助标记，主按钮和选中态始终使用荧光黄，避免偏好设置把关键操作调成低对比。
- 主题与强调色写入 `SharedPreferences`，进程重启和 Activity 重建后恢复；损坏或未知的枚举值会回退到稳定默认值。账号令牌与用户资料也保存在应用私有 `SharedPreferences`，不写入日志。
- 数据概览：任务、目标、习惯、打卡记录数量，全部来自本机 Room 缓存。
- 「每日轨道提醒」明确标注**暂未接入**：没有虚假开关，不申请 `POST_NOTIFICATIONS`，不创建通知渠道。
- 数据与隐私：任务、目标、习惯、打卡与成长流水以云端账号为准，Room 只做离线缓存；主题与强调色只保存在本机。退出登录或令牌过期会清空本机缓存，云端数据不受影响。
- 关于：应用名、一句项目简介、版本号与构建号。
- 包名 `com.example.orbit`，版本 `2.9.1`（versionCode 12），应用名 Orbit Learn；使用自适应图标：墨黑背景、纸白星球、荧光黄轨道与节点，前景收在 66dp 安全区内，并提供单色主题图标；启动窗口底色 `#101010`。圆形 / 圆角方形 / 方圆遮罩与旧版回退图标见 [`docs/icon-preview.png`](docs/icon-preview.png)。

### 质量与边界

- 系统返回键：在其他标签返回星轨首页；首页有详情时先关闭详情；再返回才离开应用；目标详情先回到目标列表。
- 横滑翻页：五个根页面左右划切换，两端不循环；目标详情与 AI 规划不响应，星轨节点拖拽与助手球拖动优先拿到指针，斜向划动交给列表滚动（判定见 `ui/navigation/TabSwipe.kt`）。
- 后台回前台会刷新「今天」，目标页与成长页共用同一份日期快照。
- 空状态、加载状态、错误状态、删除确认与保存中状态都有独立 UI，不使用「假成功」。
- 主要触控目标不小于 48dp；节点、进度环、统计条形和热力图带语义描述。
- 数据库失败会保留编辑内容或展示可重试错误；迁移没有注册任何破坏性回退。

## 账号与云同步

Android 不直接连接数据库，也不在客户端保存数据库口令。2.2 起云端账号是业务数据的权威来源，UI 只读 Room 缓存，所有入口都先经过账号门禁：

```text
OrbitRoot ── 无会话 ──▶ AuthScreen（独立登录 / 注册页）
    │
    └── 有会话 ──▶ Compose UI → ViewModel → Repository → Room（离线缓存）
                                              ↘ OrbitSyncManager → Retrofit / OkHttp
                                                                    → Spring Boot → JPA / Hibernate → MySQL 8.4
                                                                    （2.3 缺省）uniCloud URL 化 → 云数据库 / 云存储
```

登录流程是「准备缓存 → 保存会话 → 拉取并覆盖本机 → 进入主界面」；拉取失败不阻塞进入，但界面里只会出现该账号自己的行，不会把上一个账号或安装前的数据带进来。

### uniCloud 后端（2.3）

同一套 REST 契约增加了 uniCloud 支付宝云实现：云函数 URL 化、三个云数据库集合、云存储头像。客户端用 Gradle 属性 `orbitApiBaseUrl` 在构建期选择 Spring 或 uniCloud；2.3 起缺省指向控制台实际域名 `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`。云函数 `orbit-api`、`orbit_users` / `orbit_tokens` / `orbit_snapshots` 与 URL 化 PATH `/orbit` 均已部署到 `mint` 空间；health、注册、登录、昵称、快照、409、头像与退出登录已在真实空间用 HTTP 跑通，Android 真机 E2E 待补。支付宝云 URL 化不接受 204，退出登录因此返回 `200 {"status":"ok"}`。uniCloud 下头像改为 JSON base64（≤1.4 MB），Spring 保留 multipart 并新增同路径 JSON 端点。部署与迁移步骤见 [`unicloud/README.md`](unicloud/README.md)，设计细节见 [`docs/ORBIT_2.3_UNICLOUD.md`](docs/ORBIT_2.3_UNICLOUD.md)。

### AI 网关（2.4）

客户端不直接调用 Coze，也不保存任何模型密钥：

```text
AiPlanViewModel / AiReviewViewModel / AiChatViewModel
  → AiRepository（Retrofit，Bearer）
  → GET /api/ai/status · POST /api/ai/plan · POST /api/ai/review · POST /api/ai/chat
  → orbit-api（Node 18，零 npm 依赖）
       ├── 校验输入：目标 60 字、日期 0–730 天、每日 10–480 分钟、消息 ≤600 字
       ├── 配额：每账号每北京时间自然日规划 6 / 复盘 4 / 对话 20，调用前扣减
       ├── 提示词版本：plan-v1 / review-v1 / chat-v1
       ├── Coze v3 Chat：创建 → 轮询 → 拉消息，25 秒预算
       └── JSON 规范化：阶段 1–6、每阶段任务 1–5、总计 20、日期偏移夹取；对话回复去空白并截断
  ← 规范化后的阶段 / 任务、复盘或对话回复
```

- 云端环境变量 `COZE_API_TOKEN` 与 `COZE_BOT_ID` 缺一不可；只配一个或都不配时 `/api/ai/status` 返回 `configured:false`，生成接口返回 503 `AI_NOT_CONFIGURED`。
- 模型输出不是合法 JSON 时，服务端带着错误原因请求一次修复；仍失败返回 502 `AI_INVALID_OUTPUT`，不写库、不返回半成品。
- 配额在模型调用前扣减，失败也计数；这是为了不让重试绕过上限，代价是用户可能「消耗一次但没有结果」，所以界面上明确显示剩余次数。
- 对话请求最多带 8 轮历史（4 问 4 答）；客户端只传 `message + history + 聚合上下文`，服务端不再写第二份聊天记录。对话结束后的额度刷新是 best effort，失败不会把已显示的回复变成错误。
- 周复盘只发送聚合数字与目标标题；对话发送用户消息、最近 8 轮对话、聚合数字与最多 5 个目标标题，任务正文、习惯名称、备注与昵称都不发送；令牌与提示词都不进 APK。搭建步骤、系统提示词与排障表见 [`docs/COZE_SETUP.md`](docs/COZE_SETUP.md)。

### 登录门禁与缓存归属

- `OrbitRoot` 只看两件事：`session == null` 时整屏渲染 `AuthScreen`，`initialSyncing` 时渲染 `SyncBootScreen`；两者之外才创建 `Scaffold` 与五个标签。没有「游客模式」或返回键绕过。
- 登录 / 注册成功后，`AccountViewModel` 先调用 `prepareForAccount(userId)`：在 IO 线程清掉不属于该账号的缓存，再保存会话，然后 `syncNow()` 阻塞到云端快照落地才放行主界面。`Room.clearAllTables()` 不再跑在主线程上，这是一次真实设备崩溃后补的修复，并配有回归测试。
- 退出登录先清会话让门禁立即生效，再异步清空缓存并注销服务端令牌；令牌过期（401）复用同一清理路径。冷启动只凭本地令牌进入，随后后台拉取。
- 仅供仪器测试的 debug 缝隙：`AccountStore.seedDebugSession()` 写入 `qa_offline` 标记，`OrbitSyncManager` 在标记存在时跳过所有网络请求，由自定义 `OrbitTestRunner` 在 `callApplicationOnCreate` 中注入。Release 构建里该函数被 `BuildConfig.DEBUG` 拦下。

### 账号与接口

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 否 | 注册并返回用户与 30 天令牌 |
| POST | `/api/auth/login` | 否 | 校验密码（Spring 为 BCrypt，uniCloud 为 scrypt），签发 30 天令牌 |
| POST | `/api/auth/logout` | Bearer | 删除当前令牌：Spring 204；uniCloud 200 `{"status":"ok"}`（支付宝云拒绝 204） |
| GET / PATCH | `/api/me` | Bearer | 读取或修改昵称 |
| POST | `/api/me/avatar` | Bearer | multipart（Spring，≤5 MB）或 JSON base64（uniCloud，≤1.4 MB），JPG / PNG / WEBP |
| GET | `/api/sync` | Bearer | 拉取完整快照与 revision |
| PUT | `/api/sync` | Bearer | 携带 `baseRevision` 推送；版本不符返回 409 + 当前快照 |
| GET | `/api/ai/status` | Bearer | AI 是否已配置、提示词版本、今日额度与已用次数 |
| POST | `/api/ai/plan` | Bearer | 生成学习计划；服务端规范化为阶段 / 任务与 epoch day |
| POST | `/api/ai/review` | Bearer | 生成周复盘；只接收聚合统计与目标标题 |
| POST | `/api/ai/chat` | Bearer | 自由对话；接收 ≤600 字消息、≤8 轮历史与聚合上下文，返回整段回复 |

错误体统一为 `{ "code": "...", "message": "..." }`，常见错误码包括 `ACCOUNT_EXISTS`、`LOGIN_FAILED`、`UNAUTHORIZED`、`VALIDATION_FAILED`、`AVATAR_TOO_LARGE`、`AVATAR_TYPE`、`SYNC_REVISION`、`SYNC_TOO_LARGE`，AI 侧另见 `AI_NOT_CONFIGURED`、`AI_INPUT_INVALID`、`AI_NO_DATA`、`AI_QUOTA_EXCEEDED`、`AI_TIMEOUT`、`AI_BUSY`、`AI_INVALID_OUTPUT`。完整 OpenAPI 3.0 文档见 [`docs/openapi-orbit.yaml`](docs/openapi-orbit.yaml)；详细协议、MySQL 表结构与排障步骤见 [`docs/ORBIT_2.1_ACCOUNTS.md`](docs/ORBIT_2.1_ACCOUNTS.md)，2.2 的登录门禁、缓存归属与星轨完成见 [`docs/ORBIT_2.2_ACCOUNT_GATE.md`](docs/ORBIT_2.2_ACCOUNT_GATE.md)，2.3 的 uniCloud 云函数、部署与迁移见 [`docs/ORBIT_2.3_UNICLOUD.md`](docs/ORBIT_2.3_UNICLOUD.md)，2.4 的 AI 网关、配额与失败语义见 [`docs/ORBIT_2.4_AI.md`](docs/ORBIT_2.4_AI.md)。

### 快照同步语义

- 一次快照包含目标、阶段、任务、习惯、打卡与成长流水六类数据；服务端用 `(user_id, client_id)` 保存，`client_id` 是 Room 原主键，账号之间不会互相覆盖。
- 登录、手动同步与已有会话的冷启动执行拉取；Room 六张表提交写入后标记 `local_dirty`，1.2 秒防抖后推送。应用远端快照时会打开 1.5 秒的 invalidation 抑制窗口，避免回环推送。
- 服务端权威：`baseRevision` 与当前 revision 不一致时返回 409 与云端快照，客户端用云端覆盖本机并提示「云端已有更新，已采用云端版本」。
- 首次登录、切换账号或本机归属为空时，一律用云端快照覆盖本机，即使云端为空也清空缓存；只有 `owner == 当前账号` 且 `local_dirty` 时才会把本机修改推上去。安装后、登录前的本地数据会被有意丢弃，云端快照才是权威。
- 退出登录立即清会话并回到登录页，随后清空 Room 缓存与归属（服务端令牌异步注销）；重新登录只靠 GET `/api/sync` 恢复全部数据。401 走同一套清理并提示重新登录；网络失败保留会话，同步状态转为离线并显示上次成功时间。
- 整包快照与单类 5000 行上限意味着它适合个人多设备低频切换，不是实时协同，也没有字段级合并或离线修改保留。

### 系统权限

| 权限 | 申请时机 | 说明 |
| --- | --- | --- |
| `INTERNET` | 安装时 | 正常权限 |
| `ACCESS_LOCAL_NETWORK` | 仅当 Base URL 指向局域网 / 本机地址时，登录 / 注册提交前、手动同步前 | Android 17（API 37）起访问局域网必须动态申请，属于「附近的设备」组；拒绝时提示「需要“附近的设备”权限才能连接云端服务」。公网 HTTPS 地址（2.3 的 uniCloud 域名、2.2 的 Cloudflare 隧道）会跳过该权限 |
| `CAMERA` | 点击「拍照上传」且未授权 | 运行时弹窗；拒绝后展示提示与「去设置」 |
| `READ_MEDIA_IMAGES` | 从相册选择且未授权 | Android 13+ |
| `READ_MEDIA_VISUAL_USER_SELECTED` | 同上，Android 14+ | 支持「仅选择部分照片」；部分授权也继续打开选择器 |
| `READ_EXTERNAL_STORAGE` | 同上，Android 12 及以下 | `maxSdkVersion=32` |

> `ACCESS_LOCAL_NETWORK` 是本阶段最容易踩的坑：只声明清单或只在 shell 中放行 appop 都不够。若没有在登录时接受系统弹窗，OkHttp 访问 `10.0.2.2:8080` 会以 `SocketTimeoutException` 静默失败。接收权限弹窗后重新提交即可；清空应用数据会重置授权。2.2 公网版把 `BuildConfig.ORBIT_API_BASE_URL` 设为 HTTPS 隧道域名，2.3 缺省设为 uniCloud 公网域名；`hasLocalNetworkAccess` 通过 `requiresLocalNetworkPermission(url)` 判断目标：公网域名 / 公网 IP 不再申请该权限，模拟器与局域网地址仍按原逻辑弹窗。

### 安全边界

密码在 Spring 使用 BCrypt、在 uniCloud 使用 scrypt 哈希；令牌是 30 天有效的不透明字符串。Spring 只在 `auth_tokens` 中保存原文并通过 Bearer 头校验，是本机开发配置：HTTP 明文、开发数据库口令、头像保存在后端磁盘。uniCloud 版改为只存 SHA-256 令牌哈希、头像放云存储，并已在真实入口验证注册、登录、快照与头像链路。AI 的 Coze PAT 只保存在云函数环境变量，客户端、仓库与日志都不应出现；如果泄露，应在 Coze 控制台吊销后重建。

## 数据与计算规则

### 数据模型

Room v2 共六张表：

| 表 | 作用 | 关键约束 |
| --- | --- | --- |
| `tasks` | 任务 | `goalId` → `goals`、`stageId` → `goal_stages`，均 `ON DELETE SET NULL`；完成状态、截止日期、目标与阶段索引 |
| `goals` | 目标 | 状态、目标日期索引；排序字段 |
| `goal_stages` | 目标阶段 | `goalId` 外键 `ON DELETE CASCADE`；`(goalId, position)` 索引 |
| `habits` | 习惯 | `archivedAt` 索引；归档保留历史 |
| `habit_records` | 每日打卡 | `habitId` 外键 `ON DELETE CASCADE`；`(habitId, epochDay)` 唯一索引 |
| `growth_events` | 经验流水 | `sourceKey` 唯一索引；追加式，不更新、不删除 |

日期字段分两类：`dueDate`、`epochDay` 是本地自然日序号（与时区无关的日历日）；`createdAt`、`completedAt`、`checkedAt`、`occurredAt` 是 epoch 毫秒时间戳。UI 只使用前者判断「哪一天」，用后者展示与排序具体时刻。

### 目标与阶段

- 阶段进度：手动标记完成 = 1；否则若有关联任务，取已完成任务数 / 任务总数；没有任务 = 0。
- 目标进度：有阶段时取全部阶段进度的算术平均；没有阶段时取目标级关联任务完成率；两者都没有 = 0。
- 目标级任务（只挂在目标、不挂在阶段）不计入阶段平均。
- 进行中目标集合 = `ACTIVE` + `PAUSED`；星图、成长页平均进度都使用这个集合。
- 目标日期、阶段日期只做提示，不参与任何进度计算。

### 习惯

- 每日一次：`(habitId, epochDay)` 唯一；未来日期不能打卡；归档习惯不能打卡。
- 连续天数：今天已打卡则从今天回溯；今天未打卡但昨天已打卡时从昨天回溯；否则为 0。
- 最长连续：遍历全部历史打卡日，取最长连续区间，归档习惯也计入。
- 12 周热力图：以 `today` 所在周的周一为最后一周起点，向前固定 12 周，共 84 天；未来日期标记为未来。
- 7 日达标率：只统计进行中习惯；每个习惯的窗口从 `max(创建日, today - 6)` 开始；分母是窗口内「习惯 × 天」的应打卡次数，分子是实际打卡次数。

### 单词学习

- 每轮数量默认 10，夹取在 1–50；预设 5 / 10 / 20 / 30，手动选择同样以 50 为上限。
- 自动选词按「从未学过 → 最近一次答不认识 → 最近一次答认识」三桶依次取，第二、三桶按上次学习日从早到晚，桶内保持词库顺序，按 id 去重。
- 一张卡必须先翻面才能作答；本轮正确率 = 认识数 / 已答数，四舍五入到整数百分比。
- 每个词只保留最后一次作答：累计认识 / 不认识次数、最后作答日与最近结果。今日已学 = 最近一次作答落在今天的唯一词数，重练同一词不重复计数；累计词数 = 有记录的唯一词数；待复习 = 最近一次答不认识的词数。
- 重练错词只取本轮答「不认识」的词重新开一轮；「再来一组」按最新进度重新自动选词。
- 进度写在 `orbit_learning` 的 `word_study.<owner>`，不写 Room、不发经验、不参与云端快照。

### 成长经验与等级

| 来源 | 经验 | sourceKey |
| --- | --- | --- |
| 完成任务 | 10 XP（高优先级 +5） | `task:{id}` |
| 习惯打卡 | 8 XP | `habit:{habitId}:{epochDay}` |
| 完成阶段 | 40 XP | `stage:{id}` |
| 达成目标 | 120 XP | `goal:{id}` |

- 经验是只追加的流水，`INSERT OR IGNORE` 由唯一 `sourceKey` 保证同一成就只发一次；重复完成、重新打卡不会重复发放。
- 取消完成、撤销打卡、重新打开阶段不会扣回经验；等级因此只升不降。这是刻意的产品决策，不是漏算。
- 等级门槛：LV.1 从 0 XP 开始；LV.L（L ≥ 2）从 `50 × L × (L - 1)` XP 开始；最高 LV.99。
- 今日新增与本周新增按经验事件的 `epochDay` 统计；「本周」从周一开始，与「近 7 天」滚动窗口不是同一个口径。

### 任务统计口径

- 今日完成率：分母是截止日期为今天的任务；分子要求任务已完成且完成时间落在今天。
- 任务总览与分类分布：按当前状态统计全部任务，不受任务列表筛选影响。
- 近 7 天：按完成时间所属的本地自然日统计完成数量，包含 0 完成的日期；柱高代表数量，不是完成率。
- 连续达成：每天至少完成 1 项即可延续；今天尚未完成时，保留到昨天为止的记录。
- 筛选只影响任务列表，不影响统计。

上述规则分别落在无 Android 依赖的纯 Kotlin 函数中：`TaskMetrics`、`TaskStatistics`、`GoalMetrics`、`HabitMetrics`、`GrowthRules`、`GrowthInsights`、`WordStudyPlanner`、`WordStudySession`、`WordStudyProgress`；Composable 只做展示。

## 视觉系统

- **瑞士运动科技 × 秩序感**：主色只有墨黑 `#101010`、纸白 `#F5F5F2` 与荧光黄 `#D9FF00`；层级交给中性灰 `#D8D8D4` / `#242424` / `#777777`，浅蓝 `#A9C5FF` 与浅紫 `#C8B7FF` 只作为辅助数据色。黄色只出现在主按钮、选中态、关键数字与当前进度上，不铺满界面。
- **纯色块 + 发丝描边**：卡片是实色填充加 1dp 细描边，没有大面积渐变、外发光、投影堆叠或半透明玻璃；信息层级靠反色、字重、留白与数字大小建立。两种主题使用同一套对比逻辑——深色是墨黑底 + 石墨卡，浅色是纸白底 + 白卡，重点模块在两套主题下都反向着色。
- **8dp 网格与连续圆角**：`OrbitSpacing`（4 / 8 / 12 / 16 / 24 / 32 / 40dp）与 `OrbitCorners`（卡片 24、内层 16、控件 14、胶囊 100）统一间距与圆角；页面左右留白 24dp，正文最大宽度 620dp，窄屏与平板都按同一比例收敛。
- **五个标签，各有主角**：首页的双环星轨用黄色进度弧与中心大号百分比，目标页用几何星系与阶段路径（任务与习惯共用同一套卡片节奏），学习页用今日全卡与翻卡背单词，成长页用等级环与柱状图；全部 Canvas 自绘，没有把页面做成同一种卡片堆叠。
- **浮动导航与控件**：底部是悬浮胶囊——深色主题下为纸白填充、浅色主题下为墨黑填充；选中项是黄色圆底 + 墨黑线性图标 + 10sp 标签，并保留手势区与安全边距。按钮、分段选择、筛选项与删除对话框共享同一套 14dp 圆角、按压缩放和 48dp 触控目标。

## 技术栈

| 层 | 选型 |
| --- | --- |
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3（Compose BOM 2026.02.01） |
| 导航 | Navigation Compose 2.10.2；五个标签 + `goals/{goalId}` 详情路由 |
| 状态 | ViewModel + StateFlow + `collectAsStateWithLifecycle` |
| 持久化 | Room 2.8.5 + KSP，数据库 v2；外观偏好与账号令牌用 SharedPreferences |
| 网络 | Retrofit 2.11.0 + Gson、OkHttp 4.12.0；Bearer 鉴权与快照 DTO |
| 图片 | Coil 3.2.0 加载头像；ExifInterface 1.4.1 校正方向，上传前缩放与压缩 |
| 后端 | Spring Boot 4.1.2（JDK 21）、Spring Web、Spring Data JPA / Hibernate 7.4.13、BCrypt；2.3 缺省为 uniCloud 支付宝云函数（Node.js，零 npm 依赖，已部署到 `mint`，URL 化已开启并验证） |
| AI | Coze Bot（v3 非流式 Chat）+ PAT；云函数负责提示词版本、结构化校验、一次修复、25 秒预算与每账号每日配额；客户端不接触令牌 |
| 数据库 | MySQL 8.4.9；账号、令牌、资料、头像路径与六类同步表。uniCloud 版使用 `orbit_users` / `orbit_tokens` / `orbit_snapshots` 三个云数据库集合 |
| 迁移 | 手写 `MIGRATION_1_2`，导出 schema 到 `app/schemas`，`MigrationTestHelper` 验证；服务端 `ddl-auto=update` |
| 自绘 | Compose Canvas + 手势 + `Animatable` / `animate*AsState` |
| 视觉 | 纯色块 + 发丝描边 + 荧光黄强调；`OrbitSpacing` / `OrbitCorners` 统一 8dp 网格与圆角；反色重点模块；无真实模糊、大面积渐变或外发光 |
| 构建 | Gradle 9.6.0 + AGP 9.4.1，compileSdk / targetSdk 37，minSdk 24 |
| 测试 | JUnit 4、Compose UI Test、AndroidX Test、Espresso、Room MigrationTestHelper；后端 JUnit 5 + MockMvc + H2 MySQL 模式；云函数 `node --test` |

> `MigrationTestHelper` 会读取导出的 schema；`room-testing` 的序列化实现要求 debug 运行时使用 `kotlinx-serialization-core 1.8.1`，这一点已在 `app/build.gradle.kts` 显式固定。

## 架构

```text
Compose Screens / Components
        │  只读 StateFlow 派生状态，不直接访问数据库
        ▼
Task / Goal / Habit / Growth / Learning / WordStudy ViewModels
        │                                   ├─ OrbitViewModel（外观偏好）
        │                                   ├─ AccountViewModel（登录 / 资料 / 同步状态）
        │                                   ├─ AiPlanViewModel（表单 / 生成 / 勾选 / 导入）
        │                                   └─ AiReviewViewModel（本周指标 / 缓存 / 生成）
        ▼
Task / Goal / Habit / Growth Repository ── GrowthLedger（追加式经验）
        │                                   ├─ AccountRepository（认证 / 资料 / 头像 / 快照）
        │                                   └─ AiRepository（状态 / 计划 / 复盘）
        │
        ├── Room v2（tasks / goals / goal_stages / habits / habit_records / growth_events）
        │        ▲
        │        └─ OrbitSyncManager（防抖推送 / 拉取 / 冲突覆盖 / 归属隔离）
        │                └─ Retrofit + OkHttp → Spring Boot / JPA → MySQL 8.4
        │                   （2.3 缺省）uniCloud URL 化 → 云数据库 / 云存储
        │                   （2.4）uniCloud → Coze Bot（令牌只存云函数环境变量）
        └── SharedPreferences（主题与强调色 / 令牌与同步 revision / AI 周复盘缓存 /
                               学习今日选择与收藏 / 背单词进度）

纯 Kotlin：TaskMetrics · TaskStatistics · GoalMetrics · HabitMetrics ·
GrowthRules · GrowthInsights · OrbitLayout · AiReviewMetrics ·
AiPlanSelection · AiPlanImport · LearningProgress · LearningBrowse ·
WordStudyPlanner · WordStudySession · WordStudyProgress
```

- 任务、目标、习惯、成长四个业务 ViewModel 与学习、背单词两个学习状态源都是 Activity 级单例，各自只订阅一次数据源，把列表、编辑状态、加载状态和反馈合并为 UiState；目标详情复用 `GoalViewModel`，不另建状态源。学习与背单词只读写本机 `SharedPreferences`，不碰 Room，也不向成长流水写入任何东西。AI 规划与复盘各自持有独立 ViewModel，只在对应路由 / 卡片内创建，不把生成中的状态塞进业务 ViewModel。
- Repository 是唯一写入入口，负责校验、事务、完成时间与不存在实体的显式异常。删除目标依赖外键完成「级联阶段、保留任务」；AI 计划的导入同样走 `GoalRepository` 的单事务，不绕过 Repository 直接写 DAO。
- `GrowthLedger` 被任务、习惯、目标三个 Repository 共享；它只追加经验，不提供更新或删除接口。
- 账号层独立于业务 Repository：`AccountRepository` 只负责 HTTP，`OrbitSyncManager` 观察六张 Room 表并向云端推送整包快照。页面始终读 Room；唯一等待网络的是登录门禁，它必须先让云端快照覆盖缓存才显示主界面。
- `GoalMetrics` / `HabitMetrics` / `GrowthRules` / `GrowthInsights` / `WordStudyPlanner` / `WordStudySession` 都是无 Android 依赖的纯 Kotlin 聚合，可直接在 JVM 上测试。
- `OrbitLayout` 负责首页环层、槽位、吸附、碰撞与命中检测；所有函数无副作用。
- 导航、`Scaffold`、Snackbar 与四类编辑器 BottomSheet 集中在 `OrbitApp`；页面本身不持有持久化职责。
- 2.9 起「目标 / 任务 / 习惯」是同一条路由里的三个分段，不再注册独立目的地；再次点底部「目标」会把分段重置为「目标」。底部标签用稳定的 `testTag` 暴露给测试，避免与同名的分段 / 筛选标签冲突。
- 2.9.1 起横滑翻页拆成两层：`TabSwipe` 是纯 Kotlin（相邻标签顺序、根路由白名单、跟手位移 `followOffset`、橡皮筋限幅、`thresholdPx` / `evaluate` 的距离与横竖比例判定、`shouldClaim` 的手势认领门槛、`settleOutMs` 的去程时长，全部直接在 JVM 上测试），`Modifier.orbitTabSwipe` 只累加指针事件、把位移写进 `Animatable` 并用 `graphicsLayer { translationX }` 平移内容。滑动顺序取 `OrbitDestination.entries`，与 `OrbitBottomBar` 渲染的是同一份列表，因此不会出现滑到的页面和底部栏顺序不一致；触发时复用 `OrbitApp` 里那条 `navigateTo`，`popUpTo / restoreState`、分段重置等语义与点标签完全相同。`AwaitPointerEventScope` 带 `@RestrictsSuspension`，手势里只能挂起等指针事件，因此跟手与两段滑移动画都交给 `rememberCoroutineScope().launch(start = CoroutineStart.UNDISPATCHED)`，在同一帧内落地；`TabSwipeTransition.swapping` 只在这段滑移里为真，`OrbitApp` 据此把 `NavHost` 的四个转场换成 `EnterTransition.None` / `ExitTransition.None`，点底部栏时仍走原来的淡入淡出。手势归属靠两趟 pass 分工：星轨节点拖拽、阶段进度条、助手球、横向书架这些子控件在 Main pass 先拿到指针，只要在 `CLAIM_SLOP`（24dp）内消费掉横向位移就仍然归它们管，父层随即停手弹回；反过来，卡片的 `clickable` 只按「指针离开我多远」取消点击，而整层平移会让卡片相对指针静止，所以超过 24dp 后父层改到 Initial pass（父先于子）消费事件，卡片据此放弃点击，松手也不补，24dp 之内一律放行不影响普通点按。
- 分段控件 `PlannerSegment` 用 `rememberSaveable` 恢复（目标页默认「目标」）；标签页切换使用 `saveState / restoreState`，会保留各页滚动位置。

## 目录结构

```text
Orbit/
├── app/src/main/java/com/example/orbit/
│   ├── MainActivity.kt
│   ├── OrbitApplication.kt         # 账号图初始化与同步管理器启动
│   ├── account/                    # 账号模型 / Retrofit / AccountStore / 头像处理 / 同步管理器
│   ├── ai/                         # AI DTO / 错误映射 / Retrofit / 勾选 / 导入 / 周复盘指标与缓存
│   ├── learning/                   # 学习资料模型、JSON 词库加载、每日轮换、搜索 / 今日选择与 AI prompt
│   │   ├── LearningProgress.kt     # 2.8 搜索 / 收藏筛选 / 今日选择的纯逻辑
│   │   └── WordStudy.kt            # 2.9 选词、翻卡会话与作答记录的纯逻辑
│   ├── data/
│   │   ├── local/                  # Room v2：实体 / DAO / 数据库 / 迁移 / 转换器
│   │   ├── TaskRepository.kt
│   │   ├── GoalRepository.kt
│   │   ├── HabitRepository.kt
│   │   ├── GrowthRepository.kt
│   │   ├── GrowthLedger.kt
│   │   ├── LearningProgressStore.kt # 2.8/2.9 今日选择、收藏与背单词进度（按账号，本机）
│   │   └── OrbitPreferencesStore.kt
│   ├── model/                      # Task / Goal / Habit / Growth 及纯函数聚合
│   └── ui/
│       ├── components/             # 瑞士设计令牌、星轨、星系、习惯与目标组件、浮动导航
│       ├── navigation/             # OrbitDestination（五个标签 + 目标详情）+ TabSwipe（横滑顺序与阈值）
│       ├── orbit/OrbitLayout.kt    # 纯几何：槽位、吸附、碰撞、命中
│       ├── screens/                # 首页 / 目标（三段）/ 目标详情 / 学习 / 背单词 / 成长 / 我的 / 编辑器
│       ├── theme/                  # 深空 + 晨光 + 四套强调色
│       ├── AccountViewModel.kt     # 登录、昵称、头像、权限提示与同步状态
│       ├── AiPlanViewModel.kt      # AI 规划表单 / 生成 / 预览 / 导入
│       ├── AiReviewViewModel.kt    # AI 周复盘指标 / 缓存 / 生成
│       ├── LearningViewModel.kt    # 2.8 今日选择 / 收藏状态与按账号切换
│       ├── WordStudyViewModel.kt   # 2.9 选词 / 一轮会话 / 进度与按账号切换
│       ├── OrbitApp.kt
│       ├── PlannerSegment.kt
│       └── *ViewModel.kt
├── app/src/main/resources/learning/ # 唐诗三百首 309 篇 / 高中 3500 词 3,877 条（JSON）
├── app/schemas/                    # Room 导出的 v1 / v2 schema（迁移测试输入）
├── app/src/test/                   # JVM 测试（账号与同步规则 + 既有领域测试）
├── app/src/androidTest/            # 设备 / Activity 测试（含截图生成与迁移测试）
├── server/                         # Spring Boot 4.1.2 + MySQL 后端
│   ├── src/main/java/com/example/orbit/server/
│   │   ├── auth/                   # 注册、登录、令牌拦截器、资料、头像
│   │   ├── sync/                   # 六类快照实体、revision 与 409 冲突
│   │   └── common/                 # 统一错误体与异常处理
│   ├── src/test/                   # 认证 / 资料 / 头像 + 快照冲突（H2 MySQL 模式）
│   └── data/avatars/               # 本机头像存储（运行后生成）
├── unicloud/                       # 2.3 云函数（URL 化）、部署说明与迁移脚本
│   ├── cloudfunctions/orbit-api/   # Node.js 云函数、AI 网关与 55 项 node --test
│   ├── tools/migrate.mjs           # MySQL 快照迁移 CLI
│   └── tools/check-ai.mjs          # AI 配置与真实调用检查脚本
├── docs/ORBIT_2.0_PLAN.md          # Orbit 2.0 规划与验收标准
├── docs/ORBIT_2.1_ACCOUNTS.md      # 2.1 账号、同步、权限与启动说明
├── docs/ORBIT_2.2_ACCOUNT_GATE.md  # 2.2 强制登录、星轨完成与验证记录
├── docs/ORBIT_2.3_UNICLOUD.md      # 2.3 uniCloud 后端设计、部署进度与验证记录
├── docs/ORBIT_2.4_AI.md            # 2.4 AI 网关、规划 / 复盘、配额与失败语义
├── docs/ORBIT_2.4_JOB_PACKAGING.md # 2.4 岗位包装、演示脚本与面试问答
├── docs/ORBIT_2.5_LEARNING.md      # 2.5 AI 对话、学习资料与隐私边界
├── docs/ORBIT_2.6_LIBRARY.md       # 2.6 学习书架、数学讲义、可拖动交互与验证记录
├── docs/ORBIT_2.7_LIBRARY.md       # 2.7 唐诗三百首 / 3500 词接入、助手球半透明与验证记录
├── docs/ORBIT_2.8_LIBRARY.md       # 2.8 古诗搜索、每日挑选、单词收藏与本机持久化
├── docs/ORBIT_2.9_LEARNING.md      # 2.9 目标三段合并、学习页、紧凑首页与翻卡背单词
├── docs/COZE_SETUP.md              # Coze Bot / PAT / uniCloud 环境变量搭建手册
├── docs/openapi-orbit.yaml         # 账号、同步、AI 与媒体接口的 OpenAPI 3.0 文档
├── docs/icon-preview.png           # 启动图标遮罩与回退图标预览
├── docs/screenshots/phase18/       # 2.9.1 横滑翻页过程截图（含跟手中途帧）
├── docs/screenshots/phase17/       # 2.9 目标合并 / 学习页 / 背单词截图
├── docs/screenshots/phase16/       # 2.8 古诗搜索 / 今日挑选 / 单词收藏截图
├── docs/screenshots/phase15/       # 2.7 三百首 / 3500 词 / 半透明助手球截图
├── docs/screenshots/phase14/       # 2.6 书架 / 数学 / 每日一句 / 可拖动截图
├── docs/screenshots/phase13/       # 2.5 AI 助手与学习资料截图
├── docs/screenshots/phase12/       # 2.4 AI 规划与周复盘截图
├── docs/screenshots/phase11/       # 2.3 文案收尾与 uniCloud 客户端截图
├── docs/screenshots/phase10/       # 2.2 登录门禁 / 昵称 / 权限 / 星轨完成截图
├── docs/screenshots/phase9/        # 2.1 账号与云同步截图
├── docs/screenshots/phase8/        # 2.0 五标签最终截图（20 张 + 3 张首页评审）
├── artifacts/orbit-2.9.1-tab-swipe-debug.apk    # 2.9.1 跟手横滑翻页 Debug 包
├── artifacts/orbit-2.9.0-orbit-learn-debug.apk  # 2.9.0 目标合并 / 学习页 / 背单词 Debug 包
├── artifacts/orbit-2.8.0-orbit-learn-debug.apk  # 2.8.0 古诗搜索 / 今日挑选 / 单词收藏 Debug 包
├── artifacts/orbit-2.7.0-orbit-learn-debug.apk  # 2.7.0 三百首 / 3500 词 / 半透明助手球 Debug 包
├── artifacts/orbit-2.6.0-orbit-learn-debug.apk  # 2.6.0 学习书架 / 数学 / 可拖动 Debug 包
├── artifacts/orbit-2.5.0-orbit-learn-debug.apk  # 2.5.0 Orbit Learn Debug 包
├── artifacts/orbit-2.4.0-ai-debug.apk  # 2.4.0 AI 客户端 Debug 包
├── artifacts/orbit-2.4.0-job-pack.zip # 2.4.0 岗位材料包（APK + 文档 + 截图）
├── artifacts/phase18/              # 2.9.1 横滑翻页截图与测试日志
├── artifacts/phase17/              # 2.9 目标合并 / 学习页 / 背单词截图与测试日志
├── artifacts/phase16/              # 2.8 古诗搜索 / 今日挑选 / 单词收藏截图与测试日志
├── artifacts/phase15/              # 2.7 三百首 / 3500 词 / 半透明助手球截图与测试日志
├── artifacts/phase14/              # 2.6 书架 / 数学 / 可拖动截图与设备测试日志
├── artifacts/phase13/              # 2.5 AI 助手与学习资料截图
├── artifacts/phase12/              # 2.4 AI 截图与全量设备测试日志
├── artifacts/orbit-2.3.0-unicloud-debug.apk  # 2.3.0 uniCloud 客户端 Debug 包
├── artifacts/phase10/              # 2.2 Debug APK 与过程截图
├── artifacts/phase9/               # 2.1 Debug APK 交付副本
└── README.md
```

## 数据库迁移

`MIGRATION_1_2` 是手写迁移，数据库没有注册 `fallbackToDestructiveMigration`：

1. 新建 `goals`、`goal_stages`、`habits`、`habit_records`、`growth_events` 五张表与全部索引。
2. 新建带 `goalId` / `stageId` 外键与索引的 `tasks_new`，从旧 `tasks` 逐列复制数据，两个关联列写入 `NULL`。
3. 删除旧 `tasks`、重命名 `tasks_new`，重建四个任务索引。
4. Room 校验 identity hash 后把 `user_version` 升到 2；任何一步失败都会抛出异常，原数据库文件保留，而不是清空用户数据。
5. 迁移不回填历史成长事件：v1 中已经完成的任务、阶段不会补发经验，经验从升级后的下一次达成开始记录。

验证分两层：

- `OrbitMigrationTest` 使用 `MigrationTestHelper` 读取 `app/schemas` 的 v1 schema，创建数据库、写入任务后迁移到 v2，校验表结构、索引、数据与身份哈希。
- 设备上另做了一次真实升级：用 v1 schema 生成真实的 `orbit.db`（`user_version = 1`，三条任务），安装到 v2 应用的数据目录后启动。升级后确认 `user_version = 2`、六张表齐全、三条任务的标题 / 描述 / 优先级 / 分类 / 截止日期 / 完成时间原样保留、`goalId` / `stageId` 为 `NULL`。

**验证缺口**：本机没有归档的 v1 APK，因此没有重放「先安装 v1 APK，再覆盖安装 v2 APK」的完整路径；迁移用真实 v1 数据库 + `MigrationTestHelper` 验证，覆盖了 schema 与数据，但不覆盖安装器层行为。

### 服务端 MySQL

服务端与 Room 迁移相互独立。MySQL 库 `orbit` 由 Hibernate `ddl-auto=update` 创建和增列，不删除已有数据：

| 表 | 内容 |
| --- | --- |
| `app_users` | 用户名（唯一）、BCrypt 哈希、昵称、头像路径、创建 / 更新时间 |
| `auth_tokens` | 64 位令牌、用户、创建与过期时间（30 天） |
| `sync_revisions` | 每个用户的当前 revision；推送时加悲观锁 |
| `sync_goals` / `sync_stages` | 目标与阶段，`(user_id, client_id)` 唯一 |
| `sync_tasks` | 任务、优先级、分类、日期、完成时间与目标 / 阶段关联 |
| `sync_habits` / `sync_habit_records` | 习惯与每日打卡，`(user_id, client_id)` 隔离 |
| `sync_growth_events` | 追加式经验流水，`source_key` 去重 |

## 构建与运行

### 环境

- Android Studio（Gradle JDK 使用 JDK 25；本机用 Amazon Corretto 25.0.5，并通过 `org.gradle.java.installations.paths` 指向安装目录）。
- Android SDK Platform 37、Build Tools 与 Platform Tools。
- 设备或模拟器 API 24+；本项目在 API 37 模拟器上验证。

### 后端与 MySQL

```sql
CREATE DATABASE orbit CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'orbit'@'127.0.0.1' IDENTIFIED BY '<自定义的本地开发口令>';
GRANT ALL PRIVILEGES ON orbit.* TO 'orbit'@'127.0.0.1';
```

```bash
cd server
export ORBIT_DB_PASSWORD='<上面 IDENTIFIED BY 里设的口令>'   # 必须设置，仓库不再内置默认值
./mvnw spring-boot:run
# 或 ./mvnw -DskipTests package && java -jar target/orbit-server-0.0.1-SNAPSHOT.jar
```

健康检查与账号数据都在 `http://127.0.0.1:8080`。头像默认写入 `server/data/avatars`，可用 `orbit.avatar-dir` 覆盖；数据库口令只能通过 `ORBIT_DB_PASSWORD` 环境变量传入，未设置时启动即失败（避免把口令写进仓库），连接地址与用户名在 `application.properties` 中固定为本地 MySQL。默认配置只适合本机开发。

客户端 Base URL 由 Gradle 属性 `orbitApiBaseUrl` 写入 `BuildConfig.ORBIT_API_BASE_URL`，2.3 缺省指向 mint 空间的 uniCloud 地址：

```
https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/
```

云函数、集合与 URL 化 PATH `/orbit` 均已部署，入口已用真实 HTTP 验证，安装 2.4.0 APK 即可连接云端登录；AI 功能还需要在 uniCloud 控制台配置 Coze 环境变量（见下）。若仍要连本机 Spring，可传 `-PorbitApiBaseUrl=http://10.0.2.2:8080/`（模拟器；真机用电脑局域网 IP），需要 `ACCESS_LOCAL_NETWORK` 与 `network_security_config.xml` 明文白名单；2.2 的 Cloudflare Quick Tunnel 地址也可继续作为构建参数，但隧道进程与这台电脑必须保持运行，重启会换域名。

切换后端不需要改代码，构建时传入属性即可（自动补尾部 `/`）：

```bash
./gradlew :app:assembleDebug "-PorbitApiBaseUrl=https://<spaceId>.dev-hz.cloudbasefunction.cn/orbit/"
```

`BuildConfig` 仍会生成 `ORBIT_BACKEND_LABEL`（地址含 `bspapp.com` / `cloudbasefunction.cn` 时为 `uniCloud · 支付宝云`，否则为 `Spring Boot · MySQL`），自定义域名可加 `-PorbitBackendLabel` 覆盖；本次文案收短后登录页不再展示该标签。云函数的集合与索引、HBuilderX 上传、URL 化路径、curl 检查与迁移命令见 [`unicloud/README.md`](unicloud/README.md)。

### 安装 Debug 包

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows 下如未自动识别 JDK，可先设置：

```bash
export JAVA_HOME="<Android Studio 安装目录>/jbr"
```

构建产物：

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- 2.9.1 Orbit Learn：[`artifacts/orbit-2.9.1-tab-swipe-debug.apk`](artifacts/orbit-2.9.1-tab-swipe-debug.apk)（16,269,363 bytes，Base URL `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`；SHA-256 `ba3f7f27113d91e0bfeb5b60cab438a97a26286dbd4cfdf7c6252a1d04d24e29`，MD5 `c1d87c02d610b18e4f4bb0fa9520c723`；与 `app-debug.apk` 逐字节相同）。在 2.9.0 的全部功能之上增加五个主页面跟手横滑翻页、卡片起手手势归属修复与两端 / 嵌套路由橡皮筋；APK 内 `learning/poems.json` 与 `learning/words.json` 仍在；安装到 Pixel 7 Pro API 37 后冷启动与连续翻页正常，crash buffer 为空
- 2.9.0 Orbit Learn：[`artifacts/orbit-2.9.0-orbit-learn-debug.apk`](artifacts/orbit-2.9.0-orbit-learn-debug.apk)（17,509,599 bytes，Base URL `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`；SHA-256 `4901f59fedcc73c1812e02851192bc74c5f979007eef8828be4f4752c36192e1`，MD5 `d8cb81f306ec769f0369b62848d19931`）。含目标 / 规划三段合并、学习页、首页今日学习三行预览、翻卡背单词（自动选词、5–50 自定义数量、手动选词、错词重练）与登录密码显示 / 隐藏；AI 三件套（规划 / 复盘 / 对话）仍需先按 [`docs/COZE_SETUP.md`](docs/COZE_SETUP.md) 配置云端环境变量
- 2.8.0 Orbit Learn：[`artifacts/orbit-2.8.0-orbit-learn-debug.apk`](artifacts/orbit-2.8.0-orbit-learn-debug.apk)（15,733,613 bytes，Base URL `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`；SHA-256 `6ef0c755e7d76cad0e30c43f761385134bc56e1f8e0524730547486436ab31f0`，MD5 `24737571fde2c26d52bec8909c0d7e82`）。含古诗搜索、古诗 / 单词设为今日、单词收藏与「全部 / 收藏」筛选，选择按账号保存在本机；词库仍为《唐诗三百首》309 篇与高中 3500 词去重 3,877 条。AI 三件套（规划 / 复盘 / 对话）仍需先按 [`docs/COZE_SETUP.md`](docs/COZE_SETUP.md) 配置云端环境变量
- 2.7.0 Orbit Learn：[`artifacts/orbit-2.7.0-orbit-learn-debug.apk`](artifacts/orbit-2.7.0-orbit-learn-debug.apk)（16,434,003 bytes，Base URL `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`；SHA-256 `a5c70e3ff4a0a148fd68487998c322d3cf228c8d756038821222b29a8b671e0b`，MD5 `0769eaa9400378d48262c6d876e34b11`）。含《唐诗三百首》309 篇、高中 3500 词去重 3,877 条、离开默认位置后自动半透明的助手球；AI 三件套（规划 / 复盘 / 对话）仍需先按 [`docs/COZE_SETUP.md`](docs/COZE_SETUP.md) 配置云端环境变量
- 2.6.0 Orbit Learn：[`artifacts/orbit-2.6.0-orbit-learn-debug.apk`](artifacts/orbit-2.6.0-orbit-learn-debug.apk)（14,897,859 bytes，Base URL `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`；SHA-256 `73fd1d705e1b63cd201de3fe6422d16ad0858440ae6fe0511cf7daf0b468f0eb`，MD5 `74f33bf405394fe802f157b15121f566`）。含三本书的书架、18 个数学知识点、可拖动助手球与星轨跨环；AI 三件套（规划 / 复盘 / 对话）仍需先按 [`docs/COZE_SETUP.md`](docs/COZE_SETUP.md) 配置云端环境变量
- 2.5.0 Orbit Learn：[`artifacts/orbit-2.5.0-orbit-learn-debug.apk`](artifacts/orbit-2.5.0-orbit-learn-debug.apk)（14,839,099 bytes，Base URL `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`；SHA-256 `29fc7e17ef53d0e0efc07cb8e818a4b597cf88c82944d87e2d81a5509045c704`，MD5 `74be2b1e1b32b8232a69346a59ae02d8`）。AI 三件套（规划 / 复盘 / 对话）需先按 [`docs/COZE_SETUP.md`](docs/COZE_SETUP.md) 配置云端环境变量
- 2.4.0 AI 客户端：[`artifacts/orbit-2.4.0-ai-debug.apk`](artifacts/orbit-2.4.0-ai-debug.apk)（14,684,211 bytes，Base URL `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`；SHA-256 `280a24dc07c1a0592a2a0ae7b8c53209d305bcb2c4f5939606c60a82eb51c48e`，MD5 `37d26e0f67c019d8a94007c75653ca2b`）
- 2.4.0 岗位材料包：[`artifacts/orbit-2.4.0-job-pack.zip`](artifacts/orbit-2.4.0-job-pack.zip)（APK + README + AI 设计 / 岗位包装 / Coze 指南 / OpenAPI + 5 张 AI 截图）
- 2.3.0 uniCloud 客户端：[`artifacts/orbit-2.3.0-unicloud-debug.apk`](artifacts/orbit-2.3.0-unicloud-debug.apk)（14,541,492 bytes，Base URL `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`；SHA-256 `9c7eca02b4ec1f452085c0f7e0f2a8ce0f1f9630a7b75842a2a6684fee8afde2`，MD5 `118eb8d7f1f333b28f64dc2c7758c1c7`）。控制台保存 URL 化 PATH 前，登录会失败
- 2.2.0 Spring / Cloudflare 刷新包仍保留：[`artifacts/orbit-2.2.0-public-refresh-debug.apk`](artifacts/orbit-2.2.0-public-refresh-debug.apk)（15,200,210 bytes，Base URL `https://irc-shade-mailed-longest.trycloudflare.com/`；SHA-256 `6c801b9e530058a6177aa19c2b2716ab72f8d8a6bf86ffa5b1ee7befaa543849`，MD5 `c65c4be616dada503131685457838030`）。隧道与电脑必须保持运行，Quick Tunnel 重启后会换域名
- 2.0 / 2.1 的旧安装包已清理；工程未配置 Release 签名
- 包名：`com.example.orbit`
- 版本：`2.9.1`（versionCode 12，minSdk 24 / targetSdk 37）

### 运行测试

```bash
# 纯 JVM：进度 / 统计 / 习惯 / 成长 / 等级 / 排序 / 几何布局 / 账号与同步规则
./gradlew :app:testDebugUnitTest

# 设备/模拟器：Room、迁移、偏好持久化、核心 UI 流程、账号表单、手势、返回栈
./gradlew :app:connectedDebugAndroidTest

# 服务端：认证 / 资料 / 头像、快照推送与 revision 冲突（H2 MySQL 模式）
cd server && ./mvnw test

# 云函数：scrypt / token / 快照 revision / 头像 / AI 规划 / 复盘 / 对话（55 项，内存 store）
cd unicloud/cloudfunctions/orbit-api && node --test
```

AI 真实调用检查（需要先在 uniCloud 控制台配置 `COZE_API_TOKEN` / `COZE_BOT_ID`；脚本只读环境变量中的账号密码，不打印令牌）：

```bash
export ORBIT_USERNAME='<测试账号>'
export ORBIT_PASSWORD='<测试密码>'
node unicloud/tools/check-ai.mjs            # 只查 configured 与三类额度
node unicloud/tools/check-ai.mjs --plan     # 真实生成一次计划，消耗 1 次额度
node unicloud/tools/check-ai.mjs --review   # 真实生成一次复盘，消耗 1 次额度
node unicloud/tools/check-ai.mjs --chat     # 真实对话一次，消耗 1 次额度
```

> `connectedDebugAndroidTest` 默认会在结束后卸载应用与数据；如需保留安装，加 `-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`。`ScreenshotGalleryTest` 会清空测试设备上的 Orbit 数据库与偏好，只应在测试设备上运行。

### 重新生成截图

截图测试会把 PNG 写到应用专属外部目录，因此要手动运行并在卸载前取出：

```bash
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

adb shell am instrument -w \
  -e class com.example.orbit.ScreenshotGalleryTest \
  com.example.orbit.test/androidx.test.runner.AndroidJUnitRunner

adb pull /sdcard/Android/data/com.example.orbit/files/screenshots \
  docs/screenshots/phase8

# 首页焦点卡与下半屏评审图（会写入一套演示数据并保留在设备上）
adb shell am instrument -w \
  -e class com.example.orbit.HomeVisualReviewTest \
  com.example.orbit.test/androidx.test.runner.AndroidJUnitRunner

adb pull /sdcard/Android/data/com.example.orbit/files/screenshots_v2 \
  docs/screenshots/phase8

# AI 规划 / 周复盘截图（2.4）
adb shell am instrument -w \
  -e class com.example.orbit.AiVisualScreenshotTest \
  com.example.orbit.test/com.example.orbit.OrbitTestRunner
adb pull /sdcard/Android/data/com.example.orbit/files/screenshots \
  artifacts/phase12

# 学习书页搜索 / 今日 / 收藏（2.8）
adb shell am instrument -w \
  -e class com.example.orbit.LearningVisualReviewTest \
  com.example.orbit.test/com.example.orbit.OrbitTestRunner
adb pull /sdcard/Android/data/com.example.orbit/files/screenshots_learning \
  artifacts/phase16

# 目标合并 / 学习页 / 背单词（2.9）
adb shell am instrument -w \
  -e class com.example.orbit.Phase17VisualReviewTest \
  com.example.orbit.test/com.example.orbit.OrbitTestRunner
adb pull /sdcard/Android/data/com.example.orbit/files/screenshots_phase17 \
  artifacts/phase17
```

`LearningVisualReviewTest` 会输出 13 张学习相关截图，其中 2.8 新增的测试方法输出 l12–l21 共 10 张；`@Before` 会清空本机 `orbit_learning`，只应在测试设备上运行。`Phase17VisualReviewTest` 输出 `p01`–`p14` 共 14 张 2.9 截图，同样会清空本机学习偏好，并写入固定的任务、目标与习惯种子数据。

当前截图集合共 20 张，覆盖星轨首页、节点详情、任务、统计、目标星系 / 列表 / 详情、习惯、归档、成长、设置、数据概览与双主题；另有 3 张首页评审图：焦点卡深色 / 浅色各一张，列表底部的任务与习惯衔接一张。截图来自 Compose 根节点，不含系统状态栏与导航栏，23 张图哈希均不重复。2.4 的 5 张 AI 截图来自 `AiVisualScreenshotTest`，同样使用 Compose 根节点；`ai-04` / `ai-05` 分别对应已生成复盘与生成入口。

## 验证记录

### 2.9.1 跟手横滑翻页（2026-11-03）

2.9.1 只动导航手势：五个主页面可以左右划着翻页，页面按 1:1 贴着手指平移，松手按距离（屏宽 20%、不低于 56dp）提交。Room schema、云端快照、后端与 AI 契约均未改动，目标页三段的点击切换也保持原样。

| 项目 | 结果 |
| --- | --- |
| `:app:testDebugUnitTest` | 156 项通过、0 失败（新增 17 项 `TabSwipeTest`：相邻标签顺序与越界、根路由白名单、`followOffset` 的 8dp 起步门槛与 1:1 跟手、横竖比例否决、未测量宽度不动、橡皮筋 0.28 折与 10% 上限、`thresholdPx` 的 20% / 56dp 双门槛、`evaluate` 只看距离不看速度、`shouldClaim` 的 24dp 认领门槛与竖划不认领、`settleOutMs` 随时长线性收敛） |
| 设备仪器测试 | 68 项通过、0 失败（原 62 项 + `SwipeNavigationTest` 6 项：左滑进下一页、右滑回上一页、两端不循环、短划不翻页、慢速划满整屏照样翻页、卡片上起手翻页、目标详情不响应横滑） |
| 跟手与可见性 | 通过：设备实拍（按住不放截屏）显示内容区随指针 1:1 平移，划出 350px / 600px 两帧的位移与手指一致，状态栏与底部栏不动，`OrbitLearn` 标题、星轨环、今日焦点卡与助手球一起走 |
| 背景不割裂 | 通过：五个页面共用同一层 `SpaceBackdrop` 且自身不画底色，滑开露出的那一条与页面背后同为 `backgroundTop`，中途帧无接缝；横滑期间 `NavHost` 淡入淡出被让开，新页不会半透明叠在旧页上 |
| 两段滑移动画 | 通过：去程按已划距离缩短（180ms 收到 70ms），旧页继续滑出屏幕后在屏幕外挂载新页，再从对侧 230ms 滑入；点底部栏仍是原来的淡入淡出 |
| 卡片起手归属 | 修复并验证：整层 `graphicsLayer { translationX }` 会带着卡片的命中框一起走，卡片相对指针静止，它的 `clickable` 不会因位移超阈值而取消，于是一次横滑既翻页又打开详情。改为划出 24dp 后父层在 Initial pass（父先于子）消费事件，卡片据此放弃点击；`SwipeNavigationTest` 断言起手于「当前目标」卡的横滑落到学习页且详情未打开，实拍 `p08` / `p09` / `p10` 同一结论；同一处普通点按仍照常进详情（`p11`） |
| 子控件优先 | 通过：星轨节点拖拽、阶段进度条、助手球、横向书架在 Main pass 先拿到指针，24dp 内消费掉横向位移就仍归它们管，页面弹回（实拍 `p16` 拖住节点换轨时页面纹丝不动） |
| 两端与嵌套路由 | 通过：星轨右划、我的左划只做橡皮筋（400 / 600px 的手指位移分别只走约 110 / 140px），松手弹回原页；`goals/{goalId}` 详情页同样只橡皮筋，松手后与起手前逐字节相同（`p11` 与 `p13` MD5 一致），系统侧边返回手势不受影响 |
| 截图 | 16 张模拟器真实全屏截屏（深色，1440×3120，含系统状态栏），15 个互不重复的 MD5，唯一重复的一对是详情页橡皮筋的起手帧与回弹帧（按设计应当完全相同）；写入 `docs/screenshots/phase18/` 与 `artifacts/phase18/`，该目录另附 156 项 JVM XML、68 项设备测试 XML 与 Gradle 日志；全部使用离线 QA 数据，不含账号凭证 |
| 2.9.1 APK | 16,269,363 bytes；SHA-256 `ba3f7f27…d24e29`，MD5 `c1d87c02…c723`；versionName 2.9.1 / versionCode 12；Base URL 指向 mint 空间；APK 内 `learning/poems.json` 与 `learning/words.json` 仍在；安装到 Pixel 7 Pro API 37 后冷启动与连续翻页正常，crash buffer 为空 |

> 2.9.1 不新增网络请求，也不改数据层：横滑只是 `OrbitApp` 里那条 `navigateTo` 的另一个触发口，`popUpTo / launchSingleTop / restoreState` 与点底部栏完全一致，分段重置规则照旧。

### 2.9 目标合并、学习页与翻卡背单词（2026-10-29）

2.9 把「目标」与「规划」合成一页三段，并在腾出的标签上新增学习页：首页今日学习收成三行预览，完整的古诗、单词、数学与背单词入口搬到学习页。背单词新增 12 项纯 Kotlin 规则与 5 项设备流程 / 截图用例；本轮不改 Room schema、云端快照、后端与 AI 契约。

| 项目 | 结果 |
| --- | --- |
| `:app:testDebugUnitTest` | 139 项通过、0 失败（新增 12 项 `WordStudyTest`：自动选词三桶与书序、去重与数量上限、词库耗尽、手动书序 / 未知 id / 50 上限、目标夹取、翻面门槛、正确率、错词重练、全对无重练、作答计数与每日去重、搜索） |
| 设备仪器测试 | 62 项通过、0 失败（原 56 项 + `WordStudyFlowTest` 2 项 + `Phase17VisualReviewTest` 3 项 + `AuthGateTest` 密码可见性 1 项） |
| 导航合并 | 通过：底部为星轨 / 目标 / 学习 / 成长 / 我的；目标页默认「目标」，任务与习惯分段可切换；再次点底部「目标」复位；目标详情与 AI 规划的路由未受影响 |
| 首页预览 | 通过：今日学习只剩三行摘要，不再渲染长诗与公式；点任意一行或「进入学习」都打开学习页；今日选择与学习页、书架 TODAY 卡同步 |
| 自动选词回合 | 通过：预设 5 开局，翻面前看不到「认识 / 不认识」；4 认识 1 不认识得到 80% 结算，重练错词后 100%；完成后学习卡显示 5 / 5 与「今日目标已完成」；Activity `recreate()` 后进度仍在 |
| 手动选词 | 通过：分别搜索 `confident` 与 `focus` 各选 1 个，显示「已选 2 / 50」，开局按书序；键盘收起后开始按钮不被遮挡 |
| 按钮对比度 | 修复 `OrbitPrimaryButton` 的 QUIET 样式在深 / 浅主题下文字与底色同色的问题（「自己选词」「再来一轮」）：改用 `textPrimary`，深空与晨光主题实机截图均确认可读；`WordStudyFlowTest` 2 项复跑通过 |
| 密码可见性 | 登录与注册的密码框都带「显示 / 隐藏」切换，确认密码有独立状态；`AuthGateTest` 通过 `GetTextLayoutResult` 断言隐藏时布局文本不是明文、切换后等于输入的那个测试口令（`QA_TYPED_PASSWORD`，只在表单里敲进去再读回来，不发往任何后端），并用 content description 核对按钮语义；模拟器上实拍确认点按后密码明文显示、标签变为「隐藏」 |
| 截图 | 14 张模拟器真实截屏（深色，1440 宽；页面 1440×3120，弹层高度随 sheet 为 2712 / 2771），MD5 互不重复；写入 `docs/screenshots/phase17/` 与 `artifacts/phase17/`，该目录另附 139 项 JVM XML、62 项设备测试 XML 与 Gradle 日志 |
| 2.9.0 APK | 17,509,599 bytes；SHA-256 `4901f59f…92e1`，MD5 `d8cb81f3…9131`；versionName 2.9.0 / versionCode 11；Base URL 指向 mint 空间；APK 内 `learning/poems.json` 与 `learning/words.json` 仍在；安装到 Pixel 7 Pro API 37 后冷启动正常，无 AndroidRuntime 异常 |

> 2.9 不新增网络请求，不改 `/api/ai/*`、Room schema、后端与 Coze 环境变量；背单词进度、今日选择与收藏仍是本机偏好，不参与云端快照。卸载重装、清除数据或换设备会丢失这些记录，云端任务 / 目标 / 习惯 / 成长不受影响。

### 2.8 古诗搜索、每日挑选与单词收藏（2026-10-29）

2.8 在两本书之上加了一层管理与个性化：古诗页可以按诗名、作者、朝代或诗句搜索，并随时把一首诗「设为今日」；英语页可以收藏单词、只看收藏，也能把任意一条设为今日。选择按账号保存在应用私有 `SharedPreferences`，首页「今日学习」、书架 TODAY 卡与书页今日标记同步更新；跨天自动回到 epoch day 轮换。数学、星轨、账号、云同步与 AI 契约均未改变。

| 项目 | 结果 |
| --- | --- |
| `:app:testDebugUnitTest` | 127 项通过、0 失败（新增 13 项 `LearningProgressTest`：跨天失效、搜索空串 / 标题 / 作者 / 诗句 / 朝代 / 多词 / 大小写 / 无结果、收藏筛选、未知 id 回退、今日选择与轮换回退） |
| 设备仪器测试 | 56 项通过、0 失败（含新增的 `capturePoemSearchDailyPickAndWordBookmarks`，一次跑完搜索、设为今日、收藏筛选、首页同步与 Activity 重建持久化） |
| 古诗搜索 | 通过：输入「李白」实时显示「找到 32 首」（含标题含李白的杜甫诗），清空后输入「静夜思」收窄到 1 首；无结果时显示空状态与「清空搜索」 |
| 设为今日 | 通过：展开《静夜思》滚动到按钮并点击后，书页今日标记与首页预告同时更新，书架 TODAY 卡变为「古诗《静夜思》」；同一天内不重复写入 |
| 单词收藏与筛选 | 通过：收藏 `a` 后标签变「已收藏」、筛选 chip 显示「收藏 · 1」，切到收藏后列表只剩该词；收藏为空时给出引导与「全部」入口 |
| 单词设为今日 | 通过：在收藏筛选下把 `a` 设为今日，首页与书架同步；Activity `recreate()` 后首页两条选择仍在，重新打开书架 TODAY 行仍显示「静夜思 / a」 |
| 状态隔离 | 通过：`LearningProgressStore` 用 owner 后缀分账号保存，未登录回退 `local`；切换账号重新加载，不串数据 |
| 截图 | 10 张模拟器真实截屏（1440×3120），MD5 互不重复；写入 `docs/screenshots/phase16/` 与 `artifacts/phase16/`，该目录另附 127 项 JVM XML 与 56 项设备测试日志 |
| 2.8.0 APK | 15,733,613 bytes；SHA-256 `6ef0c755…ab31f0`，MD5 `24737571…0d7e82`；versionName 2.8.0 / versionCode 10；APK 内 `learning/poems.json` 1,446,194 bytes、`learning/words.json` 536,978 bytes 仍在 |

> 2.8 不新增网络请求，不改 `/api/ai/*`、后端与 Coze 环境变量；今日选择与收藏是本机偏好，不参与云端快照。卸载重装、清除数据或换设备会丢失这两类设置，云端任务 / 目标 / 习惯 / 成长记录不受影响。

### 2.7 唐诗三百首、高中 3500 词与可隐藏的助手球（2026-10-29）

2.7 把两本“样书”换成真实词库：古诗对接《唐诗三百首》309 首（含《静夜思》《长恨歌》《琵琶行》等，注释与赏析来自语料），英语对接高中 3500 词，按词名去重后 3,877 条（保留短语与变形，因此多于 3,500）。两份 JSON 随 APK 打包并在启动时后台预热；长诗默认折叠为 4 行，缺注释 / 赏析 / 例句 / 音标的条目省略对应区块。助手球新增空闲透明度：离开默认位置 0.55、贴近屏幕边缘 0.32、按住与拖动 1.0，默认角落即使靠边也保持常亮。书架、数学讲义、每日一句、星轨与云端接口均未改变。

| 项目 | 结果 |
| --- | --- |
| `:app:testDebugUnitTest` | 114 项通过、0 失败（新增 5 项：2.7 词库真实内容与数量校验 1 项、`AiAssistantFabVisibility` 4 项） |
| 设备仪器测试 | 55 项通过、0 失败（47 项断言 + 8 项截图生成；`LearningVisualReviewTest` 扩到 13 张截图） |
| 词库加载 | 通过：JVM 与设备读取同一份 `src/main/resources` JSON；309 首 / 3,877 词数量、`静夜思`、`长恨歌`、`abandon` 字段非空；每日轮换遍历全部条目不重复 |
| 长诗与空字段 | 通过：古诗折叠 4 行；无例句词条显示空态提示与 AI 入口；3 篇空注释 / 11 篇空赏析不渲染空标题 |
| 助手球半透明 | 通过：默认位置语义为「常亮」；拖离后为「半透明」，像素与透明度同步下降；语义层验证不依赖截图判断 |
| 数学与书架回归 | 通过：书架仍显示 309 篇 / 3,877 词 / 18 个知识点；数学 6 个年级 chip 与「指数运算」展开内容不变 |
| 截图 | 13 张模拟器真实截屏（1440×3120），MD5 互不重复；写入 `docs/screenshots/phase15/` 与 `artifacts/phase15/`，该目录另附本轮测试报告 |
| 2.7.0 APK | 16,434,003 bytes；SHA-256 `a5c70e3f…b671e0b`，MD5 `0769eaa9…34b11`；versionName 2.7.0 / versionCode 9；Base URL 指向 mint 空间；静态校验包内两份 JSON 资源存在 |

> 2.7 不新增网络请求，不改 `/api/ai/*` 契约、后端与 Coze 环境变量；资料仍是离线内置，只有点「AI 讲解 / 让 AI 讲讲」才调用云函数。设备测试继续使用 QA 离线会话，不消耗真实配额。

> 取舍：词库按原始词名精确去重，不合并 phrase 与词形变化，所以显示 3,877 而不是 3,500；完整词库让 APK 增加约 1.5 MB。助手球只做透明度提示，不自动移开、不缩小点击区域，58dp 命中范围保持不变。

### 2.6 学习书架、数学讲义与可拖动交互（2026-10-29）

2.6 修复了真机反馈的「古诗 / 英语列表滑到底部后疯狂跳动」，并把学习资料改成三本书的书架：唐诗选 12 篇、英语词卡 18 个、数学讲义 18 个知识点（初一至高三，可切换年级）。AI 助手悬浮球可在首页内容区拖动并记住位置；星轨节点可跨内 / 外环拖动，目标环以虚线高亮；「今日任务 / 今日习惯」更名为「今日学习任务 / 学习习惯」，其下新增可刷新的每日一句。

| 项目 | 结果 |
| --- | --- |
| `:app:testDebugUnitTest` | 109 项通过、0 失败（新增 13 项：`nearestRing` 中点边界、手动换环在完成后保持、双手动环 9 节点四宽度分离、学习库数量 / id 唯一 / 年级过滤 / 轮换 / prompt / 名句回退） |
| 设备仪器测试 | 55 项通过、0 失败（47 项断言 + 8 项截图生成；新增跨环拖动与 `LearningVisualReviewTest` 2 项） |
| 底部抖动 | 通过：古诗、英语分别滚到最后一项，截取书籍节点后推进 Compose 主时钟 1.2 秒再截一帧，两帧逐像素相同；修复为禁用整张 sheet 拖拽 + 移除 `animateContentSize` |
| 学习书架 | 通过：三本书与数量、数学 6 个年级 chip、切换到高一出现指数 / 对数 / 三角恒等、展开「指数运算」显示公式 / 例题 / 答案 / 易错提醒 |
| 助手球拖动 | 通过：从右下拖到轨道右侧，语义边界移动 ≥120px / 300px，滚动首页后位置不变，轻触仍打开助手 |
| 星轨跨环 | 通过：JVM 验证 0.32 中点边界与多宽度分离；设备端把「整理解题错因」从内环拖到外环，任务仍未完成、没有误开详情，虚线高亮可见 |
| 每日一句 | 通过：初始「路漫漫其修远兮」，点「换一句」后变为「宝剑锋从磨砺出」，按日轮换且不随机重复 |
| 截图 | 12 张模拟器真实截屏（1440×3120），MD5 互不重复；写入 `docs/screenshots/phase14/` 与 `artifacts/phase14/`，该目录另附本轮测试报告 |
| 2.6.0 APK | 14,897,859 bytes；SHA-256 `73fd1d70…f0eb`，MD5 `74f33bf4…1566`；versionName 2.6.0 / versionCode 8；Base URL 指向 mint 空间；安装后冷启动进入主界面的冒烟验证通过 |

> 2.6 不新增网络请求，不改 `/api/ai/*` 契约与 Coze 环境变量；资料仍是离线内置，只有点「AI 讲解 / 让 AI 讲讲」才调用云函数。设备测试继续使用 QA 离线会话，不消耗真实配额。

> 取舍：资料弹窗禁用整张 sheet 的下滑关闭以消除抖动，关闭入口改为顶部按钮、返回键与遮罩；手动换环只在本次进程内有效，不写 Room。

### 2.5 AI 学习助手、学习资料与 Orbit Learn（2026-10-29）

2.5 把 AI 从「生成草稿」推进到常驻对话：首页新增 58dp 助手 FAB 与底部弹窗，接入 `/api/ai/chat`（同一个 Coze Bot，prompt `chat-v1`），支持自由问答与「生成计划 / 生成周复盘 / 学习资料」快捷动作；品牌升级为 Orbit Learn，首页加入「今日古诗 / 今日英语」卡片与古诗、英语两个资料库。真实 Coze 对话已在模拟器上跑通；计划与复盘此前已在云端用脚本实测，但设备端的导入 / 复盘卡流程尚未单独回归。

| 项目 | 结果 |
| --- | --- |
| 云函数 `node --test` | 55 项通过、0 失败（原有账号 / 同步 29 项 + 2.4 AI 20 项 + 新增 chat 6 项：输入校验、历史裁剪、上下文聚合、配额、输出规范化、空回复映射） |
| `:app:testDebugUnitTest` | 96 项通过、0 失败 |
| 设备仪器测试 | 52 项通过、0 失败（48 项断言 + 4 项截图；修复 5 处品牌文案断言后复跑） |
| 真实 Coze 对话 | 通过：登录临时账号后在弹窗内连续多轮中文对话；额度随请求递减（剩余 18 → 13），回复由真实模型生成，非固定文案 |
| 学习资料 | 通过：古诗 / 英语两栏、卡片展开（注释 / 赏析、例句译文 / 记忆提示）、今日标记；古诗「让 AI 讲讲」自动带入上下文并返回逐句讲解 |
| 设备端 E2E | 通过：登录（云端同步徽章）→ 首页今日学习 → 助手弹窗在线额度 → 多轮对话 → 学习资料 → 古诗展开 / AI 讲解 → 英语展开；全程无崩溃 |
| 截图 | 9 张真实截屏（1440×3120），MD5 互不重复：`be577781…` / `a8af0a08…` / `cc3537bd…` / `60a39131…` / `b008502f…` / `9d611a87…` / `a08c7095…` / `d5e23133…` / `647f11fe…`；写入 `artifacts/phase13/final/` 与 `docs/screenshots/phase13/` |
| OpenAPI 文档 | `docs/openapi-orbit.yaml` 解析通过（12 个路径、29 个 schema）；新增 `/api/ai/chat` 请求 / 响应与 `AiStatus` 的 chat 限额字段 |
| 2.5.0 APK | 14,839,099 bytes；SHA-256 `29fc7e17…c704`，MD5 `74be2b1e…02d8`；versionName 2.5.0 / versionCode 7；Base URL 指向 mint 空间 |
| 真实计划 / 复盘 | 2026-10-28 晚已用脚本实测真实规划 / 复盘：规划约 11.3 s、3 阶段 12 个任务；复盘约 6.8 s；用量计数正常累加。2.5 未在设备上重走导入与复盘卡 |

> 本轮修复了两处问题：助手长回复会被输入栏截断（自动滚动改为随消息数量与回复长度触发）；品牌改名后 5 处仪器测试断言仍在等待「ORBIT」导致超时，已同步为「ORBIT LEARN」与关于页新文案。

> 9 张 2.5 截图来自模拟器真实截屏（含真实 Coze 回复），与仪器测试的 Compose 截图不是同一套；设备仪器测试仍走 QA 离线会话，两个 AI ViewModel 在 `syncBlocked` 时跳过网络，不消耗真实配额。截图与文档不包含令牌、Coze 原始响应或账号凭证。

### 2.4 AI 网关与学习规划（2026-10-28）

2.4 的 Android 规划页、周复盘卡、导入事务、云函数 AI 网关与 Coze 客户端已完成；客户端、云函数、OpenAPI 文档与岗位材料都已落盘。真实 Coze 调用仍等待在 uniCloud 控制台配置 `COZE_API_TOKEN` / `COZE_BOT_ID`，因此本轮验证的是「契约、规范化、配额、失败语义与 UI」，不是模型质量与真实延迟。

| 项目 | 结果 |
| --- | --- |
| 云函数 `node --test` | 49 项通过、0 失败（原有账号 / 同步 29 项 + AI 20 项） |
| `:app:testDebugUnitTest` | 96 项通过、0 失败（新增 AI 30 项：复盘指标 9、导入裁剪 9、勾选 7、错误映射 5） |
| 设备仪器测试 | 52 项通过、0 失败，耗时 611.577 s（48 项断言 + 4 项截图；含导入事务 2 条新用例） |
| AI 截图 | 5 张，MD5 互不重复：`cc2a8602…` / `24809d98…` / `2c91f943…` / `fb54138f…` / `d6847d8a…`；写入 `artifacts/phase12/` 与 `docs/screenshots/phase12/` |
| OpenAPI 文档 | `docs/openapi-orbit.yaml` 解析通过（11 个路径、24 个 schema）；覆盖账号、快照、头像、AI 与媒体跳转 |
| AI 状态 | 三个 AI 路由已在 `mint` 上线：未带令牌的 `/api/ai/status`、`/plan`、`/review` 均返回 401，`/api/health` 返回 200；Coze 环境变量未配置，客户端走未配置空态 |
| 真实 Coze 调用 | **未执行**（需要账号持有人配置 PAT 与 bot_id，不能写进代码或交给他人） |
| 真机 E2E | **未执行**（计划 → 导入 → 完成任务 → 周复盘尚未在实体设备跑通） |
| 2.4.0 APK | 14,684,211 bytes；SHA-256 `280a24dc…c48e`，MD5 `37d26e0f…ca2b`；Base URL 指向 mint 空间 |

> AI 设备测试使用 QA 会话旁路，两个 ViewModel 在 `syncBlocked` 时跳过所有 AI 网络请求，因此 48 项断言完全离线，也不会消耗真实配额。截图同样使用固定假状态，不包含真实账号或令牌。

> 2026-10-28 晚补记：账号持有人配置 `COZE_API_TOKEN` / `COZE_BOT_ID` 后，`/api/ai/status` 返回 `configured:true`，真实 `/api/ai/plan`（约 11.3 s、3 阶段 12 个任务）与 `/api/ai/review`（约 6.8 s）均通过，用量计数从 0 累加到 plan 1 / review 1。设备端的计划导入与复盘卡仍未重跑。

### 2.3 云函数、URL 化与账号链路已验证（2026-10-28）

后端迁移到 uniCloud 支付宝云：`orbit-api`、三个集合 / 索引与 URL 化 PATH `/orbit` 已部署到 `mint`（`env-00jy6ydc58ll`），入口 `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit` 已用真实 HTTP 验证，2.3.0 APK 已按该地址导出。注册、登录、昵称、空快照、推送 revision 1、过期 revision 409、头像上传与媒体跳转、退出登录后旧令牌 401、重新登录后的数据恢复全部通过；Android 真机 E2E 仍待补。联调中发现支付宝云 URL 化拒绝 204，退出登录已由 204 改为 `200 {"status":"ok"}` 并重新部署。

| 项目 | 结果 |
| --- | --- |
| 云函数 `node --test` | 29 项通过、0 失败（scrypt、令牌、路径前缀含 `/http` 网关段、base64、注册 / 登录 / 登出 / 过期、昵称、快照 revision 与 409、头像与跳转、错误体） |
| `:app:testDebugUnitTest` | 66 项通过、0 失败 |
| 设备仪器测试 | 46 项通过、0 失败（新增账号卡退出 → 登录页；设置 / 关于 / 登录页文案断言同步更新） |
| 服务端 `./mvnw test` | 2 项通过、0 失败 |
| Base URL 构建参数 | 缺省生成 `"https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/"` + `"uniCloud · 支付宝云"`，版本 2.3.0（code 5）；自定义域名加 `-PorbitBackendLabel` 按覆盖值生成 |
| 云函数上传 | 通过（HBuilderX CLI 返回「云函数orbit-api上传完成」） |
| 集合与索引 | 通过（`orbit_users` / `orbit_tokens` / `orbit_snapshots`，唯一索引与普通索引已配） |
| 云函数 URL 化 | 通过：`GET https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/api/health` 返回 200 + `{"status":"ok","backend":"unicloud"}` |
| 云端 HTTP E2E | 通过：注册 201、昵称 200（中文正确）、空快照 revision 0、推送 revision 1、过期 revision 409、头像上传 200 → 媒体跳转 → 云存储 200 image/png、退出 200 且旧令牌 401、重新登录后昵称 / 头像 / 快照仍在 |
| 退出登录修复 | 支付宝云 URL 化拒绝 204（令牌已删但响应 500），改为 `200 {"status":"ok"}` 后重新上传云函数并回归通过；Android 客户端忽略响应体，无需改代码 |
| 界面收尾 | 退出登录改为整行红色描边按钮并附换号提示；登录页删除技术规格与底部细字；关于页只留应用名、一句简介与版本号；phase11 截图已核对 |
| 迁移脚本 | 本地假后端端到端通过：旧快照 revision 7 导出；新账号注册；以 `baseRevision=0` 推送后返回 revision 1，目标 / 阶段 / 任务 / 成长记录均保留 |
| 真机 E2E | **未执行**（注册、昵称、同步冲突、头像尚未在 Android 客户端上验证） |

> 云函数单测使用内存 store，不接触支付宝云；本轮已用真实 HTTP 验证云数据库与云存储的读写、跳转和错误路径。支付宝云 URL 化的请求 / 响应上限为 32 MB，本轮只测了常规大小，未做大体积压测。

### 2.2（2026-10-27）

Windows + Pixel 7 Pro 模拟器（API 37，Android 17，1440×3120，density 560 = 411dp），后端为本机 Spring Boot + MySQL 8.4：

| 项目 | 结果 |
| --- | --- |
| `:app:testDebugUnitTest` | 66 项通过，0 失败（新增核心命中、拖动半径边界与 `requiresLocalNetworkPermission` 公网 / 私网判定用例） |
| 设备仪器测试 | 45 项通过，0 失败（登录门禁、昵称招呼、拖入核心完成、主线程清缓存回归、既有 Room / UI / 迁移用例） |
| 服务端 `./mvnw test` | 2 项通过，0 失败（含同一账号连续两次整包推送） |
| 登录门禁 | 清空数据启动只见 `AuthScreen`；无底部标签、无星轨；注册 / 登录成功后才进入主界面 |
| 首页昵称 | 昵称改为 `NovaPilot`，MySQL `app_users.nickname` 同步更新，首页问候显示「夜深了，NovaPilot」 |
| 相册权限 | 点击「从相册选择」弹出系统照片权限对话框（Allow limited access / Allow all / Don’t allow）；允许后进入系统照片选择器并完成头像上传 |
| 相机权限 | 点击「拍照上传」弹出系统相机权限对话框；拒绝后提示「需要相机权限才能继续上传」，头像保持不变 |
| 星轨完成 | 把节点拖入核心后任务完成、首页 100%（1/1），服务端任务行写入 `completed_at`，成长流水 +10 XP |
| 退出与恢复 | 退出确认后立即回到登录页；重新登录 `nova_1027` 后任务、昵称、头像与 revision 2 全部从 MySQL 恢复 |
| 公网登录 | 撤销 `ACCESS_LOCAL_NETWORK`（`granted=false`）后经 `https://politics-std-loved-equipment.trycloudflare.com` 登录：`POST /api/auth/login` 200、`GET /api/sync` 200，未弹「附近的设备」，昵称 / 任务 / 100% 进度恢复 |
| 缓存清理 | `prepareForAccount(424242)` 在 Main 调度器调用不再抛 Room 主线程异常，归属被重置 |
| Debug APK | 2.2 阶段曾生成公网版与局域网版；旧包已清理，当前保留的刷新包见「构建产物」 |

> 45 项设备测试包含 2 个截图生成用例；其余 43 项为断言型设备测试。2.2 新增 `AuthGateTest`（3 项）与「拖入核心完成」手势用例，并修正了登录后清缓存跑在主线程导致的崩溃。

### 2.1（2026-10-26）

| 项目 | 结果 |
| --- | --- |
| `:app:compileDebugKotlin` / `:app:assembleDebug` | 通过 |
| `:app:testDebugUnitTest` | 62 项通过，0 失败 |
| `:app:connectedDebugAndroidTest` | 41 项通过，0 失败（账号与云同步改动后的最终构建重跑） |
| 服务端 `./mvnw test` | 2 项通过，0 失败（认证 / 资料 / 头像、快照推送与 revision 冲突） |
| 注册 / 登录 | `orbit2026` 注册 201；授权「附近的设备」后登录成功；错误密码被拒绝 |
| 昵称与头像 | 昵称改为 `NovaPilot`（PATCH 200）；相册与相机两条路径都完成系统权限弹窗与 multipart 上传 |
| 任务云同步 | 新建 `Cloud Sync Check` 后 PUT `/api/sync` 200，MySQL `sync_tasks` 出现该行，revision 2 |
| 云端恢复 | `pm clear` 后重新登录，仅 GET `/api/sync` 即恢复任务，首页徽标显示「云端同步」 |
| 退出登录 | POST `/api/auth/logout` 204，令牌清除，本机 Room 缓存与 revision 保留 |
| ACCESS_LOCAL_NETWORK | 未授权时登录会触发系统弹窗；拒绝时显示权限提示，允许后请求才发出 |
| v1 → v2 迁移 | 通过（真实 v1 数据库设备升级 + `MigrationTestHelper`） |
| 360dp / 411dp 布局检查 | 通过（五个标签、目标详情、习惯、成长、我的；深空 / 晨光） |
| 启动图标 | 自适应图层在圆形 / 圆角方形 / 方圆遮罩下无裁切，前景位于 66dp 安全区；单色主题图标与旧版方形 / 圆形回退图均已生成 |
| 截图 | 2.1 账号 / 权限 / 首页 5 张真实设备截图；2.0 五个标签 20 张 + 3 张首页评审，411dp，无重复哈希 |
| Debug APK | 2.1 阶段已生成；历史包已清理 |

> 「41 项」包含 2 个截图生成用例（`ScreenshotGalleryTest`、`HomeVisualReviewTest`）；其余 39 项为断言型设备测试。2.1 修改了首页提示文案并把账号卡放到设置页顶部，相应更新了截图用例的滚动目标与「重新启动后主题仍选中」的滚动步骤。

> 360dp 检查通过修改模拟器 density 完成，不是物理 360dp 设备。该宽度下目标详情的状态操作会自动换成两行，这是有意保留的布局，不是溢出。

> 性能：本阶段未接入 Macrobenchmark、未测内存，也没有可复现的帧耗时或后端压测数据；请不要把旧版 Debug 模拟器上的观察值当作 2.1 或 Release 的表现。

## 已知限制

- **提醒未实现**：没有 WorkManager / AlarmManager 调度，因此设置页不提供开关；未来实现时需要同时处理通知权限、渠道、关闭行为和 Android 版本差异。
- **云同步是整包快照**：服务端权威、没有字段级合并，离线设备上的修改可能在 409 时被云端覆盖；单类数据硬上限 5000 行，没有增量日志或实时协同。
- **主键可能相撞**：同步沿用本机 Room 的 `id` 作为 `client_id`，两台设备同时离线新建时可能产生相同 id，靠下一次整包覆盖收敛；没有 UUID 迁移。
- **单账号缓存**：同一台设备只保留一份 Room 缓存，切换账号时按 `localOwnerUserId` 覆盖，不提供多账号本地沙箱；2.2 起退出登录会清空缓存，离线时无法直接进入主界面。
- **只支持在线账号**：没有找回密码、邮箱 / 手机号验证、账号注销或第三方登录；令牌 30 天有效，客户端与数据库均未对令牌再做哈希。
- **2.2 公网入口依赖本机隧道**：2.2.0 APK 把 Cloudflare Quick Tunnel 域名写进 `BuildConfig`，手机只需公网即可登录；但这台电脑、Spring Boot 与 `cloudflared` 必须常开，免费隧道重启后会换域名。2.3 已把缺省入口换成 uniCloud，不再依赖本机进程。
- **uniCloud 真机 E2E 只完成了一半**：`orbit-api`、三个集合与 URL 化已部署到 `mint`，health、注册、登录、昵称、快照、409、头像与退出登录已用真实 HTTP 验证；2.5 已在模拟器上完成真实登录与云端同步读取，但注册、409 冲突和头像上传仍未在 Android 客户端上跑通。快照受 32 MB URL 化上限与单类 5000 行限制，且没有登录限流、找回密码或账号删除。
- **AI 真机链路只覆盖了对话**：2.5 已在模拟器上完成真实登录、在线额度、多轮 Coze 对话与学习资料讲解；计划 / 复盘的真实生成虽已在云端脚本中通过，但设备端的表单 → 导入 → 复盘卡流程没有单独回归。模型输出质量、失败重试与多次调用的稳定性只由有限样本覆盖。
- **AI 对话非流式、不持久化**：回复一次返回，没有 token 流式展示；历史只存在 ViewModel 内存（最多 8 轮、单条 600 字），关闭弹窗或进程结束即清空，不写 Room、不同步云端，重开只能从欢迎页重新开始。聊天只发送聚合数字与最多 5 个目标标题，学习资料正文不会上云。
- **AI 配额与陈旧读**：每账号每日规划 6 次、复盘 4 次、对话 20 次，按北京时间在调用前扣减；状态接口偶发返回上一个实例的旧计数（联调中曾从 4 回跳到 0），下一回合会自行恢复，客户端不做本地兜底。周复盘缓存只在本机，换设备或清数据后需重新生成并再消耗一次。
- **助手 FAB 默认位置仍可能盖住列表尾部**：58dp 悬浮按钮默认在首页右下角，滚动到「今日学习」或任务列表末尾时可能压住最后一行的一角；2.6 起可以手动拖开，2.7 起拖离默认角落后会降到 0.55 / 0.32 透明度以减轻遮挡，但位置只在当前进程内保留，没有为列表自动避让，也没有吸附到边缘。
- **学习资料是离线内置语料**：309 首唐诗、3,877 条高中词汇与 18 个数学知识点以 JSON / Kotlin 常量随 APK 打包，没有搜索、收藏、发音、学习进度或掌握度；「今日」只是按日期取模，换设备不会同步学习记录。词库按原始词名去重，短语与变形让总数高于 3,500；42 条缺音标、3 篇缺注释、11 篇缺赏析时对应区块省略；数学公式是纯文本，不渲染分式与根号覆盖线。两份 JSON 首次解析有一次性开销，已在 Application 启动时后台预热，低端机的实际耗时未单独测量。
- **资料弹窗不能下滑关闭**：为消除列表底部的回弹抖动，2.6 禁用了整张 sheet 的拖拽；关闭入口只有顶部按钮、返回键与点击遮罩。该修复只在 Pixel 7 Pro / API 37 模拟器上用逐像素帧比对验证，厂商 ROM 的手势策略仍需真机复核。
- **Spring 开发配置仍是开发级**：本机后端 HTTP 明文、开发数据库口令、头像存后端磁盘；公网侧仅由 Cloudflare 隧道提供 TLS，没有应用层限流、验证码、对象存储或备份策略，不能直接当作生产环境。
- **无导出 / 导入**：卸载应用仍会清除本机缓存（云端账号数据保留）；没有 JSON / Markdown 导出或跨版本回滚。
- **迁移验证缺口**：没有归档的 v1 APK，未重放完整「安装 v1 → 覆盖安装 v2」路径；真实 v1 数据库升级与 `MigrationTestHelper` 均通过，但安装器行为未覆盖。
- **不支持降级安装**：Room 会拒绝旧版本打开 v2 数据库；没有 v1 回滚包，升级前请自行保留数据或等待后续的导出能力。
- **升级不回填历史经验**：v1 中已完成的任务 / 阶段不会补发经验；迁移后新产生的达成才写入流水。
- **只有每日习惯**：不支持每周次数、补卡日历、提醒、桌面小组件；习惯归档后不能继续打卡。
- **经验不会扣回**：取消完成或撤销打卡后，已发放的经验保留；这是设计决策，成就 / 徽章也未实现。
- **节点拖动位置不持久化**：拖动换槽与 2.6 的跨环都只影响本次显示的临时布局，重启后按默认槽位 / 默认环重新排布；拖入核心会写入完成状态，但节点坐标与手动选择的环不会保存。
- **只有 Debug 包**：工程未配置 Release 签名，未生成 Release APK；请勿把调试签名当作发布签名。
- **设备矩阵有限**：仅在 API 37 模拟器上自动化验证，未在 API 24 真机或多种厂商 ROM 上回归；平板 / 折叠屏没有专门布局。相册部分授权、相机权限拒绝等系统分支主要靠手动路径验证。
- **无障碍只做了基础检查**：触控目标、语义标签与对比度已处理，但没有完成 TalkBack 全流程和 200% 字体缩放的系统审计。
- **视觉没有真实模糊或光晕**：所有层次来自纯色块、发丝描边与反色对比，没有 backdrop blur、渐变光斑或内发光；这是为跨版本一致性与渲染成本做的取舍，也让截图与真机表现一致。
- **截图非像素级设备截图**：2.0 截图由 Compose 测试捕获，系统栏不在画面内；2.1、2.2、2.3、2.5、2.6 与 2.7 截图来自模拟器真实截屏，包含系统状态栏、权限弹窗与登录页。2.6 / 2.7 的书架与书页截图直接捕获弹窗内容节点，不含状态栏与遮罩。2.5 的 AI 对话使用临时账号与真实 Coze 回复，其余截图使用演示数据，不代表真实使用量。
- **首页首屏折叠**：411dp 高度下，首屏完整展示标题、双环星轨与焦点卡上半部，焦点卡的「完成」按钮需要向下轻滑才完全露出。这是为保留完整星轨直径所做的取舍，也是明确的滚动提示；悬浮导航本身不遮挡内容，`Scaffold` 已为其预留底部空间。
- **没有性能基准**：未接入 Macrobenchmark，也未测内存或后端压测。
- **未初始化 Git 仓库、未配置 CI**：当前目录只有 `.gitignore`；提交历史和持续集成需要另行补齐。

## Roadmap

1. 第二期能力：成就 / 徽章、专注计时、每日回顾；AI 规划与周复盘已在 2.4 落地、对话在 2.5 落地，下一步补设备端的计划导入 / 复盘卡回归、流式输出与计划质量评测。
2. 提醒：WorkManager / AlarmManager、通知渠道与权限流程，明确「非精确提醒」的边界。
3. uniCloud 上线收尾：补 Android 真机 E2E 的注册、同步冲突、头像与 AI 计划 / 复盘，再补自定义域名与限流；为 AI 对话补历史持久化与配额刷新。
4. 云同步工程化：增量同步 / 字段级合并、UUID 客户端主键、令牌刷新、账号注销；公开部署补验证码与备份策略。
5. 数据可携带：JSON 或 Markdown 导出 / 导入，配合备份说明。
6. 发布工程化：Release 签名、版本策略、CI 构建与真机 / API 24 回归。
7. 大屏与无障碍：平板布局、TalkBack 流程、字体缩放与动态颜色边界。
8. 性能：Macrobenchmark 与 Canvas 重绘基线；在长列表、大量节点和高 revision 数据下验证。

## 作品集说明

这个项目重点展示六件事：

- 用 **Jetpack Compose + Canvas** 做非标准交互，而不是套模板：星轨的几何 / 命中 / 吸附、目标星系、习惯热力图与等级环需要和数据状态一起成立。
- 用 **Room + Repository + ViewModel + StateFlow** 管理三个互相引用的领域（任务、目标、习惯）与一份追加式经验流水，保持单一数据源，并在删除、改期、重复操作与迁移等边界上自洽。
- 用 **Spring Boot + MySQL + Retrofit** 打通一套可离线使用的账号与同步链路：客户端始终读 Room，登录后由同步管理器推拉快照；BCrypt 密码、Bearer 令牌、revision 冲突与账号归属隔离都在真实设备上验证过。2.3 又用 **uniCloud 支付宝云函数 + 云数据库 + 云存储** 实现同一契约，云函数与集合已部署到 `mint`。
- 用 **Coze Bot + 云函数 AI 网关** 把大模型接成可交付的功能：提示词版本化、相对日期换算、结构化输出校验与一次修复、按账号每日配额、超时 / 繁忙 / 未配置降级；Coze 令牌只存在云函数环境变量，客户端只认 Orbit 自己的接口。
- 把 **可解释性** 当成产品需求：统计口径写进文档与纯函数测试，提醒没做就不放开关，经验不扣回、不重复发放，迁移失败不静默清库，同步冲突会明确采用云端版本；AI 只生成草稿，导入后是普通任务，失败不会清空表单。
- 通过 **114 个 JVM 测试、47 个设备断言、2 个服务端集成测试与 55 个云函数单测** 证明关键行为，而不是只凭截图判断完成；2.5 的 AI 对话还额外用真实 Coze 回复与逐次递减的配额做了端到端核对，2.6 的学习列表底部用相隔 1.2 秒的逐像素帧比对代替「看起来不抖」，2.7 的词库数量、长诗折叠、空字段降级与助手球透明度分别有 JVM / 语义断言覆盖。

本项目为个人练习与作品集用途，未附开源许可证。
