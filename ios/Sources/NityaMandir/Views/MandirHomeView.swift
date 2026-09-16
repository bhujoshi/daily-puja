import SwiftUI

public struct MandirHomeView: View {
    @State private var ritualManager = RitualManager()
    @State private var isHindi: Bool = true
    @State private var showDebug: Bool = false
    @State private var timeOffsetHours: Double = 0.0

    public init() {}

    private var effectiveDate: Date {
        Date().addingTimeInterval(timeOffsetHours * 3600.0)
    }

    private var agingState: AgingState {
        AgingEngine.calculateAging(
            lastWorshipDate: ritualManager.lastWorshipDate,
            lastCleanedDate: ritualManager.lastCleanedDate,
            currentDate: effectiveDate
        )
    }

    public var body: some View {
        ZStack {
            // Main Mandir Altar
            MandirAltarView(
                ganeshDeity: ritualManager.ganeshDeity,
                lakshmiDeity: ritualManager.lakshmiDeity,
                isDiyaLit: ritualManager.isDiyaLit,
                dustLevel: agingState.dustLevel,
                flowerWitherFactor: agingState.flowerWitherFactor,
                onGaneshTap: {
                    if ritualManager.currentStep == .tilakPushparpan {
                        if !ritualManager.ganeshDeity.tilakApplied {
                            ritualManager.applyTilak(deityId: "ganesh_ji")
                        } else {
                            ritualManager.offerFlower(deityId: "ganesh_ji")
                        }
                    }
                },
                onLakshmiTap: {
                    if ritualManager.currentStep == .tilakPushparpan {
                        if !ritualManager.lakshmiDeity.tilakApplied {
                            ritualManager.applyTilak(deityId: "lakshmi_ji")
                        } else {
                            ritualManager.offerFlower(deityId: "lakshmi_ji")
                        }
                    }
                }
            )

            // Hanging Brass Bells (Top Left & Top Right)
            VStack {
                HStack {
                    BrassBellView(onRing: { ritualManager.ringBell() })
                        .padding(.leading, 16)
                    Spacer()
                    BrassBellView(onRing: { ritualManager.ringBell() })
                        .padding(.trailing, 16)
                }
                .padding(.top, 70)
                Spacer()
            }

            // Sacred Shankh (Bottom Left)
            VStack {
                Spacer()
                HStack {
                    ShankhNaadView(
                        isBlowing: ritualManager.isShankhBlown,
                        onBlow: { ritualManager.blowShankh() }
                    )
                    .padding(.leading, 20)
                    .padding(.bottom, 160)
                    Spacer()
                }
            }

            // Top Bar
            VStack {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(isHindi ? "नित्य मंदिर" : "Nitya Mandir")
                            .font(.title3.bold())
                            .foregroundColor(SacredTheme.gold)
                        Text(isHindi ? "दैनिक साधना एवं उपासना" : "Daily Sacred Worship")
                            .font(.system(size: 10))
                            .foregroundColor(SacredTheme.textSecondary)
                    }

                    Spacer()

                    HStack(spacing: 8) {
                        // Streak Badge
                        Text("🪔 \(ritualManager.streakDays) \(isHindi ? "दिन" : "days")")
                            .font(.caption.bold())
                            .foregroundColor(SacredTheme.gold)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.orange.opacity(0.2))
                            .cornerRadius(8)

                        // Language Toggle
                        Button(action: { isHindi.toggle() }) {
                            Image(systemName: "character.book.closed.fill")
                                .foregroundColor(SacredTheme.gold)
                        }

                        // Curtain Toggle
                        Button(action: { ritualManager.toggleCurtains() }) {
                            Image(systemName: "curtains.closed")
                                .foregroundColor(SacredTheme.gold)
                        }

                        // Debug Harness Toggle
                        Button(action: { showDebug.toggle() }) {
                            Image(systemName: "wrench.and.screwdriver")
                                .foregroundColor(SacredTheme.brass)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)

                if showDebug {
                    TimeTravelDebugView(
                        currentOffsetHours: timeOffsetHours,
                        onSetOffset: { hours in
                            timeOffsetHours = hours
                            TimeTravelEngine.shared.setOffset(hours: hours)
                        },
                        onReset: {
                            timeOffsetHours = 0
                            TimeTravelEngine.shared.reset()
                            ritualManager.cleanMandir()
                        }
                    )
                    .padding(.top, 4)
                }

                Spacer()
            }

            // Bottom Ritual Panel
            VStack {
                Spacer()
                if agingState.needsCleaning {
                    CleaningView(onComplete: {
                        ritualManager.cleanMandir()
                    })
                } else if ritualManager.currentStep == .aartiStuti {
                    AartiLyricsPlayerView(
                        titleHi: "जय गणेश देवा",
                        titleEn: "Jai Ganesh Deva",
                        lyrics: [
                            AartiLyric(timestampMs: 0, lineHi: "जय गणेश, जय गणेश, जय गणेश देवा।", lineEn: "Jai Ganesh, Jai Ganesh, Jai Ganesh Deva.", meaningEn: "Glory to Lord Ganesha."),
                            AartiLyric(timestampMs: 6500, lineHi: "माता जाकी पार्वती, पिता महादेवा॥", lineEn: "Mata jaaki Parvati, Pita Mahadeva.", meaningEn: "Whose mother is Parvati, and father is Mahadeva."),
                            AartiLyric(timestampMs: 13000, lineHi: "एक दन्त दयावन्त, चार भुजाधारी।", lineEn: "Ek danta dayavanta, chaar bhujaadhaari.", meaningEn: "The single-tusked, compassionate Lord of four arms."),
                            AartiLyric(timestampMs: 19500, lineHi: "माथे पर तिलक सोहे, मूसे की सवारी॥", lineEn: "Maathe par tilak sohe, moose ki savaari.", meaningEn: "Adorned with sacred tilak, riding the mouse.")
                        ],
                        isHindi: isHindi,
                        onComplete: {
                            ritualManager.completeAarti()
                        }
                    )
                } else {
                    RitualStepBar(
                        currentStep: ritualManager.currentStep,
                        isHindi: isHindi,
                        onAction: {
                            switch ritualManager.currentStep {
                            case .samagriSangrah:
                                ritualManager.advanceStep()
                            case .deepaPrajwalan:
                                ritualManager.lightDiya()
                            case .devSnan:
                                ritualManager.completeAbhishek()
                            case .tilakPushparpan:
                                ritualManager.applyTilak(deityId: "ganesh_ji")
                                ritualManager.applyTilak(deityId: "lakshmi_ji")
                                ritualManager.offerFlower(deityId: "ganesh_ji")
                                ritualManager.offerFlower(deityId: "lakshmi_ji")
                            case .ghantiAarti:
                                ritualManager.ringBell()
                                ritualManager.ringBell()
                                ritualManager.ringBell()
                                ritualManager.advanceStep()
                            case .shankhNaad:
                                ritualManager.blowShankh()
                            case .bhogSamarpan:
                                ritualManager.offerBhog()
                            case .aartiStuti:
                                ritualManager.completeAarti()
                            }
                        }
                    )
                }
            }
            .padding(.bottom, 20)

            // Silk Curtains Overlay
            CurtainView(
                isClosed: ritualManager.curtainsClosed,
                isCleanlinessConfirmed: ritualManager.cleanlinessConfirmed,
                onConfirmPurity: {
                    ritualManager.confirmCleanliness()
                }
            )
        }
    }
}
