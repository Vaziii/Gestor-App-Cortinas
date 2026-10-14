package com.example.cortinas.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Centers screen content on wide devices and provides consistent, adaptive side gutters. */
@Composable
fun AdaptivePageFrame(
    modifier: Modifier = Modifier,
    content: @Composable (contentModifier: Modifier, compact: Boolean) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < 600.dp
        val maxContentWidth = if (maxWidth >= 840.dp) 840.dp else 760.dp
        val horizontalInset = if (compact) 16.dp else 28.dp
        val contentModifier = Modifier
            .widthIn(max = maxContentWidth)
            .fillMaxSize()
            .align(Alignment.TopCenter)
            .padding(horizontal = horizontalInset)

        content(contentModifier, compact)
    }
}

@Composable
fun ResponsiveFieldPair(
    compact: Boolean,
    modifier: Modifier = Modifier,
    spacing: androidx.compose.ui.unit.Dp = 8.dp,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    if (compact) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing)) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}
