# Orbit 2.1 · 账号、云端同步与头像

本文档记录 Orbit 2.1.0 的账号体系、MySQL 同步协议、头像上传链路与系统权限设计，以及本机验证方式。它补充 README，不替代 README 的产品说明。

## 1. 目标与边界

- Android 端保留 Room 作为唯一 UI 数据源，离线可继续使用。
- 登录后，目标、阶段、任务、习惯、打卡与成长流水同步到自己的 MySQL 账号。
- 「我的」页支持修改昵称、从相册选择头像或调用系统相机拍照，并展示同步状态。
- Android 不直接连接 MySQL。数据路径固定为：

```text
Compose UI → ViewModel → Repository → Room
                          ↘ OrbitSyncManager → Retrofit/OkHttp → Spring Boot → JPA/Hibernate → MySQL
```

- 后端只处理账号、资料、头像与快照同步；不承担业务统计，统计仍由 Android 的纯 Kotlin 聚合完成。
- 本站为个人作品集与本地开发环境：HTTP 明文、开发密码、单机头像目录，不适用于公网生产。

## 2. 客户端结构

```text
app/src/main/java/com/example/orbit/
├── OrbitApplication.kt              # 启动时初始化账号图
├── account/
│   ├── AccountModels.kt             # 登录态、快照 DTO、同步状态
│   ├── OrbitApi.kt                  # Retrofit 接口与超时、头像 URL
│   ├── AccountStore.kt              # SharedPreferences：令牌 / 用户 / 归属 / revision
│   ├── AccountRepository.kt         # 认证、资料、头像、拉取 / 推送
│   ├── AvatarProcessor.kt           # EXIF 旋转、缩放 1024、JPEG 88
│   └── OrbitSyncManager.kt          # 防抖推送、拉取、冲突与远端覆盖
├── ui/AccountViewModel.kt           # 表单、昵称、头像、同步状态
└── ui/screens/AccountSection.kt     # 登录/注册卡、账号卡、权限与系统弹窗
```

### 登录态

`AccountStore` 保存：

| 键 | 含义 |
| --- | --- |
| `token` / `user` | 30 天 Bearer 令牌与用户资料 |
| `local_owner` | 本机缓存归属的 userId；`-1` 表示未归属 |
| `sync_revision` | 本机已确认的云端 revision |
| `local_dirty` | 本机是否有尚未推送的写入 |
| `last_sync` | 最近一次成功同步的时间 |

令牌只保存在应用私有 `SharedPreferences`，不写入日志。

## 3. 后端接口

Base URL 在 `app/build.gradle.kts` 中配置：

```kotlin
buildConfigField("String", "ORBIT_API_BASE_URL", "\"http://10.0.2.2:8080/\"")
```

`10.0.2.2` 是 Android 模拟器访问宿主机的地址；真机需要改成同一局域网内的电脑 IP，并把该 IP 加进 `res/xml/network_security_config.xml` 的明文白名单。

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/health` | 否 | 健康检查 |
| POST | `/api/auth/register` | 否 | 用户名 `[A-Za-z0-9_]{3,24}`，密码 6–64 位 |
| POST | `/api/auth/login` | 否 | 返回 30 天令牌 |
| POST | `/api/auth/logout` | 是 | 删除当前令牌，204 |
| GET | `/api/me` | 是 | 当前资料 |
| PATCH | `/api/me` | 是 | 昵称 1–24 字符 |
| POST | `/api/me/avatar` | 是 | multipart `file`，JPG/PNG/WEBP，≤5 MB |
| GET | `/media/avatars/{name}` | 否 | 头像静态文件 |
| GET | `/api/sync` | 是 | 拉取完整快照 |
| PUT | `/api/sync` | 是 | 携带 `baseRevision` 提交快照；版本不符返回 409 + 当前快照 |

鉴权头：

```http
Authorization: Bearer <64 位十六进制令牌>
```

错误体统一为：

```json
{ "code": "ACCOUNT_EXISTS", "message": "这个用户名已经被注册" }
```

常见错误码：`ACCOUNT_EXISTS`、`LOGIN_FAILED`、`UNAUTHORIZED`、`VALIDATION_FAILED`、`AVATAR_MISSING`、`AVATAR_TOO_LARGE`、`AVATAR_TYPE`、`SYNC_REVISION`、`SYNC_TOO_LARGE`。

## 4. MySQL 结构

数据库 `orbit`，字符集 `utf8mb4`，由 Hibernate `ddl-auto=update` 创建 / 升级：

| 表 | 内容 |
| --- | --- |
| `app_users` | 用户名（唯一）、BCrypt 密码哈希、昵称、头像路径、创建/更新时间 |
| `auth_tokens` | 64 位令牌、userId、创建与过期时间（30 天） |
| `sync_revisions` | 每个用户的当前 revision 与更新时间 |
| `sync_goals` / `sync_stages` | 目标与阶段，`(user_id, client_id)` |
| `sync_tasks` | 任务，含优先级、分类、截止日、完成时间与目标 / 阶段关联 |
| `sync_habits` / `sync_habit_records` | 习惯与每日打卡 |
| `sync_growth_events` | 追加式经验流水，`source_key` 去重 |

所有同步表以 `user_id` 隔离；`client_id` 是 Android Room 中的原主键，服务端用 `(user_id, client_id)` 重新写入，因此不同账号的 id 不会互相覆盖。单类数据上限 5000 行。

## 5. 快照同步协议

### 数据形状

一次快照包含六类数组：

```json
{
  "revision": 3,
  "goals": [],
  "stages": [],
  "tasks": [],
  "habits": [],
  "habitRecords": [],
  "growthEvents": []
}
```

推送额外带 `baseRevision`。服务端成功时 revision + 1；`baseRevision` 不等于当前 revision 时返回 409 与服务端当前快照。

### 客户端时机

| 时机 | 行为 |
| --- | --- |
| 登录 / 注册成功 | 立即拉取；云端为空且本机未归属时上传本机数据 |
| 本机 Room 六张表发生变化 | 标记 `local_dirty`，1.2 秒防抖后推送 |
| 应用启动且已有会话 | 拉取一次 |
| 「立即同步」 | 拉取一次；必要时推送 |
| 退出登录 | 清令牌，保留 Room 缓存、归属与 revision |

### 冲突与归属

- 服务端优先。推送返回 409 时，客户端用服务端快照覆盖本机并显示「云端已有更新，已采用云端版本」。
- `localOwnerUserId = -1` 表示本机数据尚未归属任何账号。首次登录时只有在云端 revision 为 0 时才上传；否则云端覆盖本机，避免把上一账号的数据带给新账号。
- 写入远端后设置 1.5 秒的 invalidation 抑制窗口，避免 Room 回调把刚拉下来的数据再次标脏。
- 恢复会话失败（401）时清会话并提示重新登录；网络失败保留会话，状态显示离线与最近同步时间。
- 单机为缓存、云端为权威，适合个人多设备低频切换；不提供实时协同或字段级合并。

## 6. 头像与权限

### 头像链路

1. 相册：`ActivityResultContracts.GetContent("image/*")`；相机：`TakePicture` + `FileProvider`（缓存的 `camera/` 目录）。
2. `AvatarProcessor` 读取 EXIF 方向，最长边缩到 1024，JPEG 质量 88，写入 `cacheDir/avatar-upload/`。
3. Retrofit multipart 上传到 `/api/me/avatar`。
4. 后端校验类型与大小，生成 UUID 文件名，保存到 `./data/avatars`，数据库记录 `/media/avatars/...`。
5. 客户端保存返回的用户资料，Coil 通过同一网络栈加载头像；加载失败保留首字母占位。

### 系统权限

| 权限 | 申请时机 | 说明 |
| --- | --- | --- |
| `INTERNET` | 安装时 | 正常权限 |
| `ACCESS_LOCAL_NETWORK` | 登录/注册提交前、手动同步前 | Android 17（API 37）起访问局域网必须声明并动态申请，否则 socket 静默超时；属于「附近的设备」组 |
| `CAMERA` | 点击「拍照上传」且未授权 | 运行时弹窗；拒绝后显示提示与「去设置」 |
| `READ_MEDIA_IMAGES` | 点击「从相册选择」且未授权 | Android 13+ |
| `READ_MEDIA_VISUAL_USER_SELECTED` | 同上，Android 14+ | 允许「仅选择部分照片」；获得部分授权也继续打开选择器 |
| `READ_EXTERNAL_STORAGE` | 同上，Android 12 及以下 | `maxSdkVersion=32` |

`<uses-feature android:name="android.hardware.camera" android:required="false" />` 保证没有相机的设备仍可安装。

### 已知网络坑

`ACCESS_LOCAL_NETWORK` 是 API 37 新增的运行时权限。仅声明清单、或只在 shell 里放行 appop 都不够；必须在用户点登录 / 同步时走系统运行时弹窗，授权后 OkHttp 才能连上 `10.0.2.2:8080`。清空应用数据会重置该授权，下一次登录会再次弹窗。

## 7. 本地启动

### MySQL

```sql
CREATE DATABASE orbit CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'orbit'@'127.0.0.1' IDENTIFIED BY '<your-password>';
GRANT ALL PRIVILEGES ON orbit.* TO 'orbit'@'127.0.0.1';
```

本机验证使用 MySQL 8.4.9。口令不写进仓库，只能通过环境变量传入：

```bash
export ORBIT_DB_PASSWORD='<your-password>'
```

### 后端

```bash
cd server
./mvnw spring-boot:run
# 或构建后运行
./mvnw -DskipTests package
java -jar target/orbit-server-0.0.1-SNAPSHOT.jar
```

健康检查：

```bash
curl http://127.0.0.1:8080/api/health
```

头像目录默认 `./data/avatars`，可用 `orbit.avatar-dir` 覆盖。

### Android

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

模拟器可直接使用 `10.0.2.2`；真机需要：

1. 电脑防火墙放行 8080；
2. 手机与电脑同一局域网；
3. 修改 `ORBIT_API_BASE_URL`；
4. 把电脑 IP 加入 `network_security_config.xml`；
5. 首次登录时允许「附近的设备 / 本地网络」。

## 8. 验证记录

2026-10-26，Windows + Pixel 7 Pro 模拟器（API 37，Android 17，1440×3120）：

- 注册新账号 `orbit2026`：系统「附近的设备」弹窗 → 允许 → 201，写入 `app_users` 与 `sync_revisions`。
- 修改昵称为 `NovaPilot`：PATCH 200，账号卡与头像首字母同步更新。
- 相册头像：系统照片弹窗 → 允许全部 → 选择 PNG → 压缩后 multipart 上传成功，MySQL `avatar_path` 指向新 JPG。
- 相机头像：系统相机弹窗 → 使用时允许 → 相机快门 / 确认 → 上传成功。
- 新建任务 `Cloud Sync Check`：1.2 秒后 PUT `/api/sync`，MySQL 出现 `sync_tasks` 行，revision 2。
- 清空应用数据后重新登录：仅 GET `/api/sync` 即恢复该任务，首页徽标从「本机保存」变为「云端同步」。
- 退出登录：POST `/api/auth/logout` 204，令牌从本机清除，缓存与 revision 保留。
- 服务端：`./mvnw test` 2 项通过（认证 / 资料 / 头像、快照推送与 revision 冲突）。
- 客户端 JVM：62 项通过。
- 设备回归：41 项通过（41 项含 2 个截图生成用例）。修改首页提示文案与账号卡位置后，更新了 `ScreenshotGalleryTest` 的滚动目标和 `SettingsPersistenceTest` 的重启后滚动步骤。

截图：`docs/screenshots/phase9/`（已登录账号卡（相册头像）、登录 / 注册卡、Android 17 本地网络权限弹窗、首页「云端同步」徽标、相机拍照上传后的头像）。

## 9. 已知限制

- HTTP 明文与开发密码只用于本机；没有 TLS、限流、验证码、找回密码、邮箱/手机号。
- 令牌在数据库与客户端均未再做一次哈希；令牌泄漏等同于账号会话泄漏。
- 头像文件存本机磁盘，未接对象存储；删除旧头像失败时可能留下孤儿文件。
- 快照是整包覆盖，数据量增大后请求会变重；当前以 5000 行/类为硬上限，没有增量日志。
- 冲突策略是服务端优先，离线设备上的本地修改可能被覆盖；没有逐条合并 UI。
- Room 主键沿用本机 id，多设备同时新建可能撞 id，依赖下一次整包覆盖收敛。
- 同一台设备只保留一份 Room 缓存；切换账号时以上一个账号的归属为准做隔离，不提供多账号本地沙箱。
