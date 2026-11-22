package com.example.ui

import com.example.data.ObligationEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.util.currentIsoDate
import com.example.util.currentMonthRange
import com.example.util.currentWeekRange
import com.example.util.divideMinorHalfUp
import com.example.util.percentFloatExact
import com.example.util.percentHalfUp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

enum class ComparisonOperator(val symbol: String, val labelAr: String, val labelEn: String) {
    NONE("الكل", "بدون شرط", "None"),
    EQ("=", "يساوي (=)", "Equals (=)"),
    GT(">", "أكبر من (>)", "Greater (>)"),
    GTE(">=", "أكبر أو يساوي (≥)", "Greater or Equal (≥)"),
    LT("<", "أصغر من (<)", "Less (<)"),
    LTE("<=", "أصغر أو يساوي (≤)", "Less or Equal (≤)"),
    BETWEEN("↔", "بين قيمتين", "Between")
}

enum class DatePreset(val labelAr: String, val labelEn: String) {
    ALL("الكل", "All"),
    TODAY("اليوم", "Today"),
    WEEK("أسبوع", "Week"),
    MONTH("الشهر الحالي", "This Month"),
    CUSTOM("مخصص", "Custom")
}

object DebtFilter {
    const val ALL = "all"
    const val NON_DEBT = "non_debt" // مدفوع / إيراد مكتسب (ليس دين)
    const val DEBT_ALL = "debt_all" // كل المديونيات عليّ (إيرادات + مصروفات)
    const val DEBT_UNPAID = "debt_unpaid" // مديونيات قائمة مستحقة السداد
    const val DEBT_INCOME = "debt_income" // مديونية إيرادات دخلت لحسابي
    const val DEBT_EXPENSE = "debt_expense" // مديونية مصروفات
    const val DEBT_SETTLED = "debt_settled" // المدفوعات — كل المديونيات التي تم سدادها
    const val PAYMENTS_INCOME = "payments_income" // مدفوعات مديونية الإيرادات
    const val PAYMENTS_EXPENSE = "payments_expense" // مدفوعات مديونية المصروفات
}

object ReminderFilter {
    const val ALL = "all"
    const val HAS_REMINDER = "has_reminder" // كل العمليات المربوطة بموعد دفع وإشعار
    const val ACTIVE = "active" // التنبيه مفعل عليها
    const val MUTED = "muted" // تم فصل التنبيه عنها
}

data class ReportFilterState(
    val type: String = "all", // "all", "expense", "income"
    val debtFilter: String = DebtFilter.ALL,
    val reminderFilter: String = ReminderFilter.ALL,
    val sources: Set<String> = emptySet(), // empty = all
    val currencies: Set<String> = emptySet(), // empty = all
    val categories: Set<String> = emptySet(), // empty = all
    val amountOp: ComparisonOperator = ComparisonOperator.NONE,
    val amountValue1Minor: Long? = null,
    val amountValue2Minor: Long? = null,
    val datePreset: DatePreset = DatePreset.ALL,
    val dateOp: ComparisonOperator = ComparisonOperator.NONE,
    val dateValue1: String = "",
    val dateValue2: String = "",
    val timeOp: ComparisonOperator = ComparisonOperator.NONE,
    val timeValue1: String = "",
    val timeValue2: String = ""
) {
    fun hasAnyActiveFilter(): Boolean {
        return type != "all" ||
            debtFilter != DebtFilter.ALL ||
            reminderFilter != ReminderFilter.ALL ||
            sources.isNotEmpty() ||
            currencies.isNotEmpty() ||
            categories.isNotEmpty() ||
            (amountOp != ComparisonOperator.NONE && amountValue1Minor != null) ||
            datePreset != DatePreset.ALL ||
            (dateOp != ComparisonOperator.NONE && dateValue1.isNotBlank()) ||
            (timeOp != ComparisonOperator.NONE && timeValue1.isNotBlank())
    }

    fun toJson(): String {
        val obj = JSONObject()
        obj.put("type", type)
        obj.put("debtFilter", debtFilter)
        obj.put("reminderFilter", reminderFilter)
        obj.put("sources", JSONArray(sources.toList()))
        obj.put("currencies", JSONArray(currencies.toList()))
        obj.put("categories", JSONArray(categories.toList()))
        obj.put("amountOp", amountOp.name)
        if (amountValue1Minor != null) obj.put("amountValue1Minor", amountValue1Minor)
        if (amountValue2Minor != null) obj.put("amountValue2Minor", amountValue2Minor)
        obj.put("datePreset", datePreset.name)
        obj.put("dateOp", dateOp.name)
        obj.put("dateValue1", dateValue1)
        obj.put("dateValue2", dateValue2)
        obj.put("timeOp", timeOp.name)
        obj.put("timeValue1", timeValue1)
        obj.put("timeValue2", timeValue2)
        return obj.toString()
    }

    companion object {
        fun fromJson(json: String): ReportFilterState {
            return try {
                val o = JSONObject(json)
                fun readSet(key: String): Set<String> {
                    val arr = o.optJSONArray(key) ?: return emptySet()
                    return buildSet {
                        for (i in 0 until arr.length()) add(arr.getString(i))
                    }
                }
                ReportFilterState(
                    type = o.optString("type", "all"),
                    debtFilter = o.optString("debtFilter", DebtFilter.ALL),
                    reminderFilter = o.optString("reminderFilter", ReminderFilter.ALL),
                    sources = readSet("sources"),
                    currencies = readSet("currencies"),
                    categories = readSet("categories"),
                    amountOp = runCatching {
                        ComparisonOperator.valueOf(o.optString("amountOp", "NONE"))
                    }.getOrDefault(ComparisonOperator.NONE),
                    amountValue1Minor = if (o.has("amountValue1Minor")) o.optLong("amountValue1Minor") else null,
                    amountValue2Minor = if (o.has("amountValue2Minor")) o.optLong("amountValue2Minor") else null,
                    datePreset = runCatching {
                        DatePreset.valueOf(o.optString("datePreset", "ALL"))
                    }.getOrDefault(DatePreset.ALL),
                    dateOp = runCatching {
                        ComparisonOperator.valueOf(o.optString("dateOp", "NONE"))
                    }.getOrDefault(ComparisonOperator.NONE),
                    dateValue1 = o.optString("dateValue1", ""),
                    dateValue2 = o.optString("dateValue2", ""),
                    timeOp = runCatching {
                        ComparisonOperator.valueOf(o.optString("timeOp", "NONE"))
                    }.getOrDefault(ComparisonOperator.NONE),
                    timeValue1 = o.optString("timeValue1", ""),
                    timeValue2 = o.optString("timeValue2", "")
                )
            } catch (_: Exception) {
                ReportFilterState()
            }
        }
    }
}

data class CategoryBreakdown(
    val category: String,
    val totalMinor: Long,
    val count: Int,
    val percentage: Float
)

data class SourceBreakdown(
    val source: String,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val totalVolumeMinor: Long,
    val count: Int,
    val sharePercent: Float,
    val cashInMinor: Long = incomeMinor,
    val cashOutMinor: Long = expenseMinor,
    val cashBalanceMinor: Long = cashInMinor - cashOutMinor
)

data class DailyTrendPoint(
    val dateIso: String,
    val shortLabel: String,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val cumulativeNetMinor: Long
)

data class WeekdayActivity(
    val dayOfWeek: Int, // Calendar.SATURDAY .. Calendar.FRIDAY
    val labelAr: String,
    val labelEn: String,
    val expenseMinor: Long,
    val incomeMinor: Long,
    val count: Int
)

data class WeeklyFlowItem(
    val weekIndex: Int, // 1..4
    val incomeMinor: Long,
    val expenseMinor: Long
)

/**
 * Scientific personal & commercial financial summary for a single isolated currency.
 *
 * Core Reconciled Identities (strictly checked in [isReconciled]):
 * 1. Cash Flow Identity:
 *    [cashBalanceMinor] = [actualCashInMinor] - [actualCashOutMinor]
 *    where:
 *      actualCashInMinor  = earnedIncome + unpaidIncomeDebt(loan received) + settledIncomeDebt(loan received)
 *      actualCashOutMinor = paidExpense + settledExpenseDebt + settledIncomeDebt(loan repaid) + paidObligations
 * 2. Free Spendable Identity (Available Without Obligations + Pending Obligations == Cash Balance):
 *    [availableWithoutObligationsMinor] = [cashBalanceMinor] - [totalPendingObligationsMinor]
 *    [availableWithoutObligationsMinor] + [totalPendingObligationsMinor] == [cashBalanceMinor]
 * 3. Commercial Operating P&L Identity (excludes loan principal borrowing/repayment):
 *    [operatingNetProfitMinor] = [earnedIncomeMinor] - [totalIncurredExpensesMinor]
 */
data class CurrencySummary(
    val currency: String,
    val totalIncomeMinor: Long,
    val incomeCount: Int,
    val totalExpenseMinor: Long,
    val expenseCount: Int,
    val netMinor: Long,
    val savingsRatePercent: Int,
    val averageTxMinor: Long,
    val dailyAverageExpenseMinor: Long,
    val activeDaysCount: Int,
    val highestTx: TransactionEntity?,
    val earnedIncomeMinor: Long = totalIncomeMinor,
    val paidExpenseMinor: Long = totalExpenseMinor,
    val unpaidExpenseDebtMinor: Long = 0L,
    val unpaidIncomeDebtMinor: Long = 0L,
    val totalUnpaidDebtMinor: Long = 0L,
    val settledDebtMinor: Long = 0L,
    val settledIncomeDebtMinor: Long = 0L,
    val settledExpenseDebtMinor: Long = 0L,
    val unpaidDebtCount: Int = 0,
    val unpaidIncomeDebtCount: Int = 0,
    val unpaidExpenseDebtCount: Int = 0,
    val settledDebtCount: Int = 0,
    val settledIncomeDebtCount: Int = 0,
    val settledExpenseDebtCount: Int = 0,
    // --- Scientific Cash-Flow, Obligations & Commercial P&L Metrics ---
    val pendingScheduledObligationsMinor: Long = 0L,
    val pendingScheduledObligationsCount: Int = 0,
    val paidScheduledObligationsMinor: Long = 0L,
    val paidScheduledObligationsCount: Int = 0,
    val totalPendingObligationsMinor: Long = totalUnpaidDebtMinor + pendingScheduledObligationsMinor,
    val actualCashInMinor: Long = earnedIncomeMinor + unpaidIncomeDebtMinor + settledIncomeDebtMinor,
    val actualCashOutMinor: Long = paidExpenseMinor + settledExpenseDebtMinor + settledIncomeDebtMinor + paidScheduledObligationsMinor,
    val cashBalanceMinor: Long = actualCashInMinor - actualCashOutMinor,
    val availableWithoutObligationsMinor: Long = cashBalanceMinor - totalPendingObligationsMinor,
    val totalIncurredExpensesMinor: Long = paidExpenseMinor + unpaidExpenseDebtMinor + settledExpenseDebtMinor + paidScheduledObligationsMinor,
    val operatingNetProfitMinor: Long = earnedIncomeMinor - totalIncurredExpensesMinor,
    val accountsCashTotalMinor: Long = cashBalanceMinor,
    val isCashReconciled: Boolean = (availableWithoutObligationsMinor + totalPendingObligationsMinor == cashBalanceMinor),
    val isProfitReconciled: Boolean = (operatingNetProfitMinor == earnedIncomeMinor - totalIncurredExpensesMinor),
    val isAccountsReconciled: Boolean = (accountsCashTotalMinor == cashBalanceMinor)
) {
    val isFullyReconciled: Boolean
        get() = isCashReconciled && isProfitReconciled && isAccountsReconciled

    val paidExpenseCount: Int
        get() = (expenseCount - unpaidExpenseDebtCount - settledExpenseDebtCount).coerceAtLeast(0)
}

data class ReportAnalytics(
    val filteredTransactions: List<TransactionEntity>,
    val primaryCurrency: String,
    val currencySummaries: List<CurrencySummary>,
    val primarySummary: CurrencySummary,
    val expenseBreakdown: List<CategoryBreakdown>,
    val incomeBreakdown: List<CategoryBreakdown>,
    val sourceBreakdown: List<SourceBreakdown>,
    val dailyTrend: List<DailyTrendPoint>,
    val weekdayActivity: List<WeekdayActivity>,
    val weeklyFlow: List<WeeklyFlowItem>,
    val filteredObligations: List<ObligationEntity> = emptyList()
)

fun applyReportFilter(
    allTransactions: List<TransactionEntity>,
    filter: ReportFilterState
): List<TransactionEntity> {
    val today = currentIsoDate()
    val (weekStart, weekEnd) = currentWeekRange()
    val (monthStart, monthEnd) = currentMonthRange()

    return allTransactions.filter { tx ->
        // 1. Type (AND)
        if (filter.type != "all" && tx.type != filter.type) return@filter false

        // 1b. Debt / Settlement Nature (AND)
        val debtMatches = when (filter.debtFilter) {
            DebtFilter.NON_DEBT -> !tx.isDebt
            DebtFilter.DEBT_ALL -> tx.isDebt
            DebtFilter.DEBT_UNPAID -> tx.isDebt && !tx.isDebtSettled
            DebtFilter.DEBT_INCOME -> tx.isDebt && !tx.isDebtSettled && tx.type == TransactionType.INCOME
            DebtFilter.DEBT_EXPENSE -> tx.isDebt && !tx.isDebtSettled && tx.type == TransactionType.EXPENSE
            DebtFilter.DEBT_SETTLED -> tx.isDebt && tx.isDebtSettled
            DebtFilter.PAYMENTS_INCOME -> tx.isDebt && tx.isDebtSettled && tx.type == TransactionType.INCOME
            DebtFilter.PAYMENTS_EXPENSE -> tx.isDebt && tx.isDebtSettled && tx.type == TransactionType.EXPENSE
            else -> true
        }
        if (!debtMatches) return@filter false

        // 1c. Payment Reminder / Notification Status (AND)
        val reminderMatches = when (filter.reminderFilter) {
            ReminderFilter.HAS_REMINDER -> tx.hasPaymentReminder
            ReminderFilter.ACTIVE -> tx.hasPaymentReminder && !tx.reminderMuted && !tx.isDebtSettled
            ReminderFilter.MUTED -> tx.hasPaymentReminder && tx.reminderMuted
            else -> true
        }
        if (!reminderMatches) return@filter false

        // 2. Sources (AND)
        if (filter.sources.isNotEmpty() && tx.source !in filter.sources) return@filter false

        // 3. Currencies (AND)
        if (filter.currencies.isNotEmpty() && tx.currency !in filter.currencies) return@filter false

        // 4. Categories (AND)
        if (filter.categories.isNotEmpty() && tx.category !in filter.categories) return@filter false

        // 5. Amount condition (AND)
        if (filter.amountOp != ComparisonOperator.NONE && filter.amountValue1Minor != null) {
            val v1 = filter.amountValue1Minor
            val v2 = filter.amountValue2Minor ?: v1
            val amountMatches = when (filter.amountOp) {
                ComparisonOperator.NONE -> true
                ComparisonOperator.EQ -> tx.amountMinor == v1
                ComparisonOperator.GT -> tx.amountMinor > v1
                ComparisonOperator.GTE -> tx.amountMinor >= v1
                ComparisonOperator.LT -> tx.amountMinor < v1
                ComparisonOperator.LTE -> tx.amountMinor <= v1
                ComparisonOperator.BETWEEN -> tx.amountMinor in minOf(v1, v2)..maxOf(v1, v2)
            }
            if (!amountMatches) return@filter false
        }

        // 6. Date condition (AND)
        val dateMatches = when (filter.datePreset) {
            DatePreset.ALL -> true
            DatePreset.TODAY -> tx.date == today
            DatePreset.WEEK -> tx.date >= weekStart && tx.date <= weekEnd
            DatePreset.MONTH -> tx.date >= monthStart && tx.date <= monthEnd
            DatePreset.CUSTOM -> {
                if (filter.dateOp == ComparisonOperator.NONE || filter.dateValue1.isBlank()) {
                    true
                } else {
                    val d1 = filter.dateValue1
                    val d2 = filter.dateValue2.ifBlank { d1 }
                    when (filter.dateOp) {
                        ComparisonOperator.NONE -> true
                        ComparisonOperator.EQ -> tx.date == d1
                        ComparisonOperator.GT -> tx.date > d1
                        ComparisonOperator.GTE -> tx.date >= d1
                        ComparisonOperator.LT -> tx.date < d1
                        ComparisonOperator.LTE -> tx.date <= d1
                        ComparisonOperator.BETWEEN -> tx.date >= minOf(d1, d2) && tx.date <= maxOf(d1, d2)
                    }
                }
            }
        }
        if (!dateMatches) return@filter false

        // 7. Time condition (AND)
        if (filter.timeOp != ComparisonOperator.NONE && filter.timeValue1.isNotBlank()) {
            val t1 = filter.timeValue1
            val t2 = filter.timeValue2.ifBlank { t1 }
            val timeMatches = when (filter.timeOp) {
                ComparisonOperator.NONE -> true
                ComparisonOperator.EQ -> tx.time == t1
                ComparisonOperator.GT -> tx.time > t1
                ComparisonOperator.GTE -> tx.time >= t1
                ComparisonOperator.LT -> tx.time < t1
                ComparisonOperator.LTE -> tx.time <= t1
                ComparisonOperator.BETWEEN -> tx.time >= minOf(t1, t2) && tx.time <= maxOf(t1, t2)
            }
            if (!timeMatches) return@filter false
        }

        true
    }
}

/**
 * Computes mathematically exact, per-currency isolated personal & commercial accounting analytics
 * reconciling transactions, debts (credit purchases & borrowed loans), and scheduled obligations.
 */
fun computeAnalytics(
    filtered: List<TransactionEntity>,
    defaultCurrency: String,
    obligations: List<ObligationEntity> = emptyList()
): ReportAnalytics {
    // Strictly isolate per currency (never sum different currencies together)
    val byCurrency = filtered.groupBy { it.currency }
    val obligationsByCurrency = obligations.groupBy { it.currency }
    val allCurrencies = (byCurrency.keys + obligationsByCurrency.keys).toSet()
    val currenciesOrder = buildList {
        if (defaultCurrency in allCurrencies) add(defaultCurrency)
        allCurrencies.filter { it != defaultCurrency }.sorted().forEach { add(it) }
        if (isEmpty()) add(defaultCurrency)
    }

    fun computeSourceBreakdownForCurrency(
        txList: List<TransactionEntity>,
        obList: List<ObligationEntity>
    ): List<SourceBreakdown> {
        data class AccBucket(
            var incomeMinor: Long = 0L,
            var expenseMinor: Long = 0L,
            var cashInMinor: Long = 0L,
            var cashOutMinor: Long = 0L,
            var count: Int = 0
        )

        val map = LinkedHashMap<String, AccBucket>()
        var totalVol = 0L

        for (tx in txList) {
            val src = tx.source.ifBlank { "كاش / نقدي" }
            val bucket = map.getOrPut(src) { AccBucket() }
            val amt = tx.amountMinor.coerceAtLeast(0L)
            bucket.count++
            totalVol += amt

            if (tx.type == TransactionType.INCOME) {
                bucket.incomeMinor += amt
                // Both earned income and borrowed loan cash physically enter this account;
                // once a borrowed loan is repaid (settled), cash also leaves this account (net 0).
                bucket.cashInMinor += amt
                if (tx.isDebt && tx.isDebtSettled) {
                    bucket.cashOutMinor += amt
                }
            } else {
                bucket.expenseMinor += amt
                // Cash leaves this account ONLY for paid expenses or settled expense debts (NOT unpaid credit!)
                if (!tx.isDebt || tx.isDebtSettled) {
                    bucket.cashOutMinor += amt
                }
            }
        }

        for (ob in obList) {
            if (ob.isPaid) {
                val src = ob.paidSource.ifBlank { "كاش / نقدي" }
                val bucket = map.getOrPut(src) { AccBucket() }
                val amt = ob.amountMinor.coerceAtLeast(0L)
                bucket.expenseMinor += amt
                bucket.cashOutMinor += amt
                bucket.count++
                totalVol += amt
            }
        }

        return map.entries.map { (src, b) ->
            val vol = b.incomeMinor + b.expenseMinor
            SourceBreakdown(
                source = src,
                incomeMinor = b.incomeMinor,
                expenseMinor = b.expenseMinor,
                totalVolumeMinor = vol,
                count = b.count,
                sharePercent = percentFloatExact(vol, totalVol),
                cashInMinor = b.cashInMinor,
                cashOutMinor = b.cashOutMinor,
                cashBalanceMinor = b.cashInMinor - b.cashOutMinor
            )
        }.sortedByDescending { it.totalVolumeMinor }
    }

    val sourceBreakdownsByCurrency = HashMap<String, List<SourceBreakdown>>()

    val summaries = currenciesOrder.map { curr ->
        val list = byCurrency[curr].orEmpty()
        val obList = obligationsByCurrency[curr].orEmpty()
        val sources = computeSourceBreakdownForCurrency(list, obList)
        sourceBreakdownsByCurrency[curr] = sources

        var incSum = 0L
        var incCount = 0
        var expSum = 0L
        var expCount = 0
        var totalSum = 0L
        var highest: TransactionEntity? = null
        var earnedIncome = 0L
        var paidExpense = 0L
        var unpaidExpDebt = 0L
        var unpaidIncDebt = 0L
        var settledDebtSum = 0L
        var settledIncDebt = 0L
        var settledExpDebt = 0L
        var unpaidDebtCount = 0
        var unpaidIncDebtCount = 0
        var unpaidExpDebtCount = 0
        var settledDebtCount = 0
        var settledIncDebtCount = 0
        var settledExpDebtCount = 0
        val activeDaysSet = HashSet<String>()
        val expenseDaysSet = HashSet<String>()

        for (tx in list) {
            val amt = tx.amountMinor.coerceAtLeast(0L)
            totalSum += amt
            activeDaysSet.add(tx.date)
            if (highest == null || amt > highest.amountMinor) highest = tx

            if (tx.type == TransactionType.INCOME) {
                incSum += amt
                incCount++
                if (!tx.isDebt) earnedIncome += amt
            } else {
                expSum += amt
                expCount++
                expenseDaysSet.add(tx.date)
                if (!tx.isDebt) paidExpense += amt
            }

            if (tx.isDebt) {
                if (tx.isDebtSettled) {
                    settledDebtSum += amt
                    settledDebtCount++
                    if (tx.type == TransactionType.INCOME) {
                        settledIncDebt += amt
                        settledIncDebtCount++
                    } else {
                        settledExpDebt += amt
                        settledExpDebtCount++
                    }
                } else {
                    unpaidDebtCount++
                    if (tx.type == TransactionType.EXPENSE) {
                        unpaidExpDebt += amt
                        unpaidExpDebtCount++
                    } else {
                        unpaidIncDebt += amt
                        unpaidIncDebtCount++
                    }
                }
            }
        }

        // Scheduled Obligations («مبالغ مطلوب دفعها»)
        var pendingObsMinor = 0L
        var pendingObsCount = 0
        var paidObsMinor = 0L
        var paidObsCount = 0
        for (ob in obList) {
            val amt = ob.amountMinor.coerceAtLeast(0L)
            if (ob.isPaid) {
                paidObsMinor += amt
                paidObsCount++
            } else {
                pendingObsMinor += amt
                pendingObsCount++
            }
        }

        // --- Scientific Accounting Identities ---
        val totalUnpaidDebt = unpaidExpDebt + unpaidIncDebt
        val totalPendingObligations = pendingObsMinor + totalUnpaidDebt

        // Actual Cash Basis:
        // Cash IN  = Earned Revenue + Loan Cash Received (unpaid & settled)
        // Cash OUT = Paid Regular Expenses + Settled Expense Debts + Repaid Loans + Paid Obligations
        val actualCashIn = earnedIncome + unpaidIncDebt + settledIncDebt
        val actualCashOut = paidExpense + settledExpDebt + settledIncDebt + paidObsMinor
        val cashBalance = actualCashIn - actualCashOut

        // Available Spendable Free of ALL Obligations:
        // Immediately decreases when an obligation or debt is added, and stays invariant when paid!
        val availableWithoutObligations = cashBalance - totalPendingObligations

        // Commercial Operating P&L (Accrual):
        // Excludes loan principal (borrowing & repayment are balance-sheet financing, not profit/loss)
        val totalIncurredExpenses = paidExpense + unpaidExpDebt + settledExpDebt + paidObsMinor
        val operatingNetProfit = earnedIncome - totalIncurredExpenses

        // True Savings / Operating Margin against real Earned Income (or fallback to gross income if only debt)
        val marginBase = if (earnedIncome > 0L) earnedIncome else incSum
        val savingsRate = if (marginBase > 0L) {
            percentHalfUp(operatingNetProfit, marginBase, minVal = -999, maxVal = 100)
        } else 0

        val avgTx = divideMinorHalfUp(totalSum, list.size)
        val dailyAvgExp = divideMinorHalfUp(expSum, expenseDaysSet.size)
        val accountsCashSum = sources.sumOf { it.cashBalanceMinor }

        CurrencySummary(
            currency = curr,
            totalIncomeMinor = incSum,
            incomeCount = incCount,
            totalExpenseMinor = expSum + paidObsMinor,
            expenseCount = expCount + paidObsCount,
            netMinor = availableWithoutObligations,
            savingsRatePercent = savingsRate,
            averageTxMinor = avgTx,
            dailyAverageExpenseMinor = dailyAvgExp,
            activeDaysCount = activeDaysSet.size,
            highestTx = highest,
            earnedIncomeMinor = earnedIncome,
            paidExpenseMinor = paidExpense,
            unpaidExpenseDebtMinor = unpaidExpDebt,
            unpaidIncomeDebtMinor = unpaidIncDebt,
            totalUnpaidDebtMinor = totalUnpaidDebt,
            settledDebtMinor = settledDebtSum,
            settledIncomeDebtMinor = settledIncDebt,
            settledExpenseDebtMinor = settledExpDebt,
            unpaidDebtCount = unpaidDebtCount,
            unpaidIncomeDebtCount = unpaidIncDebtCount,
            unpaidExpenseDebtCount = unpaidExpDebtCount,
            settledDebtCount = settledDebtCount,
            settledIncomeDebtCount = settledIncDebtCount,
            settledExpenseDebtCount = settledExpDebtCount,
            pendingScheduledObligationsMinor = pendingObsMinor,
            pendingScheduledObligationsCount = pendingObsCount,
            paidScheduledObligationsMinor = paidObsMinor,
            paidScheduledObligationsCount = paidObsCount,
            totalPendingObligationsMinor = totalPendingObligations,
            actualCashInMinor = actualCashIn,
            actualCashOutMinor = actualCashOut,
            cashBalanceMinor = cashBalance,
            availableWithoutObligationsMinor = availableWithoutObligations,
            totalIncurredExpensesMinor = totalIncurredExpenses,
            operatingNetProfitMinor = operatingNetProfit,
            accountsCashTotalMinor = accountsCashSum,
            isCashReconciled = (availableWithoutObligations + totalPendingObligations == cashBalance),
            isProfitReconciled = (operatingNetProfit == earnedIncome - totalIncurredExpenses),
            isAccountsReconciled = (accountsCashSum == cashBalance)
        )
    }

    val primaryCurrency = currenciesOrder.first()
    val primarySummary = summaries.first()
    val primaryTx = byCurrency[primaryCurrency].orEmpty()
    val primaryObs = obligationsByCurrency[primaryCurrency].orEmpty()

    // 1. Category breakdown (Expenses & Income separately, exact BigDecimal percentages)
    fun buildCategoryBreakdown(items: List<TransactionEntity>): List<CategoryBreakdown> {
        var total = 0L
        for (i in items) total += i.amountMinor.coerceAtLeast(0L)
        if (total <= 0L) return emptyList()
        return items
            .groupBy { it.category.ifBlank { "غير محدد" } }
            .map { (cat, list) ->
                var sum = 0L
                for (t in list) sum += t.amountMinor.coerceAtLeast(0L)
                CategoryBreakdown(
                    category = cat,
                    totalMinor = sum,
                    count = list.size,
                    percentage = percentFloatExact(sum, total)
                )
            }
            .sortedByDescending { it.totalMinor }
    }

    val primaryExpenses = ArrayList<TransactionEntity>()
    val primaryIncomes = ArrayList<TransactionEntity>()
    for (tx in primaryTx) {
        if (tx.type == TransactionType.EXPENSE) primaryExpenses.add(tx)
        else primaryIncomes.add(tx)
    }

    val expenseBreakdown = buildCategoryBreakdown(primaryExpenses)
    val incomeBreakdown = buildCategoryBreakdown(primaryIncomes)

    // 2. Source / Account breakdown (already reconciled with cashBalanceMinor!)
    val sourceBreakdown = sourceBreakdownsByCurrency[primaryCurrency].orEmpty()

    // 3. Daily Trend (Reconciled cash flow sorted ascending by date, up to latest 14 distinct active days)
    val dailyInMap = java.util.TreeMap<String, Long>()
    val dailyOutMap = java.util.TreeMap<String, Long>()
    for (tx in primaryTx) {
        val d = tx.date
        val amt = tx.amountMinor.coerceAtLeast(0L)
        if (tx.type == TransactionType.INCOME) {
            dailyInMap[d] = (dailyInMap[d] ?: 0L) + amt
            if (tx.isDebt && tx.isDebtSettled) {
                dailyOutMap[d] = (dailyOutMap[d] ?: 0L) + amt
            }
        } else {
            if (!tx.isDebt || tx.isDebtSettled) {
                dailyOutMap[d] = (dailyOutMap[d] ?: 0L) + amt
            } else {
                // Ensure the date still appears in timeline if only unpaid debt occurred on it
                dailyOutMap.putIfAbsent(d, 0L)
            }
        }
    }
    for (ob in primaryObs) {
        if (ob.isPaid) {
            val d = ob.paidDate.ifBlank { ob.dueDate }
            dailyOutMap[d] = (dailyOutMap[d] ?: 0L) + ob.amountMinor.coerceAtLeast(0L)
        }
    }
    val allDates = (dailyInMap.keys + dailyOutMap.keys).toSortedSet()
    var runningNet = 0L
    val allDailyPoints = allDates.map { dateIso ->
        val inc = dailyInMap[dateIso] ?: 0L
        val exp = dailyOutMap[dateIso] ?: 0L
        runningNet += (inc - exp)
        val shortLabel = if (dateIso.length >= 10) "${dateIso.substring(8, 10)}/${dateIso.substring(5, 7)}" else dateIso
        DailyTrendPoint(
            dateIso = dateIso,
            shortLabel = shortLabel,
            incomeMinor = inc,
            expenseMinor = exp,
            cumulativeNetMinor = runningNet
        )
    }
    val dailyTrend = if (allDailyPoints.size > 14) allDailyPoints.takeLast(14) else allDailyPoints

    // 4. Weekday Activity (Saturday -> Friday) & Weekly Flow using fast integer calendar math
    val orderedDays = listOf(
        Triple(Calendar.SATURDAY, "سبت", "Sat"),
        Triple(Calendar.SUNDAY, "أحد", "Sun"),
        Triple(Calendar.MONDAY, "إثنين", "Mon"),
        Triple(Calendar.TUESDAY, "ثلاثاء", "Tue"),
        Triple(Calendar.WEDNESDAY, "أربعاء", "Wed"),
        Triple(Calendar.THURSDAY, "خميس", "Thu"),
        Triple(Calendar.FRIDAY, "جمعة", "Fri")
    )
    val dowExpense = LongArray(8)
    val dowIncome = LongArray(8)
    val dowCount = IntArray(8)
    val weekIncome = LongArray(5)
    val weekExpense = LongArray(5)

    for (tx in primaryTx) {
        val dateIso = tx.date
        val year = if (dateIso.length >= 4) dateIso.substring(0, 4).toIntOrNull() else null
        val month = if (dateIso.length >= 7) dateIso.substring(5, 7).toIntOrNull() else null
        val day = if (dateIso.length >= 10) dateIso.substring(8, 10).toIntOrNull() else null

        val dow = if (year != null && month != null && day != null) {
            fastCalendarDayOfWeek(year, month, day)
        } else {
            Calendar.SATURDAY
        }
        val d = (day ?: 1).coerceIn(1, 31)
        val week = when {
            d <= 7 -> 1
            d <= 14 -> 2
            d <= 21 -> 3
            else -> 4
        }

        val amt = tx.amountMinor.coerceAtLeast(0L)
        dowCount[dow]++
        if (tx.type == TransactionType.INCOME) {
            dowIncome[dow] += amt
            weekIncome[week] += amt
        } else {
            dowExpense[dow] += amt
            weekExpense[week] += amt
        }
    }

    val weekdayActivity = orderedDays.map { (dow, ar, en) ->
        WeekdayActivity(
            dayOfWeek = dow,
            labelAr = ar,
            labelEn = en,
            expenseMinor = dowExpense[dow],
            incomeMinor = dowIncome[dow],
            count = dowCount[dow]
        )
    }

    // 5. Weekly flow (Weeks 1..4)
    val weeklyFlow = (1..4).map { week ->
        WeeklyFlowItem(
            weekIndex = week,
            incomeMinor = weekIncome[week],
            expenseMinor = weekExpense[week]
        )
    }

    return ReportAnalytics(
        filteredTransactions = filtered,
        primaryCurrency = primaryCurrency,
        currencySummaries = summaries,
        primarySummary = primarySummary,
        expenseBreakdown = expenseBreakdown,
        incomeBreakdown = incomeBreakdown,
        sourceBreakdown = sourceBreakdown,
        dailyTrend = dailyTrend,
        weekdayActivity = weekdayActivity,
        weeklyFlow = weeklyFlow,
        filteredObligations = obligations
    )
}

/**
 * Sakamoto's algorithm returning java.util.Calendar.SUNDAY (1) .. SATURDAY (7) with zero heap allocations.
 */
private val SakamotoOffsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
private fun fastCalendarDayOfWeek(year: Int, month: Int, day: Int): Int {
    val m = month.coerceIn(1, 12)
    val y = if (m < 3) year - 1 else year
    val dow0 = (y + y / 4 - y / 100 + y / 400 + SakamotoOffsets[m - 1] + day) % 7 // 0=Sun..6=Sat
    return (if (dow0 < 0) dow0 + 7 else dow0) + 1 // 1=Calendar.SUNDAY..7=Calendar.SATURDAY
}
