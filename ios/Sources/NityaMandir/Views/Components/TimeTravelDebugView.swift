import SwiftUI

public struct TimeTravelDebugView: View {
    public let currentOffsetHours: Double
    public let onSetOffset: (Double) -> Void
    public let onReset: () -> Void

    public init(currentOffsetHours: Double, onSetOffset: @escaping (Double) -> Void, onReset: @escaping () -> Void) {
        self.currentOffsetHours = currentOffsetHours
        self.onSetOffset = onSetOffset
        self.onReset = onReset
    }

    public var body: some View {
        VStack(spacing: 8) {
            HStack {
                Text("⚙️ काल-चक्र परीक्षण (Time Travel Harness)")
                    .font(.caption.bold())
                    .foregroundColor(SacredTheme.gold)
                Spacer()
                Text("+\(Int(currentOffsetHours))h")
                    .font(.caption)
                    .foregroundColor(SacredTheme.textSecondary)
            }

            HStack(spacing: 6) {
                Button("Now") { onSetOffset(0) }
                Button("+12h") { onSetOffset(12) }
                Button("+24h") { onSetOffset(24) }
                Button("+72h") { onSetOffset(72) }
                Button("+7d") { onSetOffset(168) }
            }
            .buttonStyle(.bordered)
            .tint(SacredTheme.gold)
            .font(.system(size: 11))

            Button(action: onReset) {
                Text("Reset Mandir State")
                    .font(.caption.bold())
                    .foregroundColor(SacredTheme.deepSaffron)
            }
            .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .padding(12)
        .background(SacredTheme.teakDark.opacity(0.95))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(SacredTheme.brass, lineWidth: 1)
        )
        .padding(.horizontal, 16)
    }
}
