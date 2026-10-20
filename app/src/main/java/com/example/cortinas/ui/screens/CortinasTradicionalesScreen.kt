package com.example.cortinas.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cortinas.data.model.CotizacionItem
import com.example.cortinas.data.repository.CortinasRepository
import com.example.cortinas.ui.components.AdaptivePageFrame
import com.example.cortinas.util.FormatUtils
import java.util.Locale

private data class TraditionalPriceLine(val label: String, val detail: String, val amount: Double)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CortinasTradicionalesScreen(
    repository: CortinasRepository,
    onAddToProject: (CotizacionItem) -> Unit = {},
    onAdded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val telas by repository.telas.collectAsState()
    val context = LocalContext.current
    var anchoText by remember { mutableStateOf("2.00") }
    var altoText by remember { mutableStateOf("2.00") }
    var ambiente by remember { mutableStateOf("Ventana") }
    var tela by remember { mutableStateOf("Vicillo") }
    var categoria by remember { mutableStateOf("Económico") }
    var codigoCatalogo by remember { mutableStateOf("") }
    var catalogoExpanded by remember { mutableStateOf(false) }
    var onda by remember { mutableStateOf("Ondas perfectas") }
    var riel by remember { mutableStateOf("Riel de ondas perfectas") }
    var incluirCenefa by remember { mutableStateOf(false) }
    var incluirMotor by remember { mutableStateOf(false) }

    val ancho = anchoText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val alto = altoText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val esPesada = tela == "Tela pesada"
    val precioTela = when (tela) {
        "Vicillo" -> if (categoria == "Normal") 33.0 else 24.0
        else -> when (categoria) {
            "Normal" -> 36.0
            "Blackout" -> 35.0
            else -> 28.0
        }
    }
    val cantidadTela = ancho * if (esPesada) 2.6 else 1.0
    val rielesDisponibles = if (onda == "Ondas perfectas") {
        listOf("Sin riel", "Riel de ondas perfectas")
    } else {
        listOf("Sin riel", "Riel armado 3/4 pul", "Riel armado 1/2 pul", "Riel armado 1 pul")
    }
    val incluyeRielPerfecto = onda == "Ondas perfectas" && riel == "Riel de ondas perfectas"
    val rielPrecio = when (riel) {
        "Riel de ondas perfectas" -> 33.0
        "Riel armado 3/4 pul" -> 10.0
        "Riel armado 1/2 pul" -> 8.0
        "Riel armado 1 pul" -> 13.0
        else -> 0.0
    }
    val motorAplicado = incluirMotor && incluyeRielPerfecto
    val lines = buildList {
        add(TraditionalPriceLine(tela, "${quantity(cantidadTela)} m lineales × ${money(precioTela)}/m", cantidadTela * precioTela))
        if (incluirCenefa && onda == "Ondas tradicionales") add(TraditionalPriceLine("Cenefa estilo conchas", "Precio fijo", 80.0))
        if (rielPrecio > 0) add(TraditionalPriceLine(riel, "${quantity(ancho)} m × ${money(rielPrecio)}/m", ancho * rielPrecio))
        if (motorAplicado) add(TraditionalPriceLine("Motor", "Unidad", 240.0))
    }
    val total = lines.sumOf { it.amount }
    val catalogoTradicional = telas.filter {
        it.catalogoOrigen.contains("tradicional", ignoreCase = true) || it.tipo.contains("tradicional", ignoreCase = true)
    }
    val telaCatalogada = catalogoTradicional.firstOrNull { it.id == codigoCatalogo }

    fun buildCurtainItem() = CotizacionItem(
                ambiente = ambiente.ifBlank { "Cortina tradicional" },
                ancho = ancho,
                alto = alto,
                telaId = telaCatalogada?.id,
                telaModelo = listOfNotNull(tela, categoria, codigoCatalogo.takeIf { it.isNotBlank() }).joinToString(" · "),
                telaTipo = "Cortina tradicional",
                costoMotor = if (motorAplicado) 240.0 else 0.0,
                costoExtras = total - if (motorAplicado) 240.0 else 0.0,
                notas = buildString {
                    append("Código de catálogo: ${codigoCatalogo.ifBlank { "Sin seleccionar" }}")
                    append("\nConfección: $onda")
                    append("\nRiel: $riel")
                    if (esPesada) append("\nTela: ancho × 2,6 = ${quantity(cantidadTela)} m lineales")
                    if (incluirCenefa && onda == "Ondas tradicionales") append("\nCenefa estilo conchas: $80.00")
                    lines.forEach { line -> append("\n${line.label}: ${line.detail} = ${money(line.amount)}") }
                }
    )

    fun addCurtainToProject() {
        if (ancho <= 0.0 || alto <= 0.0) {
            Toast.makeText(context, "Ingresa un ancho y alto mayores que cero", Toast.LENGTH_SHORT).show()
            return
        }
        onAddToProject(buildCurtainItem())
        Toast.makeText(context, "Cortina agregada al proyecto", Toast.LENGTH_SHORT).show()
        onAdded()
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Text("Total de la cotización", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(FormatUtils.formatMoney(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { addCurtainToProject() }, modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Agregar cortina al proyecto", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { scaffoldPadding ->
      AdaptivePageFrame(Modifier.fillMaxSize().padding(scaffoldPadding)) { pageModifier, compact ->
       LazyColumn(
        modifier = pageModifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp)
       ) {
        item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Cortinas tradicionales", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Configura tela, confección y accesorios para calcular el precio.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        }

        item {
        ElevatedCard(shape = MaterialTheme.shapes.large, elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Medidas de la cortina", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(ambiente, { ambiente = it }, label = { Text("Ambiente o ventana") }, placeholder = { Text("Ej.: Sala, Dormitorio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = anchoText,
                    onValueChange = { value -> if (value.matches(Regex("[0-9]*[.,]?[0-9]*"))) anchoText = value },
                    label = { Text("Ancho de la cortina") },
                    suffix = { Text("m") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = altoText,
                    onValueChange = { value -> if (value.matches(Regex("[0-9]*[.,]?[0-9]*"))) altoText = value },
                    label = { Text("Alto de la cortina") },
                    suffix = { Text("m") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Tela", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ChoiceRow(listOf("Vicillo", "Tela pesada"), tela) {
                    tela = it
                    categoria = if (it == "Vicillo") "Económico" else "Económica"
                }
                Text("Categoría", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ChoiceRow(if (esPesada) listOf("Económica", "Normal", "Blackout") else listOf("Económico", "Normal"), categoria) { categoria = it }
                ExposedDropdownMenuBox(expanded = catalogoExpanded, onExpandedChange = { catalogoExpanded = it }) {
                    OutlinedTextField(
                        value = codigoCatalogo,
                        onValueChange = { codigoCatalogo = it; catalogoExpanded = true },
                        label = { Text("Código del catálogo tradicional") },
                        placeholder = { Text(if (catalogoTradicional.isEmpty()) "No hay códigos tradicionales guardados" else "Selecciona o escribe un código") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catalogoExpanded) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(expanded = catalogoExpanded && catalogoTradicional.isNotEmpty(), onDismissRequest = { catalogoExpanded = false }) {
                        catalogoTradicional.filter { tela ->
                            codigoCatalogo.isBlank() || tela.id.contains(codigoCatalogo, true) || tela.modelo.contains(codigoCatalogo, true)
                        }.forEach { tela ->
                            DropdownMenuItem(
                                text = { Text("${tela.id} · ${tela.modelo}") },
                                onClick = { codigoCatalogo = tela.id; catalogoExpanded = false }
                            )
                        }
                    }
                }
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                    Text(
                        text = if (esPesada) "Tela calculada: ancho × 2,6 = ${quantity(cantidadTela)} m lineales" else "Tela calculada: ${quantity(cantidadTela)} m lineales",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        }

        item {
        ElevatedCard(shape = MaterialTheme.shapes.large, elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Confección y accesorios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Tipo de onda", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ChoiceRow(listOf("Ondas perfectas", "Ondas tradicionales"), onda) {
                    onda = it
                    if (it != "Ondas tradicionales") incluirCenefa = false
                    if (it == "Ondas tradicionales" && riel == "Riel de ondas perfectas") {
                        riel = "Sin riel"
                        incluirMotor = false
                    }
                }
                if (onda == "Ondas tradicionales") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(checked = incluirCenefa, onCheckedChange = { incluirCenefa = it })
                        Column {
                            Text("Cenefa estilo conchas", fontWeight = FontWeight.Medium)
                            Text("Precio fijo: $80", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text("Riel", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ChoiceRow(rielesDisponibles, riel) {
                    riel = it
                    if (!incluyeRielPerfecto || it != "Riel de ondas perfectas") incluirMotor = false
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = motorAplicado, onCheckedChange = { incluirMotor = it }, enabled = incluyeRielPerfecto)
                    Column {
                        Text("Motor", fontWeight = FontWeight.Medium)
                        Text(if (incluyeRielPerfecto) "$240 · Solo para riel de ondas perfectas" else "Disponible únicamente con riel de ondas perfectas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("Rieles armados: 3/4 pul $10/m · 1/2 pul $8/m · 1 pul $13/m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        }

        item {
        ElevatedCard(shape = MaterialTheme.shapes.large, colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Resumen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                lines.forEach { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(line.label, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(line.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .75f))
                        }
                        Text(money(line.amount), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.height(2.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .2f))
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total estimado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(money(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        }
       }
      }
    }

}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(option) })
        }
    }
}

private fun quantity(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun money(value: Double): String = "$" + String.format(Locale.US, "%.2f", value)

