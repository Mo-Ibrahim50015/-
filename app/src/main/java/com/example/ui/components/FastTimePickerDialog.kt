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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalSereneColors
import com.example.util.currentTime24
import com.example.util.formatTime12Hour
import com.example.util.tr
import java.util.Locale

private fun normalizeToAsciiDigits(raw: String): String {
    val sb = StringBuilder(raw.length)
    for (ch in raw) {
        when (ch) {
            in '0'..'9' -> sb.append(ch)
            in '\u0660'..'\u0669' -> sb.append(('0'.code + (ch.code - '\u0660'.code)).toChar())
            in '\u06F0'..'\u06F9' -> sb.append(('0'.code + (ch.code - '\u06F0'.code)).toChar())
        }
    }
    return sb.toString()
}

/**
 * Pure Jetpack Compose zero-lag Time Picker dialog.
 * - Top-anchored AM/PM («صباحاً / مساءً») toggle always visible even while the soft keyboard is open.
 * - Full Arabic-Indic (٠-٩) & ASCII (0-9) manual keyboard input with auto-select on focus and rolling 2-digit entry.
 * - Natural LTR HH : MM column order even in Arabic RTL mode.
 * - Scrollable + IME-aware layout so no button is ever hidden behind the keyboard.
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val minuteFocusRequester = remember { FocusRequester() }

    val parsedParts = remember(initialTime24) {
        val clean = normalizeToAsciiDigits(initialTime24.replace(":", ":")).let {
            val parts = initialTime24.split(":")
            val hRaw = normalizeToAsciiDigits(parts.getOrNull(0).orEmpty()).toIntOrNull() ?: 9
            val mRaw = normalizeToAsciiDigits(parts.getOrNull(1).orEmpty()).toIntOrNull() ?: 0
            hRaw.coerceIn(0, 23) to mRaw.coerceIn(0, 59)
        }
        val h24 = clean.first
        val m = clean.second
        val isPmInit = h24 >= 12
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        Triple(h12, m, isPmInit)
    }

    var hourField by remember(initialTime24) {
        val text = String.format(Locale.US, "%02d", parsedParts.first)
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    var minuteField by remember(initialTime24) {
        val text = String.format(Locale.US, "%02d", parsedParts.second)
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    var isPm by remember(initialTime24) { mutableStateOf(parsedParts.third) }

    fun currentHour12(): Int {
        val n = normalizeToAsciiDigits(hourField.text).toIntOrNull()
        return when {
            n == null || n == 0 -> 12
            n > 12 -> (n - 12).coerceIn(1, 12)
            else -> n.coerceIn(1, 12)
        }
    }

    fun currentMinute(): Int {
        return (normalizeToAsciiDigits(minuteField.text).toIntOrNull() ?: 0).coerceIn(0, 59)
    }

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
        val h24 = (normalizeToAsciiDigits(p.getOrNull(0).orEmpty()).toIntOrNull() ?: 9).coerceIn(0, 23)
        val m = (normalizeToAsciiDigits(p.getOrNull(1).orEmpty()).toIntOrNull() ?: 0).coerceIn(0, 59)
        isPm = h24 >= 12
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        val hStr = String.format(Locale.US, "%02d", h12)
        val mStr = String.format(Locale.US, "%02d", m)
        hourField = TextFieldValue(hStr, TextRange(hStr.length))
        minuteField = TextFieldValue(mStr, TextRange(mStr.length))
    }

    fun dismissKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    val preview24 = toTime24()
    val presets = listOf(
        "09:00" to tr(isArabic, "٩:٠٠ ص", "9:00 AM"),
        "12:00" to tr(isArabic, "١٢:٠٠ م", "12:00 PM"),
        "15:00" to tr(isArabic, "٣:٠٠ م", "3:00 PM"),
        "18:00" to tr(isArabic, "٦:٠٠ م", "6:00 PM"),
        "21:00" to tr(isArabic, "٩:٠٠ م", "9:00 PM")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .testTag("fast_time_picker_dialog"),
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
                    // Header + Live Formatted 12h Preview + Now Button
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
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ext.incomeColor
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    dismissKeyboard()
                                    apply24(currentTime24())
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = ext.cardSubtleBackground,
                            border = BorderStroke(1.dp, ext.cardBorder)
                        ) {
                            Text(
                                text = tr(isArabic, "الآن", "Now"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    // TOP-ANCHORED AM / PM Selector (ALWAYS visible even while typing with soft keyboard!)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = tr(isArabic, "١. اختر الفترة (صباحاً أم مساءً):", "1. Select Period (AM or PM):"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .testTag("fast_time_am_btn")
                                    .clickable {
                                        isPm = false
                                    },
                                shape = RoundedCornerShape(13.dp),
                                color = if (!isPm) ext.warningAmber else ext.cardSubtleBackground,
                                border = BorderStroke(
                                    width = if (!isPm) 1.5.dp else 1.dp,
                                    color = if (!isPm) ext.warningAmber else ext.cardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.WbSunny,
                                        contentDescription = null,
                                        tint = if (!isPm) Color.White else ext.warningAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tr(isArabic, "صباحاً (ص)", "AM (Morning)"),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (!isPm) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .testTag("fast_time_pm_btn")
                                    .clickable {
                                        isPm = true
                                    },
                                shape = RoundedCornerShape(13.dp),
                                color = if (isPm) ext.heroCardBackground else ext.cardSubtleBackground,
                                border = BorderStroke(
                                    width = if (isPm) 1.5.dp else 1.dp,
                                    color = if (isPm) ext.heroCardBackground else ext.cardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.NightsStay,
                                        contentDescription = null,
                                        tint = if (isPm) ext.heroCardOnBackground else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tr(isArabic, "مساءً (م)", "PM (Evening)"),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (isPm) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Manual Hour & Minute Fields + Stepper Buttons in natural LTR (HH : MM) order
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = tr(isArabic, "٢. اكتب الساعة والدقيقة يدوياً أو استخدم (+ / -):", "2. Type Hour & Minute or use (+ / -):"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hour Column (Left side, natural clock order)
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
                                        value = hourField,
                                        onValueChange = { incoming ->
                                            val digits = normalizeToAsciiDigits(incoming.text)
                                            if (digits.isEmpty()) {
                                                hourField = TextFieldValue("", TextRange(0))
                                            } else {
                                                // Rolling 2-digit entry so typing into "09" or "12" NEVER gets stuck!
                                                val candidate = if (digits.length <= 2) digits else digits.takeLast(2)
                                                val num = candidate.toIntOrNull()
                                                val resolved = when {
                                                    num != null && num in 0..12 -> candidate
                                                    num != null && num in 13..23 -> {
                                                        isPm = true
                                                        (num - 12).toString()
                                                    }
                                                    else -> digits.takeLast(1)
                                                }
                                                hourField = TextFieldValue(resolved, TextRange(resolved.length))
                                            }
                                        },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { minuteFocusRequester.requestFocus() }
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = ext.cardSubtleBackground,
                                            unfocusedContainerColor = ext.cardSubtleBackground
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { st ->
                                                if (st.isFocused && hourField.text.isNotEmpty()) {
                                                    hourField = hourField.copy(
                                                        selection = TextRange(0, hourField.text.length)
                                                    )
                                                }
                                            }
                                            .testTag("fast_time_hour_input")
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                dismissKeyboard()
                                                val next = if (currentHour12() <= 1) 12 else currentHour12() - 1
                                                val s = String.format(Locale.US, "%02d", next)
                                                hourField = TextFieldValue(s, TextRange(s.length))
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Outlined.Remove, contentDescription = tr(isArabic, "تقليل الساعة", "Decrease hour"), modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                dismissKeyboard()
                                                val next = if (currentHour12() >= 12) 1 else currentHour12() + 1
                                                val s = String.format(Locale.US, "%02d", next)
                                                hourField = TextFieldValue(s, TextRange(s.length))
                                            },
                                            modifier = Modifier.size(40.dp)
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

                                // Minute Column (Right side, natural clock order)
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
                                        value = minuteField,
                                        onValueChange = { incoming ->
                                            val digits = normalizeToAsciiDigits(incoming.text)
                                            if (digits.isEmpty()) {
                                                minuteField = TextFieldValue("", TextRange(0))
                                            } else {
                                                // Rolling 2-digit entry so typing into "00" smoothly produces "03" -> "35"
                                                val candidate = if (digits.length <= 2) digits else digits.takeLast(2)
                                                val num = candidate.toIntOrNull()
                                                val resolved = if (num != null && num in 0..59) {
                                                    candidate
                                                } else {
                                                    digits.takeLast(1)
                                                }
                                                minuteField = TextFieldValue(resolved, TextRange(resolved.length))
                                            }
                                        },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { dismissKeyboard() }
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = ext.cardSubtleBackground,
                                            unfocusedContainerColor = ext.cardSubtleBackground
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(minuteFocusRequester)
                                            .onFocusChanged { st ->
                                                if (st.isFocused && minuteField.text.isNotEmpty()) {
                                                    minuteField = minuteField.copy(
                                                        selection = TextRange(0, minuteField.text.length)
                                                    )
                                                }
                                            }
                                            .testTag("fast_time_minute_input")
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                dismissKeyboard()
                                                val next = (currentMinute() + 59) % 60
                                                val s = String.format(Locale.US, "%02d", next)
                                                minuteField = TextFieldValue(s, TextRange(s.length))
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Outlined.Remove, contentDescription = tr(isArabic, "تقليل دقيقة", "Decrease 1 min"), modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                dismissKeyboard()
                                                val next = (currentMinute() + 1) % 60
                                                val s = String.format(Locale.US, "%02d", next)
                                                minuteField = TextFieldValue(s, TextRange(s.length))
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = tr(isArabic, "زيادة دقيقة", "Increase 1 min"), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick Minute Pills (:00, :15, :30, :45)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 15, 30, 45).forEach { minOpt ->
                            val active = currentMinute() == minOpt
                            val label = String.format(Locale.US, ":%02d", minOpt)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        dismissKeyboard()
                                        val s = String.format(Locale.US, "%02d", minOpt)
                                        minuteField = TextFieldValue(s, TextRange(s.length))
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) ext.incomeContainer else ext.cardSubtleBackground,
                                border = BorderStroke(1.dp, if (active) ext.incomeColor else ext.cardBorder)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (active) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Quick Full-Time Presets
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
                                    .clickable {
                                        dismissKeyboard()
                                        apply24(t24)
                                    },
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
                            onClick = {
                                dismissKeyboard()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(tr(isArabic, "إلغاء", "Cancel"))
                        }
                        Button(
                            onClick = {
                                dismissKeyboard()
                                onConfirm(toTime24())
                            },
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
}
