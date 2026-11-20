package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.MizanityRepository
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.util.ExportManager
import com.example.util.savePickedAvatarToInternalStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Masrofy Arabic branding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("مصروفي", appName)
    }

    @Test
    fun `personal info name and avatar save locally without crashing`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = MizanityRepository(db)
            repo.ensureSeedData()

            // Create a real local bitmap file and pass its Uri to avatar saver
            val sampleImg = File(context.cacheDir, "sample_avatar_test.png")
            val bmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
            FileOutputStream(sampleImg).use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
            bmp.recycle()

            val savedPath = savePickedAvatarToInternalStorage(context, Uri.fromFile(sampleImg), "")
            assertNotNull(savedPath)
            assertTrue(File(savedPath!!).exists())

            repo.updateSettings { it.copy(userName = "أحمد", userAvatarPath = savedPath) }
            val updated = repo.settings.first()
            assertEquals("أحمد", updated.userName)
            assertEquals(savedPath, updated.userAvatarPath)
        } finally {
            db.close()
        }
    }

    @Test
    fun `json backup export save and restore round trip succeeds`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = MizanityRepository(db)
            repo.ensureSeedData()
            repo.addTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amountMinor = 25000L,
                    currency = "EGP",
                    source = "كاش / نقدي",
                    category = "طعام وشراب",
                    date = "2026-11-20",
                    time = "18:30",
                    note = "عشاء",
                    isDebt = true,
                    isDebtSettled = false,
                    itemName = "بقالة",
                    debtParty = "محمود"
                )
            )

            val json = repo.exportBackupJson()
            val backupFile = ExportManager.exportJsonBackupFile(context, json)
            assertTrue(backupFile.exists())
            assertTrue(backupFile.length() > 20L)

            val savedLabel = ExportManager.saveToDeviceDownloads(
                context = context,
                sourceFile = backupFile,
                mimeType = "application/json",
                isImage = false,
                isArabic = true
            )
            assertNotNull(savedLabel)

            // Wipe and restore
            repo.wipeAllTransactions()
            assertEquals(0, repo.transactions.first().size)

            val restoredCount = repo.restoreBackupJson(json)
            assertEquals(1, restoredCount)
            val restoredList = repo.transactions.first()
            assertEquals(1, restoredList.size)
            assertEquals("بقالة", restoredList.first().itemName)
            assertEquals(true, restoredList.first().isDebt)
        } finally {
            db.close()
        }
    }
}
