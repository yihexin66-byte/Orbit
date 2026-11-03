package com.example.orbit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.orbit.R
import com.example.orbit.ui.theme.orbitColors

const val AiAssistantFabTag = "ai_assistant_fab"

/**
 * Opacity rules for the floating assistant. The disc starts opaque in its
 * home corner; once the learner parks it somewhere else it steps back to a
 * see-through 55%, and closer to any edge it retreats to 32% so text behind it
 * stays readable. Touching or dragging always restores full opacity.
 */
object AiAssistantFabVisibility {

    const val FULL_ALPHA = 1f
    const val MOVED_ALPHA = 0.55f
    const val EDGE_ALPHA = 0.32f

    fun restingAlpha(moved: Boolean, nearEdge: Boolean): Float = when {
        !moved -> FULL_ALPHA
        nearEdge -> EDGE_ALPHA
        else -> MOVED_ALPHA
    }

    fun isDimmed(alpha: Float): Boolean = alpha < 0.99f
}

/**
 * Free-floating entry to the assistant. A compact volt disc is the only
 * saturated element on the home screen, so it reads as the primary AI action
 * without covering the metric card behind it. The caller owns the offset so the
 * disc can be dragged anywhere inside the content area.
 */
@Composable
fun AiAssistantFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragging: Boolean = false,
    idleAlpha: Float = 1f,
) {
    val colors = MaterialTheme.orbitColors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            dragging -> 1.08f
            pressed -> 0.93f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 100),
        label = "aiFabPress",
    )
    val alpha by animateFloatAsState(
        targetValue = if (dragging || pressed) 1f else idleAlpha,
        animationSpec = tween(durationMillis = 260),
        label = "aiFabIdleAlpha",
    )
    val label = stringResource(R.string.ai_assistant_fab_desc)
    val appearance = stringResource(
        if (AiAssistantFabVisibility.isDimmed(idleAlpha)) {
            R.string.ai_assistant_fab_dimmed
        } else {
            R.string.ai_assistant_fab_visible
        },
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .shadow(
                elevation = if (dragging) 20.dp else 12.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.28f),
            )
            .clip(CircleShape)
            .background(colors.primary)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                stateDescription = appearance
            }
            .testTag(AiAssistantFabTag)
            .size(58.dp),
        contentAlignment = Alignment.Center,
    ) {
        AiSparkGlyph(
            tint = colors.onAccent,
            modifier = Modifier.size(26.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 11.dp, end = 11.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(colors.onAccent.copy(alpha = 0.55f)),
        )
    }
}

@Composable
fun AiSparkGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawAiSpark(tint)
    }
}

private fun DrawScope.drawAiSpark(tint: Color) {
    val centerX = size.width * 0.46f
    val centerY = size.height * 0.46f
    val radius = size.minDimension * 0.36f
    val path = Path().apply {
        moveTo(centerX, centerY - radius)
        quadraticTo(
            centerX + radius * 0.16f,
            centerY - radius * 0.16f,
            centerX + radius,
            centerY,
        )
        quadraticTo(
            centerX + radius * 0.16f,
            centerY + radius * 0.16f,
            centerX,
            centerY + radius,
        )
        quadraticTo(
            centerX - radius * 0.16f,
            centerY + radius * 0.16f,
            centerX - radius,
            centerY,
        )
        quadraticTo(
            centerX - radius * 0.16f,
            centerY - radius * 0.16f,
            centerX,
            centerY - radius,
        )
        close()
    }
    drawPath(path, color = tint)
    drawCircle(
        color = tint.copy(alpha = 0.78f),
        radius = size.minDimension * 0.075f,
        center = Offset(size.width * 0.84f, size.height * 0.20f),
    )
}
