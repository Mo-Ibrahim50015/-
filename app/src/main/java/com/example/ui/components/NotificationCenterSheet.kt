package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppSettingsEntity
import com.example.data.ObligationEntity
import com.example.data.ReminderAlertMode
import com.example.data.TransactionEntity
import com.example.ui.theme.LocalSereneColors
import com.example.util.PaymentAlertModeOptions
import com.example.util.PaymentNotificationManager
import com.example.util.currencySymbol
import com.example.util.formatAlertModeLabel
import com.example.util.formatAmountMinor
import com.example.util.formatReminderLeadTime
import com.example.util.formatTime12Hour
import com.example.util.tr

private data class ScheduledAlertRow(
    val key: String,
    val title: String,
    val amountDisplay: String,
    val party: String,
    val dueDate: String,
    val dueTime: String,
    val leadMinutes: Int,
    val muted: Boolean,
    val alertMode: String,
    val fullScreen: Boolean,
    val speakAloud: Boolean,
    val tx: TransactionEntity? = null,
    val obligation: ObligationEntity? = null
)

/**
 * Organized 4-Tab Notification Control Center launched from the in-app Notification Bell icon.
 * Each tab presents clear step-by-step controls:
 * 1. عام والتحكم (Master on/off, summary & quick pause)
 * 2. شكل التنبيه (Steps: Alert mode -> Full-screen -> Voice TTS -> Live Test)
 * 3. مواعيد السداد (All upcoming dated payments & obligations with per-item settings button)
 * 4. خطوات الأذونات (Notification, Exact Alarm, and Full-Screen permissions checklist)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    todayCount: Int,
    monthExpenseMinor: Long,
    settings: AppSettingsEntity,
    transactions: List<TransactionEntity>,
    obligations: List<ObligationEntity>,
    isArabic: Boolean,
    hasUnreadAlerts: Boolean = false,
    onMarkAllAsRead: () -> Unit = {},
    onToggleNotifications: (Boolean) -> Unit,
    onUpdateAlertDefaults: (alertMode: String, fullScreen: Boolean, speakAloud: Boolean, ringtone: Boolean) -> Unit,
    onUpdateTransactionReminder: (txId: Long, config: ItemReminderConfig) -> Unit,
    onUpdateObligationReminder: (obId: Long, config: ItemReminderConfig) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenObligations: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalSereneColors.current
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingRow by remember { mutableStateOf<ScheduledAlertRow?>(null) }
    var markedAllReadLocal by remember(hasUnreadAlerts) { mutableStateOf(!hasUnreadAlerts) }

    val sym = currencySymbol(settings.defaultCurrency, isArabic)
    val upcomingRows = remember(transactions, obligations, isArabic) {
        buildList {
            obligations.filter { !it.isPaid }.forEach { ob ->
                val obSym = currencySymbol(ob.currency, isArabic)
                add(
                    ScheduledAlertRow(
                        key = "ob_${ob.id}",
                        title = ob.reason.ifBlank { tr(isArabic, "التزام سداد", "Obligation") },
                        amountDisplay = "${formatAmountMinor(ob.amountMinor)} $obSym",
                        party = ob.party,
                        dueDate = ob.dueDate,
                        dueTime = ob.dueTime,
                        leadMinutes = ob.reminderLeadMinutes,
                        muted = ob.reminderMuted || !ob.reminderEnabled,
                        alertMode = ob.reminderAlertMode,
                        fullScreen = ob.reminderFullScreen,
                        speakAloud = ob.reminderSpeakAloud,
                        obligation = ob
                    )
                )
            }
            transactions.filter { it.isDebt && !it.isDebtSettled && (it.paymentDueDate.isNotBlank() || it.hasPaymentReminder) }.forEach { tx ->
                val txSym = currencySymbol(tx.currency, isArabic)
                add(
                    ScheduledAlertRow(
                        key = "tx_${tx.id}",
                        title = tx.itemName.ifBlank { tx.category },
                        amountDisplay = "${formatAmountMinor(tx.amountMinor)} $txSym",
                        party = tx.debtParty,
                        dueDate = tx.paymentDueDate.ifBlank { tx.date },
                        dueTime = tx.paymentDueTime.ifBlank { "09:00" },
                        leadMinutes = tx.reminderLeadMinutes,
                        muted = tx.reminderMuted || !tx.hasPaymentReminder,
                        alertMode = tx.reminderAlertMode,
                        fullScreen = tx.reminderFullScreen,
                        speakAloud = tx.reminderSpeakAloud,
                        tx = tx
                    )
                )
            }
        }.sortedWith(compareBy<ScheduledAlertRow> { it.dueDate }.thenBy { it.dueTime })
    }

    val tabs = listOf(
        tr(isArabic, "عام والتحكم", "Overview"),
        tr(isArabic, "شكل التنبيه", "Alert Style"),
        tr(isArabic, "المواعيد (${upcomingRows.size})", "Due (${upcomingRows.size})"),
        tr(isArabic, "الأذونات", "Permissions")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .testTag("notification_center_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with «تمييز الكل كمقروء» action
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (settings.notificationsEnabled) ext.warningAmber.copy(alpha = 0.16f) else ext.cardSubtleBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (settings.notificationsEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = if (settings.notificationsEnabled) ext.warningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = tr(isArabic, "مركز التحكم في الإشعارات والمنبه", "Notifications & Alarm Center"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (settings.notificationsEnabled) {
                                tr(isArabic, "التنبيهات مفعّلة • ${upcomingRows.count { !it.muted }} موعد نشط", "Alerts Active • ${upcomingRows.count { !it.muted }} scheduled")
                            } else {
                                tr(isArabic, "كافة الإشعارات متوقفة حالياً", "All notifications currently paused")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (settings.notificationsEnabled) ext.incomeColor else ext.expenseColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .testTag("notif_mark_all_read_btn")
                        .clickable {
                            markedAllReadLocal = true
                            onMarkAllAsRead()
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = if (markedAllReadLocal) ext.incomeContainer else ext.heroCardBackground.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (markedAllReadLocal) ext.incomeColor.copy(alpha = 0.55f) else ext.heroCardBackground.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (markedAllReadLocal) ext.incomeColor else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (markedAllReadLocal) {
                                tr(isArabic, "مقروءة ✓", "All Read ✓")
                            } else {
                                tr(isArabic, "تمييز الكل كمقروء", "Mark All Read")
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (markedAllReadLocal) ext.incomeColor else MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Organized 4-Tab Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    val active = selectedTab == idx
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .testTag("notif_center_tab_$idx")
                            .clickable { selectedTab = idx },
                        shape = RoundedCornerShape(11.dp),
                        color = if (active) ext.heroCardBackground else ext.cardSubtleBackground,
                        border = BorderStroke(1.dp, if (active) ext.heroCardBackground else ext.cardBorder)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (active) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Tab Body (Scrollable)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 1: Master Control + Step-by-step overview
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (settings.notificationsEnabled) ext.incomeContainer else ext.expenseContainer.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr(isArabic, "خطوة ١: تشغيل أو إيقاف كافة الإشعارات", "Step 1: Master Notifications Switch"),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (settings.notificationsEnabled) ext.incomeColor else ext.expenseColor
                                    )
                                    Text(
                                        text = if (settings.notificationsEnabled) {
                                            tr(isArabic, "سيصلك التنبيه في المواعيد المحددة بالصوت وملء الشاشة", "Scheduled alerts will ring and notify on time")
                                        } else {
                                            tr(isArabic, "لن يرن الهاتف أو يرسل أي إشعارات حتى تعيد التشغيل", "No rings or alerts will fire until re-enabled")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.notificationsEnabled,
                                    onCheckedChange = onToggleNotifications,
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ext.incomeColor),
                                    modifier = Modifier.testTag("notif_master_switch")
                                )
                            }
                        }

                        StepSummaryBox(
                            stepLabel = tr(isArabic, "خطوة ٢: حالة نشاط اليوم", "Step 2: Today's Activity"),
                            title = if (todayCount == 0) {
                                tr(isArabic, "لم تسجل أي عملية اليوم بعد", "No transactions logged today")
                            } else {
                                tr(isArabic, "سجّلت $todayCount عمليات اليوم ✓", "Logged $todayCount transaction(s) today ✓")
                            },
                            subtitle = tr(
                                isArabic,
                                "إجمالي مصروفات الشهر: ${formatAmountMinor(monthExpenseMinor)} $sym",
                                "Monthly expenses: ${formatAmountMinor(monthExpenseMinor)} $sym"
                            ),
                            accent = if (todayCount == 0) ext.warningAmber else ext.incomeColor
                        )

                        StepSummaryBox(
                            stepLabel = tr(isArabic, "خطوة ٣: المواعيد المجدولة للسداد", "Step 3: Scheduled Due Payments"),
                            title = tr(
                                isArabic,
                                "${upcomingRows.size} التزامات بمواعيد سداد قادمة",
                                "${upcomingRows.size} upcoming due commitments"
                            ),
                            subtitle = tr(
                                isArabic,
                                "اضغط لفتح صفحة «التزامات بمواعيد سداد» أو انتقل لتبويب المواعيد أعلاه",
                                "Tap to view Scheduled Due Commitments or switch to the Due tab"
                            ),
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = {
                                onDismiss()
                                onOpenObligations()
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { selectedTab = 1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(tr(isArabic, "تخصيص شكل التنبيه", "Alert Style"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Button(
                                onClick = {
                                    onDismiss()
                                    onOpenSettings()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ext.heroCardBackground, contentColor = ext.heroCardOnBackground)
                            ) {
                                Text(tr(isArabic, "الميزانية والتذكير", "Budget & Daily"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    1 -> {
                        // TAB 2: Step-by-step Alert Style configuration
                        Text(
                            text = tr(isArabic, "خطوة ١: اختر طريقة التنبيه الافتراضية", "Step 1: Default Alert Mode"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        PaymentAlertModeOptions.forEach { opt ->
                            val active = settings.defaultAlertMode == opt.mode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val speakNext = if (opt.mode == ReminderAlertMode.NOTIFICATION_VOICE) true else settings.speakRemindersAloud
                                        val ringNext = opt.mode == ReminderAlertMode.RING || opt.mode == ReminderAlertMode.NOTIFICATION_RING
                                        onUpdateAlertDefaults(opt.mode, settings.fullScreenAlertsEnabled, speakNext, ringNext)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) ext.incomeContainer else ext.cardSubtleBackground,
                                border = BorderStroke(1.dp, if (active) ext.incomeColor else ext.cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isArabic) opt.labelAr else opt.labelEn,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (active) ext.incomeColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isArabic) opt.descAr else opt.descEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (active) {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ext.incomeColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        // Step 2: Full-Screen Alert Toggle
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ext.cardSubtleBackground,
                            border = BorderStroke(1.dp, ext.cardBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        Icon(Icons.Outlined.Smartphone, contentDescription = null, tint = ext.warningAmber, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text(
                                                text = tr(isArabic, "خطوة ٢: إشعار بملء الشاشة", "Step 2: Full-Screen Alert"),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = tr(isArabic, "يفتح شاشة تنبيه كاملة باسم المبلغ والتفاصيل فوق شاشة القفل", "Shows amount & details full-screen over lock screen"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = settings.fullScreenAlertsEnabled,
                                        onCheckedChange = {
                                            onUpdateAlertDefaults(settings.defaultAlertMode, it, settings.speakRemindersAloud, settings.ringtoneAlertsEnabled)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ext.incomeColor)
                                    )
                                }

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
                                        Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, tint = ext.incomeColor, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text(
                                                text = tr(isArabic, "خطوة ٣: نطق التفاصيل بالصوت", "Step 3: Speak Details Aloud"),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = tr(isArabic, "ينطق الهاتف اسم المبلغ وقيمته والجهة عند التنبيه", "Phone speaks item name, amount, and creditor aloud"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = settings.speakRemindersAloud,
                                        onCheckedChange = {
                                            onUpdateAlertDefaults(settings.defaultAlertMode, settings.fullScreenAlertsEnabled, it, settings.ringtoneAlertsEnabled)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ext.incomeColor)
                                    )
                                }

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
                                        Icon(Icons.Outlined.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text(
                                                text = tr(isArabic, "تشغيل صوت الرنين والاهتزاز", "Ringtone & Vibration"),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = tr(isArabic, "تفعيل نغمة المنبه مع الإشعار", "Play alarm tone when due"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = settings.ringtoneAlertsEnabled,
                                        onCheckedChange = {
                                            onUpdateAlertDefaults(settings.defaultAlertMode, settings.fullScreenAlertsEnabled, settings.speakRemindersAloud, it)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ext.incomeColor)
                                    )
                                }
                            }
                        }

                        // Step 4: Live Test Alarm Button
                        Button(
                            onClick = {
                                PaymentNotificationManager.triggerTestAlarmPreview(
                                    context = context,
                                    settings = settings,
                                    isArabic = isArabic
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("notif_test_alarm_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ext.warningAmber,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(isArabic, "خطوة ٤: تجربة إشعار ملء الشاشة والصوت الآن", "Step 4: Test Full-Screen Alarm & Voice Now"),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    2 -> {
                        // TAB 3: All Scheduled Upcoming Dues with Per-Item Settings
                        if (upcomingRows.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ext.cardSubtleBackground
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Outlined.Alarm, contentDescription = null, tint = ext.incomeColor, modifier = Modifier.size(30.dp))
                                    Text(
                                        text = tr(isArabic, "لا توجد مواعيد سداد مجدولة قادمة حالياً", "No upcoming due reminders yet"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = tr(isArabic, "عند إضافة مبلغ مطلوب دفعه مع تحديد تاريخ ووقت سداد سيظهر هنا مع زر إعداداته.", "Any upcoming payment with a due date/time will appear here."),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            upcomingRows.forEach { row ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ext.cardSubtleBackground,
                                    border = BorderStroke(1.dp, if (row.muted) ext.cardBorder else ext.warningAmber.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "${row.title} • ${row.amountDisplay}",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${row.dueDate} • ${formatTime12Hour(row.dueTime, isArabic)} (${formatReminderLeadTime(row.leadMinutes, isArabic)})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (row.muted) {
                                                    tr(isArabic, "التنبيه متوقف لهذه العملية", "Alert muted")
                                                } else {
                                                    "${formatAlertModeLabel(row.alertMode, isArabic)}" +
                                                        if (row.fullScreen) tr(isArabic, " • ملء الشاشة", " • Full-Screen") else "" +
                                                        if (row.speakAloud) tr(isArabic, " • نطق صوتي", " • Voice") else ""
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = if (row.muted) ext.expenseColor else ext.incomeColor
                                            )
                                        }
                                        IconButton(
                                            onClick = { editingRow = row },
                                            modifier = Modifier.testTag("notif_row_settings_${row.key}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Tune,
                                                contentDescription = tr(isArabic, "إعدادات التنبيه", "Alert Settings"),
                                                tint = ext.warningAmber
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // TAB 4: Step-by-Step Permission Readiness Checklist
                        val notifOk = PaymentNotificationManager.hasNotificationPermission(context)
                        val exactOk = PaymentNotificationManager.canScheduleExactAlarms(context)
                        val fullScreenOk = PaymentNotificationManager.canUseFullScreenIntent(context)

                        PermissionStepCard(
                            stepTitle = tr(isArabic, "خطوة ١: إذن الإشعارات", "Step 1: Notifications Permission"),
                            description = tr(isArabic, "مطلوب لإظهار تنبيهات مواعيد السداد على هاتفك", "Required to show payment due notifications"),
                            isGranted = notifOk,
                            actionLabel = tr(isArabic, "تفعيل الإشعارات", "Allow Notifications"),
                            isArabic = isArabic,
                            onAction = {
                                onRequestNotificationPermission()
                                PaymentNotificationManager.openNotificationSettings(context)
                            }
                        )
                        PermissionStepCard(
                            stepTitle = tr(isArabic, "خطوة ٢: إذن المنبه الدقيق", "Step 2: Exact Alarm Permission"),
                            description = tr(isArabic, "يضمن رنين الهاتف في نفس الدقيقة المحددة للسداد بدون تأخير", "Ensures alarms fire on the exact due minute"),
                            isGranted = exactOk,
                            actionLabel = tr(isArabic, "فتح إعدادات المنبه", "Enable Exact Alarms"),
                            isArabic = isArabic,
                            onAction = { PaymentNotificationManager.openExactAlarmSettings(context) }
                        )
                        PermissionStepCard(
                            stepTitle = tr(isArabic, "خطوة ٣: إذن الظهور بملء الشاشة", "Step 3: Full-Screen Alert Permission"),
                            description = tr(isArabic, "يسمح بعرض بطاقة السداد بملء الشاشة حتى أثناء قفل الهاتف", "Allows full-screen payment alarm display"),
                            isGranted = fullScreenOk,
                            actionLabel = tr(isArabic, "إعدادات ملء الشاشة", "Full-Screen Settings"),
                            isArabic = isArabic,
                            onAction = { PaymentNotificationManager.openFullScreenIntentSettings(context) }
                        )
                    }
                }
            }
        }
    }

    editingRow?.let { row ->
        ReminderSettingsDialog(
            itemTitle = row.title,
            amountDisplay = row.amountDisplay,
            partyName = row.party,
            initialConfig = ItemReminderConfig(
                enabled = !row.muted,
                dueDate = row.dueDate,
                dueTime = row.dueTime,
                leadMinutes = row.leadMinutes,
                muted = row.muted,
                alertMode = row.alertMode,
                fullScreen = row.fullScreen,
                speakAloud = row.speakAloud
            ),
            settings = settings,
            isArabic = isArabic,
            onSave = { updated ->
                row.obligation?.let { onUpdateObligationReminder(it.id, updated) }
                row.tx?.let { onUpdateTransactionReminder(it.id, updated) }
                editingRow = null
            },
            onDismiss = { editingRow = null }
        )
    }
}

@Composable
private fun StepSummaryBox(
    stepLabel: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: (() -> Unit)? = null
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = ext.cardSubtleBackground,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = stepLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
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
private fun PermissionStepCard(
    stepTitle: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    isArabic: Boolean,
    onAction: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isGranted) ext.incomeContainer.copy(alpha = 0.7f) else ext.cardSubtleBackground,
        border = BorderStroke(1.dp, if (isGranted) ext.incomeColor.copy(alpha = 0.5f) else ext.warningAmber.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Security,
                        contentDescription = null,
                        tint = if (isGranted) ext.incomeColor else ext.warningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stepTitle,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isGranted) ext.incomeColor else ext.warningAmber)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isGranted) tr(isArabic, "مفعّل ✓", "Granted ✓") else tr(isArabic, "مطلوب", "Action"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isGranted) {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ext.warningAmber)
                ) {
                    Text(actionLabel, color = ext.warningAmber, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
