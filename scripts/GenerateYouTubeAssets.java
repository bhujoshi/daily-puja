import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Generates YouTube Studio compliant Channel Avatar (800x800) & Banner (2048x1152, <= 6MB)
 * keeping all critical text & artwork strictly inside YouTube's 1235x338 all-device safe zone
 * (x = 406..1642, y = 407..745).
 */
public class GenerateYouTubeAssets {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        File root = new File("/Users/bhuwanjoshi/work/worship");
        File outDir = new File(root, "marketing/youtube_shorts");
        outDir.mkdirs();

        // 1. Copy/scale 800x800 YouTube Avatar from our 1024x1024 profile
        BufferedImage profSrc = ImageIO.read(new File(root, "marketing/facebook_reels/page_assets/facebook_profile_1024x1024.png"));
        BufferedImage ytAvatar = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D ag = ytAvatar.createGraphics();
        ag.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        ag.drawImage(profSrc, 0, 0, 800, 800, null);
        ag.dispose();
        ImageIO.write(ytAvatar, "png", new File(outDir, "youtube_profile_800x800.png"));

        // 2. Render 2048x1152 YouTube Banner with center safe zone (x=406..1642, y=407..745)
        int w = 2048, h = 1152;
        BufferedImage banner = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = banner.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Deep Obsidian-Umber Garbhagriha backdrop for TV outer area
        GradientPaint bg = new GradientPaint(0, 0, new Color(0x0D, 0x09, 0x07), w, h, new Color(0x22, 0x12, 0x0A));
        g.setPaint(bg);
        g.fillRect(0, 0, w, h);

        // Subtle golden glow in center safe area
        RadialGradientPaint glow = new RadialGradientPaint(
            new Point2D.Float(1380f, 576f), 420f,
            new float[]{0f, 0.6f, 1f},
            new Color[]{new Color(255, 190, 72, 95), new Color(216, 130, 32, 35), new Color(13, 9, 7, 0)}
        );
        g.setPaint(glow);
        g.fillRect(0, 0, w, h);

        // Subtle horizontal gold hairlines framing the desktop/mobile banner strip (y=365..787)
        g.setColor(new Color(216, 175, 82, 85));
        g.fillRect(250, 368, w - 500, 2);
        g.fillRect(250, 784, w - 500, 2);

        // Right shrine preview inside safe zone (x=1260..1605, y=416..736 -> 345x320)
        BufferedImage coverPreview = ImageIO.read(new File(root, "marketing/facebook_reels/covers/05_friday_lakshmi_adfree_cover.png"));
        int cx = 1275, cy = 414, cw = 330, ch = 324;
        Shape clip = new RoundRectangle2D.Float(cx, cy, cw, ch, 24f, 24f);
        Shape oldClip = g.getClip();
        g.setClip(clip);
        // Crop central idol/sanctum area from 1080x1920 cover (x=120..960, y=340..1150)
        g.drawImage(coverPreview, cx, cy, cx + cw, cy + ch, 120, 340, 960, 1150, null);
        g.setClip(oldClip);
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(new Color(0xD8, 0xAF, 0x52));
        g.draw(clip);

        // Left copy strictly inside x=435..1235, y=420..732
        int lx = 440;
        RoundRectangle2D.Float pill = new RoundRectangle2D.Float(lx, 422, 365, 38, 38, 38);
        g.setColor(new Color(216, 175, 82, 48));
        g.fill(pill);
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(0xD8, 0xAF, 0x52));
        g.draw(pill);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(0xFF, 0xEC, 0xB3));
        g.drawString("१००% विज्ञापन-मुक्त • 100% AD-FREE", lx + 20, 447);

        g.setFont(new Font("Serif", Font.BOLD, 56));
        g.setColor(new Color(0xFF, 0xFD, 0xF8));
        g.drawString("पवित्र मंदिर • Pavitra Mandir", lx, 518);

        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(new Color(0xFF, 0xDE, 0x99));
        g.drawString("नित्य पूजा, दिव्य आरती, भजन एवं ४३२ Hz शंखनाद", lx, 562);

        g.setFont(new Font("SansSerif", Font.PLAIN, 21));
        g.setColor(new Color(0xCF, 0xC2, 0xB2));
        g.drawString("८ देवी-देवता • ४ भव्य मंदिर • प्रातः निर्माल्य शुद्धि • बुजुर्गों के लिए सरल", lx, 604);

        String cta = "▶  Google Play पर निशुल्क डाउनलोड करें (Pavitra Mandir)";
        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        int bw = g.getFontMetrics().stringWidth(cta) + 48;
        RoundRectangle2D.Float btn = new RoundRectangle2D.Float(lx, 636, bw, 52, 16, 16);
        g.setColor(new Color(0xD8, 0xAF, 0x52));
        g.fill(btn);
        g.setColor(new Color(0x26, 0x1C, 0x16));
        g.drawString(cta, lx + 24, 669);

        g.dispose();
        File bannerFile = new File(outDir, "youtube_banner_2048x1152.png");
        ImageIO.write(banner, "png", bannerFile);
        System.out.printf("[OK] youtube_banner_2048x1152.png (%.2f MB)%n", bannerFile.length() / (1024.0 * 1024.0));
    }
}
