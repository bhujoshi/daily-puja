import Foundation
#if canImport(UIKit)
import UIKit
#endif

public final class SoundHapticEngine: @unchecked Sendable {
    public static let shared = SoundHapticEngine()

    private init() {}

    public func triggerBellHaptic() {
        #if canImport(UIKit)
        let generator = UIImpactFeedbackGenerator(style: .rigid)
        generator.prepare()
        generator.impactOccurred(intensity: 1.0)
        #endif
    }

    public func triggerShankhHaptic() {
        #if canImport(UIKit)
        let generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(.success)
        #endif
    }

    public func triggerLightTap() {
        #if canImport(UIKit)
        let generator = UIImpactFeedbackGenerator(style: .light)
        generator.prepare()
        generator.impactOccurred()
        #endif
    }
}
