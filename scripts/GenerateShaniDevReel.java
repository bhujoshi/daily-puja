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
import javax.imageio.ImageIO;

/**
 * Renders the dedicated Saturday Shani Dev (शनिदेव महाराज) 1080x1920 HD Reel:
 *  - Flood-fill transparent cutout with edge color decontamination (zero white fringe)
 *  - Mustard-oil Panchmukhi Diya, Loban Dhoop smoke, Blue Orchid/Aparajita Pushparpan,
 *    Swinging Brass Bell, Circular Panch-Aarti, and 432 Hz Shankh resonance.
 */
public class GenerateShaniDevReel {

    private static final File ROOT = new File("/Users/bhuwanjoshi/work/worship");
    private static final File SHRINE_DIR = new File(ROOT, "android/app/src/main/assets/shrine");
    private static final File RAW_AUDIO_DIR = new File(ROOT, "android/app/src/main/res/raw");
    private static final File SHANI_RAW = new File("/Users/bhuwanjoshi/.gemini/antigravity/brain/a3f7982a-19f7-448a-a058-6a5c8df74c63/shani_dev_raw_1795233587189.jpg");
    private static final File OUT_DIR = new File(ROOT, "marketing/facebook_reels");
    private static final File VIDEO_DIR = new File(OUT_DIR, "videos");
    private static final File COVER_DIR = new File(OUT_DIR, "covers");
    private static final File ASSET_OUT_DIR = new File(OUT_DIR, "assets");

    private static final Color OBSIDIAN_UMBER = new Color(0x0D, 0x0B, 0x0A);
    private static final Color SACRED_GOLD = new Color(0xD8, 0xAF, 0x52);
    private static final Color KARPURA_IVORY = new Color(0xFF, 0xFD, 0xF8);
    private static final Color RITUAL_INK = new Color(0x26, 0x1C, 0x16);
    private static final Color RITUAL_GOLD = new Color(0xB3, 0x83, 0x3B);

    private static final int W = 1080;
    private static final int H = 1920;
    private static final int FPS = 24;
    private static final float DURATION = 13.5f;
    private static final int FRAMES = Math.round(FPS * DURATION);

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    private static BufferedImage shaniIdol;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        VIDEO_DIR.mkdirs();
        COVER_DIR.mkdirs();
        ASSET_OUT_DIR.mkdirs();

        System.out.println("1/3 Extracting clean flood-fill alpha cutout of Lord Shani Dev...");
        shaniIdol = extractCleanCutout(ImageIO.read(SHANI_RAW));
        ImageIO.write(shaniIdol, "png", new File(ASSET_OUT_DIR, "shani_dev_idol.png"));

        System.out.println("2/3 Mixing Saturday Shani Dev Temple Audio Track...");
        File audioWav = new File("/tmp/shani_dev_audio.wav");
        buildAudioTrack(audioWav);

        System.out.println("3/3 Rendering 08_saturday_shani_dev_puja.mp4 (1080x1920 HD)...");
        File mp4Out = new File(VIDEO_DIR, "08_saturday_shani_dev_puja.mp4");
        File coverOut = new File(COVER_DIR, "08_saturday_shani_dev_puja_cover.png");

        List<String> cmd = List.of(
            "/opt/homebrew/bin/ffmpeg", "-y", "-loglevel", "error",
            "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", W + "x" + H, "-r", String.valueOf(FPS),
            "-i", "-",
            "-i", audioWav.getAbsolutePath(),
            "-c:v", "libx264", "-preset", "fast", "-crf", "20", "-pix_fmt", "yuv420p",
            "-profile:v", "high", "-movflags", "+faststart",
            "-c:a", "aac", "-b:a", "192k", "-ar", "44100", "-ac", "2", "-shortest",
            mp4Out.getAbsolutePath()
        );

        Process proc = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        BufferedImage frame = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        int[] pixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
        byte[] rgbBuf = new byte[W * H * 3];

        try (OutputStream out = new BufferedOutputStream(proc.getOutputStream(), 1024 * 1024)) {
            for (int f = 0; f < FRAMES; f++) {
                float t = f / (float) FPS;
                Graphics2D g = frame.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                renderFrame(g, t);
                g.dispose();

                if (f == Math.round(6.8f * FPS)) {
                    ImageIO.write(frame, "png", coverOut);
                }

                int bi = 0;
                for (int rgb : pixels) {
                    rgbBuf[bi++] = (byte) ((rgb >> 16) & 0xFF);
                    rgbBuf[bi++] = (byte) ((rgb >> 8) & 0xFF);
                    rgbBuf[bi++] = (byte) (rgb & 0xFF);
                }
                out.write(rgbBuf);
            }
        }

        if (proc.waitFor() != 0) {
            throw new RuntimeException("ffmpeg failed");
        }
        audioWav.delete();
        System.out.printf("[OK] %s (%.2f MB)%n", mp4Out.getName(), mp4Out.length() / (1024.0 * 1024.0));
    }

    /**
     * Flood-fills pure white exterior background from image borders so interior pearls/eyes stay opaque,
     * applies soft 2px anti-aliased edge feathering + color decontamination (eliminates white halo),
     * and trims tight to the murti bounds.
     */
    private static BufferedImage extractCleanCutout(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        boolean[] bgMask = new boolean[w * h];
        ArrayDeque<Integer> q = new ArrayDeque<>();

        for (int x = 0; x < w; x++) {
            trySeed(src, x, 0, w, bgMask, q);
            trySeed(src, x, h - 1, w, bgMask, q);
        }
        for (int y = 0; y < h; y++) {
            trySeed(src, 0, y, w, bgMask, q);
            trySeed(src, w - 1, y, w, bgMask, q);
        }

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        while (!q.isEmpty()) {
            int idx = q.poll();
            int cx = idx % w, cy = idx / w;
            for (int d = 0; d < 4; d++) {
                int nx = cx + dx[d], ny = cy + dy[d];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                    int nIdx = ny * w + nx;
                    if (!bgMask[nIdx] && isNearWhiteBg(src.getRGB(nx, ny))) {
                        bgMask[nIdx] = true;
                        q.add(nIdx);
                    }
                }
            }
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int minX = w, minY = h, maxX = -1, maxY = -1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                if (bgMask[idx]) {
                    out.setRGB(x, y, 0x00000000);
                    continue;
                }
                // Count adjacent background neighbors in 5x5 neighborhood for smooth edge feather & decontamination
                int bgNeighbors = 0, totalNeighbors = 0;
                for (int oy = -2; oy <= 2; oy++) {
                    for (int ox = -2; ox <= 2; ox++) {
                        int nx = x + ox, ny = y + oy;
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                            totalNeighbors++;
                            if (bgMask[ny * w + nx]) bgNeighbors++;
                        }
                    }
                }

                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int a = 255;

                if (bgNeighbors > 0) {
                    float edgeFrac = bgNeighbors / (float) totalNeighbors;
                    // Pull out white spill on boundary pixels
                    float lum = (r + g + b) / (3f * 255f);
                    if (lum > 0.80f) {
                        a = Math.round(255 * Math.max(0f, (0.96f - lum) / 0.16f) * (1f - edgeFrac * 0.7f));
                    } else {
                        a = Math.round(255 * Math.min(1f, (1f - edgeFrac * 0.85f)));
                    }
                    // Darken slight white spill along boundary
                    r = Math.round(r * (1f - edgeFrac * 0.35f));
                    g = Math.round(g * (1f - edgeFrac * 0.35f));
                    b = Math.round(b * (1f - edgeFrac * 0.35f));
                }

                if (a >= 24) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
                out.setRGB(x, y, (Math.max(0, Math.min(255, a)) << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static void trySeed(BufferedImage src, int x, int y, int w, boolean[] mask, ArrayDeque<Integer> q) {
        int idx = y * w + x;
        if (!mask[idx] && isNearWhiteBg(src.getRGB(x, y))) {
            mask[idx] = true;
            q.add(idx);
        }
    }

    private static boolean isNearWhiteBg(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int maxC = Math.max(r, Math.max(g, b));
        int minC = Math.min(r, Math.min(g, b));
        return r >= 232 && g >= 232 && b >= 232 && (maxC - minC) <= 22;
    }

    private static void buildAudioTrack(File outWav) throws Exception {
        String bell = new File(RAW_AUDIO_DIR, "bell.ogg").getAbsolutePath();
        String flower = new File(RAW_AUDIO_DIR, "flower_offering.ogg").getAbsolutePath();
        String flute = new File(RAW_AUDIO_DIR, "mindfulness_flute.ogg").getAbsolutePath();
        String conch = new File(RAW_AUDIO_DIR, "conch.ogg").getAbsolutePath();

        String filter =
            "[0:a]aresample=44100,pan=stereo|c0=0.90*c0|c1=0.90*c0,adelay=60|60[b1];" +
            "[1:a]aresample=44100,pan=stereo|c0=0.85*c0|c1=0.85*c0,adelay=2600|2600[fw];" +
            "[0:a]aresample=44100,pan=stereo|c0=0.75*c0|c1=0.75*c0,adelay=4500|4500[b2];" +
            "[2:a]aresample=44100,pan=stereo|c0=0.70*c0|c1=0.70*c0,adelay=5600|5600[fl];" +
            "[3:a]aresample=44100,atrim=0:6.0,afade=t=in:st=0:d=0.35,afade=t=out:st=4.6:d=1.3,volume=0.78,adelay=7500|7500[sh];" +
            "[b1][fw][b2][fl][sh]amix=inputs=5:normalize=0,alimiter=limit=0.92,afade=t=out:st=12.6:d=0.9[out]";

        List<String> cmd = List.of(
            "/opt/homebrew/bin/ffmpeg", "-y", "-loglevel", "error",
            "-i", bell, "-i", flower, "-i", flute, "-i", conch,
            "-filter_complex", filter, "-map", "[out]",
            "-t", String.valueOf(DURATION), "-ar", "44100", "-ac", "2",
            outWav.getAbsolutePath()
        );
        if (new ProcessBuilder(cmd).inheritIO().start().waitFor() != 0) {
            throw new RuntimeException("Audio mix failed");
        }
    }

    private static void renderFrame(Graphics2D g, float t) throws IOException {
        g.setColor(OBSIDIAN_UMBER);
        g.fillRect(0, 0, W, H);

        BufferedImage topBg = loadAsset("backgrounds/diwali.png");
        g.drawImage(topBg, 0, 0, W, 320, 0, 0, topBg.getWidth(), Math.max(1, topBg.getHeight() / 5), null);

        float zoom = 1.05f + 0.065f * (t / DURATION);
        int sceneTopY = 95;
        Graphics2D sg = (Graphics2D) g.create();
        sg.translate(0, sceneTopY);

        int sh = H - sceneTopY;
        float imageRatio = 1672f / 940f;
        float imageWidth = Math.max((float) W, (float) sh / imageRatio) * zoom;
        float leftOffset = (W - imageWidth) / 2f;

        // 1. Dark Deepotsav Stone Sanctum (diwali.png) + subtle royal sapphire-gold Shani aura
        int bgW = Math.round(imageWidth);
        int bgH = Math.round(imageWidth * ((float) topBg.getHeight() / topBg.getWidth()));
        g.drawImage(topBg, Math.round(leftOffset), sceneTopY, bgW, bgH, null);

        // Subtle sapphire-gold Shani aura in sanctum
        float pulse = 0.90f + 0.10f * (float) Math.sin(t * 3.0);
        RadialGradientPaint sapphireAura = new RadialGradientPaint(
            new Point2D.Float(W / 2f, imageWidth * 0.48f), imageWidth * 0.46f,
            new float[]{0f, 0.55f, 1f},
            new Color[]{
                new Color(52, 86, 175, Math.round(58 * pulse)),
                new Color(216, 155, 45, Math.round(30 * pulse)),
                new Color(13, 11, 10, 0)
            }
        );
        sg.setPaint(sapphireAura);
        sg.fillRect(0, 0, W, sh);

        // 2. Garland Pillars & Toran
        BufferedImage garland = loadAsset("garlands/mogra.png");
        float fLeft = 0.145f, fTop = 0.025f, fRight = 0.855f, fBottom = 0.655f, pillarW = 0.088f, topH = 0.165f;
        int gw = garland.getWidth(), gh = garland.getHeight();
        int sideSrcW = Math.max(1, (int) (gw * 0.125f));
        int topSrcX = (int) (gw * 0.1065f);
        int topSrcW = Math.max(1, (int) (gw * (1f - 2f * 0.1065f)));
        int topSrcH = Math.max(1, (int) (gh * 0.275f));

        int topLeftX = Math.round(leftOffset + fLeft * imageWidth);
        int topLeftY = Math.round(fTop * imageWidth);
        int rightPillarX = Math.round(leftOffset + (fRight - pillarW) * imageWidth);
        int sideDstW = Math.round(pillarW * imageWidth);
        int sideDstH = Math.round((fBottom - fTop) * imageWidth);

        sg.drawImage(garland, topLeftX, topLeftY, topLeftX + sideDstW, topLeftY + sideDstH, 0, 0, sideSrcW, gh, null);
        sg.drawImage(garland, rightPillarX, topLeftY, rightPillarX + sideDstW, topLeftY + sideDstH, gw - sideSrcW, 0, gw, gh, null);
        int archX = Math.round(leftOffset + (fLeft + pillarW * 0.85f) * imageWidth);
        int archW = Math.round(((fRight - pillarW * 0.85f) - (fLeft + pillarW * 0.85f)) * imageWidth);
        sg.drawImage(garland, archX, topLeftY, archX + archW, Math.round(topH * imageWidth), topSrcX, 0, topSrcX + topSrcW, topSrcH, null);

        // 3. Prabhamandal Halo (Golden + Neelam Sapphire ring) behind Shani Dev's Mukut
        float aspect = (float) shaniIdol.getWidth() / shaniIdol.getHeight();
        float idolH = 0.415f;
        float idolW = idolH * aspect;
        float idolBottom = 0.690f;
        float idolTop = idolBottom - idolH;
        float idolLeft = 0.5f - idolW / 2f;

        float hx = leftOffset + 0.5f * imageWidth;
        float hy = (idolTop + 0.21f * idolH) * imageWidth;
        float haloR = imageWidth * 0.145f * (1f + 0.05f * (float) Math.sin(t * 2.6));
        RadialGradientPaint halo = new RadialGradientPaint(
            new Point2D.Float(hx, hy), haloR,
            new float[]{0f, 0.52f, 0.80f, 1f},
            new Color[]{
                new Color(255, 248, 215, 175),
                new Color(255, 212, 95, 105),
                new Color(75, 120, 235, 58),
                new Color(30, 50, 120, 0)
            }
        );
        sg.setPaint(halo);
        sg.fill(new Ellipse2D.Float(hx - haloR, hy - haloR, haloR * 2, haloR * 2));
        sg.setStroke(new BasicStroke(2.2f));
        sg.setColor(new Color(255, 232, 145, 145));
        sg.draw(new Ellipse2D.Float(hx - haloR * 0.66f, hy - haloR * 0.66f, haloR * 1.32f, haloR * 1.32f));

        // Draw Shani Dev Murti
        sg.drawImage(
            shaniIdol,
            Math.round(leftOffset + idolLeft * imageWidth),
            Math.round(idolTop * imageWidth),
            Math.round(idolW * imageWidth),
            Math.round(idolH * imageWidth),
            null
        );

        // 4. Loban Dhoop & Mustard-Oil Panchmukhi Diya (lighting wick-by-wick)
        drawSprite(sg, loadAsset("incense/dhoop_dani.png"), 0.315f, 0.875f, 0.088f, imageWidth, leftOffset, true, 0f, 1f);
        drawSmoke(sg, leftOffset + 0.315f * imageWidth, (0.875f - 0.095f) * imageWidth, imageWidth, t);

        BufferedImage lampBmp = loadAsset("lamps/brass_diya.png");
        float lampW = 0.102f;
        float lampH = lampW * ((float) lampBmp.getHeight() / lampBmp.getWidth());
        drawSprite(sg, lampBmp, 0.765f, 0.88f, lampW, imageWidth, leftOffset, true, 0f, 1f);

        float[][] wicks = {
            {0.765f - lampW * 0.44f, 0.88f - lampH * 0.77f},
            {0.765f - lampW * 0.15f, 0.88f - lampH * 0.73f},
            {0.765f - lampW * 0.14f, 0.88f - lampH * 0.81f},
            {0.765f + lampW * 0.41f, 0.88f - lampH * 0.80f},
            {0.765f + lampW * 0.39f, 0.88f - lampH * 0.74f}
        };
        int litCount = Math.min(5, 1 + (int) (t * 1.7f));
        for (int wi = 0; wi < litCount; wi++) {
            drawFlame(sg, leftOffset + wicks[wi][0] * imageWidth, wicks[wi][1] * imageWidth, imageWidth * 0.023f, t, wi * 1.6f);
        }

        // 5. Blue/Violet Orchid & Aparajita Offerings at Shani Dev's Lotus Feet
        BufferedImage blueOrchid = loadAsset("flowers/orchid.png");
        BufferedImage mogra = loadAsset("flowers/mogra_flower.png");
        BufferedImage marigold = loadAsset("flowers/marigold_yellow.png");
        float[] cols = {-0.055f, -0.022f, 0.018f, 0.052f, -0.035f, 0.035f};
        float[] rows = {0.692f, 0.696f, 0.696f, 0.692f, 0.712f, 0.712f};
        for (int i = 0; i < cols.length; i++) {
            BufferedImage fl = (i % 2 == 0) ? blueOrchid : mogra;
            drawSprite(sg, fl, 0.5f + cols[i], rows[i], 0.068f, imageWidth, leftOffset, false, i * 19f - 24f, 1f);
        }

        // 6. Brass Puja Thali, Swinging Bell, Prasad, Shankh & Circular Aarti
        drawSprite(sg, loadAsset("accessories/plate.png"), 0.50f, 1.07f, 0.49f, imageWidth, leftOffset, false, 0f, 1f);
        float[][] plateFlowers = {
            {0.348f, 1.032f}, {0.332f, 1.066f}, {0.382f, 1.050f},
            {0.362f, 1.088f}, {0.340f, 1.108f}, {0.394f, 1.094f}
        };
        for (int i = 0; i < plateFlowers.length; i++) {
            drawSprite(sg, i % 2 == 0 ? blueOrchid : marigold, plateFlowers[i][0], plateFlowers[i][1], 0.066f, imageWidth, leftOffset, false, i * 15f, 1f);
        }

        // Brass Bell
        float bellDeg = 0f;
        if (t < 3.0f) bellDeg = (float) (Math.sin(t * 9.5) * 16.0 * Math.exp(-t * 0.65));
        else if (t >= 4.4f && t < 7.2f) bellDeg = (float) (Math.sin((t - 4.4f) * 8.5) * 12.0 * Math.exp(-(t - 4.4f) * 0.8));
        float bellY = Math.abs(bellDeg) > 1f ? 0.86f : 0.91f;
        drawSprite(sg, loadAsset("accessories/bell.png"), 0.22f, bellY, 0.092f, imageWidth, leftOffset, false, bellDeg, 1f);

        // Prasad Bowl
        drawSprite(sg, loadAsset("prasad/halwa.png"), 0.50f, 1.138f, 0.130f, imageWidth, leftOffset, false, 0f, 1f);

        // Shankh + 432 Hz Sonic Ripples from t >= 7.5s
        drawSprite(sg, loadAsset("accessories/shankh_stand.png"), 0.630f, 1.098f, 0.124f, imageWidth, leftOffset, false, 0f, 1f);
        if (t >= 7.5f) {
            float sx = 0.50f, sy = 0.77f;
            float spx = leftOffset + sx * imageWidth, spy = sy * imageWidth;
            float phase = (t - 7.5f) * 1.45f;
            for (int r = 0; r < 4; r++) {
                float wf = (phase + r * 0.25f) % 1.0f;
                float rad = imageWidth * (0.055f + wf * 0.21f);
                sg.setStroke(new BasicStroke(3.2f));
                sg.setColor(new Color(255, 224, 130, Math.round((1f - wf) * 185)));
                sg.draw(new Ellipse2D.Float(spx - rad, spy - rad, rad * 2, rad * 2));
            }
            drawSprite(sg, loadAsset("shankh/swarna.png"), sx, sy, 0.148f, imageWidth, leftOffset, false, -18f + 2.5f * (float) Math.sin(t * 6.0), 1f);
        } else {
            drawSprite(sg, loadAsset("shankh/swarna.png"), 0.630f, 1.074f, 0.126f, imageWidth, leftOffset, false, 0f, 1f);
        }

        // Circular Orbiting Golden Aarti (t = 3.0..9.8s)
        float aartiX = 0.745f, aartiY = 0.99f;
        if (t >= 3.0f && t <= 9.8f) {
            double ang = -Math.PI / 2.0 + ((t - 3.0f) / 6.8f) * Math.PI * 4.0;
            aartiX = 0.50f + 0.170f * (float) Math.cos(ang);
            aartiY = 0.53f + 0.138f * (float) Math.sin(ang);
        }
        drawSprite(sg, loadAsset("aarti/golden.png"), aartiX, aartiY, 0.128f, imageWidth, leftOffset, false, 0f, 1f);
        drawFlame(sg, leftOffset + aartiX * imageWidth, (aartiY - 0.068f) * imageWidth, imageWidth * 0.030f, t, 3.8f);

        // Gentle Blue Orchid & Mogra Flower Shower
        if (t >= 2.2f) {
            for (int p = 0; p < 10; p++) {
                float speed = 0.22f + (p % 4) * 0.05f;
                float yf = (((t - 2.2f) * speed) + p * 0.137f) % 1.15f - 0.08f;
                if (yf < 0.02f || yf > 0.98f) continue;
                float bx = 0.18f + ((p * 0.193f) % 0.64f) + 0.025f * (float) Math.sin(t * 2.3f + p);
                drawSprite(sg, p % 2 == 0 ? blueOrchid : mogra, bx, yf, 0.045f, imageWidth, leftOffset, false, (t * 45f + p * 35f) % 360f, 0.88f);
            }
        }
        sg.dispose();

        // Top & Bottom Vignettes
        g.setPaint(new GradientPaint(0, 0, new Color(10, 7, 5, 240), 0, 410, new Color(10, 7, 5, 0)));
        g.fillRect(0, 0, W, 410);
        g.setPaint(new GradientPaint(0, H - 590, new Color(10, 7, 5, 0), 0, H - 180, new Color(10, 7, 5, 240)));
        g.fillRect(0, H - 590, W, 590);

        // Top Hook Banner
        String badge = "शनिवार विशेष दर्शन • SHANI DEV PUJA";
        g.setFont(new Font("SansSerif", Font.BOLD, 23));
        int pw = g.getFontMetrics().stringWidth(badge) + 60;
        int px = (W - pw) / 2, py = 138;
        RoundRectangle2D.Float pill = new RoundRectangle2D.Float(px, py, pw, 46, 46, 46);
        g.setColor(new Color(18, 22, 42, 225));
        g.fill(pill);
        g.setStroke(new BasicStroke(2f));
        g.setColor(SACRED_GOLD);
        g.draw(pill);
        g.setColor(new Color(0xFF, 0xEC, 0xB3));
        g.drawString(badge, px + 30, py + 31);

        String title = "शनिदेव महाराज की दिव्य आरती व दीपदान";
        int ts = 52;
        g.setFont(new Font("Serif", Font.BOLD, ts));
        while (g.getFontMetrics().stringWidth(title) > W - 90 && ts > 36) {
            ts -= 2;
            g.setFont(new Font("Serif", Font.BOLD, ts));
        }
        g.setColor(new Color(0, 0, 0, 215));
        drawCentered(g, title, W / 2, 256);
        g.setColor(KARPURA_IVORY);
        drawCentered(g, title, W / 2, 252);

        String sub = "सरसों तेल का दीपक • नीले पुष्प • १००% विज्ञापन-मुक्त मंदिर";
        int ss = 28;
        g.setFont(new Font("SansSerif", Font.BOLD, ss));
        while (g.getFontMetrics().stringWidth(sub) > W - 96 && ss > 20) {
            ss -= 1;
            g.setFont(new Font("SansSerif", Font.BOLD, ss));
        }
        g.setColor(new Color(0xFF, 0xDE, 0x99));
        drawCentered(g, sub, W / 2, 308);

        // Dynamic Ritual Step Pill above bottom CTA
        String stepText = t < 3.0f ? "✦ सरसों तेल का पंचमुखी दीप प्रज्वलन"
            : (t < 7.5f ? "✦ नीले पुष्प समर्पण व शनिदेव महा-आरती" : "✦ ४३२ Hz दिव्य शंखनाद व आशीर्वाद");
        int cardX = 54, cardW = W - 168;
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        int spw = g.getFontMetrics().stringWidth(stepText) + 54;
        RoundRectangle2D.Float sp = new RoundRectangle2D.Float(cardX + cardW / 2f - spw / 2f, 1376, spw, 50, 50, 50);
        g.setColor(new Color(22, 24, 42, 232));
        g.fill(sp);
        g.setStroke(new BasicStroke(1.8f));
        g.setColor(SACRED_GOLD);
        g.draw(sp);
        g.setColor(KARPURA_IVORY);
        drawCentered(g, stepText, cardX + cardW / 2, 1409);

        // Bottom Safe-Zone CTA Card
        int cardY = 1445, cardH = 166;
        RoundRectangle2D.Float card = new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, 28f, 28f);
        g.setPaint(new GradientPaint(cardX, cardY, new Color(255, 252, 245, 244), cardX, cardY + cardH, new Color(246, 235, 214, 244)));
        g.fill(card);
        g.setStroke(new BasicStroke(2.2f));
        g.setColor(RITUAL_GOLD);
        g.draw(card);

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(new Color(0x8C, 0x58, 0x18));
        g.drawString("पवित्र मंदिर (Pavitra Mandir)  •  Google Play पर निशुल्क", cardX + 28, cardY + 42);

        g.setFont(new Font("Serif", Font.BOLD, 30));
        g.setColor(RITUAL_INK);
        g.drawString("कमेंट में 'जय शनिदेव' लिखें", cardX + 28, cardY + 92);

        RoundRectangle2D.Float subPill = new RoundRectangle2D.Float(cardX + 24, cardY + 110, cardW - 48, 42, 16f, 16f);
        g.setColor(RITUAL_INK);
        g.fill(subPill);
        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.setColor(new Color(0xFF, 0xE8, 0xA3));
        drawCentered(g, "निशुल्क ऐप लिंक पहले कमेंट (Pinned Comment) व Bio में है", cardX + cardW / 2, cardY + 138);
    }

    private static void drawFlame(Graphics2D g, float cx, float cy, float radius, float t, float phase) {
        float wx = (float) (Math.sin(t * 11.3 + phase) * 2.4 + Math.cos(t * 17.1 + phase) * 0.9);
        float sh = 1.0f + 0.075f * (float) Math.sin(t * 14.5 + phase * 1.3);
        float fx = cx + wx, hr = radius * 2.5f * sh;
        g.setPaint(new RadialGradientPaint(
            new Point2D.Float(fx, cy), hr,
            new float[]{0f, 0.35f, 0.70f, 1f},
            new Color[]{new Color(0xFF, 0xFD, 0xF8, 245), new Color(0xFF, 0xC1, 0x07, 212), new Color(0xFF, 0x6D, 0x00, 118), new Color(0xFF, 0x9E, 0x2C, 0)}
        ));
        g.fill(new Ellipse2D.Float(fx - hr, cy - hr, hr * 2f, hr * 2f));

        Path2D.Float flame = new Path2D.Float();
        float fw = radius * 0.52f, fh = radius * 1.18f * sh;
        flame.moveTo(fx + wx * 0.4f, cy - fh);
        flame.curveTo(fx + fw, cy - fh * 0.2f, fx + fw * 0.9f, cy + fh * 0.55f, cx, cy + fh * 0.65f);
        flame.curveTo(fx - fw * 0.9f, cy + fh * 0.55f, fx - fw, cy - fh * 0.2f, fx + wx * 0.4f, cy - fh);
        flame.closePath();
        g.setColor(new Color(0xFF, 0xDF, 0x70));
        g.fill(flame);
        g.setColor(KARPURA_IVORY);
        g.fill(new Ellipse2D.Float(cx - fw * 0.35f, cy - fh * 0.25f, fw * 0.70f, fh * 0.65f));
    }

    private static void drawSmoke(Graphics2D g, float x, float y, float u, float t) {
        g.setStroke(new BasicStroke(Math.max(2.2f, u * 0.0032f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 2; i++) {
            float sway = (float) Math.sin(t * 2.2 + i * 1.8) * u * 0.012f;
            float dx = (i == 0 ? -1 : 1) * u * 0.007f;
            Path2D.Float wisp = new Path2D.Float();
            wisp.moveTo(x + dx, y);
            wisp.curveTo(x + dx - u * 0.016f + sway, y - u * 0.050f, x + dx + u * 0.020f - sway, y - u * 0.105f, x + dx - u * 0.005f + sway * 1.3f, y - u * 0.165f);
            g.setColor(new Color(238, 230, 220, 72 - i * 20));
            g.draw(wisp);
        }
    }

    private static void drawCentered(Graphics2D g, String s, int cx, int y) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
    }

    private static BufferedImage loadAsset(String rel) throws IOException {
        if (CACHE.containsKey(rel)) return CACHE.get(rel);
        File f = new File(SHRINE_DIR, rel);
        if (!f.exists() && rel.endsWith(".png")) {
            f = new File(SHRINE_DIR, rel.substring(0, rel.length() - 4) + ".webp");
        }
        BufferedImage img = ImageIO.read(f);
        if (img == null) {
            File tmpPng = File.createTempFile("shani_webp_", ".png");
            try {
                Process p = new ProcessBuilder("dwebp", "-quiet", f.getAbsolutePath(), "-o", tmpPng.getAbsolutePath())
                    .redirectErrorStream(true).start();
                if (p.waitFor() != 0) throw new IOException("dwebp failed for " + f.getAbsolutePath());
                img = ImageIO.read(tmpPng);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted decoding " + f.getAbsolutePath(), e);
            } finally {
                tmpPng.delete();
            }
        }
        CACHE.put(rel, img);
        return img;
    }

    private static void drawSprite(Graphics2D g, BufferedImage bmp, float cxFrac, float cyFrac, float widthFrac, float imageWidth, float leftOffset, boolean bottomAligned, float rotDeg, float alpha) {
        float aspect = (float) bmp.getHeight() / Math.max(1f, bmp.getWidth());
        float pw = imageWidth * widthFrac, ph = pw * aspect;
        float px = leftOffset + cxFrac * imageWidth - pw / 2f;
        float py = bottomAligned ? (cyFrac * imageWidth - ph) : (cyFrac * imageWidth - ph / 2f);
        AffineTransform oldTx = g.getTransform();
        Composite oldComp = g.getComposite();
        if (alpha < 0.999f) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
        if (Math.abs(rotDeg) > 0.01f) g.rotate(Math.toRadians(rotDeg), px + pw / 2f, py + ph / 2f);
        g.drawImage(bmp, Math.round(px), Math.round(py), Math.round(pw), Math.round(ph), null);
        g.setTransform(oldTx);
        g.setComposite(oldComp);
    }
}
