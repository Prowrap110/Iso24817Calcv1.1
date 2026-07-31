import XCTest
@testable import Iso24817CalcCore

final class CalculatorEngineParityTests: XCTestCase {
    func testReferenceVectorsMatchAndroidEngine() throws {
        let data = try Data(contentsOf: try XCTUnwrap(
            Bundle.module.url(forResource: "reference_vectors", withExtension: "json", subdirectory: "Fixtures")
        ))
        let vectors = try JSONDecoder().decode([ReferenceVector].self, from: data)

        XCTAssertEqual(vectors.count, 8)
        for vector in vectors {
            let outcome = CalculatorEngine.calculate(vector.inputs)
            if !vector.expectedErrors.isEmpty {
                guard case let .invalid(errors) = outcome else {
                    return XCTFail("\(vector.name) should be invalid")
                }
                XCTAssertEqual(errors.map(\.message), vector.expectedErrors)
                continue
            }

            guard case let .success(result) = outcome else {
                return XCTFail("\(vector.name) should be successful: \(outcome)")
            }
            guard let expected = vector.expected else { return XCTFail("\(vector.name) is missing expected values") }
            XCTAssertEqual(result.calcMethodOverlap, expected.calcMethodOverlap)
            XCTAssertEqual(result.calcMethodThickness, expected.calcMethodThickness)
            XCTAssertEqual(result.clothWidthMm, expected.clothWidthMm, accuracy: 1e-9)
            XCTAssertEqual(result.epoxyKg, expected.epoxyKg, accuracy: 1e-9)
            XCTAssertEqual(result.finalThicknessMm, expected.finalThickness, accuracy: 1e-9)
            XCTAssertEqual(result.isoLengthMm, expected.isoLength, accuracy: 1e-9)
            XCTAssertEqual(result.numBands, expected.numBands)
            XCTAssertEqual(result.numPlies, expected.numPlies)
            XCTAssertEqual(result.optimizedSquareMeters, expected.optimizedSqm, accuracy: 1e-9)
            XCTAssertEqual(result.compositeDesignPressureMpa, expected.compositeDesignPressure, accuracy: 1e-9)
            XCTAssertEqual(result.steelCapacityMpa, expected.steelCapacity, accuracy: 1e-9)
            XCTAssertEqual(result.procurementLengthMm, expected.procurementLength, accuracy: 1e-9)
            XCTAssertEqual(result.requiredThicknessMm, expected.requiredThickness, accuracy: 1e-9)
        }
    }

    func testClearStateReturnsFreshEmptyInput() {
        XCTAssertEqual(CalculatorInputs(), CalculatorInputs())
        XCTAssertNil(CalculatorInputs().clothWidthMm)
    }
}

private struct ReferenceVector: Decodable {
    let name: String
    let inputs: CalculatorInputs
    let expected: Expected?
    let expectedErrors: [String]

    struct Expected: Decodable {
        let calcMethodOverlap: String
        let calcMethodThickness: String
        let clothWidthMm: Double
        let epoxyKg: Double
        let finalThickness: Double
        let isoLength: Double
        let numBands: Int
        let numPlies: Int
        let optimizedSqm: Double
        let compositeDesignPressure: Double
        let steelCapacity: Double
        let procurementLength: Double
        let requiredThickness: Double

        enum CodingKeys: String, CodingKey {
            case calcMethodOverlap = "calc_method_overlap"
            case calcMethodThickness = "calc_method_thick"
            case clothWidthMm = "cloth_width_mm"
            case epoxyKg = "epoxy_kg"
            case finalThickness = "final_thickness"
            case isoLength = "iso_length"
            case numBands = "num_bands"
            case numPlies = "num_plies"
            case optimizedSqm = "optimized_sqm"
            case compositeDesignPressure = "p_composite_design"
            case steelCapacity = "p_steel_capacity"
            case procurementLength = "proc_length"
            case requiredThickness = "t_required"
        }
    }

    enum CodingKeys: String, CodingKey { case name, inputs, expected, expectedErrors }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        name = try container.decode(String.self, forKey: .name)
        inputs = try container.decode(CalculatorInputs.self, forKey: .inputs)
        expected = try? container.decode(Expected.self, forKey: .expected)
        expectedErrors = try container.decode([String].self, forKey: .expectedErrors)
    }
}
