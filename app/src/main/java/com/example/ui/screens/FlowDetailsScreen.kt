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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppSettingsEntity
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.DatePreset
import com.example.ui.components.CollapsibleFilterDropdownCard
import com.example.ui.components.DropdownFilterOption
import com.example.ui.components.ExportDownloadButton
import com.example.ui.components.SecurityVerificationDialog
import com.example.ui.components.SereneCard
import com.example.ui.components.SmartNoteFilterDropdown
import com.example.ui.components.SmartSingleSelectDropdown
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.currentIsoDate
import com.example.util.currentMonthRange
import com.example.util.currentWeekRange
import com.example.util.formatAmountMinor
import com.example.util.tr

private enum class FlowNatureFilter {
    ALL, REGULAR_ONLY, UNPAID_DEBT, SETTLED_DEBT
}

@Composable
fun FlowDetailsScreen(
    flowType: String, // TransactionType.INCOME or TransactionType.EXPENSE
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onAddClick: (type: String) -> Unit,
    onToggleDebtSettled: (Long, Boolean) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    val ext = LocalSereneColors.current
    val isIncome = flowType == TransactionType.INCOME
    val accentColor = if (isIncome) ext.incomeColor else ext.expenseColor
    val accentBg = if (isIncome) ext.incomeContainer else ext.expenseContainer
    val sym = currencySymbol(settings.defaultCurrency, isArabic)

    var searchQuery by remember(flowType) { mutableStateOf("") }
    var natureFilter by remember(flowType) { mutableStateOf(FlowNatureFilter.ALL) }
    var datePreset by remember(flowType) { mutableStateOf(DatePreset.ALL) }
    var selectedCategory by remember(flowType) { mutableStateOf<String?>(null) }
    var selectedSource by remember(flowType) { mutableStateOf<String?>(null) }
    var selectedParty by remember(flowType) { mutableStateOf<String?>(null) }
    var selectedItem by remember(flowType) { mutableStateOf<String?>(null) }
    var noteFilterQuery by remember(flowType) { mutableStateOf("") }
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val baseTypedList = remember(transactions, flowType) {
        transactions.filter { it.type == flowType }
    }

    val availableFlowSources = remember(baseTypedList) {
        baseTypedList.map { it.source.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableFlowParties = remember(baseTypedList) {
        baseTypedList.map { it.debtParty.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableFlowItems = remember(baseTypedList) {
        baseTypedList.map { it.itemName.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableFlowNotes = remember(baseTypedList) {
        baseTypedList.map { it.note.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredList = remember(
        baseTypedList,
        searchQuery,
        natureFilter,
        datePreset,
        selectedCategory,
        selectedSource,
        selectedParty,
        selectedItem,
        noteFilterQuery
    ) {
        val q = searchQuery.trim()
        val nq = noteFilterQuery.trim()
        val today = currentIsoDate()
        val (weekStart, weekEnd) = currentWeekRange()
        val (monthStart, monthEnd) = currentMonthRange()

        baseTypedList.filter { tx ->
            val natureOk = when (natureFilter) {
                FlowNatureFilter.ALL -> true
                FlowNatureFilter.REGULAR_ONLY -> !tx.isDebt
                FlowNatureFilter.UNPAID_DEBT -> tx.isDebt && !tx.isDebtSettled
                FlowNatureFilter.SETTLED_DEBT -> tx.isDebt && tx.isDebtSettled
            }
            val dateOk = when (datePreset) {
                DatePreset.ALL, DatePreset.CUSTOM -> true
                DatePreset.TODAY -> tx.date == today
                DatePreset.WEEK -> tx.date >= weekStart && tx.date <= weekEnd
                DatePreset.MONTH -> tx.date >= monthStart && tx.date <= monthEnd
            }
            val catOk = selectedCategory == null || tx.category == selectedCategory
            val srcOk = selectedSource == null || tx.source.trim() == selectedSource
            val partyOk = selectedParty == null || tx.debtParty.trim() == selectedParty
            val itemOk = selectedItem == null || tx.itemName.trim() == selectedItem
            val noteOk = nq.isEmpty() || tx.note.contains(nq, ignoreCase = true)
            val searchOk = q.isEmpty() ||
                tx.category.contains(q, ignoreCase = true) ||
                tx.source.contains(q, ignoreCase = true) ||
                tx.note.contains(q, ignoreCase = true) ||
                tx.itemName.contains(q, ignoreCase = true) ||
                tx.debtParty.contains(q, ignoreCase = true) ||
                formatAmountMinor(tx.amountMinor).contains(q, ignoreCase = true)

            natureOk && dateOk && catOk && srcOk && partyOk && itemOk && noteOk && searchOk
        }
    }

    val primaryCurrencyList = remember(filteredList, settings.defaultCurrency) {
        filteredList.filter { it.currency == settings.defaultCurrency }
    }

    val totalMinor = remember(primaryCurrencyList) {
        primaryCurrencyList.sumOf { it.amountMinor }
    }
    val regularMinor = remember(primaryCurrencyList) {
        primaryCurrencyList.filter { !it.isDebt }.sumOf { it.amountMinor }
    }
    val unpaidDebtMinor = remember(primaryCurrencyList) {
        primaryCurrencyList.filter { it.isDebt && !it.isDebtSettled }.sumOf { it.amountMinor }
    }
    val highestTx = remember(primaryCurrencyList) {
        primaryCurrencyList.maxByOrNull { it.amountMinor }
    }

    val categoryStats = remember(primaryCurrencyList, totalMinor) {
        if (primaryCurrencyList.isEmpty()) emptyList()
        else {
            val safeTotal = totalMinor.coerceAtLeast(1L).toFloat()
            primaryCurrencyList
                .groupBy { it.category.ifBlank { tr(isArabic, "غير محدد", "Unspecified") } }
                .map { (cat, list) ->
                    val sum = list.sumOf { it.amountMinor }
                    Triple(cat, sum, (sum.toFloat() / safeTotal) * 100f)
                }
                .sortedByDescending { it.second }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Hero Summary Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(if (isIncome) "income_details_hero" else "expense_details_hero"),
                    shape = RoundedCornerShape(18.dp),
                    color = ext.heroCardBackground,
                    shadowElevation = if (ext.isDark) 0.dp else 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (isIncome) Color(0xFF82F5C1) else Color(0xFFFF8A9B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (isIncome) {
                                            tr(isArabic, "تفاصيل إجمالي الدخل", "Total Income Details")
                                        } else {
                                            tr(isArabic, "تفاصيل إجمالي المصاريف", "Total Expenses Details")
                                        },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ext.heroCardOnBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tr(
                                            isArabic,
                                            "${filteredList.size} عملية مطابقة",
                                            "${filteredList.size} matching entries"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.heroCardSubtext,
                                        maxLines = 1
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("flow_detail_add_btn")
                                    .clickable { onAddClick(flowType) },
                                shape = RoundedCornerShape(10.dp),
                                color = accentColor
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isIncome) {
                                            tr(isArabic, "إضافة دخل", "Add Income")
                                        } else {
                                            tr(isArabic, "إضافة مصروف", "Add Expense")
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatAmountMinor(totalMinor),
                                style = AmountDisplayStyle,
                                color = ext.heroCardOnBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = sym,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = ext.heroCardSubtext,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Sub-metrics row: Earned/Paid vs Debt vs Highest
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DetailHeroPill(
                                modifier = Modifier.weight(1f),
                                label = if (isIncome) tr(isArabic, "فلوس ملكك", "Your Own Money") else tr(isArabic, "اندفعت خلاص", "Already Paid"),
                                value = "${formatAmountMinor(regularMinor, false)} $sym",
                                valueColor = Color(0xFF82F5C1)
                            )
                            DetailHeroPill(
                                modifier = Modifier.weight(1f),
                                label = if (isIncome) tr(isArabic, "فلوس مطلوب سدادها قريباً", "To Be Repaid Soon") else tr(isArabic, "مندفعتش لسه", "Not Paid Yet"),
                                value = "${formatAmountMinor(unpaidDebtMinor, false)} $sym",
                                valueColor = if (unpaidDebtMinor > 0L) Color(0xFFFBBF24) else ext.heroCardSubtext
                            )
                            DetailHeroPill(
                                modifier = Modifier.weight(1f),
                                label = tr(isArabic, "أعلى عملية", "Highest"),
                                value = "${formatAmountMinor(highestTx?.amountMinor ?: 0L, false)} $sym",
                                valueColor = ext.heroCardOnBackground
                            )
                        }
                    }
                }
            }

            // 2. Formatted Export & Collapsible Dropdown Filters
            item {
                val activeFilterCount = listOf(
                    searchQuery.isNotBlank(),
                    natureFilter != FlowNatureFilter.ALL,
                    datePreset != DatePreset.ALL,
                    selectedCategory != null,
                    selectedSource != null,
                    selectedParty != null,
                    selectedItem != null,
                    noteFilterQuery.isNotBlank()
                ).count { it }
                val natureOptions = remember(isIncome, isArabic) {
                    listOf(
                        DropdownFilterOption(FlowNatureFilter.ALL, tr(isArabic, "الكل", "All")),
                        DropdownFilterOption(
                            FlowNatureFilter.REGULAR_ONLY,
                            if (isIncome) tr(isArabic, "فلوس ملكك", "Your Own Money")
                            else tr(isArabic, "اندفعت خلاص", "Already Paid")
                        ),
                        DropdownFilterOption(
                            FlowNatureFilter.UNPAID_DEBT,
                            if (isIncome) tr(isArabic, "فلوس متسددتش لسه", "Not Repaid Yet")
                            else tr(isArabic, "مندفعتش لسه", "Not Paid Yet")
                        ),
                        DropdownFilterOption(
                            FlowNatureFilter.SETTLED_DEBT,
                            tr(isArabic, "فلوس اندفعت خلاص", "Already Paid")
                        )
                    )
                }
                val dateOptions = remember(isArabic) {
                    DatePreset.entries.filter { it != DatePreset.CUSTOM }.map { preset ->
                        DropdownFilterOption(preset, if (isArabic) preset.labelAr else preset.labelEn)
                    }
                }
                val availableFlowCategories = remember(baseTypedList) {
                    baseTypedList.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
                }
                val categoryOptions = remember(availableFlowCategories, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل التصنيفات", "All Categories"))) +
                        availableFlowCategories.map { DropdownFilterOption<String?>(it, it) }
                }
                val sourceOptions = remember(availableFlowSources, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل الحسابات", "All Accounts"))) +
                        availableFlowSources.map { DropdownFilterOption<String?>(it, it) }
                }
                val partyOptions = remember(availableFlowParties, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل الجهات والأشخاص", "All Parties"))) +
                        availableFlowParties.map { DropdownFilterOption<String?>(it, it) }
                }
                val itemOptions = remember(availableFlowItems, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل البنود والأسباب", "All Items"))) +
                        availableFlowItems.map { DropdownFilterOption<String?>(it, it) }
                }

                val exportFilterSummary = remember(
                    activeFilterCount,
                    natureFilter,
                    datePreset,
                    selectedCategory,
                    selectedSource,
                    selectedParty,
                    selectedItem,
                    noteFilterQuery,
                    searchQuery,
                    filteredList.size,
                    totalMinor,
                    sym,
                    isIncome,
                    isArabic
                ) {
                    if (activeFilterCount == 0) {
                        if (isIncome) {
                            tr(
                                isArabic,
                                "كل عمليات الدخل (بدون فلاتر) • ${filteredList.size} عملية • ${formatAmountMinor(totalMinor)} $sym",
                                "All income entries (no filters) • ${filteredList.size} entries • ${formatAmountMinor(totalMinor)} $sym"
                            )
                        } else {
                            tr(
                                isArabic,
                                "كل عمليات المصاريف (بدون فلاتر) • ${filteredList.size} عملية • ${formatAmountMinor(totalMinor)} $sym",
                                "All expense entries (no filters) • ${filteredList.size} entries • ${formatAmountMinor(totalMinor)} $sym"
                            )
                        }
                    } else {
                        val parts = mutableListOf<String>()
                        if (natureFilter != FlowNatureFilter.ALL) {
                            parts += natureOptions.firstOrNull { it.value == natureFilter }?.label ?: natureFilter.name
                        }
                        if (datePreset != DatePreset.ALL) {
                            parts += if (isArabic) datePreset.labelAr else datePreset.labelEn
                        }
                        selectedCategory?.let { parts += tr(isArabic, "التصنيف: $it", "Category: $it") }
                        selectedSource?.let { parts += tr(isArabic, "الحساب: $it", "Account: $it") }
                        selectedParty?.let { parts += tr(isArabic, "الجهة: $it", "Party: $it") }
                        selectedItem?.let { parts += tr(isArabic, "عشان إيه: $it", "For: $it") }
                        if (noteFilterQuery.isNotBlank()) parts += tr(isArabic, "الملاحظة: ${noteFilterQuery.trim()}", "Note: ${noteFilterQuery.trim()}")
                        if (searchQuery.isNotBlank()) parts += tr(isArabic, "بحث: ${searchQuery.trim()}", "Search: ${searchQuery.trim()}")
                        parts += tr(isArabic, "${filteredList.size} عملية مطابقة", "${filteredList.size} matching")
                        parts.joinToString(" • ")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportDownloadButton(
                        transactions = filteredList,
                        preferredCurrency = settings.defaultCurrency,
                        reportTitle = if (isIncome) {
                            tr(isArabic, "تفاصيل إجمالي الدخل", "Total Income Details")
                        } else {
                            tr(isArabic, "تفاصيل إجمالي المصاريف", "Total Expenses Details")
                        },
                        filterSummary = exportFilterSummary,
                        isArabic = isArabic,
                        testTag = "flow_details_export_btn",
                        modifier = Modifier.fillMaxWidth()
                    )

                    CollapsibleFilterDropdownCard(
                        isArabic = isArabic,
                        activeFilterCount = activeFilterCount,
                        onResetFilters = if (activeFilterCount > 0) {
                            {
                                searchQuery = ""
                                natureFilter = FlowNatureFilter.ALL
                                datePreset = DatePreset.ALL
                                selectedCategory = null
                                selectedSource = null
                                selectedParty = null
                                selectedItem = null
                                noteFilterQuery = ""
                            }
                        } else null,
                        title = tr(isArabic, "بحث وفلاتر التفاصيل", "Search & Filters"),
                        testTagPrefix = "flow_detail_filters"
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("flow_detail_search_input"),
                            placeholder = {
                                Text(
                                    text = tr(isArabic, "بحث سريع بالتصنيف، الجهة، البند، الملاحظة، أو المبلغ...", "Quick search category, party, item, note, amount..."),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = tr(isArabic, "مسح البحث", "Clear"),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { searchQuery = "" }
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ext.cardSubtleBackground,
                                unfocusedContainerColor = ext.cardSubtleBackground
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SmartSingleSelectDropdown(
                                label = tr(isArabic, "الحالة", "Nature"),
                                selectedValue = natureFilter,
                                options = natureOptions,
                                isArabic = isArabic,
                                onSelected = { natureFilter = it },
                                isHighlighted = natureFilter != FlowNatureFilter.ALL,
                                testTag = "flow_detail_nature_dropdown",
                                modifier = Modifier.weight(1f)
                            )
                            SmartSingleSelectDropdown(
                                label = tr(isArabic, "الفترة", "Period"),
                                selectedValue = datePreset,
                                options = dateOptions,
                                isArabic = isArabic,
                                onSelected = { datePreset = it },
                                isHighlighted = datePreset != DatePreset.ALL,
                                testTag = "flow_detail_date_dropdown",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SmartSingleSelectDropdown(
                                label = tr(isArabic, "التصنيف", "Category"),
                                selectedValue = selectedCategory,
                                options = categoryOptions,
                                isArabic = isArabic,
                                onSelected = { selectedCategory = it },
                                isHighlighted = selectedCategory != null,
                                testTag = "flow_detail_category_dropdown",
                                modifier = Modifier.weight(1f)
                            )
                            SmartSingleSelectDropdown(
                                label = tr(isArabic, "الحساب / المصدر", "Account / Source"),
                                selectedValue = selectedSource,
                                options = sourceOptions,
                                isArabic = isArabic,
                                onSelected = { selectedSource = it },
                                isHighlighted = selectedSource != null,
                                testTag = "flow_detail_source_dropdown",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SmartSingleSelectDropdown(
                                label = if (isIncome) tr(isArabic, "من مين / الجهة", "From / Party")
                                else tr(isArabic, "اندفعت لمين / الجهة", "Paid To / Party"),
                                selectedValue = selectedParty,
                                options = partyOptions,
                                isArabic = isArabic,
                                onSelected = { selectedParty = it },
                                isHighlighted = selectedParty != null,
                                testTag = "flow_detail_party_dropdown",
                                modifier = Modifier.weight(1f)
                            )
                            SmartSingleSelectDropdown(
                                label = tr(isArabic, "عشان إيه / البند", "What For / Item"),
                                selectedValue = selectedItem,
                                options = itemOptions,
                                isArabic = isArabic,
                                onSelected = { selectedItem = it },
                                isHighlighted = selectedItem != null,
                                testTag = "flow_detail_item_dropdown",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        SmartNoteFilterDropdown(
                            label = tr(isArabic, "الملاحظة الإضافية", "Additional Note"),
                            noteQuery = noteFilterQuery,
                            availableNotes = availableFlowNotes,
                            isArabic = isArabic,
                            onNoteQueryChange = { noteFilterQuery = it },
                            testTag = "flow_detail_note_dropdown",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. Category Breakdown Card (Clickable to filter by category)
            if (categoryStats.isNotEmpty()) {
                item {
                    SereneCard(contentPadding = PaddingValues(14.dp)) {
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
                                    imageVector = Icons.Outlined.Category,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tr(isArabic, "التوزيع حسب التصنيف", "Breakdown by Category"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (selectedCategory != null) {
                                Text(
                                    text = tr(isArabic, "عرض الكل", "Show All"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { selectedCategory = null }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categoryStats.take(6).forEach { (cat, sumMinor, pct) ->
                                val isChosen = selectedCategory == cat
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isChosen) accentBg else ext.cardSubtleBackground.copy(alpha = 0.65f))
                                        .clickable {
                                            selectedCategory = if (isChosen) null else cat
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${formatAmountMinor(sumMinor, false)} $sym (${pct.toInt()}%)",
                                            style = AmountBodyStyle.copy(
                                                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = accentColor,
                                            maxLines = 1
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(ext.cardContainer)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((pct / 100f).coerceIn(0.03f, 1f))
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(accentColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Section Header for Transactions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isIncome) {
                                tr(isArabic, "سجل عمليات الدخل", "Income Entries")
                            } else {
                                tr(isArabic, "سجل عمليات المصاريف", "Expense Entries")
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = tr(isArabic, "${filteredList.size} عملية", "${filteredList.size} items"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    SereneCard(contentPadding = PaddingValues(24.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isIncome) {
                                    tr(isArabic, "لا توجد عمليات دخل مطابقة حالياً", "No matching income transactions")
                                } else {
                                    tr(isArabic, "لا توجد عمليات مصاريف مطابقة حالياً", "No matching expense transactions")
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { onAddClick(flowType) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ext.heroCardBackground)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isIncome) tr(isArabic, "تسجيل دخل جديد", "Log New Income")
                                    else tr(isArabic, "تسجيل مصروف جديد", "Log New Expense")
                                )
                            }
                        }
                    }
                }
            } else {
                items(items = filteredList, key = { it.id }) { tx ->
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
        }
    }

    pendingDeleteTx?.let { tx ->
        SecurityVerificationDialog(
            title = tr(isArabic, "حذف العملية", "Delete Transaction"),
            description = tr(
                isArabic,
                "هل أنت متأكد من حذف \"${tx.itemName.ifBlank { tx.category }}\" نهائياً؟",
                "Delete \"${tx.itemName.ifBlank { tx.category }}\" permanently?"
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
private fun DetailHeroPill(
    modifier: Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = AmountBodyStyle.copy(
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                fontWeight = FontWeight.Bold
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
