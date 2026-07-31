package com.protap.iso24817calc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.protap.iso24817calc.ui.CalculatorScreen
import com.protap.iso24817calc.ui.theme.ProWrapTheme
import com.protap.iso24817calc.report.ReportShareController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProWrapTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CalculatorScreen(viewModel()) { result -> ReportShareController.share(this@MainActivity, result) }
                }
            }
        }
    }
}
