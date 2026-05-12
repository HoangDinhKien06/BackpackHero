package autobattler.ui;

import autobattler.core.Hero;
import autobattler.core.UnitData;
import autobattler.main.GamePanel;
import autobattler.main.Main;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JFrame;

public class UIManager {
    GamePanel gp;
    
    // Settings
    public static boolean enableScreenShake = true;
    public static boolean isFullScreen = false;
    
    private int[][] allResolutions = {
        {800, 600}, {1024, 768}, {1200, 900}, {1366, 768}, {1600, 900}, {1920, 1080}
    };
    private int currentResIndex = 2; // Default 1200x900
    
    // Lists of buttons for different states
    private ArrayList<UIButton> mainMenuButtons;
    private ArrayList<UIButton> settingsButtons;
    private ArrayList<UIButton> levelSelectButtons;
    private ArrayList<UIButton> profileButtons;
    
    // Visual FX
    private int time = 0;
    private int shakeTimer = 0;
    private Random random = new Random();

    // Profile screen scroll
    private int profileScrollY = 0;
    // Card drag-to-scroll state
    private boolean profileDragging      = false;
    private int profileDragStartY        = 0;
    private int profileDragScrollStart   = 0;
    private int profileDragDeltaY        = 0;
    // Scrollbar drag state
    private boolean sbDragging           = false;
    private int sbDragStartY             = 0;
    private int sbDragScrollStart        = 0;
    // Profile right-click detail popup
    private String profileSelectedType   = null;

    // Shared profile layout constants (used by both draw and hit-test)
    private static final int PROF_COLS     = 2;
    private static final int PROF_HERO_TILE = 52;
    private static final int PROF_CARD_H   = 190;  // tall enough for 6 stat rows
    private static final int PROF_CARD_GAP = 12;
    private static final int PROF_PAD_X    = 24;
    private static final int PROF_TOP_PAD  = 90;
    private static final int PROF_SB_W     = 14;   // scrollbar width
    private static final int PROF_HEADER_H = 80;
    private static final int PROF_BTN_H    = 100;  // bottom button bar height

    public UIManager(GamePanel gp) {
        this.gp = gp;
        initButtons();
    }

    public void triggerShake() {
        if (enableScreenShake) {
            shakeTimer = 10; // 10 frames of shake
        }
    }

    private void initButtons() {
        mainMenuButtons = new ArrayList<>();
        settingsButtons = new ArrayList<>();
        levelSelectButtons = new ArrayList<>();
        profileButtons = new ArrayList<>();

        // Main Menu Buttons - Horizontal Layout at the bottom (4 buttons)
        int btnWidth = 200;
        int btnHeight = 60;
        int spacing = 24;
        int totalWidth = 4 * btnWidth + 3 * spacing;
        int startX = (gp.screenWidth - totalWidth) / 2;
        int startY = gp.screenHeight - 120; // Near bottom

        // 4 distinct colors
        Color c1 = new Color(220, 50, 50);   // Red
        Color c2 = new Color(50, 200, 50);   // Green
        Color c3 = new Color(250, 150, 50);  // Orange
        Color c4 = new Color(150, 50, 200);  // Purple
        Color c5 = new Color(50, 150, 250);  // Blue (for settings)

        mainMenuButtons.add(new UIButton(startX, startY, btnWidth, btnHeight, "Bắt Đầu", c1, () -> {
            triggerShake();
            gp.gameState = GameState.LEVEL_SELECT;
        }));

        mainMenuButtons.add(new UIButton(startX + (btnWidth + spacing), startY, btnWidth, btnHeight, "Tùy Chọn", c5, () -> {
            triggerShake();
            gp.gameState = GameState.SETTINGS;
        }));

        mainMenuButtons.add(new UIButton(startX + (btnWidth + spacing) * 2, startY, btnWidth, btnHeight, "Hồ Sơ", c3, () -> {
            triggerShake();
            profileScrollY = 0;
            gp.gameState = GameState.PROFILE;
        }));

        mainMenuButtons.add(new UIButton(startX + (btnWidth + spacing) * 3, startY, btnWidth, btnHeight, "Thoát", c4, () -> {
            triggerShake();
            System.exit(0);
        }));

        // Settings Buttons
        int centerX = gp.screenWidth / 2 - 150;
        
        settingsButtons.add(new UIButton(centerX, 280, 300, 50, "Rung: Bật", c1, () -> {
            enableScreenShake = !enableScreenShake;
            settingsButtons.get(0).text = "Rung: " + (enableScreenShake ? "Bật" : "Tắt");
            triggerShake();
        }));
        
        String initResText = "Kích Thước: " + allResolutions[currentResIndex][0] + "x" + allResolutions[currentResIndex][1];
        settingsButtons.add(new UIButton(centerX, 360, 300, 50, initResText, c2, () -> {
            triggerShake();
            if (!isFullScreen) {
                currentResIndex = (currentResIndex + 1) % allResolutions.length;
                int w = allResolutions[currentResIndex][0];
                int h = allResolutions[currentResIndex][1];
                settingsButtons.get(1).text = "Kích Thước: " + w + "x" + h;

                // Để tránh Swing/Windows không resize đúng khi frame đã từng ở trạng thái khác,
                // ta dispose rồi pack lại theo preferred size của GamePanel.
                Main.window.setExtendedState(JFrame.NORMAL);
                Main.window.dispose();
                Main.window.setUndecorated(false);
                gp.setPreferredSize(new Dimension(w, h));
                gp.revalidate();
                Main.window.pack();

                // Đảm bảo client area đúng đúng w x h (tránh bị sai do insets/viền cửa sổ).
                java.awt.Insets insets = Main.window.getInsets();
                Main.window.setSize(
                        w + insets.left + insets.right,
                        h + insets.top + insets.bottom
                );
                Main.window.validate();

                Main.window.setLocationRelativeTo(null);
                Main.window.setVisible(true);
            }
        }));
        
        settingsButtons.add(new UIButton(centerX, 440, 300, 50, "Toàn Màn Hình: Tắt", c3, () -> {
            triggerShake();
            isFullScreen = !isFullScreen;
            settingsButtons.get(2).text = "Toàn Màn Hình: " + (isFullScreen ? "Bật" : "Tắt");
            
            Main.window.dispose();
            if (isFullScreen) {
                Main.window.setUndecorated(true);
                Main.window.setExtendedState(JFrame.MAXIMIZED_BOTH);
            } else {
                Main.window.setUndecorated(false);
                Main.window.setExtendedState(JFrame.NORMAL);
                int w = allResolutions[currentResIndex][0];
                int h = allResolutions[currentResIndex][1];
                gp.setPreferredSize(new Dimension(w, h));
                Main.window.pack();

                // Đảm bảo client area đúng đúng w x h (tránh bị sai do insets/viền cửa sổ).
                java.awt.Insets insets = Main.window.getInsets();
                Main.window.setSize(
                        w + insets.left + insets.right,
                        h + insets.top + insets.bottom
                );
                Main.window.validate();

                Main.window.setLocationRelativeTo(null);
            }
            Main.window.setVisible(true);
        }));

        settingsButtons.add(new UIButton(centerX, 520, 300, 50, "Ngôn Ngữ: VN", c4, () -> {
            triggerShake();
        }));
        
        settingsButtons.add(new UIButton(centerX, 600, 300, 50, "Quay Lại", c5, () -> {
            triggerShake();
            gp.gameState = GameState.MAIN_MENU;
        }));

        // Level Select Buttons
        int levelCenterX = gp.screenWidth / 2 - 125;
        levelSelectButtons.add(new UIButton(levelCenterX, 300, 250, 50, "Vào Trận", c1, () -> {
            triggerShake();
            gp.initGame(); // fresh game each time
            gp.gameState = GameState.IN_GAME;
        }));
        levelSelectButtons.add(new UIButton(levelCenterX, 580, 250, 50, "Quay Lại", c4, () -> {
            triggerShake();
            gp.gameState = GameState.MAIN_MENU;
        }));

        // Profile Buttons — just a back button at the bottom
        profileButtons.add(new UIButton(gp.screenWidth / 2 - 125, gp.screenHeight - 90, 250, 50, "\u2190 Quay L\u1ea1i", c5, () -> {
            triggerShake();
            profileSelectedType = null;
            gp.gameState = GameState.MAIN_MENU;
        }));
    }

    public void update() {
        time++;
        if (shakeTimer > 0) {
            shakeTimer--;
        }

        int mx = gp.inputH.mouseX;
        int my = gp.inputH.mouseY;
        boolean pressed = gp.inputH.mousePressed;
        boolean clicked = gp.inputH.mouseClicked;

        if (gp.gameState == GameState.MAIN_MENU) {
            for (UIButton b : mainMenuButtons) b.update(mx, my, pressed, clicked);
        } else if (gp.gameState == GameState.SETTINGS) {
            for (UIButton b : settingsButtons) b.update(mx, my, pressed, clicked);
        } else if (gp.gameState == GameState.LEVEL_SELECT) {
            for (UIButton b : levelSelectButtons) b.update(mx, my, pressed, clicked);
        } else if (gp.gameState == GameState.PROFILE) {
            for (UIButton b : profileButtons) b.update(mx, my, pressed, clicked);

            // Compute scroll bounds
            String[] types = UnitData.getAllTypes();
            int totalRows    = (int) Math.ceil((double) types.length / PROF_COLS);
            int totalContent = totalRows * (PROF_CARD_H + PROF_CARD_GAP);
            int visibleH     = gp.screenHeight - PROF_HEADER_H - PROF_BTN_H;
            int maxScroll    = Math.max(0, totalContent - visibleH + 20);

            // ── Mouse wheel scroll ──────────────────────────────────────────
            int wheel = gp.inputH.mouseWheelRot;
            if (wheel != 0) {
                profileScrollY = Math.max(0, Math.min(maxScroll, profileScrollY - wheel * 25));
                gp.inputH.mouseWheelRot = 0;
            }

            // ── Scrollbar drag ────────────────────────────────────────────
            int sbX = gp.screenWidth - PROF_SB_W - 4;
            int sbTrackY = PROF_HEADER_H + 4;
            int sbTrackH = visibleH - 8;
            boolean onScrollbar = (mx >= sbX - 4 && mx <= gp.screenWidth - 2
                                   && my >= sbTrackY && my <= sbTrackY + sbTrackH);

            if (gp.inputH.mousePressed) {
                if (sbDragging) {
                    // Drag thumb
                    int delta = my - sbDragStartY;
                    if (maxScroll > 0) {
                        // thumb height / track ratio
                        int thumbH = Math.max(30, sbTrackH * visibleH / (totalContent + 1));
                        double ratio = (double)(sbTrackH - thumbH) / maxScroll;
                        profileScrollY = Math.max(0, Math.min(maxScroll,
                                sbDragScrollStart + (int)(delta / ratio)));
                    }
                } else if (!profileDragging && onScrollbar) {
                    sbDragging       = true;
                    sbDragStartY     = my;
                    sbDragScrollStart = profileScrollY;
                } else if (!sbDragging) {
                    // Card area drag-to-scroll
                    if (!profileDragging) {
                        profileDragging       = true;
                        profileDragStartY     = my;
                        profileDragScrollStart = profileScrollY;
                        profileDragDeltaY     = 0;
                    } else {
                        profileDragDeltaY = my - profileDragStartY;
                        if (Math.abs(profileDragDeltaY) > 5) {
                            profileScrollY = Math.max(0, Math.min(maxScroll,
                                    profileDragScrollStart - profileDragDeltaY));
                        }
                    }
                }
            } else {
                sbDragging      = false;
                profileDragging = false;
            }

            // ── Right-click: select/deselect hero card ──────────────────────
            if (gp.inputH.mouseRightClicked) {
                String hitType = getProfileCardAt(mx, my);
                if (hitType != null && hitType.equals(profileSelectedType)) {
                    profileSelectedType = null;
                } else {
                    profileSelectedType = hitType;
                }
            }
            if (clicked && profileSelectedType != null) {
                String hitType = getProfileCardAt(mx, my);
                if (hitType == null) profileSelectedType = null;
            }
        } // end PROFILE block
    } // end update()


    public void draw(Graphics2D g2) {
        AffineTransform oldTx = g2.getTransform();
        
        // Apply Camera Shake
        if (shakeTimer > 0) {
            int shakeX = random.nextInt(11) - 5; // -5 to +5
            int shakeY = random.nextInt(11) - 5; // -5 to +5
            g2.translate(shakeX, shakeY);
        }

        drawBackground(g2);

        if (gp.gameState == GameState.MAIN_MENU) {
            drawLogo(g2);
            for (UIButton b : mainMenuButtons) b.draw(g2);
        } else if (gp.gameState == GameState.SETTINGS) {
            drawLogo(g2);
            for (UIButton b : mainMenuButtons) b.draw(g2); // Draw main menu behind
            drawSettingsOverlay(g2);
            for (UIButton b : settingsButtons) b.draw(g2);
        } else if (gp.gameState == GameState.LEVEL_SELECT) {
            drawLevelSelectScreen(g2);
            for (UIButton b : levelSelectButtons) b.draw(g2);
        } else if (gp.gameState == GameState.PROFILE) {
            drawProfileScreen(g2);
            for (UIButton b : profileButtons) b.draw(g2);
            // Detail popup on top of everything
            if (profileSelectedType != null) {
                drawHeroDetailPopup(g2, profileSelectedType);
            }
        }

        drawCRTOverlay(g2);

        // Restore Transform after everything
        g2.setTransform(oldTx);
    }

    private void drawBackground(Graphics2D g2) {
        // Psychedelic background using Math.sin
        for (int y = 0; y < gp.screenHeight; y += 10) {
            // Calculate a color based on sine waves and time
            float wave = (float) Math.sin((y + time * 2) * 0.02f);
            int r = (int) (20 + 10 * wave);
            int g = (int) (30 + 15 * Math.cos(time * 0.01f));
            int b = (int) (50 + 20 * wave);
            g2.setColor(new Color(r, g, b));
            g2.fillRect(0, y, gp.screenWidth, 10);
        }
    }
    
    private void drawCRTOverlay(Graphics2D g2) {
        // Scanlines
        g2.setColor(new Color(0, 0, 0, 40));
        for (int y = 0; y < gp.screenHeight; y += 3) {
            g2.drawLine(0, y, gp.screenWidth, y);
        }
    }

    private void drawLogo(Graphics2D g2) {
        // Logo glow
        g2.setColor(new Color(100, 200, 255, 50));
        g2.fillOval(gp.screenWidth / 2 - 320, 40, 640, 210);

        g2.setFont(new Font("Impact", Font.ITALIC, 85));
        String title = "BACKPACK HERO";
        int x = gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(title) / 2;

        // Shadow
        g2.setColor(new Color(0, 80, 120));
        g2.drawString(title, x + 6, 158);
        // Gradient-style: draw twice with slightly different y for depth
        g2.setColor(new Color(0, 180, 255));
        g2.drawString(title, x, 153);
        g2.setColor(new Color(180, 240, 255));
        g2.drawString(title, x, 150);

        g2.setColor(new Color(160, 230, 255));
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        String subTitle = "Auto Battler Edition";
        int subX = gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(subTitle) / 2;
        g2.drawString(subTitle, subX, 220);
    }

    private void drawSettingsOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 200)); // Darker overlay
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        
        int panelW = 600;
        int panelH = 550;
        int panelX = gp.screenWidth / 2 - panelW / 2;
        int panelY = gp.screenHeight / 2 - panelH / 2;
        
        // Panel background
        g2.setColor(new Color(30, 30, 40));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);
        
        // Panel border
        g2.setColor(new Color(100, 100, 150));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 20, 20);
        
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        String title = "TÙY CHỌN";
        g2.drawString(title, gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(title) / 2, panelY + 60);
    }
    
    private void drawLevelSelectScreen(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        String title = "CHỌN MÀN CHƠI";
        int x = gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(title) / 2;
        g2.drawString(title, x, 150);
    }

    // ─── Profile / Roster Screen ──────────────────────────────────────────────

    private void drawProfileScreen(Graphics2D g2) {
        // Title bar
        g2.setColor(new Color(10, 20, 40, 230));
        g2.fillRect(0, 0, gp.screenWidth, PROF_HEADER_H);
        g2.setColor(new Color(0, 180, 255));
        g2.setFont(new Font("SansSerif", Font.BOLD, 44));
        String heading = "H\u1ed2 S\u01a0 T\u01af\u1edaNG";
        g2.drawString(heading, gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(heading) / 2, 57);

        String[] types = UnitData.getAllTypes();

        // Layout from shared constants
        int cardW      = (gp.screenWidth - PROF_SB_W - 16 - PROF_PAD_X * 2 - 20) / PROF_COLS;
        int visibleH   = gp.screenHeight - PROF_HEADER_H - PROF_BTN_H;
        int totalRows  = (int) Math.ceil((double) types.length / PROF_COLS);
        int totalContent = totalRows * (PROF_CARD_H + PROF_CARD_GAP);
        int maxScroll  = Math.max(0, totalContent - visibleH + 20);
        profileScrollY = Math.min(profileScrollY, maxScroll);

        // Clip to scrollable area (leave room for scrollbar on right)
        java.awt.Shape oldClip = g2.getClip();
        g2.setClip(0, PROF_HEADER_H, gp.screenWidth - PROF_SB_W - 6, visibleH);

        for (int i = 0; i < types.length; i++) {
            UnitData.UnitStats st = UnitData.get(types[i]);
            if (st == null) continue;

            int col = i % PROF_COLS;
            int row = i / PROF_COLS;
            int cx  = PROF_PAD_X + col * (cardW + 20);
            int cy  = PROF_TOP_PAD - profileScrollY + row * (PROF_CARD_H + PROF_CARD_GAP);

            if (cy + PROF_CARD_H < PROF_HEADER_H || cy > gp.screenHeight - PROF_BTN_H) continue;

            // Card background
            g2.setColor(new Color(14, 20, 38, 218));
            g2.fillRoundRect(cx, cy, cardW, PROF_CARD_H, 14, 14);

            // Left accent bar
            g2.setColor(st.color);
            g2.fillRoundRect(cx, cy, 7, PROF_CARD_H, 7, 7);

            // Hero 2x1 preview vertically centered
            int heroX = cx + 14;
            int heroY = cy + (PROF_CARD_H - PROF_HERO_TILE * Hero.BODY_H) / 2;

            g2.setColor(new Color(st.color.getRed(), st.color.getGreen(), st.color.getBlue(), 35));
            g2.fillOval(heroX - 6, heroY - 6,
                        PROF_HERO_TILE * Hero.BODY_W + 12, PROF_HERO_TILE * Hero.BODY_H + 12);

            Hero preview = new Hero(0, st.name, st.heroType, st.color, 0,
                                    st.attackRange, st.baseHp, st.attackDamage);
            preview.draw(g2, heroX, heroY, PROF_HERO_TILE);

            // Text area (right of hero)
            int tx    = heroX + PROF_HERO_TILE * Hero.BODY_W + 14;
            int infoW = cardW - (tx - cx) - 10;

            // Name
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            g2.drawString(st.name, tx, cy + 18);

            // Type badge
            g2.setColor(new Color(st.color.getRed(), st.color.getGreen(), st.color.getBlue(), 160));
            g2.fillRoundRect(tx, cy + 23, 80, 15, 6, 6);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            g2.drawString(st.heroType, tx + 5, cy + 34);

            // Cost badge top-right
            g2.setColor(new Color(255, 200, 0, 210));
            g2.fillRoundRect(cx + cardW - 46, cy + 7, 38, 18, 6, 6);
            g2.setColor(new Color(20, 10, 0));
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString(st.cost + " G", cx + cardW - 40, cy + 20);

            // Description wrapped
            g2.setColor(new Color(155, 195, 230));
            g2.setFont(new Font("SansSerif", Font.ITALIC, 10));
            drawWrappedText(g2, st.description, tx, cy + 42, infoW, 12);

            // Stats: sy=cy+62, lineH=19 -> 6x19=114 -> ends at cy+176 < PROF_CARD_H=190
            int sy    = cy + 62;
            int lineH = 19;
            drawProfileStat(g2, tx, sy,             infoW, "M\u00e1u",                  st.baseHp + " HP",        new Color(80, 220, 80));
            drawProfileStat(g2, tx, sy + lineH,     infoW, "S\u00e1t th\u01b0\u01a1ng", st.attackDisplay() + " DMG",   new Color(220, 80, 80));
            drawProfileStat(g2, tx, sy + lineH * 2, infoW, "T\u1ea7m \u0111\u00e1nh",   st.attackRange + " \u00f4",new Color(80, 150, 220));
            drawProfileStat(g2, tx, sy + lineH * 3, infoW, "T\u1ed1c \u0111\u00e1nh",   st.attackCooldown + "f",  new Color(220, 200, 50));
            drawProfileStat(g2, tx, sy + lineH * 4, infoW, "T\u1ed1c ch\u1ea1y",        st.moveCooldown + "f",    new Color(160, 100, 230));
            
            boolean usesRage = st.rageMax > 0;
            int energyVal = usesRage ? st.rageMax : st.baseMana;
            String energyLabel = usesRage ? "N\u1ed9" : "Mana";
            String energySuffix = usesRage ? " n\u1ed9" : " MP";
            Color energyColor = usesRage ? new Color(255, 120, 0) : new Color(0, 150, 255);
            drawProfileStat(g2, tx, sy + lineH * 5, infoW, energyLabel, energyVal + energySuffix, energyColor);

            // Card border
            g2.setColor(new Color(st.color.getRed(), st.color.getGreen(), st.color.getBlue(), 90));
            g2.drawRoundRect(cx, cy, cardW, PROF_CARD_H, 14, 14);
        }

        g2.setClip(oldClip);

        // Scrollbar track
        int sbX      = gp.screenWidth - PROF_SB_W - 4;
        int sbTrackY = PROF_HEADER_H + 4;
        int sbTrackH = visibleH - 8;

        g2.setColor(new Color(20, 30, 55, 210));
        g2.fillRoundRect(sbX, sbTrackY, PROF_SB_W, sbTrackH, PROF_SB_W, PROF_SB_W);
        g2.setColor(new Color(50, 70, 110));
        g2.drawRoundRect(sbX, sbTrackY, PROF_SB_W, sbTrackH, PROF_SB_W, PROF_SB_W);

        // Scrollbar thumb
        if (maxScroll > 0) {
            int thumbH = Math.max(30, sbTrackH * visibleH / (totalContent + 1));
            int thumbY = sbTrackY + (int)((double) profileScrollY / maxScroll * (sbTrackH - thumbH));
            g2.setColor(sbDragging ? new Color(80, 200, 255, 220) : new Color(0, 160, 255, 190));
            g2.fillRoundRect(sbX + 2, thumbY, PROF_SB_W - 4, thumbH, PROF_SB_W - 4, PROF_SB_W - 4);
            g2.setColor(new Color(100, 220, 255, 140));
            g2.drawRoundRect(sbX + 2, thumbY, PROF_SB_W - 4, thumbH, PROF_SB_W - 4, PROF_SB_W - 4);
            // Grip dots
            g2.setColor(new Color(200, 240, 255, 130));
            int midX = sbX + PROF_SB_W / 2;
            int midY = thumbY + thumbH / 2;
            g2.drawLine(midX - 2, midY - 3, midX + 2, midY - 3);
            g2.drawLine(midX - 2, midY,     midX + 2, midY);
            g2.drawLine(midX - 2, midY + 3, midX + 2, midY + 3);
        }

        // Bottom hint
        g2.setColor(new Color(80, 100, 140));
        g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
        String hint = "Cu\u1ed9n / k\u00e9o \u0111\u1ec3 xem th\u00eam  \u2014  Click ph\u1ea3i \u0111\u1ec3 chi ti\u1ebft  \u2014  " + types.length + " t\u01b0\u1edbng";
        g2.drawString(hint, gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(hint) / 2,
                gp.screenHeight - 50);
    }

    /** Returns the heroType key of the card at screen coords (mx,my), or null. */
    private String getProfileCardAt(int mx, int my) {
        String[] types = UnitData.getAllTypes();
        int cardW = (gp.screenWidth - PROF_SB_W - 16 - PROF_PAD_X * 2 - 20) / PROF_COLS;
        for (int i = 0; i < types.length; i++) {
            int col = i % PROF_COLS;
            int row = i / PROF_COLS;
            int cx  = PROF_PAD_X + col * (cardW + 20);
            int cy  = PROF_TOP_PAD - profileScrollY + row * (PROF_CARD_H + PROF_CARD_GAP);
            if (mx >= cx && mx <= cx + cardW && my >= cy && my <= cy + PROF_CARD_H) {
                return types[i];
            }
        }
        return null;
    }


    /** Draws text that wraps automatically when wider than maxW. */
    private void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxW, int lineH) {
        FontMetrics fm = g2.getFontMetrics();
        StringBuilder line = new StringBuilder();
        int lineY = y + fm.getAscent();
        for (String word : text.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(test) > maxW && !line.isEmpty()) {
                g2.drawString(line.toString(), x, lineY);
                line = new StringBuilder(word);
                lineY += lineH;
            } else {
                line = new StringBuilder(test);
            }
        }
        if (!line.isEmpty()) g2.drawString(line.toString(), x, lineY);
    }

    /** Full detail popup when right-clicking a hero card */
    private void drawHeroDetailPopup(Graphics2D g2, String heroType) {
        UnitData.UnitStats st = UnitData.get(heroType);
        if (st == null) return;

        int popW = 400;
        int popH = 470;
        int popX = gp.screenWidth / 2 - popW / 2;
        int popY = gp.screenHeight / 2 - popH / 2;

        // ── Dim overlay ───────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // ── Panel background ─────────────────────────────────────────────
        g2.setColor(new Color(12, 18, 35, 245));
        g2.fillRoundRect(popX, popY, popW, popH, 18, 18);
        // Colored border using hero color
        g2.setColor(new Color(st.color.getRed(), st.color.getGreen(), st.color.getBlue(), 200));
        g2.drawRoundRect(popX, popY, popW, popH, 18, 18);
        // Inner glow line
        g2.setColor(new Color(st.color.getRed(), st.color.getGreen(), st.color.getBlue(), 60));
        g2.drawRoundRect(popX + 2, popY + 2, popW - 4, popH - 4, 16, 16);

        int y = popY + 16;

        // ── Hero big preview (2×1) centered at top ──────────────────────
        int bigTile = 68;
        int heroDrawX = popX + 20;
        int heroDrawY = y;
        // Glow halo
        g2.setColor(new Color(st.color.getRed(), st.color.getGreen(), st.color.getBlue(), 50));
        g2.fillOval(heroDrawX - 8, heroDrawY - 8,
                    bigTile * Hero.BODY_W + 16, bigTile * Hero.BODY_H + 16);
        Hero preview = new Hero(0, st.name, st.heroType, st.color, 0,
                                st.attackRange, st.baseHp, st.attackDamage);
        preview.draw(g2, heroDrawX, heroDrawY, bigTile);

        // ── Name + type + cost (right of hero) ────────────────────────
        int tx = heroDrawX + bigTile * Hero.BODY_W + 18;

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.drawString(st.name, tx, y + 24);

        // Type badge
        g2.setColor(new Color(st.color.getRed(), st.color.getGreen(), st.color.getBlue(), 180));
        g2.fillRoundRect(tx, y + 30, 100, 18, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString(st.heroType, tx + 6, y + 43);

        // Cost
        g2.setColor(new Color(255, 200, 0));
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString(st.cost + " G", tx, y + 70);

        // Description (full, wrapped)
        g2.setColor(new Color(160, 205, 240));
        g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
        drawWrappedText(g2, st.description, tx, y + 80, popW - (tx - popX) - 14, 15);

        // ── Divider ────────────────────────────────────────────────────
        int divY = popY + bigTile * Hero.BODY_H + 34;
        g2.setColor(new Color(st.color.getRed(), st.color.getGreen(), st.color.getBlue(), 120));
        g2.drawLine(popX + 14, divY, popX + popW - 14, divY);

        // ── Stats section ───────────────────────────────────────────────
        g2.setColor(new Color(200, 180, 100));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString("CH\u1EC8 S\u1ED0", popX + 18, divY + 16);

        // Draw each stat as a bar
        int[][] statDefs = {
            { st.baseHp,         300,  "M\u00e1u t\u1ed1i \u0111a" .length() },  // placeholder
        };

        boolean usesRage = st.rageMax > 0;
        int energyVal = usesRage ? st.rageMax : st.baseMana;
        String energyLabel = usesRage ? "N\u1ed9 t\u1ed1i \u0111a" : "Mana t\u1ed1i \u0111a";
        String energySuffix = usesRage ? " N\u1ed9" : " MP";
        Color energyColor = usesRage ? new Color(255, 100, 0) : new Color(0, 150, 255);

        String[] statLabels  = { "M\u00e1u t\u1ed1i \u0111a", "S\u00e1t th\u01b0\u01a1ng", "T\u1ea7m \u0111\u00e1nh", "T\u1ed1c \u0111\u00e1nh (delay)", "T\u1ed1c di chuy\u1ec3n", energyLabel };
        int[]    statValues  = { st.baseHp, st.attackDamage, st.attackRange, st.attackCooldown, st.moveCooldown, energyVal };
        String[] statSuffixes= { " HP", " DMG", " \u00f4", " frame", " frame", energySuffix };
        int[]    statMax     = { 2500, 250, 6, 80, 50, usesRage ? 360 : 150 };   // rough max for bar scaling
        Color[]  statColors  = {
            new Color(80, 220, 80),
            new Color(220, 80, 80),
            new Color(80, 150, 220),
            new Color(220, 200, 50),
            new Color(160, 100, 230),
            energyColor
        };

        int barX  = popX + 18;
        int barW  = popW - 36;
        int barH  = 18;
        int barGap = 28;
        int sy = divY + 26;

        for (int i = 0; i < statLabels.length; i++) {
            int val = statValues[i];
            int max = statMax[i];
            int fill = (int)((double) Math.min(val, max) / max * barW);

            // Label
            g2.setColor(statColors[i]);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString(statLabels[i], barX, sy);

            // Value right-aligned
            String valStr = val + statSuffixes[i];
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            FontMetrics fmv = g2.getFontMetrics();
            g2.drawString(valStr, barX + barW - fmv.stringWidth(valStr), sy);

            // Bar background
            g2.setColor(new Color(30, 40, 60));
            g2.fillRoundRect(barX, sy + 3, barW, barH, 5, 5);
            // Bar fill
            if (fill > 0) {
                g2.setColor(new Color(statColors[i].getRed(), statColors[i].getGreen(),
                                      statColors[i].getBlue(), 200));
                g2.fillRoundRect(barX, sy + 3, fill, barH, 5, 5);
            }
            // Bar border
            g2.setColor(new Color(60, 80, 110));
            g2.drawRoundRect(barX, sy + 3, barW, barH, 5, 5);

            sy += barGap;
        }

        // ── Hint ─────────────────────────────────────────────────────
        g2.setColor(new Color(100, 120, 160));
        g2.setFont(new Font("SansSerif", Font.ITALIC, 10));
        String closeHint = "Chu\u1ed9t ph\u1ea3i l\u1ea7n n\u1eefa \u0111\u1ec3 \u0111\u00f3ng";
        FontMetrics fmc = g2.getFontMetrics();
        g2.drawString(closeHint, popX + (popW - fmc.stringWidth(closeHint)) / 2, popY + popH - 10);
    }

    private void drawProfileStat(Graphics2D g2, int x, int y, int maxW,
                                  String label, String value, Color col) {
        g2.setColor(col);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.drawString(label, x, y + 11);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(value, x + maxW - fm.stringWidth(value), y + 11);
        g2.setColor(new Color(45, 58, 88));
        g2.drawLine(x, y + 13, x + maxW, y + 13);
    }
}
