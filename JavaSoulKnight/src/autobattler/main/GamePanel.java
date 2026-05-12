package autobattler.main;

import autobattler.core.Board;
import autobattler.core.Economy;
import autobattler.core.Hero;
import autobattler.core.Shop;
import autobattler.core.UnitData;
import autobattler.core.UnitFactory;
import autobattler.input.InputHandler;
import autobattler.ui.GameState;
import autobattler.ui.ShopPanel;
import autobattler.ui.UIManager;
import autobattler.ui.UnitInfoPanel;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable {
    public final int screenWidth = 1200;
    public final int screenHeight = 900;
    public double scale = 1.0;
    public double offsetX = 0;
    public double offsetY = 0;
    int FPS = 45; // Giảm từ 60 xuống 45 (tương đương 0.75x speed) để nhìn rõ animation & VFX

    Thread gameThread;
    public InputHandler inputH = new InputHandler(this);

    // Game State
    public GameState gameState = GameState.MAIN_MENU;
    public UIManager uiManager;

    // TFT Systems
    public Economy economy;
    public Shop shop;
    public Board board;
    public ShopPanel shopPanel;
    public UnitInfoPanel unitInfoPanel;

    // Sell zone (vùng thả để bán, hiện dưới bench)
    private static final int SELL_ZONE_X = 10;
    private static final int SELL_ZONE_H = 36;
    private int sellZoneY;
    private int sellZoneW;

    // Result screen timer (giây đếm ngược sau khi thắng/thua)
    private int resultTimer = 0;
    private static final int RESULT_DISPLAY_FRAMES = 180; // 3 giây @60fps

    // Thông tin kết quả màn để hiển thị overlay
    private int goldEarned = 0;      // vàng nhận cuối màn
    private boolean lastRoundWin = false; // thắng hay thua
    private int hpLost = 0;          // máu mất nếu thua
    private boolean isGameOver = false;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(inputH);
        this.addMouseListener(inputH);
        this.addMouseMotionListener(inputH);
        this.addMouseWheelListener(inputH);
        this.setFocusable(true);

        initGame();
        uiManager = new UIManager(this);
    }

    public void initGame() {
        economy = new Economy();
        shop = new Shop();
        board = new Board(economy);
        shopPanel = new ShopPanel(this, shop, economy, hero -> {
            if (!board.addHeroToBench(hero)) {
                economy.addGold(3);
            }
        });

        spawnEnemies(2 + (economy.round - 1) / 2); // màn 1 = 2 địch, mỗi 2 màn +1
        unitInfoPanel = new UnitInfoPanel();

        sellZoneW = board.benchCols * board.tileSize;
        sellZoneY = board.benchStartY + board.benchRows * board.benchSlotH() + 8;
    }

    /** Khởi động màn tiếp theo, giữ nguyên hero player */
    private void startNextRound() {
        economy.nextRound();
        board.generateObstaclesPublic();

        // Xóa địch cũ
        board.heroes.removeIf(h -> h.team != 0);
        for (int r = 0; r < Board.PLAYER_ROW_START; r++)
            for (int c = 0; c < board.cols; c++)
                if (board.heroGrid[r][c] != null && board.heroGrid[r][c].team != 0)
                    board.heroGrid[r][c] = null;

        // Hồi máu hero player
        for (Hero h : board.heroes) {
            h.currentHp = h.maxHp;
            h.state = Hero.State.IDLE;
            h.target = null;
        }

        // Đặt lại hero player về vị trí trên board
        for (int r = Board.PLAYER_ROW_START; r < board.rows; r++)
            for (int c = 0; c < board.cols; c++)
                if (board.heroGrid[r][c] != null) {
                    board.heroGrid[r][c].isOnBench = false;
                }

        // Số kẻ địch: bắt đầu 2, mỗi 2 round +1
        int enemyCount = 2 + (economy.round - 1) / 2;
        spawnEnemies(enemyCount);
        board.isCombatPhase = false;
        board.isPaused      = false;
        board.combatResult  = 0;
        resultTimer = 0;

        gameState = GameState.IN_GAME;
    }

    private void spawnEnemies(int count) {
        Random rand = new Random();
        String[] enemyTypes = UnitData.getAllTypes();
        int spawned = 0;
        int attempts = 0;
        // Mỗi 3 round 1 enemy được nâng sao (tối đa sao 7)
        int baseStars  = 1;
        int bonusStars = Math.min((economy.round - 1) / 3, 6); // số enemy được nâng sao

        while (spawned < count && attempts < 300) {
            attempts++;
            // Chỉ cần 1 ô chân trong enemy zone (grid 1-cell)
            int r = rand.nextInt(Board.PLAYER_ROW_START - 1) + 1; // 1..(PLAYER_ROW_START-1), để đầu nằm ở r-1
            int c = rand.nextInt(board.cols);

            // Kiểm tra ô chân và ô đầu (visual) đều trống
            if (board.grid[r][c] != 0 || board.heroGrid[r][c] != null) continue;
            if (r - 1 >= 0 && board.grid[r-1][c] != 0) continue; // obstacle nhưng không chặn đầu

            // Star level: spawned < bonusStars → nâng sao
            int starLevel = (spawned < bonusStars) ? Math.min(baseStars + 1, 7) : baseStars;

            String heroType = enemyTypes[rand.nextInt(enemyTypes.length)];
            Hero enemy = UnitFactory.createEnemy(heroType, starLevel, 2);
            enemy.isOnBench = false;
            enemy.gridX = c;
            enemy.gridY = r;           // gridY = ô chân (1-cell)
            enemy.combatGridX = c;
            enemy.combatGridY = r - (Hero.BODY_H - 1); // combat: ô đầu
            board.heroGrid[r][c] = enemy; // chỉ đánh dấu 1 ô
            board.heroes.add(enemy);
            spawned++;
        }
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
        if (gameState == GameState.IN_GAME) {
            board.update(inputH);
            shopPanel.update(inputH);

            // ── Bán tướng bằng chuột phải ──────────────────────────────
            if (inputH.mouseRightClicked) {
                Hero clicked = getHeroAtMouse(inputH.mouseX, inputH.mouseY);
                if (clicked != null) { // ĐÃ XÓA '&& clicked.team == 0' ĐỂ XEM ĐƯỢC CẢ ĐỊCH
                    if (unitInfoPanel.isShowing(clicked)) {
                        unitInfoPanel.close();
                    } else {
                        unitInfoPanel.toggle(clicked);
                    }
                } else {
                    unitInfoPanel.toggle(null);
                }
            }

            if (inputH.mouseClicked) {
                unitInfoPanel.handleClick(inputH.mouseX, inputH.mouseY, board.isCombatPhase);
            }

            unitInfoPanel.setMousePos(inputH.mouseX, inputH.mouseY);
            unitInfoPanel.update(inputH.mouseX, inputH.mouseY);

            // ── Kiểm tra kết quả chiến đấu ─────────────────────────────
            if (board.isCombatPhase && board.combatResult != 0 && resultTimer == 0) {
                // Tính toán kết quả ngay khi combat kết thúc (chỉ 1 lần)
                lastRoundWin = (board.combatResult == 1);

                if (!lastRoundWin) {
                    // Thua: tính số địch còn sống
                    int aliveEnemies = 0;
                    for (autobattler.core.Hero h : board.heroes) {
                        if (h.team != 0 && h.state != autobattler.core.Hero.State.DEAD && !h.isOnBench)
                            aliveEnemies++;
                    }
                    hpLost = economy.calcHpLoss(aliveEnemies);
                    isGameOver = economy.loseHp(hpLost);
                } else {
                    hpLost = 0;
                    isGameOver = false;
                }

                // Áp dụng vàng cuối màn
                goldEarned = economy.applyEndRoundGold(lastRoundWin);
            }

            if (board.isCombatPhase && board.combatResult != 0) {
                resultTimer++;
                if (resultTimer >= RESULT_DISPLAY_FRAMES) {
                    if (isGameOver) {
                        // HP về 0 → Game Over thật sự
                        initGame();
                    } else {
                        // Luôn qua màn tiếp (dù thắng hay thua)
                        startNextRound();
                    }
                }
            }

        } else {
            uiManager.update();
        }
        inputH.mouseClicked = false;
        inputH.mouseRightClicked = false;
    }

    /** Tìm hero tại toạ độ chuột (board + bench) */
    private Hero getHeroAtMouse(int mx, int my) {
        for (autobattler.core.Hero h : board.heroes) {
            if (h.state == autobattler.core.Hero.State.DEAD) continue;
            
            int hx, hy;
            if (h.isOnBench) {
                hx = board.benchStartX + h.gridX * board.tileSize;
                hy = board.benchStartY + h.gridY * board.benchSlotH();
            } else if (h.isDragging) {
                hx = h.worldX;
                hy = h.worldY;
            } else if (board.isCombatPhase) {
                // Trong trận: tính trực tiếp từ combatGrid
                hx = board.startX + h.combatGridX * board.tileSize;
                hy = board.startY + h.combatGridY * board.tileSize;
            } else {
                // Chuẩn bị: gridY là chân, lui lên cho đủ BODY_H
                hx = board.startX + h.gridX * board.tileSize;
                hy = board.startY + (h.gridY - (autobattler.core.Hero.BODY_H - 1)) * board.tileSize;
            }
            
            if (mx >= hx && mx < hx + board.tileSize &&
                my >= hy && my < hy + board.tileSize * autobattler.core.Hero.BODY_H) {
                return h;
            }
        }
        return null;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        scale = Math.min((double) getWidth() / screenWidth, (double) getHeight() / screenHeight);
        offsetX = (getWidth() - screenWidth * scale) / 2;
        offsetY = (getHeight() - screenHeight * scale) / 2;

        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);

        if (gameState == GameState.IN_GAME) {
            g2.setColor(new Color(15, 15, 25));
            g2.fillRect(0, 0, screenWidth, screenHeight);

            board.draw(g2);

            // Nút bán: hiện ở dưới bench khi có hero player nào đó
            drawSellZone(g2);

            g2.setColor(new Color(60, 60, 100));
            g2.fillRect(ShopPanel.PANEL_X - 2, 0, 2, screenHeight);

            shopPanel.draw(g2);

            // ── Thanh máu người chơi ────────────────────────────────────
            drawPlayerHpBar(g2);

            if (unitInfoPanel != null) unitInfoPanel.draw(g2);

            // ── Màn hình kết quả (overlay) ─────────────────────────────
            if (board.isCombatPhase && board.combatResult != 0) {
                drawResultOverlay(g2);
            }

        } else {
            uiManager.draw(g2);
        }

        g2.dispose();
    }

    private void drawSellZone(Graphics2D g2) {
        // Vùng bán nằm dưới bench
        boolean hasBoardHero = false;
        for (Hero h : board.heroes)
            if (h.team == 0) { hasBoardHero = true; break; }
        if (!hasBoardHero) return;

        g2.setColor(new Color(180, 40, 40, 200));
        g2.fillRoundRect(SELL_ZONE_X, sellZoneY, sellZoneW, SELL_ZONE_H, 12, 12);
        g2.setColor(new Color(255, 80, 80));
        g2.drawRoundRect(SELL_ZONE_X, sellZoneY, sellZoneW, SELL_ZONE_H, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        String txt = "🗑 BÁN TƯỚNG  [Chuột phải 2 lần]  +2 🪙";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(txt, SELL_ZONE_X + (sellZoneW - fm.stringWidth(txt)) / 2,
                sellZoneY + SELL_ZONE_H / 2 + fm.getAscent() / 2 - 2);
    }

    private void drawPlayerHpBar(Graphics2D g2) {
        // Vẽ HP bar ngay TRONG bench label (bên phải nhãn "BĂNG GHỌ DỰ BỊ")
        int barX = board.benchStartX + 190; // sau nhãn bench
        int barY = board.benchStartY - 14;  // cùng hàng với bench label
        int barW = 160;
        int barH = 12;

        // Nền
        g2.setColor(new Color(60, 0, 0));
        g2.fillRoundRect(barX, barY, barW, barH, 6, 6);
        // Phần máu còn lại
        int fill = (int)(economy.playerHp / 100.0 * barW);
        Color hpColor = economy.playerHp > 50 ? new Color(50, 200, 80)
                      : economy.playerHp > 25 ? new Color(240, 180, 0)
                      : new Color(220, 50, 50);
        g2.setColor(hpColor);
        g2.fillRoundRect(barX, barY, fill, barH, 6, 6);
        // Viền
        g2.setColor(new Color(180, 180, 180));
        g2.drawRoundRect(barX, barY, barW, barH, 6, 6);
        // Text
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        String hpText = "❤ " + economy.playerHp + "/100";
        g2.drawString(hpText, barX + barW + 8, barY + barH - 1);
    }

    private void drawResultOverlay(Graphics2D g2) {
        float alpha = Math.min(1f, resultTimer / 30f); // fade in

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.78f));
        if (isGameOver) {
            g2.setColor(new Color(80, 0, 0));
        } else {
            g2.setColor(lastRoundWin ? new Color(15, 50, 15) : new Color(50, 10, 10));
        }
        g2.fillRect(0, 0, screenWidth, screenHeight);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Tiêu đề
        String title;
        Color titleColor;
        if (isGameOver) {
            title = "☠ GAME OVER ☠";
            titleColor = new Color(255, 60, 60);
        } else if (lastRoundWin) {
            title = "⚔ CHIẾN THẮNG! ⚔";
            titleColor = new Color(255, 220, 50);
        } else {
            title = "💀 THẤT BẠI 💀";
            titleColor = new Color(220, 60, 60);
        }
        g2.setFont(new Font("Arial", Font.BOLD, 72));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(titleColor);
        g2.drawString(title, (screenWidth - fm.stringWidth(title)) / 2, screenHeight / 2 - 60);

        int cy = screenHeight / 2 - 10;

        // Phụ đề
        String sub;
        if (isGameOver) {
            sub = "Máu đã cạn kiệt! Trò chơi kết thúc...";
        } else if (lastRoundWin) {
            sub = "Màn " + economy.round + " hoàn thành! → Màn " + (economy.round + 1) + " bắt đầu sau 3 giây...";
        } else {
            sub = "Thất bại! Mất " + hpLost + " máu. Còn " + economy.playerHp + "/100 ❤ → Qua màn tiếp sau 3 giây...";
        }
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        fm = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString(sub, (screenWidth - fm.stringWidth(sub)) / 2, cy);
        cy += 38;

        // Thông tin vàng nhận được (nếu không game over)
        if (!isGameOver) {
            String goldInfo = "Vàng nhận: +" + goldEarned + " 🪙  (Lợi tức: " + Math.min(economy.gold / 10, 5)
                + "  |  Chuỗi: x" + (lastRoundWin ? economy.winStreak : economy.loseStreak) + ")";
            g2.setFont(new Font("Arial", Font.PLAIN, 18));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(255, 210, 80));
            g2.drawString(goldInfo, (screenWidth - fm.stringWidth(goldInfo)) / 2, cy);
            cy += 32;

            // Màn tiếp
            int nextCount = 5 + economy.round * 2;
            String info = "Màn tiếp: " + nextCount + " kẻ địch";
            g2.setFont(new Font("Arial", Font.PLAIN, 16));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(200, 200, 255));
            g2.drawString(info, (screenWidth - fm.stringWidth(info)) / 2, cy);
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
}
