package com.protap.iso24817calc.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorValidationTest {
    @Test fun blankFormListsExactVisibleLabels() {
        val outcome = CalculatorEngine.calculate(CalculatorInputs()) as CalculationOutcome.Invalid
        val messages = outcome.errors.map { it.message }
        assertTrue(messages.contains("Customer"))
        assertTrue(messages.contains("Prowrap CF cloth band width [mm]"))
    }
}
