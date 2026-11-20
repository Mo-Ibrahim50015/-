package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.SecurityVerificationDialog
import com.example.ui.components.SereneCard
import com.example.ui.theme.AmountBodyStyle
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.currencySymbol
import com.example.util.formatAmountMinor
import com.example.util.formatDisplayDateTime
import com.example.util.iconForKey
import com.example.util.tr

private enum class PaymentGroupMode { FLAT, BY_SOURCE, BY_CATEGORY }

/**
 * Dedicated Payments Screen («المدفوعات») displaying all debts that have been paid/settled.
 * Features 3 tabs: All | Settled Income Debts | Settled Expense Debts,
 * plus full multi-field search engine, source/category filters, grouping, and live counters.
 */
@Composable
fun PaymentsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    initialTab: String = "all", // "all", "income", "expense"
    onOpenDebts: () -> Unit,
    onReopenAsDebt: (id: Long) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    val ext = LocalSereneColors.current

    val allPayments = remember(transactions) {
        transactions.filter { it.isDebt && it.isDebtSettled }
    }
    val unpaidDebtCountAll = remember(transactions) {
        transactions.count { it.isDebt && !it.isDebtSettled }
    }

    var searchQuery by remember { mutableStateOf("") }
    var typeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var groupMode by remember { mutableStateOf(PaymentGroupMode.FLAT) }
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingReopenTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val availableCurrencies = remember(allPayments, settings.defaultCurrency) {
        val list = allPayments.map { it.currency }.distinct().sorted()
        if (list.isEmpty()) listOf(settings.defaultCurrency)
        else if (settings.defaultCurrency in list) listOf(settings.defaultCurrency) + list.filter { it != settings.defaultCurrency }
        else list
    }
    var activeCurrency by remember(availableCurrencies) { mutableStateOf(availableCurrencies.first()) }
    val validCurrency = if (activeCurrency in availableCurrencies) activeCurrency else availableCurrencies.first()
    val sym = currencySymbol(validCurrency, isArabic)

    val currencyPayments = remember(allPayments, validCurrency) {
        allPayments.filter { it.currency == validCurrency }
    }
    val incomePayments = remember(currencyPayments) {
        currencyPayments.filter { it.type == TransactionType.INCOME }
    }
    val expensePayments = remember(currencyPayments) {
        currencyPayments.filter { it.type == TransactionType.EXPENSE }
    }

    val totalPaidMinor = remember(currencyPayments) { currencyPayments.sumOf { it.amountMinor } }
    val incomePaidMinor = remember(incomePayments) { incomePayments.sumOf { it.amountMinor } }
    val expensePaidMinor = remember(expensePayments) { expensePayments.sumOf { it.amountMinor } }

    val availableSources = remember(currencyPayments) {
        currencyPayments.map { it.source }.distinct().sorted()
    }
    val availableCategories = remember(currencyPayments) {
        currencyPayments.map { it.category }.distinct().sorted()
    }

    val filteredPayments = remember(
        currencyPayments,
        searchQuery,
        typeTab,
        selectedSource,
        selectedCategory
    ) {
        val q = searchQuery.trim().lowercase()
        currencyPayments.filter { tx ->
            if (typeTab != "all" && tx.type != typeTab) return@filter false
            if (selectedSource != null && tx.source != selectedSource) return@filter false
            if (selectedCategory != null && tx.category != selectedCategory) return@filter false
            if (q.isNotEmpty()) {
                val amtStr = formatAmountMinor(tx.amountMinor, false)
                val hay = "${tx.itemName} ${tx.debtParty} ${tx.category} ${tx.source} ${tx.note} ${tx.date} $amtStr".lowercase()
                if (!hay.contains(q)) return@filter false
            }
            true
        }
    }
    val filteredTotalMinor = remember(filteredPayments) { filteredPayments.sumOf { it.amountMinor } }
    val hasActiveFilters = searchQuery.isNotBlank() || typeTab != "all" || selectedSource != null || selectedCategory != null

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Hero Total Payments Summary Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payments_hero_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = ext.heroCardBackground,
                    shadowElevation = 4.dp
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(ext.incomeColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Payments,
                                        contentDescription = null,
                                        tint = Color(0xFF82F5C1),
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = tr(isArabic, "إجمالي المدفوعات المسددة", "Total Settled Payments"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ext.heroCardSubtext,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tr(
                                            isArabic,
                                            "${currencyPayments.size} عملية مدفوعة • $unpaidDebtCountAll مديونية قائمة",
                                            "${currencyPayments.size} paid • $unpaidDebtCountAll unpaid debts"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF82F5C1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("payments_go_debts_btn")
                                    .clickable(onClick = onOpenDebts),
                                shape = RoundedCornerShape(10.dp),
                                color = ext.warningAmber.copy(alpha = 0.22f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.RequestQuote,
                                        contentDescription = null,
                                        tint = ext.warningAmber,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = tr(isArabic, "المديونيات ($unpaidDebtCountAll)", "Debts ($unpaidDebtCountAll)"),
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
                                text = formatAmountMinor(totalPaidMinor),
                                style = AmountDisplayStyle,
                                color = ext.heroCardOnBackground,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = sym,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF82F5C1),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        if (availableCurrencies.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableCurrencies.forEach { curr ->
                                    val chosen = curr == validCurrency
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable {
                                                activeCurrency = curr
                                                selectedSource = null
                                                selectedCategory = null
                                            },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (chosen) Color.White else Color.White.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "${currencySymbol(curr, isArabic)} ($curr)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (chosen) ext.heroCardBackground else Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Three Dedicated Payment Tabs (All | Income Debts Paid | Expense Debts Paid)
            item {
                SereneCard(contentPadding = PaddingValues(10.dp)) {
                    Text(
                        text = tr(
                            isArabic,
                            "تبويبات المدفوعات (المديونيات التي تم سدادها):",
                            "Payment Categories (Settled Debts):"
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentTabCard(
                            modifier = Modifier.weight(0.9f),
                            title = tr(isArabic, "الكل", "All"),
                            subtitle = "${currencyPayments.size} • ${formatAmountMinor(totalPaidMinor, false)}",
                            accent = ext.incomeColor,
                            selected = typeTab == "all",
                            tag = "payments_tab_all",
                            onClick = { typeTab = "all" }
                        )
                        PaymentTabCard(
                            modifier = Modifier.weight(1.1f),
                            title = tr(isArabic, "مديونية إيرادات", "Income Debts"),
                            subtitle = "${incomePayments.size} • ${formatAmountMinor(incomePaidMinor, false)} $sym",
                            accent = ext.warningAmber,
                            selected = typeTab == TransactionType.INCOME,
                            tag = "payments_tab_income",
                            onClick = { typeTab = TransactionType.INCOME }
                        )
                        PaymentTabCard(
                            modifier = Modifier.weight(1.1f),
                            title = tr(isArabic, "مديونية مصروفات", "Expense Debts"),
                            subtitle = "${expensePayments.size} • ${formatAmountMinor(expensePaidMinor, false)} $sym",
                            accent = MaterialTheme.colorScheme.primary,
                            selected = typeTab == TransactionType.EXPENSE,
                            tag = "payments_tab_expense",
                            onClick = { typeTab = TransactionType.EXPENSE }
                        )
                    }
                }
            }

            // 3. Comprehensive Search Engine + Filters Card
            item {
                SereneCard(contentPadding = PaddingValues(14.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = tr(isArabic, "مسح البحث", "Clear"),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { searchQuery = "" }
                                        .padding(4.dp)
                                )
                            }
                        },
                        placeholder = {
                            Text(
                                text = tr(
                                    isArabic,
                                    "ابحث باسم المنتج، الحاجة، الدائن، التصنيف، المصدر، أو المبلغ...",
                                    "Search product/item, creditor, category, source, amount..."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ext.cardSubtleBackground,
                            unfocusedContainerColor = ext.cardSubtleBackground,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = ext.incomeColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payments_search_input")
                    )

                    if (availableSources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(isArabic, "المصدر:", "Source:"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PaymentPill(
                                label = tr(isArabic, "الكل", "All"),
                                selected = selectedSource == null,
                                onClick = { selectedSource = null }
                            )
                            availableSources.forEach { src ->
                                PaymentPill(
                                    label = src,
                                    selected = selectedSource == src,
                                    onClick = { selectedSource = if (selectedSource == src) null else src }
                                )
                            }
                        }
                    }

                    if (availableCategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(isArabic, "التصنيف:", "Category:"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PaymentPill(
                                label = tr(isArabic, "الكل", "All"),
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null }
                            )
                            availableCategories.forEach { cat ->
                                PaymentPill(
                                    label = cat,
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(isArabic, "تنظيم:", "Group:"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PaymentPill(
                                label = tr(isArabic, "قائمة", "List"),
                                selected = groupMode == PaymentGroupMode.FLAT,
                                onClick = { groupMode = PaymentGroupMode.FLAT }
                            )
                            PaymentPill(
                                label = tr(isArabic, "حسب المصدر", "By Source"),
                                selected = groupMode == PaymentGroupMode.BY_SOURCE,
                                onClick = { groupMode = PaymentGroupMode.BY_SOURCE }
                            )
                            PaymentPill(
                                label = tr(isArabic, "حسب التصنيف", "By Category"),
                                selected = groupMode == PaymentGroupMode.BY_CATEGORY,
                                onClick = { groupMode = PaymentGroupMode.BY_CATEGORY }
                            )
                        }
                        if (hasActiveFilters) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        searchQuery = ""
                                        typeTab = "all"
                                        selectedSource = null
                                        selectedCategory = null
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    tint = ext.expenseColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = tr(isArabic, "ضبط", "Reset"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ext.expenseColor
                                )
                            }
                        }
                    }
                }
            }

            // 4. Subtotal Strip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(
                            isArabic,
                            "المدفوعات المطابقة (${filteredPayments.size})",
                            "Matching Payments (${filteredPayments.size})"
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${tr(isArabic, "الإجمالي:", "Total:")} ${formatAmountMinor(filteredTotalMinor)} $sym",
                        style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                        color = ext.incomeColor
                    )
                }
            }

            // 5. Empty State or List
            if (filteredPayments.isEmpty()) {
                item {
                    SereneCard(contentPadding = PaddingValues(24.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(ext.incomeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = if (allPayments.isEmpty()) {
                                    tr(isArabic, "لا توجد مدفوعات مسددة حتى الآن", "No settled payments yet")
                                } else {
                                    tr(isArabic, "لا توجد مدفوعات مطابقة للبحث أو الفلتر", "No payments match your search/filter")
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = tr(
                                    isArabic,
                                    "أي مديونية تضغط فيها على زر «دفع» ستنتقل تلقائياً هنا وتُضاف لإجمالي المدفوعات.",
                                    "Any debt you tap \"Pay\" on automatically moves here and adds to total payments."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onOpenDebts,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ext.heroCardBackground,
                                    contentColor = ext.heroCardOnBackground
                                )
                            ) {
                                Icon(Icons.Outlined.RequestQuote, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr(isArabic, "استعراض المديونيات القائمة", "View Active Debts"))
                            }
                        }
                    }
                }
            } else {
                when (groupMode) {
                    PaymentGroupMode.FLAT -> {
                        items(filteredPayments.size, key = { filteredPayments[it].id }) { idx ->
                            val tx = filteredPayments[idx]
                            PaymentDetailCard(
                                tx = tx,
                                categories = categories,
                                isArabic = isArabic,
                                onReopenClick = { pendingReopenTx = tx },
                                onDeleteClick = { pendingDeleteTx = tx }
                            )
                        }
                    }

                    PaymentGroupMode.BY_SOURCE, PaymentGroupMode.BY_CATEGORY -> {
                        val grouped = filteredPayments.groupBy {
                            if (groupMode == PaymentGroupMode.BY_SOURCE) it.source else it.category
                        }.entries.sortedByDescending { entry -> entry.value.sumOf { it.amountMinor } }

                        grouped.forEach { (groupTitle, items) ->
                            val groupTotal = items.sumOf { it.amountMinor }
                            item(key = "pay_header_$groupTitle") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = ext.cardSubtleBackground
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (groupMode == PaymentGroupMode.BY_SOURCE) {
                                                    Icons.Outlined.AccountBalance
                                                } else Icons.Outlined.Category,
                                                contentDescription = null,
                                                tint = ext.incomeColor,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Text(
                                                text = "$groupTitle (${items.size})",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "${formatAmountMinor(groupTotal)} $sym",
                                            style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                                            color = ext.incomeColor
                                        )
                                    }
                                }
                            }
                            items(items.size, key = { items[it].id }) { idx ->
                                val tx = items[idx]
                                PaymentDetailCard(
                                    tx = tx,
                                    categories = categories,
                                    isArabic = isArabic,
                                    onReopenClick = { pendingReopenTx = tx },
                                    onDeleteClick = { pendingDeleteTx = tx }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingReopenTx?.let { tx ->
        SecurityVerificationDialog(
            title = tr(isArabic, "إرجاع للمديونيات القائمة", "Reopen as Active Debt"),
            description = tr(
                isArabic,
                "هل تريد إرجاع \"${tx.itemName.ifBlank { tx.category }}\" إلى المديونيات القائمة مرة أخرى؟",
                "Move \"${tx.itemName.ifBlank { tx.category }}\" back to active unpaid debts?"
            ),
            confirmLabel = tr(isArabic, "إرجاع للمديونيات", "Reopen"),
            isDanger = false,
            requireSecurity = settings.confirmPaymentWithSecurity,
            settings = settings,
            isArabic = isArabic,
            onConfirmed = {
                onReopenAsDebt(tx.id)
                pendingReopenTx = null
            },
            onDismiss = { pendingReopenTx = null }
        )
    }

    pendingDeleteTx?.let { tx ->
        SecurityVerificationDialog(
            title = tr(isArabic, "حذف عملية مدفوعة", "Delete Paid Record"),
            description = tr(
                isArabic,
                "هل تريد حذف \"${tx.itemName.ifBlank { tx.category }}\" نهائياً من المدفوعات والتقارير؟",
                "Delete \"${tx.itemName.ifBlank { tx.category }}\" permanently from payments and reports?"
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
private fun PaymentTabCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    accent: Color,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else ext.cardSubtleBackground,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else ext.cardBorder.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = AmountBodyStyle.copy(
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PaymentPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) ext.heroCardBackground else ext.cardSubtleBackground,
        border = if (selected) null else BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.6f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun PaymentDetailCard(
    tx: TransactionEntity,
    categories: List<CategoryEntity>,
    isArabic: Boolean,
    onReopenClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    val isIncomeDebt = tx.type == TransactionType.INCOME
    val sym = currencySymbol(tx.currency, isArabic)
    val iconKey = categories.firstOrNull { it.name == tx.category }?.iconKey
        ?: if (isIncomeDebt) "payments" else "category"
    val itemTitle = tx.itemName.ifBlank { tx.note.ifBlank { tx.category } }
    val typeLabel = if (isIncomeDebt) {
        tr(isArabic, "سداد مديونية إيرادات", "Settled Income Debt")
    } else {
        tr(isArabic, "سداد مديونية مصروفات", "Settled Expense Debt")
    }

    SereneCard(
        modifier = Modifier.testTag("payment_card_${tx.id}"),
        borderColor = ext.incomeColor.copy(alpha = 0.4f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ext.incomeContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = tx.category,
                    tint = ext.incomeColor,
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = itemTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ext.incomeContainer)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ext.incomeColor,
                            maxLines = 1
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ext.cardSubtleBackground)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = iconForKey(iconKey),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = tx.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    if (tx.debtParty.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = tx.debtParty,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = formatDisplayDateTime(tx.date, tx.time, isArabic),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatAmountMinor(tx.amountMinor)} $sym",
                    style = AmountBodyStyle.copy(fontWeight = FontWeight.Bold),
                    color = ext.incomeColor,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tr(isArabic, "تم الدفع ✓", "Paid ✓"),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ext.incomeColor
                )
            }
        }

        if (tx.note.isNotBlank() && tx.note != itemTitle) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tx.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("reopen_payment_${tx.id}")
                    .clickable(onClick = onReopenClick),
                shape = RoundedCornerShape(10.dp),
                color = ext.cardSubtleBackground
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Undo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tr(isArabic, "إرجاع للمديونيات", "Move to Debts"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(ext.cardSubtleBackground)
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = tr(isArabic, "حذف", "Delete"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
