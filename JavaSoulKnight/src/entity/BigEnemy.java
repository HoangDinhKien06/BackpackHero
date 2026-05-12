package entity;

import math.Vector2D;
import java.awt.Color;
import java.awt.Graphics2D;

public class BigEnemy extends Enemy {
    
    private long lastDashTime = 0;
    private long dashCooldown = 3000;
    private boolean isDashing = false;
    private long dashStartTime = 0;
    private long dashDuration = 300; // Lướt trong 0.3s
    private Vector2D dashDir;

    public BigEnemy(double x, double y, Player player) {
        super(x, y, player, 60, 15); // 60 HP, rớt 15 Gold
        this.speed = 1.5;
        this.width = 40;
        this.height = 40;
    }

    @Override
    public void update() {
        updateStatusEffects();
        if (player == null || player.isDead() || freezeTimer > 0) return;

        long currentTime = System.currentTimeMillis();

        if (isDashing) {
            if (currentTime - dashStartTime > dashDuration) {
                isDashing = false;
            } else {
                // Đang lướt
                Vector2D vel = dashDir.copy();
                vel.mult(speed * 4); // Tốc độ lướt x4
                position.add(vel);
                return;
            }
        }

        Vector2D dir = Vector2D.sub(player.getPosition(), this.position);
        double dist = dir.mag();

        // Kích hoạt lướt nếu ở gần
        if (dist > 0 && dist < 200 && currentTime - lastDashTime > dashCooldown) {
            isDashing = true;
            dashStartTime = currentTime;
            lastDashTime = currentTime;
            dir.normalize();
            dashDir = dir;
        } else if (dist > 0) {
            // Đi bộ bình thường
            dir.normalize();
            dir.mult(speed);
            position.add(dir);
        }
    }

    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        int drawX = (int)(position.x - camX);
        int finalDrawY = (int)(position.y - camY - z);

        drawShadowAndHealth(g2, camX, camY, drawX, finalDrawY);

        g2.setColor(new Color(139, 0, 0)); // Đỏ sậm
        g2.fillRect(drawX - width/2, finalDrawY - height/2, width, height);
        g2.setColor(Color.WHITE);
        g2.drawRect(drawX - width/2, finalDrawY - height/2, width, height);
    }
}
