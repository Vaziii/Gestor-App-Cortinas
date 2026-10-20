package com.example.cortinas.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.cortinas.data.model.Cotizacion

object ShareUtils {

    /** Shares one customer-facing message for a mixed mechanical and traditional quote. */
    fun shareCustomerQuoteViaWhatsApp(context: Context, cotizacion: Cotizacion, nombreNegocio: String = "DECOR NAYELI CORTINAS Y LENCERIA") {
        val text = buildString {
            appendLine("*$nombreNegocio*")
            appendLine("Cotización ${cotizacion.codigo.ifBlank { "pendiente" }} · ${FormatUtils.formatDateShort(cotizacion.fecha)}")
            appendLine("Cliente: ${cotizacion.clienteNombre}")
            appendLine()
            cotizacion.items.forEachIndexed { index, item ->
                appendLine("*${index + 1}. ${item.ambiente}*")
                appendLine("${item.telaTipo}: ${item.telaModelo.ifBlank { "Por confirmar" }}")
                if (item.telaTipo == "Cortina tradicional") {
                    appendLine("Ancho: ${FormatUtils.formatNumber(item.ancho)} m")
                } else {
                    appendLine("Medida: ${FormatUtils.formatNumber(item.ancho)} m × ${FormatUtils.formatNumber(item.alto)} m")
                }
                if (item.esDosCuerpos) appendLine("Dos cuerpos: ${FormatUtils.formatNumber(item.anchoCuerpoIzq)} m + ${FormatUtils.formatNumber(item.anchoCuerpoDer)} m")
                item.notas.lineSequence().filter { it.isNotBlank() }.forEach { appendLine(it.removePrefix("• ")) }
                if (item.telaTipo != "Cortina tradicional") {
                    appendLine("Tela: ${FormatUtils.formatMoney(item.subtotalTela)}")
                    if (item.costoMotor > 0) appendLine("Motor: ${FormatUtils.formatMoney(item.costoMotor)}")
                    if (item.costoExtras > 0) appendLine("Adicionales: ${FormatUtils.formatMoney(item.costoExtras)}")
                }
                appendLine("Subtotal: ${FormatUtils.formatMoney(item.totalItem)}")
                appendLine()
            }
            if (cotizacion.descuento > 0) appendLine("Descuento: ${FormatUtils.formatMoney(cotizacion.descuento)}")
            appendLine("*TOTAL: ${FormatUtils.formatMoney(cotizacion.totalVenta)}*")
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(text)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Enviar cotización").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    /** Creates a supplier-facing request; retail prices and customer contact details are omitted. */
    fun generateWhatsAppText(cotizacion: Cotizacion, nombreNegocio: String = "DECOR NAYELI CORTINAS Y LENCERIA"): String {
        return buildString {
            if (cotizacion.codigo.isNotBlank()) {
                appendLine("Referencia: ${cotizacion.codigo} · ${FormatUtils.formatDateShort(cotizacion.fecha)}")
            } else {
                appendLine("Cotización pendiente de guardar · ${FormatUtils.formatDateShort(cotizacion.fecha)}")
            }
            appendLine()

            cotizacion.items.forEachIndexed { index, item ->
                appendLine()
                appendLine("*${index + 1}. ${item.ambiente}*")
                appendLine("• Producto: ${item.telaModelo.ifBlank { "Por confirmar" }}")
                appendLine("• Tipo: ${item.telaTipo.ifBlank { "Por confirmar" }}")
                if (item.telaTipo == "Cortina tradicional") {
                    val confeccion = item.notas.lineSequence().firstOrNull { it.trimStart().startsWith("Confe", ignoreCase = true) }
                        ?.substringAfter(":")?.trim()
                    if (!confeccion.isNullOrBlank()) appendLine("Confeccion: $confeccion")
                    appendLine("Ancho: ${FormatUtils.formatNumber(item.ancho)} m")
                    appendLine("Alto: ${FormatUtils.formatNumber(item.alto)} m")
                } else {
                    appendLine("• Medida total: ${FormatUtils.formatNumber(item.ancho)} m de ancho × ${FormatUtils.formatNumber(item.alto)} m de alto")
                }
                if (item.esDosCuerpos) {
                    appendLine("• En 2 cuerpos: izquierdo ${FormatUtils.formatNumber(item.anchoCuerpoIzq)} m; derecho ${FormatUtils.formatNumber(item.anchoCuerpoDer)} m")
                }
                if (item.telaTipo != "Cortina tradicional") {
                    if (item.caida.equals("Invertida", ignoreCase = true) || item.caida.equals("Rollo invertido", ignoreCase = true)) {
                        appendLine("Rollo invertido")
                    }
                    if (!item.esDosCuerpos) appendLine("• Mando: lado ${item.mandoLado}")
                    if (item.costoMotor > 0) appendLine("• Requiere motorización")
                }
                if (item.telaTipo != "Cortina tradicional") {
                    item.notas.lineSequence()
                        .filter { it.isNotBlank() && !it.trimStart().startsWith("Adicional:", ignoreCase = true) }
                        .forEach { appendLine(it) }
                }
            }

            appendLine()
        }
    }

    fun shareSupplierQuoteViaWhatsApp(
        context: Context,
        cotizacion: Cotizacion,
        tradicionales: Boolean,
        nombreNegocio: String = "DECOR NAYELI CORTINAS Y LENCERIA"
    ) {
        val items = cotizacion.items.filter { (it.telaTipo == "Cortina tradicional") == tradicionales }
        if (items.isEmpty()) return
        val text = "*$nombreNegocio*\n" + generateWhatsAppText(cotizacion.copy(items = items), nombreNegocio)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(text)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Enviar solicitud al proveedor").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    fun shareViaWhatsApp(context: Context, cotizacion: Cotizacion, nombreNegocio: String = "DECOR NAYELI CORTINAS Y LENCERIA") {
        val text = generateWhatsAppText(cotizacion, nombreNegocio)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            // No usar el teléfono del cliente: el destinatario debe ser elegido en WhatsApp.
            data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(text)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Enviar solicitud al distribuidor").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
}
