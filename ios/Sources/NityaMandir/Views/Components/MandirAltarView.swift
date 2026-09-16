import SwiftUI

public struct MandirAltarView: View {
    public let ganeshDeity: Deity
    public let lakshmiDeity: Deity
    public let isDiyaLit: Bool
    public let dustLevel: Float
    public let flowerWitherFactor: Float
    public let onGaneshTap: () -> Void
    public let onLakshmiTap: () -> Void

    public init(
        ganeshDeity: Deity,
        lakshmiDeity: Deity,
        isDiyaLit: Bool,
        dustLevel: Float,
        flowerWitherFactor: Float,
        onGaneshTap: @escaping () -> Void,
        onLakshmiTap: @escaping () -> Void
    ) {
        self.ganeshDeity = ganeshDeity
        self.lakshmiDeity = lakshmiDeity
        self.isDiyaLit = isDiyaLit
        self.dustLevel = dustLevel
        self.flowerWitherFactor = flowerWitherFactor
        self.onGaneshTap = onGaneshTap
        self.onLakshmiTap = onLakshmiTap
    }

    public var body: some View {
        ZStack {
            // Dark Sanctum Background
            SacredTheme.sanctumDark.ignoresSafeArea()

            // Temple Arch Architecture
            VStack {
                Spacer()
                ZStack {
                    // Carved Wooden Arch Base
                    RoundedRectangle(cornerRadius: 30)
                        .fill(
                            LinearGradient(
                                colors: [SacredTheme.teakDark, SacredTheme.teakWood, Color(red: 0.15, green: 0.07, blue: 0.03)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .frame(maxWidth: .infinity)
                        .frame(height: 480)
                        .overlay(
                            RoundedRectangle(cornerRadius: 30)
                                .stroke(SacredTheme.gold, lineWidth: 3)
                        )
                        .padding(.horizontal, 16)

                    // Deities Row
                    VStack {
                        HStack(spacing: 32) {
                            // Shri Ganesh Ji
                            DeityView(
                                deity: ganeshDeity,
                                symbol: "ॐ गं",
                                name: "श्री गणेश",
                                flowerWitherFactor: flowerWitherFactor,
                                isDiyaLit: isDiyaLit,
                                onTap: onGaneshTap
                            )

                            // Maa Lakshmi Ji
                            DeityView(
                                deity: lakshmiDeity,
                                symbol: "ॐ श्रीं",
                                name: "महालक्ष्मी",
                                flowerWitherFactor: flowerWitherFactor,
                                isDiyaLit: isDiyaLit,
                                onTap: onLakshmiTap
                            )
                        }
                        .padding(.top, 40)

                        Spacer()

                        // Altar Chouki Base with Brass Diya
                        ZStack {
                            Rectangle()
                                .fill(
                                    LinearGradient(
                                        colors: [Color(red: 0.25, green: 0.12, blue: 0.06), SacredTheme.teakDark],
                                        startPoint: .top,
                                        endPoint: .bottom
                                    )
                                )
                                .frame(height: 100)
                                .overlay(
                                    Rectangle()
                                        .stroke(SacredTheme.gold, lineWidth: 1.5)
                                )

                            // Center Diya
                            DiyaFlameView(isLit: isDiyaLit)
                                .offset(y: -10)
                        }
                        .padding(.horizontal, 16)
                    }
                    .frame(height: 480)
                }
                Spacer().frame(height: 140)
            }

            // Procedural Dust Overlay
            if dustLevel > 0.01 {
                VStack {
                    Spacer()
                    Rectangle()
                        .fill(Color(white: 0.65).opacity(Double(dustLevel * 0.75)))
                        .frame(height: 240)
                }
                .allowsHitTesting(false)
            }
        }
    }
}

private struct DeityView: View {
    let deity: Deity
    let symbol: String
    let name: String
    let flowerWitherFactor: Float
    let isDiyaLit: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 8) {
                ZStack {
                    // Radiant Golden Aura
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [
                                    isDiyaLit ? SacredTheme.gold.opacity(0.6) : SacredTheme.brass.opacity(0.3),
                                    Color.clear
                                ],
                                center: .center,
                                startRadius: 10,
                                endRadius: 55
                            )
                        )
                        .frame(width: 110, height: 110)

                    // Deity Card
                    RoundedRectangle(cornerRadius: 16)
                        .fill(
                            LinearGradient(
                                colors: [Color(red: 0.32, green: 0.18, blue: 0.09), Color(red: 0.15, green: 0.08, blue: 0.04)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .frame(width: 86, height: 86)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(SacredTheme.gold, lineWidth: 1.5)
                        )
                        .overlay(
                            VStack(spacing: 4) {
                                Text(symbol)
                                    .font(.title2.bold())
                                    .foregroundColor(SacredTheme.gold)

                                if deity.tilakApplied {
                                    Circle()
                                        .fill(SacredTheme.vermilion)
                                        .frame(width: 7, height: 7)
                                        .overlay(Circle().stroke(SacredTheme.gold, lineWidth: 0.5))
                                }
                            }
                        )

                    // Flowers Offered
                    if deity.flowersOfferedCount > 0 {
                        let fColor = flowerWitherFactor > 0.5 ? Color.brown : SacredTheme.marigoldOrange
                        HStack(spacing: 4) {
                            ForEach(0..<min(deity.flowersOfferedCount, 4), id: \.self) { _ in
                                Circle()
                                    .fill(fColor)
                                    .frame(width: 12, height: 12)
                                    .overlay(Circle().stroke(SacredTheme.gold, lineWidth: 1))
                            }
                        }
                        .offset(y: 42)
                    }
                }

                Text(name)
                    .font(.subheadline.bold())
                    .foregroundColor(SacredTheme.textPrimary)
            }
        }
        .buttonStyle(.plain)
    }
}
