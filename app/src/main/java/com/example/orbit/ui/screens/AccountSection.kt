package com.example.orbit.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.example.orbit.account.AccountUser
import com.example.orbit.account.OrbitApi
import com.example.orbit.account.SyncPhase
import com.example.orbit.account.SyncStatus
import com.example.orbit.account.requiresLocalNetworkPermission
import com.example.orbit.ui.AccountUiState
import com.example.orbit.ui.AuthMode
import com.example.orbit.ui.components.OrbitCard
import com.example.orbit.ui.components.OrbitPrimaryButton
import com.example.orbit.ui.components.OrbitTextLink
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.orbitColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal const val ACCOUNT_LOGOUT_TAG = "account_logout"
internal const val ACCOUNT_LOGOUT_BUTTON_TAG = "account_logout_button"
internal const val ACCOUNT_AVATAR_TAG = "account_avatar"
internal const val ACCOUNT_SYNC_TAG = "account_sync"
internal const val ACCOUNT_NICKNAME_TAG = "account_nickname"
internal const val ACCOUNT_NICKNAME_SAVE_TAG = "account_nickname_save"
internal const val ACCOUNT_GALLERY_TAG = "account_gallery"
internal const val ACCOUNT_CAMERA_TAG = "account_camera"

/** Which network call triggered the local-network permission dialog. */
private enum class NetworkAction { NONE, SYNC }

/** Lambdas for the account card, grouped so the settings screen stays readable. */
data class AccountActions(
    val onUsernameChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onConfirmPasswordChange: (String) -> Unit = {},
    val onModeChange: (AuthMode) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onLogout: () -> Unit = {},
    val onStartNicknameEdit: () -> Unit = {},
    val onNicknameDraftChange: (String) -> Unit = {},
    val onSaveNickname: () -> Unit = {},
    val onCancelNicknameEdit: () -> Unit = {},
    val onGalleryPicked: (Uri) -> Unit = {},
    val onCreateCameraTarget: () -> Uri? = { null },
    val onCameraResult: (Boolean) -> Unit = {},
    val onSync: () -> Unit = {},
    val onConsumeMessage: () -> Unit = {},
    val onClearError: () -> Unit = {},
    val onReportError: (String) -> Unit = {},
)

/**
 * Signed-in account card for the profile tab: avatar, editable nickname and the
 * live sync state. The mandatory login form lives in AuthScreen; this card is
 * only reachable with a session. Avatar acquisition owns its permission
 * launchers so the rest of the settings screen stays free of Activity result
 * plumbing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountCard(
    state: AccountUiState,
    actions: AccountActions,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val context = LocalContext.current
    var showAvatarSheet by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var permissionHint by rememberSaveable { mutableStateOf<String?>(null) }

    val galleryPermissions = remember {
        when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )

            Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) actions.onGalleryPicked(uri)
    }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (hasGalleryAccess(context)) {
            permissionHint = null
            galleryPicker.launch("image/*")
        } else {
            permissionHint = "需要照片访问权限才能从相册选择头像"
            actions.onReportError("相册权限未开启")
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        actions.onCameraResult(success)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            permissionHint = null
            launchCamera(context, actions, cameraLauncher)
        } else {
            permissionHint = "需要相机权限才能拍照上传"
            actions.onReportError("相机权限未开启")
        }
    }

    // Android 17 gates LAN sockets behind a runtime permission; ask right before
    // the first network call instead of on app launch.
    var pendingNetworkAction by remember { mutableStateOf(NetworkAction.NONE) }
    val localNetworkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val pending = pendingNetworkAction
        pendingNetworkAction = NetworkAction.NONE
        if (granted) {
            permissionHint = null
            if (pending == NetworkAction.SYNC) actions.onSync()
        } else {
            permissionHint = "需要“附近的设备”权限才能连接云端服务"
            actions.onReportError("本地网络权限未开启")
        }
    }
    val gatedActions = actions.copy(
        onSync = {
            if (hasLocalNetworkAccess(context)) {
                actions.onSync()
            } else {
                pendingNetworkAction = NetworkAction.SYNC
                localNetworkLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            }
        },
    )

    val user = state.user ?: return
    OrbitCard(modifier = modifier, elevated = true) {
        SignedInContent(
            state = state,
            user = user,
            actions = gatedActions,
            onAvatarClick = { showAvatarSheet = true },
            onLogoutClick = { showLogoutDialog = true },
            permissionHint = permissionHint,
            onOpenSettings = {
                permissionHint = null
                context.openAppSettings()
            },
        )
    }

    if (showAvatarSheet) {
        AvatarSourceSheet(
            onDismiss = { showAvatarSheet = false },
            onGallery = {
                showAvatarSheet = false
                if (hasGalleryAccess(context)) {
                    galleryPicker.launch("image/*")
                } else {
                    galleryPermissionLauncher.launch(galleryPermissions)
                }
            },
            onCamera = {
                showAvatarSheet = false
                if (hasCameraPermission(context)) {
                    launchCamera(context, actions, cameraLauncher)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = colors.card,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = { Text(text = "退出登录？") },
            text = {
                Text(
                    text = "退出后将返回登录页，可用其他账号登录；本机缓存会清空，云端数据保留。",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        actions.onLogout()
                    },
                    modifier = Modifier.testTag(ACCOUNT_LOGOUT_TAG),
                ) {
                    Text(text = "退出", color = colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = "取消", color = colors.textSecondary)
                }
            },
        )
    }
}

@Composable
private fun SignedInContent(
    state: AccountUiState,
    user: AccountUser,
    actions: AccountActions,
    onAvatarClick: () -> Unit,
    onLogoutClick: () -> Unit,
    permissionHint: String?,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors

    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(
            user = user,
            busy = state.avatarBusy,
            onClick = onAvatarClick,
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.nickname,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "修改昵称",
                    tint = colors.accentLine,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            enabled = !state.busy,
                            onClick = actions.onStartNicknameEdit,
                        )
                        .padding(8.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "@${user.username} · 云端账号",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                color = colors.textMuted,
            )
        }
    }

    Spacer(Modifier.height(14.dp))
    SyncStatusRow(status = state.sync, onSync = actions.onSync)

    if (state.editingNickname) {
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.nicknameDraft,
            onValueChange = actions.onNicknameDraftChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ACCOUNT_NICKNAME_TAG),
            enabled = !state.busy,
            singleLine = true,
            label = { Text(text = "昵称") },
            supportingText = {
                Text(
                    text = "${state.nicknameDraft.length} / 24",
                    color = colors.textMuted,
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            shape = RoundedCornerShape(OrbitCorners.inner),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (!state.busy) actions.onSaveNickname() },
            ),
            colors = taskFieldColors(),
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OrbitPrimaryButton(
                text = if (state.busy) "保存中…" else "保存昵称",
                onClick = actions.onSaveNickname,
                enabled = !state.busy,
                modifier = Modifier
                    .weight(1f)
                    .testTag(ACCOUNT_NICKNAME_SAVE_TAG),
            )
            Spacer(Modifier.width(4.dp))
            OrbitTextLink(
                text = "取消",
                onClick = actions.onCancelNicknameEdit,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }

    state.error?.let { error ->
        Spacer(Modifier.height(10.dp))
        AccountNotice(text = error, error = true, onDismiss = actions.onClearError)
    }
    if (permissionHint != null) {
        Spacer(Modifier.height(8.dp))
        PermissionNotice(text = permissionHint, onOpenSettings = onOpenSettings)
    }
    state.message?.let { message ->
        Spacer(Modifier.height(10.dp))
        AccountNotice(text = message, error = false)
    }

    Spacer(Modifier.height(12.dp))
    AccountDivider()
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OrbitTextLink(text = "更换头像", onClick = onAvatarClick)
        OrbitTextLink(text = "修改昵称", onClick = actions.onStartNicknameEdit)
    }
    Spacer(Modifier.height(10.dp))
    LogoutButton(busy = state.busy, onClick = onLogoutClick)
    Spacer(Modifier.height(6.dp))
    Text(
        text = "退出后可使用其他账号登录",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
        color = colors.textMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        textAlign = TextAlign.Center,
    )
}

/** Full-width danger action so signing out (and switching accounts) is easy to find. */
@Composable
private fun LogoutButton(
    busy: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(OrbitCorners.inner)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(shape)
            .background(colors.danger.copy(alpha = if (colors.isDark) 0.12f else 0.06f))
            .border(1.dp, colors.danger.copy(alpha = 0.45f), shape)
            .clickable(role = Role.Button, enabled = !busy, onClick = onClick)
            .testTag(ACCOUNT_LOGOUT_BUTTON_TAG)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = null,
            tint = if (busy) colors.textMuted else colors.danger,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (busy) "请稍候…" else "退出登录",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (busy) colors.textMuted else colors.danger,
        )
    }
}

@Composable
private fun Avatar(
    user: AccountUser,
    busy: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val initial = user.nickname.trim().firstOrNull()?.uppercase() ?: "O"
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(colors.track)
            .border(1.5.dp, colors.accentLine, CircleShape)
            .clickable(
                role = Role.Button,
                enabled = !busy,
                onClick = onClick,
            )
            .testTag(ACCOUNT_AVATAR_TAG),
        contentAlignment = Alignment.Center,
    ) {
        if (user.avatarPath.isNullOrBlank()) {
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = colors.accentLine,
            )
        } else {
            AsyncImage(
                model = OrbitApi.avatarUrl(user.avatarPath),
                contentDescription = "头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape),
            )
        }
        if (busy) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(colors.scrim.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = colors.primary,
                    strokeWidth = 2.dp,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(10.dp),
            )
        }
    }
}

@Composable
private fun SyncStatusRow(
    status: SyncStatus,
    onSync: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val dotColor = when (status.phase) {
        SyncPhase.IDLE -> colors.textMuted
        SyncPhase.SYNCING -> colors.accentLine
        SyncPhase.SYNCED -> colors.success
        SyncPhase.OFFLINE -> colors.warning
        SyncPhase.CONFLICT -> colors.warning
    }
    val label = syncLabel(status)
    val time = formatSyncTime(status.lastSyncedAt)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.track.copy(alpha = 0.6f))
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .testTag(ACCOUNT_SYNC_TAG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status.phase == SyncPhase.SYNCING) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = colors.accentLine,
                strokeWidth = 1.5.dp,
            )
        } else {
            StatusDot(color = dotColor)
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = if (status.phase == SyncPhase.SYNCED) {
                    colors.textSecondary
                } else {
                    colors.textPrimary
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (time != null) {
                Text(
                    text = "上次同步 $time",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = colors.textMuted,
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    role = Role.Button,
                    enabled = status.phase != SyncPhase.SYNCING,
                    onClick = onSync,
                )
                .heightIn(min = 40.dp)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "立即同步",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (status.phase == SyncPhase.SYNCING) {
                    colors.textMuted
                } else {
                    colors.accentLine
                },
            )
        }
    }
}

private fun syncLabel(status: SyncStatus): String = when (status.phase) {
    SyncPhase.IDLE -> "等待首次同步"
    SyncPhase.SYNCING -> "同步中，请稍候…"
    SyncPhase.SYNCED -> "云端已是最新"
    SyncPhase.OFFLINE -> status.message ?: "离线，修改会在恢复后上传"
    SyncPhase.CONFLICT -> status.message ?: "已采用云端最新版本"
}

private fun formatSyncTime(timestamp: Long?): String? {
    if (timestamp == null || timestamp <= 0L) return null
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    val pattern = if (sameDay) "HH:mm" else "M月d日 HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun AccountDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.orbitColors.cardBorder),
    )
}

@Composable
private fun AccountNotice(
    text: String,
    error: Boolean,
    onDismiss: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitCorners.inner))
            .background(colors.track.copy(alpha = 0.55f))
            .padding(start = 12.dp, end = if (onDismiss != null) 4.dp else 12.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(if (error) colors.danger else colors.accentLine),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 15.sp),
            color = if (error) colors.danger else colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        if (onDismiss != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "清除",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun PermissionNotice(
    text: String,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitCorners.inner))
            .background(colors.track.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
            color = colors.textMuted,
            modifier = Modifier.weight(1f),
        )
        OrbitTextLink(text = "去设置", onClick = onOpenSettings)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarSourceSheet(
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        scrimColor = colors.scrim.copy(alpha = 0.78f),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 10.dp)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(OrbitCorners.pill))
                    .background(colors.textMuted.copy(alpha = 0.42f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = "更换头像",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "选择一种方式，系统会请求对应权限。",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.textMuted,
            )
            Spacer(Modifier.height(18.dp))
            SourceRow(
                index = "01",
                title = "从相册选择",
                caption = "读取本机照片 · 需要照片访问权限",
                tag = ACCOUNT_GALLERY_TAG,
                onClick = onGallery,
            )
            Spacer(Modifier.height(10.dp))
            SourceRow(
                index = "02",
                title = "拍照上传",
                caption = "调用系统相机 · 需要相机权限",
                tag = ACCOUNT_CAMERA_TAG,
                onClick = onCamera,
            )
            Spacer(Modifier.height(12.dp))
            OrbitTextLink(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun SourceRow(
    index: String,
    title: String,
    caption: String,
    tag: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitCorners.inner))
            .background(colors.cardElevated)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(OrbitCorners.inner))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = colors.accentLine,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = colors.textMuted,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp),
        )
    }
}

private fun hasGalleryAccess(context: Context): Boolean = when {
    Build.VERSION.SDK_INT >= 34 -> {
        context.isGranted(Manifest.permission.READ_MEDIA_IMAGES) ||
            context.isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    }

    Build.VERSION.SDK_INT >= 33 -> context.isGranted(Manifest.permission.READ_MEDIA_IMAGES)
    else -> context.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun hasCameraPermission(context: Context): Boolean =
    context.isGranted(Manifest.permission.CAMERA)

/** API 37+ blocks LAN sockets until ACCESS_LOCAL_NETWORK is granted. */
internal fun hasLocalNetworkAccess(context: Context): Boolean =
    !requiresLocalNetworkPermission(OrbitApi.baseUrl) ||
        Build.VERSION.SDK_INT < 37 ||
        context.isGranted(Manifest.permission.ACCESS_LOCAL_NETWORK)

internal fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun launchCamera(
    context: Context,
    actions: AccountActions,
    launcher: androidx.activity.result.ActivityResultLauncher<Uri>,
) {
    val target = actions.onCreateCameraTarget()
    if (target == null) {
        actions.onReportError("无法创建照片文件，请重试")
        return
    }
    try {
        launcher.launch(target)
    } catch (failure: ActivityNotFoundException) {
        actions.onReportError("没有找到可用的相机应用")
    }
}

internal fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
