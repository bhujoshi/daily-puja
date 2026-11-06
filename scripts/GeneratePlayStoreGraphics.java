import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

/**
 * Deterministic generator for Pavitra Mandir (com.pavitramandir.app) v1.0.17 Play Store assets:
 *  - 512x512 32-bit PNG App Icon (icon.png)
 *  - 1024x500 24-bit PNG Feature Graphic (featureGraphic.png)
 *  - 8 High-Impact 1.0.17 Screenshots across Phone (1080x2280), 7-inch Tablet (1200x1920), and 10-inch Tablet (1600x2560)
 *
 * Uses the authentic 1.0.17 shrine assets from android/app/src/main/assets/shrine/ and design tokens
 * from the Pavitra Mandir Garbhagriha Chiaroscuro design system.
 */
public class GeneratePlayStoreGraphics {

    private static final File ROOT = new File("/Users/bhuwanjoshi/work/worship");
    private static final File SHRINE_DIR = new File(ROOT, "android/app/src/main/assets/shrine");
    private static final File LAUNCHER_ART = new File(ROOT, "android/app/src/main/res/drawable-nodpi/launcher_artwork.png");
    private static final File PLAY_STORE_DIR = new File(ROOT, "play_store");
    private static final File FASTLANE_META_DIR = new File(ROOT, "android/fastlane/metadata/android");

    // Garbhagriha Chiaroscuro Palette
    private static final Color OBSIDIAN_UMBER = new Color(0x0D, 0x0B, 0x0A);
    private static final Color TEAK_DEEP = new Color(0x14, 0x0D, 0x0A);
    private static final Color TEAK_WOOD = new Color(0x22, 0x14, 0x0F);
    private static final Color SACRED_GOLD = new Color(0xD8, 0xAF, 0x52);
    private static final Color SACRED_BRASS = new Color(0xC5, 0xA0, 0x59);
    private static final Color HONEY_SAFFRON = new Color(0xFF, 0x9E, 0x2C);
    private static final Color DEEP_SAFFRON = new Color(0xD8, 0x58, 0x16);
    private static final Color CEREMONIAL_KUMKUM = new Color(0xA8, 0x24, 0x24);
    private static final Color KARPURA_IVORY = new Color(0xFF, 0xFD, 0xF8);
    private static final Color SANDALWOOD_DUST = new Color(0xCF, 0xC2, 0xB2);
    private static final Color RITUAL_INK = new Color(0x26, 0x1C, 0x16);
    private static final Color RITUAL_SUBINK = new Color(0x5A, 0x46, 0x38);
    private static final Color RITUAL_GOLD = new Color(0xB3, 0x83, 0x3B);
    private static final Color SANDALWOOD_IVORY = new Color(0xFA, 0xF6, 0xEE);
    private static final Color BISQUE_SANDSTONE = new Color(0xF3, 0xE7, 0xD3);
    private static final Color WHATSAPP_GREEN = new Color(0x1F, 0x8E, 0x4D);

    private static final Map<String, BufferedImage> IMAGE_CACHE = new HashMap<>();

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        System.out.println("Generating Pavitra Mandir 1.0.17 Google Play Store graphic assets...");

        generateIcon512();
        generateFeatureGraphic1024x500();

        int[][] deviceSpecs = new int[][] {
            {1080, 2280}, // Phone (9:19)
            {1200, 1920}, // 7-inch tablet (10:16)
            {1600, 2560}  // 10-inch tablet (10:16)
        };
        String[] playStoreSubdirs = new String[] {
            "screenshots",
            "screenshots_7inch_tablet",
            "screenshots_10inch_tablet"
        };
        String[] fastlaneSubdirs = new String[] {
            "phoneScreenshots",
            "sevenInchScreenshots",
            "tenInchScreenshots"
        };

        String[] screenshotNames = new String[] {
            "01_sacred_gateway.png",
            "02_swarna_sanctum_puja.png",
            "03_radha_krishna_marble.png",
            "04_aarti_shankh_naad.png",
            "05_mandir_sajaye_customizer.png",
            "06_bhajan_aarti_player.png",
            "07_punya_coin_passbook.png",
            "08_morning_shodhan.png"
        };

        for (int d = 0; d < deviceSpecs.length; d++) {
            int w = deviceSpecs[d][0];
            int h = deviceSpecs[d][1];
            File playOutDir = new File(PLAY_STORE_DIR, playStoreSubdirs[d]);
            cleanPngDirectory(playOutDir);

            for (String locale : new String[]{"en-US", "en-IN", "hi-IN"}) {
                File flDir = new File(FASTLANE_META_DIR, locale + "/images/" + fastlaneSubdirs[d]);
                cleanPngDirectory(flDir);
            }

            for (int i = 0; i < screenshotNames.length; i++) {
                BufferedImage shotEn = renderScreenshot(i + 1, w, h, false);
                File primaryFile = new File(playOutDir, screenshotNames[i]);
                savePng(shotEn, primaryFile);
                for (String locale : new String[]{"en-US", "en-IN"}) {
                    File flDir = new File(FASTLANE_META_DIR, locale + "/images/" + fastlaneSubdirs[d]);
                    Files.copy(primaryFile.toPath(), new File(flDir, screenshotNames[i]).toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                BufferedImage shotHi = renderScreenshot(i + 1, w, h, true);
                File hiDir = new File(FASTLANE_META_DIR, "hi-IN/images/" + fastlaneSubdirs[d]);
                savePng(shotHi, new File(hiDir, screenshotNames[i]));

                System.out.printf("  [OK] %s (%dx%d en + hi)%n", screenshotNames[i], w, h);
            }
        }

        System.out.println("All Play Store graphic assets generated and synced successfully.");
    }

    private static void cleanPngDirectory(File dir) throws IOException {
        if (!dir.exists()) {
            dir.mkdirs();
            return;
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"));
        if (files != null) {
            for (File f : files) {
                Files.deleteIfExists(f.toPath());
            }
        }
    }

    private static void configureQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static BufferedImage loadAsset(String relPath) throws IOException {
        if (IMAGE_CACHE.containsKey(relPath)) {
            return IMAGE_CACHE.get(relPath);
        }
        File f = new File(SHRINE_DIR, relPath);
        if (!f.exists()) {
            throw new IOException("Missing shrine asset: " + f.getAbsolutePath());
        }
        BufferedImage raw = ImageIO.read(f);
        if (relPath.startsWith("idols/")) {
            raw = trimIdolAlpha(raw);
        }
        IMAGE_CACHE.put(relPath, raw);
        return raw;
    }

    /** Matches AssetImageCache.trimmedIdol in Android app (strips alpha < 24 edge border). */
    private static BufferedImage trimIdolAlpha(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
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

    private static void savePng(BufferedImage img, File dest) throws IOException {
        dest.getParentFile().mkdirs();
        // For large 24-bit RGB screenshots, strip imperceptible 2-bit sensor/interpolation LSB noise
        // so lossless PNG deflate stays well under Google Play's 8,000,000-byte limit (~2.5-3.5 MB).
        if (img.getType() == BufferedImage.TYPE_INT_RGB && img.getWidth() >= 1080) {
            int w = img.getWidth();
            int h = img.getHeight();
            int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = pixels[i] & 0xFFFCFCFC;
            }
            img.setRGB(0, 0, w, h, pixels, 0, w);
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (writers.hasNext()) {
            ImageWriter writer = writers.next();
            try (FileImageOutputStream out = new FileImageOutputStream(dest)) {
                writer.setOutput(out);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(0.0f); // Max deflate compression in JDK PNGImageWriter
                }
                writer.write(null, new IIOImage(img, null, null), param);
            } finally {
                writer.dispose();
            }
        } else {
            ImageIO.write(img, "png", dest);
        }
    }

    // =========================================================================
    // 1. 512x512 32-bit PNG App Icon (Full-bleed, no white padding or tiny text)
    // =========================================================================
    private static void generateIcon512() throws IOException {
        BufferedImage src = ImageIO.read(LAUNCHER_ART);
        BufferedImage icon = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        configureQuality(g);

        // Full-bleed rich saffron-vermilion background matching the inner sanctum emblem
        GradientPaint bgGrad = new GradientPaint(
            0f, 0f, new Color(0xED, 0x7A, 0x0E),
            0f, 512f, new Color(0xC9, 0x2F, 0x0A)
        );
        g.setPaint(bgGrad);
        g.fillRect(0, 0, 512, 512);

        // Warm golden radial aura behind temple shikhar & kalash
        RadialGradientPaint centerGlow = new RadialGradientPaint(
            new Point2D.Float(256f, 250f), 245f,
            new float[]{0f, 0.58f, 1f},
            new Color[]{
                new Color(255, 210, 88, 120),
                new Color(242, 132, 18, 55),
                new Color(201, 47, 10, 0)
            }
        );
        g.setPaint(centerGlow);
        g.fillRect(0, 0, 512, 512);

        // Crop the central temple + kalash + diya + agarbatti emblem from launcher_artwork.png
        // (x=196..828, y=148..732 — excluding the outer white margin and bottom small text)
        // and blend its edges smoothly with an elliptical radial mask into the full-bleed 512x512 canvas.
        int cropX = 196, cropY = 148, cropW = 632, cropH = 584;
        BufferedImage emblem = new BufferedImage(cropW, cropH, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < cropH; y++) {
            for (int x = 0; x < cropW; x++) {
                int argb = src.getRGB(cropX + x, cropY + y);
                float nx = (x - cropW / 2f) / (cropW / 2f);
                float ny = (y - cropH / 2f) / (cropH / 2f);
                float radialDist = (float) Math.hypot(nx * 0.92f, ny * 0.92f);
                float boxDist = Math.max(Math.abs(nx), Math.abs(ny));
                float dist = Math.max(radialDist, boxDist);
                float alpha = 1f;
                if (dist > 0.78f) {
                    alpha = Math.max(0f, (0.98f - dist) / 0.20f);
                }
                int a = Math.round(((argb >>> 24) & 0xFF) * alpha);
                emblem.setRGB(x, y, (a << 24) | (argb & 0x00FFFFFF));
            }
        }

        // Draw enlarged emblem centered on the 512x512 canvas
        int dstW = 492;
        int dstH = Math.round(dstW * ((float) cropH / cropW));
        int dstX = (512 - dstW) / 2;
        int dstY = (512 - dstH) / 2 + 8;
        g.drawImage(emblem, dstX, dstY, dstW, dstH, null);

        g.dispose();

        File playIcon = new File(PLAY_STORE_DIR, "icon_512x512.png");
        savePng(icon, playIcon);
        for (String locale : new String[]{"en-US", "en-IN", "hi-IN"}) {
            File flIcon = new File(FASTLANE_META_DIR, locale + "/images/icon.png");
            flIcon.getParentFile().mkdirs();
            Files.copy(playIcon.toPath(), flIcon.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("  [OK] icon_512x512.png (512x512 32-bit Full-Bleed PNG)");
    }

    // =========================================================================
    // 2. 1024x500 24-bit PNG Feature Graphic
    // =========================================================================
    private static void generateFeatureGraphic1024x500() throws IOException {
        int w = 1024;
        int h = 500;
        BufferedImage fg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = fg.createGraphics();
        configureQuality(g);

        // Deep Garbhagriha background gradient
        GradientPaint bgGrad = new GradientPaint(0, 0, new Color(0x11, 0x0B, 0x08), w, h, new Color(0x28, 0x16, 0x0C));
        g.setPaint(bgGrad);
        g.fillRect(0, 0, w, h);

        // Warm golden radial aura behind the right shrine showcase
        RadialGradientPaint rightAura = new RadialGradientPaint(
            new Point2D.Float(760f, 250f), 340f,
            new float[]{0f, 0.55f, 1f},
            new Color[]{new Color(255, 190, 75, 110), new Color(216, 140, 40, 45), new Color(17, 11, 8, 0)}
        );
        g.setPaint(rightAura);
        g.fillRect(360, 0, 664, h);

        // Render Golden Sanctum (swarna) scene inside an ornate arched temple card on the right
        int cardX = 548, cardY = 24, cardW = 442, cardH = 452;
        Shape cardClip = new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, 32f, 32f);

        // Drop shadow behind temple card
        for (int i = 16; i >= 1; i--) {
            g.setColor(new Color(0, 0, 0, 9));
            g.fill(new RoundRectangle2D.Float(cardX - i * 1.3f, cardY - i * 0.8f + 4, cardW + i * 2.6f, cardH + i * 2.2f, 36f, 36f));
        }

        BufferedImage miniSanctum = new BufferedImage(cardW, cardH + 220, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = miniSanctum.createGraphics();
        configureQuality(sg);
        renderAltarCore(sg, cardW, cardH + 220, "swarna", "original", "marigold", "brass", "kamal_dhoop", "karpura", "swarna", "laddu_bowl", true, false, false, 0f);
        sg.dispose();

        Shape oldClip = g.getClip();
        g.setClip(cardClip);
        g.drawImage(miniSanctum, cardX, cardY - 12, null);

        // Subtle bottom vignette inside card
        GradientPaint cardVig = new GradientPaint(0, cardY + cardH - 110, new Color(13, 11, 10, 0), 0, cardY + cardH, new Color(13, 11, 10, 200));
        g.setPaint(cardVig);
        g.fillRect(cardX, cardY + cardH - 110, cardW, 110);
        g.setClip(oldClip);

        // Double golden rim around right shrine card
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(SACRED_GOLD);
        g.draw(cardClip);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(255, 238, 190, 120));
        g.draw(new RoundRectangle2D.Float(cardX + 5, cardY + 5, cardW - 10, cardH - 10, 26f, 26f));

        // Left side: App Icon Badge + Title + Hindi/English Tagline + Feature Pills
        BufferedImage appIcon = ImageIO.read(new File(PLAY_STORE_DIR, "icon_512x512.png"));
        int iconSize = 88;
        int iconX = 44, iconY = 38;
        Shape iconShape = new RoundRectangle2D.Float(iconX, iconY, iconSize, iconSize, 22f, 22f);
        g.setClip(iconShape);
        g.drawImage(appIcon, iconX, iconY, iconSize, iconSize, null);
        g.setClip(oldClip);
        g.setStroke(new BasicStroke(2f));
        g.setColor(SACRED_GOLD);
        g.draw(iconShape);

        // Pill badge next to icon
        RoundRectangle2D.Float topPill = new RoundRectangle2D.Float(iconX + iconSize + 18, iconY + 14, 295, 32, 16, 16);
        g.setColor(new Color(216, 175, 82, 36));
        g.fill(topPill);
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(new Color(216, 175, 82, 160));
        g.draw(topPill);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor( new Color(0xFF, 0xDF, 0x9E));
        g.drawString("ॐ  अष्टपदी नित्य पूजा  •  100% AD-FREE", iconX + iconSize + 32, iconY + 35);

        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(SANDALWOOD_DUST);
        g.drawString("Sacred Virtual Hindu Temple & Daily Darshan", iconX + iconSize + 20, iconY + 68);

        // Main Hindi & English Headline
        g.setFont(new Font("Serif", Font.BOLD, 44));
        g.setColor(KARPURA_IVORY);
        g.drawString("पवित्र मंदिर", 44, 188);

        g.setFont(new Font("Serif", Font.BOLD, 36));
        g.setColor(SACRED_GOLD);
        g.drawString("Pavitra Mandir — Daily Puja", 44, 236);

        // Decorative brass divider line
        GradientPaint lineGrad = new GradientPaint(44, 256, SACRED_GOLD, 480, 256, new Color(216, 175, 82, 0));
        g.setPaint(lineGrad);
        g.fillRect(44, 255, 440, 2);

        // Key value proposition subtitle
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(new Color(0xF5, 0xEB, 0xDC));
        g.drawString("4 Sacred Shrines  •  8 Revered Deities  •  8-Step Vedic Puja", 44, 292);
        g.setColor(SANDALWOOD_DUST);
        g.drawString("432 Hz Shankh Naad, Aarti, Bhajan Player & Punya Passbook", 44, 322);

        // 2x2 Feature Chips at bottom-left
        String[][] chips = new String[][] {
            {"🪔  स्वर्ण व संगमरमर मंदिर", "4 Custom Temple Sanctums"},
            {"🙏  ८ दिव्य देवी-देवता दर्शन", "Ganesha, Shiva, Ram, Durga & more"},
            {"🎵  भजन, आरती एवं शंखनाद", "Deity Bhajans & 432 Hz Shankh"},
            {"🪙  पुण्य मुद्रा पासबुक व स्ट्रीक", "Daily Streak & Passbook Rewards"}
        };
        int chipW = 232, chipH = 62, gapX = 14, gapY = 12;
        for (int i = 0; i < chips.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int cx = 44 + col * (chipW + gapX);
            int cy = 346 + row * (chipH + gapY);
            RoundRectangle2D.Float chipRect = new RoundRectangle2D.Float(cx, cy, chipW, chipH, 14, 14);
            g.setColor(new Color(38, 25, 18, 220));
            g.fill(chipRect);
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(new Color(216, 175, 82, 130));
            g.draw(chipRect);

            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(KARPURA_IVORY);
            g.drawString(chips[i][0], cx + 12, cy + 25);

            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(SANDALWOOD_DUST);
            g.drawString(chips[i][1], cx + 12, cy + 46);
        }

        // Outer subtle golden border
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(216, 175, 82, 110));
        g.drawRect(1, 1, w - 3, h - 3);

        g.dispose();

        File playFg = new File(PLAY_STORE_DIR, "feature_graphic_1024x500.png");
        savePng(fg, playFg);
        for (String locale : new String[]{"en-US", "en-IN", "hi-IN"}) {
            File flFg = new File(FASTLANE_META_DIR, locale + "/images/featureGraphic.png");
            flFg.getParentFile().mkdirs();
            Files.copy(playFg.toPath(), flFg.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("  [OK] feature_graphic_1024x500.png (1024x500 24-bit PNG)");
    }

    // =========================================================================
    // 3. Core 1.0.17 Altar Scene Renderer (matches MandirAltarView.kt math)
    // =========================================================================
    private static void renderAltarCore(
        Graphics2D g, int width, int height,
        String shrineId, String idolId, String garlandId,
        String lampId, String incenseId, String aartiId,
        String shankhId, String prasadId,
        boolean litFlames, boolean showAbhishek, boolean activeAartiAndShankh,
        float witherFactor
    ) throws IOException {
        float imageRatio = 1672f / 940f;
        float zoom = 1.08f;
        float imageWidth = Math.max((float) width, (float) height / imageRatio) * zoom;
        float leftOffset = (width - imageWidth) / 2f;

        // 1. Shrine Background (swarna, marble, carved, ivory)
        BufferedImage bg = loadAsset("backgrounds/" + shrineId + ".png");
        int bgW = Math.round(imageWidth);
        int bgH = Math.round(imageWidth * ((float) bg.getHeight() / bg.getWidth()));
        int bgX = Math.round(leftOffset);
        g.drawImage(bg, bgX, 0, bgW, bgH, null);

        // 2. Garland Layer (left pillar strand, right pillar strand, upper arch toran swag)
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

        g.drawImage(garland, topLeftX, topLeftY, topLeftX + sideDstW, topLeftY + sideDstH, 0, 0, sideSrcW, sh, null);
        g.drawImage(garland, rightPillarX, topLeftY, rightPillarX + sideDstW, topLeftY + sideDstH, sw - sideSrcW, 0, sw, sh, null);

        float archLeft = fLeft + pillarW * 0.85f;
        float archRight = fRight - pillarW * 0.85f;
        int archX = Math.round(leftOffset + archLeft * imageWidth);
        int archW = Math.max(1, Math.round((archRight - archLeft) * imageWidth));
        int archH = Math.max(1, Math.round(topH * imageWidth));
        g.drawImage(garland, archX, topLeftY, archX + archW, topLeftY + archH, topSrcX, 0, topSrcX + topSrcW, topSrcH, null);

        // 3. Idol Placement & Prabhamandal Halos (behind the deity crowns)
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
        float maxW = singleDeity && !idolId.equals("ram_darbar") ? Math.min(0.37f, 0.46f) : 0.46f;
        float maxH = Math.min(0.395f, 0.680f - 0.285f);
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

        // Luminous Prabhamandal Halos behind idols
        for (float[] fh : foreheads) {
            float hx = leftOffset + (idolLeft + fh[0] * idolW) * imageWidth;
            float hy = (idolTop + fh[1] * idolH) * imageWidth;
            float haloR = imageWidth * 0.125f;
            RadialGradientPaint haloGrad = new RadialGradientPaint(
                new Point2D.Float(hx, hy), haloR,
                new float[]{0f, 0.55f, 0.82f, 1f},
                new Color[]{
                    new Color(255, 251, 227, 155),
                    new Color(255, 231, 119, 95),
                    new Color(255, 202, 58, 45),
                    new Color(255, 202, 58, 0)
                }
            );
            g.setPaint(haloGrad);
            g.fill(new Ellipse2D.Float(hx - haloR, hy - haloR, haloR * 2, haloR * 2));
            g.setStroke(new BasicStroke(Math.max(1.5f, imageWidth * 0.0022f)));
            g.setColor(new Color(255, 235, 145, 130));
            g.draw(new Ellipse2D.Float(hx - haloR * 0.66f, hy - haloR * 0.66f, haloR * 1.32f, haloR * 1.32f));
        }

        // Draw Idol
        int idolPxX = Math.round(leftOffset + idolLeft * imageWidth);
        int idolPxY = Math.round(idolTop * imageWidth);
        int idolPxW = Math.round(idolW * imageWidth);
        int idolPxH = Math.round(idolH * imageWidth);
        g.drawImage(idol, idolPxX, idolPxY, idolPxW, idolPxH, null);

        // Ceremonial Chandan, Kumkum & Akshat Tilak on foreheads
        for (float[] fh : foreheads) {
            float tx = leftOffset + (idolLeft + fh[0] * idolW) * imageWidth;
            float ty = (idolTop + fh[1] * idolH) * imageWidth;
            float mark = imageWidth * 0.0082f;
            float span = mark * 1.48f;
            float arcY = ty + mark * 0.22f;
            float wingLift = mark * 0.36f;

            // Kesar-Chandan sandalwood arc base
            Path2D.Float chandanArc = new Path2D.Float();
            chandanArc.moveTo(tx - span, arcY - wingLift);
            chandanArc.quadTo(tx, arcY + mark * 0.44f, tx + span, arcY - wingLift);
            chandanArc.quadTo(tx, arcY - mark * 0.16f, tx - span, arcY - wingLift);
            chandanArc.closePath();
            g.setColor(new Color(0xF0, 0xBE, 0x4F, 235));
            g.fill(chandanArc);

            // Ceremonial Kumkum Urdhva Tilak
            float tw = mark * 0.95f;
            float th = mark * 2.35f;
            g.setColor(new Color(0xC8, 0x20, 0x1C, 235));
            g.fill(new RoundRectangle2D.Float(tx - tw / 2f, ty - th * 0.65f, tw, th, tw, tw));

            // Sacred Akshat rice grains at bindu base
            g.setColor(new Color(0xFF, 0xFD, 0xF6, 245));
            g.fill(new Ellipse2D.Float(tx - tw * 0.36f, ty + mark * 0.35f, tw * 0.72f, tw * 0.72f));
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

        // Lit 4-stop radial diya flames on the standing lamp
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
            for (float[] w : wicks) {
                drawDiyaFlame(g, leftOffset + w[0] * imageWidth, w[1] * imageWidth, imageWidth * 0.022f);
            }
            // Gently rising incense smoke wisps
            drawIncenseSmoke(g, leftOffset + 0.325f * imageWidth, (0.875f - incenseW * 1.1f) * imageWidth, imageWidth);
        }

        // 5. Offered flowers at the deities' lotus feet
        BufferedImage marigold1 = loadAsset("flowers/sunflower.png");
        BufferedImage marigold2 = loadAsset("flowers/marigold_yellow.png");
        BufferedImage rose = loadAsset("flowers/rose.png");
        BufferedImage mogra = loadAsset("flowers/mogra_flower.png");
        float[] cols = new float[]{0f, -0.045f, 0.040f, -0.022f, 0.022f, 0f, -0.045f, 0.040f};
        float[] rows = new float[]{0.685f, 0.685f, 0.685f, 0.699f, 0.699f, 0.713f, 0.713f, 0.713f};
        for (int d = 0; d < foreheads.length; d++) {
            float cx = idolLeft + foreheads[d][0] * idolW;
            for (int s = 0; s < 6; s++) {
                BufferedImage fl = (s % 3 == 0) ? marigold1 : (s % 3 == 1 ? marigold2 : (garlandId.equals("rose_jasmine") ? rose : mogra));
                float fx = cx + cols[s];
                float fy = idolBottom + 0.008f + (rows[s] - 0.685f);
                float fSize = 0.072f * (1f - 0.22f * witherFactor);
                drawSprite(g, fl, fx, fy, fSize, imageWidth, leftOffset, false, (s * 17f) - 25f, witherFactor > 0.1f ? 0.72f : 1f);
            }
        }

        // 6. Ornate Engraved Brass Puja Thali & Offerings
        drawSprite(g, loadAsset("accessories/plate.png"), 0.50f, 1.14f, 0.52f, imageWidth, leftOffset, false, 0f, 1f);

        // Flower cluster on left of Puja Thali
        float[][] plateFlowers = new float[][]{
            {0.50f - 0.162f, 1.14f - 0.042f},
            {0.50f - 0.180f, 1.14f - 0.004f},
            {0.50f - 0.125f, 1.14f - 0.022f},
            {0.50f - 0.148f, 1.14f + 0.018f},
            {0.50f - 0.172f, 1.14f + 0.042f},
            {0.50f - 0.112f, 1.14f + 0.026f},
            {0.50f - 0.142f, 1.14f - 0.012f},
            {0.50f - 0.126f, 1.14f + 0.006f},
            {0.50f - 0.102f, 1.14f + 0.056f},
            {0.50f - 0.122f, 1.14f + 0.078f}
        };
        for (int i = 0; i < plateFlowers.length; i++) {
            BufferedImage bmp = (i == 9) ? loadAsset("flowers/peony.png") : (i % 2 == 0 ? marigold1 : marigold2);
            drawSprite(g, bmp, plateFlowers[i][0], plateFlowers[i][1], i == 9 ? 0.095f : 0.068f, imageWidth, leftOffset, false, i * 12f - 18f, 1f);
        }

        // Brass Shankh Stand + Shankh
        drawSprite(g, loadAsset("accessories/shankh_stand.png"), 0.633f, 1.171f, 0.130f, imageWidth, leftOffset, false, 0f, 1f);
        String shankhAsset = shankhId.equals("swarna") ? "shankh/swarna.png" : (shankhId.equals("ivory") ? "shankh/ivory.png" : "shankh/classic.png");
        if (activeAartiAndShankh) {
            // Active sounding Shankh lifted with 432 Hz golden sonic rings
            float sx = 0.52f, sy = 0.78f;
            float spx = leftOffset + sx * imageWidth;
            float spy = sy * imageWidth;
            for (int r = 1; r <= 3; r++) {
                float rad = imageWidth * (0.055f + r * 0.042f);
                g.setStroke(new BasicStroke(Math.max(2f, imageWidth * 0.0032f)));
                g.setColor(new Color(255, 224, 130, 175 - r * 45));
                g.draw(new Ellipse2D.Float(spx - rad, spy - rad, rad * 2, rad * 2));
            }
            drawSprite(g, loadAsset(shankhAsset), sx, sy, 0.145f, imageWidth, leftOffset, false, -18f, 1f);
        } else {
            drawSprite(g, loadAsset(shankhAsset), 0.633f, 1.145f, 0.132f, imageWidth, leftOffset, false, 0f, 1f);
        }

        // Brass Ghanti (Bell)
        drawSprite(g, loadAsset("accessories/bell.png"), 0.22f, activeAartiAndShankh ? 0.89f : 0.93f, 0.090f, imageWidth, leftOffset, false, activeAartiAndShankh ? 14f : 0f, 1f);

        // Prasad Katori Bowl
        String prasadAsset = switch (prasadId) {
            case "halwa" -> "prasad/halwa.png";
            case "modak" -> "prasad/modak.png";
            case "laddu_bowl" -> "prasad/laddu_bowl.png";
            default -> "prasad/laddu.png";
        };
        drawSprite(g, loadAsset(prasadAsset), 0.50f, 1.215f, 0.136f, imageWidth, leftOffset, false, 0f, 1f);

        // Aarti Diya / Thali
        String aartiAsset = switch (aartiId) {
            case "karpura" -> "aarti/karpura.png";
            case "golden" -> "aarti/golden.png";
            default -> "aarti/traditional.png";
        };
        float aartiX = activeAartiAndShankh ? 0.61f : 0.765f;
        float aartiY = activeAartiAndShankh ? 0.59f : 1.01f;
        drawSprite(g, loadAsset(aartiAsset), aartiX, aartiY, 0.125f, imageWidth, leftOffset, false, 0f, 1f);
        if (litFlames || activeAartiAndShankh) {
            drawDiyaFlame(g, leftOffset + aartiX * imageWidth, (aartiY - 0.068f) * imageWidth, imageWidth * 0.028f);
        }

        // 7. Optional Copper Lota Abhishek water stream from above crown
        if (showAbhishek) {
            float crownX = idolLeft + foreheads[0][0] * idolW;
            float crownY = idolTop + 0.06f * idolH;
            float lotaY = crownY - 0.085f;
            float streamPxX = leftOffset + crownX * imageWidth;
            float streamTopY = (lotaY + 0.025f) * imageWidth;
            float streamBotY = (idolBottom - 0.04f) * imageWidth;

            GradientPaint waterStream = new GradientPaint(
                streamPxX, streamTopY, new Color(210, 240, 255, 220),
                streamPxX, streamBotY, new Color(210, 240, 255, 45)
            );
            g.setPaint(waterStream);
            g.setStroke(new BasicStroke(Math.max(3f, imageWidth * 0.006f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Float(streamPxX, streamTopY, streamPxX, streamBotY));

            drawSprite(g, loadAsset("accessories/copper_lota.png"), crownX, lotaY, 0.095f, imageWidth, leftOffset, false, 22f, 1f);
        }
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

    /** 4-stop radial diya flame per pavitra-mandir-design skill. */
    private static void drawDiyaFlame(Graphics2D g, float cx, float cy, float radius) {
        RadialGradientPaint halo = new RadialGradientPaint(
            new Point2D.Float(cx, cy), radius * 2.4f,
            new float[]{0f, 0.35f, 0.70f, 1f},
            new Color[]{
                new Color(0xFF, 0xFD, 0xF8, 245),
                new Color(0xFF, 0xC1, 0x07, 210),
                new Color(0xFF, 0x6D, 0x00, 115),
                new Color(0xFF, 0x9E, 0x2C, 0)
            }
        );
        g.setPaint(halo);
        g.fill(new Ellipse2D.Float(cx - radius * 2.4f, cy - radius * 2.4f, radius * 4.8f, radius * 4.8f));

        // Teardrop inner flame body
        Path2D.Float flame = new Path2D.Float();
        float fw = radius * 0.52f;
        float fh = radius * 1.15f;
        flame.moveTo(cx, cy - fh);
        flame.curveTo(cx + fw, cy - fh * 0.2f, cx + fw * 0.9f, cy + fh * 0.55f, cx, cy + fh * 0.65f);
        flame.curveTo(cx - fw * 0.9f, cy + fh * 0.55f, cx - fw, cy - fh * 0.2f, cx, cy - fh);
        flame.closePath();
        g.setColor(new Color(0xFF, 0xDF, 0x70));
        g.fill(flame);

        // Karpura white core
        g.setColor(KARPURA_IVORY);
        g.fill(new Ellipse2D.Float(cx - fw * 0.35f, cy - fh * 0.25f, fw * 0.70f, fh * 0.65f));
    }

    private static void drawIncenseSmoke(Graphics2D g, float x, float y, float u) {
        g.setStroke(new BasicStroke(Math.max(2f, u * 0.003f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 2; i++) {
            float dx = (i == 0 ? -1 : 1) * u * 0.008f;
            Path2D.Float wisp = new Path2D.Float();
            wisp.moveTo(x + dx, y);
            wisp.curveTo(x + dx - u * 0.018f, y - u * 0.045f, x + dx + u * 0.020f, y - u * 0.095f, x + dx - u * 0.006f, y - u * 0.145f);
            g.setColor(new Color(235, 228, 220, 70 - i * 20));
            g.draw(wisp);
        }
    }

    // =========================================================================
    // 4. Individual 1.0.19 Screenshot Composers (01..08)
    // =========================================================================
    private static BufferedImage renderScreenshot(int index, int w, int h, boolean hindiLocale) throws IOException {
        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        configureQuality(g);
        float s = w / 1080f; // Scale factor relative to 1080p phone width

        switch (index) {
            case 1 -> renderShot01SacredGateway(g, w, h, s, hindiLocale);
            case 2 -> renderShot02SwarnaSanctumPuja(g, w, h, s, hindiLocale);
            case 3 -> renderShot03RadhaKrishnaMarble(g, w, h, s, hindiLocale);
            case 4 -> renderShot04AartiShankhNaad(g, w, h, s, hindiLocale);
            case 5 -> renderShot05MandirSajayeCustomizer(g, w, h, s, hindiLocale);
            case 6 -> renderShot06BhajanAartiPlayer(g, w, h, s, hindiLocale);
            case 7 -> renderShot07PunyaCoinPassbook(g, w, h, s, hindiLocale);
            case 8 -> renderShot08MorningShodhan(g, w, h, s, hindiLocale);
        }

        g.dispose();
        return canvas;
    }

    /** Top editorial showcase header with large, thumbnail-legible headlines. */
    private static void drawTopEditorialBanner(Graphics2D g, int w, float s, String badgeText, String mainTitle, String subtitle) {
        int bannerH = Math.round(256 * s);
        GradientPaint topGrad = new GradientPaint(
            0, 0, new Color(11, 8, 6, 250),
            0, bannerH, new Color(22, 13, 9, 215)
        );
        g.setPaint(topGrad);
        g.fillRect(0, 0, w, bannerH);

        // Bold gold bottom divider on editorial header
        GradientPaint lineGrad = new GradientPaint(
            w * 0.05f, bannerH, new Color(216, 175, 82, 0),
            w * 0.5f, bannerH, new Color(255, 208, 96, 235),
            true
        );
        g.setPaint(lineGrad);
        g.fillRect(Math.round(w * 0.04f), bannerH - Math.round(3 * s), Math.round(w * 0.92f), Math.round(3 * s));

        // High-contrast golden pill badge
        Font pillFont = new Font("SansSerif", Font.BOLD, Math.round(23 * s));
        g.setFont(pillFont);
        FontMetrics pm = g.getFontMetrics();
        int pillW = pm.stringWidth(badgeText) + Math.round(54 * s);
        int pillH = Math.round(46 * s);
        int pillX = (w - pillW) / 2;
        int pillY = Math.round(22 * s);

        RoundRectangle2D.Float pill = new RoundRectangle2D.Float(pillX, pillY, pillW, pillH, pillH, pillH);
        g.setColor(new Color(216, 152, 42, 68));
        g.fill(pill);
        g.setStroke(new BasicStroke(Math.max(1.6f, 2.0f * s)));
        g.setColor(new Color(0xFF, 0xCA, 0x58));
        g.draw(pill);
        g.setColor(new Color(0xFF, 0xF0, 0xB8));
        g.drawString(badgeText, pillX + Math.round(27 * s), pillY + Math.round(31 * s));

        // Extra-large, thumbnail-readable main headline (auto-fitted to width)
        int titleFontSize = Math.round(56 * s);
        Font titleFont = new Font("Serif", Font.BOLD, titleFontSize);
        g.setFont(titleFont);
        while (g.getFontMetrics().stringWidth(mainTitle) > w - Math.round(64 * s) && titleFontSize > Math.round(38 * s)) {
            titleFontSize -= 2;
            titleFont = new Font("Serif", Font.BOLD, titleFontSize);
            g.setFont(titleFont);
        }
        // Subtle dark drop shadow behind main headline for maximum pop
        g.setColor(new Color(0, 0, 0, 180));
        drawCenteredString(g, mainTitle, w / 2, Math.round(140 * s) + Math.round(3 * s));
        g.setColor(KARPURA_IVORY);
        drawCenteredString(g, mainTitle, w / 2, Math.round(140 * s));

        // High-contrast warm golden-ivory subtitle
        int subFontSize = Math.round(29 * s);
        Font subFont = new Font("SansSerif", Font.BOLD, subFontSize);
        g.setFont(subFont);
        while (g.getFontMetrics().stringWidth(subtitle) > w - Math.round(64 * s) && subFontSize > Math.round(21 * s)) {
            subFontSize -= 1;
            subFont = new Font("SansSerif", Font.BOLD, subFontSize);
            g.setFont(subFont);
        }
        g.setColor(new Color(0xFF, 0xDF, 0x9E));
        drawCenteredString(g, subtitle, w / 2, Math.round(198 * s));

        // App top-right hamburger options button & mini music bar below banner
        int topUiY = bannerH + Math.round(18 * s);
        int optSize = Math.round(64 * s);
        int optX = w - Math.round(36 * s) - optSize;
        RoundRectangle2D.Float optBtn = new RoundRectangle2D.Float(optX, topUiY, optSize, optSize, 18 * s, 18 * s);
        g.setColor(new Color(250, 246, 238, 232));
        g.fill(optBtn);
        g.setStroke(new BasicStroke(1.5f * s));
        g.setColor(RITUAL_GOLD);
        g.draw(optBtn);
        // Hamburger lines
        g.setStroke(new BasicStroke(3f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(RITUAL_INK);
        for (int i = -1; i <= 1; i++) {
            int ly = topUiY + optSize / 2 + Math.round(i * 11 * s);
            g.drawLine(optX + Math.round(18 * s), ly, optX + optSize - Math.round(18 * s), ly);
        }

        // Mini music & streak pill on top-left
        RoundRectangle2D.Float streakPill = new RoundRectangle2D.Float(Math.round(36 * s), topUiY, Math.round(370 * s), optSize, 18 * s, 18 * s);
        g.setColor(new Color(250, 246, 238, 232));
        g.fill(streakPill);
        g.setStroke(new BasicStroke(1.5f * s));
        g.setColor(RITUAL_GOLD);
        g.draw(streakPill);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(22 * s)));
        g.setColor(RITUAL_INK);
        g.drawString("🔥 २१ दिन  •  🪙 १,२५० पुण्य मुद्रा", Math.round(58 * s), topUiY + Math.round(40 * s));
    }

    /** Translucent frosted pearl-sandalwood RitualGlassPanel dock at bottom of screen. */
    private static void drawBottomRitualDock(
        Graphics2D g, int w, int h, float s,
        String stepLabel, String instructionText,
        String primaryActionText, String[] choicePills, int selectedChoiceIdx
    ) {
        boolean hasChoices = choicePills != null && choicePills.length > 0;
        int dockH = Math.round((hasChoices ? 295 : 220) * s);
        int dockX = Math.round(32 * s);
        int dockW = w - Math.round(64 * s);
        int dockY = h - dockH - Math.round(38 * s);

        // Warm ambient drop shadow
        for (int i = 14; i >= 1; i--) {
            g.setColor(new Color(26, 17, 8, 8));
            g.fill(new RoundRectangle2D.Float(dockX - i * 1.2f, dockY - i * 0.6f + 6 * s, dockW + i * 2.4f, dockH + i * 2f, 34 * s, 34 * s));
        }

        // Frosted Sandalwood Ivory surface
        RoundRectangle2D.Float panel = new RoundRectangle2D.Float(dockX, dockY, dockW, dockH, 30 * s, 30 * s);
        GradientPaint surfaceGrad = new GradientPaint(
            0, dockY, new Color(255, 253, 248, 244),
            0, dockY + dockH, new Color(250, 243, 230, 242)
        );
        g.setPaint(surfaceGrad);
        g.fill(panel);

        // 1.5px brass rim
        g.setStroke(new BasicStroke(1.8f * s));
        g.setColor(RITUAL_GOLD);
        g.draw(panel);

        int curY = dockY + Math.round(42 * s);
        // Step progress indicator
        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(21 * s)));
        g.setColor(RITUAL_GOLD);
        g.drawString(stepLabel, dockX + Math.round(32 * s), curY);

        // Instruction label
        curY += Math.round(36 * s);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(25 * s)));
        g.setColor(RITUAL_INK);
        g.drawString(instructionText, dockX + Math.round(32 * s), curY);

        // Optional RitualChoice buttons row
        if (hasChoices) {
            curY += Math.round(20 * s);
            int count = choicePills.length;
            int gap = Math.round(14 * s);
            int totalGap = gap * (count - 1);
            int btnW = (dockW - Math.round(64 * s) - totalGap) / count;
            int btnH = Math.round(62 * s);
            for (int i = 0; i < count; i++) {
                int bx = dockX + Math.round(32 * s) + i * (btnW + gap);
                RoundRectangle2D.Float choiceRect = new RoundRectangle2D.Float(bx, curY, btnW, btnH, 16 * s, 16 * s);
                if (i == selectedChoiceIdx) {
                    g.setColor(new Color(0xF0, 0xE0, 0xC8));
                    g.fill(choiceRect);
                    g.setStroke(new BasicStroke(2.2f * s));
                    g.setColor(RITUAL_GOLD);
                    g.draw(choiceRect);
                } else {
                    g.setColor(new Color(255, 255, 255, 190));
                    g.fill(choiceRect);
                    g.setStroke(new BasicStroke(1.2f * s));
                    g.setColor(new Color(179, 131, 59, 110));
                    g.draw(choiceRect);
                }
                g.setFont(new Font("SansSerif", i == selectedChoiceIdx ? Font.BOLD : Font.PLAIN, Math.round(20 * s)));
                g.setColor(RITUAL_INK);
                drawCenteredString(g, choicePills[i], bx + btnW / 2, curY + Math.round(39 * s));
            }
            curY += btnH;
        }

        // Primary Action Button (Full-width golden-saffron temple button)
        int actionH = Math.round(76 * s);
        int actionY = dockY + dockH - actionH - Math.round(24 * s);
        int actionX = dockX + Math.round(32 * s);
        int actionW = dockW - Math.round(64 * s);
        RoundRectangle2D.Float actionBtn = new RoundRectangle2D.Float(actionX, actionY, actionW, actionH, 20 * s, 20 * s);
        GradientPaint btnGrad = new GradientPaint(
            actionX, actionY, new Color(0x3A, 0x22, 0x14),
            actionX + actionW, actionY + actionH, new Color(0x26, 0x15, 0x0C)
        );
        g.setPaint(btnGrad);
        g.fill(actionBtn);
        g.setStroke(new BasicStroke(2f * s));
        g.setColor(SACRED_GOLD);
        g.draw(actionBtn);

        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(26 * s)));
        g.setColor(KARPURA_IVORY);
        drawCenteredString(g, primaryActionText, actionX + actionW / 2, actionY + Math.round(48 * s));
    }

    // -------------------------------------------------------------------------
    // Screenshot 01: Direct Home Screen Sanctum Darshan & Live Aarti (No Doors)
    // -------------------------------------------------------------------------
    private static void renderShot01SacredGateway(Graphics2D g, int w, int h, float s, boolean hi) throws IOException {
        // Direct entry into the radiant Golden Sanctum Home Screen with active Aarti, Diya & 432 Hz Shankh (door screen removed)
        renderAltarCore(g, w, h, "swarna", "original", "marigold", "brass", "kamal_dhoop", "karpura", "swarna", "laddu_bowl", true, false, true, 0f);

        drawTopEditorialBanner(
            g, w, s,
            hi ? "✨ १००% विज्ञापन-मुक्त  •  सीधा गर्भगृह दर्शन" : "✨ 100% AD-FREE  •  INSTANT HOME DARSHAN",
            hi ? "घर बैठे करें दिव्य दर्शन व आरती" : "Daily Live Puja & Darshan",
            hi ? "बिना किसी विज्ञापन के शांत और पवित्र हिंदू मंदिर" : "100% Ad-Free Hindu Temple with 8 Revered Deities"
        );

        drawBottomRitualDock(
            g, w, h, s,
            hi ? "नित्य पूजा एवं दर्शन  •  १००% विज्ञापन-मुक्त" : "STEP-BY-STEP DAILY PUJA  •  100% AD-FREE",
            hi ? "मंदिर की घंटी बजाएँ, दीप जलाएँ और दिव्य आरती करें" : "Ring temple bells, light ghee diya & perform live Aarti",
            hi ? "🪔  दैनिक पूजा आरंभ करें  •  Start Daily Puja" : "🪔  Start Daily Puja & Aarti  •  पूजा करें",
            new String[]{
                hi ? "✓ १००% विज्ञापन-मुक्त" : "✓ 100% Ad-Free",
                hi ? "✓ ८ देवी-देवता" : "✓ 8 Deities",
                hi ? "✓ भजन व आरती" : "✓ Bhajans & Aarti"
            },
            0
        );
    }

    // -------------------------------------------------------------------------
    // Screenshot 02: Golden Sanctum (Swarna) & Diya/Dhoop Puja
    // -------------------------------------------------------------------------
    private static void renderShot02SwarnaSanctumPuja(Graphics2D g, int w, int h, float s, boolean hi) throws IOException {
        renderAltarCore(g, w, h, "swarna", "original", "marigold", "brass", "kamal_dhoop", "karpura", "swarna", "laddu_bowl", true, false, false, 0f);

        drawTopEditorialBanner(
            g, w, s,
            hi ? "🪔 नित्य दीप एवं धूप  •  स्वर्ण गर्भगृह दर्शन" : "🪔 GOLDEN SANCTUM  •  DIYA & DHOOP",
            hi ? "अखंड घी दीपक और सुगंधित धूप" : "Light Sacred Diya & Dhoop",
            hi ? "पंचमुखी दीप, मयूर दीपस्तंभ और शाही कमल धूप जलाएँ" : "Panchmukhi Ghee Lamp, Mayur Deep & Royal Kamal Dhoop"
        );

        drawBottomRitualDock(
            g, w, h, s,
            "चरण २ / ८  •  दीप एवं धूप प्रज्वलन (DIYA & DHOOP)",
            "गर्भगृह में घी का दीपक और सुगंधित धूप जलाएँ",
            "🪔  दीप एवं धूप जलाएँ  •  Light Diya & Dhoop",
            new String[]{"✓ पंचमुखी दीप", "✓ शाही कमल धूप", "मयूर दीपस्तंभ"},
            0
        );
    }

    // -------------------------------------------------------------------------
    // Screenshot 03: Makrana Marble Shrine, Radha-Krishna & Dev Snan / Pushparpan
    // -------------------------------------------------------------------------
    private static void renderShot03RadhaKrishnaMarble(Graphics2D g, int w, int h, float s, boolean hi) throws IOException {
        renderAltarCore(g, w, h, "marble", "radha_krishna", "rose_jasmine", "peacock", "mayur_dhoop", "golden", "ivory", "modak", true, true, false, 0f);

        drawTopEditorialBanner(
            g, w, s,
            hi ? "🌸 ८-चरणीय पूजा विधि  •  संगमरमर मंदिर" : "🌸 8-STEP VEDIC PUJA  •  MARBLE SHRINE",
            hi ? "जल अभिषेक, तिलक व पुष्पांजलि" : "Abhishek, Tilak & Flowers",
            hi ? "तांबे के कलश से स्नान, चंदन तिलक और ताज़े फूल चढ़ाएँ" : "Bathe Deities with Copper Kalash & Offer Fresh Garlands"
        );

        drawBottomRitualDock(
            g, w, h, s,
            "चरण ३–५ / ८  •  अभिषेक, तिलक एवं पुष्पांजलि",
            "तांबे के कलश से स्नान कराएँ, तिलक लगाएँ व पुष्प अर्पित करें",
            "🌸  पुष्पांजलि अर्पित करें  •  Offer Sacred Flowers",
            new String[]{"✓ कुमकुम तिलक", "✓ गुलाब व मोगरा", "कमल व गेंदा"},
            1
        );
    }

    // -------------------------------------------------------------------------
    // Screenshot 04: Carved Sanctuary, Ram Darbar, Nagkanya Aarti & 432 Hz Shankh
    // -------------------------------------------------------------------------
    private static void renderShot04AartiShankhNaad(Graphics2D g, int w, int h, float s, boolean hi) throws IOException {
        renderAltarCore(g, w, h, "carved", "ram_darbar", "lotus_pearl", "brass", "kamal_dhoop", "karpura", "swarna", "laddu_bowl", true, false, true, 0f);

        drawTopEditorialBanner(
            g, w, s,
            hi ? "🐚 महा-आरती एवं शंखनाद  •  श्री राम दरबार" : "🐚 LIVE AARTI & 432 HZ SHANKH NAAD",
            hi ? "दिव्य आरती, घंटी और शंखनाद" : "Live Aarti, Bell & Shankh",
            hi ? "मंदिर की घंटी और ४३२ Hz शंख ध्वनि के साथ आरती उतारें" : "Perform Circular Aarti with Temple Bells & 432 Hz Conch"
        );

        drawBottomRitualDock(
            g, w, h, s,
            "चरण ६–८ / ८  •  घंटी, आरती, शंखनाद एवं भोग",
            "नागकन्या पंच-आरती उतारें, शंख बजाएँ और भोग अर्पित करें",
            "🐚  आरती एवं शंखनाद  •  Perform Aarti & Shankh",
            new String[]{"✓ नागकन्या आरती", "✓ ४३२ Hz स्वर्ण शंख", "✓ तिरुपति लड्डू भोग"},
            0
        );
    }

    // -------------------------------------------------------------------------
    // Screenshot 05: Mandir Sajaye Customizer (4 Shrines & 8 Deities)
    // -------------------------------------------------------------------------
    private static void renderShot05MandirSajayeCustomizer(Graphics2D g, int w, int h, float s, boolean hi) throws IOException {
        // Upper live shrine preview: Ivory Alcove with Lord Shiva
        renderAltarCore(g, w, h, "ivory", "shiva", "mogra", "peacock", "mayur_dhoop", "golden", "swarna", "modak", true, false, false, 0f);

        drawTopEditorialBanner(
            g, w, s,
            hi ? "🛕 मंदिर सजाएँ  •  ८ पूज्य देवी-देवता" : "🛕 MANDIR SAJAYE  •  8 REVERED DEITIES",
            hi ? "अपने ईष्ट देव का मंदिर सजाएँ" : "8 Deities & Custom Shrines",
            hi ? "गणेश-लक्ष्मी, शिव, राम दरबार, कृष्ण, हनुमान व माँ दुर्गा" : "Ganesha, Shiva, Ram Darbar, Krishna, Hanuman & Durga"
        );

        // Bottom half: Authentic Sandalwood Ivory Mandir Sajaye Sheet
        int sheetY = Math.round(h * 0.42f);
        int sheetH = h - sheetY;
        RoundRectangle2D.Float sheet = new RoundRectangle2D.Float(0, sheetY, w, sheetH + 40 * s, 42 * s, 42 * s);
        g.setColor(SANDALWOOD_IVORY);
        g.fill(sheet);
        g.setStroke(new BasicStroke(2.5f * s));
        g.setColor(RITUAL_GOLD);
        g.draw(sheet);

        int padX = Math.round(40 * s);
        int curY = sheetY + Math.round(52 * s);

        // Header row inside customizer sheet
        g.setFont(new Font("Serif", Font.BOLD, Math.round(32 * s)));
        g.setColor(RITUAL_INK);
        g.drawString("मंदिर सजाएँ  •  Customize Sanctum", padX, curY);

        // Coin balance pill on right
        String coinText = "🪙 १,२५० पुण्य मुद्रा";
        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(21 * s)));
        int cw = g.getFontMetrics().stringWidth(coinText) + Math.round(36 * s);
        RoundRectangle2D.Float coinPill = new RoundRectangle2D.Float(w - padX - cw, curY - Math.round(32 * s), cw, Math.round(44 * s), 22 * s, 22 * s);
        g.setColor(BISQUE_SANDSTONE);
        g.fill(coinPill);
        g.setColor(RITUAL_GOLD);
        g.draw(coinPill);
        g.setColor(RITUAL_INK);
        g.drawString(coinText, w - padX - cw + Math.round(18 * s), curY - Math.round(3 * s));

        // Category tabs row
        curY += Math.round(30 * s);
        String[] tabs = new String[]{"मूर्तियाँ (8)", "मंदिर (4)", "माला (7)", "दीपक (3)", "धूप (4)", "भोग (4)"};
        int tabGap = Math.round(12 * s);
        int tabW = (w - padX * 2 - tabGap * (tabs.length - 1)) / tabs.length;
        int tabH = Math.round(54 * s);
        for (int i = 0; i < tabs.length; i++) {
            int tx = padX + i * (tabW + tabGap);
            RoundRectangle2D.Float tr = new RoundRectangle2D.Float(tx, curY, tabW, tabH, 16 * s, 16 * s);
            if (i == 0) {
                g.setColor(new Color(0x36, 0x20, 0x13));
                g.fill(tr);
                g.setColor(SACRED_GOLD);
                g.draw(tr);
                g.setFont(new Font("SansSerif", Font.BOLD, Math.round(18 * s)));
                g.setColor(KARPURA_IVORY);
            } else {
                g.setColor(BISQUE_SANDSTONE);
                g.fill(tr);
                g.setColor(new Color(179, 131, 59, 110));
                g.draw(tr);
                g.setFont(new Font("SansSerif", Font.PLAIN, Math.round(18 * s)));
                g.setColor(RITUAL_INK);
            }
            drawCenteredString(g, tabs[i], tx + tabW / 2, curY + Math.round(34 * s));
        }

        // 2x4 Grid of the 8 Deities with real idol thumbnails
        curY += tabH + Math.round(26 * s);
        String[][] idolCards = new String[][]{
            {"idols/original.png", "गणेश एवं लक्ष्मी", "Ganesha & Lakshmi", "✓ लागू (Applied)"},
            {"idols/radha_krishna.png", "राधा कृष्ण", "Radha & Krishna", "✓ अनलॉक (Owned)"},
            {"idols/shiva.png", "भगवान शिव", "Lord Shiva", "✓ देख रहे हैं (Preview)"},
            {"idols/ram_darbar.png", "श्री राम दरबार", "Ram Darbar", "✓ अनलॉक (Owned)"},
            {"idols/durga.png", "माँ दुर्गा", "Maa Durga", "🪙 १५० पुण्य मुद्रा"},
            {"idols/hanuman.png", "हनुमान जी", "Lord Hanuman", "✓ अनलॉक (Owned)"},
            {"idols/lakshmi.png", "माँ लक्ष्मी", "Goddess Lakshmi", "🪙 १५० पुण्य मुद्रा"},
            {"idols/ganesh_hanuman.png", "गणेश व हनुमान", "Ganesha & Hanuman", "✓ अनलॉक (Owned)"}
        };

        int cols = 4;
        int rows = 2;
        int gridGapX = Math.round(16 * s);
        int gridGapY = Math.round(18 * s);
        int cardW = (w - padX * 2 - gridGapX * (cols - 1)) / cols;
        int availGridH = h - curY - Math.round(140 * s);
        int cardH = (availGridH - gridGapY) / rows;

        for (int i = 0; i < idolCards.length; i++) {
            int c = i % cols;
            int r = i / cols;
            int cx = padX + c * (cardW + gridGapX);
            int cy = curY + r * (cardH + gridGapY);

            RoundRectangle2D.Float cr = new RoundRectangle2D.Float(cx, cy, cardW, cardH, 20 * s, 20 * s);
            boolean active = (i == 2);
            g.setColor(active ? new Color(0xF0, 0xDF, 0xC3) : Color.WHITE);
            g.fill(cr);
            g.setStroke(new BasicStroke(active ? 2.5f * s : 1.2f * s));
            g.setColor(active ? RITUAL_GOLD : new Color(179, 131, 59, 95));
            g.draw(cr);

            // Thumbnail image centered in upper part of card
            BufferedImage thumb = loadAsset(idolCards[i][0]);
            int maxThumbH = Math.round(cardH * 0.54f);
            int maxThumbW = cardW - Math.round(24 * s);
            float tRatio = Math.min((float) maxThumbW / thumb.getWidth(), (float) maxThumbH / thumb.getHeight());
            int tw = Math.round(thumb.getWidth() * tRatio);
            int th = Math.round(thumb.getHeight() * tRatio);
            g.drawImage(thumb, cx + (cardW - tw) / 2, cy + Math.round(14 * s) + (maxThumbH - th) / 2, tw, th, null);

            g.setFont(new Font("SansSerif", Font.BOLD, Math.round(18 * s)));
            g.setColor(RITUAL_INK);
            drawCenteredString(g, idolCards[i][1], cx + cardW / 2, cy + cardH - Math.round(64 * s));

            g.setFont(new Font("SansSerif", Font.PLAIN, Math.round(14 * s)));
            g.setColor(RITUAL_SUBINK);
            drawCenteredString(g, idolCards[i][2], cx + cardW / 2, cy + cardH - Math.round(42 * s));

            g.setFont(new Font("SansSerif", Font.BOLD, Math.round(14 * s)));
            g.setColor(active ? CEREMONIAL_KUMKUM : RITUAL_GOLD);
            drawCenteredString(g, idolCards[i][3], cx + cardW / 2, cy + cardH - Math.round(16 * s));
        }

        // Pinned bottom action bar (Preview & Apply)
        int barY = h - Math.round(104 * s);
        int btnW = (w - padX * 2 - Math.round(20 * s)) / 2;
        int btnH = Math.round(74 * s);

        RoundRectangle2D.Float prevBtn = new RoundRectangle2D.Float(padX, barY, btnW, btnH, 18 * s, 18 * s);
        g.setColor(BISQUE_SANDSTONE);
        g.fill(prevBtn);
        g.setStroke(new BasicStroke(1.8f * s));
        g.setColor(RITUAL_GOLD);
        g.draw(prevBtn);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(24 * s)));
        g.setColor(RITUAL_INK);
        drawCenteredString(g, "👁️  देखें  •  Preview", padX + btnW / 2, barY + Math.round(46 * s));

        RoundRectangle2D.Float applyBtn = new RoundRectangle2D.Float(padX + btnW + Math.round(20 * s), barY, btnW, btnH, 18 * s, 18 * s);
        g.setColor(new Color(0x36, 0x20, 0x13));
        g.fill(applyBtn);
        g.setColor(SACRED_GOLD);
        g.draw(applyBtn);
        g.setColor(KARPURA_IVORY);
        drawCenteredString(g, "✓  लगाएँ  •  Apply to Mandir", padX + btnW + Math.round(20 * s) + btnW / 2, barY + Math.round(46 * s));
    }

    // -------------------------------------------------------------------------
    // Screenshot 06: Devotional Bhajan & Aarti Music Player
    // -------------------------------------------------------------------------
    private static void renderShot06BhajanAartiPlayer(Graphics2D g, int w, int h, float s, boolean hi) throws IOException {
        renderAltarCore(g, w, h, "swarna", "durga", "marigold", "brass", "kamal_dhoop", "karpura", "swarna", "laddu_bowl", true, false, false, 0f);

        drawTopEditorialBanner(
            g, w, s,
            hi ? "🎵 भजन, आरती एवं चालीसा  •  ऑफ़लाइन प्लेयर" : "🎵 BHAJANS, AARTIS & HANUMAN CHALISA",
            hi ? "मधुर भजन, आरती और चालीसा" : "Bhajans, Aartis & Chalisa",
            hi ? "हनुमान चालीसा, सुबह-शाम की आरती और ऑफ़लाइन भजन संग्रह" : "Listen to Hanuman Chalisa, Daily Aartis & Offline Bhajans"
        );

        int sheetY = Math.round(h * 0.40f);
        int sheetH = h - sheetY;
        RoundRectangle2D.Float sheet = new RoundRectangle2D.Float(0, sheetY, w, sheetH + 40 * s, 42 * s, 42 * s);
        g.setColor(SANDALWOOD_IVORY);
        g.fill(sheet);
        g.setStroke(new BasicStroke(2.5f * s));
        g.setColor(RITUAL_GOLD);
        g.draw(sheet);

        int padX = Math.round(42 * s);
        int curY = sheetY + Math.round(52 * s);

        g.setFont(new Font("Serif", Font.BOLD, Math.round(32 * s)));
        g.setColor(RITUAL_INK);
        g.drawString("🎵  भजन एवं आरती संगीत  •  Sacred Audio", padX, curY);

        // Search input bar
        curY += Math.round(24 * s);
        int searchH = Math.round(64 * s);
        RoundRectangle2D.Float searchBox = new RoundRectangle2D.Float(padX, curY, w - padX * 2, searchH, 18 * s, 18 * s);
        g.setColor(Color.WHITE);
        g.fill(searchBox);
        g.setStroke(new BasicStroke(1.5f * s));
        g.setColor(RITUAL_GOLD);
        g.draw(searchBox);
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.round(22 * s)));
        g.setColor(RITUAL_SUBINK);
        g.drawString("🔍  भजन, आरती या देवता खोजें (Search Bhajans, Aartis, Chalisas)...", padX + Math.round(24 * s), curY + Math.round(40 * s));

        // Deity filter chips
        curY += searchH + Math.round(20 * s);
        String[] filters = new String[]{"✓ सभी भजन (All)", "गणेश-लक्ष्मी", "शिव भजन", "राधा-कृष्ण", "श्री राम-हनुमान", "दुर्गा माँ"};
        int chipX = padX;
        int chipH = Math.round(48 * s);
        for (int i = 0; i < filters.length; i++) {
            g.setFont(new Font("SansSerif", i == 0 ? Font.BOLD : Font.PLAIN, Math.round(18 * s)));
            int cw = g.getFontMetrics().stringWidth(filters[i]) + Math.round(32 * s);
            if (chipX + cw > w - padX) break;
            RoundRectangle2D.Float fc = new RoundRectangle2D.Float(chipX, curY, cw, chipH, 24 * s, 24 * s);
            g.setColor(i == 0 ? new Color(0x36, 0x20, 0x13) : BISQUE_SANDSTONE);
            g.fill(fc);
            g.setColor(RITUAL_GOLD);
            g.draw(fc);
            g.setColor(i == 0 ? KARPURA_IVORY : RITUAL_INK);
            g.drawString(filters[i], chipX + Math.round(16 * s), curY + Math.round(31 * s));
            chipX += cw + Math.round(12 * s);
        }

        // Track rows
        curY += chipH + Math.round(22 * s);
        String[][] tracks = new String[][]{
            {"जय गणेश जय गणेश देवा — आरती", "Shri Ganesha Aarti  •  04:12  •  ऑफ़लाइन उपलब्ध", "🔊 बज रहा है"},
            {"ओम जय जगदीश हरे — महा आरती", "Universal Evening Aarti  •  05:08  •  ऑफ़लाइन उपलब्ध", "▶ चलाएँ"},
            {"अच्युतम् केशवम् कृष्ण दामोदरम्", "Radha Krishna Bhajan  •  06:24  •  ऑफ़लाइन उपलब्ध", "▶ चलाएँ"},
            {"श्री हनुमान चालीसा — संपूर्ण पाठ", "Sankatmochan Hanuman Chalisa  •  09:15  •  ऑफ़लाइन", "▶ चलाएँ"},
            {"जय अम्बे गौरी — माँ दुर्गा आरती", "Maa Durga Shakti Aarti  •  04:55  •  ऑफ़लाइन उपलब्ध", "▶ चलाएँ"},
            {"कर्पूरगौरं करुणावतारं — शिव स्तुति", "Lord Shiva Mantra & Stuti  •  03:48  •  ऑफ़लाइन उपलब्ध", "▶ चलाएँ"}
        };

        int rowH = Math.round(86 * s);
        int rowGap = Math.round(12 * s);
        for (int i = 0; i < tracks.length; i++) {
            if (curY + rowH > h - Math.round(125 * s)) break;
            RoundRectangle2D.Float tr = new RoundRectangle2D.Float(padX, curY, w - padX * 2, rowH, 18 * s, 18 * s);
            boolean playing = (i == 0);
            g.setColor(playing ? new Color(0xF0, 0xDF, 0xC3) : Color.WHITE);
            g.fill(tr);
            g.setStroke(new BasicStroke(playing ? 2.2f * s : 1.2f * s));
            g.setColor(playing ? RITUAL_GOLD : new Color(179, 131, 59, 90));
            g.draw(tr);

            g.setFont(new Font("SansSerif", Font.BOLD, Math.round(22 * s)));
            g.setColor(RITUAL_INK);
            g.drawString(tracks[i][0], padX + Math.round(24 * s), curY + Math.round(36 * s));

            g.setFont(new Font("SansSerif", Font.PLAIN, Math.round(17 * s)));
            g.setColor(RITUAL_SUBINK);
            g.drawString(tracks[i][1], padX + Math.round(24 * s), curY + Math.round(66 * s));

            // Right status pill
            g.setFont(new Font("SansSerif", Font.BOLD, Math.round(18 * s)));
            int bw = g.getFontMetrics().stringWidth(tracks[i][2]) + Math.round(32 * s);
            RoundRectangle2D.Float bp = new RoundRectangle2D.Float(w - padX - bw - Math.round(20 * s), curY + Math.round(21 * s), bw, Math.round(44 * s), 22 * s, 22 * s);
            g.setColor(playing ? new Color(0x36, 0x20, 0x13) : BISQUE_SANDSTONE);
            g.fill(bp);
            g.setColor(playing ? KARPURA_IVORY : RITUAL_INK);
            g.drawString(tracks[i][2], w - padX - bw - Math.round(4 * s), curY + Math.round(50 * s));

            curY += rowH + rowGap;
        }

        // Bottom Now-Playing Transport Bar
        int barH = Math.round(92 * s);
        int barY = h - barH - Math.round(22 * s);
        RoundRectangle2D.Float nowBar = new RoundRectangle2D.Float(padX, barY, w - padX * 2, barH, 22 * s, 22 * s);
        g.setColor(new Color(0x28, 0x17, 0x0E));
        g.fill(nowBar);
        g.setStroke(new BasicStroke(2f * s));
        g.setColor(SACRED_GOLD);
        g.draw(nowBar);

        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(22 * s)));
        g.setColor(KARPURA_IVORY);
        g.drawString("🎶  जय गणेश जय गणेश देवा", padX + Math.round(28 * s), barY + Math.round(40 * s));
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.round(17 * s)));
        g.setColor(SANDALWOOD_DUST);
        g.drawString("रिपीट मोड चालू (Repeat)  •  ऑफ़लाइन कैश्ड", padX + Math.round(28 * s), barY + Math.round(68 * s));

        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(26 * s)));
        g.setColor(SACRED_GOLD);
        g.drawString("⏮    ⏸    ⏭    🔁", w - padX - Math.round(260 * s), barY + Math.round(56 * s));
    }

    // -------------------------------------------------------------------------
    // Screenshot 07: Devotion Streak & Punya Coin Passbook
    // -------------------------------------------------------------------------
    private static void renderShot07PunyaCoinPassbook(Graphics2D g, int w, int h, float s, boolean hi) throws IOException {
        renderAltarCore(g, w, h, "swarna", "original", "marigold", "brass", "kamal_dhoop", "karpura", "swarna", "laddu_bowl", true, false, false, 0f);

        drawTopEditorialBanner(
            g, w, s,
            hi ? "🔥 नित्य साधना नियम  •  पुण्य मुद्रा पासबुक" : "🔥 DAILY PUJA STREAK  •  PUNYA PASSBOOK",
            hi ? "दैनिक पूजा नियम और पुण्य मुद्रा" : "Build Your Daily Puja Habit",
            hi ? "रोज़ पूजा कर पुण्य मुद्रा कमाएँ और नए मंदिर अलंकरण खोलें" : "Track Devotion Streaks & Unlock Free Temple Ornaments"
        );

        int sheetY = Math.round(h * 0.38f);
        int sheetH = h - sheetY;
        RoundRectangle2D.Float sheet = new RoundRectangle2D.Float(0, sheetY, w, sheetH + 40 * s, 42 * s, 42 * s);
        g.setColor(SANDALWOOD_IVORY);
        g.fill(sheet);
        g.setStroke(new BasicStroke(2.5f * s));
        g.setColor(RITUAL_GOLD);
        g.draw(sheet);

        int padX = Math.round(42 * s);
        int curY = sheetY + Math.round(46 * s);

        // Two summary hero cards: Streak & Punya Coins
        int cardGap = Math.round(20 * s);
        int cardW = (w - padX * 2 - cardGap) / 2;
        int cardH = Math.round(150 * s);

        RoundRectangle2D.Float streakCard = new RoundRectangle2D.Float(padX, curY, cardW, cardH, 22 * s, 22 * s);
        g.setColor(new Color(0x36, 0x20, 0x13));
        g.fill(streakCard);
        g.setColor(SACRED_GOLD);
        g.draw(streakCard);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(36 * s)));
        g.setColor(HONEY_SAFFRON);
        g.drawString("🔥 २१ दिन", padX + Math.round(26 * s), curY + Math.round(62 * s));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(20 * s)));
        g.setColor(KARPURA_IVORY);
        g.drawString("नित्य उपासना निरंतरता", padX + Math.round(26 * s), curY + Math.round(100 * s));
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.round(16 * s)));
        g.setColor(SANDALWOOD_DUST);
        g.drawString("21-Day Worship Streak", padX + Math.round(26 * s), curY + Math.round(128 * s));

        int rightX = padX + cardW + cardGap;
        RoundRectangle2D.Float coinCard = new RoundRectangle2D.Float(rightX, curY, cardW, cardH, 22 * s, 22 * s);
        g.setColor(new Color(0xF0, 0xDF, 0xC3));
        g.fill(coinCard);
        g.setColor(RITUAL_GOLD);
        g.draw(coinCard);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(36 * s)));
        g.setColor(RITUAL_INK);
        g.drawString("🪙 १,२५०", rightX + Math.round(26 * s), curY + Math.round(62 * s));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(20 * s)));
        g.setColor(RITUAL_INK);
        g.drawString("कुल पुण्य मुद्रा बैलेंस", rightX + Math.round(26 * s), curY + Math.round(100 * s));
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.round(16 * s)));
        g.setColor(RITUAL_SUBINK);
        g.drawString("Google क्लाउड से सुरक्षित", rightX + Math.round(26 * s), curY + Math.round(128 * s));

        // WhatsApp Referral Banner
        curY += cardH + Math.round(22 * s);
        int waH = Math.round(92 * s);
        RoundRectangle2D.Float waCard = new RoundRectangle2D.Float(padX, curY, w - padX * 2, waH, 20 * s, 20 * s);
        g.setColor(new Color(0xE9, 0xF7, 0xEF));
        g.fill(waCard);
        g.setStroke(new BasicStroke(1.8f * s));
        g.setColor(WHATSAPP_GREEN);
        g.draw(waCard);

        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(22 * s)));
        g.setColor(new Color(0x14, 0x5A, 0x32));
        g.drawString("💬  परिवार व मित्रों को WhatsApp पर आमंत्रित करें", padX + Math.round(24 * s), curY + Math.round(38 * s));
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.round(18 * s)));
        g.drawString("प्रत्येक सफल आमंत्रण पर +५० पुण्य मुद्रा पाएं  •  Earn +50 Coins per invite", padX + Math.round(24 * s), curY + Math.round(68 * s));

        // Passbook History Ledger
        curY += waH + Math.round(34 * s);
        g.setFont(new Font("Serif", Font.BOLD, Math.round(26 * s)));
        g.setColor(RITUAL_INK);
        g.drawString("पुण्य मुद्रा पासबुक  •  Recent Passbook History", padX, curY);

        curY += Math.round(18 * s);
        String[][] ledger = new String[][]{
            {"प्रातः अष्टपदी नित्य पूजा पूर्ण पुरस्कार", "आज, प्रातः ०७:१५  •  Daily Puja Completed", "+२५ मुद्रा", "true"},
            {"व्हाट्सऐप परिवार आमंत्रण बोनस", "कल, सायं ०६:४०  •  WhatsApp Referral Bonus", "+५० मुद्रा", "true"},
            {"स्वर्ण गर्भगृह मंदिर अलंकरण अनलॉक", "१८ अक्टूबर  •  Golden Sanctum Unlocked", "-२०० मुद्रा", "false"},
            {"२१-दिवसीय साधना निरंतरता विशेष पुरस्कार", "१७ अक्टूबर  •  21-Day Streak Milestone", "+१०० मुद्रा", "true"},
            {"शाही कमल धूप एवं पंचमुखी दीप अनलॉक", "१५ अक्टूबर  •  Royal Kamal Dhoop Unlocked", "-८० मुद्रा", "false"}
        };

        int lRowH = Math.round(80 * s);
        int lGap = Math.round(10 * s);
        for (String[] item : ledger) {
            if (curY + lRowH > h - Math.round(28 * s)) break;
            RoundRectangle2D.Float lr = new RoundRectangle2D.Float(padX, curY, w - padX * 2, lRowH, 16 * s, 16 * s);
            g.setColor(Color.WHITE);
            g.fill(lr);
            g.setStroke(new BasicStroke(1.2f * s));
            g.setColor(new Color(179, 131, 59, 85));
            g.draw(lr);

            g.setFont(new Font("SansSerif", Font.BOLD, Math.round(20 * s)));
            g.setColor(RITUAL_INK);
            g.drawString(item[0], padX + Math.round(22 * s), curY + Math.round(34 * s));

            g.setFont(new Font("SansSerif", Font.PLAIN, Math.round(16 * s)));
            g.setColor(RITUAL_SUBINK);
            g.drawString(item[1], padX + Math.round(22 * s), curY + Math.round(60 * s));

            boolean credit = Boolean.parseBoolean(item[3]);
            g.setFont(new Font("SansSerif", Font.BOLD, Math.round(22 * s)));
            g.setColor(credit ? WHATSAPP_GREEN : CEREMONIAL_KUMKUM);
            int aw = g.getFontMetrics().stringWidth(item[2]);
            g.drawString(item[2], w - padX - aw - Math.round(24 * s), curY + Math.round(48 * s));

            curY += lRowH + lGap;
        }
    }

    // -------------------------------------------------------------------------
    // Screenshot 08: Morning Temple Shodhan (मंदिर शुद्धि एवं सफाई)
    // -------------------------------------------------------------------------
    private static void renderShot08MorningShodhan(Graphics2D g, int w, int h, float s, boolean hi) throws IOException {
        renderAltarCore(g, w, h, "carved", "ganesh_hanuman", "marigold", "brass", "agarbatti", "traditional", "classic", "laddu", false, false, false, 0.85f);

        // Subtle morning dust & cobweb shimmer on upper corners before cleaning
        g.setColor(new Color(210, 195, 175, 42));
        g.fillOval(Math.round(w * 0.18f), Math.round(h * 0.56f), Math.round(w * 0.64f), Math.round(h * 0.14f));

        // Sparkle cleansing indicators over altar
        g.setFont(new Font("SansSerif", Font.BOLD, Math.round(34 * s)));
        g.setColor(new Color(255, 240, 185, 230));
        g.drawString("✨ वेदी स्वच्छ करें  •  Swipe to Clean Altar ✨", Math.round(w * 0.19f), Math.round(h * 0.64f));

        drawTopEditorialBanner(
            g, w, s,
            hi ? "🧹 प्रातः मंदिर शुद्धि  •  निर्माल्य विसर्जन" : "🧹 MORNING TEMPLE CARE  •  ALTAR SHODHAN",
            hi ? "प्रातः मंदिर सफाई व निर्माल्य" : "Real Morning Temple Care",
            hi ? "घर के मंदिर की तरह सुबह पुराने फूल हटाएँ और वेदी साफ़ करें" : "Clear Yesterday's Flowers & Purify the Altar Every Morning"
        );

        drawBottomRitualDock(
            g, w, h, s,
            "प्रातः काल शुद्धि  •  MORNING PURIFICATION",
            "सूखे फूल (निर्माल्य) हटाएँ और वेदी को पोंछकर स्वच्छ करें",
            "🧹  पुराने फूल हटाएँ व साफ़ करें  •  Clean Sanctum",
            new String[]{"✓ निर्माल्य समेटें (Clear Flowers)", "वेदी पोंछें (Wipe Altar)"},
            0
        );
    }

    private static void drawCenteredString(Graphics2D g, String text, int centerX, int baselineY) {
        FontMetrics fm = g.getFontMetrics();
        int x = centerX - fm.stringWidth(text) / 2;
        g.drawString(text, x, baselineY);
    }
}
