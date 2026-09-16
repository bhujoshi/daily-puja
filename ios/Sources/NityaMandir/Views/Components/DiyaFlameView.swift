import SwiftUI

public struct DiyaFlameView: View {
    public let isLit: Bool

    @State private var flicker = false

    public init(isLit: Bool) {
        self.isLit = isLit
    }

    public var body: some View {
        ZStack {
            // Brass Diya Base
            Canvas { context, size in
                let w = size.width
                let h = size.height
                let cx = w / 2
                let topY = h * 0.65

                var basePath = Path()
                basePath.move(to: CGPoint(x: cx - 25, y: topY))
                basePath.addCurve(
                    to: CGPoint(x: cx + 25, y: topY),
                    control1: CGPoint(x: cx - 30, y: h * 0.95),
                    control2: CGPoint(x: cx + 30, y: h * 0.95)
                )
                basePath.closeSubpath()

                context.fill(
                    basePath,
                    with: .linearGradient(
                        Gradient(colors: [SacredTheme.gold, SacredTheme.brass, Color(red: 0.55, green: 0.4, blue: 0.0)]),
                        startPoint: CGPoint(x: cx, y: topY),
                        endPoint: CGPoint(x: cx, y: h)
                    )
                )

                // Wick
                context.stroke(
                    Path { p in
                        p.move(to: CGPoint(x: cx, y: topY + 2))
                        p.addLine(to: CGPoint(x: cx, y: topY - 8))
                    },
                    with: .color(isLit ? Color(red: 0.17, green: 0.09, blue: 0.02) : Color.gray),
                    lineWidth: 3
                )
            }
            .frame(width: 80, height: 60)

            // Dynamic Animated Flame
            if isLit {
                VStack(spacing: 0) {
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [Color.orange.opacity(0.8), Color.red.opacity(0.4), Color.clear],
                                center: .center,
                                startRadius: 2,
                                endRadius: 30
                            )
                        )
                        .frame(width: 50, height: 50)
                        .scaleEffect(flicker ? 1.08 : 0.94)
                        .offset(y: -14)
                }
                .onAppear {
                    withAnimation(.easeInOut(duration: 0.25).repeatForever(autoreverses: true)) {
                        flicker.toggle()
                    }
                }
            }
        }
    }
}
