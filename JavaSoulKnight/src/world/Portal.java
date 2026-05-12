package world;

import entity.Entity;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Portal {
    public double x, y;
    public int width = 60, height = 80;
    public int targetZoneId;
    public double targetX, targetY; // Vị trí player sẽ xuất hiện ở map mới

    public Portal(double x, double y, int targetZoneId, double targetX, double targetY) {
        this.x = x;
        this.y = y;
        this.targetZoneId = targetZoneId;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x - width/2, (int)y - height/2, width, height);
    }

    public void draw(Graphics2D g2, double camX, double camY) {
        int drawX = (int)(x - camX);
        int drawY = (int)(y - camY);

        // Hiệu ứng cổng dịch chuyển
        g2.setColor(new Color(138, 43, 226, 150)); // Tím trong suốt
        g2.fillOval(drawX - width/2, drawY - height/2, width, height);
        
        g2.setColor(Color.WHITE);
        g2.drawOval(drawX - width/2, drawY - height/2, width, height);
        
        // Vòng xoáy bên trong
        g2.setColor(new Color(255, 0, 255, 100));
        long time = System.currentTimeMillis() / 10;
        int innerW = (int)(width/2 + Math.sin(time * 0.05) * 10);
        int innerH = (int)(height/2 + Math.cos(time * 0.05) * 10);
        g2.fillOval(drawX - innerW/2, drawY - innerH/2, innerW, innerH);
    }
}
