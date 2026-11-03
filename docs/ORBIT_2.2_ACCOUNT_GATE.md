# Orbit 2.2 · 强制登录、昵称与星轨完成

2.2 的目标不是再加一个设置项，而是换掉应用入口：没有账号就没有主界面，业务数据以 MySQL 账号为准，Room 退到离线缓存的位置。同时补齐两处体验——首页招呼语使用用户自定义昵称，星轨支持把节点拖进核心直接完成任务。

- 版本：`2.2.0`（versionCode 4）
- 包名：`com.example.orbit`
- 验证设备：Pixel 7 Pro 模拟器，API 37 / Android 17，1440×3120，density 560 = 411dp
- 后端：Spring Boot 4.1.2 + MySQL 8.4.9（本机 8080）

## 1. 入口重构

2.1 的登录卡片在「我的」页内部，未登录时仍可浏览本机数据。2.2 把账号层提到根节点：

```text
OrbitRoot
├── session == null      → AuthScreen（独立登录 / 注册页）
├── initialSyncing       → SyncBootScreen（等待 MySQL 快照）
└── 其他                 → Scaffold + 五个标签
```

- `AuthScreen` 独占整屏：品牌区、登录 / 注册分段、用户名、密码、（注册时）确认密码、提交按钮与「登录后自动从 MySQL 拉取」说明。没有底部导航，也没有可返回的游客路径。
- 表单校验与 2.1 一致：用户名 `[A-Za-z0-9_]{3,24}`，密码 6–64 位，注册要求两次一致；错误就地展示，不弹假成功。
- 登录 / 注册提交前会检查网络目标：若 Base URL 是局域网 / 本机地址（如 `http://10.0.2.2:8080/`），Android 17 会先要求 `ACCESS_LOCAL_NETWORK`（「附近的设备」），未授权则弹窗，拒绝会以超时静默失败；公网 HTTPS（当前 Cloudflare 隧道）跳过该权限，登录与同步都不再弹「附近的设备」。判断集中在 `requiresLocalNetworkPermission(baseUrl)`，并有 3 个 JVM 用例覆盖公网域名 / 公网 IP / 私网 / 回环 / mDNS / IPv6 / 畸形输入。

### 登录时序

1. `AccountViewModel.submit()` 校验表单。
2. 调用注册或登录 API，拿到 30 天令牌与用户资料。
3. `OrbitSyncManager.prepareForAccount(userId)`：在 `Dispatchers.IO` 下清空不属于该账号的 Room 行与归属；`clearAllTables()` 不再阻塞主线程。
4. `AccountStore.saveSession()` 保存令牌、用户、归属与 revision。
5. `syncManager.syncNow()` 拉取云端快照并覆盖本机；成功后 `initialSyncing = false`，主界面出现。
6. 如果拉取失败，仍然放行进入应用，但缓存只可能包含该账号自己的数据（第 3 步已清空），同步状态显示离线。

> 这里修过一次真实设备崩溃：注册成功时 `prepareForAccount` 从 `viewModelScope`（Main）调用，Room 抛出 `Cannot access database on the main thread`。修复是把 `prepareForAccount` / `clearLocalCache` / `syncNow` 全部包进 `withContext(Dispatchers.IO)`，并新增仪器测试 `cacheResetIsSafeFromTheMainThread`，在 Main 调度器上直接调用 `prepareForAccount(424242)` 验证不再抛异常。

### 退出与过期

- 退出登录：先 `store.clearSession()` 让门禁立即生效，UI 回到 `AuthScreen`；随后异步 `clearLocalCache()` 与 `POST /api/auth/logout`。清理期间不阻塞用户。
- 401：清会话、清缓存，提示「登录已过期，请重新登录」。
- 网络失败：保留会话与缓存，同步状态变为离线并保留上次成功时间；不把网络错误伪装成退出。

## 2. MySQL 权威与缓存归属

`OrbitSyncManager.performSync()` 的决策表：

| 本机归属 | 本地 dirty | 行为 |
| --- | --- | --- |
| 无归属 / 其他账号 | 任意 | 拉取云端快照并覆盖本机，空快照也清空；不上传 |
| 当前账号 | true | 以 `baseRevision` 推送整包快照 |
| 当前账号 | false | revision 更大则采用云端，否则标记已同步 |
| 推送返回 409 | — | 用服务端返回的快照覆盖本机，状态显示冲突 |

- `Room.clearAllTables()` 会清六张业务表，并 `resetLocalOwnership()`；`localOwnerUserId`、`syncRevision`、`localDirty` 一并复位。
- 安装后、登录前的本地行会被有意丢弃。2.1 曾允许「云端 revision 为 0 时上传本机数据」，2.2 取消了这个例外：没有账号的缓存不可信。
- 登录、退出、切换账号、令牌过期都复用同一套清理，避免上一个账号的数据泄漏给下一个账号。
- UI 仍然只读 Room，因此首页、列表与统计不需要网络回调；唯一等待网络的位置是登录门禁。

## 3. 首页昵称

- `OrbitRoot` 计算 `displayName = nickname?.takeIf { it.isNotBlank() } ?: username ?: "宇航员"`，同时传给首页与同步启动页。
- 问候语字符串从固定文案改为 `%1$s`：例如「夜深了，NovaPilot」；昵称超过一行时用省略号收尾，不挤压右侧日期。
- 昵称修改成功后，`AccountStore` 更新用户对象，首页立即重组，不需要重启或重新登录。
- 头像和昵称都写入 MySQL；重新登录或换设备时从 `/api/me` / 快照恢复。

## 4. 星轨互动

2.1 的拖动只做「换槽位」。2.2 把它扩成两种语义：

- **拖到任意空槽**：自由角度与半径跟随手指，半径限制在轨道宽度的 46% 以内；松手后吸附到最近空槽，写回只影响本次显示的槽位，带震动反馈。
- **拖入核心**：核心半径为星图宽度的 20%。当拖动点进入核心，节点缩小、核心亮度提升、提示文案切换为「松手点亮」；松手调用 `onCompleteTask(task.id)`，走与详情面板「完成」完全相同的 Repository 事务。
- 完成后的动画链：核心脉冲 → 粒子迸发 → 星座连线 → 一次性彗星；所有动画都是有限时长，并遵循系统「移除动画」设置，保证 Compose 测试可以正常 idle。
- 已完成节点不能再拖入核心；拖动半径仍受外缘限制，避免节点被拖出画布。
- 纯几何判断集中在 `OrbitLayout`：`CORE_RADIUS_FRACTION = 0.20f`、`MAX_DRAG_RADIUS_FRACTION = 0.46f`、`radiusFractionForPoint()` 与 `isOverCore()`，均有 JVM 边界测试。

一次拖入核心的真实链路：

```text
TaskOrbit 手势 → onCompleteTask(id)
  → TaskRepository.complete(id)
      → Room：completedAt = now
      → GrowthLedger：TASK_COMPLETED，10 XP（高优先级 +5）
  → OrbitSyncManager：1.2 秒防抖后 PUT /api/sync
  → MySQL：sync_tasks.completed_at、sync_growth_events、revision + 1
```

## 5. 资料与系统权限

- 昵称：点击资料卡铅笔就地编辑，1–24 字符，空白被拦下；保存后显示「昵称已更新」。
- 头像入口：底部面板提供「从相册选择」与「拍照上传」两条路径。
- 相册：未授权时先请求 `READ_MEDIA_IMAGES`（Android 14+ 同时声明 `READ_MEDIA_VISUAL_USER_SELECTED`，Android 12 及以下回退 `READ_EXTERNAL_STORAGE`），系统对话框提供「Allow limited access / Allow all / Don’t allow」，随后打开系统照片选择器。
- 相机：未授权时请求 `CAMERA`，系统对话框提供「While using the app / Only this time / Don’t allow」；拒绝后 Snackbar 提示「需要相机权限才能继续上传」，头像保持不变，并可去设置。
- 上传链路：EXIF 方向校正、最长边 1024、JPEG 88，multipart 到 `/api/me/avatar`；服务端校验类型与 5 MB 上限，路径写入 `app_users.avatar_path`，客户端用 Coil 加载。

## 6. 文案修正

沿用 2.1 的「本机优先」文案在 2.2 会误导，以下字符串已改为账号权威：

| 位置 | 2.2 文案 |
| --- | --- |
| 任务编辑器说明 | 任务会写入本机缓存并自动同步到你的 MySQL 账号，可随时编辑或删除。 |
| 删除任务确认 | “…”将从你的 MySQL 账号与本机缓存中移除，此操作无法撤销。 |
| 习惯页说明 | 打卡记录会同步到你的 MySQL 账号；同一天只会记录一次，连续天数按云端记录计算。 |
| 任务加载 / 目标缺失 | 去掉「本机」前缀，避免把云端数据说成本地数据 |

## 7. 测试

| 套件 | 结果 | 覆盖 |
| --- | --- | --- |
| `:app:testDebugUnitTest` | 66 / 66 | 领域规则、统计口径、排序、`OrbitLayout` 核心命中与拖动半径边界、账号与同步规则、`requiresLocalNetworkPermission` 公网 / 私网判定 |
| 设备仪器测试 | 45 / 45 | 登录门禁、昵称招呼、拖入核心完成、清缓存主线程回归、Room / 迁移 / 偏好 / 既有 UI 流程 |
| 服务端 `./mvnw test` | 2 / 2 | 认证 / 资料 / 头像、快照推送与 revision 冲突（H2 MySQL 模式） |

新增或重点修改：

- `AuthGateTest`（3 项）
  - `signedOutLaunchShowsOnlyTheAuthGate`：无会话启动只出现登录页，断言底部标签与星轨节点数量为 0；切到注册后出现确认密码。
  - `seededNicknameGreetsTheUserOnHome`：注入 `NovaPilot` 会话后，首页出现昵称招呼且不再出现登录页。
  - `cacheResetIsSafeFromTheMainThread`：在 Main 调度器调用 `prepareForAccount`，归属必须被重置且不抛 Room 主线程异常。
- `OrbitInteractionTest.droppingNodeIntoCoreCompletesTheTask`：创建高优先级任务，把节点拖入核心，等待进度 100%（1/1），断言任务已完成、没有打开详情面板、节点语义变为已完成。
- `OrbitLayoutTest.coreTargetAndDragRadiusStayInsideTheOrbit`：核心边界（中心、0.9 倍核心半径为真，1.1 倍为假）、拖动半径（0.25、钳制到 0.46、零宽度）。
- 服务端 `snapshotPushAndRevisionConflict` 增加第二次同账号整包推送：同一批 `client_id` 更新完成状态，断言 revision 2、任务完成时间与两条成长流水。顺带修复了六个 Repository 的批量删除（`@Modifying @Query`），避免 Hibernate 先插入后删除触发唯一键冲突。

设备测试使用自定义 `OrbitTestRunner`：在 `callApplicationOnCreate()` 之后调用 `TestSession.seed()`，写入 debug-only 的 `qa_offline` 会话，`OrbitSyncManager` 见到标记就跳过全部网络请求。Release 构建中 `seedDebugSession` 被 `BuildConfig.DEBUG` 拒绝。

## 8. 真实设备端到端

账号 `nova_1027`（用户 id 3），密码仅用于本机演示：

| 步骤 | 结果 |
| --- | --- |
| 清数据启动 | 只看到独立登录页；没有标签栏 |
| 注册 / 登录 | 局域网构建下触发 Android 17「附近的设备」系统弹窗；允许后进入主界面（公网构建跳过该权限，见下节） |
| 创建任务 | `Orbit-2.2-E2E` 写入 Room 并推送 MySQL |
| 拖入核心 | 首页 100%（1/1），任务完成，`completed_at` 写入，成长 +10 XP |
| 修改昵称 | `NovaPilot` 写入 `app_users.nickname`，首页问候立即更新 |
| 相册头像 | 系统照片权限弹窗 → 照片选择器 → 上传；`avatar_path` 写入 `/media/avatars/7be9d06aa63f47c3ba871420b46c6aa6.jpg` |
| 相机头像 | 系统相机权限弹窗；选择拒绝后 Snackbar 提示，头像不变 |
| 退出登录 | 确认后立即回到登录页；令牌异步注销，Room 清空 |
| 重新登录 | 只靠 `GET /api/sync` 恢复任务、昵称、头像与 revision 2 |

MySQL 最终状态：

```text
app_users:            id=3, username=nova_1027, nickname=NovaPilot,
                      avatar_path=/media/avatars/7be9d06aa63f47c3ba871420b46c6aa6.jpg
sync_tasks:           user_id=3, client_id=1, title=Orbit-2.2-E2E,
                      completed_at=1793062610707
sync_growth_events:   user_id=3, client_id=1, type=TASK_COMPLETED,
                      source_key=task:1, xp=10, epoch_day=20753
sync_revisions:       user_id=3, revision=2
```

截图（`docs/screenshots/phase10/`，13 张真实设备截图）：

| 文件 | 内容 |
| --- | --- |
| `01-auth-gate.png` | 无会话时的独立登录页 |
| `02-local-network-dialog.png` | Android 17 附近的设备权限 |
| `03-task-created.png` | 今日任务已创建 |
| `04-drag-complete.png` | 拖入核心后 100% |
| `05-profile-account.png` | 资料卡与云同步状态 |
| `06-nickname-editor.png` | 昵称就地编辑 |
| `07-nickname-greeting.png` | 首页昵称招呼 |
| `08-avatar-permission.png` | 照片权限系统弹窗 |
| `09-avatar-updated.png` | 相册头像上传成功 |
| `10-camera-permission.png` | 相机权限系统弹窗 |
| `11-logout-gate.png` | 退出后回到登录门禁 |
| `12-relogin-restored.png` | 重新登录恢复云端数据 |
| `13-public-network-login.png` | 撤销本地网络权限后，经公网 HTTPS 重新登录并恢复首页 |

### 公网访问验证（Cloudflare Quick Tunnel）

为了让不连同一局域网的手机也能使用，后端通过 Cloudflare Quick Tunnel 暴露为 HTTPS：

```bash
cloudflared tunnel --url http://127.0.0.1:8080 --no-autoupdate
# → https://politics-std-loved-equipment.trycloudflare.com/
```

客户端 `BuildConfig.ORBIT_API_BASE_URL` 指向该 HTTPS 域名，重新构建并安装后验证：

| 步骤 | 结果 |
| --- | --- |
| 撤销权限 | `pm revoke com.example.orbit android.permission.ACCESS_LOCAL_NETWORK` → `granted=false` |
| 退出登录 | 回到独立登录页；本机缓存清空 |
| 公网登录 | `POST https://politics-std-loved-equipment.trycloudflare.com/api/auth/login` 200（742 ms），**没有**弹出「附近的设备」 |
| 公网同步 | `GET /api/sync` 200（730 ms + 683 ms），昵称 `NovaPilot`、任务与完成状态恢复，首页 100%（1/1） |
| 证据 | `13-public-network-login.png`；OkHttp 日志见提交说明 |

限制：Quick Tunnel 依赖这台电脑、Spring Boot 与 `cloudflared` 进程持续运行；免费隧道每次重启会换域名，需要重新改 Base URL 构建。要长期稳定公网服务，应把后端部署到云服务器并绑定固定域名。

## 9. 构建产物

- 2.2 历史安装包（公网版与局域网 / 模拟器版）已按清理要求删除，不再提供下载路径。
- 当前保留的 2.2.0 客户端刷新包（含 JSON 头像，仍连 Spring / MySQL）见项目根目录 `README.md` 的「构建产物」。
- Debug 签名，仅用于演示；工程未配置 Release 签名。

## 10. 已知边界

- 只有在线账号：没有找回密码、邮箱 / 手机验证、账号注销或第三方登录；令牌 30 天，服务端存原文。
- 公网入口是 Cloudflare Quick Tunnel：依赖本机后端与隧道进程常开，重启即换域名；后端本身仍是 HTTP + 本地开发口令，长期部署前需要 TLS、限流、验证码、令牌哈希与对象存储。
- 登录门禁等待一次快照拉取；网络很慢时启动页会停留，失败才放行离线。没有本地「离线进入」开关。
- 同步仍是整包快照，没有字段级合并、增量日志或 UUID 主键；两台设备同时离线新建仍可能撞 `client_id`。
- 星轨槽位与拖动坐标不持久化；拖入核心只写完成状态。
- 截图来自 API 37 模拟器，未覆盖 API 24 真机与厂商 ROM 的权限对话框差异。
