package entity;

import math.Vector2D;
import java.awt.Color;
import java.awt.Graphics2D;

public abstract class Enemy extends Entity {
    protected Player player;
    public double maxHp;
    public double hp;
    public int goldReward;

    // Status Effects
    public long freezeTimer = 0;
    public long bleedTimer = 0;
    private long lastBleedTick = 0;
    public boolean isInLifestealArea = false;
    
    // Dot Effect (Từ thẻ số 14)
    public BulletEffect currentDotEffect = BulletEffect.NONE;
    public long dotTimer = 0;
    private long lastDotTick = 0;
    
    // Knockback
    private Vector2D knockbackVel = new Vector2D(0, 0);

    public Enemy(double x, double y, Player player, double maxHp, int goldReward) {
        super(x, y);
        this.player = player;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.goldReward = goldReward;
    }

    public void takeDamage(double damage) {
        this.hp -= damage;
        if (this.hp <= 0) {
            die();
        }
    }
    
    private void die() {
        if (this.isDead) return;
        this.setDead(true);
        player.addGold(goldReward);
        if (isInLifestealArea) {
            player.heal(5); // Hồi máu nếu chết trong vùng
        }
    }

    public void applyKnockback(Vector2D kb) {
        this.knockbackVel = kb;
    }

    public void applyDotEffect(BulletEffect effect, long duration) {
        this.currentDotEffect = effect;
        this.dotTimer = duration;
    }

    protected void updateStatusEffects() {
        long current = System.currentTimeMillis();
        
        // Bleed
        if (bleedTimer > 0) {
            bleedTimer -= 16;
            if (current - lastBleedTick > 500) {
                takeDamage(2);
                lastBleedTick = current;
            }
        }
        
        // Freeze
        if (freezeTimer > 0) {
            freezeTimer -= 16;
        }
        
        // Dot Effect (Thẻ số 14)
        if (dotTimer > 0) {
            dotTimer -= 16;
            if (current - lastDotTick > 1000) { // Mỗi giây 1 lần
                // Gọi hàm apply elemental hit giả lập từ Bullet
                // Đơn giản hóa: Sát thương nguyên tố DoT
                takeDamage(10);
                lastDotTick = current;
            }
        }

        // Apply Knockback (giảm dần)
        if (knockbackVel.mag() > 0.5) {
            position.add(knockbackVel);
            knockbackVel.mult(0.8); // Ma sát
        } else {
            knockbackVel = new Vector2D(0, 0);
        }
        
        // Reset flag
        isInLifestealArea = false; 
    }

    protected void drawShadowAndHealth(Graphics2D g2, double camX, double camY, int drawX, int finalDrawY) {
        int groundDrawY = (int)(position.y - camY - groundZ);
        // Bóng
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillOval(drawX - width/2, groundDrawY - height/4, width, height/2);
        
        // Thanh máu
        int barWidth = width;
        int barHeight = 4;
        g2.setColor(Color.RED);
        g2.fillRect(drawX - width/2, finalDrawY - height/2 - 10, barWidth, barHeight);
        
        g2.setColor(Color.GREEN);
        int hpWidth = (int)((hp / maxHp) * barWidth);
        if (hpWidth < 0) hpWidth = 0;
        g2.fillRect(drawX - width/2, finalDrawY - height/2 - 10, hpWidth, barHeight);
        
        // Hiệu ứng
        if (freezeTimer > 0) {
            g2.setColor(new Color(0, 255, 255, 100));
            g2.fillRect(drawX - width/2, finalDrawY - height/2, width, height);
        }
    }
}
