package com.example.cortinas.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "EC")).apply {
        maximumFractionDigits = 0
    }

    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    fun formatMoney(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun formatM2(area: Double): String {
        return "${numberFormat.format(area)} m²"
    }

    fun formatNumber(value: Double): String {
        return numberFormat.format(value)
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
