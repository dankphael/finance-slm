package com.habibi.financeslm.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.habibi.financeslm.android.ui.navigation.AppNavGraph
import com.habibi.financeslm.android.ui.theme.FinanceSlmTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceSlmTheme {
                AppNavGraph()
            }
        }
    }
}