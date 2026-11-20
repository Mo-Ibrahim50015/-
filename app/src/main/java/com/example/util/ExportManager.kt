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
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.ReportAnalytics
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    private fun exportsDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun timestampSlug(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun loadTypeface(context: Context): Typeface {
        return runCatching {
            ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic) ?: Typeface.DEFAULT
        }.getOrDefault(Typeface.DEFAULT)
    }

    /** Generates UTF-8 with BOM CSV file so Microsoft Excel displays Arabic text properly. */
    fun exportCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        isArabic: Boolean
    ): File {
        val file = File(exportsDir(context), "Masrofy_${timestampSlug()}.csv")
        val sb = StringBuilder()
        // UTF-8 BOM for Excel Arabic compatibility
        sb.append('\uFEFF')
        if (isArabic) {
            sb.appendLine("التاريخ,الوقت,النوع,الحالة,اسم الحاجة,الدائن,المبلغ,العملة,التصنيف,المصدر,ملاحظات")
        } else {
            sb.appendLine("Date,Time,Type,Status,Item Name,Creditor,Amount,Currency,Category,Source,Notes")
        }
        transactions.forEach { tx ->
            val typeLabel = when {
                tx.type == TransactionType.INCOME && isArabic -> "دخل"
                tx.type == TransactionType.INCOME -> "Income"
                isArabic -> "مصروف"
                else -> "Expense"
            }
            val statusLabel = debtStatusLabel(tx, isArabic)
            val signedMajor = if (tx.type == TransactionType.EXPENSE) {
                "-${formatAmountMinor(tx.amountMinor)}"
            } else {
                formatAmountMinor(tx.amountMinor)
            }
            val row = listOf(
                tx.date,
                tx.time,
                typeLabel,
                statusLabel,
                tx.itemName,
                tx.debtParty,
                signedMajor.replace(",", ""),
                tx.currency,
                tx.category,
                tx.source,
                tx.note
            ).joinToString(",") { escapeCsv(it) }
            sb.appendLine(row)
        }
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    /** Generates Excel SpreadsheetML (.xls) XML workbook with full Arabic RTL & numeric formatting. */
    fun exportExcelWorkbook(
        context: Context,
        transactions: List<TransactionEntity>,
        isArabic: Boolean
    ): File {
        val file = File(exportsDir(context), "Masrofy_${timestampSlug()}.xls")
        val headers = if (isArabic) {
            listOf("التاريخ", "الوقت", "النوع", "الحالة", "اسم الحاجة", "الدائن", "المبلغ", "العملة", "التصنيف", "المصدر", "ملاحظات")
        } else {
            listOf("Date", "Time", "Type", "Status", "Item Name", "Creditor", "Amount", "Currency", "Category", "Source", "Notes")
        }

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<?mso-application progid="Excel.Sheet"?>""")
        sb.appendLine(
            """<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
                xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">"""
        )
        sb.appendLine(
            """<Styles>
                <Style ss:ID="Header"><Font ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#091426" ss:Pattern="Solid"/></Style>
                <Style ss:ID="Income"><Font ss:Color="#006C4A" ss:Bold="1"/></Style>
                <Style ss:ID="Expense"><Font ss:Color="#BA1A1A" ss:Bold="1"/></Style>
               </Styles>"""
        )
        sb.appendLine("""<Worksheet ss:Name="Masrofy"><Table>""")

        // Header Row
        sb.append("<Row>")
        headers.forEach { h ->
            sb.append("""<Cell ss:StyleID="Header"><Data ss:Type="String">${escapeXml(h)}</Data></Cell>""")
        }
        sb.appendLine("</Row>")

        // Data Rows
        transactions.forEach { tx ->
            val isInc = tx.type == TransactionType.INCOME
            val typeLabel = if (isArabic) {
                if (isInc) "دخل" else "مصروف"
            } else {
                if (isInc) "Income" else "Expense"
            }
            val statusLabel = debtStatusLabel(tx, isArabic)
            val numericVal = (if (isInc) tx.amountMinor else -tx.amountMinor) / 100.0
            val styleId = if (isInc) "Income" else "Expense"
            sb.append("<Row>")
            sb.append("""<Cell><Data ss:Type="String">${escapeXml(tx.date)}</Data></Cell>""")
            sb.append("""<Cell><Data ss:Type="String">${escapeXml(tx.time)}</Data></Cell>""")
            sb.append("""<Cell><Data ss:Type="String">${escapeXml(typeLabel)}</Data></Cell>""")
            sb.append("""<Cell><Data ss:Type="String">${escapeXml(statusLabel)}</Data></Cell>""")
            sb.append("""<Cell><Data ss:Type="String">${escapeXml(tx.itemName)}</Data></Cell>""")
            sb.append("""<Cell><Data ss:Type="String">${escapeXml(tx.debtParty)}</Data></Cell>""")
            sb.append("""<Cell ss:StyleID="$styleId"><Data ss:Type="Number">$numericVal</Data></Cell>""")
            sb.append("""<Cell><Data ss:Type="String">${escapeXml(tx.currency)}</Data></Cell>""")
            sb.append("""<Cell><Data ss:Type="String">${escapeXml(tx.category)}</Data></Cell>""")
            sb.append("""<Cell><Data ss:Type="String">${escapeXml(tx.source)}</Data></Cell>""")
            sb.append("""<Cell><Data ss:Type="String">${escapeXml(tx.note)}</Data></Cell>""")
            sb.appendLine("</Row>")
        }

        sb.appendLine("</Table></Worksheet></Workbook>")
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    /** Generates a clean Arabic-shaped multi-page PDF report using Android PdfDocument. */
    fun exportPdfReport(
        context: Context,
        analytics: ReportAnalytics,
        filterSummary: String,
        isArabic: Boolean
    ): File {
        val file = File(exportsDir(context), "Masrofy_Report_${timestampSlug()}.pdf")
        val pdf = PdfDocument()
        val pageWidth = 595 // A4 portrait @ 72dpi
        val pageHeight = 842
        val margin = 32f
        val contentWidth = (pageWidth - margin * 2).toInt()
        val typeface = loadTypeface(context)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 11f
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
            textSize = 9.5f
            this.typeface = typeface
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas

        // Header banner
        bgPaint.color = Color.parseColor("#091426")
        canvas.drawRoundRect(RectF(margin, margin, pageWidth - margin, margin + 78f), 12f, 12f, bgPaint)
        drawAlignedText(
            canvas,
            tr(isArabic, "مصروفي — التقرير المالي", "Masrofy — Financial Report"),
            titlePaint,
            margin + 16f,
            margin + 14f,
            contentWidth - 32,
            isArabic
        )
        drawAlignedText(
            canvas,
            "${tr(isArabic, "الفلاتر المطبقة:", "Active Filters:")} $filterSummary",
            subtitlePaint,
            margin + 16f,
            margin + 44f,
            contentWidth - 32,
            isArabic
        )

        var y = margin + 96f

        // KPI Summary boxes
        val summary = analytics.primarySummary
        val sym = currencySymbol(summary.currency, isArabic)
        val boxW = (contentWidth - 16f) / 3f
        val kpis = listOf(
            Triple(
                tr(isArabic, "إجمالي الدخل", "Total Income"),
                "+ ${formatAmountMinor(summary.totalIncomeMinor)} $sym",
                Color.parseColor("#006C4A")
            ),
            Triple(
                tr(isArabic, "المصروفات", "Expenses"),
                "- ${formatAmountMinor(summary.totalExpenseMinor)} $sym",
                Color.parseColor("#BA1A1A")
            ),
            Triple(
                tr(isArabic, "صافي الفائض", "Net Balance"),
                formatSignedAmount(summary.netMinor, summary.currency, isArabic),
                Color.parseColor("#091426")
            )
        )
        kpis.forEachIndexed { idx, (label, value, accent) ->
            val x = margin + idx * (boxW + 8f)
            bgPaint.color = Color.parseColor("#F2F4F6")
            canvas.drawRoundRect(RectF(x, y, x + boxW, y + 62f), 10f, 10f, bgPaint)
            drawAlignedText(canvas, label, mutedPaint, x + 10f, y + 10f, (boxW - 20).toInt(), isArabic)
            val valPaint = TextPaint(sectionPaint).apply { color = accent; textSize = 12f }
            drawAlignedText(canvas, value, valPaint, x + 10f, y + 32f, (boxW - 20).toInt(), isArabic)
        }
        y += 80f

        // Transactions Section Header
        drawAlignedText(
            canvas,
            "${tr(isArabic, "العمليات المطابقة للفلتر", "Matching Transactions")} (${analytics.filteredTransactions.size})",
            sectionPaint,
            margin,
            y,
            contentWidth,
            isArabic
        )
        y += 26f

        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }

        analytics.filteredTransactions.forEach { tx ->
            if (y > pageHeight - 70f) {
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = margin
            }
            val txSym = currencySymbol(tx.currency, isArabic)
            val isInc = tx.type == TransactionType.INCOME
            val amountStr = "${if (isInc) "+" else "-"} ${formatAmountMinor(tx.amountMinor)} $txSym"
            val displayName = tx.itemName.ifBlank { tx.note }.ifBlank { tx.category }
            val debtTag = if (tx.isDebt) " [${debtStatusLabel(tx, isArabic)}]" else ""
            val rowTitle = "$displayName$debtTag"
            val rowMeta = "${tx.date} ${tx.time} • ${tx.category} • ${tx.source}"

            drawAlignedText(canvas, rowTitle, bodyPaint, margin, y, contentWidth - 130, isArabic)
            val amtPaint = TextPaint(bodyPaint).apply {
                color = if (isInc) Color.parseColor("#006C4A") else Color.parseColor("#BA1A1A")
                this.typeface = Typeface.create(typeface, Typeface.BOLD)
            }
            drawAlignedText(canvas, amountStr, amtPaint, pageWidth - margin - 125f, y, 125, false)
            drawAlignedText(canvas, rowMeta, mutedPaint, margin, y + 16f, contentWidth, isArabic)
            y += 38f
            canvas.drawLine(margin, y - 4f, pageWidth - margin, y - 4f, linePaint)
        }

        pdf.finishPage(page)
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    /** Generates a high-resolution PNG report card with KPIs, Donut breakdown & top transactions. */
    fun exportReportImage(
        context: Context,
        analytics: ReportAnalytics,
        filterSummary: String,
        isArabic: Boolean
    ): File {
        val width = 1080
        val topRows = analytics.filteredTransactions.take(8)
        val height = 860 + (topRows.size * 76)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val typeface = loadTypeface(context)

        // Canvas background
        canvas.drawColor(Color.parseColor("#F7F9FB"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val margin = 48f
        val contentW = (width - margin * 2).toInt()

        // Top Navy Banner
        paint.color = Color.parseColor("#091426")
        canvas.drawRoundRect(RectF(margin, margin, width - margin, 220f), 28f, 28f, paint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 24f
            this.typeface = typeface
        }
        drawAlignedText(
            canvas,
            tr(isArabic, "مصروفي • ملخص التقرير", "Masrofy • Report Summary"),
            titlePaint,
            margin + 32f,
            margin + 28f,
            contentW - 64,
            isArabic
        )
        drawAlignedText(
            canvas,
            filterSummary,
            subPaint,
            margin + 32f,
            margin + 98f,
            contentW - 64,
            isArabic
        )

        // 3 KPI Cards
        val s = analytics.primarySummary
        val sym = currencySymbol(s.currency, isArabic)
        val cardW = (contentW - 32f) / 3f
        val kpiY = 252f
        val kpiItems = listOf(
            Triple(tr(isArabic, "إجمالي الدخل", "Income"), "+ ${formatAmountMinor(s.totalIncomeMinor)} $sym", Color.parseColor("#006C4A")),
            Triple(tr(isArabic, "المصروفات", "Expenses"), "- ${formatAmountMinor(s.totalExpenseMinor)} $sym", Color.parseColor("#BA1A1A")),
            Triple(tr(isArabic, "صافي الفائض", "Net Flow"), formatSignedAmount(s.netMinor, s.currency, isArabic), Color.parseColor("#091426"))
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
                textSize = 28f
                this.typeface = Typeface.create(typeface, Typeface.BOLD)
            }
            drawAlignedText(canvas, v, vp, x + 20f, kpiY + 78f, (cardW - 40).toInt(), isArabic)
        }

        // Donut Card
        val donutCardTop = 430f
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(margin, donutCardTop, width - margin, donutCardTop + 340f), 28f, 28f, paint)
        val headPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#091426")
            textSize = 30f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        drawAlignedText(
            canvas,
            tr(isArabic, "توزيع المصروفات حسب التصنيف", "Expense Breakdown by Category"),
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
        analytics.expenseBreakdown.take(5).forEachIndexed { idx, item ->
            strokePaint.color = donutColors[idx % donutColors.size]
            val sweep = (item.percentage / 100f) * 360f
            canvas.drawArc(ringRect, startAngle, sweep, false, strokePaint)
            startAngle += sweep
        }

        // Legend
        var legY = donutCardTop + 100f
        val legPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#191C1E")
            textSize = 24f
            this.typeface = typeface
        }
        analytics.expenseBreakdown.take(5).forEachIndexed { idx, item ->
            paint.color = donutColors[idx % donutColors.size]
            canvas.drawCircle(margin + 340f, legY + 14f, 10f, paint)
            val txt = "${item.category} (${item.percentage.toInt()}%)"
            drawAlignedText(canvas, txt, legPaint, margin + 365f, legY - 4f, contentW - 400, isArabic)
            legY += 44f
        }

        // Matching rows preview
        var rowY = 805f
        topRows.forEach { tx ->
            paint.color = Color.WHITE
            canvas.drawRoundRect(RectF(margin, rowY, width - margin, rowY + 64f), 16f, 16f, paint)
            val txSym = currencySymbol(tx.currency, isArabic)
            val isInc = tx.type == TransactionType.INCOME
            val amt = "${if (isInc) "+" else "-"} ${formatAmountMinor(tx.amountMinor)} $txSym"
            drawAlignedText(
                canvas,
                "${tx.category} • ${tx.source} (${tx.date})",
                legPaint,
                margin + 22f,
                rowY + 16f,
                contentW - 280,
                isArabic
            )
            val ap = TextPaint(legPaint).apply {
                color = if (isInc) Color.parseColor("#006C4A") else Color.parseColor("#BA1A1A")
                this.typeface = Typeface.create(typeface, Typeface.BOLD)
            }
            drawAlignedText(canvas, amt, ap, width - margin - 240f, rowY + 16f, 220, false)
            rowY += 74f
        }

        val file = File(exportsDir(context), "Masrofy_Report_${timestampSlug()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bitmap.recycle()
        return file
    }

    fun exportJsonBackupFile(context: Context, jsonContent: String): File {
        val file = File(exportsDir(context), "Masrofy_Backup_${timestampSlug()}.json")
        file.writeText(jsonContent, Charsets.UTF_8)
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
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
    }

    /**
     * Saves an exported file directly into the phone's Downloads/Masrofy (or Pictures/Masrofy for images)
     * WITHOUT forcing a Share sheet and with ZERO storage permissions (Android 8+ safe).
     * Automatically retries with application/octet-stream if an OEM MediaStore rejects a specific MIME,
     * and gracefully falls back to the app-scoped Downloads/Masrofy directory so backups NEVER crash.
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

        // Android 8 & 9 (API 26-28) OR OEM fallback if MediaStore rejects insert:
        // Zero-permission app-scoped Downloads/Pictures directory
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
            tr(isArabic, "مديونية إيرادات دخلت لحسابي (تم السداد)", "Borrowed Income Debt (Settled)")
        tx.isDebt && tx.isDebtSettled ->
            tr(isArabic, "مديونية مصروفات (تم السداد)", "Expense Debt (Settled)")
        tx.isDebt && tx.type == TransactionType.INCOME ->
            tr(isArabic, "مديونية إيرادات دخلت لحسابي (مستحق السداد)", "Borrowed Income Debt (To Repay)")
        tx.isDebt ->
            tr(isArabic, "مديونية مصروفات (مستحق السداد)", "Expense Debt (To Repay)")
        tx.type == TransactionType.INCOME ->
            tr(isArabic, "إيراد مكتسب", "Earned Income")
        else ->
            tr(isArabic, "مصروف مدفوع", "Paid Expense")
    }
}
