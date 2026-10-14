package com.example.cortinas.data.model

import java.util.UUID

data class Cliente(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val telefono: String = "",
    val direccion: String = "",
    val notas: String = "",
    val fechaRegistro: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
