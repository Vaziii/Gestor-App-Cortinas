package com.example.cortinas.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cortinas.data.model.Tela
import com.example.cortinas.data.repository.CortinasRepository
import com.example.cortinas.ui.components.AdaptivePageFrame
import com.example.cortinas.ui.theme.CortinasTheme
import com.example.cortinas.util.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun MuestrarioScreen(
    repository: CortinasRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val telas by repository.telas.collectAsState()

    MuestrarioContent(
        telas = telas,
        onSaveTela = { nuevaTela ->
            coroutineScope.launch { repository.guardarTela(nuevaTela) }
        },
        onDeleteTela = { id ->
            coroutineScope.launch { repository.eliminarTela(id) }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuestrarioContent(
    telas: List<Tela>,
    onSaveTela: (Tela) -> Unit = {},
    onDeleteTela: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }
    var showDialog by remember { mutableStateOf(false) }
    var telaToEdit by remember { mutableStateOf<Tela?>(null) }

    val categories = listOf("Todas", "Translúcida", "Blackout", "Screen / Malla Solar", "Zebra / Sheer")

    val filteredTelas = remember(telas, searchQuery, selectedCategory) {
        telas.filter { tela ->
            val matchesCategory = if (selectedCategory == "Todas") true
            else tela.tipo.contains(selectedCategory.split(" ").first(), ignoreCase = true)
            val matchesSearch = tela.modelo.contains(searchQuery, ignoreCase = true) ||
                    tela.id.contains(searchQuery, ignoreCase = true) ||
                    tela.catalogoOrigen.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    telaToEdit = null
                    showDialog = true
                },
                icon          = { Icon(Icons.Default.Add, contentDescription = null) },
                text          = { Text("Agregar tela") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                shape          = MaterialTheme.shapes.large
            )
        },
        modifier = modifier
    ) { padding ->
        AdaptivePageFrame(Modifier.fillMaxSize().padding(padding)) { pageModifier, _ ->
            Column(modifier = pageModifier) {
                // Buscador
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    placeholder   = { Text("Buscar por modelo, código, catálogo…") },
                    leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon  = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    shape      = MaterialTheme.shapes.extraLarge,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Chips de categoría
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected  = selectedCategory == cat,
                            onClick   = { selectedCategory = cat },
                            label     = { Text(cat, style = MaterialTheme.typography.labelLarge) },
                            shape     = MaterialTheme.shapes.extraLarge,
                            colors    = FilterChipDefaults.filterChipColors(
                                selectedContainerColor    = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor        = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                if (filteredTelas.isEmpty()) {
                    Box(
                        modifier         = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape    = MaterialTheme.shapes.extraLarge,
                                color    = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.padding(20.dp),
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text      = if (searchQuery.isEmpty())
                                    "No hay telas en esta categoría.\nToca el botón para agregar una."
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
                        items(filteredTelas, key = { it.id }) { tela ->
                            TelaItemCard(
                                tela     = tela,
                                onEdit   = { telaToEdit = tela; showDialog = true },
                                onDelete = { onDeleteTela(tela.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        TelaDialog(
            telaInitial = telaToEdit,
            onDismiss   = { showDialog = false },
            onSave      = { nuevaTela ->
                onSaveTela(nuevaTela)
                showDialog = false
            }
        )
    }
}

@Composable
fun TelaItemCard(
    tela: Tela,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    ElevatedCard(
        shape     = MaterialTheme.shapes.large,
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tela.modelo,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${tela.tipo} • ${tela.catalogoOrigen.ifBlank { "Sin catálogo" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "${FormatUtils.formatMoney(tela.precioM2)}/m²",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (tela.detalles.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    tela.detalles,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onEdit,
                    colors  = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.width(6.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDialog(
    telaInitial: Tela?,
    onDismiss: () -> Unit,
    onSave: (Tela) -> Unit
) {
    var codigo by remember { mutableStateOf(telaInitial?.id ?: "") }
    var modelo by remember { mutableStateOf(telaInitial?.modelo ?: "") }
    var tipo by remember { mutableStateOf(telaInitial?.tipo ?: "Translúcida") }
    var precioM2Str by remember { mutableStateOf(if (telaInitial != null && telaInitial.precioM2 > 0) telaInitial.precioM2.toLong().toString() else "") }
    var catalogoOrigen by remember { mutableStateOf(telaInitial?.catalogoOrigen ?: "") }
    var detalles by remember { mutableStateOf(telaInitial?.detalles ?: "") }

    val tipos = listOf("Translúcida", "Blackout", "Screen / Malla Solar", "Zebra / Sheer", "Otra")
    var expandedTipo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        title = {
            Text(
                if (telaInitial == null) "Nueva Tela" else "Editar Tela",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = modelo,
                    onValueChange = { modelo = it },
                    label = { Text("Nombre / Modelo de la tela *") },
                    placeholder = { Text("Ej: Blackout Lino Gris") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    isError = modelo.isBlank() && modelo.isNotEmpty()
                )
                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = !expandedTipo }
                ) {
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de tela") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        tipos.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = { tipo = opt; expandedTipo = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = catalogoOrigen,
                    onValueChange = { catalogoOrigen = it },
                    label = { Text("Catálogo de origen") },
                    placeholder = { Text("Ej: Catálogo 2026") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                OutlinedTextField(
                    value = precioM2Str,
                    onValueChange = { precioM2Str = it },
                    label = { Text("Precio por m² *") },
                    placeholder = { Text("Ej: 65000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                OutlinedTextField(
                    value = detalles,
                    onValueChange = { detalles = it },
                    label = { Text("Detalles técnicos / Notas") },
                    placeholder = { Text("Ej: Ancho rollo 2.5m, lavable") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (modelo.isNotBlank()) {
                        val precio = precioM2Str.toDoubleOrNull() ?: 0.0
                        val nueva = telaInitial?.copy(
                            modelo = modelo,
                            tipo = tipo,
                            catalogoOrigen = catalogoOrigen,
                            precioM2 = precio,
                            detalles = detalles
                        ) ?: Tela(
                            id = codigo.trim().ifBlank { java.util.UUID.randomUUID().toString() },
                            modelo = modelo,
                            tipo = tipo,
                            catalogoOrigen = catalogoOrigen,
                            precioM2 = precio,
                            detalles = detalles
                        )
                        onSave(nueva)
                    }
                },
                enabled = modelo.isNotBlank(),
                shape = MaterialTheme.shapes.medium
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

private val previewSampleTelas = listOf(
    Tela(
        id = "t1",
        modelo = "Blackout Premium Gris",
        tipo = "Blackout",
        precioM2 = 75000.0,
        catalogoOrigen = "Catálogo 2026",
        detalles = "Bloqueo total 100% de luz solar, textura suave"
    ),
    Tela(
        id = "t2",
        modelo = "Screen 5% Blanco Perla",
        tipo = "Screen / Malla Solar",
        precioM2 = 68000.0,
        catalogoOrigen = "Solar Control",
        detalles = "Visibilidad al exterior con control térmico"
    ),
    Tela(
        id = "t3",
        modelo = "Sheer Elegance Dúo Beige",
        tipo = "Zebra / Sheer",
        precioM2 = 92000.0,
        catalogoOrigen = "Lujo Interior",
        detalles = "Franjas translúcidas y opacas intercaladas"
    )
)

@Preview(name = "1. Muestrario - Con Datos", showBackground = true, showSystemUi = true)
@Composable
private fun MuestrarioScreenPreview() {
    CortinasTheme {
        MuestrarioContent(telas = previewSampleTelas)
    }
}

@Preview(name = "2. Muestrario - Modo Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, showSystemUi = true)
@Composable
private fun MuestrarioScreenDarkPreview() {
    CortinasTheme(darkTheme = true) {
        MuestrarioContent(telas = previewSampleTelas)
    }
}

@Preview(name = "3. Muestrario - Sin Registros", showBackground = true, showSystemUi = true)
@Composable
private fun MuestrarioScreenEmptyPreview() {
    CortinasTheme {
        MuestrarioContent(telas = emptyList())
    }
}

@Preview(name = "4. Tarjeta Tela Individual", showBackground = true)
@Composable
private fun TelaItemCardPreview() {
    CortinasTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TelaItemCard(tela = previewSampleTelas[0])
        }
    }
}

@Preview(name = "5. Diálogo Nueva Tela", showBackground = true)
@Composable
private fun TelaDialogPreview() {
    CortinasTheme {
        TelaDialog(telaInitial = null, onDismiss = {}, onSave = {})
    }
}
