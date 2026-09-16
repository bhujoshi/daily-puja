import Foundation

public final class TimeTravelEngine: @unchecked Sendable {
    public static let shared = TimeTravelEngine()

    public private(set) var offsetHours: Double = 0.0

    private init() {}

    public func setOffset(hours: Double) {
        self.offsetHours = hours
    }

    public func reset() {
        self.offsetHours = 0.0
    }

    public func getEffectiveDate() -> Date {
        return Date().addingTimeInterval(offsetHours * 3600.0)
    }
}
