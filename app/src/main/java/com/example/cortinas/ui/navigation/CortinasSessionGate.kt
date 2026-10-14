package com.example.cortinas.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cortinas.data.repository.CortinasRepository
import com.example.cortinas.ui.components.AdaptivePageFrame
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun CortinasSessionGate(repository: CortinasRepository) {
    var authenticated      by remember { mutableStateOf(repository.config.isAuthenticated) }
    var checkingSession    by remember { mutableStateOf(repository.config.isAuthenticated) }
    var cloudConnected     by remember { mutableStateOf(false) }
    var connectionMessage  by remember { mutableStateOf("") }
    val scope              = rememberCoroutineScope()
    val sessionInvalidated by repository.config.sessionInvalidated.collectAsState()
    val authorizationDenied by repository.config.authorizationDenied.collectAsState()

    val onSignedIn: () -> Unit = {
        scope.launch {
            checkingSession = true
            authenticated = true
            val (connected, message) = repository.sincronizarConNube()
            connectionMessage = message
            cloudConnected = connected
            if (!connected && !repository.config.isAuthenticated) authenticated = false
            checkingSession = false
        }
    }

    LaunchedEffect(repository) {
        if (repository.config.isAuthenticated && repository.config.requiresFreshLogin) {
            repository.supabaseService.cerrarSesion()
            authenticated = false
            connectionMessage = "La sesión debe renovarse cada 3 días. Inicia sesión para continuar."
        }
        if (repository.config.isAuthenticated) {
            val (sessionOk, sessionMessage) = repository.supabaseService.renovarSesion()
            if (sessionOk) {
                val (connected, message) = repository.sincronizarConNube()
                cloudConnected = connected
                connectionMessage = message
                if (!connected && !repository.config.isAuthenticated) authenticated = false
            } else {
                authenticated = false
                connectionMessage = "No se pudo validar el acceso. Inicia sesión de nuevo."
            }
        }
        checkingSession = false
    }

    LaunchedEffect(repository, authenticated, cloudConnected) {
        while (authenticated && !cloudConnected && repository.config.isAuthenticated) {
            delay(20_000)
            checkingSession = true
            val (connected, message) = repository.sincronizarConNube()
            cloudConnected = connected
            connectionMessage = message
            if (!connected && !repository.config.isAuthenticated) authenticated = false
            checkingSession = false
        }
    }

    when {
        sessionInvalidated -> BusinessLoginScreen(
            repository,
            "La sesión dejó de ser válida. Inicia sesión para continuar.",
            onSignedIn
        )
        checkingSession -> Surface(
            Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement   = Arrangement.Center,
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = com.example.cortinas.R.drawable.logo),
                    contentDescription = "Logo DecorNayeli",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(240.dp).height(156.dp)
                )
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Cargando cotizaciones...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        authenticated && cloudConnected -> MainNavigation(
            repository,
            onSessionExpired = {
                repository.supabaseService.cerrarSesion()
                connectionMessage = "Tu acceso venció. Inicia sesión de nuevo para continuar."
                authenticated = false
                cloudConnected = false
            }
        )
        authenticated -> CloudConnectionScreen(
            message = if (authorizationDenied) {
                "La contraseña es correcta, pero esta cuenta no está autorizada. En Supabase agrega el usuario a public.business_members con active = true para autorizar el acceso."
            } else connectionMessage,
            busy    = checkingSession,
            onRetry = {
                scope.launch {
                    checkingSession = true
                    val (connected, message) = repository.sincronizarConNube()
                    cloudConnected = connected
                    connectionMessage = message
                    if (!connected && !repository.config.isAuthenticated) authenticated = false
                    checkingSession = false
                }
            },
            onSignOut = {
                repository.supabaseService.cerrarSesion()
                authenticated  = false
                cloudConnected = false
            }
        )
        else -> BusinessLoginScreen(repository, connectionMessage, onSignedIn)
    }
}

// ── Pantalla de error de conexión ─────────────────────────────────────────────
@Composable
private fun CloudConnectionScreen(
    message: String,
    busy: Boolean,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AdaptivePageFrame(Modifier.fillMaxSize()) { contentModifier, _ ->
            Column(
                contentModifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape    = MaterialTheme.shapes.extraLarge,
                    color    = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        Icons.Outlined.WifiOff,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Cuenta iniciada",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "CORTINAS · EQUIPO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))
                ElevatedCard(
                    shape     = MaterialTheme.shapes.large,
                    modifier  = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text     = message.ifBlank { "No se pudo actualizar Supabase. Revisa la conexión e inténtalo de nuevo." },
                        modifier = Modifier.padding(16.dp),
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(20.dp))
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text(if (busy) "Conectando…" else "No se pudo actualizar la información.")
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRetry,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Reintentar conexión") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick  = onSignOut,
                    enabled  = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape    = MaterialTheme.shapes.medium
                ) { Text("Cerrar sesión") }
            }
        }
    }
}

// ── Pantalla de inicio de sesión ──────────────────────────────────────────────
@Composable
private fun BusinessLoginScreen(
    repository: CortinasRepository,
    initialMessage: String,
    onSignedIn: () -> Unit
) {
    var email          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var message        by remember { mutableStateOf(initialMessage) }
    var busy           by remember { mutableStateOf(false) }
    var rememberSession by remember { mutableStateOf(repository.config.keepSession) }
    val scope          = rememberCoroutineScope()

    Surface(
        Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AdaptivePageFrame(Modifier.fillMaxSize()) { contentModifier, _ ->
            Column(
                modifier            = contentModifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo / Ícono expresivo
                Surface(
                    shape    = MaterialTheme.shapes.extraLarge,
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(84.dp)
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(22.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Iniciar sesión",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ingresa con tu cuenta del negocio para gestionar cotizaciones del equipo.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))

                OutlinedTextField(
                    value         = email,
                    onValueChange = { email = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Correo electrónico") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine    = true,
                    shape         = MaterialTheme.shapes.medium,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value               = password,
                    onValueChange       = { password = it },
                    modifier            = Modifier.fillMaxWidth(),
                    label               = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions     = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine          = true,
                    shape               = MaterialTheme.shapes.medium,
                    colors              = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value       = rememberSession,
                            role        = Role.Checkbox,
                            onValueChange = { rememberSession = it }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = rememberSession, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar sesión hasta 3 días", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        scope.launch {
                            busy    = true
                            message = ""
                            val (ok, _) = repository.supabaseService.iniciarSesion(email, password, rememberSession)
                            busy = false
                            if (ok) onSignedIn()
                            else message = "No se pudo iniciar sesión. Verifica tu correo y contraseña."
                        }
                    },
                    enabled  = !busy && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape    = MaterialTheme.shapes.medium
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Iniciar sesión", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (message.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        shape  = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            message,
                            modifier = Modifier.padding(12.dp),
                            color    = MaterialTheme.colorScheme.onErrorContainer,
                            style    = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
