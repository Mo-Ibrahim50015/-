package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.AppTab
import com.example.ui.ReportAnalytics
import com.example.ui.components.SereneCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.UserAvatarCircle
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.tr
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun HomeScreen(
    analytics: ReportAnalytics,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    defaultCurrency: String,
    userName: String,
    userAvatarPath: String,
    showWelcomeBanner: Boolean,
    isArabic: Boolean,
    onDismissWelcomeBanner: () -> Unit,
    onNavigate: (AppTab) -> Unit,
    onOpenDebtsTab: (String) -> Unit,
    onOpenPaymentsTab: (String) -> Unit,
    onToggleDebtSettled: (Long, Boolean) -> Unit
) {
    val ext = LocalSereneColors.current
    val summary = analytics.primarySummary
    val sym = currencySymbol(summary.currency.ifBlank { defaultCurrency }, isArabic)

    val isMorning = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        hour in 5..11
    }
    val greetingTitle = remember(isMorning, userName, isArabic) {
        val base = if (isMorning) {
            tr(isArabic, "صباح الخير", "Good Morning")
        } else {
            tr(isArabic, "مساء الخير", "Good Evening")
        }
        val cleanName = userName.trim()
        if (cleanName.isNotEmpty()) {
            if (isArabic) "$base يا $cleanName" else "$base, $cleanName"
        } else {
            tr(isArabic, "$base في مصروفي", "$base • Masrofy")
        }
    }

    var startWelcomeCountdown by remember { mutableStateOf(false) }
    LaunchedEffect(showWelcomeBanner) {
        if (showWelcomeBanner) {
            startWelcomeCountdown = true
            delay(10_000L)
            onDismissWelcomeBanner()
        }
    }
    val welcomeProgress by animateFloatAsState(
        targetValue = if (startWelcomeCountdown) 0f else 1f,
        animationSpec = tween(durationMillis = 10_000, easing = LinearEasing),
        label = "welcome_banner_timer"
    )

    val balanceSubtitle = when {
        transactions.isEmpty() -> tr(
            isArabic,
            "حسابك نظيف ومستعد لتسجيل أول خطوة مالية",
            "Your ledger is clean and ready for your first entry"
        )
        summary.netMinor > 0 -> tr(
            isArabic,
            "ميزانيتك متوازنة وتتمتع بفائض إيجابي مطمئن",
            "Your balance is healthy with a positive surplus"
        )
        summary.netMinor == 0L -> tr(
            isArabic,
            "دخلك ومصاريفك في حالة تعادل تام",
            "Your income and expenses are evenly balanced"
        )
        else -> tr(
            isArabic,
            "مصروفاتك تجاوزت دخلك — راجع التقارير للتحكم",
            "Expenses exceed income — check Reports to adjust"
        )
    }

    val statusPillText = when {
        transactions.isEmpty() -> tr(isArabic, "جاهز", "Ready")
        summary.netMinor >= 0 -> tr(isArabic, "متوازن", "Balanced")
        else -> tr(isArabic, "مراجعة", "Review")
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
        ) {
            // 1. Pinned Time-of-Day Welcome Banner right below the "الرئيسية" header
            // Auto-dismisses after 10 seconds OR immediately on Skip/Close tap
            AnimatedVisibility(
                visible = showWelcomeBanner,
                exit = fadeOut(tween(260)) + shrinkVertically(tween(260))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp)
                        .testTag("home_welcome_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = ext.cardBackground,
                    border = BorderStroke(1.dp, ext.incomeColor.copy(alpha = 0.28f)),
                    shadowElevation = if (ext.isDark) 0.dp else 2.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                UserAvatarCircle(
                                    avatarPath = userAvatarPath,
                                    userName = userName,
                                    size = 42.dp,
                                    fallbackIcon = if (isMorning) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay,
                                    contentDescription = greetingTitle
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = greetingTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tr(
                                            isArabic,
                                            "بداية مالية واعية ومنظمة ليومك • $statusPillText",
                                            "Mindful finances for your day • $statusPillText"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .testTag("home_welcome_skip_btn")
                                    .clickable(onClick = onDismissWelcomeBanner),
                                shape = RoundedCornerShape(999.dp),
                                color = ext.cardSubtleBackground,
                                border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = tr(isArabic, "تخطي", "Skip"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = tr(isArabic, "إخفاء الترحيب", "Dismiss welcome"),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { welcomeProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = ext.incomeColor,
                            trackColor = ext.incomeContainer
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

            // 2. Total Balance Hero Card (Deep Midnight Navy)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_balance_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = ext.heroCardBackground,
                    shadowElevation = 4.dp
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Subtle ambient orb matching screenshot
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 30.dp, y = 30.dp)
                                .clip(CircleShape)
                                .background(ext.heroCardAccentCircle.copy(alpha = 0.45f))
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = ext.heroCardSubtext,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tr(isArabic, "الرصيد الكلي المتاح", "Total Available Balance"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ext.heroCardSubtext
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (summary.netMinor < 0) "-${formatAmountMinor(-summary.netMinor)}"
                                    else formatAmountMinor(summary.netMinor),
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
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            Text(
                                text = balanceSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.heroCardSubtext,
                                textAlign = TextAlign.Center
                            )

                            if (summary.totalUnpaidDebtMinor > 0L) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { onNavigate(AppTab.DEBTS) },
                                    shape = RoundedCornerShape(999.dp),
                                    color = ext.warningAmber.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.RequestQuote,
                                            contentDescription = null,
                                            tint = ext.warningAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = tr(
                                                isArabic,
                                                "مديونيات قائمة عليك: ${formatAmountMinor(summary.totalUnpaidDebtMinor, false)} $sym",
                                                "Active Debts: ${formatAmountMinor(summary.totalUnpaidDebtMinor, false)} $sym"
                                            ),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // If user has secondary currencies, show separate non-converted totals pill row
                            if (analytics.currencySummaries.size > 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    analytics.currencySummaries.drop(1).take(3).forEach { cs ->
                                        val cSym = currencySymbol(cs.currency, isArabic)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color.White.copy(alpha = 0.12f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${formatAmountMinor(cs.netMinor)} $cSym",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.heroCardOnBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Dual Snapshot Cards: Income & Expenses (Clickable -> Dedicated Detail Pages)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Income Card -> Opens Income Details Screen
                    SereneCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_income_card")
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigate(AppTab.INCOME_DETAILS) },
                        contentPadding = PaddingValues(13.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(isArabic, "إجمالي الدخل", "Total Income"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(ext.incomeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDownward,
                                    contentDescription = tr(isArabic, "تفاصيل الدخل", "Income Details"),
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatAmountMinor(summary.totalIncomeMinor),
                                style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = sym,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when {
                                    summary.incomeCount == 0 -> tr(isArabic, "لم يتم تسجيل دخل", "No income")
                                    summary.unpaidIncomeDebtMinor > 0L -> tr(
                                        isArabic,
                                        "مديونية ${formatAmountMinor(summary.unpaidIncomeDebtMinor, false)}",
                                        "Debt ${formatAmountMinor(summary.unpaidIncomeDebtMinor, false)}"
                                    )
                                    else -> tr(isArabic, "${summary.incomeCount} عملية", "${summary.incomeCount} items")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (summary.unpaidIncomeDebtMinor > 0L) ext.warningAmber else ext.incomeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = ext.incomeColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Expenses Card -> Opens Expenses Details Screen
                    SereneCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_expense_card")
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigate(AppTab.EXPENSE_DETAILS) },
                        contentPadding = PaddingValues(13.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(isArabic, "إجمالي المصاريف", "Total Expenses"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(ext.expenseContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowUpward,
                                    contentDescription = tr(isArabic, "تفاصيل المصاريف", "Expenses Details"),
                                    tint = ext.expenseColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatAmountMinor(summary.totalExpenseMinor),
                                style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = sym,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when {
                                    summary.expenseCount == 0 -> tr(isArabic, "لا توجد نفقات", "No expenses")
                                    summary.unpaidExpenseDebtMinor > 0L -> tr(
                                        isArabic,
                                        "مديونية ${formatAmountMinor(summary.unpaidExpenseDebtMinor, false)}",
                                        "Debt ${formatAmountMinor(summary.unpaidExpenseDebtMinor, false)}"
                                    )
                                    else -> tr(isArabic, "${summary.expenseCount} عملية", "${summary.expenseCount} items")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (summary.unpaidExpenseDebtMinor > 0L) ext.expenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = ext.expenseColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // 4. Central Navigation Hub (4 Quick Modules)
            item {
                SereneCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(isArabic, "أقسام التطبيق والوصول السريع", "Quick Access Hub"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tr(isArabic, "6 مراكز رئيسية", "6 Core Hubs"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HubShortcutTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.AddCircle,
                            iconBg = ext.heroCardBackground,
                            iconTint = ext.heroCardOnBackground,
                            title = tr(isArabic, "إضافة عملية", "Add Transaction"),
                            subtitle = tr(isArabic, "تسجيل فوري وسريع", "Fast & instant log"),
                            badgeText = null,
                            tag = "hub_add_btn",
                            onClick = { onNavigate(AppTab.ADD) }
                        )
                        HubShortcutTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.ReceiptLong,
                            iconBg = ext.cardContainer,
                            iconTint = MaterialTheme.colorScheme.onSurface,
                            title = tr(isArabic, "سجل العمليات", "Transactions Log"),
                            subtitle = tr(isArabic, "أرشيف الحركات المالية", "Full ledger archive"),
                            badgeText = tr(isArabic, "${transactions.size} عملية", "${transactions.size} items"),
                            tag = "hub_log_btn",
                            onClick = { onNavigate(AppTab.LOG) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HubShortcutTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Analytics,
                            iconBg = ext.incomeContainer,
                            iconTint = ext.incomeColor,
                            title = tr(isArabic, "التقارير والتحليلات", "Reports & Analytics"),
                            subtitle = tr(isArabic, "مخططات ذكية وفلاتر", "Smart charts & filters"),
                            badgeText = null,
                            tag = "hub_reports_btn",
                            onClick = { onNavigate(AppTab.REPORTS) }
                        )
                        HubShortcutTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Tune,
                            iconBg = ext.cardContainer,
                            iconTint = MaterialTheme.colorScheme.onSurface,
                            title = tr(isArabic, "الإعدادات العامة", "General Settings"),
                            subtitle = tr(isArabic, "العملة والنسخ الاحتياطي", "Currency & backup"),
                            badgeText = null,
                            tag = "hub_settings_btn",
                            onClick = { onNavigate(AppTab.SETTINGS) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // Dedicated Debts Owed Icon Tile (المديونيات عليّ) with split Income/Expense Debt tabs
                    val totalUnpaidCount = transactions.count { it.isDebt && !it.isDebtSettled }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_debts_btn")
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenDebtsTab("all") },
                        shape = RoundedCornerShape(14.dp),
                        color = if (summary.totalUnpaidDebtMinor > 0L) {
                            ext.warningAmber.copy(alpha = 0.12f)
                        } else ext.cardSubtleBackground,
                        border = BorderStroke(
                            1.dp,
                            if (summary.totalUnpaidDebtMinor > 0L) ext.warningAmber.copy(alpha = 0.45f)
                            else ext.cardBorder.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(ext.warningAmber.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.RequestQuote,
                                            contentDescription = tr(isArabic, "المديونيات عليّ", "Debts Owed"),
                                            tint = ext.warningAmber,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = tr(isArabic, "المديونيات عليّ", "Debts Owed"),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (totalUnpaidCount == 0) {
                                                tr(isArabic, "لا توجد ديون قائمة — اضغط للاستعراض", "No active debts — tap to view")
                                            } else {
                                                tr(
                                                    isArabic,
                                                    "$totalUnpaidCount التزام مستحق السداد (إيرادات + مصروفات)",
                                                    "$totalUnpaidCount obligations to repay (income + expense)"
                                                )
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (summary.totalUnpaidDebtMinor > 0L) ext.expenseContainer else ext.incomeContainer
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${formatAmountMinor(summary.totalUnpaidDebtMinor, false)} $sym",
                                            style = AmountBodyStyle.copy(
                                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (summary.totalUnpaidDebtMinor > 0L) ext.expenseColor else ext.incomeColor
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Direct Quick Tabs: Income Debts Entered Account vs Expense Debts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .testTag("home_debt_income_tab")
                                        .clickable { onOpenDebtsTab(TransactionType.INCOME) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.cardBackground
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                                        Text(
                                            text = tr(isArabic, "مديونية إيرادات دخلت لحسابي", "Income Entered Account"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatAmountMinor(summary.unpaidIncomeDebtMinor, false)} $sym",
                                            style = AmountBodyStyle.copy(
                                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = ext.warningAmber,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .testTag("home_debt_expense_tab")
                                        .clickable { onOpenDebtsTab(TransactionType.EXPENSE) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.cardBackground
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                                        Text(
                                            text = tr(isArabic, "مديونية مصروفات", "Expense Debts"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatAmountMinor(summary.unpaidExpenseDebtMinor, false)} $sym",
                                            style = AmountBodyStyle.copy(
                                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = ext.expenseColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dedicated Payments («المدفوعات») Hub Card with 3 Quick Tabs & Live Totals/Count
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_payments_btn")
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenPaymentsTab("all") },
                        shape = RoundedCornerShape(14.dp),
                        color = if (summary.settledDebtMinor > 0L) {
                            ext.incomeContainer.copy(alpha = 0.55f)
                        } else ext.cardSubtleBackground,
                        border = BorderStroke(
                            1.dp,
                            if (summary.settledDebtMinor > 0L) ext.incomeColor.copy(alpha = 0.40f)
                            else ext.cardBorder.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(ext.incomeContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Payments,
                                            contentDescription = tr(isArabic, "المدفوعات", "Payments"),
                                            tint = ext.incomeColor,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tr(isArabic, "المدفوعات", "Payments"),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (summary.settledDebtCount == 0) {
                                                tr(isArabic, "المديونيات التي تم سدادها تظهر هنا تلقائياً", "Settled debts appear here automatically")
                                            } else {
                                                tr(
                                                    isArabic,
                                                    "تم سداد ${summary.settledDebtCount} عملية • بحث وتصنيف كامل",
                                                    "${summary.settledDebtCount} paid items • Full search & tabs"
                                                )
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ext.incomeContainer)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${formatAmountMinor(summary.settledDebtMinor, false)} $sym",
                                            style = AmountBodyStyle.copy(
                                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = ext.incomeColor
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // 3 Quick Tabs: All | Settled Income Debts | Settled Expense Debts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .testTag("home_payment_all_tab")
                                        .clickable { onOpenPaymentsTab("all") },
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.cardBackground
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                                        Text(
                                            text = tr(isArabic, "الكل (${summary.settledDebtCount})", "All (${summary.settledDebtCount})"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatAmountMinor(summary.settledDebtMinor, false)} $sym",
                                            style = AmountBodyStyle.copy(
                                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = ext.incomeColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .testTag("home_payment_income_tab")
                                        .clickable { onOpenPaymentsTab(TransactionType.INCOME) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.cardBackground
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                                        Text(
                                            text = tr(isArabic, "إيرادات (${summary.settledIncomeDebtCount})", "Income (${summary.settledIncomeDebtCount})"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatAmountMinor(summary.settledIncomeDebtMinor, false)} $sym",
                                            style = AmountBodyStyle.copy(
                                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = ext.incomeColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .testTag("home_payment_expense_tab")
                                        .clickable { onOpenPaymentsTab(TransactionType.EXPENSE) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.cardBackground
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                                        Text(
                                            text = tr(isArabic, "مصروفات (${summary.settledExpenseDebtCount})", "Expenses (${summary.settledExpenseDebtCount})"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatAmountMinor(summary.settledExpenseDebtMinor, false)} $sym",
                                            style = AmountBodyStyle.copy(
                                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Recent Transactions Section (Empty State OR Live List)
            item {
                SereneCard(contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = tr(isArabic, "آخر العمليات", "Recent Transactions"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ext.incomeColor)
                            )
                        }
                        if (transactions.isEmpty()) {
                            Text(
                                text = tr(isArabic, "سجل هادئ ومستقر", "Calm & clear ledger"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = tr(isArabic, "عرض الكل", "View All"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ext.incomeColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onNavigate(AppTab.LOG) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (transactions.isEmpty()) {
                        // Illustrated Empty State matching screenshot 1
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ext.cardSubtleBackground.copy(alpha = 0.7f))
                                .padding(horizontal = 18.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(76.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(66.dp)
                                        .clip(CircleShape)
                                        .background(ext.cardBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CreditCard,
                                        contentDescription = null,
                                        tint = ext.incomeColor,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = tr(isArabic, "لا توجد عمليات مسجلة حتى الآن", "No transactions recorded yet"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tr(
                                    isArabic,
                                    "ابدأ بإضافة أول مصروف أو دخل مالي لتفعيل التحليلات والرسوم البيانية بكل سهولة وسلاسة.",
                                    "Start by logging your first expense or income to activate live analytics and charts."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onNavigate(AppTab.ADD) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("home_empty_add_btn"),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ext.heroCardBackground,
                                    contentColor = ext.heroCardOnBackground
                                )
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr(isArabic, "تسجيل عملية جديدة", "Log New Transaction"),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            transactions.take(5).forEach { tx ->
                                TransactionRowItem(
                                    tx = tx,
                                    categories = categories,
                                    isArabic = isArabic,
                                    onClick = { onNavigate(AppTab.LOG) },
                                    onToggleDebtSettled = if (tx.isDebt) {
                                        { settled -> onToggleDebtSettled(tx.id, settled) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // 6. Quiet Financial Tip Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = ext.cardSubtleBackground,
                    border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ext.cardBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = tr(
                                isArabic,
                                "نصيحة مالية: تقسيم الدخل الشهري إلى بنود واضحة يساعدك على الادخار بسهولة ودون قيود مرهقة.",
                                "Financial tip: Grouping monthly income into clear categories makes saving effortless."
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun HubShortcutTile(
    modifier: Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeText: String?,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = modifier
            .testTag(tag)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = ext.cardSubtleBackground
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ext.cardContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
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
    }
}
