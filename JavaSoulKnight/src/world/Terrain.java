package world;

import entity.Entity;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Terrain {
    public int x, y, width, height;
    public double zLevel; // Độ cao của bục

    public Terrain(int x, int y, int width, int height, double zLevel) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.zLevel = zLevel;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void checkCollision(Entity e) {
        Rectangle eBounds = e.getBounds();
        
        // Nếu entity đang nằm trong khu vực của bục (nhìn từ trên xuống)
        if (this.getBounds().intersects(eBounds)) {
            // Nếu entity đang ở cao hơn bục thì cập nhật groundZ cho nó đứng lên bục
            if (e.getZ() >= this.zLevel) {
                e.setGroundZ(this.zLevel);
            } else {
                // Đập mặt vào tường (chưa nhảy đủ cao), đẩy ra
                // (Logic đẩy ra đơn giản: nếu đi vào từ trái/phải/trên/dưới)
                // Ở đây làm đơn giản: đẩy ngược lại vị trí cũ (cần lưu vị trí cũ trong entity nếu làm chuẩn)
                // Tạm thời chỉ cản không cho đi qua (set tốc độ = 0 hoặc đẩy ra)
                
                // Giải pháp đơn giản: Không cho set groundZ, rơi xuống mặt đất. 
                // Cần xử lý đẩy ra ngoài bục để tránh xuyên tường.
                
                Rectangle intersection = this.getBounds().intersection(eBounds);
                if (intersection.width < intersection.height) {
                    if (e.getPosition().x < this.x + this.width/2.0) {
                        e.getPosition().x -= intersection.width;
                    } else {
                        e.getPosition().x += intersection.width;
                    }
                } else {
                    if (e.getPosition().y < this.y + this.height/2.0) {
                        e.getPosition().y -= intersection.height;
                    } else {
                        e.getPosition().y += intersection.height;
                    }
                }
            }
        }
    }

    public void draw(Graphics2D g2, double camX, double camY) {
        int drawX = (int)(x - camX);
        int drawY = (int)(y - camY);
        
        // Vẽ mặt trên của bục (nâng lên một đoạn zLevel)
        g2.setColor(new Color(100, 100, 100)); // Xám
        g2.fillRect(drawX, (int)(drawY - zLevel), width, height);
        g2.setColor(Color.WHITE);
        g2.drawRect(drawX, (int)(drawY - zLevel), width, height);
        
        // Vẽ mặt trước (để tạo cảm giác chiều sâu 3D)
        g2.setColor(new Color(60, 60, 60)); // Xám đậm
        g2.fillRect(drawX, (int)(drawY + height - zLevel), width, (int)zLevel);
    }
}
