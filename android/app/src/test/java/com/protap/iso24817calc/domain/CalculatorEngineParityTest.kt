package com.protap.iso24817calc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineParityTest {
    private fun baseline(width: Double = 300.0, type: String = "Corrosion", location: String = "External") = CalculatorInputs(
        customer = "PROTAP", location = "Turkey", reportNo = "24-152", odMm = 457.2, wallMm = 9.53,
        yieldStrengthMpa = 359.0, pressureBar = 50.0, temperatureC = 40.0, defectType = type,
        defectLocation = location, defectLengthMm = 100.0, remainingWallMm = 4.5, internalCorrosionRateMmPerYear = if (location == "Internal") 0.1 else 0.0,
        designLifeYears = 20.0, designFactor = 0.72, installationTemperatureC = 20.0, componentType = "Straight",
        cyclicDeratingFactor = 1.0, axialLoadCase = 0, clothWidthMm = width,
    )

    @Test fun baselineMatchesPythonReference() {
        val result = (CalculatorEngine.calculate(baseline()) as CalculationOutcome.Success).result
        assertEquals("Type A (Load Sharing)", result.calcMethodThickness)
        assertEquals(3, result.numPlies)
        assertEquals(2, result.numBands)
        assertEquals(600.0, result.procurementLengthMm, 1e-9)
        assertEquals(2.585405090198256, result.optimizedSquareMeters, 1e-9)
    }

    @Test fun widthOnlyChangesProcurement() {
        val three = (CalculatorEngine.calculate(baseline(300.0)) as CalculationOutcome.Success).result
        val twoFifty = (CalculatorEngine.calculate(baseline(250.0)) as CalculationOutcome.Success).result
        assertEquals(three.numPlies, twoFifty.numPlies)
        assertEquals(500.0, twoFifty.procurementLengthMm, 1e-9)
        assertTrue(three.optimizedSquareMeters != twoFifty.optimizedSquareMeters)
    }

    @Test fun widthAtQualifiedOverlapIsAStableError() {
        val outcome = CalculatorEngine.calculate(baseline(50.0)) as CalculationOutcome.Invalid
        assertTrue(outcome.errors.any { it.code == "CLOTH_WIDTH_TOO_NARROW" })
    }

    @Test fun typeBLeakRetainsFormula12AndTwoYearCap() {
        val result = (CalculatorEngine.calculate(baseline(300.0, type = "Leak").copy(designLifeYears = 10.0)) as CalculationOutcome.Success).result
        assertEquals("Type B (Total Replacement)", result.calcMethodThickness)
        assertEquals(148, result.numPlies)
        assertEquals(11120.558488662406, result.isoLengthMm, 1e-6)
        assertEquals(45, result.numBands)
        assertEquals(13500.0, result.procurementLengthMm, 1e-9)
        assertTrue(result.typeB?.designLifeYears == 2.0)
    }

    @Test fun internalCorrosionUsesEndOfLifeWallAndTypeBRoute() {
        val result = (CalculatorEngine.calculate(baseline(300.0, location = "Internal")) as CalculationOutcome.Success).result
        assertEquals(2.5, result.remainingWallEolMm, 1e-9)
        assertEquals("Type B (Total Replacement)", result.calcMethodThickness)
    }
}
