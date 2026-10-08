package com.salesapp.android.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.salesapp.android.bluetooth.BluetoothPrinter
import com.salesapp.android.bluetooth.ReceiptBuilder
import com.salesapp.android.ui.components.BrandTopBar
import com.salesapp.android.viewmodel.MainVM
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun ReceiptScreen(vm: MainVM, nav: NavController, txId: String) {
    val ctx = LocalContext.current
    var preview by remember { mutableStateOf("") }
    var tx by remember { mutableStateOf<com.salesapp.android.data.entities.TransactionEntity?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(txId) {
        val repo = vm.repo()
        val t = repo.getTransaction(txId) ?: return@LaunchedEffect
        tx = t
        val store = repo.getStore(t.storeId)
        val profile = repo.getProfile()
        val items = repo.decodeItems(t.itemsJson)
        preview = ReceiptBuilder.buildPreviewText(t, store, items, profile)
    }

    Scaffold(
        topBar = { BrandTopBar(title = "Struk Pesanan", onBack = { nav.popBackStack() }) }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Card(Modifier.weight(1f)) {
                Text(
                    preview,
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { nav.navigate("printer") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Bluetooth, null); Spacer(Modifier.width(6.dp)); Text("Printer")
                }
                Button(
                    onClick = {
                        val t = tx ?: return@Button
                        scope.launch {
                            val repo = vm.repo()
                            val store = repo.getStore(t.storeId)
                            val items = repo.decodeItems(t.itemsJson)
                            val profile = repo.getProfile()
                            if (!BluetoothPrinter.isConnected()) {
                                Toast.makeText(ctx, "Printer belum terhubung. Buka Printer dulu.", Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            val bytes = ReceiptBuilder.build(t, store, items, profile)
                            val res = BluetoothPrinter.print(bytes)
                            Toast.makeText(ctx,
                                if (res.isSuccess) "Struk terkirim ke printer" else "Gagal: ${res.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Icon(Icons.Filled.Print, null); Spacer(Modifier.width(6.dp)); Text("Cetak") }
            }
        }
    }
}

@Composable
fun HistoryScreen(vm: MainVM, nav: NavController, storeId: String?) {
    val txs by vm.transactions.collectAsState()
    val stores by vm.stores.collectAsState()
    val filtered = if (storeId.isNullOrBlank()) txs else txs.filter { it.storeId == storeId }
    val storeMap = stores.associateBy { it.id }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = {
        BrandTopBar(
            title = "Riwayat Transaksi",
            subtitle = storeId?.let { storeMap[it]?.name } ?: "Semua Toko",
            onBack = { nav.popBackStack() }
        )
    }) { inner ->
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filtered.isEmpty()) {
                item { Text("Belum ada transaksi.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            androidx.compose.foundation.lazy.items(filtered, key = { it.id }) { tx ->
                Card {
                    Column(Modifier.padding(14.dp)) {
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text(storeMap[tx.storeId]?.name ?: "(Toko dihapus)",
                                    style = MaterialTheme.typography.titleSmall)
                                Text(com.salesapp.android.util.Fmt.datetime(tx.date),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(com.salesapp.android.util.Fmt.rupiah(tx.netTotal),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { nav.navigate("receipt/${tx.id}") }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Receipt, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Struk")
                            }
                            OutlinedButton(onClick = {
                                vm.loadForEdit(tx); nav.navigate("order/${tx.storeId}")
                            }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Edit")
                            }
                            OutlinedButton(onClick = { confirmDelete = tx.id }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Hapus")
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { id ->
        AlertDialog(onDismissRequest = { confirmDelete = null },
            title = { Text("Hapus Transaksi?") }, text = { Text("Transaksi akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteTransaction(id); confirmDelete = null }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Batal") } })
    }
}
