package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AccountEntity
import com.example.data.CategoryEntity
import com.example.data.TransactionType
import com.example.ui.components.SereneCard
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.LocalSereneColors
import com.example.util.SupportedCurrencies
import com.example.util.currencySymbol
import com.example.util.currentIsoDate
import com.example.util.currentTime24
import com.example.util.formatPickerDateTime
import com.example.util.iconForKey
import com.example.util.parseAmountToMinor
import com.example.util.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    defaultCurrency: String,
    lastSelectedSource: String,
    isArabic: Boolean,
    initialType: String = TransactionType.EXPENSE,
    initialIsDebt: Boolean = false,
    onSaveTransaction: (
        type: String,
        amountMinor: Long,
        currency: String,
        category: String,
        source: String,
        date: String,
        time: String,
        isManualDateTime: Boolean,
        note: String,
        isDebt: Boolean,
        itemName: String,
        debtParty: String
    ) -> Unit
) {
    val ext = LocalSereneColors.current
    val context = LocalContext.current

    var txType by remember(initialType) { mutableStateOf(initialType) }
    var isDebt by remember(initialIsDebt) { mutableStateOf(initialIsDebt) }
    var itemNameText by remember { mutableStateOf("") }
    var debtPartyText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var selectedCurrency by remember(defaultCurrency) { mutableStateOf(defaultCurrency) }
    var showCurrencyMenu by remember { mutableStateOf(false) }

    var categoryText by remember { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    var sourceText by remember(lastSelectedSource) {
        mutableStateOf(lastSelectedSource.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") })
    }
    var showSourceDropdown by remember { mutableStateOf(false) }

    var dateIso by remember { mutableStateOf(currentIsoDate()) }
    var time24 by remember { mutableStateOf(currentTime24()) }
    var isManualDateTime by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var notesText by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Refresh auto date-time on opening screen if not manually edited
    LaunchedEffect(Unit) {
        if (!isManualDateTime) {
            dateIso = currentIsoDate()
            time24 = currentTime24()
        }
    }

    val typeCategories = remember(categories, txType) {
        categories.filter { it.kind == txType }
    }
    val filteredCategorySuggestions = remember(typeCategories, categoryText) {
        val q = categoryText.trim()
        if (q.isEmpty()) typeCategories else typeCategories.filter { it.name.contains(q, ignoreCase = true) }
    }
    val filteredSourceSuggestions = remember(accounts, sourceText) {
        val q = sourceText.trim()
        if (q.isEmpty()) accounts else accounts.filter { it.name.contains(q, ignoreCase = true) }
    }

    val sym = currencySymbol(selectedCurrency, isArabic)

    fun adjustAmount(deltaMajor: Int) {
        val currentMinor = parseAmountToMinor(amountText) ?: 0L
        val nextMinor = currentMinor + (deltaMajor * 100L)
        amountText = String.format(Locale.US, "%.2f", nextMinor / 100.0)
        amountError = null
    }

    fun performReset(keepLastSource: Boolean) {
        amountText = ""
        amountError = null
        categoryText = ""
        itemNameText = ""
        debtPartyText = ""
        isDebt = false
        if (!keepLastSource) {
            sourceText = tr(isArabic, "كاش / نقدي", "Cash")
        }
        notesText = ""
        dateIso = currentIsoDate()
        time24 = currentTime24()
        isManualDateTime = false
        showCategoryDropdown = false
        showSourceDropdown = false
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Segmented Type Control (Expense / Income)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    color = ext.cardSubtleBackground,
                    border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SegmentedTypeTab(
                            modifier = Modifier.weight(1f),
                            label = tr(isArabic, "مصروف", "Expense"),
                            dotColor = ext.expenseColor,
                            selected = txType == TransactionType.EXPENSE,
                            tag = "type_expense_tab",
                            onClick = {
                                txType = TransactionType.EXPENSE
                                showCategoryDropdown = false
                            }
                        )
                        SegmentedTypeTab(
                            modifier = Modifier.weight(1f),
                            label = tr(isArabic, "دخل / أرباح", "Income"),
                            dotColor = ext.incomeColor,
                            selected = txType == TransactionType.INCOME,
                            tag = "type_income_tab",
                            onClick = {
                                txType = TransactionType.INCOME
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            // 2. Money Input Card
            item {
                SereneCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (txType == TransactionType.EXPENSE) {
                                tr(isArabic, "المبلغ المستحق", "Expense Amount")
                            } else {
                                tr(isArabic, "مبلغ الدخل / الأرباح", "Income Amount")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = amountText,
                                onValueChange = { raw ->
                                    if (raw.length <= 14) {
                                        amountText = raw
                                        amountError = null
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = AmountDisplayStyle.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .widthIn(min = 120.dp, max = 210.dp)
                                    .testTag("amount_input"),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (amountText.isEmpty()) {
                                            Text(
                                                text = "0.00",
                                                style = AmountDisplayStyle,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Box {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showCurrencyMenu = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.cardSubtleBackground
                                ) {
                                    Text(
                                        text = sym,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showCurrencyMenu,
                                    onDismissRequest = { showCurrencyMenu = false }
                                ) {
                                    SupportedCurrencies.forEach { curr ->
                                        DropdownMenuItem(
                                            text = {
                                                Text("${if (isArabic) curr.symbolAr else curr.symbolEn} — ${if (isArabic) curr.nameAr else curr.nameEn}")
                                            },
                                            onClick = {
                                                selectedCurrency = curr.code
                                                showCurrencyMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (amountError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = amountError!!,
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.expenseColor,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Micro Presets (+50, +100, +500, Reset)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(50, 100, 500).forEach { inc ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .testTag("quick_add_$inc")
                                        .clickable { adjustAmount(inc) },
                                    shape = RoundedCornerShape(999.dp),
                                    color = ext.cardSubtleBackground
                                ) {
                                    Text(
                                        text = "+$inc $sym",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .testTag("reset_amount_btn")
                                    .clickable {
                                        amountText = ""
                                        amountError = null
                                    },
                                shape = CircleShape,
                                color = ext.cardSubtleBackground
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.RestartAlt,
                                        contentDescription = tr(isArabic, "إعادة ضبط المبلغ", "Reset Amount"),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2b. Transaction Settlement / Debt Nature Card (Paid/Earned vs Debt Owed)
            item {
                val isExpense = txType == TransactionType.EXPENSE
                SereneCard(
                    modifier = Modifier.testTag("debt_nature_card"),
                    borderColor = if (isDebt) {
                        (if (isExpense) ext.expenseColor else ext.warningAmber).copy(alpha = 0.55f)
                    } else {
                        ext.cardBorder.copy(alpha = 0.7f)
                    },
                    contentPadding = PaddingValues(16.dp)
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
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isDebt) {
                                            if (isExpense) ext.expenseContainer else ext.warningAmber.copy(alpha = 0.16f)
                                        } else ext.incomeContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDebt) Icons.Outlined.RequestQuote else Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isDebt) {
                                        if (isExpense) ext.expenseColor else ext.warningAmber
                                    } else ext.incomeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isExpense) {
                                        tr(isArabic, "حالة المصروف", "Expense Status")
                                    } else {
                                        tr(isArabic, "طبيعة الإيراد / الدخل", "Income Nature")
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isExpense) {
                                        tr(isArabic, "هل دفعت ثمنه أم هو مديونية عليك؟", "Did you pay for it or is it a debt owed?")
                                    } else {
                                        tr(isArabic, "هل هو مكسبك الخاص أم إيراد بالمديونية (سلفة)؟", "Is it earned income or borrowed debt?")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option 1: Non-Debt (Paid Expense / Earned Income)
                        val normalSelected = !isDebt
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("status_non_debt_btn")
                                .clickable { isDebt = false },
                            shape = RoundedCornerShape(12.dp),
                            color = if (normalSelected) ext.incomeContainer else ext.cardSubtleBackground,
                            border = BorderStroke(
                                width = if (normalSelected) 1.5.dp else 1.dp,
                                color = if (normalSelected) ext.incomeColor else ext.cardBorder.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (isExpense) {
                                        tr(isArabic, "مدفوع (ليس دين)", "Paid (Not Debt)")
                                    } else {
                                        tr(isArabic, "إيراد مكتسب (مكسب)", "Earned Income")
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (normalSelected) ext.incomeColor else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (isExpense) {
                                        tr(isArabic, "تم دفع ثمنه مباشرة", "Paid immediately")
                                    } else {
                                        tr(isArabic, "دخل حقيقي خاص بي", "Owned profit / salary")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Option 2: Debt Owed (Expense on Credit / Borrowed Income)
                        val debtAccent = if (isExpense) ext.expenseColor else ext.warningAmber
                        val debtBg = if (isExpense) ext.expenseContainer else ext.warningAmber.copy(alpha = 0.16f)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("status_debt_btn")
                                .clickable { isDebt = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDebt) debtBg else ext.cardSubtleBackground,
                            border = BorderStroke(
                                width = if (isDebt) 1.5.dp else 1.dp,
                                color = if (isDebt) debtAccent else ext.cardBorder.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (isExpense) {
                                        tr(isArabic, "مديونية مصروفات", "Expense Debt")
                                    } else {
                                        tr(isArabic, "مديونية إيرادات دخلت لحسابي", "Income Entered Account (Debt)")
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDebt) debtAccent else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (isExpense) {
                                        tr(isArabic, "عليّ سدادها ضمن المديونيات", "Must repay • expense debt")
                                    } else {
                                        tr(isArabic, "سلفة / قرض عليّ سداده لاحقاً", "Must repay • borrowed cash")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Expandable Item Name & Creditor inputs when marked as Debt
                    AnimatedVisibility(visible = isDebt) {
                        val noticeAccent = if (isExpense) ext.expenseColor else ext.warningAmber
                        val noticeBg = if (isExpense) ext.expenseContainer else ext.warningAmber.copy(alpha = 0.16f)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = noticeBg
                            ) {
                                Text(
                                    text = if (isExpense) {
                                        tr(
                                            isArabic,
                                            "سيتم إدراجها في تبويبة «مديونية مصروفات» كالتزام عليك سداده في كل الحالات.",
                                            "Will be filed under Expense Debts as an obligation you need to repay."
                                        )
                                    } else {
                                        tr(
                                            isArabic,
                                            "سيتم إدراجها في تبويبة «مديونية إيرادات دخلت لحسابي» كالتزام عليك سداده في كل الحالات.",
                                            "Will be filed under Income Debts (Entered Account) as an obligation you need to repay."
                                        )
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = noticeAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                            OutlinedTextField(
                                value = itemNameText,
                                onValueChange = { itemNameText = it },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Label,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = {
                                    Text(tr(isArabic, "اسم الحاجة / الغرض من الدين", "Item / Debt Title"))
                                },
                                placeholder = {
                                    Text(
                                        text = if (isExpense) {
                                            tr(isArabic, "مثال: مشتريات بقالة، قسط موبايل، فاتورة...", "e.g. Groceries, Phone installment...")
                                        } else {
                                            tr(isArabic, "مثال: سلفة طارئة، قرض شخصي...", "e.g. Personal loan, Cash advance...")
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = ext.cardSubtleBackground,
                                    unfocusedContainerColor = ext.cardSubtleBackground,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("debt_item_name_input")
                            )

                            OutlinedTextField(
                                value = debtPartyText,
                                onValueChange = { debtPartyText = it },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = {
                                    Text(tr(isArabic, "الدائن / الجهة (اختياري)", "Creditor / Lender (optional)"))
                                },
                                placeholder = {
                                    Text(
                                        text = tr(isArabic, "مثال: أحمد، محل، بنك...", "e.g. Friend, Store, Bank..."),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = ext.cardSubtleBackground,
                                    unfocusedContainerColor = ext.cardSubtleBackground,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("debt_party_input")
                            )
                        }
                    }
                }
            }

            // 3. Category Selection Card
            item {
                SereneCard(contentPadding = PaddingValues(16.dp)) {
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
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ext.cardSubtleBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = tr(isArabic, "التصنيف", "Category"),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        val hasSelectedCat = categoryText.isNotBlank()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (hasSelectedCat) ext.incomeContainer else ext.cardContainer
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (hasSelectedCat) categoryText else tr(isArabic, "غير محدد", "Unspecified"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (hasSelectedCat) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = {
                            categoryText = it
                            showCategoryDropdown = true
                        },
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = tr(isArabic, "اكتب التصنيف أو اختر من السابق...", "Type or pick a category..."),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = if (showCategoryDropdown) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = tr(isArabic, "عرض التصنيفات", "Toggle Categories"),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { showCategoryDropdown = !showCategoryDropdown }
                                    .padding(4.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ext.cardSubtleBackground,
                            unfocusedContainerColor = ext.cardSubtleBackground,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_input")
                    )

                    AnimatedVisibility(visible = showCategoryDropdown) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ext.cardSubtleBackground)
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val trimmed = categoryText.trim()
                            if (trimmed.isNotEmpty() && typeCategories.none { it.name.equals(trimmed, true) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            categoryText = trimmed
                                            showCategoryDropdown = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddCircle,
                                        contentDescription = null,
                                        tint = ext.incomeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = tr(isArabic, "إضافة كـ تصنيف جديد: \"$trimmed\"", "Add new category: \"$trimmed\""),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ext.incomeColor
                                    )
                                }
                                HorizontalDivider(color = ext.cardBorder)
                            }

                            filteredCategorySuggestions.take(6).forEach { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            categoryText = cat.name
                                            showCategoryDropdown = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = iconForKey(cat.iconKey),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = tr(isArabic, "تصنيفات شائعة:", "Common categories:"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        typeCategories.take(6).forEach { cat ->
                            val isChosen = categoryText == cat.name
                            QuickChip(
                                label = cat.name,
                                icon = iconForKey(cat.iconKey),
                                selected = isChosen,
                                onClick = {
                                    categoryText = cat.name
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // 4. Source / Account Selection Card
            item {
                SereneCard(contentPadding = PaddingValues(16.dp)) {
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
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ext.cardSubtleBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = tr(isArabic, "مصدر الأموال / الحساب", "Source / Account"),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(ext.cardContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = sourceText.ifBlank { tr(isArabic, "كاش / نقدي", "Cash") },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = sourceText,
                        onValueChange = {
                            sourceText = it
                            showSourceDropdown = true
                        },
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = tr(isArabic, "اكتب مصدر الأموال أو الحساب...", "Type or choose account..."),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = if (showSourceDropdown) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = tr(isArabic, "عرض الحسابات", "Toggle Accounts"),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { showSourceDropdown = !showSourceDropdown }
                                    .padding(4.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ext.cardSubtleBackground,
                            unfocusedContainerColor = ext.cardSubtleBackground,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("source_input")
                    )

                    AnimatedVisibility(visible = showSourceDropdown) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ext.cardSubtleBackground)
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val trimmed = sourceText.trim()
                            if (trimmed.isNotEmpty() && accounts.none { it.name.equals(trimmed, true) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            sourceText = trimmed
                                            showSourceDropdown = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddCircle,
                                        contentDescription = null,
                                        tint = ext.incomeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = tr(isArabic, "إضافة كـ حساب جديد: \"$trimmed\"", "Add new account: \"$trimmed\""),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ext.incomeColor
                                    )
                                }
                                HorizontalDivider(color = ext.cardBorder)
                            }
                            filteredSourceSuggestions.take(7).forEach { acc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            sourceText = acc.name
                                            showSourceDropdown = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = iconForKey(acc.iconKey),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = tr(isArabic, "حسابات شائعة:", "Common accounts:"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.take(4).forEach { acc ->
                            QuickChip(
                                label = acc.name,
                                icon = iconForKey(acc.iconKey),
                                selected = sourceText == acc.name,
                                onClick = {
                                    sourceText = acc.name
                                    showSourceDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // 5. DateTime & Notes Card
            item {
                SereneCard(contentPadding = PaddingValues(16.dp)) {
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
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ext.cardSubtleBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = tr(isArabic, "التاريخ والتوقيت اللحظي", "Live Date & Time"),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isManualDateTime) {
                                        tr(isArabic, "تم التعديل يدوياً", "Manually adjusted")
                                    } else {
                                        tr(isArabic, "معتمد تلقائياً باللحظة الحالية", "Auto-stamped at save time")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isManualDateTime) ext.warningAmber else ext.incomeColor
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("datetime_picker_btn")
                                .clickable { showDatePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            color = ext.cardSubtleBackground
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formatPickerDateTime(dateIso, time24, isArabic),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Outlined.EditCalendar,
                                    contentDescription = tr(isArabic, "تغيير التاريخ", "Edit Date"),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tr(isArabic, "ملاحظات إضافية", "Additional Notes"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = tr(isArabic, "(اختياري)", "(optional)"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        minLines = 2,
                        maxLines = 4,
                        placeholder = {
                            Text(
                                text = tr(
                                    isArabic,
                                    "أضف تفاصيل أخرى، مكان الشراء، أو ملاحظة تذكيرية...",
                                    "Add details, merchant name, or a quick memo..."
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ext.cardSubtleBackground,
                            unfocusedContainerColor = ext.cardSubtleBackground,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notes_input")
                    )
                }
            }

            // 6. Primary Save & Reset Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val minor = parseAmountToMinor(amountText)
                            if (minor == null || minor <= 0L) {
                                amountError = tr(
                                    isArabic,
                                    "يرجى إدخال مبلغ صحيح أكبر من 0.00 $sym",
                                    "Please enter a valid amount greater than 0.00 $sym"
                                )
                                return@Button
                            }
                            onSaveTransaction(
                                txType,
                                minor,
                                selectedCurrency,
                                categoryText.trim().ifBlank { tr(isArabic, "غير محدد", "Unspecified") },
                                sourceText.trim().ifBlank { tr(isArabic, "كاش / نقدي", "Cash") },
                                dateIso,
                                time24,
                                isManualDateTime,
                                notesText.trim(),
                                isDebt,
                                itemNameText.trim(),
                                debtPartyText.trim()
                            )
                            // PDF Section 6: clear form while keeping last selected source for rapid entry!
                            performReset(keepLastSource = true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_transaction_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.heroCardBackground,
                            contentColor = ext.heroCardOnBackground
                        )
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDebt) {
                                tr(isArabic, "حفظ كمديونية عليّ", "Save as Debt Owed")
                            } else {
                                tr(isArabic, "حفظ العملية الآن", "Save Transaction Now")
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = {
                            val hasContent = amountText.isNotBlank() ||
                                categoryText.isNotBlank() ||
                                notesText.isNotBlank() ||
                                itemNameText.isNotBlank() ||
                                debtPartyText.isNotBlank()
                            if (hasContent) {
                                showResetConfirm = true
                            } else {
                                performReset(keepLastSource = false)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("reset_form_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.cardSubtleBackground,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Outlined.ClearAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(isArabic, "مسح وإعادة تعيين النموذج", "Clear & Reset Form"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }

    // Material 3 DatePicker followed by TimePicker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            dateIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                            isManualDateTime = true
                        }
                        showDatePicker = false
                        val parts = time24.split(":")
                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        runCatching {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    time24 = String.format(Locale.US, "%02d:%02d", h, m)
                                    isManualDateTime = true
                                },
                                hour,
                                minute,
                                false
                            ).show()
                        }
                    }
                ) {
                    Text(tr(isArabic, "التالي (الوقت)", "Next (Time)"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(tr(isArabic, "إلغاء", "Cancel"))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(tr(isArabic, "إعادة تعيين النموذج", "Reset Form")) },
            text = {
                Text(
                    tr(
                        isArabic,
                        "هل تريد مسح البيانات المكتوبة وإعادة التاريخ إلى اللحظة الحالية؟",
                        "Clear entered fields and restore current date & time?"
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        performReset(keepLastSource = false)
                        showResetConfirm = false
                    }
                ) {
                    Text(tr(isArabic, "نعم، مسح", "Yes, Reset"), color = ext.expenseColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(tr(isArabic, "إلغاء", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun SegmentedTypeTab(
    modifier: Modifier,
    label: String,
    dotColor: Color,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = modifier
            .height(44.dp)
            .testTag(tag)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) ext.cardBackground else Color.Transparent,
        shadowElevation = if (selected && !ext.isDark) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalSereneColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) ext.heroCardBackground else ext.cardSubtleBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) ext.heroCardOnBackground else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
