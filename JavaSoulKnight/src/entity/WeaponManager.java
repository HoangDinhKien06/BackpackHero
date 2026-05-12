package entity;

import manager.EntityManager;
import math.Vector2D;
import java.util.ArrayList;
import java.util.List;

public class WeaponManager {
    public List<CardType> inventory;
    private Player player;
    private EntityManager entityManager;
    
    // Cooldown trackers
    private long lastBlackholeHitTime = 0;
    private long lastMultiShotHitTime = 0;
    
    public WeaponManager(Player player, EntityManager em) {
        this.player = player;
        this.entityManager = em;
        this.inventory = new ArrayList<>();
    }
    
    public boolean addCard(CardType card) {
        if (inventory.size() < 6) {
            inventory.add(card);
            return true;
        }
        return false;
    }
    
    public void removeCard(int index) {
        if (index >= 0 && index < inventory.size()) {
            inventory.remove(index);
        }
    }

    public boolean tryTriggerBlackhole() {
        long current = System.currentTimeMillis();
        if (current - lastBlackholeHitTime >= 10000) { // 10s CD
            lastBlackholeHitTime = current;
            return true;
        }
        return false;
    }

    public boolean tryTriggerMultiShot() {
        long current = System.currentTimeMillis();
        if (current - lastMultiShotHitTime >= 7000) { // 7s CD
            lastMultiShotHitTime = current;
            return true;
        }
        return false;
    }

    public void shoot(Vector2D dir, double baseDamage, double baseSpeed) {
        int bulletCount = 1;
        double spreadAngle = 0;
        
        // Base stats
        double dmg = baseDamage;
        double speed = baseSpeed;

        for (CardType card : inventory) {
            if (card == CardType.SPREAD_3) {
                bulletCount += 2;
                spreadAngle += Math.PI / 8;
            }
            if (card == CardType.DOUBLE_FAST) {
                bulletCount += 1;
                speed *= 1.5;
                spreadAngle += Math.PI / 12;
            }
            if (card == CardType.SHORT_FAST) {
                speed *= 2.0;
                // Sẽ xử lý thời gian sống ngắn trong PlayerBullet
            }
        }
        
        double startAngle = Math.atan2(dir.y, dir.x);
        double angleStep = (bulletCount > 1) ? (spreadAngle / (bulletCount - 1)) : 0;
        double currentAngle = startAngle - (spreadAngle / 2.0);

        for (int i = 0; i < bulletCount; i++) {
            Vector2D newDir = new Vector2D(Math.cos(currentAngle), Math.sin(currentAngle));
            
            PlayerBullet b = new PlayerBullet(player.getPosition().x, player.getPosition().y, player.getZ(), newDir, dmg, speed, entityManager, player);
            
            // Pass the active effect
            b.activeEffect = player.elementManager.getActiveEffect();

            // Apply modifiers
            for (CardType card : inventory) {
                if (card == CardType.BIG_CANNON) b.isCannon = true;
                if (card == CardType.EFFECT_AREA) b.spawnEffectArea = true;
                if (card == CardType.HIT_BLACKHOLE) b.applyHitBlackhole = true;
                if (card == CardType.KNOCKBACK) b.applyKnockback = true;
                if (card == CardType.MULTI_SHOT_ON_HIT) b.applyMultiShot = true;
                if (card == CardType.BOUNCE_2) b.bounceCount += 2;
                if (card == CardType.HOMING_5S) b.isHoming5s = true;
                if (card == CardType.PIERCE_2) b.pierceCount += 2;
                if (card == CardType.SHORT_FAST) b.isShortFast = true;
                if (card == CardType.SPAWN_BLACKHOLE) b.spawnBlackhole = true;
                if (card == CardType.SHIELD_5S) b.spawnShield5s = true;
                if (card == CardType.DOT_EFFECT) b.applyDotEffect = true;
                if (card == CardType.SPEED_AREA) b.spawnSpeedArea = true;
                if (card == CardType.LIFESTEAL_AREA) b.spawnLifestealArea = true;
                if (card == CardType.SMALL_EFFECT_ON_HIT) b.applySmallEffectOnHit = true;
                if (card == CardType.BOOMERANG) b.isBoomerang = true;
            }
            
            b.setupModifiers();
            entityManager.addEntity(b);
            
            currentAngle += angleStep;
        }
    }
}
