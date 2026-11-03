# Orbit 2.3 · uniCloud（支付宝云）后端

2.3 把账号与同步后端从「本机 Spring Boot + MySQL + Cloudflare 隧道」迁到 uniCloud 支付宝云：云函数 URL 化承接 HTTP，云数据库保存账号、令牌与快照，云存储保存头像。Android 的 REST 契约、Room 缓存、revision 冲突与登录门禁都不变，客户端只换了 Base URL 与头像上传的编码方式。

- 代码状态：云函数、客户端适配、Spring 兼容端点、迁移脚本与文档已完成
- 测试状态：Node 29 / JVM 66 / 设备 46 / Spring 2 全部通过；迁移脚本通过本地假后端端到端验证
- 部署状态：云函数 `orbit-api` 与三个集合 / 索引已上传到服务空间 `mint`（`env-00jy6ydc58ll`）；URL 化 PATH 已配为 `/orbit`，入口 `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit` 已通过真实 HTTP 验证
- 版本状态：`2.3.0`（versionCode 5）；默认 Base URL 已指向 `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`

## 1. 为什么换，换到哪

原架构要求这台电脑常开、Spring Boot 与 `cloudflared` 同时运行，Quick Tunnel 重启还会换域名。uniCloud 支付宝云把运行环境、数据库、对象存储和 HTTPS 入口放在云上，不再依赖本机进程，也不需要维护 MySQL。

目标是「不重写客户端」：请求路径、JSON 字段、Bearer 令牌、409 冲突语义全部沿用 2.2。这样迁移风险集中在服务端，客户端只是构建参数和一个上传端点变化。

```text
Android（Retrofit + OkHttp）
  │  https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/api/...
  ▼
uniCloud 云函数 URL 化
  ▼
orbit-api（Node.js，零 npm 依赖）
  ├── 认证：scrypt 密码、SHA-256 令牌、30 天过期
  ├── orbit_users / orbit_tokens
  ├── orbit_snapshots（每个账号一份整包快照 + revision CAS）
  └── 云存储：头像文件 → /media/avatars/<key> 302 到临时地址
```

## 2. 客户端改动

### 2.1 Base URL 构建期可配置

`app/build.gradle.kts` 从 Gradle 属性读取地址，缺省指向本次部署的 mint 空间：

```kotlin
val orbitApiBaseUrlValue: String = providers.gradleProperty("orbitApiBaseUrl")
    .orElse("https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/")
    .get()
    .let { if (it.endsWith("/")) it else "$it/" }
val orbitApiBaseUrl = orbitApiBaseUrlValue
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
buildConfigField("String", "ORBIT_API_BASE_URL", "\"$orbitApiBaseUrl\"")
buildConfigField("String", "ORBIT_BACKEND_LABEL", "\"$orbitBackendLabel\"")
```

```bash
./gradlew :app:assembleDebug -PorbitApiBaseUrl="https://<spaceId>.dev-hz.cloudbasefunction.cn/orbit/"
```

- 自动补尾部 `/`，避免 Retrofit 相对路径拼接丢前缀。
- 反斜杠与双引号会被转义后再写进 `BuildConfig`。
- 公网 HTTPS 地址不会触发 Android 17 的 `ACCESS_LOCAL_NETWORK`；`10.0.2.2` 与局域网 IP 仍然会。
- `BuildConfig` 仍按地址生成 `ORBIT_BACKEND_LABEL`（含 `bspapp.com` / `cloudbasefunction.cn` 时为 `uniCloud · 支付宝云`，否则为 `Spring Boot · MySQL`），可用 `-PorbitBackendLabel` 覆盖；本次文案收短后登录页不再展示技术规格，该字段只用于构建排查与后续界面。
- 已核对生成物：缺省为 `"https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/"` + `"uniCloud · 支付宝云"`，版本 `2.3.0`（code 5）；自定义域名加 `-PorbitBackendLabel` 时标签按覆盖值生成。
- 支付宝云控制台给出的默认域名为 `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn`，URL 化 PATH 配成 `/orbit` 后完整入口即 `.../orbit`；`lib/http.js` 的 `normalizePath` 同时兼容完整路径、相对路径与老网关的 `/http` 段。

### 2.2 头像改为 JSON base64

uniCloud URL 化不能可靠转发 multipart，客户端改为把压缩后的 JPEG 编成 base64 放进 JSON：

```json
POST /api/me/avatar
{"contentBase64":"...","contentType":"image/jpeg"}
```

- 客户端先压缩到最长边 768 px、JPEG 82，再检查原始文件不超过 1.4 MB；超限在本地直接提示，不浪费一次上传。
- 服务端解码后再次检查 1.4 MB；支付宝云 URL 化的请求体上限为 32 MB，头像远低于该值。
- 服务端仍返回 `/media/avatars/<32 位十六进制>`；图片组件照旧用同一 Base URL 加载，云函数 302 到云存储临时地址。
- 为保持局域网调试能力，Spring 同时新增了同路径的 JSON 端点，原来的 multipart 端点保留。

### 2.3 文案与测试

「MySQL」相关界面文案统一改成「云端」；登录页原先前端标识随构建目标切换，本次文案收短后与「后端 / 令牌 / 缓存」三行规格及底部细字一起移除。

设置页「关于」只保留应用名、一句简介与版本号，数据说明改为「数据保存在云端；退出登录会清空本机缓存，重新登录即可恢复」。账号卡的退出登录从一行小字改成整行红色描边按钮（含图标与「退出后可使用其他账号登录」提示），确认后回到登录页即可换号。仪器测试 `SettingsPersistenceTest`、`ScreenshotGalleryTest` 的断言与滚动目标同步更新，另新增一条从账号卡退出并回到登录门禁的设备测试。

## 3. 云函数实现

目录：

```
unicloud/cloudfunctions/orbit-api/
├── index.js              # uniCloud 入口：注入数据库、存储、URL 前缀
├── lib/app.js            # 路由、鉴权、注册登录、资料、快照、头像
├── lib/store.js          # 三个集合的查询与条件更新
├── lib/sync.js           # 快照校验、字段白名单、稳定排序、409 响应
├── lib/crypto.js         # scrypt 哈希 / 校验、SHA-256、随机数
├── lib/http.js           # URL 化事件解析、CORS、组合响应
├── test/                 # 29 项 node --test
└── package.json          # dependencies 为空
```

### 3.1 认证

- 密码使用 Node 内置 `crypto.scryptSync`：N=16384、r=8、p=1、keylen=64，存储格式 `scrypt$N$r$p$saltHex$hashHex`，参数随哈希保存，后续可提高强度。
- 未知用户名也会跑一次固定参数的假哈希再返回 401，避免通过响应时间枚举账号。
- 令牌是 32 字节随机数的十六进制串，数据库只保存 SHA-256；30 天过期，登录与校验时顺带清理过期行。
- 登出删除当前令牌；401 的响应体保持 `{"code":"UNAUTHORIZED","message":"..."}`，客户端沿用「清会话 + 清缓存 + 提示重新登录」。

### 3.2 快照与 revision

每个账号在 `orbit_snapshots` 只有一行，查询按 `userId`。首次推送用 `add`，`userId` 唯一索引保证两台设备同时首次上传只会成功一台；已有行用 `where({_id, revision}).update(...)` 做比较并交换，更新数为 0 就说明 revision 已被别的设备推进。

```js
const current = await store.getSnapshot(userId)
const currentRevision = current ? Number(current.revision) || 0 : 0
if (request.baseRevision !== currentRevision) return json(409, snapshotFromDoc(current))
const accepted = await store.updateSnapshotIfRevision(current._id, currentRevision, next)
if (!accepted) {
  const latest = await store.getSnapshot(userId)
  return json(409, snapshotFromDoc(latest))
}
```

- 409 的 body 是原始快照，不带 `code/message` 包装，和 Spring 行为一致；客户端据此直接覆盖本机。
- 入库前做字段白名单、数组长度上限（每类 5000）与稳定排序：目标按 `sortOrder/id`，阶段按 `goalId/position`，任务、习惯、打卡与成长流水按 id，保证同一快照在不同设备上顺序一致。
- 数字 id 原样保存，不做数据库主键映射；`_id`、`userId`、内部 `updatedAt` 在返回前剔除。

### 3.3 头像与云存储

1. 解析 base64，先按请求体上限拒绝，再按解码后 1.4 MB 拒绝。
2. 只接受 `image/jpeg`、`image/png`、`image/webp`，映射扩展名；其他返回 415 `AVATAR_TYPE`。
3. 生成 16 字节随机 key，`uniCloud.uploadFile` 到 `orbit/avatars/<userId>-<key>.<ext>`，拿到 `fileID`。
4. 更新账号行：`avatarPath=/media/avatars/<key>`、`avatarKey`、`avatarFileId`；上传成功后删除旧 `fileID`，删除失败只忽略，不阻断请求。
5. `GET /media/avatars/<key>` 按 key 反查账号，`getTempFileURL` 后 302；未知 key 返回 404，不暴露文件列表。

### 3.4 路由与路径前缀

| 方法 | 路径 | 鉴权 |
| --- | --- | --- |
| GET | `/api/health` | 否 |
| POST | `/api/auth/register` / `login` / `logout` | 否 / 否 / Bearer |
| GET / PATCH | `/api/me` | Bearer |
| POST | `/api/me/avatar` | Bearer |
| GET / PUT | `/api/sync` | Bearer |
| GET | `/media/avatars/<key>` | 否 |
| OPTIONS | 任意 | 否 |

URL 化的 `event.path` 在不同网关与控制台配置下可能是 `/orbit/api/health`，也可能只剩 `/api/health`；老网关还会带 `/http` 段（`/http/orbit/api/health`）。`normalizePath` 先剥掉 `/http`，再匹配默认前缀 `/orbit`，三种形式都能命中；前缀可用环境变量 `ORBIT_URL_PREFIX` 覆盖。响应统一带 CORS 头。退出登录返回 `200 {"status":"ok"}` 而不是 204：支付宝云 URL 化运行时拒绝 204 响应，令牌删除本身不受影响。

## 4. 数据与索引

| 集合 | 内容 | 索引 |
| --- | --- | --- |
| `orbit_users` | `userId`、`username`、`passwordHash`、`nickname`、头像路径与 `fileID` | `username` 唯一、`userId` 唯一、`avatarKey` 普通 |
| `orbit_tokens` | `tokenHash`、`userId`、`createdAt`、`expiresAt` | `tokenHash` 唯一、`expiresAt` 普通 |
| `orbit_snapshots` | `userId`、`revision`、六类数组、`updatedAt` | `userId` 唯一 |

集合权限设为「仅云函数可读写」，客户端不直接访问数据库或 clientDB。缺索引时功能仍可跑，但并发注册与并发首次上传不再有数据库层兜底。

## 5. 测试与已验证内容

| 项目 | 结果 | 说明 |
| --- | --- | --- |
| `node --test`（云函数） | 29 项通过、0 失败 | scrypt、令牌、路径前缀（含支付宝 `/http` 网关段）、base64 请求体、注册 / 登录 / 登出 / 过期、昵称、快照 revision 与 409、头像与跳转、错误体 |
| `:app:testDebugUnitTest` | 66 项通过、0 失败 | 客户端改动后的 JVM 回归 |
| `:app:connectedDebugAndroidTest` | 46 项通过、0 失败 | 登录门禁、新增「账号卡退出 → 登录页」、文案断言、既有 Room / UI / 迁移用例；Pixel 7 Pro API 37 |
| `./mvnw test`（Spring） | 2 项通过、0 失败 | JSON base64 头像与畸形 base64 断言 |
| Gradle 属性 | 通过 | 缺省 URL 指向 mint 空间、`cloudbasefunction.cn` 自动识别、自定义域名加 `-PorbitBackendLabel` 均核对了生成的 `BuildConfig.java`，尾部 `/` 自动补齐 |
| 迁移脚本 | 通过（本地假后端） | 旧快照 revision 7 导出；新后端注册、`baseRevision=0` 推送、返回 revision 1，目标 / 阶段 / 任务 / 成长记录均保留 |
| 云函数上传 | 通过 | `orbit-api` 已上传到 `mint`（`env-00jy6ydc58ll`），HBuilderX CLI 返回「云函数orbit-api上传完成」 |
| 集合与索引 | 通过 | `orbit_users` / `orbit_tokens` / `orbit_snapshots` 已建，唯一索引与普通索引已配 |
| URL 化 / HTTP 入口 | 通过（2026-10-28） | `GET /orbit/api/health` 返回 200 + `{"status":"ok","backend":"unicloud"}`；控制台 PATH 为 `/orbit` |
| 云端 HTTP E2E | 通过（2026-10-28） | 真实空间上依次验证：注册 201、昵称 200（中文正确）、空快照 revision 0、推送 revision 1、过期 revision 409、头像上传 200 → 媒体跳转 → 云存储 200 image/png、退出 200 且旧令牌 401、重新登录后昵称 / 头像 / 快照仍在 |
| 真机 E2E | **未执行** | 注册、昵称、快照、头像四条链路尚未在 Android 客户端上跑过；46 项设备测试走的是测试会话旁路 |

本地测试使用内存 store，不接触真实云数据库；它验证业务逻辑与响应格式，不验证云厂商 API 的实际返回结构。

## 6. 部署进度

完整步骤、控制台路径与 curl 检查在 [`unicloud/README.md`](../unicloud/README.md)。本次已执行：

1. 通过 HBuilderX 5.26 CLI 把 `orbit-api` 上传到 `mint`（`env-00jy6ydc58ll`，支付宝云），返回「云函数orbit-api上传完成」。
2. 在控制台创建 `orbit_users`、`orbit_tokens`、`orbit_snapshots` 三个集合与索引，权限设为仅云函数可读写。
3. 版本提升到 `2.3.0`（code 5），默认 Base URL 指向控制台实际域名 `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/`，导出 APK 与 phase11 截图。
4. uniCloud 控制台 → 云函数 → `orbit-api` → 详情 → 「云函数URL化」→「编辑」，PATH 填 `/orbit` 并保存；完整入口为 `https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit`。
5. 真实 HTTP 联调发现支付宝云 URL 化运行时拒绝 204：退出登录虽已删令牌，但客户端会收到 500。云函数改为返回 `200 {"status":"ok"}`，重新上传后回归通过。

接下来只剩 Android 真机 E2E：用 2.3.0 APK 注册、改昵称、触发一次同步冲突、上传头像，并确认重新登录后数据仍在。curl 侧的 health → register → login → nickname → sync → 409 → avatar → logout 已全部通过。

## 7. 从 MySQL 迁移

密码哈希无法迁移：旧后端是 BCrypt，新后端是 scrypt。迁移工具会要求给新账号设置新密码，快照则原样搬运：

```bash
node unicloud/tools/migrate.mjs \
  --from https://<旧后端>/ \
  --to https://<URL化域名>/orbit/ \
  --dump orbit-snapshot.json
```

- 先登录旧后端读 `GET /api/sync`，可选写出本地 JSON 备份。
- 再注册或登录新账号，读取当前 revision，以它作为 `baseRevision` 推送。
- 新账号已有非空快照时默认拒绝，确认覆盖要加 `--force`；`--dry-run` 只导出。
- 密码在终端交互输入、不会写进命令历史，但会明文显示；请在私人终端执行。
- 迁移后旧后端与旧 APK 仍可用，回滚只需继续用旧 Base URL 构建，不需要改代码。

## 8. 已知限制

- 支付宝云 URL 化的请求 / 响应体上限为 32 MB（官方文档）；整包快照没有分页，超大成长流水或打卡历史仍可能触顶。本轮没有做大体积压测。
- 单类数据上限 5000 行，与 Spring 一致；同步仍是整包覆盖，没有字段级合并。
- 登录接口没有服务端限流；云函数实例无共享内存，简单计数器不可靠。公开部署应在控制台或网关层加频率限制。
- 没有找回密码、邮箱验证、账号删除；头像临时地址按云存储默认有效期，过期后重新访问媒体路径即可换新。
- 支付宝云默认 URL 化域名多个空间共享限流池，长期使用建议绑定已备案的自定义域名。
- `uploadFile({fileContent})`、`getTempFileURL` 与媒体跳转已在真实空间验证；更大的请求体、并发注册与并发首次上传仍只有单测覆盖。
- Android 客户端尚未在真机上跑完整 E2E；2.3.0 APK 已指向 mint 空间，2.2.0 旧包仍指向 Cloudflare 隧道，两个包的 Base URL 不同，不要互相推断。
