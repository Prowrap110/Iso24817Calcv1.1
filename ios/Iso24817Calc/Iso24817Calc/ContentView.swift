import SwiftUI

struct ContentView: View {
    @State private var form = CalculatorFormState()
    @State private var outcome: CalculationOutcome?

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("PROWRAP 110")
                            .font(.title2.weight(.bold))
                            .foregroundStyle(.tint)
                        Text("ISO 24817 composite repair calculator")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text("Enter the project data below. A new calculation starts blank and nothing is saved between launches.")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }

                Section("Project information") {
                    TextField("Customer", text: $form.customer)
                    TextField("Location", text: $form.location)
                    TextField("Report number", text: $form.reportNo)
                }

                Section("Pipeline data") {
                    numberField("Pipe OD", text: $form.odMm, unit: "mm")
                    numberField("Nominal wall", text: $form.wallMm, unit: "mm")
                    numberField("Pipe yield", text: $form.yieldStrengthMpa, unit: "MPa")
                }

                Section("Service conditions") {
                    numberField("Design pressure", text: $form.pressureBar, unit: "bar")
                    numberField("Operating temperature", text: $form.temperatureC, unit: "°C")
                }

                Section("Defect data") {
                    Picker("Mechanism", selection: $form.defectType) {
                        Text("Select").tag("")
                        Text("Corrosion").tag("Corrosion")
                        Text("Dent").tag("Dent")
                        Text("Leak").tag("Leak")
                        Text("Crack").tag("Crack")
                    }
                    Picker("Defect location", selection: $form.defectLocation) {
                        Text("Select").tag("")
                        Text("External").tag("External")
                        Text("Internal").tag("Internal")
                    }
                    numberField("Defect length", text: $form.defectLengthMm, unit: "mm")
                    numberField("Remaining wall", text: $form.remainingWallMm, unit: "mm")
                    if form.defectType == "Corrosion" && form.defectLocation == "Internal" {
                        numberField("Internal corrosion rate", text: $form.internalCorrosionRateMmPerYear, unit: "mm/yr")
                    }
                }

                Section("Safety and design") {
                    numberField("Design life", text: $form.designLifeYears, unit: "years")
                    numberField("Design factor (f)", text: $form.designFactor, unit: "")
                }

                Section("Installation and load") {
                    numberField("Installation temperature", text: $form.installationTemperatureC, unit: "°C")
                    Picker("Component type", selection: $form.componentType) {
                        Text("Select").tag("")
                        Text("Straight").tag("Straight")
                        Text("Bend").tag("Bend")
                        Text("Tee").tag("Tee")
                        Text("Flange").tag("Flange")
                        Text("Reducer").tag("Reducer")
                    }
                    numberField("Cyclic derating factor", text: $form.cyclicDeratingFactor, unit: "")
                    Picker("Axial load case", selection: $form.axialLoadCase) {
                        Text("Select").tag("")
                        Text("0 — no axial case").tag("0")
                        Text("1 — axial case").tag("1")
                    }
                }

                Section("Prowrap material") {
                    numberField("Prowrap CF cloth width", text: $form.clothWidthMm, unit: "mm")
                    Text("Used for procurement band count and cloth length. It must exceed the fixed 50 mm stitch overlap.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    Toggle("Force minimum three plies", isOn: $form.forceThreeLayers)
                }

                actionSection
                resultSection
            }
            .navigationTitle("ISO 24817 Calculator")
            .navigationBarTitleDisplayMode(.inline)
            .scrollDismissesKeyboard(.interactively)
        }
    }

    @ViewBuilder
    private var actionSection: some View {
        Section {
            if !form.isReady {
                Label("Complete the required fields to calculate.", systemImage: "info.circle")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            Button {
                outcome = CalculatorEngine.calculate(form.makeInputs())
            } label: {
                Label("Calculate and optimize", systemImage: "function")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(!form.isReady)

            Button(role: .destructive) {
                form = CalculatorFormState()
                outcome = nil
            } label: {
                Label("Clear all entries", systemImage: "arrow.counterclockwise")
                    .frame(maxWidth: .infinity)
            }
        }
    }

    @ViewBuilder
    private var resultSection: some View {
        if let outcome {
            switch outcome {
            case let .invalid(errors):
                Section("Check the inputs") {
                    ForEach(Array(errors.enumerated()), id: \.offset) { _, error in
                        Label(error.message, systemImage: "exclamationmark.triangle.fill")
                            .foregroundStyle(.red)
                    }
                }
            case let .success(result):
                Section("Calculation result") {
                    resultRow("Thickness method", result.calcMethodThickness)
                    resultRow("Overlap method", result.calcMethodOverlap)
                    resultRow("Required thickness", format(result.requiredThicknessMm, "mm"))
                    resultRow("Applied plies", "\(result.numPlies)")
                    resultRow("Final thickness", format(result.finalThicknessMm, "mm"))
                    resultRow("ISO repair length", format(result.isoLengthMm, "mm"))
                    resultRow("Number of bands", "\(result.numBands)")
                    resultRow("Procurement length", format(result.procurementLengthMm, "mm"))
                    resultRow("Cloth width used", format(result.clothWidthMm, "mm"))
                    resultRow("Optimized cloth", format(result.optimizedSquareMeters, "m²"))
                    resultRow("Epoxy", format(result.epoxyKg, "kg"))
                    ShareLink(item: reportText(result), subject: Text("PROWRAP ISO 24817 calculation")) {
                        Label("Share calculation summary", systemImage: "square.and.arrow.up")
                    }
                }
                if !result.warnings.isEmpty {
                    Section("Engineering notes") {
                        ForEach(result.warnings, id: \.self) { warning in
                            Label(warning, systemImage: "exclamationmark.triangle")
                                .font(.footnote)
                        }
                    }
                }
            }
        }
    }

    private func numberField(_ title: String, text: Binding<String>, unit: String) -> some View {
        HStack {
            TextField(title, text: text)
                .keyboardType(.decimalPad)
                .textInputAutocapitalization(.never)
            if !unit.isEmpty {
                Text(unit).foregroundStyle(.secondary)
            }
        }
    }

    private func resultRow(_ title: String, _ value: String) -> some View {
        LabeledContent(title) { Text(value).multilineTextAlignment(.trailing) }
    }

    private func format(_ value: Double, _ unit: String) -> String {
        let number = String(format: "%.2f", value)
        return unit.isEmpty ? number : "\(number) \(unit)"
    }

    private func reportText(_ result: CalculationResult) -> String {
        """
        PROWRAP ISO 24817 CALCULATION
        Customer: \(result.customer)
        Location: \(result.location)
        Report: \(result.reportNo)
        Cloth width: \(format(result.clothWidthMm, "mm"))
        Thickness: \(format(result.finalThicknessMm, "mm")) (\(result.numPlies) plies)
        ISO repair length: \(format(result.isoLengthMm, "mm"))
        Bands: \(result.numBands)
        Procurement length: \(format(result.procurementLengthMm, "mm"))
        Optimized cloth: \(format(result.optimizedSquareMeters, "m²"))
        Epoxy: \(format(result.epoxyKg, "kg"))
        """
    }
}

private struct CalculatorFormState: Equatable {
    var customer = ""
    var location = ""
    var reportNo = ""
    var odMm = ""
    var wallMm = ""
    var yieldStrengthMpa = ""
    var pressureBar = ""
    var temperatureC = ""
    var defectType = ""
    var defectLocation = ""
    var defectLengthMm = ""
    var remainingWallMm = ""
    var internalCorrosionRateMmPerYear = ""
    var designLifeYears = ""
    var designFactor = ""
    var installationTemperatureC = ""
    var componentType = ""
    var cyclicDeratingFactor = ""
    var axialLoadCase = ""
    var clothWidthMm = ""
    var forceThreeLayers = false

    var isReady: Bool {
        let required = [customer, location, reportNo, odMm, wallMm, yieldStrengthMpa, pressureBar, temperatureC,
                        defectType, defectLocation, defectLengthMm, remainingWallMm, designLifeYears, designFactor,
                        installationTemperatureC, componentType, cyclicDeratingFactor, axialLoadCase, clothWidthMm]
        guard required.allSatisfy({ !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) else { return false }
        if defectType == "Corrosion" && defectLocation == "Internal" {
            return !internalCorrosionRateMmPerYear.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
        return true
    }

    func makeInputs() -> CalculatorInputs {
        CalculatorInputs(
            customer: nonEmpty(customer), location: nonEmpty(location), reportNo: nonEmpty(reportNo),
            odMm: number(odMm), wallMm: number(wallMm), yieldStrengthMpa: number(yieldStrengthMpa),
            pressureBar: number(pressureBar), temperatureC: number(temperatureC), defectType: nonEmpty(defectType),
            defectLocation: nonEmpty(defectLocation), defectLengthMm: number(defectLengthMm),
            remainingWallMm: number(remainingWallMm), internalCorrosionRateMmPerYear: number(internalCorrosionRateMmPerYear),
            designLifeYears: number(designLifeYears), designFactor: number(designFactor),
            installationTemperatureC: number(installationTemperatureC), componentType: nonEmpty(componentType),
            cyclicDeratingFactor: number(cyclicDeratingFactor), axialLoadCase: Int(axialLoadCase),
            clothWidthMm: number(clothWidthMm), forceThreeLayers: forceThreeLayers
        )
    }

    private func number(_ text: String) -> Double? {
        Double(text.replacingOccurrences(of: ",", with: ".").trimmingCharacters(in: .whitespacesAndNewlines))
    }

    private func nonEmpty(_ text: String) -> String? {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}

#Preview { ContentView() }
