package entity;

import math.Vector2D;
import java.awt.Color;
import java.awt.Graphics2D;

public class EnemyBullet extends Entity {
    private Vector2D direction;
    private int damage;
    private long spawnTime;
    private long lifeTime = 3000;

    public EnemyBullet(double x, double y, double z, Vector2D direction, int damage) {
        super(x, y);
        this.z = z;
        this.direction = direction;
        this.damage = damage;
        this.speed = 6.0;
        this.width = 12;
        this.height = 12;
        this.spawnTime = System.currentTimeMillis();
    }

    public int getDamage() { return damage; }

    @Override
    public void update() {
        Vector2D velocity = direction.copy();
        velocity.mult(speed);
        position.add(velocity);

        if (System.currentTimeMillis() - spawnTime > lifeTime) {
            this.setDead(true);
        }
    }

    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        int drawX = (int)(position.x - camX);
        int drawY = (int)(position.y - camY - z);
        
        g2.setColor(Color.MAGENTA);
        g2.fillOval(drawX - width/2, drawY - height/2, width, height);
    }
}
