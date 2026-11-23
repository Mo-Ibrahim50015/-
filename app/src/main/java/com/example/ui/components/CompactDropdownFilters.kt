package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LocalSereneColors
import com.example.util.tr

data class DropdownFilterOption<T>(
    val value: T,
    val label: String,
    val subtitle: String = ""
)

/**
 * Compact collapsible Filter Panel that saves vertical space on every screen.
 * Starts collapsed by default (showing active filter count badge + optional quick reset),
 * and expands to reveal clean dropdown filter pickers.
 */
@Composable
fun CollapsibleFilterDropdownCard(
    isArabic: Boolean,
    activeFilterCount: Int,
    onResetFilters: (() -> Unit)? = null,
    title: String = tr(isArabic, "الفلاتر والبحث", "Filters & Search"),
    initiallyExpanded: Boolean = false,
    testTagPrefix: String = "compact_filters",
    trailingHeaderContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalSereneColors.current
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    SereneCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        borderColor = if (activeFilterCount > 0) ext.incomeColor.copy(alpha = 0.4f) else ext.cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .testTag("${testTagPrefix}_toggle")
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp),
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
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (activeFilterCount > 0) ext.incomeContainer else ext.cardSubtleBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = if (activeFilterCount > 0) ext.incomeColor else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (activeFilterCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(ext.incomeColor)
                                    .padding(horizontal = 7.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$activeFilterCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Text(
                        text = if (expanded) {
                            tr(isArabic, "اضغط لإغلاق قائمة الفلاتر وتوفير المساحة", "Tap to collapse filters")
                        } else {
                            tr(isArabic, "اضغط لفتح الفلاتر في قائمة منسدلة منظمة", "Tap to open dropdown filters")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (activeFilterCount > 0 && onResetFilters != null) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("${testTagPrefix}_reset")
                            .clickable(onClick = onResetFilters),
                        shape = RoundedCornerShape(8.dp),
                        color = ext.expenseContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = ext.expenseColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = tr(isArabic, "ضبط", "Reset"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ext.expenseColor
                            )
                        }
                    }
                }
                trailingHeaderContent?.invoke()
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) tr(isArabic, "إغلاق", "Collapse") else tr(isArabic, "فتح", "Expand"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Smart Single-Select Dropdown Filter.
 * ALWAYS opens a searchable scrollable dropdown picker so every dropdown has a built-in search bar.
 */
@Composable
fun <T> SmartSingleSelectDropdown(
    label: String,
    selectedValue: T,
    options: List<DropdownFilterOption<T>>,
    isArabic: Boolean,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    testTag: String = ""
) {
    val ext = LocalSereneColors.current
    var showSearchDialog by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label ?: ""

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
                .clickable { showSearchDialog = true },
            shape = RoundedCornerShape(12.dp),
            color = if (isHighlighted) ext.incomeContainer else ext.cardSubtleBackground,
            border = BorderStroke(
                1.dp,
                if (isHighlighted) ext.incomeColor.copy(alpha = 0.6f) else ext.cardBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selectedLabel.ifBlank { tr(isArabic, "الكل", "All") },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isHighlighted) ext.incomeColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = if (isHighlighted) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showSearchDialog) {
        SearchableSingleOptionDialog(
            title = label,
            options = options,
            selectedValue = selectedValue,
            isArabic = isArabic,
            onSelect = {
                onSelected(it)
                showSearchDialog = false
            },
            onDismiss = { showSearchDialog = false }
        )
    }
}

@Composable
private fun <T> SearchableSingleOptionDialog(
    title: String,
    options: List<DropdownFilterOption<T>>,
    selectedValue: T,
    isArabic: Boolean,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalSereneColors.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(options, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) options
        else options.filter {
            it.label.lowercase().contains(q) || it.subtitle.lowercase().contains(q)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$title (${options.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = tr(isArabic, "إغلاق", "Close"), modifier = Modifier.size(18.dp))
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = tr(isArabic, "مسح", "Clear"), modifier = Modifier.size(16.dp))
                            }
                        }
                    } else null,
                    placeholder = {
                        Text(
                            text = tr(isArabic, "ابحث في $title...", "Search $title..."),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ext.cardSubtleBackground,
                        unfocusedContainerColor = ext.cardSubtleBackground
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (filtered.isEmpty()) {
                    Text(
                        text = tr(isArabic, "لا توجد نتائج مطابقة للبحث", "No matching options"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 310.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered) { opt ->
                            val chosen = opt.value == selectedValue
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 46.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .clickable { onSelect(opt.value) },
                                shape = RoundedCornerShape(11.dp),
                                color = if (chosen) ext.incomeContainer else ext.cardSubtleBackground
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
                                            text = opt.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (chosen) ext.incomeColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (opt.subtitle.isNotBlank()) {
                                            Text(
                                                text = opt.subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (chosen) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = ext.incomeColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Smart Multi-Select Dropdown Filter (for multi-currency, multi-source, multi-category, multi-party, multi-item).
 * ALWAYS opens a searchable multi-select dialog so every dropdown has a built-in search bar.
 */
@Composable
fun SmartMultiSelectDropdown(
    label: String,
    options: List<String>,
    selectedValues: Set<String>,
    isArabic: Boolean,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val ext = LocalSereneColors.current
    var showSearchDialog by remember { mutableStateOf(false) }
    val active = selectedValues.isNotEmpty()
    val summaryText = when {
        selectedValues.isEmpty() -> tr(isArabic, "الكل", "All")
        selectedValues.size == 1 -> selectedValues.first()
        else -> tr(isArabic, "${selectedValues.size} محدد", "${selectedValues.size} selected")
    }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
                .clickable { showSearchDialog = true },
            shape = RoundedCornerShape(12.dp),
            color = if (active) ext.incomeContainer else ext.cardSubtleBackground,
            border = BorderStroke(
                1.dp,
                if (active) ext.incomeColor.copy(alpha = 0.6f) else ext.cardBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (active) ext.incomeColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = if (active) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showSearchDialog) {
        var query by remember { mutableStateOf("") }
        val mergedOptions = remember(options, selectedValues) {
            (selectedValues.toList() + options).distinct()
        }
        val filtered = remember(mergedOptions, query) {
            val q = query.trim().lowercase()
            if (q.isEmpty()) mergedOptions else mergedOptions.filter { it.lowercase().contains(q) }
        }
        val trimmedQuery = query.trim()
        val canApplyCustomQuery = trimmedQuery.isNotBlank() &&
            mergedOptions.none { it.equals(trimmedQuery, ignoreCase = true) }
        Dialog(onDismissRequest = { showSearchDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$label (${mergedOptions.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (selectedValues.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onClear() },
                                    shape = RoundedCornerShape(8.dp),
                                    color = ext.expenseContainer
                                ) {
                                    Text(
                                        text = tr(isArabic, "مسح الكل", "Clear"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ext.expenseColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showSearchDialog = false },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = tr(isArabic, "إغلاق", "Close"), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = tr(isArabic, "مسح", "Clear"), modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        placeholder = {
                            Text(
                                tr(isArabic, "ابحث في $label...", "Search $label..."),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ext.cardSubtleBackground,
                            unfocusedContainerColor = ext.cardSubtleBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (canApplyCustomQuery) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onToggle(trimmedQuery)
                                    query = ""
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = ext.incomeContainer,
                            border = BorderStroke(1.dp, ext.incomeColor.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = ext.incomeColor,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = tr(isArabic, "تصفية بكلمة: \"$trimmedQuery\"", "Filter by: \"$trimmedQuery\""),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ext.incomeColor
                                )
                            }
                        }
                    }

                    if (mergedOptions.isEmpty() && !canApplyCustomQuery) {
                        Text(
                            text = tr(isArabic, "لا توجد عناصر مسجلة في هذا الحقل حتى الآن — يمكنك كتابة أي كلمة في البحث أعلاه", "No recorded items yet — type any keyword above to filter"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    } else if (filtered.isEmpty() && !canApplyCustomQuery) {
                        Text(
                            text = tr(isArabic, "لا توجد نتائج مطابقة للبحث", "No matching items"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    } else if (filtered.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filtered) { item ->
                                val checked = item in selectedValues
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onToggle(item) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (checked) ext.incomeContainer else ext.cardSubtleBackground
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(checked = checked, onCheckedChange = { onToggle(item) })
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (checked) ext.incomeColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showSearchDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.heroCardBackground,
                            contentColor = ext.heroCardOnBackground
                        )
                    ) {
                        Text(tr(isArabic, "تطبيق ✓", "Apply ✓"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Searchable Dropdown Filter specifically designed for Additional Notes («الملاحظة الإضافية»).
 * Allows both:
 * 1) Typing ANY custom keyword inside the dropdown's search bar to filter matching notes, AND
 * 2) Picking any existing saved note directly from the list.
 */
@Composable
fun SmartNoteFilterDropdown(
    label: String,
    noteQuery: String,
    availableNotes: List<String>,
    isArabic: Boolean,
    onNoteQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val ext = LocalSereneColors.current
    var showDialog by remember { mutableStateOf(false) }
    val active = noteQuery.isNotBlank()

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
                .clickable { showDialog = true },
            shape = RoundedCornerShape(12.dp),
            color = if (active) ext.incomeContainer else ext.cardSubtleBackground,
            border = BorderStroke(
                1.dp,
                if (active) ext.incomeColor.copy(alpha = 0.6f) else ext.cardBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = noteQuery.ifBlank { tr(isArabic, "الكل", "All") },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (active) ext.incomeColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = if (active) ext.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDialog) {
        var draftQuery by remember(noteQuery) { mutableStateOf(noteQuery) }
        val trimmed = draftQuery.trim()
        val matchingNotes = remember(availableNotes, trimmed) {
            if (trimmed.isEmpty()) availableNotes
            else availableNotes.filter { it.contains(trimmed, ignoreCase = true) }
        }

        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$label (${availableNotes.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { showDialog = false }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = tr(isArabic, "إغلاق", "Close"), modifier = Modifier.size(18.dp))
                        }
                    }

                    OutlinedTextField(
                        value = draftQuery,
                        onValueChange = { draftQuery = it },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = if (draftQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { draftQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = tr(isArabic, "مسح", "Clear"), modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        placeholder = {
                            Text(
                                text = tr(isArabic, "ابحث بأي كلمة في الملاحظة الإضافية...", "Search inside additional notes..."),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ext.cardSubtleBackground,
                            unfocusedContainerColor = ext.cardSubtleBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        item {
                            val isAll = noteQuery.isBlank() && trimmed.isEmpty()
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onNoteQueryChange("")
                                        showDialog = false
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAll) ext.incomeContainer else ext.cardSubtleBackground
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(isArabic, "الكل (بدون فلتر للملاحظة)", "All (No note filter)"),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isAll) ext.incomeColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isAll) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = ext.incomeColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        if (trimmed.isNotEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onNoteQueryChange(trimmed)
                                            showDialog = false
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.incomeContainer,
                                    border = BorderStroke(1.dp, ext.incomeColor.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Outlined.Search, contentDescription = null, tint = ext.incomeColor, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = tr(
                                                isArabic,
                                                "بحث في كل الملاحظات التي تحتوي على: \"$trimmed\"",
                                                "Filter all notes containing: \"$trimmed\""
                                            ),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = ext.incomeColor
                                        )
                                    }
                                }
                            }
                        }

                        items(matchingNotes) { note ->
                            val chosen = noteQuery.equals(note, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onNoteQueryChange(note)
                                        showDialog = false
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (chosen) ext.incomeContainer else ext.cardSubtleBackground
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (chosen) ext.incomeColor else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (chosen) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = ext.incomeColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onNoteQueryChange(trimmed)
                            showDialog = false
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
                        Text(tr(isArabic, "تطبيق البحث ✓", "Apply Filter ✓"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

