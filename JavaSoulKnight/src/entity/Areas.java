package entity;

import manager.EntityManager;
import math.Vector2D;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

// ---- EXPLOSION ----
class ExplosionArea extends AreaEffect {
    private double damage;
    private boolean dealtDamage = false;

    public ExplosionArea(double x, double y, double z, int radius, double damage, EntityManager em) {
        super(x, y, z, radius, 300, em); // Tồn tại 0.3s
        this.damage = damage;
    }

    @Override
    protected void applyEffect() {
        if (!dealtDamage) {
            List<Enemy> enemies = entityManager.getEnemiesInRange(position, radius);
            for (Enemy e : enemies) {
                e.takeDamage(damage);
            }
            dealtDamage = true;
        }
    }

    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        g2.setColor(new Color(255, 69, 0, 150)); // Đỏ cam
        g2.fillOval((int)(position.x - camX - radius), (int)(position.y - camY - z - radius), radius*2, radius*2);
    }
}

// ---- POISON ----
class PoisonArea extends AreaEffect {
    private long lastTick = 0;
    public PoisonArea(double x, double y, double z, EntityManager em) {
        super(x, y, z, 50, 5000, em);
    }
    @Override
    protected void applyEffect() {
        long current = System.currentTimeMillis();
        if (current - lastTick > 500) { // Mỗi nửa giây
            for (Enemy e : entityManager.getEnemiesInRange(position, radius)) {
                e.takeDamage(5); // Sát thương độc
            }
            lastTick = current;
        }
    }
    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        g2.setColor(new Color(0, 255, 0, 100));
        g2.fillOval((int)(position.x - camX - radius), (int)(position.y - camY - z - radius), radius*2, radius*2);
    }
}

// ---- BLACKHOLE ----
class BlackholeArea extends AreaEffect {
    public BlackholeArea(double x, double y, double z, EntityManager em) {
        super(x, y, z, 100, 4000, em);
    }
    @Override
    protected void applyEffect() {
        for (Enemy e : entityManager.getEnemiesInRange(position, radius)) {
            Vector2D pull = Vector2D.sub(this.position, e.getPosition());
            if (pull.mag() > 10) {
                pull.normalize();
                pull.mult(2.0); // Lực hút
                e.getPosition().add(pull);
            }
        }
    }
    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillOval((int)(position.x - camX - radius), (int)(position.y - camY - z - radius), radius*2, radius*2);
    }
}

// ---- SHIELD ----
class ShieldArea extends AreaEffect {
    public ShieldArea(double x, double y, double z, EntityManager em) {
        super(x, y, z, 40, 10000, em);
    }
    @Override
    protected void applyEffect() {
        for (Enemy e : entityManager.getEnemiesInRange(position, radius)) {
            Vector2D push = Vector2D.sub(e.getPosition(), this.position);
            if (push.mag() > 0) {
                push.normalize();
                push.mult(5.0); // Lực đẩy ra (cản đường)
                e.getPosition().add(push);
            }
        }
    }
    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        g2.setColor(new Color(0, 191, 255, 150)); // Xanh dương
        g2.drawOval((int)(position.x - camX - radius), (int)(position.y - camY - z - radius), radius*2, radius*2);
    }
}

// ---- SPEED AREA ----
class SpeedArea extends AreaEffect {
    public SpeedArea(double x, double y, double z, EntityManager em) {
        super(x, y, z, 60, 5000, em);
    }
    @Override
    protected void applyEffect() {
        Player p = entityManager.getPlayer();
        if (p != null) {
            Vector2D dist = Vector2D.sub(p.getPosition(), this.position);
            if (dist.mag() <= radius) {
                p.speed = 8.0; // Buff speed
            } else {
                p.speed = 5.0; // Reset
            }
        }
    }
    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        g2.setColor(new Color(255, 215, 0, 100)); // Vàng
        g2.fillOval((int)(position.x - camX - radius), (int)(position.y - camY - z - radius), radius*2, radius*2);
    }
}

// ---- LIFESTEAL AREA ----
class LifestealArea extends AreaEffect {
    public LifestealArea(double x, double y, double z, EntityManager em) {
        super(x, y, z, 80, 5000, em);
    }
    @Override
    protected void applyEffect() {
        for (Enemy e : entityManager.getEnemiesInRange(position, radius)) {
            e.isInLifestealArea = true; // Set flag
        }
    }
    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        g2.setColor(new Color(255, 0, 0, 100)); // Đỏ
        g2.fillOval((int)(position.x - camX - radius), (int)(position.y - camY - z - radius), radius*2, radius*2);
    }
}

// ---- EFFECT AREA (Bự) ----
class EffectArea extends AreaEffect {
    private BulletEffect effect;
    private long lastTick = 0;
    
    public EffectArea(double x, double y, double z, int radius, BulletEffect effect, EntityManager em) {
        super(x, y, z, radius, 5000, em);
        this.effect = effect;
    }
    @Override
    protected void applyEffect() {
        long current = System.currentTimeMillis();
        if (current - lastTick > 500) {
            for (Enemy e : entityManager.getEnemiesInRange(position, radius)) {
                e.takeDamage(10); // Base damage của vùng
                if (effect == BulletEffect.SHOCK) e.freezeTimer = 1000;
                else if (effect == BulletEffect.ACID) e.speed *= 0.8;
            }
            lastTick = current;
        }
    }
    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        g2.setColor(new Color(255, 0, 255, 100)); // Tím
        g2.drawOval((int)(position.x - camX - radius), (int)(position.y - camY - z - radius), radius*2, radius*2);
    }
}

// ---- SMALL EFFECT AREA (Nhỏ, kích 1 lần) ----
class SmallEffectArea extends AreaEffect {
    private BulletEffect effect;
    private boolean activated = false;
    
    public SmallEffectArea(double x, double y, double z, BulletEffect effect, EntityManager em) {
        super(x, y, z, 30, 2000, em); // Nhỏ, tồn tại 2s
        this.effect = effect;
    }
    @Override
    protected void applyEffect() {
        if (!activated) {
            for (Enemy e : entityManager.getEnemiesInRange(position, radius)) {
                e.takeDamage(15);
            }
            activated = true;
        }
    }
    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        g2.setColor(new Color(255, 100, 255, 150));
        g2.fillOval((int)(position.x - camX - radius), (int)(position.y - camY - z - radius), radius*2, radius*2);
    }
}
