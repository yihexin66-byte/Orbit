package com.example.orbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.orbit.ui.theme.orbitColors

/**
 * Flat page field. The previous star field and nebula gradients were removed:
 * hierarchy is carried by typography and solid geometry instead of glow.
 */
@Composable
fun SpaceBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundTop),
        content = content,
    )
}
