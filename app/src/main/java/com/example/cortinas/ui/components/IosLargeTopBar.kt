package com.example.cortinas.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/**
 * Top app bar con el estilo M3 Expressive: título grande que colapsa al hacer scroll.
 * Reemplaza el antiguo IosLargeTopBar por el componente nativo de Material 3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosLargeTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    LargeTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text       = title,
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        },
        actions = actions,
        colors  = TopAppBarDefaults.largeTopAppBarColors(
            containerColor        = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor     = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}
