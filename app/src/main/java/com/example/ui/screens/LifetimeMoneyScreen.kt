package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppSettingsEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.CollapsibleFilterDropdownCard
import com.example.ui.components.DropdownFilterOption
import com.example.ui.components.ExportDownloadButton
import com.example.ui.components.SecurityVerificationDialog
import com.example.ui.components.SereneCard
import com.example.ui.components.SmartSingleSelectDropdown
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.formatTime12Hour
import com.example.util.tr

/**
 * Dedicated screen for «فلوسك من يوم ما استخدمت التطبيق».
 *
 * Strictly accumulates EVERY money-added (INCOME) operation ever logged since day 1,
 * completely independent of any expenses, debts paid, or scheduled obligations.
 */
@Composable
fun LifetimeMoneyScreen(
    transactions: List<TransactionEntity>,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onAddMoneyClick: () -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    val ext = LocalSereneColors.current
    val primaryCurrency = settings.defaultCurrency
    val sym = currencySymbol(primaryCurrency, isArabic)

    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val allAddedMoney = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.INCOME }
            .sortedWith(
                compareByDescending<TransactionEntity> { it.date }
                    .thenByDescending { it.time }
                    .thenByDescending { it.createdAt }
            )
    }

    val availableSources = remember(allAddedMoney) {
        allAddedMoney.map { it.source }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableCategories = remember(allAddedMoney) {
        allAddedMoney.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val totalsByCurrency = remember(allAddedMoney, primaryCurrency) {
        val grouped = allAddedMoney.groupBy { it.currency }
        val result = mutableListOf<Pair<String, Long>>()
        result += primaryCurrency to (grouped[primaryCurrency]?.sumOf { it.amountMinor } ?: 0L)
        grouped.keys
            .filter { it != primaryCurrency }
            .sorted()
            .forEach { curr ->
                result += curr to (grouped[curr]?.sumOf { it.amountMinor } ?: 0L)
            }
        result
    }

    val primaryLifetimeTotalMinor = totalsByCurrency.firstOrNull()?.second ?: 0L

    val filteredList = remember(allAddedMoney, searchQuery, selectedSource, selectedCategory) {
        val q = searchQuery.trim()
        allAddedMoney.filter { tx ->
            val srcOk = selectedSource == null || tx.source == selectedSource
            val catOk = selectedCategory == null || tx.category == selectedCategory
            val qOk = q.isEmpty() ||
                tx.category.contains(q, ignoreCase = true) ||
                tx.source.contains(q, ignoreCase = true) ||
                tx.note.contains(q, ignoreCase = true) ||
                tx.itemName.contains(q, ignoreCase = true) ||
                tx.date.contains(q, ignoreCase = true) ||
                formatAmountMinor(tx.amountMinor).contains(q, ignoreCase = true)
            srcOk && catOk && qOk
        }
    }
    val activeFilterCount = listOf(
        searchQuery.isNotBlank(),
        selectedSource != null,
        selectedCategory != null
    ).count { it }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Cumulative Hero Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lifetime_money_hero_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = ext.heroCardBackground,
                    shadowElevation = if (ext.isDark) 0.dp else 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                        .clip(CircleShape)
                                        .background(ext.incomeColor.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Savings,
                                        contentDescription = null,
                                        tint = Color(0xFF82F5C1),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = tr(
                                            isArabic,
                                            "الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج",
                                            "Your Money Since Using the App"
                                        ),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ext.heroCardOnBackground,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tr(
                                            isArabic,
                                            "إجمالي كل مبلغ أضفته • غير متأثر بأي مصاريف مدفوعة",
                                            "All money ever added • Unaffected by any expenses"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.heroCardSubtext,
                                        maxLines = 2
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                modifier = Modifier
                                    .heightIn(min = 42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("lifetime_add_money_btn")
                                    .clickable(onClick = onAddMoneyClick),
                                shape = RoundedCornerShape(12.dp),
                                color = ext.incomeColor
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tr(isArabic, "إضافة مبلغ", "Add Money"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatAmountMinor(primaryLifetimeTotalMinor),
                                style = AmountDisplayStyle,
                                color = Color(0xFF82F5C1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = sym,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = ext.heroCardSubtext,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        if (totalsByCurrency.size > 1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                totalsByCurrency.drop(1).take(3).forEach { (curr, total) ->
                                    val cSym = currencySymbol(curr, isArabic)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${formatAmountMinor(total)} $cSym",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr(isArabic, "عدد عمليات إضافة الفلوس", "Total Money-Added Entries"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.heroCardSubtext
                                )
                                Text(
                                    text = tr(
                                        isArabic,
                                        "${allAddedMoney.size} عملية",
                                        "${allAddedMoney.size} entries"
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 2. Formatted Export & Collapsible Dropdown Filters
            item {
                val sourceOptions = remember(availableSources, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل طرق الاستلام", "All Channels"))) +
                        availableSources.map { DropdownFilterOption<String?>(it, it) }
                }
                val categoryOptions = remember(availableCategories, isArabic) {
                    listOf(DropdownFilterOption<String?>(null, tr(isArabic, "كل مصادر الفلوس", "All Sources"))) +
                        availableCategories.map { DropdownFilterOption<String?>(it, it) }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportDownloadButton(
                        transactions = filteredList,
                        preferredCurrency = primaryCurrency,
                        reportTitle = tr(
                            isArabic,
                            "الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج",
                            "Your Money Since Using the App"
                        ),
                        filterSummary = tr(
                            isArabic,
                            "${filteredList.size} عملية إضافة فلوس",
                            "${filteredList.size} money-added entries"
                        ),
                        isArabic = isArabic,
                        testTag = "lifetime_export_btn",
                        modifier = Modifier.fillMaxWidth()
                    )

                    CollapsibleFilterDropdownCard(
                        isArabic = isArabic,
                        activeFilterCount = activeFilterCount,
                        onResetFilters = if (activeFilterCount > 0) {
                            {
                                searchQuery = ""
                                selectedSource = null
                                selectedCategory = null
                            }
                        } else null,
                        title = tr(isArabic, "بحث وفلاتر الإضافات", "Search & Filter Added Money"),
                        testTagPrefix = "lifetime_filters"
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lifetime_money_search_input"),
                            placeholder = {
                                Text(
                                    text = tr(
                                        isArabic,
                                        "ابحث بـ (جات إزاي، عن طريق إيه، المبلغ، أو التاريخ)...",
                                        "Search by source, method, amount, or date..."
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = tr(isArabic, "مسح البحث", "Clear"),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ext.cardSubtleBackground,
                                unfocusedContainerColor = ext.cardSubtleBackground
                            )
                        )

                        if (availableSources.isNotEmpty() || availableCategories.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (availableCategories.isNotEmpty()) {
                                    SmartSingleSelectDropdown(
                                        label = tr(isArabic, "جات إزاي", "Source"),
                                        selectedValue = selectedCategory,
                                        options = categoryOptions,
                                        isArabic = isArabic,
                                        onSelected = { selectedCategory = it },
                                        isHighlighted = selectedCategory != null,
                                        testTag = "lifetime_category_dropdown",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (availableSources.isNotEmpty()) {
                                    SmartSingleSelectDropdown(
                                        label = tr(isArabic, "عن طريق إيه", "Channel"),
                                        selectedValue = selectedSource,
                                        options = sourceOptions,
                                        isArabic = isArabic,
                                        onSelected = { selectedSource = it },
                                        isHighlighted = selectedSource != null,
                                        testTag = "lifetime_source_dropdown",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(
                            isArabic,
                            "كل العمليات التي أُضيفت فيها فلوس",
                            "All Added Money Operations"
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = tr(isArabic, "${filteredList.size} عملية", "${filteredList.size} items"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. Entries List or Empty State
            if (filteredList.isEmpty()) {
                item {
                    SereneCard(contentPadding = PaddingValues(24.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(ext.incomeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = tr(
                                    isArabic,
                                    "لم تقم بإضافة أي مبالغ بعد",
                                    "No money has been added yet"
                                ),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = tr(
                                    isArabic,
                                    "كل مبلغ تضيفه سينزل هنا تلقائياً مع تفاصيل جات إزاي وعن طريق إيه وتاريخ ووقت الاستلام.",
                                    "Every amount you add appears here with source, channel, date, and time."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onAddMoneyClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ext.incomeColor)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr(isArabic, "إضافة أول مبلغ الآن", "Add First Amount Now"))
                            }
                        }
                    }
                }
            } else {
                items(items = filteredList, key = { it.id }) { tx ->
                    LifetimeMoneyEntryCard(
                        tx = tx,
                        isArabic = isArabic,
                        showQuickDelete = settings.quickDeleteMode,
                        onDeleteClick = { pendingDeleteTx = tx }
                    )
                }
            }
        }
    }

    pendingDeleteTx?.let { tx ->
        SecurityVerificationDialog(
            title = tr(isArabic, "حذف عملية إضافة الفلوس", "Delete Added Money Entry"),
            description = tr(
                isArabic,
                "هل أنت متأكد من حذف مبلغ ${formatAmountMinor(tx.amountMinor)} ${currencySymbol(tx.currency, isArabic)} (${tx.category})؟",
                "Delete ${formatAmountMinor(tx.amountMinor)} ${currencySymbol(tx.currency, isArabic)} (${tx.category})?"
            ),
            confirmLabel = tr(isArabic, "حذف نهائي", "Delete"),
            isDanger = true,
            requireSecurity = settings.hasAnyLockMethodConfigured(),
            settings = settings,
            isArabic = isArabic,
            onConfirmed = {
                onDeleteTransaction(tx.id)
                pendingDeleteTx = null
            },
            onDismiss = { pendingDeleteTx = null }
        )
    }
}

@Composable
private fun LifetimeMoneyEntryCard(
    tx: TransactionEntity,
    isArabic: Boolean,
    showQuickDelete: Boolean,
    onDeleteClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    val txSym = currencySymbol(tx.currency, isArabic)
    val howItCame = buildString {
        append(tx.category.ifBlank { tr(isArabic, "غير محدد", "Unspecified") })
        if (tx.itemName.isNotBlank()) append(" • ${tx.itemName}")
    }

    SereneCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lifetime_entry_${tx.id}"),
        borderColor = ext.incomeColor.copy(alpha = 0.25f),
        contentPadding = PaddingValues(14.dp)
    ) {
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
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ext.incomeContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = null,
                        tint = ext.incomeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(isArabic, "الفلوس كام", "Amount"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+${formatAmountMinor(tx.amountMinor)} $txSym",
                        style = AmountBodyStyle.copy(
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ext.incomeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (showQuickDelete) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("lifetime_delete_${tx.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = tr(isArabic, "حذف", "Delete"),
                        tint = ext.expenseColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Structured details: جات إزاي / عن طريق إيه / تاريخ الاستلام / وقت الاستلام
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ext.cardSubtleBackground)
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DetailMetaRow(
                icon = Icons.Outlined.Source,
                label = tr(isArabic, "جات إزاي:", "How it came:"),
                value = howItCame
            )
            DetailMetaRow(
                icon = Icons.Outlined.AccountBalanceWallet,
                label = tr(isArabic, "عن طريق إيه:", "Received via:"),
                value = tx.source.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailMetaRow(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.CalendarToday,
                    label = tr(isArabic, "تاريخ الاستلام:", "Date:"),
                    value = tx.date
                )
                DetailMetaRow(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Schedule,
                    label = tr(isArabic, "وقت الاستلام:", "Time:"),
                    value = formatTime12Hour(tx.time, isArabic)
                )
            }
            if (tx.note.isNotBlank()) {
                Text(
                    text = "${tr(isArabic, "ملاحظة:", "Note:")} ${tx.note}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DetailMetaRow(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
