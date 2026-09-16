# Nitya Mandir (नित्य मंदिर) - Testing Strategy & Environments

This document details the quality assurance strategy, testing environments, automated test suites, and the specialized **Time-Travel Test Harness** designed to validate temple aging, dust accumulation, and cleaning mechanics.

---

## 1. Testing Environments

| Environment | Purpose | Target Infra | Data Persistence |
|---|---|---|---|
| **Local Dev** | Rapid iteration of Go backend, Android, and iOS | Local machine (`localhost:8080`, Android Studio, Xcode) | Embedded SQLite (`nityamandir.db`), In-memory / DataStore |
| **Time-Travel Harness** | Instantaneous validation of temporal aging & cleaning flows | In-app Debug Drawer on Android & iOS debug builds | Local simulated timestamp offsets |
| **Integration / Staging** | Multi-device sync, push notifications, performance tests | Cloud container (Docker + PostgreSQL) | Persistent staging DB |
| **Production** | Live user sanctum | Production cluster, CDN for audio assets | High-availability replicated PostgreSQL |

---

## 2. The Time-Travel Test Harness (मंदिर काल-चक्र परीक्षण)

### Problem Statement
Validating that the temple correctly accumulates dust after 24 hours, withers flowers into nirmalya, and requires morning cleaning cannot depend on waiting 24 to 72 actual hours during development and automated tests.

### Solution: `TimeTravelEngine`
Both Android and iOS implementations include a singleton `TimeTravelEngine` that wraps `Clock.System.now()`:
```
EffectiveTime = RealSystemTime + InjectedTimeOffset
```

### In-App Debug Drawer Controls
On debug builds, shaking the device or double-tapping the temple dome opens the Time-Travel Debug Drawer with instant presets:
- **`Now (0h)`**: Pristine sanctum post-worship. Diya burning, fresh flowers, no dust ($D=0$, $F=0$).
- **`+12 Hours`**: Evening state. Diya extinguished, flowers beginning to lose luster ($F=0$).
- **`+24 Hours (Next Morning)`**: Morning cleaning triggered! Flowers wilted into nirmalya ($F=0.5$), subtle dust layer visible ($D=0.14$), curtains closed, cleaning required before pooja.
- **`+72 Hours (3 Days Neglect)`**: Thick dust layer ($D=0.8$), dry brown flowers ($F=1.0$), extinguished lamps.
- **`+7 Days (Heavy Neglect)`**: Maximum dust opacity ($D=1.0$), dull brass textures, spider-silk hints.
- **`Reset All`**: Wipes local state and resets to virgin mandir setup.

---

## 3. Automated Test Suites

### 3.1 Go Backend Tests (`backend/`)
Execute with race detection enabled:
```bash
cd backend && go test -v -race ./...
```
- **Unit Tests**:
  - `AgingEngine_Test`: Mathematical verification of dust and flower withering formulas across arbitrary timestamps.
  - `StreakEngine_Test`: Verifies consecution rules, timezone rollover, and anti-tamper timestamp validations.
  - `PanchangService_Test`: Validates calculations for auspicious Tithi, Brahma Muhurta, and sunrise-anchored worship windows.
- **API Integration Tests (`net/http/httptest`)**:
  - `TestGetMandirState_Clean`: Expects clean state on initial user setup.
  - `TestPoojaComplete_UpdatesStreak`: Completes 8-step session, verifies streak increment and timestamp updates.
  - `TestCleanMandir_ClearsDust`: Sends clean command, asserts dust level resets to 0.

### 3.2 Android Automated Tests (`android/`)
Execute via Gradle:
```bash
./gradlew testDebugUnitTest
```
- **Robolectric / JUnit 5 Unit Tests**:
  - `AgingEngineTest.kt`: Tests mathematical aging curve at 0h, 12h, 24h, 48h, 72h.
  - `RitualManagerTest.kt`: Enforces state transitions (Steps 1 through 8) and ensures forbidden jumps error out.
- **Compose UI Tests (`createComposeRule`)**:
  - `CurtainPurityGateTest`: Asserts curtains remain closed until `onConfirmPurity` is triggered.
  - `DustWipingGestureTest`: Simulates drag gestures across the `CleaningView` and verifies dust alpha decrements to 0.

### 3.3 iOS Automated Tests (`ios/`)
Execute via Swift Package Manager / Xcode:
```bash
swift test
```
- **XCTest Suites**:
  - `AgingEngineTests.swift`: Verifies exact mathematical parity with the Android and Go calculations.
  - `RitualStateTests.swift`: Validates the 8-step completion sequence.
- **SwiftUI Preview Snapshot Matrix**:
  - Previews covering all visual states:
    1. `MandirPreview_Pristine`
    2. `MandirPreview_CurtainsClosed`
    3. `MandirPreview_NextDayDust`
    4. `MandirPreview_Neglected7Days`
    5. `MandirPreview_ActiveAarti`

---

## 4. CI/CD Pipeline Automation

A continuous integration pipeline (GitHub Actions) runs on every commit:
1. **Linting**:
   - `golangci-lint run ./...`
   - Android `ktlintCheck`
   - Swift `swiftlint`
2. **Unit & Race Tests**:
   - `go test -race ./...`
   - `./gradlew testDebugUnitTest`
   - `swift test`
3. **Build Verification**:
   - Go binary compilation: `go build -o /dev/null ./cmd/server`
   - Android debug APK compilation: `./gradlew assembleDebug`
   - iOS build: `swift build`
