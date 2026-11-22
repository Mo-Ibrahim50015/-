package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog
import com.example.data.AppSettingsEntity
import com.example.data.ReminderAlertMode
import com.example.ui.theme.LocalSereneColors
import com.example.util.PaymentAlertModeOptions
import com.example.util.PaymentNotificationManager
import com.example.util.PaymentReminderLeadOptions
import com.example.util.currentIsoDate
import com.example.util.formatTime12Hour
import com.example.util.tr
import java.util.Calendar
import java.util.Locale

data class ItemReminderConfig(
    val enabled: Boolean,
    val dueDate: String,
    val dueTime: String,
    val leadMinutes: Int,
    val muted: Boolean,
    val alertMode: String,
    val fullScreen: Boolean,
    val speakAloud: Boolean
)

/**
 * Per-item Reminder & Alarm Settings Dialog opened by the settings icon next to
 * every upcoming unpaid payment or scheduled obligation.
 */
@Composable
fun ReminderSettingsDialog(
    itemTitle: String,
    amountDisplay: String,
    partyName: String,
    initialConfig: ItemReminderConfig,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onSave: (ItemReminderConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalSereneColors.current
    val context = LocalContext.current

    var enabled by remember(initialConfig) { mutableStateOf(initialConfig.enabled && !initialConfig.muted) }
    var dueDate by remember(initialConfig) { mutableStateOf(initialConfig.dueDate.ifBlank { currentIsoDate() }) }
    var dueTime by remember(initialConfig) { mutableStateOf(initialConfig.dueTime.ifBlank { "09:00" }) }
    var leadMinutes by remember(initialConfig) { mutableIntStateOf(initialConfig.leadMinutes) }
    var alertMode by remember(initialConfig) { mutableStateOf(initialConfig.alertMode) }
    var fullScreen by remember(initialConfig) { mutableStateOf(initialConfig.fullScreen) }
    var speakAloud by remember(initialConfig) { mutableStateOf(initialConfig.speakAloud) }
    var showTimePicker by remember { mutableStateOf(false) }

    val leadOptions = remember(isArabic) {
        PaymentReminderLeadOptions.map {
            DropdownFilterOption(it.minutes, if (isArabic) it.labelAr else it.labelEn)
        }
    }
    val modeOptions = remember(isArabic) {
        PaymentAlertModeOptions.map {
            DropdownFilterOption(
                value = it.mode,
                label = if (isArabic) it.labelAr else it.labelEn,
                subtitle = if (isArabic) it.descAr else it.descEn
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("item_reminder_settings_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (enabled) ext.warningAmber.copy(alpha = 0.16f) else ext.cardSubtleBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (enabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                                contentDescription = null,
                                tint = if (enabled) ext.warningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(isArabic, "إعدادات التنبيه وموعد السداد", "Payment Alert & Due Settings"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$itemTitle • $amountDisplay",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.incomeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = tr(isArabic, "إغلاق", "Close"), modifier = Modifier.size(18.dp))
                    }
                }

                // Enable / Mute Switch
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (enabled) ext.incomeContainer else ext.expenseContainer.copy(alpha = 0.5f)
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
                                text = if (enabled) {
                                    tr(isArabic, "التنبيه مفعّل لهذه العملية", "Alert Active for This Item")
                                } else {
                                    tr(isArabic, "التنبيه متوقف / مكتوم", "Alert Paused / Muted")
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (enabled) ext.incomeColor else ext.expenseColor
                            )
                            Text(
                                text = tr(isArabic, "يمكنك إيقاف أو تشغيل التنبيه في أي وقت", "Pause or enable reminder anytime"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ext.incomeColor),
                            modifier = Modifier.testTag("item_reminder_enable_switch")
                        )
                    }
                }

                // Step 1: Due Date & Due Time (separate from creation date!)
                Text(
                    text = tr(isArabic, "١. تاريخ ووقت السداد (غير تاريخ الإضافة)", "1. Payment Due Date & Time"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val parts = dueDate.split("-")
                                val cal = Calendar.getInstance()
                                val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
                                val m = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
                                val d = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
                                DatePickerDialog(context, { _, yy, mm, dd ->
                                    dueDate = String.format(Locale.US, "%04d-%02d-%02d", yy, mm + 1, dd)
                                }, y, m, d).show()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = ext.cardSubtleBackground,
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = ext.warningAmber, modifier = Modifier.size(16.dp))
                            Column {
                                Text(tr(isArabic, "تاريخ السداد", "Due Date"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dueDate, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = ext.cardSubtleBackground,
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = ext.warningAmber, modifier = Modifier.size(16.dp))
                            Column {
                                Text(tr(isArabic, "وقت السداد", "Due Time"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatTime12Hour(dueTime, isArabic), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                // Step 2: Lead Time Dropdown + Alert Mode Dropdown
                Text(
                    text = tr(isArabic, "٢. التوقيت وطريقة التنبيه", "2. Lead Time & Alert Style"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmartSingleSelectDropdown(
                        label = tr(isArabic, "نبهني قبل الموعد بـ", "Notify Before"),
                        selectedValue = leadMinutes,
                        options = leadOptions,
                        isArabic = isArabic,
                        onSelected = { leadMinutes = it },
                        modifier = Modifier.weight(1f),
                        isHighlighted = enabled,
                        testTag = "item_reminder_lead_dropdown"
                    )
                    SmartSingleSelectDropdown(
                        label = tr(isArabic, "نوع التنبيه", "Alert Type"),
                        selectedValue = alertMode,
                        options = modeOptions,
                        isArabic = isArabic,
                        onSelected = {
                            alertMode = it
                            if (it == ReminderAlertMode.NOTIFICATION_VOICE) {
                                speakAloud = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        isHighlighted = enabled,
                        testTag = "item_reminder_mode_dropdown"
                    )
                }

                // Step 3: Full-Screen & Voice TTS Toggles
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.cardSubtleBackground,
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                Icon(Icons.Outlined.Smartphone, contentDescription = null, tint = ext.warningAmber, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(
                                        text = tr(isArabic, "إشعار بملء الشاشة", "Full-Screen Alert"),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = tr(isArabic, "يعرض المبلغ والتفاصيل على كامل الشاشة", "Displays full amount & details on screen"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = fullScreen,
                                onCheckedChange = { fullScreen = it },
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
                                Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, tint = ext.incomeColor, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(
                                        text = tr(isArabic, "نطق اسم المبلغ والتفاصيل بالصوت", "Speak Amount & Details Aloud"),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = tr(isArabic, "قراءة صوتية تلقائية عند حلول التنبيه", "Automatic voice announcement at alert time"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = speakAloud,
                                onCheckedChange = { speakAloud = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ext.incomeColor)
                            )
                        }
                    }
                }

                // Live Preview Button
                OutlinedButton(
                    onClick = {
                        PaymentNotificationManager.triggerTestAlarmPreview(
                            context = context,
                            settings = settings,
                            isArabic = isArabic,
                            customTitle = itemTitle,
                            customAmountText = amountDisplay,
                            customParty = partyName,
                            customDueInfo = "$dueDate ${formatTime12Hour(dueTime, isArabic)}",
                            alertModeOverride = alertMode,
                            fullScreenOverride = fullScreen,
                            speakAloudOverride = speakAloud
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ext.warningAmber.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = ext.warningAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(isArabic, "تجربة التنبيه والصوت لهذه العملية الآن", "Test Alert & Voice Now"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ext.warningAmber
                    )
                }

                // Save / Cancel Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(tr(isArabic, "إلغاء", "Cancel"))
                    }
                    Button(
                        onClick = {
                            onSave(
                                ItemReminderConfig(
                                    enabled = enabled,
                                    dueDate = dueDate,
                                    dueTime = dueTime,
                                    leadMinutes = leadMinutes,
                                    muted = !enabled,
                                    alertMode = alertMode,
                                    fullScreen = fullScreen,
                                    speakAloud = speakAloud
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("item_reminder_save_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.incomeColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(tr(isArabic, "حفظ التنبيه ✓", "Save Alert ✓"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        FastTimePickerDialog(
            initialTime24 = dueTime,
            isArabic = isArabic,
            title = tr(isArabic, "اختر وقت السداد", "Select Due Time"),
            onConfirm = { picked ->
                dueTime = picked
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}
