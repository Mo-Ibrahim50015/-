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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ObligationEntity
import com.example.data.TransactionEntity
import com.example.ui.components.SereneCard
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.formatTime12Hour
import com.example.util.tr

/**
 * Dedicated tab inside «المخطط» for «فلوسك الباقية من يوم ما استخدمت البرنامج».
 * Displays the cumulative total ever added (never reduced by spending) and every added-money operation
 * including «جات منين», «عشان إيه», account, date, and time.
 */
@Composable
fun LifetimeMoneyPlanTabContent(
    allIncomeTx: List<TransactionEntity>,
    primaryCurrency: String,
    lifetimeIncomeMinor: Long,
    isArabic: Boolean,
    onOpenFullLifetimeScreen: () -> Unit
) {
    val ext = LocalSereneColors.current
    val sym = currencySymbol(primaryCurrency, isArabic)
    val sortedIncome = remember(allIncomeTx) {
        allIncomeTx.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.createdAt })
    }
    val secondaryTotals = remember(allIncomeTx, primaryCurrency) {
        allIncomeTx
            .filter { it.currency != primaryCurrency }
            .groupBy { it.currency }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
            .entries
            .sortedBy { it.key }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_lifetime_hero_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = ext.heroCardBackground,
                    border = BorderStroke(1.2.dp, Color(0xFF82F5C1).copy(alpha = 0.45f)),
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
                                    .background(ext.incomeColor.copy(alpha = 0.24f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Savings,
                                    contentDescription = tr(isArabic, "الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج", "Lifetime Money"),
                                    tint = Color(0xFF82F5C1),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        isArabic,
                                        "الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج",
                                        "Your Money Since Using the App"
                                    ),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ext.heroCardOnBackground
                                )
                                Text(
                                    text = tr(
                                        isArabic,
                                        "الحالة: تراكمي لكل مبلغ أضفته • لا يتأثر بأي فلوس اندفعت",
                                        "Status: Cumulative — every added amount, unaffected by spending"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.heroCardSubtext
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatAmountMinor(lifetimeIncomeMinor),
                                style = AmountDisplayStyle,
                                color = Color(0xFF82F5C1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = sym,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = ext.heroCardSubtext,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        if (secondaryTotals.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                secondaryTotals.take(3).forEach { (curr, minor) ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color.White.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${formatAmountMinor(minor)} ${currencySymbol(curr, isArabic)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("plan_lifetime_manage_btn")
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenFullLifetimeScreen),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF82F5C1)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddCircleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF06231B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr(isArabic, "إضافة فلوس / فتح السجل الكامل", "Add Money / Full History"),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF06231B)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(isArabic, "كل العمليات اللي انضافت فيها الفلوس", "All Money-Added Operations"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tr(isArabic, "${sortedIncome.size} عملية", "${sortedIncome.size} items"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ext.incomeColor
                    )
                }
            }

            if (sortedIncome.isEmpty()) {
                item {
                    SereneCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = tr(
                                isArabic,
                                "لم يتم تسجيل أي مبالغ مضافة بعد. اضغط على «إضافة فلوس» لتسجيل أول مبلغ.",
                                "No added money entries recorded yet."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(sortedIncome, key = { "inc_${it.id}" }) { tx ->
                    val txSym = currencySymbol(tx.currency, isArabic)
                    SereneCard(contentPadding = PaddingValues(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = tx.itemName.ifBlank { tx.category.ifBlank { tr(isArabic, "فلوس جاية ليا", "Incoming Money") } },
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (tx.debtParty.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = tr(isArabic, "جات منين: ${tx.debtParty}", "From: ${tx.debtParty}"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.primary,
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
                                            imageVector = Icons.Outlined.Payments,
                                            contentDescription = null,
                                            tint = ext.incomeColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = tr(isArabic, "التصنيف: ${tx.category}", "Category: ${tx.category}"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = tx.source.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = tx.date,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (tx.time.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = formatTime12Hour(tx.time, isArabic),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+${formatAmountMinor(tx.amountMinor, false)} $txSym",
                                style = AmountBodyStyle.copy(
                                    fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ext.incomeColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated tab inside «المخطط» for «فلوسك الباقية لو حسبت المبالغ اللي هتندفع قريباً».
 * Automatically deducts every scheduled future payment and unpaid upcoming expense/debt.
 */
@Composable
fun AfterScheduledObligationsTabContent(
    lifetimeIncomeMinor: Long,
    paidExpensesMinor: Long,
    pendingObligationsMinor: Long,
    remainingAvailableMinor: Long,
    pendingObligations: List<ObligationEntity>,
    unpaidDebts: List<TransactionEntity>,
    currencySymbol: String,
    isArabic: Boolean,
    onOpenObligations: () -> Unit,
    onSetObligationPaid: (Long, Boolean) -> Unit = { _, _ -> },
    onToggleDebtSettled: (Long, Boolean) -> Unit = { _, _ -> }
) {
    val ext = LocalSereneColors.current
    val isPositive = remainingAvailableMinor >= 0L
    val heroAmountColor = if (isPositive) Color(0xFF82F5C1) else Color(0xFFFF9B9B)
    val totalPendingCount = pendingObligations.size + unpaidDebts.size
    val statusText = when {
        remainingAvailableMinor < 0L -> tr(isArabic, "الحالة: عجز متوقع — المبالغ القادمة أكبر من رصيدك", "Status: Expected shortfall")
        pendingObligationsMinor == 0L -> tr(isArabic, "الحالة: لا توجد مبالغ مطلوبة قريباً — كامل رصيدك متاح", "Status: Clear — no pending dues")
        else -> tr(isArabic, "الحالة: رصيدك يغطي كل المبالغ اللي هتندفع قريباً ✓", "Status: Covered — balance covers all upcoming dues ✓")
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_after_scheduled_hero_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = ext.heroCardBackground,
                    border = BorderStroke(1.2.dp, ext.warningAmber.copy(alpha = 0.55f)),
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
                                    .background(ext.warningAmber.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EventNote,
                                    contentDescription = tr(
                                        isArabic,
                                        "فلوسك الباقية لو حسبت المبالغ اللي هتندفع قريباً",
                                        "Remaining Money After Upcoming Payments"
                                    ),
                                    tint = Color(0xFFFFD580),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        isArabic,
                                        "فلوسك الباقية لو حسبت المبالغ اللي هتندفع قريباً",
                                        "Remaining Money After Upcoming Payments"
                                    ),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ext.heroCardOnBackground
                                )
                                Text(
                                    text = statusText,
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
                                text = formatAmountMinor(remainingAvailableMinor),
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
                                        text = tr(isArabic, "هتندفع قريباً", "Upcoming Due"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.heroCardSubtext
                                    )
                                    Text(
                                        text = "-${formatAmountMinor(pendingObligationsMinor, false)} $currencySymbol",
                                        style = AmountBodyStyle.copy(
                                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFFFFD580)
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
                                        text = tr(isArabic, "اندفعت بالفعل", "Actually Paid"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.heroCardSubtext
                                    )
                                    Text(
                                        text = "-${formatAmountMinor(paidExpensesMinor, false)} $currencySymbol",
                                        style = AmountBodyStyle.copy(
                                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFFFF9B9B)
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("plan_schedule_future_payment_btn")
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenObligations),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF82F5C1)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddCircleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF06231B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr(isArabic, "إضافة مبلغ مطلوب دفعه قريباً", "Schedule Upcoming Payment"),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF06231B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(isArabic, "المبالغ اللي هتندفع قريباً (مخصومة هنا)", "Upcoming Payments (Deducted Here)"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tr(isArabic, "$totalPendingCount بند", "$totalPendingCount items"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ext.warningAmber
                    )
                }
            }

            if (totalPendingCount == 0) {
                item {
                    SereneCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = tr(
                                isArabic,
                                "لا توجد مبالغ محددة للدفع قريباً حالياً. أي مصروف «لسه هتندفع» أو التزام جديد سيتخصم فوراً من هذا الرصيد.",
                                "No upcoming payments right now. Any unpaid expense or obligation will deduct here automatically."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(pendingObligations, key = { "pend_ob_${it.id}" }) { ob ->
                    UpcomingDueActionRow(
                        title = ob.reason.ifBlank { tr(isArabic, "مبلغ مطلوب دفعه قريباً", "Scheduled Payment") },
                        party = ob.party,
                        badge = tr(isArabic, "التزام بموعد", "Scheduled"),
                        dateLabel = ob.dueDate,
                        timeLabel = ob.dueTime,
                        amountMinor = ob.amountMinor,
                        currencySymbol = currencySymbol,
                        isArabic = isArabic,
                        tag = "plan_pay_ob_${ob.id}",
                        onMarkPaid = { onSetObligationPaid(ob.id, true) }
                    )
                }
                items(unpaidDebts, key = { "pend_debt_${it.id}" }) { debt ->
                    UpcomingDueActionRow(
                        title = debt.itemName.ifBlank { debt.category.ifBlank { tr(isArabic, "مصروف لسه هيندفع", "Upcoming Expense") } },
                        party = debt.debtParty,
                        badge = tr(isArabic, "لسه هيندفع", "Unpaid"),
                        dateLabel = debt.date,
                        timeLabel = debt.time,
                        amountMinor = debt.amountMinor,
                        currencySymbol = currencySymbol,
                        isArabic = isArabic,
                        tag = "plan_pay_debt_${debt.id}",
                        onMarkPaid = { onToggleDebtSettled(debt.id, true) }
                    )
                }
            }
        }
    }
}

/**
 * NEW Dedicated tab inside «المخطط» for «فلوسك الموجودة عليك المفروض تدفعها».
 * Aggregates EVERY unpaid amount owed / upcoming (scheduled obligations + unpaid expenses + unpaid debts)
 * with live total, count, and instant 1-tap «تم الدفع» settlement.
 */
@Composable
fun AllDueOnYouTabContent(
    pendingObligationsMinor: Long,
    pendingObligations: List<ObligationEntity>,
    unpaidDebts: List<TransactionEntity>,
    currencySymbol: String,
    isArabic: Boolean,
    onOpenObligations: () -> Unit,
    onSetObligationPaid: (Long, Boolean) -> Unit,
    onToggleDebtSettled: (Long, Boolean) -> Unit
) {
    val ext = LocalSereneColors.current
    val totalCount = pendingObligations.size + unpaidDebts.size
    val scheduledTotal = remember(pendingObligations) { pendingObligations.sumOf { it.amountMinor } }
    val unpaidTxTotal = remember(unpaidDebts) { unpaidDebts.sumOf { it.amountMinor } }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_all_due_on_you_hero_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = ext.heroCardBackground,
                    border = BorderStroke(1.3.dp, ext.expenseColor.copy(alpha = 0.55f)),
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
                                    .background(ext.expenseColor.copy(alpha = 0.24f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RequestQuote,
                                    contentDescription = tr(isArabic, "فلوسك الموجودة عليك المفروض تدفعها", "All Amounts You Owe / Due"),
                                    tint = Color(0xFFFFB4B4),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        isArabic,
                                        "فلوسك الموجودة عليك المفروض تدفعها",
                                        "Total Amounts You Need to Pay"
                                    ),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ext.heroCardOnBackground
                                )
                                Text(
                                    text = if (totalCount == 0) {
                                        tr(isArabic, "الحالة: لا توجد مبالغ عليك حالياً ✓", "Status: All clear — nothing due ✓")
                                    } else {
                                        tr(
                                            isArabic,
                                            "الحالة: عليك $totalCount مبالغ مطلوب دفعها قريباً",
                                            "Status: $totalCount due amounts to pay soon"
                                        )
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (totalCount == 0) Color(0xFF82F5C1) else Color(0xFFFFD580)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatAmountMinor(pendingObligationsMinor),
                                style = AmountDisplayStyle,
                                color = if (pendingObligationsMinor > 0L) Color(0xFFFFB4B4) else Color(0xFF82F5C1),
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
                                        text = tr(isArabic, "مصاريف لسه هتندفع (${unpaidDebts.size})", "Upcoming Expenses (${unpaidDebts.size})"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.heroCardSubtext,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${formatAmountMinor(unpaidTxTotal, false)} $currencySymbol",
                                        style = AmountBodyStyle.copy(
                                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFFFFD580)
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
                                        text = tr(isArabic, "التزامات بمواعيد (${pendingObligations.size})", "Scheduled Dues (${pendingObligations.size})"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.heroCardSubtext,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${formatAmountMinor(scheduledTotal, false)} $currencySymbol",
                                        style = AmountBodyStyle.copy(
                                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFFFFB4B4)
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("plan_due_add_obligation_btn")
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenObligations),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF82F5C1)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddCircleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF06231B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr(isArabic, "إضافة مبلغ مطلوب دفعه مع تنبيه", "Add Scheduled Due with Reminder"),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF06231B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(isArabic, "كل المبالغ الموجودة عليك الآن", "Every Amount Currently Due"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tr(isArabic, "$totalCount مبلغ", "$totalCount items"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ext.expenseColor
                    )
                }
            }

            if (totalCount == 0) {
                item {
                    SereneCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = tr(
                                isArabic,
                                "ممتاز! لا توجد أي مبالغ عليك مطلوب دفعها حالياً. أي مصروف تختار فيه «لسه هتندفع قريباً» أو أي التزام مجدول سيظهر هنا تلقائياً.",
                                "Great! You currently have no unpaid amounts due. Any expense marked 'Pay Later' or scheduled obligation appears here automatically."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(unpaidDebts, key = { "due_tx_${it.id}" }) { debt ->
                    UpcomingDueActionRow(
                        title = debt.itemName.ifBlank { debt.category.ifBlank { tr(isArabic, "مصروف لسه هيندفع", "Upcoming Expense") } },
                        party = debt.debtParty,
                        badge = tr(isArabic, "مصروف لسه هيندفع", "Pay Later"),
                        dateLabel = debt.date,
                        timeLabel = debt.time,
                        amountMinor = debt.amountMinor,
                        currencySymbol = currencySymbol,
                        isArabic = isArabic,
                        tag = "due_tab_pay_tx_${debt.id}",
                        onMarkPaid = { onToggleDebtSettled(debt.id, true) }
                    )
                }
                items(pendingObligations, key = { "due_ob_${it.id}" }) { ob ->
                    UpcomingDueActionRow(
                        title = ob.reason.ifBlank { tr(isArabic, "مبلغ مطلوب دفعه", "Scheduled Due") },
                        party = ob.party,
                        badge = tr(isArabic, "التزام بموعد وتنبيه", "Scheduled"),
                        dateLabel = ob.dueDate,
                        timeLabel = ob.dueTime,
                        amountMinor = ob.amountMinor,
                        currencySymbol = currencySymbol,
                        isArabic = isArabic,
                        tag = "due_tab_pay_ob_${ob.id}",
                        onMarkPaid = { onSetObligationPaid(ob.id, true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingDueActionRow(
    title: String,
    party: String,
    badge: String,
    dateLabel: String,
    timeLabel: String,
    amountMinor: Long,
    currencySymbol: String,
    isArabic: Boolean,
    tag: String,
    onMarkPaid: () -> Unit
) {
    val ext = LocalSereneColors.current
    SereneCard(contentPadding = PaddingValues(13.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            color = ext.warningAmber.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ext.warningAmber,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (party.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = tr(isArabic, "هتندفع لمين: $party", "To: $party"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = buildString {
                            append(dateLabel)
                            if (timeLabel.isNotBlank()) append(" • ${formatTime12Hour(timeLabel, isArabic)}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${formatAmountMinor(amountMinor, false)} $currencySymbol",
                    style = AmountBodyStyle.copy(
                        fontSize = MaterialTheme.typography.labelLarge.fontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ext.expenseColor,
                    maxLines = 1
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .testTag(tag)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onMarkPaid),
                shape = RoundedCornerShape(10.dp),
                color = ext.incomeContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = ext.incomeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(isArabic, "تم الدفع — خصم من المحفظة ونقل للمدفوع بالفعل", "Mark Paid — Deduct from Wallet"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ext.incomeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
