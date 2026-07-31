package com.protap.iso24817calc.report

import android.content.Context
import android.content.Intent
import com.protap.iso24817calc.domain.CalculationResult

object ReportShareController {
    fun share(context: Context, result: CalculationResult) {
        val uri = PdfReportWriter.write(context, result)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share PROWRAP report"))
    }
}
