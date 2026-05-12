package autobattler.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class VFXManager {

    // List vfx toàn cục dùng thread-safe
    private static final List<VFX> activeEffects = new CopyOnWriteArrayList<>();

    public static abstract class VFX {
        public boolean active = true;
        public abstract void update();
        public abstract void draw(Graphics2D g2);
    }

    // Rung màn hình (Screen Shake)
    public static int shakeX = 0;
    public static int shakeY = 0;
    private static int shakeDuration = 0;
    private static int shakeIntensity = 0;
    private static final java.util.Random rand = new java.util.Random();

    public static void triggerShake(int intensity, int frames) {
        shakeIntensity = intensity;
        shakeDuration = frames;
    }

    /** Cập nhật toàn bộ hiệu ứng (gọi trong Board.update) */
    public static void updateAll() {
        // Cập nhật Screen Shake
        if (shakeDuration > 0) {
            shakeX = rand.nextInt(shakeIntensity * 2 + 1) - shakeIntensity;
            shakeY = rand.nextInt(shakeIntensity * 2 + 1) - shakeIntensity;
            shakeDuration--;
            if (shakeDuration <= 0) {
                shakeX = 0; shakeY = 0;
            }
        }

        for (VFX vfx : activeEffects) {
            vfx.update();
            if (!vfx.active) activeEffects.remove(vfx);
        }
    }

    /** Vẽ toàn bộ hiệu ứng (gọi trong Board.draw) */
    public static void drawAll(Graphics2D g2) {
        for (VFX vfx : activeEffects) {
            if (vfx.active) vfx.draw(g2);
        }
    }

    /** Xóa sạch vfx (dùng khi reset round) */
    public static void clear() {
        activeEffects.clear();
        shakeX = 0; shakeY = 0; shakeDuration = 0;
    }

    /** Đăng ký hiệu ứng mới */
    public static void add(VFX vfx) {
        activeEffects.add(vfx);
    }

    // =========================================================================
    // 1. SLASH EFFECT (HIỆU ỨNG CHÉM CHẠY SÓNG)
    // =========================================================================
    public static class SlashEffect extends VFX {
        int cx, cy; // center location
        int w, h;
        Color color;
        double angle;
        int maxLife = 20; 
        int currentLife;
        int delay = 0; 

        public SlashEffect(int cx, int cy, int w, int h, Color c, double angle, int delay) {
            this.cx = cx; this.cy = cy; this.w = w; this.h = h;
            this.color = c; this.angle = angle;
            this.delay = delay;
            this.currentLife = maxLife;
        }

        @Override
        public void update() {
            if (delay > 0) { 
                delay--; 
                // Ngay khoảnh khắc hết delay (nếu là đòn nặng Sarga), nổ rung màn hình!
                if (delay == 0 && w > 150) {
                    VFXManager.triggerShake(8, 15);
                }
                return; 
            }
            currentLife--;
            if (currentLife <= 0) active = false;
        }

        @Override
        public void draw(Graphics2D g2) {
            if (delay > 0) return;

            AffineTransform old = g2.getTransform();
            g2.translate(cx, cy);
            g2.rotate(angle);

            // Tính toán tỷ lệ phát triển (chạy từ 0 -> 1 -> 0)
            float lifeRatio = (float) (maxLife - currentLife) / maxLife;
            
            // Phase 1: Arc Sweep (Sóng lan tỏa trong 40% đời đầu)
            float sweepRatio = Math.min(1.0f, lifeRatio / 0.4f);
            // Phase 2: Alpha Fade (Mờ dần trong 60% đời sau)
            float alphaRatio = 1.0f - Math.max(0, (lifeRatio - 0.4f) / 0.6f);
            int alpha = (int)(255 * alphaRatio);
            
            java.awt.Stroke oldStroke = g2.getStroke();
            
            // Quyết định bao nhiêu "tầng" nét chém: Đòn bự 3 tầng, đòn nhỏ 1
            int layers = (w > 150) ? 3 : 1;
            
            for (int l = 0; l < layers; l++) {
                int offset = (l - layers/2) * 15;
                int curW = w - Math.abs(offset);
                int curH = h - Math.abs(offset);

                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, alpha)));
                g2.setStroke(new BasicStroke(5.0f * alphaRatio + 2));
                
                // Vẽ "Sóng": Sweep tăng dần từ 0 lên 140 độ, đối xứng qua vector trục (-70 -> 70)
                g2.drawArc(-curW/2, -curH/2 + offset, curW, curH, -70, (int)(140 * sweepRatio));
                
                // Vệt sáng trắng lõi (lướt theo, hẹp hơn xíu)
                g2.setStroke(new BasicStroke(2.0f * alphaRatio + 1));
                g2.setColor(new Color(255, 255, 255, alpha));
                g2.drawArc(-curW/2, -curH/2 + offset, curW, curH, -60, (int)(120 * sweepRatio));
            }

            g2.setStroke(oldStroke);
            g2.setTransform(old);
        }
    }

    // =========================================================================
    // 2. PROJECTILE EFFECT (QUẢ CẦU BAY)
    // =========================================================================
    public static class ProjectileEffect extends VFX {
        double x, y;
        double tX, tY;
        double speed = 5.0;
        int damage;
        Hero source, target;
        List<double[]> trail = new ArrayList<>();

        public ProjectileEffect(int startX, int startY, int targetX, int targetY, int dmg, Hero src, Hero tgt) {
            this.x = startX; this.y = startY;
            this.tX = targetX; this.tY = targetY;
            this.damage = dmg;
            this.source = src;
            this.target = tgt;
        }

        @Override
        public void update() {
            // Nếu mục tiêu chết trước khi đòn bay tới, tự động nổ hoặc bay tiếp (cho biến mất)
            if (target.state == Hero.State.DEAD) { active = false; return; }

            // Cập nhật vị trí đích thực tế (nếu target di chuyển lúc đang bay)
            int actualTargetX = 10 + target.combatGridX * 48 + 24; // reference to startX and tileSize hack
            int actualTargetY = 25 + target.combatGridY * 48 + 48; // feet Y center hack
            // Wait, instead of hardcoding, we follow the current frame coords of target!
            // We will just let it head towards dynamic target locations computed here or static.
            
            // Add to trail
            trail.add(new double[]{x, y});
            if (trail.size() > 6) trail.remove(0);

            double dx = tX - x;
            double dy = tY - y;
            double dist = Math.hypot(dx, dy);

            if (dist <= speed) {
                // IMPACT!
                if (target.state != Hero.State.DEAD) {
                    for (int i = 0; i < source.attackHits; i++) {
                        target.takeDamage(damage, source);
                    }
                }
                active = false;
            } else {
                x += (dx / dist) * speed;
                y += (dy / dist) * speed;
            }
        }

        @Override
        public void draw(Graphics2D g2) {
            // Vẽ đường trail nhạt dần
            for (int i = 0; i < trail.size(); i++) {
                double[] p = trail.get(i);
                float ratio = (float) i / trail.size();
                g2.setColor(new Color(100, 200, 255, (int)(150 * ratio)));
                int r = (int)(4 * ratio + 2);
                g2.fillOval((int)p[0]-r, (int)p[1]-r, r*2, r*2);
            }

            // Quả cầu chính rực rỡ
            g2.setColor(new Color(255, 255, 255, 220));
            g2.fillOval((int)x - 8, (int)y - 8, 16, 16);
            g2.setColor(new Color(0, 150, 255, 180));
            java.awt.Stroke oldS = g2.getStroke();
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval((int)x - 8, (int)y - 8, 16, 16);
            g2.setStroke(oldS);
        }
    }
}
