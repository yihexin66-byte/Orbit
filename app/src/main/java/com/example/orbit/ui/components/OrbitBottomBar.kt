package com.example.orbit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.ui.navigation.OrbitDestination
import com.example.orbit.ui.theme.orbitColors

enum class OrbitGlyph {
    ORBIT,
    GOALS,
    LEARNING,
    GROWTH,
    PROFILE,
}

/**
 * Stable handle for the five bottom-bar destinations. Segment pills and filter
 * chips reuse the same Chinese labels, so tests should match this tag rather
 * than the visible text.
 */
fun navTabTag(destination: OrbitDestination): String = "orbit_nav_${destination.route}"

@Composable
fun OrbitBottomBar(
    currentRoute: String,
    onNavigate: (OrbitDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(34.dp)
    // Inverted capsule: near-black bar in light mode, warm-white in dark mode.
    val barColor = colors.textPrimary
    val onBar = colors.backgroundTop

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.22f),
                    spotColor = Color.Black.copy(alpha = 0.26f),
                )
                .clip(shape)
                .background(barColor)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrbitDestination.entries.forEach { destination ->
                val selected = destination.isSelected(currentRoute)
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val pressScale by animateFloatAsState(
                    targetValue = if (pressed) 0.94f else 1f,
                    animationSpec = tween(durationMillis = 100),
                    label = "navItemPress",
                )
                val activeFill by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(durationMillis = 180),
                    label = "navItemFill",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) onBar else onBar.copy(alpha = 0.55f),
                    animationSpec = tween(180),
                    label = "navItemContent",
                )
                val label = stringResource(destination.labelRes)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag(navTabTag(destination))
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .selectable(
                            selected = selected,
                            interactionSource = interaction,
                            indication = LocalIndication.current,
                            role = Role.Tab,
                            onClick = { onNavigate(destination) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .graphicsLayer {
                                    scaleX = activeFill
                                    scaleY = activeFill
                                    alpha = activeFill
                                }
                                .clip(CircleShape)
                                .background(colors.primary),
                        )
                        OrbitGlyphIcon(
                            glyph = destination.glyph,
                            tint = if (selected) colors.onAccent else contentColor,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = label,
                        color = contentColor,
                        fontSize = 9.5.sp,
                        lineHeight = 11.sp,
                        letterSpacing = 0.2.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun OrbitGlyphIcon(
    glyph: OrbitGlyph,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        when (glyph) {
            OrbitGlyph.ORBIT -> drawOrbitGlyph(tint)
            OrbitGlyph.GOALS -> drawGoalsGlyph(tint)
            OrbitGlyph.LEARNING -> drawLearningGlyph(tint)
            OrbitGlyph.GROWTH -> drawGrowthGlyph(tint)
            OrbitGlyph.PROFILE -> drawProfileGlyph(tint)
        }
    }
}

private fun DrawScope.drawOrbitGlyph(tint: Color) {
    val stroke = 1.6.dp.toPx()
    drawCircle(
        color = tint,
        radius = size.minDimension * 0.15f,
        center = center,
    )
    rotate(degrees = -24f, pivot = center) {
        drawOval(
            color = tint,
            topLeft = Offset(size.width * 0.02f, size.height * 0.30f),
            size = Size(size.width * 0.96f, size.height * 0.40f),
            style = Stroke(width = stroke),
        )
    }
    drawCircle(
        color = tint,
        radius = stroke * 0.8f,
        center = Offset(size.width * 0.86f, size.height * 0.66f),
    )
}

private fun DrawScope.drawGoalsGlyph(tint: Color) {
    val stroke = 1.6.dp.toPx()
    drawCircle(
        color = tint,
        radius = size.minDimension * 0.42f,
        style = Stroke(width = stroke),
    )
    drawCircle(
        color = tint,
        radius = size.minDimension * 0.20f,
        style = Stroke(width = stroke),
    )
    drawCircle(
        color = tint,
        radius = 1.8.dp.toPx(),
    )
}

/** An open book: the learning tab holds the reading and word-study entries. */
private fun DrawScope.drawLearningGlyph(tint: Color) {
    val stroke = 1.55.dp.toPx()
    val w = size.width
    val h = size.height
    val spineX = w * 0.50f
    val outerX = w * 0.11f
    val rightX = w * 0.89f
    val top = h * 0.26f
    val bottom = h * 0.80f
    val page = Path().apply {
        moveTo(spineX, top + h * 0.05f)
        quadraticBezierTo(w * 0.34f, top - h * 0.05f, outerX, top + h * 0.02f)
        lineTo(outerX, bottom - h * 0.03f)
        quadraticBezierTo(w * 0.34f, bottom - h * 0.11f, spineX, bottom)
        moveTo(spineX, top + h * 0.05f)
        quadraticBezierTo(w * 0.66f, top - h * 0.05f, rightX, top + h * 0.02f)
        lineTo(rightX, bottom - h * 0.03f)
        quadraticBezierTo(w * 0.66f, bottom - h * 0.11f, spineX, bottom)
    }
    drawPath(
        path = page,
        color = tint,
        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawLine(
        color = tint,
        start = Offset(spineX, top + h * 0.06f),
        end = Offset(spineX, bottom),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawGrowthGlyph(tint: Color) {
    val barWidth = size.width * 0.15f
    val heights = listOf(0.30f, 0.55f, 0.82f)
    heights.forEachIndexed { index, heightFraction ->
        val top = size.height * (1f - heightFraction)
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * (0.17f + 0.25f * index), top),
            size = Size(barWidth, size.height * heightFraction),
            cornerRadius = CornerRadius(barWidth / 2f),
        )
    }
}

private fun DrawScope.drawProfileGlyph(tint: Color) {
    val stroke = 1.6.dp.toPx()
    drawCircle(
        color = tint,
        radius = size.minDimension * 0.17f,
        center = Offset(size.width * 0.5f, size.height * 0.30f),
        style = Stroke(width = stroke),
    )
    drawArc(
        color = tint,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(size.width * 0.18f, size.height * 0.48f),
        size = Size(size.width * 0.64f, size.height * 0.42f),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}
