package com.protap.iso24817calc.report

import com.protap.iso24817calc.domain.CalculationResult

object RepairReportDocument {
    fun lines(result: CalculationResult): List<String> = buildList {
        add("PROWRAP COMPOSITE REPAIR REPORT")
        add("Preliminary basis: selected ISO 24817 / ASME PCC-2 concepts")
        add("")
        add("Project: ${result.customer} / ${result.location} / ${result.reportNo}")
        add("Pipe: OD ${result.odMm} mm, wall ${result.wallMm} mm, yield ${result.yieldStrengthMpa} MPa")
        add("Defect: ${result.defectType} / ${result.defectLocation}, length ${result.defectLengthMm} mm")
        add("Remaining wall: ${result.remainingWallMm} mm (end of life ${"%.2f".format(result.remainingWallEolMm)} mm)")
        add("Repair logic: ${result.calcMethodThickness}")
        add("Required plies: ${result.numPlies} (${"%.2f".format(result.finalThicknessMm)} mm)")
        add("ISO repair length: ${"%.0f".format(result.isoLengthMm)} mm")
        add("Procurement: ${result.numBands} bands × ${result.clothWidthMm} mm = ${result.procurementLengthMm} mm")
        add("Fabric: ${"%.2f".format(result.optimizedSquareMeters)} m²; epoxy: ${"%.1f".format(result.epoxyKg)} kg")
        if (result.warnings.isNotEmpty()) {
            add("")
            add("WARNINGS")
            result.warnings.forEach { add("- $it") }
        }
        add("")
        add("Installation checklist")
        add("1. Grit blast to SA 2.5; profile >60 microns.")
        add("2. Apply Prowrap filler to restore OD.")
        add("3. Saturate carbon cloth and apply ${result.numPlies} layers per band.")
        add("4. Wrap ${result.numBands} band(s) of ${result.clothWidthMm} mm cloth.")
        add("5. Verify minimum average Shore D hardness of 75.")
    }
}
