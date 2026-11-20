package com.example.ui.screens

import android.app.TimePickerDialog
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddAPhoto
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
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pattern
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.BackupDrawerMode
import com.example.ui.components.PatternSetupDialog
import com.example.ui.components.SecurityVerificationDialog
import com.example.ui.components.SereneCard
import com.example.ui.components.UserAvatarCircle
import com.example.ui.theme.LocalSereneColors
import com.example.util.SelectableIconKeys
import com.example.util.SupportedCurrencies
import com.example.util.canUseDeviceBiometricOrCredential
import com.example.util.currencyInfoFor
import com.example.util.iconForKey
import com.example.util.launchDeviceBiometricOrCredentialPrompt
import com.example.util.parseAmountToMinor
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
    CATEGORIES
}

private sealed class PendingSettingsPurge {
    data object CurrentMonth : PendingSettingsPurge()
    data class OlderThan(val months: Int) : PendingSettingsPurge()
    data class ByCategory(val category: String) : PendingSettingsPurge()
    data object WipeAll : PendingSettingsPurge()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettingsEntity,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    initialSection: SettingsSection = SettingsSection.DELETE_SECURITY,
    isArabic: Boolean,
    signedInEmail: String? = null,
    onSetQuickDelete: (Boolean) -> Unit,
    onDeleteCurrentMonth: () -> Unit,
    onDeleteOlderThanMonths: (Int) -> Unit,
    onDeleteByCategory: (String) -> Unit,
    onWipeAll: () -> Unit,
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
    onPickUserAvatar: () -> Unit,
    onRemoveUserAvatar: () -> Unit,
    onEmitMessage: (String) -> Unit
) {
    val ext = LocalSereneColors.current
    val context = LocalContext.current

    var activeSection by remember(initialSection) { mutableStateOf(initialSection) }
    var deleteSubTab by remember { mutableStateOf("delete") } // "delete" or "security"
    var pendingPurge by remember { mutableStateOf<PendingSettingsPurge?>(null) }
    var showCategoryPurgeModal by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPatternSetupDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryManagerKind by remember { mutableStateOf(TransactionType.EXPENSE) }
    var profileNameInput by remember(settings.userName) { mutableStateOf(settings.userName) }

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
                                text = tr(isArabic, "٧ أقسام رئيسية", "7 Core Areas"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ext.incomeColor
                            )
                        }
                    }

                    // Featured Row 0: Dedicated «المعلومات الشخصية» Icon + Home Screen Widget Shortcut
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
                            icon = Icons.Outlined.Widgets,
                            iconBg = Color(0xFFD8E3FB).copy(alpha = if (ext.isDark) 0.22f else 1f),
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = tr(isArabic, "أيقونة سطح المكتب", "Home Icon"),
                            subtitle = tr(isArabic, "إضافة أيقونة فقط (1×1)", "Pin 1x1 app icon"),
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
                        val isMorningPreview = remember {
                            Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 5..11
                        }
                        val previewGreeting = remember(profileNameInput, isArabic, isMorningPreview) {
                            val base = if (isMorningPreview) {
                                tr(isArabic, "صباح الخير", "Good Morning")
                            } else {
                                tr(isArabic, "مساء الخير", "Good Evening")
                            }
                            val clean = profileNameInput.trim()
                            if (clean.isNotEmpty()) {
                                if (isArabic) "$base يا $clean" else "$base, $clean"
                            } else {
                                tr(isArabic, "$base في مصروفي", "$base • Masrofy")
                            }
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
                                        Text(
                                            text = previewGreeting,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = ext.incomeColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
                                                        text = tr(isArabic, "وضع الحذف السريع", "Quick Delete Mode"),
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
                                                        "إظهار زر المسح الفوري بالسجل والتقارير",
                                                        "Show instant delete icon in Log and Reports"
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

                                // Grouped Deletion Actions Card
                                SereneCard(contentPadding = PaddingValues(0.dp)) {
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
                                                    text = tr(isArabic, "حذف العمليات السابقة", "Delete Older Entries"),
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

                                    // By Category
                                    SettingsActionRow(
                                        icon = Icons.Outlined.Category,
                                        iconBg = ext.expenseContainer.copy(alpha = 0.7f),
                                        iconTint = ext.chartRose,
                                        title = tr(isArabic, "حذف بحسب التصنيف", "Delete by Category"),
                                        subtitle = tr(
                                            isArabic,
                                            "مسح مصاريف الأكل فقط، أو التسوق، أو الفواتير",
                                            "Purge transactions belonging to a specific category"
                                        ),
                                        trailingLabel = tr(isArabic, "اختر التصنيف", "Select"),
                                        onClick = { showCategoryPurgeModal = true }
                                    )

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
                        SereneCard(contentPadding = PaddingValues(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr(isArabic, "تذكير يومي بتسجيل المصاريف", "Daily Expense Reminder"),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = tr(
                                            isArabic,
                                            "الوقت المفضل: ${settings.dailyReminderTime}",
                                            "Preferred time: ${settings.dailyReminderTime}"
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
                            TextButton(
                                onClick = {
                                    val parts = settings.dailyReminderTime.split(":")
                                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 21
                                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    runCatching {
                                        TimePickerDialog(context, { _, hour, min ->
                                            val nextTime = String.format(Locale.US, "%02d:%02d", hour, min)
                                            onSetReminders(settings.dailyReminderEnabled, nextTime, settings.monthlyBudgetLimitMinor)
                                        }, h, m, true).show()
                                    }
                                }
                            ) {
                                Text(tr(isArabic, "تغيير وقت التذكير اليومي", "Change Daily Reminder Time"))
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
                tr(isArabic, "مسح معاملات ${p.category}", "Delete ${p.category}"),
                tr(
                    isArabic,
                    "سيتم حذف جميع العمليات المرتبطة بتصنيف (${p.category}) فقط.",
                    "Delete all transactions under (${p.category})?"
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
        }

        SecurityVerificationDialog(
            title = title,
            description = if (doubleConfirm) {
                desc + "\n\n" + tr(isArabic, "تحذير: لا يمكن التراجع عن هذا الإجراء!", "Warning: This action cannot be undone!")
            } else desc,
            confirmLabel = tr(isArabic, "تأكيد الحذف", "Confirm Delete"),
            isDanger = true,
            requireSecurity = settings.hasAnyLockMethodConfigured(),
            settings = settings,
            isArabic = isArabic,
            onConfirmed = {
                when (p) {
                    PendingSettingsPurge.CurrentMonth -> onDeleteCurrentMonth()
                    is PendingSettingsPurge.OlderThan -> onDeleteOlderThanMonths(p.months)
                    is PendingSettingsPurge.ByCategory -> onDeleteByCategory(p.category)
                    PendingSettingsPurge.WipeAll -> onWipeAll()
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
