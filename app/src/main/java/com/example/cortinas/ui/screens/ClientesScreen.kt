package com.example.cortinas.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cortinas.data.model.Cliente
import com.example.cortinas.data.repository.CortinasRepository
import com.example.cortinas.ui.components.AdaptivePageFrame
import com.example.cortinas.ui.theme.CortinasTheme
import com.example.cortinas.util.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun ClientesScreen(
    repository: CortinasRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clientes by repository.clientes.collectAsState()

    ClientesContent(
        clientes = clientes,
        onSaveCliente = { nuevo ->
            coroutineScope.launch {
                val saved = repository.guardarCliente(nuevo)
                snackbarHostState.showSnackbar(
                    if (saved) "Cliente registrado" else "No se pudo registrar el cliente"
                )
            }
        },
        onDeleteCliente = { id ->
            coroutineScope.launch {
                val deleted = repository.eliminarCliente(id)
                snackbarHostState.showSnackbar(
                    if (deleted) "Cliente eliminado" else "No se pudo eliminar el cliente"
                )
            }
        },
        onWhatsApp = { tel ->
            val clean = tel.replace("[^0-9]".toRegex(), "")
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$clean"))
            context.startActivity(i)
        },
        onCall = { tel ->
            val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
            context.startActivity(i)
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesContent(
    clientes: List<Cliente>,
    onSaveCliente: (Cliente) -> Unit = {},
    onDeleteCliente: (String) -> Unit = {},
    onWhatsApp: (String) -> Unit = {},
    onCall: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    val filteredClientes = remember(clientes, searchQuery) {
        clientes.filter { c ->
            c.nombre.contains(searchQuery, ignoreCase = true) ||
                    c.telefono.contains(searchQuery, ignoreCase = true) ||
                    c.direccion.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Nuevo cliente") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                shape          = MaterialTheme.shapes.large
            )
        },
        modifier = modifier
    ) { padding ->
        AdaptivePageFrame(Modifier.fillMaxSize().padding(padding)) { pageModifier, _ ->
            Column(modifier = pageModifier) {
                // Buscador M3 estilo SearchBar compacto
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    placeholder = { Text("Buscar por nombre o teléfono…") },
                    leadingIcon  = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    shape     = MaterialTheme.shapes.extraLarge,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                if (filteredClientes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.People,
                                    contentDescription = null,
                                    modifier = Modifier.padding(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (searchQuery.isEmpty())
                                    "No hay clientes registrados.\nToca el botón para agregar uno."
                                else
                                    "Sin resultados para \"$searchQuery\"",
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding      = PaddingValues(bottom = 90.dp)
                    ) {
                        items(filteredClientes, key = { it.id }) { cliente ->
                            ClienteCard(
                                cliente        = cliente,
                                onWhatsApp     = { onWhatsApp(cliente.telefono) },
                                onCall         = { onCall(cliente.telefono) },
                                onDelete       = { onDeleteCliente(cliente.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ClienteDialog(
            onDismiss = { showDialog = false },
            onSave    = { nuevo ->
                onSaveCliente(nuevo)
                showDialog = false
            }
        )
    }
}

@Composable
fun ClienteCard(
    cliente: Cliente,
    onWhatsApp: () -> Unit = {},
    onCall: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    ElevatedCard(
        shape     = MaterialTheme.shapes.large,
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape    = MaterialTheme.shapes.extraLarge,
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        cliente.nombre,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Registrado ${FormatUtils.formatDateShort(cliente.fechaRegistro)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Datos de contacto
            if (cliente.telefono.isNotBlank() || cliente.direccion.isNotBlank()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                if (cliente.telefono.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(cliente.telefono, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (cliente.direccion.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Place, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(cliente.direccion, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (cliente.notas.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        cliente.notas,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Acciones
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (cliente.telefono.isNotBlank()) {
                    // WhatsApp
                    FilledTonalIconButton(
                        onClick = onWhatsApp,
                        colors  = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "WhatsApp", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.width(6.dp))
                    // Llamar
                    FilledTonalIconButton(
                        onClick = onCall,
                        colors  = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Llamar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                FilledTonalIconButton(
                    onClick = onDelete,
                    colors  = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ClienteDialog(
    onDismiss: () -> Unit,
    onSave: (Cliente) -> Unit
) {
    var nombre   by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var notas    by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon    = {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        title   = { Text("Nuevo Cliente", fontWeight = FontWeight.Bold) },
        text    = {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value         = nombre,
                    onValueChange = { nombre = it },
                    label         = { Text("Nombre Completo *") },
                    placeholder   = { Text("Ej: Andrea Gómez") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = MaterialTheme.shapes.medium,
                    singleLine    = true,
                    isError       = nombre.isBlank() && nombre.isNotEmpty()
                )
                OutlinedTextField(
                    value              = telefono,
                    onValueChange      = { telefono = it },
                    label              = { Text("Teléfono / WhatsApp") },
                    placeholder        = { Text("Ej: 3101234567") },
                    keyboardOptions    = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier           = Modifier.fillMaxWidth(),
                    shape              = MaterialTheme.shapes.medium,
                    singleLine         = true
                )
                OutlinedTextField(
                    value         = direccion,
                    onValueChange = { direccion = it },
                    label         = { Text("Dirección / Ubicación") },
                    placeholder   = { Text("Ej: Cra 15 # 80-20 Apto 501") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = MaterialTheme.shapes.medium,
                    singleLine    = true
                )
                OutlinedTextField(
                    value         = notas,
                    onValueChange = { notas = it },
                    label         = { Text("Observaciones") },
                    placeholder   = { Text("Ej: Casa nueva en remodelación") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotBlank()) {
                        onSave(Cliente(nombre = nombre, telefono = telefono, direccion = direccion, notas = notas))
                    }
                },
                enabled = nombre.isNotBlank(),
                shape   = MaterialTheme.shapes.medium
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS EN ANDROID STUDIO
// ─────────────────────────────────────────────────────────────────────────────

private val previewSampleClientes = listOf(
    Cliente(
        id = "c1",
        nombre = "Andrea Gómez",
        telefono = "3101234567",
        direccion = "Cra 15 # 80-20 Apto 501",
        notas = "Cliente VIP, cotizar motorizadas",
        fechaRegistro = System.currentTimeMillis() - 86400000L * 5
    ),
    Cliente(
        id = "c2",
        nombre = "Roberto Silva",
        telefono = "3209876543",
        direccion = "Calle 100 # 19-45",
        notas = "Interesado en Blackout para habitaciones",
        fechaRegistro = System.currentTimeMillis() - 86400000L * 2
    )
)

@Preview(name = "1. Clientes - Con Datos", showBackground = true, showSystemUi = true)
@Composable
private fun ClientesScreenPreview() {
    CortinasTheme {
        ClientesContent(clientes = previewSampleClientes)
    }
}

@Preview(name = "2. Clientes - Modo Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, showSystemUi = true)
@Composable
private fun ClientesScreenDarkPreview() {
    CortinasTheme(darkTheme = true) {
        ClientesContent(clientes = previewSampleClientes)
    }
}

@Preview(name = "3. Clientes - Sin Registros", showBackground = true, showSystemUi = true)
@Composable
private fun ClientesScreenEmptyPreview() {
    CortinasTheme {
        ClientesContent(clientes = emptyList())
    }
}

@Preview(name = "4. Tarjeta Cliente Individual", showBackground = true)
@Composable
private fun ClienteCardPreview() {
    CortinasTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ClienteCard(cliente = previewSampleClientes[0])
        }
    }
}

@Preview(name = "5. Diálogo Nuevo Cliente", showBackground = true)
@Composable
private fun ClienteDialogPreview() {
    CortinasTheme {
        ClienteDialog(onDismiss = {}, onSave = {})
    }
}
