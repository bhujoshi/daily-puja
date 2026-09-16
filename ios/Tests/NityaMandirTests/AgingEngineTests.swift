import XCTest
@testable import NityaMandir

final class AgingEngineTests: XCTestCase {

    func testPristineTemple() {
        let state = AgingEngine.calculateAging(
            lastWorshipDate: nil,
            lastCleanedDate: nil,
            currentDate: Date()
        )
        XCTAssertEqual(state.dustLevel, 0.0, accuracy: 0.001)
        XCTAssertEqual(state.flowerWitherFactor, 0.0, accuracy: 0.001)
        XCTAssertFalse(state.needsCleaning)
    }

    func testNextMorning_18HoursElapsed() {
        let now = Date()
        let eighteenHoursAgo = now.addingTimeInterval(-18.0 * 3600.0)

        let state = AgingEngine.calculateAging(
            lastWorshipDate: eighteenHoursAgo,
            lastCleanedDate: eighteenHoursAgo,
            currentDate: now
        )
        XCTAssertTrue(state.needsCleaning)
        XCTAssertGreaterThan(state.dustLevel, 0.03)
        XCTAssertGreaterThan(state.flowerWitherFactor, 0.20)
    }

    func testNeglectedTemple_48HoursElapsed() {
        let now = Date()
        let fortyEightHoursAgo = now.addingTimeInterval(-48.0 * 3600.0)

        let state = AgingEngine.calculateAging(
            lastWorshipDate: fortyEightHoursAgo,
            lastCleanedDate: fortyEightHoursAgo,
            currentDate: now
        )
        XCTAssertTrue(state.needsCleaning)
        XCTAssertGreaterThan(state.dustLevel, 0.40)
        XCTAssertEqual(state.flowerWitherFactor, 1.0, accuracy: 0.01)
    }
}
