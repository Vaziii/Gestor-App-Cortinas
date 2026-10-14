package com.example.cortinas.data.remote

import com.example.cortinas.data.model.Cliente
import com.example.cortinas.data.model.Cotizacion
import com.example.cortinas.data.model.Tela

data class CloudSnapshot(
    val telas: List<Tela>,
    val clientes: List<Cliente>,
    val cotizaciones: List<Cotizacion>
)
