package com.protap.iso24817calc.domain

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Pure offline implementation of the current Python calculation route. */
object CalculatorEngine {
    private const val pi = Math.PI

    fun calculate(input: CalculatorInputs): CalculationOutcome {
        val missing = missingFields(input)
        if (missing.isNotEmpty()) {
            return CalculationOutcome.Invalid(missing.map { CalculationError("MISSING_REQUIRED_FIELD", it, it) })
        }

        val od = input.odMm!!
        val wall = input.wallMm!!
        val yieldStrength = input.yieldStrengthMpa!!
        val pressureBar = input.pressureBar!!
        val temp = input.temperatureC!!
        val length = input.defectLengthMm!!
        val remWall = input.remainingWallMm!!
        val designLife = input.designLifeYears!!
        val designFactor = input.designFactor!!
        val installationTemp = input.installationTemperatureC!!
        val component = input.componentType!!
        val cyclic = input.cyclicDeratingFactor!!
        val axialCase = input.axialLoadCase!!
        val clothWidth = input.clothWidthMm!!
        val defectType = input.defectType!!
        val defectLocation = input.defectLocation!!
        val corrosionRate = input.internalCorrosionRateMmPerYear ?: 0.0

        val errors = mutableListOf<CalculationError>()
        if (od <= 0) errors += CalculationError("OD_MUST_BE_POSITIVE", "Pipe outer diameter must be greater than zero.", "Pipe OD [mm]")
        if (wall <= 0) errors += CalculationError("WALL_MUST_BE_POSITIVE", "Nominal wall thickness must be greater than zero.", "Nominal Wall [mm]")
        if (pressureBar < 0) errors += CalculationError("PRESSURE_INVALID", "Design pressure cannot be negative.", "Design Pressure [bar]")
        if (temp > MaterialSpecs.maxTempC) errors += CalculationError("TEMPERATURE_EXCEEDS_LIMIT", "Operating temperature (${fmt(temp)} degC) exceeds Prowrap limit of ${fmt(MaterialSpecs.maxTempC)} degC.", "Op. Temperature [°C]")
        if (length <= 0) errors += CalculationError("DEFECT_LENGTH_INVALID", "Defect length must be greater than zero.", "Defect Length [mm]")
        if (remWall < 0) errors += CalculationError("REMAINING_WALL_INVALID", "Remaining wall thickness cannot be negative.", "Remaining Wall [mm]")
        if (wall > 0 && remWall > wall) errors += CalculationError("REMAINING_WALL_EXCEEDS_NOMINAL", "Remaining wall thickness cannot be greater than nominal wall thickness.", "Remaining Wall [mm]")
        if (yieldStrength <= 0) errors += CalculationError("YIELD_INVALID", "Pipe yield strength must be greater than zero.", "Pipe Yield [MPa]")
        if (designFactor <= 0 || designFactor > 1) errors += CalculationError("DESIGN_FACTOR_INVALID", "Design factor must be greater than zero and less than or equal to 1.", "Design Factor (f)")
        if (designLife < 1) errors += CalculationError("DESIGN_LIFE_INVALID", "Design life must be at least 1 year.", "Design Life [years]")
        if (corrosionRate < 0) errors += CalculationError("CORROSION_RATE_INVALID", "Internal corrosion rate cannot be negative.", "Internal Corrosion Rate [mm/yr]")
        if (cyclic <= 0 || cyclic > 1) errors += CalculationError("CYCLIC_FACTOR_INVALID", "Cyclic derating factor must be greater than zero and less than or equal to 1.", "Cyclic derating factor")
        if (clothWidth <= MaterialSpecs.qualifiedStitchOverlapMm) errors += CalculationError("CLOTH_WIDTH_TOO_NARROW", "Prowrap CF cloth width must exceed the 50 mm stitch overlap.", "Prowrap CF cloth band width [mm]")
        if (componentFactor(component) == null) errors += CalculationError("COMPONENT_TYPE_INVALID", "Unknown component type. Use Straight, Bend, Tee, Flange, or Reducer.", "Component type")
        if (axialCase !in 0..1) errors += CalculationError("AXIAL_LOAD_CASE_INVALID", "Axial load case must be 0 or 1.", "Axial load case")
        if (errors.isNotEmpty()) return CalculationOutcome.Invalid(errors)

        val wallLossRatio = (wall - remWall) / wall
        val remEol = if (defectType == "Corrosion" && defectLocation == "Internal") max(remWall - corrosionRate * designLife, 0.0) else remWall
        val noSubstrate = remEol < 1.0
        val isTypeB = defectLocation == "Internal" || defectType == "Leak" || defectType == "Crack" || noSubstrate
        val calcMethod = when {
            isTypeB -> "Type B (Total Replacement)"
            defectType == "Dent" -> "Type A (Dent Reinforcement)"
            else -> "Type A (Load Sharing)"
        }
        val overlapMethod = if (isTypeB) "Type B (Shear Controlled)" else "Type A (Geometry Controlled)"
        val safetyFactor = 1.0 / designFactor
        val fPerf = 0.76 * 10.0.pow(-0.00273 * designLife)
        val delta = MaterialSpecs.maxTempC - temp
        val tempFactor = 0.0000625 * delta.pow(2) + 0.00125 * delta + 0.7
        val designStrain = cyclic * fPerf * tempFactor * MaterialSpecs.longTermStrainLcl
        val pressureMpa = pressureBar * 0.1

        val b31g = if (defectType == "Corrosion" && !noSubstrate) {
            assessB31g(od, wall, wall - remEol, length, yieldStrength, max(safetyFactor, 1.25), pressureMpa)
        } else null
        val steelCapacity = if (calcMethod.contains("Type A") && b31g?.applicable == true) b31g.safePressureMpa else 0.0
        val compositePressure = if (calcMethod.contains("Type A") && steelCapacity > 0) max(0.0, pressureMpa - steelCapacity) else pressureMpa

        var typeA: TypeADesign? = null
        var required = if (calcMethod.contains("Type A")) {
            typeA = typeADesign(od, pressureMpa, steelCapacity, temp, installationTemp, designLife, component, cyclic, axialCase)
            typeA!!.finalThicknessMm
        } else if (compositePressure > 0) {
            compositePressure * od / (2.0 * MaterialSpecs.modulusCircMpa * designStrain)
        } else 0.0

        var typeB: TypeBDetails? = null
        if (isTypeB && pressureMpa > 0) {
            val cappedLife = min(designLife, MaterialSpecs.typeBMaxLifeYears)
            val typeBCalc = typeBThickness(pressureMpa, od, wall, length, temp, cappedLife)
            typeB = typeBCalc.copy(typeAThicknessMm = required)
            typeBCalc.formula12ThicknessMm?.let { required = max(required, it) }
        }

        var plies = ceil(required / MaterialSpecs.plyThicknessMm).toInt()
        val minIso = ceil(2.0 / MaterialSpecs.plyThicknessMm).toInt()
        val minPlies = if (isTypeB) max(MaterialSpecs.typeBMinLayers, minIso) else minIso
        plies = max(plies, minPlies)
        val upgraded = input.forceThreeLayers && plies < 3
        if (upgraded) plies = 3
        val finalThickness = plies * MaterialSpecs.plyThicknessMm

        val geometricOverlap = 2.0 * sqrt(od * wall)
        val transferOverlap = if (typeA != null) typeA!!.transferOverlapMm else (3.0 * MaterialSpecs.modulusAxialMpa * designStrain * finalThickness / MaterialSpecs.longTermLapShearMpa)
        val overlap = max(50.0, max(geometricOverlap, transferOverlap))
        val taper = 5.0 * finalThickness
        val isoLength = length + 2.0 * overlap + 2.0 * taper
        val bands = ceil((isoLength - clothWidth) / (clothWidth - MaterialSpecs.qualifiedStitchOverlapMm)).toInt() + 1
        val safeBands = if (isoLength <= clothWidth) 1 else bands
        val procurementLength = safeBands * clothWidth
        val circumferenceM = pi * od / 1000.0
        val optimizedSqm = procurementLength / 1000.0 * circumferenceM * plies
        val epoxy = optimizedSqm * MaterialSpecs.epoxyKgPerSquareMeter

        val warnings = buildWarnings(isTypeB, typeB, designLife, temp, defectType, defectLocation, corrosionRate, remWall, remEol, axialCase, b31g, finalThickness, od)
        return CalculationOutcome.Success(CalculationResult(
            input.customer!!, input.location!!, input.reportNo!!, od, wall, yieldStrength, pressureBar, temp,
            defectType, defectLocation, remWall, remEol, corrosionRate, length, wallLossRatio, calcMethod, overlapMethod,
            safetyFactor, tempFactor, designStrain, pressureMpa, steelCapacity, compositePressure, required, plies,
            finalThickness, isoLength, overlap, geometricOverlap, transferOverlap, taper, safeBands, procurementLength,
            clothWidth, optimizedSqm, epoxy, upgraded, finalThickness < od / 12.0, warnings, b31g, typeB,
        ))
    }

    private fun missingFields(i: CalculatorInputs): List<String> {
        val missing = mutableListOf<String>()
        fun req(value: Any?, label: String) { if (value == null || (value is String && value.isBlank())) missing += label }
        req(i.customer, "Customer"); req(i.location, "Location"); req(i.reportNo, "Report No")
        req(i.odMm, "Pipe OD [mm]"); req(i.wallMm, "Nominal Wall [mm]"); req(i.yieldStrengthMpa, "Pipe Yield [MPa]")
        req(i.pressureBar, "Design Pressure [bar]"); req(i.temperatureC, "Op. Temperature [°C]"); req(i.defectType, "Mechanism")
        req(i.defectLocation, "Location"); req(i.defectLengthMm, "Defect Length [mm]"); req(i.remainingWallMm, "Remaining Wall [mm]")
        req(i.designLifeYears, "Design Life [years]"); req(i.designFactor, "Design Factor (f)"); req(i.installationTemperatureC, "Installation temperature [°C]")
        req(i.componentType, "Component type"); req(i.cyclicDeratingFactor, "Cyclic derating factor"); req(i.axialLoadCase, "Axial load case")
        req(i.clothWidthMm, "Prowrap CF cloth band width [mm]")
        if (i.defectType == "Corrosion" && i.defectLocation == "Internal") req(i.internalCorrosionRateMmPerYear, "Internal Corrosion Rate [mm/yr]")
        return missing
    }

    private data class TypeADesign(val finalThicknessMm: Double, val transferOverlapMm: Double)

    private fun typeADesign(od: Double, pressure: Double, substrate: Double, designTemp: Double, installTemp: Double, life: Double, component: String, cyclic: Double, axialCase: Int): TypeADesign {
        val fth = componentFactor(component)!!
        val fPerf = 0.76 * 10.0.pow(-0.00273 * life)
        val d = MaterialSpecs.maxTempC - designTemp
        val ft = 0.0000625 * d.pow(2) + 0.00125 * d + 0.7
        val epsC = cyclic * fPerf * ft * MaterialSpecs.longTermStrainLcl
        val epsA0 = if (MaterialSpecs.modulusAxialMpa > 0.5 * MaterialSpecs.modulusCircMpa) 0.003061 * 10.0.pow(-0.0044 * life) else 0.001
        val dt = designTemp - installTemp
        val epsA = cyclic * (ft * epsA0 - kotlin.math.abs(dt * (MaterialSpecs.steelCtePerC - MaterialSpecs.axialCtePpmPerC * 1e-6)))
        val feq = if (axialCase == 1) pressure * pi * od.pow(2) / 4.0 else 0.0
        val driving = pressure * od / 2.0 + MaterialSpecs.poissonCirc * feq / (pi * od)
        val resisting = substrate * od / 2.0
        val tMinC = max(0.0, (driving - resisting) / (MaterialSpecs.modulusCircMpa * epsC))
        val tMinA = max(0.0, (feq / (pi * od * MaterialSpecs.modulusAxialMpa) - pressure * od * MaterialSpecs.poissonCirc / (2.0 * MaterialSpecs.modulusCircMpa)) / epsA)
        val base = max(tMinC, tMinA)
        var t = max(base * fth, max(2.0, 2.0 * MaterialSpecs.plyThicknessMm))
        if (component.trim().uppercase() == "TEE") t = max(t, pressure * (od + od) / (2.0 * MaterialSpecs.modulusCircMpa * epsC))
        return TypeADesign(t, 3.0 * MaterialSpecs.modulusAxialMpa * epsA * base / MaterialSpecs.longTermLapShearMpa)
    }

    private fun typeBThickness(pressure: Double, od: Double, wall: Double, defectLength: Double, temp: Double, life: Double): TypeBDetails {
        val d = max(defectLength, 15.0)
        val eAc = sqrt(MaterialSpecs.modulusAxialMpa * MaterialSpecs.modulusCircMpa)
        val fLeak = 0.666 * 10.0.pow(-0.01584 * (life - 1.0))
        val limitTemp = if (life > 2.0) MaterialSpecs.glassTransitionTempC - 30.0 else MaterialSpecs.maxTempC
        val dt = limitTemp - temp
        val ft2 = 0.0000625 * dt.pow(2) + 0.00125 * dt + 0.7
        fun allowable(t: Double): Double {
            val x = ((1.0 - MaterialSpecs.poissonCirc.pow(2)) / eAc) * (3.0 * d.pow(4) / (512.0 * t.pow(3)) + d / pi) + 3.0 * d.pow(2) / (64.0 * MaterialSpecs.shearModulusMpa * t)
            return ft2 * fLeak * sqrt(0.001 * MaterialSpecs.gammaLclJPerM2 / x)
        }
        val xAsym = ((1.0 - MaterialSpecs.poissonCirc.pow(2)) / eAc) * (d / pi)
        val pMax = ft2 * fLeak * sqrt(0.001 * MaterialSpecs.gammaLclJPerM2 / xAsym)
        val validLimit = 6.0 * sqrt(od * wall)
        if (pressure >= 0.999 * pMax) return TypeBDetails(d, life, limitTemp, fLeak, ft2, MaterialSpecs.gammaLclJPerM2, eAc, pMax, validLimit, d <= validLimit, false, null, 0.0)
        var lower = 0.01; var upper = 1.0
        while (allowable(upper) < pressure) upper *= 2.0
        repeat(200) { val mid = (lower + upper) / 2.0; if (allowable(mid) < pressure) lower = mid else upper = mid }
        return TypeBDetails(d, life, limitTemp, fLeak, ft2, MaterialSpecs.gammaLclJPerM2, eAc, pMax, validLimit, d <= validLimit, true, upper, 0.0)
    }

    private fun assessB31g(od: Double, wall: Double, depth: Double, length: Double, smys: Double, sf: Double, operatingPressure: Double): B31GDetails {
        val warnings = mutableListOf<String>(); val dt = depth / wall; var applicable = true
        if (dt > 0.8) { applicable = false; warnings += "d/t > 0.80: beyond B31G applicability - repair or Level 3 (API 579) required. No substrate credit taken." }
        if (dt <= 0.10) warnings += "d/t <= 0.10: metal loss not limited as to length (section 3(a))."
        val z = length * length / (od * wall); val flow = smys + 69.0
        val m = if (z <= 50.0) sqrt(1.0 + 0.6275 * z - 0.003375 * z * z) else 0.032 * z + 3.3
        val failureStress = flow * (1.0 - 0.85 * dt) / (1.0 - 0.85 * dt / m)
        val pf = 2.0 * failureStress * wall / od; val ps = pf / sf
        return B31GDetails("modified", dt, z, m, flow, failureStress, pf, ps, applicable, failureStress >= sf * (operatingPressure * od / (2.0 * wall)) - 1e-9, warnings)
    }

    private fun componentFactor(component: String): Double? = when (component.trim().uppercase()) {
        "", "STRAIGHT", "PIPE", "STRAIGHT PIPE" -> 1.0
        "BEND" -> 1.2
        "TEE" -> 2.0
        "FLANGE", "REDUCER" -> 1.1
        else -> null
    }

    private fun buildWarnings(isTypeB: Boolean, typeB: TypeBDetails?, requestedLife: Double, temp: Double, defectType: String, defectLocation: String, rate: Double, remWall: Double, remEol: Double, axialCase: Int, b31g: B31GDetails?, finalThickness: Double, od: Double): List<String> {
        val out = mutableListOf<String>()
        if (typeB != null && !typeB.repairableFormula12) out += "NOT REPAIRABLE PER ISO 24817 FORMULA 12: maximum achievable pressure is ${fmt(typeB.maxPressureAsymptoteMpa)} MPa, below design pressure."
        if (isTypeB && typeB != null) {
            if (requestedLife > typeB.designLifeYears) out += "Type B service life is capped at ${fmt(typeB.designLifeYears)} years for PRW110 (requested: ${fmt(requestedLife)}). The repair must be inspected and revalidated or replaced."
            if (temp > typeB.serviceTempLimitC) out += "Design temperature ${fmt(temp)} degC exceeds the Type B upper service limit of ${fmt(typeB.serviceTempLimitC)} degC."
            out += "Type B design assumes a circular/near-circular defect of size ${fmt(typeB.defectSizeUsedMm)} mm at END of the design life. Annex F impact-qualified minimum of ${MaterialSpecs.typeBMinLayers} layers applied."
            if (!typeB.withinValidity) out += "Formula 12 validity exceeded: defect size ${fmt(typeB.defectSizeUsedMm)} mm > 6*sqrt(D*t) = ${fmt(typeB.validityLimitMm)} mm."
        }
        b31g?.warnings?.let { out += it.map { w -> "B31G: $w" } }
        if (defectType == "Corrosion" && defectLocation == "Internal") out += if (rate > 0) "Internal corrosion projected at ${fmt(rate)} mm/yr: remaining wall ${fmt(remWall)} mm now -> ${fmt(remEol)} mm at end of ${fmt(requestedLife)}-year design life." else "Internal corrosion with corrosion rate = 0 mm/yr: enter a corrosion rate so remaining wall can be projected to end of design life."
        if (isTypeB && axialCase == 1) out += "Axial load case 1 selected with a Type B defect: engineered assessment of the axial load path is required."
        if (finalThickness >= od / 12.0) out += "Repair thickness exceeds D/12: thin-wall design formulae are not valid for this repair."
        return out
    }

    private fun fmt(value: Double): String = "%.2f".format(java.util.Locale.US, value)
}
