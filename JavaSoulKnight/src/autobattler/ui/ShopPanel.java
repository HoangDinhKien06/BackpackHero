package autobattler.ui;

import autobattler.core.Economy;
import autobattler.core.Hero;
import autobattler.core.Shop;
import autobattler.input.InputHandler;
import autobattler.main.GamePanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.function.Consumer;

public class ShopPanel {
    GamePanel gp;
    Shop shop;
    Economy economy;
    Consumer<Hero> onBuyHero; // callback to add hero to bench
    
    // Panel layout constants (in logical 1200x900 coords)
    public static final int PANEL_X = 695;
    public static final int PANEL_Y = 0;
    public static final int PANEL_W = 505;
    public static final int PANEL_H = 900;
    
    // Slot layout
    private static final int SLOT_W = 72;
    private static final int SLOT_H = 90;
    private static final int SLOT_SPACING = 8;
    
    // Button layout
    private static final int BTN_W = 160;
    private static final int BTN_H = 45;
    
    // Hover state
    private int[] slotHovered = new int[5];
    private boolean rollHovered, lvlHovered;
    
    public ShopPanel(GamePanel gp, Shop shop, Economy economy, Consumer<Hero> onBuyHero) {
        this.gp = gp;
        this.shop = shop;
        this.economy = economy;
        this.onBuyHero = onBuyHero;
    }
    
    public void update(InputHandler input) {
        int mx = input.mouseX;
        int my = input.mouseY;
        boolean clicked = input.mouseClicked;
        
        // Update slot hovers
        int startX = PANEL_X + (PANEL_W - (5 * SLOT_W + 4 * SLOT_SPACING)) / 2;
        int startY = 40;
        
        for (int i = 0; i < 5; i++) {
            int sx = startX + i * (SLOT_W + SLOT_SPACING);
            int sy = startY;
            slotHovered[i] = new Rectangle(sx, sy, SLOT_W, SLOT_H).contains(mx, my) ? 1 : 0;
            
            if (slotHovered[i] == 1 && clicked && shop.offers[i] != null) {
                Hero bought = shop.buy(i, economy);
                if (bought != null && onBuyHero != null) {
                    onBuyHero.accept(bought);
                }
            }
        }
        
        // Roll button
        int rollX = PANEL_X + 20;
        int rollY = 180;
        rollHovered = new Rectangle(rollX, rollY, BTN_W, BTN_H).contains(mx, my);
        if (rollHovered && clicked && economy.canAffordRoll()) {
            economy.roll();
            shop.reroll();
        }
        
        // Level Up button
        int lvlX = PANEL_X + PANEL_W / 2 + 5;
        int lvlY = 180;
        lvlHovered = new Rectangle(lvlX, lvlY, BTN_W, BTN_H).contains(mx, my);
        if (lvlHovered && clicked) {
            economy.buyExp();
        }
    }
    
    public void draw(Graphics2D g2) {
        // Background
        g2.setColor(new Color(20, 20, 30, 230));
        g2.fillRect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        g2.setColor(new Color(80, 80, 120));
        g2.drawLine(PANEL_X, PANEL_Y, PANEL_X, PANEL_Y + PANEL_H);
        
        // Title
        g2.setColor(new Color(255, 215, 0));
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        drawCentered(g2, "⚔ CỬA HÀNG ⚔", PANEL_X, 15, PANEL_W);
        
        // Shop Slots
        int startX = PANEL_X + (PANEL_W - (5 * SLOT_W + 4 * SLOT_SPACING)) / 2;
        int startY = 40;
        
        for (int i = 0; i < 5; i++) {
            int sx = startX + i * (SLOT_W + SLOT_SPACING);
            int sy = startY;
            drawShopSlot(g2, sx, sy, i);
        }
        
        // Roll Button
        int rollX = PANEL_X + 20;
        int rollY = 180;
        boolean canRoll = economy.canAffordRoll();
        g2.setColor(rollHovered && canRoll ? new Color(80, 200, 80) : canRoll ? new Color(50, 150, 50) : new Color(60, 60, 60));
        g2.fillRoundRect(rollX, rollY, BTN_W, BTN_H, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(rollX, rollY, BTN_W, BTN_H, 10, 10);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        drawCentered(g2, "🔄 ROLL (" + Economy.ROLL_COST + "g)", rollX, rollY + 15, BTN_W);
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        drawCentered(g2, "Xáo tướng mới", rollX, rollY + 32, BTN_W);
        
        // Level Up Button
        int lvlX = PANEL_X + PANEL_W / 2 + 5;
        int lvlY = 180;
        boolean canLvl = economy.canAffordLevelUp();
        g2.setColor(lvlHovered && canLvl ? new Color(80, 100, 220) : canLvl ? new Color(50, 70, 180) : new Color(60, 60, 60));
        g2.fillRoundRect(lvlX, lvlY, BTN_W, BTN_H, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(lvlX, lvlY, BTN_W, BTN_H, 10, 10);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        drawCentered(g2, "⬆ LÊN CẤP (" + Economy.EXP_COST + "g)", lvlX, lvlY + 15, BTN_W);
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        drawCentered(g2, "+4 EXP", lvlX, lvlY + 32, BTN_W);
        
        // Divider
        g2.setColor(new Color(80, 80, 120));
        g2.drawLine(PANEL_X + 15, 240, PANEL_X + PANEL_W - 15, 240);
        
        // Gold display
        g2.setColor(new Color(255, 215, 0));
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("💰 " + economy.gold + " Vàng", PANEL_X + 20, 270);
        
        // Level + EXP display
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        int maxUnits = economy.getMaxFieldUnits();
        g2.drawString("Cấp: " + economy.level + "  |  Max sân: " + maxUnits + " tướng", PANEL_X + 20, 300);
        
        // EXP bar
        int barX = PANEL_X + 20;
        int barY = 310;
        int barW = PANEL_W - 40;
        int barH = 18;
        int expNeeded = economy.getExpToNextLevel();
        g2.setColor(new Color(40, 40, 60));
        g2.fillRoundRect(barX, barY, barW, barH, 8, 8);
        if (expNeeded != Integer.MAX_VALUE && expNeeded > 0) {
            int filled = (int)((double) economy.exp / expNeeded * barW);
            g2.setColor(new Color(80, 130, 255));
            g2.fillRoundRect(barX, barY, filled, barH, 8, 8);
        }
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(barX, barY, barW, barH, 8, 8);
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        String expText = economy.level >= Economy.MAX_LEVEL ? "MAX" : economy.exp + " / " + expNeeded + " EXP";
        drawCentered(g2, expText, barX, barY + 13, barW);
        
        // Divider
        g2.setColor(new Color(80, 80, 120));
        g2.drawLine(PANEL_X + 15, 345, PANEL_X + PANEL_W - 15, 345);
        
        // Instruction
        g2.setColor(new Color(150, 150, 200));
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.drawString("Click ô tướng để mua (3g)", PANEL_X + 15, 365);
        g2.drawString("Kéo 2 tướng cùng loại + sao → Merge", PANEL_X + 15, 382);
        g2.drawString("[SPACE] Bắt đầu / Dừng chiến đấu", PANEL_X + 15, 399);
    }
    
    private void drawShopSlot(Graphics2D g2, int x, int y, int index) {
        Hero hero = shop.offers[index];
        boolean hovered = slotHovered[index] == 1;
        
        // Slot background
        if (hero != null) {
            g2.setColor(hovered ? new Color(60, 70, 90) : new Color(35, 40, 55));
        } else {
            g2.setColor(new Color(25, 25, 35));
        }
        g2.fillRoundRect(x, y, SLOT_W, SLOT_H, 8, 8);
        g2.setColor(hovered ? new Color(120, 140, 200) : new Color(70, 80, 110));
        g2.drawRoundRect(x, y, SLOT_W, SLOT_H, 8, 8);
        
        if (hero != null) {
            // Draw hero
            hero.draw(g2, x + 4, y + 4, SLOT_W - 8);
            
            // Cost badge
            int cost = shop.getCost(index);
            g2.setColor(new Color(255, 215, 0));
            g2.fillRoundRect(x + 2, y + SLOT_H - 18, SLOT_W - 4, 16, 5, 5);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            drawCentered(g2, cost + "g", x + 2, y + SLOT_H - 6, SLOT_W - 4);
        } else {
            g2.setColor(new Color(60, 60, 80));
            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            drawCentered(g2, "—", x, y + SLOT_H / 2 + 8, SLOT_W);
        }
    }
    
    private void drawCentered(Graphics2D g2, String text, int x, int y, int width) {
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (width - fm.stringWidth(text)) / 2;
        g2.drawString(text, tx, y);
    }
}
