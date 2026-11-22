package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ObligationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {

    private fun openMigratedRoomDb(context: Context, dbName: String): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8
            )
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .allowMainThreadQueries()
            .build()
    }

    private fun createBaseV4Tables(sqlDb: SQLiteDatabase) {
        sqlDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `type` TEXT NOT NULL,
                `amountMinor` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `time` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `isDebt` INTEGER NOT NULL,
                `isDebtSettled` INTEGER NOT NULL,
                `itemName` TEXT NOT NULL,
                `debtParty` TEXT NOT NULL
            )
            """.trimIndent()
        )
        sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
        sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")
        sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_currency` ON `transactions` (`currency`)")
        sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_source` ON `transactions` (`source`)")
        sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category` ON `transactions` (`category`)")
        sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_amountMinor` ON `transactions` (`amountMinor`)")
        sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_createdAt` ON `transactions` (`createdAt`)")
        sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_isDebt` ON `transactions` (`isDebt`)")

        sqlDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `iconKey` TEXT NOT NULL,
                `kind` TEXT NOT NULL
            )
            """.trimIndent()
        )
        sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_kind` ON `categories` (`kind`)")

        sqlDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `accounts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `iconKey` TEXT NOT NULL,
                `currency` TEXT NOT NULL
            )
            """.trimIndent()
        )

        sqlDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `saved_filters` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `filterConfigJson` TEXT NOT NULL,
                `isDefault` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        sqlDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `settings` (
                `id` INTEGER NOT NULL,
                `defaultCurrency` TEXT NOT NULL,
                `language` TEXT NOT NULL,
                `themeMode` TEXT NOT NULL,
                `quickDeleteMode` INTEGER NOT NULL,
                `pinEnabled` INTEGER NOT NULL,
                `pinCode` TEXT NOT NULL,
                `autoLockSensitive` INTEGER NOT NULL,
                `dailyReminderEnabled` INTEGER NOT NULL,
                `dailyReminderTime` TEXT NOT NULL,
                `monthlyBudgetLimitMinor` INTEGER NOT NULL,
                `lastSelectedSource` TEXT NOT NULL,
                `userName` TEXT NOT NULL,
                `userAvatarPath` TEXT NOT NULL,
                `biometricEnabled` INTEGER NOT NULL,
                `patternEnabled` INTEGER NOT NULL,
                `patternCode` TEXT NOT NULL,
                `lockOnLaunch` INTEGER NOT NULL,
                `confirmPaymentWithSecurity` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }

    @Test
    fun `legacy v5 database without obligations migrates cleanly to v6 preserving data`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_legacy_v5_test.db"
        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { sqlDb ->
            createBaseV4Tables(sqlDb)
            // Add early v5 columns (prior to obligations & custom greeting columns)
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `hasPaymentReminder` INTEGER NOT NULL DEFAULT 0")
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `paymentDueDate` TEXT NOT NULL DEFAULT ''")
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `paymentDueTime` TEXT NOT NULL DEFAULT ''")
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `reminderLeadMinutes` INTEGER NOT NULL DEFAULT 60")
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `reminderMuted` INTEGER NOT NULL DEFAULT 0")

            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `notificationsEnabled` INTEGER NOT NULL DEFAULT 1")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `notificationsReadAtMillis` INTEGER NOT NULL DEFAULT 0")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `patternMode` TEXT NOT NULL DEFAULT 'custom'")

            sqlDb.execSQL(
                """
                INSERT INTO `transactions`
                (type, amountMinor, currency, source, category, date, time, note, createdAt, isDebt, isDebtSettled, itemName, debtParty)
                VALUES ('income', 500000, 'EGP', 'كاش / نقدي', 'راتب', '2026-11-20', '10:00', 'راتب شهري', 1700000000000, 0, 0, '', '')
                """.trimIndent()
            )
            sqlDb.execSQL(
                """
                INSERT INTO `settings`
                (id, defaultCurrency, language, themeMode, quickDeleteMode, pinEnabled, pinCode, autoLockSensitive, dailyReminderEnabled, dailyReminderTime, monthlyBudgetLimitMinor, lastSelectedSource, userName, userAvatarPath, biometricEnabled, patternEnabled, patternCode, lockOnLaunch, confirmPaymentWithSecurity)
                VALUES (1, 'EGP', 'ar', 'light', 1, 0, '', 0, 1, '21:00', 0, 'كاش / نقدي', 'عمر', '', 0, 0, '', 0, 0)
                """.trimIndent()
            )
            sqlDb.version = 5
        }

        val roomDb = openMigratedRoomDb(context, dbName)
        try {
            val txs = roomDb.transactionDao().observeAll().first()
            assertEquals(1, txs.size)
            assertEquals(500000L, txs.first().amountMinor)

            val settings = roomDb.settingsDao().getSettingsOnce()
            assertEquals("عمر", settings?.userName)
            assertEquals("cards", settings?.transactionsViewMode)
            assertEquals(true, settings?.showBaseGreeting)
            assertEquals(true, settings?.showCustomGreeting)

            val insertedId = roomDb.obligationDao().insert(
                ObligationEntity(
                    amountMinor = 150000L,
                    currency = "EGP",
                    reason = "إيجار المحل",
                    party = "المالك",
                    dueDate = "2026-12-01",
                    dueTime = "10:00"
                )
            )
            assertTrue(insertedId > 0L)
            assertEquals(1, roomDb.obligationDao().observeAll().first().size)
        } finally {
            roomDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `v4 database migrates cleanly through v5 to v6 preserving data`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_v4_to_v6_test.db"
        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { sqlDb ->
            createBaseV4Tables(sqlDb)
            sqlDb.execSQL(
                """
                INSERT INTO `transactions`
                (type, amountMinor, currency, source, category, date, time, note, createdAt, isDebt, isDebtSettled, itemName, debtParty)
                VALUES ('expense', 12000, 'EGP', 'كاش / نقدي', 'طعام', '2026-11-18', '14:15', 'غداء', 1700000000000, 0, 0, '', '')
                """.trimIndent()
            )
            sqlDb.version = 4
        }

        val roomDb = openMigratedRoomDb(context, dbName)
        try {
            val txs = roomDb.transactionDao().observeAll().first()
            assertEquals(1, txs.size)
            assertEquals(12000L, txs.first().amountMinor)
            assertEquals(0, roomDb.obligationDao().observeAll().first().size)
        } finally {
            roomDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `v5 database with existing obligations migrates idempotently to v6`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_v5_with_obligations_test.db"
        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { sqlDb ->
            createBaseV4Tables(sqlDb)
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `hasPaymentReminder` INTEGER NOT NULL DEFAULT 0")
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `paymentDueDate` TEXT NOT NULL DEFAULT ''")
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `paymentDueTime` TEXT NOT NULL DEFAULT ''")
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `reminderLeadMinutes` INTEGER NOT NULL DEFAULT 60")
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `reminderMuted` INTEGER NOT NULL DEFAULT 0")

            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `notificationsEnabled` INTEGER NOT NULL DEFAULT 1")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `notificationsReadAtMillis` INTEGER NOT NULL DEFAULT 0")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `patternMode` TEXT NOT NULL DEFAULT 'custom'")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `customGreetingText` TEXT NOT NULL DEFAULT ''")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `customMorningGreeting` TEXT NOT NULL DEFAULT ''")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `customEveningGreeting` TEXT NOT NULL DEFAULT ''")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `transactionsViewMode` TEXT NOT NULL DEFAULT 'cards'")

            sqlDb.execSQL(
                "CREATE TABLE IF NOT EXISTS `obligations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amountMinor` INTEGER NOT NULL, `currency` TEXT NOT NULL, `reason` TEXT NOT NULL, `party` TEXT NOT NULL, `dueDate` TEXT NOT NULL, `dueTime` TEXT NOT NULL, `reminderEnabled` INTEGER NOT NULL, `reminderLeadMinutes` INTEGER NOT NULL, `reminderMuted` INTEGER NOT NULL, `isPaid` INTEGER NOT NULL, `paidDate` TEXT NOT NULL, `paidSource` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
            )
            sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_obligations_currency` ON `obligations` (`currency`)")
            sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_obligations_isPaid` ON `obligations` (`isPaid`)")
            sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_obligations_dueDate` ON `obligations` (`dueDate`)")
            sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_obligations_createdAt` ON `obligations` (`createdAt`)")

            sqlDb.execSQL(
                """
                INSERT INTO `obligations`
                (amountMinor, currency, reason, party, dueDate, dueTime, reminderEnabled, reminderLeadMinutes, reminderMuted, isPaid, paidDate, paidSource, note, createdAt)
                VALUES (75000, 'EGP', 'فاتورة كهرباء', 'شركة الكهرباء', '2026-12-05', '12:00', 1, 60, 0, 0, '', 'كاش / نقدي', '', 1700000000000)
                """.trimIndent()
            )
            sqlDb.version = 5
        }

        val roomDb = openMigratedRoomDb(context, dbName)
        try {
            val obligations = roomDb.obligationDao().observeAll().first()
            assertEquals(1, obligations.size)
            assertEquals("فاتورة كهرباء", obligations.first().reason)
            assertEquals(75000L, obligations.first().amountMinor)
        } finally {
            roomDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `v7 database migrates cleanly to v8 preserving data and adding alarm columns`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_v7_to_v8_test.db"
        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { sqlDb ->
            createBaseV4Tables(sqlDb)
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `hasPaymentReminder` INTEGER NOT NULL DEFAULT 0")
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `paymentDueDate` TEXT NOT NULL DEFAULT ''")
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `paymentDueTime` TEXT NOT NULL DEFAULT ''")
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `reminderLeadMinutes` INTEGER NOT NULL DEFAULT 60")
            sqlDb.execSQL("ALTER TABLE `transactions` ADD COLUMN `reminderMuted` INTEGER NOT NULL DEFAULT 0")

            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `notificationsEnabled` INTEGER NOT NULL DEFAULT 1")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `notificationsReadAtMillis` INTEGER NOT NULL DEFAULT 0")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `patternMode` TEXT NOT NULL DEFAULT 'custom'")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `customGreetingText` TEXT NOT NULL DEFAULT ''")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `customMorningGreeting` TEXT NOT NULL DEFAULT ''")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `customEveningGreeting` TEXT NOT NULL DEFAULT ''")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `transactionsViewMode` TEXT NOT NULL DEFAULT 'cards'")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `showBaseGreeting` INTEGER NOT NULL DEFAULT 1")
            sqlDb.execSQL("ALTER TABLE `settings` ADD COLUMN `showCustomGreeting` INTEGER NOT NULL DEFAULT 1")

            sqlDb.execSQL(
                "CREATE TABLE IF NOT EXISTS `obligations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amountMinor` INTEGER NOT NULL, `currency` TEXT NOT NULL, `reason` TEXT NOT NULL, `party` TEXT NOT NULL, `dueDate` TEXT NOT NULL, `dueTime` TEXT NOT NULL, `reminderEnabled` INTEGER NOT NULL, `reminderLeadMinutes` INTEGER NOT NULL, `reminderMuted` INTEGER NOT NULL, `isPaid` INTEGER NOT NULL, `paidDate` TEXT NOT NULL, `paidSource` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
            )
            sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_obligations_currency` ON `obligations` (`currency`)")
            sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_obligations_isPaid` ON `obligations` (`isPaid`)")
            sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_obligations_dueDate` ON `obligations` (`dueDate`)")
            sqlDb.execSQL("CREATE INDEX IF NOT EXISTS `index_obligations_createdAt` ON `obligations` (`createdAt`)")

            sqlDb.execSQL(
                """
                INSERT INTO `transactions`
                (type, amountMinor, currency, source, category, date, time, note, createdAt, isDebt, isDebtSettled, itemName, debtParty, hasPaymentReminder, paymentDueDate, paymentDueTime, reminderLeadMinutes, reminderMuted)
                VALUES ('expense', 30000, 'EGP', 'كاش / نقدي', 'قسط', '2026-11-20', '10:00', 'قسط شهري', 1700000000000, 1, 0, 'قسط موبايل', 'معرض', 1, '2026-12-01', '14:00', 120, 0)
                """.trimIndent()
            )
            sqlDb.execSQL(
                """
                INSERT INTO `settings`
                (id, defaultCurrency, language, themeMode, quickDeleteMode, pinEnabled, pinCode, autoLockSensitive, dailyReminderEnabled, dailyReminderTime, monthlyBudgetLimitMinor, lastSelectedSource, userName, userAvatarPath, biometricEnabled, patternEnabled, patternCode, lockOnLaunch, confirmPaymentWithSecurity, showBaseGreeting, showCustomGreeting)
                VALUES (1, 'EGP', 'ar', 'light', 1, 0, '', 0, 1, '21:00', 0, 'كاش / نقدي', 'سارة', '', 0, 0, '', 0, 0, 1, 0)
                """.trimIndent()
            )
            sqlDb.version = 7
        }

        val roomDb = openMigratedRoomDb(context, dbName)
        try {
            val txs = roomDb.transactionDao().observeAll().first()
            assertEquals(1, txs.size)
            assertEquals("notification_ring", txs.first().reminderAlertMode)
            assertEquals(true, txs.first().reminderFullScreen)
            assertEquals(false, txs.first().reminderSpeakAloud)

            val settings = roomDb.settingsDao().getSettingsOnce()
            assertEquals("سارة", settings?.userName)
            assertEquals(true, settings?.showBaseGreeting)
            assertEquals(false, settings?.showCustomGreeting)
            assertEquals("notification_ring", settings?.defaultAlertMode)
            assertEquals(true, settings?.fullScreenAlertsEnabled)
            assertEquals(true, settings?.ringtoneAlertsEnabled)
        } finally {
            roomDb.close()
            context.deleteDatabase(dbName)
        }
    }
}
