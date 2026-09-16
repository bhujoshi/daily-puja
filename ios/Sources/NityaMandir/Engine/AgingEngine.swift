import Foundation

public struct AgingEngine {

    /// Computes procedural dust accumulation and flower withering based on time elapsed.
    public static func calculateAging(
        lastWorshipDate: Date?,
        lastCleanedDate: Date?,
        currentDate: Date
    ) -> AgingState {
        guard let lastWorship = lastWorshipDate else {
            return AgingState(
                elapsedHours: 0.0,
                dustLevel: 0.0,
                flowerWitherFactor: 0.0,
                needsCleaning: false,
                statusHi: "मंदिर नवनिर्मित एवं पवित्र है। प्रथम पूजा आरंभ करें।",
                statusEn: "Temple is pristine. Begin your first pooja."
            )
        }

        let elapsedWorshipSecs = max(0, currentDate.timeIntervalSince(lastWorship))
        let elapsedWorshipHours = Float(elapsedWorshipSecs / 3600.0)

        // Dust calculation references the most recent clean or worship
        let cleanRef = (lastCleanedDate != nil && lastCleanedDate! > lastWorship) ? lastCleanedDate! : lastWorship
        let elapsedCleanSecs = max(0, currentDate.timeIntervalSince(cleanRef))
        let elapsedCleanHours = Float(elapsedCleanSecs / 3600.0)

        // Dust formula: D = min(1.0, (t - 14) / 72)
        let dustLevel: Float
        if elapsedCleanHours > 14.0 {
            dustLevel = min(1.0, (elapsedCleanHours - 14.0) / 72.0)
        } else {
            dustLevel = 0.0
        }

        // Flower withering formula: F = min(1.0, (t - 12) / 24)
        let flowerWither: Float
        if elapsedWorshipHours > 12.0 {
            flowerWither = min(1.0, (elapsedWorshipHours - 12.0) / 24.0)
        } else {
            flowerWither = 0.0
        }

        let needsCleaning = elapsedWorshipHours >= 14.0 || dustLevel > 0.05 || flowerWither > 0.1

        let statusHi: String
        let statusEn: String

        switch elapsedWorshipHours {
        case ..<12.0:
            statusHi = "मंदिर प्रकाशमान एवं पावन है।"
            statusEn = "Temple is glowing and sacred."
        case 12.0..<24.0:
            statusHi = "कल के पुष्प निर्माल्य हो रहे हैं। पूजा से पूर्व सफाई करें।"
            statusEn = "Yesterday's flowers have withered. Morning cleaning required."
        case 24.0..<72.0:
            statusHi = "मंदिर में धूल व निर्माल्य एकत्रित हो गया है। कृपा कर मंदिर स्वच्छ करें।"
            statusEn = "Dust and nirmalya have accumulated. Please clean the temple."
        default:
            statusHi = "मंदिर अत्यधिक जीर्ण व धूल-धूसरित हो गया है। संपूर्ण शुद्धि आवश्यक है।"
            statusEn = "Temple is heavily neglected. Deep cleaning required."
        }

        return AgingState(
            elapsedHours: (elapsedWorshipHours * 100).rounded() / 100.0,
            dustLevel: (dustLevel * 1000).rounded() / 1000.0,
            flowerWitherFactor: (flowerWither * 1000).rounded() / 1000.0,
            needsCleaning: needsCleaning,
            statusHi: statusHi,
            statusEn: statusEn
        )
    }
}
