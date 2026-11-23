package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppSettingsEntity
import com.example.data.ObligationEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.CollapsibleFilterDropdownCard
import com.example.ui.components.DropdownFilterOption
import com.example.ui.components.ExportDownloadButton
import com.example.ui.components.ItemReminderConfig
import com.example.ui.components.ReminderSettingsDialog
import com.example.ui.components.SereneCard
import com.example.ui.components.SmartNoteFilterDropdown
import com.example.ui.components.SmartSingleSelectDropdown
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.formatDisplayDateTime
import com.example.util.formatTime12Hour
import com.example.util.tr

enum class FlowDrillDownKind {
    ALL_INCOMING_BY_WAY,
    SINGLE_INCOMING_WAY,
    ALL_INCOMING_BY_CHANNEL,
    SINGLE_INCOMING_CHANNEL,
    ALL_OUTGOING_BY_DESTINATION,
    SINGLE_OUTGOING_DESTINATION,
    ALL_OUTGOING_BY_CHANNEL,
    SINGLE_OUTGOING_CHANNEL,
    EARNED_INCOME,
    BORROWED_INCOME_TO_REPAY
}

data class FlowDrillDownSpec(
    val kind: FlowDrillDownKind,
    val title: String,
    val subtitle: String = "",
    val filterValue: String = "",
    val currency: String = "EGP"
)

/**
 * Unified Drill-Down Operations Page inside Reports / «شرح فلوسك فين».
 * Displays all operations for any clicked section header, individual category/way,
 * individual receiving/payment channel, or the two HomeScreen incoming tabs,
 * complete with collapsible dropdown filters (>5 searchable rule) and formatted Export & Download.
 */
@Composable
fun FlowDrillDownScreen(
    spec: FlowDrillDownSpec,
    transactions: List<TransactionEntity>,
    obligations: List<ObligationEntity>,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onUpdateTransactionReminder: (Long, ItemReminderConfig) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val ext = LocalSereneColors.current
    val isIncomingScope = remember(spec.kind) {
        spec.kind in setOf(
            FlowDrillDownKind.ALL_INCOMING_BY_WAY,
            FlowDrillDownKind.SINGLE_INCOMING_WAY,
            FlowDrillDownKind.ALL_INCOMING_BY_CHANNEL,
            FlowDrillDownKind.SINGLE_INCOMING_CHANNEL,
            FlowDrillDownKind.EARNED_INCOME,
            FlowDrillDownKind.BORROWED_INCOME_TO_REPAY
        )
    }

    var searchQuery by remember(spec) { mutableStateOf("") }
    var selectedCategory by remember(spec) { mutableStateOf<String?>(null) }
    var selectedChannel by remember(spec) { mutableStateOf<String?>(null) }
    var selectedParty by remember(spec) { mutableStateOf<String?>(null) }
    var selectedItem by remember(spec) { mutableStateOf<String?>(null) }
    var noteFilterQuery by remember(spec) { mutableStateOf("") }
    var selectedCurrency by remember(spec) { mutableStateOf(spec.currency) }
    var reminderTargetTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val baseTransactions = remember(transactions, spec, isArabic) {
        val defaultCash = tr(isArabic, "كاش / نقدي", "Cash")
        val defaultCat = tr(isArabic, "غير محدد", "Unspecified")
        transactions.filter { tx ->
            val cleanCat = tx.category.ifBlank { defaultCat }
            val cleanSrc = tx.source.ifBlank { defaultCash }
            when (spec.kind) {
                FlowDrillDownKind.ALL_INCOMING_BY_WAY,
                FlowDrillDownKind.ALL_INCOMING_BY_CHANNEL ->
                    tx.type == TransactionType.INCOME

                FlowDrillDownKind.SINGLE_INCOMING_WAY ->
                    tx.type == TransactionType.INCOME && cleanCat == spec.filterValue

                FlowDrillDownKind.SINGLE_INCOMING_CHANNEL ->
                    tx.type == TransactionType.INCOME && cleanSrc == spec.filterValue

                FlowDrillDownKind.ALL_OUTGOING_BY_DESTINATION,
                FlowDrillDownKind.ALL_OUTGOING_BY_CHANNEL ->
                    (tx.type == TransactionType.EXPENSE && !tx.isDebt) || (tx.isDebt && tx.isDebtSettled)

                FlowDrillDownKind.SINGLE_OUTGOING_DESTINATION ->
                    ((tx.type == TransactionType.EXPENSE && !tx.isDebt) || (tx.isDebt && tx.isDebtSettled)) &&
                        cleanCat == spec.filterValue

                FlowDrillDownKind.SINGLE_OUTGOING_CHANNEL ->
                    ((tx.type == TransactionType.EXPENSE && !tx.isDebt) || (tx.isDebt && tx.isDebtSettled)) &&
                        cleanSrc == spec.filterValue

                FlowDrillDownKind.EARNED_INCOME ->
                    tx.type == TransactionType.INCOME && !tx.isDebt

                FlowDrillDownKind.BORROWED_INCOME_TO_REPAY ->
                    tx.type == TransactionType.INCOME && tx.isDebt && !tx.isDebtSettled
            }
        }.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.time }.thenByDescending { it.createdAt })
    }

    val matchingPaidObligations = remember(obligations, spec, isArabic) {
        if (isIncomingScope) emptyList()
        else {
            val defaultCash = tr(isArabic, "كاش / نقدي", "Cash")
            val obCat = tr(isArabic, "التزام مجدول مدفوع", "Paid Obligation")
            obligations.filter { ob ->
                if (!ob.isPaid) return@filter false
                val obSrc = ob.paidSource.ifBlank { defaultCash }
                when (spec.kind) {
                    FlowDrillDownKind.ALL_OUTGOING_BY_DESTINATION,
                    FlowDrillDownKind.ALL_OUTGOING_BY_CHANNEL -> true
                    FlowDrillDownKind.SINGLE_OUTGOING_DESTINATION ->
                        ob.reason.ifBlank { obCat } == spec.filterValue
                    FlowDrillDownKind.SINGLE_OUTGOING_CHANNEL ->
                        obSrc == spec.filterValue
                    else -> false
                }
            }
        }
    }

    val availableCurrencies = remember(baseTransactions, matchingPaidObligations, spec.currency) {
        val list = (baseTransactions.map { it.currency } + matchingPaidObligations.map { it.currency })
            .distinct()
            .sorted()
        if (list.isEmpty()) listOf(spec.currency)
        else if (spec.currency in list) listOf(spec.currency) + list.filter { it != spec.currency }
        else list
    }
    val validCurrency = if (selectedCurrency in availableCurrencies) selectedCurrency else availableCurrencies.first()
    val sym = currencySymbol(validCurrency, isArabic)

    val currencyTx = remember(baseTransactions, validCurrency) {
        baseTransactions.filter { it.currency == validCurrency }
    }
    val currencyObs = remember(matchingPaidObligations, validCurrency) {
        matchingPaidObligations.filter { it.currency == validCurrency }
    }

    val availableCategories = remember(currencyTx) {
        currencyTx.map { it.category.ifBlank { "غير محدد" } }.distinct().sorted()
    }
    val availableChannels = remember(currencyTx, currencyObs, isArabic) {
        val defCash = tr(isArabic, "كاش / نقدي", "Cash")
        (currencyTx.map { it.source.ifBlank { defCash } } + currencyObs.map { it.paidSource.ifBlank { defCash } })
            .distinct()
            .sorted()
    }
    val availableParties = remember(currencyTx, currencyObs) {
        (currencyTx.map { it.debtParty.trim() } + currencyObs.map { it.party.trim() })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val availableItems = remember(currencyTx, currencyObs) {
        (currencyTx.map { it.itemName.trim() } + currencyObs.map { it.reason.trim() })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val availableNotes = remember(currencyTx, currencyObs) {
        (currencyTx.map { it.note.trim() } + currencyObs.map { it.note.trim() })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredTx = remember(
        currencyTx,
        searchQuery,
        selectedCategory,
        selectedChannel,
        selectedParty,
        selectedItem,
        noteFilterQuery
    ) {
        val q = searchQuery.trim().lowercase()
        val nq = noteFilterQuery.trim()
        currencyTx.filter { tx ->
            if (selectedCategory != null && tx.category != selectedCategory) return@filter false
            if (selectedChannel != null && tx.source != selectedChannel) return@filter false
            if (selectedParty != null && tx.debtParty.trim() != selectedParty) return@filter false
            if (selectedItem != null && tx.itemName.trim() != selectedItem) return@filter false
            if (nq.isNotEmpty() && !tx.note.contains(nq, ignoreCase = true)) return@filter false
            if (q.isNotEmpty()) {
                val hay = "${tx.itemName} ${tx.debtParty} ${tx.category} ${tx.source} ${tx.note} ${tx.date} ${formatAmountMinor(tx.amountMinor, false)}".lowercase()
                if (!hay.contains(q)) return@filter false
            }
            true
        }
    }

    val filteredObs = remember(
        currencyObs,
        searchQuery,
        selectedChannel,
        selectedParty,
        selectedItem,
        noteFilterQuery
    ) {
        val q = searchQuery.trim().lowercase()
        val nq = noteFilterQuery.trim()
        currencyObs.filter { ob ->
            if (selectedChannel != null && ob.paidSource != selectedChannel) return@filter false
            if (selectedParty != null && ob.party.trim() != selectedParty) return@filter false
            if (selectedItem != null && ob.reason.trim() != selectedItem) return@filter false
            if (nq.isNotEmpty() && !ob.note.contains(nq, ignoreCase = true)) return@filter false
            if (q.isNotEmpty()) {
                val hay = "${ob.reason} ${ob.party} ${ob.paidSource} ${ob.note} ${ob.paidDate} ${formatAmountMinor(ob.amountMinor, false)}".lowercase()
                if (!hay.contains(q)) return@filter false
            }
            true
        }
    }

    val totalAmountMinor = remember(filteredTx, filteredObs) {
        filteredTx.sumOf { it.amountMinor } + filteredObs.sumOf { it.amountMinor }
    }
    val activeFilterCount = listOf(
        searchQuery.isNotBlank(),
        selectedCategory != null,
        selectedChannel != null,
        selectedParty != null,
        selectedItem != null,
        noteFilterQuery.isNotBlank()
    ).count { it }
    val accent = when (spec.kind) {
        FlowDrillDownKind.BORROWED_INCOME_TO_REPAY -> ext.warningAmber
        else -> if (isIncomingScope) ext.incomeColor else ext.expenseColor
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("flow_drilldown_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Back Header + Hero Total
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ext.heroCardBackground,
                    shadowElevation = if (ext.isDark) 0.dp else 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .testTag("drilldown_back_btn")
                                    .clickable(onClick = onBack),
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = tr(isArabic, "رجوع", "Back"),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spec.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ext.heroCardOnBackground,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (spec.subtitle.isNotBlank()) {
                                    Text(
                                        text = spec.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.heroCardSubtext,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = formatAmountMinor(totalAmountMinor),
                                    style = AmountDisplayStyle,
                                    color = if (isIncomingScope) Color(0xFF82F5C1) else Color(0xFFFFB4AB),
                                    maxLines = 1
                                )
                                Text(
                                    text = sym,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = ext.heroCardSubtext,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tr(
                                        isArabic,
                                        "${filteredTx.size + filteredObs.size} عملية",
                                        "${filteredTx.size + filteredObs.size} items"
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 2. Formatted Export & Download Bar
            item {
                val exportSummary = remember(
                    spec.title,
                    activeFilterCount,
                    selectedCategory,
                    selectedChannel,
                    selectedParty,
                    selectedItem,
                    noteFilterQuery,
                    searchQuery,
                    filteredTx.size,
                    filteredObs.size,
                    isArabic
                ) {
                    val totalItems = filteredTx.size + filteredObs.size
                    if (activeFilterCount == 0) {
                        tr(
                            isArabic,
                            "${spec.title} (الكل بدون فلاتر) • $totalItems عملية",
                            "${spec.title} (All, no filters) • $totalItems items"
                        )
                    } else {
                        val parts = mutableListOf(spec.title)
                        selectedCategory?.let { parts += tr(isArabic, "التصنيف: $it", "Category: $it") }
                        selectedChannel?.let { parts += tr(isArabic, "الحساب: $it", "Account: $it") }
                        selectedParty?.let { parts += tr(isArabic, "الجهة: $it", "Party: $it") }
                        selectedItem?.let { parts += tr(isArabic, "عشان إيه: $it", "For: $it") }
                        if (noteFilterQuery.isNotBlank()) parts += tr(isArabic, "الملاحظة: ${noteFilterQuery.trim()}", "Note: ${noteFilterQuery.trim()}")
                        if (searchQuery.isNotBlank()) parts += tr(isArabic, "بحث: ${searchQuery.trim()}", "Search: ${searchQuery.trim()}")
                        parts += tr(isArabic, "$totalItems عملية مطابقة", "$totalItems matching")
                        parts.joinToString(" • ")
                    }
                }
                ExportDownloadButton(
                    transactions = filteredTx,
                    obligations = filteredObs,
                    preferredCurrency = validCurrency,
                    reportTitle = spec.title,
                    filterSummary = exportSummary,
                    isArabic = isArabic,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "drilldown_export_btn"
                )
            }

            // 3. Collapsible Dropdown Filters (All filters in searchable dropdowns)
            item {
                val partyOpts = remember(availableParties, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل الجهات والأشخاص", "All Parties"))) +
                        availableParties.map { DropdownFilterOption<String?>(it, it) }
                }
                val itemOpts = remember(availableItems, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل البنود والأسباب", "All Items"))) +
                        availableItems.map { DropdownFilterOption<String?>(it, it) }
                }
                CollapsibleFilterDropdownCard(
                    isArabic = isArabic,
                    activeFilterCount = activeFilterCount,
                    onResetFilters = if (activeFilterCount > 0) {
                        {
                            searchQuery = ""
                            selectedCategory = null
                            selectedChannel = null
                            selectedParty = null
                            selectedItem = null
                            noteFilterQuery = ""
                        }
                    } else null,
                    testTagPrefix = "drilldown_filters"
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        placeholder = {
                            Text(
                                text = tr(isArabic, "بحث سريع بالاسم، الجهة، التصنيف، الملاحظة، أو المبلغ...", "Quick search item, party, category, note, amount..."),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ext.cardSubtleBackground,
                            unfocusedContainerColor = ext.cardSubtleBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val catOpts = listOf(DropdownFilterOption<String?>(null, tr(isArabic, "الكل", "All"))) +
                            availableCategories.map { DropdownFilterOption<String?>(it, it) }
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "التصنيف / السبب", "Category / Way"),
                            selectedValue = selectedCategory,
                            options = catOpts,
                            isArabic = isArabic,
                            onSelected = { selectedCategory = it },
                            isHighlighted = selectedCategory != null,
                            testTag = "drilldown_category_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                        val chOpts = listOf(DropdownFilterOption<String?>(null, tr(isArabic, "الكل", "All"))) +
                            availableChannels.map { DropdownFilterOption<String?>(it, it) }
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "الحساب / الطريقة", "Account / Channel"),
                            selectedValue = selectedChannel,
                            options = chOpts,
                            isArabic = isArabic,
                            onSelected = { selectedChannel = it },
                            isHighlighted = selectedChannel != null,
                            testTag = "drilldown_channel_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmartSingleSelectDropdown(
                            label = if (isIncomingScope) tr(isArabic, "من مين / الجهة", "From / Party")
                            else tr(isArabic, "اندفعت لمين / الجهة", "Paid To / Party"),
                            selectedValue = selectedParty,
                            options = partyOpts,
                            isArabic = isArabic,
                            onSelected = { selectedParty = it },
                            isHighlighted = selectedParty != null,
                            testTag = "drilldown_party_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "عشان إيه / البند", "What For / Item"),
                            selectedValue = selectedItem,
                            options = itemOpts,
                            isArabic = isArabic,
                            onSelected = { selectedItem = it },
                            isHighlighted = selectedItem != null,
                            testTag = "drilldown_item_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SmartNoteFilterDropdown(
                        label = tr(isArabic, "الملاحظة الإضافية", "Additional Note"),
                        noteQuery = noteFilterQuery,
                        availableNotes = availableNotes,
                        isArabic = isArabic,
                        onNoteQueryChange = { noteFilterQuery = it },
                        testTag = "drilldown_note_dropdown",
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (availableCurrencies.size > 1) {
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "العملة", "Currency"),
                            selectedValue = validCurrency,
                            options = availableCurrencies.map { c ->
                                DropdownFilterOption(c, "${currencySymbol(c, isArabic)} ($c)")
                            },
                            isArabic = isArabic,
                            onSelected = { selectedCurrency = it },
                            testTag = "drilldown_currency_dropdown",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 4. Operations List
            if (filteredTx.isEmpty() && filteredObs.isEmpty()) {
                item {
                    SereneCard(contentPadding = PaddingValues(24.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = accent, modifier = Modifier.size(34.dp))
                            Text(
                                text = tr(isArabic, "لا توجد عمليات مطابقة في هذا القسم حالياً", "No matching operations in this section"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            } else {
                items(filteredTx, key = { "dd_tx_${it.id}" }) { tx ->
                    val txSym = currencySymbol(tx.currency, isArabic)
                    val isInc = tx.type == TransactionType.INCOME
                    val rowAccent = when {
                        tx.isDebt && !tx.isDebtSettled -> ext.warningAmber
                        isInc -> ext.incomeColor
                        else -> ext.expenseColor
                    }
                    SereneCard(
                        contentPadding = PaddingValues(13.dp),
                        borderColor = rowAccent.copy(alpha = 0.28f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = tx.itemName.ifBlank { tx.category },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${tx.category} • ${tx.source.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (tx.debtParty.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Outlined.Person, contentDescription = null, tint = rowAccent, modifier = Modifier.size(13.dp))
                                        Text(
                                            text = tx.debtParty,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = rowAccent
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = formatDisplayDateTime(tx.date, tx.time, isArabic),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (tx.isDebt && !tx.isDebtSettled && tx.paymentDueDate.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = ext.warningAmber, modifier = Modifier.size(13.dp))
                                        Text(
                                            text = tr(
                                                isArabic,
                                                "موعد السداد: ${tx.paymentDueDate} • ${formatTime12Hour(tx.paymentDueTime.ifBlank { "09:00" }, true)}",
                                                "Due: ${tx.paymentDueDate} • ${formatTime12Hour(tx.paymentDueTime.ifBlank { "09:00" }, false)}"
                                            ),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = ext.warningAmber
                                        )
                                    }
                                }
                                if (tx.note.isNotBlank()) {
                                    Text(
                                        text = tx.note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${if (isInc) "+" else "-"}${formatAmountMinor(tx.amountMinor)} $txSym",
                                    style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                                    color = rowAccent
                                )
                                if (tx.isDebt && !tx.isDebtSettled) {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { reminderTargetTx = tx },
                                        shape = RoundedCornerShape(8.dp),
                                        color = ext.warningAmber.copy(alpha = 0.14f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Outlined.Tune, contentDescription = null, tint = ext.warningAmber, modifier = Modifier.size(13.dp))
                                            Text(
                                                text = tr(isArabic, "تنبيه السداد", "Alert"),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ext.warningAmber
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(filteredObs, key = { "dd_ob_${it.id}" }) { ob ->
                    val obSym = currencySymbol(ob.currency, isArabic)
                    SereneCard(
                        contentPadding = PaddingValues(13.dp),
                        borderColor = ext.expenseColor.copy(alpha = 0.25f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = ob.reason,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${tr(isArabic, "التزام مدفوع", "Paid Obligation")} • ${ob.paidSource.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (ob.party.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Outlined.Person, contentDescription = null, tint = ext.expenseColor, modifier = Modifier.size(13.dp))
                                        Text(
                                            text = ob.party,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = ext.expenseColor
                                        )
                                    }
                                }
                                Text(
                                    text = "${ob.paidDate.ifBlank { ob.dueDate }} • ${formatTime12Hour(ob.dueTime, isArabic)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (ob.note.isNotBlank()) {
                                    Text(
                                        text = ob.note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "-${formatAmountMinor(ob.amountMinor)} $obSym",
                                style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = ext.expenseColor
                            )
                        }
                    }
                }
            }
        }
    }

    reminderTargetTx?.let { tx ->
        val txSym = currencySymbol(tx.currency, isArabic)
        ReminderSettingsDialog(
            itemTitle = tx.itemName.ifBlank { tx.category },
            amountDisplay = "${formatAmountMinor(tx.amountMinor)} $txSym",
            partyName = tx.debtParty,
            initialConfig = ItemReminderConfig(
                enabled = tx.hasPaymentReminder,
                dueDate = tx.paymentDueDate.ifBlank { tx.date },
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
                onUpdateTransactionReminder(tx.id, cfg)
                reminderTargetTx = null
            },
            onDismiss = { reminderTargetTx = null }
        )
    }
}
