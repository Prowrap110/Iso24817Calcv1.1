package com.protap.iso24817calc.domain

data class CalculationError(val code: String, val message: String, val field: String? = null)

sealed interface CalculationOutcome {
    data class Success(val result: CalculationResult) : CalculationOutcome
    data class Invalid(val errors: List<CalculationError>) : CalculationOutcome
}
