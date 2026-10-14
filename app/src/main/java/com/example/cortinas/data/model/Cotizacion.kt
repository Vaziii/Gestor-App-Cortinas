package com.example.cortinas.data.model

import java.util.UUID

data class Cotizacion(
    val id: String = UUID.randomUUID().toString(),
    val codigo: String = "",
    val clienteId: String = "",
    val clienteNombre: String = "",
    val clienteTelefono: String = "",
    val clienteDireccion: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val items: List<CotizacionItem> = emptyList(),
    val costoInstalacionGeneral: Double = 0.0,
    val descuento: Double = 0.0,
    val estado: EstadoCotizacion = EstadoCotizacion.COTIZACION,
    val notas: String = "",
    val abonos: List<AbonoPago> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val subtotalItems: Double
        get() = items.sumOf { it.totalItem }

    val totalVenta: Double
        get() = maxOf(0.0, subtotalItems + costoInstalacionGeneral - descuento)

    val totalAbonado: Double
        get() = abonos.sumOf { it.monto }

    val saldoPendiente: Double
        get() = maxOf(0.0, totalVenta - totalAbonado)

    val estaPagadaTotalmente: Boolean
        get() = totalVenta > 0 && totalAbonado >= totalVenta

    val porcentajePagado: Float
        get() = if (totalVenta > 0) ((totalAbonado / totalVenta).toFloat()).coerceIn(0f, 1f) else 0f

    val totalMetrosCuadrados: Double
        get() = items.sumOf { it.areaM2 }
}
