package com.example.cortinas.util

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.cortinas.R
import com.example.cortinas.data.model.Cotizacion
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Generates the business's invoice-style sales PDF. This draft is not SRI-authorized. */
object InvoicePdfUtils {
    private const val NOMBRE_COMERCIAL = "DECOR NAYELI CORTINAS Y LENCERIA"
    private const val RUC_EMISOR = "1311695637001"
    private const val RAZON_SOCIAL = "QUIROZ ALCIVAR ROSSANA ELIZABETH"
    private const val DIRECCION = "FRANCISCO PACHECO S/N"
    private const val INTERSECCION = "10 DE AGOSTO"
    private const val UNIDADES_POR_DOLAR = 1000.0

    /** Converts legacy catalog amounts stored in thousands to dollars in the invoice. */
    private fun invoiceAmount(value: Double): Double =
        if (abs(value) >= UNIDADES_POR_DOLAR) value / UNIDADES_POR_DOLAR else value

    private data class InvoiceLine(
        val code: String,
        val description: String,
        val detail: String,
        val quantity: Double,
        val unit: String,
        val unitPrice: Double,
        val total: Double
    )

    fun share(context: Context, cotizacion: Cotizacion, nombreCliente: String, identificacion: String) {
        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val safeCode = cotizacion.codigo.ifBlank { cotizacion.id.take(8) }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "factura_$safeCode.pdf")
        createPdf(context, file, cotizacion, nombreCliente, identificacion)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Factura ${cotizacion.codigo}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir factura"))
    }

    private fun createPdf(context: Context, file: File, cot: Cotizacion, customerName: String, customerId: String) {
        val lines = buildList {
            val windowCounters = mutableMapOf<String, Int>()
            cot.items.forEachIndexed { index, item ->
                val windowNumber = (windowCounters[item.ambiente] ?: 0) + 1
                windowCounters[item.ambiente] = windowNumber
                val windowName = if (Regex("\\bventana\\s+\\d+\\b", RegexOption.IGNORE_CASE).containsMatchIn(item.ambiente)) item.ambiente else "${item.ambiente} - Ventana $windowNumber"
                val dimensions = "${FormatUtils.formatNumber(item.ancho)} x ${FormatUtils.formatNumber(item.alto)} m"
                val twoBodyDetail = if (item.esDosCuerpos) {
                    " - 2 cuerpos: ${FormatUtils.formatNumber(item.anchoCuerpoIzq)} + ${FormatUtils.formatNumber(item.anchoCuerpoDer)} m"
                } else ""
                if (item.telaTipo == "Cortina tradicional") {
                    add(
                        InvoiceLine(
                            code = (index + 1).toString().padStart(3, '0'),
                            description = "$windowName - ${item.telaModelo.ifBlank { item.telaTipo }}",
                            detail = "Medidas: ${FormatUtils.formatNumber(item.ancho)} x ${FormatUtils.formatNumber(item.alto)} m - ${item.notas.replace('\n', ' ')}",
                            quantity = 1.0,
                            unit = "cortina",
                            unitPrice = invoiceAmount(item.totalItem),
                            total = invoiceAmount(item.totalItem)
                        )
                    )
                } else {
                    add(
                        InvoiceLine(
                            code = (index + 1).toString().padStart(3, '0'),
                            description = "$windowName - ${item.telaModelo.ifBlank { item.telaTipo }}",
                            detail = "Medidas: $dimensions$twoBodyDetail",
                            quantity = item.areaM2,
                            unit = "m\u00b2",
                            unitPrice = invoiceAmount(item.precioM2),
                            total = item.areaM2 * invoiceAmount(item.precioM2)
                        )
                    )
                }
                if (item.telaTipo != "Cortina tradicional" && item.costoMotor > 0) {
                    val motor = invoiceAmount(item.costoMotor)
                    add(InvoiceLine("", "Motor - $windowName", "Adicional", 1.0, "unidad", motor, motor))
                }
                if (item.telaTipo != "Cortina tradicional" && item.costoExtras > 0) {
                    val extras = invoiceAmount(item.costoExtras)
                    add(InvoiceLine("", "Adicionales - $windowName", "Accesorios y trabajos complementarios", 1.0, "lote", extras, extras))
                }
            }
        }
        val subtotal = lines.sumOf { it.total }
        val installation = invoiceAmount(cot.costoInstalacionGeneral)
        val discount = invoiceAmount(cot.descuento)
        val invoiceTotal = maxOf(0.0, subtotal + installation - discount)
        val paidRatio = if (cot.totalVenta > 0) (cot.totalAbonado / cot.totalVenta).coerceIn(0.0, 1.0) else 0.0
        val paid = invoiceTotal * paidRatio
        val balance = maxOf(0.0, invoiceTotal - paid)
        val width = 595
        val rowHeight = 48
        val height = maxOf(842, 610 + lines.size * rowHeight)
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, 1).create())
        val canvas = page.canvas
        val ink = 0xff252525.toInt()
        val border = 0xff858585.toInt()
        val light = 0xfff1f1f1.toInt()
        val accent = 0xffb7862d.toInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 10f }

        fun text(value: String, x: Float, y: Float, size: Float = 10f, bold: Boolean = false, color: Int = ink) {
            paint.color = color
            paint.textSize = size
            paint.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            canvas.drawText(value.take(100), x, y, paint)
        }
        fun box(left: Float, top: Float, right: Float, bottom: Float, fill: Int? = null) {
            paint.color = fill ?: border
            paint.style = if (fill == null) Paint.Style.STROKE else Paint.Style.FILL
            paint.strokeWidth = 0.8f
            canvas.drawRect(left, top, right, bottom, paint)
            paint.style = Paint.Style.FILL
        }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            paint.color = border; paint.strokeWidth = 0.65f
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
        fun money(value: Double) = "$" + String.format(Locale.US, "%,.2f", value)

        // Encabezado abierto con acento de color, sin recuadros superiores.
        paint.color = accent
        canvas.drawRect(24f, 20f, 571f, 24f, paint)
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
        if (logo != null) {
            paint.isFilterBitmap = true
            canvas.drawBitmap(logo, null, RectF(34f, 36f, 142f, 101f), paint)
            logo.recycle()
        }
        text("DECOR NAYELI", 151f, 52f, 15f, true)
        text("CORTINAS Y LENCERIA", 151f, 70f, 8f, true, accent)
        text("Raz\u00f3n Social: $RAZON_SOCIAL", 34f, 121f, 8f)
        text("RUC: $RUC_EMISOR", 34f, 139f, 9f, true)
        text("Direcci\u00f3n: $DIRECCION - $INTERSECCION", 34f, 157f, 8f)
        text("Contribuyente negocio popular - R\u00e9gimen RIMPE", 34f, 177f, 8f, true)

        text("FACTURA", 398f, 57f, 25f, true)
        text("COMPROBANTE DE VENTA", 400f, 76f, 8f, true, accent)
        text("RUC: $RUC_EMISOR", 400f, 101f, 8f, true)
        text("Referencia: ${cot.codigo.ifBlank { "BORRADOR" }}", 400f, 119f, 8f)
        text("Fecha: ${SimpleDateFormat("dd/MM/yyyy", Locale("es", "EC")).format(Date(cot.fecha))}", 400f, 137f, 8f)
        text("Obligado a llevar contabilidad: NO", 400f, 155f, 7f)

        // Datos del comprador como campos abiertos, separados por jerarquia tipografica.
        text("FACTURAR A", 34f, 222f, 8f, true, accent)
        text(customerName.ifBlank { "Cliente Ocasional" }, 34f, 242f, 10f, true)
        text("IDENTIFICACI\u00d3N", 330f, 222f, 7f, true, accent)
        text(customerId.ifBlank { "No registrada" }, 330f, 242f, 9f)
        text("FECHA", 478f, 222f, 7f, true, accent)
        text(SimpleDateFormat("dd/MM/yyyy", Locale("es", "EC")).format(Date(cot.fecha)), 478f, 242f, 9f)
        text("Direcci\u00f3n: ${cot.clienteDireccion.ifBlank { "No registrada" }}", 34f, 266f, 8f)
        text("Tel\u00e9fono: ${cot.clienteTelefono.ifBlank { "No registrado" }}", 330f, 266f, 8f)
        paint.color = accent; paint.strokeWidth = 1.1f
        canvas.drawLine(24f, 283f, 571f, 283f, paint)

        // Detalle con cantidad, descripcion, precio por unidad, descuento y total.
        val tableTop = 300f
        val tableBottom = tableTop + 28f + lines.size * rowHeight
        box(24f, tableTop, 571f, tableBottom)
        box(24f, tableTop, 571f, tableTop + 28f, light)
        val col = floatArrayOf(24f, 64f, 310f, 362f, 430f, 571f)
        for (x in col.drop(1).dropLast(1)) line(x, tableTop, x, tableBottom)
        text("Cod.", 31f, tableTop + 18f, 7f, true)
        text("Descripci\u00f3n", 69f, tableTop + 18f, 8f, true)
        text("Cantidad", 317f, tableTop + 18f, 7f, true)
        text("P. unitario", 366f, tableTop + 18f, 7f, true)
        text("Precio total", 438f, tableTop + 18f, 7f, true)
        lines.forEachIndexed { index, invoiceLine ->
            val yTop = tableTop + 28f + index * rowHeight
            val baseline = yTop + 18f
            if (index > 0) line(24f, yTop, 571f, yTop)
            text(invoiceLine.code, 29f, baseline, 7f)
            text(invoiceLine.description.take(43), 69f, baseline, 7f)
            text(invoiceLine.detail.take(43), 69f, baseline + 13f, 7f, false, 0xff555555.toInt())
            text("${FormatUtils.formatNumber(invoiceLine.quantity)} ${invoiceLine.unit}", 315f, baseline, 7f)
            val unitPriceText = money(invoiceLine.unitPrice)
            text(unitPriceText, 366f, baseline, 7f)
            text(money(invoiceLine.total), 438f, baseline, 7f)
        }

        // Informacion adicional y totales con divisores ligeros, sin cajas.
        val lowerTop = tableBottom + 16f
        line(24f, lowerTop, 571f, lowerTop)
        paint.color = accent; paint.strokeWidth = 2.2f
        canvas.drawLine(24f, lowerTop, 86f, lowerTop, paint)
        text("INFORMACI\u00d3N ADICIONAL", 34f, lowerTop + 22f, 9f, true, accent)
        text("Forma de pago: No registrada", 34f, lowerTop + 44f, 8f)
        text("Tel\u00e9fono: ${cot.clienteTelefono.ifBlank { "No registrado" }}", 34f, lowerTop + 62f, 8f)
        text("Direcci\u00f3n: ${cot.clienteDireccion.ifBlank { "No registrada" }}".take(55), 34f, lowerTop + 80f, 8f)
        line(352f, lowerTop + 10f, 352f, lowerTop + 139f)

        var totalY = lowerTop + 18f
        fun totalRow(label: String, value: String, bold: Boolean = false) {
            text(label, 366f, totalY, 8f, bold)
            text(value, 500f, totalY, 8f, bold)
            totalY += 17f
        }
        totalRow("SUBTOTAL SIN IMPUESTOS", money(subtotal))
        totalRow("DESCUENTO GENERAL", money(discount))
        if (installation > 0) totalRow("INSTALACI\u00d3N", money(installation))
        totalRow("IMPUESTOS (SRI)", "Pendiente")
        totalRow("VALOR TOTAL", money(invoiceTotal), true)
        totalRow("ABONADO", money(paid))
        totalRow("SALDO PENDIENTE", money(balance), true)

        val footerY = height - 23f
        line(24f, footerY - 17f, 571f, footerY - 17f)
        text("Gracias por su compra", 24f, footerY, 8f, true, 0xff555555.toInt())
        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
    }
}




