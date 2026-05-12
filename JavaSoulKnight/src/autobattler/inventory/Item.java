package autobattler.inventory;

import java.awt.Color;
import java.awt.Graphics2D;

public class Item {
    public String name;
    public int[][] shape; // 1 represents a block, 0 is empty
    public Color color;
    public int width, height;
    
    // Position on screen when not in grid (loose)
    public int worldX, worldY;
    public boolean isDragging = false;
    
    // Position inside the grid
    public int gridX = -1;
    public int gridY = -1;
    public boolean isInGrid = false;
    
    public Item(String name, int[][] shape, Color color) {
        this.name = name;
        this.shape = shape;
        this.color = color;
        this.height = shape.length;
        this.width = shape[0].length;
    }
    
    public void draw(Graphics2D g2, int tileSize, int gridStartX, int gridStartY) {
        int drawX = isDragging || !isInGrid ? worldX : gridStartX + gridX * tileSize;
        int drawY = isDragging || !isInGrid ? worldY : gridStartY + gridY * tileSize;
        
        for(int r = 0; r < height; r++) {
            for(int c = 0; c < width; c++) {
                if(shape[r][c] == 1) {
                    g2.setColor(color);
                    g2.fillRect(drawX + c * tileSize, drawY + r * tileSize, tileSize, tileSize);
                    g2.setColor(Color.WHITE);
                    g2.drawRect(drawX + c * tileSize, drawY + r * tileSize, tileSize, tileSize);
                }
            }
        }
    }
}
