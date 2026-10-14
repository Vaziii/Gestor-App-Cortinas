package com.example.cortinas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cortinas.data.model.Tela
import com.example.cortinas.util.FormatUtils

@Composable
fun FabricSelectorCarousel(
    telas: List<Tela>,
    selectedTela: Tela?,
    onSelectTela: (Tela) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchCode by remember { mutableStateOf("") }
    val code = searchCode.trim()
    val matchedTela = remember(telas, code) {
        if (code.isBlank()) null else telas.firstOrNull { it.id.trim().equals(code, ignoreCase = true) }
    }
    val codeSuggestions = remember(telas, code) {
        if (code.isBlank() || matchedTela != null) emptyList()
        else telas.filter { it.id.trim().startsWith(code, ignoreCase = true) }.take(6)
    }

    LaunchedEffect(matchedTela?.id) {
        matchedTela?.let { tela ->
            if (tela.id != selectedTela?.id) onSelectTela(tela)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Selección de Tela del Muestrario",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = searchCode,
            onValueChange = { searchCode = it },
            placeholder = { Text("Escribe el código exacto de la tela") },
            label = { Text("Buscar por código") },
            leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchCode.isNotEmpty()) {
                    IconButton(onClick = { searchCode = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar código")
                    }
                }
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (codeSuggestions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column {
                    codeSuggestions.forEach { tela ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchCode = tela.id
                                    onSelectTela(tela)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(tela.id, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${tela.modelo} · ${tela.tipo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        when {
            code.isBlank() -> Text(
                text = "Ingresa un código para consultar una tela. El catálogo no se muestra completo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            codeSuggestions.isNotEmpty() -> Unit

            matchedTela == null -> Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No se encontró una tela con el código \"$code\". Verifica el código e inténtalo de nuevo.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                val tela = matchedTela
                val isSelected = selectedTela?.id == tela.id
                val sampleColor = when {
                    tela.tipo.contains("Blackout", true) -> Color(0xFF1E293B)
                    tela.tipo.contains("Screen", true) -> Color(0xFF64748B)
                    tela.tipo.contains("Zebra", true) -> Color(0xFF475569)
                    else -> Color(0xFFE2E8F0)
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth().clickable { onSelectTela(tela) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(sampleColor)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                                Column(modifier = Modifier.padding(start = 10.dp)) {
                                    Text(tela.modelo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Código: ${tela.id}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Tela seleccionada", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(tela.tipo, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
                            }
                            Text(
                                "${FormatUtils.formatMoney(tela.precioM2)}/m²",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (tela.anchoCatalogo.isNotBlank()) {
                            Text("Ancho: ${tela.anchoCatalogo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (tela.catalogoOrigen.isNotBlank()) {
                            Text("Catálogo: ${tela.catalogoOrigen}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (tela.detalles.isNotBlank()) {
                            Text(tela.detalles, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
