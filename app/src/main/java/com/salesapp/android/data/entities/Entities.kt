package com.salesapp.android.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class Store(
    @PrimaryKey val id: String,
    val name: String,
    val address: String = "",
    val isLoyalty: Boolean = false,
    val target: Long = 0L
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // KSNI, Simba, Hello Panda, Amo
    val price: Long,       // Rp per CTN
    val boxPerCtn: Int = 1,
    val pcsPerBox: Int = 1,
    val discountPerCtn: Long = 0L // flat Rp discount per CTN (optional per product)
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val date: Long,             // epoch millis
    val itemsJson: String,      // list of TxItem serialized
    val totalBruto: Long,
    val totalReguler: Long,
    val totalStrata: Long,
    val codDiscount: Long,
    val netTotal: Long
)

data class TxItem(
    val prodId: String,
    val name: String,
    val category: String,
    val price: Long,
    val ctn: Int,
    val box: Int,
    val pcs: Int,
    val totalCtn: Double,
    val discountPerCtn: Long,
    val itemFinalNet: Long
)

@Entity(tableName = "monthly_targets")
data class MonthlyTarget(
    @PrimaryKey val yearMonth: String, // "2026-05"
    val ksni: Long = 0L,
    val simba: Long = 0L,
    val hp: Long = 0L,
    val roaKsni: Long = 0L
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val code: String = "",
    val photoPath: String = ""
)
