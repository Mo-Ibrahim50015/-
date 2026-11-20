package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccountEntity
import com.example.data.AppDatabase
import com.example.data.AppSettingsEntity
import com.example.data.CategoryEntity
import com.example.data.CloudBackupRepository
import com.example.data.CloudBackupSummary
import com.example.data.MizanityRepository
import com.example.data.SavedFilterEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.util.ExportManager
import com.example.util.currentIsoDate
import com.example.util.currentMonthRange
import com.example.util.currentTime24
import com.example.util.currentWeekRange
import com.example.util.deleteLocalAvatarFile
import com.example.util.isoDateMonthsAgo
import com.example.util.savePickedAvatarToInternalStorage
import com.example.util.tr
import com.example.widget.MasrofyWidgetProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val route: String) {
    HOME("home"),
    ADD("add"),
    LOG("log"),
    REPORTS("reports"),
    SETTINGS("settings"),
    DEBTS("debts"),
    PAYMENTS("payments"),
    INCOME_DETAILS("income_details"),
    EXPENSE_DETAILS("expense_details")
}

class MizanityViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = MizanityRepository(db)
    private val cloudBackupRepo by lazy { CloudBackupRepository(application) }

    private val coroutineErrorHandler = CoroutineExceptionHandler { _, _ ->
        emitToast(
            tr(
                isAr(),
                "حدث خطأ غير متوقع، يرجى المحاولة مرة أخرى",
                "An unexpected error occurred, please try again"
            )
        )
    }

    private fun safeIoLaunch(
        failureAr: String = "تعذر إتمام العملية، حاول مرة أخرى",
        failureEn: String = "Operation failed, please try again",
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineErrorHandler) {
            try {
                block()
            } catch (_: Throwable) {
                emitToast(tr(isAr(), failureAr, failureEn))
            }
        }
    }

    val transactions: StateFlow<List<TransactionEntity>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedFilters: StateFlow<List<SavedFilterEntity>> = repository.savedFilters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettingsEntity> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsEntity())

    private val _reportFilter = MutableStateFlow(ReportFilterState())
    val reportFilter: StateFlow<ReportFilterState> = _reportFilter.asStateFlow()

    data class AlertSummary(
        val todayCount: Int = 0,
        val monthExpenseMinor: Long = 0L,
        val hasAlertDot: Boolean = false
    )

    val reportAnalytics: StateFlow<ReportAnalytics> = combine(
        transactions,
        reportFilter,
        settings
    ) { allTx, filter, appSettings ->
        val filtered = applyReportFilter(allTx, filter)
        val preferredCurr = filter.currencies.firstOrNull() ?: appSettings.defaultCurrency
        computeAnalytics(filtered, preferredCurr)
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            computeAnalytics(emptyList(), "EGP")
        )

    val homeAnalytics: StateFlow<ReportAnalytics> = combine(
        transactions,
        settings
    ) { allTx, appSettings ->
        computeAnalytics(allTx, appSettings.defaultCurrency)
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            computeAnalytics(emptyList(), "EGP")
        )

    val alertSummary: StateFlow<AlertSummary> = combine(
        transactions,
        settings
    ) { allTx, appSettings ->
        val todayIso = currentIsoDate()
        val (monthStart, monthEnd) = currentMonthRange()
        var todayCnt = 0
        var monthExp = 0L
        for (tx in allTx) {
            if (tx.date == todayIso) todayCnt++
            if (tx.type == TransactionType.EXPENSE &&
                tx.currency == appSettings.defaultCurrency &&
                tx.date >= monthStart && tx.date <= monthEnd
            ) {
                monthExp += tx.amountMinor
            }
        }
        val dot = (appSettings.dailyReminderEnabled && todayCnt == 0) ||
            (appSettings.monthlyBudgetLimitMinor > 0 && monthExp >= appSettings.monthlyBudgetLimitMinor)
        AlertSummary(todayCount = todayCnt, monthExpenseMinor = monthExp, hasAlertDot = dot)
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AlertSummary()
        )

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessages: SharedFlow<String> = _toastMessages.asSharedFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _isSecurityReady = MutableStateFlow(false)
    val isSecurityReady: StateFlow<Boolean> = _isSecurityReady.asStateFlow()

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    private val _showWelcomeBanner = MutableStateFlow(true)
    val showWelcomeBanner: StateFlow<Boolean> = _showWelcomeBanner.asStateFlow()

    private val _isCloudBackupBusy = MutableStateFlow(false)
    val isCloudBackupBusy: StateFlow<Boolean> = _isCloudBackupBusy.asStateFlow()

    private val _cloudBackupSummary = MutableStateFlow<CloudBackupSummary?>(null)
    val cloudBackupSummary: StateFlow<CloudBackupSummary?> = _cloudBackupSummary.asStateFlow()

    private val _cloudBackupError = MutableStateFlow<String?>(null)
    val cloudBackupError: StateFlow<String?> = _cloudBackupError.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO + coroutineErrorHandler) {
            try {
                repository.ensureSeedData()
                // Read persisted settings immediately so Launch Lock activates BEFORE splash screen
                val persisted = repository.settings.first()
                val shouldLock = (persisted.lockOnLaunch || persisted.autoLockSensitive) &&
                    persisted.hasAnyLockMethodConfigured()
                _isAppLocked.value = shouldLock
            } catch (_: Throwable) {
                _isAppLocked.value = false
            } finally {
                _isSecurityReady.value = true
            }

            // Apply saved default report filter if present (non-fatal if corrupted)
            runCatching {
                repository.getDefaultFilter()?.let { saved ->
                    _reportFilter.value = ReportFilterState.fromJson(saved.filterConfigJson)
                }
            }
        }
    }

    private fun isAr(): Boolean = settings.value.language != "en"

    fun finishSplash() {
        _showSplash.value = false
    }

    fun dismissWelcomeBanner() {
        _showWelcomeBanner.value = false
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun emitToast(message: String) {
        _toastMessages.tryEmit(message)
    }

    // --- Transactions ---
    fun addTransaction(
        type: String,
        amountMinor: Long,
        currency: String,
        category: String,
        source: String,
        date: String,
        time: String,
        isManualDateTime: Boolean,
        note: String,
        isDebt: Boolean = false,
        itemName: String = "",
        debtParty: String = ""
    ) {
        safeIoLaunch(
            failureAr = "تعذر حفظ العملية، يرجى المحاولة مرة أخرى",
            failureEn = "Failed to save transaction, please try again"
        ) {
            val finalDate = if (isManualDateTime) date else currentIsoDate()
            val finalTime = if (isManualDateTime) time else currentTime24()
            val finalCategory = category.trim().ifBlank { tr(isAr(), "غير محدد", "Unspecified") }
            val finalSource = source.trim().ifBlank { tr(isAr(), "كاش / نقدي", "Cash") }

            repository.addTransaction(
                TransactionEntity(
                    type = type,
                    amountMinor = amountMinor,
                    currency = currency,
                    source = finalSource,
                    category = finalCategory,
                    date = finalDate,
                    time = finalTime,
                    note = note.trim(),
                    isDebt = isDebt,
                    isDebtSettled = false,
                    itemName = itemName.trim(),
                    debtParty = debtParty.trim()
                )
            )
            val label = when {
                isDebt && type == TransactionType.EXPENSE ->
                    tr(isAr(), "تم تسجيل المديونية (مصروف) بنجاح", "Expense debt saved successfully")
                isDebt && type == TransactionType.INCOME ->
                    tr(isAr(), "تم تسجيل إيراد بالمديونية بنجاح", "Borrowed income debt saved")
                type == TransactionType.EXPENSE ->
                    tr(isAr(), "تم حفظ المصروف المدفوع بنجاح", "Paid expense saved successfully")
                else ->
                    tr(isAr(), "تم حفظ الإيراد المكتسب بنجاح", "Earned income saved successfully")
            }
            emitToast(label)
        }
    }

    fun setDebtSettled(id: Long, settled: Boolean) {
        safeIoLaunch {
            repository.setDebtSettled(id, settled)
            emitToast(
                if (settled) tr(isAr(), "تم الدفع ونقل العملية إلى المدفوعات ✓", "Paid & moved to Payments ✓")
                else tr(isAr(), "تمت إعادة العملية إلى المديونيات القائمة", "Moved back to active debts")
            )
        }
    }

    fun deleteTransaction(id: Long) {
        safeIoLaunch {
            repository.deleteTransaction(id)
            emitToast(tr(isAr(), "تم حذف العملية بنجاح", "Transaction deleted"))
        }
    }

    fun deleteTransactions(ids: List<Long>) {
        if (ids.isEmpty()) return
        safeIoLaunch {
            repository.deleteTransactions(ids)
            emitToast(
                tr(
                    isAr(),
                    "تم حذف ${ids.size} عمليات وتحديث الرصيد",
                    "Deleted ${ids.size} transactions"
                )
            )
        }
    }

    fun deleteCurrentWeekTransactions() {
        safeIoLaunch {
            val (start, end) = currentWeekRange()
            repository.deleteTransactionsInRange(start, end)
            emitToast(tr(isAr(), "تم مسح عمليات الأسبوع الحالي", "Current week transactions deleted"))
        }
    }

    fun deleteCurrentMonthTransactions() {
        safeIoLaunch {
            val (start, end) = currentMonthRange()
            repository.deleteTransactionsInRange(start, end)
            emitToast(tr(isAr(), "تم مسح عمليات الشهر الحالي", "Current month transactions deleted"))
        }
    }

    fun deleteTransactionsOlderThanMonths(months: Int) {
        safeIoLaunch {
            val cutoff = isoDateMonthsAgo(months)
            repository.deleteTransactionsOlderThan(cutoff)
            emitToast(
                tr(
                    isAr(),
                    "تم حذف العمليات الأقدم من $months شهور",
                    "Deleted transactions older than $months months"
                )
            )
        }
    }

    fun deleteTransactionsByCategory(category: String) {
        safeIoLaunch {
            repository.deleteTransactionsByCategory(category)
            emitToast(
                tr(
                    isAr(),
                    "تم حذف عمليات تصنيف ($category)",
                    "Deleted transactions in $category"
                )
            )
        }
    }

    fun wipeAllTransactions() {
        safeIoLaunch {
            repository.wipeAllTransactions()
            emitToast(
                tr(
                    isAr(),
                    "تم مسح كافة العمليات وتصفير الأرصدة",
                    "All transactions wiped and balances reset"
                )
            )
        }
    }

    // --- Report Filters ---
    fun updateReportFilter(transform: (ReportFilterState) -> ReportFilterState) {
        _reportFilter.value = transform(_reportFilter.value)
    }

    fun resetReportFilter() {
        _reportFilter.value = ReportFilterState()
        emitToast(tr(isAr(), "تم إعادة ضبط الفلاتر", "Filters reset"))
    }

    fun saveCurrentFilter(name: String, asDefault: Boolean) {
        safeIoLaunch {
            val finalName = name.trim().ifBlank {
                if (asDefault) tr(isAr(), "الفلتر الافتراضي", "Default Filter")
                else tr(isAr(), "فلتر محفوظ", "Saved Filter")
            }
            repository.saveFilter(finalName, _reportFilter.value.toJson(), asDefault)
            emitToast(
                if (asDefault) tr(isAr(), "تم حفظ الفلاتر كإعدادات افتراضية", "Saved as default filters")
                else tr(isAr(), "تم حفظ الفلتر: $finalName", "Filter saved: $finalName")
            )
        }
    }

    fun applySavedFilter(saved: SavedFilterEntity) {
        runCatching {
            _reportFilter.value = ReportFilterState.fromJson(saved.filterConfigJson)
            emitToast(tr(isAr(), "تم تطبيق الفلتر: ${saved.name}", "Applied filter: ${saved.name}"))
        }
    }

    fun deleteSavedFilter(id: Long) {
        safeIoLaunch {
            repository.deleteSavedFilter(id)
            emitToast(tr(isAr(), "تم حذف الفلتر المحفوظ", "Saved filter removed"))
        }
    }

    // --- Categories & Accounts ---
    fun addCategory(name: String, iconKey: String, kind: String) {
        if (name.isBlank()) return
        safeIoLaunch {
            repository.addCategory(name, iconKey, kind)
            emitToast(tr(isAr(), "تمت إضافة التصنيف بنجاح", "Category added"))
        }
    }

    fun updateCategory(category: CategoryEntity) {
        safeIoLaunch {
            repository.updateCategory(category)
            emitToast(tr(isAr(), "تم تحديث التصنيف", "Category updated"))
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        safeIoLaunch {
            repository.deleteCategory(category)
            emitToast(tr(isAr(), "تم حذف التصنيف", "Category deleted"))
        }
    }

    fun addAccount(name: String, iconKey: String, currency: String) {
        if (name.isBlank()) return
        safeIoLaunch {
            repository.addAccount(name, iconKey, currency)
            emitToast(tr(isAr(), "تمت إضافة الحساب بنجاح", "Account added"))
        }
    }

    fun deleteAccount(account: AccountEntity) {
        safeIoLaunch {
            repository.deleteAccount(account)
            emitToast(tr(isAr(), "تم حذف الحساب", "Account deleted"))
        }
    }

    // --- Settings ---
    fun toggleDarkMode() {
        safeIoLaunch {
            repository.updateSettings { s ->
                val next = if (s.themeMode == "dark") "light" else "dark"
                s.copy(themeMode = next)
            }
        }
    }

    fun setThemeMode(mode: String) {
        safeIoLaunch {
            repository.updateSettings { it.copy(themeMode = mode) }
        }
    }

    fun toggleLanguage() {
        safeIoLaunch {
            repository.updateSettings { s ->
                s.copy(language = if (s.language == "ar") "en" else "ar")
            }
        }
    }

    fun setLanguage(lang: String) {
        safeIoLaunch {
            repository.updateSettings { it.copy(language = lang) }
        }
    }

    fun setDefaultCurrency(currency: String) {
        safeIoLaunch {
            repository.updateSettings { it.copy(defaultCurrency = currency) }
            emitToast(tr(isAr(), "تم تغيير العملة الافتراضية إلى $currency", "Default currency set to $currency"))
        }
    }

    fun setQuickDeleteMode(enabled: Boolean) {
        safeIoLaunch {
            repository.updateSettings { it.copy(quickDeleteMode = enabled) }
            emitToast(
                if (enabled) tr(isAr(), "تم تفعيل وضع الحذف السريع", "Quick delete mode enabled")
                else tr(isAr(), "تم تعطيل وضع الحذف السريع", "Quick delete mode disabled")
            )
        }
    }

    fun setPinProtection(enabled: Boolean, pinCode: String? = null) {
        safeIoLaunch {
            repository.updateSettings { s ->
                s.copy(
                    pinEnabled = enabled,
                    pinCode = pinCode ?: s.pinCode
                )
            }
            emitToast(
                if (enabled) tr(isAr(), "تم تفعيل الحماية برمز PIN", "PIN protection enabled")
                else tr(isAr(), "تم تعطيل الحماية برمز PIN", "PIN protection disabled")
            )
        }
    }

    fun setAutoLockSensitive(enabled: Boolean) {
        safeIoLaunch {
            repository.updateSettings { it.copy(autoLockSensitive = enabled, lockOnLaunch = enabled) }
            emitToast(
                if (enabled) tr(isAr(), "تم تفعيل القفل عند فتح التطبيق", "App launch lock enabled")
                else tr(isAr(), "تم تعطيل القفل عند فتح التطبيق", "App launch lock disabled")
            )
        }
    }

    fun setBiometricProtection(enabled: Boolean) {
        safeIoLaunch {
            repository.updateSettings { it.copy(biometricEnabled = enabled) }
            emitToast(
                if (enabled) tr(isAr(), "تم تفعيل البصمة وقفل/نمط الجهاز", "Biometric & device lock enabled")
                else tr(isAr(), "تم تعطيل البصمة وقفل الجهاز", "Biometric & device lock disabled")
            )
        }
    }

    fun setPatternProtection(enabled: Boolean, patternCode: String? = null) {
        safeIoLaunch {
            repository.updateSettings { s ->
                s.copy(
                    patternEnabled = enabled,
                    patternCode = patternCode ?: s.patternCode
                )
            }
            emitToast(
                if (enabled) tr(isAr(), "تم تفعيل النمط الخاص بنجاح", "Custom pattern lock enabled")
                else tr(isAr(), "تم تعطيل النمط الخاص", "Custom pattern lock disabled")
            )
        }
    }

    fun setConfirmPaymentWithSecurity(enabled: Boolean) {
        safeIoLaunch {
            repository.updateSettings { it.copy(confirmPaymentWithSecurity = enabled) }
            emitToast(
                if (enabled) tr(isAr(), "تم تفعيل تأكيد عمليات الدفع بالحماية", "Payment verification enabled")
                else tr(isAr(), "تم تعطيل تأكيد الدفع بالحماية", "Payment verification disabled")
            )
        }
    }

    fun requestPinHomeWidget() {
        val ok = runCatching {
            MasrofyWidgetProvider.requestPinWidget(getApplication())
        }.getOrDefault(false)
        emitToast(
            if (ok) {
                tr(
                    isArabic = isAr(),
                    ar = "أكّد إضافة أيقونة «مصروفي» من النافذة التي ظهرت لتثبيتها على سطح المكتب",
                    en = "Confirm adding the Masrofy icon to pin it to your Home Screen"
                )
            } else {
                tr(
                    isArabic = isAr(),
                    ar = "اضغط مطولاً على أيقونة «مصروفي» في قائمة التطبيقات واسحبها إلى الشاشة الرئيسية",
                    en = "Long-press the Masrofy app icon and drag it to your Home Screen"
                )
            }
        )
    }

    fun setReminderSettings(enabled: Boolean, time: String, monthlyLimitMinor: Long) {
        safeIoLaunch {
            repository.updateSettings {
                it.copy(
                    dailyReminderEnabled = enabled,
                    dailyReminderTime = time,
                    monthlyBudgetLimitMinor = monthlyLimitMinor
                )
            }
            emitToast(tr(isAr(), "تم حفظ إعدادات التذكير والميزانية", "Reminder & budget settings saved"))
        }
    }

    fun updateUserName(rawName: String) {
        val cleaned = rawName.trim()
        safeIoLaunch(
            failureAr = "تعذر حفظ الاسم الشخصي، حاول مرة أخرى",
            failureEn = "Failed to save profile name"
        ) {
            repository.updateSettings { it.copy(userName = cleaned) }
            emitToast(
                if (cleaned.isNotEmpty()) tr(isAr(), "تم حفظ الاسم الشخصي بنجاح", "Profile name saved")
                else tr(isAr(), "تم مسح الاسم الشخصي", "Profile name cleared")
            )
        }
    }

    fun updateUserAvatar(uri: Uri) {
        safeIoLaunch(
            failureAr = "تعذر تحميل الصورة المختارة، جرب صورة أخرى",
            failureEn = "Failed to load selected photo, try another image"
        ) {
            val ctx = getApplication<Application>()
            val oldPath = settings.value.userAvatarPath
            val savedPath = savePickedAvatarToInternalStorage(ctx, uri, oldPath)
            if (savedPath != null) {
                repository.updateSettings { it.copy(userAvatarPath = savedPath) }
                emitToast(tr(isAr(), "تم تحديث الصورة الشخصية بنجاح", "Profile photo updated"))
            } else {
                emitToast(tr(isAr(), "تعذر تحميل الصورة المختارة", "Failed to load selected photo"))
            }
        }
    }

    fun removeUserAvatar() {
        safeIoLaunch {
            val oldPath = settings.value.userAvatarPath
            deleteLocalAvatarFile(oldPath)
            repository.updateSettings { it.copy(userAvatarPath = "") }
            emitToast(tr(isAr(), "تمت إزالة الصورة الشخصية", "Profile photo removed"))
        }
    }

    // --- Export & Direct Download Actions (Saves directly to Downloads/Pictures WITHOUT forcing share!) ---
    fun exportTransactionsCsv(list: List<TransactionEntity>) {
        if (list.isEmpty()) {
            emitToast(tr(isAr(), "لا توجد عمليات للتصدير حالياً", "No transactions to export"))
            return
        }
        safeIoLaunch(
            failureAr = "تعذر حفظ ملف CSV",
            failureEn = "Failed to save CSV"
        ) {
            val ctx = getApplication<Application>()
            val file = ExportManager.exportCsv(ctx, list, isAr())
            val savedPath = ExportManager.saveToDeviceDownloads(ctx, file, "text/csv", isImage = false, isArabic = isAr())
            emitToast(
                if (savedPath != null) tr(isAr(), "تم تنزيل CSV مباشرة: $savedPath", "CSV downloaded: $savedPath")
                else tr(isAr(), "تعذر حفظ ملف CSV", "Failed to save CSV")
            )
        }
    }

    fun exportTransactionsExcel(list: List<TransactionEntity>) {
        if (list.isEmpty()) {
            emitToast(tr(isAr(), "لا توجد عمليات للتصدير حالياً", "No transactions to export"))
            return
        }
        safeIoLaunch(
            failureAr = "تعذر حفظ ملف Excel",
            failureEn = "Failed to save Excel"
        ) {
            val ctx = getApplication<Application>()
            val file = ExportManager.exportExcelWorkbook(ctx, list, isAr())
            val savedPath = ExportManager.saveToDeviceDownloads(ctx, file, "application/vnd.ms-excel", isImage = false, isArabic = isAr())
            emitToast(
                if (savedPath != null) tr(isAr(), "تم تنزيل ملف Excel مباشرة: $savedPath", "Excel downloaded: $savedPath")
                else tr(isAr(), "تعذر حفظ ملف Excel", "Failed to save Excel")
            )
        }
    }

    fun exportAllExcelDirect() {
        exportTransactionsExcel(transactions.value)
    }

    fun exportFilteredPdf(filterSummary: String) {
        val analytics = reportAnalytics.value
        if (analytics.filteredTransactions.isEmpty()) {
            emitToast(tr(isAr(), "لا توجد نتائج مفلترة لتصديرها", "No filtered results to export"))
            return
        }
        safeIoLaunch(
            failureAr = "تعذر حفظ تقرير PDF",
            failureEn = "Failed to save PDF"
        ) {
            val ctx = getApplication<Application>()
            val file = ExportManager.exportPdfReport(ctx, analytics, filterSummary, isAr())
            val savedPath = ExportManager.saveToDeviceDownloads(ctx, file, "application/pdf", isImage = false, isArabic = isAr())
            emitToast(
                if (savedPath != null) tr(isAr(), "تم تنزيل PDF مباشرة: $savedPath", "PDF downloaded: $savedPath")
                else tr(isAr(), "تعذر حفظ تقرير PDF", "Failed to save PDF")
            )
        }
    }

    fun exportAllPdfDirect() {
        safeIoLaunch(
            failureAr = "تعذر حفظ ملف PDF",
            failureEn = "Failed to save PDF"
        ) {
            val ctx = getApplication<Application>()
            val allAnalytics = homeAnalytics.value
            val file = ExportManager.exportPdfReport(
                ctx,
                allAnalytics,
                tr(isAr(), "نسخة كاملة لكل العمليات", "Full Ledger Backup"),
                isAr()
            )
            val savedPath = ExportManager.saveToDeviceDownloads(ctx, file, "application/pdf", isImage = false, isArabic = isAr())
            emitToast(
                if (savedPath != null) tr(isAr(), "تم حفظ PDF مباشرة: $savedPath", "PDF saved: $savedPath")
                else tr(isAr(), "تعذر حفظ ملف PDF", "Failed to save PDF")
            )
        }
    }

    fun exportFilteredImage(filterSummary: String) {
        val analytics = reportAnalytics.value
        if (analytics.filteredTransactions.isEmpty()) {
            emitToast(tr(isAr(), "لا توجد نتائج مفلترة لتصديرها", "No filtered results to export"))
            return
        }
        safeIoLaunch(
            failureAr = "تعذر حفظ صورة التقرير",
            failureEn = "Failed to save image"
        ) {
            val ctx = getApplication<Application>()
            val file = ExportManager.exportReportImage(ctx, analytics, filterSummary, isAr())
            val savedPath = ExportManager.saveToDeviceDownloads(ctx, file, "image/png", isImage = true, isArabic = isAr())
            emitToast(
                if (savedPath != null) tr(isAr(), "تم حفظ الصورة مباشرة: $savedPath", "Image saved: $savedPath")
                else tr(isAr(), "تعذر حفظ صورة التقرير", "Failed to save image")
            )
        }
    }

    fun exportAllImageDirect() {
        safeIoLaunch(
            failureAr = "تعذر حفظ الصورة",
            failureEn = "Failed to save image"
        ) {
            val ctx = getApplication<Application>()
            val allAnalytics = homeAnalytics.value
            val file = ExportManager.exportReportImage(
                ctx,
                allAnalytics,
                tr(isAr(), "ملخص كامل للحساب", "Full Ledger Summary"),
                isAr()
            )
            val savedPath = ExportManager.saveToDeviceDownloads(ctx, file, "image/png", isImage = true, isArabic = isAr())
            emitToast(
                if (savedPath != null) tr(isAr(), "تم حفظ الصورة مباشرة: $savedPath", "Image saved: $savedPath")
                else tr(isAr(), "تعذر حفظ الصورة", "Failed to save image")
            )
        }
    }

    fun writeReportPdfToUri(uri: Uri) {
        safeIoLaunch(
            failureAr = "تعذر حفظ ملف PDF",
            failureEn = "Failed to save PDF"
        ) {
            val ctx = getApplication<Application>()
            val allAnalytics = homeAnalytics.value
            val file = ExportManager.exportPdfReport(
                ctx,
                allAnalytics,
                tr(isAr(), "نسخة كاملة لكل العمليات", "Full Ledger Backup"),
                isAr()
            )
            val ok = ExportManager.writeFileToUri(ctx, file, uri)
            emitToast(
                if (ok) tr(isAr(), "تم حفظ نسخة PDF بنجاح ✓", "PDF backup saved ✓")
                else tr(isAr(), "تعذر حفظ ملف PDF", "Failed to save PDF")
            )
        }
    }

    fun writeReportExcelToUri(uri: Uri) {
        safeIoLaunch(
            failureAr = "تعذر حفظ ملف Excel",
            failureEn = "Failed to save Excel"
        ) {
            val ctx = getApplication<Application>()
            val list = transactions.value
            val file = ExportManager.exportExcelWorkbook(ctx, list, isAr())
            val ok = ExportManager.writeFileToUri(ctx, file, uri)
            emitToast(
                if (ok) tr(isAr(), "تم حفظ نسخة Excel بنجاح ✓", "Excel backup saved ✓")
                else tr(isAr(), "تعذر حفظ ملف Excel", "Failed to save Excel")
            )
        }
    }

    fun writeReportImageToUri(uri: Uri) {
        safeIoLaunch(
            failureAr = "تعذر حفظ الصورة",
            failureEn = "Failed to save image"
        ) {
            val ctx = getApplication<Application>()
            val allAnalytics = homeAnalytics.value
            val file = ExportManager.exportReportImage(
                ctx,
                allAnalytics,
                tr(isAr(), "ملخص كامل للحساب", "Full Ledger Summary"),
                isAr()
            )
            val ok = ExportManager.writeFileToUri(ctx, file, uri)
            emitToast(
                if (ok) tr(isAr(), "تم حفظ صورة الملخص بنجاح ✓", "Image summary saved ✓")
                else tr(isAr(), "تعذر حفظ الصورة", "Failed to save image")
            )
        }
    }

    fun shareJsonBackup() {
        safeIoLaunch(
            failureAr = "تعذر حفظ النسخة الاحتياطية",
            failureEn = "Failed to save backup"
        ) {
            val ctx = getApplication<Application>()
            val json = repository.exportBackupJson()
            val file = ExportManager.exportJsonBackupFile(ctx, json)
            val savedPath = ExportManager.saveToDeviceDownloads(ctx, file, "application/json", isImage = false, isArabic = isAr())
            emitToast(
                if (savedPath != null) tr(isAr(), "تم تنزيل النسخة الاحتياطية: $savedPath", "Backup saved: $savedPath")
                else tr(isAr(), "تعذر حفظ النسخة الاحتياطية", "Failed to save backup")
            )
        }
    }

    fun writeJsonBackupToUri(uri: Uri) {
        safeIoLaunch(
            failureAr = "تعذر حفظ النسخة الاحتياطية",
            failureEn = "Failed to save backup"
        ) {
            val ctx = getApplication<Application>()
            val json = repository.exportBackupJson()
            val bytes = json.toByteArray(Charsets.UTF_8)
            val written = (ctx.contentResolver.openOutputStream(uri, "wt")
                ?: ctx.contentResolver.openOutputStream(uri))?.use { out ->
                out.write(bytes)
                out.flush()
                true
            } ?: false
            emitToast(
                if (written) tr(isAr(), "تم حفظ النسخة الاحتياطية بنجاح ✓", "Backup saved successfully ✓")
                else tr(isAr(), "تعذر حفظ النسخة الاحتياطية", "Failed to save backup")
            )
        }
    }

    fun restoreJsonBackupFromUri(uri: Uri) {
        safeIoLaunch(
            failureAr = "ملف النسخة الاحتياطية غير صالح",
            failureEn = "Invalid backup file"
        ) {
            val ctx = getApplication<Application>()
            val content = ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            } ?: error("Empty file")
            val count = repository.restoreBackupJson(content)
            emitToast(
                tr(
                    isAr(),
                    "تمت الاستعادة بنجاح ($count عملية)",
                    "Restored successfully ($count transactions)"
                )
            )
        }
    }

    // --- Cloud Backup (Firebase Firestore, scoped strictly to signed-in Google email) ---
    fun clearCloudBackupState() {
        _cloudBackupSummary.value = null
        _cloudBackupError.value = null
        _isCloudBackupBusy.value = false
    }

    fun setCloudBackupError(message: String?) {
        _cloudBackupError.value = message
    }

    fun loadCloudBackupSummary() {
        viewModelScope.launch(Dispatchers.IO + coroutineErrorHandler) {
            _isCloudBackupBusy.value = true
            _cloudBackupError.value = null
            try {
                val res = cloudBackupRepo.fetchBackupSummary()
                res.onSuccess { summary ->
                    _cloudBackupSummary.value = summary
                }.onFailure { err ->
                    _cloudBackupError.value = tr(
                        isAr(),
                        "تعذر قراءة حالة النسخة السحابية. تأكد من الاتصال بالإنترنت.",
                        "Failed to load cloud backup info: ${err.localizedMessage ?: "Network error"}"
                    )
                }
            } finally {
                _isCloudBackupBusy.value = false
            }
        }
    }

    fun saveCloudBackupToSignedInEmail(ownerEmail: String) {
        viewModelScope.launch(Dispatchers.IO + coroutineErrorHandler) {
            _isCloudBackupBusy.value = true
            _cloudBackupError.value = null
            try {
                val snapshot = repository.getFullLocalSnapshot()
                val res = cloudBackupRepo.uploadBackup(snapshot, ownerEmail)
                res.onSuccess { summary ->
                    _cloudBackupSummary.value = summary
                    emitToast(
                        tr(
                            isAr(),
                            "تم حفظ النسخة الاحتياطية على الإيميل بنجاح (${summary.transactionCount} عملية) ✓",
                            "Cloud backup saved (${summary.transactionCount} items) ✓"
                        )
                    )
                }.onFailure {
                    _cloudBackupError.value = tr(
                        isAr(),
                        "تعذر حفظ النسخة على الإيميل. تأكد من اتصال الإنترنت وحاول مرة أخرى.",
                        "Failed to save cloud backup. Please check your connection and try again."
                    )
                }
            } finally {
                _isCloudBackupBusy.value = false
            }
        }
    }

    fun restoreCloudBackupFromSignedInEmail() {
        viewModelScope.launch(Dispatchers.IO + coroutineErrorHandler) {
            _isCloudBackupBusy.value = true
            _cloudBackupError.value = null
            try {
                val res = cloudBackupRepo.downloadBackup()
                res.onSuccess { payload ->
                    if (payload == null) {
                        _cloudBackupError.value = tr(
                            isAr(),
                            "لا توجد نسخة احتياطية محفوظة لهذا الإيميل بعد.",
                            "No cloud backup found for this email yet."
                        )
                    } else {
                        val s = payload.summary
                        val restoredCount = repository.restoreFromCloudEntities(
                            importedTx = payload.transactions,
                            importedCats = payload.categories,
                            importedAccs = payload.accounts,
                            defaultCurrency = s.defaultCurrency,
                            language = s.language,
                            themeMode = s.themeMode,
                            quickDeleteMode = s.quickDeleteMode,
                            dailyReminderEnabled = s.dailyReminderEnabled,
                            dailyReminderTime = s.dailyReminderTime,
                            monthlyBudgetLimitMinor = s.monthlyBudgetLimitMinor,
                            lastSelectedSource = s.lastSelectedSource,
                            userName = s.userName
                        )
                        _cloudBackupSummary.value = s
                        emitToast(
                            tr(
                                isAr(),
                                "تمت استعادة البيانات من الإيميل بنجاح ($restoredCount عملية) ✓",
                                "Restored from email successfully ($restoredCount items) ✓"
                            )
                        )
                    }
                }.onFailure {
                    _cloudBackupError.value = tr(
                        isAr(),
                        "تعذر استعادة النسخة من الإيميل. تأكد من الاتصال بالإنترنت وحاول مرة أخرى.",
                        "Failed to restore cloud backup. Please check your connection."
                    )
                }
            } finally {
                _isCloudBackupBusy.value = false
            }
        }
    }
}
