package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pattern
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AppSettingsEntity
import com.example.ui.theme.LocalSereneColors
import com.example.util.launchDeviceBiometricOrCredentialPrompt
import com.example.util.tr
import kotlin.math.hypot

private enum class AuthTab { BIOMETRIC, PATTERN, PIN }

/**
 * Interactive 3x3 Pattern Lock Pad supporting smooth drag drawing across 9 nodes.
 * Emits dash-separated node indices (e.g. "0-1-2-5-8") when finger lifts with >= 4 dots.
 */
@Composable
fun PatternLockGrid(
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onPatternComplete: (String) -> Unit
) {
    val ext = LocalSereneColors.current
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentPointer by remember { mutableStateOf<Offset?>(null) }

    val lineColor = if (isError) ext.expenseColor else ext.incomeColor
    val inactiveDotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val surfaceRingColor = ext.cardSubtleBackground

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(surfaceRingColor)
            .testTag("pattern_lock_grid"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .pointerInput(Unit) {
                    fun nodeCenters(): List<Offset> {
                        val stepX = size.width / 3f
                        val stepY = size.height / 3f
                        return buildList(9) {
                            for (row in 0..2) {
                                for (col in 0..2) {
                                    add(Offset(stepX * col + stepX / 2f, stepY * row + stepY / 2f))
                                }
                            }
                        }
                    }

                    fun hitNode(pos: Offset, centers: List<Offset>): Int? {
                        val hitRadius = minOf(size.width, size.height) / 7.5f
                        centers.forEachIndexed { index, c ->
                            if (hypot((pos.x - c.x).toDouble(), (pos.y - c.y).toDouble()) <= hitRadius) {
                                return index
                            }
                        }
                        return null
                    }

                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedNodes.clear()
                            currentPointer = offset
                            val centers = nodeCenters()
                            hitNode(offset, centers)?.let { selectedNodes.add(it) }
                        },
                        onDrag = { change, _ ->
                            val pos = change.position
                            currentPointer = pos
                            val centers = nodeCenters()
                            val hit = hitNode(pos, centers)
                            if (hit != null && hit !in selectedNodes) {
                                selectedNodes.add(hit)
                            }
                        },
                        onDragEnd = {
                            currentPointer = null
                            if (selectedNodes.isNotEmpty()) {
                                val pattern = selectedNodes.joinToString("-")
                                selectedNodes.clear()
                                onPatternComplete(pattern)
                            }
                        },
                        onDragCancel = {
                            currentPointer = null
                            selectedNodes.clear()
                        }
                    )
                }
        ) {
            val stepX = size.width / 3f
            val stepY = size.height / 3f
            val centers = buildList(9) {
                for (row in 0..2) {
                    for (col in 0..2) {
                        add(Offset(stepX * col + stepX / 2f, stepY * row + stepY / 2f))
                    }
                }
            }

            // Draw connecting lines between selected nodes
            if (selectedNodes.size >= 2) {
                for (i in 0 until selectedNodes.size - 1) {
                    val start = centers[selectedNodes[i]]
                    val end = centers[selectedNodes[i + 1]]
                    drawLine(
                        color = lineColor,
                        start = start,
                        end = end,
                        strokeWidth = 9f,
                        cap = StrokeCap.Round
                    )
                }
            }
            // Active trailing line to finger
            if (selectedNodes.isNotEmpty() && currentPointer != null) {
                drawLine(
                    color = lineColor.copy(alpha = 0.55f),
                    start = centers[selectedNodes.last()],
                    end = currentPointer!!,
                    strokeWidth = 7f,
                    cap = StrokeCap.Round
                )
            }

            // Draw 9 nodes
            centers.forEachIndexed { idx, center ->
                val active = idx in selectedNodes
                if (active) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.22f),
                        radius = 30f,
                        center = center
                    )
                }
                drawCircle(
                    color = if (active) lineColor else inactiveDotColor,
                    radius = if (active) 13f else 9f,
                    center = center
                )
            }
        }
    }
}

/**
 * Two-step Pattern Setup dialog (Draw -> Confirm, minimum 4 connected dots).
 */
@Composable
fun PatternSetupDialog(
    isArabic: Boolean,
    onSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalSereneColors.current
    var firstPattern by remember { mutableStateOf<String?>(null) }
    var statusError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Pattern, contentDescription = null, tint = ext.incomeColor)
                Text(
                    text = if (firstPattern == null) {
                        tr(isArabic, "رسم النمط الخاص", "Draw Custom Pattern")
                    } else {
                        tr(isArabic, "أعد رسم النمط للتأكيد", "Confirm Your Pattern")
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = statusError ?: if (firstPattern == null) {
                        tr(isArabic, "صل ٤ نقاط على الأقل لتعيين نمط الحماية", "Connect at least 4 dots to set your pattern")
                    } else {
                        tr(isArabic, "ارسم نفس النمط مرة أخرى للتأكيد", "Draw the same pattern again to confirm")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusError != null) ext.expenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                PatternLockGrid(
                    modifier = Modifier.size(230.dp),
                    isError = statusError != null,
                    onPatternComplete = { drawn ->
                        val dots = drawn.split("-").size
                        if (dots < 4) {
                            statusError = tr(isArabic, "يجب توصيل ٤ نقاط على الأقل", "Connect at least 4 dots")
                        } else if (firstPattern == null) {
                            firstPattern = drawn
                            statusError = null
                        } else if (firstPattern == drawn) {
                            onSaved(drawn)
                        } else {
                            statusError = tr(isArabic, "النمط غير متطابق، حاول مرة أخرى", "Patterns do not match, try again")
                            firstPattern = null
                        }
                    }
                )
            }
        },
        confirmButton = {
            if (firstPattern != null) {
                TextButton(onClick = {
                    firstPattern = null
                    statusError = null
                }) {
                    Text(tr(isArabic, "إعادة الرسم", "Redraw"))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(isArabic, "إلغاء", "Cancel"))
            }
        }
    )
}

/**
 * Full-screen Security Gate shown BEFORE the Splash Video when launch lock is enabled.
 * Supports Fingerprint / Device Pattern, Custom 3x3 Pattern, and PIN.
 */
@Composable
fun AppLockGateScreen(
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onUnlocked: () -> Unit
) {
    val ext = LocalSereneColors.current
    val context = LocalContext.current

    val availableTabs = remember(settings) {
        buildList {
            if (settings.biometricEnabled) add(AuthTab.BIOMETRIC)
            if (settings.patternEnabled && settings.patternCode.isNotBlank()) add(AuthTab.PATTERN)
            if (settings.pinEnabled && settings.pinCode.length >= 4) add(AuthTab.PIN)
        }
    }
    var activeTab by remember(availableTabs) {
        mutableStateOf(availableTabs.firstOrNull() ?: AuthTab.PIN)
    }
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Auto-unlock immediately if user somehow has no lock method configured
    LaunchedEffect(availableTabs) {
        if (availableTabs.isEmpty()) onUnlocked()
    }

    // Auto-trigger Biometric / Device Credential prompt on launch after window settles
    LaunchedEffect(activeTab) {
        if (activeTab == AuthTab.BIOMETRIC && settings.biometricEnabled) {
            kotlinx.coroutines.delay(180L)
            launchDeviceBiometricOrCredentialPrompt(
                context = context,
                title = tr(isArabic, "فتح تطبيق مصروفي", "Unlock Masrofy"),
                subtitle = tr(
                    isArabic,
                    "تحقق بالبصمة أو نمط / قفل الجهاز للمتابعة",
                    "Verify with fingerprint or device pattern/lock"
                ),
                isArabic = isArabic,
                onSuccess = onUnlocked,
                onFailure = { msg -> errorMsg = msg }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        SereneCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .testTag("app_lock_gate"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(ext.heroCardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (activeTab) {
                            AuthTab.BIOMETRIC -> Icons.Outlined.Fingerprint
                            AuthTab.PATTERN -> Icons.Outlined.Pattern
                            AuthTab.PIN -> Icons.Outlined.Lock
                        },
                        contentDescription = null,
                        tint = Color(0xFF82F5C1),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = tr(isArabic, "تطبيق مصروفي محمي", "Masrofy is Locked"),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = tr(
                        isArabic,
                        "أكّد هويتك أولاً للدخول إلى بياناتك المالية",
                        "Verify your identity before entering your ledger"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Method switcher pills if multiple lock methods are active
                if (availableTabs.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableTabs.forEach { tab ->
                            val chosen = activeTab == tab
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        activeTab = tab
                                        errorMsg = null
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (chosen) ext.heroCardBackground else ext.cardSubtleBackground
                            ) {
                                Text(
                                    text = when (tab) {
                                        AuthTab.BIOMETRIC -> tr(isArabic, "البصمة", "Biometric")
                                        AuthTab.PATTERN -> tr(isArabic, "النمط", "Pattern")
                                        AuthTab.PIN -> "PIN"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (chosen) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                when (activeTab) {
                    AuthTab.BIOMETRIC -> {
                        Button(
                            onClick = {
                                errorMsg = null
                                launchDeviceBiometricOrCredentialPrompt(
                                    context = context,
                                    title = tr(isArabic, "فتح تطبيق مصروفي", "Unlock Masrofy"),
                                    subtitle = tr(
                                        isArabic,
                                        "استخدم البصمة أو نمط الجهاز للفتح",
                                        "Use fingerprint or device lock"
                                    ),
                                    isArabic = isArabic,
                                    onSuccess = onUnlocked,
                                    onFailure = { msg -> errorMsg = msg }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("lock_biometric_trigger_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ext.heroCardBackground,
                                contentColor = ext.heroCardOnBackground
                            )
                        ) {
                            Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(isArabic, "تحقق بالبصمة أو نمط الجهاز", "Unlock with Fingerprint / Device"),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    AuthTab.PATTERN -> {
                        PatternLockGrid(
                            modifier = Modifier.size(230.dp),
                            isError = errorMsg != null,
                            onPatternComplete = { drawn ->
                                if (drawn == settings.patternCode) {
                                    errorMsg = null
                                    onUnlocked()
                                } else {
                                    errorMsg = tr(isArabic, "النمط غير صحيح، حاول مرة أخرى", "Incorrect pattern, try again")
                                }
                            }
                        )
                    }

                    AuthTab.PIN -> {
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { raw ->
                                pinInput = raw.filter { it.isDigit() }.take(6)
                                errorMsg = null
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            placeholder = { Text(tr(isArabic, "أدخل رمز PIN", "Enter PIN")) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lock_pin_input")
                        )
                        Button(
                            onClick = {
                                if (pinInput == settings.pinCode) {
                                    errorMsg = null
                                    onUnlocked()
                                } else {
                                    errorMsg = tr(isArabic, "رمز PIN غير صحيح", "Incorrect PIN")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("lock_pin_submit_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ext.heroCardBackground,
                                contentColor = ext.heroCardOnBackground
                            )
                        ) {
                            Text(tr(isArabic, "فتح التطبيق", "Unlock"))
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = ext.expenseColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Unified Security Verification Dialog used for Debt Payment («دفع») and Sensitive Actions
 * whenever Biometric/Device Lock, Custom Pattern, or PIN protection is enabled.
 */
@Composable
fun SecurityVerificationDialog(
    title: String,
    description: String,
    confirmLabel: String,
    isDanger: Boolean = false,
    requireSecurity: Boolean,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalSereneColors.current
    val context = LocalContext.current

    val effectiveSecurity = requireSecurity && settings.hasAnyLockMethodConfigured()
    val availableTabs = remember(settings) {
        buildList {
            if (settings.biometricEnabled) add(AuthTab.BIOMETRIC)
            if (settings.patternEnabled && settings.patternCode.isNotBlank()) add(AuthTab.PATTERN)
            if (settings.pinEnabled && settings.pinCode.length >= 4) add(AuthTab.PIN)
        }
    }
    var activeTab by remember(availableTabs) {
        mutableStateOf(availableTabs.firstOrNull() ?: AuthTab.PIN)
    }
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val accent = if (isDanger) ext.expenseColor else ext.incomeColor

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (effectiveSecurity) Icons.Outlined.VerifiedUser else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = accent
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                if (effectiveSecurity) {
                    if (availableTabs.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableTabs.forEach { tab ->
                                val chosen = activeTab == tab
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(9.dp))
                                        .clickable {
                                            activeTab = tab
                                            errorMsg = null
                                        },
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (chosen) ext.heroCardBackground else ext.cardSubtleBackground
                                ) {
                                    Text(
                                        text = when (tab) {
                                            AuthTab.BIOMETRIC -> tr(isArabic, "البصمة", "Biometric")
                                            AuthTab.PATTERN -> tr(isArabic, "النمط", "Pattern")
                                            AuthTab.PIN -> "PIN"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (chosen) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    when (activeTab) {
                        AuthTab.BIOMETRIC -> {
                            OutlinedButton(
                                onClick = {
                                    errorMsg = null
                                    launchDeviceBiometricOrCredentialPrompt(
                                        context = context,
                                        title = title,
                                        subtitle = tr(
                                            isArabic,
                                            "أكّد بالبصمة أو نمط الجهاز لإتمام العملية",
                                            "Verify with fingerprint or device lock to proceed"
                                        ),
                                        isArabic = isArabic,
                                        onSuccess = onConfirmed,
                                        onFailure = { msg -> errorMsg = msg }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("dialog_biometric_btn"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.2.dp, accent)
                            ) {
                                Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = accent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr(isArabic, "تأكيد بالبصمة / نمط الجهاز", "Verify Fingerprint / Device"),
                                    color = accent,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        AuthTab.PATTERN -> {
                            PatternLockGrid(
                                modifier = Modifier.size(210.dp),
                                isError = errorMsg != null,
                                onPatternComplete = { drawn ->
                                    if (drawn == settings.patternCode) {
                                        errorMsg = null
                                        onConfirmed()
                                    } else {
                                        errorMsg = tr(isArabic, "النمط غير صحيح", "Incorrect pattern")
                                    }
                                }
                            )
                        }

                        AuthTab.PIN -> {
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { raw ->
                                    pinInput = raw.filter { it.isDigit() }.take(6)
                                    errorMsg = null
                                },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                placeholder = { Text(tr(isArabic, "أدخل رمز PIN للتأكيد", "Enter PIN to confirm")) },
                                leadingIcon = { Icon(Icons.Outlined.Pin, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.expenseColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!effectiveSecurity || activeTab == AuthTab.PIN) {
                Button(
                    onClick = {
                        if (!effectiveSecurity) {
                            onConfirmed()
                        } else if (pinInput == settings.pinCode) {
                            onConfirmed()
                        } else {
                            errorMsg = tr(isArabic, "رمز PIN غير صحيح", "Incorrect PIN")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(confirmLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(isArabic, "إلغاء", "Cancel"))
            }
        }
    )
}
