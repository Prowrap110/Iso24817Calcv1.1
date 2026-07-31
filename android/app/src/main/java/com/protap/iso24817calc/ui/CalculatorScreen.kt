package com.protap.iso24817calc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.protap.iso24817calc.domain.CalculationResult

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CalculatorScreen(viewModel: CalculatorViewModel, onCreatePdf: (CalculationResult) -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = { TopAppBar(title = { Text("PROWRAP ISO 24817 Calculator") }) },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().imePadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.formError != null) Text(state.formError!!, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = viewModel::calculate, modifier = Modifier.weight(1f)) { Text("Calculate & Optimize") }
                    OutlinedButton(onClick = { viewModel.clearCalculation(); scope.launch { listState.animateScrollToItem(0) } }, modifier = Modifier.weight(1f)) { Text("Clear") }
                }
            }
        },
    ) { padding ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { InputSection("Project Info") { TextInput(FieldId.CUSTOMER, state, viewModel); TextInput(FieldId.LOCATION, state, viewModel); TextInput(FieldId.REPORT_NO, state, viewModel) } }
            item { InputSection("Pipeline Data") { NumberInput(InputFieldSpec(FieldId.OD_MM, "Pipe OD", "mm"), state, viewModel); NumberInput(InputFieldSpec(FieldId.WALL_MM, "Nominal Wall", "mm"), state, viewModel); NumberInput(InputFieldSpec(FieldId.YIELD_MPA, "Pipe Yield", "MPa"), state, viewModel) } }
            item { InputSection("Service Conditions") { NumberInput(InputFieldSpec(FieldId.PRESSURE_BAR, "Design Pressure", "bar"), state, viewModel); NumberInput(InputFieldSpec(FieldId.TEMPERATURE_C, "Op. Temperature", "°C"), state, viewModel) } }
            item { InputSection("Defect Data") {
                SelectionField(FieldId.DEFECT_TYPE, "Mechanism", state.selections[FieldId.DEFECT_TYPE] ?: SELECT_PLACEHOLDER, listOf("Corrosion", "Dent", "Leak", "Crack"), state.errors[FieldId.DEFECT_TYPE]) { viewModel.select(FieldId.DEFECT_TYPE, it) }
                SelectionField(FieldId.DEFECT_LOCATION, "Location", state.selections[FieldId.DEFECT_LOCATION] ?: SELECT_PLACEHOLDER, listOf("External", "Internal"), state.errors[FieldId.DEFECT_LOCATION]) { viewModel.select(FieldId.DEFECT_LOCATION, it) }
                NumberInput(InputFieldSpec(FieldId.DEFECT_LENGTH_MM, "Defect Length", "mm"), state, viewModel); NumberInput(InputFieldSpec(FieldId.REMAINING_WALL_MM, "Remaining Wall", "mm"), state, viewModel)
                if (state.selections[FieldId.DEFECT_TYPE] == "Corrosion" && state.selections[FieldId.DEFECT_LOCATION] == "Internal") NumberInput(InputFieldSpec(FieldId.CORROSION_RATE, "Internal Corrosion Rate", "mm/yr"), state, viewModel)
            } }
            item { InputSection("Safety & Design") { NumberInput(InputFieldSpec(FieldId.DESIGN_LIFE, "Design Life", "years"), state, viewModel); NumberInput(InputFieldSpec(FieldId.DESIGN_FACTOR, "Design Factor (f)"), state, viewModel) } }
            item { InputSection("Installation & Load") {
                NumberInput(InputFieldSpec(FieldId.INSTALLATION_TEMPERATURE_C, "Installation temperature", "°C"), state, viewModel)
                SelectionField(FieldId.COMPONENT_TYPE, "Component type", state.selections[FieldId.COMPONENT_TYPE] ?: SELECT_PLACEHOLDER, listOf("Straight", "Bend", "Tee", "Flange", "Reducer"), state.errors[FieldId.COMPONENT_TYPE]) { viewModel.select(FieldId.COMPONENT_TYPE, it) }
                NumberInput(InputFieldSpec(FieldId.CYCLIC_FACTOR, "Cyclic derating factor"), state, viewModel)
                SelectionField(FieldId.AXIAL_LOAD_CASE, "Axial load case", state.selections[FieldId.AXIAL_LOAD_CASE] ?: SELECT_PLACEHOLDER, listOf("0", "1"), state.errors[FieldId.AXIAL_LOAD_CASE]) { viewModel.select(FieldId.AXIAL_LOAD_CASE, it) }
            } }
            item { InputSection("Prowrap Material") {
                NumberInput(InputFieldSpec(FieldId.CLOTH_WIDTH_MM, "Prowrap CF cloth band width", "mm"), state, viewModel)
                Text("Used for procurement band count and cloth length. It must exceed the fixed 50 mm stitch overlap.")
            } }
            item { state.result?.let { result -> ResultsCard(result, viewModel::requestThreeLayers) { onCreatePdf(result) }; WarningCard(result.warnings) } }
            item { androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 80.dp)) }
        }
    }
}

@Composable private fun TextInput(field: FieldId, state: CalculatorFormState, vm: CalculatorViewModel) {
    androidx.compose.material3.OutlinedTextField(value = state.fields[field] ?: "", onValueChange = { vm.updateField(field, it) }, label = { Text(field.label) }, isError = state.errors[field] != null, supportingText = state.errors[field]?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable private fun NumberInput(spec: InputFieldSpec, state: CalculatorFormState, vm: CalculatorViewModel) {
    ValidatedNumberField(spec.field, spec.label, spec.unit, state.fields[spec.field] ?: "", state.errors[spec.field], spec.integer) { vm.updateField(spec.field, it) }
}
