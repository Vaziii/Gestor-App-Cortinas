package com.example.cortinas.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Cotizador     : Screen("cotizador",        "Cotizar",  Icons.Outlined.Calculate)
    object CotizarMecanicas : Screen("cotizar/mecanicas", "Mecánicas", Icons.Outlined.Calculate)
    object CotizarClasicas  : Screen("cotizar/clasicas",  "Clásicas",  Icons.Outlined.Collections)
    object Ventas        : Screen("ventas",           "Ventas",   Icons.Outlined.PointOfSale)
    object Muestrario    : Screen("muestrario",       "Telas",    Icons.Outlined.Collections)
    object Clientes      : Screen("clientes",         "Clientes", Icons.Outlined.Group)
    object Ajustes       : Screen("ajustes",          "Ajustes",  Icons.Outlined.Settings)
}

