package com.salesapp.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.salesapp.android.data.entities.Store
import com.salesapp.android.ui.components.BrandTopBar
import com.salesapp.android.util.Fmt
import com.salesapp.android.viewmodel.MainVM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoresScreen(vm: MainVM, nav: NavController) {
    val stores by vm.stores.collectAsState()
    var query by remember { mutableStateOf("") }
    var actionStore by remember { mutableStateOf<Store?>(null) }
    var confirmDelete by remember { mutableStateOf<Store?>(null) }

    val filtered = stores.filter {
        query.isBlank() || it.name.contains(query, true) || it.address.contains(query, true)
    }

    Scaffold(
        topBar = { BrandTopBar(title = "Toko", subtitle = "${stores.size} toko terdaftar") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { nav.navigate("store_form?id=") },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Filled.Add, "Tambah", tint = MaterialTheme.colorScheme.onPrimary) }
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Cari nama atau alamat toko…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { store ->
                    Card(onClick = { actionStore = store }) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(store.name, fontWeight = FontWeight.SemiBold)
                                    if (store.isLoyalty) {
                                        Spacer(Modifier.width(8.dp))
                                        AssistChip(onClick = {}, label = { Text("Loyalty", style = MaterialTheme.typography.labelSmall) })
                                    }
                                }
                                if (store.address.isNotBlank())
                                    Text(store.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (store.isLoyalty && store.target > 0)
                                    Text("Target: ${Fmt.rupiah(store.target)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Filled.MoreVert, null)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    actionStore?.let { store ->
        ModalBottomSheet(onDismissRequest = { actionStore = null }) {
            Column(Modifier.padding(16.dp)) {
                Text(store.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                ActionRow(Icons.Filled.ShoppingCart, "Buat Transaksi / Pesanan") {
                    vm.startOrder(store.id); actionStore = null; nav.navigate("order/${store.id}")
                }
                ActionRow(Icons.Filled.History, "Riwayat Transaksi") {
                    actionStore = null; nav.navigate("history?storeId=${store.id}")
                }
                ActionRow(Icons.Filled.Edit, "Edit Toko") {
                    actionStore = null; nav.navigate("store_form?id=${store.id}")
                }
                ActionRow(Icons.Filled.Delete, "Hapus Toko", danger = true) {
                    confirmDelete = store; actionStore = null
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    confirmDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Hapus Toko?") },
            text = { Text("Toko \"${s.name}\" akan dihapus. Riwayat transaksi tidak ikut terhapus.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteStore(s); confirmDelete = null }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Text(label, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

// ---- Store Form ----
@Composable
fun StoreFormScreen(vm: MainVM, nav: NavController, idArg: String?) {
    val stores by vm.stores.collectAsState()
    val editing = remember(idArg, stores) { stores.firstOrNull { it.id == idArg } }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var address by remember { mutableStateOf(editing?.address ?: "") }
    var isLoyalty by remember { mutableStateOf(editing?.isLoyalty ?: false) }
    var target by remember { mutableStateOf((editing?.target ?: 0L).toString().takeIf { it != "0" } ?: "") }

    Scaffold(
        topBar = { BrandTopBar(title = if (editing == null) "Tambah Toko" else "Edit Toko", onBack = { nav.popBackStack() }) }
    ) { inner ->
        Column(Modifier.padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nama Toko *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(address, { address = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(isLoyalty, { isLoyalty = it })
                Spacer(Modifier.width(4.dp))
                Text("Toko Program Loyalty")
            }
            if (isLoyalty) {
                OutlinedTextField(
                    target, { target = it.filter(Char::isDigit) },
                    label = { Text("Target Rupiah Loyalty (KSNI)") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val s = Store(
                        id = editing?.id ?: vm.newId(),
                        name = name.trim(),
                        address = address.trim(),
                        isLoyalty = isLoyalty,
                        target = target.toLongOrNull() ?: 0L
                    )
                    vm.saveStore(s); nav.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) { Text(if (editing == null) "Simpan Toko" else "Update Toko") }
        }
    }
}
