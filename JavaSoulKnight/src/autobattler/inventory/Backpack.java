package autobattler.inventory;

import autobattler.input.InputHandler;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public class Backpack {
    public int cols, rows;
    public int startX, startY;
    public int tileSize;
    
    public Item[][] grid;
    public List<Item> looseItems = new ArrayList<>();
    public List<Item> gridItems = new ArrayList<>();
    
    public Item draggedItem = null;
    public int dragOffsetX, dragOffsetY;

    public Backpack(int cols, int rows, int startX, int startY, int tileSize) {
        this.cols = cols;
        this.rows = rows;
        this.startX = startX;
        this.startY = startY;
        this.tileSize = tileSize;
        grid = new Item[rows][cols];
    }
    
    public void update(InputHandler input) {
        // Handle dragging
        if (input.mousePressed) {
            if (draggedItem == null) {
                // Try to pick up an item
                // Check loose items first
                for (int i = looseItems.size() - 1; i >= 0; i--) {
                    Item item = looseItems.get(i);
                    if (isPointInItem(input.mouseX, input.mouseY, item, false)) {
                        draggedItem = item;
                        draggedItem.isDragging = true;
                        dragOffsetX = input.mouseX - item.worldX;
                        dragOffsetY = input.mouseY - item.worldY;
                        break;
                    }
                }
                
                // If not found in loose, check grid items
                if (draggedItem == null) {
                    for (int i = gridItems.size() - 1; i >= 0; i--) {
                        Item item = gridItems.get(i);
                        if (isPointInItem(input.mouseX, input.mouseY, item, true)) {
                            draggedItem = item;
                            draggedItem.isDragging = true;
                            draggedItem.isInGrid = false;
                            
                            // Remove from grid
                            removeFromGrid(item);
                            gridItems.remove(item);
                            looseItems.add(item); // Temporary put it in loose
                            
                            // Set world pos for drawing during drag
                            draggedItem.worldX = startX + item.gridX * tileSize;
                            draggedItem.worldY = startY + item.gridY * tileSize;
                            
                            dragOffsetX = input.mouseX - draggedItem.worldX;
                            dragOffsetY = input.mouseY - draggedItem.worldY;
                            break;
                        }
                    }
                }
            } else {
                // Dragging
                draggedItem.worldX = input.mouseX - dragOffsetX;
                draggedItem.worldY = input.mouseY - dragOffsetY;
            }
        } else {
            // Mouse released
            if (draggedItem != null) {
                draggedItem.isDragging = false;
                
                // Check if dropped inside grid bounds
                // Snap to closest grid cell
                int snapCol = (draggedItem.worldX + tileSize/2 - startX) / tileSize;
                int snapRow = (draggedItem.worldY + tileSize/2 - startY) / tileSize;
                
                if (canPlaceItem(draggedItem, snapCol, snapRow)) {
                    placeItem(draggedItem, snapCol, snapRow);
                    looseItems.remove(draggedItem);
                    gridItems.add(draggedItem);
                }
                // If cannot place, it stays in looseItems at worldX, worldY
                
                draggedItem = null;
            }
        }
    }
    
    private boolean isPointInItem(int px, int py, Item item, boolean inGridMode) {
        int ix = inGridMode ? startX + item.gridX * tileSize : item.worldX;
        int iy = inGridMode ? startY + item.gridY * tileSize : item.worldY;
        
        for(int r = 0; r < item.height; r++) {
            for(int c = 0; c < item.width; c++) {
                if(item.shape[r][c] == 1) {
                    int cellX = ix + c * tileSize;
                    int cellY = iy + r * tileSize;
                    if (px >= cellX && px < cellX + tileSize && py >= cellY && py < cellY + tileSize) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean canPlaceItem(Item item, int col, int row) {
        if (col < 0 || row < 0 || col + item.width > cols || row + item.height > rows) return false;
        
        for(int r = 0; r < item.height; r++) {
            for(int c = 0; c < item.width; c++) {
                if(item.shape[r][c] == 1) {
                    if (grid[row + r][col + c] != null) {
                        return false; // Collision
                    }
                }
            }
        }
        return true;
    }
    
    private void placeItem(Item item, int col, int row) {
        item.gridX = col;
        item.gridY = row;
        item.isInGrid = true;
        
        for(int r = 0; r < item.height; r++) {
            for(int c = 0; c < item.width; c++) {
                if(item.shape[r][c] == 1) {
                    grid[row + r][col + c] = item;
                }
            }
        }
    }
    
    private void removeFromGrid(Item item) {
        for(int r = 0; r < item.height; r++) {
            for(int c = 0; c < item.width; c++) {
                if(item.shape[r][c] == 1) {
                    grid[item.gridY + r][item.gridX + c] = null;
                }
            }
        }
    }

    public void draw(Graphics2D g2) {
        // Draw Grid
        g2.setColor(new Color(50, 50, 50));
        g2.fillRect(startX, startY, cols * tileSize, rows * tileSize);
        
        g2.setColor(Color.DARK_GRAY);
        for(int r = 0; r <= rows; r++) {
            g2.drawLine(startX, startY + r * tileSize, startX + cols * tileSize, startY + r * tileSize);
        }
        for(int c = 0; c <= cols; c++) {
            g2.drawLine(startX + c * tileSize, startY, startX + c * tileSize, startY + rows * tileSize);
        }
        
        // Draw Items in Grid
        for(Item item : gridItems) {
            item.draw(g2, tileSize, startX, startY);
        }
        
        // Draw Loose Items
        for(Item item : looseItems) {
            if (item != draggedItem) {
                item.draw(g2, tileSize, startX, startY);
            }
        }
        
        // Draw Dragged Item last so it's on top
        if (draggedItem != null) {
            draggedItem.draw(g2, tileSize, startX, startY);
            
            // Draw a ghost snap projection if close to grid
            int snapCol = (draggedItem.worldX + tileSize/2 - startX) / tileSize;
            int snapRow = (draggedItem.worldY + tileSize/2 - startY) / tileSize;
            if (canPlaceItem(draggedItem, snapCol, snapRow)) {
                g2.setColor(new Color(0, 255, 0, 100));
                for(int r = 0; r < draggedItem.height; r++) {
                    for(int c = 0; c < draggedItem.width; c++) {
                        if(draggedItem.shape[r][c] == 1) {
                            g2.fillRect(startX + (snapCol + c) * tileSize, startY + (snapRow + r) * tileSize, tileSize, tileSize);
                        }
                    }
                }
            } else if (snapCol >= -draggedItem.width && snapRow >= -draggedItem.height && snapCol < cols && snapRow < rows) {
                g2.setColor(new Color(255, 0, 0, 100));
                for(int r = 0; r < draggedItem.height; r++) {
                    for(int c = 0; c < draggedItem.width; c++) {
                        if(draggedItem.shape[r][c] == 1) {
                            // Only draw red if it's within screen roughly
                            g2.fillRect(startX + (snapCol + c) * tileSize, startY + (snapRow + r) * tileSize, tileSize, tileSize);
                        }
                    }
                }
            }
        }
    }
}
