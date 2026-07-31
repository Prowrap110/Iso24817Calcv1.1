package com.protap.iso24817calc

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectScaffoldTest {
    @Test
    fun packageNameIsStable() {
        assertEquals("com.protap.iso24817calc", BuildConfig.APPLICATION_ID)
    }
}
