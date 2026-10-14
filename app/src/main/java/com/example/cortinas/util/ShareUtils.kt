package com.example.cortinas.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.cortinas.data.model.Cotizacion

object ShareUtils {

    /** Creates a supplier-facing request; retail prices and customer contact details are omitted. */
    fun generateWhatsAppText(cotizacion: Cotizacion, nombreNegocio: String = "Cortinas & Persianas"): String {
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
                appendLine("• Medida total: ${FormatUtils.formatNumber(item.ancho)} m de ancho × ${FormatUtils.formatNumber(item.alto)} m de alto")
                if (item.esDosCuerpos) {
                    appendLine("• En 2 cuerpos: izquierdo ${FormatUtils.formatNumber(item.anchoCuerpoIzq)} m; derecho ${FormatUtils.formatNumber(item.anchoCuerpoDer)} m")
                }
                appendLine("• Caída: ${item.caida}")
                appendLine("• Mando: lado ${item.mandoLado}")
                if (item.costoMotor > 0) appendLine("• Requiere motorización")
                if (item.costoExtras > 0) appendLine("• Total adicionales: ${FormatUtils.formatMoney(item.costoExtras)}")
                item.notas.lineSequence().filter { it.isNotBlank() }.forEach { appendLine("• $it") }
            }

            appendLine()
        }
    }

    fun shareViaWhatsApp(context: Context, cotizacion: Cotizacion, nombreNegocio: String = "Cortinas & Persianas") {
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
