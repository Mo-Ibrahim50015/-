package com.example

import com.example.data.AppSettingsEntity
import com.example.data.ObligationEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.computeAnalytics
import com.example.util.PaymentNotificationManager
import com.example.util.buildUserGreeting
import com.example.util.buildUserGreetingDisplay
import com.example.util.divideMinorHalfUp
import com.example.util.formatAmountMinor
import com.example.util.formatSignedAmount
import com.example.util.isMorningHour
import com.example.util.minorToPlainDecimal
import com.example.util.parseAmountToMinor
import com.example.util.percentHalfUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive verification suite proving mathematical, scientific, personal & commercial
 * accounting precision, zero double-deduction reconciliation, strict currency isolation,
 * and live time-of-day custom greeting accuracy.
 */
class AccountingEngineTest {

    @Test
    fun exactBigDecimalParsingAndFormatting_zeroFloatingPointDrift() {
        // Classic IEEE-754 traps like 19.99, 0.1 + 0.2, 1250.55
        assertEquals(1999L, parseAmountToMinor("19.99"))
        assertEquals(1L, parseAmountToMinor("0.01"))
        assertEquals(125050L, parseAmountToMinor("1,250.50"))
        assertEquals(100000000L, parseAmountToMinor("1,000,000"))
        assertEquals(125075L, parseAmountToMinor("١٢٥٠٫٧٥")) // Arabic-Indic digits + Arabic decimal
        assertEquals(125075L, parseAmountToMinor("۱٬۲۵۰.۷۵")) // Persian digits + thousands separator
        assertEquals(1235L, parseAmountToMinor("12.345")) // HALF_UP rounding on 3rd decimal
        assertEquals(1234L, parseAmountToMinor("12.344"))

        // Signed/zero parsing & invalid/overflow rejection
        assertEquals(0L, parseAmountToMinor("0"))
        assertEquals(-5000L, parseAmountToMinor("-50"))
        assertNull(parseAmountToMinor(""))
        assertNull(parseAmountToMinor("abc"))
        assertNull(parseAmountToMinor("12.34.56"))
        assertNull(parseAmountToMinor("99999999999999999"))

        // Negative minor formatting (specifically sub-unit negatives like -50 minor = -0.50, never "-0.00")
        assertEquals("-0.50", formatAmountMinor(-50L))
        assertEquals("-1,234.05", formatAmountMinor(-123405L))
        assertEquals("0.00", formatAmountMinor(0L))
        assertEquals("-0.50", minorToPlainDecimal(-50L))
        assertEquals("+ 100.25 USD", formatSignedAmount(10025L, "USD", isArabic = false))
        assertEquals("- 100.25 USD", formatSignedAmount(-10025L, "USD", isArabic = false))
        assertEquals("0.00 USD", formatSignedAmount(0L, "USD", isArabic = false))

        // Exact HALF_UP division & percentage math
        assertEquals(334L, divideMinorHalfUp(1001L, 3))
        assertEquals(333L, divideMinorHalfUp(1000L, 3))
        assertEquals(0L, divideMinorHalfUp(500L, 0))
        assertEquals(67, percentHalfUp(2L, 3L))
        assertEquals(-50, percentHalfUp(-500L, 1000L))
    }

    @Test
    fun personalFinanceScenario_obligationsAndDebtsReconcileExactly() {
        val txs = listOf(
            // Salary received into Bank: 10,000.00 EGP
            TransactionEntity(
                id = 1, type = TransactionType.INCOME, amountMinor = 1_000_000L,
                currency = "EGP", source = "بنك", category = "راتب",
                date = "2026-05-01", time = "09:00", isDebt = false
            ),
            // Groceries paid in Cash: 1,500.00 EGP
            TransactionEntity(
                id = 2, type = TransactionType.EXPENSE, amountMinor = 150_000L,
                currency = "EGP", source = "كاش", category = "طعام",
                date = "2026-05-02", time = "14:00", isDebt = false
            ),
            // Unpaid Expense Debt (Credit card / unpaid bill): 500.00 EGP (has NOT left cash yet!)
            TransactionEntity(
                id = 3, type = TransactionType.EXPENSE, amountMinor = 50_000L,
                currency = "EGP", source = "بطاقة", category = "فواتير",
                date = "2026-05-03", time = "16:00", isDebt = true, isDebtSettled = false
            )
        )
        val obs = listOf(
            // Scheduled Rent Obligation due to pay: 2,500.00 EGP (Pending)
            ObligationEntity(
                id = 10, amountMinor = 250_000L, currency = "EGP",
                reason = "إيجار الشقة", party = "المالك",
                dueDate = "2026-05-10", dueTime = "10:00", isPaid = false
            )
        )

        val analytics = computeAnalytics(txs, "EGP", obs)
        val s = analytics.primarySummary

        // Actual Cash: 10,000 in - 1,500 out = 8,500.00 (unpaid debt & pending obligation haven't left cash yet)
        assertEquals(1_000_000L, s.actualCashInMinor)
        assertEquals(150_000L, s.actualCashOutMinor)
        assertEquals(850_000L, s.cashBalanceMinor)

        // Pending Obligations: 2,500 scheduled + 500 unpaid debt = 3,000.00
        assertEquals(250_000L, s.pendingScheduledObligationsMinor)
        assertEquals(50_000L, s.totalUnpaidDebtMinor)
        assertEquals(300_000L, s.totalPendingObligationsMinor)

        // Available Without Obligations: 8,500 - 3,000 = 5,500.00
        assertEquals(550_000L, s.availableWithoutObligationsMinor)
        assertEquals(550_000L, s.netMinor)

        // Reconciled Identity: Available + Total Pending Obligations == Actual Cash Balance
        assertEquals(s.cashBalanceMinor, s.availableWithoutObligationsMinor + s.totalPendingObligationsMinor)
        assertTrue(s.isFullyReconciled)

        // Account balances must reconcile strictly with Actual Cash Balance
        val accountsSum = analytics.sourceBreakdown.sumOf { it.cashBalanceMinor }
        assertEquals(s.cashBalanceMinor, accountsSum)
        assertEquals(s.cashBalanceMinor, analytics.dailyTrend.last().cumulativeNetMinor)
    }

    @Test
    fun obligationPaymentLifecycle_zeroDoubleDeductionInvariant() {
        val baseIncome = listOf(
            TransactionEntity(
                id = 1, type = TransactionType.INCOME, amountMinor = 500_000L,
                currency = "EGP", source = "بنك", category = "مبيعات",
                date = "2026-05-01", time = "09:00"
            )
        )
        val pendingOb = ObligationEntity(
            id = 20, amountMinor = 120_000L, currency = "EGP",
            reason = "فاتورة مورد", party = "شركة النيل",
            dueDate = "2026-05-05", dueTime = "12:00", isPaid = false
        )

        // Step 1: Before adding obligation -> cash = 5,000, available = 5,000
        val before = computeAnalytics(baseIncome, "EGP", emptyList()).primarySummary
        assertEquals(500_000L, before.cashBalanceMinor)
        assertEquals(500_000L, before.availableWithoutObligationsMinor)

        // Step 2: Immediately after adding obligation -> cash stays 5,000, available drops immediately to 3,800
        val afterAdd = computeAnalytics(baseIncome, "EGP", listOf(pendingOb)).primarySummary
        assertEquals(500_000L, afterAdd.cashBalanceMinor)
        assertEquals(120_000L, afterAdd.totalPendingObligationsMinor)
        assertEquals(380_000L, afterAdd.availableWithoutObligationsMinor)
        assertTrue(afterAdd.isFullyReconciled)

        // Step 3: After marking obligation PAID from "بنك" -> cash drops to 3,800, pending = 0,
        // and availableWithoutObligationsMinor stays STRICTLY 3,800 (ZERO double-deduction!)
        val paidOb = pendingOb.copy(isPaid = true, paidDate = "2026-05-05", paidSource = "بنك")
        val afterPayAnalytics = computeAnalytics(baseIncome, "EGP", listOf(paidOb))
        val afterPay = afterPayAnalytics.primarySummary
        assertEquals(380_000L, afterPay.cashBalanceMinor)
        assertEquals(0L, afterPay.totalPendingObligationsMinor)
        assertEquals(380_000L, afterPay.availableWithoutObligationsMinor)
        assertEquals(380_000L, afterPay.operatingNetProfitMinor)
        assertTrue(afterPay.isFullyReconciled)

        // Bank account itself must show 5,000 - 1,200 = 3,800
        val bankAcc = afterPayAnalytics.sourceBreakdown.first { it.source == "بنك" }
        assertEquals(380_000L, bankAcc.cashBalanceMinor)
    }

    @Test
    fun commercialAccountingScenario_loanPrincipalExcludedFromOperatingProfit() {
        val txs = listOf(
            // Business Sales Revenue: 20,000.00 SAR
            TransactionEntity(
                id = 1, type = TransactionType.INCOME, amountMinor = 2_000_000L,
                currency = "SAR", source = "حساب تجاري", category = "مبيعات",
                date = "2026-05-01", time = "10:00", isDebt = false
            ),
            // Business Working Capital Loan Received (Unpaid Debt Income): 10,000.00 SAR
            TransactionEntity(
                id = 2, type = TransactionType.INCOME, amountMinor = 1_000_000L,
                currency = "SAR", source = "حساب تجاري", category = "قرض",
                date = "2026-05-02", time = "11:00", isDebt = true, isDebtSettled = false
            ),
            // Paid Operating Expense (Salaries): 6,000.00 SAR
            TransactionEntity(
                id = 3, type = TransactionType.EXPENSE, amountMinor = 600_000L,
                currency = "SAR", source = "حساب تجاري", category = "رواتب",
                date = "2026-05-03", time = "12:00", isDebt = false
            ),
            // Supplier Credit Purchase (Unpaid Expense Debt): 4,000.00 SAR
            TransactionEntity(
                id = 4, type = TransactionType.EXPENSE, amountMinor = 400_000L,
                currency = "SAR", source = "موردين", category = "بضاعة",
                date = "2026-05-04", time = "13:00", isDebt = true, isDebtSettled = false
            )
        )

        val beforeLoanRepaid = computeAnalytics(txs, "SAR").primarySummary
        // Actual Cash = 20,000 sales + 10,000 loan - 6,000 paid salaries = 24,000.00
        assertEquals(2_400_000L, beforeLoanRepaid.cashBalanceMinor)
        // Pending Obligations = 10,000 loan + 4,000 supplier payable = 14,000.00
        assertEquals(1_400_000L, beforeLoanRepaid.totalPendingObligationsMinor)
        // Free Available = 24,000 - 14,000 = 10,000.00
        assertEquals(1_000_000L, beforeLoanRepaid.availableWithoutObligationsMinor)
        // Commercial Operating Profit = 20,000 Earned Revenue - (6,000 salaries + 4,000 supplier goods) = 10,000.00
        // Loan principal (10,000) is strictly EXCLUDED from profit!
        assertEquals(2_000_000L, beforeLoanRepaid.earnedIncomeMinor)
        assertEquals(1_000_000L, beforeLoanRepaid.totalIncurredExpensesMinor)
        assertEquals(1_000_000L, beforeLoanRepaid.operatingNetProfitMinor)
        assertEquals(50, beforeLoanRepaid.savingsRatePercent) // 10,000 / 20,000 = 50%
        assertTrue(beforeLoanRepaid.isFullyReconciled)

        // Now repay the 10,000 loan -> Cash drops by 10,000 (to 14,000), Pending drops by 10,000 (to 4,000),
        // Free Available stays strictly 10,000, and Operating Profit stays strictly 10,000!
        val afterLoanRepaid = computeAnalytics(
            txs.map { if (it.id == 2L) it.copy(isDebtSettled = true) else it },
            "SAR"
        ).primarySummary
        assertEquals(1_400_000L, afterLoanRepaid.cashBalanceMinor)
        assertEquals(400_000L, afterLoanRepaid.totalPendingObligationsMinor)
        assertEquals(1_000_000L, afterLoanRepaid.availableWithoutObligationsMinor)
        assertEquals(1_000_000L, afterLoanRepaid.operatingNetProfitMinor)
        assertTrue(afterLoanRepaid.isFullyReconciled)
    }

    @Test
    fun multiCurrencyStrictIsolation_neverMixesCurrencies() {
        val txs = listOf(
            TransactionEntity(
                id = 1, type = TransactionType.INCOME, amountMinor = 100_000L,
                currency = "EGP", source = "كاش", category = "دخل", date = "2026-05-01", time = "09:00"
            ),
            TransactionEntity(
                id = 2, type = TransactionType.INCOME, amountMinor = 50_000L,
                currency = "USD", source = "بنك دولار", category = "عميل", date = "2026-05-01", time = "10:00"
            )
        )
        val obs = listOf(
            ObligationEntity(
                id = 1, amountMinor = 10_000L, currency = "USD",
                reason = "استضافة خوادم", party = "Cloud", dueDate = "2026-05-15", dueTime = "12:00"
            )
        )

        val analytics = computeAnalytics(txs, "EGP", obs)
        val egp = analytics.currencySummaries.first { it.currency == "EGP" }
        val usd = analytics.currencySummaries.first { it.currency == "USD" }

        assertEquals(100_000L, egp.availableWithoutObligationsMinor)
        assertEquals(0L, egp.totalPendingObligationsMinor)

        assertEquals(50_000L, usd.cashBalanceMinor)
        assertEquals(10_000L, usd.totalPendingObligationsMinor)
        assertEquals(40_000L, usd.availableWithoutObligationsMinor)
        assertTrue(egp.isFullyReconciled && usd.isFullyReconciled)
    }

    @Test
    fun liveGreetingBoundariesAndCustomOverrides() {
        // 12:00 AM (0) through 11:59 AM (11) -> Morning
        assertTrue(isMorningHour(0))
        assertTrue(isMorningHour(6))
        assertTrue(isMorningHour(11))
        // 12:00 PM (12) through 11:59 PM (23) -> Evening
        assertFalse(isMorningHour(12))
        assertFalse(isMorningHour(18))
        assertFalse(isMorningHour(23))

        val baseSettings = AppSettingsEntity(userName = "أحمد")
        assertEquals("صباح الخير يا أحمد", buildUserGreeting(0, baseSettings, isArabic = true))
        assertEquals("مساء الخير يا أحمد", buildUserGreeting(12, baseSettings, isArabic = true))

        // Both base & additional enabled -> shown together
        val bothOn = baseSettings.copy(
            customGreetingText = "أهلاً بك يا {name} • يوم موفق",
            showBaseGreeting = true,
            showCustomGreeting = true
        )
        val bothDisplay = buildUserGreetingDisplay(15, bothOn, isArabic = true)
        assertTrue(bothDisplay.hasAnyGreeting)
        assertEquals("مساء الخير يا أحمد", bothDisplay.primaryLine)
        assertEquals("أهلاً بك يا أحمد • يوم موفق", bothDisplay.secondaryLine)

        // Base OFF, Additional ON -> only additional greeting shown
        val customOnly = bothOn.copy(showBaseGreeting = false, showCustomGreeting = true)
        val customOnlyDisplay = buildUserGreetingDisplay(15, customOnly, isArabic = true)
        assertTrue(customOnlyDisplay.hasAnyGreeting)
        assertEquals("أهلاً بك يا أحمد • يوم موفق", customOnlyDisplay.primaryLine)
        assertEquals(null, customOnlyDisplay.secondaryLine)

        // Base ON, Additional OFF -> only base greeting shown
        val baseOnly = bothOn.copy(showBaseGreeting = true, showCustomGreeting = false)
        val baseOnlyDisplay = buildUserGreetingDisplay(15, baseOnly, isArabic = true)
        assertTrue(baseOnlyDisplay.hasAnyGreeting)
        assertEquals("مساء الخير يا أحمد", baseOnlyDisplay.primaryLine)
        assertEquals(null, baseOnlyDisplay.secondaryLine)

        // Both OFF -> greeting completely hidden
        val bothOff = bothOn.copy(showBaseGreeting = false, showCustomGreeting = false)
        val bothOffDisplay = buildUserGreetingDisplay(15, bothOff, isArabic = true)
        assertFalse(bothOffDisplay.hasAnyGreeting)
        assertEquals("", bothOffDisplay.combinedText)

        // Custom morning & evening phrases
        val customPeriods = baseSettings.copy(
            customMorningGreeting = "صباح النور",
            customEveningGreeting = "مساء الفل"
        )
        assertEquals("صباح النور يا أحمد", buildUserGreeting(2, customPeriods, isArabic = true))
        assertEquals("مساء الفل يا أحمد", buildUserGreeting(14, customPeriods, isArabic = true))
    }

    @Test
    fun incomingSplitAndReminderTriggerCalculation_areExact() {
        val txs = listOf(
            // Earned income & profit: 7,500 EGP
            TransactionEntity(
                id = 1, type = TransactionType.INCOME, amountMinor = 750_000L,
                currency = "EGP", source = "بنك", category = "راتب",
                date = "2026-06-01", time = "09:00", isDebt = false
            ),
            // Borrowed incoming money that must be repaid: 2,500 EGP (unpaid)
            TransactionEntity(
                id = 2, type = TransactionType.INCOME, amountMinor = 250_000L,
                currency = "EGP", source = "كاش", category = "سلفة",
                date = "2026-06-02", time = "11:00", isDebt = true, isDebtSettled = false
            )
        )
        val summary = computeAnalytics(txs, "EGP").primarySummary
        // Earned income tab total = 7,500; Borrowed-to-repay incoming tab total = 2,500
        assertEquals(750_000L, summary.earnedIncomeMinor)
        assertEquals(250_000L, summary.unpaidIncomeDebtMinor)
        assertEquals(1_000_000L, summary.actualCashInMinor)

        // Reminder lead-time trigger calculation: 60 min earlier must be strictly 3,600,000 ms less
        val atDue = PaymentNotificationManager.computeTriggerMillis("2026-12-15", "18:30", 0)
        val oneHourBefore = PaymentNotificationManager.computeTriggerMillis("2026-12-15", "18:30", 60)
        assertNotNull(atDue)
        assertNotNull(oneHourBefore)
        assertEquals(3_600_000L, atDue!! - oneHourBefore!!)
        assertNull(PaymentNotificationManager.computeTriggerMillis("", "18:30", 60))
    }
}
