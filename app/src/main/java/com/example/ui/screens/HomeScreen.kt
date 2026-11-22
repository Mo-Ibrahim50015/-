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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppSettingsEntity
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.AppTab
import com.example.ui.CurrencySummary
import com.example.ui.ReportAnalytics
import com.example.ui.SourceBreakdown
import com.example.ui.components.SereneCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.UserAvatarCircle
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.buildUserGreetingDisplay
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.isMorningHour
import com.example.util.rememberCurrentHourOfDay
import com.example.util.tr
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    analytics: ReportAnalytics,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    settings: AppSettingsEntity,
    pendingObligationsCount: Int = 0,
    showWelcomeBanner: Boolean,
    isArabic: Boolean,
    onDismissWelcomeBanner: () -> Unit,
    onNavigate: (AppTab) -> Unit,
    onOpenDebtsTab: (String) -> Unit,
    onOpenPaymentsTab: (String) -> Unit,
    onToggleDebtSettled: (Long, Boolean) -> Unit,
    onOpenFlowDrillDown: (FlowDrillDownSpec) -> Unit = {},
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onRequestNotificationPermission: () -> Unit = {}
) {
    val ext = LocalSereneColors.current
    val summary = analytics.primarySummary
    val defaultCurrency = settings.defaultCurrency
    val userName = settings.userName
    val userAvatarPath = settings.userAvatarPath
    val sym = currencySymbol(summary.currency.ifBlank { defaultCurrency }, isArabic)
    var showReconciliationDialog by remember { mutableStateOf(false) }
    var showAddObligationModal by remember { mutableStateOf(false) }

    val currentHour = rememberCurrentHourOfDay()
    val isMorning = isMorningHour(currentHour)
    val greetingDisplay = remember(
        currentHour,
        settings.userName,
        settings.customGreetingText,
        settings.customMorningGreeting,
        settings.customEveningGreeting,
        settings.showBaseGreeting,
        settings.showCustomGreeting,
        isArabic
    ) {
        buildUserGreetingDisplay(
            hourOfDay = currentHour,
            settings = settings,
            isArabic = isArabic
        )
    }

    var startWelcomeCountdown by remember { mutableStateOf(false) }
    LaunchedEffect(showWelcomeBanner, greetingDisplay.hasAnyGreeting) {
        if (showWelcomeBanner && greetingDisplay.hasAnyGreeting) {
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
                visible = showWelcomeBanner && greetingDisplay.hasAnyGreeting,
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
                                    contentDescription = greetingDisplay.combinedText
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = greetingDisplay.primaryLine,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val extraLine = greetingDisplay.secondaryLine
                                    if (!extraLine.isNullOrBlank()) {
                                        Text(
                                            text = extraLine,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = ext.incomeColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
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
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

            // 2. Hero Wallet Card: «المبلغ المتاح للصرف (بدون التزامات)»
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_balance_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = ext.heroCardBackground,
                    shadowElevation = 5.dp
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 30.dp, y = 30.dp)
                                .clip(CircleShape)
                                .background(ext.heroCardAccentCircle.copy(alpha = 0.45f))
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = ext.heroCardSubtext,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tr(
                                        isArabic,
                                        "المبلغ المتاح للصرف (بدون التزامات)",
                                        "Available to Spend (Without Obligations)"
                                    ),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = ext.heroCardSubtext,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formatAmountMinor(summary.availableWithoutObligationsMinor),
                                    style = AmountDisplayStyle,
                                    color = if (summary.availableWithoutObligationsMinor < 0L) Color(0xFFFF9B9B) else ext.heroCardOnBackground,
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

                            // Interactive Scientific Accounting & Reconciliation Verified Pill
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .testTag("home_reconciliation_pill")
                                    .clickable { showReconciliationDialog = true },
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(
                                    1.dp,
                                    if (summary.isFullyReconciled) Color(0xFF82F5C1).copy(alpha = 0.45f)
                                    else ext.warningAmber.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF82F5C1),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = tr(
                                            isArabic,
                                            "توفيق ومطابقة الحسابات (شخصي وتجاري) ✓",
                                            "Reconciled Accounting Audit ✓"
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }

                            if (analytics.currencySummaries.size > 1) {
                                Spacer(modifier = Modifier.height(2.dp))
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
                                                text = "${formatAmountMinor(cs.availableWithoutObligationsMinor)} $cSym",
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

            // 3. Two Requested Complementary Key Metric Boxes: «إجمالي الدخل (متاح بدون التزامات + التزامات)» & «إجمالي التزامات مطلوب دفعها»
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Box 1: إجمالي الدخل متاح بدون التزامات + التزامات
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("home_actual_balance_tile"),
                        shape = RoundedCornerShape(16.dp),
                        color = ext.cardBackground,
                        border = BorderStroke(1.dp, ext.incomeColor.copy(alpha = 0.35f)),
                        shadowElevation = if (ext.isDark) 1.dp else 2.5.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ext.incomeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Savings,
                                    contentDescription = null,
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(isArabic, "إجمالي الدخل (متاح + التزامات)", "Total Cash (Free + Due)"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatAmountMinor(summary.cashBalanceMinor, false)} $sym",
                                    style = AmountBodyStyle.copy(
                                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ext.incomeColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tr(isArabic, "رصيد المحفظة الفعلي", "Actual Cash Balance"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Box 2: إجمالي التزامات مطلوب دفعها
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("home_total_obligations_tile")
                            .clickable { onNavigate(AppTab.OBLIGATIONS) },
                        shape = RoundedCornerShape(16.dp),
                        color = ext.cardBackground,
                        border = BorderStroke(1.dp, ext.warningAmber.copy(alpha = 0.42f)),
                        shadowElevation = if (ext.isDark) 1.dp else 2.5.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ext.warningAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EventNote,
                                    contentDescription = null,
                                    tint = ext.warningAmber,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(isArabic, "إجمالي التزامات مطلوب دفعها", "Total Obligations Due"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatAmountMinor(summary.totalPendingObligationsMinor, false)} $sym",
                                    style = AmountBodyStyle.copy(
                                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (summary.totalPendingObligationsMinor > 0L) ext.warningAmber else ext.incomeColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val dueCount = summary.pendingScheduledObligationsCount + summary.unpaidDebtCount
                                Text(
                                    text = if (dueCount == 0) tr(isArabic, "لا توجد التزامات مستحقة", "No pending dues")
                                           else tr(isArabic, "$dueCount بند مطلوب سداده", "$dueCount items due"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 4. App Sections (4 Elevated Circular-Badge Cards matching reference design) + Amounts Hub
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        text = tr(isArabic, "أقسام التطبيق", "App Sections"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Row 1: إضافة عملية (Add) & التقارير (Reports)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HubShortcutTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Add,
                            iconBg = if (ext.isDark) Color(0xFF1A4234) else Color(0xFFC8F3DF),
                            iconTint = if (ext.isDark) Color(0xFF82F5C1) else Color(0xFF0A4D34),
                            title = tr(isArabic, "إضافة عملية", "Add Transaction"),
                            subtitle = tr(isArabic, "إضافة دخل أو مصروف", "Add income or expense"),
                            badgeText = null,
                            tag = "hub_add_btn",
                            onClick = { onNavigate(AppTab.ADD) }
                        )
                        HubShortcutTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.BarChart,
                            iconBg = if (ext.isDark) Color(0xFF1D2E54) else Color(0xFFDFEBFF),
                            iconTint = if (ext.isDark) Color(0xFF93B8FF) else Color(0xFF0B2568),
                            title = tr(isArabic, "التقارير", "Reports"),
                            subtitle = tr(isArabic, "تحليل المصروفات", "Expenses analytics"),
                            badgeText = null,
                            tag = "hub_reports_btn",
                            onClick = { onNavigate(AppTab.REPORTS) }
                        )
                    }

                    // Row 2: سجل العمليات (Log) & الإعدادات (Settings)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HubShortcutTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.History,
                            iconBg = if (ext.isDark) Color(0xFF3E3119) else Color(0xFFFCEBC9),
                            iconTint = if (ext.isDark) Color(0xFFFFD685) else Color(0xFF0F213D),
                            title = tr(isArabic, "سجل العمليات", "Transactions Log"),
                            subtitle = tr(isArabic, "كل الحركات المالية", "All financial movements"),
                            badgeText = null,
                            tag = "hub_log_btn",
                            onClick = { onNavigate(AppTab.LOG) }
                        )
                        HubShortcutTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Settings,
                            iconBg = if (ext.isDark) Color(0xFF282952) else Color(0xFFE7E8FB),
                            iconTint = if (ext.isDark) Color(0xFFB5B9FF) else Color(0xFF162766),
                            title = tr(isArabic, "الإعدادات", "Settings"),
                            subtitle = tr(isArabic, "تخصيص التطبيق", "Customize app"),
                            badgeText = null,
                            tag = "hub_settings_btn",
                            onClick = { onNavigate(AppTab.SETTINGS) }
                        )
                    }

                    // «مبالغ مطلوب دفعها» with subtle soft shading & direct Add action
                    val totalUpcomingDueCount = summary.unpaidDebtCount + summary.pendingScheduledObligationsCount
                    val totalUpcomingDueMinor = summary.totalPendingObligationsMinor
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_debts_btn")
                            .clip(RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        color = if (totalUpcomingDueMinor > 0L) {
                            ext.warningAmber.copy(alpha = if (ext.isDark) 0.14f else 0.09f)
                        } else ext.cardBackground,
                        border = BorderStroke(
                            1.dp,
                            if (totalUpcomingDueMinor > 0L) ext.warningAmber.copy(alpha = 0.38f)
                            else ext.cardBorder.copy(alpha = 0.45f)
                        ),
                        shadowElevation = if (ext.isDark) 2.dp else 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigate(AppTab.OBLIGATIONS) },
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
                                            imageVector = Icons.Outlined.EventNote,
                                            contentDescription = tr(isArabic, "مبالغ مطلوب دفعها", "All Upcoming Due Payments"),
                                            tint = ext.warningAmber,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tr(isArabic, "مبالغ مطلوب دفعها", "Amounts Due to Pay"),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (totalUpcomingDueCount == 0) {
                                                tr(isArabic, "لا توجد مبالغ مطلوبة قريباً — اضغط لإضافة التزام جديد", "No upcoming dues — tap to add new obligation")
                                            } else {
                                                tr(
                                                    isArabic,
                                                    "$totalUpcomingDueCount بند مطلوب دفعه (تخصم فوراً من المتاح للصرف)",
                                                    "$totalUpcomingDueCount items due (deducted from free available)"
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
                                                if (totalUpcomingDueMinor > 0L) ext.expenseContainer else ext.incomeContainer
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${formatAmountMinor(totalUpcomingDueMinor, false)} $sym",
                                            style = AmountBodyStyle.copy(
                                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (totalUpcomingDueMinor > 0L) ext.expenseColor else ext.incomeColor
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

                            // Dedicated Action Button: + إضافة مبالغ مطلوب دفعها (صفحة جديدة)
                            Button(
                                onClick = { showAddObligationModal = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("home_add_obligation_btn"),
                                shape = RoundedCornerShape(11.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (ext.isDark) Color(0xFF4A3800) else Color(0xFFFFF1C2),
                                    contentColor = if (ext.isDark) Color(0xFFFFD54F) else Color(0xFF6B4C00)
                                )
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr(isArabic, "+ إضافة مبالغ مطلوب دفعها جديدة", "+ Add New Amount Due to Pay"),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // Two Quick Sub-Pills: Expenses to pay soon vs Scheduled Obligations
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                            text = tr(isArabic, "مصاريف لسه هتندفع", "Unpaid Expenses"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatAmountMinor(summary.totalUnpaidDebtMinor, false)} $sym",
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
                                val scheduledDueTxMinor = remember(transactions, defaultCurrency) {
                                    transactions
                                        .filter {
                                             it.currency == defaultCurrency &&
                                                it.isDebt &&
                                                !it.isDebtSettled &&
                                                (it.hasPaymentReminder || it.paymentDueDate.isNotBlank())
                                        }
                                        .sumOf { it.amountMinor }
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .testTag("home_debt_income_tab")
                                        .clickable { onNavigate(AppTab.OBLIGATIONS) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.cardBackground
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                                        Text(
                                            text = tr(isArabic, "التزامات بمواعيد سداد", "Scheduled Payment Dues"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatAmountMinor(summary.pendingScheduledObligationsMinor + scheduledDueTxMinor, false)} $sym",
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
                            }
                        }
                    }

                    // «كل المبالغ اللي اندفعت بالفعل» with subtle soft shading
                    val totalActuallyPaidMinor = summary.actualCashOutMinor
                    val totalSettledOnlyMinor = summary.settledDebtMinor + summary.paidScheduledObligationsMinor
                    val totalActuallyPaidCount = transactions.count {
                        (it.type == TransactionType.EXPENSE && (!it.isDebt || it.isDebtSettled)) ||
                            (it.type == TransactionType.INCOME && it.isDebt && it.isDebtSettled)
                    } + summary.paidScheduledObligationsCount
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_payments_btn")
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenPaymentsTab("all") },
                        shape = RoundedCornerShape(18.dp),
                        color = if (totalActuallyPaidMinor > 0L) {
                            ext.incomeContainer.copy(alpha = if (ext.isDark) 0.35f else 0.45f)
                        } else ext.cardBackground,
                        border = BorderStroke(
                            1.dp,
                            if (totalActuallyPaidMinor > 0L) ext.incomeColor.copy(alpha = 0.35f)
                            else ext.cardBorder.copy(alpha = 0.45f)
                        ),
                        shadowElevation = if (ext.isDark) 2.dp else 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                            contentDescription = tr(isArabic, "كل المبالغ اللي اندفعت بالفعل", "All Actually Paid Amounts"),
                                            tint = ext.incomeColor,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tr(isArabic, "كل المبالغ اللي اندفعت بالفعل", "All Actually Paid Amounts"),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (totalActuallyPaidCount == 0) {
                                                tr(isArabic, "كل المصروفات والمبالغ المسددة تظهر هنا تلقائياً", "All paid expenses & settled dues appear here")
                                            } else {
                                                tr(
                                                    isArabic,
                                                    "تم دفع $totalActuallyPaidCount عملية فعلية • سجل كامل",
                                                    "$totalActuallyPaidCount actually paid items • Full view"
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
                                            text = "${formatAmountMinor(totalActuallyPaidMinor, false)} $sym",
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .testTag("home_payment_expense_tab")
                                        .clickable { onOpenPaymentsTab(TransactionType.EXPENSE) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.cardBackground
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                                        Text(
                                            text = tr(isArabic, "مصروفات اندفعت بالفعل", "Paid Expenses"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatAmountMinor(summary.paidExpenseMinor, false)} $sym",
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
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .testTag("home_payment_all_tab")
                                        .clickable { onOpenPaymentsTab("settled") },
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.cardBackground
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                                        Text(
                                            text = tr(isArabic, "التزامات وديون مسددة", "Settled Dues"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatAmountMinor(totalSettledOnlyMinor, false)} $sym",
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
                            }
                        }
                    }

                    // New Tab 1: «الفلوس اللي جاية ليا كدخل وربح» -> opens Reports drill-down
                    val primaryCurr = summary.currency.ifBlank { defaultCurrency }
                    val earnedIncomeList = remember(transactions, primaryCurr) {
                        transactions.filter {
                            it.currency == primaryCurr &&
                                it.type == TransactionType.INCOME &&
                                !it.isDebt
                        }
                    }
                    val earnedIncomeMinor = remember(earnedIncomeList) {
                        earnedIncomeList.sumOf { it.amountMinor }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_earned_income_tab")
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                onOpenFlowDrillDown(
                                    FlowDrillDownSpec(
                                        kind = FlowDrillDownKind.EARNED_INCOME,
                                        title = tr(isArabic, "الفلوس اللي جاية ليا كدخل وربح", "Incoming Money as Income & Profit"),
                                        subtitle = tr(isArabic, "كل عمليات الدخل والأرباح المستلمة فعلياً (بدون السلف)", "All earned income & profits received (excluding loans)"),
                                        currency = primaryCurr
                                    )
                                )
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = if (earnedIncomeMinor > 0L) {
                            ext.incomeContainer.copy(alpha = if (ext.isDark) 0.30f else 0.38f)
                        } else ext.cardBackground,
                        border = BorderStroke(
                            1.dp,
                            if (earnedIncomeMinor > 0L) ext.incomeColor.copy(alpha = 0.38f)
                            else ext.cardBorder.copy(alpha = 0.45f)
                        ),
                        shadowElevation = if (ext.isDark) 2.dp else 4.dp
                    ) {
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
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(ext.incomeContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDownward,
                                        contentDescription = tr(isArabic, "الفلوس اللي جاية ليا كدخل وربح", "Earned Income & Profit"),
                                        tint = ext.incomeColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr(isArabic, "الفلوس اللي جاية ليا كدخل وربح", "Incoming Money (Income & Profit)"),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (earnedIncomeList.isEmpty()) {
                                            tr(isArabic, "اضغط لعرض تفاصيل الدخل والأرباح في التقارير", "Tap to view income & profit report")
                                        } else {
                                            tr(isArabic, "${earnedIncomeList.size} عملية دخل وربح • عرض كل التفاصيل", "${earnedIncomeList.size} income entries • View all")
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
                                        text = "${formatAmountMinor(earnedIncomeMinor, false)} $sym",
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
                    }

                    // New Tab 2: «فلوس جاية ليا وهتندفع» right underneath -> opens Reports drill-down
                    val borrowedToRepayList = remember(transactions, primaryCurr) {
                        transactions.filter {
                            it.currency == primaryCurr &&
                                it.type == TransactionType.INCOME &&
                                it.isDebt &&
                                !it.isDebtSettled
                        }
                    }
                    val borrowedToRepayMinor = remember(borrowedToRepayList) {
                        borrowedToRepayList.sumOf { it.amountMinor }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_borrowed_income_tab")
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                onOpenFlowDrillDown(
                                    FlowDrillDownSpec(
                                        kind = FlowDrillDownKind.BORROWED_INCOME_TO_REPAY,
                                        title = tr(isArabic, "فلوس جاية ليا وهتندفع", "Incoming Money to Be Repaid"),
                                        subtitle = tr(isArabic, "السلف والمبالغ المستلمة اللي لسه مطلوب ردها أو سدادها", "Borrowed funds & amounts received awaiting repayment"),
                                        currency = primaryCurr
                                    )
                                )
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = if (borrowedToRepayMinor > 0L) {
                            ext.warningAmber.copy(alpha = if (ext.isDark) 0.14f else 0.10f)
                        } else ext.cardBackground,
                        border = BorderStroke(
                            1.dp,
                            if (borrowedToRepayMinor > 0L) ext.warningAmber.copy(alpha = 0.42f)
                            else ext.cardBorder.copy(alpha = 0.45f)
                        ),
                        shadowElevation = if (ext.isDark) 2.dp else 4.dp
                    ) {
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
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(ext.warningAmber.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.RequestQuote,
                                        contentDescription = tr(isArabic, "فلوس جاية ليا وهتندفع", "Incoming Money to Repay"),
                                        tint = ext.warningAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr(isArabic, "فلوس جاية ليا وهتندفع", "Incoming Money to Repay"),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (borrowedToRepayList.isEmpty()) {
                                            tr(isArabic, "لا توجد سلف مطلوبة للرد حالياً — عرض التقرير", "No borrowed amounts pending — View report")
                                        } else {
                                            tr(isArabic, "${borrowedToRepayList.size} مبلغ مستلم مطلوب سداده • عرض الكل", "${borrowedToRepayList.size} borrowed items pending • View all")
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
                                            if (borrowedToRepayMinor > 0L) ext.warningAmber.copy(alpha = 0.2f)
                                            else ext.cardSubtleBackground
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${formatAmountMinor(borrowedToRepayMinor, false)} $sym",
                                        style = AmountBodyStyle.copy(
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (borrowedToRepayMinor > 0L) ext.warningAmber else MaterialTheme.colorScheme.onSurfaceVariant
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

    if (showReconciliationDialog) {
        AccountingReconciliationDialog(
            summary = summary,
            sourceBreakdown = analytics.sourceBreakdown,
            sym = sym,
            isArabic = isArabic,
            onOpenObligations = { onNavigate(AppTab.OBLIGATIONS) },
            onDismiss = { showReconciliationDialog = false }
        )
    }

    if (showAddObligationModal) {
        AddObligationDialog(
            defaultCurrency = summary.currency.ifBlank { defaultCurrency },
            currentAvailableMinor = summary.availableWithoutObligationsMinor,
            settings = settings,
            isArabic = isArabic,
            onDismiss = { showAddObligationModal = false },
            onSave = { amt, curr, reason, party, dueDate, dueTime, remEnabled, leadMin, note, alertMode, fullScreen, speakAloud ->
                if (remEnabled) {
                    onRequestNotificationPermission()
                }
                onAddObligation(amt, curr, reason, party, dueDate, dueTime, remEnabled, leadMin, note, alertMode, fullScreen, speakAloud)
                showAddObligationModal = false
            }
        )
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
            .heightIn(min = 148.dp)
            .testTag(tag)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = ext.cardBackground,
        border = BorderStroke(
            width = 1.dp,
            color = ext.cardBorder.copy(alpha = if (ext.isDark) 0.45f else 0.35f)
        ),
        shadowElevation = if (ext.isDark) 2.dp else 5.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (badgeText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(ext.cardSubtleBackground)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(22.dp)
            )
        }
    }
}

@Composable
private fun CompactSmallSummaryTile(
    modifier: Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    amountText: String,
    amountColor: Color,
    subLabel: String,
    tag: String,
    highlightBorder: Color? = null,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = modifier
            .testTag(tag)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = ext.cardBackground,
        border = BorderStroke(1.dp, highlightBorder ?: ext.cardBorder.copy(alpha = 0.65f)),
        shadowElevation = if (ext.isDark) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = amountText,
                style = AmountBodyStyle.copy(
                    fontSize = MaterialTheme.typography.titleSmall.fontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = amountColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun AccountingReconciliationDialog(
    summary: CurrencySummary,
    sourceBreakdown: List<SourceBreakdown>,
    sym: String,
    isArabic: Boolean,
    onOpenObligations: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalSereneColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 540.dp)
                .testTag("accounting_reconciliation_dialog"),
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ext.incomeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Verified,
                                contentDescription = null,
                                tint = ext.incomeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tr(isArabic, "توفيق ومطابقة الحسابات", "Accounting Reconciliation"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (summary.isFullyReconciled) {
                                    tr(isArabic, "جميع المعادلات النقدية والتجارية متطابقة 100% ✓", "All cash, obligations & P&L equations match 100% ✓")
                                } else {
                                    tr(isArabic, "مراجعة الأرصدة (${summary.currency})", "Balance audit (${summary.currency})")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.incomeColor
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = tr(isArabic, "إغلاق", "Close"))
                    }
                }

                // Section 1: Cash Flow Basis
                ReconciliationSectionCard(
                    title = tr(isArabic, "١. التدفق النقدي الفعلي (الأساس النقدي)", "1. Actual Cash Position (Cash Basis)"),
                    badge = if (summary.isCashReconciled) "✓" else "!"
                ) {
                    ReconciliationLine(
                        label = tr(isArabic, "إجمالي المقبوضات النقدية (+)", "Actual Cash Inflows (+)"),
                        value = "${formatAmountMinor(summary.actualCashInMinor)} $sym",
                        valueColor = ext.incomeColor
                    )
                    ReconciliationLine(
                        label = tr(isArabic, "إجمالي المدفوعات النقدية (-)", "Actual Cash Outflows (-)"),
                        value = "${formatAmountMinor(summary.actualCashOutMinor)} $sym",
                        valueColor = ext.expenseColor
                    )
                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                    ReconciliationLine(
                        label = tr(isArabic, "الرصيد النقدي الفعلي (متاح + التزامات)", "Actual Cash Balance (Free + Due)"),
                        value = "${formatAmountMinor(summary.cashBalanceMinor)} $sym",
                        bold = true
                    )
                    ReconciliationLine(
                        label = tr(isArabic, "(-) التزامات مجدولة مطلوب دفعها", "(-) Scheduled Obligations Due"),
                        value = "${formatAmountMinor(summary.pendingScheduledObligationsMinor)} $sym",
                        valueColor = ext.warningAmber
                    )
                    ReconciliationLine(
                        label = tr(isArabic, "(-) مديونيات قائمة غير مسددة", "(-) Active Unpaid Debts"),
                        value = "${formatAmountMinor(summary.totalUnpaidDebtMinor)} $sym",
                        valueColor = ext.warningAmber
                    )
                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                    ReconciliationLine(
                        label = tr(isArabic, "= المتاح للصرف بدون التزامات", "= Free Available Without Obligations"),
                        value = "${formatAmountMinor(summary.availableWithoutObligationsMinor)} $sym",
                        valueColor = if (summary.availableWithoutObligationsMinor >= 0L) ext.incomeColor else ext.expenseColor,
                        bold = true
                    )
                }

                // Section 2: Commercial Operating Profit & Loss (Accrual Basis — excludes Loan Principal!)
                ReconciliationSectionCard(
                    title = tr(isArabic, "٢. صافي الربح التشغيلي (للاستخدام التجاري والشخصي)", "2. Operating Profit & Loss (Commercial & Personal)"),
                    badge = if (summary.isProfitReconciled) "✓" else "!"
                ) {
                    ReconciliationLine(
                        label = tr(isArabic, "الإيرادات المكتسبة (بدون قروض)", "Earned Revenue (Excl. Loans)"),
                        value = "${formatAmountMinor(summary.earnedIncomeMinor)} $sym",
                        valueColor = ext.incomeColor
                    )
                    ReconciliationLine(
                        label = tr(isArabic, "إجمالي المصروفات والتكاليف المستحقة", "Total Incurred Expenses"),
                        value = "${formatAmountMinor(summary.totalIncurredExpensesMinor)} $sym",
                        valueColor = ext.expenseColor
                    )
                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                    ReconciliationLine(
                        label = tr(isArabic, "صافي الربح / الفائض التشغيلي", "Operating Net Profit / Surplus"),
                        value = "${formatAmountMinor(summary.operatingNetProfitMinor)} $sym",
                        valueColor = if (summary.operatingNetProfitMinor >= 0L) ext.incomeColor else ext.expenseColor,
                        bold = true
                    )
                    ReconciliationLine(
                        label = tr(isArabic, "نسبة الهامش / الادخار الصافي", "Operating Margin / Savings Rate"),
                        value = "${summary.savingsRatePercent}%",
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                }

                // Section 3: Per-Account Cash Reconciliation
                if (sourceBreakdown.isNotEmpty()) {
                    ReconciliationSectionCard(
                        title = tr(isArabic, "٣. مطابقة أرصدة الحسابات والمحافظ", "3. Account-by-Account Cash Reconciliation"),
                        badge = if (summary.isAccountsReconciled) "✓" else "!"
                    ) {
                        sourceBreakdown.forEach { acc ->
                            ReconciliationLine(
                                label = acc.source,
                                value = "${formatAmountMinor(acc.cashBalanceMinor)} $sym",
                                valueColor = if (acc.cashBalanceMinor >= 0L) MaterialTheme.colorScheme.onSurface else ext.expenseColor
                            )
                        }
                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                        ReconciliationLine(
                            label = tr(isArabic, "مجموع أرصدة الحسابات (= الرصيد الفعلي)", "Sum of Accounts (= Actual Cash)"),
                            value = "${formatAmountMinor(summary.accountsCashTotalMinor)} $sym",
                            valueColor = ext.incomeColor,
                            bold = true
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenObligations()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.heroCardBackground,
                            contentColor = ext.heroCardOnBackground
                        )
                    ) {
                        Text(
                            text = tr(isArabic, "إدارة الالتزامات", "Manage Obligations"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconciliationSectionCard(
    title: String,
    badge: String,
    content: @Composable () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ext.cardSubtleBackground,
        border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = ext.incomeContainer
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ext.incomeColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun ReconciliationLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.labelSmall,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = AmountBodyStyle.copy(
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = valueColor,
            maxLines = 1,
            softWrap = false
        )
    }
}
