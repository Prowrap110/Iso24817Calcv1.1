package com.protap.iso24817calc.ui

import com.protap.iso24817calc.domain.CalculationResult

const val SELECT_PLACEHOLDER = "Select…"

enum class FieldId(val label: String) {
    CUSTOMER("Customer"), LOCATION("Location"), REPORT_NO("Report No"), OD_MM("Pipe OD [mm]"),
    WALL_MM("Nominal Wall [mm]"), YIELD_MPA("Pipe Yield [MPa]"), PRESSURE_BAR("Design Pressure [bar]"),
    TEMPERATURE_C("Op. Temperature [°C]"), DEFECT_TYPE("Mechanism"), DEFECT_LOCATION("Location"),
    DEFECT_LENGTH_MM("Defect Length [mm]"), REMAINING_WALL_MM("Remaining Wall [mm]"),
    CORROSION_RATE("Internal Corrosion Rate [mm/yr]"), DESIGN_LIFE("Design Life [years]"),
    DESIGN_FACTOR("Design Factor (f)"), INSTALLATION_TEMPERATURE_C("Installation temperature [°C]"),
    COMPONENT_TYPE("Component type"), CYCLIC_FACTOR("Cyclic derating factor"), AXIAL_LOAD_CASE("Axial load case"),
    CLOTH_WIDTH_MM("Prowrap CF cloth band width [mm]"),
}

data class CalculatorFormState(
    val fields: Map<FieldId, String> = FieldId.entries.associateWith { "" },
    val selections: Map<FieldId, String> = mapOf(
        FieldId.DEFECT_TYPE to SELECT_PLACEHOLDER,
        FieldId.DEFECT_LOCATION to SELECT_PLACEHOLDER,
        FieldId.COMPONENT_TYPE to SELECT_PLACEHOLDER,
        FieldId.AXIAL_LOAD_CASE to SELECT_PLACEHOLDER,
    ),
    val result: CalculationResult? = null,
    val errors: Map<FieldId, String> = emptyMap(),
    val formError: String? = null,
    val isCalculating: Boolean = false,
    val forceThreeLayers: Boolean = false,
)
