package com.protap.iso24817calc.report

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.protap.iso24817calc.domain.CalculationResult
import java.io.File

object PdfReportWriter {
    fun write(context: Context, result: CalculationResult): android.net.Uri {
        val file = File(context.cacheDir, "prowrap-${result.reportNo.ifBlank { "repair" }}.pdf")
        val document = PdfDocument()
        val paint = Paint().apply { textSize = 10f; color = android.graphics.Color.BLACK }
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas = page.canvas
        var y = 36f
        RepairReportDocument.lines(result).forEach { line ->
            if (y > 805f) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                canvas = page.canvas
                y = 36f
            }
            canvas.drawText(line.take(105), 36f, y, paint)
            y += 16f
        }
        document.finishPage(page)
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
