package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.TransactionType
import com.example.ui.CategoryBreakdown
import com.example.ui.CurrencySummary
import com.example.ui.DailyTrendPoint
import com.example.ui.ReportFilterState
import com.example.ui.WeekdayActivity
import com.example.ui.WeeklyFlowItem
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.formatAmountMinor
import com.example.util.formatCompactAmount
import com.example.util.tr
import kotlin.math.abs

private enum class TrendChartMode {
    CUMULATIVE, DAILY
}

@Composable
fun CashFlowTrendChartCard(
    dailyTrend: List<DailyTrendPoint>,
    filterType: String,
    currencySymbol: String,
    isArabic: Boolean
) {
    val ext = LocalSereneColors.current
    var mode by remember { mutableStateOf(TrendChartMode.CUMULATIVE) }
    var selectedIdx by remember(dailyTrend) {
        mutableIntStateOf((dailyTrend.size - 1).coerceAtLeast(0))
    }

    SereneCard(
        modifier = Modifier.testTag("cash_flow_trend_card"),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = tr(isArabic, "منحنى التدفق النقدي", "Cash Flow Trend"),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Mode Switcher Pills
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = ext.cardSubtleBackground
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    MiniSegmentedPill(
                        label = tr(isArabic, "التراكمي", "Cumulative"),
                        selected = mode == TrendChartMode.CUMULATIVE,
                        tag = "trend_mode_cumulative",
                        onClick = { mode = TrendChartMode.CUMULATIVE }
                    )
                    MiniSegmentedPill(
                        label = tr(isArabic, "يومي", "Daily"),
                        selected = mode == TrendChartMode.DAILY,
                        tag = "trend_mode_daily",
                        onClick = { mode = TrendChartMode.DAILY }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (dailyTrend.isEmpty()) {
            Text(
                text = tr(
                    isArabic,
                    "لا توجد عمليات زمنية مطابقة للفلتر لعرض منحنى التدفق.",
                    "No dated transactions match the active filter to plot trends."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp)
            )
            return@SereneCard
        }

        val safeIdx = selectedIdx.coerceIn(0, dailyTrend.lastIndex)
        val focusedPoint = dailyTrend[safeIdx]

        // Interactive Selected Day Readout Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = ext.cardSubtleBackground
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = focusedPoint.dateIso,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (mode == TrendChartMode.CUMULATIVE) {
                            val sign = if (focusedPoint.cumulativeNetMinor >= 0) "+" else "-"
                            tr(
                                isArabic,
                                "الصافي التراكمي: $sign${formatAmountMinor(abs(focusedPoint.cumulativeNetMinor), false)} $currencySymbol",
                                "Cumulative: $sign${formatAmountMinor(abs(focusedPoint.cumulativeNetMinor), false)} $currencySymbol"
                            )
                        } else {
                            tr(
                                isArabic,
                                "دخل +${formatAmountMinor(focusedPoint.incomeMinor, false)} / صرف -${formatAmountMinor(focusedPoint.expenseMinor, false)}",
                                "In +${formatAmountMinor(focusedPoint.incomeMinor, false)} / Out -${formatAmountMinor(focusedPoint.expenseMinor, false)}"
                            )
                        },
                        style = AmountBodyStyle.copy(
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (mode == TrendChartMode.CUMULATIVE && focusedPoint.cumulativeNetMinor < 0) {
                            ext.expenseColor
                        } else {
                            ext.incomeColor
                        }
                    )
                }
                Text(
                    text = tr(isArabic, "آخر ${dailyTrend.size} أيام نشطة", "Last ${dailyTrend.size} active days"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Canvas Line & Area Chart
        val lineIncomeColor = ext.incomeColor
        val lineExpenseColor = ext.chartRose
        val gridLineColor = ext.cardBorder
        val surfaceBg = ext.cardBackground

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            val leftPad = 8.dp.toPx()
            val rightPad = 8.dp.toPx()
            val topPad = 14.dp.toPx()
            val bottomPad = 14.dp.toPx()
            val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            val stepX = if (dailyTrend.size > 1) chartW / (dailyTrend.size - 1) else 0f

            // Subtle horizontal grid lines
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            for (g in 0..2) {
                val gy = topPad + chartH * (g / 2f)
                drawLine(
                    color = gridLineColor.copy(alpha = 0.65f),
                    start = Offset(leftPad, gy),
                    end = Offset(size.width - rightPad, gy),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            if (mode == TrendChartMode.CUMULATIVE) {
                val values = dailyTrend.map { it.cumulativeNetMinor.toFloat() }
                val minVal = minOf(0f, values.minOrNull() ?: 0f)
                val maxVal = maxOf(1f, values.maxOrNull() ?: 1f)
                val range = (maxVal - minVal).coerceAtLeast(1f)

                fun yFor(v: Float): Float = topPad + chartH * (1f - ((v - minVal) / range))

                // Zero reference baseline if negative values exist
                if (minVal < 0f && maxVal > 0f) {
                    val zeroY = yFor(0f)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(leftPad, zeroY),
                        end = Offset(size.width - rightPad, zeroY),
                        strokeWidth = 1.4.dp.toPx()
                    )
                }

                val points = dailyTrend.mapIndexed { i, pt ->
                    val x = if (dailyTrend.size == 1) leftPad + chartW / 2f else leftPad + i * stepX
                    Offset(x, yFor(pt.cumulativeNetMinor.toFloat()))
                }
                val isPositiveEnd = dailyTrend.last().cumulativeNetMinor >= 0L
                val curveColor = if (isPositiveEnd) lineIncomeColor else lineExpenseColor

                if (points.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val midX = (prev.x + curr.x) / 2f
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, topPad + chartH)
                        lineTo(points.first().x, topPad + chartH)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(curveColor.copy(alpha = 0.24f), curveColor.copy(alpha = 0.01f)),
                            startY = topPad,
                            endY = topPad + chartH
                        )
                    )
                    drawPath(
                        path = linePath,
                        color = curveColor,
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                points.forEachIndexed { i, pt ->
                    val isSelected = i == safeIdx
                    if (isSelected) {
                        drawLine(
                            color = curveColor.copy(alpha = 0.35f),
                            start = Offset(pt.x, topPad),
                            end = Offset(pt.x, topPad + chartH),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawCircle(color = curveColor.copy(alpha = 0.2f), radius = 9.dp.toPx(), center = pt)
                    }
                    drawCircle(color = surfaceBg, radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(), center = pt)
                    drawCircle(
                        color = curveColor,
                        radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                        center = pt
                    )
                }
            } else {
                // Daily Income vs Expense Dual Lines
                val maxVal = dailyTrend.maxOfOrNull { maxOf(it.incomeMinor, it.expenseMinor) }
                    ?.coerceAtLeast(1L)?.toFloat() ?: 1f

                fun yFor(v: Float): Float = topPad + chartH * (1f - (v / maxVal))

                val incPoints = dailyTrend.mapIndexed { i, pt ->
                    val x = if (dailyTrend.size == 1) leftPad + chartW / 2f else leftPad + i * stepX
                    Offset(x, yFor(pt.incomeMinor.toFloat()))
                }
                val expPoints = dailyTrend.mapIndexed { i, pt ->
                    val x = if (dailyTrend.size == 1) leftPad + chartW / 2f else leftPad + i * stepX
                    Offset(x, yFor(pt.expenseMinor.toFloat()))
                }

                fun drawSeries(pts: List<Offset>, color: Color) {
                    if (pts.size >= 2) {
                        val path = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 1 until pts.size) {
                                val prev = pts[i - 1]
                                val curr = pts[i]
                                val midX = (prev.x + curr.x) / 2f
                                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    pts.forEachIndexed { idx, p ->
                        drawCircle(color = surfaceBg, radius = if (idx == safeIdx) 4.5.dp.toPx() else 3.dp.toPx(), center = p)
                        drawCircle(color = color, radius = if (idx == safeIdx) 3.5.dp.toPx() else 2.2.dp.toPx(), center = p)
                    }
                }

                if (filterType != TransactionType.EXPENSE) {
                    drawSeries(incPoints, lineIncomeColor)
                }
                if (filterType != TransactionType.INCOME) {
                    drawSeries(expPoints, lineExpenseColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Tappable Date Axis Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dailyTrend.forEachIndexed { idx, pt ->
                val isSelected = idx == safeIdx
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedIdx = idx },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) ext.heroCardBackground else ext.cardSubtleBackground
                ) {
                    Text(
                        text = pt.shortLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (mode == TrendChartMode.DAILY) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDotLabel(color = ext.incomeColor, label = tr(isArabic, "الدخل اليومي", "Daily Income"))
                Spacer(modifier = Modifier.width(18.dp))
                LegendDotLabel(color = ext.chartRose, label = tr(isArabic, "المصروف اليومي", "Daily Expense"))
            }
        }
    }
}

@Composable
fun InteractiveCategoryDonutCard(
    expenseBreakdown: List<CategoryBreakdown>,
    incomeBreakdown: List<CategoryBreakdown>,
    totalExpenseMinor: Long,
    totalIncomeMinor: Long,
    filter: ReportFilterState,
    currencySymbol: String,
    isArabic: Boolean,
    onToggleCategoryFilter: (String) -> Unit
) {
    val ext = LocalSereneColors.current
    var showIncome by remember(filter.type) {
        mutableStateOf(filter.type == TransactionType.INCOME)
    }
    val breakdown = if (showIncome) incomeBreakdown else expenseBreakdown
    val totalMinor = if (showIncome) totalIncomeMinor else totalExpenseMinor

    var selectedCategoryName by remember(breakdown) { mutableStateOf<String?>(null) }
    val selectedItem = remember(selectedCategoryName, breakdown) {
        breakdown.firstOrNull { it.category == selectedCategoryName }
    }

    val palette = listOf(
        ext.heroCardBackground,
        ext.incomeColor,
        ext.chartSlate,
        ext.chartRose,
        ext.warningAmber,
        MaterialTheme.colorScheme.secondary
    )

    SereneCard(
        modifier = Modifier.testTag("category_donut_card"),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (showIncome) {
                        tr(isArabic, "توزيع المدخولات", "Income Breakdown")
                    } else {
                        tr(isArabic, "توزيع المصروفات", "Expense Breakdown")
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = ext.cardSubtleBackground
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    MiniSegmentedPill(
                        label = tr(isArabic, "مصاريف", "Spend"),
                        selected = !showIncome,
                        tag = "donut_mode_expense",
                        onClick = { showIncome = false }
                    )
                    MiniSegmentedPill(
                        label = tr(isArabic, "دخل", "Income"),
                        selected = showIncome,
                        tag = "donut_mode_income",
                        onClick = { showIncome = true }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (breakdown.isEmpty()) {
            Text(
                text = if (showIncome) {
                    tr(isArabic, "لا توجد مدخولات مطابقة للفلتر الحالي.", "No matching income for the active filter.")
                } else {
                    tr(isArabic, "لا توجد مصروفات مطابقة للفلتر الحالي.", "No matching expenses for the active filter.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            )
            return@SereneCard
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(134.dp),
                contentAlignment = Alignment.Center
            ) {
                val ringTrack = ext.cardContainer
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val baseStroke = 18.dp.toPx()
                    val highlightStroke = 23.dp.toPx()
                    drawArc(
                        color = ringTrack,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = baseStroke, cap = StrokeCap.Butt)
                    )
                    var startAngle = -90f
                    breakdown.take(6).forEachIndexed { idx, item ->
                        val sweep = (item.percentage / 100f) * 360f
                        val isFocused = selectedItem?.category == item.category
                        drawArc(
                            color = palette[idx % palette.size].copy(
                                alpha = if (selectedItem == null || isFocused) 1f else 0.35f
                            ),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(
                                width = if (isFocused) highlightStroke else baseStroke,
                                cap = StrokeCap.Butt
                            )
                        )
                        startAngle += sweep
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = selectedItem?.category ?: tr(isArabic, "الإجمالي", "Total"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = formatCompactAmount(selectedItem?.totalMinor ?: totalMinor),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (selectedItem != null) {
                        Text(
                            text = "${selectedItem.percentage.toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ext.incomeColor
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                breakdown.take(6).forEachIndexed { idx, item ->
                    val isFocused = selectedItem?.category == item.category
                    val isFiltered = item.category in filter.categories
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedCategoryName = if (isFocused) null else item.category
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isFocused || isFiltered) ext.cardSubtleBackground else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(palette[idx % palette.size])
                            )
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${item.percentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isFiltered) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Interactive Drill-Down Action Strip when a slice is tapped
        if (selectedItem != null) {
            val isFiltered = selectedItem.category in filter.categories
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = ext.cardSubtleBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedItem.category} • ${selectedItem.count} ${tr(isArabic, "عمليات", "tx")}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatAmountMinor(selectedItem.totalMinor, false)} $currencySymbol",
                            style = AmountBodyStyle.copy(
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("donut_filter_category_btn")
                            .clickable { onToggleCategoryFilter(selectedItem.category) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isFiltered) ext.expenseContainer else ext.heroCardBackground
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterAlt,
                                contentDescription = null,
                                tint = if (isFiltered) ext.expenseColor else ext.heroCardOnBackground,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isFiltered) {
                                    tr(isArabic, "إزالة من الفلتر", "Remove Filter")
                                } else {
                                    tr(isArabic, "تصفية بالتصنيف", "Filter Category")
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isFiltered) ext.expenseColor else ext.heroCardOnBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekdayRhythmChartCard(
    weekdayActivity: List<WeekdayActivity>,
    filterType: String,
    currencySymbol: String,
    isArabic: Boolean
) {
    val ext = LocalSereneColors.current
    val isIncomeMode = filterType == TransactionType.INCOME
    val values = weekdayActivity.map { if (isIncomeMode) it.incomeMinor else it.expenseMinor }
    val maxVal = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val hasAny = values.any { it > 0L }
    val peakIdx = remember(values) {
        values.indices.maxByOrNull { values[it] } ?: 0
    }
    var selectedDayIdx by remember(weekdayActivity) { mutableIntStateOf(peakIdx) }
    val safeDayIdx = selectedDayIdx.coerceIn(0, (weekdayActivity.size - 1).coerceAtLeast(0))

    SereneCard(
        modifier = Modifier.testTag("weekday_rhythm_card"),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarViewWeek,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isIncomeMode) {
                        tr(isArabic, "إيقاع الدخل الأسبوعي", "Weekly Income Rhythm")
                    } else {
                        tr(isArabic, "إيقاع الصرف بأيام الأسبوع", "Spending by Weekday")
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (hasAny && weekdayActivity.isNotEmpty()) {
                val focused = weekdayActivity[safeDayIdx]
                val focusedVal = if (isIncomeMode) focused.incomeMinor else focused.expenseMinor
                Text(
                    text = "${if (isArabic) focused.labelAr else focused.labelEn}: ${formatCompactAmount(focusedVal)} $currencySymbol",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isIncomeMode) ext.incomeColor else ext.chartRose
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!hasAny) {
            Text(
                text = tr(
                    isArabic,
                    "لا توجد بيانات كافية لعرض نشاط أيام الأسبوع.",
                    "Not enough matching activity to display weekday rhythm."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp)
            )
            return@SereneCard
        }

        val activeBarColor = if (isIncomeMode) ext.incomeColor else ext.chartRose
        val defaultBarColor = ext.heroCardBackground.copy(alpha = 0.72f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            weekdayActivity.forEachIndexed { idx, day ->
                val v = if (isIncomeMode) day.incomeMinor else day.expenseMinor
                val frac = (v.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val isSelected = idx == safeDayIdx
                val isPeak = idx == peakIdx && v > 0L

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedDayIdx = idx },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = if (v > 0L) formatCompactAmount(v) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected || isPeak) activeBarColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.68f)
                                .fillMaxHeight(frac.coerceAtLeast(if (v > 0L) 0.08f else 0.03f))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(
                                    when {
                                        v == 0L -> ext.cardContainer
                                        isSelected || isPeak -> activeBarColor
                                        else -> defaultBarColor
                                    }
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isArabic) day.labelAr else day.labelEn,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyFlowComparisonCard(
    weeklyFlow: List<WeeklyFlowItem>,
    summary: CurrencySummary,
    currencySymbol: String,
    isArabic: Boolean
) {
    val ext = LocalSereneColors.current
    val balanceLabel = when {
        summary.totalIncomeMinor == 0L && summary.totalExpenseMinor == 0L ->
            tr(isArabic, "لا توجد بيانات", "No data")
        summary.netMinor >= 0 -> tr(isArabic, "توازن ممتاز", "Well Balanced")
        else -> tr(isArabic, "عجز نقدي", "Deficit")
    }

    SereneCard(
        modifier = Modifier.testTag("weekly_flow_card"),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = tr(isArabic, "التدفق عبر الأسابيع", "Weekly Cash Flow"),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = balanceLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (summary.netMinor >= 0) ext.incomeColor else ext.expenseColor
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val maxWeekTotal = remember(weeklyFlow) {
            weeklyFlow.maxOfOrNull { it.incomeMinor + it.expenseMinor }
                ?.coerceAtLeast(1L) ?: 1L
        }
        val weekNames = if (isArabic) {
            listOf("الأسبوع الأول", "الأسبوع الثاني", "الأسبوع الثالث", "الأسبوع الرابع")
        } else {
            listOf("Week 1", "Week 2", "Week 3", "Week 4")
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            weeklyFlow.forEachIndexed { idx, w ->
                val incFrac = (w.incomeMinor.toFloat() / maxWeekTotal.toFloat()).coerceIn(0f, 1f)
                val expFrac = (w.expenseMinor.toFloat() / maxWeekTotal.toFloat()).coerceIn(0f, 1f)
                val weekNet = w.incomeMinor - w.expenseMinor

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = weekNames.getOrElse(idx) { "W${idx + 1}" },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (w.incomeMinor > 0L || w.expenseMinor > 0L) {
                                val netSign = if (weekNet >= 0L) "+" else "-"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (weekNet >= 0L) ext.incomeContainer else ext.expenseContainer
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "$netSign${formatCompactAmount(abs(weekNet))}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (weekNet >= 0L) ext.incomeColor else ext.expenseColor
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${formatAmountMinor(w.incomeMinor, false)} ${tr(isArabic, "دخل", "in")} / ${formatAmountMinor(w.expenseMinor, false)} ${tr(isArabic, "صرف", "out")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(11.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(ext.cardContainer)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (incFrac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(incFrac)
                                        .background(ext.incomeColor)
                                )
                            }
                            if (expFrac > 0f) {
                                val remainingSpace = (1f - incFrac).coerceAtLeast(0.01f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth((expFrac / remainingSpace).coerceIn(0f, 1f))
                                        .background(ext.heroCardBackground)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDotLabel(color = ext.incomeColor, label = tr(isArabic, "الدخل الوارد", "Income"))
            Spacer(modifier = Modifier.width(20.dp))
            LegendDotLabel(color = ext.heroCardBackground, label = tr(isArabic, "المصروف الصادر", "Expense"))
        }
    }
}

@Composable
private fun MiniSegmentedPill(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) ext.heroCardBackground else Color.Transparent,
        border = if (selected) null else BorderStroke(0.dp, Color.Transparent)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun LegendDotLabel(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 12.dp, height = 8.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
