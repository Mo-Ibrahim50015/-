package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.ObligationEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.ReportAnalytics
import com.example.ui.computeAnalytics
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportOutcome(
    val file: File,
    val savedDisplayPath: String?,
    val mimeType: String,
    val fileName: String
)

object ExportManager {

    private fun exportsDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun timestampSlug(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun readableTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

    private fun safeFilePrefix(titleSlug: String): String {
        val cleaned = titleSlug.replace(Regex("[^A-Za-z0-9_\\-]"), "_").trim('_')
        return if (cleaned.isNotBlank()) cleaned.take(28) else "Masrofy"
    }

    private fun loadTypeface(context: Context): Typeface {
        return runCatching {
            ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic) ?: Typeface.DEFAULT
        }.getOrDefault(Typeface.DEFAULT)
    }

    /** Generates UTF-8 with BOM CSV file so Microsoft Excel & Sheets display Arabic text properly. */
    fun exportCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        isArabic: Boolean,
        reportTitle: String = "",
        obligations: List<ObligationEntity> = emptyList()
    ): File {
        val file = File(exportsDir(context), "${safeFilePrefix("Masrofy")}_${timestampSlug()}.csv")
        val sb = StringBuilder()
        sb.append('\uFEFF')
        if (reportTitle.isNotBlank()) {
            sb.appendLine(escapeCsv("$reportTitle — ${readableTimestamp()}"))
            sb.appendLine()
        }
        if (isArabic) {
            sb.appendLine("التاريخ,الوقت,النوع,الحالة,البيان / السبب,الجهة / الشخص,المبلغ,العملة,التصنيف,الحساب / الوسيلة,موعد السداد,ملاحظات")
        } else {
            sb.appendLine("Date,Time,Type,Status,Item / Reason,Party,Amount,Currency,Category,Account / Method,Due Date,Notes")
        }
        transactions.forEach { tx ->
            val typeLabel = when {
                tx.type == TransactionType.INCOME && isArabic -> "فلوس جاية (دخل)"
                tx.type == TransactionType.INCOME -> "Incoming"
                isArabic -> "مصروف"
                else -> "Expense"
            }
            val statusLabel = debtStatusLabel(tx, isArabic)
            val signedMajor = if (tx.type == TransactionType.EXPENSE) {
                "-${minorToPlainDecimal(tx.amountMinor)}"
            } else {
                minorToPlainDecimal(tx.amountMinor)
            }
            val dueStr = if (tx.paymentDueDate.isNotBlank()) "${tx.paymentDueDate} ${tx.paymentDueTime}".trim() else ""
            val row = listOf(
                tx.date,
                tx.time,
                typeLabel,
                statusLabel,
                tx.itemName.ifBlank { tx.category },
                tx.debtParty,
                signedMajor,
                tx.currency,
                tx.category,
                tx.source,
                dueStr,
                tx.note
            ).joinToString(",") { escapeCsv(it) }
            sb.appendLine(row)
        }
        obligations.forEach { ob ->
            val typeLabel = tr(isArabic, "التزام بموعد سداد", "Scheduled Obligation")
            val statusLabel = if (ob.isPaid) tr(isArabic, "تم السداد", "Paid") else tr(isArabic, "مطلوب سداده", "Pending Due")
            val row = listOf(
                ob.dueDate,
                ob.dueTime,
                typeLabel,
                statusLabel,
                ob.reason,
                ob.party,
                "-${minorToPlainDecimal(ob.amountMinor)}",
                ob.currency,
                tr(isArabic, "التزامات بمواعيد", "Obligations"),
                ob.paidSource.ifBlank { "-" },
                "${ob.dueDate} ${ob.dueTime}".trim(),
                ob.note
            ).joinToString(",") { escapeCsv(it) }
            sb.appendLine(row)
        }
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    /** Generates styled Excel SpreadsheetML (.xls) XML workbook with full Arabic RTL, column widths, and summary row. */
    fun exportExcelWorkbook(
        context: Context,
        transactions: List<TransactionEntity>,
        isArabic: Boolean,
        reportTitle: String = "",
        obligations: List<ObligationEntity> = emptyList()
    ): File {
        val file = File(exportsDir(context), "Masrofy_${timestampSlug()}.xls")
        val headers = if (isArabic) {
            listOf("التاريخ", "الوقت", "النوع", "الحالة", "البيان / السبب", "الجهة / الشخص", "المبلغ", "العملة", "التصنيف", "الحساب / الوسيلة", "موعد السداد", "ملاحظات")
        } else {
            listOf("Date", "Time", "Type", "Status", "Item / Reason", "Party", "Amount", "Currency", "Category", "Account / Method", "Due Date", "Notes")
        }
        val colWidths = listOf(78, 56, 86, 130, 140, 110, 88, 56, 110, 115, 105, 160)

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<?mso-application progid="Excel.Sheet"?>""")
        sb.appendLine(
            """<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
                xmlns:o="urn:schemas-microsoft-com:office:office"
                xmlns:x="urn:schemas-microsoft-com:office:excel"
                xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">"""
        )
        sb.appendLine(
            """<Styles>
                <Style ss:ID="Title"><Font ss:Bold="1" ss:Size="13" ss:Color="#FFFFFF"/><Interior ss:Color="#091426" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center" ss:Vertical="Center"/></Style>
                <Style ss:ID="Header"><Font ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#1E3A5F" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:WrapText="1"/></Style>
                <Style ss:ID="CellText"><Alignment ss:Vertical="Center" ss:WrapText="1"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/></Borders></Style>
                <Style ss:ID="Income"><Font ss:Color="#006C4A" ss:Bold="1"/><NumberFormat ss:Format="#,##0.00"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/></Borders></Style>
                <Style ss:ID="Expense"><Font ss:Color="#BA1A1A" ss:Bold="1"/><NumberFormat ss:Format="#,##0.00"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/></Borders></Style>
                <Style ss:ID="Total"><Font ss:Bold="1" ss:Color="#091426"/><Interior ss:Color="#EEF2F6" ss:Pattern="Solid"/><NumberFormat ss:Format="#,##0.00"/></Style>
               </Styles>"""
        )
        val sheetTitle = if (isArabic) "مصروفي" else "Masrofy"
        val rtlAttr = if (isArabic) """ ss:RightToLeft="1"""" else ""
        sb.appendLine("""<Worksheet ss:Name="$sheetTitle"$rtlAttr><Table>""")

        colWidths.forEach { w ->
            sb.appendLine("""<Column ss:AutoFitWidth="0" ss:Width="$w"/>""")
        }

        // Title Banner Row
        val bannerText = reportTitle.ifBlank { tr(isArabic, "مصروفي — كشف العمليات المالي", "Masrofy — Financial Statement") } +
            " (${readableTimestamp()})"
        sb.appendLine(
            """<Row ss:Height="28"><Cell ss:MergeAcross="${headers.size - 1}" ss:StyleID="Title"><Data ss:Type="String">${escapeXml(bannerText)}</Data></Cell></Row>"""
        )

        // Header Row
        sb.append("""<Row ss:Height="24">""")
        headers.forEach { h ->
            sb.append("""<Cell ss:StyleID="Header"><Data ss:Type="String">${escapeXml(h)}</Data></Cell>""")
        }
        sb.appendLine("</Row>")

        var totalInMinor = 0L
        var totalOutMinor = 0L

        transactions.forEach { tx ->
            val isInc = tx.type == TransactionType.INCOME
            if (isInc) totalInMinor += tx.amountMinor else totalOutMinor += tx.amountMinor
            val typeLabel = if (isArabic) {
                if (isInc) "فلوس جاية" else "مصروف"
            } else {
                if (isInc) "Incoming" else "Expense"
            }
            val statusLabel = debtStatusLabel(tx, isArabic)
            val signedMinor = if (isInc) tx.amountMinor else -tx.amountMinor
            val numericVal = minorToPlainDecimal(signedMinor)
            val styleId = if (isInc) "Income" else "Expense"
            val dueStr = if (tx.paymentDueDate.isNotBlank()) "${tx.paymentDueDate} ${tx.paymentDueTime}".trim() else ""
            sb.append("<Row>")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(tx.date)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(tx.time)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(typeLabel)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(statusLabel)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(tx.itemName.ifBlank { tx.category })}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(tx.debtParty)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="$styleId"><Data ss:Type="Number">$numericVal</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(tx.currency)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(tx.category)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(tx.source)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(dueStr)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(tx.note)}</Data></Cell>""")
            sb.appendLine("</Row>")
        }

        obligations.forEach { ob ->
            totalOutMinor += ob.amountMinor
            val typeLabel = tr(isArabic, "التزام سداد", "Obligation")
            val statusLabel = if (ob.isPaid) tr(isArabic, "تم السداد", "Paid") else tr(isArabic, "مطلوب سداده", "Pending Due")
            sb.append("<Row>")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(ob.dueDate)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(ob.dueTime)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(typeLabel)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(statusLabel)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(ob.reason)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(ob.party)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="Expense"><Data ss:Type="Number">-${minorToPlainDecimal(ob.amountMinor)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(ob.currency)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(tr(isArabic, "التزامات", "Obligations"))}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(ob.paidSource.ifBlank { "-" })}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml("${ob.dueDate} ${ob.dueTime}".trim())}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="CellText"><Data ss:Type="String">${escapeXml(ob.note)}</Data></Cell>""")
            sb.appendLine("</Row>")
        }

        // Summary Totals Row
        val netMinor = totalInMinor - totalOutMinor
        val summaryLabel = tr(
            isArabic,
            "الإجمالي (${transactions.size + obligations.size} بند) • الداخل: ${formatAmountMinor(totalInMinor)} • الخارج: ${formatAmountMinor(totalOutMinor)}",
            "Total (${transactions.size + obligations.size} rows) • In: ${formatAmountMinor(totalInMinor)} • Out: ${formatAmountMinor(totalOutMinor)}"
        )
        sb.appendLine(
            """<Row ss:Height="24">
                <Cell ss:MergeAcross="5" ss:StyleID="Total"><Data ss:Type="String">${escapeXml(summaryLabel)}</Data></Cell>
                <Cell ss:StyleID="Total"><Data ss:Type="Number">${minorToPlainDecimal(netMinor)}</Data></Cell>
                <Cell ss:MergeAcross="4" ss:StyleID="Total"><Data ss:Type="String">${escapeXml(tr(isArabic, "الصافي", "Net"))}</Data></Cell>
               </Row>"""
        )

        sb.appendLine("</Table></Worksheet></Workbook>")
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    /** Generates a formatted Arabic-shaped multi-page PDF report with KPI summary & clean structured rows. */
    fun exportPdfReport(
        context: Context,
        analytics: ReportAnalytics,
        filterSummary: String,
        isArabic: Boolean,
        reportTitle: String = "",
        obligations: List<ObligationEntity> = emptyList()
    ): File {
        val file = File(exportsDir(context), "Masrofy_Report_${timestampSlug()}.pdf")
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 32f
        val contentWidth = (pageWidth - margin * 2).toInt()
        val typeface = loadTypeface(context)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 17f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 10.5f
            this.typeface = typeface
        }
        val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#091426")
            textSize = 13f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191C1E")
            textSize = 10.5f
            this.typeface = typeface
        }
        val mutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9.2f
            this.typeface = typeface
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas

        // Header banner
        bgPaint.color = Color.parseColor("#091426")
        canvas.drawRoundRect(RectF(margin, margin, pageWidth - margin, margin + 84f), 14f, 14f, bgPaint)
        val mainHeading = reportTitle.ifBlank {
            tr(isArabic, "مصروفي — التقرير المالي المنسق", "Masrofy — Financial Report")
        }
        drawAlignedText(canvas, mainHeading, titlePaint, margin + 16f, margin + 14f, contentWidth - 32, isArabic)
        drawAlignedText(
            canvas,
            "${tr(isArabic, "النطاق:", "Scope:")} $filterSummary • ${readableTimestamp()}",
            subtitlePaint,
            margin + 16f,
            margin + 46f,
            contentWidth - 32,
            isArabic
        )

        var y = margin + 100f

        // KPI Summary boxes
        val summary = analytics.primarySummary
        val sym = currencySymbol(summary.currency, isArabic)
        val boxW = (contentWidth - 16f) / 3f
        val kpis = listOf(
            Triple(
                tr(isArabic, "إجمالي الفلوس الجاية", "Total Incoming"),
                "+ ${formatAmountMinor(summary.totalIncomeMinor)} $sym",
                Color.parseColor("#006C4A")
            ),
            Triple(
                tr(isArabic, "إجمالي المصروفات", "Total Outflow"),
                "- ${formatAmountMinor(summary.totalExpenseMinor)} $sym",
                Color.parseColor("#BA1A1A")
            ),
            Triple(
                tr(isArabic, "الصافي المتبقي", "Net Balance"),
                formatSignedAmount(summary.netMinor, summary.currency, isArabic),
                Color.parseColor("#091426")
            )
        )
        kpis.forEachIndexed { idx, (label, value, accent) ->
            val x = margin + idx * (boxW + 8f)
            bgPaint.color = Color.parseColor("#F2F4F6")
            canvas.drawRoundRect(RectF(x, y, x + boxW, y + 62f), 10f, 10f, bgPaint)
            drawAlignedText(canvas, label, mutedPaint, x + 10f, y + 10f, (boxW - 20).toInt(), isArabic)
            val valPaint = TextPaint(sectionPaint).apply { color = accent; textSize = 11.5f }
            drawAlignedText(canvas, value, valPaint, x + 10f, y + 32f, (boxW - 20).toInt(), isArabic)
        }
        y += 78f

        val totalItemCount = analytics.filteredTransactions.size + obligations.size
        drawAlignedText(
            canvas,
            "${tr(isArabic, "تفاصيل العمليات", "Operations Details")} ($totalItemCount)",
            sectionPaint,
            margin,
            y,
            contentWidth,
            isArabic
        )
        y += 24f

        // Table Header Bar
        bgPaint.color = Color.parseColor("#1E3A5F")
        canvas.drawRoundRect(RectF(margin, y, pageWidth - margin, y + 22f), 6f, 6f, bgPaint)
        val thPaint = TextPaint(mutedPaint).apply {
            color = Color.WHITE
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        drawAlignedText(
            canvas,
            tr(isArabic, "البيان والتفاصيل", "Item & Details"),
            thPaint,
            if (isArabic) margin + 132f else margin + 8f,
            y + 5f,
            contentWidth - 144,
            isArabic
        )
        drawAlignedText(
            canvas,
            tr(isArabic, "المبلغ", "Amount"),
            thPaint,
            if (isArabic) margin + 8f else pageWidth - margin - 130f,
            y + 5f,
            122,
            !isArabic
        )
        y += 28f

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - 54f) {
                // Footer page number
                drawAlignedText(
                    canvas,
                    "${tr(isArabic, "صفحة", "Page")} $pageNum • ${tr(isArabic, "مصروفي", "Masrofy")}",
                    mutedPaint,
                    margin,
                    pageHeight - 30f,
                    contentWidth,
                    isArabic
                )
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = margin
            }
        }

        analytics.filteredTransactions.forEachIndexed { idx, tx ->
            ensureSpace(44f)
            if (idx % 2 == 1) {
                bgPaint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(margin, y - 2f, pageWidth - margin, y + 38f, bgPaint)
            }
            val txSym = currencySymbol(tx.currency, isArabic)
            val isInc = tx.type == TransactionType.INCOME
            val amountStr = "${if (isInc) "+" else "-"} ${formatAmountMinor(tx.amountMinor)} $txSym"
            val displayName = tx.itemName.ifBlank { tx.note }.ifBlank { tx.category }
            val debtTag = if (tx.isDebt) " [${debtStatusLabel(tx, isArabic)}]" else ""
            val partyTag = if (tx.debtParty.isNotBlank()) " • ${tx.debtParty}" else ""
            val rowTitle = "$displayName$partyTag$debtTag"
            val dueTag = if (tx.paymentDueDate.isNotBlank()) {
                " • ${tr(isArabic, "سداد:", "Due:")} ${tx.paymentDueDate}"
            } else ""
            val rowMeta = "${tx.date} ${tx.time} • ${tx.category} • ${tx.source}$dueTag"

            val amtPaint = TextPaint(bodyPaint).apply {
                color = if (isInc) Color.parseColor("#006C4A") else Color.parseColor("#BA1A1A")
                this.typeface = Typeface.create(typeface, Typeface.BOLD)
            }

            if (isArabic) {
                drawAlignedText(canvas, rowTitle, bodyPaint, margin + 132f, y, contentWidth - 136, true)
                drawAlignedText(canvas, amountStr, amtPaint, margin + 4f, y, 124, false)
                drawAlignedText(canvas, rowMeta, mutedPaint, margin + 132f, y + 17f, contentWidth - 136, true)
            } else {
                drawAlignedText(canvas, rowTitle, bodyPaint, margin + 4f, y, contentWidth - 136, false)
                drawAlignedText(canvas, amountStr, amtPaint, pageWidth - margin - 128f, y, 124, true)
                drawAlignedText(canvas, rowMeta, mutedPaint, margin + 4f, y + 17f, contentWidth - 136, false)
            }

            y += 40f
            canvas.drawLine(margin, y - 3f, pageWidth - margin, y - 3f, linePaint)
        }

        obligations.forEachIndexed { idx, ob ->
            ensureSpace(44f)
            if ((analytics.filteredTransactions.size + idx) % 2 == 1) {
                bgPaint.color = Color.parseColor("#FFFBEB")
                canvas.drawRect(margin, y - 2f, pageWidth - margin, y + 38f, bgPaint)
            }
            val obSym = currencySymbol(ob.currency, isArabic)
            val amountStr = "- ${formatAmountMinor(ob.amountMinor)} $obSym"
            val statusTxt = if (ob.isPaid) tr(isArabic, "تم السداد", "Paid") else tr(isArabic, "مطلوب سداده", "Due")
            val partyTag = if (ob.party.isNotBlank()) " • ${ob.party}" else ""
            val rowTitle = "${ob.reason}$partyTag [$statusTxt]"
            val rowMeta = "${tr(isArabic, "موعد السداد:", "Due:")} ${ob.dueDate} ${ob.dueTime}" +
                if (ob.note.isNotBlank()) " • ${ob.note}" else ""

            val amtPaint = TextPaint(bodyPaint).apply {
                color = if (ob.isPaid) Color.parseColor("#006C4A") else Color.parseColor("#D97706")
                this.typeface = Typeface.create(typeface, Typeface.BOLD)
            }

            if (isArabic) {
                drawAlignedText(canvas, rowTitle, bodyPaint, margin + 132f, y, contentWidth - 136, true)
                drawAlignedText(canvas, amountStr, amtPaint, margin + 4f, y, 124, false)
                drawAlignedText(canvas, rowMeta, mutedPaint, margin + 132f, y + 17f, contentWidth - 136, true)
            } else {
                drawAlignedText(canvas, rowTitle, bodyPaint, margin + 4f, y, contentWidth - 136, false)
                drawAlignedText(canvas, amountStr, amtPaint, pageWidth - margin - 128f, y, 124, true)
                drawAlignedText(canvas, rowMeta, mutedPaint, margin + 4f, y + 17f, contentWidth - 136, false)
            }
            y += 40f
            canvas.drawLine(margin, y - 3f, pageWidth - margin, y - 3f, linePaint)
        }

        // Final page footer
        drawAlignedText(
            canvas,
            "${tr(isArabic, "صفحة", "Page")} $pageNum • ${tr(isArabic, "تم الإنشاء بواسطة مصروفي", "Generated by Masrofy")}",
            mutedPaint,
            margin,
            pageHeight - 30f,
            contentWidth,
            isArabic
        )
        pdf.finishPage(page)
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    /** Convenience PDF export directly from any list of transactions/obligations on any page. */
    fun exportTransactionsPdf(
        context: Context,
        transactions: List<TransactionEntity>,
        obligations: List<ObligationEntity> = emptyList(),
        preferredCurrency: String,
        reportTitle: String,
        filterSummary: String,
        isArabic: Boolean
    ): File {
        val analytics = computeAnalytics(transactions, preferredCurrency, obligations)
        return exportPdfReport(context, analytics, filterSummary, isArabic, reportTitle, obligations)
    }

    /** Generates a high-resolution PNG report card with KPIs, Donut breakdown & top operations. */
    fun exportReportImage(
        context: Context,
        analytics: ReportAnalytics,
        filterSummary: String,
        isArabic: Boolean,
        reportTitle: String = ""
    ): File {
        val width = 1080
        val topRows = analytics.filteredTransactions.take(10)
        val height = 880 + (topRows.size * 76)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val typeface = loadTypeface(context)

        canvas.drawColor(Color.parseColor("#F7F9FB"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val margin = 48f
        val contentW = (width - margin * 2).toInt()

        // Top Navy Banner
        paint.color = Color.parseColor("#091426")
        canvas.drawRoundRect(RectF(margin, margin, width - margin, 224f), 28f, 28f, paint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 23f
            this.typeface = typeface
        }
        drawAlignedText(
            canvas,
            reportTitle.ifBlank { tr(isArabic, "مصروفي • ملخص التقرير", "Masrofy • Report Summary") },
            titlePaint,
            margin + 32f,
            margin + 28f,
            contentW - 64,
            isArabic
        )
        drawAlignedText(
            canvas,
            "$filterSummary • ${readableTimestamp()}",
            subPaint,
            margin + 32f,
            margin + 100f,
            contentW - 64,
            isArabic
        )

        // 3 KPI Cards
        val s = analytics.primarySummary
        val sym = currencySymbol(s.currency, isArabic)
        val cardW = (contentW - 32f) / 3f
        val kpiY = 256f
        val kpiItems = listOf(
            Triple(tr(isArabic, "إجمالي الدخل", "Incoming"), "+ ${formatAmountMinor(s.totalIncomeMinor)} $sym", Color.parseColor("#006C4A")),
            Triple(tr(isArabic, "المصروفات", "Expenses"), "- ${formatAmountMinor(s.totalExpenseMinor)} $sym", Color.parseColor("#BA1A1A")),
            Triple(tr(isArabic, "الصافي", "Net Flow"), formatSignedAmount(s.netMinor, s.currency, isArabic), Color.parseColor("#091426"))
        )
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 22f
            this.typeface = typeface
        }
        kpiItems.forEachIndexed { i, (lbl, v, col) ->
            val x = margin + i * (cardW + 16f)
            paint.color = Color.WHITE
            canvas.drawRoundRect(RectF(x, kpiY, x + cardW, kpiY + 150f), 24f, 24f, paint)
            drawAlignedText(canvas, lbl, labelPaint, x + 20f, kpiY + 24f, (cardW - 40).toInt(), isArabic)
            val vp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = col
                textSize = 27f
                this.typeface = Typeface.create(typeface, Typeface.BOLD)
            }
            drawAlignedText(canvas, v, vp, x + 20f, kpiY + 78f, (cardW - 40).toInt(), isArabic)
        }

        // Donut Card
        val donutCardTop = 434f
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(margin, donutCardTop, width - margin, donutCardTop + 340f), 28f, 28f, paint)
        val headPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#091426")
            textSize = 29f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val breakdownList = if (analytics.expenseBreakdown.isNotEmpty()) {
            analytics.expenseBreakdown
        } else {
            analytics.incomeBreakdown
        }
        drawAlignedText(
            canvas,
            if (analytics.expenseBreakdown.isNotEmpty()) {
                tr(isArabic, "توزيع المصروفات حسب التصنيف", "Expense Breakdown by Category")
            } else {
                tr(isArabic, "توزيع الفلوس الجاية حسب المصدر", "Incoming Breakdown by Source")
            },
            headPaint,
            margin + 28f,
            donutCardTop + 24f,
            contentW - 56,
            isArabic
        )

        val donutColors = listOf(
            Color.parseColor("#091426"),
            Color.parseColor("#006C4A"),
            Color.parseColor("#545F73"),
            Color.parseColor("#FF4E67"),
            Color.parseColor("#D97706")
        )
        val ringRect = RectF(margin + 60f, donutCardTop + 95f, margin + 270f, donutCardTop + 305f)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 36f
            color = Color.parseColor("#ECEEF0")
        }
        canvas.drawOval(ringRect, strokePaint)
        var startAngle = -90f
        breakdownList.take(5).forEachIndexed { idx, item ->
            strokePaint.color = donutColors[idx % donutColors.size]
            val sweep = (item.percentage / 100f) * 360f
            canvas.drawArc(ringRect, startAngle, sweep, false, strokePaint)
            startAngle += sweep
        }

        var legY = donutCardTop + 100f
        val legPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191C1E")
            textSize = 24f
            this.typeface = typeface
        }
        breakdownList.take(5).forEachIndexed { idx, item ->
            paint.color = donutColors[idx % donutColors.size]
            canvas.drawCircle(margin + 340f, legY + 14f, 10f, paint)
            val txt = "${item.category} (${item.percentage.toInt()}%)"
            drawAlignedText(canvas, txt, legPaint, margin + 365f, legY - 4f, contentW - 400, isArabic)
            legY += 44f
        }

        var rowY = 810f
        topRows.forEach { tx ->
            paint.color = Color.WHITE
            canvas.drawRoundRect(RectF(margin, rowY, width - margin, rowY + 64f), 16f, 16f, paint)
            val txSym = currencySymbol(tx.currency, isArabic)
            val isInc = tx.type == TransactionType.INCOME
            val amt = "${if (isInc) "+" else "-"} ${formatAmountMinor(tx.amountMinor)} $txSym"
            val rowText = "${tx.itemName.ifBlank { tx.category }} • ${tx.source} (${tx.date})"
            val ap = TextPaint(legPaint).apply {
                color = if (isInc) Color.parseColor("#006C4A") else Color.parseColor("#BA1A1A")
                this.typeface = Typeface.create(typeface, Typeface.BOLD)
            }
            if (isArabic) {
                drawAlignedText(canvas, rowText, legPaint, margin + 255f, rowY + 16f, contentW - 280, true)
                drawAlignedText(canvas, amt, ap, margin + 20f, rowY + 16f, 225, false)
            } else {
                drawAlignedText(canvas, rowText, legPaint, margin + 22f, rowY + 16f, contentW - 280, false)
                drawAlignedText(canvas, amt, ap, width - margin - 245f, rowY + 16f, 225, true)
            }
            rowY += 74f
        }

        val file = File(exportsDir(context), "Masrofy_Report_${timestampSlug()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bitmap.recycle()
        return file
    }

    fun exportTransactionsImage(
        context: Context,
        transactions: List<TransactionEntity>,
        obligations: List<ObligationEntity> = emptyList(),
        preferredCurrency: String,
        reportTitle: String,
        filterSummary: String,
        isArabic: Boolean
    ): File {
        val analytics = computeAnalytics(transactions, preferredCurrency, obligations)
        return exportReportImage(context, analytics, filterSummary, isArabic, reportTitle)
    }

    fun exportJsonBackupFile(context: Context, jsonContent: String): File {
        val file = File(exportsDir(context), "Masrofy_Backup_${timestampSlug()}.json")
        file.writeText(jsonContent, Charsets.UTF_8)
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String): Boolean {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }

    fun openExportedFile(context: Context, file: File, mimeType: String): Boolean {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
            true
        }.getOrElse {
            shareFile(context, file, mimeType, file.name)
        }
    }

    /**
     * Saves an exported file directly into the phone's Downloads/Masrofy (or Pictures/Masrofy for images)
     * with ZERO storage permissions (Android 8+ safe).
     */
    fun saveToDeviceDownloads(
        context: Context,
        sourceFile: File,
        mimeType: String,
        isImage: Boolean,
        isArabic: Boolean
    ): String? {
        val fileName = sourceFile.name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mimeCandidates = if (isImage) {
                listOf(mimeType)
            } else {
                listOf(mimeType, "application/octet-stream").distinct()
            }
            for (candidateMime in mimeCandidates) {
                val saved = runCatching {
                    val collection = if (isImage) {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    }
                    val relativePath = if (isImage) {
                        "${Environment.DIRECTORY_PICTURES}/Masrofy"
                    } else {
                        "${Environment.DIRECTORY_DOWNLOADS}/Masrofy"
                    }
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, candidateMime)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val itemUri = resolver.insert(collection, values) ?: return@runCatching null
                    try {
                        resolver.openOutputStream(itemUri)?.use { out ->
                            sourceFile.inputStream().use { input -> input.copyTo(out) }
                        } ?: error("Cannot open MediaStore output stream")
                        val finishValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                        resolver.update(itemUri, finishValues, null, null)
                    } catch (t: Throwable) {
                        runCatching { resolver.delete(itemUri, null, null) }
                        throw t
                    }
                    if (isImage) {
                        tr(isArabic, "الصور / Masrofy / $fileName", "Pictures/Masrofy/$fileName")
                    } else {
                        tr(isArabic, "التنزيلات / Masrofy / $fileName", "Downloads/Masrofy/$fileName")
                    }
                }.getOrNull()
                if (saved != null) return saved
            }
        }

        return runCatching {
            val baseDir = context.getExternalFilesDir(
                if (isImage) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DOWNLOADS
            ) ?: context.filesDir
            val targetDir = File(baseDir, "Masrofy").apply { if (!exists()) mkdirs() }
            val dest = File(targetDir, fileName)
            sourceFile.copyTo(dest, overwrite = true)
            tr(isArabic, "Masrofy / ${dest.name}", "Masrofy/${dest.name}")
        }.getOrNull()
    }

    /** Writes any generated export file directly to a SAF Uri (local folder or Google Drive). */
    fun writeFileToUri(context: Context, sourceFile: File, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            } ?: context.contentResolver.openOutputStream(targetUri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            } ?: return false
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun drawAlignedText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        width: Int,
        isArabic: Boolean
    ) {
        if (width <= 0) return
        val align = if (isArabic) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(align)
            .setMaxLines(2)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun escapeXml(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun debtStatusLabel(tx: TransactionEntity, isArabic: Boolean): String = when {
        tx.isDebt && tx.isDebtSettled && tx.type == TransactionType.INCOME ->
            tr(isArabic, "فلوس جاية وهتندفع (تم السداد)", "Borrowed Income (Settled)")
        tx.isDebt && tx.isDebtSettled ->
            tr(isArabic, "مبلغ مطلوب دفعه (تم السداد)", "Upcoming Expense (Paid)")
        tx.isDebt && tx.type == TransactionType.INCOME ->
            tr(isArabic, "فلوس جاية وهتندفع (مستحق السداد)", "Borrowed Income (To Repay)")
        tx.isDebt ->
            tr(isArabic, "مبلغ مطلوب دفعه قريباً", "Upcoming Expense Due")
        tx.type == TransactionType.INCOME ->
            tr(isArabic, "فلوس جاية كدخل وربح", "Earned Income")
        else ->
            tr(isArabic, "مصروف اندفع بالفعل", "Actually Paid Expense")
    }
}
