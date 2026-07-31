package com.protap.iso24817calc.domain

/** Typed, nullable form contract. Null means the user has not entered a value. */
data class CalculatorInputs(
    val customer: String? = null,
    val location: String? = null,
    val reportNo: String? = null,
    val odMm: Double? = null,
    val wallMm: Double? = null,
    val yieldStrengthMpa: Double? = null,
    val pressureBar: Double? = null,
    val temperatureC: Double? = null,
    val defectType: String? = null,
    val defectLocation: String? = null,
    val defectLengthMm: Double? = null,
    val remainingWallMm: Double? = null,
    val internalCorrosionRateMmPerYear: Double? = null,
    val designLifeYears: Double? = null,
    val designFactor: Double? = null,
    val installationTemperatureC: Double? = null,
    val componentType: String? = null,
    val cyclicDeratingFactor: Double? = null,
    val axialLoadCase: Int? = null,
    val clothWidthMm: Double? = null,
    val forceThreeLayers: Boolean = false,
)
