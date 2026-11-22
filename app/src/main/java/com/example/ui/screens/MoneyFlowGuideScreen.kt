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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppSettingsEntity
import com.example.data.ObligationEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.ExportDownloadButton
import com.example.ui.components.SereneCard
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.formatTime12Hour
import com.example.util.tr

private enum class MoneyFlowSubTab {
    AFTER_UPCOMING,
    AFTER_PAID,
    LIFETIME_MONEY,
    ALL_DUE_ON_YOU,
    FLOW_BREAKDOWN
}

private enum class BreakdownScopeTab {
    ALL,
    OUTGOING,
    INCOMING
}

private data class FlowBreakdownItem(
    val title: String,
    val count: Int,
    val amountMinor: Long,
    val sharePct: Float
)

/**
 * Organized vertical tabs hub inside «المخطط» (Reports -> المخطط).
 *
 * Displays all financial tabs neatly stacked one under another in a clear, structured layout,
 * each showing its exact live dynamic amount and current status pill:
 * 1. «فلوسك الباقية لو حسبت المبالغ اللي هتندفع قريباً»
 * 2. «فلوسك الباقية لو حسبت المبالغ اللي اندفعت بالفعل فقط»
 * 3. «فلوسك الباقية من يوم ما استخدمت البرنامج»
 * 4. «فلوسك الموجودة عليك المفروض تدفعها»
 * 5. «المخطط التفصيلي (فلوسك جات منين وراحت فين)»
 */
@Composable
fun MoneyFlowGuideScreen(
    transactions: List<TransactionEntity>,
    obligations: List<ObligationEntity>,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onOpenLifetimeMoney: () -> Unit,
    onOpenExpenseDetails: () -> Unit,
    onOpenObligations: () -> Unit,
    onSetObligationPaid: (Long, Boolean) -> Unit = { _, _ -> },
    onToggleDebtSettled: (Long, Boolean) -> Unit = { _, _ -> },
    onOpenDrillDown: (FlowDrillDownSpec) -> Unit = {}
) {
    val ext = LocalSereneColors.current
    val primaryCurrency = settings.defaultCurrency
    val sym = currencySymbol(primaryCurrency, isArabic)

    val primaryTx = remember(transactions, primaryCurrency) {
        transactions.filter { it.currency == primaryCurrency }
    }
    val allIncomeTx = remember(transactions) {
        transactions.filter { it.type == TransactionType.INCOME }
    }
    val primaryIncomeTx = remember(primaryTx) {
        primaryTx.filter { it.type == TransactionType.INCOME }
    }
    val primaryExpenseTx = remember(primaryTx) {
        primaryTx.filter { it.type == TransactionType.EXPENSE }
    }
    val primaryObligations = remember(obligations, primaryCurrency) {
        obligations.filter { it.currency == primaryCurrency }
    }

    var activeSubTabName by rememberSaveable { mutableStateOf<String?>(null) }
    val activeSubTab = remember(activeSubTabName) {
        activeSubTabName?.let { runCatching { MoneyFlowSubTab.valueOf(it) }.getOrNull() }
    }

    BackHandler(enabled = activeSubTab != null) {
        activeSubTabName = null
    }

    val lifetimeIncomeMinor = remember(primaryIncomeTx) {
        primaryIncomeTx.sumOf { it.amountMinor }
    }
    val regularPaidExpenses = remember(primaryExpenseTx) {
        primaryExpenseTx.filter { !it.isDebt }
    }
    val settledExpenseDebts = remember(primaryExpenseTx) {
        primaryExpenseTx.filter { it.isDebt && it.isDebtSettled }
    }
    val settledIncomeDebts = remember(primaryIncomeTx) {
        primaryIncomeTx.filter { it.isDebt && it.isDebtSettled }
    }
    val paidObligationsList = remember(primaryObligations) {
        primaryObligations.filter { it.isPaid }
    }
    val regularPaidExpensesMinor = remember(regularPaidExpenses) {
        regularPaidExpenses.sumOf { it.amountMinor }
    }
    val paidObligationsMinor = remember(paidObligationsList) {
        paidObligationsList.sumOf { it.amountMinor }
    }
    val settledDebtsMinor = remember(settledExpenseDebts, settledIncomeDebts) {
        settledExpenseDebts.sumOf { it.amountMinor } + settledIncomeDebts.sumOf { it.amountMinor }
    }
    val settledDebtsCount = settledExpenseDebts.size + settledIncomeDebts.size
    val paidExpensesMinor = regularPaidExpensesMinor + paidObligationsMinor + settledDebtsMinor

    val pendingObligationsList = remember(primaryObligations) {
        primaryObligations.filter { !it.isPaid }
    }
    val unpaidDebtsList = remember(primaryExpenseTx, primaryIncomeTx) {
        primaryExpenseTx.filter { it.isDebt && !it.isDebtSettled } +
            primaryIncomeTx.filter { it.isDebt && !it.isDebtSettled }
    }
    val pendingObligationsMinor = remember(pendingObligationsList, unpaidDebtsList) {
        pendingObligationsList.sumOf { it.amountMinor } + unpaidDebtsList.sumOf { it.amountMinor }
    }
    val pendingObligationsCount = pendingObligationsList.size + unpaidDebtsList.size

    // Exact canonical balances synced across Home, Reports, Dashboard & Plan:
    val afterActuallyPaidMinor = lifetimeIncomeMinor - paidExpensesMinor
    val remainingAvailableMinor = afterActuallyPaidMinor - pendingObligationsMinor

    val deductionEntries = remember(regularPaidExpenses, paidObligationsList, settledExpenseDebts, settledIncomeDebts, isArabic) {
        buildList {
            regularPaidExpenses.forEach { tx ->
                add(
                    PaidDeductionEntry(
                        key = "exp_${tx.id}",
                        kind = PaidDeductionKind.EXPENSE,
                        title = tx.itemName.ifBlank { tx.category.ifBlank { tr(isArabic, "مصروف", "Expense") } },
                        subtitle = buildString {
                            if (tx.debtParty.isNotBlank()) append(tr(isArabic, "لمـن: ${tx.debtParty}", "To: ${tx.debtParty}"))
                            if (tx.category.isNotBlank()) {
                                if (isNotEmpty()) append(" • ")
                                append(tx.category)
                            }
                        },
                        source = tx.source.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") },
                        dateLabel = if (tx.time.isNotBlank()) "${tx.date} • ${formatTime12Hour(tx.time, isArabic)}" else tx.date,
                        amountMinor = tx.amountMinor,
                        sortTimestamp = tx.createdAt
                    )
                )
            }
            paidObligationsList.forEach { ob ->
                add(
                    PaidDeductionEntry(
                        key = "ob_${ob.id}",
                        kind = PaidDeductionKind.PAID_OBLIGATION,
                        title = ob.reason.ifBlank { tr(isArabic, "مبلغ مطلوب تم دفعه", "Paid Obligation") },
                        subtitle = ob.party.ifBlank { ob.note },
                        source = ob.paidSource.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") },
                        dateLabel = ob.paidDate.ifBlank { ob.dueDate },
                        amountMinor = ob.amountMinor,
                        sortTimestamp = ob.createdAt
                    )
                )
            }
            (settledExpenseDebts + settledIncomeDebts).forEach { tx ->
                add(
                    PaidDeductionEntry(
                        key = "debt_${tx.id}",
                        kind = PaidDeductionKind.SETTLED_DEBT,
                        title = tx.itemName.ifBlank { tx.debtParty.ifBlank { tx.category } },
                        subtitle = tx.debtParty.ifBlank { tx.note },
                        source = tx.source.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") },
                        dateLabel = if (tx.time.isNotBlank()) "${tx.date} • ${formatTime12Hour(tx.time, isArabic)}" else tx.date,
                        amountMinor = tx.amountMinor,
                        sortTimestamp = tx.createdAt
                    )
                )
            }
        }.sortedByDescending { it.sortTimestamp }
    }

    val incomeByReason = remember(primaryIncomeTx, lifetimeIncomeMinor, isArabic) {
        val denom = lifetimeIncomeMinor.coerceAtLeast(1L).toFloat()
        primaryIncomeTx
            .groupBy { it.category.ifBlank { tr(isArabic, "غير محدد", "Unspecified") } }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amountMinor }
                FlowBreakdownItem(cat, list.size, sum, (sum.toFloat() / denom) * 100f)
            }
            .sortedByDescending { it.amountMinor }
    }

    val incomeByChannel = remember(primaryIncomeTx, lifetimeIncomeMinor, isArabic) {
        val denom = lifetimeIncomeMinor.coerceAtLeast(1L).toFloat()
        primaryIncomeTx
            .groupBy { it.source.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") } }
            .map { (src, list) ->
                val sum = list.sumOf { it.amountMinor }
                FlowBreakdownItem(src, list.size, sum, (sum.toFloat() / denom) * 100f)
            }
            .sortedByDescending { it.amountMinor }
    }

    val expensesByDestination = remember(primaryExpenseTx, primaryIncomeTx, primaryObligations, paidExpensesMinor, isArabic) {
        val map = linkedMapOf<String, Pair<Int, Long>>()
        primaryExpenseTx.filter { !it.isDebt || it.isDebtSettled }.forEach { tx ->
            val key = tx.category.ifBlank { tr(isArabic, "غير محدد", "Unspecified") }
            val prev = map[key] ?: (0 to 0L)
            map[key] = (prev.first + 1) to (prev.second + tx.amountMinor)
        }
        primaryIncomeTx.filter { it.isDebt && it.isDebtSettled }.forEach { tx ->
            val key = tx.category.ifBlank { tr(isArabic, "غير محدد", "Unspecified") }
            val prev = map[key] ?: (0 to 0L)
            map[key] = (prev.first + 1) to (prev.second + tx.amountMinor)
        }
        primaryObligations.filter { it.isPaid }.forEach { ob ->
            val key = ob.reason.ifBlank { tr(isArabic, "التزام مجدول مدفوع", "Paid Obligation") }
            val prev = map[key] ?: (0 to 0L)
            map[key] = (prev.first + 1) to (prev.second + ob.amountMinor)
        }
        val denom = paidExpensesMinor.coerceAtLeast(1L).toFloat()
        map.entries
            .map { (title, pair) ->
                FlowBreakdownItem(title, pair.first, pair.second, (pair.second.toFloat() / denom) * 100f)
            }
            .sortedByDescending { it.amountMinor }
    }

    val expensesByChannel = remember(primaryExpenseTx, primaryIncomeTx, primaryObligations, paidExpensesMinor, isArabic) {
        val map = linkedMapOf<String, Pair<Int, Long>>()
        primaryExpenseTx.filter { !it.isDebt || it.isDebtSettled }.forEach { tx ->
            val key = tx.source.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") }
            val prev = map[key] ?: (0 to 0L)
            map[key] = (prev.first + 1) to (prev.second + tx.amountMinor)
        }
        primaryIncomeTx.filter { it.isDebt && it.isDebtSettled }.forEach { tx ->
            val key = tx.source.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") }
            val prev = map[key] ?: (0 to 0L)
            map[key] = (prev.first + 1) to (prev.second + tx.amountMinor)
        }
        primaryObligations.filter { it.isPaid }.forEach { ob ->
            val key = ob.paidSource.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") }
            val prev = map[key] ?: (0 to 0L)
            map[key] = (prev.first + 1) to (prev.second + ob.amountMinor)
        }
        val denom = paidExpensesMinor.coerceAtLeast(1L).toFloat()
        map.entries
            .map { (title, pair) ->
                FlowBreakdownItem(title, pair.first, pair.second, (pair.second.toFloat() / denom) * 100f)
            }
            .sortedByDescending { it.amountMinor }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (activeSubTab != null) {
            // Sticky Back Bar to return to the organized vertical tabs menu
            val activeTabTitle = when (activeSubTab) {
                MoneyFlowSubTab.AFTER_UPCOMING -> tr(isArabic, "فلوسك الباقية لو حسبت المبالغ اللي هتندفع قريباً", "Remaining After Upcoming")
                MoneyFlowSubTab.AFTER_PAID -> tr(isArabic, "فلوسك الباقية لو حسبت المبالغ اللي اندفعت بالفعل فقط", "Remaining After Paid Only")
                MoneyFlowSubTab.LIFETIME_MONEY -> tr(isArabic, "الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج", "Lifetime Money Added")
                MoneyFlowSubTab.ALL_DUE_ON_YOU -> tr(isArabic, "فلوسك الموجودة عليك المفروض تدفعها", "All Amounts Due on You")
                MoneyFlowSubTab.FLOW_BREAKDOWN -> tr(isArabic, "المخطط التفصيلي (جات منين وراحت فين)", "Full Money Flow Breakdown")
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                    .heightIn(min = 48.dp)
                    .testTag("plan_back_to_tabs_btn")
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { activeSubTabName = null },
                shape = RoundedCornerShape(14.dp),
                color = ext.cardBackground,
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = tr(isArabic, "العودة لكل تبويبات المخطط", "Back to Plan Tabs"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(isArabic, "العودة لكل تبويبات المخطط", "Back to All Plan Tabs"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = activeTabTitle,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        when (activeSubTab) {
            null -> {
                // Organized Vertically Stacked Tabs inside «المخطط»
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = tr(isArabic, "تبويبات المخطط المالي", "Financial Plan Tabs"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = tr(
                                        isArabic,
                                        "اضغط على أي تبويبة لعرض التفاصيل والحسابات والعمليات الخاصة بها",
                                        "Tap any tab below to view its live breakdown and itemized records"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Tab 1: فلوسك الباقية لو حسبت المبالغ اللي هتندفع قريباً
                        item {
                            val upcomingPositive = remainingAvailableMinor >= 0L
                            PlanVerticalTabCard(
                                title = tr(
                                    isArabic,
                                    "فلوسك الباقية لو حسبت المبالغ اللي هتندفع قريباً",
                                    "Remaining Money After Upcoming Payments"
                                ),
                                statusText = when {
                                    remainingAvailableMinor < 0L -> tr(isArabic, "الحالة: عجز متوقع", "Status: Shortfall")
                                    pendingObligationsCount == 0 -> tr(isArabic, "الحالة: لا توجد مبالغ قريبة — متاح بالكامل", "Status: Fully Available")
                                    else -> tr(isArabic, "الحالة: آمن بعد خصم $pendingObligationsCount مبالغ قريبة", "Status: Safe after $pendingObligationsCount dues")
                                },
                                subtitle = tr(
                                    isArabic,
                                    "يخصم تلقائياً كل مصروف «لسه هيندفع» أو التزام مجدول قبل ما تدفعه",
                                    "Auto-deducts every unpaid upcoming expense & scheduled obligation"
                                ),
                                amountText = "${formatAmountMinor(remainingAvailableMinor)} $sym",
                                amountColor = if (upcomingPositive) ext.incomeColor else ext.expenseColor,
                                accentColor = ext.warningAmber,
                                accentBg = ext.warningAmber.copy(alpha = 0.16f),
                                icon = Icons.Outlined.EventNote,
                                tag = "plan_tab_after_upcoming",
                                onClick = { activeSubTabName = MoneyFlowSubTab.AFTER_UPCOMING.name }
                            )
                        }

                        // Tab 2: فلوسك الباقية لو حسبت المبالغ اللي اندفعت بالفعل فقط
                        item {
                            val paidPositive = afterActuallyPaidMinor >= 0L
                            PlanVerticalTabCard(
                                title = tr(
                                    isArabic,
                                    "فلوسك الباقية لو حسبت المبالغ اللي اندفعت بالفعل فقط",
                                    "Remaining Money After Actually Paid Amounts"
                                ),
                                statusText = if (paidPositive) {
                                    tr(isArabic, "الحالة: مطابق للباقي في محفظتك (${deductionEntries.size} عملية مدفوعة)", "Status: Matches Wallet (${deductionEntries.size} paid)")
                                } else {
                                    tr(isArabic, "الحالة: المصروف الفعلي أعلى من الدخل", "Status: Paid exceeds income")
                                },
                                subtitle = tr(
                                    isArabic,
                                    "يخصم فقط اللي اندفع فعلياً من مصاريفك والالتزامات المسددة",
                                    "Deducts only paid expenses and settled obligations"
                                ),
                                amountText = "${formatAmountMinor(afterActuallyPaidMinor)} $sym",
                                amountColor = if (paidPositive) ext.incomeColor else ext.expenseColor,
                                accentColor = MaterialTheme.colorScheme.primary,
                                accentBg = MaterialTheme.colorScheme.primaryContainer,
                                icon = Icons.Outlined.TaskAlt,
                                tag = "plan_tab_after_paid",
                                onClick = { activeSubTabName = MoneyFlowSubTab.AFTER_PAID.name }
                            )
                        }

                        // Tab 3: الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج
                        item {
                            PlanVerticalTabCard(
                                title = tr(
                                    isArabic,
                                    "الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج",
                                    "Your Money Since Using the App"
                                ),
                                statusText = tr(
                                    isArabic,
                                    "الحالة: تراكمي (${allIncomeTx.size} عملية إضافة) • لا ينقص بالصرف",
                                    "Status: Cumulative (${allIncomeTx.size} deposits) • unaffected by spending"
                                ),
                                subtitle = tr(
                                    isArabic,
                                    "يعرض كل الفلوس اللي دخلتلك وجات منين وعشان إيه بالتاريخ والوقت",
                                    "Shows every incoming deposit with source, reason, date & time"
                                ),
                                amountText = "${formatAmountMinor(lifetimeIncomeMinor)} $sym",
                                amountColor = ext.incomeColor,
                                accentColor = ext.incomeColor,
                                accentBg = ext.incomeContainer,
                                icon = Icons.Outlined.Savings,
                                tag = "plan_tab_lifetime",
                                onClick = { activeSubTabName = MoneyFlowSubTab.LIFETIME_MONEY.name }
                            )
                        }

                        // Tab 4: فلوسك الموجودة عليك المفروض تدفعها (NEW)
                        item {
                            PlanVerticalTabCard(
                                title = tr(
                                    isArabic,
                                    "فلوسك الموجودة عليك المفروض تدفعها",
                                    "Amounts Currently Due on You"
                                ),
                                statusText = if (pendingObligationsCount == 0) {
                                    tr(isArabic, "الحالة: لا توجد مبالغ عليك حالياً ✓", "Status: Nothing due ✓")
                                } else {
                                    tr(isArabic, "الحالة: مطلوب سداد $pendingObligationsCount مبالغ قريباً", "Status: $pendingObligationsCount items to pay soon")
                                },
                                subtitle = tr(
                                    isArabic,
                                    "يجمع كل المبالغ اللي مفروض تدفعها (مصاريف لسه هتندفع + التزامات بمواعيد)",
                                    "Collects all unpaid expenses & scheduled obligations with 1-tap Pay"
                                ),
                                amountText = "${formatAmountMinor(pendingObligationsMinor)} $sym",
                                amountColor = if (pendingObligationsMinor > 0L) ext.expenseColor else ext.incomeColor,
                                accentColor = ext.expenseColor,
                                accentBg = ext.expenseContainer,
                                icon = Icons.Outlined.RequestQuote,
                                tag = "plan_tab_all_due_on_you",
                                onClick = { activeSubTabName = MoneyFlowSubTab.ALL_DUE_ON_YOU.name }
                            )
                        }

                        // Tab 5: المخطط التفصيلي (جات منين وراحت فين)
                        item {
                            PlanVerticalTabCard(
                                title = tr(
                                    isArabic,
                                    "المخطط التفصيلي (فلوسك جات منين وراحت فين)",
                                    "Full Flow Breakdown (In vs Out)"
                                ),
                                statusText = tr(
                                    isArabic,
                                    "الحالة: شرح شامل للمصادر والمصاريف والحسابات",
                                    "Status: Full breakdown by source, category & wallet"
                                ),
                                subtitle = tr(
                                    isArabic,
                                    "معادلة مسار الفلوس الكاملة وتوزيع كل جنيه دخل أو اتصرف",
                                    "Complete flow equation & percentage breakdown"
                                ),
                                amountText = tr(isArabic, "عرض الشرح", "View Guide"),
                                amountColor = MaterialTheme.colorScheme.primary,
                                accentColor = MaterialTheme.colorScheme.primary,
                                accentBg = ext.cardSubtleBackground,
                                icon = Icons.Outlined.AccountTree,
                                tag = "plan_tab_flow_breakdown",
                                onClick = { activeSubTabName = MoneyFlowSubTab.FLOW_BREAKDOWN.name }
                            )
                        }
                    }
                }
            }

            MoneyFlowSubTab.AFTER_UPCOMING -> {
                AfterScheduledObligationsTabContent(
                    lifetimeIncomeMinor = lifetimeIncomeMinor,
                    paidExpensesMinor = paidExpensesMinor,
                    pendingObligationsMinor = pendingObligationsMinor,
                    remainingAvailableMinor = remainingAvailableMinor,
                    pendingObligations = pendingObligationsList,
                    unpaidDebts = unpaidDebtsList,
                    currencySymbol = sym,
                    isArabic = isArabic,
                    onOpenObligations = onOpenObligations,
                    onSetObligationPaid = onSetObligationPaid,
                    onToggleDebtSettled = onToggleDebtSettled
                )
            }

            MoneyFlowSubTab.AFTER_PAID -> {
                MoneyAfterPaidTabContent(
                    lifetimeIncomeMinor = lifetimeIncomeMinor,
                    regularPaidExpensesMinor = regularPaidExpensesMinor,
                    regularPaidExpensesCount = regularPaidExpenses.size,
                    paidObligationsMinor = paidObligationsMinor,
                    paidObligationsCount = paidObligationsList.size,
                    settledDebtsMinor = settledDebtsMinor,
                    settledDebtsCount = settledDebtsCount,
                    actualPaidOutflowMinor = paidExpensesMinor,
                    afterActuallyPaidMinor = afterActuallyPaidMinor,
                    pendingObligationsMinor = pendingObligationsMinor,
                    pendingObligationsCount = pendingObligationsCount,
                    deductionEntries = deductionEntries,
                    currencySymbol = sym,
                    isArabic = isArabic,
                    onOpenLifetimeMoney = onOpenLifetimeMoney,
                    onOpenExpenseDetails = onOpenExpenseDetails,
                    onOpenObligations = onOpenObligations
                )
            }

            MoneyFlowSubTab.LIFETIME_MONEY -> {
                LifetimeMoneyPlanTabContent(
                    allIncomeTx = allIncomeTx,
                    primaryCurrency = primaryCurrency,
                    lifetimeIncomeMinor = lifetimeIncomeMinor,
                    isArabic = isArabic,
                    onOpenFullLifetimeScreen = onOpenLifetimeMoney
                )
            }

            MoneyFlowSubTab.ALL_DUE_ON_YOU -> {
                AllDueOnYouTabContent(
                    pendingObligationsMinor = pendingObligationsMinor,
                    pendingObligations = pendingObligationsList,
                    unpaidDebts = unpaidDebtsList,
                    currencySymbol = sym,
                    isArabic = isArabic,
                    onOpenObligations = onOpenObligations,
                    onSetObligationPaid = onSetObligationPaid,
                    onToggleDebtSettled = onToggleDebtSettled
                )
            }

            MoneyFlowSubTab.FLOW_BREAKDOWN -> {
                var breakdownScope by rememberSaveable { mutableStateOf(BreakdownScopeTab.ALL.name) }
                val activeScope = runCatching { BreakdownScopeTab.valueOf(breakdownScope) }.getOrDefault(BreakdownScopeTab.ALL)

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 3 Organized Scope Tabs + Export Bar
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val scopeTabs = listOf(
                                        Triple(BreakdownScopeTab.ALL, tr(isArabic, "الكل (الجاي والمنصرف)", "All Flow"), "plan_scope_all"),
                                        Triple(BreakdownScopeTab.OUTGOING, tr(isArabic, "الفلوس اللي انصرفت", "Spent Money"), "plan_scope_outgoing"),
                                        Triple(BreakdownScopeTab.INCOMING, tr(isArabic, "الفلوس اللي جاية", "Incoming Money"), "plan_scope_incoming")
                                    )
                                    scopeTabs.forEach { (tab, label, tag) ->
                                        val selected = activeScope == tab
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .testTag(tag)
                                                .clickable { breakdownScope = tab.name },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) ext.heroCardBackground else ext.cardBackground,
                                            border = BorderStroke(1.dp, if (selected) ext.heroCardBackground else ext.cardBorder)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(isArabic, "اضغط على أي عنوان أو بند لعرض كل عملياته وتصديرها", "Tap any header or item to drill down & export"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ExportDownloadButton(
                                        transactions = primaryTx,
                                        obligations = primaryObligations,
                                        preferredCurrency = primaryCurrency,
                                        reportTitle = tr(isArabic, "مخطط مسار فلوسك بالكامل", "Complete Money Flow Plan"),
                                        filterSummary = tr(
                                            isArabic,
                                            "${primaryTx.size} عملية • ${primaryObligations.size} التزام",
                                            "${primaryTx.size} transactions • ${primaryObligations.size} obligations"
                                        ),
                                        isArabic = isArabic,
                                        compactLabel = true,
                                        testTag = "plan_breakdown_export_btn"
                                    )
                                }
                            }
                        }

                        if (activeScope == BreakdownScopeTab.ALL) {
                            item {
                                SereneCard(contentPadding = PaddingValues(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AccountTree,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = tr(isArabic, "معادلة مسار فلوسك بالكامل", "Complete Money Flow Equation"),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = tr(isArabic, "من لحظة الاستلام وحتى الصرف والمتبقي", "From receipt to spending and remaining balance"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    FlowEquationStepRow(
                                        icon = Icons.Outlined.ArrowDownward,
                                        tint = ext.incomeColor,
                                        bg = ext.incomeContainer,
                                        title = tr(isArabic, "1. الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج", "1. Total Money Received"),
                                        subtitle = tr(isArabic, "${primaryIncomeTx.size} عملية إضافة فلوس", "${primaryIncomeTx.size} added entries"),
                                        amountText = "+${formatAmountMinor(lifetimeIncomeMinor)} $sym",
                                        onClick = { activeSubTabName = MoneyFlowSubTab.LIFETIME_MONEY.name }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowEquationStepRow(
                                        icon = Icons.Outlined.ArrowUpward,
                                        tint = ext.expenseColor,
                                        bg = ext.expenseContainer,
                                        title = tr(isArabic, "2. كل المبالغ اللي اندفعت بالفعل", "2. Actually Paid Outflows"),
                                        subtitle = tr(isArabic, "مصاريف مدفوعة + التزامات وديون مسددة", "Paid expenses + settled dues"),
                                        amountText = "-${formatAmountMinor(paidExpensesMinor)} $sym",
                                        onClick = { activeSubTabName = MoneyFlowSubTab.AFTER_PAID.name }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowEquationStepRow(
                                        icon = Icons.Outlined.EventNote,
                                        tint = ext.warningAmber,
                                        bg = ext.warningAmber.copy(alpha = 0.16f),
                                        title = tr(isArabic, "3. فلوسك الموجودة عليك المفروض تدفعها", "3. Amounts Due to Pay Soon"),
                                        subtitle = tr(isArabic, "$pendingObligationsCount مبالغ لسه هتندفع قريباً", "$pendingObligationsCount upcoming items"),
                                        amountText = "-${formatAmountMinor(pendingObligationsMinor)} $sym",
                                        onClick = { activeSubTabName = MoneyFlowSubTab.ALL_DUE_ON_YOU.name }
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.7f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tr(isArabic, "الباقي في محفظتي الآن (بعد المدفوع)", "Wallet Remaining (After Paid)"),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${formatAmountMinor(afterActuallyPaidMinor)} $sym",
                                            style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                                            color = if (afterActuallyPaidMinor >= 0L) ext.incomeColor else ext.expenseColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tr(isArabic, "الباقي لو حسبت اللي هيندفع قريباً", "Remaining After Upcoming"),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${formatAmountMinor(remainingAvailableMinor)} $sym",
                                            style = AmountBodyStyle.copy(
                                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (remainingAvailableMinor >= 0L) ext.incomeColor else ext.expenseColor
                                        )
                                    }
                                }
                            }
                        }

                        if (activeScope == BreakdownScopeTab.ALL || activeScope == BreakdownScopeTab.INCOMING) {
                            item {
                                FlowBreakdownCard(
                                    title = tr(isArabic, "الفلوس جات منين وإزاي؟", "Where Did Your Money Come From?"),
                                    subtitle = tr(isArabic, "تفصيل كل مصادر الفلوس اللي دخلتلك (اضغط لعرض الكل)", "Breakdown by income source (tap to view all)"),
                                    emptyText = tr(isArabic, "لم يتم تسجيل أي مبالغ مضافة بعد", "No added money entries yet"),
                                    items = incomeByReason,
                                    currencySymbol = sym,
                                    accentColor = ext.incomeColor,
                                    accentBg = ext.incomeContainer,
                                    icon = Icons.Outlined.Payments,
                                    isArabic = isArabic,
                                    onHeaderClick = {
                                        onOpenDrillDown(
                                            FlowDrillDownSpec(
                                                kind = FlowDrillDownKind.ALL_INCOMING_BY_WAY,
                                                title = tr(isArabic, "الفلوس جات منين وإزاي؟ (الكل)", "All Incoming Money Sources"),
                                                subtitle = tr(isArabic, "كل العمليات المجمعة لكل طرق استلام الفلوس", "All aggregated incoming operations"),
                                                currency = primaryCurrency
                                            )
                                        )
                                    },
                                    onItemClick = { item ->
                                        onOpenDrillDown(
                                            FlowDrillDownSpec(
                                                kind = FlowDrillDownKind.SINGLE_INCOMING_WAY,
                                                title = tr(isArabic, "مصدر الفلوس: ${item.title}", "Income Source: ${item.title}"),
                                                subtitle = tr(isArabic, "كل العمليات المسجلة تحت هذا المصدر", "All transactions under this source"),
                                                filterValue = item.title,
                                                currency = primaryCurrency
                                            )
                                        )
                                    }
                                )
                            }

                            if (incomeByChannel.isNotEmpty()) {
                                item {
                                    FlowBreakdownCard(
                                        title = tr(isArabic, "استلمت الفلوس عن طريق إيه؟", "How Was Money Received?"),
                                        subtitle = tr(isArabic, "توزيع المبالغ حسب وسيلة الاستلام / الحساب (اضغط لعرض الكل)", "Breakdown by receiving account (tap to view all)"),
                                        emptyText = "",
                                        items = incomeByChannel,
                                        currencySymbol = sym,
                                        accentColor = MaterialTheme.colorScheme.primary,
                                        accentBg = MaterialTheme.colorScheme.primaryContainer,
                                        icon = Icons.Outlined.AccountBalanceWallet,
                                        isArabic = isArabic,
                                        onHeaderClick = {
                                            onOpenDrillDown(
                                                FlowDrillDownSpec(
                                                    kind = FlowDrillDownKind.ALL_INCOMING_BY_CHANNEL,
                                                    title = tr(isArabic, "استلمت الفلوس عن طريق إيه؟ (الكل)", "All Receiving Channels"),
                                                    subtitle = tr(isArabic, "كل الفلوس المستلمة موزعة حسب الحساب والمحفظة", "All incoming money by receiving account"),
                                                    currency = primaryCurrency
                                                )
                                            )
                                        },
                                        onItemClick = { item ->
                                            onOpenDrillDown(
                                                FlowDrillDownSpec(
                                                    kind = FlowDrillDownKind.SINGLE_INCOMING_CHANNEL,
                                                    title = tr(isArabic, "استلام عن طريق: ${item.title}", "Received Via: ${item.title}"),
                                                    subtitle = tr(isArabic, "كل المبالغ المستلمة على هذا الحساب/الوسيلة", "All amounts received via this channel"),
                                                    filterValue = item.title,
                                                    currency = primaryCurrency
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        if (activeScope == BreakdownScopeTab.ALL || activeScope == BreakdownScopeTab.OUTGOING) {
                            item {
                                FlowBreakdownCard(
                                    title = tr(isArabic, "الفلوس راحت فين؟", "Where Did Your Money Go?"),
                                    subtitle = tr(isArabic, "تفصيل كل المصاريف والالتزامات المدفوعة (اضغط لعرض الكل)", "Breakdown of every expense & paid item (tap to view all)"),
                                    emptyText = tr(isArabic, "لا توجد مصاريف مدفوعة مسجلة حالياً", "No paid expenses recorded yet"),
                                    items = expensesByDestination,
                                    currencySymbol = sym,
                                    accentColor = ext.expenseColor,
                                    accentBg = ext.expenseContainer,
                                    icon = Icons.Outlined.ArrowUpward,
                                    isArabic = isArabic,
                                    onHeaderClick = {
                                        onOpenDrillDown(
                                            FlowDrillDownSpec(
                                                kind = FlowDrillDownKind.ALL_OUTGOING_BY_DESTINATION,
                                                title = tr(isArabic, "الفلوس راحت فين؟ (الكل)", "All Outgoing Destinations"),
                                                subtitle = tr(isArabic, "كل المصاريف والالتزامات المدفوعة مجمعة", "All paid expenses and obligations aggregated"),
                                                currency = primaryCurrency
                                            )
                                        )
                                    },
                                    onItemClick = { item ->
                                        onOpenDrillDown(
                                            FlowDrillDownSpec(
                                                kind = FlowDrillDownKind.SINGLE_OUTGOING_DESTINATION,
                                                title = tr(isArabic, "الفلوس راحت في: ${item.title}", "Money Spent On: ${item.title}"),
                                                subtitle = tr(isArabic, "كل عمليات الصرف المسجلة لهذا البند فقط", "All spending entries for this destination"),
                                                filterValue = item.title,
                                                currency = primaryCurrency
                                            )
                                        )
                                    }
                                )
                            }

                            if (expensesByChannel.isNotEmpty()) {
                                item {
                                    FlowBreakdownCard(
                                        title = tr(isArabic, "دفعت المصاريف عن طريق إيه؟", "Which Accounts Were Used to Pay?"),
                                        subtitle = tr(isArabic, "توزيع المدفوعات حسب الحساب / المحفظة (اضغط لعرض الكل)", "Outgoing flow per account (tap to view all)"),
                                        emptyText = "",
                                        items = expensesByChannel,
                                        currencySymbol = sym,
                                        accentColor = ext.warningAmber,
                                        accentBg = ext.warningAmber.copy(alpha = 0.16f),
                                        icon = Icons.Outlined.AccountBalanceWallet,
                                        isArabic = isArabic,
                                        onHeaderClick = {
                                            onOpenDrillDown(
                                                FlowDrillDownSpec(
                                                    kind = FlowDrillDownKind.ALL_OUTGOING_BY_CHANNEL,
                                                    title = tr(isArabic, "دفعت المصاريف عن طريق إيه؟ (الكل)", "All Payment Channels"),
                                                    subtitle = tr(isArabic, "كل المدفوعات والمصاريف مجمعة حسب الحساب", "All outgoing payments across channels"),
                                                    currency = primaryCurrency
                                                )
                                            )
                                        },
                                        onItemClick = { item ->
                                            onOpenDrillDown(
                                                FlowDrillDownSpec(
                                                    kind = FlowDrillDownKind.SINGLE_OUTGOING_CHANNEL,
                                                    title = tr(isArabic, "مدفوعات عن طريق: ${item.title}", "Paid Via: ${item.title}"),
                                                    subtitle = tr(isArabic, "كل المبالغ اللي اندفعت من هذا الحساب/المحفظة فقط", "All expenses paid through this account"),
                                                    filterValue = item.title,
                                                    currency = primaryCurrency
                                                )
                                            )
                                        }
                                    )
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
private fun PlanVerticalTabCard(
    title: String,
    statusText: String,
    subtitle: String,
    amountText: String,
    amountColor: Color,
    accentColor: Color,
    accentBg: Color,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = ext.cardBackground,
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.35f)),
        shadowElevation = if (ext.isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(accentBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accentBg.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = amountText,
                    style = AmountBodyStyle.copy(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = amountColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun FlowEquationStepRow(
    icon: ImageVector,
    tint: Color,
    bg: Color,
    title: String,
    subtitle: String,
    amountText: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = ext.cardSubtleBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 10.dp),
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
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = amountText,
                style = AmountBodyStyle.copy(
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = tint,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FlowBreakdownCard(
    title: String,
    subtitle: String,
    emptyText: String,
    items: List<FlowBreakdownItem>,
    currencySymbol: String,
    accentColor: Color,
    accentBg: Color,
    icon: ImageVector,
    isArabic: Boolean,
    onHeaderClick: () -> Unit = {},
    onItemClick: (FlowBreakdownItem) -> Unit = {}
) {
    val ext = LocalSereneColors.current
    SereneCard(contentPadding = PaddingValues(15.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onHeaderClick)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = tr(isArabic, "عرض كل العمليات مجمعة", "View all aggregated"),
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ext.cardSubtleBackground.copy(alpha = 0.75f))
                            .clickable { onItemClick(entry) }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${entry.title} (${tr(isArabic, "${entry.count} عملية", "${entry.count}")})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "${formatAmountMinor(entry.amountMinor, false)} $currencySymbol (${entry.sharePct.toInt()}%)",
                                    style = AmountBodyStyle.copy(
                                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = accentColor,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(ext.cardContainer)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((entry.sharePct / 100f).coerceIn(0.03f, 1f))
                                    .height(6.dp)
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
