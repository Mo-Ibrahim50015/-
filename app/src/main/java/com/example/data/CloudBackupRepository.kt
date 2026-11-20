package com.example.data

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

@Keep
@IgnoreExtraProperties
data class CloudBackupSummary(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ownerEmail: String = "",
    val transactionCount: Int = 0,
    val categoryCount: Int = 0,
    val accountCount: Int = 0,
    val defaultCurrency: String = "EGP",
    val language: String = "ar",
    val themeMode: String = "light",
    val quickDeleteMode: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderTime: String = "21:00",
    val monthlyBudgetLimitMinor: Long = 0L,
    val lastSelectedSource: String = "كاش / نقدي",
    val userName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudBackupTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = TransactionType.EXPENSE,
    val amountMinor: Long = 0L,
    val currency: String = "EGP",
    val source: String = "كاش / نقدي",
    val category: String = "غير محدد",
    val date: String = "2026-01-01",
    val time: String = "12:00",
    val note: String = "",
    val localCreatedAt: Long = 0L,
    val debt: Boolean = false,
    val debtSettled: Boolean = false,
    val itemName: String = "",
    val debtParty: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudBackupCategory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iconKey: String = "category",
    val kind: String = TransactionType.EXPENSE,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudBackupAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iconKey: String = "wallet",
    val currency: String = "EGP",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CloudBackupPayload(
    val summary: CloudBackupSummary,
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val accounts: List<AccountEntity>
)

class CloudBackupRepository(context: Context) {
    private val appContext = context.applicationContext

    // Always bind to the single provisioned named database from firebase_applet_config.xml
    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
    }

    private fun requireUserId(): String {
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
        require(!uid.isNullOrBlank()) { "Authentication required for cloud backup" }
        return uid
    }

    private fun handleFirestoreError(
        error: Throwable,
        operationType: OperationType,
        path: String?
    ) {
        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
        val providerArray = JSONArray().apply {
            user?.providerData?.forEach { info ->
                put(
                    JSONObject().apply {
                        put("providerId", info.providerId)
                        put("displayName", info.displayName ?: "")
                        put("email", info.email ?: "")
                        put("photoUrl", info.photoUrl?.toString() ?: "")
                    }
                )
            }
        }
        val payload = JSONObject().apply {
            put("error", error.message ?: error.toString())
            put("operationType", operationType.value)
            put("path", path ?: "")
            put(
                "authInfo",
                JSONObject().apply {
                    put("userId", user?.uid ?: "")
                    put("email", user?.email ?: "")
                    put("emailVerified", user?.isEmailVerified ?: false)
                    put("isAnonymous", user?.isAnonymous ?: false)
                    put("tenantId", user?.tenantId ?: "")
                    put("providerInfo", providerArray)
                }
            )
        }
        Log.e("FirestoreError", "Firestore Error: $payload", error)
    }

    suspend fun fetchBackupSummary(): Result<CloudBackupSummary?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid/backups/$SUMMARY_DOC_ID"
        return try {
            val snap = db.collection("users")
                .document(uid)
                .collection("backups")
                .document(SUMMARY_DOC_ID)
                .get()
                .await()
            if (!snap.exists()) {
                Result.success(null)
            } else {
                val summary = snap.toObject(
                    CloudBackupSummary::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                Result.success(summary)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun uploadBackup(
        snapshot: MizanityRepository.LocalBackupSnapshot,
        ownerEmail: String
    ): Result<CloudBackupSummary> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val userDoc = db.collection("users").document(uid)
        val batchStamp = System.currentTimeMillis()
        val path = "users/$uid/backups/$SUMMARY_DOC_ID"

        return try {
            // 1. Delete previous subcollection docs in bounded chunks so stale records are never left behind
            deleteCollectionInBatches(userDoc.collection("backup_transactions"))
            deleteCollectionInBatches(userDoc.collection("backup_categories"))
            deleteCollectionInBatches(userDoc.collection("backup_accounts"))

            // 2. Write transactions in safe batches (max 400 ops per commit)
            val boundedTx = snapshot.transactions.take(100000)
            boundedTx.chunked(BATCH_LIMIT).forEachIndexed { chunkIdx, chunk ->
                val batch = db.batch()
                chunk.forEachIndexed { idx, tx ->
                    val docId = "tx_${batchStamp}_${chunkIdx}_${idx}"
                    val docRef = userDoc.collection("backup_transactions").document(docId)
                    val safeType = if (tx.type == TransactionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
                    val data = mapOf(
                        "userId" to uid,
                        "type" to safeType,
                        "amountMinor" to tx.amountMinor.coerceAtLeast(0L),
                        "currency" to tx.currency.trim().ifBlank { "EGP" }.take(12),
                        "source" to tx.source.trim().ifBlank { "كاش / نقدي" }.take(120),
                        "category" to tx.category.trim().ifBlank { "غير محدد" }.take(120),
                        "date" to tx.date.trim().ifBlank { "2026-01-01" }.take(20),
                        "time" to tx.time.trim().ifBlank { "12:00" }.take(20),
                        "note" to tx.note.take(500),
                        "localCreatedAt" to tx.createdAt.coerceAtLeast(0L),
                        "debt" to tx.isDebt,
                        "debtSettled" to tx.isDebtSettled,
                        "itemName" to tx.itemName.take(160),
                        "debtParty" to tx.debtParty.take(160),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, data)
                }
                batch.commit().await()
            }

            // 3. Write categories
            val boundedCats = snapshot.categories.take(5000)
            boundedCats.chunked(BATCH_LIMIT).forEachIndexed { chunkIdx, chunk ->
                val batch = db.batch()
                chunk.forEachIndexed { idx, cat ->
                    val docId = "cat_${batchStamp}_${chunkIdx}_${idx}"
                    val docRef = userDoc.collection("backup_categories").document(docId)
                    val safeKind = if (cat.kind == TransactionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
                    val data = mapOf(
                        "userId" to uid,
                        "name" to cat.name.trim().ifBlank { "غير محدد" }.take(120),
                        "iconKey" to cat.iconKey.trim().ifBlank { "category" }.take(64),
                        "kind" to safeKind,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, data)
                }
                batch.commit().await()
            }

            // 4. Write accounts
            val boundedAccs = snapshot.accounts.take(5000)
            boundedAccs.chunked(BATCH_LIMIT).forEachIndexed { chunkIdx, chunk ->
                val batch = db.batch()
                chunk.forEachIndexed { idx, acc ->
                    val docId = "acc_${batchStamp}_${chunkIdx}_${idx}"
                    val docRef = userDoc.collection("backup_accounts").document(docId)
                    val data = mapOf(
                        "userId" to uid,
                        "name" to acc.name.trim().ifBlank { "كاش / نقدي" }.take(120),
                        "iconKey" to acc.iconKey.trim().ifBlank { "wallet" }.take(64),
                        "currency" to acc.currency.trim().ifBlank { "EGP" }.take(12),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, data)
                }
                batch.commit().await()
            }

            // 5. Create or partial-update summary doc (preserving immutable userId & createdAt on update)
            val s = snapshot.settings
            val safeLang = if (s.language == "en") "en" else "ar"
            val safeTheme = if (s.themeMode in listOf("light", "dark", "system")) s.themeMode else "light"
            val summaryRef = userDoc.collection("backups").document(SUMMARY_DOC_ID)
            val existingSummary = summaryRef.get().await()

            val mutableFields = mapOf(
                "ownerEmail" to ownerEmail.trim().take(254),
                "transactionCount" to boundedTx.size,
                "categoryCount" to boundedCats.size,
                "accountCount" to boundedAccs.size,
                "defaultCurrency" to s.defaultCurrency.trim().ifBlank { "EGP" }.take(12),
                "language" to safeLang,
                "themeMode" to safeTheme,
                "quickDeleteMode" to s.quickDeleteMode,
                "dailyReminderEnabled" to s.dailyReminderEnabled,
                "dailyReminderTime" to s.dailyReminderTime.trim().ifBlank { "21:00" }.take(10),
                "monthlyBudgetLimitMinor" to s.monthlyBudgetLimitMinor.coerceAtLeast(0L),
                "lastSelectedSource" to s.lastSelectedSource.take(120),
                "userName" to s.userName.take(80),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (existingSummary.exists()) {
                summaryRef.update(mutableFields).await()
            } else {
                val createFields = mutableFields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                summaryRef.set(createFields).await()
            }

            val updatedSnap = summaryRef.get().await()
            val summary = updatedSnap.toObject(
                CloudBackupSummary::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: CloudBackupSummary(
                id = SUMMARY_DOC_ID,
                userId = uid,
                ownerEmail = ownerEmail.trim().take(254),
                transactionCount = boundedTx.size,
                categoryCount = boundedCats.size,
                accountCount = boundedAccs.size
            )
            Result.success(summary)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun downloadBackup(): Result<CloudBackupPayload?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val userDoc = db.collection("users").document(uid)
        val path = "users/$uid/backups/$SUMMARY_DOC_ID"

        return try {
            val summarySnap = userDoc.collection("backups").document(SUMMARY_DOC_ID).get().await()
            if (!summarySnap.exists()) {
                return Result.success(null)
            }
            val summary = summarySnap.toObject(
                CloudBackupSummary::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.success(null)

            val txSnaps = userDoc.collection("backup_transactions").get().await()
            val catSnaps = userDoc.collection("backup_categories").get().await()
            val accSnaps = userDoc.collection("backup_accounts").get().await()

            val transactions = txSnaps.documents.mapNotNull { doc ->
                doc.toObject(
                    CloudBackupTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.let { item ->
                    TransactionEntity(
                        type = if (item.type == TransactionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE,
                        amountMinor = item.amountMinor.coerceAtLeast(0L),
                        currency = item.currency.ifBlank { "EGP" },
                        source = item.source.ifBlank { "كاش / نقدي" },
                        category = item.category.ifBlank { "غير محدد" },
                        date = item.date.ifBlank { "2026-01-01" },
                        time = item.time.ifBlank { "12:00" },
                        note = item.note,
                        createdAt = if (item.localCreatedAt > 0L) item.localCreatedAt else System.currentTimeMillis(),
                        isDebt = item.debt,
                        isDebtSettled = item.debtSettled,
                        itemName = item.itemName,
                        debtParty = item.debtParty
                    )
                }
            }

            val categories = catSnaps.documents.mapNotNull { doc ->
                doc.toObject(
                    CloudBackupCategory::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.takeIf { it.name.isNotBlank() }?.let { c ->
                    CategoryEntity(
                        name = c.name,
                        iconKey = c.iconKey.ifBlank { "category" },
                        kind = if (c.kind == TransactionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
                    )
                }
            }

            val accounts = accSnaps.documents.mapNotNull { doc ->
                doc.toObject(
                    CloudBackupAccount::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.takeIf { it.name.isNotBlank() }?.let { a ->
                    AccountEntity(
                        name = a.name,
                        iconKey = a.iconKey.ifBlank { "wallet" },
                        currency = a.currency.ifBlank { "EGP" }
                    )
                }
            }

            Result.success(
                CloudBackupPayload(
                    summary = summary,
                    transactions = transactions,
                    categories = categories,
                    accounts = accounts
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    private suspend fun deleteCollectionInBatches(
        collectionRef: com.google.firebase.firestore.CollectionReference
    ) {
        val existing = collectionRef.get().await()
        if (existing.isEmpty) return
        existing.documents.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        }
    }

    companion object {
        private const val SUMMARY_DOC_ID = "latest"
        private const val BATCH_LIMIT = 400
    }
}
