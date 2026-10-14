package com.example.cortinas.data.model

import java.util.UUID

data class Tela(
    val id: String = UUID.randomUUID().toString(),
    val modelo: String,
    val tipo: String = "Translúcida", // Translúcida, Blackout, Screen, Zebra / Sheer, Otra
    val precioM2: Double = 0.0,
    val detalles: String = "",
    val aptoImpresion: Boolean? = null,
    val aptoExterior: Boolean? = null,
    val anchoCatalogo: String = "",
    val catalogoOrigen: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
