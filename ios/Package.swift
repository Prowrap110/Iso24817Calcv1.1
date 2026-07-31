// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "Iso24817CalcCore",
    platforms: [
        .iOS(.v16),
        .macOS(.v13),
    ],
    products: [
        .library(name: "Iso24817CalcCore", targets: ["Iso24817CalcCore"]),
    ],
    targets: [
        .target(name: "Iso24817CalcCore"),
        .testTarget(
            name: "Iso24817CalcCoreTests",
            dependencies: ["Iso24817CalcCore"],
            resources: [.copy("Fixtures")]
        ),
    ]
)
