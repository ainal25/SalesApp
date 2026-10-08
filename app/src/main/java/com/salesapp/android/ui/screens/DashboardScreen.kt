package com.salesapp.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.salesapp.android.data.entities.MonthlyTarget
import com.salesapp.android.data.entities.Store
import com.salesapp.android.data.entities.TransactionEntity
import com.salesapp.android.ui.components.BrandTopBar
import com.salesapp.android.ui.theme.Pink
import com.salesapp.android.ui.theme.BrandGreen
import com.salesapp.android.ui.theme.WarningOrange
import com.salesapp.android.util.Fmt
import com.salesapp.android.viewmodel.MainVM

@Composable
fun DashboardScreen(vm: MainVM, nav: NavController) {
    val txs by vm.transactions.collectAsState()
    val stores by vm.stores.collectAsState()
    val ym by vm.selectedYm.collectAsState()
    val target by vm.target.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }

    // Compute achievements for selected month
    val txMonth = remember(txs, ym) { txs.filter { Fmt.ymOf(it.date) == ym } }
    val repo = remember { vm.repo() }
    val achieve = remember(txMonth) {
        val m = mutableMapOf("KSNI" to 0L, "Simba" to 0L, "Hello Panda" to 0L, "Amo" to 0L)
        txMonth.forEach { tx ->
            repo.decodeItems(tx.itemsJson).forEach { item ->
                m[item.category] = (m[item.category] ?: 0L) + item.itemFinalNet
            }
        }
        m
    }

    Column(Modifier.fillMaxSize()) {
        BrandTopBar(
            title = "SalesApp",
            subtitle = "Manajemen Salesman",
            trailing = {
                IconButton(onClick = { nav.navigate("profile") }) {
                    Icon(Icons.Filled.AccountCircle, "Profil", tint = Color.White)
                }
            }
        )
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pencapaian & Target", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    AssistChip(
                        onClick = { showMonthPicker = true },
                        label = { Text(Fmt.ymLabel(ym)) },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            item {
                TargetCard("KSNI", achieve["KSNI"] ?: 0L, target?.ksni ?: 0L, BrandGreen)
            }
            item {
                TargetCard("Simba", achieve["Simba"] ?: 0L, target?.simba ?: 0L, WarningOrange)
            }
            item {
                TargetCard("Hello Panda", achieve["Hello Panda"] ?: 0L, target?.hp ?: 0L, Pink)
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Toko Loyalty", fontWeight = FontWeight.Bold)
            }
            items(stores.filter { it.isLoyalty }, key = { it.id }) { s ->
                LoyaltyStoreCard(s, txMonth, repo)
            }
            if (stores.none { it.isLoyalty }) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            "Belum ada toko loyalty. Tandai sebuah toko sebagai Loyalty di tab Toko.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = { Text("Pilih Bulan") },
            text = {
                LazyColumn(Modifier.height(300.dp)) {
                    items(Fmt.ymList()) { month ->
                        TextButton(onClick = {
                            vm.setYm(month); showMonthPicker = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(Fmt.ymLabel(month))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMonthPicker = false }) { Text("Tutup") } }
        )
    }
}

@Composable
private fun TargetCard(name: String, achieved: Long, target: Long, accent: Color) {
    val pct = if (target > 0) (achieved * 100 / target).coerceAtMost(999) else 0
    val deficit = (target - achieved).coerceAtLeast(0)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name.uppercase(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(color = accent.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text("$pct%", color = accent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text("Target: ${Fmt.rupiah(target)}", color = accent, style = MaterialTheme.typography.labelMedium)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Fmt.rupiah(achieved), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Kekurangan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Fmt.rupiah(deficit), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
            LinearProgressIndicator(
                progress = { if (target > 0) (achieved.toFloat() / target).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = accent,
                trackColor = accent.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
private fun LoyaltyStoreCard(store: Store, txMonth: List<TransactionEntity>, repo: com.salesapp.android.data.Repository) {
    val ach = txMonth.filter { it.storeId == store.id }.sumOf { tx ->
        repo.decodeItems(tx.itemsJson).filter { it.category == "KSNI" }.sumOf { it.itemFinalNet }
    }
    val pct = if (store.target > 0) (ach * 100 / store.target).coerceAtMost(999) else 0
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(store.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("$pct%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text("KSNI: ${Fmt.rupiah(ach)} / ${Fmt.rupiah(store.target)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { if (store.target > 0) (ach.toFloat()/store.target).coerceIn(0f,1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
            )
        }
    }
}
