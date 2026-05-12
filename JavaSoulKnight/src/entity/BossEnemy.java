package entity;

import manager.EntityManager;
import math.Vector2D;
import java.awt.Color;
import java.awt.Graphics2D;

public class BossEnemy extends Enemy {
    
    private EntityManager entityManager;
    private long lastShootTime = 0;
    private long shootCooldown = 1500;

    public BossEnemy(double x, double y, Player player, EntityManager entityManager) {
        super(x, y, player, 500, 100); // 500 HP, rớt 100 Gold
        this.entityManager = entityManager;
        this.speed = 1.0;
        this.width = 80;
        this.height = 80;
    }

    @Override
    public void update() {
        updateStatusEffects();
        if (player == null || player.isDead() || freezeTimer > 0) return;

        // Di chuyển tà tà về phía người chơi
        Vector2D dir = Vector2D.sub(player.getPosition(), this.position);
        if (dir.mag() > 0) {
            dir.normalize();
            
            // Bắn đạn
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastShootTime > shootCooldown) {
                shoot(dir);
                lastShootTime = currentTime;
            }
            
            // Giữ khoảng cách, không lại quá gần
            if (dir.mag() > 150) {
                dir.mult(speed);
                position.add(dir);
            }
        }
    }

    private void shoot(Vector2D dir) {
        // Bắn 3 viên tỏa ra
        double angle = Math.atan2(dir.y, dir.x);
        for(int i = -1; i <= 1; i++) {
            double newAngle = angle + (i * Math.PI / 8);
            Vector2D newDir = new Vector2D(Math.cos(newAngle), Math.sin(newAngle));
            EnemyBullet bullet = new EnemyBullet(position.x, position.y, z, newDir, 15);
            entityManager.addEntity(bullet);
        }
    }

    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        int drawX = (int)(position.x - camX);
        int finalDrawY = (int)(position.y - camY - z);

        drawShadowAndHealth(g2, camX, camY, drawX, finalDrawY);

        g2.setColor(new Color(75, 0, 130)); // Indigo
        g2.fillRect(drawX - width/2, finalDrawY - height/2, width, height);
        g2.setColor(Color.YELLOW);
        g2.drawRect(drawX - width/2, finalDrawY - height/2, width, height);
        
        // Vẽ thêm mắt cho ngầu
        g2.setColor(Color.RED);
        g2.fillRect(drawX - 20, finalDrawY - 10, 10, 10);
        g2.fillRect(drawX + 10, finalDrawY - 10, 10, 10);
    }
}
