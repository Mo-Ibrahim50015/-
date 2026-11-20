package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.TransactionType
import com.example.ui.CurrencySummary
import com.example.ui.DebtFilter
import com.example.ui.ReportAnalytics
import com.example.ui.ReportFilterState
import com.example.ui.SourceBreakdown
import com.example.ui.computeAnalytics
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.tr
import kotlin.math.abs

@Composable
fun ReportsAnalyticsDashboard(
    analytics: ReportAnalytics,
    filter: ReportFilterState,
    isArabic: Boolean,
    showCharts: Boolean,
    onToggleCategoryFilter: (String) -> Unit,
    onToggleSourceFilter: (String) -> Unit,
    onSelectDebtFilter: (String) -> Unit
) {
    // Allow switching currency tabs seamlessly when multiple currencies match the active filter
    var selectedCurrency by remember(analytics.primaryCurrency, analytics.currencySummaries) {
        mutableStateOf(analytics.primaryCurrency)
    }
    val validCurrency = remember(selectedCurrency, analytics.currencySummaries) {
        if (analytics.currencySummaries.any { it.currency == selectedCurrency }) {
            selectedCurrency
        } else {
            analytics.primaryCurrency
        }
    }
    val activeAnalytics = remember(analytics, validCurrency) {
        if (validCurrency == analytics.primaryCurrency) {
            analytics
        } else {
            computeAnalytics(analytics.filteredTransactions, validCurrency)
        }
    }

    val summary = activeAnalytics.primarySummary
    val sym = currencySymbol(summary.currency, isArabic)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Multi-currency selector pill strip (strictly separates currencies without auto-conversion)
        if (analytics.currencySummaries.size > 1) {
            DashboardCurrencySwitcher(
                summaries = analytics.currencySummaries,
                selectedCurrency = validCurrency,
                isArabic = isArabic,
                onSelectCurrency = { selectedCurrency = it }
            )
        }

        // Expanded 6-Metric Live KPI Grid (always visible & synced with filters)
        ExpandedKpiGrid(
            summary = summary,
            currencySymbol = sym,
            isArabic = isArabic
        )

        // Live Debt & Earned/Paid Breakdown Card (synced with filters)
        if (summary.unpaidDebtCount > 0 || summary.settledDebtCount > 0 || filter.debtFilter != DebtFilter.ALL) {
            DebtOverviewDashboardCard(
                summary = summary,
                activeDebtFilter = filter.debtFilter,
                currencySymbol = sym,
                isArabic = isArabic,
                onSelectDebtFilter = onSelectDebtFilter
            )
        }

        if (showCharts) {
            // Live Smart Financial Insights Card
            SmartInsightsCard(
                analytics = activeAnalytics,
                filter = filter,
                currencySymbol = sym,
                isArabic = isArabic
            )

            // Responsive Bento Layout: 2 columns on medium/tablet screens (>= 540dp), single stack on phones
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 540.dp
                if (isWide) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CashFlowTrendChartCard(
                            dailyTrend = activeAnalytics.dailyTrend,
                            filterType = filter.type,
                            currencySymbol = sym,
                            isArabic = isArabic
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                InteractiveCategoryDonutCard(
                                    expenseBreakdown = activeAnalytics.expenseBreakdown,
                                    incomeBreakdown = activeAnalytics.incomeBreakdown,
                                    totalExpenseMinor = summary.totalExpenseMinor,
                                    totalIncomeMinor = summary.totalIncomeMinor,
                                    filter = filter,
                                    currencySymbol = sym,
                                    isArabic = isArabic,
                                    onToggleCategoryFilter = onToggleCategoryFilter
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                AccountsSourceBreakdownCard(
                                    sources = activeAnalytics.sourceBreakdown,
                                    activeSources = filter.sources,
                                    currencySymbol = sym,
                                    isArabic = isArabic,
                                    onToggleSourceFilter = onToggleSourceFilter
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                WeekdayRhythmChartCard(
                                    weekdayActivity = activeAnalytics.weekdayActivity,
                                    filterType = filter.type,
                                    currencySymbol = sym,
                                    isArabic = isArabic
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                WeeklyFlowComparisonCard(
                                    weeklyFlow = activeAnalytics.weeklyFlow,
                                    summary = summary,
                                    currencySymbol = sym,
                                    isArabic = isArabic
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CashFlowTrendChartCard(
                            dailyTrend = activeAnalytics.dailyTrend,
                            filterType = filter.type,
                            currencySymbol = sym,
                            isArabic = isArabic
                        )
                        InteractiveCategoryDonutCard(
                            expenseBreakdown = activeAnalytics.expenseBreakdown,
                            incomeBreakdown = activeAnalytics.incomeBreakdown,
                            totalExpenseMinor = summary.totalExpenseMinor,
                            totalIncomeMinor = summary.totalIncomeMinor,
                            filter = filter,
                            currencySymbol = sym,
                            isArabic = isArabic,
                            onToggleCategoryFilter = onToggleCategoryFilter
                        )
                        AccountsSourceBreakdownCard(
                            sources = activeAnalytics.sourceBreakdown,
                            activeSources = filter.sources,
                            currencySymbol = sym,
                            isArabic = isArabic,
                            onToggleSourceFilter = onToggleSourceFilter
                        )
                        WeekdayRhythmChartCard(
                            weekdayActivity = activeAnalytics.weekdayActivity,
                            filterType = filter.type,
                            currencySymbol = sym,
                            isArabic = isArabic
                        )
                        WeeklyFlowComparisonCard(
                            weeklyFlow = activeAnalytics.weeklyFlow,
                            summary = summary,
                            currencySymbol = sym,
                            isArabic = isArabic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCurrencySwitcher(
    summaries: List<CurrencySummary>,
    selectedCurrency: String,
    isArabic: Boolean,
    onSelectCurrency: (String) -> Unit
) {
    val ext = LocalSereneColors.current
    SereneCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Payments,
                contentDescription = null,
                tint = ext.incomeColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = tr(isArabic, "عملة التحليل:", "Chart Currency:"),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            summaries.forEach { cs ->
                val selected = cs.currency == selectedCurrency
                val sym = currencySymbol(cs.currency, isArabic)
                val count = cs.incomeCount + cs.expenseCount
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .testTag("dashboard_currency_${cs.currency}")
                        .clickable { onSelectCurrency(cs.currency) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) ext.heroCardBackground else ext.cardSubtleBackground,
                    border = if (selected) null else BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "$sym (${cs.currency}) • $count",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedKpiGrid(
    summary: CurrencySummary,
    currencySymbol: String,
    isArabic: Boolean
) {
    val ext = LocalSereneColors.current
    val totalCount = summary.incomeCount + summary.expenseCount

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Row 1: Primary Financial Triad (Income / Expenses / Net Flow)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DashboardKpiTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_income"),
                title = tr(isArabic, "إجمالي الدخل", "Income"),
                icon = Icons.Outlined.TrendingUp,
                iconTint = ext.incomeColor,
                valueText = "+ ${formatAmountMinor(summary.totalIncomeMinor, false)} $currencySymbol",
                valueColor = ext.incomeColor,
                subtext = tr(isArabic, "${summary.incomeCount} عمليات", "${summary.incomeCount} items")
            )
            DashboardKpiTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_expense"),
                title = tr(isArabic, "المصروفات", "Expenses"),
                icon = Icons.Outlined.TrendingDown,
                iconTint = ext.expenseColor,
                valueText = "- ${formatAmountMinor(summary.totalExpenseMinor, false)} $currencySymbol",
                valueColor = MaterialTheme.colorScheme.onSurface,
                subtext = tr(isArabic, "${summary.expenseCount} عملية", "${summary.expenseCount} items")
            )
            DashboardKpiTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_net"),
                title = tr(isArabic, "صافي الفائض", "Net Flow"),
                icon = Icons.Outlined.AccountBalanceWallet,
                iconTint = if (summary.netMinor >= 0) ext.incomeColor else ext.expenseColor,
                valueText = "${if (summary.netMinor >= 0) "+" else "-"} ${formatAmountMinor(abs(summary.netMinor), false)} $currencySymbol",
                valueColor = if (summary.netMinor >= 0) ext.incomeColor else ext.expenseColor,
                subtext = tr(isArabic, "ادخار ${summary.savingsRatePercent}%", "Savings ${summary.savingsRatePercent}%")
            )
        }

        // Row 2: Analytical Depth Triad (Avg Tx / Highest Tx / Daily Burn Rate)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DashboardKpiTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_avg_tx"),
                title = tr(isArabic, "متوسط العملية", "Avg Tx"),
                icon = Icons.Outlined.AutoGraph,
                iconTint = ext.chartSlate,
                valueText = "${formatAmountMinor(summary.averageTxMinor, false)} $currencySymbol",
                valueColor = MaterialTheme.colorScheme.primary,
                subtext = tr(isArabic, "من $totalCount عملية", "Across $totalCount tx")
            )
            val highest = summary.highestTx
            DashboardKpiTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_highest_tx"),
                title = tr(isArabic, "أعلى عملية", "Peak Tx"),
                icon = Icons.Outlined.LocalFireDepartment,
                iconTint = ext.warningAmber,
                valueText = if (highest != null) {
                    "${formatAmountMinor(highest.amountMinor, false)} $currencySymbol"
                } else {
                    "0 $currencySymbol"
                },
                valueColor = if (highest?.type == TransactionType.INCOME) ext.incomeColor else ext.expenseColor,
                subtext = highest?.category?.ifBlank { tr(isArabic, "غير محدد", "General") }
                    ?: tr(isArabic, "لا توجد بيانات", "No data")
            )
            DashboardKpiTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_daily_avg"),
                title = tr(isArabic, "معدل الصرف/يوم", "Daily Spend"),
                icon = Icons.Outlined.CalendarToday,
                iconTint = ext.chartRose,
                valueText = "${formatAmountMinor(summary.dailyAverageExpenseMinor, false)} $currencySymbol",
                valueColor = MaterialTheme.colorScheme.onSurface,
                subtext = tr(
                    isArabic,
                    "${summary.activeDaysCount} يوم نشط",
                    "${summary.activeDaysCount} active days"
                )
            )
        }
    }
}

@Composable
private fun DashboardKpiTile(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    iconTint: Color,
    valueText: String,
    valueColor: Color,
    subtext: String
) {
    SereneCard(modifier = modifier, contentPadding = PaddingValues(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = valueText,
            style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtext,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DebtOverviewDashboardCard(
    summary: CurrencySummary,
    activeDebtFilter: String,
    currencySymbol: String,
    isArabic: Boolean,
    onSelectDebtFilter: (String) -> Unit
) {
    val ext = LocalSereneColors.current
    val totalDebtEver = (summary.totalUnpaidDebtMinor + summary.settledDebtMinor).coerceAtLeast(1L)
    val settledFraction = (summary.settledDebtMinor.toFloat() / totalDebtEver.toFloat()).coerceIn(0f, 1f)

    SereneCard(
        modifier = Modifier.testTag("dashboard_debts_card"),
        borderColor = ext.warningAmber.copy(alpha = 0.45f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(ext.warningAmber.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RequestQuote,
                        contentDescription = null,
                        tint = ext.warningAmber,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column {
                    Text(
                        text = tr(isArabic, "تحليل المديونيات (إيرادات دخلت لحسابي / مصروفات)", "Debts Breakdown (Income vs Expense)"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tr(
                            isArabic,
                            "في كل الحالات التزامات عليك سدادها • ${summary.unpaidDebtCount} قائم / ${summary.settledDebtCount} مسدد",
                            "All obligations to repay • ${summary.unpaidDebtCount} unpaid / ${summary.settledDebtCount} settled"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = "${formatAmountMinor(summary.totalUnpaidDebtMinor, false)} $currencySymbol",
                style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                color = if (summary.totalUnpaidDebtMinor > 0L) ext.expenseColor else ext.incomeColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Tappable Debt Type Cards (click to filter whole report by Income Debt or Expense Debt)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val incomeDebtSelected = activeDebtFilter == DebtFilter.DEBT_INCOME
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("dashboard_filter_debt_income")
                    .clickable {
                        onSelectDebtFilter(if (incomeDebtSelected) DebtFilter.ALL else DebtFilter.DEBT_INCOME)
                    },
                shape = RoundedCornerShape(10.dp),
                color = if (incomeDebtSelected) ext.warningAmber.copy(alpha = 0.16f) else ext.cardSubtleBackground,
                border = if (incomeDebtSelected) BorderStroke(1.5.dp, ext.warningAmber) else null
            ) {
                Column(modifier = Modifier.padding(9.dp)) {
                    Text(
                        text = tr(isArabic, "مديونية إيرادات دخلت لحسابي", "Income Entered Account"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (incomeDebtSelected) ext.warningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatAmountMinor(summary.unpaidIncomeDebtMinor, false)} $currencySymbol",
                        style = AmountBodyStyle.copy(
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ext.warningAmber,
                        maxLines = 1
                    )
                }
            }
            val expenseDebtSelected = activeDebtFilter == DebtFilter.DEBT_EXPENSE
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("dashboard_filter_debt_expense")
                    .clickable {
                        onSelectDebtFilter(if (expenseDebtSelected) DebtFilter.ALL else DebtFilter.DEBT_EXPENSE)
                    },
                shape = RoundedCornerShape(10.dp),
                color = if (expenseDebtSelected) ext.expenseContainer else ext.cardSubtleBackground,
                border = if (expenseDebtSelected) BorderStroke(1.5.dp, ext.expenseColor) else null
            ) {
                Column(modifier = Modifier.padding(9.dp)) {
                    Text(
                        text = tr(isArabic, "مديونية مصروفات", "Expense Debts"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (expenseDebtSelected) ext.expenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatAmountMinor(summary.unpaidExpenseDebtMinor, false)} $currencySymbol",
                        style = AmountBodyStyle.copy(
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ext.expenseColor,
                        maxLines = 1
                    )
                }
            }
            Surface(
                modifier = Modifier.weight(0.95f),
                shape = RoundedCornerShape(10.dp),
                color = ext.cardSubtleBackground
            ) {
                Column(modifier = Modifier.padding(9.dp)) {
                    Text(
                        text = tr(isArabic, "إيراد مكتسب صافي", "Earned Income"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatAmountMinor(summary.earnedIncomeMinor, false)} $currencySymbol",
                        style = AmountBodyStyle.copy(
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ext.incomeColor,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Settled Payments Breakdown Row (المدفوعات — الكل / إيرادات مسددة / مصروفات مسددة)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allPaymentsSelected = activeDebtFilter == DebtFilter.DEBT_SETTLED
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("dashboard_filter_payments_all")
                    .clickable {
                        onSelectDebtFilter(if (allPaymentsSelected) DebtFilter.ALL else DebtFilter.DEBT_SETTLED)
                    },
                shape = RoundedCornerShape(10.dp),
                color = if (allPaymentsSelected) ext.incomeContainer else ext.cardSubtleBackground,
                border = if (allPaymentsSelected) BorderStroke(1.5.dp, ext.incomeColor) else null
            ) {
                Column(modifier = Modifier.padding(9.dp)) {
                    Text(
                        text = tr(isArabic, "المدفوعات (${summary.settledDebtCount})", "Payments (${summary.settledDebtCount})"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (allPaymentsSelected) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatAmountMinor(summary.settledDebtMinor, false)} $currencySymbol",
                        style = AmountBodyStyle.copy(
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ext.incomeColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val incPaySelected = activeDebtFilter == DebtFilter.PAYMENTS_INCOME
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("dashboard_filter_payments_income")
                    .clickable {
                        onSelectDebtFilter(if (incPaySelected) DebtFilter.ALL else DebtFilter.PAYMENTS_INCOME)
                    },
                shape = RoundedCornerShape(10.dp),
                color = if (incPaySelected) ext.incomeContainer else ext.cardSubtleBackground,
                border = if (incPaySelected) BorderStroke(1.5.dp, ext.incomeColor) else null
            ) {
                Column(modifier = Modifier.padding(9.dp)) {
                    Text(
                        text = tr(isArabic, "سداد إيراد (${summary.settledIncomeDebtCount})", "Paid Inc (${summary.settledIncomeDebtCount})"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (incPaySelected) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatAmountMinor(summary.settledIncomeDebtMinor, false)} $currencySymbol",
                        style = AmountBodyStyle.copy(
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ext.warningAmber,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val expPaySelected = activeDebtFilter == DebtFilter.PAYMENTS_EXPENSE
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("dashboard_filter_payments_expense")
                    .clickable {
                        onSelectDebtFilter(if (expPaySelected) DebtFilter.ALL else DebtFilter.PAYMENTS_EXPENSE)
                    },
                shape = RoundedCornerShape(10.dp),
                color = if (expPaySelected) ext.incomeContainer else ext.cardSubtleBackground,
                border = if (expPaySelected) BorderStroke(1.5.dp, ext.incomeColor) else null
            ) {
                Column(modifier = Modifier.padding(9.dp)) {
                    Text(
                        text = tr(isArabic, "سداد مصروف (${summary.settledExpenseDebtCount})", "Paid Exp (${summary.settledExpenseDebtCount})"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (expPaySelected) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatAmountMinor(summary.settledExpenseDebtMinor, false)} $currencySymbol",
                        style = AmountBodyStyle.copy(
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tr(
                    isArabic,
                    "المديونيات القائمة: ${summary.unpaidDebtCount} • المدفوعات: ${summary.settledDebtCount}",
                    "Active Debts: ${summary.unpaidDebtCount} • Payments: ${summary.settledDebtCount}"
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = tr(
                    isArabic,
                    "نسبة السداد ${(settledFraction * 100).toInt()}%",
                    "Repaid ${(settledFraction * 100).toInt()}%"
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ext.incomeColor
            )
        }
    }
}

@Composable
private fun SmartInsightsCard(
    analytics: ReportAnalytics,
    filter: ReportFilterState,
    currencySymbol: String,
    isArabic: Boolean
) {
    val ext = LocalSereneColors.current
    val summary = analytics.primarySummary
    val totalVolume = summary.totalIncomeMinor + summary.totalExpenseMinor
    if (totalVolume <= 0L) return

    val incomeRatio = (summary.totalIncomeMinor.toFloat() / totalVolume.toFloat()).coerceIn(0f, 1f)
    val expenseRatio = (summary.totalExpenseMinor.toFloat() / totalVolume.toFloat()).coerceIn(0f, 1f)
    val topExpenseCat = analytics.expenseBreakdown.firstOrNull()
    val topIncomeCat = analytics.incomeBreakdown.firstOrNull()
    val topSource = analytics.sourceBreakdown.firstOrNull()
    val peakDay = analytics.weekdayActivity
        .maxByOrNull { if (filter.type == TransactionType.INCOME) it.incomeMinor else it.expenseMinor }
        ?.takeIf { (if (filter.type == TransactionType.INCOME) it.incomeMinor else it.expenseMinor) > 0L }

    val healthBadgeText = when {
        filter.type == TransactionType.EXPENSE -> tr(isArabic, "تحليل المصاريف", "Expense View")
        filter.type == TransactionType.INCOME -> tr(isArabic, "تحليل الدخل", "Income View")
        summary.savingsRatePercent >= 25 -> tr(isArabic, "ادخار ممتاز (${summary.savingsRatePercent}%)", "Strong Savings (${summary.savingsRatePercent}%)")
        summary.netMinor >= 0 -> tr(isArabic, "توازن مستقر", "Balanced Flow")
        else -> tr(isArabic, "المصروف يتجاوز الدخل", "Spending Exceeds Income")
    }
    val healthColor = when {
        filter.type == TransactionType.EXPENSE -> ext.chartRose
        summary.netMinor >= 0 -> ext.incomeColor
        else -> ext.expenseColor
    }

    SereneCard(
        modifier = Modifier.testTag("smart_insights_card"),
        contentPadding = PaddingValues(14.dp)
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
                    imageVector = Icons.Outlined.Savings,
                    contentDescription = null,
                    tint = ext.incomeColor,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = tr(isArabic, "ملخص الأداء المالي اللحظي", "Live Financial Pulse"),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(healthColor.copy(alpha = 0.14f))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = healthBadgeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = healthColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Income vs Expense Proportion Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(ext.cardContainer)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (incomeRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(incomeRatio)
                            .background(ext.incomeColor)
                    )
                }
                if (expenseRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(expenseRatio)
                            .background(ext.heroCardBackground)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tr(isArabic, "الدخل ${(incomeRatio * 100).toInt()}%", "Income ${(incomeRatio * 100).toInt()}%"),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = ext.incomeColor
            )
            Text(
                text = tr(isArabic, "المصروف ${(expenseRatio * 100).toInt()}%", "Expense ${(expenseRatio * 100).toInt()}%"),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Highlight Pills Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val topCat = if (filter.type == TransactionType.INCOME) topIncomeCat else topExpenseCat
            if (topCat != null) {
                InsightMiniChip(
                    label = if (filter.type == TransactionType.INCOME) {
                        tr(isArabic, "أعلى مصدر دخل", "Top Income")
                    } else {
                        tr(isArabic, "أعلى بند صرف", "Top Spend")
                    },
                    value = "${topCat.category} (${topCat.percentage.toInt()}%)",
                    accent = if (filter.type == TransactionType.INCOME) ext.incomeColor else ext.chartRose
                )
            }
            if (topSource != null) {
                InsightMiniChip(
                    label = tr(isArabic, "الحساب الأكثر نشاطاً", "Most Used Account"),
                    value = "${topSource.source} (${topSource.sharePercent.toInt()}%)",
                    accent = ext.heroCardBackground
                )
            }
            if (peakDay != null) {
                val peakAmt = if (filter.type == TransactionType.INCOME) peakDay.incomeMinor else peakDay.expenseMinor
                InsightMiniChip(
                    label = tr(isArabic, "ذروة الأسبوع", "Peak Weekday"),
                    value = "${if (isArabic) peakDay.labelAr else peakDay.labelEn} • ${formatAmountMinor(peakAmt, false)} $currencySymbol",
                    accent = ext.warningAmber
                )
            }
        }
    }
}

@Composable
private fun InsightMiniChip(
    label: String,
    value: String,
    accent: Color
) {
    val ext = LocalSereneColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ext.cardSubtleBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AccountsSourceBreakdownCard(
    sources: List<SourceBreakdown>,
    activeSources: Set<String>,
    currencySymbol: String,
    isArabic: Boolean,
    onToggleSourceFilter: (String) -> Unit
) {
    val ext = LocalSereneColors.current
    SereneCard(
        modifier = Modifier.testTag("accounts_breakdown_card"),
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
                    imageVector = Icons.Outlined.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = tr(isArabic, "تحليل المصادر والحسابات", "Accounts & Sources"),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterAlt,
                    contentDescription = null,
                    tint = ext.incomeColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = tr(isArabic, "اضغط للتصفية", "Tap to filter"),
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.incomeColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (sources.isEmpty()) {
            Text(
                text = tr(
                    isArabic,
                    "لا توجد حركات حسابات مطابقة للفلتر الحالي.",
                    "No account activity matches the active filter."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp)
            )
        } else {
            val maxVol = sources.maxOfOrNull { it.totalVolumeMinor }?.coerceAtLeast(1L) ?: 1L
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sources.take(5).forEach { src ->
                    val isSelected = src.source in activeSources
                    val incRatio = (src.incomeMinor.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
                    val expRatio = (src.expenseMinor.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("source_bar_${src.source}")
                            .clickable { onToggleSourceFilter(src.source) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ext.incomeContainer else ext.cardSubtleBackground,
                        border = if (isSelected) BorderStroke(1.dp, ext.incomeColor) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = tr(isArabic, "محدد", "Selected"),
                                            tint = ext.incomeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = src.source,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "(${src.count})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${formatAmountMinor(src.totalVolumeMinor, false)} $currencySymbol • ${src.sharePercent.toInt()}%",
                                    style = AmountBodyStyle.copy(
                                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(ext.cardContainer)
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    if (incRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(incRatio)
                                                .background(ext.incomeColor)
                                        )
                                    }
                                    if (expRatio > 0f) {
                                        val rem = (1f - incRatio).coerceAtLeast(0.01f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth((expRatio / rem).coerceIn(0f, 1f))
                                                .background(ext.heroCardBackground)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${tr(isArabic, "دخل:", "In:")} ${formatAmountMinor(src.incomeMinor, false)} $currencySymbol",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.incomeColor
                                )
                                Text(
                                    text = "${tr(isArabic, "صرف:", "Out:")} ${formatAmountMinor(src.expenseMinor, false)} $currencySymbol",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
