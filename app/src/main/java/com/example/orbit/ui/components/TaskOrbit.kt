package com.example.orbit.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.orbit.R
import com.example.orbit.model.Task
import com.example.orbit.model.TaskPriority
import com.example.orbit.ui.orbit.OrbitLayout
import com.example.orbit.ui.theme.orbitColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

const val OrbitTestTag = "home_orbit"
const val OrbitCoreTestTag = "home_orbit_core"

private data class OrbitDrag(
    val taskId: Long,
    val ringIndex: Int,
    val angleDegrees: Float,
    val radiusFraction: Float,
    val startX: Float,
    val startY: Float,
    val targetRing: Int = ringIndex,
    val dragging: Boolean = false,
    val overCore: Boolean = false,
)

/**
 * Display-only placement cache. It survives recompositions and lets a drag
 * commit a new angle without ever touching the database. The cache is cleared
 * when the composable leaves the composition.
 */
private class PlacementCache {
    var placements: List<OrbitLayout.Placement> = emptyList()
    private val manualRings = mutableMapOf<Long, Int>()

    fun ringOverrides(): Map<Long, Int> = manualRings.toMap()

    fun commit(taskId: Long, ringIndex: Int, angleDegrees: Float) {
        manualRings[taskId] = ringIndex
        val known = placements.any { it.taskId == taskId }
        placements = if (known) {
            placements.map { placement ->
                if (placement.taskId == taskId) {
                    placement.copy(ringIndex = ringIndex, angleDegrees = angleDegrees)
                } else {
                    placement
                }
            }
        } else {
            placements + OrbitLayout.Placement(taskId, ringIndex, angleDegrees)
        }
    }
}

/**
 * Interactive task orbit.
 *
 * The parent box owns a single pointer handler: it resolves the touched node
 * against the pure layout, follows the finger along the node's ring and snaps
 * back to the nearest free slot on release. A movement below touch slop remains
 * a tap. Nodes themselves only publish semantics, so drags never fight a child
 * clickable.
 */
@Composable
fun TaskOrbit(
    tasks: List<Task>,
    selectedTaskId: Long?,
    progress: Float,
    completed: Int,
    planned: Int,
    isLoading: Boolean,
    loadFailed: Boolean,
    onSelectTask: (Long) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
    onCompleteTask: (Long) -> Unit = {},
) {
    val colors = MaterialTheme.orbitColors
    val displayedTasks = remember(tasks) { tasks.take(OrbitLayout.MAX_NODES) }
    val specs = displayedTasks.map { task ->
        OrbitLayout.NodeSpec(
            taskId = task.id,
            priority = task.priority,
            isCompleted = task.isCompleted,
        )
    }
    val currentSelectTask by rememberUpdatedState(onSelectTask)
    val currentAddTask by rememberUpdatedState(onAddTask)
    val currentCompleteTask by rememberUpdatedState(onCompleteTask)
    val currentTasks by rememberUpdatedState(displayedTasks)

    val ringDescription = when {
        isLoading -> stringResource(R.string.task_loading)
        loadFailed -> stringResource(R.string.task_load_failed_title)
        else -> stringResource(
            R.string.home_progress_desc,
            (progress * 100).roundToInt(),
            completed,
            planned,
        )
    }
    val percent = (progress * 100).roundToInt()
    val coreDescription = stringResource(R.string.home_orbit_core_desc)
    val armedTitle = stringResource(R.string.home_orbit_armed_title)
    val armedEyebrow = stringResource(R.string.home_orbit_armed_eyebrow)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .testTag(OrbitTestTag),
        ) {
            val density = LocalDensity.current
            val widthDp = maxWidth.value
            val widthPx = constraints.maxWidth.toFloat()
            val orbitCenter = widthPx / 2f
            val nodeSize = OrbitLayout.NODE_DIAMETER_DP.dp
            val nodeSizePx = with(density) { nodeSize.toPx() }
            val hitRadiusPx = with(density) { (OrbitLayout.NODE_HIT_DIAMETER_DP / 2f).dp.toPx() }
            val haptics = LocalHapticFeedback.current
            val placementCache = remember { PlacementCache() }
            var layoutEpoch by remember { mutableIntStateOf(0) }
            var dragState by remember { mutableStateOf<OrbitDrag?>(null) }

            val placements = remember(specs, widthDp, layoutEpoch) {
                OrbitLayout.arrange(
                    nodes = specs,
                    previous = placementCache.placements,
                    widthDp = widthDp,
                    manualRings = placementCache.ringOverrides(),
                ).also { placementCache.placements = it }
            }

            val ringReveal = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                ringReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
                )
            }

            // One-shot flyby after the rings settle. Finite on purpose: the home
            // screen must reach an idle frame for Compose UI tests.
            val comet = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                comet.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1100,
                        delayMillis = 460,
                        easing = LinearEasing,
                    ),
                )
            }

            // A completion pulse is only drawn when the count rises after the
            // first real snapshot; loading the screen must not look like a win.
            val completionBurst = remember { Animatable(0f) }
            var burstBaselineReady by remember { mutableStateOf(false) }
            var previousCompleted by remember { mutableStateOf<Int?>(null) }
            LaunchedEffect(isLoading, loadFailed, completed) {
                if (isLoading || loadFailed) return@LaunchedEffect
                if (!burstBaselineReady) {
                    burstBaselineReady = true
                    previousCompleted = completed
                    return@LaunchedEffect
                }
                val previous = previousCompleted
                previousCompleted = completed
                if (previous != null && completed > previous) {
                    completionBurst.snapTo(0f)
                    completionBurst.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 760,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                }
            }

            val activeDrag = dragState
            val armedTask = activeDrag
                ?.takeIf { it.dragging && it.overCore }
                ?.let { drag -> displayedTasks.firstOrNull { it.id == drag.taskId } }
                ?.takeIf { !it.isCompleted }
            val coreArmedScale by animateFloatAsState(
                targetValue = if (armedTask != null) 1.05f else 1f,
                animationSpec = tween(durationMillis = 140),
                label = "orbitCoreArmedScale",
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val reveal = ringReveal.value
                val lineAccent = colors.accentLine

                // Two drafting rings with slot ticks. Hairlines only: the
                // yellow gauge outside the node track carries the progress.
                OrbitLayout.RING_RADIUS_FRACTIONS.forEachIndexed { index, fraction ->
                    val radius = size.width * fraction
                    drawCircle(
                        color = colors.cardBorder.copy(
                            alpha = if (index == OrbitLayout.OUTER_RING) 1f else 0.55f,
                        ),
                        radius = radius,
                        style = Stroke(width = 1.dp.toPx()),
                        alpha = reveal,
                    )
                    OrbitLayout.slotAngles(index).forEach { angle ->
                        val point = OrbitLayout.pointFor(
                            angleDegrees = angle,
                            radiusFraction = fraction,
                            centerX = center.x,
                            centerY = center.y,
                            widthPx = size.width,
                        )
                        val radians = angle * PI.toFloat() / 180f
                        val half = 2.2.dp.toPx()
                        drawLine(
                            color = colors.cardBorder,
                            start = Offset(
                                point.x - cos(radians) * half,
                                point.y - sin(radians) * half,
                            ),
                            end = Offset(
                                point.x + cos(radians) * half,
                                point.y + sin(radians) * half,
                            ),
                            strokeWidth = 1.dp.toPx(),
                            alpha = reveal * 0.85f,
                        )
                    }
                }

                // Completed nodes are strung together like a constellation:
                // a faint, dashed path that grows with today's progress.
                val completedIds = displayedTasks
                    .asSequence()
                    .filter { it.isCompleted }
                    .map { it.id }
                    .toSet()
                val constellation = placements
                    .filter { it.taskId in completedIds }
                    .sortedBy { it.angleDegrees }
                if (constellation.size >= 2) {
                    val path = Path()
                    constellation.forEachIndexed { index, placement ->
                        val point = OrbitLayout.pointFor(
                            placement = placement,
                            centerX = center.x,
                            centerY = center.y,
                            widthPx = size.width,
                        )
                        if (index == 0) {
                            path.moveTo(point.x, point.y)
                        } else {
                            path.lineTo(point.x, point.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = lineAccent.copy(alpha = 0.30f * reveal),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(2.dp.toPx(), 6.dp.toPx()),
                            ),
                        ),
                    )
                }

                // During a drag the core exposes itself as a dashed drop target.
                val drag = activeDrag
                if (drag?.dragging == true) {
                    val coreRadius = size.width * OrbitLayout.CORE_RADIUS_FRACTION
                    drawCircle(
                        color = if (armedTask != null) {
                            lineAccent
                        } else {
                            colors.textMuted.copy(alpha = 0.55f)
                        },
                        radius = coreRadius,
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(4.dp.toPx(), 6.dp.toPx()),
                            ),
                        ),
                        alpha = if (armedTask != null) 0.95f else 0.5f,
                    )
                    if (armedTask != null) {
                        drawCircle(
                            color = lineAccent.copy(alpha = 0.08f),
                            radius = coreRadius,
                        )
                    }
                }

                // The ring under the finger is the ring the node will snap to.
                // Crossing to the other orbit brightens the dashed target so the
                // drop is predictable before release.
                if (drag?.dragging == true && !drag.overCore) {
                    val targetRadius =
                        size.width * OrbitLayout.RING_RADIUS_FRACTIONS[drag.targetRing]
                    drawCircle(
                        color = lineAccent,
                        radius = targetRadius,
                        style = Stroke(
                            width = 1.4.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(5.dp.toPx(), 7.dp.toPx()),
                            ),
                        ),
                        alpha = if (drag.targetRing != drag.ringIndex) 0.60f else 0.22f,
                    )
                }

                val gaugeRadius = size.width * 0.452f
                drawCircle(
                    color = colors.cardBorder.copy(alpha = 0.45f),
                    radius = gaugeRadius,
                    style = Stroke(width = 2.dp.toPx()),
                    alpha = reveal,
                )
                val sweep = 360f * progress.coerceIn(0f, 1f) * reveal
                if (sweep > 0.4f) {
                    drawArc(
                        color = lineAccent,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - gaugeRadius, center.y - gaugeRadius),
                        size = Size(gaugeRadius * 2f, gaugeRadius * 2f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    )
                    val endRadians = (-90f + sweep) * PI.toFloat() / 180f
                    drawCircle(
                        color = lineAccent,
                        radius = 3.dp.toPx(),
                        center = Offset(
                            center.x + cos(endRadians) * gaugeRadius,
                            center.y + sin(endRadians) * gaugeRadius,
                        ),
                    )
                }

                val selectedPlacement = placements.firstOrNull { it.taskId == selectedTaskId }
                if (selectedPlacement != null) {
                    val point = OrbitLayout.pointFor(
                        angleDegrees = selectedPlacement.angleDegrees,
                        radiusFraction = OrbitLayout.RING_RADIUS_FRACTIONS[
                            selectedPlacement.ringIndex
                        ],
                        centerX = center.x,
                        centerY = center.y,
                        widthPx = size.width,
                    )
                    // A hairline halo instead of a glow, drawn under the node.
                    drawCircle(
                        color = lineAccent,
                        radius = nodeSizePx * 0.60f,
                        center = Offset(point.x, point.y),
                        style = Stroke(width = 1.5.dp.toPx()),
                        alpha = reveal * 0.75f,
                    )
                }

                // A single satellite flies along the progress gauge once after
                // the rings appear, then the screen is fully still.
                val flyby = comet.value
                if (flyby > 0f && flyby < 1f) {
                    val fade = sin((PI * flyby).toFloat()).coerceIn(0f, 1f)
                    val angle = 205f + 250f * flyby
                    val head = OrbitLayout.pointFor(
                        angleDegrees = angle,
                        radiusFraction = 0.452f,
                        centerX = center.x,
                        centerY = center.y,
                        widthPx = size.width,
                    )
                    val tail = OrbitLayout.pointFor(
                        angleDegrees = angle - 8f,
                        radiusFraction = 0.452f,
                        centerX = center.x,
                        centerY = center.y,
                        widthPx = size.width,
                    )
                    drawLine(
                        color = lineAccent.copy(alpha = 0.35f * fade),
                        start = Offset(tail.x, tail.y),
                        end = Offset(head.x, head.y),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = lineAccent.copy(alpha = 0.85f * fade),
                        radius = 2.2.dp.toPx(),
                        center = Offset(head.x, head.y),
                    )
                }

                // Completion pulse: expanding ring plus twelve particles. It is
                // driven by the completed count, so it also plays when a task is
                // finished from the detail card or the list.
                val burst = completionBurst.value
                if (burst > 0f && burst < 1f) {
                    val eased = 1f - (1f - burst) * (1f - burst)
                    val coreRadius = size.width * OrbitLayout.CORE_RADIUS_FRACTION * 0.88f
                    drawCircle(
                        color = lineAccent.copy(alpha = (1f - burst) * 0.8f),
                        radius = coreRadius + size.width * 0.16f * eased,
                        style = Stroke(
                            width = (2.2f - burst).dp.toPx(),
                            cap = StrokeCap.Round,
                        ),
                    )
                    repeat(12) { index ->
                        val radians = (index * 30f + 12f) * PI.toFloat() / 180f
                        val distance = coreRadius +
                            size.width * (0.05f + 0.08f * ((index % 3) / 2f)) * eased
                        drawCircle(
                            color = lineAccent.copy(alpha = (1f - burst) * 0.85f),
                            radius = (2.4f * (1f - burst) + 0.6f).dp.toPx(),
                            center = Offset(
                                center.x + cos(radians) * distance,
                                center.y + sin(radians) * distance,
                            ),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(widthDp.dp * 0.36f)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = ringReveal.value
                        val scale = (0.92f + 0.08f * ringReveal.value) * coreArmedScale
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(colors.primary)
                    // One accessible element for the gauge/drop target. Merging
                    // (instead of clearing) keeps the rendered figure visible to
                    // semantics queries and UI tests while the description stays
                    // the single spoken summary.
                    .semantics(mergeDescendants = true) {
                        contentDescription = if (armedTask != null) {
                            coreDescription
                        } else {
                            ringDescription
                        }
                    }
                    .testTag(OrbitCoreTestTag),
                contentAlignment = Alignment.Center,
            ) {
                if (armedTask != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = armedEyebrow,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = colors.onAccent.copy(alpha = 0.68f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = armedTitle,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 30.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = colors.onAccent,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = armedTask.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                letterSpacing = 0.sp,
                            ),
                            color = colors.onAccent.copy(alpha = 0.64f),
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 22.dp),
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.home_progress_eyebrow),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                letterSpacing = 2.sp,
                            ),
                            color = colors.onAccent.copy(alpha = 0.58f),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = if (isLoading || loadFailed) "—" else stringResource(
                                R.string.stats_percent,
                                percent,
                            ),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 38.sp,
                                lineHeight = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = colors.onAccent,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isLoading || loadFailed) "—" else stringResource(
                                R.string.home_center_ratio,
                                completed,
                                planned,
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = colors.onAccent.copy(alpha = 0.62f),
                        )
                    }
                }
            }

            placements.forEach { placement ->
                val task = displayedTasks.firstOrNull { it.id == placement.taskId }
                    ?: return@forEach
                val index = displayedTasks.indexOfFirst { it.id == task.id }
                val drag = activeDrag?.takeIf { it.taskId == task.id }

                key(task.id) {
                    OrbitTaskNode(
                        task = task,
                        placement = placement,
                        draggingAngle = drag?.angleDegrees,
                        draggingRadiusFraction = drag?.radiusFraction,
                        isDragging = drag?.dragging == true,
                        isOverCore = drag?.overCore == true,
                        isSelected = task.id == selectedTaskId,
                        revealIndex = index,
                        widthPx = widthPx,
                        centerPx = orbitCenter,
                        nodeSize = nodeSize,
                        nodeSizePx = nodeSizePx,
                        onSelectTask = currentSelectTask,
                    )
                }
            }

            // The gesture layer sits above the visuals so nodes can be dragged
            // without any child clickable stealing the pointer stream.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(placements, widthPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val hit = OrbitLayout.hitTest(
                                placements = placements,
                                x = down.position.x,
                                y = down.position.y,
                                centerX = orbitCenter,
                                centerY = orbitCenter,
                                widthPx = widthPx,
                                hitRadiusPx = hitRadiusPx,
                            )
                            if (hit == null) return@awaitEachGesture

                            down.consume()
                            var gesture = OrbitDrag(
                                taskId = hit.taskId,
                                ringIndex = hit.ringIndex,
                                angleDegrees = hit.angleDegrees,
                                radiusFraction = OrbitLayout.RING_RADIUS_FRACTIONS[hit.ringIndex],
                                startX = down.position.x,
                                startY = down.position.y,
                            )
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes
                                        .firstOrNull { it.id == down.id }
                                        ?: break
                                    if (change.changedToUpIgnoreConsumed()) {
                                        if (gesture.dragging) {
                                            if (gesture.overCore) {
                                                // Released inside the core: the
                                                // pending task is completed and
                                                // the count-driven pulse plays.
                                                haptics.performHapticFeedback(
                                                    HapticFeedbackType.LongPress,
                                                )
                                                currentCompleteTask(gesture.taskId)
                                            } else {
                                                val snapped = OrbitLayout.snapAngle(
                                                    placements = placements,
                                                    taskId = gesture.taskId,
                                                    ringIndex = gesture.targetRing,
                                                    proposedAngle = gesture.angleDegrees,
                                                    widthDp = widthDp,
                                                )
                                                if (gesture.targetRing != gesture.ringIndex) {
                                                    haptics.performHapticFeedback(
                                                        HapticFeedbackType.LongPress,
                                                    )
                                                }
                                                placementCache.commit(
                                                    taskId = gesture.taskId,
                                                    ringIndex = gesture.targetRing,
                                                    angleDegrees = snapped,
                                                )
                                                layoutEpoch += 1
                                            }
                                        } else {
                                            currentSelectTask(gesture.taskId)
                                        }
                                        change.consume()
                                        break
                                    }
                                    if (!change.pressed) break

                                    val distance = hypot(
                                        change.position.x - gesture.startX,
                                        change.position.y - gesture.startY,
                                    )
                                    if (!gesture.dragging && distance > viewConfiguration.touchSlop) {
                                        gesture = gesture.copy(dragging = true)
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    if (gesture.dragging) {
                                        val task = currentTasks.firstOrNull {
                                            it.id == gesture.taskId
                                        }
                                        val nextRadius =
                                            OrbitLayout.radiusFractionForPoint(
                                                x = change.position.x,
                                                y = change.position.y,
                                                centerX = orbitCenter,
                                                centerY = orbitCenter,
                                                widthPx = widthPx,
                                            )
                                        // Completed nodes may be dragged but
                                        // never un-completed by the gesture.
                                        val overCore = task != null &&
                                            !task.isCompleted &&
                                            OrbitLayout.isOverCore(
                                                x = change.position.x,
                                                y = change.position.y,
                                                centerX = orbitCenter,
                                                centerY = orbitCenter,
                                                widthPx = widthPx,
                                            )
                                        val targetRing = OrbitLayout.nearestRing(nextRadius)
                                        if (
                                            targetRing != gesture.targetRing &&
                                            !overCore
                                        ) {
                                            haptics.performHapticFeedback(
                                                HapticFeedbackType.TextHandleMove,
                                            )
                                        }
                                        gesture = gesture.copy(
                                            angleDegrees = OrbitLayout.angleForPoint(
                                                x = change.position.x,
                                                y = change.position.y,
                                                centerX = orbitCenter,
                                                centerY = orbitCenter,
                                                fallbackAngle = gesture.angleDegrees,
                                            ),
                                            radiusFraction = nextRadius,
                                            targetRing = targetRing,
                                            overCore = overCore,
                                        )
                                        dragState = gesture
                                        change.consume()
                                    }
                                }
                            } finally {
                                dragState = null
                            }
                        }
                    },
            )

            if (!isLoading && !loadFailed && displayedTasks.isEmpty()) {
                val ghostPoint = OrbitLayout.pointFor(
                    angleDegrees = 0f,
                    radiusFraction = OrbitLayout.RING_RADIUS_FRACTIONS[OrbitLayout.INNER_RING],
                    centerX = orbitCenter,
                    centerY = orbitCenter,
                    widthPx = widthPx,
                )
                val ghostSize = 58.dp
                val ghostSizePx = with(density) { ghostSize.toPx() }
                OrbitGhostNode(
                    onAddTask = currentAddTask,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (ghostPoint.x - ghostSizePx / 2f).roundToInt(),
                                y = (ghostPoint.y - ghostSizePx / 2f).roundToInt(),
                            )
                        }
                        .size(ghostSize),
                )
            }
        }
    }
}

@Composable
private fun OrbitTaskNode(
    task: Task,
    placement: OrbitLayout.Placement,
    draggingAngle: Float?,
    draggingRadiusFraction: Float?,
    isDragging: Boolean,
    isOverCore: Boolean,
    isSelected: Boolean,
    revealIndex: Int,
    widthPx: Float,
    centerPx: Float,
    nodeSize: Dp,
    nodeSizePx: Float,
    onSelectTask: (Long) -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val priorityLabel = stringResource(task.priority.labelRes)
    val status = stringResource(
        if (task.isCompleted) R.string.status_completed else R.string.status_pending,
    )
    val description = stringResource(
        R.string.orbit_node_desc,
        task.title,
        priorityLabel,
        status,
    )
    val actionLabel = stringResource(R.string.orbit_node_action, task.title)

    val targetAngle = draggingAngle ?: placement.angleDegrees
    val animatedAngle = animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = if (isDragging) {
            snap()
        } else {
            spring(dampingRatio = 0.76f, stiffness = 360f)
        },
        label = "orbitNodeAngle",
    )
    val animatedRing = animateFloatAsState(
        targetValue = draggingRadiusFraction
            ?: OrbitLayout.RING_RADIUS_FRACTIONS[placement.ringIndex],
        animationSpec = if (isDragging) {
            snap()
        } else {
            spring(dampingRatio = 0.78f, stiffness = 280f)
        },
        label = "orbitNodeRing",
    )
    val selectedScale = animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "orbitNodeSelectedScale",
    )
    val draggingScale = animateFloatAsState(
        targetValue = when {
            isOverCore -> 0.72f
            isDragging -> 1.16f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.66f, stiffness = Spring.StiffnessMedium),
        label = "orbitNodeDragScale",
    )
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 440,
                delayMillis = revealIndex.coerceIn(0, OrbitLayout.MAX_NODES) * 55,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    val checkAlpha = animateFloatAsState(
        targetValue = if (task.isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "orbitNodeCheckAlpha",
    )
    val checkScale = animateFloatAsState(
        targetValue = if (task.isCompleted) 1f else 0.55f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 480f),
        label = "orbitNodeCheckScale",
    )
    val labelAlpha = animateFloatAsState(
        targetValue = if (task.isCompleted) 0f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "orbitNodeLabelAlpha",
    )

    Box(
        modifier = Modifier
            .offset {
                val point = OrbitLayout.pointFor(
                    angleDegrees = animatedAngle.value,
                    radiusFraction = animatedRing.value,
                    centerX = centerPx,
                    centerY = centerPx,
                    widthPx = widthPx,
                )
                IntOffset(
                    x = (point.x - nodeSizePx / 2f).roundToInt(),
                    y = (point.y - nodeSizePx / 2f).roundToInt(),
                )
            }
            .size(nodeSize)
            .graphicsLayer {
                val entry = reveal.value
                val scale = (0.72f + 0.28f * entry) *
                    selectedScale.value *
                    draggingScale.value
                scaleX = scale
                scaleY = scale
                alpha = entry * if (isOverCore) 0.55f else 1f
            }
            .zIndex(if (isDragging) 4f else if (isSelected) 3f else 1f)
            .clearAndSetSemantics {
                contentDescription = description
                selected = isSelected
                role = Role.Button
                onClick(label = actionLabel) {
                    onSelectTask(task.id)
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val edge = size.minDimension / 2f
            val fill = when {
                isSelected -> colors.primary
                task.isCompleted -> colors.textPrimary
                else -> colors.card
            }
            val outline = when {
                isSelected -> colors.onAccent
                task.isCompleted -> colors.textPrimary
                task.priority == TaskPriority.HIGH -> colors.accentLine
                else -> colors.textMuted.copy(alpha = 0.64f)
            }
            drawCircle(color = fill, radius = edge)
            drawCircle(
                color = outline,
                radius = edge - 0.7.dp.toPx(),
                style = Stroke(
                    width = when {
                        isSelected -> 1.dp.toPx()
                        task.priority == TaskPriority.HIGH && !task.isCompleted -> 1.8.dp.toPx()
                        else -> 1.2.dp.toPx()
                    },
                ),
            )
        }
        val nodeContent = when {
            isSelected -> colors.onAccent
            task.isCompleted -> colors.backgroundTop
            else -> colors.textPrimary
        }
        Text(
            text = remember(task.title) {
                task.title.trim().take(1).uppercase()
            },
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
            ),
            color = nodeContent,
            modifier = Modifier.graphicsLayer { alpha = labelAlpha.value },
        )
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = nodeContent,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    alpha = checkAlpha.value
                    scaleX = checkScale.value
                    scaleY = checkScale.value
                },
        )
    }
}

@Composable
private fun OrbitGhostNode(
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    val description = stringResource(R.string.home_orbit_add_node)
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 520,
                delayMillis = 260,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onAddTask)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(label = description) {
                    onAddTask()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = reveal.value
                    val scale = 0.74f + 0.26f * reveal.value
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            val edge = size.minDimension / 2f
            drawCircle(
                color = colors.card,
                radius = edge * 0.70f,
                center = center,
            )
            drawCircle(
                color = colors.accentLine,
                radius = edge * 0.70f,
                style = Stroke(
                    width = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(3.dp.toPx(), 5.dp.toPx()),
                    ),
                ),
            )
        }
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = colors.accentLine,
            modifier = Modifier.size(20.dp),
        )
    }
}
