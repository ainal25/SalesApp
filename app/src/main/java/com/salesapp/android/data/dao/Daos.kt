package com.salesapp.android.data.dao

import androidx.room.*
import com.salesapp.android.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Store>>

    @Query("SELECT * FROM stores WHERE id = :id")
    suspend fun getById(id: String): Store?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(store: Store)

    @Delete suspend fun delete(store: Store)

    @Query("SELECT COUNT(*) FROM stores") suspend fun count(): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY category, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: Product)

    @Delete suspend fun delete(product: Product)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE storeId = :storeId ORDER BY date DESC")
    fun observeByStore(storeId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface TargetDao {
    @Query("SELECT * FROM monthly_targets WHERE yearMonth = :ym")
    suspend fun get(ym: String): MonthlyTarget?

    @Query("SELECT * FROM monthly_targets WHERE yearMonth = :ym")
    fun observe(ym: String): Flow<MonthlyTarget?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(target: MonthlyTarget)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)
}
