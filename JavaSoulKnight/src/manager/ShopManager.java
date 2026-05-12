package manager;

import core.GamePanel;
import core.GameState;
import core.InputHandler;
import entity.CardType;
import entity.Player;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShopManager {
    private GamePanel gamePanel;
    private InputHandler inputH;
    
    // Thẻ được roll ra trong shop (3 thẻ)
    private List<CardType> shopCards = new ArrayList<>();
    private Rectangle[] shopCardRects = new Rectangle[3];
    // Giá cơ bản
    private final int BASE_PRICE = 50;
    
    // Nút Đóng Shop
    private Rectangle btnCloseShop = new Rectangle(300, 500, 200, 50);
    
    // Gói Nguyên Tố
    private Rectangle btnBuyElementPack = new Rectangle(550, 420, 200, 50);
    
    // Quản lý trạng thái "Vứt thẻ"
    private boolean isDiscarding = false;
    private CardType pendingCardToBuy = null;
    
    private Random random = new Random();

    public ShopManager(GamePanel gamePanel, InputHandler inputH) {
        this.gamePanel = gamePanel;
        this.inputH = inputH;
        
        shopCardRects[0] = new Rectangle(80, 200, 200, 250);
        shopCardRects[1] = new Rectangle(300, 200, 200, 250);
        shopCardRects[2] = new Rectangle(520, 200, 200, 250);
    }
    
    public void rerollCards() {
        shopCards.clear();
        CardType[] allCards = CardType.values();
        for (int i = 0; i < 3; i++) {
            shopCards.add(allCards[random.nextInt(allCards.length)]);
        }
        isDiscarding = false;
        pendingCardToBuy = null;
    }

    public void update() {
        if (gamePanel.gameState != GameState.SHOP) return;

        // Tự động roll thẻ nếu shop trống (lúc vừa vào)
        if (shopCards.isEmpty()) {
            rerollCards();
        }

        if (inputH.mouseClickedUI) {
            Player p = gamePanel.player;
            int mx = inputH.mouseX;
            int my = inputH.mouseY;

            if (isDiscarding) {
                // Click vào túi đồ để vứt thẻ
                for (int i = 0; i < p.weaponManager.inventory.size(); i++) {
                    Rectangle invRect = new Rectangle(100 + i * 100, 50, 90, 40);
                    if (invRect.contains(mx, my)) {
                        int currentPrice = BASE_PRICE * pendingCardToBuy.priceMultiplier;
                        p.weaponManager.removeCard(i);
                        p.weaponManager.addCard(pendingCardToBuy);
                        p.gold -= currentPrice;
                        shopCards.remove(pendingCardToBuy);
                        
                        isDiscarding = false;
                        pendingCardToBuy = null;
                        break;
                    }
                }
                
                // Click ra ngoài để hủy
                if (!new Rectangle(100, 50, 600, 40).contains(mx, my)) {
                    isDiscarding = false;
                    pendingCardToBuy = null;
                }
            } else {
                // Mua thẻ
                for (int i = 0; i < shopCards.size(); i++) {
                    CardType card = shopCards.get(i);
                    int currentPrice = BASE_PRICE * card.priceMultiplier;
                    
                    if (shopCardRects[i].contains(mx, my) && p.gold >= currentPrice) {
                        if (p.weaponManager.inventory.size() < 6) {
                            p.weaponManager.addCard(card);
                            p.gold -= currentPrice;
                            shopCards.remove(i);
                        } else {
                            // Bật mode vứt thẻ
                            isDiscarding = true;
                            pendingCardToBuy = card;
                        }
                        break;
                    }
                }

                // Mua Gói Nguyên Tố
                if (btnBuyElementPack.contains(mx, my)) {
                    int price = p.elementManager.elementPackPrice;
                    if (p.gold >= price) {
                        p.gold -= price;
                        p.elementManager.elementPackPrice *= 2; // Gấp đôi
                        // Random 3 nguyên tố
                        entity.Element[] allElements = entity.Element.values();
                        for (int k = 0; k < 3; k++) {
                            entity.Element randElem = allElements[random.nextInt(allElements.length)];
                            p.elementManager.addElement(randElem, 1);
                        }
                    }
                }

                // Đóng shop
                if (btnCloseShop.contains(mx, my)) {
                    gamePanel.gameState = GameState.PLAYING;
                }
            }
            
            inputH.mouseClickedUI = false;
        }
    }

    public void draw(Graphics2D g2) {
        Player p = gamePanel.player;
        
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, gamePanel.screenWidth, gamePanel.screenHeight);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        g2.drawString("CHỌN THẺ BÀI", 200, 40);
        g2.setColor(Color.YELLOW);
        g2.drawString("Vàng: " + p.gold, 650, 40);

        // Vẽ túi đồ hiện tại (6 ô)
        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        for (int i = 0; i < 6; i++) {
            Rectangle invRect = new Rectangle(100 + i * 100, 50, 90, 40);
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(invRect.x, invRect.y, invRect.width, invRect.height);
            
            if (i < p.weaponManager.inventory.size()) {
                g2.setColor(Color.CYAN);
                g2.drawString(p.weaponManager.inventory.get(i).name, invRect.x + 5, invRect.y + 25);
            }
            
            g2.setColor(Color.WHITE);
            g2.drawRect(invRect.x, invRect.y, invRect.width, invRect.height);
            
            if (isDiscarding) {
                g2.setColor(new Color(255, 0, 0, 100));
                g2.fillRect(invRect.x, invRect.y, invRect.width, invRect.height);
            }
        }

        if (isDiscarding) {
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString("CHỌN MỘT THẺ BÊN TRÊN ĐỂ VỨT BỎ!", 200, 120);
        }

        // Vẽ thẻ trong Shop
        for (int i = 0; i < shopCards.size(); i++) {
            Rectangle rect = shopCardRects[i];
            CardType card = shopCards.get(i);
            
            if (rect.contains(inputH.mouseX, inputH.mouseY)) g2.setColor(Color.LIGHT_GRAY);
            else g2.setColor(Color.GRAY);
            
            if (pendingCardToBuy == card) g2.setColor(Color.GREEN);
            
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2.setColor(Color.WHITE);
            g2.drawRect(rect.x, rect.y, rect.width, rect.height);
            
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString(card.name, rect.x + 10, rect.y + 40);
            
            g2.setColor(Color.RED);
            g2.drawString("Giá: " + (BASE_PRICE * card.priceMultiplier), rect.x + 10, rect.y + 65);
            
            // Tự xuống dòng thủ công cho description (đơn giản hóa bằng cách chia nhỏ chuỗi)
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            String[] words = card.description.split(" ");
            int lineY = rect.y + 90;
            String currentLine = "";
            for (String word : words) {
                if (g2.getFontMetrics().stringWidth(currentLine + " " + word) < rect.width - 20) {
                    currentLine += word + " ";
                } else {
                    g2.drawString(currentLine, rect.x + 10, lineY);
                    lineY += 20;
                    currentLine = word + " ";
                }
            }
            g2.drawString(currentLine, rect.x + 10, lineY);
        }

        // Nút mua Gói Nguyên Tố
        int packPrice = p.elementManager.elementPackPrice;
        if (btnBuyElementPack.contains(inputH.mouseX, inputH.mouseY)) g2.setColor(Color.LIGHT_GRAY);
        else g2.setColor(new Color(255, 140, 0)); // Màu cam
        
        g2.fillRect(btnBuyElementPack.x, btnBuyElementPack.y, btnBuyElementPack.width, btnBuyElementPack.height);
        g2.setColor(Color.WHITE);
        g2.drawRect(btnBuyElementPack.x, btnBuyElementPack.y, btnBuyElementPack.width, btnBuyElementPack.height);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("MUA GÓI NGUYÊN TỐ: " + packPrice, btnBuyElementPack.x + 10, btnBuyElementPack.y + 30);

        // Nút Đóng
        if (btnCloseShop.contains(inputH.mouseX, inputH.mouseY)) g2.setColor(Color.LIGHT_GRAY);
        else g2.setColor(Color.GRAY);
        
        g2.fillRect(btnCloseShop.x, btnCloseShop.y, btnCloseShop.width, btnCloseShop.height);
        g2.setColor(Color.WHITE);
        g2.drawRect(btnCloseShop.x, btnCloseShop.y, btnCloseShop.width, btnCloseShop.height);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("ĐÓNG CỬA HÀNG", btnCloseShop.x + 10, btnCloseShop.y + 32);
    }
}
