package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.CloudBackupSummary
import com.example.ui.theme.LocalSereneColors
import com.example.util.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BackupDrawerMode {
    SAVE,
    RESTORE
}

@Composable
fun BackupSideDrawerOverlay(
    mode: BackupDrawerMode?,
    isArabic: Boolean,
    signedInEmail: String?,
    signedInName: String?,
    isAuthBusy: Boolean,
    isCloudBusy: Boolean,
    cloudSummary: CloudBackupSummary?,
    cloudError: String?,
    localTransactionCount: Int,
    onSwitchMode: (BackupDrawerMode) -> Unit,
    onChooseGoogleEmailSignIn: () -> Unit,
    onSwitchGoogleEmail: () -> Unit,
    onSignOutGoogle: () -> Unit,
    onRefreshCloudSummary: () -> Unit,
    onSaveCloudBackup: () -> Unit,
    onRestoreCloudBackup: () -> Unit,
    onSaveJsonDeviceFiles: () -> Unit,
    onRestoreJsonDeviceFiles: () -> Unit,
    onDownloadJsonDirect: () -> Unit,
    onSavePdfDeviceFiles: () -> Unit,
    onSaveExcelDeviceFiles: () -> Unit,
    onSaveImageDeviceFiles: () -> Unit,
    onDismiss: () -> Unit
) {
    val isOpen = mode != null
    val activeMode = mode ?: BackupDrawerMode.SAVE
    val ext = LocalSereneColors.current
    val layoutDir = LocalLayoutDirection.current
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    var showConfirmCloudRestore by remember { mutableStateOf(false) }

    BackHandler(enabled = isOpen) {
        onDismiss()
    }

    // Dimmed Scrim
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    // Sliding Side Drawer Panel (slides cleanly from start edge respecting RTL/LTR)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth ->
                    if (layoutDir == LayoutDirection.Rtl) fullWidth else -fullWidth
                }
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth ->
                    if (layoutDir == LayoutDirection.Rtl) fullWidth else -fullWidth
                }
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.88f)
                    .widthIn(max = 390.dp)
                    .testTag("backup_side_drawer_panel")
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(
                    topEnd = 24.dp,
                    bottomEnd = 24.dp
                ),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = systemBarsPadding.calculateTopPadding() + 12.dp,
                            bottom = systemBarsPadding.calculateBottomPadding() + 16.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ext.heroCardBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (activeMode == BackupDrawerMode.SAVE) {
                                        Icons.Outlined.CloudUpload
                                    } else {
                                        Icons.Outlined.CloudDownload
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFF82F5C1),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (activeMode == BackupDrawerMode.SAVE) {
                                        tr(isArabic, "حفظ نسخة احتياطية", "Save Backup")
                                    } else {
                                        tr(isArabic, "استعادة نسخة احتياطية", "Restore Backup")
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = tr(
                                        isArabic,
                                        "اختر الإيميل وسجّل الدخول أو استخدم ملفات الجهاز",
                                        "Choose your email to sign in or use device files"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("backup_drawer_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = tr(isArabic, "إغلاق النافذة الجانبية", "Close side drawer"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. Mode Switcher (Save / Restore)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ext.cardSubtleBackground)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DrawerModePill(
                            modifier = Modifier.weight(1f),
                            selected = activeMode == BackupDrawerMode.SAVE,
                            label = tr(isArabic, "حفظ نسخة", "Save Backup"),
                            icon = Icons.Outlined.CloudUpload,
                            tag = "backup_drawer_mode_save",
                            onClick = { onSwitchMode(BackupDrawerMode.SAVE) }
                        )
                        DrawerModePill(
                            modifier = Modifier.weight(1f),
                            selected = activeMode == BackupDrawerMode.RESTORE,
                            label = tr(isArabic, "استعادة نسخة", "Restore Backup"),
                            icon = Icons.Outlined.CloudDownload,
                            tag = "backup_drawer_mode_restore",
                            onClick = { onSwitchMode(BackupDrawerMode.RESTORE) }
                        )
                    }

                    // 3. Google Email Account Card (Interactive Email Picker + Cloud Backup/Restore)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = ext.cardSubtleBackground,
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MarkEmailRead,
                                    contentDescription = null,
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = tr(isArabic, "الحساب السحابي (اختيار الإيميل)", "Cloud Account (Choose Email)"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (signedInEmail.isNullOrBlank()) {
                                Text(
                                    text = tr(
                                        isArabic,
                                        "اضغط بالأسفل لاختيار إيميل جوجل الخاص بك وتسجيل الدخول. تُحفظ بياناتك الخاصة بهذا الإيميل فقط ولا يراها أي جهاز آخر.",
                                        "Tap below to choose your Google email and sign in. Your backup is private to your chosen email."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = onChooseGoogleEmailSignIn,
                                    enabled = !isAuthBusy && !isCloudBusy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .testTag("backup_drawer_google_signin_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ext.heroCardBackground,
                                        contentColor = ext.heroCardOnBackground
                                    )
                                ) {
                                    if (isAuthBusy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = ext.heroCardOnBackground
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(tr(isArabic, "جاري فتح قائمة الإيميلات...", "Opening email chooser..."))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = tr(isArabic, "اختيار الإيميل وتسجيل الدخول", "Choose Email & Sign In"),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            } else {
                                // Signed-in email badge
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = ext.incomeContainer.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, ext.incomeColor.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(ext.incomeColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.VerifiedUser,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            if (!signedInName.isNullOrBlank()) {
                                                Text(
                                                    text = signedInName,
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = signedInEmail,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = ext.incomeColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = onRefreshCloudSummary,
                                            enabled = !isCloudBusy,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Refresh,
                                                contentDescription = tr(isArabic, "تحديث حالة النسخة", "Refresh backup status"),
                                                tint = ext.incomeColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Cloud backup metadata status
                                val formattedTime = remember(cloudSummary?.updatedAt) {
                                    val date = cloudSummary?.updatedAt?.toDate()
                                    if (date != null) {
                                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(date)
                                    } else {
                                        null
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudDone,
                                        contentDescription = null,
                                        tint = if (cloudSummary != null) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (cloudSummary != null) {
                                            tr(
                                                isArabic,
                                                "النسخة السحابية: ${cloudSummary.transactionCount} عملية" +
                                                    if (formattedTime != null) " • $formattedTime" else "",
                                                "Cloud backup: ${cloudSummary.transactionCount} items" +
                                                    if (formattedTime != null) " • $formattedTime" else ""
                                            )
                                        } else {
                                            tr(
                                                isArabic,
                                                "لا توجد نسخة سحابية محفوظة لهذا الإيميل حتى الآن",
                                                "No cloud backup saved for this email yet"
                                            )
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Primary Cloud Action button
                                if (activeMode == BackupDrawerMode.SAVE) {
                                    Button(
                                        onClick = onSaveCloudBackup,
                                        enabled = !isCloudBusy && !isAuthBusy,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 50.dp)
                                            .testTag("backup_drawer_cloud_save_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ext.heroCardBackground,
                                            contentColor = ext.heroCardOnBackground
                                        )
                                    ) {
                                        if (isCloudBusy) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = ext.heroCardOnBackground
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(tr(isArabic, "جاري حفظ النسخة على الإيميل...", "Saving backup to email..."))
                                        } else {
                                            Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = tr(
                                                    isArabic,
                                                    "حفظ النسخة على الإيميل ($localTransactionCount عملية)",
                                                    "Save Backup to Email ($localTransactionCount items)"
                                                ),
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { showConfirmCloudRestore = true },
                                        enabled = !isCloudBusy && !isAuthBusy,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 50.dp)
                                            .testTag("backup_drawer_cloud_restore_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ext.incomeColor,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        if (isCloudBusy) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(tr(isArabic, "جاري استعادة البيانات...", "Restoring from email..."))
                                        } else {
                                            Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = tr(isArabic, "استعادة النسخة من هذا الإيميل", "Restore Backup from Email"),
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }

                                // Switch Email / Sign Out controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onSwitchGoogleEmail,
                                        enabled = !isAuthBusy && !isCloudBusy,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("backup_drawer_switch_email_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Outlined.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = tr(isArabic, "تغيير الإيميل", "Switch Email"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = onSignOutGoogle,
                                        enabled = !isAuthBusy && !isCloudBusy,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("backup_drawer_signout_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.expenseColor)
                                    ) {
                                        Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = tr(isArabic, "تسجيل الخروج", "Sign Out"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            if (!cloudError.isNullOrBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.expenseContainer
                                ) {
                                    Text(
                                        text = cloudError,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.expenseColor,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))

                    // 4. Local Device Files Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = tr(isArabic, "أو عبر ملفات الجهاز مباشرة (بدون إنترنت)", "Or via Local Device Files (Offline)"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (activeMode == BackupDrawerMode.SAVE) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onSaveJsonDeviceFiles()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("backup_drawer_local_save_json_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tr(isArabic, "حفظ ملف JSON في ملفات الجهاز", "Save JSON to Device Files"))
                            }

                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onDownloadJsonDirect()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("backup_drawer_local_download_json_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tr(isArabic, "تنزيل مباشر في Downloads/Masrofy", "Direct Download to Downloads/Masrofy"))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onSavePdfDeviceFiles()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PDF", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onSaveExcelDeviceFiles()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Outlined.TableView, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Excel", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onSaveImageDeviceFiles()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tr(isArabic, "صورة", "PNG"), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onRestoreJsonDeviceFiles()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("backup_drawer_local_restore_json_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tr(isArabic, "اختيار ملف JSON من ملفات الجهاز", "Pick JSON File from Device Files"))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmCloudRestore) {
        AlertDialog(
            onDismissRequest = { showConfirmCloudRestore = false },
            title = {
                Text(tr(isArabic, "تأكيد الاستعادة من الإيميل", "Confirm Cloud Restore"))
            },
            text = {
                Text(
                    tr(
                        isArabic,
                        "سيتم استبدال العمليات الحالية على هذا الهاتف بالنسخة الاحتياطية المحفوظة على الإيميل ($signedInEmail). هل تريد المتابعة؟",
                        "Current local transactions on this phone will be replaced with the cloud backup saved under ($signedInEmail). Continue?"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCloudRestore = false
                        onRestoreCloudBackup()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ext.incomeColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(tr(isArabic, "استعادة الآن", "Restore Now"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCloudRestore = false }) {
                    Text(tr(isArabic, "إلغاء", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun DrawerModePill(
    modifier: Modifier = Modifier,
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) ext.heroCardBackground else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color(0xFF82F5C1) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
