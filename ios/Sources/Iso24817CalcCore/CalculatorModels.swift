import Foundation

public enum MaterialSpecs {
    public static let plyThicknessMm = 0.83
    public static let modulusCircMpa = 45_460.0
    public static let modulusAxialMpa = 43_800.0
    public static let poissonCirc = 0.066
    public static let shearModulusMpa = 2_450.0
    public static let longTermLapShearMpa = 9.62
    public static let longTermStrainLcl = 0.0055
    public static let gammaLclJPerM2 = 250.0
    public static let typeBMinLayers = 3
    public static let typeBMaxLifeYears = 2.0
    public static let glassTransitionTempC = 78.18
    public static let maxTempC = 58.18
    public static let axialCtePpmPerC = 22.81
    public static let steelCtePerC = 12e-6
    public static let qualifiedStitchOverlapMm = 50.0
    public static let epoxyKgPerSquareMeter = 1.2
}

/// Typed form contract. Nil values represent a new, blank calculation.
public struct CalculatorInputs: Codable, Equatable, Sendable {
    public var customer: String?
    public var location: String?
    public var reportNo: String?
    public var odMm: Double?
    public var wallMm: Double?
    public var yieldStrengthMpa: Double?
    public var pressureBar: Double?
    public var temperatureC: Double?
    public var defectType: String?
    public var defectLocation: String?
    public var defectLengthMm: Double?
    public var remainingWallMm: Double?
    public var internalCorrosionRateMmPerYear: Double?
    public var designLifeYears: Double?
    public var designFactor: Double?
    public var installationTemperatureC: Double?
    public var componentType: String?
    public var cyclicDeratingFactor: Double?
    public var axialLoadCase: Int?
    public var clothWidthMm: Double?
    public var forceThreeLayers: Bool

    public init(
        customer: String? = nil,
        location: String? = nil,
        reportNo: String? = nil,
        odMm: Double? = nil,
        wallMm: Double? = nil,
        yieldStrengthMpa: Double? = nil,
        pressureBar: Double? = nil,
        temperatureC: Double? = nil,
        defectType: String? = nil,
        defectLocation: String? = nil,
        defectLengthMm: Double? = nil,
        remainingWallMm: Double? = nil,
        internalCorrosionRateMmPerYear: Double? = nil,
        designLifeYears: Double? = nil,
        designFactor: Double? = nil,
        installationTemperatureC: Double? = nil,
        componentType: String? = nil,
        cyclicDeratingFactor: Double? = nil,
        axialLoadCase: Int? = nil,
        clothWidthMm: Double? = nil,
        forceThreeLayers: Bool = false
    ) {
        self.customer = customer; self.location = location; self.reportNo = reportNo
        self.odMm = odMm; self.wallMm = wallMm; self.yieldStrengthMpa = yieldStrengthMpa
        self.pressureBar = pressureBar; self.temperatureC = temperatureC
        self.defectType = defectType; self.defectLocation = defectLocation
        self.defectLengthMm = defectLengthMm; self.remainingWallMm = remainingWallMm
        self.internalCorrosionRateMmPerYear = internalCorrosionRateMmPerYear
        self.designLifeYears = designLifeYears; self.designFactor = designFactor
        self.installationTemperatureC = installationTemperatureC; self.componentType = componentType
        self.cyclicDeratingFactor = cyclicDeratingFactor; self.axialLoadCase = axialLoadCase
        self.clothWidthMm = clothWidthMm; self.forceThreeLayers = forceThreeLayers
    }

    private enum CodingKeys: String, CodingKey {
        case customer, location, reportNo = "report_no", odMm = "od", wallMm = "wall"
        case yieldStrengthMpa = "yield_strength", pressureBar = "pressure", temperatureC = "temp"
        case defectType = "defect_type", defectLocation = "defect_loc", defectLengthMm = "length"
        case remainingWallMm = "rem_wall", internalCorrosionRateMmPerYear = "internal_corrosion_rate"
        case designLifeYears = "design_life", designFactor = "design_factor"
        case installationTemperatureC = "installation_temp", componentType = "component_type"
        case cyclicDeratingFactor = "cyclic_derating_factor", axialLoadCase = "axial_load_case"
        case clothWidthMm = "cloth_width_mm", forceThreeLayers = "force_3_layers"
    }
}

public struct CalculationError: Equatable, Sendable {
    public let code: String
    public let message: String
    public let field: String?

    public init(code: String, message: String, field: String? = nil) {
        self.code = code; self.message = message; self.field = field
    }
}

public enum CalculationOutcome: Equatable, Sendable, CustomStringConvertible {
    case success(CalculationResult)
    case invalid([CalculationError])

    public var description: String {
        switch self {
        case .success: return "success"
        case let .invalid(errors): return "invalid: " + errors.map(\.message).joined(separator: "; ")
        }
    }
}

public struct B31GDetails: Equatable, Sendable {
    public let method: String; public let dOverT: Double; public let z: Double; public let foliasM: Double
    public let flowStressMpa: Double; public let failureStressMpa: Double; public let failurePressureMpa: Double
    public let safePressureMpa: Double; public let applicable: Bool; public let acceptable: Bool
    public let warnings: [String]
}

public struct TypeBDetails: Equatable, Sendable {
    public let defectSizeUsedMm: Double; public let designLifeYears: Double; public let serviceTempLimitC: Double
    public let fLeak: Double; public let fT2: Double; public let gammaLclJPerM2: Double; public let eAcMpa: Double
    public let maxPressureAsymptoteMpa: Double; public let validityLimitMm: Double; public let withinValidity: Bool
    public let repairableFormula12: Bool; public let formula12ThicknessMm: Double?; public let typeAThicknessMm: Double
}

public struct CalculationResult: Equatable, Sendable {
    public let customer: String; public let location: String; public let reportNo: String
    public let odMm: Double; public let wallMm: Double; public let yieldStrengthMpa: Double
    public let pressureBar: Double; public let temperatureC: Double; public let defectType: String
    public let defectLocation: String; public let remainingWallMm: Double; public let remainingWallEolMm: Double
    public let internalCorrosionRateMmPerYear: Double; public let defectLengthMm: Double; public let wallLossRatio: Double
    public let calcMethodThickness: String; public let calcMethodOverlap: String; public let safetyFactor: Double
    public let temperatureFactor: Double; public let designStrain: Double; public let pressureMpa: Double
    public let steelCapacityMpa: Double; public let compositeDesignPressureMpa: Double; public let requiredThicknessMm: Double
    public let numPlies: Int; public let finalThicknessMm: Double; public let isoLengthMm: Double
    public let overlapLengthMm: Double; public let geometricOverlapMm: Double; public let transferOverlapMm: Double
    public let taperLengthMm: Double; public let numBands: Int; public let procurementLengthMm: Double
    public let clothWidthMm: Double; public let optimizedSquareMeters: Double; public let epoxyKg: Double
    public let isUpgraded: Bool; public let thicknessCheckOk: Bool; public let warnings: [String]
    public let b31g: B31GDetails?; public let typeB: TypeBDetails?
}
