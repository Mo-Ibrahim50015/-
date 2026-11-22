package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object TransactionType {
    const val EXPENSE = "expense"
    const val INCOME = "income"
}

object ReminderAlertMode {
    const val NOTIFICATION = "notification"
    const val RING = "ring"
    const val NOTIFICATION_RING = "notification_ring"
    const val NOTIFICATION_VOICE = "notification_voice"
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["currency"]),
        Index(value = ["source"]),
        Index(value = ["category"]),
        Index(value = ["amountMinor"]),
        Index(value = ["createdAt"]),
        Index(value = ["isDebt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "expense" or "income"
    val amountMinor: Long, // Stored in piasters/cents (minor units) to avoid float errors
    val currency: String = "EGP",
    val source: String = "كاش / نقدي",
    val category: String = "غير محدد",
    val date: String, // ISO YYYY-MM-DD (local timezone)
    val time: String, // HH:mm (24h local timezone)
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDebt: Boolean = false, // true = مديونية عليّ (expense on credit OR borrowed income/loan)
    val isDebtSettled: Boolean = false, // true = تم سداد المديونية
    val itemName: String = "", // اسم الحاجة / الغرض
    val debtParty: String = "", // الجهة أو الشخص الدائن (اختياري)
    val hasPaymentReminder: Boolean = false, // true = مربوطة بموعد دفع وإشعار
    val paymentDueDate: String = "", // ISO YYYY-MM-DD
    val paymentDueTime: String = "", // HH:mm
    val reminderLeadMinutes: Int = 0, // التنبيه في نفس الموعد افتراضياً (أو قبل الموعد بدقائق)
    val reminderMuted: Boolean = false, // true = تم فصل التنبيه عن هذه العملية فقط
    val reminderAlertMode: String = ReminderAlertMode.NOTIFICATION_RING,
    val reminderFullScreen: Boolean = true,
    val reminderSpeakAloud: Boolean = false
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["kind"])]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val kind: String // "expense" or "income"
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val currency: String = "EGP"
)

@Entity(tableName = "saved_filters")
data class SavedFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val filterConfigJson: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val defaultCurrency: String = "EGP",
    val language: String = "ar", // "ar" or "en"
    val themeMode: String = "light", // "light", "dark", "system"
    val quickDeleteMode: Boolean = true,
    val pinEnabled: Boolean = false,
    val pinCode: String = "",
    val autoLockSensitive: Boolean = false,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderTime: String = "21:00",
    val monthlyBudgetLimitMinor: Long = 0L,
    val lastSelectedSource: String = "كاش / نقدي",
    val userName: String = "",
    val userAvatarPath: String = "",
    val biometricEnabled: Boolean = false,
    val patternEnabled: Boolean = false,
    val patternCode: String = "",
    val lockOnLaunch: Boolean = false,
    val confirmPaymentWithSecurity: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val notificationsReadAtMillis: Long = 0L,
    val patternMode: String = "custom", // "device" or "custom"
    val customGreetingText: String = "",
    val customMorningGreeting: String = "",
    val customEveningGreeting: String = "",
    val showBaseGreeting: Boolean = true,
    val showCustomGreeting: Boolean = true,
    val transactionsViewMode: String = "cards", // "cards" or "table"
    val defaultAlertMode: String = ReminderAlertMode.NOTIFICATION_RING,
    val fullScreenAlertsEnabled: Boolean = true,
    val speakRemindersAloud: Boolean = false,
    val ringtoneAlertsEnabled: Boolean = true
) {
    fun hasAnyLockMethodConfigured(): Boolean =
        biometricEnabled ||
            (patternEnabled && (patternMode == "device" || patternCode.isNotBlank())) ||
            (pinEnabled && pinCode.length >= 4)

    fun analyticsDashboardConfig(): AnalyticsDashboardConfig =
        AnalyticsDashboardConfig.decode(transactionsViewMode)

    fun cleanTransactionsViewMode(): String =
        transactionsViewMode.substringBefore("|").ifBlank { "cards" }

    fun primaryDailyReminderTime(): String {
        val raw = dailyReminderTime.substringBefore("*").substringBefore("|").trim()
        val parts = raw.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()
        val m = parts.getOrNull(1)?.toIntOrNull()
        return if (h != null && m != null && h in 0..23 && m in 0..59) {
            "%02d:%02d".format(java.util.Locale.US, h, m)
        } else {
            "21:00"
        }
    }

    fun dailyReminderCount(): Int {
        val rawCount = if ("*" in dailyReminderTime) {
            dailyReminderTime.substringAfter("*").substringBefore("|").trim().toIntOrNull()
        } else if ("|" in dailyReminderTime) {
            dailyReminderTime.substringAfter("|").trim().toIntOrNull()
        } else {
            1
        }
        return (rawCount ?: 1).coerceIn(1, 6)
    }

    fun dailyReminderTimesList(): List<String> =
        computeDailyReminderSlots(primaryDailyReminderTime(), dailyReminderCount())

    companion object {
        fun encodeDailyReminderTimeAndCount(baseTime24: String, count: Int): String {
            val parts = baseTime24.substringBefore("*").substringBefore("|").trim().split(":")
            val h = (parts.getOrNull(0)?.toIntOrNull() ?: 21).coerceIn(0, 23)
            val m = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)
            val cleanTime = "%02d:%02d".format(java.util.Locale.US, h, m)
            val cleanCount = count.coerceIn(1, 6)
            return if (cleanCount <= 1) cleanTime else "$cleanTime*$cleanCount"
        }

        fun computeDailyReminderSlots(baseTime24: String, count: Int): List<String> {
            val safeCount = count.coerceIn(1, 6)
            val parts = baseTime24.split(":")
            val baseHour = (parts.getOrNull(0)?.toIntOrNull() ?: 21).coerceIn(0, 23)
            val baseMin = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)
            val baseTotal = baseHour * 60 + baseMin
            if (safeCount == 1) {
                return listOf("%02d:%02d".format(java.util.Locale.US, baseHour, baseMin))
            }
            val stepMinutes = when (safeCount) {
                2 -> 360 // 6 hours apart
                3 -> 240 // 4 hours apart
                4 -> 180 // 3 hours apart
                5 -> 150 // 2.5 hours apart
                else -> 120 // 2 hours apart
            }
            return (0 until safeCount).map { idx ->
                val rawMin = (baseTotal + idx * stepMinutes) % 1440
                val hh = (rawMin / 60).coerceIn(0, 23)
                val mm = (rawMin % 60).coerceIn(0, 59)
                "%02d:%02d".format(java.util.Locale.US, hh, mm)
            }.distinct()
        }
    }
}

data class AnalyticsDashboardConfig(
    val contextualFilterCardsOnly: Boolean = true,
    val showQuickDetailsBoxes: Boolean = true,
    val showKpiGrid: Boolean = true,
    val showDebtsAndPaymentsCard: Boolean = true,
    val showSmartInsightsCard: Boolean = true,
    val showTrendChart: Boolean = true,
    val showCategoryChart: Boolean = true,
    val showAccountChart: Boolean = true,
    val showWeeklyCharts: Boolean = true
) {
    fun encodeWithViewMode(baseViewMode: String): String {
        val cleanBase = baseViewMode.substringBefore("|").ifBlank { "cards" }
        val flags = listOf(
            if (contextualFilterCardsOnly) '1' else '0',
            if (showQuickDetailsBoxes) '1' else '0',
            if (showKpiGrid) '1' else '0',
            if (showDebtsAndPaymentsCard) '1' else '0',
            if (showSmartInsightsCard) '1' else '0',
            if (showTrendChart) '1' else '0',
            if (showCategoryChart) '1' else '0',
            if (showAccountChart) '1' else '0',
            if (showWeeklyCharts) '1' else '0'
        ).joinToString("")
        return "$cleanBase|$flags"
    }

    companion object {
        fun decode(raw: String): AnalyticsDashboardConfig {
            val parts = raw.split("|")
            if (parts.size < 2 || parts[1].length < 9) return AnalyticsDashboardConfig()
            val f = parts[1]
            return AnalyticsDashboardConfig(
                contextualFilterCardsOnly = f[0] != '0',
                showQuickDetailsBoxes = f[1] != '0',
                showKpiGrid = f[2] != '0',
                showDebtsAndPaymentsCard = f[3] != '0',
                showSmartInsightsCard = f[4] != '0',
                showTrendChart = f[5] != '0',
                showCategoryChart = f[6] != '0',
                showAccountChart = f[7] != '0',
                showWeeklyCharts = f[8] != '0'
            )
        }
    }
}

@Entity(
    tableName = "obligations",
    indices = [
        Index(value = ["currency"]),
        Index(value = ["isPaid"]),
        Index(value = ["dueDate"]),
        Index(value = ["createdAt"])
    ]
)
data class ObligationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long, // Exact minor units (piasters/cents)
    val currency: String = "EGP",
    val reason: String, // سبب الدفع
    val party: String = "", // الجهة المطلوب الدفع لها
    val dueDate: String, // ISO YYYY-MM-DD
    val dueTime: String = "09:00", // HH:mm
    val reminderEnabled: Boolean = true, // تنبيه بالإشعارات
    val reminderLeadMinutes: Int = 0, // التنبيه في نفس الموعد افتراضياً
    val reminderMuted: Boolean = false, // فصل التنبيه لهذا الالتزام
    val isPaid: Boolean = false, // true = تم الدفع الفعلي
    val paidDate: String = "", // تاريخ الدفع الفعلي ISO YYYY-MM-DD
    val paidSource: String = "كاش / نقدي", // الحساب المدفوع منه
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val reminderAlertMode: String = ReminderAlertMode.NOTIFICATION_RING,
    val reminderFullScreen: Boolean = true,
    val reminderSpeakAloud: Boolean = false
)
