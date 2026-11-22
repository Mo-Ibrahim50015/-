package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class MizanityRepository(private val db: AppDatabase) {
    private val txDao = db.transactionDao()
    private val catDao = db.categoryDao()
    private val accDao = db.accountDao()
    private val filterDao = db.savedFilterDao()
    private val settingsDao = db.settingsDao()
    private val obligationDao = db.obligationDao()

    val transactions: Flow<List<TransactionEntity>> = txDao.observeAll()
    val obligations: Flow<List<ObligationEntity>> = obligationDao.observeAll()
    val categories: Flow<List<CategoryEntity>> = catDao.observeAll()
    val accounts: Flow<List<AccountEntity>> = accDao.observeAll()
    val savedFilters: Flow<List<SavedFilterEntity>> = filterDao.observeAll()
    val settings: Flow<AppSettingsEntity> = settingsDao.observeSettings().map {
        it ?: AppSettingsEntity()
    }

    suspend fun ensureSeedData() {
        if (settingsDao.getSettingsOnce() == null) {
            settingsDao.upsert(AppSettingsEntity())
        }
        if (catDao.count() == 0) {
            catDao.insertAll(defaultCategories())
        }
        if (accDao.count() == 0) {
            accDao.insertAll(defaultAccounts())
        }
    }

    suspend fun addTransaction(tx: TransactionEntity): Long {
        // Auto-register custom category if not already present
        val existingCats = catDao.getAllOnce()
        if (tx.category.isNotBlank() && tx.category != "غير محدد" &&
            existingCats.none { it.name.equals(tx.category, ignoreCase = true) && it.kind == tx.type }
        ) {
            catDao.insert(
                CategoryEntity(
                    name = tx.category,
                    iconKey = if (tx.type == TransactionType.INCOME) "payments" else "category",
                    kind = tx.type
                )
            )
        }
        // Auto-register custom account/source if not already present
        val existingAccs = accDao.getAllOnce()
        if (tx.source.isNotBlank() &&
            existingAccs.none { it.name.equals(tx.source, ignoreCase = true) }
        ) {
            accDao.insert(
                AccountEntity(
                    name = tx.source,
                    iconKey = "wallet",
                    currency = tx.currency
                )
            )
        }
        // Remember last used source
        val currentSettings = settingsDao.getSettingsOnce() ?: AppSettingsEntity()
        if (currentSettings.lastSelectedSource != tx.source) {
            settingsDao.upsert(currentSettings.copy(lastSelectedSource = tx.source))
        }
        return txDao.insert(tx)
    }

    suspend fun updateTransaction(tx: TransactionEntity) = txDao.update(tx)
    suspend fun getTransactionByIdOnce(id: Long): TransactionEntity? = txDao.getByIdOnce(id)
    suspend fun setDebtSettled(id: Long, settled: Boolean) = txDao.setDebtSettled(id, settled)
    suspend fun setReminderMuted(id: Long, muted: Boolean) = txDao.setReminderMuted(id, muted)
    suspend fun updateTransactionReminderSettings(
        id: Long,
        enabled: Boolean,
        dueDate: String,
        dueTime: String,
        leadMinutes: Int,
        muted: Boolean,
        alertMode: String,
        fullScreen: Boolean,
        speakAloud: Boolean
    ) = txDao.updateReminderSettings(id, enabled, dueDate, dueTime, leadMinutes, muted, alertMode, fullScreen, speakAloud)
    suspend fun deleteTransaction(id: Long) = txDao.deleteById(id)
    suspend fun deleteTransactions(ids: List<Long>) = txDao.deleteByIds(ids)
    suspend fun deleteTransactionsInRange(startDate: String, endDate: String) =
        txDao.deleteInDateRange(startDate, endDate)

    suspend fun deleteTransactionsOlderThan(cutoffDate: String) =
        txDao.deleteOlderThan(cutoffDate)

    suspend fun deleteTransactionsByCategory(category: String) =
        txDao.deleteByCategory(category)

    suspend fun deleteTransactionsBySource(source: String) =
        txDao.deleteBySource(source)

    suspend fun deleteTransactionsByType(type: String) =
        txDao.deleteByType(type)

    suspend fun deleteEarnedIncomeOnly() =
        txDao.deleteEarnedIncomeOnly()

    suspend fun deleteUnpaidIncomeDebts() =
        txDao.deleteUnpaidIncomeDebts()

    suspend fun deleteSettledIncomeDebts() =
        txDao.deleteSettledIncomeDebts()

    suspend fun deletePaidExpensesOnly() =
        txDao.deletePaidExpensesOnly()

    suspend fun deleteUnpaidExpenseDebts() =
        txDao.deleteUnpaidExpenseDebts()

    suspend fun deleteSettledExpenseDebts() =
        txDao.deleteSettledExpenseDebts()

    suspend fun deleteUnpaidDebts() =
        txDao.deleteUnpaidDebts()

    suspend fun deleteSettledDebts() =
        txDao.deleteSettledDebts()

    suspend fun wipeAllTransactions() = txDao.deleteAll()

    // --- Scheduled Obligations Due to Pay («مبالغ مطلوب دفعها») ---
    suspend fun addObligation(obligation: ObligationEntity): Long = obligationDao.insert(obligation)
    suspend fun getObligationByIdOnce(id: Long): ObligationEntity? = obligationDao.getByIdOnce(id)
    suspend fun getAllObligationsOnce(): List<ObligationEntity> = obligationDao.getAllOnce()
    suspend fun setObligationPaid(id: Long, paid: Boolean, paidDate: String, paidSource: String) =
        obligationDao.setPaidStatus(id, paid, paidDate, paidSource)
    suspend fun setObligationReminderMuted(id: Long, muted: Boolean) =
        obligationDao.setReminderMuted(id, muted)
    suspend fun updateObligationReminderSettings(
        id: Long,
        enabled: Boolean,
        dueDate: String,
        dueTime: String,
        leadMinutes: Int,
        muted: Boolean,
        alertMode: String,
        fullScreen: Boolean,
        speakAloud: Boolean
    ) = obligationDao.updateReminderSettings(id, enabled, dueDate, dueTime, leadMinutes, muted, alertMode, fullScreen, speakAloud)
    suspend fun deleteObligation(id: Long) = obligationDao.deleteById(id)
    suspend fun deletePendingObligationsOnly() = obligationDao.deletePendingOnly()
    suspend fun deletePaidObligationsOnly() = obligationDao.deletePaidOnly()
    suspend fun wipeAllObligations() = obligationDao.deleteAll()

    suspend fun clearUnscheduledTransactionDueDates() = txDao.clearUnscheduledDueDates()
    suspend fun clearAllDebtTransactionReminders() = txDao.clearAllDebtTransactionReminders()
    suspend fun unifyScheduledReminderTimes(newTime24: String, includeScheduledTransactions: Boolean = true) {
        val cleanTime = newTime24.trim().ifBlank { "09:00" }
        obligationDao.unifyPendingObligationDueTime(cleanTime)
        if (includeScheduledTransactions) {
            txDao.unifyScheduledTransactionDueTime(cleanTime)
        }
    }

    suspend fun addCategory(name: String, iconKey: String, kind: String) {
        catDao.insert(CategoryEntity(name = name.trim(), iconKey = iconKey, kind = kind))
    }

    suspend fun updateCategory(category: CategoryEntity) = catDao.update(category)
    suspend fun deleteCategory(category: CategoryEntity) = catDao.delete(category)

    suspend fun addAccount(name: String, iconKey: String, currency: String) {
        accDao.insert(AccountEntity(name = name.trim(), iconKey = iconKey, currency = currency))
    }

    suspend fun deleteAccount(account: AccountEntity) = accDao.delete(account)

    suspend fun saveFilter(name: String, configJson: String, isDefault: Boolean) {
        if (isDefault) {
            filterDao.clearDefaultFlag()
        }
        filterDao.insert(
            SavedFilterEntity(
                name = name.trim(),
                filterConfigJson = configJson,
                isDefault = isDefault
            )
        )
    }

    suspend fun getDefaultFilter(): SavedFilterEntity? = filterDao.getDefaultFilter()
    suspend fun deleteSavedFilter(id: Long) = filterDao.deleteById(id)

    suspend fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        val current = settingsDao.getSettingsOnce() ?: AppSettingsEntity()
        settingsDao.upsert(transform(current))
    }

    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("app", "Mizanity")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val txArr = JSONArray()
        txDao.getAllOnce().forEach { tx ->
            txArr.put(JSONObject().apply {
                put("type", tx.type)
                put("amountMinor", tx.amountMinor)
                put("currency", tx.currency)
                put("source", tx.source)
                put("category", tx.category)
                put("date", tx.date)
                put("time", tx.time)
                put("note", tx.note)
                put("createdAt", tx.createdAt)
                put("isDebt", tx.isDebt)
                put("isDebtSettled", tx.isDebtSettled)
                put("itemName", tx.itemName)
                put("debtParty", tx.debtParty)
                put("hasPaymentReminder", tx.hasPaymentReminder)
                put("paymentDueDate", tx.paymentDueDate)
                put("paymentDueTime", tx.paymentDueTime)
                put("reminderLeadMinutes", tx.reminderLeadMinutes)
                put("reminderMuted", tx.reminderMuted)
                put("reminderAlertMode", tx.reminderAlertMode)
                put("reminderFullScreen", tx.reminderFullScreen)
                put("reminderSpeakAloud", tx.reminderSpeakAloud)
            })
        }
        root.put("transactions", txArr)

        val catArr = JSONArray()
        catDao.getAllOnce().forEach { c ->
            catArr.put(JSONObject().apply {
                put("name", c.name)
                put("iconKey", c.iconKey)
                put("kind", c.kind)
            })
        }
        root.put("categories", catArr)

        val accArr = JSONArray()
        accDao.getAllOnce().forEach { a ->
            accArr.put(JSONObject().apply {
                put("name", a.name)
                put("iconKey", a.iconKey)
                put("currency", a.currency)
            })
        }
        root.put("accounts", accArr)

        val filterArr = JSONArray()
        filterDao.getAllOnce().forEach { f ->
            filterArr.put(JSONObject().apply {
                put("name", f.name)
                put("filterConfigJson", f.filterConfigJson)
                put("isDefault", f.isDefault)
                put("createdAt", f.createdAt)
            })
        }
        root.put("savedFilters", filterArr)

        val obArr = JSONArray()
        obligationDao.getAllOnce().forEach { ob ->
            obArr.put(JSONObject().apply {
                put("amountMinor", ob.amountMinor)
                put("currency", ob.currency)
                put("reason", ob.reason)
                put("party", ob.party)
                put("dueDate", ob.dueDate)
                put("dueTime", ob.dueTime)
                put("reminderEnabled", ob.reminderEnabled)
                put("reminderLeadMinutes", ob.reminderLeadMinutes)
                put("reminderMuted", ob.reminderMuted)
                put("isPaid", ob.isPaid)
                put("paidDate", ob.paidDate)
                put("paidSource", ob.paidSource)
                put("note", ob.note)
                put("createdAt", ob.createdAt)
                put("reminderAlertMode", ob.reminderAlertMode)
                put("reminderFullScreen", ob.reminderFullScreen)
                put("reminderSpeakAloud", ob.reminderSpeakAloud)
            })
        }
        root.put("obligations", obArr)

        val s = settingsDao.getSettingsOnce() ?: AppSettingsEntity()
        root.put("settings", JSONObject().apply {
            put("defaultCurrency", s.defaultCurrency)
            put("language", s.language)
            put("themeMode", s.themeMode)
            put("quickDeleteMode", s.quickDeleteMode)
            put("dailyReminderEnabled", s.dailyReminderEnabled)
            put("dailyReminderTime", s.dailyReminderTime)
            put("monthlyBudgetLimitMinor", s.monthlyBudgetLimitMinor)
            put("lastSelectedSource", s.lastSelectedSource)
            put("notificationsEnabled", s.notificationsEnabled)
            put("customGreetingText", s.customGreetingText)
            put("customMorningGreeting", s.customMorningGreeting)
            put("customEveningGreeting", s.customEveningGreeting)
            put("showBaseGreeting", s.showBaseGreeting)
            put("showCustomGreeting", s.showCustomGreeting)
            put("transactionsViewMode", s.transactionsViewMode)
            put("defaultAlertMode", s.defaultAlertMode)
            put("fullScreenAlertsEnabled", s.fullScreenAlertsEnabled)
            put("speakRemindersAloud", s.speakRemindersAloud)
            put("ringtoneAlertsEnabled", s.ringtoneAlertsEnabled)
        })

        return root.toString(2)
    }

    suspend fun restoreBackupJson(jsonText: String): Int {
        val root = JSONObject(jsonText)
        val txArr = root.optJSONArray("transactions") ?: JSONArray()
        val importedTx = mutableListOf<TransactionEntity>()
        for (i in 0 until txArr.length()) {
            val o = txArr.getJSONObject(i)
            importedTx.add(
                TransactionEntity(
                    type = o.optString("type", TransactionType.EXPENSE),
                    amountMinor = o.optLong("amountMinor", 0L),
                    currency = o.optString("currency", "EGP"),
                    source = o.optString("source", "كاش / نقدي"),
                    category = o.optString("category", "غير محدد"),
                    date = o.optString("date", "2026-01-01"),
                    time = o.optString("time", "12:00"),
                    note = o.optString("note", ""),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    isDebt = o.optBoolean("isDebt", false),
                    isDebtSettled = o.optBoolean("isDebtSettled", false),
                    itemName = o.optString("itemName", ""),
                    debtParty = o.optString("debtParty", ""),
                    hasPaymentReminder = o.optBoolean("hasPaymentReminder", false),
                    paymentDueDate = o.optString("paymentDueDate", ""),
                    paymentDueTime = o.optString("paymentDueTime", ""),
                    reminderLeadMinutes = o.optInt("reminderLeadMinutes", 60),
                    reminderMuted = o.optBoolean("reminderMuted", false),
                    reminderAlertMode = o.optString("reminderAlertMode", ReminderAlertMode.NOTIFICATION_RING),
                    reminderFullScreen = o.optBoolean("reminderFullScreen", true),
                    reminderSpeakAloud = o.optBoolean("reminderSpeakAloud", false)
                )
            )
        }

        txDao.deleteAll()
        if (importedTx.isNotEmpty()) {
            txDao.insertAll(importedTx)
        }

        val catArr = root.optJSONArray("categories")
        if (catArr != null && catArr.length() > 0) {
            catDao.deleteAll()
            val cats = mutableListOf<CategoryEntity>()
            for (i in 0 until catArr.length()) {
                val o = catArr.getJSONObject(i)
                cats.add(
                    CategoryEntity(
                        name = o.getString("name"),
                        iconKey = o.optString("iconKey", "category"),
                        kind = o.optString("kind", TransactionType.EXPENSE)
                    )
                )
            }
            catDao.insertAll(cats)
        }

        val accArr = root.optJSONArray("accounts")
        if (accArr != null && accArr.length() > 0) {
            accDao.deleteAll()
            val accs = mutableListOf<AccountEntity>()
            for (i in 0 until accArr.length()) {
                val o = accArr.getJSONObject(i)
                accs.add(
                    AccountEntity(
                        name = o.getString("name"),
                        iconKey = o.optString("iconKey", "wallet"),
                        currency = o.optString("currency", "EGP")
                    )
                )
            }
            accDao.insertAll(accs)
        }

        val filterArr = root.optJSONArray("savedFilters")
        if (filterArr != null) {
            filterDao.deleteAll()
            val filters = mutableListOf<SavedFilterEntity>()
            for (i in 0 until filterArr.length()) {
                val o = filterArr.getJSONObject(i)
                filters.add(
                    SavedFilterEntity(
                        name = o.getString("name"),
                        filterConfigJson = o.getString("filterConfigJson"),
                        isDefault = o.optBoolean("isDefault", false),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            if (filters.isNotEmpty()) filterDao.insertAll(filters)
        }

        val obArr = root.optJSONArray("obligations")
        if (obArr != null) {
            obligationDao.deleteAll()
            val obs = mutableListOf<ObligationEntity>()
            for (i in 0 until obArr.length()) {
                val o = obArr.getJSONObject(i)
                obs.add(
                    ObligationEntity(
                        amountMinor = o.optLong("amountMinor", 0L).coerceAtLeast(0L),
                        currency = o.optString("currency", "EGP"),
                        reason = o.optString("reason", ""),
                        party = o.optString("party", ""),
                        dueDate = o.optString("dueDate", "2026-01-01"),
                        dueTime = o.optString("dueTime", "09:00"),
                        reminderEnabled = o.optBoolean("reminderEnabled", true),
                        reminderLeadMinutes = o.optInt("reminderLeadMinutes", 60),
                        reminderMuted = o.optBoolean("reminderMuted", false),
                        isPaid = o.optBoolean("isPaid", false),
                        paidDate = o.optString("paidDate", ""),
                        paidSource = o.optString("paidSource", "كاش / نقدي"),
                        note = o.optString("note", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        reminderAlertMode = o.optString("reminderAlertMode", ReminderAlertMode.NOTIFICATION_RING),
                        reminderFullScreen = o.optBoolean("reminderFullScreen", true),
                        reminderSpeakAloud = o.optBoolean("reminderSpeakAloud", false)
                    )
                )
            }
            if (obs.isNotEmpty()) obligationDao.insertAll(obs)
        }

        val sObj = root.optJSONObject("settings")
        if (sObj != null) {
            val current = settingsDao.getSettingsOnce() ?: AppSettingsEntity()
            settingsDao.upsert(
                current.copy(
                    defaultCurrency = sObj.optString("defaultCurrency", current.defaultCurrency),
                    language = sObj.optString("language", current.language),
                    themeMode = sObj.optString("themeMode", current.themeMode),
                    quickDeleteMode = sObj.optBoolean("quickDeleteMode", current.quickDeleteMode),
                    dailyReminderEnabled = sObj.optBoolean("dailyReminderEnabled", current.dailyReminderEnabled),
                    dailyReminderTime = sObj.optString("dailyReminderTime", current.dailyReminderTime),
                    monthlyBudgetLimitMinor = sObj.optLong("monthlyBudgetLimitMinor", current.monthlyBudgetLimitMinor),
                    lastSelectedSource = sObj.optString("lastSelectedSource", current.lastSelectedSource),
                    notificationsEnabled = sObj.optBoolean("notificationsEnabled", current.notificationsEnabled),
                    customGreetingText = sObj.optString("customGreetingText", current.customGreetingText),
                    customMorningGreeting = sObj.optString("customMorningGreeting", current.customMorningGreeting),
                    customEveningGreeting = sObj.optString("customEveningGreeting", current.customEveningGreeting),
                    showBaseGreeting = sObj.optBoolean("showBaseGreeting", current.showBaseGreeting),
                    showCustomGreeting = sObj.optBoolean("showCustomGreeting", current.showCustomGreeting),
                    transactionsViewMode = sObj.optString("transactionsViewMode", current.transactionsViewMode),
                    defaultAlertMode = sObj.optString("defaultAlertMode", current.defaultAlertMode),
                    fullScreenAlertsEnabled = sObj.optBoolean("fullScreenAlertsEnabled", current.fullScreenAlertsEnabled),
                    speakRemindersAloud = sObj.optBoolean("speakRemindersAloud", current.speakRemindersAloud),
                    ringtoneAlertsEnabled = sObj.optBoolean("ringtoneAlertsEnabled", current.ringtoneAlertsEnabled)
                )
            )
        }

        ensureSeedData()
        return importedTx.size
    }

    data class LocalBackupSnapshot(
        val transactions: List<TransactionEntity>,
        val categories: List<CategoryEntity>,
        val accounts: List<AccountEntity>,
        val settings: AppSettingsEntity
    )

    suspend fun getFullLocalSnapshot(): LocalBackupSnapshot {
        return LocalBackupSnapshot(
            transactions = txDao.getAllOnce(),
            categories = catDao.getAllOnce(),
            accounts = accDao.getAllOnce(),
            settings = settingsDao.getSettingsOnce() ?: AppSettingsEntity()
        )
    }

    suspend fun restoreFromCloudEntities(
        importedTx: List<TransactionEntity>,
        importedCats: List<CategoryEntity>,
        importedAccs: List<AccountEntity>,
        defaultCurrency: String?,
        language: String?,
        themeMode: String?,
        quickDeleteMode: Boolean?,
        dailyReminderEnabled: Boolean?,
        dailyReminderTime: String?,
        monthlyBudgetLimitMinor: Long?,
        lastSelectedSource: String?,
        userName: String?
    ): Int {
        txDao.deleteAll()
        if (importedTx.isNotEmpty()) {
            txDao.insertAll(importedTx)
        }
        if (importedCats.isNotEmpty()) {
            catDao.deleteAll()
            catDao.insertAll(importedCats)
        }
        if (importedAccs.isNotEmpty()) {
            accDao.deleteAll()
            accDao.insertAll(importedAccs)
        }
        val current = settingsDao.getSettingsOnce() ?: AppSettingsEntity()
        settingsDao.upsert(
            current.copy(
                defaultCurrency = defaultCurrency?.takeIf { it.isNotBlank() } ?: current.defaultCurrency,
                language = language?.takeIf { it == "ar" || it == "en" } ?: current.language,
                themeMode = themeMode?.takeIf { it in listOf("light", "dark", "system") } ?: current.themeMode,
                quickDeleteMode = quickDeleteMode ?: current.quickDeleteMode,
                dailyReminderEnabled = dailyReminderEnabled ?: current.dailyReminderEnabled,
                dailyReminderTime = dailyReminderTime?.takeIf { it.isNotBlank() } ?: current.dailyReminderTime,
                monthlyBudgetLimitMinor = monthlyBudgetLimitMinor?.coerceAtLeast(0L) ?: current.monthlyBudgetLimitMinor,
                lastSelectedSource = lastSelectedSource?.takeIf { it.isNotBlank() } ?: current.lastSelectedSource,
                userName = userName?.takeIf { it.isNotBlank() } ?: current.userName
            )
        )
        ensureSeedData()
        return importedTx.size
    }

    companion object {
        fun defaultCategories(): List<CategoryEntity> = listOf(
            // Expense categories matching screenshots
            CategoryEntity(name = "طعام ومشروبات", iconKey = "restaurant", kind = TransactionType.EXPENSE),
            CategoryEntity(name = "تسوق ومشتريات", iconKey = "shopping_bag", kind = TransactionType.EXPENSE),
            CategoryEntity(name = "مواصلات ونقل", iconKey = "directions_car", kind = TransactionType.EXPENSE),
            CategoryEntity(name = "فواتير والتزامات", iconKey = "receipt", kind = TransactionType.EXPENSE),
            CategoryEntity(name = "صحة وأدوية", iconKey = "medication", kind = TransactionType.EXPENSE),
            CategoryEntity(name = "سكن وإيجار", iconKey = "home", kind = TransactionType.EXPENSE),
            CategoryEntity(name = "ترفيه وسفر", iconKey = "movie", kind = TransactionType.EXPENSE),
            CategoryEntity(name = "تعليم وتطوير", iconKey = "school", kind = TransactionType.EXPENSE),
            // Income categories matching screenshots
            CategoryEntity(name = "راتب شهري", iconKey = "payments", kind = TransactionType.INCOME),
            CategoryEntity(name = "أعمال حرة (فريلانس)", iconKey = "laptop_mac", kind = TransactionType.INCOME),
            CategoryEntity(name = "استثمارات وأرباح", iconKey = "trending_up", kind = TransactionType.INCOME),
            CategoryEntity(name = "مكافآت وحوافز", iconKey = "card_giftcard", kind = TransactionType.INCOME),
            CategoryEntity(name = "بيع أغراض", iconKey = "storefront", kind = TransactionType.INCOME),
            CategoryEntity(name = "أخرى", iconKey = "attach_money", kind = TransactionType.INCOME)
        )

        fun defaultAccounts(): List<AccountEntity> = listOf(
            AccountEntity(name = "كاش / نقدي", iconKey = "wallet", currency = "EGP"),
            AccountEntity(name = "البنك الأهلي المصري", iconKey = "account_balance", currency = "EGP"),
            AccountEntity(name = "بنك مصر", iconKey = "account_balance", currency = "EGP"),
            AccountEntity(name = "فودافون كاش", iconKey = "phone_android", currency = "EGP"),
            AccountEntity(name = "إنستاباي (InstaPay)", iconKey = "flash_on", currency = "EGP"),
            AccountEntity(name = "محفظة إلكترونية", iconKey = "contactless", currency = "EGP"),
            AccountEntity(name = "البنك التجاري الدولي (CIB)", iconKey = "credit_card", currency = "EGP")
        )
    }
}
