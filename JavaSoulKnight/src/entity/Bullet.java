package entity;

import java.awt.Graphics2D;

// Lớp này đã không còn được sử dụng ở bản V2 (thay bằng PlayerBullet và EnemyBullet).
// Cập nhật lại cấu trúc để sửa lỗi biên dịch.
public class Bullet extends Entity {
    public Bullet() {
        super(0, 0);
    }

    @Override
    public void update() {}

    @Override
    public void draw(Graphics2D g2, double camX, double camY) {}
}
