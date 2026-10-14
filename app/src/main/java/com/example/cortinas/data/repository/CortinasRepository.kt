package com.example.cortinas.data.repository

import android.content.Context
import com.example.cortinas.data.model.AbonoPago
import com.example.cortinas.data.model.Cliente
import com.example.cortinas.data.model.Cotizacion
import com.example.cortinas.data.model.EstadoCotizacion
import com.example.cortinas.data.model.Tela
import com.example.cortinas.data.remote.SupabaseConfig
import com.example.cortinas.data.remote.SupabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repositorio central de datos en la nube.
 * Solo opera con conexión a internet en tiempo real y no almacena copias locales en SQLite.
 */
class CortinasRepository(context: Context) {

    val config = SupabaseConfig(context)
    val supabaseService = SupabaseService(config)

    private val _telas = MutableStateFlow<List<Tela>>(emptyList())
    val telas: StateFlow<List<Tela>> = _telas.asStateFlow()

    private val _clientes = MutableStateFlow<List<Cliente>>(emptyList())
    val clientes: StateFlow<List<Cliente>> = _clientes.asStateFlow()

    private val _cotizaciones = MutableStateFlow<List<Cotizacion>>(emptyList())
    val cotizaciones: StateFlow<List<Cotizacion>> = _cotizaciones.asStateFlow()

    /**
     * Sincronización automática con Supabase en la nube.
     * Descarga el snapshot actual directamente a memoria.
     */
    suspend fun sincronizarConNube(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!config.isAuthenticated) {
            return@withContext false to "Inicia sesión para consultar la base de datos en la nube."
        }
        try {
            val (snapshot, errorMsg) = supabaseService.downloadSnapshot()
            if (snapshot != null) {
                _telas.value = snapshot.telas
                _clientes.value = snapshot.clientes
                _cotizaciones.value = snapshot.cotizaciones
                config.clearAuthorizationDenied()
                true to "Datos actualizados automáticamente desde la nube."
            } else {
                false to (errorMsg ?: "No se pudo conectar con Supabase. Se requiere conexión a internet.")
            }
        } catch (e: Exception) {
            false to (e.localizedMessage ?: "Error de red al consultar la nube.")
        }
    }

    suspend fun guardarTela(tela: Tela): Boolean = withContext(Dispatchers.IO) {
        val actualizada = tela.copy(updatedAt = maxOf(System.currentTimeMillis(), tela.updatedAt + 1))
        val (ok, _) = supabaseService.syncTelas(listOf(actualizada))
        if (ok) {
            _telas.value = _telas.value.filter { it.id != actualizada.id } + actualizada
        }
        ok
    }

    suspend fun eliminarTela(id: String): Boolean = withContext(Dispatchers.IO) {
        val (ok, _) = supabaseService.deleteRecord("telas", id)
        if (ok) {
            _telas.value = _telas.value.filter { it.id != id }
        }
        ok
    }

    suspend fun guardarCliente(cliente: Cliente): Boolean = withContext(Dispatchers.IO) {
        val actualizada = cliente.copy(updatedAt = maxOf(System.currentTimeMillis(), cliente.updatedAt + 1))
        val (ok, _) = supabaseService.syncClientes(listOf(actualizada))
        if (ok) {
            _clientes.value = _clientes.value.filter { it.id != actualizada.id } + actualizada
        }
        ok
    }

    suspend fun eliminarCliente(id: String): Boolean = withContext(Dispatchers.IO) {
        val (ok, _) = supabaseService.deleteRecord("clientes", id)
        if (ok) {
            _clientes.value = _clientes.value.filter { it.id != id }
        }
        ok
    }

    suspend fun guardarCotizacion(cotizacion: Cotizacion): Cotizacion? = withContext(Dispatchers.IO) {
        val actualizada = cotizacion.copy(updatedAt = maxOf(System.currentTimeMillis(), cotizacion.updatedAt + 1))
        val (guardada, _) = supabaseService.guardarCotizacion(actualizada)
        if (guardada != null) {
            _cotizaciones.value = _cotizaciones.value.filter { it.id != guardada.id } + guardada
        }
        guardada
    }

    suspend fun cambiarEstadoCotizacion(cotizacionId: String, nuevoEstado: EstadoCotizacion): Boolean = withContext(Dispatchers.IO) {
        val current = _cotizaciones.value.find { it.id == cotizacionId } ?: return@withContext false
        val updated = current.copy(estado = nuevoEstado, updatedAt = maxOf(System.currentTimeMillis(), current.updatedAt + 1))
        val (guardada, _) = supabaseService.guardarCotizacion(updated)
        if (guardada != null) {
            _cotizaciones.value = _cotizaciones.value.filter { it.id != cotizacionId } + guardada
        }
        guardada != null
    }

    suspend fun registrarAbono(abono: AbonoPago): Boolean = withContext(Dispatchers.IO) {
        val current = _cotizaciones.value.find { it.id == abono.cotizacionId } ?: return@withContext false
        if (abono.monto <= 0.0 || abono.monto > current.saldoPendiente) return@withContext false
        val updated = current.copy(
            abonos = current.abonos + abono,
            updatedAt = maxOf(System.currentTimeMillis(), current.updatedAt + 1)
        )
        val (guardada, _) = supabaseService.guardarCotizacion(updated)
        if (guardada != null) {
            _cotizaciones.value = _cotizaciones.value.filter { it.id != updated.id } + guardada
        }
        guardada != null
    }

    suspend fun eliminarCotizacion(id: String): Boolean = withContext(Dispatchers.IO) {
        val (ok, _) = supabaseService.deleteRecord("cotizaciones", id)
        if (ok) {
            _cotizaciones.value = _cotizaciones.value.filter { it.id != id }
        }
        ok
    }
}
