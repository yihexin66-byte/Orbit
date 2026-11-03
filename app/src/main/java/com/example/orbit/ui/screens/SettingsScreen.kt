package com.example.orbit.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.BuildConfig
import com.example.orbit.R
import com.example.orbit.model.AccentTone
import com.example.orbit.model.ThemeMode
import com.example.orbit.ui.AccountUiState
import com.example.orbit.ui.OrbitPreferencesUiState
import com.example.orbit.ui.components.GlassDivider
import com.example.orbit.ui.components.OrbitCard
import com.example.orbit.ui.components.OrbitLazyScreen
import com.example.orbit.ui.components.OrbitMark
import com.example.orbit.ui.components.OrbitScreenHeader
import com.example.orbit.ui.components.SpaceBackdrop
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.OrbitTheme
import com.example.orbit.ui.theme.accentPreviewColors
import com.example.orbit.ui.theme.orbitColors
import com.example.orbit.ui.theme.orbitColorsFor

/** Live Room counts shown on the profile tab. */
data class OrbitDataOverview(
    val tasks: Int = 0,
    val goals: Int = 0,
    val habits: Int = 0,
    val checkIns: Int = 0,
)

@Composable
fun SettingsScreen(
    uiState: OrbitPreferencesUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentToneChange: (AccentTone) -> Unit,
    modifier: Modifier = Modifier,
    dataOverview: OrbitDataOverview = OrbitDataOverview(),
    accountState: AccountUiState = AccountUiState(),
    accountActions: AccountActions = AccountActions(),
) {
    OrbitLazyScreen(
        modifier = modifier,
        content = {
            item(key = "header") {
                OrbitScreenHeader(
                    eyebrow = stringResource(R.string.settings_eyebrow),
                    title = stringResource(R.string.settings_title),
                    subtitle = stringResource(R.string.settings_subtitle),
                    trailing = { SavedBadge() },
                )
            }
            item(key = "account") {
                AccountCard(state = accountState, actions = accountActions)
            }
            item(key = "local-note") {
                LocalPreferenceNote(
                    title = stringResource(R.string.settings_local_banner_title),
                    description = stringResource(R.string.settings_local_banner_desc),
                )
            }
            item(key = "appearance") {
                SettingsSection(label = stringResource(R.string.settings_appearance)) {
                    AppearanceCard(
                        themeMode = uiState.themeMode,
                        accentTone = uiState.accentTone,
                        onThemeModeChange = onThemeModeChange,
                        onAccentToneChange = onAccentToneChange,
                    )
                }
            }
            item(key = "overview") {
                SettingsSection(label = stringResource(R.string.settings_overview_title)) {
                    DataOverviewCard(overview = dataOverview)
                }
            }
            item(key = "notifications") {
                SettingsSection(label = stringResource(R.string.settings_notifications_title)) {
                    ReminderCard()
                }
            }
            item(key = "data") {
                SettingsSection(label = stringResource(R.string.settings_data_title)) {
                    DataPrivacyCard(accountState = accountState)
                }
            }
            item(key = "about") {
                SettingsSection(label = stringResource(R.string.settings_about_title)) {
                    AboutCard()
                }
            }
            item(key = "footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OrbitMark(diameter = 20.dp)
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = stringResource(R.string.settings_footer),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp,
                        ),
                        color = MaterialTheme.orbitColors.textMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    )
}

@Composable
private fun SavedBadge() {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier.heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(colors.success),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.settings_saved_badge),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                letterSpacing = 0.8.sp,
            ),
            color = colors.textMuted,
        )
    }
}

@Composable
private fun LocalPreferenceNote(
    title: String,
    description: String,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(3.dp)
                .height(30.dp)
                .background(colors.accentLine),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                ),
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun SettingsSection(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.orbitColors.textMuted,
            modifier = Modifier.padding(start = 2.dp),
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun AppearanceCard(
    themeMode: ThemeMode,
    accentTone: AccentTone,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentToneChange: (AccentTone) -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    OrbitCard(padding = PaddingValues(horizontal = 14.dp, vertical = 18.dp)) {
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = stringResource(R.string.settings_theme_title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.settings_theme_desc),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                color = colors.textMuted,
            )
        }
        Spacer(Modifier.height(10.dp))
        ThemeMode.entries.forEachIndexed { index, mode ->
            ThemeOptionRow(
                mode = mode,
                accentTone = accentTone,
                selected = themeMode == mode,
                onClick = { onThemeModeChange(mode) },
            )
            if (index != ThemeMode.entries.lastIndex) {
                GlassDivider(
                    modifier = Modifier.padding(start = 60.dp, end = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        GlassDivider(modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(18.dp))
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = stringResource(R.string.settings_accent_title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.settings_accent_desc),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                color = colors.textMuted,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AccentTone.entries.forEach { tone ->
                AccentOption(
                    tone = tone,
                    selected = accentTone == tone,
                    onClick = { onAccentToneChange(tone) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    mode: ThemeMode,
    accentTone: AccentTone,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val description = stringResource(
        when (mode) {
            ThemeMode.DEEP_SPACE -> R.string.theme_deep_space_desc
            ThemeMode.DAWN -> R.string.theme_dawn_desc
            ThemeMode.SYSTEM -> R.string.theme_system_desc
        },
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.988f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "themeRowPress",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.track else Color.Transparent)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeSwatch(mode = mode, accentTone = accentTone, selected = selected)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(mode.labelRes),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                color = colors.textMuted,
            )
        }
        Spacer(Modifier.width(10.dp))
        SelectionMark(selected = selected)
    }
}

@Composable
private fun ThemeSwatch(
    mode: ThemeMode,
    accentTone: AccentTone,
    selected: Boolean,
) {
    val colors = MaterialTheme.orbitColors
    val dark = orbitColorsFor(accentTone, dark = true)
    val light = orbitColorsFor(accentTone, dark = false)
    val shape = RoundedCornerShape(10.dp)
    val background = when (mode) {
        ThemeMode.DEEP_SPACE -> dark.backgroundTop
        ThemeMode.DAWN -> light.backgroundTop
        ThemeMode.SYSTEM -> light.backgroundTop
    }
    val dot = when (mode) {
        ThemeMode.DEEP_SPACE -> accentPreviewColors(accentTone, dark = true).first
        ThemeMode.DAWN -> accentPreviewColors(accentTone, dark = false).first
        ThemeMode.SYSTEM -> accentPreviewColors(accentTone, dark = true).first
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(shape)
            .background(background)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.accentLine else colors.cardBorder,
                shape = shape,
            ),
    ) {
        if (mode == ThemeMode.SYSTEM) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(light.backgroundTop),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(dark.backgroundTop),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(9.dp)
                .clip(CircleShape)
                .background(dot),
        )
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(5.dp)
    if (selected) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(shape)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(13.dp),
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(shape)
                .border(1.dp, colors.cardBorder, shape),
        )
    }
}

@Composable
private fun AccentOption(
    tone: AccentTone,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val toneColor = accentPreviewColors(tone, colors.isDark).first
    val checkColor = if (colors.isDark) colors.onAccent else colors.backgroundTop
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "accentOptionPress",
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(12.dp))
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .then(
                    if (selected) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = colors.accentLine,
                            shape = CircleShape,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(toneColor),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = checkColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(tone.labelRes),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.sp,
            ),
            color = if (selected) colors.textPrimary else colors.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun ReminderCard() {
    val colors = MaterialTheme.orbitColors
    OrbitCard {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(6.dp)
                    .background(colors.warning),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_reminder_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.settings_reminder_desc),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = colors.textMuted,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.settings_reminder_status),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = colors.warning,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_reminder_note),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                lineHeight = 15.sp,
            ),
            color = colors.textMuted,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun DataOverviewCard(overview: OrbitDataOverview) {
    val colors = MaterialTheme.orbitColors
    val description = stringResource(
        R.string.settings_overview_desc,
        overview.tasks,
        overview.goals,
        overview.habits,
        overview.checkIns,
    )

    OrbitCard(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            OverviewMetric(
                value = overview.tasks,
                label = stringResource(R.string.settings_overview_tasks),
                modifier = Modifier.weight(1f),
            )
            OverviewMetric(
                value = overview.goals,
                label = stringResource(R.string.settings_overview_goals),
                modifier = Modifier.weight(1f),
            )
            OverviewMetric(
                value = overview.habits,
                label = stringResource(R.string.settings_overview_habits),
                modifier = Modifier.weight(1f),
            )
            OverviewMetric(
                value = overview.checkIns,
                label = stringResource(R.string.settings_overview_checkins),
                accent = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(16.dp))
        GlassDivider()
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_overview_note),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                lineHeight = 15.sp,
            ),
            color = colors.textMuted,
        )
    }
}

@Composable
private fun OverviewMetric(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val colors = MaterialTheme.orbitColors
    Column(modifier = modifier) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 26.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFeatureSettings = "tnum",
            ),
            color = if (accent) colors.accentLine else colors.textPrimary,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = colors.textMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun DataPrivacyCard(accountState: AccountUiState) {
    val colors = MaterialTheme.orbitColors
    val accountValue = accountState.user?.let {
        stringResource(R.string.settings_account_signed_in, it.username)
    } ?: stringResource(R.string.settings_account_signed_out)
    OrbitCard {
        DataRow(
            label = stringResource(R.string.settings_data_tasks_label),
            value = stringResource(R.string.settings_data_tasks_value),
        )
        GlassDivider()
        DataRow(
            label = stringResource(R.string.settings_data_prefs_label),
            value = stringResource(R.string.settings_data_prefs_value),
        )
        GlassDivider()
        DataRow(
            label = stringResource(R.string.settings_account_title),
            value = accountValue,
            valueColor = colors.textMuted,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OrbitCorners.inner))
                .background(colors.track.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(3.dp)
                    .height(13.dp)
                    .background(colors.accentLine),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(R.string.settings_data_body),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                ),
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun DataRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.orbitColors.textSecondary,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = colors.textMuted,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 15.sp,
            ),
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.35f),
        )
    }
}

@Composable
private fun AboutCard() {
    val colors = MaterialTheme.orbitColors
    OrbitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OrbitMark(diameter = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.settings_about_desc),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.settings_version,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.4.sp,
                    ),
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101010, widthDp = 393, heightDp = 852)
@Composable
private fun SettingsScreenPreview() {
    OrbitTheme {
        SpaceBackdrop {
            SettingsScreen(
                uiState = OrbitPreferencesUiState(),
                onThemeModeChange = {},
                onAccentToneChange = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F2, widthDp = 393, heightDp = 852)
@Composable
private fun SettingsScreenDawnPreview() {
    OrbitTheme(themeMode = ThemeMode.DAWN) {
        SpaceBackdrop {
            SettingsScreen(
                uiState = OrbitPreferencesUiState(themeMode = ThemeMode.DAWN),
                onThemeModeChange = {},
                onAccentToneChange = {},
            )
        }
    }
}
