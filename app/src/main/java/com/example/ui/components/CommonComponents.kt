package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.loadLocalAvatarImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.window.Dialog
import com.example.data.AppSettingsEntity
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.AppTab
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.formatDisplayDateTime
import com.example.util.iconForKey
import com.example.util.tr

@Composable
fun SereneCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    containerColor: Color = LocalSereneColors.current.cardBackground,
    borderColor: Color = LocalSereneColors.current.cardBorder.copy(alpha = 0.7f),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (LocalSereneColors.current.isDark) 0.dp else 1.5.dp
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun UserAvatarCircle(
    avatarPath: String,
    userName: String,
    size: Dp,
    modifier: Modifier = Modifier,
    fallbackIcon: ImageVector = Icons.Outlined.Person,
    contentDescription: String? = null
) {
    val ext = LocalSereneColors.current
    val avatarBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = avatarPath) {
        value = if (avatarPath.isBlank()) null
        else withContext(Dispatchers.IO) { loadLocalAvatarImageBitmap(avatarPath) }
    }
    val initialChar = remember(userName) {
        userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ext.incomeContainer)
            .border(1.dp, ext.incomeColor.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val bmp = avatarBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (initialChar.isNotEmpty()) {
            Text(
                text = initialChar,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ext.incomeColor,
                maxLines = 1
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = contentDescription,
                tint = ext.incomeColor,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

@Composable
fun MizanityTopBar(
    title: String,
    isDark: Boolean,
    isArabic: Boolean,
    hasAlertDot: Boolean,
    userName: String = "",
    userAvatarPath: String = "",
    onNotificationsClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onProfileClick: () -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            val isCompact = maxWidth < 380.dp
            val actionBtnSize = if (isCompact) 36.dp else 40.dp
            val iconSize = if (isCompact) 18.dp else 20.dp
            val gap = if (isCompact) 4.dp else 6.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = if (isCompact) 10.dp else 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Start: Back button (when on sub-screen) or Wallet emblem + Screen Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 7.dp else 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(actionBtnSize)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ext.heroCardBackground)
                            .then(if (onBackClick != null) Modifier.clickable(onClick = onBackClick) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (onBackClick != null) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = if (onBackClick != null) {
                                tr(isArabic, "رجوع", "Back")
                            } else {
                                tr(isArabic, "شعار مصروفي", "Masrofy Wallet")
                            },
                            tint = ext.heroCardOnBackground,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    Text(
                        text = title,
                        style = if (isCompact) {
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        },
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // End: Notifications, Dark Mode, Language (EN/عربي), Local Profile Avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    HeaderActionButton(
                        testTag = "header_notifications_btn",
                        size = actionBtnSize,
                        onClick = onNotificationsClick
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = tr(isArabic, "الإشعارات", "Notifications"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(iconSize)
                            )
                            if (hasAlertDot) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(ext.incomeColor)
                                )
                            }
                        }
                    }

                    HeaderActionButton(
                        testTag = "header_theme_btn",
                        size = actionBtnSize,
                        onClick = onToggleTheme
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = tr(isArabic, "الوضع الداكن", "Toggle Dark Mode"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(iconSize)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .height(actionBtnSize)
                            .testTag("header_lang_btn")
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleLanguage),
                        shape = RoundedCornerShape(12.dp),
                        color = ext.cardBackground,
                        border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.6f))
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = if (isCompact) 7.dp else 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "EN" else "عربي",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Local Profile Avatar (updates live with user's picked photo or initial)
                    UserAvatarCircle(
                        avatarPath = userAvatarPath,
                        userName = userName,
                        size = actionBtnSize,
                        contentDescription = tr(isArabic, "ملفي الشخصي", "Profile"),
                        modifier = Modifier
                            .testTag("header_profile_btn")
                            .clickable(onClick = onProfileClick)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderActionButton(
    testTag: String,
    size: Dp = 40.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .size(size)
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = ext.cardBackground,
        border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.6f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun MizanityBottomBar(
    currentTab: AppTab,
    isArabic: Boolean,
    onSelectTab: (AppTab) -> Unit
) {
    val ext = LocalSereneColors.current
    val activeShadeColor = ext.incomeColor.copy(alpha = if (ext.isDark) 0.28f else 0.18f)
    val activeBorderColor = ext.incomeColor.copy(alpha = if (ext.isDark) 0.60f else 0.45f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ext.cardBackground.copy(alpha = 0.98f),
        border = BorderStroke(0.5.dp, ext.cardBorder),
        shadowElevation = if (ext.isDark) 0.dp else 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(IntrinsicSize.Min)
                .heightIn(min = 74.dp)
                .padding(horizontal = 5.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            BottomNavItem(
                icon = Icons.Outlined.Home,
                selectedIcon = Icons.Filled.Home,
                label = tr(isArabic, "الرئيسية", "Home"),
                selected = currentTab == AppTab.HOME,
                tag = "nav_home",
                onClick = { onSelectTab(AppTab.HOME) }
            )

            // Prominent circular Add Transaction tab with visible active shading
            val addSelected = currentTab == AppTab.ADD
            val addBg by animateColorAsState(
                targetValue = if (addSelected) activeShadeColor else Color.Transparent,
                label = "nav_add_bg"
            )
            val addBorder by animateColorAsState(
                targetValue = if (addSelected) activeBorderColor else Color.Transparent,
                label = "nav_add_border"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("nav_add")
                    .clip(RoundedCornerShape(14.dp))
                    .background(addBg)
                    .border(1.dp, addBorder, RoundedCornerShape(14.dp))
                    .clickable { onSelectTab(AppTab.ADD) }
                    .padding(vertical = 5.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(if (addSelected) 24.dp else 0.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (addSelected) ext.incomeColor else Color.Transparent)
                )
                Spacer(modifier = Modifier.height(if (addSelected) 3.dp else 0.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .shadow(if (addSelected) 5.dp else 2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            if (addSelected) ext.incomeColor else ext.heroCardBackground
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircle,
                        contentDescription = tr(isArabic, "إضافة عملية", "Add Transaction"),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tr(isArabic, "إضافة", "Add"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (addSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (addSelected) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BottomNavItem(
                icon = Icons.Outlined.ReceiptLong,
                selectedIcon = Icons.Filled.ReceiptLong,
                label = tr(isArabic, "السجل", "Log"),
                selected = currentTab == AppTab.LOG,
                tag = "nav_log",
                onClick = { onSelectTab(AppTab.LOG) }
            )
            BottomNavItem(
                icon = Icons.Outlined.Analytics,
                selectedIcon = Icons.Filled.Analytics,
                label = tr(isArabic, "التقارير", "Reports"),
                selected = currentTab == AppTab.REPORTS,
                tag = "nav_reports",
                onClick = { onSelectTab(AppTab.REPORTS) }
            )
            BottomNavItem(
                icon = Icons.Outlined.Settings,
                selectedIcon = Icons.Filled.Settings,
                label = tr(isArabic, "الإعدادات", "Settings"),
                selected = currentTab == AppTab.SETTINGS,
                tag = "nav_settings",
                onClick = { onSelectTab(AppTab.SETTINGS) }
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomNavItem(
    icon: ImageVector,
    selectedIcon: ImageVector,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    val activeShadeColor = ext.incomeColor.copy(alpha = if (ext.isDark) 0.28f else 0.18f)
    val activeBorderColor = ext.incomeColor.copy(alpha = if (ext.isDark) 0.60f else 0.45f)

    val containerBg by animateColorAsState(
        targetValue = if (selected) activeShadeColor else Color.Transparent,
        label = "${tag}_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) activeBorderColor else Color.Transparent,
        label = "${tag}_border"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "${tag}_tint"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .testTag(tag)
            .clip(RoundedCornerShape(14.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(if (selected) 24.dp else 0.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected) ext.incomeColor else Color.Transparent)
        )
        Spacer(modifier = Modifier.height(if (selected) 4.dp else 2.dp))
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TransactionRowItem(
    tx: TransactionEntity,
    categories: List<CategoryEntity>,
    isArabic: Boolean,
    multiSelectActive: Boolean = false,
    isSelected: Boolean = false,
    showQuickDelete: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onToggleDebtSettled: ((Boolean) -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val ext = LocalSereneColors.current
    val isIncome = tx.type == TransactionType.INCOME
    val iconKey = remember(tx.category, isIncome, categories) {
        categories.firstOrNull { it.name == tx.category }?.iconKey
            ?: if (isIncome) "payments" else "category"
    }
    val amountText = remember(tx.amountMinor, tx.currency, isIncome, isArabic) {
        val sym = currencySymbol(tx.currency, isArabic)
        "${if (isIncome) "+" else "-"} ${formatAmountMinor(tx.amountMinor)} $sym"
    }
    val displayDateTime = remember(tx.date, tx.time, isArabic) {
        formatDisplayDateTime(tx.date, tx.time, isArabic)
    }
    val amountColor = if (isIncome) ext.incomeColor else ext.expenseColor
    val iconBg = if (isIncome) ext.incomeContainer else ext.expenseContainer
    val titleText = tx.itemName.ifBlank { tx.note.ifBlank { tx.category } }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                if (multiSelectActive && onToggleSelect != null) {
                    onToggleSelect()
                } else {
                    onClick?.invoke()
                }
            },
        shape = RoundedCornerShape(14.dp),
        color = ext.cardBackground,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = when {
                isSelected -> ext.expenseColor.copy(alpha = 0.55f)
                tx.isDebt && !tx.isDebtSettled -> (if (isIncome) ext.warningAmber else ext.expenseColor).copy(alpha = 0.4f)
                else -> ext.cardBorder.copy(alpha = 0.65f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (multiSelectActive && onToggleSelect != null) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(checkedColor = ext.expenseColor),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Category Icon Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForKey(iconKey),
                        contentDescription = tx.category,
                        tint = amountColor,
                        modifier = Modifier.size(21.dp)
                    )
                }

                // Title + DateTime + Category pill
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayDateTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ext.cardSubtleBackground)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tx.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Amount + Source
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = amountText,
                        style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                        color = amountColor,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tx.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (showQuickDelete && onDeleteClick != null) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(ext.cardSubtleBackground)
                            .clickable(onClick = onDeleteClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = tr(isArabic, "حذف العملية", "Delete"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Unified Debt Status Footer Strip (visible on all pages whenever transaction is a debt)
            if (tx.isDebt) {
                val badgeBg = when {
                    tx.isDebtSettled -> ext.incomeContainer
                    isIncome -> ext.warningAmber.copy(alpha = 0.16f)
                    else -> ext.expenseContainer
                }
                val badgeFg = when {
                    tx.isDebtSettled -> ext.incomeColor
                    isIncome -> ext.warningAmber
                    else -> ext.expenseColor
                }
                val debtTypeBadge = when {
                    tx.isDebtSettled && isIncome -> tr(isArabic, "مديونية إيرادات • تم السداد", "Income Debt • Settled")
                    tx.isDebtSettled -> tr(isArabic, "مديونية مصروفات • تم السداد", "Expense Debt • Settled")
                    isIncome -> tr(isArabic, "مديونية إيرادات دخلت لحسابي • مستحق السداد", "Income Debt • To Repay")
                    else -> tr(isArabic, "مديونية مصروفات • مستحق السداد", "Expense Debt • To Repay")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(badgeBg)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = debtTypeBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = badgeFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (tx.debtParty.isNotBlank()) {
                            Text(
                                text = tx.debtParty,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (onToggleDebtSettled != null) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .testTag("row_settle_debt_${tx.id}")
                                .clickable { onToggleDebtSettled(!tx.isDebtSettled) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (tx.isDebtSettled) ext.cardSubtleBackground else ext.incomeContainer
                        ) {
                            Text(
                                text = if (tx.isDebtSettled) {
                                    tr(isArabic, "إعادة كدين", "Reopen")
                                } else {
                                    tr(isArabic, "سداد الآن", "Settle")
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (tx.isDebtSettled) MaterialTheme.colorScheme.onSurfaceVariant else ext.incomeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    description: String,
    isArabic: Boolean,
    requireDoubleConfirm: Boolean = false,
    pinRequired: Boolean = false,
    expectedPin: String = "",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalSereneColors.current
    var step by remember { mutableStateOf(1) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ext.cardBackground,
            border = BorderStroke(1.dp, ext.cardBorder),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ext.expenseContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (requireDoubleConfirm) Icons.Outlined.Warning else Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = ext.expenseColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (step == 2) tr(isArabic, "تأكيد نهائي أخير!", "Final Confirmation!") else title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (step == 2) {
                        tr(
                            isArabic,
                            "هذا الإجراء لا يمكن التراجع عنه أبداً. هل تريد بالتأكيد تصفير ومسح جميع العمليات الآن؟",
                            "This action cannot be undone. Are you completely sure you want to wipe all data?"
                        )
                    } else description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (pinRequired && expectedPin.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ext.cardSubtleBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = ext.incomeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(isArabic, "أدخل رمز PIN للموافقة", "Enter PIN to confirm"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all(Char::isDigit)) {
                                pinInput = it
                                pinError = false
                            }
                        },
                        singleLine = true,
                        isError = pinError,
                        placeholder = { Text("••••") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Text(
                            text = tr(isArabic, "رمز PIN غير صحيح", "Incorrect PIN"),
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.expenseColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (requireDoubleConfirm && step == 1) {
                            step = 2
                            return@Button
                        }
                        if (pinRequired && expectedPin.isNotBlank() && pinInput != expectedPin) {
                            pinError = true
                            return@Button
                        }
                        onConfirm()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_delete_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ext.expenseColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (requireDoubleConfirm && step == 1) {
                            tr(isArabic, "متابعة للتأكيد النهائي", "Continue to Final Confirm")
                        } else {
                            tr(isArabic, "نعم، حذف نهائي", "Yes, Delete Permanently")
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ext.cardSubtleBackground,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(tr(isArabic, "إلغاء الأمر", "Cancel"), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    expectedPin: String,
    isArabic: Boolean,
    onUnlocked: () -> Unit
) {
    val ext = LocalSereneColors.current
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        SereneCard(contentPadding = PaddingValues(24.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ext.heroCardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = ext.heroCardOnBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = tr(isArabic, "مصروفي محمي برمز مرور", "Masrofy PIN Lock"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tr(isArabic, "أدخل رمز PIN للمتابعة بأمان", "Enter your PIN to continue securely"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(18.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) {
                            pin = it
                            isError = false
                            if (it == expectedPin) onUnlocked()
                        }
                    },
                    singleLine = true,
                    isError = isError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    placeholder = { Text("••••") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = tr(isArabic, "رمز المرور غير صحيح", "Invalid PIN"),
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.expenseColor,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (pin == expectedPin) onUnlocked() else isError = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ext.heroCardBackground,
                        contentColor = ext.heroCardOnBackground
                    )
                ) {
                    Text(tr(isArabic, "فتح التطبيق", "Unlock App"))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    settings: AppSettingsEntity,
    todayCount: Int,
    monthExpenseMinor: Long,
    isArabic: Boolean,
    onGoToAdd: () -> Unit,
    onGoToSettingsReminders: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalSereneColors.current
    val sym = currencySymbol(settings.defaultCurrency, isArabic)
    val overBudget = settings.monthlyBudgetLimitMinor > 0 && monthExpenseMinor >= settings.monthlyBudgetLimitMinor

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ext.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = ext.incomeColor)
                Text(
                    text = tr(isArabic, "التنبيهات والتذكير المالي", "Financial Alerts & Reminders"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Daily check-in card
            SereneCard(containerColor = ext.cardSubtleBackground) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (todayCount > 0) Icons.Outlined.CheckCircle else Icons.Outlined.Spa,
                        contentDescription = null,
                        tint = ext.incomeColor
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (todayCount > 0) {
                                tr(isArabic, "أحسنت! سجّلت $todayCount عملية اليوم", "Great! $todayCount logged today")
                            } else {
                                tr(isArabic, "تذكير يومي (${settings.dailyReminderTime})", "Daily Reminder (${settings.dailyReminderTime})")
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (todayCount > 0) {
                                tr(isArabic, "متابعة يومية منتظمة تحافظ على صفاء ميزانيتك.", "Consistent tracking keeps your budget serene.")
                            } else {
                                tr(isArabic, "لم تسجّل مصاريف اليوم بعد. سجّل حركاتك في أقل من 10 ثوانٍ.", "No transactions today yet. Log one in seconds.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Budget limit status if configured
            if (settings.monthlyBudgetLimitMinor > 0) {
                SereneCard(
                    containerColor = if (overBudget) ext.expenseContainer else ext.incomeContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (overBudget) Icons.Outlined.Warning else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = if (overBudget) ext.expenseColor else ext.incomeColor
                        )
                        Column {
                            Text(
                                text = if (overBudget) {
                                    tr(isArabic, "تنبيه: تجاوزت حد الميزانية الشهرية!", "Alert: Monthly budget limit reached!")
                                } else {
                                    tr(isArabic, "الميزانية الشهرية ضمن النطاق الآمن", "Monthly budget is on track")
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (overBudget) ext.expenseColor else ext.incomeColor
                            )
                            Text(
                                text = "${formatAmountMinor(monthExpenseMinor)} / ${formatAmountMinor(settings.monthlyBudgetLimitMinor)} $sym",
                                style = AmountBodyStyle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onGoToAdd()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ext.heroCardBackground,
                        contentColor = ext.heroCardOnBackground
                    )
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr(isArabic, "إضافة عملية", "Add Entry"))
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        onGoToSettingsReminders()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr(isArabic, "ضبط التذكير", "Configure"))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
