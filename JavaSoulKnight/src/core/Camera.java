package core;

import entity.Player;

public class Camera {
    public double x, y;
    private int screenWidth, screenHeight;
    public int mapWidth = 2000;
    public int mapHeight = 2000;

    public Camera(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void update(Player player) {
        // Camera luôn cố gắng giữ người chơi ở giữa màn hình
        if (player != null && !player.isDead()) {
            this.x = player.getPosition().x - (screenWidth / 2.0);
            this.y = player.getPosition().y - (screenHeight / 2.0);

            // Giới hạn camera không vượt quá biên map
            if (this.x < 0) this.x = 0;
            if (this.y < 0) this.y = 0;
            if (this.x > mapWidth - screenWidth) this.x = mapWidth - screenWidth;
            if (this.y > mapHeight - screenHeight) this.y = mapHeight - screenHeight;
        }
    }
}
