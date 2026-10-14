package com.example.cortinas.data.model

import java.util.UUID

data class CotizacionItem(
    val id: String = UUID.randomUUID().toString(),
    val cotizacionId: String = "",
    val ambiente: String = "Ventana", // Sala, Habitación, etc.
    val ancho: Double = 1.0, // Ancho total en metros
    val alto: Double = 1.0, // Alto en metros
    val esDosCuerpos: Boolean = false,
    val anchoCuerpoIzq: Double = 0.0, // Ancho cuerpo izquierdo en metros
    val anchoCuerpoDer: Double = 0.0, // Ancho cuerpo derecho en metros
    val telaId: String? = null,
    val telaModelo: String = "",
    val telaTipo: String = "Translúcida",
    val precioM2: Double = 0.0,
    val costoMotor: Double = 0.0,
    val costoExtras: Double = 0.0,
    val mandoLado: String = "Derecha",
    val caida: String = "Normal",
    val notas: String = ""
) {
    val areaM2: Double
        get() = (ancho * alto)

    val subtotalTela: Double
        get() = areaM2 * precioM2

    val totalItem: Double
        get() = subtotalTela + costoMotor + costoExtras
}
