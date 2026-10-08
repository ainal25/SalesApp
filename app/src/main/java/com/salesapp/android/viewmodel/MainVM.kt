package com.salesapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salesapp.android.data.Repository
import com.salesapp.android.data.entities.*
import com.salesapp.android.util.Fmt
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartLine(
    val product: Product,
    var ctn: Int = 0,
    var box: Int = 0,
    var pcs: Int = 0
) {
    fun totalCtn(): Double {
        val bpc = if (product.boxPerCtn <= 0) 1 else product.boxPerCtn
        val ppb = if (product.pcsPerBox <= 0) 1 else product.pcsPerBox
        return ctn + box.toDouble() / bpc + pcs.toDouble() / (bpc * ppb)
    }
    fun gross(): Double = totalCtn() * product.price
    fun discount(): Double = totalCtn() * product.discountPerCtn
    fun net(): Double = gross() - discount()
    fun isEmpty() = ctn == 0 && box == 0 && pcs == 0
}

class MainVM(private val repo: Repository) : ViewModel() {
    val stores: StateFlow<List<Store>> = repo.stores.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val products: StateFlow<List<Product>> = repo.products.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = repo.transactions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val profile: StateFlow<ProfileEntity?> = repo.profile.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _selectedYm = MutableStateFlow(Fmt.currentYm())
    val selectedYm: StateFlow<String> = _selectedYm
    fun setYm(ym: String) { _selectedYm.value = ym }
    val target: StateFlow<MonthlyTarget?> = _selectedYm
        .flatMapLatest { repo.targetFor(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ----- cart -----
    private val _cart = MutableStateFlow<Map<String, CartLine>>(emptyMap())
    val cart: StateFlow<Map<String, CartLine>> = _cart
    private val _codDiscount = MutableStateFlow(0L)
    val codDiscount: StateFlow<Long> = _codDiscount
    fun setCod(v: Long) { _codDiscount.value = v }

    private val _activeStoreId = MutableStateFlow<String?>(null)
    val activeStoreId: StateFlow<String?> = _activeStoreId
    private val _editTxId = MutableStateFlow<String?>(null)
    val editTxId: StateFlow<String?> = _editTxId

    fun startOrder(storeId: String) {
        _activeStoreId.value = storeId
        _editTxId.value = null
        _cart.value = emptyMap()
        _codDiscount.value = 0
    }

    fun loadForEdit(tx: TransactionEntity) {
        _activeStoreId.value = tx.storeId
        _editTxId.value = tx.id
        _codDiscount.value = tx.codDiscount
        val items = repo.decodeItems(tx.itemsJson)
        viewModelScope.launch {
            val productMap = products.value.associateBy { it.id }
            val lines = items.mapNotNull { item ->
                productMap[item.prodId]?.let { p ->
                    p.id to CartLine(p, item.ctn, item.box, item.pcs)
                }
            }
            _cart.value = lines.toMap()
        }
    }

    fun setLine(product: Product, ctn: Int, box: Int, pcs: Int) {
        val m = _cart.value.toMutableMap()
        if (ctn == 0 && box == 0 && pcs == 0) m.remove(product.id)
        else m[product.id] = CartLine(product, ctn, box, pcs)
        _cart.value = m
    }
    fun removeLine(id: String) { _cart.value = _cart.value - id }

    fun cartTotalGross(): Long = _cart.value.values.sumOf { it.gross() }.toLong()
    fun cartTotalDiscount(): Long = _cart.value.values.sumOf { it.discount() }.toLong()
    fun cartNet(): Long = cartTotalGross() - cartTotalDiscount() - _codDiscount.value

    suspend fun saveTransaction(): TransactionEntity {
        val txId = _editTxId.value ?: repo.newId()
        val txItems = _cart.value.values.map {
            TxItem(
                prodId = it.product.id,
                name = it.product.name,
                category = it.product.category,
                price = it.product.price,
                ctn = it.ctn, box = it.box, pcs = it.pcs,
                totalCtn = it.totalCtn(),
                discountPerCtn = it.product.discountPerCtn,
                itemFinalNet = it.net().toLong()
            )
        }
        val entity = TransactionEntity(
            id = txId,
            storeId = _activeStoreId.value ?: "",
            date = System.currentTimeMillis(),
            itemsJson = repo.encodeItems(txItems),
            totalBruto = cartTotalGross(),
            totalReguler = cartTotalDiscount(),
            totalStrata = 0L,
            codDiscount = _codDiscount.value,
            netTotal = cartNet()
        )
        repo.saveTransaction(entity)
        return entity
    }

    fun resetCart() {
        _cart.value = emptyMap()
        _codDiscount.value = 0
        _activeStoreId.value = null
        _editTxId.value = null
    }

    // ----- CRUD helpers -----
    fun saveStore(s: Store) = viewModelScope.launch { repo.saveStore(s) }
    fun deleteStore(s: Store) = viewModelScope.launch { repo.deleteStore(s) }
    fun saveProduct(p: Product) = viewModelScope.launch { repo.saveProduct(p) }
    fun deleteProduct(p: Product) = viewModelScope.launch { repo.deleteProduct(p) }
    fun saveTarget(t: MonthlyTarget) = viewModelScope.launch { repo.saveTarget(t) }
    fun saveProfile(p: ProfileEntity) = viewModelScope.launch { repo.saveProfile(p) }
    fun deleteTransaction(id: String) = viewModelScope.launch { repo.deleteTransaction(id) }

    fun newId() = repo.newId()
    fun repo() = repo

    class Factory(private val repo: Repository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST") return MainVM(repo) as T
        }
    }
}
