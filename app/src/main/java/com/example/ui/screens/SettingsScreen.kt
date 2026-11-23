package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pattern
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AccountEntity
import com.example.data.AppSettingsEntity
import com.example.data.CategoryEntity
import com.example.data.ObligationEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.BackupDrawerMode
import com.example.ui.components.FastTimePickerDialog
import com.example.ui.components.PatternSetupDialog
import com.example.ui.components.SecurityVerificationDialog
import com.example.ui.components.SereneCard
import com.example.ui.components.UserAvatarCircle
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.SelectableIconKeys
import com.example.util.SupportedCurrencies
import com.example.util.buildUserGreetingDisplay
import com.example.util.canUseDeviceBiometricOrCredential
import com.example.util.currencyInfoFor
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.iconForKey
import com.example.util.isMorningHour
import com.example.util.launchDeviceBiometricOrCredentialPrompt
import com.example.util.parseAmountToMinor
import com.example.util.rememberCurrentHourOfDay
import com.example.util.tr
import java.util.Calendar
import java.util.Locale

enum class SettingsSection {
    PERSONAL_INFO,
    DELETE_SECURITY,
    CURRENCIES_ACCOUNTS,
    BACKUP_DATA,
    APPEARANCE,
    NOTIFICATIONS,
    CATEGORIES,
    ANALYTICS
}

private sealed class PendingSettingsPurge {
    data object CurrentWeek : PendingSettingsPurge()
    data object CurrentMonth : PendingSettingsPurge()
    data class OlderThan(val months: Int) : PendingSettingsPurge()
    data class ByCategory(val category: String) : PendingSettingsPurge()
    data class BySource(val source: String) : PendingSettingsPurge()
    data class SingleTransaction(val tx: TransactionEntity) : PendingSettingsPurge()
    data class SingleObligation(val ob: ObligationEntity) : PendingSettingsPurge()
    data object WipeAll : PendingSettingsPurge()
    data object ResetLifetimeIncome : PendingSettingsPurge()
    data object ResetEarnedIncomeOnly : PendingSettingsPurge()
    data object ResetUnpaidIncomeDebts : PendingSettingsPurge()
    data object ResetSettledIncomeDebts : PendingSettingsPurge()
    data object ResetExpenses : PendingSettingsPurge()
    data object ResetPaidExpensesOnly : PendingSettingsPurge()
    data object ResetUnpaidExpenseDebts : PendingSettingsPurge()
    data object ResetSettledExpenseDebts : PendingSettingsPurge()
    data object ResetUnpaidDebts : PendingSettingsPurge()
    data object ResetSettledPayments : PendingSettingsPurge()
    data object ResetObligations : PendingSettingsPurge()
    data object ResetPendingObligations : PendingSettingsPurge()
    data object ResetPaidObligations : PendingSettingsPurge()
    data object ResetMonthlyBudget : PendingSettingsPurge()
    data object ResetAllAppNumbers : PendingSettingsPurge()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettingsEntity,
    transactions: List<TransactionEntity>,
    obligations: List<ObligationEntity> = emptyList(),
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    initialSection: SettingsSection = SettingsSection.DELETE_SECURITY,
    isArabic: Boolean,
    signedInEmail: String? = null,
    onSetQuickDelete: (Boolean) -> Unit,
    onDeleteCurrentWeek: () -> Unit = {},
    onDeleteCurrentMonth: () -> Unit,
    onDeleteOlderThanMonths: (Int) -> Unit,
    onDeleteByCategory: (String) -> Unit,
    onDeleteBySource: (String) -> Unit = {},
    onDeleteSingleTransaction: (Long) -> Unit = {},
    onDeleteSingleObligation: (Long) -> Unit = {},
    onWipeAll: () -> Unit,
    onResetLifetimeIncome: () -> Unit = {},
    onResetEarnedIncomeOnly: () -> Unit = {},
    onResetUnpaidIncomeDebts: () -> Unit = {},
    onResetSettledIncomeDebts: () -> Unit = {},
    onResetExpenses: () -> Unit = {},
    onResetPaidExpensesOnly: () -> Unit = {},
    onResetUnpaidExpenseDebts: () -> Unit = {},
    onResetSettledExpenseDebts: () -> Unit = {},
    onResetUnpaidDebts: () -> Unit = {},
    onResetSettledPayments: () -> Unit = {},
    onResetObligations: () -> Unit = {},
    onResetPendingObligations: () -> Unit = {},
    onResetPaidObligations: () -> Unit = {},
    onResetMonthlyBudget: () -> Unit = {},
    onResetAllAppNumbers: () -> Unit = {},
    onUpdateAnalyticsConfig: ((com.example.data.AnalyticsDashboardConfig) -> com.example.data.AnalyticsDashboardConfig) -> Unit = {},
    onSetPinProtection: (Boolean, String?) -> Unit,
    onSetBiometricProtection: (Boolean) -> Unit,
    onSetPatternProtection: (Boolean, String?) -> Unit,
    onSetConfirmPaymentWithSecurity: (Boolean) -> Unit,
    onSetAutoLock: (Boolean) -> Unit,
    onPinHomeWidget: () -> Unit,
    onSetDefaultCurrency: (String) -> Unit,
    onAddAccount: (name: String, iconKey: String, currency: String) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    onSetThemeMode: (String) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetReminders: (enabled: Boolean, time: String, monthlyLimitMinor: Long) -> Unit,
    onAddCategory: (name: String, iconKey: String, kind: String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onExportExcelAll: () -> Unit,
    onShareJsonBackup: () -> Unit,
    onOpenBackupSideDrawer: (BackupDrawerMode) -> Unit,
    onLaunchSaveBackupSaf: () -> Unit,
    onLaunchSavePdfSaf: () -> Unit,
    onLaunchSaveExcelSaf: () -> Unit,
    onLaunchSaveImageSaf: () -> Unit,
    onLaunchRestoreBackupSaf: () -> Unit,
    onUpdateUserName: (String) -> Unit,
    onUpdateGreetingPreferences: (
        customFull: String,
        customMorning: String,
        customEvening: String,
        showBaseGreeting: Boolean,
        showCustomGreeting: Boolean
    ) -> Unit = { _, _, _, _, _ -> },
    onSetGreetingVisibility: (showBaseGreeting: Boolean, showCustomGreeting: Boolean) -> Unit = { _, _ -> },
    onPickUserAvatar: () -> Unit,
    onRemoveUserAvatar: () -> Unit,
    onEmitMessage: (String) -> Unit
) {
    val ext = LocalSereneColors.current
    val context = LocalContext.current

    var activeSection by remember(initialSection) { mutableStateOf(initialSection) }
    var deleteSubTab by remember { mutableStateOf("delete") } // "delete" or "security"
    var deleteSearchQuery by remember { mutableStateOf("") }
    var deletePageFilter by remember { mutableStateOf("all") }
    var showResetNumbersGrid by remember { mutableStateOf(false) }
    BackHandler(enabled = activeSection == SettingsSection.DELETE_SECURITY && deleteSubTab == "delete" && showResetNumbersGrid) {
        showResetNumbersGrid = false
    }
    var pendingPurge by remember { mutableStateOf<PendingSettingsPurge?>(null) }
    var showCategoryPurgeModal by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPatternSetupDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showDailyReminderTimePicker by remember { mutableStateOf(false) }
    var categoryManagerKind by remember { mutableStateOf(TransactionType.EXPENSE) }
    var profileNameInput by remember(settings.userName) { mutableStateOf(settings.userName) }
    var customGreetingInput by remember(settings.customGreetingText) { mutableStateOf(settings.customGreetingText) }
    var customMorningInput by remember(settings.customMorningGreeting) { mutableStateOf(settings.customMorningGreeting) }
    var customEveningInput by remember(settings.customEveningGreeting) { mutableStateOf(settings.customEveningGreeting) }
    var showBaseGreetingInput by remember(settings.showBaseGreeting) { mutableStateOf(settings.showBaseGreeting) }
    var showCustomGreetingInput by remember(settings.showCustomGreeting) { mutableStateOf(settings.showCustomGreeting) }

    val currInfo = currencyInfoFor(settings.defaultCurrency)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Compact Personal Info Shortcut Banner (tapping opens «المعلومات الشخصية» section)
            item {
                SereneCard(
                    modifier = Modifier
                        .testTag("settings_profile_summary_banner")
                        .clickable { activeSection = SettingsSection.PERSONAL_INFO },
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            UserAvatarCircle(
                                avatarPath = settings.userAvatarPath,
                                userName = settings.userName,
                                size = 48.dp,
                                fallbackIcon = Icons.Outlined.Person,
                                contentDescription = tr(isArabic, "صورة المستخدم", "User photo")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = settings.userName.ifBlank {
                                        tr(isArabic, "المعلومات الشخصية", "Personal Information")
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tr(
                                        isArabic,
                                        "اضغط لتعديل الاسم، الصورة الشخصية، وإضافة أيقونة سطح المكتب",
                                        "Tap to edit name, profile photo, and Home Screen icon"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = ext.incomeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 1. Header Status Card Banner
            item {
                SereneCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ext.heroCardBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DashboardCustomize,
                                    contentDescription = null,
                                    tint = ext.heroCardOnBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = tr(isArabic, "مركز التحكم والتفضيلات", "Control & Preferences"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = ext.incomeColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = tr(
                                            isArabic,
                                            "العملة: ${currInfo.nameAr} (${currInfo.symbolAr}) • محلي وآمن",
                                            "Currency: ${currInfo.code} • Local & Private"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Sync button opens Backup & Restore Side Drawer directly
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("settings_sync_btn")
                                .clickable { onOpenBackupSideDrawer(BackupDrawerMode.SAVE) },
                            shape = RoundedCornerShape(10.dp),
                            color = ext.cardSubtleBackground,
                            border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = null,
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tr(isArabic, "مزامنة", "Backup"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Modern Settings Icons Grid (7 Core Areas including Personal Info)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(isArabic, "أقسام الإعدادات", "Settings Sections"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(ext.incomeContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = tr(isArabic, "٨ أقسام رئيسية", "8 Core Areas"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ext.incomeColor
                            )
                        }
                    }

                    // Featured Row 0: Dedicated «المعلومات الشخصية» Icon + «إعدادات التحليل» Icon + Home Screen Widget Shortcut
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Person,
                            iconBg = ext.incomeContainer,
                            iconTint = ext.incomeColor,
                            title = tr(isArabic, "المعلومات الشخصية", "Personal Info"),
                            subtitle = settings.userName.ifBlank { tr(isArabic, "الاسم والصورة", "Name & photo") },
                            selected = activeSection == SettingsSection.PERSONAL_INFO,
                            isDangerHighlight = false,
                            tag = "settings_tile_personal_info",
                            onClick = { activeSection = SettingsSection.PERSONAL_INFO }
                        )
                        SettingsGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Analytics,
                            iconBg = ext.warningAmber.copy(alpha = 0.18f),
                            iconTint = ext.warningAmber,
                            title = tr(isArabic, "إعدادات التحليل", "Analytics Settings"),
                            subtitle = tr(isArabic, "مربعات الداش بورد", "Dashboard boxes"),
                            selected = activeSection == SettingsSection.ANALYTICS,
                            isDangerHighlight = false,
                            tag = "settings_tile_analytics",
                            onClick = { activeSection = SettingsSection.ANALYTICS }
                        )
                        SettingsGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Widgets,
                            iconBg = Color(0xFFD8E3FB).copy(alpha = if (ext.isDark) 0.22f else 1f),
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = tr(isArabic, "أيقونة سطح المكتب", "Home Icon"),
                            subtitle = tr(isArabic, "إضافة أيقونة (1×1)", "Pin 1x1 app icon"),
                            selected = false,
                            isDangerHighlight = false,
                            tag = "settings_tile_pin_widget",
                            onClick = onPinHomeWidget
                        )
                    }

                    // Row 1 of 3x2 grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.DeleteSweep,
                            iconBg = ext.expenseContainer,
                            iconTint = ext.expenseColor,
                            title = tr(isArabic, "الحذف والأمان", "Delete & Security"),
                            subtitle = tr(isArabic, "البصمة والنمط والحذف", "Biometric & purge"),
                            selected = activeSection == SettingsSection.DELETE_SECURITY,
                            isDangerHighlight = true,
                            tag = "settings_tile_security",
                            onClick = { activeSection = SettingsSection.DELETE_SECURITY }
                        )
                        SettingsGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.AccountBalance,
                            iconBg = Color(0xFFD8E3FB).copy(alpha = if (ext.isDark) 0.22f else 1f),
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = tr(isArabic, "العملات والحسابات", "Currencies"),
                            subtitle = tr(isArabic, "${currInfo.symbolAr} • المحافظ", "${currInfo.code} • Accounts"),
                            selected = activeSection == SettingsSection.CURRENCIES_ACCOUNTS,
                            isDangerHighlight = false,
                            tag = "settings_tile_currencies",
                            onClick = { activeSection = SettingsSection.CURRENCIES_ACCOUNTS }
                        )
                        SettingsGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.CloudSync,
                            iconBg = ext.incomeContainer,
                            iconTint = ext.incomeColor,
                            title = tr(isArabic, "النسخ والبيانات", "Backup"),
                            subtitle = tr(isArabic, "إيميل سحابي وملفات", "Cloud email & files"),
                            selected = activeSection == SettingsSection.BACKUP_DATA,
                            isDangerHighlight = false,
                            tag = "settings_tile_backup",
                            onClick = { activeSection = SettingsSection.BACKUP_DATA }
                        )
                    }

                    // Row 2 of 3x2 grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Palette,
                            iconBg = ext.cardContainer,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = tr(isArabic, "المظهر والألوان", "Appearance"),
                            subtitle = tr(isArabic, "السمات واللغة", "Themes & lang"),
                            selected = activeSection == SettingsSection.APPEARANCE,
                            isDangerHighlight = false,
                            tag = "settings_tile_appearance",
                            onClick = { activeSection = SettingsSection.APPEARANCE }
                        )
                        SettingsGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.NotificationsActive,
                            iconBg = ext.incomeContainer,
                            iconTint = ext.incomeColor,
                            title = tr(isArabic, "الإشعارات والتذكير", "Reminders"),
                            subtitle = tr(isArabic, "التنبيه اليومي", "Daily & budget"),
                            selected = activeSection == SettingsSection.NOTIFICATIONS,
                            isDangerHighlight = false,
                            tag = "settings_tile_notifications",
                            onClick = { activeSection = SettingsSection.NOTIFICATIONS }
                        )
                        SettingsGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Category,
                            iconBg = ext.expenseContainer.copy(alpha = 0.7f),
                            iconTint = ext.chartRose,
                            title = tr(isArabic, "إدارة التصنيفات", "Categories"),
                            subtitle = tr(isArabic, "تخصيص البنود", "Custom tags"),
                            selected = activeSection == SettingsSection.CATEGORIES,
                            isDangerHighlight = false,
                            tag = "settings_tile_categories",
                            onClick = { activeSection = SettingsSection.CATEGORIES }
                        )
                    }
                }
            }

            // 3. Detailed Active Section Content
            item {
                when (activeSection) {
                    SettingsSection.PERSONAL_INFO -> {
                        val currentHourPreview = rememberCurrentHourOfDay()
                        val isMorningPreview = isMorningHour(currentHourPreview)
                        val previewGreetingDisplay = remember(
                            currentHourPreview,
                            profileNameInput,
                            customGreetingInput,
                            customMorningInput,
                            customEveningInput,
                            showBaseGreetingInput,
                            showCustomGreetingInput,
                            isArabic
                        ) {
                            buildUserGreetingDisplay(
                                hourOfDay = currentHourPreview,
                                settings = settings,
                                isArabic = isArabic,
                                nameOverride = profileNameInput,
                                customGreetingOverride = customGreetingInput,
                                morningGreetingOverride = customMorningInput,
                                eveningGreetingOverride = customEveningInput,
                                showBaseGreetingOverride = showBaseGreetingInput,
                                showCustomGreetingOverride = showCustomGreetingInput
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SereneCard(
                                modifier = Modifier.testTag("settings_profile_card"),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = ext.incomeColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = tr(isArabic, "المعلومات الشخصية", "Personal Information"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .testTag("settings_avatar_picker_btn")
                                            .clickable(onClick = onPickUserAvatar),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        UserAvatarCircle(
                                            avatarPath = settings.userAvatarPath,
                                            userName = settings.userName,
                                            size = 66.dp,
                                            fallbackIcon = Icons.Outlined.Person,
                                            contentDescription = tr(isArabic, "صورة المستخدم", "User photo")
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .background(ext.heroCardBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AddAPhoto,
                                                contentDescription = tr(isArabic, "تغيير الصورة", "Change photo"),
                                                tint = ext.heroCardOnBackground,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (previewGreetingDisplay.hasAnyGreeting) {
                                            Text(
                                                text = previewGreetingDisplay.primaryLine,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ext.incomeColor,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val previewExtra = previewGreetingDisplay.secondaryLine
                                            if (!previewExtra.isNullOrBlank()) {
                                                Text(
                                                    text = previewExtra,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = tr(isArabic, "التحية مخفية حالياً من الرئيسية", "Greeting hidden on Home"),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = tr(
                                                isArabic,
                                                "محفوظ محلياً على هذا الهاتف فقط • كل جهاز مستقل ببياناته",
                                                "Saved on this device only • Each phone is fully independent"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .testTag("settings_change_photo_btn")
                                                    .clickable(onClick = onPickUserAvatar),
                                                shape = RoundedCornerShape(999.dp),
                                                color = ext.incomeContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.AddAPhoto,
                                                        contentDescription = null,
                                                        tint = ext.incomeColor,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = if (settings.userAvatarPath.isBlank()) {
                                                            tr(isArabic, "إضافة صورة من الملفات", "Pick from Files")
                                                        } else {
                                                            tr(isArabic, "تغيير من ملفات الجهاز", "Change from Files")
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = ext.incomeColor
                                                    )
                                                }
                                            }
                                            if (settings.userAvatarPath.isNotBlank()) {
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .testTag("settings_remove_photo_btn")
                                                        .clickable(onClick = onRemoveUserAvatar),
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = ext.expenseContainer
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Delete,
                                                            contentDescription = null,
                                                            tint = ext.expenseColor,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = tr(isArabic, "حذف الصورة", "Remove"),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = ext.expenseColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = profileNameInput,
                                        onValueChange = { if (it.length <= 40) profileNameInput = it },
                                        singleLine = true,
                                        label = {
                                            Text(
                                                text = tr(isArabic, "اسمك للترحيب (صباح/مساء الخير)", "Your Name for Greeting"),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        placeholder = {
                                            Text(tr(isArabic, "مثال: محمد", "e.g. Alex"), maxLines = 1)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_user_name_input")
                                    )
                                    Button(
                                        onClick = { onUpdateUserName(profileNameInput) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ext.heroCardBackground,
                                            contentColor = ext.heroCardOnBackground
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .heightIn(min = 52.dp)
                                            .testTag("settings_save_name_btn")
                                    ) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = tr(isArabic, "حفظ", "Save"),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Custom Greeting Editor with Independent Toggle Switches for Base & Additional Greetings
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tr(isArabic, "تخصيص التحية الترحيبية", "Customize Greeting"),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = ext.incomeContainer
                                        ) {
                                            Text(
                                                text = if (isMorningPreview) {
                                                    tr(isArabic, "الآن: فترة الصباح (١٢ ص - ١٢ ظ)", "Now: Morning (12 AM–12 PM)")
                                                } else {
                                                    tr(isArabic, "الآن: فترة المساء (١٢ ظ - ١٢ ص)", "Now: Evening (12 PM–12 AM)")
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ext.incomeColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = tr(
                                            isArabic,
                                            "تحكّم في إظهار أو إخفاء التحية الأساسية والتحية الإضافية بشكل مستقل عبر زر التشغيل بجانب كل منهما (يمكنك إظهارهما معاً أو إظهار واحدة فقط أو إخفاؤهما).",
                                            "Control Base and Additional greetings independently using the toggle beside each one (show both together, show only one, or hide both)."
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // 1) Base Greeting Card (Morning / Evening) + Toggle Switch
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (showBaseGreetingInput) {
                                            ext.incomeContainer.copy(alpha = if (ext.isDark) 0.18f else 0.28f)
                                        } else {
                                            ext.cardSubtleBackground
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (showBaseGreetingInput) ext.incomeColor.copy(alpha = 0.32f)
                                            else ext.cardBorder.copy(alpha = 0.55f)
                                        )
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
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(
                                                                if (showBaseGreetingInput) ext.incomeContainer
                                                                else ext.cardBackground
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isMorningPreview) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay,
                                                            contentDescription = null,
                                                            tint = if (showBaseGreetingInput) ext.incomeColor
                                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = tr(isArabic, "التحية الأساسية (صباح / مساء الخير)", "Base Greeting (Morning / Evening)"),
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = if (showBaseGreetingInput) {
                                                                tr(isArabic, "مفعّلة — تظهر في الصفحة الرئيسية", "Enabled — visible on Home")
                                                            } else {
                                                                tr(isArabic, "مخفية — لن تظهر التحية الأساسية", "Hidden — base greeting won't show")
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (showBaseGreetingInput) ext.incomeColor
                                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Switch(
                                                    checked = showBaseGreetingInput,
                                                    onCheckedChange = { enabled ->
                                                        showBaseGreetingInput = enabled
                                                        onSetGreetingVisibility(enabled, showCustomGreetingInput)
                                                    },
                                                    modifier = Modifier.testTag("settings_toggle_base_greeting"),
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = ext.incomeColor
                                                    )
                                                )
                                            }

                                            AnimatedVisibility(visible = showBaseGreetingInput) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = customMorningInput,
                                                        onValueChange = { if (it.length <= 35) customMorningInput = it },
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(12.dp),
                                                        label = {
                                                            Text(
                                                                tr(isArabic, "تحية الصباح (١٢ ص+)", "Morning (12 AM+)"),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        },
                                                        placeholder = { Text(tr(isArabic, "صباح الخير", "Good Morning")) },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("settings_morning_greeting_input")
                                                    )
                                                    OutlinedTextField(
                                                        value = customEveningInput,
                                                        onValueChange = { if (it.length <= 35) customEveningInput = it },
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(12.dp),
                                                        label = {
                                                            Text(
                                                                tr(isArabic, "تحية المساء (١٢ ظ+)", "Evening (12 PM+)"),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        },
                                                        placeholder = { Text(tr(isArabic, "مساء الخير", "Good Evening")) },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("settings_evening_greeting_input")
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 2) Additional Custom Greeting Card + Toggle Switch
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (showCustomGreetingInput) {
                                            ext.cardContainer.copy(alpha = if (ext.isDark) 0.25f else 0.45f)
                                        } else {
                                            ext.cardSubtleBackground
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (showCustomGreetingInput) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            else ext.cardBorder.copy(alpha = 0.55f)
                                        )
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
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(
                                                                if (showCustomGreetingInput) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                                                else ext.cardBackground
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.AutoAwesome,
                                                            contentDescription = null,
                                                            tint = if (showCustomGreetingInput) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = tr(isArabic, "التحية الإضافية (المخصصة)", "Additional Custom Greeting"),
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        val customStatusText = when {
                                                            !showCustomGreetingInput -> tr(
                                                                isArabic,
                                                                "مخفية — لن تظهر التحية الإضافية",
                                                                "Hidden — additional greeting won't show"
                                                            )
                                                            showBaseGreetingInput -> tr(
                                                                isArabic,
                                                                "مفعّلة — تظهر مع التحية الأساسية معاً",
                                                                "Enabled — shown alongside Base greeting"
                                                            )
                                                            else -> tr(
                                                                isArabic,
                                                                "مفعّلة — تظهر بمفردها (الأساسية مخفية)",
                                                                "Enabled — shown alone (Base hidden)"
                                                            )
                                                        }
                                                        Text(
                                                            text = customStatusText,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (showCustomGreetingInput) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Switch(
                                                    checked = showCustomGreetingInput,
                                                    onCheckedChange = { enabled ->
                                                        showCustomGreetingInput = enabled
                                                        onSetGreetingVisibility(showBaseGreetingInput, enabled)
                                                    },
                                                    modifier = Modifier.testTag("settings_toggle_custom_greeting"),
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            }

                                            OutlinedTextField(
                                                value = customGreetingInput,
                                                onValueChange = { if (it.length <= 60) customGreetingInput = it },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                label = {
                                                    Text(
                                                        tr(isArabic, "نص التحية الإضافية (استخدم {name} لاسمك)", "Additional Greeting Text ({name} optional)"),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                placeholder = {
                                                    Text(
                                                        tr(isArabic, "مثال: يومك سعيد ومليء بالبركة", "e.g., Wishing you a blessed day"),
                                                        maxLines = 1
                                                    )
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("settings_custom_greeting_input")
                                            )
                                        }
                                    }

                                    // Live Preview Strip showing exact result on Home
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = ext.cardSubtleBackground,
                                        border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.55f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (previewGreetingDisplay.hasAnyGreeting) Icons.Outlined.Check else Icons.Outlined.VisibilityOff,
                                                contentDescription = null,
                                                tint = if (previewGreetingDisplay.hasAnyGreeting) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = tr(isArabic, "معاينة ظهور التحية في الرئيسية:", "Home Screen Preview:"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (previewGreetingDisplay.hasAnyGreeting) {
                                                    Text(
                                                        text = previewGreetingDisplay.primaryLine,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    val secondaryPreview = previewGreetingDisplay.secondaryLine
                                                    if (!secondaryPreview.isNullOrBlank()) {
                                                        Text(
                                                            text = secondaryPreview,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = ext.incomeColor
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = if (!showBaseGreetingInput && showCustomGreetingInput && customGreetingInput.isBlank()) {
                                                            tr(isArabic, "اكتب التحية الإضافية في الخانة أعلاه لتظهر", "Type your additional greeting above to display it")
                                                        } else {
                                                            tr(isArabic, "التحية مخفية حالياً (فعّل أحد الزرين لإظهارها)", "Greeting is currently hidden (toggle either switch ON)")
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                onUpdateUserName(profileNameInput)
                                                onUpdateGreetingPreferences(
                                                    customGreetingInput,
                                                    customMorningInput,
                                                    customEveningInput,
                                                    showBaseGreetingInput,
                                                    showCustomGreetingInput
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ext.incomeColor,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("settings_save_greeting_btn")
                                        ) {
                                            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = tr(isArabic, "حفظ إعدادات التحية", "Save Greeting Settings"),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        if (
                                            customGreetingInput.isNotBlank() ||
                                            customMorningInput.isNotBlank() ||
                                            customEveningInput.isNotBlank() ||
                                            !showBaseGreetingInput ||
                                            !showCustomGreetingInput
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    customGreetingInput = ""
                                                    customMorningInput = ""
                                                    customEveningInput = ""
                                                    showBaseGreetingInput = true
                                                    showCustomGreetingInput = true
                                                    onUpdateGreetingPreferences("", "", "", true, true)
                                                },
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("settings_reset_greeting_btn")
                                            ) {
                                                Text(
                                                    text = tr(isArabic, "افتراضي", "Reset"),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Home Screen Widget & App Icon Shortcuts Card
                            SereneCard(contentPadding = PaddingValues(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ext.heroCardBackground),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Widgets,
                                            contentDescription = null,
                                            tint = Color(0xFF82F5C1),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tr(isArabic, "أيقونة مصروفي على سطح المكتب", "Masrofy Home Screen Icon"),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = tr(
                                                isArabic,
                                                "أضف أيقونة مصروفي فقط (1×1) على الشاشة الرئيسية للوصول السريع بدون بطاقات كبيرة",
                                                "Pin a clean 1x1 Masrofy app icon only to your Home Screen"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onPinHomeWidget,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_pin_widget_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ext.heroCardBackground,
                                        contentColor = ext.heroCardOnBackground
                                    )
                                ) {
                                    Icon(Icons.Outlined.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(tr(isArabic, "إضافة أيقونة مصروفي على سطح المكتب", "Add Masrofy Icon to Home Screen"))
                                }
                            }
                        }
                    }

                    SettingsSection.DELETE_SECURITY -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Header with Sub-tabs (الحذف / الأمان)
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
                                        Icons.Outlined.Shield,
                                        contentDescription = null,
                                        tint = ext.expenseColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = tr(isArabic, "إدارة الحذف والأمان", "Delete & Security"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ext.cardSubtleBackground,
                                    border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.5f))
                                ) {
                                    Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        SubTabPill(
                                            label = tr(isArabic, "الحذف", "Delete"),
                                            icon = Icons.Outlined.DeleteSweep,
                                            selected = deleteSubTab == "delete",
                                            activeColor = ext.expenseColor,
                                            onClick = { deleteSubTab = "delete" }
                                        )
                                        SubTabPill(
                                            label = tr(isArabic, "الأمان", "Security"),
                                            icon = Icons.Outlined.LockReset,
                                            selected = deleteSubTab == "security",
                                            activeColor = MaterialTheme.colorScheme.primary,
                                            onClick = { deleteSubTab = "security" }
                                        )
                                    }
                                }
                            }

                            if (deleteSubTab == "delete") {
                                val sym = currencySymbol(settings.defaultCurrency, isArabic)
                                val primaryCurr = settings.defaultCurrency
                                val lifetimeIncomeMinor = remember(transactions, primaryCurr) {
                                    transactions.filter { it.type == TransactionType.INCOME && it.currency == primaryCurr }
                                        .sumOf { it.amountMinor }
                                }
                                val lifetimeIncomeCount = remember(transactions) {
                                    transactions.count { it.type == TransactionType.INCOME }
                                }
                                val earnedIncomeOnlyMinor = remember(transactions, primaryCurr) {
                                    transactions.filter { it.type == TransactionType.INCOME && !it.isDebt && it.currency == primaryCurr }
                                        .sumOf { it.amountMinor }
                                }
                                val earnedIncomeOnlyCount = remember(transactions) {
                                    transactions.count { it.type == TransactionType.INCOME && !it.isDebt }
                                }
                                val unpaidIncomeDebtMinor = remember(transactions, primaryCurr) {
                                    transactions.filter { it.type == TransactionType.INCOME && it.isDebt && !it.isDebtSettled && it.currency == primaryCurr }
                                        .sumOf { it.amountMinor }
                                }
                                val unpaidIncomeDebtCount = remember(transactions) {
                                    transactions.count { it.type == TransactionType.INCOME && it.isDebt && !it.isDebtSettled }
                                }
                                val settledIncomeDebtMinor = remember(transactions, primaryCurr) {
                                    transactions.filter { it.type == TransactionType.INCOME && it.isDebt && it.isDebtSettled && it.currency == primaryCurr }
                                        .sumOf { it.amountMinor }
                                }
                                val settledIncomeDebtCount = remember(transactions) {
                                    transactions.count { it.type == TransactionType.INCOME && it.isDebt && it.isDebtSettled }
                                }
                                val totalExpenseMinor = remember(transactions, primaryCurr) {
                                    transactions.filter { it.type == TransactionType.EXPENSE && it.currency == primaryCurr }
                                        .sumOf { it.amountMinor }
                                }
                                val totalExpenseCount = remember(transactions) {
                                    transactions.count { it.type == TransactionType.EXPENSE }
                                }
                                val paidExpenseOnlyMinor = remember(transactions, primaryCurr) {
                                    transactions.filter { it.type == TransactionType.EXPENSE && !it.isDebt && it.currency == primaryCurr }
                                        .sumOf { it.amountMinor }
                                }
                                val paidExpenseOnlyCount = remember(transactions) {
                                    transactions.count { it.type == TransactionType.EXPENSE && !it.isDebt }
                                }
                                val unpaidExpenseDebtMinor = remember(transactions, primaryCurr) {
                                    transactions.filter { it.type == TransactionType.EXPENSE && it.isDebt && !it.isDebtSettled && it.currency == primaryCurr }
                                        .sumOf { it.amountMinor }
                                }
                                val unpaidExpenseDebtCount = remember(transactions) {
                                    transactions.count { it.type == TransactionType.EXPENSE && it.isDebt && !it.isDebtSettled }
                                }
                                val settledExpenseDebtMinor = remember(transactions, primaryCurr) {
                                    transactions.filter { it.type == TransactionType.EXPENSE && it.isDebt && it.isDebtSettled && it.currency == primaryCurr }
                                        .sumOf { it.amountMinor }
                                }
                                val settledExpenseDebtCount = remember(transactions) {
                                    transactions.count { it.type == TransactionType.EXPENSE && it.isDebt && it.isDebtSettled }
                                }
                                val unpaidDebtMinor = remember(transactions, primaryCurr) {
                                    transactions.filter { it.isDebt && !it.isDebtSettled && it.currency == primaryCurr }
                                        .sumOf { it.amountMinor }
                                }
                                val unpaidDebtCount = remember(transactions) {
                                    transactions.count { it.isDebt && !it.isDebtSettled }
                                }
                                val settledPaymentMinor = remember(transactions, primaryCurr) {
                                    transactions.filter { it.isDebt && it.isDebtSettled && it.currency == primaryCurr }
                                        .sumOf { it.amountMinor }
                                }
                                val settledPaymentCount = remember(transactions) {
                                    transactions.count { it.isDebt && it.isDebtSettled }
                                }
                                val obligationsTotalMinor = remember(obligations, primaryCurr) {
                                    obligations.filter { it.currency == primaryCurr }.sumOf { it.amountMinor }
                                }
                                val obligationsCount = obligations.size
                                val pendingObligationsMinor = remember(obligations, primaryCurr) {
                                    obligations.filter { !it.isPaid && it.currency == primaryCurr }.sumOf { it.amountMinor }
                                }
                                val pendingObligationsCount = remember(obligations) {
                                    obligations.count { !it.isPaid }
                                }
                                val paidObligationsMinor = remember(obligations, primaryCurr) {
                                    obligations.filter { it.isPaid && it.currency == primaryCurr }.sumOf { it.amountMinor }
                                }
                                val paidObligationsCount = remember(obligations) {
                                    obligations.count { it.isPaid }
                                }

                                if (showResetNumbersGrid) {
                                    // Dedicated «تصفير الأرقام» Icon Grid matching the app's tile scheme for EVERY app number
                                    SereneCard(
                                        modifier = Modifier.testTag("settings_reset_numbers_grid"),
                                        contentPadding = PaddingValues(14.dp)
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
                                                        .background(ext.expenseContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.RestartAlt,
                                                        contentDescription = null,
                                                        tint = ext.expenseColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = tr(isArabic, "تصفير أرقام التطبيق", "Reset App Numbers"),
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = tr(
                                                            isArabic,
                                                            "اختر أي رقم لتصفيره على حدة أو صفّر الكل",
                                                            "Tap any metric tile to reset it to 0.00"
                                                        ),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .testTag("settings_reset_numbers_back_btn")
                                                    .clickable { showResetNumbersGrid = false },
                                                shape = RoundedCornerShape(10.dp),
                                                color = ext.cardSubtleBackground,
                                                border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.6f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Text(
                                                        text = tr(isArabic, "رجوع", "Back"),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Row 1: الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج | إجمالي المصاريف
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            ResetNumberGridTile(
                                                modifier = Modifier.weight(1f),
                                                icon = Icons.Outlined.Savings,
                                                iconBg = ext.incomeContainer,
                                                iconTint = ext.incomeColor,
                                                title = tr(isArabic, "الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج", "Lifetime Added Money"),
                                                valueText = "${formatAmountMinor(lifetimeIncomeMinor, false)} $sym",
                                                countText = tr(isArabic, "$lifetimeIncomeCount عملية", "$lifetimeIncomeCount entries"),
                                                tag = "reset_tile_lifetime_income",
                                                onClick = { pendingPurge = PendingSettingsPurge.ResetLifetimeIncome }
                                            )
                                            ResetNumberGridTile(
                                                modifier = Modifier.weight(1f),
                                                icon = Icons.Outlined.TrendingDown,
                                                iconBg = ext.expenseContainer,
                                                iconTint = ext.expenseColor,
                                                title = tr(isArabic, "إجمالي المصاريف", "Total Expenses"),
                                                valueText = "${formatAmountMinor(totalExpenseMinor, false)} $sym",
                                                countText = tr(isArabic, "$totalExpenseCount عملية", "$totalExpenseCount entries"),
                                                tag = "reset_tile_expenses",
                                                onClick = { pendingPurge = PendingSettingsPurge.ResetExpenses }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Row 2: كل المبالغ المطلوب دفعها قريباً | كل المبالغ اللي اندفعت بالفعل
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            ResetNumberGridTile(
                                                modifier = Modifier.weight(1f),
                                                icon = Icons.Outlined.RequestQuote,
                                                iconBg = ext.warningAmber.copy(alpha = 0.18f),
                                                iconTint = ext.warningAmber,
                                                title = tr(isArabic, "كل المبالغ المطلوب دفعها قريباً", "Active Debts"),
                                                valueText = "${formatAmountMinor(unpaidDebtMinor, false)} $sym",
                                                countText = tr(isArabic, "$unpaidDebtCount مديونية", "$unpaidDebtCount active"),
                                                tag = "reset_tile_unpaid_debts",
                                                onClick = { pendingPurge = PendingSettingsPurge.ResetUnpaidDebts }
                                            )
                                            ResetNumberGridTile(
                                                modifier = Modifier.weight(1f),
                                                icon = Icons.Outlined.Payments,
                                                iconBg = ext.incomeContainer,
                                                iconTint = ext.incomeColor,
                                                title = tr(isArabic, "كل المبالغ اللي اندفعت بالفعل", "Settled Payments"),
                                                valueText = "${formatAmountMinor(settledPaymentMinor, false)} $sym",
                                                countText = tr(isArabic, "$settledPaymentCount مدفوعة", "$settledPaymentCount paid"),
                                                tag = "reset_tile_settled_payments",
                                                onClick = { pendingPurge = PendingSettingsPurge.ResetSettledPayments }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Row 3: التزامات بمواعيد سداد | حد الميزانية الشهرية
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            ResetNumberGridTile(
                                                modifier = Modifier.weight(1f),
                                                icon = Icons.Outlined.EventNote,
                                                iconBg = ext.warningAmber.copy(alpha = 0.18f),
                                                iconTint = ext.warningAmber,
                                                title = tr(isArabic, "التزامات بمواعيد سداد", "Obligations Due"),
                                                valueText = "${formatAmountMinor(obligationsTotalMinor, false)} $sym",
                                                countText = tr(isArabic, "$obligationsCount التزام", "$obligationsCount items"),
                                                tag = "reset_tile_obligations",
                                                onClick = { pendingPurge = PendingSettingsPurge.ResetObligations }
                                            )
                                            ResetNumberGridTile(
                                                modifier = Modifier.weight(1f),
                                                icon = Icons.Outlined.AccountBalanceWallet,
                                                iconBg = MaterialTheme.colorScheme.primaryContainer,
                                                iconTint = MaterialTheme.colorScheme.primary,
                                                title = tr(isArabic, "حد الميزانية الشهرية", "Monthly Budget Limit"),
                                                valueText = "${formatAmountMinor(settings.monthlyBudgetLimitMinor, false)} $sym",
                                                countText = tr(isArabic, "تصفير الحد فقط", "Zero limit only"),
                                                tag = "reset_tile_budget_limit",
                                                onClick = { pendingPurge = PendingSettingsPurge.ResetMonthlyBudget }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Full-width Master Reset Tile: تصفير جميع أرقام التطبيق بالكامل
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .testTag("reset_tile_all_numbers")
                                                .clickable { pendingPurge = PendingSettingsPurge.ResetAllAppNumbers },
                                            shape = RoundedCornerShape(14.dp),
                                            color = ext.expenseContainer,
                                            border = BorderStroke(1.2.dp, ext.expenseColor.copy(alpha = 0.55f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(ext.expenseColor),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.DeleteForever,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Column {
                                                        Text(
                                                            text = tr(isArabic, "تصفير جميع أرقام التطبيق بالكامل", "Reset All App Numbers to 0.00"),
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = ext.expenseColor
                                                        )
                                                        Text(
                                                            text = tr(
                                                                isArabic,
                                                                "تصفير الإيرادات + المصاريف + المديونيات + الالتزامات + الميزانية",
                                                                "Zeros income, expenses, debts, obligations & budget"
                                                            ),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = ext.expenseColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                // Dedicated «تصفير الأرقام» Icon Card inside Delete Section
                                SereneCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .testTag("settings_open_reset_numbers_btn")
                                        .clickable { showResetNumbersGrid = true },
                                    borderColor = ext.expenseColor.copy(alpha = 0.4f),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(13.dp))
                                                    .background(ext.expenseContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.RestartAlt,
                                                    contentDescription = tr(isArabic, "تصفير الأرقام", "Reset Numbers"),
                                                    tint = ext.expenseColor,
                                                    modifier = Modifier.size(23.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tr(isArabic, "تصفير الأرقام السريع (شبكة الأيقونات)", "Quick Reset Numbers Grid"),
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = ext.expenseColor
                                                )
                                                Text(
                                                    text = tr(
                                                        isArabic,
                                                        "أيقونات لكل أرقام التطبيق لتصفير أي رقم على حدة أو الكل",
                                                        "Grid of all app numbers to reset individually or all at once"
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = ext.expenseColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // NEW: Search & Filter Hub for Deleting/Resetting across ALL App Pages & Branches
                                SereneCard(contentPadding = PaddingValues(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(ext.expenseContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Search,
                                                contentDescription = null,
                                                tint = ext.expenseColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tr(isArabic, "محرك بحث وفلترة الحذف والتصفير الشامل", "Universal Delete & Reset Search Engine"),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = tr(
                                                    isArabic,
                                                    "ابحث عن أي صفحة أو فرع أو تصنيف أو عملية في التطبيق لمسحها أو تصفيرها فوراً",
                                                    "Search any app page, branch, category, account, or transaction to delete or zero out"
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = deleteSearchQuery,
                                        onValueChange = { deleteSearchQuery = it },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_delete_search_input"),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Search,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        trailingIcon = {
                                            if (deleteSearchQuery.isNotBlank()) {
                                                IconButton(onClick = { deleteSearchQuery = "" }) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Clear,
                                                        contentDescription = tr(isArabic, "مسح البحث", "Clear search")
                                                    )
                                                }
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                text = tr(
                                                    isArabic,
                                                    "ابحث باسم الصفحة، الفرع، المبلغ، التصنيف، أو اسم العملية...",
                                                    "Search page, branch, category, account, or entry..."
                                                ),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    val pageFilters = remember(isArabic) {
                                        listOf(
                                            "all" to tr(isArabic, "كل صفحات وفروع التطبيق", "All Pages & Branches"),
                                            "home" to tr(isArabic, "الرئيسية والمحفظة", "Home & Wallet"),
                                            "plan" to tr(isArabic, "المخطط المالي وفروعه", "Money Plan & Tabs"),
                                            "income" to tr(isArabic, "تفاصيل الدخل وفروعها", "Income Details & Branches"),
                                            "expense" to tr(isArabic, "تفاصيل المصاريف وفروعها", "Expense Details & Branches"),
                                            "obligations" to tr(isArabic, "التزامات بمواعيد سداد", "Scheduled Obligations"),
                                            "log" to tr(isArabic, "السجل والتصنيفات والحسابات", "Log, Categories & Accounts"),
                                            "items" to tr(isArabic, "عمليات والتزامات فردية", "Individual Entries")
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pageFilters.forEach { (key, label) ->
                                            val selected = deletePageFilter == key
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .testTag("settings_delete_filter_$key")
                                                    .clickable { deletePageFilter = key },
                                                shape = RoundedCornerShape(999.dp),
                                                color = if (selected) ext.expenseColor else ext.cardSubtleBackground,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (selected) ext.expenseColor else ext.cardBorder.copy(alpha = 0.6f)
                                                )
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                val q = deleteSearchQuery.trim().lowercase(Locale.ROOT)
                                fun matchesDeleteQuery(vararg texts: String): Boolean {
                                    if (q.isEmpty()) return true
                                    return texts.any { it.lowercase(Locale.ROOT).contains(q) }
                                }

                                // 1. الصفحة الرئيسية والمحفظة
                                val showHomeBranch = (deletePageFilter == "all" || deletePageFilter == "home") &&
                                    matchesDeleteQuery(
                                        "الصفحة الرئيسية", "المحفظة", "الباقي في محفظتي بعد كل الفلوس اللي اندفعت بالفعل",
                                        "تصفير جميع أرقام التطبيق بالكامل", "حد الميزانية الشهرية", "home", "wallet", "budget"
                                    )
                                if (showHomeBranch) {
                                    SereneCard(contentPadding = PaddingValues(0.dp)) {
                                        DeleteBranchHeader(
                                            icon = Icons.Outlined.AccountBalanceWallet,
                                            tint = MaterialTheme.colorScheme.primary,
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            title = tr(isArabic, "١. الصفحة الرئيسية والمحفظة", "1. Home Screen & Wallet"),
                                            subtitle = tr(isArabic, "الباقي في محفظتي وحد الميزانية الشهرية وتصفير التطبيق", "Wallet balance, monthly budget limit & master reset")
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.AccountBalanceWallet,
                                            iconBg = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.primary,
                                            title = tr(isArabic, "تصفير حد الميزانية الشهرية", "Reset Monthly Budget Limit"),
                                            subtitle = tr(
                                                isArabic,
                                                "الحد الحالي: ${formatAmountMinor(settings.monthlyBudgetLimitMinor, false)} $sym — إعادة الحد إلى 0.00",
                                                "Current limit: ${formatAmountMinor(settings.monthlyBudgetLimitMinor, false)} $sym"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير", "Zero"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetMonthlyBudget }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.DeleteForever,
                                            iconBg = ext.expenseContainer,
                                            iconTint = ext.expenseColor,
                                            title = tr(isArabic, "تصفير الباقي في محفظتي وجميع أرقام التطبيق", "Reset Wallet & All App Numbers"),
                                            subtitle = tr(
                                                isArabic,
                                                "تصفير كافة الأرصدة والدخل والمصاريف والمديونيات والالتزامات (${transactions.size} عملية و$obligationsCount التزام)",
                                                "Zero all wallet balances, transactions, debts, obligations & budget"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير شامل", "Reset All"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetAllAppNumbers }
                                        )
                                    }
                                }

                                // 2. المخطط المالي وفروعه
                                val showPlanBranch = (deletePageFilter == "all" || deletePageFilter == "plan") &&
                                    matchesDeleteQuery(
                                        "المخطط", "المخطط المالي", "الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج",
                                        "كل المبالغ المطلوب دفعها قريباً", "فلوسك الموجودة عليك المفروض تدفعها",
                                        "كل المبالغ اللي اندفعت بالفعل", "الفلوس الباقية", "plan", "lifetime"
                                    )
                                if (showPlanBranch) {
                                    SereneCard(contentPadding = PaddingValues(0.dp)) {
                                        DeleteBranchHeader(
                                            icon = Icons.Outlined.AutoAwesome,
                                            tint = ext.incomeColor,
                                            containerColor = ext.incomeContainer,
                                            title = tr(isArabic, "٢. صفحة المخطط المالي وفروعها", "2. Money Flow Plan & Sub-Tabs"),
                                            subtitle = tr(isArabic, "تصفير أي تبويبة من تبويبات المخطط المالي الأربعة", "Reset any of the 4 Money Flow Plan tabs")
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.Savings,
                                            iconBg = ext.incomeContainer,
                                            iconTint = ext.incomeColor,
                                            title = tr(isArabic, "تصفير «الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج»", "Reset Lifetime Added Money"),
                                            subtitle = tr(
                                                isArabic,
                                                "الإجمالي الحالي: ${formatAmountMinor(lifetimeIncomeMinor, false)} $sym ($lifetimeIncomeCount عملية)",
                                                "Current total: ${formatAmountMinor(lifetimeIncomeMinor, false)} $sym ($lifetimeIncomeCount entries)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير", "Reset"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetLifetimeIncome }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.RequestQuote,
                                            iconBg = ext.warningAmber.copy(alpha = 0.18f),
                                            iconTint = ext.warningAmber,
                                            title = tr(isArabic, "تصفير «كل المبالغ المطلوب دفعها قريباً» (فلوسك الموجودة عليك)", "Reset All Amounts Due Soon"),
                                            subtitle = tr(
                                                isArabic,
                                                "المبالغ غير المسددة: ${formatAmountMinor(unpaidDebtMinor, false)} $sym ($unpaidDebtCount عملية) + الالتزامات القادمة ($pendingObligationsCount)",
                                                "Unpaid debts: ${formatAmountMinor(unpaidDebtMinor, false)} $sym ($unpaidDebtCount items)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير", "Reset"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetUnpaidDebts }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.Payments,
                                            iconBg = ext.incomeContainer,
                                            iconTint = ext.incomeColor,
                                            title = tr(isArabic, "تصفير «كل المبالغ اللي اندفعت بالفعل»", "Reset All Amounts Already Paid"),
                                            subtitle = tr(
                                                isArabic,
                                                "المديونيات المسددة: ${formatAmountMinor(settledPaymentMinor, false)} $sym ($settledPaymentCount عملية مسددة)",
                                                "Settled debt payments: ${formatAmountMinor(settledPaymentMinor, false)} $sym ($settledPaymentCount items)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير", "Reset"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetSettledPayments }
                                        )
                                    }
                                }

                                // 3. صفحة تفاصيل الدخل وفروعها
                                val showIncomeBranch = (deletePageFilter == "all" || deletePageFilter == "income") &&
                                    matchesDeleteQuery(
                                        "تفاصيل الدخل", "إجمالي الدخل", "فلوس ملكك", "مكتسب فقط",
                                        "فلوس مطلوب سدادها قريباً", "فلوس متسددتش لسه", "فلوس اندفعت خلاص",
                                        "مديونية إيراد", "income"
                                    )
                                if (showIncomeBranch) {
                                    SereneCard(contentPadding = PaddingValues(0.dp)) {
                                        DeleteBranchHeader(
                                            icon = Icons.Outlined.Savings,
                                            tint = ext.incomeColor,
                                            containerColor = ext.incomeContainer,
                                            title = tr(isArabic, "٣. صفحة تفاصيل الدخل وفروعها", "3. Income Details Page & Branches"),
                                            subtitle = tr(isArabic, "فلوس ملكك • فلوس مطلوب سدادها قريباً • فلوس متسددتش لسه • فلوس اندفعت خلاص", "Your money • Due soon • Unpaid • Already paid")
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.Check,
                                            iconBg = ext.incomeContainer,
                                            iconTint = ext.incomeColor,
                                            title = tr(isArabic, "تصفير فرع «فلوس ملكك» فقط", "Reset 'Your Own Money' Branch Only"),
                                            subtitle = tr(
                                                isArabic,
                                                "الدخل المكتسب غير المرتبط بمديونية: ${formatAmountMinor(earnedIncomeOnlyMinor, false)} $sym ($earnedIncomeOnlyCount عملية)",
                                                "Earned non-debt income: ${formatAmountMinor(earnedIncomeOnlyMinor, false)} $sym ($earnedIncomeOnlyCount items)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير الفرع", "Reset"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetEarnedIncomeOnly }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.RequestQuote,
                                            iconBg = ext.warningAmber.copy(alpha = 0.18f),
                                            iconTint = ext.warningAmber,
                                            title = tr(isArabic, "تصفير فرع «فلوس مطلوب سدادها قريباً / فلوس متسددتش لسه»", "Reset Unpaid Income Debts Branch"),
                                            subtitle = tr(
                                                isArabic,
                                                "فلوس دخل مطلوب سدادها ولم تسدد بعد: ${formatAmountMinor(unpaidIncomeDebtMinor, false)} $sym ($unpaidIncomeDebtCount عملية)",
                                                "Unpaid income debts: ${formatAmountMinor(unpaidIncomeDebtMinor, false)} $sym ($unpaidIncomeDebtCount items)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير الفرع", "Reset"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetUnpaidIncomeDebts }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.Payments,
                                            iconBg = ext.incomeContainer,
                                            iconTint = ext.incomeColor,
                                            title = tr(isArabic, "تصفير فرع «فلوس اندفعت خلاص» (في صفحة الدخل)", "Reset Paid Income Debts Branch"),
                                            subtitle = tr(
                                                isArabic,
                                                "مديونيات الدخل التي اندفعت خلاص: ${formatAmountMinor(settledIncomeDebtMinor, false)} $sym ($settledIncomeDebtCount عملية)",
                                                "Settled income debts: ${formatAmountMinor(settledIncomeDebtMinor, false)} $sym ($settledIncomeDebtCount items)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير الفرع", "Reset"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetSettledIncomeDebts }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.DeleteSweep,
                                            iconBg = ext.expenseContainer,
                                            iconTint = ext.expenseColor,
                                            title = tr(isArabic, "تصفير صفحة تفاصيل الدخل بالكامل (كل الفروع)", "Reset Entire Income Details Page"),
                                            subtitle = tr(
                                                isArabic,
                                                "مسح كافة فروع الدخل (${formatAmountMinor(lifetimeIncomeMinor, false)} $sym • $lifetimeIncomeCount عملية)",
                                                "Reset all income branches ($lifetimeIncomeCount entries)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير الكل", "Reset All"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetLifetimeIncome }
                                        )
                                    }
                                }

                                // 4. صفحة تفاصيل المصاريف وفروعها
                                val showExpenseBranch = (deletePageFilter == "all" || deletePageFilter == "expense") &&
                                    matchesDeleteQuery(
                                        "تفاصيل المصاريف", "إجمالي المصاريف", "اندفعت خلاص", "مدفوع فقط",
                                        "مندفعتش لسه", "فلوس اندفعت خلاص", "مديونية مصروف", "expense"
                                    )
                                if (showExpenseBranch) {
                                    SereneCard(contentPadding = PaddingValues(0.dp)) {
                                        DeleteBranchHeader(
                                            icon = Icons.Outlined.TrendingDown,
                                            tint = ext.expenseColor,
                                            containerColor = ext.expenseContainer,
                                            title = tr(isArabic, "٤. صفحة تفاصيل المصاريف وفروعها", "4. Expense Details Page & Branches"),
                                            subtitle = tr(isArabic, "اندفعت خلاص • مندفعتش لسه • فلوس اندفعت خلاص", "Paid expenses • Unpaid expense debts • Settled expense debts")
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.TrendingDown,
                                            iconBg = ext.expenseContainer,
                                            iconTint = ext.expenseColor,
                                            title = tr(isArabic, "تصفير فرع «اندفعت خلاص» (مصاريف مدفوعة مباشرة)", "Reset 'Already Paid' Direct Expenses Branch"),
                                            subtitle = tr(
                                                isArabic,
                                                "المصاريف المدفوعة مباشرة: ${formatAmountMinor(paidExpenseOnlyMinor, false)} $sym ($paidExpenseOnlyCount عملية)",
                                                "Direct paid expenses: ${formatAmountMinor(paidExpenseOnlyMinor, false)} $sym ($paidExpenseOnlyCount items)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير الفرع", "Reset"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetPaidExpensesOnly }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.RequestQuote,
                                            iconBg = ext.warningAmber.copy(alpha = 0.18f),
                                            iconTint = ext.warningAmber,
                                            title = tr(isArabic, "تصفير فرع «مندفعتش لسه» (في صفحة المصاريف)", "Reset 'Not Paid Yet' Expense Debts Branch"),
                                            subtitle = tr(
                                                isArabic,
                                                "مصاريف مسجلة كمديونية لم تدفع بعد: ${formatAmountMinor(unpaidExpenseDebtMinor, false)} $sym ($unpaidExpenseDebtCount عملية)",
                                                "Unpaid expense debts: ${formatAmountMinor(unpaidExpenseDebtMinor, false)} $sym ($unpaidExpenseDebtCount items)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير الفرع", "Reset"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetUnpaidExpenseDebts }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.Payments,
                                            iconBg = ext.incomeContainer,
                                            iconTint = ext.incomeColor,
                                            title = tr(isArabic, "تصفير فرع «فلوس اندفعت خلاص» (مديونيات مصاريف مسددة)", "Reset 'Money Already Paid' Settled Expense Debts"),
                                            subtitle = tr(
                                                isArabic,
                                                "مديونيات المصاريف المسددة: ${formatAmountMinor(settledExpenseDebtMinor, false)} $sym ($settledExpenseDebtCount عملية)",
                                                "Settled expense debts: ${formatAmountMinor(settledExpenseDebtMinor, false)} $sym ($settledExpenseDebtCount items)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير الفرع", "Reset"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetSettledExpenseDebts }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.DeleteSweep,
                                            iconBg = ext.expenseContainer,
                                            iconTint = ext.expenseColor,
                                            title = tr(isArabic, "تصفير صفحة تفاصيل المصاريف بالكامل (كل الفروع)", "Reset Entire Expense Details Page"),
                                            subtitle = tr(
                                                isArabic,
                                                "مسح كافة فروع المصاريف (${formatAmountMinor(totalExpenseMinor, false)} $sym • $totalExpenseCount عملية)",
                                                "Reset all expense branches ($totalExpenseCount entries)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير الكل", "Reset All"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetExpenses }
                                        )
                                    }
                                }

                                // 5. صفحة التزامات بمواعيد سداد وفروعها
                                val showObligationsBranch = (deletePageFilter == "all" || deletePageFilter == "obligations") &&
                                    matchesDeleteQuery(
                                        "التزامات بمواعيد سداد", "التزامات", "مواعيد سداد", "قادمة", "لسه هتندفع",
                                        "مدفوعة", "obligations", "bills"
                                    )
                                if (showObligationsBranch) {
                                    SereneCard(contentPadding = PaddingValues(0.dp)) {
                                        DeleteBranchHeader(
                                            icon = Icons.Outlined.EventNote,
                                            tint = ext.warningAmber,
                                            containerColor = ext.warningAmber.copy(alpha = 0.18f),
                                            title = tr(isArabic, "٥. صفحة التزامات بمواعيد سداد وفروعها", "5. Scheduled Obligations & Branches"),
                                            subtitle = tr(isArabic, "التزامات قادمة لسه هتندفع • التزامات اندفعت خلاص • كل الالتزامات", "Upcoming obligations • Paid obligations • All obligations")
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.NotificationsActive,
                                            iconBg = ext.warningAmber.copy(alpha = 0.18f),
                                            iconTint = ext.warningAmber,
                                            title = tr(isArabic, "تصفير الالتزامات بمواعيد سداد القادمة (لسه هتندفع)", "Reset Upcoming Pending Obligations"),
                                            subtitle = tr(
                                                isArabic,
                                                "الالتزامات المنتظرة: ${formatAmountMinor(pendingObligationsMinor, false)} $sym ($pendingObligationsCount التزام قادم)",
                                                "Pending obligations: ${formatAmountMinor(pendingObligationsMinor, false)} $sym ($pendingObligationsCount items)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير الفرع", "Reset"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetPendingObligations }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.Check,
                                            iconBg = ext.incomeContainer,
                                            iconTint = ext.incomeColor,
                                            title = tr(isArabic, "تصفير الالتزامات المسددة (اللي اندفعت خلاص)", "Reset Paid Obligations Branch"),
                                            subtitle = tr(
                                                isArabic,
                                                "الالتزامات المسددة: ${formatAmountMinor(paidObligationsMinor, false)} $sym ($paidObligationsCount التزام مسدد)",
                                                "Paid obligations: ${formatAmountMinor(paidObligationsMinor, false)} $sym ($paidObligationsCount items)"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير الفرع", "Reset"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetPaidObligations }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        SettingsActionRow(
                                            icon = Icons.Outlined.EventNote,
                                            iconBg = ext.expenseContainer,
                                            iconTint = ext.expenseColor,
                                            title = tr(isArabic, "تصفير جميع الالتزامات بمواعيد سداد بالكامل", "Reset All Scheduled Obligations"),
                                            subtitle = tr(
                                                isArabic,
                                                "مسح كافة الالتزامات وإلغاء منبهاتها (${formatAmountMinor(obligationsTotalMinor, false)} $sym • $obligationsCount التزام)",
                                                "Clear all $obligationsCount obligations and cancel reminders"
                                            ),
                                            trailingLabel = tr(isArabic, "تصفير الكل", "Reset All"),
                                            onClick = { pendingPurge = PendingSettingsPurge.ResetObligations }
                                        )
                                    }
                                }

                                // 6. سجل العمليات والفروع (الفترات الزمنية / التصنيفات / الحسابات وطرق الدفع)
                                val categoryCounts = remember(transactions) {
                                    transactions.groupBy { it.category }.mapValues { it.value.size }
                                }
                                val sourceCounts = remember(transactions) {
                                    transactions.groupBy { it.source }.mapValues { it.value.size }
                                }
                                val showLogBranch = (deletePageFilter == "all" || deletePageFilter == "log") &&
                                    (matchesDeleteQuery(
                                        "سجل العمليات", "السجل", "التصنيفات", "الحسابات", "طرق الدفع", "الأسبوع الحالي",
                                        "الشهر الحالي", "شهور", "سنة", "وضع الحذف السريع", "log", "category", "account", "month", "week"
                                    ) || categoryCounts.keys.any { matchesDeleteQuery(it) } || sourceCounts.keys.any { matchesDeleteQuery(it) })

                                if (showLogBranch) {
                                    // Quick Deletion Mode Toggle Card
                                    SereneCard(contentPadding = PaddingValues(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(ext.incomeContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Bolt,
                                                        contentDescription = null,
                                                        tint = ext.incomeColor,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Column {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = tr(isArabic, "وضع الحذف السريع لعنصر فردي", "Quick Single-Item Delete Mode"),
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(ext.incomeContainer)
                                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = tr(isArabic, "فوري", "Fast"),
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = ext.incomeColor
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = tr(
                                                            isArabic,
                                                            "تجاوز نافذة التأكيد عند حذف عملية مفردة",
                                                            "Skip confirmation dialog when deleting a single entry"
                                                        ),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Switch(
                                                checked = settings.quickDeleteMode,
                                                onCheckedChange = onSetQuickDelete,
                                                colors = SwitchDefaults.colors(checkedTrackColor = ext.incomeColor)
                                            )
                                        }
                                    }

                                    // Grouped Deletion Actions Card (Time Periods, Categories, Accounts/Sources, Wipe All)
                                    SereneCard(contentPadding = PaddingValues(0.dp)) {
                                        DeleteBranchHeader(
                                            icon = Icons.Outlined.HistoryToggleOff,
                                            tint = MaterialTheme.colorScheme.primary,
                                            containerColor = ext.cardContainer,
                                            title = tr(isArabic, "٦. سجل العمليات والفروع (الفترات • التصنيفات • الحسابات)", "6. Operations Log, Periods, Categories & Accounts"),
                                            subtitle = tr(isArabic, "حذف مخصص حسب الفترة الزمنية أو التصنيف أو المحفظة/الحساب", "Targeted purge by time period, category, or wallet/account")
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))

                                        // Current Week
                                        SettingsActionRow(
                                            icon = Icons.Outlined.CalendarMonth,
                                            iconBg = ext.cardContainer,
                                            iconTint = MaterialTheme.colorScheme.primary,
                                            title = tr(isArabic, "حذف عمليات الأسبوع الحالي", "Delete Current Week"),
                                            subtitle = tr(
                                                isArabic,
                                                "مسح الحركات المسجلة خلال آخر 7 أيام فقط",
                                                "Remove entries recorded in the last 7 days"
                                            ),
                                            trailingLabel = tr(isArabic, "حذف الأسبوع", "Delete"),
                                            onClick = { pendingPurge = PendingSettingsPurge.CurrentWeek }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))

                                        // Current Month
                                        SettingsActionRow(
                                            icon = Icons.Outlined.CalendarMonth,
                                            iconBg = ext.incomeContainer,
                                            iconTint = ext.incomeColor,
                                            title = tr(isArabic, "حذف عمليات الشهر الحالي", "Delete Current Month"),
                                            subtitle = tr(
                                                isArabic,
                                                "إلغاء قيود الشهر الجاري دون المساس بالأشهر السابقة",
                                                "Remove this month's entries while keeping history"
                                            ),
                                            trailingLabel = tr(isArabic, "حذف الشهر", "Delete"),
                                            onClick = { pendingPurge = PendingSettingsPurge.CurrentMonth }
                                        )
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))

                                        // Past Periods (3 / 6 / 12 months)
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(ext.cardContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.HistoryToggleOff,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = tr(isArabic, "حذف العمليات الأقدم حسب الفترة", "Delete Older Entries by Period"),
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    Text(
                                                        text = tr(
                                                            isArabic,
                                                            "تنظيف الأرشيف وتخفيف حجم قاعدة البيانات",
                                                            "Clean older history to keep database lean"
                                                        ),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf(
                                                    3 to tr(isArabic, "أقدم من ٣ شهور", "> 3 Months"),
                                                    6 to tr(isArabic, "أقدم من ٦ شهور", "> 6 Months"),
                                                    12 to tr(isArabic, "أقدم من سنة", "> 1 Year")
                                                ).forEach { (months, lbl) ->
                                                    Surface(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                pendingPurge = PendingSettingsPurge.OlderThan(months)
                                                            },
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = ext.cardSubtleBackground,
                                                        border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.5f))
                                                    ) {
                                                        Text(
                                                            text = lbl,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.primary,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))

                                        // Direct Category Chips + Modal Picker
                                        Column(modifier = Modifier.padding(14.dp)) {
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
                                                            .size(38.dp)
                                                            .clip(RoundedCornerShape(11.dp))
                                                            .background(ext.expenseContainer.copy(alpha = 0.7f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Outlined.Category,
                                                            contentDescription = null,
                                                            tint = ext.chartRose,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Column {
                                                        Text(
                                                            text = tr(isArabic, "حذف وتصفير حسب التصنيف الفرعي", "Delete by Category Branch"),
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                        )
                                                        Text(
                                                            text = tr(isArabic, "اضغط على أي تصنيف لتصفير وحذف عملياته فقط", "Tap any category chip to delete its entries"),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { showCategoryPurgeModal = true },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = ext.cardSubtleBackground
                                                ) {
                                                    Text(
                                                        text = tr(isArabic, "اختر التصنيف", "Select"),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                            if (categoryCounts.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    categoryCounts.entries
                                                        .filter { matchesDeleteQuery(it.key, "التصنيفات", "category") }
                                                        .forEach { (catName, count) ->
                                                            Surface(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(10.dp))
                                                                    .clickable { pendingPurge = PendingSettingsPurge.ByCategory(catName) },
                                                                shape = RoundedCornerShape(10.dp),
                                                                color = ext.cardSubtleBackground,
                                                                border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.55f))
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "$catName ($count)",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Delete,
                                                                        contentDescription = null,
                                                                        tint = ext.expenseColor,
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))

                                        // Direct Account / Payment Source Chips
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(11.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.AccountBalance,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = tr(isArabic, "حذف وتصفير حسب الحساب / طريقة الدفع", "Delete by Account / Payment Method"),
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    Text(
                                                        text = tr(isArabic, "تصفير حركات محفظة أو حساب معين دون المساس بباقي الحسابات", "Purge transactions belonging to a specific account or payment method"),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (sourceCounts.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    sourceCounts.entries
                                                        .filter { matchesDeleteQuery(it.key, "الحسابات", "طرق الدفع", "account") }
                                                        .forEach { (srcName, count) ->
                                                            Surface(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(10.dp))
                                                                    .clickable { pendingPurge = PendingSettingsPurge.BySource(srcName) },
                                                                shape = RoundedCornerShape(10.dp),
                                                                color = ext.cardSubtleBackground,
                                                                border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.55f))
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "$srcName ($count)",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Delete,
                                                                        contentDescription = null,
                                                                        tint = ext.expenseColor,
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))

                                        // Wipe All (Double Confirmation!)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { pendingPurge = PendingSettingsPurge.WipeAll }
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(ext.expenseContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.DeleteForever,
                                                        contentDescription = null,
                                                        tint = ext.expenseColor,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = tr(isArabic, "تصفير وحذف جميع العمليات", "Wipe All & Reset"),
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = ext.expenseColor
                                                    )
                                                    Text(
                                                        text = tr(
                                                            isArabic,
                                                            "تصفير شامل لقاعدة البيانات (${transactions.size} عملية)",
                                                            "Reset entire ledger (${transactions.size} entries)"
                                                        ),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(ext.expenseColor)
                                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                            ) {
                                                Text(
                                                    text = tr(isArabic, "مسح شامل", "Wipe All"),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                // 7. نتائج البحث والفلترة في العمليات والالتزامات الفردية
                                val showIndividualItems = deletePageFilter == "items" || q.isNotEmpty()
                                if (showIndividualItems) {
                                    val matchingTransactions = remember(transactions, q) {
                                        transactions.filter { tx ->
                                            if (q.isEmpty()) true
                                            else {
                                                tx.category.lowercase(Locale.ROOT).contains(q) ||
                                                    tx.note.lowercase(Locale.ROOT).contains(q) ||
                                                    tx.source.lowercase(Locale.ROOT).contains(q) ||
                                                    tx.itemName.lowercase(Locale.ROOT).contains(q) ||
                                                    tx.debtParty.lowercase(Locale.ROOT).contains(q) ||
                                                    formatAmountMinor(tx.amountMinor, false).contains(q)
                                            }
                                        }.take(25)
                                    }
                                    val matchingObligations = remember(obligations, q) {
                                        obligations.filter { ob ->
                                            if (q.isEmpty()) true
                                            else {
                                                ob.reason.lowercase(Locale.ROOT).contains(q) ||
                                                    ob.party.lowercase(Locale.ROOT).contains(q) ||
                                                    ob.note.lowercase(Locale.ROOT).contains(q) ||
                                                    formatAmountMinor(ob.amountMinor, false).contains(q)
                                            }
                                        }.take(20)
                                    }

                                    SereneCard(contentPadding = PaddingValues(14.dp)) {
                                        Text(
                                            text = tr(
                                                isArabic,
                                                "٧. حذف عمليات أو التزامات فردية محددة (${matchingTransactions.size + matchingObligations.size})",
                                                "7. Delete Specific Individual Entries (${matchingTransactions.size + matchingObligations.size})"
                                            ),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = tr(
                                                isArabic,
                                                "نتائج البحث المباشر في كل العمليات والالتزامات المسجلة بالتطبيق",
                                                "Direct search results across all recorded transactions and obligations"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        if (matchingTransactions.isEmpty() && matchingObligations.isEmpty()) {
                                            Text(
                                                text = tr(isArabic, "لا توجد عمليات أو التزامات مطابقة لبحثك.", "No matching entries found."),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                matchingTransactions.forEach { tx ->
                                                    val isInc = tx.type == TransactionType.INCOME
                                                    val branchTag = when {
                                                        isInc && !tx.isDebt -> tr(isArabic, "تفاصيل الدخل • فلوس ملكك", "Income • Owned")
                                                        isInc && tx.isDebt && !tx.isDebtSettled -> tr(isArabic, "تفاصيل الدخل • فلوس مطلوب سدادها قريباً / متسددتش لسه", "Income • Unpaid Debt")
                                                        isInc && tx.isDebt && tx.isDebtSettled -> tr(isArabic, "تفاصيل الدخل • فلوس اندفعت خلاص", "Income • Settled")
                                                        !isInc && !tx.isDebt -> tr(isArabic, "تفاصيل المصاريف • اندفعت خلاص", "Expense • Paid")
                                                        !isInc && tx.isDebt && !tx.isDebtSettled -> tr(isArabic, "تفاصيل المصاريف • مندفعتش لسه", "Expense • Unpaid Debt")
                                                        else -> tr(isArabic, "تفاصيل المصاريف • فلوس اندفعت خلاص", "Expense • Settled")
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(11.dp),
                                                        color = ext.cardSubtleBackground,
                                                        border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.55f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = buildString {
                                                                        append(tx.category)
                                                                        if (tx.itemName.isNotBlank()) append(" • ${tx.itemName}")
                                                                        if (tx.note.isNotBlank()) append(" • ${tx.note}")
                                                                        if (tx.debtParty.isNotBlank()) append(" (${tx.debtParty})")
                                                                    },
                                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = "$branchTag • ${tx.source}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Text(
                                                                    text = "${formatAmountMinor(tx.amountMinor, false)} ${currencySymbol(tx.currency, isArabic)}",
                                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                                    color = if (isInc) ext.incomeColor else ext.expenseColor
                                                                )
                                                                Surface(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .clickable {
                                                                            if (settings.quickDeleteMode && !settings.hasAnyLockMethodConfigured()) {
                                                                                onDeleteSingleTransaction(tx.id)
                                                                            } else {
                                                                                pendingPurge = PendingSettingsPurge.SingleTransaction(tx)
                                                                            }
                                                                        },
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    color = ext.expenseContainer
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Delete,
                                                                        contentDescription = tr(isArabic, "حذف العملية", "Delete entry"),
                                                                        tint = ext.expenseColor,
                                                                        modifier = Modifier
                                                                            .padding(6.dp)
                                                                            .size(16.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                matchingObligations.forEach { ob ->
                                                    val obBranch = if (ob.isPaid) {
                                                        tr(isArabic, "التزامات بمواعيد سداد • اندفعت خلاص", "Obligation • Paid")
                                                    } else {
                                                        tr(isArabic, "التزامات بمواعيد سداد • لسه هتندفع", "Obligation • Upcoming")
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(11.dp),
                                                        color = ext.cardSubtleBackground,
                                                        border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.55f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = buildString {
                                                                        append(ob.reason)
                                                                        if (ob.party.isNotBlank()) append(" (${ob.party})")
                                                                    },
                                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = obBranch,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Text(
                                                                    text = "${formatAmountMinor(ob.amountMinor, false)} ${currencySymbol(ob.currency, isArabic)}",
                                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                                    color = ext.warningAmber
                                                                )
                                                                Surface(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .clickable {
                                                                            if (settings.quickDeleteMode && !settings.hasAnyLockMethodConfigured()) {
                                                                                onDeleteSingleObligation(ob.id)
                                                                            } else {
                                                                                pendingPurge = PendingSettingsPurge.SingleObligation(ob)
                                                                            }
                                                                        },
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    color = ext.expenseContainer
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Delete,
                                                                        contentDescription = tr(isArabic, "حذف الالتزام", "Delete obligation"),
                                                                        tint = ext.expenseColor,
                                                                        modifier = Modifier
                                                                            .padding(6.dp)
                                                                            .size(16.dp)
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
                                }
                            } else {
                                // SECURITY SUB-TAB (Real Biometric/Device Lock, Custom 3x3 Pattern, PIN, Launch Lock, Payment Confirm)
                                SereneCard(contentPadding = PaddingValues(0.dp)) {
                                    // 1. Fingerprint / Device Pattern & Lock Toggle
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(ext.heroCardBackground),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Fingerprint,
                                                    contentDescription = null,
                                                    tint = Color(0xFF82F5C1),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = tr(isArabic, "البصمة ونمط / قفل الجهاز", "Fingerprint & Device Lock"),
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = tr(
                                                        isArabic,
                                                        "تشغيل بصمة الإصبع أو نمط شاشة الهاتف تلقائياً",
                                                        "Use phone fingerprint or system pattern/lock"
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = settings.biometricEnabled,
                                            onCheckedChange = { enable ->
                                                if (!enable) {
                                                    onSetBiometricProtection(false)
                                                } else if (!canUseDeviceBiometricOrCredential(context)) {
                                                    onEmitMessage(
                                                        tr(
                                                            isArabic,
                                                            "يرجى تفعيل البصمة أو قفل الشاشة من إعدادات الهاتف أولاً",
                                                            "Please enable fingerprint or screen lock in phone settings first"
                                                        )
                                                    )
                                                } else {
                                                    launchDeviceBiometricOrCredentialPrompt(
                                                        context = context,
                                                        title = tr(isArabic, "تفعيل حماية مصروفي", "Enable Masrofy Protection"),
                                                        subtitle = tr(
                                                            isArabic,
                                                            "أكّد بالبصمة أو نمط الجهاز لتفعيل الحماية",
                                                            "Verify fingerprint or device lock to enable"
                                                        ),
                                                        isArabic = isArabic,
                                                        onSuccess = { onSetBiometricProtection(true) },
                                                        onFailure = { msg -> onEmitMessage(msg) }
                                                    )
                                                }
                                            },
                                            modifier = Modifier.testTag("security_biometric_switch"),
                                            colors = SwitchDefaults.colors(checkedTrackColor = ext.incomeColor)
                                        )
                                    }

                                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))

                                    // 2. Custom 3x3 In-App Pattern Lock
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showPatternSetupDialog = true }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(ext.incomeContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Pattern,
                                                    contentDescription = null,
                                                    tint = ext.incomeColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = tr(isArabic, "النمط الخاص داخل التطبيق", "Custom App Pattern"),
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = if (settings.patternCode.isNotBlank()) {
                                                        tr(isArabic, "النمط محفوظ • اضغط لتعديله أو التبديل", "Pattern saved • tap to redraw")
                                                    } else {
                                                        tr(isArabic, "ارسم نمطاً مخصصاً لقفل التطبيق والعمليات", "Draw a custom 3x3 security pattern")
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = settings.patternEnabled,
                                            onCheckedChange = { enabled ->
                                                if (enabled && settings.patternCode.isBlank()) {
                                                    showPatternSetupDialog = true
                                                } else {
                                                    onSetPatternProtection(enabled, null)
                                                }
                                            },
                                            modifier = Modifier.testTag("security_pattern_switch"),
                                            colors = SwitchDefaults.colors(checkedTrackColor = ext.incomeColor)
                                        )
                                    }

                                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))

                                    // 3. Backup Security PIN
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showPinSetupDialog = true }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFFD8E3FB).copy(alpha = if (ext.isDark) 0.22f else 1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Pin,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = tr(isArabic, "رمز المرور الاحتياطي PIN", "Backup Security PIN"),
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = if (settings.pinCode.length >= 4) {
                                                        tr(isArabic, "الرمز مُعيّن — اضغط لتعديله", "PIN configured — tap to change")
                                                    } else {
                                                        tr(isArabic, "ضبط رمز أمان مالي من 4 إلى 6 أرقام", "Set a 4-6 digit security PIN")
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = settings.pinEnabled,
                                            onCheckedChange = { enabled ->
                                                if (enabled && settings.pinCode.length < 4) {
                                                    showPinSetupDialog = true
                                                } else {
                                                    onSetPinProtection(enabled, null)
                                                }
                                            },
                                            modifier = Modifier.testTag("security_pin_switch"),
                                            colors = SwitchDefaults.colors(checkedTrackColor = ext.incomeColor)
                                        )
                                    }
                                }

                                // Lock Behavior Controls (Before Splash + On Debt Payment)
                                SereneCard(contentPadding = PaddingValues(0.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(ext.cardContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Outlined.EnhancedEncryption,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = tr(isArabic, "قفل التطبيق عند الفتح (قبل البداية)", "Lock App Before Splash"),
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = tr(
                                                        isArabic,
                                                        "يطلب البصمة أو النمط أو PIN قبل ظهور شاشة البداية",
                                                        "Requires unlock before the intro video plays"
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = settings.lockOnLaunch || settings.autoLockSensitive,
                                            onCheckedChange = { on ->
                                                if (on && !settings.hasAnyLockMethodConfigured()) {
                                                    onEmitMessage(
                                                        tr(
                                                            isArabic,
                                                            "فعّل البصمة أو النمط أو PIN أولاً لتشغيل القفل عند الفتح",
                                                            "Enable Biometric, Pattern, or PIN first"
                                                        )
                                                    )
                                                } else {
                                                    onSetAutoLock(on)
                                                }
                                            },
                                            modifier = Modifier.testTag("security_launch_lock_switch"),
                                            colors = SwitchDefaults.colors(checkedTrackColor = ext.incomeColor)
                                        )
                                    }

                                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(ext.incomeContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Outlined.VerifiedUser,
                                                    contentDescription = null,
                                                    tint = ext.incomeColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = tr(isArabic, "تأكيد عمليات الدفع بالحماية", "Verify Debt Payments"),
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = tr(
                                                        isArabic,
                                                        "طلب البصمة أو النمط أو PIN عند الضغط على زر «دفع»",
                                                        "Require security confirmation when paying a debt"
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = settings.confirmPaymentWithSecurity,
                                            onCheckedChange = { on ->
                                                if (on && !settings.hasAnyLockMethodConfigured()) {
                                                    onEmitMessage(
                                                        tr(
                                                            isArabic,
                                                            "فعّل البصمة أو النمط أو PIN أولاً لتأكيد المدفوعات",
                                                            "Enable Biometric, Pattern, or PIN first"
                                                        )
                                                    )
                                                } else {
                                                    onSetConfirmPaymentWithSecurity(on)
                                                }
                                            },
                                            modifier = Modifier.testTag("security_payment_confirm_switch"),
                                            colors = SwitchDefaults.colors(checkedTrackColor = ext.incomeColor)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingsSection.CURRENCIES_ACCOUNTS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SereneCard(contentPadding = PaddingValues(16.dp)) {
                                Text(
                                    text = tr(isArabic, "العملة الافتراضية للتطبيق", "Default App Currency"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SupportedCurrencies.forEach { c ->
                                        val selected = settings.defaultCurrency == c.code
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .clickable { onSetDefaultCurrency(c.code) },
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (selected) ext.heroCardBackground else ext.cardSubtleBackground
                                        ) {
                                            Text(
                                                text = "${if (isArabic) c.symbolAr else c.symbolEn} • ${if (isArabic) c.nameAr else c.nameEn}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            SereneCard(contentPadding = PaddingValues(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(isArabic, "المحافظ والحسابات (${accounts.size})", "Accounts & Wallets (${accounts.size})"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    TextButton(onClick = { showAddAccountDialog = true }) {
                                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(tr(isArabic, "حساب جديد", "Add Account"))
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    accounts.forEach { acc ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(ext.cardSubtleBackground)
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = iconForKey(acc.iconKey),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                                Column {
                                                    Text(acc.name, style = MaterialTheme.typography.labelLarge)
                                                    Text(
                                                        acc.currency,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (accounts.size > 1) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = tr(isArabic, "حذف الحساب", "Delete Account"),
                                                    tint = ext.expenseColor,
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clickable { onDeleteAccount(acc) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettingsSection.BACKUP_DATA -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SereneCard(contentPadding = PaddingValues(16.dp)) {
                                Text(
                                    text = tr(isArabic, "النسخ الاحتياطي والاستعادة (بالإيميل أو الجهاز)", "Cloud & Device Backup / Restore"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tr(
                                        isArabic,
                                        "اضغط حفظ أو استعادة لفتح النافذة الجانبية واختيار إيميل جوجل والتسجيل به، أو حفظ النسخة على ملفات الجهاز.",
                                        "Tap Save or Restore to open the side drawer, choose your Google email to sign in, or save/restore via device files."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!signedInEmail.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = ext.incomeContainer.copy(alpha = 0.7f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.VerifiedUser,
                                                contentDescription = null,
                                                tint = ext.incomeColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = tr(
                                                    isArabic,
                                                    "الإيميل المتصل حالياً: $signedInEmail",
                                                    "Connected email: $signedInEmail"
                                                ),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ext.incomeColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { onOpenBackupSideDrawer(BackupDrawerMode.SAVE) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("backup_save_json_saf_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ext.heroCardBackground,
                                            contentColor = ext.heroCardOnBackground
                                        )
                                    ) {
                                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(tr(isArabic, "حفظ نسخة احتياطية (اختيار الإيميل)", "Save Backup (Choose Email)"))
                                    }

                                    Button(
                                        onClick = { onOpenBackupSideDrawer(BackupDrawerMode.RESTORE) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("backup_restore_json_saf_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ext.incomeContainer,
                                            contentColor = ext.incomeColor
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(0.dp)
                                    ) {
                                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(tr(isArabic, "استعادة نسخة احتياطية (اختيار الإيميل)", "Restore Backup (Choose Email)"))
                                    }

                                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))

                                    Text(
                                        text = tr(isArabic, "حفظ التقارير والملخصات بصيغ متعددة في ملفات الجهاز:", "Save Multi-Format Reports to Device Files:"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onLaunchSavePdfSaf,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("backup_save_pdf_saf_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ext.expenseContainer,
                                                contentColor = ext.expenseColor
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                            elevation = ButtonDefaults.buttonElevation(0.dp)
                                        ) {
                                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PDF", maxLines = 1)
                                        }
                                        Button(
                                            onClick = onLaunchSaveExcelSaf,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("backup_save_excel_saf_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ext.incomeContainer,
                                                contentColor = ext.incomeColor
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                            elevation = ButtonDefaults.buttonElevation(0.dp)
                                        ) {
                                            Icon(Icons.Outlined.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Excel", maxLines = 1)
                                        }
                                        Button(
                                            onClick = onLaunchSaveImageSaf,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("backup_save_image_saf_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ext.cardSubtleBackground,
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                            elevation = ButtonDefaults.buttonElevation(0.dp)
                                        ) {
                                            Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(tr(isArabic, "صورة", "PNG"), maxLines = 1)
                                        }
                                    }

                                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))

                                    Text(
                                        text = tr(isArabic, "تنزيل مباشر وفوري إلى مجلد التنزيلات (Downloads/Masrofy):", "Direct Download to Downloads/Masrofy:"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onShareJsonBackup,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ext.cardSubtleBackground,
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(0.dp)
                                        ) {
                                            Text(tr(isArabic, "تنزيل JSON", "Download JSON"), maxLines = 1)
                                        }
                                        Button(
                                            onClick = onExportExcelAll,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ext.cardSubtleBackground,
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(0.dp)
                                        ) {
                                            Icon(Icons.Outlined.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(tr(isArabic, "تنزيل Excel", "Download Excel"), maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettingsSection.APPEARANCE -> {
                        SereneCard(contentPadding = PaddingValues(16.dp)) {
                            Text(
                                text = tr(isArabic, "المظهر والسمة اللونية", "Theme & Appearance"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "light" to tr(isArabic, "فاتح", "Light"),
                                    "dark" to tr(isArabic, "داكن", "Dark"),
                                    "system" to tr(isArabic, "تلقائي", "System")
                                ).forEach { (mode, lbl) ->
                                    val chosen = settings.themeMode == mode
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSetThemeMode(mode) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (chosen) ext.heroCardBackground else ext.cardSubtleBackground
                                    ) {
                                        Text(
                                            text = lbl,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (chosen) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = tr(isArabic, "لغة التطبيق والاتجاه", "Language & Direction"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("ar" to "العربية (RTL)", "en" to "English (LTR)").forEach { (code, lbl) ->
                                    val chosen = settings.language == code
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSetLanguage(code) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (chosen) ext.incomeColor else ext.cardSubtleBackground
                                    ) {
                                        Text(
                                            text = lbl,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (chosen) Color.White else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingsSection.NOTIFICATIONS -> {
                        var budgetInput by remember(settings.monthlyBudgetLimitMinor) {
                            mutableStateOf(
                                if (settings.monthlyBudgetLimitMinor > 0) {
                                    String.format(Locale.US, "%.0f", settings.monthlyBudgetLimitMinor / 100.0)
                                } else ""
                            )
                        }
                        val dailyBaseTime = remember(settings.dailyReminderTime) { settings.primaryDailyReminderTime() }
                        val dailyCount = remember(settings.dailyReminderTime) { settings.dailyReminderCount() }
                        val dailySlots = remember(settings.dailyReminderTime) { settings.dailyReminderTimesList() }
                        SereneCard(contentPadding = PaddingValues(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr(isArabic, "تذكير يومي بتسجيل العمليات والمصاريف", "Daily Expense & Operations Reminder"),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = tr(
                                            isArabic,
                                            "$dailyCount مرات يومياً • الوقت الأساسي: ${com.example.util.formatTime12Hour(dailyBaseTime, true)}",
                                            "$dailyCount× daily • Main time: ${com.example.util.formatTime12Hour(dailyBaseTime, false)}"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.incomeColor
                                    )
                                }
                                Switch(
                                    checked = settings.dailyReminderEnabled,
                                    onCheckedChange = { enabled ->
                                        onSetReminders(enabled, settings.dailyReminderTime, settings.monthlyBudgetLimitMinor)
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = ext.incomeColor)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = tr(isArabic, "عدد مرات التذكير في اليوم:", "Reminders per day:"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (1..6).forEach { cnt ->
                                    val selected = dailyCount == cnt
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                val encoded = AppSettingsEntity.encodeDailyReminderTimeAndCount(dailyBaseTime, cnt)
                                                onSetReminders(true, encoded, settings.monthlyBudgetLimitMinor)
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) ext.heroCardBackground else ext.cardSubtleBackground,
                                        border = BorderStroke(1.dp, if (selected) ext.heroCardBackground else ext.cardBorder)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isArabic) "${cnt}×" else "${cnt}x",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            if (settings.dailyReminderEnabled && dailySlots.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr(isArabic, "مواعيد اليوم: ", "Today's times: ") +
                                        dailySlots.joinToString(" • ") { com.example.util.formatTime12Hour(it, isArabic) },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { showDailyReminderTimePicker = true }
                            ) {
                                Text(tr(isArabic, "تغيير وقت التذكير اليومي", "Change Daily Reminder Time"))
                            }
                            if (showDailyReminderTimePicker) {
                                FastTimePickerDialog(
                                    initialTime24 = dailyBaseTime,
                                    isArabic = isArabic,
                                    title = tr(isArabic, "اختر وقت التذكير اليومي", "Select Daily Reminder Time"),
                                    onConfirm = { picked ->
                                        val encoded = AppSettingsEntity.encodeDailyReminderTimeAndCount(picked, dailyCount)
                                        onSetReminders(settings.dailyReminderEnabled, encoded, settings.monthlyBudgetLimitMinor)
                                        showDailyReminderTimePicker = false
                                    },
                                    onDismiss = { showDailyReminderTimePicker = false }
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = ext.cardBorder)

                            Text(
                                text = tr(isArabic, "حد تنبيه الميزانية الشهرية (${currInfo.symbolAr})", "Monthly Budget Alert Limit (${currInfo.code})"),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = budgetInput,
                                    onValueChange = { budgetInput = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    placeholder = { Text(tr(isArabic, "مثال: 10000", "e.g. 10000")) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val minor = parseAmountToMinor(budgetInput) ?: 0L
                                        onSetReminders(settings.dailyReminderEnabled, settings.dailyReminderTime, minor)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ext.heroCardBackground,
                                        contentColor = ext.heroCardOnBackground
                                    )
                                ) {
                                    Text(tr(isArabic, "حفظ", "Save"))
                                }
                            }
                        }
                    }

                    SettingsSection.CATEGORIES -> {
                        SereneCard(contentPadding = PaddingValues(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr(isArabic, "إدارة التصنيفات", "Manage Categories"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                TextButton(onClick = { showAddCategoryDialog = true }) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tr(isArabic, "إضافة تصنيف", "Add"))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    TransactionType.EXPENSE to tr(isArabic, "تصنيفات المصاريف", "Expenses"),
                                    TransactionType.INCOME to tr(isArabic, "تصنيفات الدخل", "Income")
                                ).forEach { (k, lbl) ->
                                    val chosen = categoryManagerKind == k
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { categoryManagerKind = k },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (chosen) ext.heroCardBackground else ext.cardSubtleBackground
                                    ) {
                                        Text(
                                            text = lbl,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (chosen) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 9.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            val filteredCats = categories.filter { it.kind == categoryManagerKind }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredCats.forEach { cat ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ext.cardSubtleBackground)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = iconForKey(cat.iconKey),
                                                contentDescription = null,
                                                tint = if (cat.kind == TransactionType.INCOME) ext.incomeColor else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                            Text(cat.name, style = MaterialTheme.typography.labelLarge)
                                        }
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = tr(isArabic, "حذف التصنيف", "Delete Category"),
                                            tint = ext.expenseColor,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { onDeleteCategory(cat) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingsSection.ANALYTICS -> {
                        val analyticsCfg = remember(settings.transactionsViewMode) {
                            settings.analyticsDashboardConfig()
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionDetailHeader(
                                icon = Icons.Outlined.Analytics,
                                title = tr(isArabic, "إعدادات التحليل والداش بورد", "Analytics & Dashboard Settings"),
                                subtitle = tr(
                                    isArabic,
                                    "تحكم في ظهور مربعات الداش بورد المرتبطة بالفلتر المختار والمخططات البيانية في تبويبة تحليل البيانات",
                                    "Control contextual filter-linked dashboard boxes and charts in Data Analysis"
                                )
                            )

                            // Primary Feature Card: Contextual Filter-Driven Dashboard Cards
                            SereneCard(
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(13.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.FilterList,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(23.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = tr(
                                                    isArabic,
                                                    "عرض مربعات الداش بورد المرتبطة بالفلتر المختار فقط",
                                                    "Show Only Dashboard Boxes Related to Selected Filter"
                                                ),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = tr(
                                                    isArabic,
                                                    "عند اختيار فلتر معين في تبويبة تحليل البيانات (مثل: الدخل فقط، المصاريف فقط، مطلوب دفعها قريباً، أو اندفعت بالفعل)، يتم عرض مربعات الداش بورد الخاصة بهذا الفلتر فقط وإخفاء المربعات غير المرتبطة به.",
                                                    "When choosing a specific filter in Data Analysis, display only the dashboard cards relevant to that filter."
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = analyticsCfg.contextualFilterCardsOnly,
                                        onCheckedChange = { enabled ->
                                            onUpdateAnalyticsConfig { it.copy(contextualFilterCardsOnly = enabled) }
                                        },
                                        modifier = Modifier.testTag("analytics_setting_contextual_filter_switch"),
                                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }

                            // Individual Dashboard Sections & Boxes Toggles
                            SereneCard(contentPadding = PaddingValues(0.dp)) {
                                DeleteBranchHeader(
                                    icon = Icons.Outlined.DashboardCustomize,
                                    tint = ext.incomeColor,
                                    containerColor = ext.incomeContainer,
                                    title = tr(isArabic, "التحكم في مربعات ومخططات تبويبة تحليل البيانات", "Customize Data Analysis Dashboard Boxes & Charts"),
                                    subtitle = tr(isArabic, "إظهار أو إخفاء أي قسم من أقسام لوحة تحليل البيانات", "Show or hide individual sections in Data Analysis")
                                )
                                HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))

                                AnalyticsSettingSwitchRow(
                                    title = tr(isArabic, "مربعات صفحات التفاصيل السريعة (أعلى التحليل)", "Quick Details Boxes (Top Strip)"),
                                    subtitle = tr(isArabic, "مربعات الانتقال السريع لتفاصيل الدخل والمصاريف والالتزامات والفلوس اللي دخلت لك", "Quick navigation cards for Income, Expenses, Obligations & Lifetime Money"),
                                    checked = analyticsCfg.showQuickDetailsBoxes,
                                    tag = "analytics_setting_quick_boxes_switch",
                                    onCheckedChange = { v -> onUpdateAnalyticsConfig { it.copy(showQuickDetailsBoxes = v) } }
                                )
                                HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))

                                AnalyticsSettingSwitchRow(
                                    title = tr(isArabic, "مربعات الملخص المالي والأرصدة (6 مربعات KPI)", "Financial Summary & KPI Boxes"),
                                    subtitle = tr(isArabic, "إجمالي الدخل، إجمالي المصاريف، الصافي، المتوسط اليومي، أكبر مصروف، ومعدل الالتزام", "Income, Expenses, Net Balance, Daily Average, Largest Expense & Savings Rate"),
                                    checked = analyticsCfg.showKpiGrid,
                                    tag = "analytics_setting_kpi_grid_switch",
                                    onCheckedChange = { v -> onUpdateAnalyticsConfig { it.copy(showKpiGrid = v) } }
                                )
                                HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))

                                AnalyticsSettingSwitchRow(
                                    title = tr(isArabic, "مربع تحليل المبالغ المطلوب دفعها قريباً والمسددة", "Amounts Due Soon & Paid Debts Card"),
                                    subtitle = tr(isArabic, "يعرض المبالغ المطلوب دفعها قريباً، المبالغ اللي اندفعت بالفعل، ونسبة التحصيل والسداد", "Displays unpaid debts, settled payments, and settlement progress bar"),
                                    checked = analyticsCfg.showDebtsAndPaymentsCard,
                                    tag = "analytics_setting_debts_card_switch",
                                    onCheckedChange = { v -> onUpdateAnalyticsConfig { it.copy(showDebtsAndPaymentsCard = v) } }
                                )
                                HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))

                                AnalyticsSettingSwitchRow(
                                    title = tr(isArabic, "مربع ملخص الأداء المالي اللحظي", "Live Financial Performance Summary Card"),
                                    subtitle = tr(isArabic, "تحليل نصي ذكي للفترة المختارة وأعلى تصنيف وحساب", "Instant analytical summary for the selected filter and period"),
                                    checked = analyticsCfg.showSmartInsightsCard,
                                    tag = "analytics_setting_insights_card_switch",
                                    onCheckedChange = { v -> onUpdateAnalyticsConfig { it.copy(showSmartInsightsCard = v) } }
                                )
                                HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))

                                AnalyticsSettingSwitchRow(
                                    title = tr(isArabic, "مخطط حركة السيولة اليومية (الدخل مقابل المصروف)", "Daily Cash-Flow Trend Chart"),
                                    subtitle = tr(isArabic, "الرسم البياني الخطي لحركة الفلوس عبر الأيام", "Line chart tracking daily income and expenses"),
                                    checked = analyticsCfg.showTrendChart,
                                    tag = "analytics_setting_trend_chart_switch",
                                    onCheckedChange = { v -> onUpdateAnalyticsConfig { it.copy(showTrendChart = v) } }
                                )
                                HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))

                                AnalyticsSettingSwitchRow(
                                    title = tr(isArabic, "مخطط توزيع التصنيفات (الدائري والأعمدة)", "Category Breakdown Donut & Bar Chart"),
                                    subtitle = tr(isArabic, "تحليل النسب المئوية لكل تصنيف مع إمكانية الفلترة بالضغط", "Percentage breakdown per category with interactive drill-down"),
                                    checked = analyticsCfg.showCategoryChart,
                                    tag = "analytics_setting_category_chart_switch",
                                    onCheckedChange = { v -> onUpdateAnalyticsConfig { it.copy(showCategoryChart = v) } }
                                )
                                HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))

                                AnalyticsSettingSwitchRow(
                                    title = tr(isArabic, "مخطط توزيع الحسابات وطرق الدفع", "Account & Payment Method Distribution"),
                                    subtitle = tr(isArabic, "تحليل العمليات الموزعة على الكاش والمحافظ والحسابات البنكية", "Distribution of transactions across cash, wallets, and bank accounts"),
                                    checked = analyticsCfg.showAccountChart,
                                    tag = "analytics_setting_account_chart_switch",
                                    onCheckedChange = { v -> onUpdateAnalyticsConfig { it.copy(showAccountChart = v) } }
                                )
                                HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))

                                AnalyticsSettingSwitchRow(
                                    title = tr(isArabic, "مخططات الإيقاع الأسبوعي والمقارنة الشهرية", "Weekly Rhythm & Month-over-Month Comparison"),
                                    subtitle = tr(isArabic, "توزيع العمليات على أيام الأسبوع ومقارنة الفترة الحالية بالسابقة", "Day-of-week distribution and period comparison charts"),
                                    checked = analyticsCfg.showWeeklyCharts,
                                    tag = "analytics_setting_weekly_charts_switch",
                                    onCheckedChange = { v -> onUpdateAnalyticsConfig { it.copy(showWeeklyCharts = v) } }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Financial Peace Note Card (matching screen_6d8d020b.png)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = ext.incomeContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, ext.incomeColor.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Spa,
                            contentDescription = null,
                            tint = ext.incomeColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = tr(isArabic, "راحة مالية تبدأ بالتنظيم والتحكم", "Financial peace begins with clarity"),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = ext.incomeColor
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tr(
                                    isArabic,
                                    "تطبيق مصروفي يعمل باستقلالية تامة، بدون إعلانات وبأعلى درجات الخصوصية لتجربة مستخدم آمنة وسلسة.",
                                    "Masrofy works completely offline, ad-free, and with full on-device privacy."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Category Purge Picker Dialog
    if (showCategoryPurgeModal) {
        val grouped = remember(transactions) {
            transactions.groupBy { it.category }.mapValues { it.value.size }
        }
        AlertDialog(
            onDismissRequest = { showCategoryPurgeModal = false },
            title = { Text(tr(isArabic, "اختر التصنيف المراد مسح عملياته", "Choose Category to Delete")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (grouped.isEmpty()) {
                        Text(tr(isArabic, "لا توجد عمليات مسجلة حالياً", "No recorded transactions"))
                    } else {
                        grouped.forEach { (cat, count) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        showCategoryPurgeModal = false
                                        pendingPurge = PendingSettingsPurge.ByCategory(cat)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = ext.cardSubtleBackground
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        tr(isArabic, "$count عملية", "$count items"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.expenseColor
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryPurgeModal = false }) {
                    Text(tr(isArabic, "إلغاء", "Cancel"))
                }
            }
        )
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        var newPin by remember { mutableStateOf("") }
        var err by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text(tr(isArabic, "تعيين رمز المرور PIN", "Set Security PIN")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr(isArabic, "أدخل رمزاً من 4 إلى 6 أرقام لحماية عمليات الحذف وفتح التطبيق.", "Enter a 4 to 6 digit PIN.")
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = {
                            if (it.length <= 6 && it.all(Char::isDigit)) {
                                newPin = it
                                err = false
                            }
                        },
                        singleLine = true,
                        isError = err,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        placeholder = { Text("••••") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (err) {
                        Text(
                            tr(isArabic, "يجب إدخال 4 أرقام على الأقل", "Must be at least 4 digits"),
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.expenseColor
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPin.length < 4) {
                            err = true
                        } else {
                            onSetPinProtection(true, newPin)
                            showPinSetupDialog = false
                        }
                    }
                ) {
                    Text(tr(isArabic, "حفظ وتفعيل", "Save & Enable"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) {
                    Text(tr(isArabic, "إلغاء", "Cancel"))
                }
            }
        )
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        var accName by remember { mutableStateOf("") }
        var accCurr by remember { mutableStateOf(settings.defaultCurrency) }
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text(tr(isArabic, "إضافة حساب / محفظة جديدة", "Add Account / Wallet")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        singleLine = true,
                        placeholder = { Text(tr(isArabic, "مثال: محفظة أورنج كاش", "e.g. Savings Card")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SupportedCurrencies.forEach { c ->
                            val sel = accCurr == c.code
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { accCurr = c.code },
                                shape = RoundedCornerShape(999.dp),
                                color = if (sel) ext.heroCardBackground else ext.cardSubtleBackground
                            ) {
                                Text(
                                    text = c.code,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (sel) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (accName.isNotBlank()) {
                            onAddAccount(accName, "account_balance", accCurr)
                            showAddAccountDialog = false
                        }
                    }
                ) {
                    Text(tr(isArabic, "إضافة", "Add"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text(tr(isArabic, "إلغاء", "Cancel"))
                }
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        var selectedIcon by remember { mutableStateOf("category") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text(tr(isArabic, "إضافة تصنيف جديد", "New Category")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        singleLine = true,
                        placeholder = { Text(tr(isArabic, "اسم التصنيف...", "Category name...")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SelectableIconKeys.take(12).forEach { key ->
                            val sel = selectedIcon == key
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) ext.heroCardBackground else ext.cardSubtleBackground)
                                    .clickable { selectedIcon = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconForKey(key),
                                    contentDescription = key,
                                    tint = if (sel) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (catName.isNotBlank()) {
                            onAddCategory(catName, selectedIcon, categoryManagerKind)
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text(tr(isArabic, "إضافة", "Add"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(tr(isArabic, "إلغاء", "Cancel"))
                }
            }
        )
    }

    // Purge Confirmation Modal (with Double Confirm for WipeAll + PIN check if enabled)
    pendingPurge?.let { p ->
        val (title, desc, doubleConfirm) = when (p) {
            PendingSettingsPurge.CurrentWeek -> Triple(
                tr(isArabic, "مسح معاملات الأسبوع الحالي", "Delete Current Week"),
                tr(
                    isArabic,
                    "سيتم حذف كافة الحركات المالية المسجلة خلال آخر 7 أيام فقط مع الاحتفاظ بباقي السجل.",
                    "Delete all transactions recorded in the last 7 days while preserving earlier history?"
                ),
                false
            )
            PendingSettingsPurge.CurrentMonth -> Triple(
                tr(isArabic, "مسح معاملات الشهر الحالي", "Delete Current Month"),
                tr(
                    isArabic,
                    "سيتم حذف كافة الحركات المالية المسجلة خلال الشهر الجاري فقط مع الاحتفاظ بباقي الشهور.",
                    "Delete all transactions recorded this month while preserving earlier months?"
                ),
                false
            )
            is PendingSettingsPurge.OlderThan -> Triple(
                tr(isArabic, "حذف العمليات الأقدم من ${p.months} شهور", "Delete Older Than ${p.months} Months"),
                tr(
                    isArabic,
                    "سيتم مسح جميع الحركات المالية المسجلة قبل ${p.months} شهور نهائياً.",
                    "Permanently delete transactions older than ${p.months} months?"
                ),
                false
            )
            is PendingSettingsPurge.ByCategory -> Triple(
                tr(isArabic, "مسح معاملات تصنيف (${p.category})", "Delete ${p.category}"),
                tr(
                    isArabic,
                    "سيتم حذف جميع العمليات المرتبطة بتصنيف (${p.category}) فقط.",
                    "Delete all transactions under (${p.category})?"
                ),
                false
            )
            is PendingSettingsPurge.BySource -> Triple(
                tr(isArabic, "مسح معاملات حساب (${p.source})", "Delete ${p.source} Entries"),
                tr(
                    isArabic,
                    "سيتم حذف جميع العمليات المسجلة على حساب / طريقة الدفع (${p.source}) فقط.",
                    "Delete all transactions recorded under account/method (${p.source})?"
                ),
                false
            )
            is PendingSettingsPurge.SingleTransaction -> Triple(
                tr(isArabic, "حذف العملية المحددة (${p.tx.category})", "Delete Transaction (${p.tx.category})"),
                tr(
                    isArabic,
                    "سيتم حذف عملية (${p.tx.category}) بقيمة ${formatAmountMinor(p.tx.amountMinor, false)} ${currencySymbol(p.tx.currency, isArabic)} نهائياً.",
                    "Permanently delete this (${p.tx.category}) transaction?"
                ),
                false
            )
            is PendingSettingsPurge.SingleObligation -> Triple(
                tr(isArabic, "حذف الالتزام المحدد (${p.ob.reason})", "Delete Obligation (${p.ob.reason})"),
                tr(
                    isArabic,
                    "سيتم حذف الالتزام (${p.ob.reason}) وإلغاء منبهاته المجدولة نهائياً.",
                    "Permanently delete obligation (${p.ob.reason}) and cancel its reminders?"
                ),
                false
            )
            PendingSettingsPurge.WipeAll -> Triple(
                tr(isArabic, "تصفير وحذف جميع العمليات دفعة واحدة", "Wipe All Transactions"),
                tr(
                    isArabic,
                    "سيتم مسح كافة سجلات الدخل والمصاريف (${transactions.size} عملية) وتصفير الأرصدة بالكامل.",
                    "Erase all ${transactions.size} transactions and reset balances to zero?"
                ),
                true
            )
            PendingSettingsPurge.ResetLifetimeIncome -> Triple(
                tr(isArabic, "تصفير «الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج»", "Reset Lifetime Added Money"),
                tr(
                    isArabic,
                    "سيتم تصفير كل عمليات إضافة الفلوس (الإيرادات) وإعادتها إلى 0.00 مع الإبقاء على باقي الأرقام.",
                    "Reset all added-money (income) entries to 0.00 while keeping other numbers?"
                ),
                true
            )
            PendingSettingsPurge.ResetEarnedIncomeOnly -> Triple(
                tr(isArabic, "تصفير فرع «فلوس ملكك» (في تفاصيل الدخل)", "Reset 'Your Own Money' Branch"),
                tr(
                    isArabic,
                    "سيتم تصفير عمليات الدخل المكتسب (فلوس ملكك) وإعادتها إلى 0.00 دون المساس بباقي الفروع.",
                    "Reset earned non-debt income entries to 0.00?"
                ),
                false
            )
            PendingSettingsPurge.ResetUnpaidIncomeDebts -> Triple(
                tr(isArabic, "تصفير فرع «فلوس مطلوب سدادها قريباً / فلوس متسددتش لسه»", "Reset Unpaid Income Debts Branch"),
                tr(
                    isArabic,
                    "سيتم تصفير مديونيات الدخل غير المسددة في صفحة تفاصيل الدخل وإعادتها إلى 0.00.",
                    "Reset unpaid income debt entries to 0.00?"
                ),
                false
            )
            PendingSettingsPurge.ResetSettledIncomeDebts -> Triple(
                tr(isArabic, "تصفير فرع «فلوس اندفعت خلاص» (في تفاصيل الدخل)", "Reset Settled Income Debts Branch"),
                tr(
                    isArabic,
                    "سيتم تصفير مديونيات الدخل المسددة في صفحة تفاصيل الدخل وإعادتها إلى 0.00.",
                    "Reset settled income debt entries to 0.00?"
                ),
                false
            )
            PendingSettingsPurge.ResetExpenses -> Triple(
                tr(isArabic, "تصفير إجمالي المصاريف بالكامل", "Reset Total Expenses"),
                tr(
                    isArabic,
                    "سيتم تصفير جميع عمليات المصاريف بكافة فروعها وإعادتها إلى 0.00 مع الإبقاء على فلوسك المضافة.",
                    "Reset all expense entries to 0.00 while preserving your added money?"
                ),
                true
            )
            PendingSettingsPurge.ResetPaidExpensesOnly -> Triple(
                tr(isArabic, "تصفير فرع «اندفعت خلاص» (مصاريف مدفوعة مباشرة)", "Reset Paid Direct Expenses Branch"),
                tr(
                    isArabic,
                    "سيتم تصفير المصاريف المدفوعة مباشرة في صفحة تفاصيل المصاريف وإعادتها إلى 0.00.",
                    "Reset direct paid expense entries to 0.00?"
                ),
                false
            )
            PendingSettingsPurge.ResetUnpaidExpenseDebts -> Triple(
                tr(isArabic, "تصفير فرع «مندفعتش لسه» (في تفاصيل المصاريف)", "Reset Unpaid Expense Debts Branch"),
                tr(
                    isArabic,
                    "سيتم تصفير مديونيات المصاريف التي لم تدفع بعد وإعادتها إلى 0.00.",
                    "Reset unpaid expense debt entries to 0.00?"
                ),
                false
            )
            PendingSettingsPurge.ResetSettledExpenseDebts -> Triple(
                tr(isArabic, "تصفير فرع «فلوس اندفعت خلاص» (في تفاصيل المصاريف)", "Reset Settled Expense Debts Branch"),
                tr(
                    isArabic,
                    "سيتم تصفير مديونيات المصاريف المسددة وإعادتها إلى 0.00.",
                    "Reset settled expense debt entries to 0.00?"
                ),
                false
            )
            PendingSettingsPurge.ResetUnpaidDebts -> Triple(
                tr(isArabic, "تصفير «كل المبالغ المطلوب دفعها قريباً»", "Reset All Amounts Due Soon"),
                tr(
                    isArabic,
                    "سيتم تصفير كافة المبالغ غير المسددة وإعادتها إلى 0.00.",
                    "Reset all unpaid debt entries to 0.00?"
                ),
                false
            )
            PendingSettingsPurge.ResetSettledPayments -> Triple(
                tr(isArabic, "تصفير «كل المبالغ اللي اندفعت بالفعل»", "Reset All Settled Payments"),
                tr(
                    isArabic,
                    "سيتم تصفير كافة المبالغ والمديونيات التي اندفعت بالفعل وإعادتها إلى 0.00.",
                    "Reset all settled debt payment records to 0.00?"
                ),
                false
            )
            PendingSettingsPurge.ResetObligations -> Triple(
                tr(isArabic, "تصفير كل الالتزامات بمواعيد سداد", "Reset All Scheduled Obligations"),
                tr(
                    isArabic,
                    "سيتم مسح وتصفير كافة الالتزامات بمواعيد سداد وإلغاء تنبيهاتها المجدولة.",
                    "Clear and reset all scheduled obligations and cancel their reminders?"
                ),
                true
            )
            PendingSettingsPurge.ResetPendingObligations -> Triple(
                tr(isArabic, "تصفير الالتزامات بمواعيد سداد القادمة (لسه هتندفع)", "Reset Upcoming Obligations"),
                tr(
                    isArabic,
                    "سيتم مسح وتصفير كافة الالتزامات القادمة غير المسددة وإلغاء منبهاتها.",
                    "Clear and reset all upcoming unpaid obligations?"
                ),
                false
            )
            PendingSettingsPurge.ResetPaidObligations -> Triple(
                tr(isArabic, "تصفير الالتزامات المسددة (اللي اندفعت خلاص)", "Reset Paid Obligations"),
                tr(
                    isArabic,
                    "سيتم مسح وتصفير سجل الالتزامات المسددة وإعادتها إلى 0.00.",
                    "Clear and reset all paid obligations?"
                ),
                false
            )
            PendingSettingsPurge.ResetMonthlyBudget -> Triple(
                tr(isArabic, "تصفير حد الميزانية الشهرية", "Reset Monthly Budget Limit"),
                tr(
                    isArabic,
                    "سيتم إعادة حد الميزانية الشهرية إلى 0.00 (بدون حد).",
                    "Reset the monthly budget limit to 0.00?"
                ),
                false
            )
            PendingSettingsPurge.ResetAllAppNumbers -> Triple(
                tr(isArabic, "تصفير جميع أرقام التطبيق بالكامل", "Reset ALL App Numbers"),
                tr(
                    isArabic,
                    "سيتم تصفير الفلوس اللي دخلت لك، المصاريف، المبالغ المطلوب دفعها، الالتزامات، وحد الميزانية بالكامل إلى 0.00 مع الحفاظ على حسابك وإعداداتك.",
                    "Reset all money, expenses, debts, obligations, and budget numbers to 0.00 while keeping your profile and settings?"
                ),
                true
            )
        }

        SecurityVerificationDialog(
            title = title,
            description = if (doubleConfirm) {
                desc + "\n\n" + tr(isArabic, "تحذير: لا يمكن التراجع عن هذا الإجراء!", "Warning: This action cannot be undone!")
            } else desc,
            confirmLabel = tr(isArabic, "تأكيد التصفير / الحذف", "Confirm Reset / Delete"),
            isDanger = true,
            requireSecurity = settings.hasAnyLockMethodConfigured(),
            settings = settings,
            isArabic = isArabic,
            onConfirmed = {
                when (p) {
                    PendingSettingsPurge.CurrentWeek -> onDeleteCurrentWeek()
                    PendingSettingsPurge.CurrentMonth -> onDeleteCurrentMonth()
                    is PendingSettingsPurge.OlderThan -> onDeleteOlderThanMonths(p.months)
                    is PendingSettingsPurge.ByCategory -> onDeleteByCategory(p.category)
                    is PendingSettingsPurge.BySource -> onDeleteBySource(p.source)
                    is PendingSettingsPurge.SingleTransaction -> onDeleteSingleTransaction(p.tx.id)
                    is PendingSettingsPurge.SingleObligation -> onDeleteSingleObligation(p.ob.id)
                    PendingSettingsPurge.WipeAll -> onWipeAll()
                    PendingSettingsPurge.ResetLifetimeIncome -> onResetLifetimeIncome()
                    PendingSettingsPurge.ResetEarnedIncomeOnly -> onResetEarnedIncomeOnly()
                    PendingSettingsPurge.ResetUnpaidIncomeDebts -> onResetUnpaidIncomeDebts()
                    PendingSettingsPurge.ResetSettledIncomeDebts -> onResetSettledIncomeDebts()
                    PendingSettingsPurge.ResetExpenses -> onResetExpenses()
                    PendingSettingsPurge.ResetPaidExpensesOnly -> onResetPaidExpensesOnly()
                    PendingSettingsPurge.ResetUnpaidExpenseDebts -> onResetUnpaidExpenseDebts()
                    PendingSettingsPurge.ResetSettledExpenseDebts -> onResetSettledExpenseDebts()
                    PendingSettingsPurge.ResetUnpaidDebts -> onResetUnpaidDebts()
                    PendingSettingsPurge.ResetSettledPayments -> onResetSettledPayments()
                    PendingSettingsPurge.ResetObligations -> onResetObligations()
                    PendingSettingsPurge.ResetPendingObligations -> onResetPendingObligations()
                    PendingSettingsPurge.ResetPaidObligations -> onResetPaidObligations()
                    PendingSettingsPurge.ResetMonthlyBudget -> onResetMonthlyBudget()
                    PendingSettingsPurge.ResetAllAppNumbers -> onResetAllAppNumbers()
                }
                pendingPurge = null
            },
            onDismiss = { pendingPurge = null }
        )
    }

    if (showPatternSetupDialog) {
        PatternSetupDialog(
            isArabic = isArabic,
            onSaved = { pattern ->
                onSetPatternProtection(true, pattern)
                showPatternSetupDialog = false
            },
            onDismiss = { showPatternSetupDialog = false }
        )
    }
}

@Composable
private fun SettingsGridTile(
    modifier: Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    selected: Boolean,
    isDangerHighlight: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    val borderColor = when {
        selected && isDangerHighlight -> ext.expenseColor.copy(alpha = 0.55f)
        selected -> ext.incomeColor.copy(alpha = 0.65f)
        else -> ext.cardBorder.copy(alpha = 0.7f)
    }
    Surface(
        modifier = modifier
            .heightIn(min = 104.dp)
            .testTag(tag)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = ext.cardBackground,
        border = BorderStroke(if (selected) 1.8.dp else 1.dp, borderColor),
        shadowElevation = if (selected && !ext.isDark) 3.dp else 0.5.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(if (isDangerHighlight) ext.expenseColor else ext.incomeColor)
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isDangerHighlight && selected) ext.expenseColor else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ResetNumberGridTile(
    modifier: Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    valueText: String,
    countText: String,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = modifier
            .heightIn(min = 128.dp)
            .testTag(tag)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = ext.cardSubtleBackground,
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = valueText,
                style = AmountBodyStyle.copy(
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = iconTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = countText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SubTabPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) ext.cardBackground else Color.Transparent,
        shadowElevation = if (selected && !ext.isDark) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingLabel: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (trailingLabel != null) {
                Text(
                    text = trailingLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SectionDetailHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val ext = LocalSereneColors.current
    SereneCard(contentPadding = PaddingValues(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
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
        }
    }
}

@Composable
private fun DeleteBranchHeader(
    icon: ImageVector,
    tint: Color,
    containerColor: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
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
    }
}

@Composable
private fun AnalyticsSettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val ext = LocalSereneColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(checkedTrackColor = ext.incomeColor)
        )
    }
}

