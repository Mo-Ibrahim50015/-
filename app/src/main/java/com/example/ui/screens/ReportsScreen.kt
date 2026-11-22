package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AccountEntity
import com.example.data.AppSettingsEntity
import com.example.data.CategoryEntity
import com.example.data.ObligationEntity
import com.example.data.SavedFilterEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.ComparisonOperator
import com.example.ui.DatePreset
import com.example.ui.DebtFilter
import com.example.ui.ReportAnalytics
import com.example.ui.ReportFilterState
import com.example.ui.components.CollapsibleFilterDropdownCard
import com.example.ui.components.DropdownFilterOption
import com.example.ui.components.ExportDownloadButton
import com.example.ui.components.FastTimePickerDialog
import com.example.ui.components.ReportsAnalyticsDashboard
import com.example.ui.components.SecurityVerificationDialog
import com.example.ui.components.SereneCard
import com.example.ui.components.SmartMultiSelectDropdown
import com.example.ui.components.SmartSingleSelectDropdown
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.SupportedCurrencies
import com.example.util.currencySymbol
import com.example.util.currentIsoDate
import com.example.util.formatAmountMinor
import com.example.util.formatTime12Hour
import com.example.util.iconForKey
import com.example.util.parseAmountToMinor
import com.example.util.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportsTopTab {
    REPORTS, DASHBOARD, PLAN
}

private enum class FilterDockTab {
    TYPE, SOURCE, CURRENCY, OPERATOR, DATES
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    analytics: ReportAnalytics,
    filter: ReportFilterState,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    savedFilters: List<SavedFilterEntity>,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    allTransactions: List<TransactionEntity> = emptyList(),
    obligations: List<ObligationEntity> = emptyList(),
    requestedTopTab: ReportsTopTab = ReportsTopTab.REPORTS,
    requestedTopTabNonce: Int = 0,
    onUpdateFilter: ((ReportFilterState) -> ReportFilterState) -> Unit,
    onResetFilter: () -> Unit,
    onSaveFilter: (name: String, asDefault: Boolean) -> Unit,
    onApplySavedFilter: (SavedFilterEntity) -> Unit,
    onDeleteSavedFilter: (Long) -> Unit,
    onExportPdf: (filterSummary: String) -> Unit,
    onExportImage: (filterSummary: String) -> Unit,
    onExportExcel: (List<TransactionEntity>) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onToggleDebtSettled: (Long, Boolean) -> Unit,
    onSetObligationPaid: (Long, Boolean) -> Unit = { _, _ -> },
    onViewFullLog: () -> Unit,
    onOpenIncomeDetails: () -> Unit,
    onOpenExpenseDetails: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenLifetimeMoney: () -> Unit = {},
    onOpenObligations: () -> Unit = {},
    onOpenMoneyFlowGuide: () -> Unit = {},
    onOpenDrillDown: (FlowDrillDownSpec) -> Unit = {}
) {
    val ext = LocalSereneColors.current

    var activeTopTab by rememberSaveable { mutableStateOf(requestedTopTab) }
    var lastAppliedTabNonce by rememberSaveable { mutableStateOf(requestedTopTabNonce) }
    LaunchedEffect(requestedTopTabNonce) {
        if (requestedTopTabNonce > lastAppliedTabNonce) {
            activeTopTab = requestedTopTab
            lastAppliedTabNonce = requestedTopTabNonce
        }
    }
    var activeDockTab by remember { mutableStateOf(FilterDockTab.TYPE) }
    var showSaveFilterDialog by remember { mutableStateOf(false) }
    var saveFilterName by remember { mutableStateOf("") }
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val reportsListState = rememberLazyListState()
    val dashboardListState = rememberLazyListState()

    // Local text states synced with amount filter
    var amountInput1 by remember(filter.amountValue1Minor) {
        mutableStateOf(filter.amountValue1Minor?.let { String.format(Locale.US, "%.0f", it / 100.0) } ?: "")
    }
    var amountInput2 by remember(filter.amountValue2Minor) {
        mutableStateOf(filter.amountValue2Minor?.let { String.format(Locale.US, "%.0f", it / 100.0) } ?: "")
    }

    // Date & Time picker target slots: 1 or 2
    var activeDatePickerSlot by remember { mutableStateOf<Int?>(null) }
    var activeTimePickerSlot by remember { mutableStateOf<Int?>(null) }

    val summary = analytics.primarySummary
    val sym = currencySymbol(summary.currency, isArabic)

    val activeFilterChips = remember(filter, isArabic) {
        buildList {
            when (filter.type) {
                TransactionType.EXPENSE -> add(tr(isArabic, "مصروفات", "Expenses"))
                TransactionType.INCOME -> add(tr(isArabic, "مدخولات", "Income"))
                else -> add(tr(isArabic, "الكل", "All"))
            }
            when (filter.debtFilter) {
                DebtFilter.NON_DEBT -> add(tr(isArabic, "مدفوع ومكتسب", "Paid & Earned"))
                DebtFilter.DEBT_ALL -> add(tr(isArabic, "كل المديونيات", "All Debts"))
                DebtFilter.DEBT_UNPAID -> add(tr(isArabic, "مديونيات قائمة", "Unpaid Debts"))
                DebtFilter.DEBT_INCOME -> add(tr(isArabic, "مديونية إيرادات قائمة", "Unpaid Income Debts"))
                DebtFilter.DEBT_EXPENSE -> add(tr(isArabic, "مديونية مصروفات قائمة", "Unpaid Expense Debts"))
                DebtFilter.DEBT_SETTLED -> add(tr(isArabic, "المدفوعات (الكل)", "Payments (All)"))
                DebtFilter.PAYMENTS_INCOME -> add(tr(isArabic, "مدفوعات إيرادات مسددة", "Settled Income Payments"))
                DebtFilter.PAYMENTS_EXPENSE -> add(tr(isArabic, "مدفوعات مصروفات مسددة", "Settled Expense Payments"))
            }
            if (filter.currencies.isNotEmpty()) {
                val cs = filter.currencies.joinToString("، ") { currencySymbol(it, isArabic) }
                add(tr(isArabic, "العملة: $cs", "Currency: $cs"))
            }
            if (filter.sources.isNotEmpty()) {
                add(tr(isArabic, "المصدر: ${filter.sources.size}", "Sources: ${filter.sources.size}"))
            }
            if (filter.categories.isNotEmpty()) {
                add(tr(isArabic, "التصنيف: ${filter.categories.size}", "Categories: ${filter.categories.size}"))
            }
            if (filter.amountOp != ComparisonOperator.NONE && filter.amountValue1Minor != null) {
                val v1 = formatAmountMinor(filter.amountValue1Minor, false)
                val amtStr = if (filter.amountOp == ComparisonOperator.BETWEEN && filter.amountValue2Minor != null) {
                    val v2 = formatAmountMinor(filter.amountValue2Minor, false)
                    tr(isArabic, "المبلغ: $v1 - $v2 $sym", "Amount: $v1 - $v2 $sym")
                } else {
                    tr(isArabic, "المبلغ ${filter.amountOp.symbol} $v1 $sym", "Amount ${filter.amountOp.symbol} $v1 $sym")
                }
                add(amtStr)
            }
            if (filter.datePreset != DatePreset.ALL) {
                if (filter.datePreset == DatePreset.CUSTOM && filter.dateOp != ComparisonOperator.NONE && filter.dateValue1.isNotBlank()) {
                    val dText = if (filter.dateOp == ComparisonOperator.BETWEEN && filter.dateValue2.isNotBlank()) {
                        "${filter.dateValue1} ↔ ${filter.dateValue2}"
                    } else "${filter.dateOp.symbol} ${filter.dateValue1}"
                    add(tr(isArabic, "التاريخ: $dText", "Date: $dText"))
                } else {
                    add(if (isArabic) filter.datePreset.labelAr else filter.datePreset.labelEn)
                }
            }
            if (filter.timeOp != ComparisonOperator.NONE && filter.timeValue1.isNotBlank()) {
                val tText = if (filter.timeOp == ComparisonOperator.BETWEEN && filter.timeValue2.isNotBlank()) {
                    "${filter.timeValue1} ↔ ${filter.timeValue2}"
                } else "${filter.timeOp.symbol} ${filter.timeValue1}"
                add(tr(isArabic, "الوقت: $tText", "Time: $tText"))
            }
        }
    }
    val filterSummaryText = activeFilterChips.joinToString(" • ")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp)
        ) {
            // Sticky Top Tab Switcher: Reports & Filters | Data Analytics Dashboard
            ReportsTopTabBar(
                activeTab = activeTopTab,
                matchingCount = analytics.filteredTransactions.size,
                hasActiveFilters = filter.hasAnyActiveFilter(),
                isArabic = isArabic,
                onSelectTab = { activeTopTab = it }
            )

            when (activeTopTab) {
                ReportsTopTab.REPORTS -> {
                    LazyColumn(
                        state = reportsListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 0. Quick Access Strip to Full Detail Pages (Income, Expenses, Upcoming Due, Actually Paid)
                        item {
                            val actualPaidCount = summary.paidExpenseCount + summary.settledDebtCount + summary.paidScheduledObligationsCount
                            val upcomingDueCount = summary.unpaidDebtCount + summary.pendingScheduledObligationsCount
                            ReportsQuickDetailsStrip(
                                totalIncomeMinor = summary.actualCashInMinor,
                                totalExpenseMinor = summary.totalExpenseMinor,
                                totalUnpaidDebtMinor = summary.totalPendingObligationsMinor,
                                unpaidDebtCount = upcomingDueCount,
                                totalPaidMinor = summary.actualCashOutMinor,
                                paidCount = actualPaidCount,
                                currencySymbol = sym,
                                isArabic = isArabic,
                                onOpenIncomeDetails = onOpenIncomeDetails,
                                onOpenExpenseDetails = onOpenExpenseDetails,
                                onOpenDebts = onOpenDebts,
                                onOpenPayments = onOpenPayments,
                                onOpenMoneyFlowGuide = { activeTopTab = ReportsTopTab.PLAN }
                            )
                        }

                        // 1. Smart Collapsible Filter Dropdown Card (Saves space & auto-searchable >5 items)
                        item {
                            val activeCount = remember(filter) {
                                (if (filter.type != "all") 1 else 0) +
                                    (if (filter.debtFilter != DebtFilter.ALL) 1 else 0) +
                                    (if (filter.categories.isNotEmpty()) 1 else 0) +
                                    (if (filter.sources.isNotEmpty()) 1 else 0) +
                                    (if (filter.currencies.isNotEmpty()) 1 else 0) +
                                    (if (filter.amountOp != ComparisonOperator.NONE && filter.amountValue1Minor != null) 1 else 0) +
                                    (if (filter.datePreset != DatePreset.ALL) 1 else 0) +
                                    (if (filter.timeOp != ComparisonOperator.NONE) 1 else 0)
                            }
                            CollapsibleFilterDropdownCard(
                                isArabic = isArabic,
                                activeFilterCount = activeCount,
                                onResetFilters = if (activeCount > 0) {
                                    {
                                        amountInput1 = ""
                                        amountInput2 = ""
                                        onResetFilter()
                                    }
                                } else null,
                                title = tr(isArabic, "الفلاتر الذكية السريعة", "Smart Quick Filters"),
                                testTagPrefix = "reports_filter_dropdown"
                            ) {
                                // Save Filter / Save Default Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .testTag("save_filter_btn")
                                            .clickable {
                                                saveFilterName = ""
                                                showSaveFilterDialog = true
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = ext.cardContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.BookmarkAdd,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = tr(isArabic, "حفظ الفلتر", "Save Preset"),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .testTag("save_default_filter_btn")
                                            .clickable {
                                                onSaveFilter(
                                                    tr(isArabic, "الفلتر الافتراضي", "Default Filter"),
                                                    true
                                                )
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = ext.heroCardBackground
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.Verified,
                                                contentDescription = null,
                                                tint = ext.heroCardOnBackground,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = tr(isArabic, "حفظ كافتراضية", "Set Default"),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ext.heroCardOnBackground
                                            )
                                        }
                                    }
                                }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5-Column Compact Icon Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterDockIconTab(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.SwapHoriz,
                            label = tr(isArabic, "النوع", "Type"),
                            selected = activeDockTab == FilterDockTab.TYPE,
                            hasActive = filter.type != "all" || filter.debtFilter != DebtFilter.ALL || filter.categories.isNotEmpty(),
                            onClick = { activeDockTab = FilterDockTab.TYPE }
                        )
                        FilterDockIconTab(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.AccountBalance,
                            label = tr(isArabic, "المصدر", "Source"),
                            selected = activeDockTab == FilterDockTab.SOURCE,
                            hasActive = filter.sources.isNotEmpty(),
                            onClick = { activeDockTab = FilterDockTab.SOURCE }
                        )
                        FilterDockIconTab(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Payments,
                            label = tr(isArabic, "العملة", "Currency"),
                            selected = activeDockTab == FilterDockTab.CURRENCY,
                            hasActive = filter.currencies.isNotEmpty(),
                            onClick = { activeDockTab = FilterDockTab.CURRENCY }
                        )
                        FilterDockIconTab(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Calculate,
                            label = tr(isArabic, "الشرط", "Amount"),
                            selected = activeDockTab == FilterDockTab.OPERATOR,
                            hasActive = filter.amountOp != ComparisonOperator.NONE && filter.amountValue1Minor != null,
                            onClick = { activeDockTab = FilterDockTab.OPERATOR }
                        )
                        FilterDockIconTab(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.CalendarMonth,
                            label = tr(isArabic, "التاريخ", "Date"),
                            selected = activeDockTab == FilterDockTab.DATES,
                            hasActive = filter.datePreset != DatePreset.ALL || filter.timeOp != ComparisonOperator.NONE,
                            onClick = { activeDockTab = FilterDockTab.DATES }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic Sub-Panel
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ext.cardSubtleBackground
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (activeDockTab) {
                                FilterDockTab.TYPE -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = tr(isArabic, "تحديد اتجاه الحركة المالية", "Transaction Direction"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = when (filter.type) {
                                                TransactionType.EXPENSE -> tr(isArabic, "مصروفات", "Expenses")
                                                TransactionType.INCOME -> tr(isArabic, "مدخولات", "Income")
                                                else -> tr(isArabic, "الكل", "All")
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = ext.incomeColor
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TypeFilterOptionButton(
                                            modifier = Modifier.weight(1f),
                                            label = tr(isArabic, "الكل", "All"),
                                            icon = Icons.Outlined.AllInclusive,
                                            iconTint = null,
                                            selected = filter.type == "all",
                                            onClick = { onUpdateFilter { it.copy(type = "all") } }
                                        )
                                        TypeFilterOptionButton(
                                            modifier = Modifier.weight(1f),
                                            label = tr(isArabic, "مصروفات", "Expenses"),
                                            icon = Icons.Outlined.ArrowUpward,
                                            iconTint = ext.expenseColor,
                                            selected = filter.type == TransactionType.EXPENSE,
                                            onClick = { onUpdateFilter { it.copy(type = TransactionType.EXPENSE) } }
                                        )
                                        TypeFilterOptionButton(
                                            modifier = Modifier.weight(1f),
                                            label = tr(isArabic, "مدخولات", "Income"),
                                            icon = Icons.Outlined.ArrowDownward,
                                            iconTint = ext.incomeColor,
                                            selected = filter.type == TransactionType.INCOME,
                                            onClick = { onUpdateFilter { it.copy(type = TransactionType.INCOME) } }
                                        )
                                    }

                                    // Debt / Settlement Nature Searchable Dropdown (>5 items)
                                    val debtDropdownOptions = remember(isArabic) {
                                        listOf(
                                            DropdownFilterOption(DebtFilter.ALL, tr(isArabic, "الكل", "All")),
                                            DropdownFilterOption(DebtFilter.NON_DEBT, tr(isArabic, "مدفوع ومكتسب", "Paid & Earned")),
                                            DropdownFilterOption(DebtFilter.DEBT_UNPAID, tr(isArabic, "مبالغ لسه هتندفع", "Upcoming Unpaid")),
                                            DropdownFilterOption(DebtFilter.DEBT_INCOME, tr(isArabic, "سلف ومبالغ مستلمة للرد", "Borrowed to Repay")),
                                            DropdownFilterOption(DebtFilter.DEBT_EXPENSE, tr(isArabic, "مصاريف لسه هتندفع", "Unpaid Expenses")),
                                            DropdownFilterOption(DebtFilter.DEBT_SETTLED, tr(isArabic, "المدفوعات المسددة (الكل)", "Settled Payments (All)")),
                                            DropdownFilterOption(DebtFilter.PAYMENTS_INCOME, tr(isArabic, "سلف تم سدادها", "Repaid Loans")),
                                            DropdownFilterOption(DebtFilter.PAYMENTS_EXPENSE, tr(isArabic, "مصاريف مؤجلة تم سدادها", "Settled Expenses")),
                                            DropdownFilterOption(DebtFilter.DEBT_ALL, tr(isArabic, "كل العمليات المؤجلة", "All Deferred"))
                                        )
                                    }
                                    SmartSingleSelectDropdown(
                                        label = tr(isArabic, "حالة السداد والمبالغ المؤجلة", "Payment & Debt Status"),
                                        selectedValue = filter.debtFilter,
                                        options = debtDropdownOptions,
                                        isArabic = isArabic,
                                        onSelected = { key -> onUpdateFilter { it.copy(debtFilter = key) } },
                                        isHighlighted = filter.debtFilter != DebtFilter.ALL,
                                        testTag = "reports_debt_status_dropdown"
                                    )

                                    // Multi-select Category Dropdown (>5 auto-searchable!)
                                    if (categories.isNotEmpty()) {
                                        SmartMultiSelectDropdown(
                                            label = tr(isArabic, "التصنيف (اختيار متعدد)", "Category (multi-select)"),
                                            options = categories.map { it.name }.distinct(),
                                            selectedValues = filter.categories,
                                            isArabic = isArabic,
                                            onToggle = { cat ->
                                                onUpdateFilter { f ->
                                                    val next = if (cat in f.categories) f.categories - cat else f.categories + cat
                                                    f.copy(categories = next)
                                                }
                                            },
                                            onClear = { onUpdateFilter { it.copy(categories = emptySet()) } },
                                            testTag = "reports_categories_dropdown"
                                        )
                                    }
                                }

                                FilterDockTab.SOURCE -> {
                                    SmartMultiSelectDropdown(
                                        label = tr(isArabic, "طريقة الدفع أو الحساب المالي", "Accounts / Payment Sources"),
                                        options = accounts.map { it.name }.distinct(),
                                        selectedValues = filter.sources,
                                        isArabic = isArabic,
                                        onToggle = { src ->
                                            onUpdateFilter { f ->
                                                val next = if (src in f.sources) f.sources - src else f.sources + src
                                                f.copy(sources = next)
                                            }
                                        },
                                        onClear = { onUpdateFilter { it.copy(sources = emptySet()) } },
                                        testTag = "reports_sources_dropdown"
                                    )
                                }

                                FilterDockTab.CURRENCY -> {
                                    SmartMultiSelectDropdown(
                                        label = tr(isArabic, "عملة العمليات المسجلة", "Transaction Currencies"),
                                        options = SupportedCurrencies.map { it.code },
                                        selectedValues = filter.currencies,
                                        isArabic = isArabic,
                                        onToggle = { code ->
                                            onUpdateFilter { f ->
                                                val next = if (code in f.currencies) f.currencies - code else f.currencies + code
                                                f.copy(currencies = next)
                                            }
                                        },
                                        onClear = { onUpdateFilter { it.copy(currencies = emptySet()) } },
                                        testTag = "reports_currencies_dropdown"
                                    )
                                }

                                FilterDockTab.OPERATOR -> {
                                    Text(
                                        text = tr(isArabic, "الشرط الحسابي لقيمة المبلغ:", "Amount Comparison Operator:"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ComparisonOperator.entries.forEach { op ->
                                            SelectableFilterPill(
                                                label = if (op == ComparisonOperator.NONE) {
                                                    tr(isArabic, "بدون", "None")
                                                } else if (op == ComparisonOperator.BETWEEN) {
                                                    tr(isArabic, "بين قيمتين", "Between")
                                                } else op.symbol,
                                                selected = filter.amountOp == op,
                                                onClick = {
                                                    onUpdateFilter { state ->
                                                        if (op == ComparisonOperator.NONE) {
                                                            amountInput1 = ""
                                                            amountInput2 = ""
                                                            state.copy(amountOp = op, amountValue1Minor = null, amountValue2Minor = null)
                                                        } else {
                                                            state.copy(amountOp = op)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    if (filter.amountOp != ComparisonOperator.NONE) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = amountInput1,
                                                onValueChange = { raw ->
                                                    amountInput1 = raw
                                                    onUpdateFilter { it.copy(amountValue1Minor = parseAmountToMinor(raw)) }
                                                },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                placeholder = {
                                                    Text(
                                                        if (filter.amountOp == ComparisonOperator.BETWEEN) tr(isArabic, "من", "Min")
                                                        else tr(isArabic, "مثال: 1000", "e.g. 1000")
                                                    )
                                                },
                                                suffix = { Text(sym, style = MaterialTheme.typography.labelSmall) },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = ext.cardBackground,
                                                    unfocusedContainerColor = ext.cardBackground
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (filter.amountOp == ComparisonOperator.BETWEEN) {
                                                OutlinedTextField(
                                                    value = amountInput2,
                                                    onValueChange = { raw ->
                                                        amountInput2 = raw
                                                        onUpdateFilter { it.copy(amountValue2Minor = parseAmountToMinor(raw)) }
                                                    },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    placeholder = { Text(tr(isArabic, "إلى", "Max")) },
                                                    suffix = { Text(sym, style = MaterialTheme.typography.labelSmall) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = ext.cardBackground,
                                                        unfocusedContainerColor = ext.cardBackground
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }

                                FilterDockTab.DATES -> {
                                    Text(
                                        text = tr(isArabic, "الفترة الزمنية للتقرير:", "Report Date Range:"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        DatePreset.entries.forEach { preset ->
                                            SelectableFilterPill(
                                                label = if (isArabic) preset.labelAr else preset.labelEn,
                                                selected = filter.datePreset == preset,
                                                onClick = {
                                                    onUpdateFilter { state ->
                                                        state.copy(
                                                            datePreset = preset,
                                                            dateOp = if (preset == DatePreset.CUSTOM && state.dateOp == ComparisonOperator.NONE) {
                                                                ComparisonOperator.BETWEEN
                                                            } else state.dateOp,
                                                            dateValue1 = if (preset == DatePreset.CUSTOM && state.dateValue1.isBlank()) {
                                                                currentIsoDate()
                                                            } else state.dateValue1
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    // Custom Date Operator & Pickers
                                    if (filter.datePreset == DatePreset.CUSTOM) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            ComparisonOperator.entries.filter { it != ComparisonOperator.NONE }.forEach { op ->
                                                SelectableFilterPill(
                                                    label = if (op == ComparisonOperator.BETWEEN) tr(isArabic, "بين تاريخين", "Between") else op.symbol,
                                                    selected = filter.dateOp == op,
                                                    onClick = { onUpdateFilter { it.copy(dateOp = op) } }
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { activeDatePickerSlot = 1 },
                                                shape = RoundedCornerShape(10.dp),
                                                color = ext.cardBackground
                                            ) {
                                                Text(
                                                    text = filter.dateValue1.ifBlank { tr(isArabic, "اختر التاريخ 1", "Pick Date 1") },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    modifier = Modifier.padding(12.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            if (filter.dateOp == ComparisonOperator.BETWEEN) {
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable { activeDatePickerSlot = 2 },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = ext.cardBackground
                                                ) {
                                                    Text(
                                                        text = filter.dateValue2.ifBlank { tr(isArabic, "اختر التاريخ 2", "Pick Date 2") },
                                                        style = MaterialTheme.typography.labelMedium,
                                                        modifier = Modifier.padding(12.dp),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Time Filter Condition (PDF Section 8.1: Time inside Date filter!)
                                    Text(
                                        text = tr(isArabic, "شرط الوقت (اختياري):", "Time Condition (optional):"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ComparisonOperator.entries.forEach { op ->
                                            SelectableFilterPill(
                                                label = when (op) {
                                                    ComparisonOperator.NONE -> tr(isArabic, "بدون", "None")
                                                    ComparisonOperator.BETWEEN -> tr(isArabic, "بين وقتين", "Between")
                                                    else -> op.symbol
                                                },
                                                selected = filter.timeOp == op,
                                                onClick = {
                                                    onUpdateFilter { state ->
                                                        if (op == ComparisonOperator.NONE) {
                                                            state.copy(timeOp = op, timeValue1 = "", timeValue2 = "")
                                                        } else {
                                                            state.copy(
                                                                timeOp = op,
                                                                timeValue1 = state.timeValue1.ifBlank { "09:00" },
                                                                timeValue2 = if (op == ComparisonOperator.BETWEEN) state.timeValue2.ifBlank { "21:00" } else state.timeValue2
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    if (filter.timeOp != ComparisonOperator.NONE) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { activeTimePickerSlot = 1 },
                                                shape = RoundedCornerShape(10.dp),
                                                color = ext.cardBackground
                                            ) {
                                                Text(
                                                    text = formatTime12Hour(filter.timeValue1.ifBlank { "09:00" }, isArabic),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    modifier = Modifier.padding(10.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            if (filter.timeOp == ComparisonOperator.BETWEEN) {
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable { activeTimePickerSlot = 2 },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = ext.cardBackground
                                                ) {
                                                    Text(
                                                        text = formatTime12Hour(filter.timeValue2.ifBlank { "21:00" }, isArabic),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        modifier = Modifier.padding(10.dp),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Saved Filters Strip (if any saved filters exist)
                    if (savedFilters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tr(isArabic, "محفوظة:", "Saved:"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ext.incomeColor
                            )
                            savedFilters.forEach { sf ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (sf.isDefault) ext.incomeContainer else ext.cardSubtleBackground
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clickable { onApplySavedFilter(sf) }
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = sf.name,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (sf.isDefault) ext.incomeColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = tr(isArabic, "حذف الفلتر", "Delete Filter"),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { onDeleteSavedFilter(sf.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Filter Summary Strip ("المطبق:") + Reset button
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tr(isArabic, "المطبق:", "Active:"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        activeFilterChips.forEach { chip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ext.cardContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = chip,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .testTag("reports_reset_filters_btn")
                                .clickable {
                                    amountInput1 = ""
                                    amountInput2 = ""
                                    onResetFilter()
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = ext.expenseColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = tr(isArabic, "إعادة ضبط", "Reset"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ext.expenseColor
                            )
                        }
                    }
                }
            }

            // 2. Interactive Export & Direct Download Bar (Formatted PDF / Excel / CSV / PNG!)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportDownloadButton(
                        transactions = analytics.filteredTransactions,
                        obligations = obligations,
                        preferredCurrency = summary.currency.ifBlank { settings.defaultCurrency },
                        reportTitle = tr(isArabic, "تقرير مصروفي المفلتر", "Masrofy Filtered Report"),
                        filterSummary = filterSummaryText,
                        isArabic = isArabic,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "reports_universal_export_btn"
                    )
                    ReportsExportToolbarCard(
                        isArabic = isArabic,
                        onExportPdf = { onExportPdf(filterSummaryText) },
                        onExportImage = { onExportImage(filterSummaryText) },
                        onExportExcel = { onExportExcel(analytics.filteredTransactions) }
                    )
                }
            }

            // 3. Matching Transactions List Card (Dashboard moved to dedicated top tab)
            item {
                SereneCard(contentPadding = PaddingValues(16.dp)) {
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
                                Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = tr(isArabic, "العمليات المطابقة للفلتر", "Matching Transactions"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(ext.cardContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tr(
                                    isArabic,
                                    "${analytics.filteredTransactions.size} عملية",
                                    "${analytics.filteredTransactions.size} items"
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (analytics.filteredTransactions.isEmpty()) {
                        Text(
                            text = tr(
                                isArabic,
                                "لا توجد عمليات مطابقة لشروط الفلتر الحالية.",
                                "No transactions match the current filter criteria."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        )
                    } else {
                        val preview = analytics.filteredTransactions.take(10)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            preview.forEach { tx ->
                                TransactionRowItem(
                                    tx = tx,
                                    categories = categories,
                                    isArabic = isArabic,
                                    showQuickDelete = settings.quickDeleteMode,
                                    onToggleDebtSettled = if (tx.isDebt) {
                                        { settled -> onToggleDebtSettled(tx.id, settled) }
                                    } else null,
                                    onDeleteClick = { pendingDeleteTx = tx }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(
                                    isArabic,
                                    "عرض ${preview.size} من أصل ${analytics.filteredTransactions.size} نتيجة",
                                    "Showing ${preview.size} of ${analytics.filteredTransactions.size}"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onViewFullLog)
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tr(isArabic, "عرض السجل كاملاً", "View Full Log"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ext.incomeColor
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
                    }
                }

                ReportsTopTab.DASHBOARD -> {
                    LazyColumn(
                        state = dashboardListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Live Filter Sync Bar (Synced with Reports Filters + Quick Controls)
                        item {
                            DashboardFilterSyncCard(
                                filter = filter,
                                activeFilterChips = activeFilterChips,
                                matchingCount = analytics.filteredTransactions.size,
                                isArabic = isArabic,
                                onUpdateFilter = onUpdateFilter,
                                onResetFilter = {
                                    amountInput1 = ""
                                    amountInput2 = ""
                                    onResetFilter()
                                },
                                onOpenFullFilters = { activeTopTab = ReportsTopTab.REPORTS }
                            )
                        }

                        // 1b. Quick Access to Dedicated Detail Pages from Dashboard (contextually filtered)
                        val dashboardConfig = settings.analyticsDashboardConfig()
                        if (dashboardConfig.showQuickDetailsBoxes) {
                            item {
                                val actualPaidCount = summary.paidExpenseCount + summary.settledDebtCount + summary.paidScheduledObligationsCount
                                val upcomingDueCount = summary.unpaidDebtCount + summary.pendingScheduledObligationsCount
                                ReportsQuickDetailsStrip(
                                    totalIncomeMinor = summary.actualCashInMinor,
                                    totalExpenseMinor = summary.totalExpenseMinor,
                                    totalUnpaidDebtMinor = summary.totalPendingObligationsMinor,
                                    unpaidDebtCount = upcomingDueCount,
                                    totalPaidMinor = summary.actualCashOutMinor,
                                    paidCount = actualPaidCount,
                                    currencySymbol = sym,
                                    isArabic = isArabic,
                                    filter = filter,
                                    contextualOnly = dashboardConfig.contextualFilterCardsOnly,
                                    onOpenIncomeDetails = onOpenIncomeDetails,
                                    onOpenExpenseDetails = onOpenExpenseDetails,
                                    onOpenDebts = onOpenDebts,
                                    onOpenPayments = onOpenPayments,
                                    onOpenMoneyFlowGuide = { activeTopTab = ReportsTopTab.PLAN }
                                )
                            }
                        }

                        // 2. Full Interactive & Responsive Data Analytics Dashboard
                        item {
                            ReportsAnalyticsDashboard(
                                analytics = analytics,
                                filter = filter,
                                isArabic = isArabic,
                                showCharts = true,
                                dashboardConfig = dashboardConfig,
                                onToggleCategoryFilter = { category ->
                                    onUpdateFilter { state ->
                                        val next = if (category in state.categories) {
                                            state.categories - category
                                        } else {
                                            state.categories + category
                                        }
                                        state.copy(categories = next)
                                    }
                                },
                                onToggleSourceFilter = { source ->
                                    onUpdateFilter { state ->
                                        val next = if (source in state.sources) {
                                            state.sources - source
                                        } else {
                                            state.sources + source
                                        }
                                        state.copy(sources = next)
                                    }
                                },
                                onSelectDebtFilter = { debtFilterKey ->
                                    onUpdateFilter { state -> state.copy(debtFilter = debtFilterKey) }
                                }
                            )
                        }

                        // 3. Export Filtered Dashboard Results
                        item {
                            ReportsExportToolbarCard(
                                isArabic = isArabic,
                                onExportPdf = { onExportPdf(filterSummaryText) },
                                onExportImage = { onExportImage(filterSummaryText) },
                                onExportExcel = { onExportExcel(analytics.filteredTransactions) }
                            )
                        }
                    }
                }

                ReportsTopTab.PLAN -> {
                    MoneyFlowGuideScreen(
                        transactions = allTransactions,
                        obligations = obligations,
                        settings = settings,
                        isArabic = isArabic,
                        onOpenLifetimeMoney = onOpenLifetimeMoney,
                        onOpenExpenseDetails = onOpenExpenseDetails,
                        onOpenObligations = onOpenObligations,
                        onSetObligationPaid = onSetObligationPaid,
                        onToggleDebtSettled = onToggleDebtSettled,
                        onOpenDrillDown = onOpenDrillDown
                    )
                }
            }
        }
    }

    // Save Named Filter Dialog
    if (showSaveFilterDialog) {
        AlertDialog(
            onDismissRequest = { showSaveFilterDialog = false },
            title = { Text(tr(isArabic, "حفظ تركيبة الفلاتر", "Save Filter Preset")) },
            text = {
                OutlinedTextField(
                    value = saveFilterName,
                    onValueChange = { saveFilterName = it },
                    singleLine = true,
                    placeholder = {
                        Text(tr(isArabic, "مثال: مصاريف الشهر فوق 1000", "e.g. Monthly > 1000"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveFilter(saveFilterName, false)
                        showSaveFilterDialog = false
                    }
                ) {
                    Text(tr(isArabic, "حفظ", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveFilterDialog = false }) {
                    Text(tr(isArabic, "إلغاء", "Cancel"))
                }
            }
        )
    }

    // Custom Date Picker Dialog
    activeDatePickerSlot?.let { slot ->
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { activeDatePickerSlot = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ms = pickerState.selectedDateMillis
                        if (ms != null) {
                            val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))
                            onUpdateFilter { state ->
                                if (slot == 1) state.copy(dateValue1 = iso)
                                else state.copy(dateValue2 = iso)
                            }
                        }
                        activeDatePickerSlot = null
                    }
                ) {
                    Text(tr(isArabic, "تأكيد", "OK"))
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDatePickerSlot = null }) {
                    Text(tr(isArabic, "إلغاء", "Cancel"))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    // Zero-lag FastTimePickerDialog for Time Filter
    activeTimePickerSlot?.let { slot ->
        FastTimePickerDialog(
            initialTime24 = if (slot == 1) filter.timeValue1.ifBlank { "09:00" } else filter.timeValue2.ifBlank { "21:00" },
            isArabic = isArabic,
            title = if (slot == 1) tr(isArabic, "اختر وقت البداية", "Select Start Time") else tr(isArabic, "اختر وقت النهاية", "Select End Time"),
            onConfirm = { picked ->
                onUpdateFilter { state ->
                    if (slot == 1) state.copy(timeValue1 = picked)
                    else state.copy(timeValue2 = picked)
                }
                activeTimePickerSlot = null
            },
            onDismiss = { activeTimePickerSlot = null }
        )
    }

    pendingDeleteTx?.let { tx ->
        SecurityVerificationDialog(
            title = tr(isArabic, "حذف العملية", "Delete Transaction"),
            description = tr(
                isArabic,
                "هل أنت متأكد من حذف \"${tx.itemName.ifBlank { tx.category }}\" من التقرير والسجل؟",
                "Delete \"${tx.itemName.ifBlank { tx.category }}\" from reports and ledger?"
            ),
            confirmLabel = tr(isArabic, "حذف نهائي", "Delete"),
            isDanger = true,
            requireSecurity = settings.hasAnyLockMethodConfigured(),
            settings = settings,
            isArabic = isArabic,
            onConfirmed = {
                onDeleteTransaction(tx.id)
                pendingDeleteTx = null
            },
            onDismiss = { pendingDeleteTx = null }
        )
    }
}

@Composable
private fun FilterDockIconTab(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    hasActive: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) ext.heroCardBackground else ext.cardSubtleBackground
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasActive && !selected) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(ext.incomeColor)
                )
            }
        }
    }
}

@Composable
private fun TypeFilterOptionButton(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    iconTint: Color?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) ext.heroCardBackground else ext.cardBackground
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ext.heroCardOnBackground else (iconTint ?: MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SelectableFilterPill(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) ext.heroCardBackground else ext.cardBackground,
        border = if (selected) null else BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ExportActionPill(
    label: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = ext.cardContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(17.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ReportsTopTabBar(
    activeTab: ReportsTopTab,
    matchingCount: Int,
    hasActiveFilters: Boolean,
    isArabic: Boolean,
    onSelectTab: (ReportsTopTab) -> Unit
) {
    val ext = LocalSereneColors.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 420.dp
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = ext.cardBackground,
            border = BorderStroke(1.dp, ext.cardBorder),
            shadowElevation = if (ext.isDark) 0.dp else 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReportsTopTabButton(
                    modifier = Modifier.weight(1f),
                    label = tr(isArabic, "التقارير", "Reports"),
                    icon = Icons.Outlined.Tune,
                    badgeText = if (compact) null else "$matchingCount",
                    showLiveDot = false,
                    selected = activeTab == ReportsTopTab.REPORTS,
                    compact = compact,
                    tag = "reports_top_tab_filters",
                    onClick = { onSelectTab(ReportsTopTab.REPORTS) }
                )
                ReportsTopTabButton(
                    modifier = Modifier.weight(1f),
                    label = tr(isArabic, "التحليل", "Analytics"),
                    icon = Icons.Outlined.Insights,
                    badgeText = null,
                    showLiveDot = hasActiveFilters,
                    selected = activeTab == ReportsTopTab.DASHBOARD,
                    compact = compact,
                    tag = "reports_top_tab_dashboard",
                    onClick = { onSelectTab(ReportsTopTab.DASHBOARD) }
                )
                ReportsTopTabButton(
                    modifier = Modifier.weight(1f),
                    label = tr(isArabic, "المخطط", "Plan"),
                    icon = Icons.Outlined.AccountTree,
                    badgeText = null,
                    showLiveDot = false,
                    selected = activeTab == ReportsTopTab.PLAN,
                    compact = compact,
                    tag = "reports_top_tab_plan",
                    onClick = { onSelectTab(ReportsTopTab.PLAN) }
                )
            }
        }
    }
}

@Composable
private fun ReportsTopTabButton(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    badgeText: String?,
    showLiveDot: Boolean,
    selected: Boolean,
    compact: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    val bgColor by animateColorAsState(
        targetValue = if (selected) ext.heroCardBackground else Color.Transparent,
        label = "${tag}_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "${tag}_fg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF82F5C1) else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "${tag}_icon"
    )

    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 4.dp else 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(if (compact) 15.dp else 17.dp)
            )
            Spacer(modifier = Modifier.width(if (compact) 4.dp else 6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(if (compact) 4.dp else 6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (selected) Color.White.copy(alpha = 0.16f)
                            else ext.cardContainer
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) Color(0xFF82F5C1) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            if (showLiveDot) {
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color(0xFF82F5C1) else ext.incomeColor)
                )
            }
        }
    }
}

@Composable
private fun ReportsQuickDetailsStrip(
    totalIncomeMinor: Long,
    totalExpenseMinor: Long,
    totalUnpaidDebtMinor: Long,
    unpaidDebtCount: Int,
    totalPaidMinor: Long,
    paidCount: Int,
    currencySymbol: String,
    isArabic: Boolean,
    filter: ReportFilterState = ReportFilterState(),
    contextualOnly: Boolean = true,
    onOpenIncomeDetails: () -> Unit,
    onOpenExpenseDetails: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenMoneyFlowGuide: () -> Unit
) {
    val ext = LocalSereneColors.current
    val isIncomeFocused = contextualOnly && (
        filter.type == TransactionType.INCOME ||
            filter.debtFilter == DebtFilter.DEBT_INCOME ||
            filter.debtFilter == DebtFilter.PAYMENTS_INCOME
        )
    val isExpenseFocused = contextualOnly && (
        filter.type == TransactionType.EXPENSE ||
            filter.debtFilter == DebtFilter.DEBT_EXPENSE ||
            filter.debtFilter == DebtFilter.PAYMENTS_EXPENSE
        )
    val isUpcomingFocused = contextualOnly && (
        filter.debtFilter == DebtFilter.DEBT_UNPAID ||
            filter.debtFilter == DebtFilter.DEBT_INCOME ||
            filter.debtFilter == DebtFilter.DEBT_EXPENSE
        )
    val isSettledFocused = contextualOnly && (
        filter.debtFilter == DebtFilter.DEBT_SETTLED ||
            filter.debtFilter == DebtFilter.PAYMENTS_INCOME ||
            filter.debtFilter == DebtFilter.PAYMENTS_EXPENSE
        )
    val showIncomeTile = !isExpenseFocused && !isUpcomingFocused && !isSettledFocused || isIncomeFocused
    val showExpenseTile = !isIncomeFocused && !isUpcomingFocused && !isSettledFocused || isExpenseFocused
    val showUpcomingTile = !isSettledFocused && !(contextualOnly && filter.debtFilter == DebtFilter.NON_DEBT)
    val showPaidTile = !isUpcomingFocused

    SereneCard(contentPadding = PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr(isArabic, "صفحات التفاصيل السريعة", "Quick Detail Pages"),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (contextualOnly && (isIncomeFocused || isExpenseFocused || isUpcomingFocused || isSettledFocused)) {
                    tr(isArabic, "مرتبطة بالفلتر المختار", "Filtered view")
                } else {
                    tr(isArabic, "اضغط للعرض الكامل", "Tap for full view")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Featured Icon Card: «شرح تفصيلي بفلوس راحت فين وجات إزاي»
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .testTag("reports_open_money_flow_guide")
                .clickable(onClick = onOpenMoneyFlowGuide),
            shape = RoundedCornerShape(14.dp),
            color = ext.heroCardBackground,
            border = BorderStroke(1.dp, Color(0xFF82F5C1).copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(ext.incomeColor.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountTree,
                            contentDescription = tr(
                                isArabic,
                                "شرح تفصيلي بفلوس راحت فين وجات إزاي",
                                "Detailed Money Flow Explanation"
                            ),
                            tint = Color(0xFF82F5C1),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(
                                isArabic,
                                "شرح تفصيلي بفلوس راحت فين وجات إزاي",
                                "Detailed Guide: Where Money Went & Came From"
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = ext.heroCardOnBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tr(
                                isArabic,
                                "يشمل «الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج» و«فلوسك بعد حساب كل المبالغ المطلوب دفعها»",
                                "Includes Lifetime Money Added & Money After All Due Payments"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.heroCardSubtext,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF82F5C1),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showIncomeTile || showExpenseTile) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showIncomeTile) {
                        ReportsQuickDetailTile(
                            modifier = Modifier.weight(1f),
                            title = tr(isArabic, "تفاصيل الدخل", "Income"),
                            amount = "${formatAmountMinor(totalIncomeMinor, false)} $currencySymbol",
                            badge = null,
                            icon = Icons.Outlined.TrendingUp,
                            accentColor = ext.incomeColor,
                            accentBg = ext.incomeContainer,
                            tag = "reports_open_income_details",
                            onClick = onOpenIncomeDetails
                        )
                    }
                    if (showExpenseTile) {
                        ReportsQuickDetailTile(
                            modifier = Modifier.weight(1f),
                            title = tr(isArabic, "تفاصيل المصاريف", "Expenses"),
                            amount = "${formatAmountMinor(totalExpenseMinor, false)} $currencySymbol",
                            badge = null,
                            icon = Icons.Outlined.TrendingDown,
                            accentColor = ext.expenseColor,
                            accentBg = ext.expenseContainer,
                            tag = "reports_open_expense_details",
                            onClick = onOpenExpenseDetails
                        )
                    }
                }
            }
            if (showUpcomingTile || showPaidTile) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showUpcomingTile) {
                        ReportsQuickDetailTile(
                            modifier = Modifier.weight(1f),
                            title = tr(isArabic, "كل المطلوب دفعه قريباً", "Upcoming Due"),
                            amount = "${formatAmountMinor(totalUnpaidDebtMinor, false)} $currencySymbol",
                            badge = tr(isArabic, "$unpaidDebtCount لسه هيندفع", "$unpaidDebtCount due"),
                            icon = Icons.Outlined.RequestQuote,
                            accentColor = ext.warningAmber,
                            accentBg = ext.warningAmber.copy(alpha = 0.16f),
                            tag = "reports_open_debts_details",
                            onClick = onOpenDebts
                        )
                    }
                    if (showPaidTile) {
                        ReportsQuickDetailTile(
                            modifier = Modifier.weight(1f),
                            title = tr(isArabic, "كل اللي اندفع بالفعل", "Actually Paid"),
                            amount = "${formatAmountMinor(totalPaidMinor, false)} $currencySymbol",
                            badge = tr(isArabic, "$paidCount مدفوعة", "$paidCount paid"),
                            icon = Icons.Outlined.Payments,
                            accentColor = ext.incomeColor,
                            accentBg = ext.incomeContainer,
                            tag = "reports_open_payments_details",
                            onClick = onOpenPayments
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsQuickDetailTile(
    modifier: Modifier,
    title: String,
    amount: String,
    badge: String?,
    icon: ImageVector,
    accentColor: Color,
    accentBg: Color,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = modifier
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = ext.cardSubtleBackground,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(accentBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor,
                            maxLines = 1
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = amount,
                style = AmountBodyStyle.copy(
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardFilterSyncCard(
    filter: ReportFilterState,
    activeFilterChips: List<String>,
    matchingCount: Int,
    isArabic: Boolean,
    onUpdateFilter: ((ReportFilterState) -> ReportFilterState) -> Unit,
    onResetFilter: () -> Unit,
    onOpenFullFilters: () -> Unit
) {
    val ext = LocalSereneColors.current
    SereneCard(contentPadding = PaddingValues(14.dp)) {
        // Header row: Dashboard title + sync status + "Customize Filters" shortcut
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ext.incomeContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Insights,
                        contentDescription = null,
                        tint = ext.incomeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = tr(isArabic, "داشبورد تحليل البيانات", "Data Analytics Dashboard"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tr(
                            isArabic,
                            "متزامن مع الفلاتر • $matchingCount عملية مطابقة",
                            "Synced with filters • $matchingCount matching items"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.incomeColor
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("dashboard_open_filters_btn")
                    .clickable(onClick = onOpenFullFilters),
                shape = RoundedCornerShape(10.dp),
                color = ext.heroCardBackground
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = Color(0xFF82F5C1),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = tr(isArabic, "الفلاتر", "Filters"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ext.heroCardOnBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Type & Debt Filter Row directly synced with ReportFilterState
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SelectableFilterPill(
                label = tr(isArabic, "الكل", "All"),
                selected = filter.type == "all" && filter.debtFilter == DebtFilter.ALL,
                onClick = { onUpdateFilter { it.copy(type = "all", debtFilter = DebtFilter.ALL) } }
            )
            SelectableFilterPill(
                label = tr(isArabic, "مصروفات", "Expenses"),
                selected = filter.type == TransactionType.EXPENSE && filter.debtFilter == DebtFilter.ALL,
                onClick = { onUpdateFilter { it.copy(type = TransactionType.EXPENSE, debtFilter = DebtFilter.ALL) } }
            )
            SelectableFilterPill(
                label = tr(isArabic, "مدخولات", "Income"),
                selected = filter.type == TransactionType.INCOME && filter.debtFilter == DebtFilter.ALL,
                onClick = { onUpdateFilter { it.copy(type = TransactionType.INCOME, debtFilter = DebtFilter.ALL) } }
            )
            SelectableFilterPill(
                label = tr(isArabic, "مديونيات قائمة", "Unpaid Debts"),
                selected = filter.debtFilter == DebtFilter.DEBT_UNPAID,
                onClick = { onUpdateFilter { it.copy(type = "all", debtFilter = DebtFilter.DEBT_UNPAID) } }
            )
            SelectableFilterPill(
                label = tr(isArabic, "مديونية إيرادات", "Borrowed Income"),
                selected = filter.debtFilter == DebtFilter.DEBT_INCOME,
                onClick = { onUpdateFilter { it.copy(type = "all", debtFilter = DebtFilter.DEBT_INCOME) } }
            )
            SelectableFilterPill(
                label = tr(isArabic, "مديونية مصروفات", "Expense Debts"),
                selected = filter.debtFilter == DebtFilter.DEBT_EXPENSE,
                onClick = { onUpdateFilter { it.copy(type = "all", debtFilter = DebtFilter.DEBT_EXPENSE) } }
            )
            SelectableFilterPill(
                label = tr(isArabic, "المدفوعات (الكل)", "Payments (All)"),
                selected = filter.debtFilter == DebtFilter.DEBT_SETTLED,
                onClick = { onUpdateFilter { it.copy(type = "all", debtFilter = DebtFilter.DEBT_SETTLED) } }
            )
            SelectableFilterPill(
                label = tr(isArabic, "مدفوعات الإيرادات", "Paid Income"),
                selected = filter.debtFilter == DebtFilter.PAYMENTS_INCOME,
                onClick = { onUpdateFilter { it.copy(type = "all", debtFilter = DebtFilter.PAYMENTS_INCOME) } }
            )
            SelectableFilterPill(
                label = tr(isArabic, "مدفوعات المصروفات", "Paid Expenses"),
                selected = filter.debtFilter == DebtFilter.PAYMENTS_EXPENSE,
                onClick = { onUpdateFilter { it.copy(type = "all", debtFilter = DebtFilter.PAYMENTS_EXPENSE) } }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Date Range Strip directly synced with ReportFilterState
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DatePreset.entries.filter { it != DatePreset.CUSTOM }.forEach { preset ->
                SelectableFilterPill(
                    label = if (isArabic) preset.labelAr else preset.labelEn,
                    selected = filter.datePreset == preset,
                    onClick = { onUpdateFilter { it.copy(datePreset = preset) } }
                )
            }
        }

        // Active Filter Summary Strip + Reset
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tr(isArabic, "المطبق:", "Active:"),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            activeFilterChips.forEach { chip ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ext.cardContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (filter.hasAnyActiveFilter()) {
                Spacer(modifier = Modifier.width(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .testTag("dashboard_reset_filters_btn")
                        .clickable(onClick = onResetFilter)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = ext.expenseColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = tr(isArabic, "إعادة ضبط", "Reset"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ext.expenseColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportsExportToolbarCard(
    isArabic: Boolean,
    onExportPdf: () -> Unit,
    onExportImage: () -> Unit,
    onExportExcel: () -> Unit
) {
    val ext = LocalSereneColors.current
    SereneCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.IosShare,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = tr(isArabic, "تصدير النتائج:", "Export:"),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ExportActionPill(
                    label = "PDF",
                    icon = Icons.Outlined.PictureAsPdf,
                    tint = ext.expenseColor,
                    tag = "export_pdf_btn",
                    onClick = onExportPdf
                )
                ExportActionPill(
                    label = tr(isArabic, "صورة", "Image"),
                    icon = Icons.Outlined.Image,
                    tint = ext.incomeColor,
                    tag = "export_image_btn",
                    onClick = onExportImage
                )
                ExportActionPill(
                    label = "Excel",
                    icon = Icons.Outlined.TableView,
                    tint = ext.incomeColor,
                    tag = "export_excel_btn",
                    onClick = onExportExcel
                )
            }
        }
    }
}
