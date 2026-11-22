package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, time DESC, createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, time DESC, createdAt DESC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Long): TransactionEntity?

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDebtSettled = :settled WHERE id = :id")
    suspend fun setDebtSettled(id: Long, settled: Boolean)

    @Query("UPDATE transactions SET reminderMuted = :muted WHERE id = :id")
    suspend fun setReminderMuted(id: Long, muted: Boolean)

    @Query(
        """UPDATE transactions
           SET hasPaymentReminder = :enabled,
               paymentDueDate = :dueDate,
               paymentDueTime = :dueTime,
               reminderLeadMinutes = :leadMinutes,
               reminderMuted = :muted,
               reminderAlertMode = :alertMode,
               reminderFullScreen = :fullScreen,
               reminderSpeakAloud = :speakAloud
           WHERE id = :id"""
    )
    suspend fun updateReminderSettings(
        id: Long,
        enabled: Boolean,
        dueDate: String,
        dueTime: String,
        leadMinutes: Int,
        muted: Boolean,
        alertMode: String,
        fullScreen: Boolean,
        speakAloud: Boolean
    )

    @Query("UPDATE transactions SET paymentDueDate = '', paymentDueTime = '' WHERE hasPaymentReminder = 0 AND paymentDueDate != ''")
    suspend fun clearUnscheduledDueDates()

    @Query("UPDATE transactions SET hasPaymentReminder = 0, paymentDueDate = '', paymentDueTime = '', reminderMuted = 0 WHERE isDebt = 1")
    suspend fun clearAllDebtTransactionReminders()

    @Query("UPDATE transactions SET paymentDueTime = :newTime24, reminderLeadMinutes = 0 WHERE isDebt = 1 AND isDebtSettled = 0 AND hasPaymentReminder = 1 AND paymentDueDate != ''")
    suspend fun unifyScheduledTransactionDueTime(newTime24: String)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM transactions WHERE date >= :startDate AND date <= :endDate")
    suspend fun deleteInDateRange(startDate: String, endDate: String)

    @Query("DELETE FROM transactions WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)

    @Query("DELETE FROM transactions WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query("DELETE FROM transactions WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Query("DELETE FROM transactions WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Query("DELETE FROM transactions WHERE type = 'income' AND isDebt = 0")
    suspend fun deleteEarnedIncomeOnly()

    @Query("DELETE FROM transactions WHERE type = 'income' AND isDebt = 1 AND isDebtSettled = 0")
    suspend fun deleteUnpaidIncomeDebts()

    @Query("DELETE FROM transactions WHERE type = 'income' AND isDebt = 1 AND isDebtSettled = 1")
    suspend fun deleteSettledIncomeDebts()

    @Query("DELETE FROM transactions WHERE type = 'expense' AND (isDebt = 0 OR isDebtSettled = 1)")
    suspend fun deletePaidExpensesOnly()

    @Query("DELETE FROM transactions WHERE type = 'expense' AND isDebt = 1 AND isDebtSettled = 0")
    suspend fun deleteUnpaidExpenseDebts()

    @Query("DELETE FROM transactions WHERE type = 'expense' AND isDebt = 1 AND isDebtSettled = 1")
    suspend fun deleteSettledExpenseDebts()

    @Query("DELETE FROM transactions WHERE isDebt = 1 AND isDebtSettled = 0")
    suspend fun deleteUnpaidDebts()

    @Query("DELETE FROM transactions WHERE isDebt = 1 AND isDebtSettled = 1")
    suspend fun deleteSettledDebts()

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAllOnce(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAllOnce(): List<AccountEntity>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface SavedFilterDao {
    @Query("SELECT * FROM saved_filters ORDER BY isDefault DESC, createdAt DESC")
    fun observeAll(): Flow<List<SavedFilterEntity>>

    @Query("SELECT * FROM saved_filters ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<SavedFilterEntity>

    @Query("SELECT * FROM saved_filters WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultFilter(): SavedFilterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(filter: SavedFilterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(filters: List<SavedFilterEntity>)

    @Query("UPDATE saved_filters SET isDefault = 0")
    suspend fun clearDefaultFlag()

    @Query("DELETE FROM saved_filters WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_filters")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)
}

@Dao
interface ObligationDao {
    @Query("SELECT * FROM obligations ORDER BY isPaid ASC, dueDate ASC, dueTime ASC, createdAt DESC")
    fun observeAll(): Flow<List<ObligationEntity>>

    @Query("SELECT * FROM obligations ORDER BY isPaid ASC, dueDate ASC, dueTime ASC, createdAt DESC")
    suspend fun getAllOnce(): List<ObligationEntity>

    @Query("SELECT * FROM obligations WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Long): ObligationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(obligation: ObligationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(obligations: List<ObligationEntity>)

    @Update
    suspend fun update(obligation: ObligationEntity)

    @Query("UPDATE obligations SET isPaid = :paid, paidDate = :paidDate, paidSource = :paidSource WHERE id = :id")
    suspend fun setPaidStatus(id: Long, paid: Boolean, paidDate: String, paidSource: String)

    @Query("UPDATE obligations SET reminderMuted = :muted WHERE id = :id")
    suspend fun setReminderMuted(id: Long, muted: Boolean)

    @Query(
        """UPDATE obligations
           SET reminderEnabled = :enabled,
               dueDate = :dueDate,
               dueTime = :dueTime,
               reminderLeadMinutes = :leadMinutes,
               reminderMuted = :muted,
               reminderAlertMode = :alertMode,
               reminderFullScreen = :fullScreen,
               reminderSpeakAloud = :speakAloud
           WHERE id = :id"""
    )
    suspend fun updateReminderSettings(
        id: Long,
        enabled: Boolean,
        dueDate: String,
        dueTime: String,
        leadMinutes: Int,
        muted: Boolean,
        alertMode: String,
        fullScreen: Boolean,
        speakAloud: Boolean
    )

    @Query("UPDATE obligations SET dueTime = :newTime24, reminderLeadMinutes = 0 WHERE isPaid = 0 AND reminderEnabled = 1")
    suspend fun unifyPendingObligationDueTime(newTime24: String)

    @Query("DELETE FROM obligations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM obligations WHERE isPaid = 0")
    suspend fun deletePendingOnly()

    @Query("DELETE FROM obligations WHERE isPaid = 1")
    suspend fun deletePaidOnly()

    @Query("DELETE FROM obligations")
    suspend fun deleteAll()
}
