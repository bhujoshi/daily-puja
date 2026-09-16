import SwiftUI

public struct RitualStepBar: View {
    public let currentStep: PoojaStep
    public let isHindi: Bool
    public let onAction: () -> Void

    public init(currentStep: PoojaStep, isHindi: Bool, onAction: @escaping () -> Void) {
        self.currentStep = currentStep
        self.isHindi = isHindi
        self.onAction = onAction
    }

    private var actionButtonTitle: String {
        switch currentStep {
        case .samagriSangrah: return isHindi ? "थाली में सामग्री एकत्र करें" : "Gather Items on Thali"
        case .deepaPrajwalan: return isHindi ? "दीपक व धूप प्रज्वलित करें" : "Light Diya & Dhoop"
        case .devSnan: return isHindi ? "जल अभिषेक व वस्त्र मार्जन करें" : "Perform Abhishek & Clean"
        case .tilakPushparpan: return isHindi ? "तिलक लगाएं व पुष्प अर्पित करें" : "Apply Tilak & Offer Flowers"
        case .ghantiAarti: return isHindi ? "आरती घुमाएं व घंटी बजाएं" : "Rotate Aarti & Ring Bell"
        case .shankhNaad: return isHindi ? "शंख नाद करें" : "Blow the Sacred Conch"
        case .bhogSamarpan: return isHindi ? "भोग व आचमन अर्पित करें" : "Offer Bhog & Achaman"
        case .aartiStuti: return isHindi ? "आरती गायन एवं समापन" : "Sing Aarti & Complete"
        }
    }

    public var body: some View {
        VStack(spacing: 8) {
            // Header: Step pill and title
            HStack {
                Text(isHindi ? "चरण \(currentStep.rawValue) / 8" : "Step \(currentStep.rawValue) / 8")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(SacredTheme.deepSaffron)
                    .cornerRadius(6)

                Spacer()

                Text(isHindi ? currentStep.titleHi : currentStep.titleEn)
                    .font(.subheadline.bold())
                    .foregroundColor(SacredTheme.gold)
            }

            // Instruction
            Text(isHindi ? currentStep.instructionHi : currentStep.instructionEn)
                .font(.caption)
                .foregroundColor(SacredTheme.textPrimary)
                .multilineTextAlignment(.center)

            // Sacred Mantra Box
            Text(isHindi ? currentStep.mantraHi : currentStep.mantraEn)
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(SacredTheme.gold)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .frame(maxWidth: .infinity)
                .background(Color.black.opacity(0.3))
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(SacredTheme.brass.opacity(0.5), lineWidth: 1)
                )

            // Action Button
            Button(action: onAction) {
                Text(actionButtonTitle)
                    .font(.subheadline.bold())
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(SacredTheme.deepSaffron)
                    .cornerRadius(10)
            }
        }
        .padding(14)
        .background(SacredTheme.teakWood.opacity(0.96))
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(SacredTheme.gold, lineWidth: 1.5)
        )
        .padding(.horizontal, 16)
    }
}
