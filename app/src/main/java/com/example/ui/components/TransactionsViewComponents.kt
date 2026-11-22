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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.formatReminderLeadTime
import com.example.util.tr

/**
 * Compact small icon toggle switching seamlessly between Cards view ("cards") and Table view ("table").
 */
@Composable
fun ViewModeToggleIcon(
    viewMode: String,
    isArabic: Boolean,
    onToggle: (String) -> Unit,
    testTagPrefix: String = "tx_view_mode"
) {
    val ext = LocalSereneColors.current
    val isTable = viewMode == "table"

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ext.cardSubtleBackground,
        border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.7f)),
        modifier = Modifier.testTag("${testTagPrefix}_toggle")
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isTable) ext.heroCardBackground else Color.Transparent)
                    .testTag("${testTagPrefix}_cards")
                    .clickable { onToggle("cards") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ViewAgenda,
                    contentDescription = tr(isArabic, "عرض البطاقات", "Cards View"),
                    tint = if (!isTable) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isTable) ext.heroCardBackground else Color.Transparent)
                    .testTag("${testTagPrefix}_table")
                    .clickable { onToggle("table") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.TableRows,
                    contentDescription = tr(isArabic, "عرض الجدول", "Table View"),
                    tint = if (isTable) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Clean, horizontally-scrollable Material 3 Table View for transactions used across
 * Home, Log, Reports, Debts, Payments, and Income/Expense Details screens.
 */
@Composable
fun TransactionsTableView(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    isArabic: Boolean,
    multiSelectActive: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    showQuickDelete: Boolean = false,
    onToggleSelect: ((Long) -> Unit)? = null,
    onRowClick: ((TransactionEntity) -> Unit)? = null,
    onToggleDebtSettled: ((TransactionEntity) -> Unit)? = null,
    onToggleReminderMuted: ((Long, Boolean) -> Unit)? = null,
    onDeleteClick: ((TransactionEntity) -> Unit)? = null
) {
    val ext = LocalSereneColors.current
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transactions_table_view"),
        shape = RoundedCornerShape(14.dp),
        color = ext.cardBackground,
        border = BorderStroke(1.dp, ext.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            // Table Header
            Row(
                modifier = Modifier
                    .background(ext.heroCardBackground.copy(alpha = 0.94f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (multiSelectActive) {
                    Spacer(modifier = Modifier.width(26.dp))
                }
                TableHeaderCell(text = tr(isArabic, "البند / التصنيف", "Item / Category"), widthDp = 148)
                TableHeaderCell(text = tr(isArabic, "المبلغ", "Amount"), widthDp = 104)
                TableHeaderCell(text = tr(isArabic, "التاريخ والوقت", "Date & Time"), widthDp = 116)
                TableHeaderCell(text = tr(isArabic, "المصدر / الحساب", "Account"), widthDp = 112)
                TableHeaderCell(text = tr(isArabic, "الحالة / التنبيه", "Status / Alert"), widthDp = 148)
                if (onToggleDebtSettled != null || onToggleReminderMuted != null || (showQuickDelete && onDeleteClick != null)) {
                    TableHeaderCell(text = tr(isArabic, "إجراءات", "Actions"), widthDp = 96)
                }
            }

            // Table Rows
            transactions.forEachIndexed { idx, tx ->
                val isIncome = tx.type == TransactionType.INCOME
                val sym = currencySymbol(tx.currency, isArabic)
                val sign = if (isIncome) "+" else "-"
                val amountColor = if (isIncome) ext.incomeColor else ext.expenseColor
                val title = tx.itemName.ifBlank { tx.category }
                val rowBg = if (idx % 2 == 0) ext.cardBackground else ext.cardSubtleBackground.copy(alpha = 0.55f)

                Row(
                    modifier = Modifier
                        .background(rowBg)
                        .clickable(enabled = onRowClick != null || multiSelectActive) {
                            if (multiSelectActive) onToggleSelect?.invoke(tx.id)
                            else onRowClick?.invoke(tx)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("tx_table_row_${tx.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (multiSelectActive) {
                        Checkbox(
                            checked = tx.id in selectedIds,
                            onCheckedChange = { onToggleSelect?.invoke(tx.id) },
                            colors = CheckboxDefaults.colors(checkedColor = ext.incomeColor),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // 1. Item / Category
                    Row(
                        modifier = Modifier.width(148.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(amountColor)
                        )
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (tx.itemName.isNotBlank() && tx.category.isNotBlank()) {
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

                    // 2. Amount
                    Text(
                        text = "$sign${formatAmountMinor(tx.amountMinor, false)} $sym",
                        style = AmountBodyStyle.copy(
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = amountColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(104.dp)
                    )

                    // 3. Date & Time
                    Column(modifier = Modifier.width(116.dp)) {
                        Text(
                            text = tx.date,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = tx.time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    // 4. Source
                    Text(
                        text = tx.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(112.dp)
                    )

                    // 5. Status / Due Reminder
                    Column(
                        modifier = Modifier.width(148.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val statusText = when {
                            tx.isDebt && tx.isDebtSettled -> tr(isArabic, "مديونية مسددة ✓", "Paid Debt ✓")
                            tx.isDebt -> tr(isArabic, "مديونية قائمة", "Active Debt")
                            isIncome -> tr(isArabic, "إيراد مكتسب", "Income")
                            else -> tr(isArabic, "مصروف مدفوع", "Expense")
                        }
                        val statusColor = when {
                            tx.isDebt && tx.isDebtSettled -> ext.incomeColor
                            tx.isDebt -> ext.warningAmber
                            isIncome -> ext.incomeColor
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tx.hasPaymentReminder) {
                            val remColor = if (tx.reminderMuted) MaterialTheme.colorScheme.outline else ext.warningAmber
                            Text(
                                text = if (tx.reminderMuted) {
                                    tr(isArabic, "تنبيه مفصول • ${tx.paymentDueDate}", "Muted • ${tx.paymentDueDate}")
                                } else {
                                    "${tx.paymentDueDate} ${tx.paymentDueTime} (${formatReminderLeadTime(tx.reminderLeadMinutes, isArabic)})"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = remColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 6. Compact Actions
                    if (onToggleDebtSettled != null || onToggleReminderMuted != null || (showQuickDelete && onDeleteClick != null)) {
                        Row(
                            modifier = Modifier.width(96.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tx.hasPaymentReminder && onToggleReminderMuted != null) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (tx.reminderMuted) ext.cardContainer
                                            else ext.warningAmber.copy(alpha = 0.18f)
                                        )
                                        .testTag("table_mute_reminder_${tx.id}")
                                        .clickable { onToggleReminderMuted(tx.id, !tx.reminderMuted) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tx.reminderMuted) Icons.Outlined.NotificationsOff else Icons.Outlined.NotificationsActive,
                                        contentDescription = if (tx.reminderMuted) {
                                            tr(isArabic, "تفعيل التنبيه", "Enable reminder")
                                        } else {
                                            tr(isArabic, "فصل التنبيه", "Mute reminder")
                                        },
                                        tint = if (tx.reminderMuted) MaterialTheme.colorScheme.onSurfaceVariant else ext.warningAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (tx.isDebt && onToggleDebtSettled != null) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (tx.isDebtSettled) ext.cardContainer else ext.incomeContainer)
                                        .testTag("table_settle_debt_${tx.id}")
                                        .clickable { onToggleDebtSettled(tx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tx.isDebtSettled) Icons.Outlined.Undo else Icons.Outlined.CheckCircle,
                                        contentDescription = if (tx.isDebtSettled) {
                                            tr(isArabic, "إرجاع كدين", "Reopen debt")
                                        } else {
                                            tr(isArabic, "دفع المديونية", "Pay debt")
                                        },
                                        tint = if (tx.isDebtSettled) MaterialTheme.colorScheme.onSurfaceVariant else ext.incomeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (showQuickDelete && onDeleteClick != null) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ext.expenseContainer)
                                        .testTag("table_delete_${tx.id}")
                                        .clickable { onDeleteClick(tx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = tr(isArabic, "حذف", "Delete"),
                                        tint = ext.expenseColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                if (idx < transactions.lastIndex) {
                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, widthDp: Int) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(widthDp.dp)
    )
}
