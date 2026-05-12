package core;

import entity.Player;
import manager.EntityManager;
import manager.ZoneManager;
import manager.ShopManager;
import manager.CraftingManager;
import world.Portal;
import world.Terrain;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel implements Runnable {

    public final int screenWidth = 800;
    public final int screenHeight = 600;
    int FPS = 60;

    Thread gameThread;
    public InputHandler inputH = new InputHandler();
    public GameState gameState = GameState.PLAYING;
    
    public Camera camera;
    public EntityManager entityManager;
    public ZoneManager zoneManager;
    public ShopManager shopManager;
    public CraftingManager craftingManager;
    public Player player;
    
    public List<Terrain> terrains = new ArrayList<>();
    
    // NPC Shop (Chỉ xuất hiện ở Làng)
    public Rectangle shopNPC = new Rectangle(300, 200, 60, 60);
    // Bàn Dung Hợp (Cũng ở Làng)
    public Rectangle craftNPC = new Rectangle(400, 200, 60, 60);

    // Nút Restart
    private Rectangle btnRestart = new Rectangle(screenWidth/2 - 100, screenHeight/2 + 50, 200, 50);

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.DARK_GRAY);
        this.setDoubleBuffered(true);
        this.addKeyListener(inputH);
        this.addMouseListener(inputH);
        this.addMouseMotionListener(inputH);
        this.setFocusable(true);

        initSystem();
    }

    public void initSystem() {
        camera = new Camera(screenWidth, screenHeight);
        entityManager = new EntityManager();
        
        player = new Player(400, 300, inputH, entityManager, camera);
        entityManager.setPlayer(player);
        
        zoneManager = new ZoneManager(this, entityManager);
        shopManager = new ShopManager(this, inputH);
        craftingManager = new CraftingManager(this, inputH);
        
        gameState = GameState.PLAYING;
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint(); 
                delta--;
            }
        }
    }

    public void update() {
        if (gameState == GameState.PLAYING) {
            // Cập nhật Camera giới hạn theo Map
            camera.mapWidth = zoneManager.currentZone.width;
            camera.mapHeight = zoneManager.currentZone.height;
            
            player.update();
            camera.update(player);
            
            player.setGroundZ(0); 
            for(Terrain t : terrains) {
                t.checkCollision(player);
            }
            
            entityManager.update();
            zoneManager.update();
            
            // Xử lý click vào NPC Shop và Bàn Dung Hợp
            if (zoneManager.currentZone.id == 0 && inputH.mouseClickedUI) {
                // Tọa độ thế giới của chuột
                double worldX = inputH.mouseX + camera.x;
                double worldY = inputH.mouseY + camera.y;
                double dist = math.Vector2D.sub(new math.Vector2D(worldX, worldY), player.getPosition()).mag();
                
                if (dist < 150) {
                    if (shopNPC.contains(worldX, worldY)) {
                        shopManager.rerollCards();
                        gameState = GameState.SHOP;
                    } else if (craftNPC.contains(worldX, worldY)) {
                        gameState = GameState.CRAFTING;
                    }
                }
                inputH.mouseClickedUI = false;
            }

            if (player.isDead()) {
                gameState = GameState.GAME_OVER;
            }
        } 
        else if (gameState == GameState.SHOP) {
            shopManager.update();
        }
        else if (gameState == GameState.CRAFTING) {
            craftingManager.update();
        }
        else if (gameState == GameState.GAME_OVER || gameState == GameState.VICTORY) {
            if (inputH.mouseClickedUI) {
                if (btnRestart.contains(inputH.mouseX, inputH.mouseY)) {
                    initSystem(); 
                }
                inputH.mouseClickedUI = false;
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (gameState == GameState.PLAYING || gameState == GameState.SHOP || gameState == GameState.GAME_OVER) {
            
            // Vẽ nền theo Zone
            g2.setColor(zoneManager.currentZone.bgColor);
            g2.fillRect(0, 0, screenWidth, screenHeight);
            drawGrid(g2);
            
            // Vẽ Portal
            for(Portal p : zoneManager.currentZone.portals) {
                p.draw(g2, camera.x, camera.y);
            }

            // Vẽ địa hình
            for(Terrain t : terrains) {
                t.draw(g2, camera.x, camera.y);
            }
            
            // Vẽ NPC Shop ở Làng
            if (zoneManager.currentZone.id == 0) {
                int drawX = (int)(shopNPC.x - camera.x);
                int drawY = (int)(shopNPC.y - camera.y);
                
                g2.setColor(Color.ORANGE);
                g2.fillRect(drawX, drawY, shopNPC.width, shopNPC.height);
                g2.setColor(Color.WHITE);
                g2.drawRect(drawX, drawY, shopNPC.width, shopNPC.height);
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                g2.drawString("SHOP", drawX + 10, drawY + 35);

                // Vẽ Bàn Dung Hợp
                int cx = (int)(craftNPC.x - camera.x);
                int cy = (int)(craftNPC.y - camera.y);
                g2.setColor(Color.MAGENTA);
                g2.fillRect(cx, cy, craftNPC.width, craftNPC.height);
                g2.setColor(Color.WHITE);
                g2.drawRect(cx, cy, craftNPC.width, craftNPC.height);
                g2.setColor(Color.WHITE);
                g2.drawString("CRAFT", cx + 5, cy + 35);
            }

            // Vẽ Entity
            entityManager.draw(g2, camera);

            // Vẽ UI
            drawUI(g2);
        }

        if (gameState == GameState.SHOP) {
            shopManager.draw(g2);
        }

        if (gameState == GameState.CRAFTING) {
            craftingManager.draw(g2);
        }

        if (gameState == GameState.GAME_OVER) {
            drawGameOverScreen(g2, "GAME OVER");
        }
        
        if (gameState == GameState.VICTORY) {
            drawGameOverScreen(g2, "VICTORY! BẠN ĐÃ PHÁ ĐẢO!");
        }

        g2.dispose();
    }
    
    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 30)); // Vạch mờ
        int gridSize = 100;
        int startX = (int)(-(camera.x % gridSize));
        int startY = (int)(-(camera.y % gridSize));
        
        for (int i = startX; i < screenWidth; i += gridSize) {
            g2.drawLine(i, 0, i, screenHeight);
        }
        for (int j = startY; j < screenHeight; j += gridSize) {
            g2.drawLine(0, j, screenWidth, j);
        }
    }

    private void drawUI(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("Map: " + zoneManager.currentZone.name, 20, 30);
        g2.setColor(Color.YELLOW);
        g2.drawString("Vàng: " + player.gold, 20, 60);
        
        // Túi đồ hiện tại
        g2.setColor(Color.WHITE);
        g2.drawString("Cards:", 200, 30);
        for(int i = 0; i < player.weaponManager.inventory.size(); i++) {
            g2.setColor(Color.CYAN);
            g2.fillRect(280 + i * 40, 10, 35, 30);
        }
        
        // Thanh máu người chơi
        g2.setColor(Color.RED);
        g2.fillRect(20, 80, 200, 20);
        g2.setColor(Color.GREEN);
        int hpWidth = (int)((double)player.hp / player.maxHp * 200);
        if(hpWidth < 0) hpWidth = 0;
        g2.fillRect(20, 80, hpWidth, 20);
        g2.setColor(Color.WHITE);
        g2.drawRect(20, 80, 200, 20);
        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        g2.drawString(player.hp + "/" + player.maxHp, 25, 95);

        // Hiệu ứng Đạn Đang Dùng
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("Hiệu ứng Đạn (Phím " + (player.elementManager.activeEffectIndex + 1) + "): " + player.elementManager.getActiveEffect().name, 20, 130);
    }
    
    private void drawGameOverScreen(Graphics2D g2, String title) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        g2.setColor(Color.RED);
        if (title.contains("VICTORY")) g2.setColor(Color.GREEN);
        
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.drawString(title, screenWidth/2 - 150, screenHeight/2 - 20);
        
        if (btnRestart.contains(inputH.mouseX, inputH.mouseY)) {
            g2.setColor(Color.LIGHT_GRAY);
        } else {
            g2.setColor(Color.GRAY);
        }
        g2.fillRect(btnRestart.x, btnRestart.y, btnRestart.width, btnRestart.height);
        
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("CHƠI LẠI", btnRestart.x + 50, btnRestart.y + 32);
    }
}
