package com.example.ui

import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.util.currentIsoDate
import com.example.util.currentMonthRange
import com.example.util.currentWeekRange
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

data class ReportFilterState(
    val type: String = "all", // "all", "expense", "income"
    val debtFilter: String = DebtFilter.ALL,
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
    val sharePercent: Float
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
    val settledExpenseDebtCount: Int = 0
)

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
    val weeklyFlow: List<WeeklyFlowItem>
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

fun computeAnalytics(
    filtered: List<TransactionEntity>,
    defaultCurrency: String
): ReportAnalytics {
    // Group per currency without conversion (PDF Section 10 rule)
    val byCurrency = filtered.groupBy { it.currency }
    val currenciesOrder = buildList {
        if (byCurrency.containsKey(defaultCurrency)) add(defaultCurrency)
        byCurrency.keys.filter { it != defaultCurrency }.sorted().forEach { add(it) }
        if (isEmpty()) add(defaultCurrency)
    }

    val summaries = currenciesOrder.map { curr ->
        val list = byCurrency[curr].orEmpty()
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
            val amt = tx.amountMinor
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

        val net = incSum - expSum
        val savingsRate = if (incSum > 0) {
            ((net.toDouble() / incSum.toDouble()) * 100.0).toInt().coerceIn(-999, 100)
        } else 0
        val avgTx = if (list.isNotEmpty()) totalSum / list.size else 0L
        val dailyAvgExp = if (expenseDaysSet.isNotEmpty()) expSum / expenseDaysSet.size else 0L
        val totalUnpaidDebt = unpaidExpDebt + unpaidIncDebt

        CurrencySummary(
            currency = curr,
            totalIncomeMinor = incSum,
            incomeCount = incCount,
            totalExpenseMinor = expSum,
            expenseCount = expCount,
            netMinor = net,
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
            settledExpenseDebtCount = settledExpDebtCount
        )
    }

    val primaryCurrency = currenciesOrder.first()
    val primarySummary = summaries.first()
    val primaryTx = byCurrency[primaryCurrency].orEmpty()

    // 1. Category breakdown (Expenses & Income separately)
    fun buildCategoryBreakdown(items: List<TransactionEntity>): List<CategoryBreakdown> {
        var total = 0L
        for (i in items) total += i.amountMinor
        if (total <= 0L) return emptyList()
        return items
            .groupBy { it.category.ifBlank { "غير محدد" } }
            .map { (cat, list) ->
                var sum = 0L
                for (t in list) sum += t.amountMinor
                CategoryBreakdown(
                    category = cat,
                    totalMinor = sum,
                    count = list.size,
                    percentage = (sum.toFloat() / total.toFloat()) * 100f
                )
            }
            .sortedByDescending { it.totalMinor }
    }

    val primaryExpenses = ArrayList<TransactionEntity>()
    val primaryIncomes = ArrayList<TransactionEntity>()
    var totalVolume = 0L
    for (tx in primaryTx) {
        totalVolume += tx.amountMinor
        if (tx.type == TransactionType.EXPENSE) primaryExpenses.add(tx)
        else primaryIncomes.add(tx)
    }
    val safeTotalVolume = totalVolume.coerceAtLeast(1L)

    val expenseBreakdown = buildCategoryBreakdown(primaryExpenses)
    val incomeBreakdown = buildCategoryBreakdown(primaryIncomes)

    // 2. Source / Account breakdown
    val sourceBreakdown = primaryTx
        .groupBy { it.source.ifBlank { "كاش / نقدي" } }
        .map { (src, list) ->
            var inc = 0L
            var exp = 0L
            for (t in list) {
                if (t.type == TransactionType.INCOME) inc += t.amountMinor else exp += t.amountMinor
            }
            val vol = inc + exp
            SourceBreakdown(
                source = src,
                incomeMinor = inc,
                expenseMinor = exp,
                totalVolumeMinor = vol,
                count = list.size,
                sharePercent = (vol.toFloat() / safeTotalVolume.toFloat()) * 100f
            )
        }
        .sortedByDescending { it.totalVolumeMinor }

    // 3. Daily Trend (sorted ascending by date, up to latest 14 distinct active days)
    val sortedByDate = primaryTx.groupBy { it.date }.toSortedMap()
    var runningNet = 0L
    val allDailyPoints = sortedByDate.map { (dateIso, list) ->
        var inc = 0L
        var exp = 0L
        for (t in list) {
            if (t.type == TransactionType.INCOME) inc += t.amountMinor else exp += t.amountMinor
        }
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

    // 4. Weekday Activity (Saturday -> Friday) & Weekly Flow using fast integer calendar math (zero SimpleDateFormat)
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

        dowCount[dow]++
        if (tx.type == TransactionType.INCOME) {
            dowIncome[dow] += tx.amountMinor
            weekIncome[week] += tx.amountMinor
        } else {
            dowExpense[dow] += tx.amountMinor
            weekExpense[week] += tx.amountMinor
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
        weeklyFlow = weeklyFlow
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
