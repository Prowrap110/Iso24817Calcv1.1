# PROWRAP ISO 24817 iPhone app

This directory contains the native SwiftUI iPhone app and the tested offline calculation core. It is isolated on the `feature/ios-iphone-app` branch; the existing Streamlit, macOS, and Android implementations are not modified.

## Open and run

1. Open `Iso24817Calc/Iso24817Calc.xcodeproj` in Xcode 26 or newer.
2. Select the `Iso24817Calc` scheme and an iPhone simulator or connected iPhone.
3. Build and run. The form intentionally starts blank on every launch and the **Clear all entries** button resets the form and results without removing any fields.

The app has no network calls, persistence, or employee data storage. The cloth-width entry is an explicit variable and is passed to the calculation core; the fixed 50 mm value remains the qualified stitch overlap only.

## Verification

The `Iso24817CalcCore` Swift package includes the eight Android reference vectors under `Tests/Iso24817CalcCoreTests/Fixtures`. Run the package tests with Xcode's Test action before archiving.

The app target includes the same core source files directly so the iPhone target can archive as a self-contained app; keep the package copy and app copy in sync when changing calculation logic.

## TestFlight distribution

For employee distribution, configure the bundle identifier `com.protapglobal.Iso24817Calc` in the Apple Developer account, select the team's signing team in Xcode, archive the app, and upload it to App Store Connect. Testers can then install it through TestFlight. No signing credentials or provisioning profiles are stored in this repository.
