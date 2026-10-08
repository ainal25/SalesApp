package com.salesapp.android.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.salesapp.android.backup.BackupManager
import com.salesapp.android.bluetooth.BluetoothPrinter
import com.salesapp.android.data.entities.MonthlyTarget
import com.salesapp.android.data.entities.ProfileEntity
import com.salesapp.android.ui.components.BrandTopBar
import com.salesapp.android.util.Fmt
import com.salesapp.android.viewmodel.MainVM
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: MainVM, nav: NavController) {
    Scaffold(topBar = { BrandTopBar(title = "Pengaturan") }) { inner ->
        LazyColumn(
            Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionTitle("Data & Target") }
            item { SettingItem(Icons.Filled.TrackChanges, "Atur Target Bulanan", "KSNI, Simba, Hello Panda") { nav.navigate("targets") } }
            item { SettingItem(Icons.Filled.History, "Semua Riwayat Transaksi", "Lihat, edit, hapus, cetak") { nav.navigate("history?storeId=") } }
            item { Spacer(Modifier.height(8.dp)); SectionTitle("Perangkat") }
            item { SettingItem(Icons.Filled.Bluetooth, "Printer Bluetooth 58mm", "ESC/POS thermal printer") { nav.navigate("printer") } }
            item { Spacer(Modifier.height(8.dp)); SectionTitle("Akun & Data") }
            item { SettingItem(Icons.Filled.Person, "Profil Salesman", "Nama, kode, foto") { nav.navigate("profile") } }
            item { SettingItem(Icons.Filled.CloudDownload, "Backup & Restore Data", "Export / import JSON") { nav.navigate("backup") } }
            item { Spacer(Modifier.height(32.dp)) }
            item { Text("Versi 1.0.0 • SalesApp Native", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun SectionTitle(t: String) {
    Text(t.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
@Composable private fun SettingItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null)
        }
    }
}

@Composable
fun TargetsScreen(vm: MainVM, nav: NavController) {
    val ym by vm.selectedYm.collectAsState()
    val target by vm.target.collectAsState()
    var k by remember(target) { mutableStateOf(target?.ksni?.toString().orEmpty().takeIf { it != "0" } ?: "") }
    var s by remember(target) { mutableStateOf(target?.simba?.toString().orEmpty().takeIf { it != "0" } ?: "") }
    var h by remember(target) { mutableStateOf(target?.hp?.toString().orEmpty().takeIf { it != "0" } ?: "") }
    var showMonth by remember { mutableStateOf(false) }

    Scaffold(topBar = { BrandTopBar(title = "Atur Target Bulanan", onBack = { nav.popBackStack() }) }) { inner ->
        Column(Modifier.padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AssistChip(onClick = { showMonth = true }, label = { Text("Bulan: ${Fmt.ymLabel(ym)}") })
            OutlinedTextField(k, { k = it.filter(Char::isDigit) }, label = { Text("Target KSNI") }, prefix = { Text("Rp ") },
                modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(s, { s = it.filter(Char::isDigit) }, label = { Text("Target Simba") }, prefix = { Text("Rp ") },
                modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(h, { h = it.filter(Char::isDigit) }, label = { Text("Target Hello Panda") }, prefix = { Text("Rp ") },
                modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(4.dp))
            Button(onClick = {
                vm.saveTarget(MonthlyTarget(
                    yearMonth = ym,
                    ksni = k.toLongOrNull() ?: 0,
                    simba = s.toLongOrNull() ?: 0,
                    hp = h.toLongOrNull() ?: 0
                ))
                nav.popBackStack()
            }, modifier = Modifier.fillMaxWidth()) { Text("Simpan Target") }
        }
    }
    if (showMonth) {
        AlertDialog(onDismissRequest = { showMonth = false },
            title = { Text("Pilih Bulan") },
            text = {
                LazyColumn(Modifier.height(300.dp)) {
                    items(Fmt.ymList()) { m ->
                        TextButton(onClick = { vm.setYm(m); showMonth = false }, modifier = Modifier.fillMaxWidth()) {
                            Text(Fmt.ymLabel(m))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMonth = false }) { Text("Tutup") } })
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun PrinterScreen(vm: MainVM, nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val perms = rememberMultiplePermissionsState(
        if (Build.VERSION.SDK_INT >= 31)
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        else listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    )
    var devices by remember { mutableStateOf<List<android.bluetooth.BluetoothDevice>>(emptyList()) }
    var connectedName by remember { mutableStateOf(BluetoothPrinter.connectedName) }

    LaunchedEffect(perms.allPermissionsGranted) {
        if (perms.allPermissionsGranted) devices = BluetoothPrinter.pairedDevices(ctx)
    }

    Scaffold(topBar = { BrandTopBar(title = "Printer Bluetooth", subtitle = "58mm Thermal (ESC/POS)", onBack = { nav.popBackStack() }) }) { inner ->
        Column(Modifier.padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!perms.allPermissionsGranted) {
                Card { Column(Modifier.padding(14.dp)) {
                    Text("Izin Bluetooth diperlukan untuk mencari printer.", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { perms.launchMultiplePermissionRequest() }) { Text("Beri Izin") }
                }}
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bluetooth, null, tint = if (connectedName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(connectedName?.let { "Terhubung: $it" } ?: "Belum terhubung",
                        fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick = {
                    devices = BluetoothPrinter.pairedDevices(ctx)
                    Toast.makeText(ctx, "Ditemukan ${devices.size} perangkat berpasangan", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Muat Ulang Daftar")
                }
                Text("Pasangkan printer terlebih dahulu dari Pengaturan Bluetooth Android, lalu pilih di bawah.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                    items(devices, key = { it.address }) { d ->
                        Card(onClick = {
                            scope.launch {
                                val res = BluetoothPrinter.connect(d)
                                if (res.isSuccess) {
                                    connectedName = BluetoothPrinter.connectedName
                                    Toast.makeText(ctx, "Terhubung ke ${connectedName}", Toast.LENGTH_SHORT).show()
                                } else Toast.makeText(ctx, "Gagal: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Print, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(try { d.name ?: "Unknown" } catch (_: SecurityException) { "Unknown" }, fontWeight = FontWeight.SemiBold)
                                    Text(d.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                if (connectedName != null) {
                    OutlinedButton(onClick = { BluetoothPrinter.disconnect(); connectedName = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Icon(Icons.Filled.BluetoothDisabled, null); Spacer(Modifier.width(6.dp)); Text("Putuskan") }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(vm: MainVM, nav: NavController) {
    val profile by vm.profile.collectAsState()
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var code by remember(profile) { mutableStateOf(profile?.code ?: "") }
    Scaffold(topBar = { BrandTopBar(title = "Profil Salesman", onBack = { nav.popBackStack() }) }) { inner ->
        Column(Modifier.padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(code, { code = it }, label = { Text("Kode Salesman") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                vm.saveProfile(ProfileEntity(id = 1, name = name.trim(), code = code.trim(), photoPath = profile?.photoPath ?: ""))
                nav.popBackStack()
            }, modifier = Modifier.fillMaxWidth()) { Text("Simpan") }
        }
    }
}

@Composable
fun BackupScreen(vm: MainVM, nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val r = BackupManager.export(ctx, uri)
            Toast.makeText(ctx, if (r.isSuccess) "Backup berhasil" else "Gagal: ${r.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val r = BackupManager.import(ctx, uri)
            Toast.makeText(ctx, if (r.isSuccess) "Restore berhasil (${r.getOrNull()} record)" else "Gagal: ${r.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
        }
    }
    Scaffold(topBar = { BrandTopBar(title = "Backup & Restore", onBack = { nav.popBackStack() }) }) { inner ->
        Column(Modifier.padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { exportLauncher.launch("salesapp-backup-${System.currentTimeMillis()}.json") },
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CloudUpload, null); Spacer(Modifier.width(6.dp)); Text("Export Backup (JSON)")
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json","*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CloudDownload, null); Spacer(Modifier.width(6.dp)); Text("Restore dari File JSON")
            }
            Text("Catatan: Restore akan menimpa data yang memiliki ID sama. Data lain tetap aman.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
