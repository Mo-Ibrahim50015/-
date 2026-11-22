package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AppSettingsEntity
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.CollapsibleFilterDropdownCard
import com.example.ui.components.DropdownFilterOption
import com.example.ui.components.ExportDownloadButton
import com.example.ui.components.SecurityVerificationDialog
import com.example.ui.components.SereneCard
import com.example.ui.components.SmartSingleSelectDropdown
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.AmountHeadlineStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.formatSignedAmount
import com.example.util.tr

private sealed class PendingLogDelete {
    data class Single(val tx: TransactionEntity) : PendingLogDelete()
    data class Batch(val ids: List<Long>) : PendingLogDelete()
    data object Week : PendingLogDelete()
    data object Month : PendingLogDelete()
    data class ByCategory(val category: String) : PendingLogDelete()
    data object WipeAll : PendingLogDelete()
}

@Composable
fun LogScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onAddClick: () -> Unit,
    onDeleteSingle: (Long) -> Unit,
    onDeleteBatch: (List<Long>) -> Unit,
    onDeleteWeek: () -> Unit,
    onDeleteMonth: () -> Unit,
    onDeleteByCategory: (String) -> Unit,
    onWipeAll: () -> Unit,
    onExportExcel: (List<TransactionEntity>) -> Unit,
    onExportCsv: (List<TransactionEntity>) -> Unit,
    onToggleDebtSettled: (Long, Boolean) -> Unit
) {
    val ext = LocalSereneColors.current
    var searchQuery by remember { mutableStateOf("") }
    var activeTypeTab by remember { mutableStateOf("all") } // all, expense, income, debt, debt_income, debt_expense
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var pendingDelete by remember { mutableStateOf<PendingLogDelete?>(null) }
    var showCategoryPurgePicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val availableCategories = remember(transactions) {
        transactions.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredList = remember(transactions, searchQuery, activeTypeTab, selectedCategory) {
        val q = searchQuery.trim()
        transactions.filter { tx ->
            val typeOk = when (activeTypeTab) {
                "all" -> true
                "debt" -> tx.isDebt
                "debt_income" -> tx.isDebt && tx.type == TransactionType.INCOME
                "debt_expense" -> tx.isDebt && tx.type == TransactionType.EXPENSE
                else -> tx.type == activeTypeTab
            }
            val catOk = selectedCategory == null || tx.category == selectedCategory
            val searchOk = q.isEmpty() ||
                tx.category.contains(q, ignoreCase = true) ||
                tx.source.contains(q, ignoreCase = true) ||
                tx.note.contains(q, ignoreCase = true) ||
                tx.itemName.contains(q, ignoreCase = true) ||
                tx.debtParty.contains(q, ignoreCase = true) ||
                formatAmountMinor(tx.amountMinor).contains(q, ignoreCase = true)
            typeOk && catOk && searchOk
        }
    }
    val activeFilterCount = listOf(
        searchQuery.isNotBlank(),
        activeTypeTab != "all",
        selectedCategory != null
    ).count { it }

    val activeCurrency = settings.defaultCurrency
    val sameCurrencyTx = remember(filteredList, activeCurrency) {
        filteredList.filter { it.currency == activeCurrency }
    }
    val netMinor = remember(sameCurrencyTx) {
        sameCurrencyTx.sumOf { if (it.type == TransactionType.INCOME) it.amountMinor else -it.amountMinor }
    }
    val unpaidIncomeDebtMinor = remember(sameCurrencyTx) {
        sameCurrencyTx.filter { it.isDebt && !it.isDebtSettled && it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    }
    val unpaidExpenseDebtMinor = remember(sameCurrencyTx) {
        sameCurrencyTx.filter { it.isDebt && !it.isDebtSettled && it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    }
    val totalUnpaidDebtMinor = unpaidIncomeDebtMinor + unpaidExpenseDebtMinor

    val allSelected = filteredList.isNotEmpty() && filteredList.all { it.id in selectedIds }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Summary Balance & Counter Hub Card
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
                                imageVector = Icons.Outlined.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = tr(isArabic, "إجمالي العمليات المسجلة", "Total Recorded Transactions"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(ext.cardSubtleBackground)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tr(isArabic, "${filteredList.size} عملية", "${filteredList.size} entries"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = tr(isArabic, "صافي حركات السجل", "Net Ledger Flow"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatSignedAmount(netMinor, activeCurrency, isArabic),
                        style = AmountHeadlineStyle,
                        color = when {
                            netMinor > 0 -> ext.incomeColor
                            netMinor < 0 -> ext.expenseColor
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )

                    if (totalUnpaidDebtMinor > 0L) {
                        val sym = currencySymbol(activeCurrency, isArabic)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = ext.warningAmber.copy(alpha = 0.12f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = tr(
                                        isArabic,
                                        "مديونيات قائمة مستحقة السداد: ${formatAmountMinor(totalUnpaidDebtMinor, false)} $sym",
                                        "Active Debts To Repay: ${formatAmountMinor(totalUnpaidDebtMinor, false)} $sym"
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ext.expenseColor
                                )
                                Text(
                                    text = tr(
                                        isArabic,
                                        "إيرادات دخلت لحسابي: ${formatAmountMinor(unpaidIncomeDebtMinor, false)} $sym • مصروفات: ${formatAmountMinor(unpaidExpenseDebtMinor, false)} $sym",
                                        "Income Debt: ${formatAmountMinor(unpaidIncomeDebtMinor, false)} $sym • Expense Debt: ${formatAmountMinor(unpaidExpenseDebtMinor, false)} $sym"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    ExportDownloadButton(
                        transactions = filteredList,
                        preferredCurrency = activeCurrency,
                        reportTitle = tr(isArabic, "سجل العمليات المالية", "Financial Transactions Log"),
                        filterSummary = tr(
                            isArabic,
                            "${filteredList.size} عملية مطابقة للفلاتر",
                            "${filteredList.size} matching transactions"
                        ),
                        isArabic = isArabic,
                        compactLabel = true,
                        testTag = "log_export_btn",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    )
                }
            }

            // 2. Compact Collapsible Dropdown Filters (Search + Type + Category)
            item {
                val typeOptions = remember(transactions, isArabic) {
                    val allDebtsCount = transactions.count { it.isDebt && !it.isDebtSettled }
                    val incDebtCount = transactions.count { it.isDebt && !it.isDebtSettled && it.type == TransactionType.INCOME }
                    val expDebtCount = transactions.count { it.isDebt && !it.isDebtSettled && it.type == TransactionType.EXPENSE }
                    listOf(
                        DropdownFilterOption("all", tr(isArabic, "الكل (${transactions.size})", "All (${transactions.size})")),
                        DropdownFilterOption(TransactionType.EXPENSE, tr(isArabic, "مصاريف فقط", "Expenses Only")),
                        DropdownFilterOption(TransactionType.INCOME, tr(isArabic, "دخل فقط", "Income Only")),
                        DropdownFilterOption("debt", tr(isArabic, "كل المديونيات ($allDebtsCount)", "All Debts ($allDebtsCount)")),
                        DropdownFilterOption("debt_income", tr(isArabic, "مديونية إيرادات دخلت لحسابي ($incDebtCount)", "Income Debts ($incDebtCount)")),
                        DropdownFilterOption("debt_expense", tr(isArabic, "مديونية مصروفات ($expDebtCount)", "Expense Debts ($expDebtCount)"))
                    )
                }
                val categoryOptions = remember(availableCategories, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل التصنيفات", "All Categories"))) +
                        availableCategories.map { DropdownFilterOption<String?>(it, it) }
                }

                CollapsibleFilterDropdownCard(
                    isArabic = isArabic,
                    activeFilterCount = activeFilterCount,
                    onResetFilters = if (activeFilterCount > 0) {
                        {
                            searchQuery = ""
                            activeTypeTab = "all"
                            selectedCategory = null
                        }
                    } else null,
                    title = tr(isArabic, "بحث وفلاتر السجل", "Log Search & Filters"),
                    testTagPrefix = "log_filters"
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = tr(
                                    isArabic,
                                    "ابحث في الأسماء، المصادر، والملاحظات...",
                                    "Search categories, sources, notes, amounts..."
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Outlined.Cancel,
                                    contentDescription = tr(isArabic, "مسح البحث", "Clear Search"),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { searchQuery = "" }
                                        .padding(4.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ext.cardSubtleBackground,
                            unfocusedContainerColor = ext.cardSubtleBackground,
                            unfocusedBorderColor = ext.cardBorder.copy(alpha = 0.6f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("log_search_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "النوع", "Type"),
                            selectedValue = activeTypeTab,
                            options = typeOptions,
                            isArabic = isArabic,
                            onSelected = { activeTypeTab = it },
                            isHighlighted = activeTypeTab != "all",
                            testTag = "log_type_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                        if (availableCategories.isNotEmpty()) {
                            SmartSingleSelectDropdown(
                                label = tr(isArabic, "التصنيف", "Category"),
                                selectedValue = selectedCategory,
                                options = categoryOptions,
                                isArabic = isArabic,
                                onSelected = { selectedCategory = it },
                                isHighlighted = selectedCategory != null,
                                testTag = "log_category_dropdown",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Empty State OR Populated List with Multi-Select & Quick Delete Toolbar
            if (filteredList.isEmpty()) {
                item {
                    SereneCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 36.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.size(86.dp), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(ext.cardSubtleBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .align(Alignment.BottomStart)
                                        .clip(CircleShape)
                                        .background(ext.cardContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.EditNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (transactions.isEmpty()) {
                                    tr(isArabic, "سجل العمليات فارغ حالياً", "Transactions log is empty")
                                } else {
                                    tr(isArabic, "لا توجد عمليات مطابقة للبحث", "No matching transactions found")
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = tr(
                                    isArabic,
                                    "كل عملية تضيفها من شاشة الإدخال ستظهر هنا مباشرة وبشكل دائم مع تفاصيل التاريخ والتوقيت والمصدر.",
                                    "Every transaction you add appears here immediately with full date, time, and account details."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onAddClick,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("log_empty_add_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ext.heroCardBackground,
                                    contentColor = ext.heroCardOnBackground
                                )
                            ) {
                                Icon(Icons.Outlined.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr(isArabic, "إضافة أول عملية الآن", "Add First Transaction"),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = tr(
                                        isArabic,
                                        "بياناتك محفوظة محلياً وتخضع لخصوصيتك التامة",
                                        "Your data stays 100% private and local on your device"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            } else {
                // Bulk Management & Quick Delete Toolbar matching screen_503943cc.png
                item {
                    SereneCard(contentPadding = PaddingValues(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .testTag("log_multiselect_toggle")
                                        .clickable {
                                            multiSelectMode = !multiSelectMode
                                            if (!multiSelectMode) selectedIds = emptySet()
                                        },
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (multiSelectMode) ext.incomeContainer else ext.cardSubtleBackground
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Checklist,
                                            contentDescription = null,
                                            tint = ext.incomeColor,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = tr(isArabic, "تحديد متعدد", "Multi-select"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (multiSelectMode) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            selectedIds = if (allSelected) emptySet()
                                            else filteredList.map { it.id }.toSet()
                                        }
                                    ) {
                                        Checkbox(
                                            checked = allSelected,
                                            onCheckedChange = { checked ->
                                                selectedIds = if (checked) filteredList.map { it.id }.toSet() else emptySet()
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = ext.expenseColor),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = tr(isArabic, "تحديد الكل", "All"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }

                            if (multiSelectMode && selectedIds.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .testTag("log_delete_selected_btn")
                                        .clickable {
                                            pendingDelete = PendingLogDelete.Batch(selectedIds.toList())
                                        },
                                    shape = RoundedCornerShape(9.dp),
                                    color = ext.expenseColor
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = tr(isArabic, "حذف المحدد (${selectedIds.size})", "Delete (${selectedIds.size})"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            } else if (multiSelectMode) {
                                Text(
                                    text = tr(
                                        isArabic,
                                        "تم تحديد ${selectedIds.size} من ${filteredList.size}",
                                        "Selected ${selectedIds.size} of ${filteredList.size}"
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fast Filter Removal Chips Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tr(isArabic, "حذف سريع:", "Quick delete:"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            QuickDeleteChip(
                                label = tr(isArabic, "الأسبوع الحالي", "This Week"),
                                isDanger = false,
                                onClick = { pendingDelete = PendingLogDelete.Week }
                            )
                            QuickDeleteChip(
                                label = tr(isArabic, "الشهر الحالي", "This Month"),
                                isDanger = false,
                                onClick = { pendingDelete = PendingLogDelete.Month }
                            )
                            QuickDeleteChip(
                                label = tr(isArabic, "حسب تصنيف", "By Category"),
                                isDanger = false,
                                onClick = { showCategoryPurgePicker = true }
                            )
                            QuickDeleteChip(
                                label = tr(isArabic, "مسح السجل كاملاً", "Wipe All"),
                                isDanger = true,
                                onClick = { pendingDelete = PendingLogDelete.WipeAll }
                            )
                        }
                    }
                }

                // Virtualized LazyColumn Items
                items(items = filteredList, key = { it.id }) { tx ->
                    TransactionRowItem(
                        tx = tx,
                        categories = categories,
                        isArabic = isArabic,
                        multiSelectActive = multiSelectMode,
                        isSelected = tx.id in selectedIds,
                        showQuickDelete = settings.quickDeleteMode,
                        onToggleSelect = {
                            selectedIds = if (tx.id in selectedIds) selectedIds - tx.id else selectedIds + tx.id
                        },
                        onToggleDebtSettled = if (tx.isDebt) {
                            { settled -> onToggleDebtSettled(tx.id, settled) }
                        } else null,
                        onDeleteClick = { pendingDelete = PendingLogDelete.Single(tx) }
                    )
                }

                // Mindful Safety & Add Action Footer Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = ext.cardBackground,
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tr(
                                        isArabic,
                                        "الحذف يخضع لتأكيد مسبق لضمان سلامة حساباتك وميزانيتك",
                                        "Every deletion requires confirmation to protect your budget"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onAddClick)
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tr(isArabic, "إضافة عملية", "Add"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Format Dialog (Excel / CSV)
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(tr(isArabic, "تصدير سجل العمليات", "Export Transactions")) },
            text = {
                Text(
                    tr(
                        isArabic,
                        "اختر صيغة التصدير (${filteredList.size} عملية):",
                        "Choose export format (${filteredList.size} entries):"
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        onExportExcel(filteredList)
                    }
                ) {
                    Text("Excel (.xls)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        onExportCsv(filteredList)
                    }
                ) {
                    Text("CSV (UTF-8)")
                }
            }
        )
    }

    // Category Purge Picker Dialog
    if (showCategoryPurgePicker) {
        val grouped = remember(transactions) {
            transactions.groupBy { it.category }.mapValues { it.value.size }
        }
        AlertDialog(
            onDismissRequest = { showCategoryPurgePicker = false },
            title = { Text(tr(isArabic, "اختر التصنيف المراد مسح عملياته", "Select Category to Purge")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (grouped.isEmpty()) {
                        Text(tr(isArabic, "لا توجد تصنيفات مسجلة", "No categories found"))
                    } else {
                        grouped.forEach { (catName, count) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        showCategoryPurgePicker = false
                                        pendingDelete = PendingLogDelete.ByCategory(catName)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = ext.cardSubtleBackground
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(catName, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        tr(isArabic, "$count عملية", "$count items"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.expenseColor
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryPurgePicker = false }) {
                    Text(tr(isArabic, "إغلاق", "Close"))
                }
            }
        )
    }

    // Safety Confirmation Modal for all deletes
    pendingDelete?.let { action ->
        val (title, desc, doubleConfirm) = when (action) {
            is PendingLogDelete.Single -> Triple(
                tr(isArabic, "تأكيد حذف العملية", "Delete Transaction"),
                tr(
                    isArabic,
                    "هل أنت متأكد من حذف العملية \"${action.tx.category}\" بقيمة ${formatAmountMinor(action.tx.amountMinor)} ${currencySymbol(action.tx.currency, isArabic)}؟",
                    "Delete \"${action.tx.category}\" (${formatAmountMinor(action.tx.amountMinor)} ${action.tx.currency})?"
                ),
                false
            )
            is PendingLogDelete.Batch -> Triple(
                tr(isArabic, "تأكيد حذف العمليات المحددة", "Delete Selected"),
                tr(
                    isArabic,
                    "سيتم حذف ${action.ids.size} عمليات محددة نهائياً وتحديث إحصائيات ميزانيتك فوراً.",
                    "Permanently delete ${action.ids.size} selected transactions?"
                ),
                false
            )
            is PendingLogDelete.Week -> Triple(
                tr(isArabic, "مسح عمليات الأسبوع الحالي", "Delete Current Week"),
                tr(
                    isArabic,
                    "سيتم حذف جميع العمليات المسجلة خلال آخر 7 أيام.",
                    "Delete all transactions from the last 7 days?"
                ),
                false
            )
            is PendingLogDelete.Month -> Triple(
                tr(isArabic, "مسح عمليات الشهر الحالي", "Delete Current Month"),
                tr(
                    isArabic,
                    "سيتم حذف جميع العمليات المسجلة خلال الشهر الحالي مع الاحتفاظ بالشهور السابقة.",
                    "Delete all transactions from the current month?"
                ),
                false
            )
            is PendingLogDelete.ByCategory -> Triple(
                tr(isArabic, "مسح عمليات التصنيف", "Delete Category Entries"),
                tr(
                    isArabic,
                    "سيتم حذف جميع العمليات المرتبطة بتصنيف (${action.category}) فقط.",
                    "Delete all transactions in (${action.category})?"
                ),
                false
            )
            is PendingLogDelete.WipeAll -> Triple(
                tr(isArabic, "مسح السجل بالكامل", "Wipe Entire Ledger"),
                tr(
                    isArabic,
                    "سيتم مسح كافة العمليات المسجلة (${transactions.size} عملية) وتصفير جميع الأرصدة والإحصائيات.",
                    "Erase all ${transactions.size} transactions and reset all balances to zero?"
                ),
                true
            )
        }

        SecurityVerificationDialog(
            title = title,
            description = if (doubleConfirm) {
                desc + "\n\n" + tr(isArabic, "تحذير: لا يمكن التراجع عن هذا الإجراء!", "Warning: This action cannot be undone!")
            } else desc,
            confirmLabel = tr(isArabic, "تأكيد الحذف", "Confirm Delete"),
            isDanger = true,
            requireSecurity = settings.hasAnyLockMethodConfigured(),
            settings = settings,
            isArabic = isArabic,
            onConfirmed = {
                when (action) {
                    is PendingLogDelete.Single -> onDeleteSingle(action.tx.id)
                    is PendingLogDelete.Batch -> {
                        onDeleteBatch(action.ids)
                        selectedIds = emptySet()
                        multiSelectMode = false
                    }
                    is PendingLogDelete.Week -> onDeleteWeek()
                    is PendingLogDelete.Month -> onDeleteMonth()
                    is PendingLogDelete.ByCategory -> onDeleteByCategory(action.category)
                    is PendingLogDelete.WipeAll -> {
                        onWipeAll()
                        selectedIds = emptySet()
                        multiSelectMode = false
                    }
                }
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun LogFilterTabPill(
    label: String,
    countBadge: Int?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color?,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .height(38.dp)
            .testTag(tag)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) ext.heroCardBackground else ext.cardBackground,
        border = if (selected) null else BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ext.heroCardOnBackground else (iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (countBadge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (selected) Color.White.copy(alpha = 0.2f) else ext.cardSubtleBackground
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = countBadge.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickDeleteChip(
    label: String,
    isDanger: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isDanger) ext.expenseContainer.copy(alpha = 0.5f) else ext.cardSubtleBackground
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isDanger) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isDanger) ext.expenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
