package com.example.orbit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.orbitColors

/**
 * Named for source compatibility with the previous glass system. The values
 * are now flat: one solid fill plus one hairline. No blur, gradient, sheen or
 * shadow is drawn, which keeps text crisp and scrolling cheap.
 */
@Immutable
data class GlassStyle(
    val fill: Color,
    val border: Color,
)

@Composable
fun rememberGlassStyle(
    accent: Color? = null,
    emphasized: Boolean = false,
): GlassStyle {
    val colors = MaterialTheme.orbitColors
    return remember(colors, emphasized) {
        GlassStyle(
            fill = if (emphasized) colors.cardElevated else colors.card,
            border = colors.cardBorder,
        )
    }
}

fun Modifier.glassSurface(
    style: GlassStyle,
    shape: Shape,
    elevation: Dp = 0.dp,
): Modifier = this
    .clip(shape)
    .drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawBehind {
            drawOutline(outline, color = style.fill, style = Fill)
            drawOutline(outline, color = style.border, style = Stroke(width = 1.dp.toPx()))
        }
    }

/**
 * Base surface for every content card. Colour is carried by content and
 * selected states, not by a different tint per card.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(20.dp),
    shape: Shape = RoundedCornerShape(OrbitCorners.card),
    accent: Color? = null,
    elevated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = rememberGlassStyle(emphasized = elevated)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.988f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "cardPress",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .glassSurface(style = style, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(padding),
        content = content,
    )
}

/** Flat tinted tile used behind small glyphs. */
@Composable
fun GlassIconTile(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.orbitColors.primary,
    size: Dp = 38.dp,
    shape: Shape = RoundedCornerShape(OrbitCorners.inner),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(tint.copy(alpha = if (colors.isDark) 0.16f else 0.11f)),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Single hairline. */
@Composable
fun GlassDivider(
    modifier: Modifier = Modifier,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp,
) {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = endPadding)
            .height(1.dp)
            .background(colors.cardBorder),
    )
}
