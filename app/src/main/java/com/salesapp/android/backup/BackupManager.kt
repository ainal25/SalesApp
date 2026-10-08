package com.salesapp.android.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.salesapp.android.data.AppDatabase
import com.salesapp.android.data.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class BackupBlob(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val stores: List<Store>,
    val products: List<Product>,
    val transactions: List<TransactionEntity>,
    val targets: List<MonthlyTarget>,
    val profile: ProfileEntity?
)

object BackupManager {
    private val gson = Gson()

    suspend fun export(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.get(context)
            val blob = BackupBlob(
                stores = db.storeDao().observeAll().first(),
                products = db.productDao().observeAll().first(),
                transactions = db.transactionDao().observeAll().first(),
                targets = emptyList(),
                profile = db.profileDao().get()
            )
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(gson.toJson(blob).toByteArray())
            } ?: return@withContext Result.failure(IllegalStateException("Tidak bisa menulis file"))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun import(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext Result.failure(IllegalStateException("Tidak bisa membaca file"))
            val blob = gson.fromJson(text, BackupBlob::class.java)
            val db = AppDatabase.get(context)
            blob.stores.forEach { db.storeDao().upsert(it) }
            blob.products.forEach { db.productDao().upsert(it) }
            blob.transactions.forEach { db.transactionDao().upsert(it) }
            blob.profile?.let { db.profileDao().upsert(it) }
            Result.success(blob.stores.size + blob.products.size + blob.transactions.size)
        } catch (e: Exception) { Result.failure(e) }
    }
}
