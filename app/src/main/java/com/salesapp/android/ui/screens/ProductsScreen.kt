package com.salesapp.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.salesapp.android.data.entities.Product
import com.salesapp.android.ui.components.BrandTopBar
import com.salesapp.android.util.Fmt
import com.salesapp.android.util.categories
import com.salesapp.android.viewmodel.MainVM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(vm: MainVM, nav: NavController) {
    val products by vm.products.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Semua") }
    var actionProduct by remember { mutableStateOf<Product?>(null) }
    var confirmDelete by remember { mutableStateOf<Product?>(null) }

    val filtered = products.filter {
        (filter == "Semua" || it.category == filter) &&
            (query.isBlank() || it.name.contains(query, true))
    }

    Scaffold(
        topBar = { BrandTopBar(title = "Produk", subtitle = "${products.size} produk") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { nav.navigate("product_form?id=") },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Filled.Add, "Tambah", tint = MaterialTheme.colorScheme.onPrimary) }
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            OutlinedTextField(
                query, { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
                placeholder = { Text("Cari nama produk…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true, shape = MaterialTheme.shapes.medium
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (listOf("Semua") + categories).forEach { cat ->
                    FilterChip(
                        selected = filter == cat,
                        onClick = { filter = cat },
                        label = { Text(cat) }
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { p ->
                    Card(onClick = { actionProduct = p }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.SemiBold)
                                Text("${p.category} • ${p.boxPerCtn} BOX/CTN • ${p.pcsPerBox} PCS/BOX",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Fmt.rupiah(p.price) + " / CTN", color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                if (p.discountPerCtn > 0)
                                    Text("Diskon ${Fmt.rupiah(p.discountPerCtn)}/CTN",
                                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                            Icon(Icons.Filled.MoreVert, null)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    actionProduct?.let { p ->
        ModalBottomSheet(onDismissRequest = { actionProduct = null }) {
            Column(Modifier.padding(16.dp)) {
                Text(p.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                ListItem(
                    headlineContent = { Text("Edit Produk") },
                    leadingContent = { Icon(Icons.Filled.Edit, null) },
                    modifier = Modifier.clickable { actionProduct = null; nav.navigate("product_form?id=${p.id}") }
                )
                ListItem(
                    headlineContent = { Text("Hapus Produk", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { confirmDelete = p; actionProduct = null }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    confirmDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Hapus Produk?") },
            text = { Text("Produk \"${p.name}\" akan dihapus.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteProduct(p); confirmDelete = null }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Batal") } }
        )
    }
}

@Composable
fun ProductFormScreen(vm: MainVM, nav: NavController, idArg: String?) {
    val products by vm.products.collectAsState()
    val editing = remember(idArg, products) { products.firstOrNull { it.id == idArg } }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var category by remember { mutableStateOf(editing?.category ?: "KSNI") }
    var price by remember { mutableStateOf(editing?.price?.toString()?.takeIf { it != "0" } ?: "") }
    var boxPerCtn by remember { mutableStateOf((editing?.boxPerCtn ?: 1).toString()) }
    var pcsPerBox by remember { mutableStateOf((editing?.pcsPerBox ?: 1).toString()) }
    var discount by remember { mutableStateOf(editing?.discountPerCtn?.toString()?.takeIf { it != "0" } ?: "") }
    var expandCat by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { BrandTopBar(title = if (editing == null) "Tambah Produk" else "Edit Produk", onBack = { nav.popBackStack() }) }
    ) { inner ->
        Column(Modifier.padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nama Produk *") }, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = expandCat, onExpandedChange = { expandCat = !expandCat }) {
                OutlinedTextField(
                    category, {}, readOnly = true,
                    label = { Text("Kategori") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandCat) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expandCat, onDismissRequest = { expandCat = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expandCat = false })
                    }
                }
            }
            OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Harga per CTN *") }, prefix = { Text("Rp ") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(boxPerCtn, { boxPerCtn = it.filter(Char::isDigit) }, label = { Text("BOX / CTN") }, modifier = Modifier.weight(1f))
                OutlinedTextField(pcsPerBox, { pcsPerBox = it.filter(Char::isDigit) }, label = { Text("PCS / BOX") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(discount, { discount = it.filter(Char::isDigit) }, label = { Text("Diskon per CTN (opsional)") }, prefix = { Text("Rp ") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (name.isBlank() || price.isBlank()) return@Button
                    val p = Product(
                        id = editing?.id ?: vm.newId(),
                        name = name.trim(),
                        category = category,
                        price = price.toLongOrNull() ?: 0L,
                        boxPerCtn = boxPerCtn.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                        pcsPerBox = pcsPerBox.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                        discountPerCtn = discount.toLongOrNull() ?: 0L
                    )
                    vm.saveProduct(p); nav.popBackStack()
                },
                enabled = name.isNotBlank() && price.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (editing == null) "Simpan Produk" else "Update Produk") }
        }
    }
}
