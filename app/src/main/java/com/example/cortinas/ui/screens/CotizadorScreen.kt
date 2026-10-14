package com.example.cortinas.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cortinas.data.model.*
import com.example.cortinas.data.repository.CortinasRepository
import com.example.cortinas.ui.components.CurtainPreview
import com.example.cortinas.ui.components.AdaptivePageFrame
import com.example.cortinas.ui.components.ResponsiveFieldPair
import com.example.cortinas.ui.components.FabricSelectorCarousel
import com.example.cortinas.ui.components.MechanismControls
import com.example.cortinas.ui.components.AdicionalInput
import com.example.cortinas.ui.theme.CortinasTheme
import com.example.cortinas.ui.theme.WhatsAppGreen
import com.example.cortinas.util.FormatUtils
import com.example.cortinas.util.ShareUtils
import kotlinx.coroutines.launch
import java.util.UUID

// Parser para interpretar tanto punto '.' como coma ',' como separador decimal en metros
fun parseMetros(input: String): Double {
    if (input.isBlank()) return 0.0
    val normalized = input.trim().replace(',', '.')
    return normalized.toDoubleOrNull() ?: 0.0
}

@Composable
fun CotizadorScreen(
    repository: CortinasRepository,
    modifier: Modifier = Modifier
) {
    val telas by repository.telas.collectAsState()
    val clientes by repository.clientes.collectAsState()

    CotizadorContent(
        telas = telas,
        clientes = clientes,
        nombreNegocio = repository.config.nombreNegocio,
        onGuardarCotizacion = { cot -> repository.guardarCotizacion(cot) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CotizadorContent(
    telas: List<Tela>,
    clientes: List<Cliente>,
    nombreNegocio: String = "Cortinas & Estilo",
    onGuardarCotizacion: suspend (Cotizacion) -> Cotizacion? = { it },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var savingCotizacion by remember { mutableStateOf(false) }
    var savedCotizacionDraftKey by remember { mutableStateOf<String?>(null) }
    var savedCotizacion by remember { mutableStateOf<Cotizacion?>(null) }
    var showConfirmGuardarDialog by remember { mutableStateOf(false) }

    // Datos del cliente
    var clienteNombre by remember { mutableStateOf("") }
    var clienteTelefono by remember { mutableStateOf("") }
    var clienteDireccion by remember { mutableStateOf("") }
    var selectedClienteId by remember { mutableStateOf("") }
    var showClientDialog by remember { mutableStateOf(false) }

    // Lista de cortinas
    val itemsCotizacion = remember { mutableStateListOf<CotizacionItem>() }

    // Formulario de la cortina en curso (Medidas en metros, ej: 1.24 o 2,3)
    var ambiente by remember { mutableStateOf("Sala") }
    var anchoStr by remember { mutableStateOf("2.00") }
    var altoStr by remember { mutableStateOf("1.80") }

    // Configuración de 2 cuerpos
    var esDosCuerpos by remember { mutableStateOf(false) }
    var cuerpoEnEdicion by remember { mutableStateOf("IZQUIERDO") }
    var anchoCuerpoSeleccionadoStr by remember { mutableStateOf("") }

    var selectedTela by remember { mutableStateOf<Tela?>(null) }
    var costoMotorStr by remember { mutableStateOf("") }
    var adicionales by remember { mutableStateOf(listOf<AdicionalInput>()) }
    var mandoLado by remember { mutableStateOf("Derecha") }
    var caida by remember { mutableStateOf("Normal") } // "Normal" o "Invertida"

    // Finanzas globales (Descuento monetario o porcentual, sin instalación)
    var descuentoStr by remember { mutableStateOf("") }
    var anticipoStr by remember { mutableStateOf("") }
    var notasCotizacion by remember { mutableStateOf("") }

    val draftKey = remember(
        clienteNombre,
        clienteTelefono,
        clienteDireccion,
        selectedClienteId,
        itemsCotizacion.toList(),
        descuentoStr,
        anticipoStr,
        notasCotizacion
    ) { UUID.randomUUID().toString() }
    val draftCotizacionId = remember(draftKey) { UUID.randomUUID().toString() }

    // Valores calculados interpretando '.' y ','
    val ancho = parseMetros(anchoStr)
    val alto = parseMetros(altoStr)
    val anchoCuerpoManual = parseMetros(anchoCuerpoSeleccionadoStr)

    // Cálculo automático de los 2 cuerpos:
    // Se puede fijar cualquiera de los dos cuerpos; el opuesto ocupa el restante.
    val (anchoCuerpoIzq, anchoCuerpoDer) = remember(esDosCuerpos, ancho, anchoCuerpoSeleccionadoStr, cuerpoEnEdicion) {
        if (esDosCuerpos) {
            if (anchoCuerpoSeleccionadoStr.isBlank()) {
                val mitad = if (ancho > 0) ancho / 2.0 else 0.0
                Pair(mitad, mitad)
            } else if (cuerpoEnEdicion == "IZQUIERDO") {
                Pair(anchoCuerpoManual, ancho - anchoCuerpoManual)
            } else {
                Pair(ancho - anchoCuerpoManual, anchoCuerpoManual)
            }
        } else {
            Pair(0.0, 0.0)
        }
    }
    val cuerposValidos = !esDosCuerpos ||
        (anchoCuerpoIzq > 0.0 && anchoCuerpoDer > 0.0 && anchoCuerpoIzq + anchoCuerpoDer <= ancho + 0.000001)

    val areaM2 = ancho * alto
    val precioTela = selectedTela?.precioM2 ?: 0.0
    val subtotalTela = areaM2 * precioTela
    val costoMotor = parseMetros(costoMotorStr)
    val costoExtras = adicionales.sumOf { parseMetros(it.precio) }
    val subtotalCortinaActual = subtotalTela + costoMotor + costoExtras

    // Totales globales (sin cobro de instalación)
    val subtotalTodasLasCortinas = itemsCotizacion.sumOf { it.totalItem }
    val descuento = parseMetros(descuentoStr)
    val totalVenta = maxOf(0.0, subtotalTodasLasCortinas - descuento)
    val anticipo = parseMetros(anticipoStr)
    val saldoPendiente = maxOf(0.0, totalVenta - anticipo)

    // Preseleccionar primera tela
    LaunchedEffect(telas) {
        if (selectedTela == null && telas.isNotEmpty()) {
            selectedTela = telas.first()
        }
    }

    // Función para construir la cotización con el estado y los abonos correspondientes
    fun buildCotizacion(estado: EstadoCotizacion): Cotizacion {
        val cotId = draftCotizacionId
        val itemsActualizados = itemsCotizacion.map { it.copy(cotizacionId = cotId) }

        val abonos = if (anticipo > 0) {
            listOf(
                AbonoPago(
                    cotizacionId = cotId,
                    monto = anticipo,
                    metodoPago = "Anticipo Inicial",
                    notas = "Abono inicial"
                )
            )
        } else emptyList()

        return Cotizacion(
            id = cotId,
            codigo = "",
            clienteId = selectedClienteId,
            clienteNombre = if (clienteNombre.isNotBlank()) clienteNombre else "Cliente Ocasional",
            clienteTelefono = clienteTelefono,
            clienteDireccion = clienteDireccion,
            fecha = System.currentTimeMillis(),
            items = itemsActualizados,
            costoInstalacionGeneral = 0.0,
            descuento = descuento,
            estado = estado,
            notas = notasCotizacion,
            abonos = abonos
        )
    }

    Scaffold(
        floatingActionButton = {
            SmallFloatingActionButton(onClick = {
                    itemsCotizacion.clear()
                    clienteNombre = ""
                    clienteTelefono = ""
                    clienteDireccion = ""
                    selectedClienteId = ""
                    ambiente = "Sala"
                    anchoStr = "2.00"
                    altoStr = "1.80"
                    esDosCuerpos = false
                    anchoCuerpoSeleccionadoStr = ""
                    cuerpoEnEdicion = "IZQUIERDO"
                    selectedTela = telas.firstOrNull()
                    costoMotorStr = ""
                    adicionales = emptyList()
                    mandoLado = "Derecha"
                    caida = "Normal"
                    descuentoStr = ""
                    anticipoStr = ""
                    notasCotizacion = ""
                    Toast.makeText(context, "Nuevo proyecto iniciado", Toast.LENGTH_SHORT).show()
                }) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reiniciar")
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Total Proyecto (${itemsCotizacion.size} cortinas):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                FormatUtils.formatMoney(totalVenta),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (anticipo > 0) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "Abono: ${FormatUtils.formatMoney(anticipo)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (itemsCotizacion.isEmpty()) {
                                    Toast.makeText(context, "Agrega al menos una cortina para cotizar", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (descuento < 0 || descuento > subtotalTodasLasCortinas) {
                                    Toast.makeText(context, "El descuento debe estar entre cero y el subtotal", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                if (anticipo < 0 || anticipo > totalVenta) {
                                    Toast.makeText(context, "El abono inicial debe estar entre cero y el total de la cotización", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                val cot = buildCotizacion(if (anticipo > 0) EstadoCotizacion.VENDIDA else EstadoCotizacion.COTIZACION)
                                val cotizacionCompartida = savedCotizacion
                                    ?.takeIf { savedCotizacionDraftKey == draftKey }
                                    ?: cot
                                ShareUtils.shareViaWhatsApp(context, cotizacionCompartida, nombreNegocio)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (itemsCotizacion.isEmpty()) {
                                    Toast.makeText(context, "Agrega al menos una cortina al proyecto antes de guardar", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (descuento < 0 || descuento > subtotalTodasLasCortinas) {
                                    Toast.makeText(context, "El descuento debe estar entre cero y el subtotal", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                if (anticipo < 0 || anticipo > totalVenta) {
                                    Toast.makeText(context, "El abono inicial debe estar entre cero y el total de la cotización", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                showConfirmGuardarDialog = true
                            },
                            enabled = !savingCotizacion && savedCotizacionDraftKey != draftKey,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (savingCotizacion) "Guardando…"
                                else if (savedCotizacionDraftKey == draftKey) "Guardado"
                                else "Guardar",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        AdaptivePageFrame(Modifier.fillMaxSize().padding(padding)) { pageModifier, compact ->
        LazyColumn(
            modifier = pageModifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // 1. TARJETA MODERNA DEL CLIENTE
            item {
                ElevatedCard(
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PersonOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cliente de la Cotización", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }

                            if (clientes.isNotEmpty()) {
                                TextButton(onClick = { showClientDialog = true }) {
                                    Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Seleccionar", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = clienteNombre,
                            onValueChange = { clienteNombre = it },
                            label = { Text("Nombre del Cliente") },
                            placeholder = { Text("Ej: Andrea Gómez") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true
                        )
                        ResponsiveFieldPair(
                            compact = compact,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            first = { fieldModifier ->
                                OutlinedTextField(value = clienteTelefono, onValueChange = { clienteTelefono = it }, label = { Text("WhatsApp") }, placeholder = { Text("3001234567") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = fieldModifier, shape = MaterialTheme.shapes.medium, singleLine = true)
                            },
                            second = { fieldModifier ->
                                OutlinedTextField(value = clienteDireccion, onValueChange = { clienteDireccion = it }, label = { Text("Dirección") }, placeholder = { Text("Calle / Apto") }, modifier = fieldModifier, shape = MaterialTheme.shapes.medium, singleLine = true)
                            }
                        )

                    }
                }
            }

            // 2. SIMULADOR VISUAL ANIMADO DE CORTINA (SOPORTA 1 O 2 CUERPOS)
            item {
                CurtainPreview(
                    ancho = ancho,
                    alto = alto,
                    tela = selectedTela,
                    mandoLado = mandoLado,
                    esDosCuerpos = esDosCuerpos,
                    anchoCuerpoIzq = anchoCuerpoIzq,
                    anchoCuerpoDer = anchoCuerpoDer
                )
            }

            // 3. MEDIDAS EN METROS Y DIVISIÓN EN 2 CUERPOS
            item {
                ElevatedCard(
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Medidas de la Cortina", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("En metros (ej: 1.24 o 2,3)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedTextField(
                            value = ambiente,
                            onValueChange = { ambiente = it },
                            label = { Text("Ambiente o Ventana") },
                            placeholder = { Text("Ej: Sala Ventanal, Habitación") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true
                        )
                        ResponsiveFieldPair(
                            compact = compact,
                            modifier = Modifier.fillMaxWidth(),
                            spacing = 10.dp,
                            first = { fieldModifier ->
                                OutlinedTextField(value = anchoStr, onValueChange = { anchoStr = it }, label = { Text("Ancho total (m)") }, placeholder = { Text("Ej: 2.80 o 2,8") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), modifier = fieldModifier, shape = MaterialTheme.shapes.medium, singleLine = true)
                            },
                            second = { fieldModifier ->
                                OutlinedTextField(value = altoStr, onValueChange = { altoStr = it }, label = { Text("Alto total (m)") }, placeholder = { Text("Ej: 1.80 o 2,15") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), modifier = fieldModifier, shape = MaterialTheme.shapes.medium, singleLine = true)
                            }
                        )

                        // Presets rápidos para sumar medidas con 1 toque
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sumar:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SuggestionChip(
                                onClick = {
                                    val a = parseMetros(anchoStr) + 0.10
                                    anchoStr = "%.2f".format(a).replace(",", ".")
                                },
                                label = { Text("+0.10m ancho", style = MaterialTheme.typography.labelSmall) },
                                shape = MaterialTheme.shapes.extraLarge
                            )
                            SuggestionChip(
                                onClick = {
                                    val h = parseMetros(altoStr) + 0.10
                                    altoStr = "%.2f".format(h).replace(",", ".")
                                },
                                label = { Text("+0.10m alto", style = MaterialTheme.typography.labelSmall) },
                                shape = MaterialTheme.shapes.extraLarge
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // APARTADO: DIVISIÓN EN 2 CUERPOS
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Splitscreen,
                                            contentDescription = null,
                                            tint = if (esDosCuerpos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                "¿Cortina dividida en 2 Cuerpos?",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (esDosCuerpos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                if (esDosCuerpos) "2 paños independientes en el mismo ventanal" else "1 solo paño entero de lado a lado",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = esDosCuerpos,
                                        onCheckedChange = { esDosCuerpos = it }
                                    )
                                }

                                AnimatedVisibility(
                                    visible = esDosCuerpos,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(top = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Deja el campo vacío para dividir el ancho en dos partes iguales. También puedes fijar uno de los cuerpos y el otro ocupará el espacio restante.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilterChip(
                                                selected = cuerpoEnEdicion == "IZQUIERDO",
                                                onClick = {
                                                    if (cuerpoEnEdicion != "IZQUIERDO") {
                                                        anchoCuerpoSeleccionadoStr = if (cuerposValidos) anchoCuerpoIzq.toString() else ""
                                                        cuerpoEnEdicion = "IZQUIERDO"
                                                    }
                                                },
                                                label = { Text("Editar izquierdo") }
                                            )
                                            FilterChip(
                                                selected = cuerpoEnEdicion == "DERECHO",
                                                onClick = {
                                                    if (cuerpoEnEdicion != "DERECHO") {
                                                        anchoCuerpoSeleccionadoStr = if (cuerposValidos) anchoCuerpoDer.toString() else ""
                                                        cuerpoEnEdicion = "DERECHO"
                                                    }
                                                },
                                                label = { Text("Editar derecho") }
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = anchoCuerpoSeleccionadoStr,
                                                onValueChange = { anchoCuerpoSeleccionadoStr = it },
                                                label = { Text("Ancho Cuerpo ${if (cuerpoEnEdicion == "IZQUIERDO") "Izquierdo" else "Derecho"} (m)") },
                                                placeholder = { Text("Ej: 1.3 o 1,30") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                                shape = MaterialTheme.shapes.medium,
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                isError = anchoCuerpoSeleccionadoStr.isNotBlank() && !cuerposValidos
                                            )

                                            SuggestionChip(
                                                onClick = { anchoCuerpoSeleccionadoStr = "" },
                                                label = { Text("Mitad y Mitad", fontSize = 11.sp) }
                                            )
                                        }
                                        if (anchoCuerpoSeleccionadoStr.isNotBlank() && !cuerposValidos) {
                                            Text(
                                                "El ancho debe ser mayor que 0 y menor que el ancho total (${FormatUtils.formatNumber(ancho)} m).",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        // Distribución visual de los dos cuerpos calculados
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    "Distribución Resultante de los 2 Paños:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        "◀ Cuerpo Izquierdo: ${FormatUtils.formatNumber(anchoCuerpoIzq)} m",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        "Cuerpo Derecho: ${FormatUtils.formatNumber(anchoCuerpoDer)} m ▶",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Indicador de metros y área total en tiempo real
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Área Total: ${FormatUtils.formatNumber(ancho)}m × ${FormatUtils.formatNumber(alto)}m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = FormatUtils.formatM2(areaM2),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 4. MUESTRARIO VISUAL DE TELAS CON BÚSQUEDA AUTOMÁTICA POR CÓDIGO
            item {
                FabricSelectorCarousel(
                    telas = telas,
                    selectedTela = selectedTela,
                    onSelectTela = { selectedTela = it }
                )
            }

            // 5. MECANISMOS Y EXTRAS (CAÍDA NORMAL O INVERTIDA)
            item {
                MechanismControls(
                    mandoLado = mandoLado,
                    onMandoLadoChange = { mandoLado = it },
                    caida = caida,
                    onCaidaChange = { caida = it },
                    costoMotor = costoMotorStr,
                    onCostoMotorChange = { costoMotorStr = it },
                    adicionales = adicionales,
                    onAdicionalesChange = { adicionales = it }
                )
            }

            // 6. BOTÓN AGREGAR CORTINA CON ANIMACIÓN
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Subtotal esta cortina:", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = FormatUtils.formatMoney(subtotalCortinaActual),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = {
                                if (ancho > 0 && alto > 0 && selectedTela != null && cuerposValidos) {
                                    val nuevoItem = CotizacionItem(
                                        id = UUID.randomUUID().toString(),
                                        ambiente = if (ambiente.isNotBlank()) ambiente else "Ventana ${itemsCotizacion.size + 1}",
                                        ancho = ancho,
                                        alto = alto,
                                        esDosCuerpos = esDosCuerpos,
                                        anchoCuerpoIzq = if (esDosCuerpos) anchoCuerpoIzq else 0.0,
                                        anchoCuerpoDer = if (esDosCuerpos) anchoCuerpoDer else 0.0,
                                        telaId = selectedTela?.id,
                                        telaModelo = "${selectedTela?.modelo} ${selectedTela?.id}".trim(),
                                        telaTipo = selectedTela?.tipo ?: "Translúcida",
                                        precioM2 = precioTela,
                                        costoMotor = costoMotor,
                                        costoExtras = costoExtras,
                                        mandoLado = mandoLado,
                                        caida = caida,
                                        notas = adicionales.mapNotNull { adicional ->
                                            val precio = parseMetros(adicional.precio)
                                            if (adicional.descripcion.isBlank() && precio == 0.0) null
                                            else "Adicional: ${adicional.descripcion.ifBlank { "Sin descripción" }} -- ${FormatUtils.formatMoney(precio)}"
                                        }.joinToString("\n")
                                    )
                                    itemsCotizacion.add(nuevoItem)
                                    // Preparar siguiente ventana
                                    ambiente = "Ventana ${itemsCotizacion.size + 1}"
                                    costoMotorStr = ""
                                    adicionales = emptyList()
                                    Toast.makeText(context, "¡Cortina agregada a la cotización!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val error = if (esDosCuerpos && !cuerposValidos) {
                                        "Cada cuerpo debe tener un ancho mayor que 0 y menor al ancho total"
                                    } else "Ingresa medidas válidas en metros y selecciona tela"
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar al Proyecto", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // 7. LISTA DE CORTINAS CON ANIMACIÓN
            if (itemsCotizacion.isNotEmpty()) {
                item {
                    Text(
                        "Cortinas en esta cotización (${itemsCotizacion.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                itemsIndexed(itemsCotizacion) { index, item ->
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(spring(dampingRatio = 0.7f)) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        ElevatedCard(
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.ambiente, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        if (item.esDosCuerpos) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    "2 Cuerpos",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (item.esDosCuerpos) {
                                        Text(
                                            "Total ${FormatUtils.formatNumber(item.ancho)}m (Izq: ${FormatUtils.formatNumber(item.anchoCuerpoIzq)}m + Der: ${FormatUtils.formatNumber(item.anchoCuerpoDer)}m) × ${FormatUtils.formatNumber(item.alto)}m = ${FormatUtils.formatM2(item.areaM2)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    } else {
                                        Text(
                                            "${FormatUtils.formatNumber(item.ancho)}m × ${FormatUtils.formatNumber(item.alto)}m = ${FormatUtils.formatM2(item.areaM2)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Text(
                                        "${item.telaModelo} • Caída ${item.caida} • Mando ${item.mandoLado}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (item.costoMotor > 0) {
                                        Text("⚡ Motor: ${FormatUtils.formatMoney(item.costoMotor)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706))
                                    }
                                    item.notas.lineSequence().filter { it.isNotBlank() }.forEach { adicional ->
                                        Text(adicional, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        FormatUtils.formatMoney(item.totalItem),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    IconButton(
                                        onClick = { itemsCotizacion.removeAt(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. RESUMEN FINANCIERO Y DESCUENTO (SIN INSTALACIÓN)
            if (itemsCotizacion.isNotEmpty()) {
                item {
                    ElevatedCard(
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Resumen Económico & Descuento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            // BOTONES PREDETERMINADOS DE DESCUENTO (10%, 15%, 20%, 25%, Sin descuento)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Descuentos Rápidos Predeterminados:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val percentOptions = listOf(10, 15, 20, 25)
                                    items(percentOptions) { pct ->
                                        val calculatedDisc = subtotalTodasLasCortinas * (pct / 100.0)
                                        val isCurrent = descuentoStr == calculatedDisc.toLong().toString()
                                        FilterChip(
                                            selected = isCurrent,
                                            onClick = {
                                                descuentoStr = calculatedDisc.toLong().toString()
                                            },
                                            label = { Text("$pct% (-${FormatUtils.formatMoney(calculatedDisc)})", style = MaterialTheme.typography.labelSmall) },
                                            shape = MaterialTheme.shapes.extraLarge
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = descuentoStr.isEmpty() || descuento == 0.0,
                                            onClick = { descuentoStr = "" },
                                            label = { Text("Sin Descuento", style = MaterialTheme.typography.labelSmall) },
                                            shape = MaterialTheme.shapes.extraLarge
                                        )
                                    }
                                }
                            }

                            // Campo manual de descuento monetario
                            OutlinedTextField(
                                value = descuentoStr,
                                onValueChange = { descuentoStr = it },
                                label = { Text("Descuento Monetario ($) a restar") },
                                placeholder = { Text("Ej: 50000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    if (descuento > 0 && subtotalTodasLasCortinas > 0) {
                                        val percent = (descuento / subtotalTodasLasCortinas) * 100
                                        Text(
                                            text = "%.1f%%".format(percent),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                }
                            )

                            // Abono / Anticipo inicial
                            OutlinedTextField(
                                value = anticipoStr,
                                onValueChange = { anticipoStr = it },
                                label = { Text("Abono / Anticipo Recibido ($)") },
                                placeholder = { Text("Ej: 50% de la venta") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Desglose de totales
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal Cortinas:", style = MaterialTheme.typography.bodyMedium)
                                Text(FormatUtils.formatMoney(subtotalTodasLasCortinas), fontWeight = FontWeight.SemiBold)
                            }

                            if (descuento > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Descuento Aplicado:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                    Text("- ${FormatUtils.formatMoney(descuento)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL PROYECTO:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    FormatUtils.formatMoney(totalVenta),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (anticipo > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Abono Recibido:", style = MaterialTheme.typography.bodyMedium)
                                    Text(FormatUtils.formatMoney(anticipo), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // Alerta de Saldo con tokens semánticos
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (saldoPendiente > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (saldoPendiente > 0) "⚠️ Saldo Pendiente por Cobrar:" else "✅ 100% Pagado",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (saldoPendiente > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (saldoPendiente > 0) {
                                        Text(
                                            text = FormatUtils.formatMoney(saldoPendiente),
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}

    // DIÁLOGO DE CONFIRMACIÓN DE GUARDADO (Material 3 Expressive)
    if (showConfirmGuardarDialog) {
        val tieneAbono = anticipo > 0
        AlertDialog(
            onDismissRequest = { showConfirmGuardarDialog = false },
            icon = {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (tieneAbono) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (tieneAbono) Icons.Default.PointOfSale else Icons.Default.Save,
                        contentDescription = null,
                        tint = if (tieneAbono) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (tieneAbono) "¿Registrar como Venta?" else "¿Guardar Cotización?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (clienteNombre.isNotBlank()) "Cliente: $clienteNombre" else "Cliente: Cliente Ocasional",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Total del proyecto: ${FormatUtils.formatMoney(totalVenta)} (${itemsCotizacion.size} cortinas)",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (tieneAbono) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Abono recibido: ${FormatUtils.formatMoney(anticipo)}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Saldo pendiente: ${FormatUtils.formatMoney(saldoPendiente)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Como ingresaste un abono, este proyecto se guardará automáticamente como VENTA.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No se ha ingresado abono inicial. Este proyecto se guardará como COTIZACIÓN PENDIENTE (no vendida).",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmGuardarDialog = false
                        val estadoFinal = if (anticipo > 0) EstadoCotizacion.VENDIDA else EstadoCotizacion.COTIZACION
                        val cot = buildCotizacion(estadoFinal)
                        savingCotizacion = true
                        coroutineScope.launch {
                            try {
                                val cotizacionGuardada = onGuardarCotizacion(cot)
                                if (cotizacionGuardada != null) {
                                    savedCotizacionDraftKey = draftKey
                                    savedCotizacion = cotizacionGuardada
                                    val mensaje = if (estadoFinal == EstadoCotizacion.VENDIDA)
                                        "¡Venta ${cotizacionGuardada.codigo} registrada con abono!"
                                    else
                                        "Cotización ${cotizacionGuardada.codigo} guardada exitosamente"
                                    Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No se pudo guardar la cotización. Intenta de nuevo.", Toast.LENGTH_LONG).show()
                                }
                            } catch (_: Exception) {
                                Toast.makeText(context, "Ocurrió un error al guardar.", Toast.LENGTH_LONG).show()
                            } finally {
                                savingCotizacion = false
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Confirmar y Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmGuardarDialog = false }) {
                    Text("Cancelar")
                }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    // DIÁLOGO SELECCIONAR CLIENTE
    if (showClientDialog) {
        AlertDialog(
            onDismissRequest = { showClientDialog = false },
            icon = {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Contacts,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            },
            title = {
                Text(
                    "Seleccionar Cliente",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    itemsIndexed(clientes) { _, cli ->
                        ListItem(
                            headlineContent = { Text(cli.nombre, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(cli.telefono) },
                            leadingContent = {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                selectedClienteId = cli.id
                                clienteNombre = cli.nombre
                                clienteTelefono = cli.telefono
                                clienteDireccion = cli.direccion
                                showClientDialog = false
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClientDialog = false }) { Text("Cerrar") }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS EN ANDROID STUDIO
// ─────────────────────────────────────────────────────────────────────────────

private val previewSampleCotizadorTelas = listOf(
    Tela(
        id = "t1",
        modelo = "Blackout Premium Gris",
        tipo = "Blackout",
        precioM2 = 75000.0,
        catalogoOrigen = "Catálogo 2026"
    ),
    Tela(
        id = "t2",
        modelo = "Screen 5% Blanco Perla",
        tipo = "Screen / Malla Solar",
        precioM2 = 68000.0,
        catalogoOrigen = "Solar Control"
    )
)

private val previewSampleCotizadorClientes = listOf(
    Cliente(
        id = "c1",
        nombre = "Andrea Gómez",
        telefono = "3101234567",
        direccion = "Cra 15 # 80-20"
    )
)

@Preview(name = "1. Cotizador - Claro", showBackground = true, showSystemUi = true)
@Composable
private fun CotizadorScreenPreview() {
    CortinasTheme {
        CotizadorContent(
            telas = previewSampleCotizadorTelas,
            clientes = previewSampleCotizadorClientes
        )
    }
}

@Preview(name = "2. Cotizador - Modo Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, showSystemUi = true)
@Composable
private fun CotizadorScreenDarkPreview() {
    CortinasTheme(darkTheme = true) {
        CotizadorContent(
            telas = previewSampleCotizadorTelas,
            clientes = previewSampleCotizadorClientes
        )
    }
}
