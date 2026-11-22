package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.SereneCard
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.formatAmountMinor
import com.example.util.tr

enum class PaidDeductionKind {
    EXPENSE,
    PAID_OBLIGATION,
    SETTLED_DEBT
}

data class PaidDeductionEntry(
    val key: String,
    val kind: PaidDeductionKind,
    val title: String,
    val subtitle: String,
    val source: String,
    val dateLabel: String,
    val amountMinor: Long,
    val sortTimestamp: Long
)

/**
 * Interactive overview card shown inside «المخطط» right below
 * «فلوسك بعد حساب كل المبالغ المطلوب دفعها».
 *
 * Represents «فلوسك بعد دفع المبالغ المطلوب دفعها»:
 * deducts ONLY actual paid expenses, paid obligations, and settled debts
 * (unpaid future obligations are NOT deducted here until actually paid).
 */
@Composable
fun MoneyAfterPaidSummaryCard(
    afterActuallyPaidMinor: Long,
    actualPaidOutflowMinor: Long,
    paidEntriesCount: Int,
    pendingObligationsMinor: Long,
    currencySymbol: String,
    isArabic: Boolean,
    onOpenTab: () -> Unit
) {
    val ext = LocalSereneColors.current
    val isPositive = afterActuallyPaidMinor >= 0L
    val amountColor = if (isPositive) ext.incomeColor else ext.expenseColor
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guide_after_actually_paid_card")
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenTab),
        shape = RoundedCornerShape(20.dp),
        color = ext.cardBackground,
        border = BorderStroke(1.3.dp, accent.copy(alpha = 0.42f)),
        shadowElevation = if (ext.isDark) 0.dp else 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TaskAlt,
                            contentDescription = tr(
                                isArabic,
                                "فلوسك بعد دفع المبالغ المطلوب دفعها",
                                "Your Money After Paying Due Amounts"
                            ),
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = tr(
                                isArabic,
                                "فلوسك بعد دفع المبالغ المطلوب دفعها",
                                "Your Money After Paying Due Amounts"
                            ),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tr(
                                isArabic,
                                "يخصم فقط في حالة الدفع الفعلي من المصروفات أو اللي سددتها",
                                "Deducts only when actually paid from expenses or settled dues"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatAmountMinor(afterActuallyPaidMinor),
                    style = AmountDisplayStyle,
                    color = amountColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (actualPaidOutflowMinor > 0L) ext.expenseContainer else ext.cardSubtleBackground,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (actualPaidOutflowMinor > 0L) {
                            tr(
                                isArabic,
                                "المخصوم فعلياً: -${formatAmountMinor(actualPaidOutflowMinor, false)} $currencySymbol ($paidEntriesCount مدفوعة/مسددة)",
                                "Actually paid: -${formatAmountMinor(actualPaidOutflowMinor, false)} $currencySymbol ($paidEntriesCount items)"
                            )
                        } else {
                            tr(
                                isArabic,
                                "لم يتم خصم أي مصروفات أو مبالغ مسددة بعد",
                                "No paid expenses or settled items deducted yet"
                            )
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (actualPaidOutflowMinor > 0L) ext.expenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier
                        .testTag("guide_open_after_paid_tab_button")
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenTab),
                    shape = RoundedCornerShape(10.dp),
                    color = ext.heroCardBackground
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tr(isArabic, "عرض التفاصيل", "View Details"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ext.heroCardOnBackground,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF82F5C1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (pendingObligationsMinor > 0L) {
                Text(
                    text = tr(
                        isArabic,
                        "يوجد ${formatAmountMinor(pendingObligationsMinor, false)} $currencySymbol مطلوب دفعها لاحقاً ولم تُخصم هنا بعد حتى تسددها.",
                        "${formatAmountMinor(pendingObligationsMinor, false)} $currencySymbol scheduled for later — not deducted here until paid."
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Full dedicated tab content for «فلوسك بعد دفع المبالغ المطلوب دفعها».
 * Shows live balance after actual payments, clear equation breakdown of paid expenses,
 * paid obligations, and settled debts, and the complete chronological list of deducted items.
 */
@Composable
fun MoneyAfterPaidTabContent(
    lifetimeIncomeMinor: Long,
    regularPaidExpensesMinor: Long,
    regularPaidExpensesCount: Int,
    paidObligationsMinor: Long,
    paidObligationsCount: Int,
    settledDebtsMinor: Long,
    settledDebtsCount: Int,
    actualPaidOutflowMinor: Long,
    afterActuallyPaidMinor: Long,
    pendingObligationsMinor: Long,
    pendingObligationsCount: Int,
    deductionEntries: List<PaidDeductionEntry>,
    currencySymbol: String,
    isArabic: Boolean,
    onOpenLifetimeMoney: () -> Unit,
    onOpenExpenseDetails: () -> Unit,
    onOpenObligations: () -> Unit
) {
    val ext = LocalSereneColors.current
    val isPositive = afterActuallyPaidMinor >= 0L
    val heroAmountColor = if (isPositive) Color(0xFF82F5C1) else Color(0xFFFF9B9B)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Balance Card: «فلوسك بعد دفع المبالغ المطلوب دفعها»
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("after_paid_hero_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = ext.heroCardBackground,
                    border = BorderStroke(1.2.dp, heroAmountColor.copy(alpha = 0.45f)),
                    shadowElevation = if (ext.isDark) 0.dp else 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(heroAmountColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TaskAlt,
                                    contentDescription = tr(
                                        isArabic,
                                        "فلوسك بعد دفع المبالغ المطلوب دفعها",
                                        "Your Money After Paying Due Amounts"
                                    ),
                                    tint = heroAmountColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        isArabic,
                                        "فلوسك الباقية لو حسبت المبالغ اللي اندفعت بالفعل فقط",
                                        "Remaining Money After Actually Paid Amounts Only"
                                    ),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ext.heroCardOnBackground
                                )
                                Text(
                                    text = if (isPositive) {
                                        tr(
                                            isArabic,
                                            "الحالة: مطابق للباقي في محفظتك بعد كل اللي اندفع فعلياً ✓",
                                            "Status: Matches your real wallet balance after actual payments ✓"
                                        )
                                    } else {
                                        tr(
                                            isArabic,
                                            "الحالة: مصروفاتك المدفوعة تجاوزت دخلك المسجل",
                                            "Status: Paid expenses exceed recorded income"
                                        )
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = heroAmountColor
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatAmountMinor(afterActuallyPaidMinor),
                                style = AmountDisplayStyle,
                                color = heroAmountColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = currencySymbol,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = ext.heroCardSubtext,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = tr(isArabic, "إجمالي المخصوم الفعلي", "Total Paid Out"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.heroCardSubtext,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "-${formatAmountMinor(actualPaidOutflowMinor, false)} $currencySymbol",
                                        style = AmountBodyStyle.copy(
                                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFFFF9B9B),
                                        maxLines = 1
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = tr(isArabic, "مطلوب دفعه (لم يُخصم بعد)", "Pending (Not Paid Yet)"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.heroCardSubtext,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${formatAmountMinor(pendingObligationsMinor, false)} $currencySymbol",
                                        style = AmountBodyStyle.copy(
                                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFFFFD580),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Step-by-Step Actual Deduction Breakdown
            item {
                SereneCard(contentPadding = PaddingValues(16.dp)) {
                    Text(
                        text = tr(isArabic, "تفصيل الخصم الفعلي", "Actual Payment Breakdown"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = tr(
                            isArabic,
                            "أي التزام غير مدفوع لا يُخصم هنا إلا بعد ما تدفعه أو تسدده فعلياً",
                            "Unpaid obligations stay un-deducted here until you mark them paid"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AfterPaidEquationRow(
                        icon = Icons.Outlined.ArrowDownward,
                        tint = ext.incomeColor,
                        bg = ext.incomeContainer,
                        title = tr(isArabic, "إجمالي فلوسك المضافة", "Total Money Added"),
                        subtitle = tr(isArabic, "كل الفلوس اللي دخلتلك من البداية", "All incoming funds"),
                        amountText = "+${formatAmountMinor(lifetimeIncomeMinor)} $currencySymbol",
                        onClick = onOpenLifetimeMoney
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AfterPaidEquationRow(
                        icon = Icons.Outlined.ArrowUpward,
                        tint = ext.expenseColor,
                        bg = ext.expenseContainer,
                        title = tr(isArabic, "المصروفات المدفوعة", "Paid Expenses"),
                        subtitle = tr(isArabic, "$regularPaidExpensesCount عملية مصروف", "$regularPaidExpensesCount expense items"),
                        amountText = "-${formatAmountMinor(regularPaidExpensesMinor)} $currencySymbol",
                        onClick = onOpenExpenseDetails
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AfterPaidEquationRow(
                        icon = Icons.Outlined.CheckCircle,
                        tint = ext.expenseColor,
                        bg = ext.expenseContainer,
                        title = tr(isArabic, "المبالغ المطلوبة التي تم دفعها", "Paid Scheduled Obligations"),
                        subtitle = tr(
                            isArabic,
                            "$paidObligationsCount تم دفعها • $pendingObligationsCount لسه ما اندفعتش",
                            "$paidObligationsCount paid • $pendingObligationsCount pending"
                        ),
                        amountText = "-${formatAmountMinor(paidObligationsMinor)} $currencySymbol",
                        onClick = onOpenObligations
                    )
                    if (settledDebtsMinor > 0L || settledDebtsCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AfterPaidEquationRow(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            tint = ext.expenseColor,
                            bg = ext.expenseContainer,
                            title = tr(isArabic, "المديونيات التي تم سدادها", "Settled Debts"),
                            subtitle = tr(isArabic, "$settledDebtsCount دين تم سداده", "$settledDebtsCount settled debts"),
                            amountText = "-${formatAmountMinor(settledDebtsMinor)} $currencySymbol",
                            onClick = onOpenExpenseDetails
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(isArabic, "الرصيد بعد الدفع الفعلي", "Balance After Actual Payments"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${formatAmountMinor(afterActuallyPaidMinor)} $currencySymbol",
                            style = AmountBodyStyle.copy(
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isPositive) ext.incomeColor else ext.expenseColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("after_paid_go_obligations_btn")
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenObligations),
                            shape = RoundedCornerShape(12.dp),
                            color = ext.heroCardBackground
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EventNote,
                                    contentDescription = null,
                                    tint = Color(0xFF82F5C1),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr(isArabic, "سداد مبالغ مطلوبة", "Pay Due Items"),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ext.heroCardOnBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("after_paid_go_expenses_btn")
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenExpenseDetails),
                            shape = RoundedCornerShape(12.dp),
                            color = ext.cardSubtleBackground,
                            border = BorderStroke(1.dp, ext.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    tint = ext.expenseColor,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr(isArabic, "كل المصروفات", "All Expenses"),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 3. Header for Itemized Deducted Operations
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(isArabic, "العمليات المخصومة (المدفوعة والمسددة)", "Deducted Paid & Settled Items"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tr(isArabic, "${deductionEntries.size} عملية", "${deductionEntries.size} items"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ext.expenseColor
                    )
                }
            }

            if (deductionEntries.isEmpty()) {
                item {
                    SereneCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = tr(
                                isArabic,
                                "لا توجد مصروفات مدفوعة أو مبالغ مسددة حتى الآن. عند دفع أي مصروف أو سداد أي مبلغ مطلوب سيتم خصمه وإظهاره هنا فوراً.",
                                "No paid expenses or settled obligations yet. Once you pay an expense or settle a due amount, it will deduct and appear here immediately."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(deductionEntries, key = { it.key }) { entry ->
                    val kindLabel = when (entry.kind) {
                        PaidDeductionKind.EXPENSE -> tr(isArabic, "مصروف مدفوع", "Paid Expense")
                        PaidDeductionKind.PAID_OBLIGATION -> tr(isArabic, "التزام تم دفعه", "Paid Obligation")
                        PaidDeductionKind.SETTLED_DEBT -> tr(isArabic, "دين تم سداده", "Settled Debt")
                    }
                    val badgeColor = when (entry.kind) {
                        PaidDeductionKind.EXPENSE -> ext.expenseColor
                        PaidDeductionKind.PAID_OBLIGATION -> MaterialTheme.colorScheme.primary
                        PaidDeductionKind.SETTLED_DEBT -> ext.warningAmber
                    }

                    SereneCard(contentPadding = PaddingValues(13.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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
                                        color = badgeColor.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = kindLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = entry.dateLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        append(entry.source)
                                        if (entry.subtitle.isNotBlank()) {
                                            append(" • ")
                                            append(entry.subtitle)
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "-${formatAmountMinor(entry.amountMinor, false)} $currencySymbol",
                                style = AmountBodyStyle.copy(
                                    fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ext.expenseColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AfterPaidEquationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
