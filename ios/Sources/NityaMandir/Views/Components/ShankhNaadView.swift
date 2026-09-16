import SwiftUI

public struct ShankhNaadView: View {
    public let isBlowing: Bool
    public let onBlow: () -> Void

    @State private var waveScale: CGFloat = 0.8
    @State private var waveOpacity: Double = 0.8

    public init(isBlowing: Bool, onBlow: @escaping () -> Void) {
        self.isBlowing = isBlowing
        self.onBlow = onBlow
    }

    public var body: some View {
        Button(action: {
            SoundHapticEngine.shared.triggerShankhHaptic()
            onBlow()
        }) {
            ZStack {
                // Expanding Sonic Vibration Rings
                if isBlowing {
                    Circle()
                        .stroke(SacredTheme.gold.opacity(waveOpacity), lineWidth: 2)
                        .scaleEffect(waveScale)
                        .frame(width: 70, height: 70)
                        .onAppear {
                            withAnimation(.easeOut(duration: 0.8).repeatForever(autoreverses: false)) {
                                waveScale = 1.6
                                waveOpacity = 0.0
                            }
                        }
                }

                // White Sacred Shankh
                ZStack {
                    Circle()
                        .fill(Color.white.opacity(0.15))
                        .frame(width: 64, height: 64)

                    Image(systemName: "waveform.circle.fill")
                        .font(.system(size: 42))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [Color.white, Color(white: 0.92), SacredTheme.gold],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                }
            }
        }
        .buttonStyle(.plain)
    }
}
