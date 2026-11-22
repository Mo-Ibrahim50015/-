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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LocalSereneColors
import com.example.util.currentTime24
import com.example.util.formatTime12Hour
import com.example.util.tr
import java.util.Locale

/**
 * Pure Jetpack Compose zero-lag Time Picker dialog.
 * Replaces legacy android.app.TimePickerDialog which freezes/lags in manual keyboard mode.
 * Supports smooth manual hour/minute typing, +/- steppers, AM/PM toggle, and instant quick presets.
 */
@Composable
fun FastTimePickerDialog(
    initialTime24: String,
    isArabic: Boolean,
    title: String = tr(isArabic, "اختر الوقت", "Select Time"),
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalSereneColors.current
    val parsedParts = remember(initialTime24) {
        val parts = initialTime24.split(":")
        val h24 = (parts.getOrNull(0)?.toIntOrNull() ?: 9).coerceIn(0, 23)
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)
        val isPmInit = h24 >= 12
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        Triple(h12, m, isPmInit)
    }

    var hourText by remember { mutableStateOf(parsedParts.first.toString()) }
    var minuteText by remember { mutableStateOf(String.format(Locale.US, "%02d", parsedParts.second)) }
    var isPm by remember { mutableStateOf(parsedParts.third) }

    fun currentHour12(): Int = (hourText.toIntOrNull() ?: 12).coerceIn(1, 12)
    fun currentMinute(): Int = (minuteText.toIntOrNull() ?: 0).coerceIn(0, 59)

    fun toTime24(): String {
        val h12 = currentHour12()
        val m = currentMinute()
        val h24 = when {
            !isPm && h12 == 12 -> 0
            isPm && h12 == 12 -> 12
            isPm -> h12 + 12
            else -> h12
        }
        return String.format(Locale.US, "%02d:%02d", h24, m)
    }

    fun apply24(time24: String) {
        val p = time24.split(":")
        val h24 = (p.getOrNull(0)?.toIntOrNull() ?: 9).coerceIn(0, 23)
        val m = (p.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)
        isPm = h24 >= 12
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        hourText = h12.toString()
        minuteText = String.format(Locale.US, "%02d", m)
    }

    val preview24 = toTime24()
    val presets = listOf(
        "09:00" to tr(isArabic, "٩:٠٠ ص", "9:00 AM"),
        "12:00" to tr(isArabic, "١٢:٠٠ ظ", "12:00 PM"),
        "15:00" to tr(isArabic, "٣:٠٠ م", "3:00 PM"),
        "18:00" to tr(isArabic, "٦:٠٠ م", "6:00 PM"),
        "21:00" to tr(isArabic, "٩:٠٠ م", "9:00 PM")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fast_time_picker_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ext.incomeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = ext.incomeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatTime12Hour(preview24, isArabic),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ext.incomeColor
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { apply24(currentTime24()) },
                        shape = RoundedCornerShape(10.dp),
                        color = ext.cardSubtleBackground
                    ) {
                        Text(
                            text = tr(isArabic, "الآن", "Now"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Manual Hour & Minute Fields + Stepper Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour Column
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tr(isArabic, "الساعة (١-١٢)", "Hour (1-12)"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(2)
                                val num = digits.toIntOrNull()
                                hourText = when {
                                    digits.isEmpty() -> ""
                                    num != null && num in 0..23 -> {
                                        if (num > 12) {
                                            isPm = true
                                            (num - 12).toString()
                                        } else {
                                            digits
                                        }
                                    }
                                    else -> hourText
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ext.cardSubtleBackground,
                                unfocusedContainerColor = ext.cardSubtleBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fast_time_hour_input")
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val next = if (currentHour12() <= 1) 12 else currentHour12() - 1
                                    hourText = next.toString()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Outlined.Remove, contentDescription = tr(isArabic, "تقليل الساعة", "Decrease hour"), modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    val next = if (currentHour12() >= 12) 1 else currentHour12() + 1
                                    hourText = next.toString()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = tr(isArabic, "زيادة الساعة", "Increase hour"), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Minute Column
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tr(isArabic, "الدقيقة (٠٠-٥٩)", "Minute (00-59)"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(2)
                                val num = digits.toIntOrNull()
                                minuteText = when {
                                    digits.isEmpty() -> ""
                                    num != null && num in 0..59 -> digits
                                    else -> minuteText
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ext.cardSubtleBackground,
                                unfocusedContainerColor = ext.cardSubtleBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fast_time_minute_input")
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val next = (currentMinute() + 55) % 60
                                    minuteText = String.format(Locale.US, "%02d", next)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Outlined.Remove, contentDescription = tr(isArabic, "تقليل ٥ دقائق", "Decrease 5 min"), modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    val next = (currentMinute() + 5) % 60
                                    minuteText = String.format(Locale.US, "%02d", next)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = tr(isArabic, "زيادة ٥ دقائق", "Increase 5 min"), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // AM / PM Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("fast_time_am_btn")
                            .clickable { isPm = false },
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isPm) ext.heroCardBackground else ext.cardSubtleBackground,
                        border = BorderStroke(
                            1.dp,
                            if (!isPm) ext.heroCardBackground else ext.cardBorder
                        )
                    ) {
                        Text(
                            text = tr(isArabic, "صباحاً (ص)", "AM"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (!isPm) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 11.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("fast_time_pm_btn")
                            .clickable { isPm = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPm) ext.heroCardBackground else ext.cardSubtleBackground,
                        border = BorderStroke(
                            1.dp,
                            if (isPm) ext.heroCardBackground else ext.cardBorder
                        )
                    ) {
                        Text(
                            text = tr(isArabic, "مساءً (م)", "PM"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isPm) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 11.dp)
                        )
                    }
                }

                // Quick Time Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (t24, label) ->
                        val active = preview24 == t24
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { apply24(t24) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) ext.incomeContainer else ext.cardSubtleBackground,
                            border = BorderStroke(1.dp, if (active) ext.incomeColor else ext.cardBorder)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (active) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Confirm / Cancel Buttons
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
                        onClick = { onConfirm(toTime24()) },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("fast_time_confirm_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.incomeColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = tr(isArabic, "تأكيد الوقت ✓", "Set Time ✓"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
