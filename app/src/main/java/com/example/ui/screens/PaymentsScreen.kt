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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Undo
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
import com.example.data.ObligationEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.CollapsibleFilterDropdownCard
import com.example.ui.components.DropdownFilterOption
import com.example.ui.components.ExportDownloadButton
import com.example.ui.components.SecurityVerificationDialog
import com.example.ui.components.SereneCard
import com.example.ui.components.SmartSingleSelectDropdown
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.formatDisplayDateTime
import com.example.util.iconForKey
import com.example.util.tr

private enum class PaymentGroupMode { FLAT, BY_SOURCE, BY_CATEGORY }

private enum class ActualPaidKind { PAID_EXPENSE, SETTLED_DEBT, PAID_OBLIGATION }

private data class ActualPaidRecord(
    val key: String,
    val kind: ActualPaidKind,
    val title: String,
    val party: String,
    val category: String,
    val source: String,
    val currency: String,
    val amountMinor: Long,
    val date: String,
    val time: String,
    val note: String,
    val sortTimestamp: Long,
    val tx: TransactionEntity? = null,
    val obligation: ObligationEntity? = null
)

/**
 * Unified «كل المبالغ اللي اندفعت بالفعل» screen.
 *
 * Displays EVERY actually paid outflow synced 1:1 with HomeScreen, «المخطط», Reports, and Dashboard:
 * 1. Direct paid expenses (`type == EXPENSE && !isDebt`)
 * 2. Settled upcoming expenses & debts (`isDebt && isDebtSettled`)
 * 3. Paid scheduled obligations (`ob.isPaid`)
 */
@Composable
fun PaymentsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    obligations: List<ObligationEntity> = emptyList(),
    initialTab: String = "all", // "all", TransactionType.EXPENSE, "settled"
    onOpenDebts: () -> Unit,
    onReopenAsDebt: (id: Long) -> Unit,
    onSetObligationPaid: (id: Long, paid: Boolean) -> Unit = { _, _ -> },
    onDeleteTransaction: (Long) -> Unit
) {
    val ext = LocalSereneColors.current

    val allPaidRecords = remember(transactions, obligations, isArabic) {
        buildList {
            transactions.forEach { tx ->
                when {
                    tx.type == TransactionType.EXPENSE && !tx.isDebt -> {
                        add(
                            ActualPaidRecord(
                                key = "tx_exp_${tx.id}",
                                kind = ActualPaidKind.PAID_EXPENSE,
                                title = tx.itemName.ifBlank { tx.note.ifBlank { tx.category } },
                                party = tx.debtParty,
                                category = tx.category,
                                source = tx.source.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") },
                                currency = tx.currency,
                                amountMinor = tx.amountMinor,
                                date = tx.date,
                                time = tx.time,
                                note = tx.note,
                                sortTimestamp = tx.createdAt,
                                tx = tx
                            )
                        )
                    }
                    tx.isDebt && tx.isDebtSettled -> {
                        add(
                            ActualPaidRecord(
                                key = "tx_debt_${tx.id}",
                                kind = ActualPaidKind.SETTLED_DEBT,
                                title = tx.itemName.ifBlank { tx.note.ifBlank { tx.category } },
                                party = tx.debtParty,
                                category = tx.category,
                                source = tx.source.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") },
                                currency = tx.currency,
                                amountMinor = tx.amountMinor,
                                date = tx.date,
                                time = tx.time,
                                note = tx.note,
                                sortTimestamp = tx.createdAt,
                                tx = tx
                            )
                        )
                    }
                }
            }
            obligations.filter { it.isPaid }.forEach { ob ->
                add(
                    ActualPaidRecord(
                        key = "ob_paid_${ob.id}",
                        kind = ActualPaidKind.PAID_OBLIGATION,
                        title = ob.reason.ifBlank { tr(isArabic, "التزام تم دفعه", "Paid Obligation") },
                        party = ob.party,
                        category = tr(isArabic, "التزام مجدول", "Scheduled Obligation"),
                        source = ob.paidSource.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") },
                        currency = ob.currency,
                        amountMinor = ob.amountMinor,
                        date = ob.paidDate.ifBlank { ob.dueDate },
                        time = ob.dueTime,
                        note = ob.note,
                        sortTimestamp = ob.createdAt,
                        obligation = ob
                    )
                )
            }
        }.sortedWith(compareByDescending<ActualPaidRecord> { it.date }.thenByDescending { it.sortTimestamp })
    }

    val upcomingDueCountAll = remember(transactions, obligations) {
        transactions.count { it.isDebt && !it.isDebtSettled } + obligations.count { !it.isPaid }
    }

    var searchQuery by remember { mutableStateOf("") }
    var typeTab by remember(initialTab) {
        mutableStateOf(
            when (initialTab) {
                TransactionType.EXPENSE -> TransactionType.EXPENSE
                "settled", TransactionType.INCOME -> "settled"
                else -> "all"
            }
        )
    }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var groupMode by remember { mutableStateOf(PaymentGroupMode.FLAT) }
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingReopenTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val availableCurrencies = remember(allPaidRecords, settings.defaultCurrency) {
        val list = allPaidRecords.map { it.currency }.distinct().sorted()
        if (list.isEmpty()) listOf(settings.defaultCurrency)
        else if (settings.defaultCurrency in list) listOf(settings.defaultCurrency) + list.filter { it != settings.defaultCurrency }
        else list
    }
    var activeCurrency by remember(availableCurrencies) { mutableStateOf(availableCurrencies.first()) }
    val validCurrency = if (activeCurrency in availableCurrencies) activeCurrency else availableCurrencies.first()
    val sym = currencySymbol(validCurrency, isArabic)

    val currencyPayments = remember(allPaidRecords, validCurrency) {
        allPaidRecords.filter { it.currency == validCurrency }
    }
    val directExpenses = remember(currencyPayments) {
        currencyPayments.filter { it.kind == ActualPaidKind.PAID_EXPENSE }
    }
    val settledObligationsAndDebts = remember(currencyPayments) {
        currencyPayments.filter { it.kind != ActualPaidKind.PAID_EXPENSE }
    }

    val totalPaidMinor = remember(currencyPayments) { currencyPayments.sumOf { it.amountMinor } }
    val directExpensePaidMinor = remember(directExpenses) { directExpenses.sumOf { it.amountMinor } }
    val settledPaidMinor = remember(settledObligationsAndDebts) { settledObligationsAndDebts.sumOf { it.amountMinor } }

    val availableSources = remember(currencyPayments) {
        currencyPayments.map { it.source }.distinct().sorted()
    }
    val availableCategories = remember(currencyPayments) {
        currencyPayments.map { it.category }.distinct().sorted()
    }

    val filteredPayments = remember(
        currencyPayments,
        searchQuery,
        typeTab,
        selectedSource,
        selectedCategory
    ) {
        val q = searchQuery.trim().lowercase()
        currencyPayments.filter { rec ->
            val matchesTab = when (typeTab) {
                TransactionType.EXPENSE -> rec.kind == ActualPaidKind.PAID_EXPENSE
                "settled" -> rec.kind != ActualPaidKind.PAID_EXPENSE
                else -> true
            }
            if (!matchesTab) return@filter false
            if (selectedSource != null && rec.source != selectedSource) return@filter false
            if (selectedCategory != null && rec.category != selectedCategory) return@filter false
            if (q.isNotEmpty()) {
                val amtStr = formatAmountMinor(rec.amountMinor, false)
                val hay = "${rec.title} ${rec.party} ${rec.category} ${rec.source} ${rec.note} ${rec.date} $amtStr".lowercase()
                if (!hay.contains(q)) return@filter false
            }
            true
        }
    }
    val filteredTotalMinor = remember(filteredPayments) { filteredPayments.sumOf { it.amountMinor } }
    val hasActiveFilters = searchQuery.isNotBlank() || typeTab != "all" || selectedSource != null || selectedCategory != null

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Hero Total Actually Paid Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payments_hero_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = ext.heroCardBackground,
                    shadowElevation = 4.dp
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(ext.incomeColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Payments,
                                        contentDescription = null,
                                        tint = Color(0xFF82F5C1),
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = tr(isArabic, "كل المبالغ اللي اندفعت بالفعل", "All Actually Paid Amounts"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ext.heroCardSubtext,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tr(
                                            isArabic,
                                            "${currencyPayments.size} عملية مدفوعة فعلياً • $upcomingDueCountAll لسه هتندفع",
                                            "${currencyPayments.size} actually paid • $upcomingDueCountAll upcoming"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF82F5C1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("payments_go_debts_btn")
                                    .clickable(onClick = onOpenDebts),
                                shape = RoundedCornerShape(10.dp),
                                color = ext.warningAmber.copy(alpha = 0.22f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.RequestQuote,
                                        contentDescription = null,
                                        tint = ext.warningAmber,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = tr(isArabic, "القادم ($upcomingDueCountAll)", "Due ($upcomingDueCountAll)"),
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
                                text = formatAmountMinor(totalPaidMinor),
                                style = AmountDisplayStyle,
                                color = ext.heroCardOnBackground,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = sym,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF82F5C1),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        if (availableCurrencies.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableCurrencies.forEach { curr ->
                                    val chosen = curr == validCurrency
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable {
                                                activeCurrency = curr
                                                selectedSource = null
                                                selectedCategory = null
                                            },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (chosen) Color.White else Color.White.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "${currencySymbol(curr, isArabic)} ($curr)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (chosen) ext.heroCardBackground else Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Three Dedicated Tabs: All | Direct Paid Expenses | Settled Obligations & Debts
            item {
                SereneCard(contentPadding = PaddingValues(10.dp)) {
                    Text(
                        text = tr(
                            isArabic,
                            "تصنيف المبالغ اللي اندفعت بالفعل:",
                            "Filter Actually Paid Amounts:"
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentTabCard(
                            modifier = Modifier.weight(0.85f),
                            title = tr(isArabic, "الكل", "All"),
                            subtitle = "${currencyPayments.size} • ${formatAmountMinor(totalPaidMinor, false)}",
                            accent = ext.incomeColor,
                            selected = typeTab == "all",
                            tag = "payments_tab_all",
                            onClick = { typeTab = "all" }
                        )
                        PaymentTabCard(
                            modifier = Modifier.weight(1.1f),
                            title = tr(isArabic, "مصروفات مدفوعة", "Paid Expenses"),
                            subtitle = "${directExpenses.size} • ${formatAmountMinor(directExpensePaidMinor, false)} $sym",
                            accent = ext.expenseColor,
                            selected = typeTab == TransactionType.EXPENSE,
                            tag = "payments_tab_expense",
                            onClick = { typeTab = TransactionType.EXPENSE }
                        )
                        PaymentTabCard(
                            modifier = Modifier.weight(1.15f),
                            title = tr(isArabic, "التزامات مسددة", "Settled Dues"),
                            subtitle = "${settledObligationsAndDebts.size} • ${formatAmountMinor(settledPaidMinor, false)} $sym",
                            accent = MaterialTheme.colorScheme.primary,
                            selected = typeTab == "settled",
                            tag = "payments_tab_settled",
                            onClick = { typeTab = "settled" }
                        )
                    }
                }
            }

            // 3. Compact Collapsible Dropdown Filters & Formatted Export
            item {
                val activeFilterCount = listOf(
                    searchQuery.isNotBlank(),
                    selectedSource != null,
                    selectedCategory != null,
                    groupMode != PaymentGroupMode.FLAT
                ).count { it }
                val sourceOptions = remember(availableSources, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل الحسابات", "All Accounts"))) +
                        availableSources.map { DropdownFilterOption<String?>(it, it) }
                }
                val categoryOptions = remember(availableCategories, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل التصنيفات", "All Categories"))) +
                        availableCategories.map { DropdownFilterOption<String?>(it, it) }
                }
                val groupOptions = remember(isArabic) {
                    listOf(
                        DropdownFilterOption(PaymentGroupMode.FLAT, tr(isArabic, "قائمة عادية", "Flat List")),
                        DropdownFilterOption(PaymentGroupMode.BY_SOURCE, tr(isArabic, "حسب الحساب", "By Account")),
                        DropdownFilterOption(PaymentGroupMode.BY_CATEGORY, tr(isArabic, "حسب التصنيف", "By Category"))
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportDownloadButton(
                        transactions = filteredPayments.mapNotNull { it.tx },
                        obligations = filteredPayments.mapNotNull { it.obligation },
                        preferredCurrency = validCurrency,
                        reportTitle = tr(isArabic, "كل المبالغ اللي اندفعت بالفعل", "All Actually Paid Amounts"),
                        filterSummary = tr(
                            isArabic,
                            "${filteredPayments.size} عملية مدفوعة • ${formatAmountMinor(filteredTotalMinor)} $sym",
                            "${filteredPayments.size} paid entries • ${formatAmountMinor(filteredTotalMinor)} $sym"
                        ),
                        isArabic = isArabic,
                        testTag = "payments_export_btn",
                        modifier = Modifier.fillMaxWidth()
                    )

                    CollapsibleFilterDropdownCard(
                        isArabic = isArabic,
                        activeFilterCount = activeFilterCount,
                        onResetFilters = if (hasActiveFilters || groupMode != PaymentGroupMode.FLAT) {
                            {
                                searchQuery = ""
                                typeTab = "all"
                                selectedSource = null
                                selectedCategory = null
                                groupMode = PaymentGroupMode.FLAT
                            }
                        } else null,
                        title = tr(isArabic, "بحث وفلاتر المدفوعات", "Payments Search & Filters"),
                        testTagPrefix = "payments_filters"
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
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
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = tr(isArabic, "مسح البحث", "Clear"),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { searchQuery = "" }
                                            .padding(4.dp)
                                    )
                                }
                            },
                            placeholder = {
                                Text(
                                    text = tr(
                                        isArabic,
                                        "ابحث اندفعت لمين، عشان إيه، التصنيف، الحساب، أو المبلغ...",
                                        "Search recipient, reason, category, account, amount..."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ext.cardSubtleBackground,
                                unfocusedContainerColor = ext.cardSubtleBackground,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = ext.incomeColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payments_search_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (availableSources.isNotEmpty()) {
                                SmartSingleSelectDropdown(
                                    label = tr(isArabic, "الحساب", "Account"),
                                    selectedValue = selectedSource,
                                    options = sourceOptions,
                                    isArabic = isArabic,
                                    onSelected = { selectedSource = it },
                                    isHighlighted = selectedSource != null,
                                    testTag = "payments_source_dropdown",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (availableCategories.isNotEmpty()) {
                                SmartSingleSelectDropdown(
                                    label = tr(isArabic, "التصنيف", "Category"),
                                    selectedValue = selectedCategory,
                                    options = categoryOptions,
                                    isArabic = isArabic,
                                    onSelected = { selectedCategory = it },
                                    isHighlighted = selectedCategory != null,
                                    testTag = "payments_category_dropdown",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "طريقة العرض والتنظيم", "Grouping Mode"),
                            selectedValue = groupMode,
                            options = groupOptions,
                            isArabic = isArabic,
                            onSelected = { groupMode = it },
                            isHighlighted = groupMode != PaymentGroupMode.FLAT,
                            testTag = "payments_group_dropdown",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 4. Subtotal Strip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(
                            isArabic,
                            "المدفوع فعلياً (${filteredPayments.size})",
                            "Actually Paid (${filteredPayments.size})"
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${tr(isArabic, "الإجمالي:", "Total:")} ${formatAmountMinor(filteredTotalMinor)} $sym",
                        style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                        color = ext.expenseColor
                    )
                }
            }

            // 5. Empty State or List
            if (filteredPayments.isEmpty()) {
                item {
                    SereneCard(contentPadding = PaddingValues(24.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(ext.incomeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = if (allPaidRecords.isEmpty()) {
                                    tr(isArabic, "لا توجد مبالغ اندفعت بالفعل حتى الآن", "No paid amounts recorded yet")
                                } else {
                                    tr(isArabic, "لا توجد نتائج مطابقة للبحث أو الفلتر", "No records match your search/filter")
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = tr(
                                    isArabic,
                                    "أي مصروف تسجله كـ «اندفعت بالفعل» أو أي مبلغ مطلوب تضغط عليه «تم الدفع» يظهر هنا فوراً.",
                                    "Any expense logged as Paid or any upcoming item you mark Paid appears here immediately."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onOpenDebts,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ext.heroCardBackground,
                                    contentColor = ext.heroCardOnBackground
                                )
                            ) {
                                Icon(Icons.Outlined.RequestQuote, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr(isArabic, "عرض المبالغ المطلوب دفعها قريباً", "View Upcoming Payments"))
                            }
                        }
                    }
                }
            } else {
                when (groupMode) {
                    PaymentGroupMode.FLAT -> {
                        items(filteredPayments.size, key = { filteredPayments[it].key }) { idx ->
                            val rec = filteredPayments[idx]
                            ActualPaidRecordCard(
                                record = rec,
                                categories = categories,
                                isArabic = isArabic,
                                onReopenClick = {
                                    when {
                                        rec.tx != null && rec.tx.isDebt -> pendingReopenTx = rec.tx
                                        rec.obligation != null -> onSetObligationPaid(rec.obligation.id, false)
                                    }
                                },
                                onDeleteClick = {
                                    rec.tx?.let { pendingDeleteTx = it }
                                }
                            )
                        }
                    }

                    PaymentGroupMode.BY_SOURCE, PaymentGroupMode.BY_CATEGORY -> {
                        val grouped = filteredPayments.groupBy {
                            if (groupMode == PaymentGroupMode.BY_SOURCE) it.source else it.category
                        }.entries.sortedByDescending { entry -> entry.value.sumOf { it.amountMinor } }

                        grouped.forEach { (groupTitle, items) ->
                            val groupTotal = items.sumOf { it.amountMinor }
                            item(key = "pay_header_$groupTitle") {
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (groupMode == PaymentGroupMode.BY_SOURCE) {
                                                    Icons.Outlined.AccountBalance
                                                } else Icons.Outlined.Category,
                                                contentDescription = null,
                                                tint = ext.incomeColor,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Text(
                                                text = "$groupTitle (${items.size})",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "${formatAmountMinor(groupTotal)} $sym",
                                            style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                                            color = ext.expenseColor
                                        )
                                    }
                                }
                            }
                            items(items.size, key = { items[it].key }) { idx ->
                                val rec = items[idx]
                                ActualPaidRecordCard(
                                    record = rec,
                                    categories = categories,
                                    isArabic = isArabic,
                                    onReopenClick = {
                                        when {
                                            rec.tx != null && rec.tx.isDebt -> pendingReopenTx = rec.tx
                                            rec.obligation != null -> onSetObligationPaid(rec.obligation.id, false)
                                        }
                                    },
                                    onDeleteClick = {
                                        rec.tx?.let { pendingDeleteTx = it }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingReopenTx?.let { tx ->
        SecurityVerificationDialog(
            title = tr(isArabic, "إرجاع للمبالغ المطلوب دفعها قريباً", "Move Back to Upcoming Due"),
            description = tr(
                isArabic,
                "هل تريد إرجاع \"${tx.itemName.ifBlank { tx.category }}\" إلى «كل المبالغ المطلوب دفعها قريباً»؟",
                "Move \"${tx.itemName.ifBlank { tx.category }}\" back to Upcoming Payments Due?"
            ),
            confirmLabel = tr(isArabic, "إرجاع للقادم", "Move Back"),
            isDanger = false,
            requireSecurity = settings.confirmPaymentWithSecurity,
            settings = settings,
            isArabic = isArabic,
            onConfirmed = {
                onReopenAsDebt(tx.id)
                pendingReopenTx = null
            },
            onDismiss = { pendingReopenTx = null }
        )
    }

    pendingDeleteTx?.let { tx ->
        SecurityVerificationDialog(
            title = tr(isArabic, "حذف عملية مدفوعة", "Delete Paid Record"),
            description = tr(
                isArabic,
                "هل تريد حذف \"${tx.itemName.ifBlank { tx.category }}\" نهائياً من قاعدة البيانات والتقارير؟",
                "Delete \"${tx.itemName.ifBlank { tx.category }}\" permanently from records and reports?"
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
private fun PaymentTabCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    accent: Color,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else ext.cardSubtleBackground,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else ext.cardBorder.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = AmountBodyStyle.copy(
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PaymentPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) ext.heroCardBackground else ext.cardSubtleBackground,
        border = if (selected) null else BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.6f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ActualPaidRecordCard(
    record: ActualPaidRecord,
    categories: List<CategoryEntity>,
    isArabic: Boolean,
    onReopenClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    val sym = currencySymbol(record.currency, isArabic)
    val iconKey = categories.firstOrNull { it.name == record.category }?.iconKey ?: "payments"
    val kindLabel = when (record.kind) {
        ActualPaidKind.PAID_EXPENSE -> tr(isArabic, "مصروف اندفع بالفعل", "Paid Expense")
        ActualPaidKind.SETTLED_DEBT -> tr(isArabic, "مبلغ قادم تم دفعه", "Settled Due")
        ActualPaidKind.PAID_OBLIGATION -> tr(isArabic, "التزام مجدول تم دفعه", "Paid Obligation")
    }
    val kindColor = when (record.kind) {
        ActualPaidKind.PAID_EXPENSE -> ext.expenseColor
        ActualPaidKind.SETTLED_DEBT -> ext.incomeColor
        ActualPaidKind.PAID_OBLIGATION -> MaterialTheme.colorScheme.primary
    }
    val canReopen = (record.tx != null && record.tx.isDebt) || record.obligation != null
    val canDelete = record.tx != null

    SereneCard(
        modifier = Modifier.testTag("payment_card_${record.key}"),
        borderColor = kindColor.copy(alpha = 0.35f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(kindColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = record.category,
                    tint = kindColor,
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(kindColor.copy(alpha = 0.14f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = kindLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = kindColor,
                            maxLines = 1
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ext.cardSubtleBackground)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = record.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = iconForKey(iconKey),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = record.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    if (record.party.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = tr(isArabic, "اندفعت لمين: ${record.party}", "To: ${record.party}"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = formatDisplayDateTime(record.date, record.time, isArabic),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${formatAmountMinor(record.amountMinor)} $sym",
                    style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                    color = ext.expenseColor,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tr(isArabic, "تم الدفع ✓", "Paid ✓"),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ext.incomeColor
                )
            }
        }

        if (record.note.isNotBlank() && record.note != record.title) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = record.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (canReopen || canDelete) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canReopen) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .testTag("reopen_payment_${record.key}")
                            .clickable(onClick = onReopenClick),
                        shape = RoundedCornerShape(10.dp),
                        color = ext.cardSubtleBackground
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Undo,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tr(isArabic, "إرجاع للقادم", "Move to Upcoming"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (canDelete) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(ext.cardSubtleBackground)
                            .clickable(onClick = onDeleteClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = tr(isArabic, "حذف", "Delete"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
