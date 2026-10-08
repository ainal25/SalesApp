package com.salesapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salesapp.android.ui.MainNav
import com.salesapp.android.ui.theme.SalesAppTheme
import com.salesapp.android.viewmodel.MainVM

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repo = (application as SalesApplication).repo
        setContent {
            SalesAppTheme {
                val vm: MainVM = viewModel(factory = MainVM.Factory(repo))
                MainNav(vm)
            }
        }
    }
}
