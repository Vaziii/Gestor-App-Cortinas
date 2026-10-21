package com.example.cortinas

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.cortinas.data.repository.CortinasRepository
import com.example.cortinas.data.remote.AppUpdate
import com.example.cortinas.data.remote.AppUpdateService
import com.example.cortinas.ui.navigation.MainNavigation
import com.example.cortinas.ui.theme.CortinasTheme

class  MainActivity : ComponentActivity() {

    private lateinit var repository: CortinasRepository
    private lateinit var updateService: AppUpdateService
    private var availableUpdate by mutableStateOf<AppUpdate?>(null)
    private var updateMessage by mutableStateOf("")
    private var downloadingUpdate by mutableStateOf(false)
    private var pendingApk: java.io.File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        repository = CortinasRepository(applicationContext)
        updateService = AppUpdateService(applicationContext, repository.config)

        setContent {
            CortinasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    com.example.cortinas.ui.navigation.CortinasSessionGate(repository)
                    availableUpdate?.let { update ->
                        AlertDialog(
                            onDismissRequest = { if (!update.mandatory && !downloadingUpdate) availableUpdate = null },
                            title = { Text("Actualización disponible ${update.versionName}") },
                            text = { Text(buildString {
                                append(update.releaseNotes.ifBlank { "Hay una nueva versión de Cortinas." })
                                if (updateMessage.isNotBlank()) append("\n\n").append(updateMessage)
                            }) },
                            confirmButton = {
                                TextButton(enabled = !downloadingUpdate, onClick = { downloadUpdate(update) }) {
                                    Text(if (downloadingUpdate) "Descargando…" else "Actualizar")
                                }
                            },
                            dismissButton = if (!update.mandatory) ({
                                TextButton(enabled = !downloadingUpdate, onClick = { availableUpdate = null }) { Text("Después") }
                            }) else null
                        )
                    }
                }
            }
        }
        checkForUpdate()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= 26 && packageManager.canRequestPackageInstalls()) {
            pendingApk?.let { apk -> pendingApk = null; openInstaller(apk) }
        }
    }

    private fun checkForUpdate() {
        if (!BuildConfig.ENABLE_APP_UPDATES) return
        Thread {
            try {
                val update = updateService.checkForUpdate()
                runOnUiThread { availableUpdate = update; updateMessage = "" }
            } catch (_: Exception) {
                // Fallar la consulta de versiones no debe impedir usar la app.
            }
        }.start()
    }

    private fun downloadUpdate(update: AppUpdate) {
        if (downloadingUpdate) return
        downloadingUpdate = true
        updateMessage = "Descargando la actualización…"
        Thread {
            try {
                val apk = updateService.download(update)
                runOnUiThread {
                    downloadingUpdate = false
                    updateMessage = "Descarga lista. Android te pedirá confirmar la instalación."
                    if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
                        pendingApk = apk
                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:$packageName")))
                    } else openInstaller(apk)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    downloadingUpdate = false
                    updateMessage = e.message ?: "No se pudo descargar la actualización."
                }
            }
        }.start()
    }

    private fun openInstaller(apk: java.io.File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(updateService.installerUri(apk), "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }
}
