package com.example.cortinas.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cortinas.ui.theme.CortinasTheme

@Composable
fun CotizarTipoScreen(
    onMecanicasClick: () -> Unit = {},
    onClasicasClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text  = "Nueva cotización",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text  = "Selecciona el tipo de cortina",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        CotizarTipoCard(
            title       = "Cortinas mecánicas",
            description = "Persianas enrollables, blackout, screen y zebra con mecanismo de tracción.",
            icon        = Icons.Default.Calculate,
            onClick     = onMecanicasClick,
            accentColor = MaterialTheme.colorScheme.primaryContainer
        )
        CotizarTipoCard(
            title       = "Cortinas clásicas de tela",
            description = "Cotización de cortinas tradicionales de tela, paneles y romanas.",
            icon        = Icons.Default.Collections,
            onClick     = onClasicasClick,
            accentColor = MaterialTheme.colorScheme.secondaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CotizarTipoCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = accentColor,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurface,
                    modifier           = Modifier
                        .padding(12.dp)
                        .fillMaxSize()
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(name = "Cotizar Tipo - Claro", showBackground = true, showSystemUi = true)
@Composable
private fun CotizarTipoScreenPreview() {
    CortinasTheme {
        CotizarTipoScreen()
    }
}

@Preview(name = "Cotizar Tipo - Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, showSystemUi = true)
@Composable
private fun CotizarTipoScreenDarkPreview() {
    CortinasTheme(darkTheme = true) {
        CotizarTipoScreen()
    }
}
