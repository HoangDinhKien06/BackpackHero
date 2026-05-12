package entity;

import math.Vector2D;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public abstract class Entity implements Comparable<Entity> {
    protected Vector2D position;
    
    // 2.5D Z-Axis
    protected double z = 0; // Độ cao hiện tại
    protected double zVelocity = 0; // Vận tốc nhảy
    protected double groundZ = 0; // Độ cao của mặt đất bên dưới chân

    protected double speed;
    protected int width, height;
    protected boolean isDead = false;

    public Entity(double x, double y) {
        this.position = new Vector2D(x, y);
    }

    public abstract void update();
    
    // Hàm vẽ nhận thêm Camera X và Y để vẽ cho đúng
    public abstract void draw(Graphics2D g2, double camX, double camY);

    public Vector2D getPosition() { return position; }
    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) { this.isDead = dead; }
    
    public double getZ() { return z; }
    public void setGroundZ(double groundZ) { this.groundZ = groundZ; }
    public double getGroundZ() { return groundZ; }

    // Tính toán va chạm 2D trên mặt đất
    public Rectangle getBounds() {
        return new Rectangle((int)position.x - width/2, (int)position.y - height/2, width, height);
    }

    public boolean collidesWith(Entity other) {
        // Chỉ va chạm nếu ở cùng độ cao tương đối (chênh lệch Z không quá lớn)
        if (Math.abs(this.z - other.z) > 20) return false;
        return this.getBounds().intersects(other.getBounds());
    }

    // Y-Sorting: Entity nào có Y lớn hơn sẽ vẽ sau (đè lên trên)
    @Override
    public int compareTo(Entity other) {
        return Double.compare(this.position.y, other.position.y);
    }
}
