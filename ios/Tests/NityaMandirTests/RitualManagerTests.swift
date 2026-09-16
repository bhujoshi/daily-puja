import XCTest
@testable import NityaMandir

final class RitualManagerTests: XCTestCase {

    func testPurityGate_curtainsClosedInitially() {
        let manager = RitualManager()
        XCTAssertTrue(manager.curtainsClosed)
        XCTAssertFalse(manager.cleanlinessConfirmed)

        manager.confirmCleanliness()

        XCTAssertTrue(manager.cleanlinessConfirmed)
        XCTAssertFalse(manager.curtainsClosed)
    }

    func testRitualStepProgression() {
        let manager = RitualManager()
        XCTAssertEqual(manager.currentStep, .samagriSangrah)

        manager.advanceStep()
        XCTAssertEqual(manager.currentStep, .deepaPrajwalan)

        manager.lightDiya()
        XCTAssertTrue(manager.isDiyaLit)
        XCTAssertEqual(manager.currentStep, .devSnan)

        manager.completeAbhishek()
        XCTAssertTrue(manager.isAbhishekCompleted)
        XCTAssertEqual(manager.currentStep, .tilakPushparpan)
    }

    func testCleaningResetsRitual() {
        let manager = RitualManager()
        manager.lightDiya()
        manager.completeAbhishek()

        manager.cleanMandir()

        XCTAssertFalse(manager.isDiyaLit)
        XCTAssertFalse(manager.isAbhishekCompleted)
        XCTAssertEqual(manager.currentStep, .samagriSangrah)
    }
}
