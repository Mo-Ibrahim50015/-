package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        SavedFilterEntity::class,
        AppSettingsEntity::class,
        ObligationEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun savedFilterDao(): SavedFilterDao
    abstract fun settingsDao(): SettingsDao
    abstract fun obligationDao(): ObligationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            table: String,
            column: String,
            definition: String
        ) {
            var exists = false
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex).equals(column, ignoreCase = true)) {
                        exists = true
                        break
                    }
                }
            }
            if (!exists) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
            }
        }

        private fun ensureV5ColumnsAndObligationsTable(db: SupportSQLiteDatabase) {
            addColumnIfMissing(db, "transactions", "hasPaymentReminder", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "transactions", "paymentDueDate", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "transactions", "paymentDueTime", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "transactions", "reminderLeadMinutes", "INTEGER NOT NULL DEFAULT 60")
            addColumnIfMissing(db, "transactions", "reminderMuted", "INTEGER NOT NULL DEFAULT 0")

            addColumnIfMissing(db, "settings", "notificationsEnabled", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "settings", "notificationsReadAtMillis", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "settings", "patternMode", "TEXT NOT NULL DEFAULT 'custom'")
            addColumnIfMissing(db, "settings", "customGreetingText", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "settings", "customMorningGreeting", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "settings", "customEveningGreeting", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "settings", "showBaseGreeting", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "settings", "showCustomGreeting", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "settings", "transactionsViewMode", "TEXT NOT NULL DEFAULT 'cards'")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `obligations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amountMinor` INTEGER NOT NULL, `currency` TEXT NOT NULL, `reason` TEXT NOT NULL, `party` TEXT NOT NULL, `dueDate` TEXT NOT NULL, `dueTime` TEXT NOT NULL, `reminderEnabled` INTEGER NOT NULL, `reminderLeadMinutes` INTEGER NOT NULL, `reminderMuted` INTEGER NOT NULL, `isPaid` INTEGER NOT NULL, `paidDate` TEXT NOT NULL, `paidSource` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `reminderAlertMode` TEXT NOT NULL DEFAULT 'notification_ring', `reminderFullScreen` INTEGER NOT NULL DEFAULT 1, `reminderSpeakAloud` INTEGER NOT NULL DEFAULT 0)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_obligations_currency` ON `obligations` (`currency`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_obligations_isPaid` ON `obligations` (`isPaid`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_obligations_dueDate` ON `obligations` (`dueDate`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_obligations_createdAt` ON `obligations` (`createdAt`)")
        }

        private fun ensureV8ReminderColumns(db: SupportSQLiteDatabase) {
            ensureV5ColumnsAndObligationsTable(db)
            addColumnIfMissing(db, "transactions", "reminderAlertMode", "TEXT NOT NULL DEFAULT 'notification_ring'")
            addColumnIfMissing(db, "transactions", "reminderFullScreen", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "transactions", "reminderSpeakAloud", "INTEGER NOT NULL DEFAULT 0")

            addColumnIfMissing(db, "obligations", "reminderAlertMode", "TEXT NOT NULL DEFAULT 'notification_ring'")
            addColumnIfMissing(db, "obligations", "reminderFullScreen", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "obligations", "reminderSpeakAloud", "INTEGER NOT NULL DEFAULT 0")

            addColumnIfMissing(db, "settings", "defaultAlertMode", "TEXT NOT NULL DEFAULT 'notification_ring'")
            addColumnIfMissing(db, "settings", "fullScreenAlertsEnabled", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "settings", "speakRemindersAloud", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "settings", "ringtoneAlertsEnabled", "INTEGER NOT NULL DEFAULT 1")
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "transactions", "isDebt", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "transactions", "isDebtSettled", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "transactions", "itemName", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "transactions", "debtParty", "TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isDebt ON transactions(isDebt)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "settings", "userName", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "settings", "userAvatarPath", "TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "settings", "biometricEnabled", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "settings", "patternEnabled", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "settings", "patternCode", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "settings", "lockOnLaunch", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "settings", "confirmPaymentWithSecurity", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureV5ColumnsAndObligationsTable(db)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureV5ColumnsAndObligationsTable(db)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureV5ColumnsAndObligationsTable(db)
                addColumnIfMissing(db, "settings", "showBaseGreeting", "INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "settings", "showCustomGreeting", "INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureV8ReminderColumns(db)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mizanity_local.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
