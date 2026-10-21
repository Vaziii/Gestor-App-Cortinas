package com.example.cortinas.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.cortinas.data.model.Cotizacion
import com.example.cortinas.data.model.CotizacionItem
import com.example.cortinas.util.ShareUtils
import java.util.UUID
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cortinas.data.repository.CortinasRepository
import com.example.cortinas.ui.screens.*
import com.example.cortinas.ui.theme.CortinasTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    repository: CortinasRepository,
    onSessionExpired: () -> Unit = {},
    onSignOut: () -> Unit = onSessionExpired,
    modifier: Modifier = Modifier,
    initialCloudAvailable: Boolean = false,
    onCloudStatusChanged: (Boolean, String) -> Unit = { _, _ -> },
    enableAutoSync: Boolean = true
) {
    val navController       = rememberNavController()
    val drawerState         = rememberDrawerState(DrawerValue.Closed)
    val scope               = rememberCoroutineScope()
    var cloudAvailable      by remember(initialCloudAvailable) { mutableStateOf(initialCloudAvailable) }
    var cloudMessage        by remember { mutableStateOf("") }

    // SincronizaciÃ³n automÃ¡tica al inicio
    // CortinasSessionGate valida la sesión y carga los datos antes de abrir esta pantalla.
    LaunchedEffect(Unit) {
        if (enableAutoSync) {
            cloudAvailable = initialCloudAvailable
            onCloudStatusChanged(initialCloudAvailable, "")
            while (true) {
                delay(20_000)
                if (repository.config.isLoginExpired) {
                    repository.supabaseService.cerrarSesion()
                    cloudAvailable = false
                    cloudMessage = "Inicia sesión para continuar la sincronización."
                    onCloudStatusChanged(false, cloudMessage)
                    onSessionExpired()
                    continue
                }
                if (!repository.config.isAuthenticated) continue
                val (ok, message) = repository.sincronizarConNube()
                cloudAvailable = ok
                cloudMessage = message
                onCloudStatusChanged(ok, message)
                if (!repository.config.isAuthenticated) onSessionExpired()
            }
        }
    }
    val screens = listOf(
        Screen.Cotizador,
        Screen.Ventas,
        Screen.Muestrario,
        Screen.Clientes,
        Screen.Ajustes
    )
    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentRoute       = navBackStackEntry?.destination?.route

    fun navigate(screen: Screen) {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Menú",
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                screens.forEach { screen ->
                    val selected = currentRoute == screen.route ||
                        (screen == Screen.Cotizador && currentRoute?.startsWith("cotizar/") == true)
                    NavigationDrawerItem(
                        label = { Text(screen.title) },
                        icon = { Icon(screen.icon, contentDescription = null) },
                        selected = selected,
                        onClick = { navigate(screen) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize()) {
            MainContent(
                modifier = Modifier.fillMaxSize().padding(top = 56.dp),
                repository = repository,
                navController = navController,
                onSignOut = {
                    repository.supabaseService.cerrarSesion()
                    onSignOut()
                    cloudAvailable = false
                    cloudMessage = "Inicia sesión para sincronizar la información en la nube."
                    onCloudStatusChanged(false, cloudMessage)
                }
            )
            IconButton(
                onClick = { scope.launch { drawerState.open() } },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 8.dp, top = 12.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
            }
        }
    }
}

@Preview(name = "Navegación principal", showBackground = true, showSystemUi = true)
@Composable
private fun MainNavigationPreview() {
    CortinasTheme {
        MainNavigation(
            repository = CortinasRepository(LocalContext.current),
            initialCloudAvailable = true,
            enableAutoSync = false
        )
    }
}

@Preview(
    name = "Navegación principal - Modo oscuro",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun MainNavigationDarkPreview() {
    CortinasTheme(darkTheme = true) {
        MainNavigation(
            repository = CortinasRepository(LocalContext.current),
            initialCloudAvailable = true,
            enableAutoSync = false
        )
    }
}

@Composable
private fun MainContent(
    modifier: Modifier,
    repository: CortinasRepository,
    navController: androidx.navigation.NavHostController,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val projectItems = remember { mutableStateListOf<CotizacionItem>() }
    val clientes by repository.clientes.collectAsState()
    var clienteNombre by remember { mutableStateOf("") }
    var clienteId by remember { mutableStateOf("") }
    var clienteTelefono by remember { mutableStateOf("") }
    var clienteDireccion by remember { mutableStateOf("") }
    var savingQuote by remember { mutableStateOf(false) }

    fun saveProject(whatsAppTipo: String? = null) {
        if (savingQuote || projectItems.isEmpty()) return
        savingQuote = true
        val quoteId = UUID.randomUUID().toString()
        val quote = Cotizacion(
            id = quoteId,
            clienteId = clienteId,
            clienteNombre = clienteNombre.ifBlank { "Cliente Ocasional" },
            clienteTelefono = clienteTelefono,
            clienteDireccion = clienteDireccion,
            items = projectItems.map { it.copy(cotizacionId = quoteId) }
        )
        scope.launch {
            val (saved, saveMessage) = repository.guardarCotizacionConResultado(quote)
            savingQuote = false
            if (saved == null) {
                Toast.makeText(context, saveMessage, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Cotización ${saved.codigo.ifBlank { "guardada" }}", Toast.LENGTH_SHORT).show()
                if (whatsAppTipo != null) ShareUtils.shareSupplierQuoteViaWhatsApp(context, saved, whatsAppTipo == "tradicional", repository.config.nombreNegocio)
                projectItems.clear()
                clienteNombre = ""
                clienteId = ""
                clienteTelefono = ""
                clienteDireccion = ""
            }
        }
    }

    Column(modifier) {
        NavHost(
            navController      = navController,
            startDestination   = Screen.Cotizador.route,
            modifier           = Modifier.weight(1f)
        ) {
            composable(Screen.Cotizador.route) {
                CotizarTipoScreen(
                    onMecanicasClick = { navController.navigate(Screen.CotizarMecanicas.route) },
                    onClasicasClick  = { navController.navigate(Screen.CotizarClasicas.route) },
                    projectItems = projectItems,
                    clientes = clientes,
                    onSelectCliente = { cliente ->
                        clienteId = cliente.id
                        clienteNombre = cliente.nombre
                        clienteTelefono = cliente.telefono
                        clienteDireccion = cliente.direccion
                    },
                    clienteNombre = clienteNombre,
                    onClienteNombreChange = { clienteNombre = it },
                    clienteTelefono = clienteTelefono,
                    onClienteTelefonoChange = { clienteTelefono = it },
                    clienteDireccion = clienteDireccion,
                    onClienteDireccionChange = { clienteDireccion = it },
                    onRemoveItem = { index -> if (index in projectItems.indices) projectItems.removeAt(index) },
                    onSaveQuote = { saveProject() },
                    onSendMecanicasWhatsApp = { saveProject("mecanica") },
                    onSendTradicionalesWhatsApp = { saveProject("tradicional") },
                    saving = savingQuote
                )
            }
            composable(Screen.CotizarMecanicas.route) {
                CotizadorScreen(
                    repository = repository,
                    integrated = true,
                    onAddToProject = { projectItems.add(it) },
                    onAdded = { navController.popBackStack() }
                )
            }
            composable(Screen.CotizarClasicas.route) {
                CortinasTradicionalesScreen(
                    repository = repository,
                    onAddToProject = { projectItems.add(it) },
                    onAdded = { navController.popBackStack() }
                )
            }
            composable(Screen.Ventas.route) {
                VentasScreen(repository = repository)
            }
            composable(Screen.Muestrario.route) {
                MuestrarioScreen(repository = repository)
            }
            composable(Screen.Clientes.route) {
                ClientesScreen(repository = repository)
            }
            composable(Screen.Ajustes.route) {
                AjustesScreen(repository = repository, onSignOut = onSignOut)
            }
        }
    }
}



