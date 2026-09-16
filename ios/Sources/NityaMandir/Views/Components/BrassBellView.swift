import SwiftUI

public struct BrassBellView: View {
    public let onRing: () -> Void

    @State private var swingAngle: Double = 0.0

    public init(onRing: @escaping () -> Void) {
        self.onRing = onRing
    }

    public var body: some View {
        Button(action: {
            SoundHapticEngine.shared.triggerBellHaptic()
            onRing()
            withAnimation(.spring(response: 0.15, dampingFraction: 0.2)) {
                swingAngle = 18.0
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                withAnimation(.spring(response: 0.25, dampingFraction: 0.4)) {
                    swingAngle = 0.0
                }
            }
        }) {
            Canvas { context, size in
                let w = size.width
                let cx = w / 2

                // Hanging Chain
                context.stroke(
                    Path { p in
                        p.move(to: CGPoint(x: cx, y: 0))
                        p.addLine(to: CGPoint(x: cx, y: 22))
                    },
                    with: .color(SacredTheme.gold),
                    lineWidth: 3
                )

                // Bell Body
                var bell = Path()
                bell.move(to: CGPoint(x: cx - 6, y: 22))
                bell.addCurve(
                    to: CGPoint(x: cx - 20, y: 55),
                    control1: CGPoint(x: cx - 8, y: 32),
                    control2: CGPoint(x: cx - 18, y: 46)
                )
                bell.addLine(to: CGPoint(x: cx + 20, y: 55))
                bell.addCurve(
                    to: CGPoint(x: cx + 6, y: 22),
                    control1: CGPoint(x: cx + 18, y: 46),
                    control2: CGPoint(x: cx + 8, y: 32)
                )
                bell.closeSubpath()

                context.fill(
                    bell,
                    with: .linearGradient(
                        Gradient(colors: [SacredTheme.gold, SacredTheme.brass, Color(red: 0.55, green: 0.4, blue: 0.0)]),
                        startPoint: CGPoint(x: cx - 20, y: 22),
                        endPoint: CGPoint(x: cx + 20, y: 55)
                    )
                )

                // Bell Clapper
                context.fill(
                    Path(ellipseIn: CGRect(x: cx - 4, y: 56, width: 8, height: 8)),
                    with: .color(SacredTheme.gold)
                )
            }
            .frame(width: 50, height: 75)
            .rotationEffect(.degrees(swingAngle), anchor: .top)
        }
        .buttonStyle(.plain)
    }
}
