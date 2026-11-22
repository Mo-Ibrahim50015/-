package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.ObligationEntity
import com.example.data.TransactionEntity
import com.example.ui.theme.LocalSereneColors
import com.example.util.ExportManager
import com.example.util.ExportOutcome
import com.example.util.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reusable compact Export & Download trigger bar for EVERY page in Masrofy.
 * Generates cleanly formatted PDF, Excel (.xls), CSV, or PNG files on Dispatchers.IO,
 * saves directly into Downloads/Masrofy (or Pictures/Masrofy), AND provides 1-tap Open & Share!
 */
@Composable
fun ExportDownloadButton(
    transactions: List<TransactionEntity>,
    obligations: List<ObligationEntity> = emptyList(),
    preferredCurrency: String,
    reportTitle: String,
    filterSummary: String,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    compactLabel: Boolean = false,
    testTag: String = "page_export_download_btn"
) {
    val ext = LocalSereneColors.current
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .testTag(testTag)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(12.dp),
        color = ext.incomeContainer,
        border = BorderStroke(1.dp, ext.incomeColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compactLabel) 10.dp else 12.dp,
                vertical = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = tr(isArabic, "تصدير وتنزيل", "Export & Download"),
                tint = ext.incomeColor,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = if (compactLabel) {
                    tr(isArabic, "تصدير / تنزيل", "Export")
                } else {
                    tr(isArabic, "تصدير وتنزيل الملف (PDF / Excel / CSV / صورة)", "Export & Download (PDF / Excel / CSV / Image)")
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ext.incomeColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showDialog) {
        UniversalExportDialog(
            transactions = transactions,
            obligations = obligations,
            preferredCurrency = preferredCurrency,
            reportTitle = reportTitle,
            filterSummary = filterSummary,
            isArabic = isArabic,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun UniversalExportDialog(
    transactions: List<TransactionEntity>,
    obligations: List<ObligationEntity> = emptyList(),
    preferredCurrency: String,
    reportTitle: String,
    filterSummary: String,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val ext = LocalSereneColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isExporting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var lastOutcome by remember { mutableStateOf<ExportOutcome?>(null) }

    val totalCount = transactions.size + obligations.size

    fun runExport(format: String) {
        if (totalCount == 0) {
            errorMsg = tr(isArabic, "لا توجد عمليات في هذا القسم للتصدير حالياً", "No items available to export right now")
            return
        }
        errorMsg = null
        isExporting = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    when (format) {
                        "pdf" -> {
                            val file = ExportManager.exportTransactionsPdf(
                                context = context,
                                transactions = transactions,
                                obligations = obligations,
                                preferredCurrency = preferredCurrency,
                                reportTitle = reportTitle,
                                filterSummary = filterSummary,
                                isArabic = isArabic
                            )
                            val saved = ExportManager.saveToDeviceDownloads(context, file, "application/pdf", false, isArabic)
                            ExportOutcome(file, saved, "application/pdf", file.name)
                        }
                        "xls" -> {
                            val file = ExportManager.exportExcelWorkbook(
                                context = context,
                                transactions = transactions,
                                isArabic = isArabic,
                                reportTitle = reportTitle,
                                obligations = obligations
                            )
                            val saved = ExportManager.saveToDeviceDownloads(context, file, "application/vnd.ms-excel", false, isArabic)
                            ExportOutcome(file, saved, "application/vnd.ms-excel", file.name)
                        }
                        "csv" -> {
                            val file = ExportManager.exportCsv(
                                context = context,
                                transactions = transactions,
                                isArabic = isArabic,
                                reportTitle = reportTitle,
                                obligations = obligations
                            )
                            val saved = ExportManager.saveToDeviceDownloads(context, file, "text/csv", false, isArabic)
                            ExportOutcome(file, saved, "text/csv", file.name)
                        }
                        else -> {
                            val file = ExportManager.exportTransactionsImage(
                                context = context,
                                transactions = transactions,
                                obligations = obligations,
                                preferredCurrency = preferredCurrency,
                                reportTitle = reportTitle,
                                filterSummary = filterSummary,
                                isArabic = isArabic
                            )
                            val saved = ExportManager.saveToDeviceDownloads(context, file, "image/png", true, isArabic)
                            ExportOutcome(file, saved, "image/png", file.name)
                        }
                    }
                }
            }
            isExporting = false
            result.onSuccess { outcome ->
                lastOutcome = outcome
            }.onFailure { err ->
                errorMsg = tr(
                    isArabic,
                    "حدث خطأ أثناء تجهيز الملف، حاول مرة أخرى (${err.localizedMessage ?: ""})",
                    "Export failed: ${err.localizedMessage ?: "Try again"}"
                )
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("universal_export_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(isArabic, "تصدير وتنزيل منسق", "Formatted Export & Download"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "$reportTitle • ${tr(isArabic, "$totalCount عملية", "$totalCount items")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = tr(isArabic, "إغلاق", "Close"), modifier = Modifier.size(18.dp))
                    }
                }

                // 4 Formatted Export Choices (2x2 Grid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.PictureAsPdf,
                        title = "PDF",
                        subtitle = tr(isArabic, "تقرير جدول منسق", "Formatted Table"),
                        accent = ext.expenseColor,
                        enabled = !isExporting,
                        tag = "export_format_pdf",
                        onClick = { runExport("pdf") }
                    )
                    ExportFormatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.TableChart,
                        title = "Excel (.xls)",
                        subtitle = tr(isArabic, "شيت عربي وإجماليات", "RTL Sheet & Totals"),
                        accent = ext.incomeColor,
                        enabled = !isExporting,
                        tag = "export_format_excel",
                        onClick = { runExport("xls") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Description,
                        title = "CSV",
                        subtitle = tr(isArabic, "ملف بيانات جاهز", "UTF-8 Data File"),
                        accent = MaterialTheme.colorScheme.primary,
                        enabled = !isExporting,
                        tag = "export_format_csv",
                        onClick = { runExport("csv") }
                    )
                    ExportFormatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Image,
                        title = tr(isArabic, "صورة PNG", "PNG Image"),
                        subtitle = tr(isArabic, "بطاقة ملخص جاهزة", "Summary Card"),
                        accent = ext.warningAmber,
                        enabled = !isExporting,
                        tag = "export_format_image",
                        onClick = { runExport("png") }
                    )
                }

                if (isExporting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ext.incomeColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(isArabic, "جاري تنسيق وتنزيل الملف...", "Formatting & downloading file..."),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                errorMsg?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ext.expenseContainer
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ext.expenseColor,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                lastOutcome?.let { outcome ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ext.incomeContainer,
                        border = BorderStroke(1.dp, ext.incomeColor.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ext.incomeColor, modifier = Modifier.size(18.dp))
                                Text(
                                    text = tr(isArabic, "تم التنزيل بنجاح على الهاتف ✓", "Downloaded to Device ✓"),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = ext.incomeColor
                                )
                            }
                            Text(
                                text = outcome.savedDisplayPath ?: outcome.fileName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { ExportManager.openExportedFile(context, outcome.file, outcome.mimeType) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ext.incomeColor, contentColor = Color.White)
                                ) {
                                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tr(isArabic, "فتح الملف", "Open"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                OutlinedButton(
                                    onClick = {
                                        ExportManager.shareFile(
                                            context,
                                            outcome.file,
                                            outcome.mimeType,
                                            tr(isArabic, "مشاركة $reportTitle", "Share $reportTitle")
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ext.incomeColor)
                                ) {
                                    Icon(Icons.Outlined.Share, contentDescription = null, tint = ext.incomeColor, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tr(isArabic, "مشاركة / حفظ", "Share"), color = ext.incomeColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportFormatTile(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .testTag(tag)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = ext.cardSubtleBackground,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
