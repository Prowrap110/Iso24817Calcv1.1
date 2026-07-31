package com.protap.iso24817calc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.protap.iso24817calc.domain.CalculationResult

@Composable
fun ResultsCard(result: CalculationResult, onRequestThreeLayers: () -> Unit, onCreatePdf: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Calculation complete", style = MaterialTheme.typography.headlineSmall)
            Text("${result.calcMethodThickness} • ${result.numPlies} layers • ${"%.2f".format(result.finalThicknessMm)} mm")
            Metric("Required repair length", "%.0f mm".format(result.isoLengthMm))
            Metric("Procurement", "${result.numBands} × %.0f mm = %.0f mm".format(result.clothWidthMm, result.procurementLengthMm))
            Metric("Optimized fabric", "%.2f m²".format(result.optimizedSquareMeters))
            Metric("Epoxy", "%.1f kg".format(result.epoxyKg))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRequestThreeLayers, modifier = Modifier.weight(1f)) { Text("Use 3 layers") }
                Button(onClick = onCreatePdf, modifier = Modifier.weight(1f)) { Text("Create PDF") }
            }
        }
    }
}

@Composable private fun Metric(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, style = MaterialTheme.typography.titleMedium) }
}
