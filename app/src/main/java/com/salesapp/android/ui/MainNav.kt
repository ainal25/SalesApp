package com.salesapp.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.salesapp.android.ui.screens.*
import com.salesapp.android.viewmodel.MainVM

sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Dest("dashboard", "Beranda", Icons.Filled.Home)
    data object Stores    : Dest("stores",    "Toko",    Icons.Filled.Storefront)
    data object Products  : Dest("products",  "Produk",  Icons.Filled.Inventory2)
    data object Settings  : Dest("settings",  "Atur",    Icons.Filled.Settings)
}
private val bottomTabs = listOf(Dest.Dashboard, Dest.Stores, Dest.Products, Dest.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNav(vm: MainVM) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isTabRoute = bottomTabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTabRoute) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomTabs.forEach { tab ->
                        val selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Dest.Dashboard.route,
            modifier = Modifier.padding(inner)
        ) {
            composable(Dest.Dashboard.route) { DashboardScreen(vm, nav) }
            composable(Dest.Stores.route)    { StoresScreen(vm, nav) }
            composable(Dest.Products.route)  { ProductsScreen(vm, nav) }
            composable(Dest.Settings.route)  { SettingsScreen(vm, nav) }

            composable("store_form?id={id}") {
                StoreFormScreen(vm, nav, it.arguments?.getString("id"))
            }
            composable("product_form?id={id}") {
                ProductFormScreen(vm, nav, it.arguments?.getString("id"))
            }
            composable("order/{storeId}") {
                OrderScreen(vm, nav, it.arguments?.getString("storeId") ?: "")
            }
            composable("cart") { CartScreen(vm, nav) }
            composable("receipt/{txId}") {
                ReceiptScreen(vm, nav, it.arguments?.getString("txId") ?: "")
            }
            composable("history?storeId={storeId}") {
                HistoryScreen(vm, nav, it.arguments?.getString("storeId"))
            }
            composable("targets") { TargetsScreen(vm, nav) }
            composable("printer") { PrinterScreen(vm, nav) }
            composable("profile") { ProfileScreen(vm, nav) }
            composable("backup")  { BackupScreen(vm, nav) }
        }
    }
}
