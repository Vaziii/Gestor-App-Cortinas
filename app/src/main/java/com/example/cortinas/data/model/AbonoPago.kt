package com.example.cortinas.data.model

import java.util.UUID

data class AbonoPago(
    val id: String = UUID.randomUUID().toString(),
    val cotizacionId: String,
    val monto: Double,
    val fecha: Long = System.currentTimeMillis(),
    val metodoPago: String = "Efectivo", // Efectivo, Transferencia, Tarjeta Débito/Crédito u Otro
    val referencia: String = "",
    val notas: String = ""
)
