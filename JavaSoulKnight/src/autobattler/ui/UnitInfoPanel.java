package autobattler.ui;

import autobattler.core.Hero;
import autobattler.core.UnitData;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * UnitInfoPanel — Bảng thông tin tướng kiểu TFT.
 * Hiện ra khi người chơi click chuột phải vào tướng.
 * Hover vào từng chỉ số để xem giải thích chi tiết.
 */
public class UnitInfoPanel {

    // Vị trí panel (phần dưới của ShopPanel)
    private static final int PANEL_X = ShopPanel.PANEL_X + 10;
    private static final int PANEL_W = ShopPanel.PANEL_W - 20;
    private static final int PANEL_Y = 380; // Dịch lên một chút
    private static final int PANEL_H = 550; // Tăng chiều cao để chứa Kỹ Năng

    private Hero inspectedHero = null;

    // Stat rows dùng để detect hover
    private StatRow[] statRows;

    // Tooltip hiện tại
    private String tooltipTitle   = null;
    private String tooltipDesc    = null;
    private String tooltipValue   = null;
    private int    tooltipX, tooltipY;

    // Hitboxes for skill boxes
    private Rectangle skill1Box;
    private Rectangle skill2Box;

    // ─── Stat Row definition ───────────────────────────────────────────────
    private static class StatRow {
        String icon;    // Emoji icon
        String label;   // Tên ngắn
        String tooltipTitle;
        String tooltipDesc;
        Rectangle hitbox;
        int value;      // Giá trị số để hiển thị

        StatRow(String icon, String label, String tooltipTitle, String tooltipDesc) {
            this.icon         = icon;
            this.label        = label;
            this.tooltipTitle = tooltipTitle;
            this.tooltipDesc  = tooltipDesc;
        }
    }

    // ─── Danh sách chỉ số cần hiển thị ───────────────────────────────────
    private static final StatRow[] STAT_TEMPLATES = {
        new StatRow("❤",  "Máu tối đa",    "❤ Máu Tối Đa",
                "Tổng lượng máu của tướng.\nTướng chết khi máu về 0.\nTăng ×1.8 mỗi sao."),
        new StatRow("⚔",  "Sát thương",    "⚔ Sát Thương",
                "Lượng sát thương vật lý gây ra\nmỗi đòn đánh thường.\nTăng ×2.0 mỗi sao."),
        new StatRow("✨",  "Lực phép",      "✨ Lực Phép",
                "Lượng sức mạnh phép thuật\nđược dùng cho các Kỹ năng đặc biệt."),
        new StatRow("🏹", "Tầm đánh",      "🏹 Tầm Đánh",
                "Số ô (tile) mà tướng có thể\ntấn công mà không cần di chuyển.\n1 = cận chiến, >1 = tầm xa."),
        new StatRow("⚡",  "Tốc đánh",      "⚡ Tốc Độ Đánh",
                "Số frame giữa 2 lần tấn công.\nGiá trị càng nhỏ = đánh càng nhanh."),
        new StatRow("👟", "Tốc chạy",      "👟 Tốc Độ Di Chuyển",
                "Số frame giữa 2 bước di chuyển.\nGiá trị càng nhỏ = di chuyển nhanh hơn."),
        new StatRow("★",  "Cấp sao",       "★ Cấp Sao",
                "Cấp độ hiện tại của tướng (1-7)."),
    };

    /** Gọi khi người chơi right-click vào một tướng */
    public void toggle(Hero hero) {
        if (hero == null) {
            inspectedHero = null;
        } else if (inspectedHero == hero) {
            inspectedHero = null; // Click lần 2 = tắt
        } else {
            inspectedHero = hero;
        }
        buildStatRows();
    }

    /** Kiểm tra bảng đang hiển thị đúng hero này không */
    public boolean isShowing(Hero hero) {
        return inspectedHero != null && inspectedHero == hero;
    }

    /** Đóng bảng thông tin */
    public void close() {
        inspectedHero = null;
        statRows = null;
    }

    /** Cập nhật giá trị từ hero hiện tại + phát hiện hover */
    public void update(int mouseX, int mouseY) {
        tooltipTitle = null;
        tooltipDesc  = null;
        tooltipValue = null;

        if (inspectedHero == null || statRows == null) return;

        // Cập nhật giá trị mỗi frame (HP có thể thay đổi khi chiến đấu)
        refreshValues();

        for (StatRow row : statRows) {
            if (row.hitbox != null && row.hitbox.contains(mouseX, mouseY)) {
                tooltipTitle = row.tooltipTitle;
                tooltipDesc  = row.tooltipDesc;
                tooltipValue = row.label + ": " + row.value;
                tooltipX     = mouseX;
                tooltipY     = mouseY;
                break;
            }
        }
    }

    public boolean isVisible() {
        return inspectedHero != null;
    }

    public Hero getInspectedHero() {
        return inspectedHero;
    }

    // ─── Build / Refresh ──────────────────────────────────────────────────

    private void buildStatRows() {
        statRows = new StatRow[STAT_TEMPLATES.length];
        for (int i = 0; i < STAT_TEMPLATES.length; i++) {
            StatRow t = STAT_TEMPLATES[i];
            statRows[i] = new StatRow(t.icon, t.label, t.tooltipTitle, t.tooltipDesc);
        }
        refreshValues();
    }

    private void refreshValues() {
        if (inspectedHero == null || statRows == null) return;
        // Sử dụng các hàm .getFinal...() để cập nhật realtime các Buff của Luni!
        statRows[0].value = inspectedHero.maxHp;
        statRows[1].value = inspectedHero.getFinalDamage();
        statRows[2].value = inspectedHero.getFinalMagicPower();
        statRows[3].value = inspectedHero.attackRange;
        statRows[4].value = inspectedHero.attackCooldown;
        statRows[5].value = inspectedHero.moveCooldown;
        statRows[6].value = inspectedHero.starLevel;
    }

    // ─── Draw ─────────────────────────────────────────────────────────────

    public void draw(Graphics2D g2) {
        if (inspectedHero == null) return;

        // Panel background
        g2.setColor(new Color(18, 18, 28, 235));
        g2.fillRoundRect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 12, 12);
        g2.setColor(new Color(inspectedHero.color.getRed(),
                              inspectedHero.color.getGreen(),
                              inspectedHero.color.getBlue(), 180));
        g2.drawRoundRect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 12, 12);

        int y = PANEL_Y + 10;

        // ── Header: tên tướng ─────────────────────────────────────────────
        drawSectionTitle(g2, "📋 THÔNG TIN TƯỚNG", y);
        y += 22;

        // Hero icon area (colored circle + name)
        g2.setColor(inspectedHero.color);
        g2.fillOval(PANEL_X + 8, y, 38, 38);
        g2.setColor(Color.WHITE);
        g2.drawOval(PANEL_X + 8, y, 38, 38);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 15));
        g2.drawString(inspectedHero.name, PANEL_X + 54, y + 15);

        // Star level badge
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        String stars = "★".repeat(Math.min(inspectedHero.starLevel, 3));
        Color starColor = inspectedHero.starLevel >= 5 ? new Color(255,215,0) : new Color(200,200,255);
        g2.setColor(starColor);
        g2.drawString(stars + " (Sao " + inspectedHero.starLevel + ")", PANEL_X + 54, y + 33);

        // Team badge
        g2.setColor(inspectedHero.team == 0 ? new Color(80, 200, 80) : new Color(220, 80, 80));
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        g2.drawString(inspectedHero.team == 0 ? "● Đồng minh" : "● Kẻ địch", PANEL_X + 54, y + 48);
        y += 55;

        // ── HP Bar ────────────────────────────────────────────────────────
        drawDivider(g2, y);
        y += 8;

        int barW = PANEL_W - 16;
        int barH = 16;
        int hpFill = inspectedHero.maxHp > 0
                ? (int)((double) inspectedHero.currentHp / inspectedHero.maxHp * barW) : 0;

        g2.setColor(new Color(40, 40, 40));
        g2.fillRoundRect(PANEL_X + 8, y, barW, barH, 6, 6);
        g2.setColor(new Color(50, 200, 70));
        if (hpFill > 0) g2.fillRoundRect(PANEL_X + 8, y, hpFill, barH, 6, 6);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(PANEL_X + 8, y, barW, barH, 6, 6);

        g2.setFont(new Font("Arial", Font.BOLD, 11));
        String hpText = inspectedHero.currentHp + " / " + inspectedHero.maxHp + " HP";
        drawCentered(g2, hpText, PANEL_X + 8, y + 12, barW);
        y += 24;

        // ── Mana/Rage Bar ──────────────────────────────────────────────────
        if (inspectedHero.maxMana > 0 || inspectedHero.maxRage > 0) {
            boolean usesRage = inspectedHero.maxRage > 0;
            int maxEnergy = usesRage ? inspectedHero.maxRage : inspectedHero.maxMana;
            int currentEnergy = usesRage ? inspectedHero.currentRage : inspectedHero.currentMana;
            
            int energyFill = maxEnergy > 0 ? (int)((double) currentEnergy / maxEnergy * barW) : 0;
            g2.setColor(new Color(40, 40, 40));
            g2.fillRoundRect(PANEL_X + 8, y, barW, barH, 6, 6);
            
            if (usesRage) {
                g2.setColor(new Color(0, 150, 255)); // Đổi màu nộ sang xanh dương giống mana
            } else {
                g2.setColor(new Color(0, 150, 255)); // Mana = Blue
            }
            if (energyFill > 0) g2.fillRoundRect(PANEL_X + 8, y, energyFill, barH, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(PANEL_X + 8, y, barW, barH, 6, 6);
            
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            String energyText = currentEnergy + " / " + maxEnergy + (usesRage ? " N\u1ed8" : " MP");
            drawCentered(g2, energyText, PANEL_X + 8, y + 12, barW);
            y += 24;
        }

        // ── Type & description ────────────────────────────────────────────
        UnitData.UnitStats stats = UnitData.get(inspectedHero.heroType);
        if (stats != null) {
            g2.setColor(new Color(160, 160, 220));
            g2.setFont(new Font("Arial", Font.ITALIC, 11));
            drawWrapped(g2, stats.description, PANEL_X + 8, y, PANEL_W - 16);
            y += 28;
        }

        // ── Stats list ────────────────────────────────────────────────────
        drawSectionTitle(g2, "CHỈ SỐ", y);
        y += 20;
        drawDivider(g2, y);
        y += 6;

        int rowH = 28;
        for (int i = 0; i < statRows.length; i++) {
            StatRow row = statRows[i];
            int rowY = y + i * rowH;
            row.hitbox = new Rectangle(PANEL_X + 4, rowY - 4, PANEL_W - 8, rowH);

            // Highlight on hover
            if (row.hitbox.contains(lastMouseX, lastMouseY)) {
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fillRoundRect(PANEL_X + 4, rowY - 4, PANEL_W - 8, rowH - 2, 6, 6);
            }

            // Icon
            g2.setColor(getStatColor(i));
            g2.setFont(new Font("Dialog", Font.PLAIN, 14));
            g2.drawString(row.icon, PANEL_X + 10, rowY + 12);

            // Label
            g2.setColor(new Color(180, 180, 180));
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.drawString(row.label, PANEL_X + 30, rowY + 12);

            // Value (right-aligned)
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            String valStr = formatStatValue(i, row.value);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(valStr, PANEL_X + PANEL_W - fm.stringWidth(valStr) - 12, rowY + 12);
        }

        y += statRows.length * rowH + 4;

        // ── Kỹ năng (Skill Selection) ──────────────────────────────────────
        if (stats != null && (stats.skill1 != null || stats.skill2 != null)) {
            drawDivider(g2, y);
            y += 8;
            drawSectionTitle(g2, "KỸ NĂNG (Chọn 1)", y);
            y += 20;

            int boxW = (PANEL_W - 24) / 2;
            int boxH = 40;
            
            skill1Box = new Rectangle(PANEL_X + 8, y, boxW, boxH);
            skill2Box = new Rectangle(PANEL_X + 16 + boxW, y, boxW, boxH);

            String s1Name = stats.skill1 != null ? stats.skill1.name : "N/A";
            String s1Desc = stats.skill1 != null ? stats.skill1.description : "";
            String s2Name = stats.skill2 != null ? stats.skill2.name : "N/A";
            String s2Desc = stats.skill2 != null ? stats.skill2.description : "";

            drawSkillBox(g2, skill1Box, s1Name, inspectedHero.activeSkillIndex == 1, 
                         lastMouseX, lastMouseY, "Skill 1: " + s1Name, s1Desc);
            drawSkillBox(g2, skill2Box, s2Name, inspectedHero.activeSkillIndex == 2, 
                         lastMouseX, lastMouseY, "Skill 2: " + s2Name, s2Desc);
            y += boxH + 8;
        } else {
            skill1Box = null;
            skill2Box = null;
        }

        // ── Close hint ────────────────────────────────────────────────────
        drawDivider(g2, y);
        y += 8;
        g2.setColor(new Color(120, 120, 150));
        g2.setFont(new Font("Arial", Font.ITALIC, 10));
        drawCentered(g2, "Chuột phải lần nữa để đóng", PANEL_X, y, PANEL_W);

        // ── Tooltip popup ─────────────────────────────────────────────────
        if (tooltipTitle != null) {
            drawTooltip(g2);
        }
    }

    // Keep track of mouse for highlight
    private int lastMouseX, lastMouseY;

    public void setMousePos(int mx, int my) {
        lastMouseX = mx;
        lastMouseY = my;
    }

    public void handleClick(int mx, int my, boolean isCombatPhase) {
        if (inspectedHero == null || isCombatPhase) return;
        if (skill1Box != null && skill1Box.contains(mx, my)) {
            inspectedHero.activeSkillIndex = 1;
        } else if (skill2Box != null && skill2Box.contains(mx, my)) {
            inspectedHero.activeSkillIndex = 2;
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private void drawSectionTitle(Graphics2D g2, String title, int y) {
        g2.setColor(new Color(200, 180, 100));
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString(title, PANEL_X + 8, y + 13);
    }

    private void drawDivider(Graphics2D g2, int y) {
        g2.setColor(new Color(70, 70, 100));
        g2.drawLine(PANEL_X + 8, y, PANEL_X + PANEL_W - 8, y);
    }

    private void drawCentered(Graphics2D g2, String text, int x, int y, int width) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x + (width - fm.stringWidth(text)) / 2, y);
    }

    private void drawWrapped(Graphics2D g2, String text, int x, int y, int maxWidth) {
        g2.setFont(new Font("Arial", Font.ITALIC, 11));
        FontMetrics fm = g2.getFontMetrics();
        StringBuilder line = new StringBuilder();
        int lineY = y + 12;
        for (String word : text.split(" ")) {
            if (fm.stringWidth(line + word) > maxWidth) {
                g2.drawString(line.toString().trim(), x, lineY);
                line = new StringBuilder();
                lineY += 14;
            }
            line.append(word).append(" ");
        }
        if (!line.toString().trim().isEmpty()) {
            g2.drawString(line.toString().trim(), x, lineY);
        }
    }

    private void drawTooltip(Graphics2D g2) {
        int tw = 240, th = 80;
        int tx = Math.max(10, Math.min(tooltipX - tw / 2, 1200 - tw - 10));
        int ty = tooltipY - th - 10;
        if (ty < 10) ty = tooltipY + 20;

        // Background
        g2.setColor(new Color(12, 12, 20, 230));
        g2.fillRoundRect(tx, ty, tw, th, 8, 8);
        g2.setColor(new Color(200, 180, 100));
        g2.drawRoundRect(tx, ty, tw, th, 8, 8);

        // Title
        g2.setColor(new Color(255, 215, 0));
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.drawString(tooltipTitle, tx + 8, ty + 18);

        // Value
        int descY = ty + 32;
        if (tooltipValue != null && !tooltipValue.isEmpty()) {
            g2.setColor(new Color(150, 255, 150));
            g2.drawString(tooltipValue, tx + 8, descY);
            descY += 14;
        }

        // Desc
        g2.setColor(new Color(200, 200, 200));
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        String[] lines = tooltipDesc.split("\n");
        for (int i = 0; i < lines.length; i++) {
            g2.drawString(lines[i], tx + 8, descY + i * 13);
        }
    }

    private void drawSkillBox(Graphics2D g2, Rectangle box, String name, boolean isActive, 
                              int mx, int my, String tTitle, String tDesc) {
        if (name == null) name = "Chưa mở khóa";
        
        boolean hovered = box.contains(mx, my);
        if (hovered) {
            tooltipTitle = tTitle;
            tooltipDesc = tDesc;
            tooltipX = mx; tooltipY = my;
            tooltipValue = "";
        }

        if (isActive) {
            g2.setColor(new Color(50, 100, 200));
            g2.fillRoundRect(box.x, box.y, box.width, box.height, 8, 8);
            g2.setColor(new Color(100, 200, 255));
            g2.drawRoundRect(box.x, box.y, box.width, box.height, 8, 8);
        } else {
            g2.setColor(hovered ? new Color(60, 60, 80) : new Color(40, 40, 50));
            g2.fillRoundRect(box.x, box.y, box.width, box.height, 8, 8);
            g2.setColor(new Color(100, 100, 120));
            g2.drawRoundRect(box.x, box.y, box.width, box.height, 8, 8);
        }
        
        g2.setColor(isActive ? Color.WHITE : new Color(180, 180, 180));
        g2.setFont(new Font("Arial", isActive ? Font.BOLD : Font.PLAIN, 12));
        drawCentered(g2, name, box.x, box.y + box.height/2 + 4, box.width);
    }

    private Color getStatColor(int index) {
        Color[] colors = {
            new Color(80, 220, 80),    // HP - xanh lá
            new Color(220, 80, 80),    // ATK - đỏ
            new Color(80, 150, 220),   // Range - xanh
            new Color(220, 200, 50),   // ATK speed - vàng
            new Color(150, 100, 220),  // Move speed - tím
            new Color(255, 215, 0),    // Star - vàng
        };
        return index < colors.length ? colors[index] : Color.WHITE;
    }

    private String formatStatValue(int statIndex, int value) {
        switch (statIndex) {
            case 0: return value + " HP";
            case 1: return value + " DMG";
            case 2: return value + " ô";
            case 3: return value + " frame";
            case 4: return value + " frame";
            case 5: return "★".repeat(Math.min(value, 3)) + " (" + value + ")";
            default: return String.valueOf(value);
        }
    }
}
