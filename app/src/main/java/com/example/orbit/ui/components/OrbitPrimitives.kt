package com.example.orbit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.ui.theme.ContentMaxWidth
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.OrbitSpacing
import com.example.orbit.ui.theme.orbitColors

@Composable
fun OrbitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(OrbitSpacing.lg),
    accent: Color? = null,
    elevated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        padding = padding,
        accent = accent,
        elevated = elevated,
        content = content,
    )
}

@Composable
fun OrbitLazyScreen(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(20.dp),
    contentPadding: PaddingValues = PaddingValues(
        start = 24.dp,
        end = 24.dp,
        top = 16.dp,
        bottom = 32.dp,
    ),
    content: LazyListScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .statusBarsPadding(),
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

@Composable
fun OrbitScreenHeader(
    eyebrow: String,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
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
                    text = eyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    letterSpacing = 1.8.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = colors.textPrimary,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.padding(top = 2.dp)) {
                trailing()
            }
        }
    }
}

@Composable
fun OrbitSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
        )
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
    }
}

@Composable
fun OrbitTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
        Spacer(Modifier.width(6.dp))
        Canvas(modifier = Modifier.size(13.dp)) {
            val stroke = 1.5.dp.toPx()
            drawLine(
                color = colors.accentLine,
                start = Offset(size.width * 0.10f, size.height * 0.50f),
                end = Offset(size.width * 0.86f, size.height * 0.50f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = colors.accentLine,
                start = Offset(size.width * 0.56f, size.height * 0.18f),
                end = Offset(size.width * 0.86f, size.height * 0.50f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = colors.accentLine,
                start = Offset(size.width * 0.56f, size.height * 0.82f),
                end = Offset(size.width * 0.86f, size.height * 0.50f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun OrbitBadge(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = MaterialTheme.orbitColors.primary,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(OrbitCorners.pill))
            .background(colors.track)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (colors.isDark) color else colors.textPrimary),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.8.sp,
            ),
            color = colors.textSecondary,
        )
    }
}

@Composable
fun TagPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val contentColor = if (!colors.isDark && (color == colors.primary || color == colors.warning)) {
        colors.textPrimary
    } else {
        color
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 10.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.1.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = contentColor,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(OrbitCorners.pill))
            .background(color.copy(alpha = if (colors.isDark) 0.15f else 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** High-contrast circular action. Yellow marks the primary add action. */
@Composable
fun OrbitIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Add,
    size: Dp = 48.dp,
    filled: Boolean = false,
) {
    val colors = MaterialTheme.orbitColors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "iconButtonPress",
    )
    val fill = if (filled) colors.primary else colors.textPrimary
    val content = if (filled) colors.onAccent else colors.backgroundTop
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(fill)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
    }
}

enum class OrbitButtonStyle {
    /** Electric yellow with black text. */
    ACCENT,

    /** Inverted ink surface for high-priority secondary actions. */
    INK,

    /** Neutral raised surface. */
    QUIET,
}

@Composable
fun OrbitPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    style: OrbitButtonStyle = OrbitButtonStyle.ACCENT,
) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(OrbitCorners.inner)
    val fill = when {
        !enabled -> colors.track
        style == OrbitButtonStyle.ACCENT -> colors.primary
        style == OrbitButtonStyle.INK -> colors.textPrimary
        else -> colors.cardElevated
    }
    val contentColor = when {
        !enabled -> colors.textMuted
        style == OrbitButtonStyle.ACCENT -> colors.onAccent
        // INK is an inverted surface; QUIET sits on cardElevated, so it needs
        // the regular text colour instead of the background colour.
        style == OrbitButtonStyle.INK -> colors.backgroundTop
        else -> colors.textPrimary
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "primaryButtonPress",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(fill)
            .then(
                if (style == OrbitButtonStyle.QUIET && enabled) {
                    Modifier.border(1.dp, colors.cardBorder, shape)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
        )
    }
}

/**
 * App mark: a yellow tile with a black orbit. Flat, geometric and unrelated to
 * the glossy launcher art on purpose; screens should not carry 3D decoration.
 */
@Composable
fun OrbitMark(
    modifier: Modifier = Modifier,
    diameter: Dp = 38.dp,
) {
    val colors = MaterialTheme.orbitColors
    val markDescription = stringResource(R.string.content_desc_app_mark)
    Box(
        modifier = modifier
            .size(diameter)
            .clip(RoundedCornerShape(diameter * 0.30f))
            .background(colors.primary)
            .semantics { contentDescription = markDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(diameter * 0.22f),
        ) {
            val ink = colors.onAccent
            val stroke = 1.4.dp.toPx()
            drawCircle(color = ink, radius = size.minDimension * 0.16f, center = center)
            rotate(degrees = -24f, pivot = center) {
                drawOval(
                    color = ink.copy(alpha = 0.82f),
                    topLeft = Offset(0f, size.height * 0.27f),
                    size = Size(size.width, size.height * 0.46f),
                    style = Stroke(width = stroke),
                )
            }
            drawCircle(
                color = ink,
                radius = stroke * 0.9f,
                center = Offset(size.width * 0.82f, size.height * 0.64f),
            )
        }
    }
}

@Composable
fun OrbitNote(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(3.dp)
                .height(14.dp)
                .background(colors.accentLine),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 17.sp),
            color = colors.textMuted,
        )
    }
}

@Composable
fun OrbitEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.orbitColors
    OrbitCard(
        modifier = modifier,
        padding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(colors.accentLine),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = colors.textSecondary,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(10.dp))
            OrbitTextLink(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.padding(start = 0.dp),
            )
        }
    }
}
