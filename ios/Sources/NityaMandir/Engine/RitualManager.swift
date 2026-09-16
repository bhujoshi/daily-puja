import Foundation
import SwiftUI

@Observable
public final class RitualManager: @unchecked Sendable {
    public var currentStep: PoojaStep = .samagriSangrah
    public var curtainsClosed: Bool = true
    public var cleanlinessConfirmed: Bool = false
    public var isDiyaLit: Bool = false
    public var isDhoopLit: Bool = false
    public var isAbhishekCompleted: Bool = false
    public var isDryClothUsed: Bool = false

    public var ganeshDeity = Deity(id: "ganesh_ji", nameHi: "भगवान श्री गणेश", nameEn: "Lord Shri Ganesha")
    public var lakshmiDeity = Deity(id: "lakshmi_ji", nameHi: "माता महालक्ष्मी", nameEn: "Mother Maha Lakshmi")

    public var bellRingsCount: Int = 0
    public var isShankhBlown: Bool = false
    public var isBhogOffered: Bool = false
    public var isPoojaComplete: Bool = false
    public var streakDays: Int = 1

    public var lastWorshipDate: Date? = nil
    public var lastCleanedDate: Date? = nil

    public init() {}

    public func confirmCleanliness() {
        self.cleanlinessConfirmed = true
        self.curtainsClosed = false
    }

    public func toggleCurtains() {
        if cleanlinessConfirmed {
            curtainsClosed.toggle()
        }
    }

    public func advanceStep() {
        if let next = PoojaStep(rawValue: currentStep.rawValue + 1) {
            currentStep = next
        } else {
            completeAarti()
        }
    }

    public func lightDiya() {
        isDiyaLit = true
        isDhoopLit = true
        if currentStep == .deepaPrajwalan {
            advanceStep()
        }
    }

    public func completeAbhishek() {
        isAbhishekCompleted = true
        isDryClothUsed = true
        if currentStep == .devSnan {
            advanceStep()
        }
    }

    public func applyTilak(deityId: String) {
        if deityId == "ganesh_ji" {
            ganeshDeity.tilakApplied = true
        } else if deityId == "lakshmi_ji" {
            lakshmiDeity.tilakApplied = true
        }
        checkStep4Completion()
    }

    public func offerFlower(deityId: String) {
        if deityId == "ganesh_ji" {
            ganeshDeity.flowersOfferedCount += 1
        } else if deityId == "lakshmi_ji" {
            lakshmiDeity.flowersOfferedCount += 1
        }
        checkStep4Completion()
    }

    private func checkStep4Completion() {
        if currentStep == .tilakPushparpan {
            if ganeshDeity.tilakApplied && lakshmiDeity.tilakApplied &&
                (ganeshDeity.flowersOfferedCount + lakshmiDeity.flowersOfferedCount) >= 2 {
                advanceStep()
            }
        }
    }

    public func ringBell() {
        bellRingsCount += 1
    }

    public func blowShankh() {
        isShankhBlown = true
        if currentStep == .shankhNaad {
            advanceStep()
        }
    }

    public func offerBhog() {
        isBhogOffered = true
        if currentStep == .bhogSamarpan {
            advanceStep()
        }
    }

    public func completeAarti() {
        isPoojaComplete = true
        let now = TimeTravelEngine.shared.getEffectiveDate()
        lastWorshipDate = now
        lastCleanedDate = now
        streakDays += 1
    }

    public func cleanMandir() {
        let now = TimeTravelEngine.shared.getEffectiveDate()
        lastCleanedDate = now
        ganeshDeity.tilakApplied = false
        ganeshDeity.flowersOfferedCount = 0
        lakshmiDeity.tilakApplied = false
        lakshmiDeity.flowersOfferedCount = 0
        isDiyaLit = false
        isDhoopLit = false
        isAbhishekCompleted = false
        isBhogOffered = false
        isShankhBlown = false
        bellRingsCount = 0
        currentStep = .samagriSangrah
    }
}
