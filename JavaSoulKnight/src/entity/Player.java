package entity;

import core.InputHandler;
import core.Camera;
import manager.EntityManager;
import manager.ElementManager;
import math.Vector2D;
import java.awt.Color;
import java.awt.Graphics2D;

public class Player extends Entity {
    private InputHandler inputH;
    private EntityManager entityManager;
    private Camera camera;
    
    public WeaponManager weaponManager;
    public ElementManager elementManager;
    
    public int maxHp = 100;
    public int hp = maxHp;
    public double damage = 10;
    public double bulletSpeed = 10.0;
    public int gold = 0;
    
    private long lastShootTime = 0;
    private long shootCooldown = 300; // Tăng base cooldown một chút để hợp với nhiều đạn
    
    private double gravity = 0.5;
    
    // Skill: Charge 10
    private int chargeCount = 0;
    private boolean wasMousePressed = false;
    private long lastChargeTick = 0;
    
    // Skill: Invincibility
    public long invincibilityTimer = 0;
    private long lastInvincibilityUsed = 0;

    public Player(double x, double y, InputHandler inputH, EntityManager entityManager, Camera camera) {
        super(x, y);
        this.inputH = inputH;
        this.entityManager = entityManager;
        this.camera = camera;
        this.weaponManager = new WeaponManager(this, entityManager);
        this.elementManager = new ElementManager();
        this.speed = 5.0;
        this.width = 32;
        this.height = 32;
    }

    @Override
    public void update() {
        long currentTime = System.currentTimeMillis();

        // Xử lý Timer
        if (invincibilityTimer > 0) invincibilityTimer -= 16; // Giả sử 60FPS (~16ms/frame)

        // Tự động kích hoạt Bất Tử nếu có thẻ và hết cooldown        // Logic Bất tử
        if (weaponManager.inventory.contains(CardType.INVINCIBLE)) {
            if (currentTime - lastInvincibilityUsed > 20000) {
                invincibilityTimer = 10000; // 10s bất tử
                lastInvincibilityUsed = currentTime;
            }
        }

        // Logic di chuyển
        Vector2D velocity = new Vector2D(0, 0);

        if (inputH.upPressed) velocity.y -= 1;
        if (inputH.downPressed) velocity.y += 1;
        if (inputH.leftPressed) velocity.x -= 1;
        if (inputH.rightPressed) velocity.x += 1;

        if (velocity.mag() > 0) {
            velocity.normalize();
            velocity.mult(speed);
            position.add(velocity);
        }

        if (position.x < width/2) position.x = width/2;
        if (position.y < height/2) position.y = height/2;
        if (position.x > camera.mapWidth - width/2) position.x = camera.mapWidth - width/2;
        if (position.y > camera.mapHeight - height/2) position.y = camera.mapHeight - height/2;

        if (inputH.spacePressed && z == groundZ) {
            zVelocity = 8.0;
        }
        
        z += zVelocity;
        if (z > groundZ) {
            zVelocity -= gravity;
        } else {
            z = groundZ;
            zVelocity = 0;
        }

        // Đổi hiệu ứng đạn
        if (inputH.key1Pressed) elementManager.activeEffectIndex = 0;
        if (inputH.key2Pressed) elementManager.activeEffectIndex = 1;
        if (inputH.key3Pressed) elementManager.activeEffectIndex = 2;

        // Logic bắn súng (Tích đạn - Charge 10)
        boolean hasCharge = weaponManager.inventory.contains(CardType.CHARGE_10);
        
        if (hasCharge) {
            if (inputH.mousePressed) {
                if (currentTime - lastChargeTick >= shootCooldown && chargeCount < 10) {
                    chargeCount++;
                    lastChargeTick = currentTime;
                    // TODO: Gắn âm thanh hoặc hiệu ứng tích đạn ở đây
                }
            } else if (wasMousePressed) {
                // Nhả chuột -> Bắn toàn bộ đạn đã tích
                if (chargeCount > 0) {
                    // Cần chạy vòng lặp bắn (có thể hơi lag nếu bắn một lúc quá nhiều đạn)
                    for (int i = 0; i < chargeCount; i++) {
                        executeShootWithOffset(i * 0.1); // Thêm chút lệch góc để đạn tỏa ra 
                    }
                    chargeCount = 0;
                    lastShootTime = currentTime;
                }
            }
        } else {
            // Logic bắn thường
            if (inputH.mousePressed) {
                if (currentTime - lastShootTime >= shootCooldown) {
                    executeShootWithOffset(0);
                    lastShootTime = currentTime;
                }
            }
        }
        
        wasMousePressed = inputH.mousePressed;
    }

    private void executeShootWithOffset(double angleOffset) {
        double worldMouseX = inputH.mouseX + camera.x;
        double worldMouseY = inputH.mouseY + camera.y;
        Vector2D mousePos = new Vector2D(worldMouseX, worldMouseY);
        Vector2D visualPos = new Vector2D(position.x, position.y - z);
        
        Vector2D dir = Vector2D.sub(mousePos, visualPos);
        
        // Thêm lệch góc
        if (angleOffset != 0) {
            double currentAngle = Math.atan2(dir.y, dir.x);
            currentAngle += angleOffset - 0.5; // Tỏa ra hai bên
            dir.x = Math.cos(currentAngle);
            dir.y = Math.sin(currentAngle);
        }
        
        dir.normalize();
        
        weaponManager.shoot(dir, damage, bulletSpeed);
    }

    public void takeDamage(int dmg) {
        if (invincibilityTimer > 0) return; // Bất tử
        
        this.hp -= dmg;
        if (this.hp <= 0) {
            this.hp = 0;
            this.setDead(true);
        }
    }
    
    public void heal(int amount) {
        this.hp += amount;
        if (this.hp > maxHp) this.hp = maxHp;
    }

    public void addGold(int amount) {
        this.gold += amount;
    }

    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        int drawX = (int)(position.x - camX);
        int drawY = (int)(position.y - camY);
        
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillOval(drawX - width/2, (int)(drawY - groundZ) - height/4, width, height/2);

        int finalDrawY = (int)(drawY - z);
        
        g2.setColor(Color.BLUE);
        if (invincibilityTimer > 0) {
            // Hiệu ứng nhấp nháy khi bất tử
            if ((invincibilityTimer / 100) % 2 == 0) g2.setColor(Color.YELLOW);
        }
        
        g2.fillRect(drawX - width/2, finalDrawY - height/2, width, height);
        g2.setColor(Color.WHITE);
        g2.drawRect(drawX - width/2, finalDrawY - height/2, width, height);

        double worldMouseX = inputH.mouseX + camX;
        double worldMouseY = inputH.mouseY + camY;
        Vector2D mousePos = new Vector2D(worldMouseX, worldMouseY);
        Vector2D visualPos = new Vector2D(position.x, position.y - z);
        Vector2D dir = Vector2D.sub(mousePos, visualPos);
        dir.normalize();
        dir.mult(20);
        
        g2.setColor(Color.GRAY);
        g2.drawLine(drawX, finalDrawY, (int)(drawX + dir.x), (int)(finalDrawY + dir.y));
    }
}
