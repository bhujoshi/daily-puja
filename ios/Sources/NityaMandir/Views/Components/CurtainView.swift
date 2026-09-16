import SwiftUI

public struct CurtainView: View {
    public let isClosed: Bool
    public let isCleanlinessConfirmed: Bool
    public let onConfirmPurity: () -> Void

    @State private var showSnanMantra = false

    public init(isClosed: Bool, isCleanlinessConfirmed: Bool, onConfirmPurity: @escaping () -> Void) {
        self.isClosed = isClosed
        self.isCleanlinessConfirmed = isCleanlinessConfirmed
        self.onConfirmPurity = onConfirmPurity
    }

    public var body: some View {
        GeometryReader { proxy in
            let width = proxy.size.width
            let openOffset = isClosed ? 0 : width * 0.55

            ZStack {
                // Left Curtain
                HStack(spacing: 0) {
                    Rectangle()
                        .fill(
                            LinearGradient(
                                colors: [SacredTheme.silkRed, Color(red: 0.42, green: 0.0, blue: 0.0), SacredTheme.silkRed],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                    Rectangle()
                        .fill(
                            LinearGradient(
                                colors: [SacredTheme.gold, Color(red: 0.72, green: 0.53, blue: 0.04), SacredTheme.gold],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .frame(width: 8)
                }
                .frame(width: width * 0.5)
                .position(x: (width * 0.25) - openOffset, y: proxy.size.height * 0.5)
                .animation(.spring(response: 1.0, dampingFraction: 0.8), value: isClosed)

                // Right Curtain
                HStack(spacing: 0) {
                    Rectangle()
                        .fill(
                            LinearGradient(
                                colors: [SacredTheme.gold, Color(red: 0.72, green: 0.53, blue: 0.04), SacredTheme.gold],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .frame(width: 8)
                    Rectangle()
                        .fill(
                            LinearGradient(
                                colors: [SacredTheme.silkRed, Color(red: 0.42, green: 0.0, blue: 0.0), SacredTheme.silkRed],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                }
                .frame(width: width * 0.5)
                .position(x: (width * 0.75) + openOffset, y: proxy.size.height * 0.5)
                .animation(.spring(response: 1.0, dampingFraction: 0.8), value: isClosed)

                // Purity Dialog Modal
                if isClosed && !isCleanlinessConfirmed {
                    VStack(spacing: 16) {
                        Text("ॐ शुद्धि संकल्प ॐ")
                            .font(.title3.bold())
                            .foregroundColor(SacredTheme.gold)

                        Text("क्या आपने स्नान कर शुद्धि प्राप्त कर ली है?\n(Have you bathed & cleansed yourself?)")
                            .font(.subheadline)
                            .foregroundColor(SacredTheme.textPrimary)
                            .multilineTextAlignment(.center)

                        Text("मंदिर में प्रवेश करने से पूर्व तन और मन की शुचिता अनिवार्य है।")
                            .font(.caption)
                            .foregroundColor(SacredTheme.textSecondary)
                            .multilineTextAlignment(.center)

                        if showSnanMantra {
                            Text("गङ्गे च यमुने चैव गोदावरि सरस्वति।\nनर्मदे सिन्धु कावेरि जलेऽस्मिन् संनिधिं कुरु॥")
                                .font(.caption.bold())
                                .foregroundColor(SacredTheme.gold)
                                .padding(10)
                                .background(Color.black.opacity(0.4))
                                .cornerRadius(8)
                                .multilineTextAlignment(.center)
                        }

                        HStack(spacing: 12) {
                            Button(action: { showSnanMantra.toggle() }) {
                                Text(showSnanMantra ? "मंत्र छिपाएं" : "स्नान मंत्र")
                                    .font(.caption.bold())
                                    .foregroundColor(SacredTheme.gold)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(SacredTheme.brass, lineWidth: 1)
                                    )
                            }

                            Button(action: onConfirmPurity) {
                                Text("हाँ, मैं शुद्ध हूँ")
                                    .font(.caption.bold())
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                                    .background(SacredTheme.deepSaffron)
                                    .cornerRadius(8)
                            }
                        }
                    }
                    .padding(20)
                    .background(SacredTheme.teakWood.opacity(0.96))
                    .cornerRadius(16)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(SacredTheme.gold, lineWidth: 1.5)
                    )
                    .padding(.horizontal, 28)
                    .shadow(radius: 20)
                }
            }
        }
    }
}
