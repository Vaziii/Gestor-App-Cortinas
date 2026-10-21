package com.example.cortinas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cortinas.data.model.CotizacionItem
import com.example.cortinas.data.model.Cliente
import com.example.cortinas.ui.components.AdaptivePageFrame
import com.example.cortinas.ui.theme.CortinasTheme
import com.example.cortinas.ui.theme.WhatsAppGreen
import com.example.cortinas.util.FormatUtils

@Composable
fun CotizarTipoScreen(
    onMecanicasClick: () -> Unit = {},
    onClasicasClick: () -> Unit = {},
    projectItems: List<CotizacionItem> = emptyList(),
    clientes: List<Cliente> = emptyList(),
    onSelectCliente: (Cliente) -> Unit = {},
    clienteNombre: String = "",
    onClienteNombreChange: (String) -> Unit = {},
    clienteTelefono: String = "",
    onClienteTelefonoChange: (String) -> Unit = {},
    clienteDireccion: String = "",
    onClienteDireccionChange: (String) -> Unit = {},
    onRemoveItem: (Int) -> Unit = {},
    onSaveQuote: () -> Unit = {},
    onSendMecanicasWhatsApp: () -> Unit = {},
    onSendTradicionalesWhatsApp: () -> Unit = {},
    saving: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showClientDialog by remember { mutableStateOf(false) }
    val total = projectItems.sumOf { it.totalItem }
    Scaffold(
        modifier = modifier,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Cotización actual · ${projectItems.size} cortinas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FormatUtils.formatMoney(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onSendMecanicasWhatsApp, enabled = projectItems.any { it.telaTipo != "Cortina tradicional" } && !saving, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen), shape = MaterialTheme.shapes.medium) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Enviar mecánicas", fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = onSendTradicionalesWhatsApp, enabled = projectItems.any { it.telaTipo == "Cortina tradicional" } && !saving, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen), shape = MaterialTheme.shapes.medium) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Enviar tradicionales", fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(onClick = onSaveQuote, enabled = projectItems.isNotEmpty() && !saving, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (saving) "Guardando…" else "Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        AdaptivePageFrame(Modifier.fillMaxSize().padding(padding)) { pageModifier, compact ->
            LazyColumn(pageModifier, verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Nueva cotización", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Agrega cortinas de distintos tipos al mismo proyecto.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    ElevatedCard(shape = MaterialTheme.shapes.large, elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Cliente de la cotización", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (clientes.isNotEmpty()) TextButton(onClick = { showClientDialog = true }) { Text("Seleccionar") }
                            }
                            OutlinedTextField(clienteNombre, onClienteNombreChange, label = { Text("Nombre del cliente") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            if (compact) {
                                OutlinedTextField(clienteTelefono, onClienteTelefonoChange, label = { Text("WhatsApp") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(clienteDireccion, onClienteDireccionChange, label = { Text("Dirección") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(clienteTelefono, onClienteTelefonoChange, label = { Text("WhatsApp") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(clienteDireccion, onClienteDireccionChange, label = { Text("Dirección") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                item { Text("Agregar cortina", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                item {
                    CotizarTipoCard(
                        title = "Cortinas mecánicas",
                        description = "Persianas enrollables, blackout, screen y zebra.",
                        icon = Icons.Default.Calculate,
                        onClick = onMecanicasClick,
                        accentColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
                item {
                    CotizarTipoCard(
                        title = "Cortinas tradicionales",
                        description = "Vicillo, tela pesada, ondas, cenefa y rieles.",
                        icon = Icons.Default.Collections,
                        onClick = onClasicasClick,
                        accentColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
                if (projectItems.isNotEmpty()) {
                    item { Text("Cortinas en esta cotización (${projectItems.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    itemsIndexed(projectItems, key = { _, item -> item.id }) { index, item ->
                        ElevatedCard(shape = MaterialTheme.shapes.large, elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text("${index + 1}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(item.ambiente, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(if (item.telaTipo == "Cortina tradicional") "Tradicional · ${item.telaModelo}" else "Mecánica · ${item.telaModelo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        if (item.telaTipo == "Cortina tradicional") "${FormatUtils.formatNumber(item.ancho)} m x ${FormatUtils.formatNumber(item.alto)} m" else "${FormatUtils.formatNumber(item.ancho)} m × ${FormatUtils.formatNumber(item.alto)} m",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(FormatUtils.formatMoney(item.totalItem), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onRemoveItem(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Quitar ${item.ambiente}", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showClientDialog) {
        AlertDialog(
            onDismissRequest = { showClientDialog = false },
            title = { Text("Seleccionar cliente") },
            text = {
                LazyColumn(Modifier.fillMaxWidth()) {
                    itemsIndexed(clientes) { _, cliente ->
                        TextButton(onClick = { onSelectCliente(cliente); showClientDialog = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("${cliente.nombre} · ${cliente.telefono}", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showClientDialog = false }) { Text("Cerrar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CotizarTipoCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.medium, color = accentColor, modifier = Modifier.size(52.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(12.dp).fillMaxSize())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CotizarTipoScreenPreview() { CortinasTheme { CotizarTipoScreen() } }
