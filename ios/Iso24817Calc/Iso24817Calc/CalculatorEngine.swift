import Foundation

/// Pure offline implementation shared by the iPhone UI and its test harness.
public enum CalculatorEngine {
    private static let pi = Double.pi

    public static func calculate(_ input: CalculatorInputs) -> CalculationOutcome {
        let missing = missingFields(input)
        if !missing.isEmpty {
            return .invalid(missing.map { CalculationError(code: "MISSING_REQUIRED_FIELD", message: $0, field: $0) })
        }

        let od = input.odMm!
        let wall = input.wallMm!
        let yieldStrength = input.yieldStrengthMpa!
        let pressureBar = input.pressureBar!
        let temp = input.temperatureC!
        let length = input.defectLengthMm!
        let remWall = input.remainingWallMm!
        let designLife = input.designLifeYears!
        let designFactor = input.designFactor!
        let installationTemp = input.installationTemperatureC!
        let component = input.componentType!
        let cyclic = input.cyclicDeratingFactor!
        let axialCase = input.axialLoadCase!
        let clothWidth = input.clothWidthMm!
        let defectType = input.defectType!
        let defectLocation = input.defectLocation!
        let corrosionRate = input.internalCorrosionRateMmPerYear ?? 0.0

        var errors: [CalculationError] = []
        if od <= 0 { errors.append(CalculationError(code: "OD_MUST_BE_POSITIVE", message: "Pipe outer diameter must be greater than zero.", field: "Pipe OD [mm]")) }
        if wall <= 0 { errors.append(CalculationError(code: "WALL_MUST_BE_POSITIVE", message: "Nominal wall thickness must be greater than zero.", field: "Nominal Wall [mm]")) }
        if pressureBar < 0 { errors.append(CalculationError(code: "PRESSURE_INVALID", message: "Design pressure cannot be negative.", field: "Design Pressure [bar]")) }
        if temp > MaterialSpecs.maxTempC { errors.append(CalculationError(code: "TEMPERATURE_EXCEEDS_LIMIT", message: "Operating temperature (\(fmt(temp)) degC) exceeds Prowrap limit of \(fmt(MaterialSpecs.maxTempC)) degC.", field: "Op. Temperature [°C]")) }
        if length <= 0 { errors.append(CalculationError(code: "DEFECT_LENGTH_INVALID", message: "Defect length must be greater than zero.", field: "Defect Length [mm]")) }
        if remWall < 0 { errors.append(CalculationError(code: "REMAINING_WALL_INVALID", message: "Remaining wall thickness cannot be negative.", field: "Remaining Wall [mm]")) }
        if wall > 0 && remWall > wall { errors.append(CalculationError(code: "REMAINING_WALL_EXCEEDS_NOMINAL", message: "Remaining wall thickness cannot be greater than nominal wall thickness.", field: "Remaining Wall [mm]")) }
        if yieldStrength <= 0 { errors.append(CalculationError(code: "YIELD_INVALID", message: "Pipe yield strength must be greater than zero.", field: "Pipe Yield [MPa]")) }
        if designFactor <= 0 || designFactor > 1 { errors.append(CalculationError(code: "DESIGN_FACTOR_INVALID", message: "Design factor must be greater than zero and less than or equal to 1.", field: "Design Factor (f)")) }
        if designLife < 1 { errors.append(CalculationError(code: "DESIGN_LIFE_INVALID", message: "Design life must be at least 1 year.", field: "Design Life [years]")) }
        if corrosionRate < 0 { errors.append(CalculationError(code: "CORROSION_RATE_INVALID", message: "Internal corrosion rate cannot be negative.", field: "Internal Corrosion Rate [mm/yr]")) }
        if cyclic <= 0 || cyclic > 1 { errors.append(CalculationError(code: "CYCLIC_FACTOR_INVALID", message: "Cyclic derating factor must be greater than zero and less than or equal to 1.", field: "Cyclic derating factor")) }
        if clothWidth <= MaterialSpecs.qualifiedStitchOverlapMm { errors.append(CalculationError(code: "CLOTH_WIDTH_TOO_NARROW", message: "Prowrap CF cloth width must exceed the 50 mm stitch overlap.", field: "Prowrap CF cloth band width [mm]")) }
        if componentFactor(component) == nil { errors.append(CalculationError(code: "COMPONENT_TYPE_INVALID", message: "Unknown component type. Use Straight, Bend, Tee, Flange, or Reducer.", field: "Component type")) }
        if !(0...1).contains(axialCase) { errors.append(CalculationError(code: "AXIAL_LOAD_CASE_INVALID", message: "Axial load case must be 0 or 1.", field: "Axial load case")) }
        if !errors.isEmpty { return .invalid(errors) }

        let wallLossRatio = (wall - remWall) / wall
        let remEol = defectType == "Corrosion" && defectLocation == "Internal" ? max(remWall - corrosionRate * designLife, 0.0) : remWall
        let noSubstrate = remEol < 1.0
        let isTypeB = defectLocation == "Internal" || defectType == "Leak" || defectType == "Crack" || noSubstrate
        let calcMethod = isTypeB ? "Type B (Total Replacement)" : (defectType == "Dent" ? "Type A (Dent Reinforcement)" : "Type A (Load Sharing)")
        let overlapMethod = isTypeB ? "Type B (Shear Controlled)" : "Type A (Geometry Controlled)"
        let safetyFactor = 1.0 / designFactor
        let fPerf = 0.76 * pow(10.0, -0.00273 * designLife)
        let delta = MaterialSpecs.maxTempC - temp
        let tempFactor = 0.0000625 * pow(delta, 2) + 0.00125 * delta + 0.7
        let designStrain = cyclic * fPerf * tempFactor * MaterialSpecs.longTermStrainLcl
        let pressureMpa = pressureBar * 0.1

        let b31g: B31GDetails? = defectType == "Corrosion" && !noSubstrate
            ? assessB31g(od: od, wall: wall, depth: wall - remEol, length: length, smys: yieldStrength, sf: max(safetyFactor, 1.25), operatingPressure: pressureMpa)
            : nil
        let steelCapacity = calcMethod.contains("Type A") && b31g?.applicable == true ? b31g!.safePressureMpa : 0.0
        let compositePressure = calcMethod.contains("Type A") && steelCapacity > 0 ? max(0.0, pressureMpa - steelCapacity) : pressureMpa

        var typeADesignResult: TypeADesign?
        var required = calcMethod.contains("Type A")
            ? 0.0
            : (compositePressure > 0 ? compositePressure * od / (2.0 * MaterialSpecs.modulusCircMpa * designStrain) : 0.0)
        if calcMethod.contains("Type A") {
            let design = typeADesign(od: od, pressure: pressureMpa, substrate: steelCapacity, designTemp: temp, installTemp: installationTemp, life: designLife, component: component, cyclic: cyclic, axialCase: axialCase)
            typeADesignResult = design
            required = design.finalThicknessMm
        }

        var typeB: TypeBDetails?
        if isTypeB && pressureMpa > 0 {
            let cappedLife = min(designLife, MaterialSpecs.typeBMaxLifeYears)
            let typeBCalc = typeBThickness(pressure: pressureMpa, od: od, wall: wall, defectLength: length, temp: temp, life: cappedLife)
            typeB = TypeBDetails(defectSizeUsedMm: typeBCalc.defectSizeUsedMm, designLifeYears: typeBCalc.designLifeYears, serviceTempLimitC: typeBCalc.serviceTempLimitC, fLeak: typeBCalc.fLeak, fT2: typeBCalc.fT2, gammaLclJPerM2: typeBCalc.gammaLclJPerM2, eAcMpa: typeBCalc.eAcMpa, maxPressureAsymptoteMpa: typeBCalc.maxPressureAsymptoteMpa, validityLimitMm: typeBCalc.validityLimitMm, withinValidity: typeBCalc.withinValidity, repairableFormula12: typeBCalc.repairableFormula12, formula12ThicknessMm: typeBCalc.formula12ThicknessMm, typeAThicknessMm: required)
            if let t = typeBCalc.formula12ThicknessMm { required = max(required, t) }
        }

        var plies = Int(ceil(required / MaterialSpecs.plyThicknessMm))
        let minIso = Int(ceil(2.0 / MaterialSpecs.plyThicknessMm))
        let minPlies = isTypeB ? max(MaterialSpecs.typeBMinLayers, minIso) : minIso
        plies = max(plies, minPlies)
        let upgraded = input.forceThreeLayers && plies < 3
        if upgraded { plies = 3 }
        let finalThickness = Double(plies) * MaterialSpecs.plyThicknessMm

        let geometricOverlap = 2.0 * sqrt(od * wall)
        let transferOverlap = typeADesignResult?.transferOverlapMm ?? (3.0 * MaterialSpecs.modulusAxialMpa * designStrain * finalThickness / MaterialSpecs.longTermLapShearMpa)
        let overlap = max(50.0, max(geometricOverlap, transferOverlap))
        let taper = 5.0 * finalThickness
        let isoLength = length + 2.0 * overlap + 2.0 * taper
        let bands = Int(ceil((isoLength - clothWidth) / (clothWidth - MaterialSpecs.qualifiedStitchOverlapMm))) + 1
        let safeBands = isoLength <= clothWidth ? 1 : bands
        let procurementLength = Double(safeBands) * clothWidth
        let circumferenceM = pi * od / 1000.0
        let optimizedSqm = procurementLength / 1000.0 * circumferenceM * Double(plies)
        let epoxy = optimizedSqm * MaterialSpecs.epoxyKgPerSquareMeter
        let warnings = buildWarnings(isTypeB: isTypeB, typeB: typeB, requestedLife: designLife, temp: temp, defectType: defectType, defectLocation: defectLocation, corrosionRate: corrosionRate, remWall: remWall, remEol: remEol, axialCase: axialCase, b31g: b31g, finalThickness: finalThickness, od: od)

        return .success(CalculationResult(
            customer: input.customer!, location: input.location!, reportNo: input.reportNo!, odMm: od, wallMm: wall,
            yieldStrengthMpa: yieldStrength, pressureBar: pressureBar, temperatureC: temp, defectType: defectType,
            defectLocation: defectLocation, remainingWallMm: remWall, remainingWallEolMm: remEol,
            internalCorrosionRateMmPerYear: corrosionRate, defectLengthMm: length, wallLossRatio: wallLossRatio,
            calcMethodThickness: calcMethod, calcMethodOverlap: overlapMethod, safetyFactor: safetyFactor,
            temperatureFactor: tempFactor, designStrain: designStrain, pressureMpa: pressureMpa,
            steelCapacityMpa: steelCapacity, compositeDesignPressureMpa: compositePressure, requiredThicknessMm: required,
            numPlies: plies, finalThicknessMm: finalThickness, isoLengthMm: isoLength, overlapLengthMm: overlap,
            geometricOverlapMm: geometricOverlap, transferOverlapMm: transferOverlap, taperLengthMm: taper,
            numBands: safeBands, procurementLengthMm: procurementLength, clothWidthMm: clothWidth,
            optimizedSquareMeters: optimizedSqm, epoxyKg: epoxy, isUpgraded: upgraded,
            thicknessCheckOk: finalThickness < od / 12.0, warnings: warnings, b31g: b31g, typeB: typeB
        ))
    }

    private static func missingFields(_ i: CalculatorInputs) -> [String] {
        var missing: [String] = []
        func req(_ value: Any?, _ label: String) {
            if value == nil || (value as? String)?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == true { missing.append(label) }
        }
        req(i.customer, "Customer"); req(i.location, "Location"); req(i.reportNo, "Report No")
        req(i.odMm, "Pipe OD [mm]"); req(i.wallMm, "Nominal Wall [mm]"); req(i.yieldStrengthMpa, "Pipe Yield [MPa]")
        req(i.pressureBar, "Design Pressure [bar]"); req(i.temperatureC, "Op. Temperature [°C]"); req(i.defectType, "Mechanism")
        req(i.defectLocation, "Location"); req(i.defectLengthMm, "Defect Length [mm]"); req(i.remainingWallMm, "Remaining Wall [mm]")
        req(i.designLifeYears, "Design Life [years]"); req(i.designFactor, "Design Factor (f)"); req(i.installationTemperatureC, "Installation temperature [°C]")
        req(i.componentType, "Component type"); req(i.cyclicDeratingFactor, "Cyclic derating factor"); req(i.axialLoadCase, "Axial load case")
        req(i.clothWidthMm, "Prowrap CF cloth band width [mm]")
        if i.defectType == "Corrosion" && i.defectLocation == "Internal" { req(i.internalCorrosionRateMmPerYear, "Internal Corrosion Rate [mm/yr]") }
        return missing
    }

    private struct TypeADesign { let finalThicknessMm: Double; let transferOverlapMm: Double }

    private static func typeADesign(od: Double, pressure: Double, substrate: Double, designTemp: Double, installTemp: Double, life: Double, component: String, cyclic: Double, axialCase: Int) -> TypeADesign {
        let fth = componentFactor(component)!
        let fPerf = 0.76 * pow(10.0, -0.00273 * life)
        let d = MaterialSpecs.maxTempC - designTemp
        let ft = 0.0000625 * pow(d, 2) + 0.00125 * d + 0.7
        let epsC = cyclic * fPerf * ft * MaterialSpecs.longTermStrainLcl
        let epsA0 = MaterialSpecs.modulusAxialMpa > 0.5 * MaterialSpecs.modulusCircMpa ? 0.003061 * pow(10.0, -0.0044 * life) : 0.001
        let dt = designTemp - installTemp
        let epsA = cyclic * (ft * epsA0 - abs(dt * (MaterialSpecs.steelCtePerC - MaterialSpecs.axialCtePpmPerC * 1e-6)))
        let feq = axialCase == 1 ? pressure * pi * pow(od, 2) / 4.0 : 0.0
        let driving = pressure * od / 2.0 + MaterialSpecs.poissonCirc * feq / (pi * od)
        let resisting = substrate * od / 2.0
        let tMinC = max(0.0, (driving - resisting) / (MaterialSpecs.modulusCircMpa * epsC))
        let tMinA = max(0.0, (feq / (pi * od * MaterialSpecs.modulusAxialMpa) - pressure * od * MaterialSpecs.poissonCirc / (2.0 * MaterialSpecs.modulusCircMpa)) / epsA)
        let base = max(tMinC, tMinA)
        var t = max(base * fth, max(2.0, 2.0 * MaterialSpecs.plyThicknessMm))
        if component.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() == "TEE" { t = max(t, pressure * (od + od) / (2.0 * MaterialSpecs.modulusCircMpa * epsC)) }
        return TypeADesign(finalThicknessMm: t, transferOverlapMm: 3.0 * MaterialSpecs.modulusAxialMpa * epsA * base / MaterialSpecs.longTermLapShearMpa)
    }

    private static func typeBThickness(pressure: Double, od: Double, wall: Double, defectLength: Double, temp: Double, life: Double) -> TypeBDetails {
        let d = max(defectLength, 15.0)
        let eAc = sqrt(MaterialSpecs.modulusAxialMpa * MaterialSpecs.modulusCircMpa)
        let fLeak = 0.666 * pow(10.0, -0.01584 * (life - 1.0))
        let limitTemp = life > 2.0 ? MaterialSpecs.glassTransitionTempTempMinus30 : MaterialSpecs.maxTempC
        let dt = limitTemp - temp
        let ft2 = 0.0000625 * pow(dt, 2) + 0.00125 * dt + 0.7
        func allowable(_ t: Double) -> Double {
            let x = ((1.0 - pow(MaterialSpecs.poissonCirc, 2)) / eAc) * (3.0 * pow(d, 4) / (512.0 * pow(t, 3)) + d / pi) + 3.0 * pow(d, 2) / (64.0 * MaterialSpecs.shearModulusMpa * t)
            return ft2 * fLeak * sqrt(0.001 * MaterialSpecs.gammaLclJPerM2 / x)
        }
        let xAsym = ((1.0 - pow(MaterialSpecs.poissonCirc, 2)) / eAc) * (d / pi)
        let pMax = ft2 * fLeak * sqrt(0.001 * MaterialSpecs.gammaLclJPerM2 / xAsym)
        let validLimit = 6.0 * sqrt(od * wall)
        if pressure >= 0.999 * pMax {
            return TypeBDetails(defectSizeUsedMm: d, designLifeYears: life, serviceTempLimitC: limitTemp, fLeak: fLeak, fT2: ft2, gammaLclJPerM2: MaterialSpecs.gammaLclJPerM2, eAcMpa: eAc, maxPressureAsymptoteMpa: pMax, validityLimitMm: validLimit, withinValidity: d <= validLimit, repairableFormula12: false, formula12ThicknessMm: nil, typeAThicknessMm: 0.0)
        }
        var lower = 0.01; var upper = 1.0
        while allowable(upper) < pressure { upper *= 2.0 }
        for _ in 0..<200 {
            let mid = (lower + upper) / 2.0
            if allowable(mid) < pressure { lower = mid } else { upper = mid }
        }
        return TypeBDetails(defectSizeUsedMm: d, designLifeYears: life, serviceTempLimitC: limitTemp, fLeak: fLeak, fT2: ft2, gammaLclJPerM2: MaterialSpecs.gammaLclJPerM2, eAcMpa: eAc, maxPressureAsymptoteMpa: pMax, validityLimitMm: validLimit, withinValidity: d <= validLimit, repairableFormula12: true, formula12ThicknessMm: upper, typeAThicknessMm: 0.0)
    }

    private static func assessB31g(od: Double, wall: Double, depth: Double, length: Double, smys: Double, sf: Double, operatingPressure: Double) -> B31GDetails {
        var warnings: [String] = []; let dt = depth / wall; var applicable = true
        if dt > 0.8 { applicable = false; warnings.append("d/t > 0.80: beyond B31G applicability - repair or Level 3 (API 579) required. No substrate credit taken.") }
        if dt <= 0.10 { warnings.append("d/t <= 0.10: metal loss not limited as to length (section 3(a)).") }
        let z = length * length / (od * wall); let flow = smys + 69.0
        let m = z <= 50.0 ? sqrt(1.0 + 0.6275 * z - 0.003375 * pow(z, 2)) : 0.032 * z + 3.3
        let failureStress = flow * (1.0 - 0.85 * dt) / (1.0 - 0.85 * dt / m)
        let pf = 2.0 * failureStress * wall / od; let ps = pf / sf
        return B31GDetails(method: "modified", dOverT: dt, z: z, foliasM: m, flowStressMpa: flow, failureStressMpa: failureStress, failurePressureMpa: pf, safePressureMpa: ps, applicable: applicable, acceptable: failureStress >= sf * (operatingPressure * od / (2.0 * wall)) - 1e-9, warnings: warnings)
    }

    private static func componentFactor(_ component: String) -> Double? {
        switch component.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() {
        case "", "STRAIGHT", "PIPE", "STRAIGHT PIPE": return 1.0
        case "BEND": return 1.2
        case "TEE": return 2.0
        case "FLANGE", "REDUCER": return 1.1
        default: return nil
        }
    }

    private static func buildWarnings(isTypeB: Bool, typeB: TypeBDetails?, requestedLife: Double, temp: Double, defectType: String, defectLocation: String, corrosionRate: Double, remWall: Double, remEol: Double, axialCase: Int, b31g: B31GDetails?, finalThickness: Double, od: Double) -> [String] {
        var out: [String] = []
        if let typeB, !typeB.repairableFormula12 { out.append("NOT REPAIRABLE PER ISO 24817 FORMULA 12: maximum achievable pressure is \(fmt(typeB.maxPressureAsymptoteMpa)) MPa, below design pressure.") }
        if isTypeB, let typeB {
            if requestedLife > typeB.designLifeYears { out.append("Type B service life is capped at \(fmt(typeB.designLifeYears)) years for PRW110 (requested: \(fmt(requestedLife))). The repair must be inspected and revalidated or replaced.") }
            if temp > typeB.serviceTempLimitC { out.append("Design temperature \(fmt(temp)) degC exceeds the Type B upper service limit of \(fmt(typeB.serviceTempLimitC)) degC.") }
            out.append("Type B design assumes a circular/near-circular defect of size \(fmt(typeB.defectSizeUsedMm)) mm at END of the design life. Annex F impact-qualified minimum of \(MaterialSpecs.typeBMinLayers) layers applied.")
            if !typeB.withinValidity { out.append("Formula 12 validity exceeded: defect size \(fmt(typeB.defectSizeUsedMm)) mm > 6*sqrt(D*t) = \(fmt(typeB.validityLimitMm)) mm.") }
        }
        if let b31g { out.append(contentsOf: b31g.warnings.map { "B31G: \($0)" }) }
        if defectType == "Corrosion" && defectLocation == "Internal" {
            out.append(corrosionRate > 0 ? "Internal corrosion projected at \(fmt(corrosionRate)) mm/yr: remaining wall \(fmt(remWall)) mm now -> \(fmt(remEol)) mm at end of \(fmt(requestedLife))-year design life." : "Internal corrosion with corrosion rate = 0 mm/yr: enter a corrosion rate so remaining wall can be projected to end of design life.")
        }
        if isTypeB && axialCase == 1 { out.append("Axial load case 1 selected with a Type B defect: engineered assessment of the axial load path is required.") }
        if finalThickness >= od / 12.0 { out.append("Repair thickness exceeds D/12: thin-wall design formulae are not valid for this repair.") }
        return out
    }

    private static func fmt(_ value: Double) -> String { String(format: "%.2f", value) }
}

private extension MaterialSpecs {
    static let glassTransitionTempTempMinus30 = glassTransitionTempC - 30.0
}
