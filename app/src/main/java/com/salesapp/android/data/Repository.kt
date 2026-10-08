package com.salesapp.android.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.salesapp.android.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class Repository(context: Context) {
    private val db = AppDatabase.get(context)
    private val gson = Gson()

    val stores: Flow<List<Store>> = db.storeDao().observeAll()
    val products: Flow<List<Product>> = db.productDao().observeAll()
    val transactions: Flow<List<TransactionEntity>> = db.transactionDao().observeAll()
    val profile: Flow<ProfileEntity?> = db.profileDao().observe()

    fun transactionsForStore(id: String) = db.transactionDao().observeByStore(id)
    fun targetFor(ym: String): Flow<MonthlyTarget?> = db.targetDao().observe(ym)

    suspend fun saveStore(s: Store) = db.storeDao().upsert(s)
    suspend fun deleteStore(s: Store) = db.storeDao().delete(s)
    suspend fun getStore(id: String) = db.storeDao().getById(id)

    suspend fun saveProduct(p: Product) = db.productDao().upsert(p)
    suspend fun deleteProduct(p: Product) = db.productDao().delete(p)
    suspend fun getProduct(id: String) = db.productDao().getById(id)

    suspend fun saveTarget(t: MonthlyTarget) = db.targetDao().upsert(t)
    suspend fun getTarget(ym: String) = db.targetDao().get(ym)

    suspend fun saveProfile(p: ProfileEntity) = db.profileDao().upsert(p)
    suspend fun getProfile(): ProfileEntity? = db.profileDao().get()

    suspend fun saveTransaction(tx: TransactionEntity) = db.transactionDao().upsert(tx)
    suspend fun deleteTransaction(id: String) = db.transactionDao().deleteById(id)
    suspend fun getTransaction(id: String) = db.transactionDao().getById(id)

    fun encodeItems(items: List<TxItem>): String = gson.toJson(items)
    fun decodeItems(json: String): List<TxItem> =
        gson.fromJson(json, object: TypeToken<List<TxItem>>() {}.type) ?: emptyList()

    fun newId(): String = UUID.randomUUID().toString()
}
