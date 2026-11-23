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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.text.style.TextDecoration
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
import com.example.ui.components.ItemReminderConfig
import com.example.ui.components.ReminderSettingsDialog
import com.example.ui.components.SecurityVerificationDialog
import com.example.ui.components.SereneCard
import com.example.ui.components.SmartNoteFilterDropdown
import com.example.ui.components.SmartSingleSelectDropdown
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.currentIsoDate
import com.example.util.formatAmountMinor
import com.example.util.formatDisplayDateTime
import com.example.util.formatTime12Hour
import com.example.util.iconForKey
import com.example.util.tr

private enum class DebtStatusFilter { UNPAID, ALL, SETTLED }
private enum class DebtGroupMode { FLAT, BY_SOURCE, BY_CATEGORY }

@Composable
fun DebtsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    obligations: List<ObligationEntity> = emptyList(),
    initialDebtTab: String = "all",
    onAddDebtClick: (initialType: String) -> Unit,
    onOpenPayments: () -> Unit,
    onOpenObligations: () -> Unit = {},
    onToggleSettled: (id: Long, settled: Boolean) -> Unit,
    onSetObligationPaid: (id: Long, paid: Boolean) -> Unit = { _, _ -> },
    onUpdateTransactionReminder: (Long, ItemReminderConfig) -> Unit = { _, _ -> },
    onUpdateObligationReminder: (Long, ItemReminderConfig) -> Unit = { _, _ -> },
    onRequestNotificationPermission: () -> Unit = {},
    onDeleteTransaction: (Long) -> Unit
) {
    val ext = LocalSereneColors.current

    val allDebts = remember(transactions) { transactions.filter { it.isDebt } }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(DebtStatusFilter.UNPAID) }
    var typeFilter by remember(initialDebtTab) { mutableStateOf(initialDebtTab) } // "all", "income", "expense"
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedParty by remember { mutableStateOf<String?>(null) }
    var selectedItem by remember { mutableStateOf<String?>(null) }
    var noteFilterQuery by remember { mutableStateOf("") }
    var groupMode by remember { mutableStateOf(DebtGroupMode.FLAT) }
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingPayTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var reminderEditTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var reminderEditObligation by remember { mutableStateOf<ObligationEntity?>(null) }

    val availableCurrencies = remember(allDebts, obligations, settings.defaultCurrency) {
        val list = (allDebts.map { it.currency } + obligations.map { it.currency }).distinct().sorted()
        if (list.isEmpty()) listOf(settings.defaultCurrency)
        else if (settings.defaultCurrency in list) listOf(settings.defaultCurrency) + list.filter { it != settings.defaultCurrency }
        else list
    }
    var activeCurrency by remember(availableCurrencies) {
        mutableStateOf(availableCurrencies.first())
    }
    val validCurrency = if (activeCurrency in availableCurrencies) activeCurrency else availableCurrencies.first()
    val sym = currencySymbol(validCurrency, isArabic)

    val currencyDebts = remember(allDebts, validCurrency) {
        allDebts.filter { it.currency == validCurrency }
    }
    val currencyObligations = remember(obligations, validCurrency) {
        obligations.filter { it.currency == validCurrency }
    }
    val pendingObligations = remember(currencyObligations) {
        currencyObligations.filter { !it.isPaid }
    }
    val paidObligations = remember(currencyObligations) {
        currencyObligations.filter { it.isPaid }
    }
    val pendingObligationsMinor = remember(pendingObligations) {
        pendingObligations.sumOf { it.amountMinor }
    }
    val paidObligationsMinor = remember(paidObligations) {
        paidObligations.sumOf { it.amountMinor }
    }
    val availableSources = remember(currencyDebts) {
        currencyDebts.map { it.source }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableCategories = remember(currencyDebts) {
        currencyDebts.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableParties = remember(currencyDebts, currencyObligations) {
        (currencyDebts.map { it.debtParty.trim() } + currencyObligations.map { it.party.trim() })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val availableItems = remember(currencyDebts, currencyObligations) {
        (currencyDebts.map { it.itemName.trim() } + currencyObligations.map { it.reason.trim() })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val availableNotes = remember(currencyDebts, currencyObligations) {
        (currencyDebts.map { it.note.trim() } + currencyObligations.map { it.note.trim() })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredDebts = remember(
        currencyDebts,
        searchQuery,
        statusFilter,
        typeFilter,
        selectedSource,
        selectedCategory,
        selectedParty,
        selectedItem,
        noteFilterQuery
    ) {
        val q = searchQuery.trim().lowercase()
        val nq = noteFilterQuery.trim()
        currencyDebts.filter { tx ->
            val matchesStatus = when (statusFilter) {
                DebtStatusFilter.UNPAID -> !tx.isDebtSettled
                DebtStatusFilter.SETTLED -> tx.isDebtSettled
                DebtStatusFilter.ALL -> true
            }
            if (!matchesStatus) return@filter false
            if (typeFilter != "all" && tx.type != typeFilter) return@filter false
            if (selectedSource != null && tx.source != selectedSource) return@filter false
            if (selectedCategory != null && tx.category != selectedCategory) return@filter false
            if (selectedParty != null && tx.debtParty.trim() != selectedParty) return@filter false
            if (selectedItem != null && tx.itemName.trim() != selectedItem) return@filter false
            if (nq.isNotEmpty() && !tx.note.contains(nq, ignoreCase = true)) return@filter false
            if (q.isNotEmpty()) {
                val amtStr = formatAmountMinor(tx.amountMinor, false)
                val hay = "${tx.itemName} ${tx.debtParty} ${tx.category} ${tx.source} ${tx.note} ${tx.date} $amtStr".lowercase()
                if (!hay.contains(q)) return@filter false
            }
            true
        }
    }

    val filteredPendingObligations = remember(
        pendingObligations,
        searchQuery,
        statusFilter,
        typeFilter,
        selectedSource,
        selectedCategory,
        selectedParty,
        selectedItem,
        noteFilterQuery
    ) {
        if (statusFilter == DebtStatusFilter.SETTLED || typeFilter == TransactionType.INCOME || selectedSource != null || selectedCategory != null) {
            emptyList()
        } else {
            val q = searchQuery.trim().lowercase()
            val nq = noteFilterQuery.trim()
            pendingObligations.filter { ob ->
                if (selectedParty != null && ob.party.trim() != selectedParty) return@filter false
                if (selectedItem != null && ob.reason.trim() != selectedItem) return@filter false
                if (nq.isNotEmpty() && !ob.note.contains(nq, ignoreCase = true)) return@filter false
                if (q.isEmpty()) true
                else {
                    val amtStr = formatAmountMinor(ob.amountMinor, false)
                    "${ob.reason} ${ob.party} ${ob.note} ${ob.dueDate} $amtStr".lowercase().contains(q)
                }
            }
        }
    }

    // Live unified totals synced with HomeScreen, Plan, Reports & Dashboard
    val unpaidList = remember(currencyDebts) { currencyDebts.filter { !it.isDebtSettled } }
    val settledList = remember(currencyDebts) { currencyDebts.filter { it.isDebtSettled } }
    val totalUnpaidDebtMinor = remember(unpaidList) { unpaidList.sumOf { it.amountMinor } }
    val totalUnpaidMinor = totalUnpaidDebtMinor + pendingObligationsMinor
    val unpaidExpenseDebtMinor = remember(unpaidList) {
        unpaidList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    }
    val unpaidIncomeDebtMinor = remember(unpaidList) {
        unpaidList.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    }
    val totalSettledMinor = remember(settledList, paidObligationsMinor) {
        settledList.sumOf { it.amountMinor } + paidObligationsMinor
    }
    val filteredTotalMinor = remember(filteredDebts, filteredPendingObligations) {
        filteredDebts.sumOf { it.amountMinor } + filteredPendingObligations.sumOf { it.amountMinor }
    }
    val totalPendingCount = unpaidList.size + pendingObligations.size
    val totalSettledCount = settledList.size + paidObligations.size

    val hasActiveFilters = searchQuery.isNotBlank() ||
        statusFilter != DebtStatusFilter.UNPAID ||
        typeFilter != "all" ||
        selectedSource != null ||
        selectedCategory != null ||
        selectedParty != null ||
        selectedItem != null ||
        noteFilterQuery.isNotBlank()

    val exportFilterSummary = remember(
        hasActiveFilters,
        filteredDebts.size,
        filteredPendingObligations.size,
        filteredTotalMinor,
        statusFilter,
        typeFilter,
        selectedSource,
        selectedCategory,
        selectedParty,
        selectedItem,
        noteFilterQuery,
        searchQuery,
        sym,
        isArabic
    ) {
        val totalItems = filteredDebts.size + filteredPendingObligations.size
        if (!hasActiveFilters) {
            tr(
                isArabic,
                "كل المبالغ المطلوب دفعها قريباً ($totalItems عملية • ${formatAmountMinor(filteredTotalMinor)} $sym — بدون فلاتر)",
                "All Upcoming Payments Due ($totalItems items • ${formatAmountMinor(filteredTotalMinor)} $sym — No Filters)"
            )
        } else {
            buildList {
                if (statusFilter != DebtStatusFilter.UNPAID) {
                    add(if (statusFilter == DebtStatusFilter.ALL) tr(isArabic, "كل الحالات", "All Statuses") else tr(isArabic, "المسددة", "Settled"))
                }
                if (typeFilter == TransactionType.EXPENSE) add(tr(isArabic, "مصاريف لسه هتندفع", "Upcoming Expenses"))
                if (typeFilter == TransactionType.INCOME) add(tr(isArabic, "سلف ومبالغ مستلمة", "Borrowed"))
                selectedSource?.let { add(tr(isArabic, "الحساب: $it", "Account: $it")) }
                selectedCategory?.let { add(tr(isArabic, "التصنيف: $it", "Category: $it")) }
                selectedParty?.let { add(tr(isArabic, "هتندفع لمين: $it", "Paid To: $it")) }
                selectedItem?.let { add(tr(isArabic, "عشان إيه: $it", "What For: $it")) }
                if (noteFilterQuery.isNotBlank()) add(tr(isArabic, "ملاحظة: \"${noteFilterQuery.trim()}\"", "Note: \"${noteFilterQuery.trim()}\""))
                if (searchQuery.isNotBlank()) add(tr(isArabic, "بحث: \"${searchQuery.trim()}\"", "Search: \"${searchQuery.trim()}\""))
                add("$totalItems • ${formatAmountMinor(filteredTotalMinor)} $sym")
            }.joinToString(" • ")
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
            // 1. Hero Total Debts Summary Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debts_hero_card"),
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(ext.warningAmber.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.RequestQuote,
                                        contentDescription = null,
                                        tint = ext.warningAmber,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = tr(isArabic, "كل المبالغ المطلوب دفعها قريباً", "All Upcoming Payments Due"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ext.heroCardSubtext
                                    )
                                    Text(
                                        text = tr(
                                            isArabic,
                                            "$totalPendingCount مبلغ لسه هيندفع • $totalSettledCount تم دفعه",
                                            "$totalPendingCount upcoming • $totalSettledCount paid"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.heroCardSubtext.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("debts_add_new_btn")
                                    .clickable {
                                        onAddDebtClick(TransactionType.EXPENSE)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = ext.incomeColor
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
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
                                        text = tr(isArabic, "إضافة مبلغ", "Add Due"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatAmountMinor(totalUnpaidMinor),
                                style = AmountDisplayStyle,
                                color = ext.heroCardOnBackground
                            )
                            Text(
                                text = sym,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = ext.warningAmber,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Clear Repayment Notice
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = ext.warningAmber.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = tr(
                                    isArabic,
                                    "يجمع كل مصاريفك اللي لسه هتندفع قريباً وكل الالتزامات المجدولة — وأي مبلغ تدفعه يتخصم فوراً من محفظتك",
                                    "Unifies all upcoming unpaid expenses & scheduled obligations — paying any item deducts immediately from your wallet"
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // Breakdown Split: Upcoming Expenses vs Scheduled Obligations vs Paid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DebtHeroMetricChip(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { typeFilter = TransactionType.EXPENSE },
                                label = tr(isArabic, "مصاريف لسه هتندفع", "Upcoming Exp."),
                                value = "${formatAmountMinor(unpaidExpenseDebtMinor + unpaidIncomeDebtMinor, false)} $sym",
                                accent = ext.chartRose
                            )
                            DebtHeroMetricChip(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenObligations() },
                                label = tr(isArabic, "التزامات بمواعيد", "Scheduled Dues"),
                                value = "${formatAmountMinor(pendingObligationsMinor, false)} $sym",
                                accent = ext.warningAmber
                            )
                            DebtHeroMetricChip(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("debts_open_payments_chip")
                                    .clickable { onOpenPayments() },
                                label = tr(isArabic, "اندفعت بالفعل", "Actually Paid"),
                                value = "${formatAmountMinor(totalSettledMinor, false)} $sym",
                                accent = Color(0xFF34D399)
                            )
                        }

                        // Multi-currency selector if debts exist in >1 currency
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

            // 1b. Scheduled Obligations Due Soon Section (synced directly with ObligationsScreen)
            if (filteredPendingObligations.isNotEmpty()) {
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
                                "التزامات بمواعيد سداد (${filteredPendingObligations.size})",
                                "Scheduled Payment Dues (${filteredPendingObligations.size})"
                            ),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ext.warningAmber
                        )
                        Text(
                            text = tr(isArabic, "إدارة المواعيد ←", "Manage →"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenObligations)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                items(filteredPendingObligations.size, key = { "sched_ob_${filteredPendingObligations[it].id}" }) { idx ->
                    val ob = filteredPendingObligations[idx]
                    val obSym = currencySymbol(ob.currency, isArabic)
                    SereneCard(
                        modifier = Modifier.testTag("upcoming_obligation_card_${ob.id}"),
                        borderColor = ext.warningAmber.copy(alpha = 0.45f),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = ob.reason.ifBlank { tr(isArabic, "مبلغ مطلوب دفعه", "Scheduled Payment") },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (ob.party.isNotBlank()) {
                                    Text(
                                        text = tr(isArabic, "هتندفع لمين: ${ob.party}", "To: ${ob.party}"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = buildString {
                                        append(tr(isArabic, "الموعد: ${ob.dueDate}", "Due: ${ob.dueDate}"))
                                        if (ob.dueTime.isNotBlank()) append(" • ${formatTime12Hour(ob.dueTime, isArabic)}")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${formatAmountMinor(ob.amountMinor)} $obSym",
                                style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = ext.expenseColor
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("pay_obligation_${ob.id}")
                                    .clickable { onSetObligationPaid(ob.id, true) },
                                shape = RoundedCornerShape(10.dp),
                                color = ext.incomeColor
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Payments,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tr(isArabic, "تم الدفع", "Mark Paid"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("debt_ob_reminder_btn_${ob.id}")
                                    .clickable { reminderEditObligation = ob },
                                shape = RoundedCornerShape(10.dp),
                                color = ext.cardSubtleBackground,
                                border = BorderStroke(1.dp, ext.cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = tr(isArabic, "إعدادات التنبيه", "Alert settings"),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = tr(isArabic, "إعدادات التنبيه", "Alert"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Unified Collapsible Searchable Dropdown Filters Card + Export Bar
            item {
                val unpaidIncomeCount = remember(unpaidList) {
                    unpaidList.count { it.type == TransactionType.INCOME }
                }
                val unpaidExpenseCount = remember(unpaidList) {
                    unpaidList.count { it.type == TransactionType.EXPENSE }
                }
                val activeCount = (if (searchQuery.isNotBlank()) 1 else 0) +
                    (if (statusFilter != DebtStatusFilter.UNPAID) 1 else 0) +
                    (if (typeFilter != "all") 1 else 0) +
                    (if (selectedSource != null) 1 else 0) +
                    (if (selectedCategory != null) 1 else 0) +
                    (if (selectedParty != null) 1 else 0) +
                    (if (selectedItem != null) 1 else 0) +
                    (if (noteFilterQuery.isNotBlank()) 1 else 0)
                val statusLabel = when (statusFilter) {
                    DebtStatusFilter.UNPAID -> tr(isArabic, "القائمة (${unpaidList.size})", "Unpaid (${unpaidList.size})")
                    DebtStatusFilter.ALL -> tr(isArabic, "الكل (${currencyDebts.size})", "All (${currencyDebts.size})")
                    DebtStatusFilter.SETTLED -> tr(isArabic, "المسددة (${settledList.size})", "Settled (${settledList.size})")
                }
                val partyOptions = remember(availableParties, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "الكل", "All"))) +
                        availableParties.map { DropdownFilterOption<String?>(it, it) }
                }
                val itemOptions = remember(availableItems, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "الكل", "All"))) +
                        availableItems.map { DropdownFilterOption<String?>(it, it) }
                }
                CollapsibleFilterDropdownCard(
                    isArabic = isArabic,
                    activeFilterCount = activeCount,
                    onResetFilters = if (hasActiveFilters) {
                        {
                            searchQuery = ""
                            statusFilter = DebtStatusFilter.UNPAID
                            typeFilter = "all"
                            selectedSource = null
                            selectedCategory = null
                            selectedParty = null
                            selectedItem = null
                            noteFilterQuery = ""
                        }
                    } else null,
                    title = tr(isArabic, "فلاتر وبحث المبالغ المطلوبة ($statusLabel)", "Filter & Search ($statusLabel)"),
                    testTagPrefix = "debts_filter_dropdown"
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
                                text = tr(isArabic, "ابحث باسم الحاجة، الجهة، التصنيف، أو المصدر...", "Search item, party, category, source..."),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ext.cardSubtleBackground,
                            unfocusedContainerColor = ext.cardSubtleBackground,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("debts_search_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "نوع المبالغ المطلوبة", "Upcoming Type"),
                            selectedValue = typeFilter,
                            options = listOf(
                                DropdownFilterOption("all", tr(isArabic, "الكل (${unpaidList.size})", "All (${unpaidList.size})")),
                                DropdownFilterOption(TransactionType.EXPENSE, tr(isArabic, "مصاريف لسه هتندفع ($unpaidExpenseCount)", "Upcoming Expenses ($unpaidExpenseCount)")),
                                DropdownFilterOption(TransactionType.INCOME, tr(isArabic, "سلف ومبالغ مستلمة ($unpaidIncomeCount)", "Borrowed ($unpaidIncomeCount)"))
                            ),
                            isArabic = isArabic,
                            onSelected = { typeFilter = it },
                            isHighlighted = typeFilter != "all",
                            testTag = "debt_type_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "حالة السداد", "Settlement Status"),
                            selectedValue = statusFilter,
                            options = listOf(
                                DropdownFilterOption(DebtStatusFilter.UNPAID, tr(isArabic, "القائمة (${unpaidList.size})", "Unpaid (${unpaidList.size})")),
                                DropdownFilterOption(DebtStatusFilter.ALL, tr(isArabic, "الكل (${currencyDebts.size})", "All (${currencyDebts.size})")),
                                DropdownFilterOption(DebtStatusFilter.SETTLED, tr(isArabic, "تم سدادها (${settledList.size})", "Settled (${settledList.size})"))
                            ),
                            isArabic = isArabic,
                            onSelected = { statusFilter = it },
                            isHighlighted = statusFilter != DebtStatusFilter.UNPAID,
                            testTag = "debt_status_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "المصدر / الحساب", "Source / Account"),
                            selectedValue = selectedSource,
                            options = listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل المصادر", "All Sources"))) +
                                availableSources.map { DropdownFilterOption<String?>(it, it) },
                            isArabic = isArabic,
                            onSelected = { selectedSource = it },
                            isHighlighted = selectedSource != null,
                            testTag = "debt_source_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "التصنيف", "Category"),
                            selectedValue = selectedCategory,
                            options = listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل التصنيفات", "All Categories"))) +
                                availableCategories.map { DropdownFilterOption<String?>(it, it) },
                            isArabic = isArabic,
                            onSelected = { selectedCategory = it },
                            isHighlighted = selectedCategory != null,
                            testTag = "debt_category_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "هتندفع لمين / الجهة", "Paid To / Party"),
                            selectedValue = selectedParty,
                            options = partyOptions,
                            isArabic = isArabic,
                            onSelected = { selectedParty = it },
                            isHighlighted = selectedParty != null,
                            testTag = "debt_party_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "عشان إيه / البند", "What For / Item"),
                            selectedValue = selectedItem,
                            options = itemOptions,
                            isArabic = isArabic,
                            onSelected = { selectedItem = it },
                            isHighlighted = selectedItem != null,
                            testTag = "debt_item_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmartNoteFilterDropdown(
                            label = tr(isArabic, "الملاحظة الإضافية", "Additional Notes"),
                            noteQuery = noteFilterQuery,
                            availableNotes = availableNotes,
                            isArabic = isArabic,
                            onNoteQueryChange = { noteFilterQuery = it },
                            testTag = "debt_note_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "طريقة العرض", "Group By"),
                            selectedValue = groupMode,
                            options = listOf(
                                DropdownFilterOption(DebtGroupMode.FLAT, tr(isArabic, "قائمة عادية", "Flat List")),
                                DropdownFilterOption(DebtGroupMode.BY_SOURCE, tr(isArabic, "مجمع حسب المصدر", "By Source")),
                                DropdownFilterOption(DebtGroupMode.BY_CATEGORY, tr(isArabic, "مجمع حسب التصنيف", "By Category"))
                            ),
                            isArabic = isArabic,
                            onSelected = { groupMode = it },
                            isHighlighted = groupMode != DebtGroupMode.FLAT,
                            testTag = "debt_group_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (availableCurrencies.size > 1) {
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "العملة", "Currency"),
                            selectedValue = validCurrency,
                            options = availableCurrencies.map { curr ->
                                DropdownFilterOption(curr, "${currencySymbol(curr, isArabic)} ($curr)")
                            },
                            isArabic = isArabic,
                            onSelected = { curr ->
                                activeCurrency = curr
                                selectedSource = null
                                selectedCategory = null
                                selectedParty = null
                                selectedItem = null
                            },
                            isHighlighted = validCurrency != settings.defaultCurrency,
                            testTag = "debt_currency_dropdown"
                        )
                    }
                }
            }

            // 3. Filtered Results Subtotal Strip + Formatted Export Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(
                                isArabic,
                                "المبالغ المطلوب دفعها قريباً (${filteredDebts.size + filteredPendingObligations.size})",
                                "Upcoming Payments Due (${filteredDebts.size + filteredPendingObligations.size})"
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
                    ExportDownloadButton(
                        transactions = filteredDebts,
                        obligations = filteredPendingObligations,
                        preferredCurrency = validCurrency,
                        reportTitle = tr(isArabic, "المبالغ المطلوب دفعها قريباً", "Upcoming Payments Due"),
                        filterSummary = exportFilterSummary,
                        isArabic = isArabic,
                        compactLabel = true,
                        testTag = "debts_export_btn"
                    )
                }
            }

            // 4. Empty State OR Organized Cards
            if (filteredDebts.isEmpty() && filteredPendingObligations.isEmpty()) {
                item {
                    SereneCard(contentPadding = PaddingValues(24.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(ext.incomeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TaskAlt,
                                    contentDescription = null,
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = if (allDebts.isEmpty() && pendingObligations.isEmpty()) {
                                    tr(isArabic, "لا توجد مبالغ مطلوب دفعها قريباً حالياً ✓", "No upcoming payments due right now ✓")
                                } else {
                                    tr(isArabic, "لا توجد نتائج مطابقة للفلاتر الحالية", "No items match your current filters")
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = tr(
                                    isArabic,
                                    "يمكنك إضافة أي مصروف «لسه هيندفع قريباً» من شاشة الإضافة أو جدولة التزام بموعد وتنبيه.",
                                    "Add any upcoming expense from the Add screen or schedule an obligation with reminders."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { onAddDebtClick(TransactionType.EXPENSE) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ext.heroCardBackground,
                                    contentColor = ext.heroCardOnBackground
                                )
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr(isArabic, "إضافة مبلغ مطلوب دفعه قريباً", "Add Upcoming Payment"))
                            }
                        }
                    }
                }
            } else {
                when (groupMode) {
                    DebtGroupMode.FLAT -> {
                        items(filteredDebts.size, key = { filteredDebts[it].id }) { idx ->
                            val tx = filteredDebts[idx]
                            DebtDetailCard(
                                tx = tx,
                                categories = categories,
                                isArabic = isArabic,
                                onToggleSettled = { pendingPayTx = tx },
                                onReminderClick = { reminderEditTx = tx },
                                onDeleteClick = { pendingDeleteTx = tx }
                            )
                        }
                    }

                    DebtGroupMode.BY_SOURCE, DebtGroupMode.BY_CATEGORY -> {
                        val grouped = filteredDebts.groupBy {
                            if (groupMode == DebtGroupMode.BY_SOURCE) it.source else it.category
                        }.entries.sortedByDescending { entry -> entry.value.sumOf { it.amountMinor } }

                        grouped.forEach { (groupTitle, items) ->
                            val groupTotal = items.sumOf { it.amountMinor }
                            item(key = "header_$groupTitle") {
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
                                                imageVector = if (groupMode == DebtGroupMode.BY_SOURCE) {
                                                    Icons.Outlined.AccountBalance
                                                } else {
                                                    Icons.Outlined.Category
                                                },
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
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
                            items(items.size, key = { items[it].id }) { idx ->
                                val tx = items[idx]
                                DebtDetailCard(
                                    tx = tx,
                                    categories = categories,
                                    isArabic = isArabic,
                                    onToggleSettled = { pendingPayTx = tx },
                                    onReminderClick = { reminderEditTx = tx },
                                    onDeleteClick = { pendingDeleteTx = tx }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingPayTx?.let { tx ->
        val payingNow = !tx.isDebtSettled
        val txSym = currencySymbol(tx.currency, isArabic)
        val itemLabel = tx.itemName.ifBlank { tx.category }
        SecurityVerificationDialog(
            title = if (payingNow) {
                tr(isArabic, "تأكيد دفع المديونية", "Confirm Debt Payment")
            } else {
                tr(isArabic, "إعادة كدين قائم", "Reopen Debt")
            },
            description = if (payingNow) {
                tr(
                    isArabic,
                    "سيتم سداد \"$itemLabel\" بقيمة ${formatAmountMinor(tx.amountMinor)} $txSym وإزالتها من المديونيات ونقلها مباشرة إلى قسم المدفوعات.",
                    "Pay \"$itemLabel\" (${formatAmountMinor(tx.amountMinor)} $txSym), remove from active debts, and move to Payments."
                )
            } else {
                tr(
                    isArabic,
                    "هل تريد إعادة \"$itemLabel\" إلى المديونيات القائمة؟",
                    "Move \"$itemLabel\" back to active unpaid debts?"
                )
            },
            confirmLabel = if (payingNow) tr(isArabic, "تأكيد الدفع", "Confirm Pay") else tr(isArabic, "إعادة", "Reopen"),
            isDanger = false,
            requireSecurity = settings.confirmPaymentWithSecurity,
            settings = settings,
            isArabic = isArabic,
            onConfirmed = {
                onToggleSettled(tx.id, payingNow)
                pendingPayTx = null
            },
            onDismiss = { pendingPayTx = null }
        )
    }

    pendingDeleteTx?.let { tx ->
        SecurityVerificationDialog(
            title = tr(isArabic, "حذف المديونية", "Delete Debt"),
            description = tr(
                isArabic,
                "هل تريد حذف \"${tx.itemName.ifBlank { tx.category }}\" نهائياً من قاعدة البيانات والتقارير؟",
                "Delete \"${tx.itemName.ifBlank { tx.category }}\" permanently from database and reports?"
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

    reminderEditTx?.let { tx ->
        ReminderSettingsDialog(
            itemTitle = tx.itemName.ifBlank { tx.category },
            amountDisplay = "${formatAmountMinor(tx.amountMinor)} ${currencySymbol(tx.currency, isArabic)}",
            partyName = tx.debtParty,
            initialConfig = ItemReminderConfig(
                enabled = tx.hasPaymentReminder,
                dueDate = tx.paymentDueDate.ifBlank { currentIsoDate() },
                dueTime = tx.paymentDueTime.ifBlank { "09:00" },
                leadMinutes = tx.reminderLeadMinutes,
                muted = tx.reminderMuted,
                alertMode = tx.reminderAlertMode,
                fullScreen = tx.reminderFullScreen,
                speakAloud = tx.reminderSpeakAloud
            ),
            settings = settings,
            isArabic = isArabic,
            onSave = { cfg ->
                if (cfg.enabled) onRequestNotificationPermission()
                onUpdateTransactionReminder(tx.id, cfg)
                reminderEditTx = null
            },
            onDismiss = { reminderEditTx = null }
        )
    }

    reminderEditObligation?.let { ob ->
        ReminderSettingsDialog(
            itemTitle = ob.reason,
            amountDisplay = "${formatAmountMinor(ob.amountMinor)} ${currencySymbol(ob.currency, isArabic)}",
            partyName = ob.party,
            initialConfig = ItemReminderConfig(
                enabled = ob.reminderEnabled && !ob.reminderMuted,
                dueDate = ob.dueDate.ifBlank { currentIsoDate() },
                dueTime = ob.dueTime.ifBlank { "09:00" },
                leadMinutes = ob.reminderLeadMinutes,
                muted = ob.reminderMuted,
                alertMode = ob.reminderAlertMode,
                fullScreen = ob.reminderFullScreen,
                speakAloud = ob.reminderSpeakAloud
            ),
            settings = settings,
            isArabic = isArabic,
            onSave = { cfg ->
                if (cfg.enabled) onRequestNotificationPermission()
                onUpdateObligationReminder(ob.id, cfg)
                reminderEditObligation = null
            },
            onDismiss = { reminderEditObligation = null }
        )
    }
}

@Composable
private fun DebtHeroMetricChip(
    modifier: Modifier,
    label: String,
    value: String,
    accent: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = AmountBodyStyle.copy(
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DebtDetailCard(
    tx: TransactionEntity,
    categories: List<CategoryEntity>,
    isArabic: Boolean,
    onToggleSettled: () -> Unit,
    onReminderClick: () -> Unit = {},
    onDeleteClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    val isIncomeDebt = tx.type == TransactionType.INCOME
    val sym = currencySymbol(tx.currency, isArabic)
    val iconKey = categories.firstOrNull { it.name == tx.category }?.iconKey
        ?: if (isIncomeDebt) "payments" else "category"
    val itemTitle = tx.itemName.ifBlank { tx.note.ifBlank { tx.category } }
    val typeLabel = if (isIncomeDebt) {
        tr(isArabic, "مديونية إيرادات دخلت لحسابي", "Income Entered Account")
    } else {
        tr(isArabic, "مديونية مصروفات", "Expense Debt")
    }
    val typeAccent = if (isIncomeDebt) ext.warningAmber else ext.expenseColor
    val typeBg = if (isIncomeDebt) ext.warningAmber.copy(alpha = 0.14f) else ext.expenseContainer

    SereneCard(
        modifier = Modifier.testTag("debt_card_${tx.id}"),
        borderColor = if (tx.isDebtSettled) ext.incomeColor.copy(alpha = 0.45f) else ext.cardBorder.copy(alpha = 0.8f),
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
                    .background(if (tx.isDebtSettled) ext.incomeContainer else typeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (tx.isDebtSettled) Icons.Outlined.CheckCircle else iconForKey(iconKey),
                    contentDescription = tx.category,
                    tint = if (tx.isDebtSettled) ext.incomeColor else typeAccent,
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Item Name (اسم الحاجة)
                Text(
                    text = itemTitle,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (tx.isDebtSettled) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (tx.isDebtSettled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Badges Row: Type (مصروف/إيراد بالمديونية) + Category (التصنيف)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeBg)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = typeAccent,
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
                            text = tx.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Meta Row: Source (المصدر) + Creditor (الدائن) + Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = tx.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    if (tx.debtParty.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = tx.debtParty,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = formatDisplayDateTime(tx.date, tx.time, isArabic),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Amount Column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatAmountMinor(tx.amountMinor)} $sym",
                    style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                    color = if (tx.isDebtSettled) ext.incomeColor else typeAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (tx.isDebtSettled) {
                        tr(isArabic, "تم السداد", "Settled")
                    } else {
                        tr(isArabic, "دين قائم", "Unpaid")
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (tx.isDebtSettled) ext.incomeColor else ext.expenseColor
                )
            }
        }

        // Optional Note if distinct from itemTitle
        if (tx.note.isNotBlank() && tx.note != itemTitle) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tx.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Row: Mark Settled / Reopen + Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("pay_debt_${tx.id}")
                    .clickable(onClick = onToggleSettled),
                shape = RoundedCornerShape(10.dp),
                color = if (tx.isDebtSettled) ext.cardSubtleBackground else ext.incomeColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (tx.isDebtSettled) Icons.Outlined.Undo else Icons.Outlined.Payments,
                        contentDescription = null,
                        tint = if (tx.isDebtSettled) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (tx.isDebtSettled) {
                            tr(isArabic, "إرجاع للمطلوب دفعه قريباً", "Move to Upcoming")
                        } else {
                            tr(isArabic, "تم الدفع", "Mark Paid")
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (tx.isDebtSettled) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!tx.isDebtSettled) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .testTag("debt_tx_reminder_btn_${tx.id}")
                            .clickable(onClick = onReminderClick),
                        shape = RoundedCornerShape(9.dp),
                        color = if (tx.hasPaymentReminder) ext.incomeContainer else ext.cardSubtleBackground,
                        border = BorderStroke(
                            1.dp,
                            if (tx.hasPaymentReminder) ext.incomeColor.copy(alpha = 0.4f) else ext.cardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = tr(isArabic, "إعدادات التنبيه", "Alert settings"),
                                tint = if (tx.hasPaymentReminder) ext.incomeColor else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (tx.hasPaymentReminder && tx.paymentDueDate.isNotBlank()) {
                                    tr(isArabic, "تنبيه (${tx.paymentDueDate})", "Alert (${tx.paymentDueDate})")
                                } else {
                                    tr(isArabic, "ضبط ميعاد", "Set Due")
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (tx.hasPaymentReminder) ext.incomeColor else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
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

@Composable
private fun DebtFilterChip(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) ext.heroCardBackground else ext.cardSubtleBackground
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DebtCompactPill(
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
private fun DebtTypeTabCard(
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
                .padding(horizontal = 8.dp, vertical = 9.dp),
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
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
