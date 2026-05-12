package manager;

import core.GamePanel;
import core.GameState;
import core.InputHandler;
import entity.BulletEffect;
import entity.Element;
import entity.Player;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class CraftingManager {
    private GamePanel gamePanel;
    private InputHandler inputH;
    
    // UI Rectangles
    private Rectangle btnClose = new Rectangle(600, 500, 150, 40);
    private Rectangle btnFuse = new Rectangle(350, 250, 100, 40);
    
    // Ghép đồ
    private Element slot1 = null;
    private Element slot2 = null;
    
    // Trang bị
    private BulletEffect selectedEffectToEquip = null;

    public CraftingManager(GamePanel gamePanel, InputHandler inputH) {
        this.gamePanel = gamePanel;
        this.inputH = inputH;
    }

    public void update() {
        if (gamePanel.gameState != GameState.CRAFTING) return;
        
        Player p = gamePanel.player;

        if (inputH.mouseClickedUI) {
            int mx = inputH.mouseX;
            int my = inputH.mouseY;

            // Đóng
            if (btnClose.contains(mx, my)) {
                slot1 = null; slot2 = null; selectedEffectToEquip = null;
                gamePanel.gameState = GameState.PLAYING;
            }

            // Click túi Nguyên tố (để cho vào slot ghép)
            Element[] elements = Element.values();
            for (int i = 0; i < elements.length; i++) {
                Rectangle rect = new Rectangle(50 + i * 80, 100, 70, 70);
                if (rect.contains(mx, my) && p.elementManager.elementInventory.get(elements[i]) > 0) {
                    if (slot1 == null) slot1 = elements[i];
                    else if (slot2 == null) slot2 = elements[i];
                }
            }
            
            // Click slot ghép để bỏ ra
            if (new Rectangle(300, 150, 60, 60).contains(mx, my)) slot1 = null;
            if (new Rectangle(440, 150, 60, 60).contains(mx, my)) slot2 = null;

            // Bấm Nút Ghép
            if (btnFuse.contains(mx, my) && slot1 != null && slot2 != null) {
                boolean success = p.elementManager.fuse(slot1, slot2);
                if (success) {
                    slot1 = null;
                    slot2 = null;
                }
            }

            // Click túi Hiệu ứng (để chọn)
            for (int i = 0; i < p.elementManager.effectInventory.size(); i++) {
                int rx = 50 + (i % 5) * 100;
                int ry = 350 + (i / 5) * 60;
                Rectangle rect = new Rectangle(rx, ry, 90, 50);
                if (rect.contains(mx, my)) {
                    selectedEffectToEquip = p.elementManager.effectInventory.get(i);
                }
            }

            // Click 3 Ô trang bị (để gắn hiệu ứng đã chọn hoặc gỡ ra)
            for (int i = 0; i < 3; i++) {
                Rectangle rect = new Rectangle(500, 350 + i * 60, 200, 50);
                if (rect.contains(mx, my)) {
                    if (selectedEffectToEquip != null) {
                        p.elementManager.equipEffect(i, selectedEffectToEquip);
                        selectedEffectToEquip = null;
                    } else {
                        p.elementManager.unequipEffect(i);
                    }
                }
            }

            inputH.mouseClickedUI = false;
        }
    }

    public void draw(Graphics2D g2) {
        Player p = gamePanel.player;

        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(0, 0, gamePanel.screenWidth, gamePanel.screenHeight);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.drawString("BÀN DUNG HỢP NGUYÊN TỐ", 250, 40);

        // 1. Kho Nguyên Tố
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.drawString("Kho Nguyên Tố:", 50, 80);
        Element[] elements = Element.values();
        for (int i = 0; i < elements.length; i++) {
            Rectangle rect = new Rectangle(50 + i * 80, 100, 70, 70);
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2.setColor(Color.WHITE);
            g2.drawRect(rect.x, rect.y, rect.width, rect.height);
            
            int count = p.elementManager.elementInventory.get(elements[i]);
            g2.drawString(elements[i].name, rect.x + 5, rect.y + 30);
            g2.setColor(Color.YELLOW);
            g2.drawString("x" + count, rect.x + 20, rect.y + 50);
        }

        // 2. Khu vực ghép
        g2.setColor(Color.WHITE);
        g2.drawString("Dung hợp:", 300, 120);
        
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(300, 150, 60, 60); // Slot 1
        g2.fillRect(440, 150, 60, 60); // Slot 2
        g2.setColor(Color.WHITE);
        g2.drawRect(300, 150, 60, 60);
        g2.drawRect(440, 150, 60, 60);
        
        g2.drawString("+", 395, 185);
        
        if (slot1 != null) g2.drawString(slot1.name, 310, 185);
        if (slot2 != null) g2.drawString(slot2.name, 450, 185);

        // Nút Fuse
        if (slot1 != null && slot2 != null) g2.setColor(new Color(0, 200, 0));
        else g2.setColor(Color.GRAY);
        g2.fillRect(btnFuse.x, btnFuse.y, btnFuse.width, btnFuse.height);
        g2.setColor(Color.WHITE);
        g2.drawString("GHÉP", btnFuse.x + 25, btnFuse.y + 25);
        
        // Preview kết quả
        if (slot1 != null && slot2 != null) {
            BulletEffect result = FusionTable.fuse(slot1, slot2);
            g2.setColor(Color.CYAN);
            g2.drawString("-> " + result.name, 460, 275);
        }

        // 3. Kho Hiệu ứng Đạn
        g2.setColor(Color.WHITE);
        g2.drawString("Túi Hiệu Ứng (Đã ghép):", 50, 330);
        for (int i = 0; i < p.elementManager.effectInventory.size(); i++) {
            BulletEffect ef = p.elementManager.effectInventory.get(i);
            int rx = 50 + (i % 5) * 100;
            int ry = 350 + (i / 5) * 60;
            Rectangle rect = new Rectangle(rx, ry, 90, 50);
            
            if (selectedEffectToEquip == ef) g2.setColor(Color.ORANGE);
            else g2.setColor(Color.BLUE);
            
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2.setColor(Color.WHITE);
            g2.drawRect(rect.x, rect.y, rect.width, rect.height);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.drawString(ef.name, rect.x + 5, rect.y + 25);
        }

        // 4. Các ô Trang bị
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("TRANG BỊ (Nhấn phím 1, 2, 3 để đổi):", 500, 330);
        for (int i = 0; i < 3; i++) {
            BulletEffect eq = p.elementManager.equippedEffects[i];
            Rectangle rect = new Rectangle(500, 350 + i * 60, 200, 50);
            
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2.setColor(Color.WHITE);
            g2.drawRect(rect.x, rect.y, rect.width, rect.height);
            
            g2.drawString("Ô " + (i+1) + ": " + eq.name, rect.x + 10, rect.y + 30);
        }

        // Nút Đóng
        g2.setColor(Color.RED);
        g2.fillRect(btnClose.x, btnClose.y, btnClose.width, btnClose.height);
        g2.setColor(Color.WHITE);
        g2.drawString("ĐÓNG (THOÁT)", btnClose.x + 10, btnClose.y + 25);
    }
}
