package com.example.orbit.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.account.SyncPhase
import com.example.orbit.ui.AccountUiState
import com.example.orbit.ui.AuthMode
import com.example.orbit.ui.components.OrbitCard
import com.example.orbit.ui.components.OrbitMark
import com.example.orbit.ui.components.OrbitPrimaryButton
import com.example.orbit.ui.components.OrbitTextLink
import com.example.orbit.ui.theme.ContentMaxWidth
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.orbitColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal const val AUTH_SCREEN_TAG = "auth_screen"
internal const val AUTH_USERNAME_TAG = "auth_username"
internal const val AUTH_PASSWORD_TAG = "auth_password"
internal const val AUTH_CONFIRM_TAG = "auth_confirm"
internal const val AUTH_PASSWORD_VISIBILITY_TAG = "auth_password_visibility"
internal const val AUTH_CONFIRM_VISIBILITY_TAG = "auth_confirm_visibility"
internal const val AUTH_SUBMIT_TAG = "auth_submit"
internal const val AUTH_LOGIN_TAB_TAG = "auth_login_tab"
internal const val AUTH_REGISTER_TAB_TAG = "auth_register_tab"
internal const val AUTH_SYNC_BOOT_TAG = "auth_sync_boot"

/**
 * Mandatory account gate. The main navigation only exists after a session is
 * saved, so this screen is the whole app until the user authenticates.
 *
 * The login call is wrapped with the Android 17 local-network permission: a
 * target-37 app cannot open a socket to the Spring Boot host until
 * ACCESS_LOCAL_NETWORK is granted, and the system dialog must be triggered from
 * a user action. Gallery/camera permissions stay in the profile card because
 * those capabilities are only needed after sign-in.
 */
@Composable
fun AuthScreen(
    state: AccountUiState,
    actions: AccountActions,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val registering = state.mode == AuthMode.REGISTER
    val inputsEnabled = !state.busy

    var permissionHint by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSubmit by remember { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }
    val localNetworkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val shouldSubmit = pendingSubmit
        pendingSubmit = false
        if (granted) {
            permissionHint = null
            if (shouldSubmit) actions.onSubmit()
        } else {
            permissionHint = context.getString(R.string.auth_network_denied)
            actions.onReportError("本地网络权限未开启")
        }
    }
    val submit: () -> Unit = {
        if (!inputsEnabled) {
            Unit
        } else if (hasLocalNetworkAccess(context)) {
            actions.onSubmit()
        } else {
            pendingSubmit = true
            localNetworkLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .testTag(AUTH_SCREEN_TAG),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 14.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "brand") {
                AuthBrandRow()
            }
            item(key = "headline") {
                AuthHeadline(registering = registering)
            }
            item(key = "form") {
                OrbitCard(elevated = true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoicePill(
                            label = stringResource(R.string.auth_tab_login),
                            selected = !registering,
                            accent = colors.accentLine,
                            enabled = inputsEnabled,
                            onClick = { actions.onModeChange(AuthMode.LOGIN) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag(AUTH_LOGIN_TAB_TAG),
                        )
                        ChoicePill(
                            label = stringResource(R.string.auth_tab_register),
                            selected = registering,
                            accent = colors.accentLine,
                            enabled = inputsEnabled,
                            onClick = { actions.onModeChange(AuthMode.REGISTER) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag(AUTH_REGISTER_TAB_TAG),
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = actions.onUsernameChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AUTH_USERNAME_TAG),
                        enabled = inputsEnabled,
                        singleLine = true,
                        label = { Text(stringResource(R.string.auth_username_label)) },
                        placeholder = {
                            Text(stringResource(R.string.auth_username_placeholder))
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = colors.textPrimary,
                        ),
                        shape = RoundedCornerShape(OrbitCorners.inner),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                        colors = taskFieldColors(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = actions.onPasswordChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AUTH_PASSWORD_TAG),
                        enabled = inputsEnabled,
                        singleLine = true,
                        label = { Text(stringResource(R.string.auth_password_label)) },
                        placeholder = {
                            Text(stringResource(R.string.auth_password_placeholder))
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = colors.textPrimary,
                        ),
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            PasswordVisibilityToggle(
                                visible = passwordVisible,
                                enabled = inputsEnabled,
                                onToggle = { passwordVisible = !passwordVisible },
                                modifier = Modifier.testTag(AUTH_PASSWORD_VISIBILITY_TAG),
                            )
                        },
                        shape = RoundedCornerShape(OrbitCorners.inner),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (registering) ImeAction.Next else ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = { submit() },
                        ),
                        colors = taskFieldColors(),
                    )
                    if (registering) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.confirmPassword,
                            onValueChange = actions.onConfirmPasswordChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(AUTH_CONFIRM_TAG),
                            enabled = inputsEnabled,
                            singleLine = true,
                            label = {
                                Text(stringResource(R.string.auth_confirm_label))
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = colors.textPrimary,
                            ),
                            visualTransformation = if (confirmVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                PasswordVisibilityToggle(
                                    visible = confirmVisible,
                                    enabled = inputsEnabled,
                                    onToggle = { confirmVisible = !confirmVisible },
                                    modifier = Modifier.testTag(AUTH_CONFIRM_VISIBILITY_TAG),
                                )
                            },
                            shape = RoundedCornerShape(OrbitCorners.inner),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            colors = taskFieldColors(),
                        )
                    }

                    state.error?.let { error ->
                        Spacer(Modifier.height(12.dp))
                        AuthNotice(
                            text = error,
                            error = true,
                            onDismiss = actions.onClearError,
                        )
                    }
                    if (permissionHint != null) {
                        Spacer(Modifier.height(10.dp))
                        AuthPermissionNotice(
                            text = permissionHint.orEmpty(),
                            onOpenSettings = {
                                permissionHint = null
                                context.openAppSettings()
                            },
                        )
                    } else if (state.sync.phase == SyncPhase.OFFLINE && state.session == null) {
                        Spacer(Modifier.height(10.dp))
                        AuthNotice(
                            text = state.sync.message ?: stringResource(R.string.auth_expired),
                            error = true,
                        )
                    }
                    state.message?.let { message ->
                        Spacer(Modifier.height(10.dp))
                        AuthNotice(text = message, error = false)
                    }

                    Spacer(Modifier.height(16.dp))
                    OrbitPrimaryButton(
                        text = when {
                            state.busy && registering ->
                                stringResource(R.string.auth_busy_register)

                            state.busy -> stringResource(R.string.auth_busy_login)
                            registering -> stringResource(R.string.auth_submit_register)
                            else -> stringResource(R.string.auth_submit_login)
                        },
                        onClick = submit,
                        enabled = inputsEnabled,
                        modifier = Modifier.testTag(AUTH_SUBMIT_TAG),
                    )

                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.cardBorder),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(colors.accentLine),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (registering) {
                                    R.string.auth_register_note
                                } else {
                                    R.string.auth_cloud_note
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                            ),
                            color = colors.textMuted,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Explicit 显示 / 隐藏 action inside a password field. Text is easier to spot
 * than a glyph and avoids pulling in the extended Material icon pack for one
 * eye; the content description spells out what is being revealed.
 */
@Composable
private fun PasswordVisibilityToggle(
    visible: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val label = stringResource(
        if (visible) R.string.auth_password_hide else R.string.auth_password_show,
    )
    val description = stringResource(
        if (visible) R.string.auth_password_hide_desc else R.string.auth_password_show_desc,
    )
    Box(
        modifier = modifier
            .size(width = 58.dp, height = 48.dp)
            .clip(RoundedCornerShape(OrbitCorners.pill))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onToggle,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold,
            color = if (enabled) colors.accentLine else colors.textMuted,
        )
    }
}

@Composable
private fun AuthBrandRow() {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OrbitMark(diameter = 36.dp)
        Spacer(Modifier.width(11.dp))
        Column {
            Text(
                text = stringResource(R.string.home_wordmark),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                ),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    letterSpacing = 1.7.sp,
                ),
                color = colors.textMuted,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.auth_secure_access),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                letterSpacing = 1.4.sp,
            ),
            color = colors.textMuted,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colors.accentLine),
        )
    }
}

@Composable
private fun AuthHeadline(registering: Boolean) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(colors.accentLine),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.auth_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    letterSpacing = 1.8.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(
                    if (registering) R.string.auth_title_register else R.string.auth_title_login,
                ),
                style = MaterialTheme.typography.displayMedium,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.auth_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.width(10.dp))
        GateOrbitArt(modifier = Modifier.size(92.dp))
    }
}

/** Flat orbital diagram: a quiet, finite entry animation, no looping glow. */
@Composable
private fun GateOrbitArt(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.orbitColors
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }
    Canvas(modifier = modifier.clearAndSetSemantics {}) {
        val r = reveal.value
        val center = center
        val outer = size.minDimension * 0.46f
        val inner = size.minDimension * 0.29f
        drawCircle(
            color = colors.cardBorder,
            radius = outer,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
            alpha = 0.85f * r,
        )
        drawCircle(
            color = colors.cardBorder,
            radius = inner,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
            alpha = 0.55f * r,
        )
        listOf(0f, 90f, 180f, 270f).forEach { angle ->
            val radians = angle * PI.toFloat() / 180f
            val start = Offset(
                center.x + cos(radians) * (outer - 3.dp.toPx()),
                center.y + sin(radians) * (outer - 3.dp.toPx()),
            )
            val end = Offset(
                center.x + cos(radians) * (outer + 3.dp.toPx()),
                center.y + sin(radians) * (outer + 3.dp.toPx()),
            )
            drawLine(
                color = colors.textMuted,
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx(),
                alpha = 0.7f * r,
            )
        }
        // Two occupied slots and one still empty.
        listOf(205f to inner, 320f to outer).forEach { (angle, radius) ->
            val radians = angle * PI.toFloat() / 180f
            drawCircle(
                color = colors.primary,
                radius = 3.dp.toPx(),
                center = Offset(
                    center.x + cos(radians) * radius,
                    center.y + sin(radians) * radius,
                ),
                alpha = r,
            )
        }
        val emptyRadians = 62f * PI.toFloat() / 180f
        drawCircle(
            color = colors.textMuted,
            radius = 2.6.dp.toPx(),
            center = Offset(
                center.x + cos(emptyRadians) * outer,
                center.y + sin(emptyRadians) * outer,
            ),
            style = Stroke(width = 1.dp.toPx()),
            alpha = 0.7f * r,
        )
        drawCircle(
            color = colors.accentLine,
            radius = 4.dp.toPx(),
            center = center,
            alpha = r,
        )
        drawCircle(
            color = colors.accentLine,
            radius = 7.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx()),
            alpha = 0.45f * r,
        )
    }
}

@Composable
private fun AuthNotice(
    text: String,
    error: Boolean,
    onDismiss: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitCorners.inner))
            .background(colors.track.copy(alpha = 0.62f))
            .padding(
                start = 12.dp,
                end = if (onDismiss != null) 4.dp else 12.dp,
                top = 9.dp,
                bottom = 9.dp,
            ),
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
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
            ),
            color = if (error) colors.danger else colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        if (onDismiss != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                OrbitTextLink(text = "清除", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun AuthPermissionNotice(
    text: String,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitCorners.inner))
            .background(colors.track.copy(alpha = 0.62f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                lineHeight = 14.sp,
            ),
            color = colors.textMuted,
            modifier = Modifier.weight(1f),
        )
        OrbitTextLink(
            text = stringResource(R.string.auth_go_settings),
            onClick = onOpenSettings,
        )
    }
}

/**
 * First-session hand-off: the local cache must be replaced by the account's
 * MySQL snapshot before the editable UI appears, otherwise a quick tap could be
 * overwritten by the incoming pull.
 */
@Composable
fun SyncBootScreen(
    nickname: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(AUTH_SYNC_BOOT_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OrbitMark(diameter = 58.dp)
            Spacer(Modifier.height(26.dp))
            Text(
                text = "ORBIT / SYNC",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                ),
                color = colors.accentLine,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.sync_boot_title),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sync_boot_desc),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                ),
                color = colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { 0.72f },
                    modifier = Modifier.size(14.dp),
                    color = colors.accentLine,
                    trackColor = colors.track,
                    strokeWidth = 1.6.dp,
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = stringResource(R.string.sync_boot_nickname, nickname),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.5.sp),
                    color = colors.textPrimary,
                )
            }
        }
    }
}
