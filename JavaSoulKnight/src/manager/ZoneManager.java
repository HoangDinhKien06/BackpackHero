package manager;

import core.GamePanel;
import entity.Enemy;
import entity.SmallEnemy;
import entity.BigEnemy;
import entity.BossEnemy;
import world.Portal;
import world.Terrain;
import world.Zone;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ZoneManager {
    private GamePanel gamePanel;
    private EntityManager entityManager;
    private Map<Integer, Zone> zones;
    public Zone currentZone;
    
    private long lastSpawnTime = 0;
    private Random random = new Random();

    public ZoneManager(GamePanel gamePanel, EntityManager entityManager) {
        this.gamePanel = gamePanel;
        this.entityManager = entityManager;
        this.zones = new HashMap<>();
        
        initZones();
        loadZone(0, 400, 300); // Khởi đầu ở Làng
    }

    private void initZones() {
        // ZONE 0: LÀNG (Safe Zone)
        Zone safeZone = new Zone(0, "Làng Khởi Điểm", 800, 600, new Color(34, 139, 34)); // Xanh lá
        safeZone.allowedEnemyTypes = new int[]{}; // Không sinh quái
        safeZone.portals.add(new Portal(700, 300, 1, 100, 1000)); // Cổng sang Rừng
        zones.put(0, safeZone);

        // ZONE 1: RỪNG RẬM
        Zone forest = new Zone(1, "Rừng Rậm", 2000, 2000, new Color(0, 100, 0)); // Xanh đậm
        forest.allowedEnemyTypes = new int[]{1}; // Quái nhỏ
        forest.spawnInterval = 1000;
        // Bục nhảy (Đá trong rừng)
        forest.terrains.add(new Terrain(500, 500, 200, 200, 40));
        forest.terrains.add(new Terrain(1200, 800, 300, 150, 60));
        forest.portals.add(new Portal(50, 1000, 0, 600, 300)); // Về Làng
        forest.portals.add(new Portal(1900, 1000, 2, 100, 500)); // Sang Sa Mạc
        zones.put(1, forest);

        // ZONE 2: SA MẠC CHẾT
        Zone desert = new Zone(2, "Sa Mạc Chết", 3000, 1000, new Color(210, 180, 140)); // Cát
        desert.allowedEnemyTypes = new int[]{2}; // Quái to lướt
        desert.spawnInterval = 2000;
        desert.terrains.add(new Terrain(1000, 300, 500, 200, 20)); // Cồn cát thấp
        desert.portals.add(new Portal(50, 500, 1, 1800, 1000)); // Về Rừng
        desert.portals.add(new Portal(2900, 500, 3, 100, 300)); // Vào Lâu Đài
        zones.put(2, desert);

        // ZONE 3: LÂU ĐÀI BOSS
        Zone castle = new Zone(3, "Lâu Đài Trùm", 1000, 1000, new Color(50, 0, 50)); // Tím đen
        castle.allowedEnemyTypes = new int[]{3}; // Boss
        castle.spawnInterval = 9999999; // Chỉ sinh 1 lần (sẽ xử lý cứng)
        castle.portals.add(new Portal(50, 300, 2, 2800, 500)); // Về Sa Mạc
        zones.put(3, castle);
    }

    public void loadZone(int zoneId, double startX, double startY) {
        this.currentZone = zones.get(zoneId);
        
        // Reset toàn bộ quái và đạn hiện tại
        entityManager.clearEnemiesAndBullets();
        
        // Cập nhật Terrain và Portal cho GamePanel/EntityManager vẽ
        gamePanel.terrains = currentZone.terrains;
        
        // Dịch chuyển Player
        if (gamePanel.player != null) {
            gamePanel.player.getPosition().x = startX;
            gamePanel.player.getPosition().y = startY;
            gamePanel.player.setGroundZ(0);
        }
        
        System.out.println("Đã tới: " + currentZone.name);

        // Sinh Boss nếu là map Lâu Đài
        if (zoneId == 3) {
            entityManager.addEntity(new BossEnemy(500, 500, gamePanel.player, entityManager));
        }
    }

    public void update() {
        // Check Portal collision cho mọi Zone
        for (Portal p : currentZone.portals) {
            if (p.getBounds().intersects(gamePanel.player.getBounds())) {
                loadZone(p.targetZoneId, p.targetX, p.targetY);
                return; // Tránh lỗi Concurrent khi list bị clear
            }
        }

        if (currentZone.allowedEnemyTypes.length == 0) return; // Map an toàn, không sinh quái

        // Sinh quái
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpawnTime > currentZone.spawnInterval) {
            spawnEnemy();
            lastSpawnTime = currentTime;
        }
    }

    private void spawnEnemy() {
        // Chỉ sinh quái nếu map 1 hoặc 2
        if (currentZone.id != 1 && currentZone.id != 2) return;
        
        // Tránh sinh quá nhiều quái làm lag (giới hạn 30 con)
        if (entityManager.getEnemiesCount() > 30) return;

        double camX = gamePanel.camera.x;
        double camY = gamePanel.camera.y;
        
        // Sinh ngẫu nhiên ngoài rìa màn hình
        double x = camX + random.nextInt(gamePanel.screenWidth + 400) - 200;
        double y = camY + random.nextInt(gamePanel.screenHeight + 400) - 200;
        
        // Không cho rớt khỏi viền map
        if (x < 0) x = 50; if (x > currentZone.width) x = currentZone.width - 50;
        if (y < 0) y = 50; if (y > currentZone.height) y = currentZone.height - 50;

        Enemy enemy = null;
        if (currentZone.id == 1) {
            enemy = new SmallEnemy(x, y, gamePanel.player);
        } else if (currentZone.id == 2) {
            enemy = new BigEnemy(x, y, gamePanel.player);
        }
        
        if (enemy != null) {
            entityManager.addEntity(enemy);
        }
    }
}
