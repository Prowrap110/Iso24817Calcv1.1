package com.protap.iso24817calc.ui

data class InputFieldSpec(val field: FieldId, val label: String, val unit: String = "", val integer: Boolean = false)

val projectFields = listOf(
    InputFieldSpec(FieldId.CUSTOMER, "Customer"), InputFieldSpec(FieldId.LOCATION, "Location"), InputFieldSpec(FieldId.REPORT_NO, "Report No"),
)
val pipelineFields = listOf(
    InputFieldSpec(FieldId.OD_MM, "Pipe OD", "mm"), InputFieldSpec(FieldId.WALL_MM, "Nominal Wall", "mm"), InputFieldSpec(FieldId.YIELD_MPA, "Pipe Yield", "MPa"),
)
val serviceFields = listOf(
    InputFieldSpec(FieldId.PRESSURE_BAR, "Design Pressure", "bar"), InputFieldSpec(FieldId.TEMPERATURE_C, "Op. Temperature", "°C"),
)
val defectFields = listOf(
    InputFieldSpec(FieldId.DEFECT_LENGTH_MM, "Defect Length", "mm"), InputFieldSpec(FieldId.REMAINING_WALL_MM, "Remaining Wall", "mm"),
    InputFieldSpec(FieldId.CORROSION_RATE, "Internal Corrosion Rate", "mm/yr"),
)
val safetyFields = listOf(InputFieldSpec(FieldId.DESIGN_LIFE, "Design Life", "years"), InputFieldSpec(FieldId.DESIGN_FACTOR, "Design Factor (f)"))
val installationFields = listOf(InputFieldSpec(FieldId.INSTALLATION_TEMPERATURE_C, "Installation temperature", "°C"), InputFieldSpec(FieldId.CYCLIC_FACTOR, "Cyclic derating factor"))
