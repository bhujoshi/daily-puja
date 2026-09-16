import SwiftUI

public struct CleaningView: View {
    public let onComplete: () -> Void

    @State private var flowersDone = false
    @State private var wipeDone = false
    @State private var diyaDone = false

    public init(onComplete: @escaping () -> Void) {
        self.onComplete = onComplete
    }

    private var allDone: Bool {
        flowersDone && wipeDone && diyaDone
    }

    public var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "sparkles")
                    .foregroundColor(SacredTheme.gold)
                Text("मंदिर शुद्धि एवं सवेरे की सफाई")
                    .font(.headline.bold())
                    .foregroundColor(SacredTheme.gold)
            }

            Text("कल के फूल निर्माल्य हो चुके हैं एवं मंदिर में धूल की परत आ गई है। पूजा आरंभ करने से पूर्व पवित्र सफाई करें।")
                .font(.caption)
                .foregroundColor(SacredTheme.textSecondary)
                .multilineTextAlignment(.center)

            VStack(spacing: 8) {
                CleanRow(
                    title: "1. निर्माल्य संग्रह (Nirmalya Collection)",
                    subtitle: "बासी पुष्पों को उठाकर पवित्र निर्माल्य पात्र में रखें",
                    isDone: flowersDone,
                    onTap: { flowersDone = true }
                )

                CleanRow(
                    title: "2. चौकी मार्जन (Wiping Altar)",
                    subtitle: "गीले स्वच्छ वस्त्र से मंदिर की चौकी की धूल पोंछें",
                    isDone: wipeDone,
                    onTap: { wipeDone = true }
                )

                CleanRow(
                    title: "3. दीपक प्रक्षालन (Washing Diya)",
                    subtitle: "दीपक को धोकर स्वच्छ करें व नवीन बाती लगाएं",
                    isDone: diyaDone,
                    onTap: { diyaDone = true }
                )
            }

            Button(action: onComplete) {
                Text(allDone ? "शुद्धिकरण पूर्ण • अब पूजा आरंभ करें" : "तीनों चरण पूर्ण करें")
                    .font(.subheadline.bold())
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(allDone ? SacredTheme.deepSaffron : Color.gray.opacity(0.4))
                    .cornerRadius(10)
            }
            .disabled(!allDone)
        }
        .padding(16)
        .background(SacredTheme.teakDark.opacity(0.96))
        .cornerRadius(18)
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(SacredTheme.gold, lineWidth: 1.5)
        )
        .padding(.horizontal, 16)
    }
}

private struct CleanRow: View {
    let title: String
    let subtitle: String
    let isDone: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.caption.bold())
                        .foregroundColor(SacredTheme.textPrimary)
                    Text(subtitle)
                        .font(.system(size: 10))
                        .foregroundColor(SacredTheme.textSecondary)
                }
                Spacer()
                Image(systemName: isDone ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(isDone ? .green : SacredTheme.brass)
                    .font(.title3)
            }
            .padding(10)
            .background(isDone ? Color.green.opacity(0.15) : Color.white.opacity(0.06))
            .cornerRadius(10)
        }
        .buttonStyle(.plain)
    }
}
