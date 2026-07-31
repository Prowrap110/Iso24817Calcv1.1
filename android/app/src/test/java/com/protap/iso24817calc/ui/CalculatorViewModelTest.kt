package com.protap.iso24817calc.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun startsBlankAndClearPreservesControlContract() {
        val vm = CalculatorViewModel()
        assertTrue(vm.state.value.fields.values.all { it.isEmpty() })
        vm.updateField(FieldId.CLOTH_WIDTH_MM, "300")
        vm.clearCalculation()
        assertEquals("", vm.state.value.fields[FieldId.CLOTH_WIDTH_MM])
        assertEquals(SELECT_PLACEHOLDER, vm.state.value.selections[FieldId.DEFECT_TYPE])
        assertEquals(null, vm.state.value.result)
    }

    @Test fun incompleteCalculationIsActionable() = runTest {
        val vm = CalculatorViewModel()
        vm.calculate()
        testScheduler.advanceUntilIdle()
        assertEquals("Complete the highlighted fields before calculating.", vm.state.value.formError)
        assertTrue(vm.state.value.errors.values.any { it == "Customer" })
    }
}
