package com.salesapp.android.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.salesapp.android.data.entities.Product
import com.salesapp.android.ui.components.BrandTopBar
import com.salesapp.android.util.Fmt
import com.salesapp.android.util.categories
import com.salesapp.android.viewmodel.CartLine
import com.salesapp.android.viewmodel.MainVM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(vm: MainVM, nav: NavController, storeId: String) {
    val stores by vm.stores.collectAsState()
    val products by vm.products.collectAsState()
    val cart by vm.cart.collectAsState()
    val store = stores.firstOrNull { it.id == storeId }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Semua") }
    var qtyProduct by remember { mutableStateOf<Product?>(null) }

    val filtered = products.filter {
        (filter == "Semua" || it.category == filter) &&
            (query.isBlank() || it.name.contains(query, true))
    }

    Scaffold(
        topBar = {
            BrandTopBar(
                title = "Buat Pesanan",
                subtitle = store?.name ?: "",
                onBack = { nav.popBackStack() }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Total (bersih)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Fmt.rupiah(vm.cartNet()), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = { nav.navigate("cart") },
                        enabled = cart.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.ShoppingCart, null); Spacer(Modifier.width(6.dp))
                        Text("Keranjang (${cart.size})")
                    }
                }
            }
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            OutlinedTextField(
                query, { query = it },
                placeholder = { Text("Cari produk pesanan…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (listOf("Semua") + categories).forEach { cat ->
                    FilterChip(selected = filter == cat, onClick = { filter = cat }, label = { Text(cat) })
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { p ->
                    val line = cart[p.id]
                    Card(onClick = { qtyProduct = p }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.SemiBold)
                                Text(Fmt.rupiah(p.price) + " / CTN",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium)
                                if (line != null) {
                                    Text("Pesanan: ${qtyText(line)} • ${Fmt.rupiah(line.net().toLong())}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                            Icon(if (line == null) Icons.Filled.Add else Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    qtyProduct?.let { p ->
        QtyPopup(product = p, current = cart[p.id],
            onDismiss = { qtyProduct = null },
            onSave = { c, b, pcs -> vm.setLine(p, c, b, pcs); qtyProduct = null }
        )
    }
}

private fun qtyText(l: CartLine): String {
    val parts = mutableListOf<String>()
    if (l.ctn > 0) parts += "${l.ctn} CTN"
    if (l.box > 0) parts += "${l.box} BOX"
    if (l.pcs > 0) parts += "${l.pcs} PCS"
    return parts.joinToString(" + ").ifEmpty { "0" }
}

@Composable
private fun QtyPopup(product: Product, current: CartLine?, onDismiss: () -> Unit, onSave: (Int, Int, Int) -> Unit) {
    var ctn by remember { mutableStateOf(current?.ctn?.toString() ?: "") }
    var box by remember { mutableStateOf(current?.box?.toString() ?: "") }
    var pcs by remember { mutableStateOf(current?.pcs?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Harga: ${Fmt.rupiah(product.price)} / CTN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(ctn, { ctn = it.filter(Char::isDigit) }, label = { Text("CTN") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(box, { box = it.filter(Char::isDigit) }, label = { Text("BOX") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(pcs, { pcs = it.filter(Char::isDigit) }, label = { Text("PCS") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Text("Isi: 1 CTN = ${product.boxPerCtn} BOX = ${product.boxPerCtn * product.pcsPerBox} PCS",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(ctn.toIntOrNull() ?: 0, box.toIntOrNull() ?: 0, pcs.toIntOrNull() ?: 0) }) {
                Text("Simpan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(vm: MainVM, nav: NavController) {
    val cart by vm.cart.collectAsState()
    val cod by vm.codDiscount.collectAsState()
    val stores by vm.stores.collectAsState()
    val storeId by vm.activeStoreId.collectAsState()
    val store = stores.firstOrNull { it.id == storeId }
    var codText by remember(cod) { mutableStateOf(if (cod == 0L) "" else cod.toString()) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { BrandTopBar(title = "Keranjang", subtitle = store?.name ?: "", onBack = { nav.popBackStack() }) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp).navigationBarsPadding()) {
                    Row { Text("Bruto", Modifier.weight(1f)); Text(Fmt.rupiah(vm.cartTotalGross())) }
                    if (vm.cartTotalDiscount() > 0) {
                        Row { Text("Diskon Produk", Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                            Text("- " + Fmt.rupiah(vm.cartTotalDiscount()), color = MaterialTheme.colorScheme.error) }
                    }
                    if (cod > 0) Row {
                        Text("Diskon COD", Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                        Text("- " + Fmt.rupiah(cod), color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Text("TOTAL BERSIH", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(Fmt.rupiah(vm.cartNet()), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val tx = vm.saveTransaction()
                                vm.resetCart()
                                nav.navigate("receipt/${tx.id}") { popUpTo("stores") }
                            }
                        },
                        enabled = cart.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Simpan & Cetak Struk") }
                }
            }
        }
    ) { inner ->
        LazyColumn(
            Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cart.values.toList(), key = { it.product.id }) { line ->
                Card {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(line.product.name, fontWeight = FontWeight.SemiBold)
                            Text(qtyText(line), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Fmt.rupiah(line.net().toLong()), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { vm.removeLine(line.product.id) }) {
                            Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    codText, {
                        codText = it.filter(Char::isDigit)
                        vm.setCod(codText.toLongOrNull() ?: 0L)
                    },
                    label = { Text("Diskon COD (opsional)") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            if (cart.isEmpty()) {
                item {
                    Text("Keranjang kosong. Kembali untuk pilih produk.",
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

