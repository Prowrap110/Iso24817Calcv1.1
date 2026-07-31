package com.protap.iso24817calc.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceVectorSchemaTest {
    @Test fun exportedVectorsHaveStableSchemaMarkers() {
        val json = javaClass.classLoader!!.getResource("reference_vectors.json")!!.readText()
        assertTrue(json.contains("\"name\""))
        assertTrue(json.contains("\"inputs\""))
        assertTrue(json.contains("\"expected\""))
        assertTrue(json.contains("baseline_external_corrosion_300mm"))
        assertTrue(json.contains("invalid_width_equal_overlap"))
    }
}
