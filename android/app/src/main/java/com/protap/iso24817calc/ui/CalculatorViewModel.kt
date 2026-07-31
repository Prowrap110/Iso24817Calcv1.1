package com.protap.iso24817calc.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.protap.iso24817calc.domain.CalculationError
import com.protap.iso24817calc.domain.CalculationOutcome
import com.protap.iso24817calc.domain.CalculatorEngine
import com.protap.iso24817calc.domain.CalculatorInputs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalculatorViewModel : ViewModel() {
    private val _state = MutableStateFlow(CalculatorFormState())
    val state: StateFlow<CalculatorFormState> = _state.asStateFlow()

    fun updateField(field: FieldId, value: String) {
        val fields = _state.value.fields.toMutableMap().apply { put(field, value) }
        _state.value = _state.value.copy(fields = fields, result = null, errors = emptyMap(), formError = null)
    }

    fun select(field: FieldId, value: String) {
        val selections = _state.value.selections.toMutableMap().apply { put(field, value) }
        _state.value = _state.value.copy(selections = selections, result = null, errors = emptyMap(), formError = null)
    }

    fun requestThreeLayers() {
        _state.value = _state.value.copy(forceThreeLayers = true, result = null)
        calculate()
    }

    fun clearCalculation() {
        _state.value = CalculatorFormState()
    }

    fun calculate() {
        if (_state.value.isCalculating) return
        _state.value = _state.value.copy(isCalculating = true, errors = emptyMap(), formError = null)
        viewModelScope.launch {
            when (val outcome = CalculatorEngine.calculate(toInputs(_state.value))) {
                is CalculationOutcome.Success -> _state.value = _state.value.copy(isCalculating = false, result = outcome.result)
                is CalculationOutcome.Invalid -> {
                    val fieldErrors = outcome.errors.mapNotNull { error -> fieldFor(error)?.let { it to error.message } }.toMap()
                    _state.value = _state.value.copy(
                        isCalculating = false,
                        errors = fieldErrors,
                        formError = "Complete the highlighted fields before calculating.",
                    )
                }
            }
        }
    }

    private fun fieldFor(error: CalculationError): FieldId? = FieldId.entries.firstOrNull { it.label == error.field }

    private fun toInputs(s: CalculatorFormState): CalculatorInputs {
        fun text(field: FieldId): String? = s.fields[field]?.trim()?.takeIf { it.isNotEmpty() }
        fun number(field: FieldId): Double? = text(field)?.replace(',', '.')?.toDoubleOrNull()
        fun selection(field: FieldId): String? = s.selections[field]?.takeUnless { it == SELECT_PLACEHOLDER || it.isBlank() }
        return CalculatorInputs(
            customer = text(FieldId.CUSTOMER), location = text(FieldId.LOCATION), reportNo = text(FieldId.REPORT_NO),
            odMm = number(FieldId.OD_MM), wallMm = number(FieldId.WALL_MM), yieldStrengthMpa = number(FieldId.YIELD_MPA),
            pressureBar = number(FieldId.PRESSURE_BAR), temperatureC = number(FieldId.TEMPERATURE_C), defectType = selection(FieldId.DEFECT_TYPE),
            defectLocation = selection(FieldId.DEFECT_LOCATION), defectLengthMm = number(FieldId.DEFECT_LENGTH_MM), remainingWallMm = number(FieldId.REMAINING_WALL_MM),
            internalCorrosionRateMmPerYear = number(FieldId.CORROSION_RATE), designLifeYears = number(FieldId.DESIGN_LIFE), designFactor = number(FieldId.DESIGN_FACTOR),
            installationTemperatureC = number(FieldId.INSTALLATION_TEMPERATURE_C), componentType = selection(FieldId.COMPONENT_TYPE), cyclicDeratingFactor = number(FieldId.CYCLIC_FACTOR),
            axialLoadCase = selection(FieldId.AXIAL_LOAD_CASE)?.toIntOrNull(), clothWidthMm = number(FieldId.CLOTH_WIDTH_MM), forceThreeLayers = s.forceThreeLayers,
        )
    }
}
