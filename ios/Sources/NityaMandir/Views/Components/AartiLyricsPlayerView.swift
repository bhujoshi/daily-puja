import SwiftUI

public struct AartiLyricsPlayerView: View {
    public let titleHi: String
    public let titleEn: String
    public let lyrics: [AartiLyric]
    public let isHindi: Bool
    public let onComplete: () -> Void

    @State private var currentLineIndex: Int = 0
    @State private var isPlaying: Bool = true

    public init(
        titleHi: String,
        titleEn: String,
        lyrics: [AartiLyric],
        isHindi: Bool,
        onComplete: @escaping () -> Void
    ) {
        self.titleHi = titleHi
        self.titleEn = titleEn
        self.lyrics = lyrics
        self.isHindi = isHindi
        self.onComplete = onComplete
    }

    public var body: some View {
        VStack(spacing: 12) {
            // Header
            HStack {
                Image(systemName: "music.note")
                    .foregroundColor(SacredTheme.gold)
                Text(isHindi ? titleHi : titleEn)
                    .font(.headline.bold())
                    .foregroundColor(SacredTheme.gold)
                Spacer()
                Button(action: { isPlaying.toggle() }) {
                    Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                        .foregroundColor(SacredTheme.gold)
                }
            }

            // Synced Scrolling Lyrics
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(spacing: 8) {
                        ForEach(Array(lyrics.enumerated()), id: \.offset) { index, lyric in
                            let isCurrent = index == currentLineIndex
                            VStack(spacing: 2) {
                                Text(isHindi ? lyric.lineHi : lyric.lineEn)
                                    .font(isCurrent ? .subheadline.bold() : .caption)
                                    .foregroundColor(isCurrent ? SacredTheme.gold : SacredTheme.textSecondary)
                                    .multilineTextAlignment(.center)

                                if !isHindi {
                                    Text(lyric.meaningEn)
                                        .font(.system(size: 10))
                                        .foregroundColor(Color.gray)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 4)
                            .background(isCurrent ? Color.yellow.opacity(0.15) : Color.clear)
                            .cornerRadius(6)
                            .id(index)
                        }
                    }
                }
                .frame(height: 120)
                .task {
                    while isPlaying && currentLineIndex < lyrics.count - 1 {
                        try? await Task.sleep(nanoseconds: 3_500_000_000)
                        if isPlaying && currentLineIndex < lyrics.count - 1 {
                            currentLineIndex += 1
                            withAnimation {
                                proxy.scrollTo(currentLineIndex, anchor: .center)
                            }
                        }
                    }
                }
            }

            Button(action: onComplete) {
                Text(isHindi ? "आरती पूर्ण व आशीर्वाद ग्रहण करें" : "Complete Aarti & Receive Blessings")
                    .font(.subheadline.bold())
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(SacredTheme.deepSaffron)
                    .cornerRadius(10)
            }
        }
        .padding(14)
        .background(SacredTheme.teakDark.opacity(0.96))
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(SacredTheme.gold, lineWidth: 1.5)
        )
        .padding(.horizontal, 16)
    }
}
