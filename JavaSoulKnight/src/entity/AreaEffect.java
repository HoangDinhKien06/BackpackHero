package entity;

import manager.EntityManager;
import java.awt.Graphics2D;

public abstract class AreaEffect extends Entity {
    protected EntityManager entityManager;
    protected long spawnTime;
    protected long lifeTime;
    protected int radius;

    public AreaEffect(double x, double y, double z, int radius, long lifeTime, EntityManager em) {
        super(x, y);
        this.z = z;
        this.radius = radius;
        this.width = radius * 2;
        this.height = radius * 2;
        this.lifeTime = lifeTime;
        this.entityManager = em;
        this.spawnTime = System.currentTimeMillis();
    }

    @Override
    public void update() {
        if (System.currentTimeMillis() - spawnTime > lifeTime) {
            this.setDead(true);
        } else {
            applyEffect();
        }
    }

    protected abstract void applyEffect();
    
    // Y-Sorting cho AreaEffect thường vẽ dưới đất (trừ khi nổ)
    @Override
    public int compareTo(Entity other) {
        if (other instanceof AreaEffect) return 0;
        return -1; // Vẽ trước (chìm dưới) các Entity khác
    }
}
