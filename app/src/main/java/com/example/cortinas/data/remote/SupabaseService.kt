package com.example.cortinas.data.remote

import com.example.cortinas.data.model.AbonoPago
import com.example.cortinas.data.model.Cliente
import com.example.cortinas.data.model.Cotizacion
import com.example.cortinas.data.model.CotizacionItem
import com.example.cortinas.data.model.EstadoCotizacion
import com.example.cortinas.data.model.Tela
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.time.Instant

class SupabaseService(private val config: SupabaseConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun iniciarSesion(email: String, password: String, rememberSession: Boolean = true): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val body = JsonObject().apply {
            addProperty("email", email.trim())
            addProperty("password", password)
        }
        val request = Request.Builder()
            .url("${config.url.trimEnd('/')}/auth/v1/token?grant_type=password")
            .addHeader("apikey", config.anonKey)
            .post(gson.toJson(body).toRequestBody(jsonMediaType))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val content = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorBody = runCatching { gson.fromJson(content, JsonObject::class.java) }.getOrNull()
                    val message = listOf("msg", "message", "error_description", "error")
                        .firstNotNullOfOrNull { key ->
                            runCatching { errorBody?.get(key)?.takeUnless { it.isJsonNull }?.asString }.getOrNull()
                        }
                        ?: "No se pudo iniciar sesión (código ${response.code}). Verifica el correo y la contraseña."
                    return@withContext false to message
                }
                val session = gson.fromJson(content, JsonObject::class.java)
                val access = session.get("access_token")?.asString.orEmpty()
                val refresh = session.get("refresh_token")?.asString.orEmpty()
                val expires = session.get("expires_in")?.asLong ?: 3600L
                if (access.isBlank() || refresh.isBlank()) return@withContext false to "No se pudo iniciar una sesión válida."
                config.saveSession(access, refresh, expires)
                config.recordLogin(rememberSession)
                true to "Sesión iniciada."
            }
        } catch (e: Exception) {
            false to "No se pudo conectar. Revisa tu conexión a internet."
        }
    }

    suspend fun renovarSesion(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val refreshToken = config.refreshToken
        if (refreshToken.isBlank()) return@withContext false to "Inicia sesión para continuar."
        val body = JsonObject().apply { addProperty("refresh_token", refreshToken) }
        val request = Request.Builder()
            .url("${config.url.trimEnd('/')}/auth/v1/token?grant_type=refresh_token")
            .addHeader("apikey", config.anonKey)
            .post(gson.toJson(body).toRequestBody(jsonMediaType))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val content = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    config.clearSession()
                    return@withContext false to "La sesión venció. Inicia sesión otra vez."
                }
                val session = gson.fromJson(content, JsonObject::class.java)
                val access = session.get("access_token")?.asString.orEmpty()
                val refresh = session.get("refresh_token")?.asString.orEmpty()
                val expires = session.get("expires_in")?.asLong ?: 3600L
                if (access.isBlank() || refresh.isBlank()) return@withContext false to "No se pudo renovar la sesión."
                config.saveSession(access, refresh, expires)
                true to "Sesión renovada."
            }
        } catch (e: Exception) {
            false to (e.localizedMessage ?: "No se pudo renovar la sesión.")
        }
    }

    fun cerrarSesion() = config.clearSession()

    private fun authToken(): String = config.accessToken.ifBlank { config.anonKey }

    private suspend fun refreshTokenIfNeeded(): Boolean {
        if (!config.isAuthenticated) return false
        if (config.tokenExpiresAt <= System.currentTimeMillis() + 120_000L) {
            return renovarSesion().first
        }
        return true
    }

    suspend fun downloadSnapshot(): Pair<CloudSnapshot?, String?> = withContext(Dispatchers.IO) {
        if (!refreshTokenIfNeeded()) return@withContext null to "Inicia sesión para consultar la información."
        val telasRows = fetchRows("telas")
        if (telasRows.first == null) return@withContext null to telasRows.second
        val clientesRows = fetchRows("clientes")
        if (clientesRows.first == null) return@withContext null to clientesRows.second
        val cotRows = fetchRows("cotizaciones")
        if (cotRows.first == null) return@withContext null to cotRows.second
        val itemRows = fetchRows("cotizacion_items")
        if (itemRows.first == null) return@withContext null to itemRows.second
        val abonoRows = fetchRows("abonos")
        if (abonoRows.first == null) return@withContext null to abonoRows.second

        val telas = telasRows.first!!.map { row ->
            Tela(
                id = row.text("id"), modelo = row.text("modelo"),
                tipo = row.text("tipo"),
                precioM2 = row.number("precio_m2"), detalles = row.text("detalles"),
                aptoImpresion = row.nullableFlag("apto_impresion"),
                aptoExterior = row.nullableFlag("apto_exterior"),
                anchoCatalogo = row.text("ancho_catalogo"),
                catalogoOrigen = row.text("catalogo_origen"),
                updatedAt = row.timestamp("updated_at"),
            )
        }
        val clientes = clientesRows.first!!.map { row ->
            Cliente(
                id = row.text("id"), nombre = row.text("nombre"), telefono = row.text("telefono"),
                direccion = row.text("direccion"), notas = row.text("notas"), fechaRegistro = row.integer("fecha_registro"),
                updatedAt = row.timestamp("updated_at")
            )
        }
        val items = itemRows.first!!.map { row ->
            CotizacionItem(
                id = row.text("id"), cotizacionId = row.text("cotizacion_id"), ambiente = row.text("ambiente", "Ventana"),
                ancho = row.number("ancho", 1.0), alto = row.number("alto", 1.0),
                esDosCuerpos = row.flag("es_dos_cuerpos"), anchoCuerpoIzq = row.number("ancho_cuerpo_izq"),
                anchoCuerpoDer = row.number("ancho_cuerpo_der"), telaId = row.nullableText("tela_id"),
                telaModelo = row.text("tela_modelo"), telaTipo = row.text("tela_tipo", "Translúcida"),
                precioM2 = row.number("precio_m2"), costoMotor = row.number("costo_motor"),
                costoExtras = row.number("costo_extras"), mandoLado = row.text("mando_lado", "Derecha"),
                caida = row.text("caida", "Normal"), notas = row.text("notas")
            )
        }
        val abonos = abonoRows.first!!.map { row ->
            AbonoPago(
                id = row.text("id"), cotizacionId = row.text("cotizacion_id"), monto = row.number("monto"),
                fecha = row.integer("fecha"), metodoPago = row.text("metodo_pago", "Efectivo"),
                referencia = row.text("referencia"), notas = row.text("notas")
            )
        }
        val itemsPorCotizacion = items.groupBy { it.cotizacionId }
        val abonosPorCotizacion = abonos.groupBy { it.cotizacionId }
        val cotizaciones = cotRows.first!!.map { row ->
            val id = row.text("id")
            Cotizacion(
                id = id, codigo = row.text("codigo"), clienteId = row.text("cliente_id"),
                clienteNombre = row.text("cliente_nombre"), clienteTelefono = row.text("cliente_telefono"),
                clienteDireccion = row.text("cliente_direccion"), fecha = row.integer("fecha"),
                items = itemsPorCotizacion[id].orEmpty(), costoInstalacionGeneral = row.number("costo_instalacion"),
                descuento = row.number("descuento"), estado = EstadoCotizacion.fromString(row.text("estado", "COTIZACION")),
                notas = row.text("notas"), abonos = abonosPorCotizacion[id].orEmpty(),
                updatedAt = row.timestamp("updated_at")
            )
        }
        CloudSnapshot(telas, clientes, cotizaciones) to null
    }

    private suspend fun fetchRows(table: String): Pair<List<JsonObject>?, String?> {
        val pageSize = 1000
        var lastId: String? = null
        val rows = mutableListOf<JsonObject>()
        while (true) {
            val afterId = lastId?.let {
                "&id=gt.${java.net.URLEncoder.encode(it, Charsets.UTF_8.name())}"
            }.orEmpty()
            val request = Request.Builder()
                .url("${config.url.trimEnd('/')}/rest/v1/$table?select=*&order=id.asc&limit=$pageSize$afterId")
                .addHeader("apikey", config.anonKey)
                .addHeader("Authorization", "Bearer ${authToken()}")
                .get().build()
            try {
                val (page, error) = client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        if (response.code == 401) config.clearSession()
                        if (response.code == 403) config.markAuthorizationDenied()
                        null to "No se pudo cargar la información. Revisa tu conexión e inténtalo de nuevo."
                    } else {
                        JsonParser.parseString(body).asJsonArray.map { it.asJsonObject } to null
                    }
                }
                if (error != null) return null to error
                val currentPage = page.orEmpty()
                rows.addAll(currentPage)
                if (currentPage.size < pageSize) return rows to null
                lastId = currentPage.last().text("id")
                if (lastId.isNullOrBlank()) return null to "La respuesta de $table no contiene IDs para paginar."
            } catch (e: Exception) {
                return null to "Error de conexión al consultar la información."
            }
        }
    }

    private fun JsonObject.text(key: String, default: String = ""): String =
        get(key)?.takeUnless(JsonElement::isJsonNull)?.asString ?: default

    private fun JsonObject.nullableText(key: String): String? =
        get(key)?.takeUnless(JsonElement::isJsonNull)?.asString

    private fun JsonObject.number(key: String, default: Double = 0.0): Double =
        get(key)?.takeUnless(JsonElement::isJsonNull)?.asDouble ?: default

    private fun JsonObject.integer(key: String): Long =
        get(key)?.takeUnless(JsonElement::isJsonNull)?.asLong ?: 0L

    private fun JsonObject.flag(key: String): Boolean =
        get(key)?.takeUnless(JsonElement::isJsonNull)?.asBoolean ?: false

    private fun JsonObject.nullableFlag(key: String): Boolean? =
        get(key)?.takeUnless(JsonElement::isJsonNull)?.asBoolean

    private fun JsonObject.timestamp(key: String): Long = runCatching {
        Instant.parse(text(key)).toEpochMilli()
    }.getOrDefault(0L)

    private fun timestampValue(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).toString()

    suspend fun deleteRecord(table: String, id: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!refreshTokenIfNeeded()) return@withContext false to "Inicia sesión para sincronizar."
        val safeId = java.net.URLEncoder.encode(id, Charsets.UTF_8.name())
        val request = Request.Builder()
            .url("${config.url.trimEnd('/')}/rest/v1/$table?id=eq.$safeId")
            .addHeader("apikey", config.anonKey)
            .addHeader("Authorization", "Bearer ${authToken()}")
            .delete()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) true to "Registro eliminado en la nube."
                else {
                    if (response.code == 401) config.clearSession()
                    if (response.code == 403) config.markAuthorizationDenied()
                    false to "No se pudo eliminar en $table (${response.code}): ${response.body?.string().orEmpty()}"
                }
            }
        } catch (e: Exception) {
            false to (e.localizedMessage ?: "Error de red al eliminar.")
        }
    }

    suspend fun syncTelas(telas: List<Tela>): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!refreshTokenIfNeeded()) return@withContext false to "Inicia sesión para guardar los cambios."
        if (telas.isEmpty()) return@withContext Pair(true, "No hay telas para sincronizar.")
        val cleanUrl = config.url.trimEnd('/')

        val payload = telas.map { tela ->
            JsonObject().apply {
                addProperty("id", tela.id)
                addProperty("modelo", tela.modelo)
                addProperty("tipo", tela.tipo)
                addProperty("precio_m2", tela.precioM2)
                addProperty("detalles", tela.detalles)
                if (tela.aptoImpresion == null) add("apto_impresion", com.google.gson.JsonNull.INSTANCE) else addProperty("apto_impresion", tela.aptoImpresion)
                if (tela.aptoExterior == null) add("apto_exterior", com.google.gson.JsonNull.INSTANCE) else addProperty("apto_exterior", tela.aptoExterior)
                addProperty("ancho_catalogo", tela.anchoCatalogo)
                addProperty("catalogo_origen", tela.catalogoOrigen)
                addProperty("updated_at", timestampValue(tela.updatedAt))
            }
        }

        val request = Request.Builder()
            .url("$cleanUrl/rest/v1/telas")
            .addHeader("apikey", config.anonKey)
            .addHeader("Authorization", "Bearer ${authToken()}")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Pair(true, "Telas sincronizadas")
                else {
                    if (response.code == 401) config.clearSession()
                    if (response.code == 403) config.markAuthorizationDenied()
                    Pair(false, "Error al sincronizar telas: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Error de sincronización")
        }
    }

    suspend fun syncClientes(clientes: List<Cliente>): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!refreshTokenIfNeeded()) return@withContext false to "Inicia sesión para guardar los cambios."
        if (clientes.isEmpty()) return@withContext Pair(true, "No hay clientes para sincronizar.")
        val cleanUrl = config.url.trimEnd('/')

        val payload = clientes.map { c ->
            JsonObject().apply {
                addProperty("id", c.id)
                addProperty("nombre", c.nombre)
                addProperty("telefono", c.telefono)
                addProperty("direccion", c.direccion)
                addProperty("notas", c.notas)
                addProperty("fecha_registro", c.fechaRegistro)
                addProperty("updated_at", timestampValue(c.updatedAt))
            }
        }

        val request = Request.Builder()
            .url("$cleanUrl/rest/v1/clientes")
            .addHeader("apikey", config.anonKey)
            .addHeader("Authorization", "Bearer ${authToken()}")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Pair(true, "Clientes sincronizados")
                else {
                    if (response.code == 401) config.clearSession()
                    if (response.code == 403) config.markAuthorizationDenied()
                    Pair(false, "Error al sincronizar clientes: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Error")
        }
    }

    suspend fun guardarCotizacion(cotizacion: Cotizacion): Pair<Cotizacion?, String> = withContext(Dispatchers.IO) {
        if (!refreshTokenIfNeeded()) {
            return@withContext null to "Inicia sesión para guardar la cotización."
        }

        val cotizacionJson = JsonObject().apply {
            addProperty("id", cotizacion.id)
            addProperty("codigo", cotizacion.codigo)
            addProperty("cliente_id", cotizacion.clienteId)
            addProperty("cliente_nombre", cotizacion.clienteNombre)
            addProperty("cliente_telefono", cotizacion.clienteTelefono)
            addProperty("cliente_direccion", cotizacion.clienteDireccion)
            addProperty("fecha", cotizacion.fecha)
            addProperty("costo_instalacion", cotizacion.costoInstalacionGeneral)
            addProperty("descuento", cotizacion.descuento)
            addProperty("estado", cotizacion.estado.name)
            addProperty("notas", cotizacion.notas)
            addProperty("updated_at", timestampValue(cotizacion.updatedAt))
        }
        val itemsJson = JsonArray().apply {
            cotizacion.items.forEach { item ->
                add(JsonObject().apply {
                    addProperty("id", item.id)
                    addProperty("cotizacion_id", item.cotizacionId.ifBlank { cotizacion.id })
                    addProperty("ambiente", item.ambiente)
                    addProperty("ancho", item.ancho)
                    addProperty("alto", item.alto)
                    addProperty("es_dos_cuerpos", item.esDosCuerpos)
                    addProperty("ancho_cuerpo_izq", item.anchoCuerpoIzq)
                    addProperty("ancho_cuerpo_der", item.anchoCuerpoDer)
                    addProperty("tela_id", item.telaId)
                    addProperty("tela_modelo", item.telaModelo)
                    addProperty("tela_tipo", item.telaTipo)
                    addProperty("precio_m2", item.precioM2)
                    addProperty("costo_motor", item.costoMotor)
                    addProperty("costo_extras", item.costoExtras)
                    addProperty("mando_lado", item.mandoLado)
                    addProperty("caida", item.caida)
                    addProperty("notas", item.notas)
                })
            }
        }
        val abonosJson = JsonArray().apply {
            cotizacion.abonos.forEach { abono ->
                add(JsonObject().apply {
                    addProperty("id", abono.id)
                    addProperty("cotizacion_id", abono.cotizacionId.ifBlank { cotizacion.id })
                    addProperty("monto", abono.monto)
                    addProperty("fecha", abono.fecha)
                    addProperty("metodo_pago", abono.metodoPago)
                    addProperty("referencia", abono.referencia)
                    addProperty("notas", abono.notas)
                })
            }
        }
        val requestBody = JsonObject().apply {
            add("p_cotizacion", cotizacionJson)
            add("p_items", itemsJson)
            add("p_abonos", abonosJson)
        }
        val request = Request.Builder()
            .url("${config.url.trimEnd('/')}/rest/v1/rpc/guardar_cotizacion_completa")
            .addHeader("apikey", config.anonKey)
            .addHeader("Authorization", "Bearer ${authToken()}")
            .post(gson.toJson(requestBody).toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    if (response.code == 401) config.clearSession()
                    if (response.code == 403) config.markAuthorizationDenied()
                    return@withContext null to "No se pudo guardar la cotización (${response.code}): $body"
                }
                val codigo = runCatching {
                    JsonParser.parseString(body).asJsonObject.get("codigo")?.asString
                }.getOrNull().orEmpty().ifBlank { cotizacion.codigo }
                cotizacion.copy(codigo = codigo) to "Cotización guardada."
            }
        } catch (e: Exception) {
            null to (e.localizedMessage ?: "Error al guardar la cotización.")
        }
    }

}
