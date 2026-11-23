package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AccountEntity
import com.example.data.AppSettingsEntity
import com.example.data.ObligationEntity
import com.example.data.TransactionEntity
import com.example.ui.ReportAnalytics
import com.example.ui.components.CollapsibleFilterDropdownCard
import com.example.ui.components.DropdownFilterOption
import com.example.ui.components.ExportDownloadButton
import com.example.ui.components.FastTimePickerDialog
import com.example.ui.components.ItemReminderConfig
import com.example.ui.components.ReminderSettingsDialog
import com.example.ui.components.SecurityVerificationDialog
import com.example.ui.components.SereneCard
import com.example.ui.components.SmartNoteFilterDropdown
import com.example.ui.components.SmartSingleSelectDropdown
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.PaymentAlertModeOptions
import com.example.util.PaymentReminderLeadOptions
import com.example.util.SupportedCurrencies
import com.example.util.currencySymbol
import com.example.util.currentIsoDate
import com.example.util.formatAlertModeLabel
import com.example.util.formatAmountMinor
import com.example.util.formatReminderLeadTime
import com.example.util.formatTime12Hour
import com.example.util.parseAmountToMinor
import com.example.util.tr
import java.util.Calendar
import java.util.Locale

private enum class ObligationFilterTab { PENDING, ALL, PAID }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ObligationsScreen(
    obligations: List<ObligationEntity>,
    transactions: List<TransactionEntity> = emptyList(),
    accounts: List<AccountEntity>,
    analytics: ReportAnalytics,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onAddObligation: (
        amountMinor: Long,
        currency: String,
        reason: String,
        party: String,
        dueDate: String,
        dueTime: String,
        reminderEnabled: Boolean,
        reminderLeadMinutes: Int,
        note: String,
        reminderAlertMode: String,
        reminderFullScreen: Boolean,
        reminderSpeakAloud: Boolean
    ) -> Unit,
    onSetObligationPaid: (id: Long, paid: Boolean, paidSource: String) -> Unit,
    onSetObligationReminderMuted: (id: Long, muted: Boolean) -> Unit,
    onUpdateObligationReminder: (id: Long, config: ItemReminderConfig) -> Unit = { _, _ -> },
    onUpdateTransactionReminder: (txId: Long, config: ItemReminderConfig) -> Unit = { _, _ -> },
    onSetTransactionDebtSettled: (txId: Long, settled: Boolean) -> Unit = { _, _ -> },
    onDeleteObligation: (id: Long) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val ext = LocalSereneColors.current
    val todayIso = remember { currentIsoDate() }

    var activeTab by remember { mutableStateOf(ObligationFilterTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCurrency by remember(settings.defaultCurrency) { mutableStateOf(settings.defaultCurrency) }
    var selectedFilterCurrency by remember { mutableStateOf<String?>(null) }
    var selectedParty by remember { mutableStateOf<String?>(null) }
    var selectedItem by remember { mutableStateOf<String?>(null) }
    var noteFilterQuery by remember { mutableStateOf("") }
    var showAddModal by remember { mutableStateOf(false) }
    var reminderEditObligation by remember { mutableStateOf<ObligationEntity?>(null) }
    var reminderEditTx by remember { mutableStateOf<TransactionEntity?>(null) }

    var pendingPayTarget by remember { mutableStateOf<ObligationEntity?>(null) }
    var selectedPaySource by remember(settings.lastSelectedSource) {
        mutableStateOf(settings.lastSelectedSource.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") })
    }
    var pendingSecurityPayTarget by remember { mutableStateOf<Pair<ObligationEntity, String>?>(null) }
    var pendingDeleteTarget by remember { mutableStateOf<ObligationEntity?>(null) }

    val availableCurrencies = remember(analytics.currencySummaries, obligations, transactions, settings.defaultCurrency) {
        val list = (analytics.currencySummaries.map { it.currency } +
            obligations.map { it.currency } +
            transactions.filter { it.isDebt && it.hasPaymentReminder && it.paymentDueDate.isNotBlank() }.map { it.currency })
            .distinct()
        if (list.isEmpty()) listOf(settings.defaultCurrency) else list
    }
    val effectiveCurrency = remember(selectedCurrency, availableCurrencies) {
        if (selectedCurrency in availableCurrencies) selectedCurrency else availableCurrencies.first()
    }
    val summary = remember(analytics.currencySummaries, effectiveCurrency) {
        analytics.currencySummaries.firstOrNull { it.currency == effectiveCurrency }
            ?: analytics.primarySummary
    }
    val sym = currencySymbol(summary.currency.ifBlank { settings.defaultCurrency }, isArabic)

    // Scheduled unpaid/paid transactions that explicitly have a payment reminder & due date set
    val scheduledTransactions = remember(transactions) {
        transactions.filter { it.isDebt && it.hasPaymentReminder && it.paymentDueDate.isNotBlank() }
            .sortedWith(compareBy<TransactionEntity> { it.isDebtSettled }.thenBy { it.paymentDueDate.ifBlank { "9999-99-99" } }.thenBy { it.paymentDueTime })
    }

    val pendingScheduledTxMinor = remember(scheduledTransactions, effectiveCurrency) {
        scheduledTransactions
            .filter { !it.isDebtSettled && it.currency == effectiveCurrency }
            .sumOf { it.amountMinor }
    }
    val pendingCount = remember(obligations) {
        obligations.count { !it.isPaid }
    }
    val paidCount = remember(obligations) {
        obligations.count { it.isPaid }
    }

    val availableParties = remember(obligations, scheduledTransactions) {
        (obligations.map { it.party } + scheduledTransactions.map { it.debtParty })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val availableItems = remember(obligations, scheduledTransactions) {
        (obligations.map { it.reason } + scheduledTransactions.map { it.itemName })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val availableNotes = remember(obligations, scheduledTransactions) {
        (obligations.map { it.note } + scheduledTransactions.map { it.note })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredObligations = remember(
        obligations,
        activeTab,
        searchQuery,
        selectedFilterCurrency,
        selectedParty,
        selectedItem,
        noteFilterQuery
    ) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        val nq = noteFilterQuery.trim()
        obligations.filter { ob ->
            val tabOk = when (activeTab) {
                ObligationFilterTab.PENDING -> !ob.isPaid
                ObligationFilterTab.PAID -> ob.isPaid
                ObligationFilterTab.ALL -> true
            }
            if (!tabOk) return@filter false
            if (selectedFilterCurrency != null && ob.currency != selectedFilterCurrency) return@filter false
            if (selectedParty != null && ob.party.trim() != selectedParty) return@filter false
            if (selectedItem != null && ob.reason.trim() != selectedItem) return@filter false
            if (nq.isNotEmpty() && !ob.note.contains(nq, ignoreCase = true)) return@filter false
            if (q.isEmpty()) return@filter true
            ob.reason.lowercase(Locale.getDefault()).contains(q) ||
                ob.party.lowercase(Locale.getDefault()).contains(q) ||
                ob.note.lowercase(Locale.getDefault()).contains(q) ||
                ob.dueDate.contains(q)
        }
    }

    val filteredScheduledTx = remember(
        scheduledTransactions,
        activeTab,
        searchQuery,
        selectedFilterCurrency,
        selectedParty,
        selectedItem,
        noteFilterQuery
    ) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        val nq = noteFilterQuery.trim()
        scheduledTransactions.filter { tx ->
            val tabOk = when (activeTab) {
                ObligationFilterTab.PENDING -> !tx.isDebtSettled
                ObligationFilterTab.PAID -> tx.isDebtSettled
                ObligationFilterTab.ALL -> true
            }
            if (!tabOk) return@filter false
            if (selectedFilterCurrency != null && tx.currency != selectedFilterCurrency) return@filter false
            if (selectedParty != null && tx.debtParty.trim() != selectedParty) return@filter false
            if (selectedItem != null && tx.itemName.trim() != selectedItem) return@filter false
            if (nq.isNotEmpty() && !tx.note.contains(nq, ignoreCase = true)) return@filter false
            if (q.isEmpty()) return@filter true
            tx.itemName.lowercase(Locale.getDefault()).contains(q) ||
                tx.debtParty.lowercase(Locale.getDefault()).contains(q) ||
                tx.category.lowercase(Locale.getDefault()).contains(q) ||
                tx.note.lowercase(Locale.getDefault()).contains(q) ||
                tx.paymentDueDate.contains(q)
        }
    }

    val activeFiltersCount = listOf(
        activeTab != ObligationFilterTab.ALL,
        selectedFilterCurrency != null,
        selectedParty != null,
        selectedItem != null,
        noteFilterQuery.isNotBlank(),
        searchQuery.isNotBlank()
    ).count { it }

    val exportFilterSummary = remember(
        activeFiltersCount,
        activeTab,
        selectedFilterCurrency,
        selectedParty,
        selectedItem,
        noteFilterQuery,
        searchQuery,
        filteredObligations.size,
        filteredScheduledTx.size,
        isArabic
    ) {
        val totalMatched = filteredObligations.size + filteredScheduledTx.size
        if (activeFiltersCount == 0) {
            tr(
                isArabic,
                "كل الالتزامات المجدولة (بدون فلاتر) • $totalMatched التزام",
                "All scheduled obligations (no filters) • $totalMatched dues"
            )
        } else {
            val parts = mutableListOf<String>()
            when (activeTab) {
                ObligationFilterTab.PENDING -> parts += tr(isArabic, "الحالة: مطلوب دفعها قريباً", "Status: Upcoming")
                ObligationFilterTab.PAID -> parts += tr(isArabic, "الحالة: تم دفعها", "Status: Paid")
                ObligationFilterTab.ALL -> Unit
            }
            selectedFilterCurrency?.let { parts += tr(isArabic, "العملة: $it", "Currency: $it") }
            selectedParty?.let { parts += tr(isArabic, "الجهة: $it", "Party: $it") }
            selectedItem?.let { parts += tr(isArabic, "عشان إيه: $it", "For: $it") }
            if (noteFilterQuery.isNotBlank()) parts += tr(isArabic, "الملاحظة: ${noteFilterQuery.trim()}", "Note: ${noteFilterQuery.trim()}")
            if (searchQuery.isNotBlank()) parts += tr(isArabic, "بحث: ${searchQuery.trim()}", "Search: ${searchQuery.trim()}")
            parts += tr(isArabic, "$totalMatched التزام مطابق", "$totalMatched matching")
            parts.joinToString(" • ")
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Reconciled Hero Card for Pending Obligations & Free Available Balance
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("obligations_hero_card"),
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EventNote,
                                    contentDescription = null,
                                    tint = ext.warningAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tr(isArabic, "التزامات بمواعيد سداد", "Scheduled Payment Dues"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ext.heroCardSubtext
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = ext.warningAmber.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = tr(isArabic, "$pendingCount قادم • $paidCount مدفوع", "$pendingCount due • $paidCount paid"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatAmountMinor(summary.pendingScheduledObligationsMinor),
                                style = AmountDisplayStyle,
                                color = ext.heroCardOnBackground,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = sym,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = ext.heroCardSubtext,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        if (pendingScheduledTxMinor > 0L) {
                            Text(
                                text = tr(
                                    isArabic,
                                    "بالإضافة إلى ${formatAmountMinor(pendingScheduledTxMinor, false)} $sym مصاريف مؤجلة مفعّل لها تنبيه",
                                    "Plus ${formatAmountMinor(pendingScheduledTxMinor, false)} $sym deferred expenses with active alerts"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.heroCardSubtext
                            )
                        }

                        // Multi-currency selector if >1 currency exists
                        if (availableCurrencies.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableCurrencies.forEach { curr ->
                                    val selected = curr == effectiveCurrency
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable { selectedCurrency = curr },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (selected) Color.White else Color.White.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = curr,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (selected) ext.heroCardBackground else Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Reconciled Balance Impact Strip (Shows Available Without Obligations vs Cash Balance)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.09f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr(isArabic, "المتاح بدون التزامات", "Free Available"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.heroCardSubtext,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${formatAmountMinor(summary.availableWithoutObligationsMinor)} $sym",
                                        style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                                        color = if (summary.availableWithoutObligationsMinor >= 0L) Color(0xFF82F5C1) else Color(0xFFFF9B9B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = tr(isArabic, "الإجمالي (متاح + التزامات)", "Total (Free + Due)"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.heroCardSubtext,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${formatAmountMinor(summary.cashBalanceMinor)} $sym",
                                        style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                                        color = ext.heroCardOnBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Primary Add Obligation CTA + Export Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { showAddModal = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("obligations_add_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ext.incomeColor,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr(isArabic, "إضافة التزام بموعد سداد", "Add Scheduled Payment"),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            ExportDownloadButton(
                                transactions = filteredScheduledTx,
                                obligations = filteredObligations,
                                preferredCurrency = selectedFilterCurrency ?: effectiveCurrency,
                                reportTitle = tr(isArabic, "التزامات بمواعيد سداد", "Scheduled Payment Dues"),
                                filterSummary = exportFilterSummary,
                                isArabic = isArabic,
                                compactLabel = true,
                                testTag = "obligations_export_btn"
                            )
                        }
                    }
                }
            }

            // 2. Scientific Note Banner (Zero Double Deduction Guarantee)
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ext.incomeContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, ext.incomeColor.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = ext.incomeColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = tr(
                                isArabic,
                                "أي مبلغ تضيفه هنا يُحجز فوراً من «المتاح للصرف بدون التزامات»، وعند دفعه يُخصم من الرصيد النقدي الفعلي بدون أي تكرار في الخصم.",
                                "Amounts added here immediately reserve funds from Free Available Balance, and reconcile cleanly from Actual Cash when paid with zero double-deduction."
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 3. Collapsible Dropdown Filter Panel (All filters in searchable dropdowns)
            item {
                val statusLabel = when (activeTab) {
                    ObligationFilterTab.PENDING -> tr(isArabic, "قادمة ($pendingCount)", "Upcoming ($pendingCount)")
                    ObligationFilterTab.PAID -> tr(isArabic, "مدفوعة ($paidCount)", "Paid ($paidCount)")
                    ObligationFilterTab.ALL -> tr(isArabic, "الكل (${obligations.size + scheduledTransactions.size})", "All (${obligations.size + scheduledTransactions.size})")
                }
                val partyOptions = remember(availableParties, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل الجهات والأشخاص", "All Parties"))) +
                        availableParties.map { DropdownFilterOption<String?>(it, it) }
                }
                val itemOptions = remember(availableItems, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل البنود والأسباب", "All Reasons / Items"))) +
                        availableItems.map { DropdownFilterOption<String?>(it, it) }
                }
                val currencyOptions = remember(availableCurrencies, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل العملات", "All Currencies"))) +
                        availableCurrencies.map { DropdownFilterOption<String?>(it, "${currencySymbol(it, isArabic)} ($it)") }
                }

                CollapsibleFilterDropdownCard(
                    isArabic = isArabic,
                    activeFilterCount = activeFiltersCount,
                    onResetFilters = if (activeFiltersCount > 0) {
                        {
                            activeTab = ObligationFilterTab.ALL
                            selectedFilterCurrency = null
                            selectedParty = null
                            selectedItem = null
                            noteFilterQuery = ""
                            searchQuery = ""
                        }
                    } else null,
                    title = tr(isArabic, "فلترة وبحث المواعيد ($statusLabel)", "Filter & Search ($statusLabel)"),
                    testTagPrefix = "obligations_filter_dropdown"
                ) {
                    SmartSingleSelectDropdown(
                        label = tr(isArabic, "حالة السداد", "Payment Status"),
                        selectedValue = activeTab,
                        options = listOf(
                            DropdownFilterOption(ObligationFilterTab.ALL, tr(isArabic, "كل الالتزامات والمواعيد (${obligations.size + scheduledTransactions.size})", "All Scheduled (${obligations.size + scheduledTransactions.size})")),
                            DropdownFilterOption(ObligationFilterTab.PENDING, tr(isArabic, "مطلوب دفعها قريباً ($pendingCount)", "Upcoming Due ($pendingCount)")),
                            DropdownFilterOption(ObligationFilterTab.PAID, tr(isArabic, "تم دفعها ($paidCount)", "Paid ($paidCount)"))
                        ),
                        isArabic = isArabic,
                        onSelected = { activeTab = it },
                        isHighlighted = activeTab != ObligationFilterTab.ALL,
                        testTag = "obligations_status_dropdown"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "هتندفع لمين / الجهة", "Paid To / Entity"),
                            selectedValue = selectedParty,
                            options = partyOptions,
                            isArabic = isArabic,
                            onSelected = { selectedParty = it },
                            isHighlighted = selectedParty != null,
                            testTag = "obligations_party_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "عشان إيه / السبب", "What For / Reason"),
                            selectedValue = selectedItem,
                            options = itemOptions,
                            isArabic = isArabic,
                            onSelected = { selectedItem = it },
                            isHighlighted = selectedItem != null,
                            testTag = "obligations_item_dropdown",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SmartNoteFilterDropdown(
                        label = tr(isArabic, "الملاحظة الإضافية", "Additional Note"),
                        noteQuery = noteFilterQuery,
                        availableNotes = availableNotes,
                        isArabic = isArabic,
                        onNoteQueryChange = { noteFilterQuery = it },
                        testTag = "obligations_note_dropdown",
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (availableCurrencies.size > 1) {
                        SmartSingleSelectDropdown(
                            label = tr(isArabic, "العملة", "Currency"),
                            selectedValue = selectedFilterCurrency,
                            options = currencyOptions,
                            isArabic = isArabic,
                            onSelected = { chosen ->
                                selectedFilterCurrency = chosen
                                if (chosen != null) selectedCurrency = chosen
                            },
                            isHighlighted = selectedFilterCurrency != null,
                            testTag = "obligations_currency_dropdown"
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("obligations_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = tr(isArabic, "مسح البحث", "Clear search"),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else null,
                        placeholder = {
                            Text(
                                text = tr(isArabic, "بحث سريع بالسبب، الجهة، الملاحظة، أو التاريخ...", "Quick search reason, entity, note, due date..."),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }

            // 4. Unified List: Scheduled Obligations + Dated Unpaid Transactions
            if (filteredObligations.isEmpty() && filteredScheduledTx.isEmpty()) {
                item {
                    SereneCard(contentPadding = PaddingValues(24.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(ext.cardSubtleBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EventNote,
                                    contentDescription = null,
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = when (activeTab) {
                                    ObligationFilterTab.PENDING -> tr(isArabic, "لا توجد التزامات بمواعيد سداد قادمة حالياً", "No upcoming scheduled payments")
                                    ObligationFilterTab.PAID -> tr(isArabic, "لا توجد التزامات مدفوعة بعد", "No paid scheduled payments yet")
                                    ObligationFilterTab.ALL -> tr(isArabic, "قائمة الالتزامات بمواعيد سداد فارغة", "Scheduled payments list is empty")
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = tr(
                                    isArabic,
                                    "أي التزام أو مصروف أو سلفة تحدد لها تاريخ ووقت سداد ستظهر هنا مع زر إعدادات التنبيه والرنين والنطق الصوتي.",
                                    "Any obligation or payment with a due date appears here with full ringtone, notification, and spoken reminder controls."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                if (filteredScheduledTx.isNotEmpty()) {
                    item {
                        Text(
                            text = tr(isArabic, "عمليات ومبالغ محددة بموعد سداد (${filteredScheduledTx.size})", "Transactions with Payment Deadlines (${filteredScheduledTx.size})"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    items(filteredScheduledTx, key = { "sched_tx_${it.id}" }) { tx ->
                        ScheduledTransactionDueCard(
                            tx = tx,
                            todayIso = todayIso,
                            isArabic = isArabic,
                            onReminderSettingsClick = { reminderEditTx = tx },
                            onToggleSettledClick = { onSetTransactionDebtSettled(tx.id, !tx.isDebtSettled) }
                        )
                    }
                }

                if (filteredObligations.isNotEmpty()) {
                    item {
                        Text(
                            text = tr(isArabic, "الالتزامات المجدولة (${filteredObligations.size})", "Scheduled Obligations (${filteredObligations.size})"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    items(filteredObligations, key = { "ob_${it.id}" }) { ob ->
                        ObligationItemCard(
                            obligation = ob,
                            todayIso = todayIso,
                            isArabic = isArabic,
                            onPayClick = {
                                selectedPaySource = settings.lastSelectedSource.ifBlank {
                                    accounts.firstOrNull()?.name ?: tr(isArabic, "كاش / نقدي", "Cash")
                                }
                                pendingPayTarget = ob
                            },
                            onReopenClick = { onSetObligationPaid(ob.id, false, "") },
                            onReminderSettingsClick = { reminderEditObligation = ob },
                            onToggleMuteClick = { onSetObligationReminderMuted(ob.id, !ob.reminderMuted) },
                            onDeleteClick = { pendingDeleteTarget = ob }
                        )
                    }
                }
            }
        }
    }

    // Per-item Reminder Settings Dialog for Obligation
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

    // Per-item Reminder Settings Dialog for Scheduled Transaction
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

    // Add Obligation Modal Dialog
    if (showAddModal) {
        AddObligationDialog(
            defaultCurrency = effectiveCurrency,
            currentAvailableMinor = summary.availableWithoutObligationsMinor,
            settings = settings,
            isArabic = isArabic,
            onDismiss = { showAddModal = false },
            onSave = { amt, curr, reason, party, dueDate, dueTime, remEnabled, leadMin, note, alertMode, fullScreen, speakAloud ->
                if (remEnabled) {
                    onRequestNotificationPermission()
                }
                onAddObligation(amt, curr, reason, party, dueDate, dueTime, remEnabled, leadMin, note, alertMode, fullScreen, speakAloud)
                showAddModal = false
            }
        )
    }

    // Pay Obligation Account Picker Dialog
    pendingPayTarget?.let { target ->
        val tSym = currencySymbol(target.currency, isArabic)
        val accountOptions = remember(accounts, isArabic) {
            val names = accounts.map { it.name }.distinct()
            if (names.isEmpty()) listOf(tr(isArabic, "كاش / نقدي", "Cash")) else names
        }
        AlertDialog(
            onDismissRequest = { pendingPayTarget = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Payments, contentDescription = null, tint = ext.incomeColor)
                    Text(
                        text = tr(isArabic, "تأكيد سداد الالتزام", "Confirm Obligation Payment"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tr(
                            isArabic,
                            "سيتم سداد «${target.reason}» بمبلغ ${formatAmountMinor(target.amountMinor)} $tSym وخصمه من الحساب المختار دون تكرار في الخصم.",
                            "Pay \"${target.reason}\" (${formatAmountMinor(target.amountMinor)} $tSym) from the chosen account with exact reconciliation."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tr(isArabic, "اختر الحساب / المحفظة للدفع:", "Select Payment Account:"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accountOptions.forEach { accName ->
                            val selected = selectedPaySource == accName
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { selectedPaySource = accName },
                                shape = RoundedCornerShape(999.dp),
                                color = if (selected) ext.heroCardBackground else ext.cardSubtleBackground,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) ext.heroCardBackground else ext.cardBorder
                                )
                            ) {
                                Text(
                                    text = accName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosenSource = selectedPaySource
                        pendingPayTarget = null
                        if (settings.confirmPaymentWithSecurity && settings.hasAnyLockMethodConfigured()) {
                            pendingSecurityPayTarget = target to chosenSource
                        } else {
                            onSetObligationPaid(target.id, true, chosenSource)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ext.incomeColor, contentColor = Color.White),
                    modifier = Modifier.testTag("obligation_confirm_pay_btn")
                ) {
                    Text(tr(isArabic, "تأكيد الدفع", "Confirm Payment"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPayTarget = null }) {
                    Text(tr(isArabic, "إلغاء", "Cancel"))
                }
            }
        )
    }

    // Optional Security Verification Gate on Payment
    pendingSecurityPayTarget?.let { (target, sourceName) ->
        val tSym = currencySymbol(target.currency, isArabic)
        SecurityVerificationDialog(
            title = tr(isArabic, "التحقق قبل دفع الالتزام", "Verify Before Paying"),
            description = tr(
                isArabic,
                "تأكيد دفع «${target.reason}» بقيمة ${formatAmountMinor(target.amountMinor)} $tSym من ($sourceName)",
                "Confirm paying \"${target.reason}\" (${formatAmountMinor(target.amountMinor)} $tSym) from ($sourceName)"
            ),
            confirmLabel = tr(isArabic, "تأكيد السداد", "Confirm Payment"),
            isDanger = false,
            requireSecurity = true,
            settings = settings,
            isArabic = isArabic,
            onConfirmed = {
                onSetObligationPaid(target.id, true, sourceName)
                pendingSecurityPayTarget = null
            },
            onDismiss = { pendingSecurityPayTarget = null }
        )
    }

    // Delete Confirmation Dialog
    pendingDeleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTarget = null },
            title = {
                Text(
                    text = tr(isArabic, "حذف الالتزام؟", "Delete Obligation?"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = tr(
                        isArabic,
                        "سيتم حذف «${target.reason}» وإلغاء تنبيهاته وإعادة حساب الرصيد المتاح تلقائياً.",
                        "\"${target.reason}\" will be deleted, its reminder cancelled, and your balance recalculated."
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteObligation(target.id)
                        pendingDeleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ext.expenseColor, contentColor = Color.White),
                    modifier = Modifier.testTag("obligation_confirm_delete_btn")
                ) {
                    Text(tr(isArabic, "حذف", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTarget = null }) {
                    Text(tr(isArabic, "إلغاء", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun ScheduledTransactionDueCard(
    tx: TransactionEntity,
    todayIso: String,
    isArabic: Boolean,
    onReminderSettingsClick: () -> Unit,
    onToggleSettledClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    val sym = currencySymbol(tx.currency, isArabic)
    val isOverdue = !tx.isDebtSettled && tx.paymentDueDate.isNotBlank() && tx.paymentDueDate < todayIso
    val isDueToday = !tx.isDebtSettled && tx.paymentDueDate == todayIso
    val statusColor = when {
        tx.isDebtSettled -> ext.incomeColor
        isOverdue -> ext.expenseColor
        isDueToday -> ext.warningAmber
        else -> MaterialTheme.colorScheme.primary
    }
    val statusLabel = when {
        tx.isDebtSettled -> tr(isArabic, "تم السداد ✓", "Paid ✓")
        isOverdue -> tr(isArabic, "متأخر عن موعد السداد", "Overdue")
        isDueToday -> tr(isArabic, "موعد سداده اليوم", "Due Today")
        else -> tr(isArabic, "بموعد سداد قادم", "Upcoming Due")
    }

    SereneCard(
        modifier = Modifier.testTag("scheduled_tx_card_${tx.id}"),
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = statusColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        if (!tx.isDebtSettled && tx.hasPaymentReminder) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = ext.incomeContainer
                            ) {
                                Text(
                                    text = "${formatReminderLeadTime(tx.reminderLeadMinutes, isArabic)} • ${formatAlertModeLabel(tx.reminderAlertMode, isArabic)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.incomeColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = tx.itemName.ifBlank { tx.category },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tx.debtParty.isNotBlank()) {
                        Text(
                            text = tr(isArabic, "لـ: ${tx.debtParty}", "To: ${tx.debtParty}"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = tr(
                            isArabic,
                            "تاريخ السداد: ${tx.paymentDueDate.ifBlank { "غير محدد" }} ${if (tx.paymentDueTime.isNotBlank()) "• ${formatTime12Hour(tx.paymentDueTime, true)}" else ""}",
                            "Due: ${tx.paymentDueDate.ifBlank { "Not set" }} ${if (tx.paymentDueTime.isNotBlank()) "• ${formatTime12Hour(tx.paymentDueTime, false)}" else ""}"
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                    Text(
                        text = tr(
                            isArabic,
                            "تاريخ التسجيل/الاستلام: ${tx.date} • ${formatTime12Hour(tx.time, true)}",
                            "Recorded on: ${tx.date} • ${formatTime12Hour(tx.time, false)}"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${formatAmountMinor(tx.amountMinor)} $sym",
                    style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                    color = if (tx.isDebtSettled) ext.incomeColor else ext.expenseColor,
                    maxLines = 1,
                    softWrap = false
                )
            }

            HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!tx.isDebtSettled) {
                    Button(
                        onClick = onToggleSettledClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.incomeColor,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.heightIn(min = 42.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tr(isArabic, "تم الدفع", "Mark Paid"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    OutlinedButton(
                        onClick = onReminderSettingsClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("sched_tx_reminder_btn_${tx.id}")
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr(isArabic, "إعدادات التنبيه", "Alert Settings"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggleSettledClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.heightIn(min = 42.dp)
                    ) {
                        Icon(Icons.Outlined.Replay, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tr(isArabic, "إعادة كمستحق", "Reopen"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ObligationItemCard(
    obligation: ObligationEntity,
    todayIso: String,
    isArabic: Boolean,
    onPayClick: () -> Unit,
    onReopenClick: () -> Unit,
    onReminderSettingsClick: () -> Unit,
    onToggleMuteClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    val sym = currencySymbol(obligation.currency, isArabic)
    val isOverdue = !obligation.isPaid && obligation.dueDate.isNotBlank() && obligation.dueDate < todayIso
    val isDueToday = !obligation.isPaid && obligation.dueDate == todayIso

    val statusColor = when {
        obligation.isPaid -> ext.incomeColor
        isOverdue -> ext.expenseColor
        isDueToday -> ext.warningAmber
        else -> MaterialTheme.colorScheme.primary
    }
    val statusLabel = when {
        obligation.isPaid -> tr(isArabic, "تم الدفع ✓", "Paid ✓")
        isOverdue -> tr(isArabic, "متأخر عن الموعد", "Overdue")
        isDueToday -> tr(isArabic, "مستحق اليوم", "Due Today")
        else -> tr(isArabic, "مطلوب دفعه", "Pending")
    }

    SereneCard(
        modifier = Modifier.testTag("obligation_card_${obligation.id}"),
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = statusColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        if (obligation.reminderEnabled && !obligation.isPaid) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (obligation.reminderMuted) ext.cardSubtleBackground else ext.incomeContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (obligation.reminderMuted) Icons.Outlined.NotificationsOff else Icons.Outlined.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (obligation.reminderMuted) MaterialTheme.colorScheme.onSurfaceVariant else ext.incomeColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (obligation.reminderMuted) tr(isArabic, "مكتوم", "Muted")
                                        else formatReminderLeadTime(obligation.reminderLeadMinutes, isArabic),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (obligation.reminderMuted) MaterialTheme.colorScheme.onSurfaceVariant else ext.incomeColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = obligation.reason,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (obligation.party.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = tr(isArabic, "الجهة: ${obligation.party}", "Entity: ${obligation.party}"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${formatAmountMinor(obligation.amountMinor)} $sym",
                        style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (obligation.isPaid) ext.incomeColor else ext.expenseColor,
                        maxLines = 1,
                        softWrap = false
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${obligation.dueDate} • ${formatTime12Hour(obligation.dueTime, isArabic)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (obligation.note.isNotBlank()) {
                Text(
                    text = obligation.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (obligation.isPaid && (obligation.paidSource.isNotBlank() || obligation.paidDate.isNotBlank())) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ext.incomeContainer.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = ext.incomeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = tr(isArabic, "مدفوع من: ${obligation.paidSource}", "Paid via: ${obligation.paidSource}"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = ext.incomeColor
                            )
                        }
                        if (obligation.paidDate.isNotBlank()) {
                            Text(
                                text = obligation.paidDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!obligation.isPaid) {
                    Button(
                        onClick = onPayClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.incomeColor,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("obligation_pay_btn_${obligation.id}")
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tr(isArabic, "تسجيل الدفع", "Mark Paid"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onReopenClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("obligation_reopen_btn_${obligation.id}")
                    ) {
                        Icon(Icons.Outlined.Replay, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tr(isArabic, "إعادة كمستحق", "Move to Pending"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (!obligation.isPaid) {
                        OutlinedButton(
                            onClick = onReminderSettingsClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("obligation_reminder_settings_btn_${obligation.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = tr(isArabic, "إعدادات التنبيه", "Alert settings"),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr(isArabic, "تنبيه", "Alert"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    if (obligation.reminderEnabled && !obligation.isPaid) {
                        IconButton(
                            onClick = onToggleMuteClick,
                            modifier = Modifier.testTag("obligation_mute_btn_${obligation.id}")
                        ) {
                            Icon(
                                imageVector = if (obligation.reminderMuted) Icons.Outlined.NotificationsOff else Icons.Outlined.NotificationsActive,
                                contentDescription = tr(isArabic, "تبديل التنبيه", "Toggle reminder"),
                                tint = if (obligation.reminderMuted) MaterialTheme.colorScheme.onSurfaceVariant else ext.incomeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("obligation_delete_btn_${obligation.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = tr(isArabic, "حذف الالتزام", "Delete obligation"),
                            tint = ext.expenseColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddObligationDialog(
    defaultCurrency: String,
    currentAvailableMinor: Long,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        amountMinor: Long,
        currency: String,
        reason: String,
        party: String,
        dueDate: String,
        dueTime: String,
        reminderEnabled: Boolean,
        reminderLeadMinutes: Int,
        note: String,
        reminderAlertMode: String,
        reminderFullScreen: Boolean,
        reminderSpeakAloud: Boolean
    ) -> Unit
) {
    val ext = LocalSereneColors.current
    val context = LocalContext.current

    var amountInput by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var reasonInput by remember { mutableStateOf("") }
    var partyInput by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(currentIsoDate()) }
    var dueTime by remember { mutableStateOf("09:00") }
    var reminderEnabled by remember { mutableStateOf(true) }
    var leadMinutes by remember { mutableIntStateOf(0) }
    var alertMode by remember(settings.defaultAlertMode) { mutableStateOf(settings.defaultAlertMode) }
    var fullScreenAlert by remember(settings.fullScreenAlertsEnabled) { mutableStateOf(settings.fullScreenAlertsEnabled) }
    var speakAloud by remember(settings.speakRemindersAloud) { mutableStateOf(settings.speakRemindersAloud) }
    var noteInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showFastTimePicker by remember { mutableStateOf(false) }

    val parsedMinor = remember(amountInput) { parseAmountToMinor(amountInput) }
    val sym = currencySymbol(currency, isArabic)
    val projectedAvailable = remember(currentAvailableMinor, parsedMinor, currency, defaultCurrency) {
        if (parsedMinor != null && currency == defaultCurrency) {
            currentAvailableMinor - parsedMinor
        } else null
    }

    val reasonPresets = remember(isArabic) {
        if (isArabic) listOf("إيجار", "فاتورة مورد", "قسط شهري", "كهرباء ومياه", "رواتب", "ضرائب وتأمينات")
        else listOf("Rent", "Supplier Invoice", "Installment", "Utilities", "Payroll", "Taxes")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 540.dp),
            shape = RoundedCornerShape(22.dp),
            color = ext.cardBackground,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ext.warningAmber.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EventNote,
                                contentDescription = null,
                                tint = ext.warningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tr(isArabic, "إضافة مبلغ مطلوب دفعه", "Add Amount Due to Pay"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = tr(isArabic, "يُخصم تلقائياً من المتاح للصرف بدون التزامات", "Deducts immediately from Free Available Balance"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = tr(isArabic, "إغلاق", "Close"))
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        validationError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("obligation_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(tr(isArabic, "المبلغ المطلوب دفعه ($sym) *", "Amount Due ($sym) *")) },
                    placeholder = { Text("0.00") }
                )

                // Currency Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SupportedCurrencies.forEach { info ->
                        val selected = currency == info.code
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { currency = info.code },
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) ext.heroCardBackground else ext.cardSubtleBackground,
                            border = BorderStroke(1.dp, if (selected) ext.heroCardBackground else ext.cardBorder)
                        ) {
                            Text(
                                text = "${info.code} (${if (isArabic) info.symbolAr else info.symbolEn})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Live Available Balance Impact Preview
                if (projectedAvailable != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (projectedAvailable >= 0L) ext.incomeContainer.copy(alpha = 0.55f)
                        else ext.expenseContainer.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(isArabic, "المتاح بدون التزامات بعد الإضافة:", "Available after adding:"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${formatAmountMinor(projectedAvailable)} $sym",
                                style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = if (projectedAvailable >= 0L) ext.incomeColor else ext.expenseColor
                            )
                        }
                    }
                }

                // Payment Reason Input + Quick Presets
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = {
                        reasonInput = it
                        validationError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("obligation_reason_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(tr(isArabic, "سبب الدفع *", "Payment Reason *")) },
                    placeholder = { Text(tr(isArabic, "مثال: إيجار المحل / فاتورة مورد", "e.g., Shop Rent / Supplier Bill")) }
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reasonPresets.forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    reasonInput = preset
                                    validationError = null
                                },
                            shape = RoundedCornerShape(999.dp),
                            color = ext.cardSubtleBackground,
                            border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Entity / Party Input
                OutlinedTextField(
                    value = partyInput,
                    onValueChange = { partyInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("obligation_party_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Outlined.Business, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    label = { Text(tr(isArabic, "الجهة (شخص / شركة / بنك)", "Entity / Party")) },
                    placeholder = { Text(tr(isArabic, "مثال: شركة الكهرباء / المالك / المورد", "e.g., Landlord / Vendor / Bank")) }
                )

                // Due Date & Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("obligation_date_picker_btn")
                            .clickable {
                                val cal = Calendar.getInstance()
                                val parts = dueDate.split("-")
                                val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
                                val m = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
                                val d = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
                                DatePickerDialog(context, { _, yr, mo, dy ->
                                    dueDate = String.format(Locale.US, "%04d-%02d-%02d", yr, mo + 1, dy)
                                }, y, m, d).show()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = ext.cardSubtleBackground,
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = ext.incomeColor, modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    text = tr(isArabic, "تاريخ الدفع", "Due Date"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dueDate,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("obligation_time_picker_btn")
                            .clickable { showFastTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = ext.cardSubtleBackground,
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = ext.incomeColor, modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    text = tr(isArabic, "وقت السداد", "Due Time"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatTime12Hour(dueTime, isArabic),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Notification Reminder Section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ext.cardSubtleBackground,
                    border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (reminderEnabled) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = tr(isArabic, "تنبيه بالإشعارات قبل الموعد", "Notification Reminder"),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = tr(isArabic, "إشعار تلقائي في التاريخ والوقت المحدد", "Automatic alert at scheduled date & time"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { reminderEnabled = it },
                                modifier = Modifier.testTag("obligation_reminder_switch"),
                                colors = SwitchDefaults.colors(checkedTrackColor = ext.incomeColor)
                            )
                        }

                        if (reminderEnabled) {
                            SmartSingleSelectDropdown(
                                label = tr(isArabic, "التنبيه قبل الموعد بمدة", "Lead Time"),
                                selectedValue = leadMinutes,
                                options = PaymentReminderLeadOptions.map {
                                    DropdownFilterOption(it.minutes, if (isArabic) it.labelAr else it.labelEn)
                                },
                                isArabic = isArabic,
                                onSelected = { leadMinutes = it },
                                testTag = "obligation_lead_dropdown"
                            )

                            SmartSingleSelectDropdown(
                                label = tr(isArabic, "نوع التنبيه (إشعار / رنين / نطق)", "Alert Style"),
                                selectedValue = alertMode,
                                options = PaymentAlertModeOptions.map {
                                    DropdownFilterOption(it.mode, if (isArabic) it.labelAr else it.labelEn, if (isArabic) it.descAr else it.descEn)
                                },
                                isArabic = isArabic,
                                onSelected = { alertMode = it },
                                testTag = "obligation_mode_dropdown"
                            )

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
                                    Icon(Icons.Outlined.Fullscreen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                                    Text(
                                        text = tr(isArabic, "إشعار بملء الشاشة", "Full-Screen Display"),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Switch(
                                    checked = fullScreenAlert,
                                    onCheckedChange = { fullScreenAlert = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = ext.incomeColor)
                                )
                            }

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
                                    Icon(Icons.Outlined.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                                    Text(
                                        text = tr(isArabic, "نطق تفاصيل المبلغ صوتياً", "Speak Details Aloud"),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Switch(
                                    checked = speakAloud,
                                    onCheckedChange = { speakAloud = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = ext.incomeColor)
                                )
                            }
                        }
                    }
                }

                // Optional Note
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { if (it.length <= 200) noteInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("obligation_note_input"),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(tr(isArabic, "ملاحظات (اختياري)", "Notes (optional)")) }
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.expenseColor
                    )
                }

                // Save Button
                Button(
                    onClick = {
                        val amt = parseAmountToMinor(amountInput)
                        when {
                            amt == null || amt <= 0L -> {
                                validationError = tr(isArabic, "يرجى إدخال مبلغ صحيح أكبر من الصفر", "Please enter a valid amount greater than zero")
                            }
                            reasonInput.trim().isEmpty() -> {
                                validationError = tr(isArabic, "يرجى كتابة سبب الدفع", "Please enter the payment reason")
                            }
                            else -> {
                                onSave(
                                    amt,
                                    currency,
                                    reasonInput.trim(),
                                    partyInput.trim(),
                                    dueDate,
                                    dueTime,
                                    reminderEnabled,
                                    leadMinutes,
                                    noteInput.trim(),
                                    alertMode,
                                    fullScreenAlert,
                                    speakAloud
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("obligation_save_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ext.heroCardBackground,
                        contentColor = ext.heroCardOnBackground
                    )
                ) {
                    Text(
                        text = tr(isArabic, "حفظ وخصم من المتاح بدون التزامات", "Save & Deduct from Available Balance"),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    if (showFastTimePicker) {
        FastTimePickerDialog(
            initialTime24 = dueTime,
            isArabic = isArabic,
            title = tr(isArabic, "اختر وقت السداد والتنبيه", "Select Due Payment Time"),
            onConfirm = { picked ->
                dueTime = picked
                showFastTimePicker = false
            },
            onDismiss = { showFastTimePicker = false }
        )
    }
}
