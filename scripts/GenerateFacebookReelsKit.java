import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

/**
 * Generates the complete zero-budget Facebook Reels Launch Kit for Pavitra Mandir (com.pavitramandir.app):
 *  1. Facebook Page Profile Avatar (1024x1024 PNG) & Cover Banner (1640x924 PNG)
 *  2. 7 Animated 1080x1920 (9:16, 24fps, 13.5s) MP4 Reels (Monday..Sunday) with synced temple audio
 *  3. 7 Matching Reel Cover Thumbnails (1080x1920 PNG)
 */
public class GenerateFacebookReelsKit {

    private static final File ROOT = new File("/Users/bhuwanjoshi/work/worship");
    private static final File SHRINE_DIR = new File(ROOT, "android/app/src/main/assets/shrine");
    private static final File RAW_AUDIO_DIR = new File(ROOT, "android/app/src/main/res/raw");
    private static final File LAUNCHER_ART = new File(ROOT, "temple_models/source/launcher_artwork.png");
    private static final File OUT_DIR = new File(ROOT, "marketing/facebook_reels");
    private static final File PAGE_DIR = new File(OUT_DIR, "page_assets");
    private static final File VIDEO_DIR = new File(OUT_DIR, "videos");
    private static final File COVER_DIR = new File(OUT_DIR, "covers");
    private static final File AUDIO_TMP_DIR = new File("/tmp/pavitra_reels_audio");

    // Garbhagriha Chiaroscuro Palette (pavitra-mandir-design)
    private static final Color OBSIDIAN_UMBER = new Color(0x0D, 0x0B, 0x0A);
    private static final Color SACRED_GOLD = new Color(0xD8, 0xAF, 0x52);
    private static final Color HONEY_SAFFRON = new Color(0xFF, 0x9E, 0x2C);
    private static final Color KARPURA_IVORY = new Color(0xFF, 0xFD, 0xF8);
    private static final Color SANDALWOOD_DUST = new Color(0xCF, 0xC2, 0xB2);
    private static final Color RITUAL_INK = new Color(0x26, 0x1C, 0x16);
    private static final Color RITUAL_GOLD = new Color(0xB3, 0x83, 0x3B);

    private static final int REEL_W = 1080;
    private static final int REEL_H = 1920;
    private static final int FPS = 24;
    private static final float DURATION_SEC = 13.5f;
    private static final int TOTAL_FRAMES = Math.round(FPS * DURATION_SEC); // 324 frames

    private static final Map<String, BufferedImage> IMAGE_CACHE = new HashMap<>();

    private record ReelSpec(
        String slug,
        String dayBadge,
        String hookHeadlineHi,
        String hookSubtitle,
        String shrineId,
        String idolId,
        String garlandId,
        String lampId,
        String incenseId,
        String aartiId,
        String shankhId,
        String prasadId,
        String mode, // "abhishek", "aarti", "shodhan", "shankh", "adfree", "sandhya", "eightstep"
        String commentPromptHi,
        String bottomSubCta
    ) {}

    private static final List<ReelSpec> REELS = List.of(
        new ReelSpec(
            "01_monday_shiva_abhishek",
            "सोमवार महादेव दर्शन • MONDAY PUJA",
            "घर बैठे करें शिव रुद्राभिषेक व आरती",
            "100% विज्ञापन-मुक्त (Ad-Free) पवित्र मंदिर ऐप",
            "ivory", "shiva", "mogra", "brass", "dhoop", "traditional", "classic", "halwa",
            "abhishek",
            "कमेंट में 'हर हर महादेव' लिखें",
            "निशुल्क ऐप लिंक पहले कमेंट (Pinned Comment) में है"
        ),
        new ReelSpec(
            "02_tuesday_hanuman_aarti",
            "मंगलवार बजरंगबली दर्शन • TUESDAY AARTI",
            "संकटमोचन हनुमान जी की दिव्य आरती",
            "बिना किसी विज्ञापन के शांत नित्य पूजा व चालीसा",
            "carved", "hanuman", "marigold", "brass", "agarbatti", "golden", "swarna", "laddu_bowl",
            "aarti",
            "कमेंट में 'जय बजरंगबली' लिखें",
            "निशुल्क ऐप लिंक पहले कमेंट (Pinned Comment) में है"
        ),
        new ReelSpec(
            "03_wednesday_ganesha_shodhan",
            "प्रातः मंदिर शुद्धि • MORNING SHODHAN",
            "असली मंदिर की तरह पुराने फूल हटाएँ",
            "ताज़े फूलों से शृंगार करें और मोदक भोग लगाएँ",
            "divya_darshan", "original", "rose_jasmine", "peacock", "kamal_dhoop", "golden", "swarna", "modak",
            "shodhan",
            "कमेंट में 'गणपति बप्पा मोरया' लिखें",
            "निशुल्क ऐप लिंक पहले कमेंट (Pinned Comment) में है"
        ),
        new ReelSpec(
            "04_thursday_radha_krishna_shankh",
            "गुरुवार राधा-कृष्ण दर्शन • 432 Hz SHANKH",
            "राधा-कृष्ण दर्शन व ४३२ Hz शंखनाद",
            "मकराना संगमरमर मंदिर में बांसुरी व दिव्य आरती",
            "marble", "radha_krishna", "mogra", "golden", "mayur_dhoop", "karpura", "swarna", "halwa",
            "shankh",
            "कमेंट में 'राधे राधे' लिखें",
            "निशुल्क ऐप लिंक पहले कमेंट (Pinned Comment) में है"
        ),
        new ReelSpec(
            "05_friday_lakshmi_adfree",
            "०% विज्ञापन (ZERO ADS) • माता-पिता के लिए सुरक्षित",
            "आरती के बीच Ads से परेशान हैं?",
            "स्वर्ण गर्भगृह में करें १००% विज्ञापन-मुक्त लक्ष्मी पूजा",
            "swarna", "lakshmi", "lotus_pearl", "peacock", "kamal_dhoop", "golden", "swarna", "laddu_bowl",
            "adfree",
            "कमेंट में 'जय माँ लक्ष्मी' लिखें",
            "अपने व माता-पिता के फ़ोन के लिए पहले कमेंट से डाउनलोड करें"
        ),
        new ReelSpec(
            "06_saturday_sandhya_deepdan",
            "शनिवार संध्या आरती • EVENING DEEPDAN",
            "शांत संध्या दीपदान, लोबान धूप व घंटी",
            "दिन भर की थकान मिटाएँ — २ मिनट की आत्मिक शांति",
            "kamal_peeth", "ganesh_hanuman", "marigold", "brass", "dhoop", "karpura", "classic", "laddu",
            "sandhya",
            "कमेंट में 'जय श्री गणेश-हनुमान' लिखें",
            "निशुल्क ऐप लिंक पहले कमेंट (Pinned Comment) में है"
        ),
        new ReelSpec(
            "07_sunday_ram_darbar_8step",
            "घर से दूर हॉस्टल या यात्रा में? • 8-STEP PUJA",
            "२ मिनट में सम्पूर्ण ८-चरणीय नित्य पूजा",
            "श्री राम दरबार: घंटी • दीप • अभिषेक • तिलक • पुष्प • आरती • शंख • भोग",
            "diwali", "ram_darbar", "marigold", "peacock", "kamal_dhoop", "golden", "swarna", "laddu_bowl",
            "eightstep",
            "कमेंट में 'जय श्री राम' लिखें",
            "निशुल्क ऐप लिंक पहले कमेंट (Pinned Comment) में है"
        )
    );

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        PAGE_DIR.mkdirs();
        VIDEO_DIR.mkdirs();
        COVER_DIR.mkdirs();
        AUDIO_TMP_DIR.mkdirs();

        System.out.println("1/3 Generating Facebook Page Profile & Cover Graphics...");
        generatePageProfile1024();
        generatePageCover1640x924();

        System.out.println("2/3 Rendering 7 Animated 1080x1920 HD Facebook Reels + Synced Temple Audio...");
        byte[] rgbRowBuf = new byte[REEL_W * REEL_H * 3];

        for (int i = 0; i < REELS.size(); i++) {
            ReelSpec spec = REELS.get(i);
            System.out.printf("  -> [%d/7] Building %s.mp4 ...%n", i + 1, spec.slug());

            File audioWav = new File(AUDIO_TMP_DIR, spec.slug() + ".wav");
            buildRitualAudioTrack(spec, audioWav);

            File mp4Out = new File(VIDEO_DIR, spec.slug() + ".mp4");
            File coverOut = new File(COVER_DIR, spec.slug() + "_cover.png");

            List<String> cmd = List.of(
                "/opt/homebrew/bin/ffmpeg", "-y",
                "-loglevel", "error",
                "-f", "rawvideo",
                "-pix_fmt", "rgb24",
                "-s", REEL_W + "x" + REEL_H,
                "-r", String.valueOf(FPS),
                "-i", "-",
                "-i", audioWav.getAbsolutePath(),
                "-c:v", "libx264",
                "-preset", "fast",
                "-crf", "20",
                "-pix_fmt", "yuv420p",
                "-profile:v", "high",
                "-movflags", "+faststart",
                "-c:a", "aac",
                "-b:a", "192k",
                "-ar", "44100",
                "-ac", "2",
                "-shortest",
                mp4Out.getAbsolutePath()
            );

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process proc = pb.start();

            BufferedImage frameImg = new BufferedImage(REEL_W, REEL_H, BufferedImage.TYPE_INT_RGB);
            int[] pixels = ((DataBufferInt) frameImg.getRaster().getDataBuffer()).getData();

            try (OutputStream out = new BufferedOutputStream(proc.getOutputStream(), 1024 * 1024)) {
                for (int f = 0; f < TOTAL_FRAMES; f++) {
                    float t = f / (float) FPS;
                    Graphics2D g = frameImg.createGraphics();
                    configureQuality(g);
                    renderReelFrame(g, REEL_W, REEL_H, spec, t, f);
                    g.dispose();

                    if (f == Math.round(7.2f * FPS)) {
                        savePng(frameImg, coverOut);
                    }

                    int bi = 0;
                    for (int p = 0; p < pixels.length; p++) {
                        int rgb = pixels[p];
                        rgbRowBuf[bi++] = (byte) ((rgb >> 16) & 0xFF);
                        rgbRowBuf[bi++] = (byte) ((rgb >> 8) & 0xFF);
                        rgbRowBuf[bi++] = (byte) (rgb & 0xFF);
                    }
                    out.write(rgbRowBuf);
                }
            }

            int code = proc.waitFor();
            if (code != 0) {
                throw new RuntimeException("ffmpeg failed for " + spec.slug() + " with exit code " + code);
            }
            System.out.printf("     [OK] %s (%.2f MB)%n", mp4Out.getName(), mp4Out.length() / (1024.0 * 1024.0));
        }

        System.out.println("3/3 All Facebook Reels & Page Assets generated successfully!");
    }

    private static void buildRitualAudioTrack(ReelSpec spec, File outWav) throws Exception {
        String bell = new File(RAW_AUDIO_DIR, "bell.ogg").getAbsolutePath();
        String water = new File(RAW_AUDIO_DIR, "water_offering.ogg").getAbsolutePath();
        String flower = new File(RAW_AUDIO_DIR, "flower_offering.ogg").getAbsolutePath();
        String flute = new File(RAW_AUDIO_DIR, "mindfulness_flute.ogg").getAbsolutePath();
        String note = new File(RAW_AUDIO_DIR, "mindfulness_note.ogg").getAbsolutePath();
        String conch = new File(RAW_AUDIO_DIR, "conch.ogg").getAbsolutePath();

        String filter;
        switch (spec.mode()) {
            case "abhishek" -> filter =
                "[0:a]aresample=44100,pan=stereo|c0=0.85*c0|c1=0.85*c0,adelay=60|60[b1];" +
                "[1:a]aresample=44100,pan=stereo|c0=0.95*c0|c1=0.95*c0,adelay=900|900[w1];" +
                "[3:a]aresample=44100,pan=stereo|c0=0.65*c0|c1=0.65*c0,adelay=3800|3800[fl];" +
                "[0:a]aresample=44100,pan=stereo|c0=0.65*c0|c1=0.65*c0,adelay=5800|5800[b2];" +
                "[5:a]aresample=44100,atrim=0:6.0,afade=t=in:st=0:d=0.4,afade=t=out:st=4.6:d=1.3,volume=0.72,adelay=7400|7400[sh];" +
                "[b1][w1][fl][b2][sh]amix=inputs=5:normalize=0,alimiter=limit=0.92,afade=t=out:st=12.6:d=0.9[out]";
            case "shankh" -> filter =
                "[3:a]aresample=44100,pan=stereo|c0=0.90*c0|c1=0.90*c0,adelay=80|80[fl1];" +
                "[0:a]aresample=44100,pan=stereo|c0=0.75*c0|c1=0.75*c0,adelay=1800|1800[b1];" +
                "[2:a]aresample=44100,pan=stereo|c0=0.80*c0|c1=0.80*c0,adelay=3500|3500[fw];" +
                "[5:a]aresample=44100,atrim=0:8.5,afade=t=in:st=0:d=0.35,afade=t=out:st=7.0:d=1.4,volume=0.85,adelay=4800|4800[sh];" +
                "[fl1][b1][fw][sh]amix=inputs=4:normalize=0,alimiter=limit=0.92,afade=t=out:st=12.6:d=0.9[out]";
            case "shodhan" -> filter =
                "[4:a]aresample=44100,pan=stereo|c0=0.80*c0|c1=0.80*c0,adelay=200|200[n1];" +
                "[0:a]aresample=44100,pan=stereo|c0=0.90*c0|c1=0.90*c0,adelay=3600|3600[b1];" +
                "[2:a]aresample=44100,pan=stereo|c0=0.90*c0|c1=0.90*c0,adelay=4200|4200[fw];" +
                "[3:a]aresample=44100,pan=stereo|c0=0.75*c0|c1=0.75*c0,adelay=6000|6000[fl];" +
                "[5:a]aresample=44100,atrim=0:4.8,afade=t=in:st=0:d=0.35,afade=t=out:st=3.5:d=1.2,volume=0.70,adelay=8600|8600[sh];" +
                "[n1][b1][fw][fl][sh]amix=inputs=5:normalize=0,alimiter=limit=0.92,afade=t=out:st=12.6:d=0.9[out]";
            default -> filter =
                "[0:a]aresample=44100,pan=stereo|c0=0.88*c0|c1=0.88*c0,adelay=50|50[b1];" +
                "[2:a]aresample=44100,pan=stereo|c0=0.85*c0|c1=0.85*c0,adelay=2400|2400[fw];" +
                "[0:a]aresample=44100,pan=stereo|c0=0.72*c0|c1=0.72*c0,adelay=4300|4300[b2];" +
                "[3:a]aresample=44100,pan=stereo|c0=0.72*c0|c1=0.72*c0,adelay=5500|5500[fl];" +
                "[5:a]aresample=44100,atrim=0:5.6,afade=t=in:st=0:d=0.35,afade=t=out:st=4.2:d=1.3,volume=0.75,adelay=7800|7800[sh];" +
                "[b1][fw][b2][fl][sh]amix=inputs=5:normalize=0,alimiter=limit=0.92,afade=t=out:st=12.6:d=0.9[out]";
        }

        List<String> cmd = List.of(
            "/opt/homebrew/bin/ffmpeg", "-y", "-loglevel", "error",
            "-i", bell,
            "-i", water,
            "-i", flower,
            "-i", flute,
            "-i", note,
            "-i", conch,
            "-filter_complex", filter,
            "-map", "[out]",
            "-t", String.valueOf(DURATION_SEC),
            "-ar", "44100", "-ac", "2",
            outWav.getAbsolutePath()
        );

        Process p = new ProcessBuilder(cmd).inheritIO().start();
        if (p.waitFor() != 0) {
            throw new RuntimeException("Audio mix failed for " + spec.slug());
        }
    }

    // =========================================================================
    // Frame Renderer (1080x1920 9:16 Vertical Reel)
    // =========================================================================
    private static void renderReelFrame(Graphics2D g, int w, int h, ReelSpec spec, float t, int frameIdx) throws IOException {
        g.setColor(OBSIDIAN_UMBER);
        g.fillRect(0, 0, w, h);

        // Smooth sacred camera breathing zoom (1.05 -> 1.11)
        float progress = t / DURATION_SEC;
        float cameraZoom = 1.05f + 0.065f * progress;

        // Mode-specific dynamic ritual states
        float witherFactor = 0f;
        boolean litFlames = true;
        int litWickCount = 5;
        boolean showAbhishek = false;
        float abhishekProgress = 0f;
        boolean activeShankh = false;
        float shankhWavePhase = 0f;
        float aartiOrbitProgress = -1f; // -1 = resting on plate
        float bellSwingDeg = 0f;
        float petalIntensity = 0.5f;

        if (t < 3.0f) {
            bellSwingDeg = (float) (Math.sin(t * 9.5) * 16.0 * Math.exp(-t * 0.65));
        } else if (t >= 4.2f && t < 7.0f) {
            float bt = t - 4.2f;
            bellSwingDeg = (float) (Math.sin(bt * 8.5) * 12.0 * Math.exp(-bt * 0.8));
        }

        switch (spec.mode()) {
            case "abhishek" -> {
                showAbhishek = (t >= 0.8f && t <= 5.2f);
                abhishekProgress = Math.max(0f, Math.min(1f, (t - 0.8f) / 4.2f));
                if (t >= 5.0f && t <= 10.5f) {
                    aartiOrbitProgress = (t - 5.0f) / 5.5f;
                }
                if (t >= 7.4f) {
                    activeShankh = true;
                    shankhWavePhase = (t - 7.4f) * 1.4f;
                }
                petalIntensity = t > 4.5f ? 1.0f : 0.35f;
            }
            case "shodhan" -> {
                if (t < 3.6f) {
                    witherFactor = 1.0f;
                    litFlames = false;
                    petalIntensity = 0f;
                } else if (t < 4.6f) {
                    witherFactor = 1.0f - (t - 3.6f);
                    litFlames = true;
                    petalIntensity = 1.2f;
                } else {
                    witherFactor = 0f;
                    litFlames = true;
                    petalIntensity = 1.0f;
                }
                if (t >= 5.2f && t <= 10.8f) {
                    aartiOrbitProgress = (t - 5.2f) / 5.6f;
                }
                if (t >= 8.6f) {
                    activeShankh = true;
                    shankhWavePhase = (t - 8.6f) * 1.4f;
                }
            }
            case "shankh" -> {
                petalIntensity = 0.85f;
                if (t >= 1.5f && t <= 6.2f) {
                    aartiOrbitProgress = (t - 1.5f) / 4.7f;
                }
                if (t >= 4.8f) {
                    activeShankh = true;
                    shankhWavePhase = (t - 4.8f) * 1.55f;
                }
            }
            case "adfree" -> {
                litWickCount = Math.min(5, 1 + (int) (t * 1.8f));
                petalIntensity = 1.0f;
                if (t >= 3.0f && t <= 9.8f) {
                    aartiOrbitProgress = (t - 3.0f) / 6.8f;
                }
                if (t >= 7.8f) {
                    activeShankh = true;
                    shankhWavePhase = (t - 7.8f) * 1.4f;
                }
            }
            case "eightstep" -> {
                showAbhishek = (t >= 3.2f && t < 5.0f);
                abhishekProgress = (t - 3.2f) / 1.8f;
                petalIntensity = (t >= 6.4f) ? 1.25f : 0.35f;
                if (t >= 8.0f && t <= 11.2f) {
                    aartiOrbitProgress = (t - 8.0f) / 3.2f;
                }
                if (t >= 10.2f) {
                    activeShankh = true;
                    shankhWavePhase = (t - 10.2f) * 1.6f;
                }
            }
            default -> {
                petalIntensity = 0.9f;
                if (t >= 2.8f && t <= 9.5f) {
                    aartiOrbitProgress = (t - 2.8f) / 6.7f;
                }
                if (t >= 7.8f) {
                    activeShankh = true;
                    shankhWavePhase = (t - 7.8f) * 1.4f;
                }
            }
        }

        // Draw temple upper stone/umbra backdrop at top, then shift shrine down 95px so crowns sit cleanly below top hook
        BufferedImage topBg = loadAsset("backgrounds/" + spec.shrineId() + ".png");
        g.drawImage(topBg, 0, 0, w, 320, 0, 0, topBg.getWidth(), Math.max(1, topBg.getHeight() / 5), null);

        int sceneTopY = 95;
        Graphics2D sg = (Graphics2D) g.create();
        sg.translate(0, sceneTopY);
        renderAnimatedAltar(
            sg, w, h - sceneTopY, cameraZoom, t, frameIdx,
            spec.shrineId(), spec.idolId(), spec.garlandId(),
            spec.lampId(), spec.incenseId(), spec.aartiId(),
            spec.shankhId(), spec.prasadId(),
            litFlames, litWickCount, showAbhishek, abhishekProgress,
            aartiOrbitProgress, activeShankh, shankhWavePhase,
            bellSwingDeg, witherFactor, petalIntensity
        );
        sg.dispose();

        // Shodhan golden purification sweep at t = 3.5..4.5s
        if (spec.mode().equals("shodhan") && t >= 3.4f && t <= 4.7f) {
            float sp = (t - 3.4f) / 1.3f;
            int sweepY = Math.round(350 + sp * 1100);
            RadialGradientPaint wave = new RadialGradientPaint(
                new Point2D.Float(w / 2f, sweepY), 460f,
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(255, 236, 150, 125), new Color(255, 185, 65, 55), new Color(255, 185, 65, 0)}
            );
            g.setPaint(wave);
            g.fillRect(0, Math.max(0, sweepY - 460), w, 920);
        }

        // Top & Bottom Garbhagriha Vignettes for crystal-clear Reels text legibility
        GradientPaint topVig = new GradientPaint(0, 0, new Color(10, 7, 5, 238), 0, 410, new Color(10, 7, 5, 0));
        g.setPaint(topVig);
        g.fillRect(0, 0, w, 410);

        GradientPaint botVig = new GradientPaint(0, h - 590, new Color(10, 7, 5, 0), 0, h - 180, new Color(10, 7, 5, 238));
        g.setPaint(botVig);
        g.fillRect(0, h - 590, w, 590);

        // Top Hook Card (Safe Zone: y = 138..320, completely above deity crowns)
        drawTopReelHook(g, w, spec, t);

        // Mode-specific dynamic step badge tucked neatly just above the bottom CTA card (never near deity crown!)
        int badgeCenterX = 54 + (w - 168) / 2;
        if (spec.mode().equals("eightstep")) {
            int stepIdx = Math.min(7, (int) (t / (DURATION_SEC / 8f)));
            String[] steps = {
                "चरण १/८ • मंदिर घंटी नाद",
                "चरण २/८ • मयूर दीप एवं कमल धूप",
                "चरण ३/८ • ताम्र कलश अभिषेक",
                "चरण ४/८ • केसर-चंदन व कुमकुम तिलक",
                "चरण ५/८ • ताज़ा गेंदा पुष्पांजलि",
                "चरण ६/८ • स्वर्ण पंच-आरती",
                "चरण ७/८ • ४३२ Hz पाञ्चजन्य शंखनाद",
                "चरण ८/८ • मोतीचूर लड्डू भोग समर्पण"
            };
            drawFloatingPill(g, badgeCenterX, 1402, steps[stepIdx], true);
        } else if (spec.mode().equals("shodhan")) {
            String stateLabel = t < 3.6f
                ? "१. रात्रि के मुरझाए निर्माल्य फूल हटाएँ"
                : "२. ताज़ा गुलाब-चमेली माला व दिव्य आरती";
            drawFloatingPill(g, badgeCenterX, 1402, stateLabel, t >= 3.6f);
        } else if (spec.mode().equals("adfree")) {
            drawFloatingPill(g, badgeCenterX, 1402, "✓ ०% विज्ञापन  •  ✓ ८ देवी-देवता  •  ✓ बड़े स्पष्ट बटन", true);
        }

        // Bottom Safe-Zone Call-To-Action Card (y = 1430..1625, safely above FB caption overlay & left of right icons)
        drawBottomReelCta(g, w, spec, t);
    }

    private static void renderAnimatedAltar(
        Graphics2D g, int width, int height, float zoom, float t, int frameIdx,
        String shrineId, String idolId, String garlandId,
        String lampId, String incenseId, String aartiId,
        String shankhId, String prasadId,
        boolean litFlames, int litWickCount, boolean showAbhishek, float abhishekProgress,
        float aartiOrbitProgress, boolean activeShankh, float shankhWavePhase,
        float bellSwingDeg, float witherFactor, float petalIntensity
    ) throws IOException {
        float imageRatio = 1672f / 940f;
        float imageWidth = Math.max((float) width, (float) height / imageRatio) * zoom;
        float leftOffset = (width - imageWidth) / 2f;

        // 1. Shrine Background
        BufferedImage bg = loadAsset("backgrounds/" + shrineId + ".png");
        int bgW = Math.round(imageWidth);
        int bgH = Math.round(imageWidth * ((float) bg.getHeight() / bg.getWidth()));
        int bgX = Math.round(leftOffset);
        g.drawImage(bg, bgX, 0, bgW, bgH, null);

        // Warm sanctuary ambient light pulse when diyas are lit
        if (litFlames) {
            float pulse = 0.88f + 0.12f * (float) Math.sin(t * 3.4);
            RadialGradientPaint sanctumGlow = new RadialGradientPaint(
                new Point2D.Float(width / 2f, imageWidth * 0.58f), imageWidth * 0.52f,
                new float[]{0f, 0.6f, 1f},
                new Color[]{
                    new Color(255, 182, 66, Math.round(48 * pulse)),
                    new Color(216, 120, 28, Math.round(22 * pulse)),
                    new Color(13, 11, 10, 0)
                }
            );
            g.setPaint(sanctumGlow);
            g.fillRect(0, 0, width, height);
        }

        // 2. Garland Layer (dims slightly when withered in Morning Shodhan)
        String garlandPath = switch (garlandId) {
            case "rose_jasmine" -> "garlands/rose_jasmine.png";
            case "lotus_pearl" -> "garlands/lotus_pearl.png";
            case "mogra" -> "garlands/mogra.png";
            default -> "garlands/marigold.png";
        };
        BufferedImage garland = loadAsset(garlandPath);
        float fLeft = shrineId.equals("carved") ? 0.140f : 0.145f;
        float fTop = 0.025f;
        float fRight = shrineId.equals("carved") ? 0.860f : 0.855f;
        float fBottom = 0.655f;
        float pillarW = 0.088f;
        float topH = 0.165f;

        int sw = garland.getWidth(), sh = garland.getHeight();
        int sideSrcW = Math.max(1, (int) (sw * 0.125f));
        int topSrcX = (int) (sw * 0.1065f);
        int topSrcW = Math.max(1, (int) (sw * (1f - 2f * 0.1065f)));
        int topSrcH = Math.max(1, (int) (sh * 0.275f));

        int topLeftX = Math.round(leftOffset + fLeft * imageWidth);
        int topLeftY = Math.round(fTop * imageWidth);
        int rightPillarX = Math.round(leftOffset + (fRight - pillarW) * imageWidth);
        int sideDstW = Math.max(1, Math.round(pillarW * imageWidth));
        int sideDstH = Math.max(1, Math.round((fBottom - fTop) * imageWidth));

        Composite oldGarlandComp = g.getComposite();
        if (witherFactor > 0.05f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - 0.55f * witherFactor));
        }
        g.drawImage(garland, topLeftX, topLeftY, topLeftX + sideDstW, topLeftY + sideDstH, 0, 0, sideSrcW, sh, null);
        g.drawImage(garland, rightPillarX, topLeftY, rightPillarX + sideDstW, topLeftY + sideDstH, sw - sideSrcW, 0, sw, sh, null);

        float archLeft = fLeft + pillarW * 0.85f;
        float archRight = fRight - pillarW * 0.85f;
        int archX = Math.round(leftOffset + archLeft * imageWidth);
        int archW = Math.max(1, Math.round((archRight - archLeft) * imageWidth));
        int archH = Math.max(1, Math.round(topH * imageWidth));
        g.drawImage(garland, archX, topLeftY, archX + archW, topLeftY + archH, topSrcX, 0, topSrcX + topSrcW, topSrcH, null);
        g.setComposite(oldGarlandComp);

        // 3. Idol Placement & Animated Breathing Prabhamandal Halos (behind deity crowns)
        BufferedImage idol = loadAsset("idols/" + idolId + ".png");
        boolean singleDeity = idolId.equals("shiva") || idolId.equals("lakshmi") || idolId.equals("durga") || idolId.equals("hanuman") || idolId.equals("ram_darbar");
        float aspect = switch (idolId) {
            case "ganesh_hanuman" -> 930f / 940f;
            case "original" -> 928f / 914f;
            case "radha_krishna" -> 650f / 944f;
            case "shiva" -> 725f / 978f;
            case "lakshmi" -> 726f / 985f;
            case "durga" -> 707f / 968f;
            case "ram_darbar" -> 723f / 924f;
            case "hanuman" -> 609f / 969f;
            default -> 723f / 924f;
        };
        float maxW = singleDeity && !idolId.equals("ram_darbar") ? 0.38f : 0.46f;
        float maxH = 0.395f;
        float idolW = Math.min(maxW, maxH * aspect);
        float idolH = idolW / aspect;
        float idolBottom = 0.680f;
        float idolLeft = 0.5f - idolW / 2f;
        float idolTop = idolBottom - idolH;

        float[][] foreheads = switch (idolId) {
            case "original" -> new float[][]{{0.320f, 0.215f}, {0.710f, 0.218f}};
            case "ganesh_hanuman" -> new float[][]{{0.272f, 0.218f}, {0.765f, 0.205f}};
            case "radha_krishna" -> new float[][]{{0.330f, 0.222f}, {0.650f, 0.198f}};
            default -> new float[][]{{0.500f, 0.200f}};
        };

        float haloPulse = 1.0f + 0.055f * (float) Math.sin(t * 2.6);
        for (float[] fh : foreheads) {
            float hx = leftOffset + (idolLeft + fh[0] * idolW) * imageWidth;
            float hy = (idolTop + fh[1] * idolH) * imageWidth;
            float haloR = imageWidth * 0.132f * haloPulse;
            RadialGradientPaint haloGrad = new RadialGradientPaint(
                new Point2D.Float(hx, hy), haloR,
                new float[]{0f, 0.55f, 0.82f, 1f},
                new Color[]{
                    new Color(255, 251, 227, 170),
                    new Color(255, 231, 119, 105),
                    new Color(255, 202, 58, 48),
                    new Color(255, 202, 58, 0)
                }
            );
            g.setPaint(haloGrad);
            g.fill(new Ellipse2D.Float(hx - haloR, hy - haloR, haloR * 2, haloR * 2));
            g.setStroke(new BasicStroke(Math.max(1.6f, imageWidth * 0.0024f)));
            g.setColor(new Color(255, 238, 155, 145));
            g.draw(new Ellipse2D.Float(hx - haloR * 0.66f, hy - haloR * 0.66f, haloR * 1.32f, haloR * 1.32f));
        }

        // Draw Idol
        int idolPxX = Math.round(leftOffset + idolLeft * imageWidth);
        int idolPxY = Math.round(idolTop * imageWidth);
        int idolPxW = Math.round(idolW * imageWidth);
        int idolPxH = Math.round(idolH * imageWidth);
        g.drawImage(idol, idolPxX, idolPxY, idolPxW, idolPxH, null);

        // Ceremonial Tilak on foreheads
        if (witherFactor < 0.5f) {
            for (float[] fh : foreheads) {
                float tx = leftOffset + (idolLeft + fh[0] * idolW) * imageWidth;
                float ty = (idolTop + fh[1] * idolH) * imageWidth;
                float mark = imageWidth * 0.0082f;
                float span = mark * 1.48f;
                float arcY = ty + mark * 0.22f;
                float wingLift = mark * 0.36f;

                Path2D.Float chandanArc = new Path2D.Float();
                chandanArc.moveTo(tx - span, arcY - wingLift);
                chandanArc.quadTo(tx, arcY + mark * 0.44f, tx + span, arcY - wingLift);
                chandanArc.quadTo(tx, arcY - mark * 0.16f, tx - span, arcY - wingLift);
                chandanArc.closePath();
                g.setColor(new Color(0xF0, 0xBE, 0x4F, 235));
                g.fill(chandanArc);

                float tw = mark * 0.95f;
                float th = mark * 2.35f;
                g.setColor(new Color(0xC8, 0x20, 0x1C, 235));
                g.fill(new RoundRectangle2D.Float(tx - tw / 2f, ty - th * 0.65f, tw, th, tw, tw));

                g.setColor(new Color(0xFF, 0xFD, 0xF6, 245));
                g.fill(new Ellipse2D.Float(tx - tw * 0.36f, ty + mark * 0.35f, tw * 0.72f, tw * 0.72f));
            }
        }

        // 4. Incense & Standing Lamp on altar step
        String incenseAsset = switch (incenseId) {
            case "dhoop" -> "incense/dhoop_dani.png";
            case "kamal_dhoop" -> "incense/kamal_dhoop.png";
            case "mayur_dhoop" -> "incense/mayur_dhoop.png";
            default -> "incense/agarbatti.png";
        };
        float incenseW = switch (incenseId) {
            case "dhoop" -> 0.084f;
            case "kamal_dhoop" -> 0.102f;
            default -> 0.096f;
        };
        drawSprite(g, loadAsset(incenseAsset), 0.325f, 0.875f, incenseW, imageWidth, leftOffset, true, 0f, 1f);

        String lampAsset = switch (lampId) {
            case "peacock" -> "lamps/peacock_lamp.png";
            case "golden" -> "lamps/golden_oil.png";
            default -> "lamps/brass_diya.png";
        };
        BufferedImage lampBmp = loadAsset(lampAsset);
        float lampW = 0.098f;
        float lampH = lampW * ((float) lampBmp.getHeight() / lampBmp.getWidth());
        drawSprite(g, lampBmp, 0.76f, 0.88f, lampW, imageWidth, leftOffset, true, 0f, 1f);

        if (litFlames) {
            float[][] wicks = lampId.equals("golden")
                ? new float[][]{{0.76f, 0.88f - lampH * 0.80f}}
                : new float[][]{
                    {0.76f - lampW * 0.44f, 0.88f - lampH * 0.77f},
                    {0.76f - lampW * 0.15f, 0.88f - lampH * 0.73f},
                    {0.76f - lampW * 0.14f, 0.88f - lampH * 0.81f},
                    {0.76f + lampW * 0.41f, 0.88f - lampH * 0.80f},
                    {0.76f + lampW * 0.39f, 0.88f - lampH * 0.74f}
                };
            for (int wi = 0; wi < Math.min(wicks.length, litWickCount); wi++) {
                float[] w = wicks[wi];
                drawAnimatedDiyaFlame(g, leftOffset + w[0] * imageWidth, w[1] * imageWidth, imageWidth * 0.022f, t, wi * 1.7f);
            }
            drawAnimatedIncenseSmoke(g, leftOffset + 0.325f * imageWidth, (0.875f - incenseW * 1.1f) * imageWidth, imageWidth, t);
        }

        // 5. Offered flowers at the deities' lotus feet
        BufferedImage marigold1 = loadAsset("flowers/sunflower.png");
        BufferedImage marigold2 = loadAsset("flowers/marigold_yellow.png");
        BufferedImage rose = loadAsset("flowers/rose.png");
        BufferedImage mogra = loadAsset("flowers/mogra_flower.png");
        float[] cols = new float[]{0f, -0.045f, 0.040f, -0.022f, 0.022f, 0f};
        float[] rows = new float[]{0.685f, 0.685f, 0.685f, 0.699f, 0.699f, 0.713f};
        for (int d = 0; d < foreheads.length; d++) {
            float cx = idolLeft + foreheads[d][0] * idolW;
            for (int s = 0; s < 6; s++) {
                BufferedImage fl = (s % 3 == 0) ? marigold1 : (s % 3 == 1 ? marigold2 : (garlandId.equals("rose_jasmine") ? rose : mogra));
                float fx = cx + cols[s] + witherFactor * 0.014f * (s % 2 == 0 ? 1 : -1);
                float fy = idolBottom + 0.008f + (rows[s] - 0.685f) + witherFactor * 0.015f;
                float fSize = 0.072f * (1f - 0.28f * witherFactor);
                drawSprite(g, fl, fx, fy, fSize, imageWidth, leftOffset, false, (s * 17f) - 25f + witherFactor * 38f, witherFactor > 0.1f ? 0.55f : 1f);
            }
        }

        // 6. Ornate Engraved Brass Puja Thali & Offerings
        drawSprite(g, loadAsset("accessories/plate.png"), 0.50f, 1.07f, 0.49f, imageWidth, leftOffset, false, 0f, 1f);

        float[][] plateFlowers = new float[][]{
            {0.50f - 0.152f, 1.07f - 0.038f},
            {0.50f - 0.168f, 1.07f - 0.004f},
            {0.50f - 0.118f, 1.07f - 0.020f},
            {0.50f - 0.138f, 1.07f + 0.018f},
            {0.50f - 0.160f, 1.07f + 0.038f},
            {0.50f - 0.106f, 1.07f + 0.024f},
            {0.50f - 0.112f, 1.07f + 0.066f}
        };
        for (int i = 0; i < plateFlowers.length; i++) {
            BufferedImage bmp = (i == 6) ? loadAsset("flowers/peony.png") : (i % 2 == 0 ? marigold1 : marigold2);
            drawSprite(g, bmp, plateFlowers[i][0], plateFlowers[i][1], i == 6 ? 0.088f : 0.064f, imageWidth, leftOffset, false, i * 14f - 18f, 1f);
        }

        // Brass Shankh Stand + Animated 432 Hz Shankh
        drawSprite(g, loadAsset("accessories/shankh_stand.png"), 0.630f, 1.098f, 0.124f, imageWidth, leftOffset, false, 0f, 1f);
        String shankhAsset = shankhId.equals("swarna") ? "shankh/swarna.png" : (shankhId.equals("ivory") ? "shankh/ivory.png" : "shankh/classic.png");
        if (activeShankh) {
            float sx = 0.50f, sy = 0.77f;
            float spx = leftOffset + sx * imageWidth;
            float spy = sy * imageWidth;
            for (int r = 0; r < 4; r++) {
                float waveFrac = (shankhWavePhase + r * 0.25f) % 1.0f;
                float rad = imageWidth * (0.055f + waveFrac * 0.21f);
                int alpha = Math.round((1f - waveFrac) * 185);
                g.setStroke(new BasicStroke(Math.max(2f, imageWidth * 0.0034f)));
                g.setColor(new Color(255, 224, 130, Math.max(0, Math.min(255, alpha))));
                g.draw(new Ellipse2D.Float(spx - rad, spy - rad, rad * 2, rad * 2));
            }
            drawSprite(g, loadAsset(shankhAsset), sx, sy, 0.148f, imageWidth, leftOffset, false, -18f + 2.5f * (float) Math.sin(t * 6.0), 1f);
        } else {
            drawSprite(g, loadAsset(shankhAsset), 0.630f, 1.074f, 0.126f, imageWidth, leftOffset, false, 0f, 1f);
        }

        // Animated Swinging Brass Ghanti (Bell)
        float bellY = Math.abs(bellSwingDeg) > 1.0f ? 0.86f : 0.91f;
        drawSprite(g, loadAsset("accessories/bell.png"), 0.22f, bellY, 0.092f, imageWidth, leftOffset, false, bellSwingDeg, 1f);
        if (Math.abs(bellSwingDeg) > 2.5f) {
            float bx = leftOffset + 0.22f * imageWidth;
            float by = bellY * imageWidth;
            g.setStroke(new BasicStroke(2.2f));
            g.setColor(new Color(255, 218, 120, Math.min(180, Math.round(Math.abs(bellSwingDeg) * 11f))));
            float arcR = imageWidth * 0.072f;
            g.draw(new Arc2D.Float(bx - arcR, by - arcR, arcR * 2, arcR * 2, -45, 90, Arc2D.OPEN));
            g.draw(new Arc2D.Float(bx - arcR, by - arcR, arcR * 2, arcR * 2, 135, 90, Arc2D.OPEN));
        }

        // Prasad Bowl
        String prasadAsset = switch (prasadId) {
            case "halwa" -> "prasad/halwa.png";
            case "modak" -> "prasad/modak.png";
            case "laddu_bowl" -> "prasad/laddu_bowl.png";
            default -> "prasad/laddu.png";
        };
        drawSprite(g, loadAsset(prasadAsset), 0.50f, 1.138f, 0.130f, imageWidth, leftOffset, false, 0f, 1f);

        // Animated Circular Orbiting Panch-Aarti Thali
        String aartiAsset = switch (aartiId) {
            case "karpura" -> "aarti/karpura.png";
            case "golden" -> "aarti/golden.png";
            default -> "aarti/traditional.png";
        };
        float aartiX, aartiY;
        if (aartiOrbitProgress >= 0f && aartiOrbitProgress <= 1f) {
            // 2 clockwise ceremonial circles around the central shrine
            double angle = -Math.PI / 2.0 + aartiOrbitProgress * Math.PI * 4.0;
            aartiX = 0.50f + 0.165f * (float) Math.cos(angle);
            aartiY = 0.54f + 0.135f * (float) Math.sin(angle);
        } else {
            aartiX = 0.745f;
            aartiY = 0.99f;
        }
        drawSprite(g, loadAsset(aartiAsset), aartiX, aartiY, 0.128f, imageWidth, leftOffset, false, 0f, 1f);
        if (litFlames || aartiOrbitProgress >= 0f) {
            drawAnimatedDiyaFlame(g, leftOffset + aartiX * imageWidth, (aartiY - 0.068f) * imageWidth, imageWidth * 0.030f, t, 4.2f);
        }

        // 7. Animated Copper Lota Abhishek Water Stream
        if (showAbhishek) {
            float crownX = idolLeft + foreheads[0][0] * idolW;
            float crownY = idolTop + 0.05f * idolH;
            float lotaY = crownY - 0.088f;
            float streamPxX = leftOffset + crownX * imageWidth;
            float streamTopY = (lotaY + 0.025f) * imageWidth;
            float streamBotY = (idolBottom - 0.03f) * imageWidth;

            g.setStroke(new BasicStroke(Math.max(4f, imageWidth * 0.007f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            GradientPaint waterStream = new GradientPaint(
                streamPxX, streamTopY, new Color(215, 244, 255, 230),
                streamPxX, streamBotY, new Color(215, 244, 255, 55)
            );
            g.setPaint(waterStream);
            g.draw(new Line2D.Float(streamPxX, streamTopY, streamPxX, streamBotY));

            // Animated water droplets glistening down the stream
            g.setColor(new Color(240, 252, 255, 215));
            for (int dr = 0; dr < 6; dr++) {
                float df = ((t * 2.4f) + dr / 6f) % 1.0f;
                float dy = streamTopY + df * (streamBotY - streamTopY);
                float dx = streamPxX + (float) Math.sin(df * 18 + dr) * 3.5f;
                g.fill(new Ellipse2D.Float(dx - 3.5f, dy - 6f, 7f, 12f));
            }
            drawSprite(g, loadAsset("accessories/copper_lota.png"), crownX + 0.018f, lotaY, 0.098f, imageWidth, leftOffset, false, 25f, 1f);
        }

        // 8. Gentle Pushparpan Falling Flower Shower
        if (petalIntensity > 0.05f) {
            int petalCount = Math.round(11 * petalIntensity);
            for (int p = 0; p < petalCount; p++) {
                float speed = 0.22f + (p % 4) * 0.05f;
                float yFrac = ((t * speed) + (p * 0.137f)) % 1.15f - 0.08f;
                if (yFrac < 0.02f || yFrac > 0.98f) continue;
                float baseX = 0.18f + ((p * 0.193f) % 0.64f);
                float sway = 0.026f * (float) Math.sin(t * 2.3f + p * 1.4f);
                BufferedImage flBmp = (p % 3 == 0) ? marigold1 : (p % 3 == 1 ? rose : mogra);
                float rot = (t * 48f + p * 37f) % 360f;
                drawSprite(g, flBmp, baseX + sway, yFrac, 0.044f, imageWidth, leftOffset, false, rot, 0.88f);
            }
        }
    }

    /** 4-stop radial diya flame with organic micro-flicker per pavitra-mandir-design skill. */
    private static void drawAnimatedDiyaFlame(Graphics2D g, float cx, float cy, float radius, float t, float phase) {
        float wobbleX = (float) (Math.sin(t * 11.3 + phase) * 2.4 + Math.cos(t * 17.1 + phase) * 0.9);
        float scaleH = 1.0f + 0.075f * (float) Math.sin(t * 14.5 + phase * 1.3);
        float fx = cx + wobbleX;

        RadialGradientPaint halo = new RadialGradientPaint(
            new Point2D.Float(fx, cy), radius * 2.5f * scaleH,
            new float[]{0f, 0.35f, 0.70f, 1f},
            new Color[]{
                new Color(0xFF, 0xFD, 0xF8, 245),
                new Color(0xFF, 0xC1, 0x07, 212),
                new Color(0xFF, 0x6D, 0x00, 118),
                new Color(0xFF, 0x9E, 0x2C, 0)
            }
        );
        g.setPaint(halo);
        float hr = radius * 2.5f * scaleH;
        g.fill(new Ellipse2D.Float(fx - hr, cy - hr, hr * 2f, hr * 2f));

        Path2D.Float flame = new Path2D.Float();
        float fw = radius * 0.52f;
        float fh = radius * 1.18f * scaleH;
        flame.moveTo(fx + wobbleX * 0.4f, cy - fh);
        flame.curveTo(fx + fw, cy - fh * 0.2f, fx + fw * 0.9f, cy + fh * 0.55f, cx, cy + fh * 0.65f);
        flame.curveTo(fx - fw * 0.9f, cy + fh * 0.55f, fx - fw, cy - fh * 0.2f, fx + wobbleX * 0.4f, cy - fh);
        flame.closePath();
        g.setColor(new Color(0xFF, 0xDF, 0x70));
        g.fill(flame);

        g.setColor(KARPURA_IVORY);
        g.fill(new Ellipse2D.Float(cx - fw * 0.35f, cy - fh * 0.25f, fw * 0.70f, fh * 0.65f));
    }

    private static void drawAnimatedIncenseSmoke(Graphics2D g, float x, float y, float u, float t) {
        g.setStroke(new BasicStroke(Math.max(2.2f, u * 0.0032f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 2; i++) {
            float sway = (float) Math.sin(t * 2.2 + i * 1.8) * u * 0.012f;
            float dx = (i == 0 ? -1 : 1) * u * 0.007f;
            Path2D.Float wisp = new Path2D.Float();
            wisp.moveTo(x + dx, y);
            wisp.curveTo(
                x + dx - u * 0.016f + sway, y - u * 0.050f,
                x + dx + u * 0.020f - sway, y - u * 0.105f,
                x + dx - u * 0.005f + sway * 1.3f, y - u * 0.165f
            );
            g.setColor(new Color(238, 230, 220, 72 - i * 20));
            g.draw(wisp);
        }
    }

    private static void drawTopReelHook(Graphics2D g, int w, ReelSpec spec, float t) {
        // Golden pill badge at y = 138 (safely below FB Reels top status bar, well above deity crowns)
        Font badgeFont = new Font("SansSerif", Font.BOLD, 23);
        g.setFont(badgeFont);
        int pillW = g.getFontMetrics().stringWidth(spec.dayBadge()) + 60;
        int pillH = 46;
        int pillX = (w - pillW) / 2;
        int pillY = 138;

        RoundRectangle2D.Float pill = new RoundRectangle2D.Float(pillX, pillY, pillW, pillH, pillH, pillH);
        g.setColor(new Color(24, 15, 10, 218));
        g.fill(pill);
        g.setStroke(new BasicStroke(2.0f));
        g.setColor(SACRED_GOLD);
        g.draw(pill);
        g.setColor(new Color(0xFF, 0xEC, 0xB3));
        g.drawString(spec.dayBadge(), pillX + 30, pillY + 31);

        // Main Hindi Hook Headline
        int titleSize = 54;
        Font titleFont = new Font("Serif", Font.BOLD, titleSize);
        g.setFont(titleFont);
        while (g.getFontMetrics().stringWidth(spec.hookHeadlineHi()) > w - 90 && titleSize > 36) {
            titleSize -= 2;
            titleFont = new Font("Serif", Font.BOLD, titleSize);
            g.setFont(titleFont);
        }
        g.setColor(new Color(0, 0, 0, 215));
        drawCenteredString(g, spec.hookHeadlineHi(), w / 2, 256);
        g.setColor(KARPURA_IVORY);
        drawCenteredString(g, spec.hookHeadlineHi(), w / 2, 252);

        // Warm Golden Subtitle
        int subSize = 29;
        Font subFont = new Font("SansSerif", Font.BOLD, subSize);
        g.setFont(subFont);
        while (g.getFontMetrics().stringWidth(spec.hookSubtitle()) > w - 96 && subSize > 21) {
            subSize -= 1;
            subFont = new Font("SansSerif", Font.BOLD, subSize);
            g.setFont(subFont);
        }
        g.setColor(new Color(0, 0, 0, 195));
        drawCenteredString(g, spec.hookSubtitle(), w / 2, 311);
        g.setColor(new Color(0xFF, 0xDE, 0x99));
        drawCenteredString(g, spec.hookSubtitle(), w / 2, 308);
    }

    private static void drawFloatingPill(Graphics2D g, int centerX, int centerY, String text, boolean activeGold) {
        Font f = new Font("SansSerif", Font.BOLD, 25);
        g.setFont(f);
        int pw = g.getFontMetrics().stringWidth(text) + 56;
        int ph = 52;
        int px = centerX - pw / 2;
        int py = centerY - ph / 2;

        RoundRectangle2D.Float r = new RoundRectangle2D.Float(px, py, pw, ph, ph, ph);
        g.setColor(activeGold ? new Color(32, 20, 12, 228) : new Color(55, 22, 18, 228));
        g.fill(r);
        g.setStroke(new BasicStroke(1.8f));
        g.setColor(activeGold ? SACRED_GOLD : new Color(0xE0, 0x7A, 0x5F));
        g.draw(r);
        g.setColor(KARPURA_IVORY);
        drawCenteredString(g, text, centerX, py + 34);
    }

    private static void drawBottomReelCta(Graphics2D g, int w, ReelSpec spec, float t) {
        // Placed at y = 1435..1615, centered-left slightly (x = 48..920) so FB's right-rail icons & bottom caption never cover it
        int cardX = 54;
        int cardW = w - 168; // leaves 114px on right for FB Reels Like/Comment/Share rail!
        int cardY = 1445;
        int cardH = 166;

        RoundRectangle2D.Float card = new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, 28f, 28f);
        GradientPaint gp = new GradientPaint(
            cardX, cardY, new Color(255, 252, 245, 244),
            cardX, cardY + cardH, new Color(246, 235, 214, 244)
        );
        g.setPaint(gp);
        g.fill(card);

        float pulseBorder = 2.0f + 0.8f * (float) Math.sin(t * 4.2);
        g.setStroke(new BasicStroke(pulseBorder));
        g.setColor(RITUAL_GOLD);
        g.draw(card);

        // Brand row inside CTA card
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(new Color(0x8C, 0x58, 0x18));
        g.drawString("पवित्र मंदिर (Pavitra Mandir)  •  Google Play पर निशुल्क", cardX + 28, cardY + 42);

        // Comment CTA bold line
        int ctaSize = 31;
        Font ctaFont = new Font("Serif", Font.BOLD, ctaSize);
        g.setFont(ctaFont);
        while (g.getFontMetrics().stringWidth(spec.commentPromptHi()) > cardW - 56 && ctaSize > 23) {
            ctaSize -= 1;
            ctaFont = new Font("Serif", Font.BOLD, ctaSize);
            g.setFont(ctaFont);
        }
        g.setColor(RITUAL_INK);
        g.drawString(spec.commentPromptHi(), cardX + 28, cardY + 92);

        // Pinned comment instruction pill
        RoundRectangle2D.Float subPill = new RoundRectangle2D.Float(cardX + 24, cardY + 110, cardW - 48, 42, 16f, 16f);
        g.setColor(new Color(0x26, 0x1C, 0x16));
        g.fill(subPill);

        int subSize = 21;
        Font subFont = new Font("SansSerif", Font.BOLD, subSize);
        g.setFont(subFont);
        while (g.getFontMetrics().stringWidth(spec.bottomSubCta()) > cardW - 76 && subSize > 16) {
            subSize -= 1;
            subFont = new Font("SansSerif", Font.BOLD, subSize);
            g.setFont(subFont);
        }
        g.setColor(new Color(0xFF, 0xE8, 0xA3));
        drawCenteredString(g, spec.bottomSubCta(), cardX + cardW / 2, cardY + 138);
    }

    // =========================================================================
    // Facebook Page Avatar (1024x1024) & Cover Banner (1640x924)
    // =========================================================================
    private static void generatePageProfile1024() throws IOException {
        BufferedImage src = ImageIO.read(LAUNCHER_ART);
        int size = 1024;
        BufferedImage avatar = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = avatar.createGraphics();
        configureQuality(g);

        GradientPaint bgGrad = new GradientPaint(0f, 0f, new Color(0x24, 0x12, 0x0A), 0f, size, new Color(0x0D, 0x0B, 0x0A));
        g.setPaint(bgGrad);
        g.fillRect(0, 0, size, size);

        RadialGradientPaint centerGlow = new RadialGradientPaint(
            new Point2D.Float(size / 2f, size / 2f), 470f,
            new float[]{0f, 0.62f, 1f},
            new Color[]{new Color(255, 196, 72, 155), new Color(216, 98, 20, 75), new Color(13, 11, 10, 0)}
        );
        g.setPaint(centerGlow);
        g.fillRect(0, 0, size, size);

        int cropX = 196, cropY = 148, cropW = 632, cropH = 584;
        BufferedImage emblem = new BufferedImage(cropW, cropH, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < cropH; y++) {
            for (int x = 0; x < cropW; x++) {
                int argb = src.getRGB(cropX + x, cropY + y);
                float nx = (x - cropW / 2f) / (cropW / 2f);
                float ny = (y - cropH / 2f) / (cropH / 2f);
                float dist = (float) Math.hypot(nx * 0.95f, ny * 0.95f);
                float alpha = dist > 0.76f ? Math.max(0f, (0.97f - dist) / 0.21f) : 1f;
                int a = Math.round(((argb >>> 24) & 0xFF) * alpha);
                emblem.setRGB(x, y, (a << 24) | (argb & 0x00FFFFFF));
            }
        }
        int dstW = 820;
        int dstH = Math.round(dstW * ((float) cropH / cropW));
        g.drawImage(emblem, (size - dstW) / 2, (size - dstH) / 2 + 12, dstW, dstH, null);

        // Circular safe-zone golden ring (looks regal inside Facebook's circle avatar crop)
        g.setStroke(new BasicStroke(8f));
        g.setColor(new Color(216, 175, 82, 195));
        g.draw(new Ellipse2D.Float(36, 36, size - 72, size - 72));

        g.dispose();
        savePng(avatar, new File(PAGE_DIR, "facebook_profile_1024x1024.png"));
        System.out.println("  [OK] page_assets/facebook_profile_1024x1024.png");
    }

    private static void generatePageCover1640x924() throws IOException {
        int w = 1640, h = 924;
        BufferedImage cover = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cover.createGraphics();
        configureQuality(g);

        GradientPaint bgGrad = new GradientPaint(0, 0, new Color(0x0F, 0x0A, 0x08), w, h, new Color(0x28, 0x16, 0x0C));
        g.setPaint(bgGrad);
        g.fillRect(0, 0, w, h);

        RadialGradientPaint rightAura = new RadialGradientPaint(
            new Point2D.Float(1160f, 462f), 520f,
            new float[]{0f, 0.58f, 1f},
            new Color[]{new Color(255, 190, 75, 115), new Color(216, 140, 40, 45), new Color(15, 10, 8, 0)}
        );
        g.setPaint(rightAura);
        g.fillRect(620, 0, 1020, h);

        // Right shrine showcase card inside mobile+desktop safe zone
        int cardX = 940, cardY = 120, cardW = 540, cardH = 684;
        BufferedImage mini = new BufferedImage(cardW, cardH + 140, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = mini.createGraphics();
        configureQuality(mg);
        renderAnimatedAltar(
            mg, cardW, cardH + 140, 1.06f, 2.5f, 60,
            "swarna", "original", "marigold", "peacock", "kamal_dhoop", "golden", "swarna", "laddu_bowl",
            true, 5, false, 0f, 0.25f, true, 0.35f, 8f, 0f, 0.6f
        );
        mg.dispose();

        Shape clip = new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, 36f, 36f);
        Shape oldClip = g.getClip();
        g.setClip(clip);
        g.drawImage(mini, cardX, cardY, null);
        g.setClip(oldClip);
        g.setStroke(new BasicStroke(3f));
        g.setColor(SACRED_GOLD);
        g.draw(clip);

        // Left copy (safe inside mobile crop x=160..900, y=170..755)
        int lx = 155;
        RoundRectangle2D.Float pill = new RoundRectangle2D.Float(lx, 185, 435, 48, 48, 48);
        g.setColor(new Color(216, 175, 82, 48));
        g.fill(pill);
        g.setStroke(new BasicStroke(1.8f));
        g.setColor(SACRED_GOLD);
        g.draw(pill);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(new Color(0xFF, 0xEC, 0xB3));
        g.drawString("१००% विज्ञापन-मुक्त  •  100% AD-FREE", lx + 26, 217);

        g.setFont(new Font("Serif", Font.BOLD, 74));
        g.setColor(KARPURA_IVORY);
        g.drawString("पवित्र मंदिर", lx, 325);

        g.setFont(new Font("Serif", Font.BOLD, 42));
        g.setColor(SACRED_GOLD);
        g.drawString("Pavitra Mandir: Daily Puja & Aarti", lx, 385);

        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        g.setColor(SANDALWOOD_DUST);
        g.drawString("घर बैठे करें ८-चरणीय नित्य पूजा, दिव्य आरती, भजन", lx, 455);
        g.drawString("एवं ४३२ Hz शंखनाद — बिना किसी विज्ञापन के।", lx, 496);

        String[] chips = {
            "✓ ८ देवी-देवता व ४ भव्य मंदिर",
            "✓ प्रातः निर्माल्य शुद्धि व शृंगार",
            "✓ ४३२ Hz शंखनाद व आरती संग्रह",
            "✓ बुजुर्गों के लिए सरल व सुरक्षित"
        };
        for (int i = 0; i < chips.length; i++) {
            int cx = lx + (i % 2) * 370;
            int cy = 545 + (i / 2) * 68;
            RoundRectangle2D.Float cr = new RoundRectangle2D.Float(cx, cy, 352, 52, 16, 16);
            g.setColor(new Color(38, 25, 18, 225));
            g.fill(cr);
            g.setStroke(new BasicStroke(1.3f));
            g.setColor(new Color(216, 175, 82, 135));
            g.draw(cr);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.setColor(KARPURA_IVORY);
            g.drawString(chips[i], cx + 18, cy + 33);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 25));
        int btnTextW = g.getFontMetrics().stringWidth("▶  Google Play पर निशुल्क डाउनलोड करें");
        RoundRectangle2D.Float playBtn = new RoundRectangle2D.Float(lx, 705, btnTextW + 64, 66, 20, 20);
        g.setColor(SACRED_GOLD);
        g.fill(playBtn);
        g.setColor(RITUAL_INK);
        g.drawString("▶  Google Play पर निशुल्क डाउनलोड करें", lx + 32, 747);

        g.dispose();
        savePng(cover, new File(PAGE_DIR, "facebook_cover_1640x924.png"));
        System.out.println("  [OK] page_assets/facebook_cover_1640x924.png");
    }

    // =========================================================================
    // Shared Utilities
    // =========================================================================
    private static void configureQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private static void drawCenteredString(Graphics2D g, String text, int centerX, int baselineY) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, centerX - fm.stringWidth(text) / 2, baselineY);
    }

    private static BufferedImage loadAsset(String relPath) throws IOException {
        if (IMAGE_CACHE.containsKey(relPath)) {
            return IMAGE_CACHE.get(relPath);
        }
        File f = new File(SHRINE_DIR, relPath);
        if (!f.exists() && relPath.endsWith(".png")) {
            f = new File(SHRINE_DIR, relPath.substring(0, relPath.length() - 4) + ".webp");
        }
        if (!f.exists()) {
            throw new IOException("Missing shrine asset: " + f.getAbsolutePath());
        }
        BufferedImage raw = readRasterImage(f);
        if (relPath.startsWith("idols/")) {
            raw = trimIdolAlpha(raw);
        }
        IMAGE_CACHE.put(relPath, raw);
        return raw;
    }

    private static BufferedImage readRasterImage(File f) throws IOException {
        BufferedImage direct = ImageIO.read(f);
        if (direct != null) return direct;
        File tmpPng = File.createTempFile("reels_webp_", ".png");
        try {
            Process p = new ProcessBuilder("dwebp", "-quiet", f.getAbsolutePath(), "-o", tmpPng.getAbsolutePath())
                .redirectErrorStream(true).start();
            if (p.waitFor() != 0) {
                throw new IOException("dwebp failed for " + f.getAbsolutePath());
            }
            BufferedImage img = ImageIO.read(tmpPng);
            if (img == null) throw new IOException("Unreadable decoded image: " + f.getAbsolutePath());
            return img;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted decoding " + f.getAbsolutePath(), e);
        } finally {
            tmpPng.delete();
        }
    }

    private static BufferedImage trimIdolAlpha(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (src.getRGB(x, y) >>> 24) & 0xFF;
                if (a >= 24) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY) return src;
        return src.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static void drawSprite(
        Graphics2D g, BufferedImage bmp,
        float cxFrac, float cyFrac, float widthFrac,
        float imageWidth, float leftOffset,
        boolean bottomAligned, float rotationDeg, float alpha
    ) {
        float aspect = (float) bmp.getHeight() / Math.max(1f, bmp.getWidth());
        float pw = imageWidth * widthFrac;
        float ph = pw * aspect;
        float px = leftOffset + cxFrac * imageWidth - pw / 2f;
        float py = bottomAligned ? (cyFrac * imageWidth - ph) : (cyFrac * imageWidth - ph / 2f);

        AffineTransform oldTx = g.getTransform();
        Composite oldComp = g.getComposite();
        if (alpha < 0.999f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
        }
        if (Math.abs(rotationDeg) > 0.01f) {
            g.rotate(Math.toRadians(rotationDeg), px + pw / 2f, py + ph / 2f);
        }
        g.drawImage(bmp, Math.round(px), Math.round(py), Math.round(pw), Math.round(ph), null);
        g.setTransform(oldTx);
        g.setComposite(oldComp);
    }

    private static void savePng(BufferedImage img, File dest) throws IOException {
        dest.getParentFile().mkdirs();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (writers.hasNext()) {
            ImageWriter writer = writers.next();
            try (FileImageOutputStream out = new FileImageOutputStream(dest)) {
                writer.setOutput(out);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(0.15f);
                }
                writer.write(null, new IIOImage(img, null, null), param);
            } finally {
                writer.dispose();
            }
        } else {
            ImageIO.write(img, "png", dest);
        }
    }
}
