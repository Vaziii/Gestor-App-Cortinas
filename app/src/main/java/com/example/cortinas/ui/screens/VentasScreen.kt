package com.example.cortinas.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cortinas.data.model.AbonoPago
import com.example.cortinas.data.model.Cotizacion
import com.example.cortinas.data.model.CotizacionItem
import com.example.cortinas.data.model.EstadoCotizacion
import com.example.cortinas.data.repository.CortinasRepository
import com.example.cortinas.ui.components.AdaptivePageFrame
import com.example.cortinas.ui.theme.CortinasTheme
import com.example.cortinas.ui.theme.WhatsAppGreen
import com.example.cortinas.util.FormatUtils
import com.example.cortinas.util.InvoicePdfUtils
import com.example.cortinas.util.ShareUtils
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun VentasScreen(
    repository: CortinasRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cotizaciones by repository.cotizaciones.collectAsState()

    VentasContent(
        cotizaciones = cotizaciones,
        onRegistrarAbono = { abono ->
            coroutineScope.launch {
                val registrado = repository.registrarAbono(abono)
                val mensaje = if (registrado) "¡Abono registrado con éxito!" else "El abono supera el saldo o no se pudo guardar."
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        },
        onCambiarEstado = { id, nuevoEstado ->
            coroutineScope.launch {
                repository.cambiarEstadoCotizacion(id, nuevoEstado)
                val texto = if (nuevoEstado == EstadoCotizacion.EN_PROCESO) "Estado: En Fabricación" else "Estado: ${nuevoEstado.titulo}"
                Toast.makeText(context, texto, Toast.LENGTH_SHORT).show()
            }
        },
        onEliminarCotizacion = { id ->
            coroutineScope.launch {
                repository.eliminarCotizacion(id)
                Toast.makeText(context, "Registro eliminado", Toast.LENGTH_SHORT).show()
            }
        },
        onCompartirFactura = { cot, nombre, identificacion ->
            try {
                InvoicePdfUtils.share(context, cot, nombre, identificacion)
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage ?: "No se pudo crear la factura", Toast.LENGTH_LONG).show()
            }
        },
        onEnviarProveedor = { cotizacion, tradicionales ->
            ShareUtils.shareSupplierQuoteViaWhatsApp(context, cotizacion, tradicionales, repository.config.nombreNegocio)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasContent(
    cotizaciones: List<Cotizacion>,
    onRegistrarAbono: (AbonoPago) -> Unit = {},
    onCambiarEstado: (String, EstadoCotizacion) -> Unit = { _, _ -> },
    onEliminarCotizacion: (String) -> Unit = {},
    onCompartirFactura: (Cotizacion, String, String) -> Unit = { _, _, _ -> },
    onEnviarProveedor: (Cotizacion, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val tabTitles = listOf("Pendientes de Abono", "Todas las Ventas", "Completamente Pagadas", "Cotizaciones")

    var selectedCotizacionForAbono by remember { mutableStateOf<Cotizacion?>(null) }
    var selectedCotizacionForInvoice by remember { mutableStateOf<Cotizacion?>(null) }
    var selectedCotizacionForDetail by remember { mutableStateOf<Cotizacion?>(null) }

    // Métricas del Dashboard
    val totalVendido = remember(cotizaciones) {
        cotizaciones.filter { it.estado != EstadoCotizacion.CANCELADA && it.estado != EstadoCotizacion.COTIZACION }.sumOf { it.totalVenta }
    }
    val totalAbonado = remember(cotizaciones) {
        cotizaciones.filter { it.estado != EstadoCotizacion.CANCELADA && it.estado != EstadoCotizacion.COTIZACION }.sumOf { it.totalAbonado }
    }
    val totalSaldoPorCobrar = remember(cotizaciones) {
        cotizaciones.filter { it.estado != EstadoCotizacion.CANCELADA && it.estado != EstadoCotizacion.COTIZACION }.sumOf { it.saldoPendiente }
    }

    val porcentajeTotalRecaudado = if (totalVendido > 0) (totalAbonado / totalVendido).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = porcentajeTotalRecaudado,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progressAnimation"
    )

    // Filtrar cotizaciones según la pestaña y por nombre del cliente / teléfono / código
    val filteredCotizaciones = remember(cotizaciones, selectedTab, searchQuery) {
        val byTab = when (selectedTab) {
            0 -> cotizaciones.filter { it.saldoPendiente > 0 && it.estado != EstadoCotizacion.CANCELADA }
            1 -> cotizaciones.filter { it.estado != EstadoCotizacion.COTIZACION }
            2 -> cotizaciones.filter { it.estaPagadaTotalmente }
            3 -> cotizaciones.filter { it.estado == EstadoCotizacion.COTIZACION }
            else -> cotizaciones
        }
        val query = searchQuery.trim()
        if (query.isBlank()) {
            byTab
        } else {
            byTab.filter { c ->
                c.clienteNombre.contains(query, ignoreCase = true) ||
                c.clienteTelefono.contains(query, ignoreCase = true) ||
                c.codigo.contains(query, ignoreCase = true) ||
                c.items.any { item ->
                    item.ambiente.contains(query, ignoreCase = true) ||
                    item.telaModelo.contains(query, ignoreCase = true)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
    ) { padding ->
        AdaptivePageFrame(Modifier.fillMaxSize().padding(padding)) { pageModifier, _ ->
            Column(modifier = pageModifier) {
                // PANEL DASHBOARD M3 EXPRESSIVE
                ElevatedCard(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Text(
                                    text = "Panel de Cobranza & Caja",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${(animatedProgress * 100).toInt()}% Recaudado",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    "Total vendido",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    FormatUtils.formatMoney(totalVendido),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Saldo por cobrar",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    FormatUtils.formatMoney(totalSaldoPorCobrar),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Barra de progreso expresiva
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                        )
                    }
                }

                // BARRA DE BÚSQUEDA EXPRESIVA M3
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por cliente, teléfono o código…") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    singleLine = true
                )

                // PESTAÑAS M3 CON FILTER CHIPS EXPRESIVOS
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(tabTitles.indices.toList()) { index ->
                        val isSelected = selectedTab == index
                        val count = when (index) {
                            0 -> cotizaciones.count { it.saldoPendiente > 0 && it.estado != EstadoCotizacion.CANCELADA }
                            1 -> cotizaciones.count { it.estado != EstadoCotizacion.COTIZACION }
                            2 -> cotizaciones.count { it.estaPagadaTotalmente }
                            3 -> cotizaciones.count { it.estado == EstadoCotizacion.COTIZACION }
                            else -> 0
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            label = {
                                Text(
                                    text = "${tabTitles[index]} ($count)",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            shape = MaterialTheme.shapes.extraLarge,
                            leadingIcon = if (index == 0 && count > 0) {
                                {
                                    Icon(
                                        Icons.Outlined.PendingActions,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }

                // LISTA DE VENTAS CON ELEVATED CARDS
                if (filteredCotizaciones.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
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
                                    Icons.Outlined.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                            Text(
                                text = if (searchQuery.isBlank()) "No hay registros en esta sección."
                                else "No se encontraron cotizaciones para \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(filteredCotizaciones, key = { it.id }) { cotizacion ->
                            VentaItemCard(
                                cotizacion = cotizacion,
                                isExpanded = false,
                                onToggleExpand = { selectedCotizacionForDetail = cotizacion },
                                onOpenAbono = { selectedCotizacionForAbono = cotizacion },
                                onCambiarEstado = { nuevoEstado -> onCambiarEstado(cotizacion.id, nuevoEstado) },
                                onCompartir = { selectedCotizacionForInvoice = cotizacion },
                                onEliminar = { onEliminarCotizacion(cotizacion.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedCotizacionForDetail?.let { cotizacion ->
        Dialog(
            onDismissRequest = { selectedCotizacionForDetail = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedCotizacionForDetail = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                        Text("Detalle de cotización", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
                    ) {
                        item {
                            VentaItemCard(
                                cotizacion = cotizacion,
                                isExpanded = true,
                                onToggleExpand = { selectedCotizacionForDetail = null },
                                onOpenAbono = { selectedCotizacionForAbono = cotizacion },
                                onCambiarEstado = { nuevoEstado -> onCambiarEstado(cotizacion.id, nuevoEstado) },
                                onCompartir = { selectedCotizacionForInvoice = cotizacion },
                                onEliminar = {
                                    onEliminarCotizacion(cotizacion.id)
                                    selectedCotizacionForDetail = null
                                }
                            )
                        }
                        item {
                            val tieneMecanicas = cotizacion.items.any { it.telaTipo != "Cortina tradicional" }
                            val tieneTradicionales = cotizacion.items.any { it.telaTipo == "Cortina tradicional" }
                            if (tieneMecanicas || tieneTradicionales) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (tieneMecanicas) {
                                        Button(
                                            onClick = { onEnviarProveedor(cotizacion, false) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Enviar mecánicas", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (tieneTradicionales) {
                                        Button(
                                            onClick = { onEnviarProveedor(cotizacion, true) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Enviar tradicionales", fontWeight = FontWeight.Bold)
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

    // MODAL REGISTRAR ABONO
    if (selectedCotizacionForAbono != null) {
        RegistrarAbonoDialog(
            cotizacion = selectedCotizacionForAbono!!,
            onDismiss = { selectedCotizacionForAbono = null },
            onConfirmAbono = { abono ->
                onRegistrarAbono(abono)
                selectedCotizacionForAbono = null
            }
        )
    }
    selectedCotizacionForInvoice?.let { cotizacion ->
        InvoiceShareDialog(
            cotizacion = cotizacion,
            onDismiss = { selectedCotizacionForInvoice = null },
            onShare = { nombre, identificacion ->
                onCompartirFactura(cotizacion, nombre, identificacion)
                selectedCotizacionForInvoice = null
            }
        )
    }
}

@Composable
private fun InvoiceShareDialog(cotizacion: Cotizacion, onDismiss: () -> Unit, onShare: (String, String) -> Unit) {
    var nombre by remember(cotizacion.id) { mutableStateOf(cotizacion.clienteNombre) }
    var identificacion by remember(cotizacion.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compartir factura") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Revisa o completa los datos del cliente para esta factura. Los datos de la cotización se mantienen sin cambios.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre / Razón Social") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = identificacion,
                    onValueChange = { identificacion = it },
                    label = { Text("RUC / Cédula (opcional)") },
                    placeholder = { Text("Agregar identificación") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Se compartirá como PDF de venta. La integración y autorización del SRI se añadirá en una etapa posterior.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { onShare(nombre.trim(), identificacion.trim()) }, enabled = nombre.isNotBlank()) { Text("Compartir PDF") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/**
 * Tarjeta individual de Venta / Cotización en Material 3 Expressive.
 */
@Composable
fun VentaItemCard(
    cotizacion: Cotizacion,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenAbono: () -> Unit,
    onCambiarEstado: (EstadoCotizacion) -> Unit,
    onCompartir: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .then(if (isExpanded) Modifier else Modifier.clickable { onToggleExpand() })
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Encabezado de la venta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = cotizacion.codigo,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Estado Badge con tonos M3
                    val (badgeColor, badgeTextColor) = when (cotizacion.estado) {
                        EstadoCotizacion.COTIZACION -> Pair(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
                        EstadoCotizacion.VENDIDA -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                        EstadoCotizacion.EN_PROCESO -> Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                        EstadoCotizacion.INSTALADA -> Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                        EstadoCotizacion.CANCELADA -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = badgeColor
                    ) {
                        Text(
                            text = cotizacion.estado.titulo,
                            color = badgeTextColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = cotizacion.clienteNombre,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${FormatUtils.formatDateShort(cotizacion.fecha)} • ${cotizacion.items.size} cortinas (${FormatUtils.formatM2(cotizacion.totalMetrosCuadrados)})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Barra de progreso individual
            LinearProgressIndicator(
                progress = { cotizacion.porcentajePagado },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.small),
                color = if (cotizacion.estaPagadaTotalmente) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fila de Saldos y Totales con contenedores tonales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        FormatUtils.formatMoney(cotizacion.totalVenta),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Column {
                    Text("Abonado:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        FormatUtils.formatMoney(cotizacion.totalAbonado),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Saldo Pendiente:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (cotizacion.saldoPendiente > 0) FormatUtils.formatMoney(cotizacion.saldoPendiente) else "✅ PAGADO",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (cotizacion.saldoPendiente > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            if (cotizacion.estado != EstadoCotizacion.COTIZACION) {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onCompartir,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(Icons.Outlined.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir factura PDF", fontWeight = FontWeight.Bold)
                }
            }

            // CONTENIDO EXPANDIBLE ANIMADO
            if (isExpanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Text(
                    "Cortinas Confeccionadas:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    cotizacion.items.forEach { item ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.ambiente,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    val detalleMedidas = if (item.telaTipo == "Cortina tradicional") {
                                        "${FormatUtils.formatNumber(item.ancho)}m x ${FormatUtils.formatNumber(item.alto)}m"
                                    } else if (item.esDosCuerpos) {
                                        "${FormatUtils.formatNumber(item.ancho)}m (2 Cuerpos: ${FormatUtils.formatNumber(item.anchoCuerpoIzq)}m + ${FormatUtils.formatNumber(item.anchoCuerpoDer)}m) × ${FormatUtils.formatNumber(item.alto)}m"
                                    } else {
                                        "${FormatUtils.formatNumber(item.ancho)}m × ${FormatUtils.formatNumber(item.alto)}m"
                                    }
                                    Text(
                                        "$detalleMedidas • ${item.telaModelo}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    FormatUtils.formatMoney(item.totalItem),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Historial de abonos
                if (cotizacion.abonos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Historial de Abonos:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        cotizacion.abonos.forEach { abono ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "• ${FormatUtils.formatDateShort(abono.fecha)} (${abono.metodoPago})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    FormatUtils.formatMoney(abono.monto),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Acciones de Estado
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { onCambiarEstado(EstadoCotizacion.EN_PROCESO) },
                        label = { Text("A Fabricación", style = MaterialTheme.typography.labelSmall) },
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    SuggestionChip(
                        onClick = { onCambiarEstado(EstadoCotizacion.INSTALADA) },
                        label = { Text("Instalada", style = MaterialTheme.typography.labelSmall) },
                        shape = MaterialTheme.shapes.extraLarge
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // BOTONES DE ACCIÓN RÁPIDA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (cotizacion.saldoPendiente > 0) {
                        Button(
                            onClick = onOpenAbono,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Registrar Abono", fontWeight = FontWeight.Bold)
                        }
                    }

                    FilledTonalIconButton(
                        onClick = onEliminar,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Diálogo Modal para registrar abonos parciales o saldar la venta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarAbonoDialog(
    cotizacion: Cotizacion,
    onDismiss: () -> Unit,
    onConfirmAbono: (AbonoPago) -> Unit
) {
    var montoStr by remember { mutableStateOf("") }
    var metodoPago by remember { mutableStateOf("Efectivo") }
    var referencia by remember { mutableStateOf("") }
    val notas by remember { mutableStateOf("") }
    val monto = parseMetros(montoStr)

    val metodos = listOf("Efectivo", "Transferencia", "Tarjeta Débito/Crédito", "Otro")
    var expandedMetodo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.AddCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Registrar Abono", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${cotizacion.codigo} • ${cotizacion.clienteNombre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Saldo Pendiente:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            FormatUtils.formatMoney(cotizacion.saldoPendiente),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Botones rápidos: [Saldar Total] o [50%]
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(
                        onClick = { montoStr = cotizacion.saldoPendiente.toLong().toString() },
                        label = { Text("Pagar Todo (100%)", style = MaterialTheme.typography.labelSmall) },
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    SuggestionChip(
                        onClick = { montoStr = (cotizacion.saldoPendiente / 2).toLong().toString() },
                        label = { Text("Abonar 50%", style = MaterialTheme.typography.labelSmall) },
                        shape = MaterialTheme.shapes.extraLarge
                    )
                }

                OutlinedTextField(
                    value = montoStr,
                    onValueChange = { montoStr = it },
                    label = { Text("Monto a Abonar ($) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )

                // Selector de Método de Pago
                ExposedDropdownMenuBox(
                    expanded = expandedMetodo,
                    onExpandedChange = { expandedMetodo = !expandedMetodo }
                ) {
                    OutlinedTextField(
                        value = metodoPago,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Método de Pago") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMetodo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMetodo,
                        onDismissRequest = { expandedMetodo = false }
                    ) {
                        metodos.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    metodoPago = m
                                    if (m == "Efectivo") referencia = ""
                                    expandedMetodo = false
                                }
                            )
                        }
                    }
                }

                if (metodoPago != "Efectivo") {
                    OutlinedTextField(
                        value = referencia,
                        onValueChange = { referencia = it },
                        label = { Text("Comprobante / N° Transacción") },
                        placeholder = { Text("Ej: Aprobación 74920") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (monto > 0) {
                        val abono = AbonoPago(
                            id = UUID.randomUUID().toString(),
                            cotizacionId = cotizacion.id,
                            monto = monto,
                            fecha = System.currentTimeMillis(),
                            metodoPago = metodoPago,
                            referencia = referencia,
                            notas = notas
                        )
                        onConfirmAbono(abono)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                enabled = monto in 0.01..cotizacion.saldoPendiente
            ) {
                Text("Confirmar Abono")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS EN ANDROID STUDIO
// ─────────────────────────────────────────────────────────────────────────────

private val previewSampleCotizaciones = listOf(
    Cotizacion(
        id = "preview-1",
        codigo = "COT-001",
        clienteNombre = "Carlos Mendoza",
        clienteTelefono = "3101234567",
        clienteDireccion = "Cra 45 # 123-45",
        fecha = System.currentTimeMillis() - 86400000L,
        items = listOf(
            CotizacionItem(
                id = "item-1",
                cotizacionId = "preview-1",
                ambiente = "Sala Principal",
                ancho = 2.4,
                alto = 2.1,
                esDosCuerpos = true,
                anchoCuerpoIzq = 1.2,
                anchoCuerpoDer = 1.2,
                telaModelo = "Blackout Premium Gris",
                precioM2 = 75000.0,
                costoMotor = 180000.0
            ),
            CotizacionItem(
                id = "item-2",
                cotizacionId = "preview-1",
                ambiente = "Habitación Principal",
                ancho = 1.8,
                alto = 2.0,
                telaModelo = "Screen 5% Blanco",
                precioM2 = 68000.0
            )
        ),
        costoInstalacionGeneral = 0.0,
        descuento = 50000.0,
        estado = EstadoCotizacion.VENDIDA,
        notas = "Instalar después de las 2pm",
        abonos = listOf(
            AbonoPago(
                id = "abono-1",
                cotizacionId = "preview-1",
                monto = 400000.0,
                metodoPago = "Transferencia",
                referencia = "Nequi #98421"
            )
        )
    ),
    Cotizacion(
        id = "preview-2",
        codigo = "COT-002",
        clienteNombre = "María Fernanda Gómez",
        clienteTelefono = "3209876543",
        clienteDireccion = "Calle 100 # 15-20",
        fecha = System.currentTimeMillis() - 172800000L,
        items = listOf(
            CotizacionItem(
                id = "item-3",
                cotizacionId = "preview-2",
                ambiente = "Estudio",
                ancho = 1.5,
                alto = 1.6,
                telaModelo = "Zebra Dúo Beige",
                precioM2 = 92000.0
            )
        ),
        costoInstalacionGeneral = 0.0,
        descuento = 0.0,
        estado = EstadoCotizacion.VENDIDA,
        abonos = listOf(
            AbonoPago(
                id = "abono-2",
                cotizacionId = "preview-2",
                monto = 220800.0,
                metodoPago = "Efectivo"
            )
        )
    ),
    Cotizacion(
        id = "preview-3",
        codigo = "COT-003",
        clienteNombre = "Juan David Restrepo",
        clienteTelefono = "3015551234",
        clienteDireccion = "Poblado Torre 2",
        fecha = System.currentTimeMillis(),
        items = listOf(
            CotizacionItem(
                id = "item-4",
                cotizacionId = "preview-3",
                ambiente = "Balcón",
                ancho = 3.0,
                alto = 2.4,
                telaModelo = "Screen 1% Gris Carbón",
                precioM2 = 85000.0
            )
        ),
        costoInstalacionGeneral = 0.0,
        descuento = 20000.0,
        estado = EstadoCotizacion.COTIZACION
    )
)

@Preview(name = "1. Ventas - Con Datos", showBackground = true, showSystemUi = true)
@Composable
private fun VentasScreenPreview() {
    CortinasTheme {
        VentasContent(cotizaciones = previewSampleCotizaciones)
    }
}

@Preview(name = "2. Ventas - Modo Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, showSystemUi = true)
@Composable
private fun VentasScreenDarkPreview() {
    CortinasTheme(darkTheme = true) {
        VentasContent(cotizaciones = previewSampleCotizaciones)
    }
}

@Preview(name = "3. Ventas - Sin Registros", showBackground = true, showSystemUi = true)
@Composable
private fun VentasScreenEmptyPreview() {
    CortinasTheme {
        VentasContent(cotizaciones = emptyList())
    }
}

@Preview(name = "4. Tarjeta Venta - Colapsada", showBackground = true)
@Composable
private fun VentaItemCardCollapsedPreview() {
    CortinasTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            VentaItemCard(
                cotizacion = previewSampleCotizaciones[0],
                isExpanded = false,
                onToggleExpand = {},
                onOpenAbono = {},
                onCambiarEstado = {},
                onCompartir = {},
                onEliminar = {}
            )
        }
    }
}

@Preview(name = "5. Tarjeta Venta - Expandida", showBackground = true)
@Composable
private fun VentaItemCardExpandedPreview() {
    CortinasTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            VentaItemCard(
                cotizacion = previewSampleCotizaciones[0],
                isExpanded = true,
                onToggleExpand = {},
                onOpenAbono = {},
                onCambiarEstado = {},
                onCompartir = {},
                onEliminar = {}
            )
        }
    }
}

@Preview(name = "6. Diálogo Registrar Abono", showBackground = true)
@Composable
private fun RegistrarAbonoDialogPreview() {
    CortinasTheme {
        RegistrarAbonoDialog(
            cotizacion = previewSampleCotizaciones[0],
            onDismiss = {},
            onConfirmAbono = {}
        )
    }
}
