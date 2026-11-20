package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object TransactionType {
    const val EXPENSE = "expense"
    const val INCOME = "income"
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
    val debtParty: String = "" // الجهة أو الشخص الدائن (اختياري)
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
    val confirmPaymentWithSecurity: Boolean = false
) {
    fun hasAnyLockMethodConfigured(): Boolean =
        biometricEnabled || (patternEnabled && patternCode.isNotBlank()) || (pinEnabled && pinCode.length >= 4)
}
