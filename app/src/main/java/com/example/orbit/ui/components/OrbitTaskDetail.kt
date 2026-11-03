package com.example.orbit.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.model.Task
import com.example.orbit.ui.theme.orbitColors

/**
 * Detail panel for the selected orbit node. It renders only the task passed in
 * from [com.example.orbit.ui.TaskUiState], so edits, completion and deletion
 * made elsewhere are reflected without a second local copy of the task.
 */
@Composable
fun OrbitTaskDetail(
    task: Task,
    today: Long,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    goalTitle: String? = null,
    stageTitle: String? = null,
) {
    val colors = MaterialTheme.orbitColors
    val closeLabel = stringResource(R.string.home_detail_close)
    val actionLabel = stringResource(
        if (task.isCompleted) R.string.home_detail_reopen else R.string.home_detail_complete,
    )

    // The selected node is surfaced as one floating glass focus panel rather
    // than repeating its title in a second strip above the orbit.
    OrbitCard(
        modifier = modifier.animateContentSize(),
        padding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        accent = colors.primary,
        elevated = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.home_detail_label),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = colors.accentLine,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onClose)
                    .semantics { contentDescription = closeLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (!task.description.isNullOrBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.textMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        TaskMetaFlow(
            task = task,
            today = today,
            goalTitle = goalTitle,
            stageTitle = stageTitle,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailActionButton(
                text = actionLabel,
                icon = if (task.isCompleted) Icons.Filled.Refresh else Icons.Filled.Check,
                onClick = onToggle,
            )
            Spacer(Modifier.weight(1f))
            DetailEditLink(onClick = onEdit)
        }
    }
}

@Composable
private fun DetailActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(colors.textPrimary)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.backgroundTop,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = colors.backgroundTop,
        )
    }
}

@Composable
private fun DetailEditLink(
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.home_detail_edit),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}
