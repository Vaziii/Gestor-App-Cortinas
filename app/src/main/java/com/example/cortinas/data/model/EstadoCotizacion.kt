package com.example.cortinas.data.model

enum class EstadoCotizacion(val titulo: String) {
    COTIZACION("Cotización"),
    VENDIDA("Vendida"),
    EN_PROCESO("En Fabricación"),
    INSTALADA("Instalada / Entregada"),
    CANCELADA("Cancelada");

    companion object {
        fun fromString(value: String): EstadoCotizacion {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: COTIZACION
        }
    }
}
