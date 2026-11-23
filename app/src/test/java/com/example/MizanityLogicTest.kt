package com.example

import com.example.data.ObligationEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.ComparisonOperator
import com.example.ui.DatePreset
import com.example.ui.DebtFilter
import com.example.ui.ReportFilterState
import com.example.ui.applyObligationsReportFilter
import com.example.ui.applyReportFilter
import com.example.ui.computeAnalytics
import com.example.util.formatAmountMinor
import com.example.util.parseAmountToMinor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MizanityLogicTest {

    private val sampleTx = listOf(
        TransactionEntity(
            id = 1,
            type = TransactionType.EXPENSE,
            amountMinor = 32000L, // 320.00
            currency = "EGP",
            source = "فودافون كاش",
            category = "طعام ومشروبات",
            date = "2026-11-10",
            time = "14:40"
        ),
        TransactionEntity(
            id = 2,
            type = TransactionType.INCOME,
            amountMinor = 850000L, // 8500.00
            currency = "EGP",
            source = "بنك مصر",
            category = "راتب شهري",
            date = "2026-11-01",
            time = "11:15"
        ),
        TransactionEntity(
            id = 3,
            type = TransactionType.EXPENSE,
            amountMinor = 65000L, // 650.00
            currency = "EGP",
            source = "كاش / نقدي",
            category = "مواصلات ونقل",
            date = "2026-11-18",
            time = "16:00"
        ),
        TransactionEntity(
            id = 4,
            type = TransactionType.EXPENSE,
            amountMinor = 10000L, // 100.00 USD
            currency = "USD",
            source = "كاش / نقدي",
            category = "تسوق ومشتريات",
            date = "2026-11-19",
            time = "09:30"
        )
    )

    @Test
    fun parsesArabicAndWesternNumeralsToMinorUnits() {
        assertEquals(12550L, parseAmountToMinor("125.50"))
        assertEquals(12550L, parseAmountToMinor("١٢٥٫٥٠"))
        assertEquals(50000L, parseAmountToMinor("500"))
        assertNull(parseAmountToMinor("abc"))
        assertEquals("7,530.00", formatAmountMinor(753000L))
    }

    @Test
    fun filtersCombineWithStrictAndLogic() {
        val filter = ReportFilterState(
            type = TransactionType.EXPENSE,
            currencies = setOf("EGP"),
            amountOp = ComparisonOperator.GTE,
            amountValue1Minor = 50000L,
            datePreset = DatePreset.CUSTOM,
            dateOp = ComparisonOperator.BETWEEN,
            dateValue1 = "2026-11-01",
            dateValue2 = "2026-11-20",
            timeOp = ComparisonOperator.GT,
            timeValue1 = "15:00"
        )
        val result = applyReportFilter(sampleTx, filter)
        assertEquals(1, result.size)
        assertEquals(3L, result.first().id)
    }

    @Test
    fun analyticsSeparatesCurrenciesWithoutConversion() {
        val analytics = computeAnalytics(sampleTx, "EGP")
        assertEquals("EGP", analytics.primaryCurrency)
        assertEquals(850000L, analytics.primarySummary.totalIncomeMinor)
        assertEquals(97000L, analytics.primarySummary.totalExpenseMinor)
        assertEquals(753000L, analytics.primarySummary.netMinor)
        assertEquals(2, analytics.currencySummaries.size)
    }

    @Test
    fun expandedDashboardAnalyticsReactsToFilters() {
        // Full EGP dataset: 3 transactions (1 income 8500, 2 expenses 320 & 650)
        val full = computeAnalytics(sampleTx, "EGP")
        assertEquals(2, full.expenseBreakdown.size)
        assertEquals(1, full.incomeBreakdown.size)
        assertEquals(3, full.sourceBreakdown.size)
        assertEquals(3, full.dailyTrend.size)
        assertEquals(7, full.weekdayActivity.size)
        assertEquals(850000L, full.primarySummary.highestTx?.amountMinor)
        assertEquals(3, full.primarySummary.activeDaysCount)
        assertEquals(48500L, full.primarySummary.dailyAverageExpenseMinor) // 97000 / 2 expense days
        // Cumulative trend ends at net 753000 (850000 - 32000 - 65000)
        assertEquals(753000L, full.dailyTrend.last().cumulativeNetMinor)

        // Filtered to Expenses + Vodafone Cash only -> Dashboard updates to 1 tx
        val filteredList = applyReportFilter(
            sampleTx,
            ReportFilterState(
                type = TransactionType.EXPENSE,
                sources = setOf("فودافون كاش")
            )
        )
        val filteredAnalytics = computeAnalytics(filteredList, "EGP")
        assertEquals(0L, filteredAnalytics.primarySummary.totalIncomeMinor)
        assertEquals(32000L, filteredAnalytics.primarySummary.totalExpenseMinor)
        assertEquals(32000L, filteredAnalytics.primarySummary.averageTxMinor)
        assertEquals(1, filteredAnalytics.sourceBreakdown.size)
        assertEquals("فودافون كاش", filteredAnalytics.sourceBreakdown.first().source)
        assertEquals(1, filteredAnalytics.dailyTrend.size)
        assertEquals(-32000L, filteredAnalytics.dailyTrend.first().cumulativeNetMinor)
    }

    @Test
    fun debtFilteringAndSummaryAnalyticsWorkCorrectly() {
        val mixedTx = listOf(
            // Paid expense (not debt)
            TransactionEntity(
                id = 10,
                type = TransactionType.EXPENSE,
                amountMinor = 25000L, // 250.00
                currency = "EGP",
                source = "كاش / نقدي",
                category = "طعام ومشروبات",
                itemName = "غداء",
                isDebt = false,
                date = "2026-11-10",
                time = "13:00"
            ),
            // Unpaid expense debt
            TransactionEntity(
                id = 11,
                type = TransactionType.EXPENSE,
                amountMinor = 150000L, // 1500.00
                currency = "EGP",
                source = "بطاقة ائتمان",
                category = "تسوق ومشتريات",
                itemName = "سماعة لاسلكية",
                debtParty = "متجر إلكترونيات",
                isDebt = true,
                isDebtSettled = false,
                date = "2026-11-11",
                time = "18:00"
            ),
            // Earned income (not debt)
            TransactionEntity(
                id = 12,
                type = TransactionType.INCOME,
                amountMinor = 900000L, // 9000.00
                currency = "EGP",
                source = "بنك مصر",
                category = "راتب شهري",
                itemName = "راتب نوفمبر",
                isDebt = false,
                date = "2026-11-01",
                time = "10:00"
            ),
            // Borrowed income (income as debt owed)
            TransactionEntity(
                id = 13,
                type = TransactionType.INCOME,
                amountMinor = 200000L, // 2000.00
                currency = "EGP",
                source = "فودافون كاش",
                category = "سلفة شخصية",
                itemName = "سلفة إيجار",
                debtParty = "أحمد",
                isDebt = true,
                isDebtSettled = false,
                date = "2026-11-12",
                time = "12:00"
            ),
            // Settled expense debt
            TransactionEntity(
                id = 14,
                type = TransactionType.EXPENSE,
                amountMinor = 50000L, // 500.00
                currency = "EGP",
                source = "كاش / نقدي",
                category = "فواتير والتزامات",
                itemName = "فاتورة كهرباء",
                debtParty = "زميل العمل",
                isDebt = true,
                isDebtSettled = true,
                date = "2026-11-05",
                time = "09:00"
            )
        )

        val analytics = computeAnalytics(mixedTx, "EGP")
        val s = analytics.primarySummary
        assertEquals(900000L, s.earnedIncomeMinor)
        assertEquals(25000L, s.paidExpenseMinor)
        assertEquals(150000L, s.unpaidExpenseDebtMinor)
        assertEquals(200000L, s.unpaidIncomeDebtMinor)
        assertEquals(350000L, s.totalUnpaidDebtMinor)
        assertEquals(50000L, s.settledDebtMinor)
        assertEquals(2, s.unpaidDebtCount)
        assertEquals(1, s.unpaidIncomeDebtCount)
        assertEquals(1, s.unpaidExpenseDebtCount)
        assertEquals(1, s.settledDebtCount)
        assertEquals(0, s.settledIncomeDebtCount)
        assertEquals(1, s.settledExpenseDebtCount)
        assertEquals(50000L, s.settledExpenseDebtMinor)

        // Filter tests
        val nonDebts = applyReportFilter(mixedTx, ReportFilterState(debtFilter = DebtFilter.NON_DEBT))
        assertEquals(listOf(10L, 12L), nonDebts.map { it.id })

        val unpaidDebts = applyReportFilter(mixedTx, ReportFilterState(debtFilter = DebtFilter.DEBT_UNPAID))
        assertEquals(listOf(11L, 13L), unpaidDebts.map { it.id })

        val settledDebts = applyReportFilter(mixedTx, ReportFilterState(debtFilter = DebtFilter.DEBT_SETTLED))
        assertEquals(listOf(14L), settledDebts.map { it.id })

        val allDebts = applyReportFilter(mixedTx, ReportFilterState(debtFilter = DebtFilter.DEBT_ALL))
        assertEquals(listOf(11L, 13L, 14L), allDebts.map { it.id })

        val incomeDebts = applyReportFilter(mixedTx, ReportFilterState(debtFilter = DebtFilter.DEBT_INCOME))
        assertEquals(listOf(13L), incomeDebts.map { it.id })

        // Unpaid expense debts only (paid debt id=14 moves to Payments!)
        val expenseDebts = applyReportFilter(mixedTx, ReportFilterState(debtFilter = DebtFilter.DEBT_EXPENSE))
        assertEquals(listOf(11L), expenseDebts.map { it.id })

        val paidExpenseDebts = applyReportFilter(mixedTx, ReportFilterState(debtFilter = DebtFilter.PAYMENTS_EXPENSE))
        assertEquals(listOf(14L), paidExpenseDebts.map { it.id })

        val paidIncomeDebts = applyReportFilter(mixedTx, ReportFilterState(debtFilter = DebtFilter.PAYMENTS_INCOME))
        assertEquals(emptyList<Long>(), paidIncomeDebts.map { it.id })

        // Combined filter (Income + Unpaid Debt -> borrowed income only)
        val borrowedIncomeFilter = ReportFilterState(
            type = TransactionType.INCOME,
            debtFilter = DebtFilter.DEBT_UNPAID
        )
        assertTrue(borrowedIncomeFilter.hasAnyActiveFilter())
        val borrowedIncomeOnly = applyReportFilter(mixedTx, borrowedIncomeFilter)
        assertEquals(listOf(13L), borrowedIncomeOnly.map { it.id })

        // Combined filter (Expense + Non-Debt -> paid expenses only)
        val paidExpensesOnly = applyReportFilter(
            mixedTx,
            ReportFilterState(type = TransactionType.EXPENSE, debtFilter = DebtFilter.NON_DEBT)
        )
        assertEquals(listOf(10L), paidExpensesOnly.map { it.id })
    }

    @Test
    fun partyItemAndNoteFiltersWorkOnTransactionsAndObligations() {
        val txs = listOf(
            TransactionEntity(
                id = 101,
                type = TransactionType.EXPENSE,
                amountMinor = 45000L,
                currency = "EGP",
                source = "كاش / نقدي",
                category = "فواتير والتزامات",
                itemName = "اشتراك إنترنت",
                debtParty = "شركة وي",
                note = "باقة شهر نوفمبر السريعة",
                date = "2026-11-10",
                time = "10:00"
            ),
            TransactionEntity(
                id = 102,
                type = TransactionType.EXPENSE,
                amountMinor = 120000L,
                currency = "EGP",
                source = "فودافون كاش",
                category = "صحة وعلاج",
                itemName = "كشف أسنان",
                debtParty = "د. كريم",
                note = "جلسة تنظيف ومتابعة",
                date = "2026-11-12",
                time = "17:30"
            )
        )
        val obs = listOf(
            ObligationEntity(
                id = 201,
                amountMinor = 300000L,
                currency = "EGP",
                reason = "إيجار الشقة",
                party = "الحاج محمود",
                dueDate = "2026-11-25",
                dueTime = "12:00",
                note = "تحويل إنستاباي قبل الظهر",
                isPaid = false
            ),
            ObligationEntity(
                id = 202,
                amountMinor = 45000L,
                currency = "EGP",
                reason = "اشتراك إنترنت",
                party = "شركة وي",
                dueDate = "2026-11-28",
                dueTime = "09:00",
                note = "باقة شهر نوفمبر السريعة",
                isPaid = true,
                paidSource = "فودافون كاش",
                paidDate = "2026-11-15"
            )
        )

        // Empty filter returns ALL transactions and ALL obligations
        val emptyFilter = ReportFilterState()
        assertFalse(emptyFilter.hasAnyActiveFilter())
        assertEquals(2, applyReportFilter(txs, emptyFilter).size)
        assertEquals(2, applyObligationsReportFilter(obs, emptyFilter).size)

        // Filter by party («اندفعت لمين / الجهة»)
        val byParty = ReportFilterState(parties = setOf("شركة وي"))
        assertTrue(byParty.hasAnyActiveFilter())
        assertEquals(listOf(101L), applyReportFilter(txs, byParty).map { it.id })
        assertEquals(listOf(202L), applyObligationsReportFilter(obs, byParty).map { it.id })

        // Filter by item/reason («عشان إيه»)
        val byItem = ReportFilterState(items = setOf("إيجار الشقة", "كشف أسنان"))
        assertTrue(byItem.hasAnyActiveFilter())
        assertEquals(listOf(102L), applyReportFilter(txs, byItem).map { it.id })
        assertEquals(listOf(201L), applyObligationsReportFilter(obs, byItem).map { it.id })

        // Filter by additional note keyword («البحث في الملاحظة الإضافية»)
        val byNote = ReportFilterState(noteQuery = "إنستاباي")
        assertTrue(byNote.hasAnyActiveFilter())
        assertEquals(emptyList<Long>(), applyReportFilter(txs, byNote).map { it.id })
        assertEquals(listOf(201L), applyObligationsReportFilter(obs, byNote).map { it.id })

        val sharedNote = ReportFilterState(noteQuery = "نوفمبر")
        assertEquals(listOf(101L), applyReportFilter(txs, sharedNote).map { it.id })
        assertEquals(listOf(202L), applyObligationsReportFilter(obs, sharedNote).map { it.id })

        // Partial keyword typed into searchable party/item dropdown matches seamlessly
        val partialParty = ReportFilterState(parties = setOf("وي"))
        assertEquals(listOf(101L), applyReportFilter(txs, partialParty).map { it.id })
        assertEquals(listOf(202L), applyObligationsReportFilter(obs, partialParty).map { it.id })

        // Combined party + item + note filter works with strict AND logic
        val combined = ReportFilterState(
            parties = setOf("شركة وي", "د. كريم"),
            items = setOf("اشتراك إنترنت"),
            noteQuery = "نوفمبر"
        )
        assertTrue(combined.hasAnyActiveFilter())
        assertEquals(listOf(101L), applyReportFilter(txs, combined).map { it.id })
        assertEquals(listOf(202L), applyObligationsReportFilter(obs, combined).map { it.id })
    }

    @Test
    fun smartExportFileNameReflectsReportTitleAndActiveFilters() {
        // 1. Filtered Arabic report includes report title + active filter subject + date + extension
        val filteredName = com.example.util.ExportManager.buildSmartFileName(
            reportTitle = "تقرير مصروفي المفلتر",
            filterSummary = "اندفعت لمين: شركة وي • عشان إيه: إنترنت • 5 عملية",
            extension = "pdf",
            isArabic = true,
            dateStamp = "2026-11-22"
        )
        assertTrue(filteredName.contains("تقرير_مصروفي_المفلتر"))
        assertTrue(filteredName.contains("شركة_وي"))
        assertTrue(filteredName.endsWith("_2026-11-22.pdf"))
        assertFalse(filteredName.contains(":"))
        assertFalse(filteredName.contains("/"))

        // 2. Unfiltered report strips boilerplate "كل البيانات (بدون فلاتر)"
        val unfilteredName = com.example.util.ExportManager.buildSmartFileName(
            reportTitle = "تفاصيل إجمالي المصاريف",
            filterSummary = "كل البيانات (بدون فلاتر)",
            extension = "xls",
            isArabic = true,
            dateStamp = "2026-11-22"
        )
        assertEquals("تفاصيل_إجمالي_المصاريف_2026-11-22.xls", unfilteredName)

        // 3. Custom user filename is sanitized and keeps extension cleanly without doubling
        val customName = com.example.util.ExportManager.buildSmartFileName(
            reportTitle = "تقرير",
            extension = "pdf",
            isArabic = true,
            customFileName = "فواتير / شهر: نوفمبر.pdf",
            dateStamp = "2026-11-22"
        )
        assertEquals("فواتير_شهر_نوفمبر.pdf", customName)
    }
}
