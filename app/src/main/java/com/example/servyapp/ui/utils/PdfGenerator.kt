package com.example.servyapp.ui.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.graphics.withTranslation
import com.example.servyapp.domain.model.Order
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object PdfGenerator {

    // Constantes para el layout del PDF
    private const val PAGE_WIDTH = 595 // Ancho A4 en puntos
    private const val PAGE_HEIGHT = 842 // Alto A4 en puntos
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 18f
    private const val LINE_SPACING = 5f

    // Función principal
    @RequiresApi(Build.VERSION_CODES.Q)
    fun createOrderPdf(context: Context, order: Order): Boolean {
        // Formateador de fecha
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // 1. Crear el documento PDF
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // 2. Definir los Pinceles (Estilos de texto)
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f
            color = 0xFF000000.toInt() // Negro
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = 0xFF000000.toInt()
        }
        val bodyPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 12f
            color = 0xFF000000.toInt()
        }
        val bodyBoldPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            color = 0xFF000000.toInt()
        }

        canvas.withTranslation(MARGIN, MARGIN) {
            var yPos = 0f

            drawText("Recibo de Orden: #${order.orderNumber}", 0f, yPos, titlePaint)
            yPos += LINE_HEIGHT * 2

            // --- Info General ---
            drawText("Fecha: ${dateFormat.format(order.createdAt.toDate())}", 0f, yPos, bodyPaint)
            yPos += LINE_HEIGHT
            drawText("Estado: ${order.status}", 0f, yPos, bodyPaint)
            yPos += LINE_HEIGHT
            drawText("Pagado con: ${order.paymentMethod.ifEmpty { "No especificado" }}", 0f, yPos, bodyPaint)
            yPos += LINE_HEIGHT * 2

            order.pedidos.forEach { pedido ->
                drawText("Restaurante: ${pedido.restaurantName}", 0f, yPos, headerPaint)
                yPos += LINE_HEIGHT + LINE_SPACING

                // Encabezados de tabla
                drawText("Cant.", 0f, yPos, bodyBoldPaint)
                drawText("Platillo", 40f, yPos, bodyBoldPaint)
                drawText("Total", PAGE_WIDTH - MARGIN * 2 - 50f, yPos, bodyBoldPaint)
                yPos += LINE_HEIGHT

                // Línea divisora
                drawLine(0f, yPos, PAGE_WIDTH - MARGIN * 2, yPos, bodyPaint)
                yPos += LINE_HEIGHT

                // Items del pedido
                pedido.items.forEach { item ->
                    drawText("${item.quantity}x", 0f, yPos, bodyPaint)
                    drawText(item.dishName, 40f, yPos, bodyPaint)
                    val itemTotal = "S/ ${String.format("%.2f", item.totalPrice)}"
                    drawText(itemTotal, PAGE_WIDTH - MARGIN * 2 - 50f, yPos, bodyPaint)
                    yPos += LINE_HEIGHT
                }

                val subtotal = "Subtotal: S/ ${String.format("%.2f", pedido.subtotal)}"
                drawText(subtotal, PAGE_WIDTH - MARGIN * 2 - 100f, yPos, bodyBoldPaint)
                yPos += LINE_HEIGHT * 2
            }

            drawLine(0f, yPos, PAGE_WIDTH - MARGIN * 2, yPos, headerPaint)
            yPos += LINE_HEIGHT
            val totalText = "TOTAL: S/ ${String.format("%.2f", order.totalAmount)}"
            drawText(totalText, PAGE_WIDTH - MARGIN * 2 - 150f, yPos, headerPaint)
        }

        document.finishPage(page)

        // 5. Guardar el archivo en "Descargas"
        val fileName = "ServyApp_Orden_${order.orderNumber}.pdf"
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri).use { outputStream ->
                    document.writeTo(outputStream)
                }
            } else {
                throw Exception("MediaStore URI fue nulo")
            }

            document.close()
            return true // Éxito
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            return false // Fracaso
        }
    }
}