package entity;

import manager.EntityManager;
import math.Vector2D;
import world.Terrain;
import java.awt.Color;
import java.awt.Graphics2D;

public class PlayerBullet extends Entity {
    public Vector2D direction;
    public double damage;
    private long spawnTime;
    private long lifeTime = 2000;
    private EntityManager entityManager;
    private Player player;
    
    public BulletEffect activeEffect = BulletEffect.NONE;

    // Card Flags
    public boolean isCannon = false;
    public boolean spawnEffectArea = false;
    public boolean applyHitBlackhole = false;
    public boolean applyKnockback = false;
    public boolean applyMultiShot = false;
    public int bounceCount = 0;
    public boolean isHoming5s = false;
    public int pierceCount = 0;
    public boolean isShortFast = false;
    public boolean spawnBlackhole = false;
    public boolean spawnShield5s = false;
    public boolean applyDotEffect = false;
    public boolean spawnSpeedArea = false;
    public boolean spawnLifestealArea = false;
    public boolean applySmallEffectOnHit = false;
    public boolean isBoomerang = false;

    private boolean boomerangReturning = false;

    public PlayerBullet(double x, double y, double z, Vector2D direction, double damage, double speed, EntityManager em, Player player) {
        super(x, y);
        this.z = z;
        this.direction = direction;
        this.damage = damage;
        this.speed = speed;
        this.entityManager = em;
        this.player = player;
        this.width = 10;
        this.height = 10;
        this.spawnTime = System.currentTimeMillis();
    }

    public void setupModifiers() {
        if (isCannon) {
            this.width = 25;
            this.height = 25;
            this.damage *= 2.5;
            this.speed *= 0.6;
        }
        if (isShortFast) {
            this.speed *= 2.0;
            this.lifeTime = 600; // Sống 0.6s
        }
    }

    public double getDamage() { return damage; }

    @Override
    public void update() {
        long currentTime = System.currentTimeMillis();
        long age = currentTime - spawnTime;

        // Logic Boomerang
        if (isBoomerang && age > lifeTime / 2 && !boomerangReturning) {
            boomerangReturning = true;
        }
        if (boomerangReturning) {
            if (player != null && !player.isDead()) {
                direction = Vector2D.sub(player.getPosition(), this.position);
                direction.normalize();
                if (this.collidesWith(player)) {
                    this.setDead(true);
                    return;
                }
            }
        }

        // Logic Homing 5s
        if (isHoming5s && age < 5000 && !boomerangReturning) {
            Enemy target = entityManager.getNearestEnemy(this.position);
            if (target != null) {
                Vector2D toTarget = Vector2D.sub(target.getPosition(), this.position);
                toTarget.normalize();
                direction.x = direction.x * 0.9 + toTarget.x * 0.1;
                direction.y = direction.y * 0.9 + toTarget.y * 0.1;
                direction.normalize();
            }
        }

        // Cập nhật vị trí
        Vector2D velocity = direction.copy();
        velocity.mult(speed);
        position.add(velocity);
        
        if (age > lifeTime && !isBoomerang) {
            die();
        }
    }
    
    public void bounce(Terrain t) {
        if (bounceCount > 0) {
            bounceCount--;
            direction.x *= -1;
            direction.y *= -1;
        } else {
            die();
        }
    }

    public void onHitEnemy(Enemy e) {
        // Áp dụng sát thương/hiệu ứng nguyên tố TỨC THÌ
        applyElementalHit(e);

        if (applyKnockback) {
            Vector2D kb = direction.copy();
            kb.mult(15);
            e.applyKnockback(kb);
        }
        if (applyHitBlackhole && player.weaponManager.tryTriggerBlackhole()) {
            entityManager.addEntity(new BlackholeArea(e.getPosition().x, e.getPosition().y, z, entityManager));
        }
        if (applyMultiShot && player.weaponManager.tryTriggerMultiShot()) {
            // Đẻ 3 viên đạn dí theo nó
            for(int i=-1; i<=1; i++) {
                Vector2D nd = direction.copy();
                double angle = Math.atan2(nd.y, nd.x) + i * 0.5;
                nd.x = Math.cos(angle); nd.y = Math.sin(angle);
                PlayerBullet b = new PlayerBullet(position.x, position.y, z, nd, damage*0.5, speed, entityManager, player);
                b.isHoming5s = true; // Dí
                entityManager.addEntity(b);
            }
        }
        if (applyDotEffect) {
            e.applyDotEffect(activeEffect, 5000); // Kéo dài 5s
        }
        if (applySmallEffectOnHit) {
            entityManager.addEntity(new SmallEffectArea(position.x, position.y, z, activeEffect, entityManager));
        }

        if (pierceCount > 0) {
            pierceCount--;
        } else {
            die();
        }
    }

    private void applyElementalHit(Enemy e) {
        // Xử lý 21 hiệu ứng khi trúng đạn
        switch(activeEffect) {
            case PLASMA:
                e.takeDamage(damage * 1.5);
                this.pierceCount += 1;
                break;
            case ACID:
                e.takeDamage(damage); // Sát thương thêm
                e.speed *= 0.9; // Giảm tốc
                break;
            case BURN:
                e.bleedTimer = 3000;
                break;
            case MUD:
                e.freezeTimer = 2000; // Tạm dùng freeze cho trói chân
                break;
            case MOSS:
                e.takeDamage(damage);
                player.heal(2);
                break;
            case SHOCK:
                e.freezeTimer = 1000;
                break;
            case REBIRTH:
                e.goldReward *= 2; // Rớt nhiều vàng/máu hơn
                break;
            case METAL:
                e.takeDamage(damage * 2.0);
                break;
            case SOUND:
                double dist = Vector2D.sub(player.getPosition(), this.position).mag();
                e.takeDamage(damage * (1 + dist/500.0)); // Xa càng đau
                this.pierceCount += 10;
                break;
            default:
                break;
        }
    }

    public void die() {
        if (this.isDead) return;
        this.setDead(true);
        
        if (spawnEffectArea) {
            entityManager.addEntity(new EffectArea(position.x, position.y, z, 80, activeEffect, entityManager));
        }
        if (spawnBlackhole) {
            entityManager.addEntity(new BlackholeArea(position.x, position.y, z, entityManager));
        }
        if (spawnShield5s) {
            entityManager.addEntity(new ShieldArea(position.x, position.y, z, entityManager));
        }
        if (spawnSpeedArea) {
            entityManager.addEntity(new SpeedArea(position.x, position.y, z, entityManager));
        }
        if (spawnLifestealArea) {
            entityManager.addEntity(new LifestealArea(position.x, position.y, z, entityManager));
        }

        // Spawn AoE Elements
        if (activeEffect == BulletEffect.FIRE_AREA) {
            entityManager.addEntity(new PoisonArea(position.x, position.y, z, entityManager)); // Dùng tạm PoisonArea làm lửa
        }
        if (activeEffect == BulletEffect.MAGMA) {
            entityManager.addEntity(new PoisonArea(position.x, position.y, z, entityManager));
        }
        if (activeEffect == BulletEffect.WATER_AREA) {
            entityManager.addEntity(new PoisonArea(position.x, position.y, z, entityManager));
        }
        if (activeEffect == BulletEffect.TORNADO) {
            entityManager.addEntity(new BlackholeArea(position.x, position.y, z, entityManager));
        }
        if (activeEffect == BulletEffect.SEED) {
            entityManager.addEntity(new ShieldArea(position.x, position.y, z, entityManager));
        }
        if (activeEffect == BulletEffect.POISON_GAS) {
            entityManager.addEntity(new PoisonArea(position.x, position.y, z, entityManager));
        }
    }

    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        int drawX = (int)(position.x - camX);
        int drawY = (int)(position.y - camY - z);
        
        // Màu đạn dựa trên Effect
        if (activeEffect.name().contains("FIRE") || activeEffect == BulletEffect.MAGMA || activeEffect == BulletEffect.PLASMA) g2.setColor(Color.RED);
        else if (activeEffect.name().contains("WATER") || activeEffect == BulletEffect.STEAM) g2.setColor(Color.BLUE);
        else if (activeEffect.name().contains("ELECTRIC") || activeEffect == BulletEffect.SHOCK) g2.setColor(Color.YELLOW);
        else if (activeEffect.name().contains("PLANT") || activeEffect == BulletEffect.ACID) g2.setColor(Color.GREEN);
        else if (activeEffect.name().contains("EARTH") || activeEffect == BulletEffect.METAL) g2.setColor(new Color(139, 69, 19));
        else if (activeEffect.name().contains("WIND") || activeEffect == BulletEffect.SOUND) g2.setColor(Color.WHITE);
        else g2.setColor(Color.ORANGE);

        if (isCannon) {
            g2.fillOval(drawX - width/2, drawY - height/2, width, height);
            g2.setColor(Color.WHITE);
            g2.drawOval(drawX - width/2, drawY - height/2, width, height);
        } else {
            g2.fillOval(drawX - width/2, drawY - height/2, width, height);
        }
    }
}
