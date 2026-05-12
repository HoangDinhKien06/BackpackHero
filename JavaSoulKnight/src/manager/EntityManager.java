package manager;

import entity.Entity;
import entity.Player;
import entity.PlayerBullet;
import entity.EnemyBullet;
import entity.Enemy;
import core.Camera;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class EntityManager {
    private List<Entity> entities;
    private Player player;

    public EntityManager() {
        entities = new ArrayList<>();
    }

    public Player getPlayer() {
        return player;
    }

    public int getEnemiesCount() {
        int count = 0;
        for (Entity e : entities) {
            if (e instanceof Enemy && !e.isDead()) count++;
        }
        return count;
    }

    public void addEntity(Entity e) {
        entities.add(e);
    }

    public void setPlayer(Player player) {
        this.player = player;
        addEntity(player);
    }
    
    public Enemy getNearestEnemy(math.Vector2D pos) {
        Enemy nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Entity e : entities) {
            if (e instanceof Enemy && !e.isDead()) {
                double dist = math.Vector2D.sub(pos, e.getPosition()).mag();
                if (dist < minDist) {
                    minDist = dist;
                    nearest = (Enemy) e;
                }
            }
        }
        return nearest;
    }

    public List<Enemy> getEnemiesInRange(math.Vector2D pos, double radius) {
        List<Enemy> result = new ArrayList<>();
        for (Entity e : entities) {
            if (e instanceof Enemy && !e.isDead()) {
                double dist = math.Vector2D.sub(pos, e.getPosition()).mag();
                if (dist <= radius) {
                    result.add((Enemy) e);
                }
            }
        }
        return result;
    }

    public void clearEnemiesAndBullets() {
        Iterator<Entity> it = entities.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            if (e instanceof Enemy || e instanceof PlayerBullet || e instanceof EnemyBullet) {
                it.remove();
            }
        }
    }

    public void update() {
        for (int i = 0; i < entities.size(); i++) {
            entities.get(i).update();
        }

        checkCollisions();

        Iterator<Entity> it = entities.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            if (e.isDead()) {
                it.remove();
            }
        }
    }

    private void checkCollisions() {
        List<PlayerBullet> pBullets = new ArrayList<>();
        List<EnemyBullet> eBullets = new ArrayList<>();
        List<Enemy> enemies = new ArrayList<>();

        for (Entity e : entities) {
            if (e instanceof PlayerBullet) pBullets.add((PlayerBullet) e);
            else if (e instanceof EnemyBullet) eBullets.add((EnemyBullet) e);
            else if (e instanceof Enemy) enemies.add((Enemy) e);
        }

        // Đạn người chơi trúng quái
        for (PlayerBullet b : pBullets) {
            for (Enemy e : enemies) {
                if (!b.isDead() && !e.isDead() && b.collidesWith(e)) {
                    b.onHitEnemy(e);
                    e.takeDamage(b.getDamage());
                }
            }
            
            // Xử lý nảy tường (Nếu ra khỏi map)
            // Đơn giản hóa: Map giả định 2000x2000
            if (!b.isDead() && b.bounceCount > 0) {
                if (b.getPosition().x < 0 || b.getPosition().x > 2000 || 
                    b.getPosition().y < 0 || b.getPosition().y > 2000) {
                    b.bounce(null);
                    // Giữ đạn trong map
                    if (b.getPosition().x < 0) b.getPosition().x = 1;
                    if (b.getPosition().x > 2000) b.getPosition().x = 1999;
                    if (b.getPosition().y < 0) b.getPosition().y = 1;
                    if (b.getPosition().y > 2000) b.getPosition().y = 1999;
                }
            }
        }
        
        // Đạn quái trúng người chơi
        if (player != null && !player.isDead()) {
            for (EnemyBullet b : eBullets) {
                if (!b.isDead() && b.collidesWith(player)) {
                    player.takeDamage(b.getDamage());
                    b.setDead(true);
                }
            }
        }

        // Quái chạm người chơi (Chỉ chạm khi ở cùng độ cao)
        if (player != null && !player.isDead()) {
            for (Enemy e : enemies) {
                if (!e.isDead() && e.collidesWith(player)) {
                    player.takeDamage(1); 
                }
            }
        }
    }

    public void draw(Graphics2D g2, Camera camera) {
        // Y-Sorting: Sắp xếp danh sách trước khi vẽ để tạo chiều sâu
        // Đối tượng có Y nhỏ hơn (ở xa hơn về phía trên) sẽ được vẽ trước
        Collections.sort(entities);

        for (Entity e : entities) {
            e.draw(g2, camera.x, camera.y);
        }
    }
}
