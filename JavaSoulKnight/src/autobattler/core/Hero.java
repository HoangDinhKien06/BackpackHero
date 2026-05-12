package autobattler.core;

import autobattler.inventory.Backpack;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class Hero {
    public int id;
    public String name;
    public String heroType;
    public Color color;

    // Kích thước thân (tính bằng ô lưới)
    public static final int BODY_W = 1; // 1 ô ngang
    public static final int BODY_H = 2; // 2 ô cao

    public int gridX, gridY;
    public boolean isOnBench;
    
    // Combat Stats (base values)
    public int team; // 0 = Player, 1 = Enemy
    public int baseMaxHp = 100;
    public int baseDamage = 10;
    public int baseMagicPower = 0; 
    public int attackHits = 1;   // số cú trong 1 đòn (hỗ trợ "77x4")
    public int maxHp = 100;
    public int currentHp = 100;
    
    // Mana & Rage
    public int maxMana = 0;
    public int currentMana = 0;
    public int maxRage = 0;
    public int currentRage = 0;

    public int damage = 10;
    public int magicPower = 0;
    public int attackRange = 1;
    public int attackCooldown = 60;
    public int moveCooldown = 30;

    
    // Star Level (TFT-style merge)
    public int starLevel = 1; // 1-7
    
    // Skill & Buff System
    public int activeSkillIndex = 1; // 1 hoặc 2
    public int attackCount = 0;
    public boolean isTank = false;
    public Hero linkedAlly = null; // Luni specific linkage

    public enum BuffType {
        STUN, ABSOLUTE_DEFENSE, DAMAGE_AMP, MOVE_SPD_BUFF, ATK_SPD_BUFF,
        ATK_SPD_DEBUFF, MARK, NO_MANA_REGEN, INVINCIBLE, DAMAGE_REDUCE, TAUNT, LOCKED_RANGE,
        FLAT_DAMAGE_BUFF, FLAT_MAGIC_POWER_BUFF
    }

    public static class Buff {
        public BuffType type;
        public int durationFrames;
        public double value;
        public Buff(BuffType type, int durationFrames, double value) {
            this.type = type; this.durationFrames = durationFrames; this.value = value;
        }
    }

    public List<Buff> activeBuffs = new ArrayList<>();

    public boolean hasBuff(BuffType type) {
        for (Buff b : activeBuffs) if (b.type == type) return true;
        return false;
    }

    public void removeBuff(BuffType type) {
        activeBuffs.removeIf(b -> b.type == type);
    }
    
    // Helper lấy chỉ số sau khi tính các buff số thực (Flat values)
    public int getFinalDamage() {
        int val = damage;
        for (Buff b : activeBuffs) if (b.type == BuffType.FLAT_DAMAGE_BUFF) val += (int)b.value;
        return val;
    }
    
    public int getFinalMagicPower() {
        int val = magicPower;
        for (Buff b : activeBuffs) if (b.type == BuffType.FLAT_MAGIC_POWER_BUFF) val += (int)b.value;
        return val;
    }
    
    // Combat State
    public enum State { IDLE, MOVING, ATTACKING, DEAD, CASTING }
    public State state = State.IDLE;
    public int combatGridX, combatGridY;
    public Hero target = null;
    public int actionTimer = 0;
    public int maxActionTimer = 1; // phục vụ hiển thị Ratio thanh casting
    
    // Position for dragging/drawing
    public int worldX, worldY;
    public boolean isDragging = false;
    
    public Backpack backpack;
    
    // Legacy constructor (keeps backward compatibility)
    public Hero(int id, String name, Color color, int team, int attackRange) {
        this.id = id;
        this.name = name;
        this.heroType = name.toUpperCase().replace(" ", "_");
        this.color = color;
        this.team = team;
        this.attackRange = attackRange;
        this.baseMaxHp = 100;
        this.baseDamage = 10;
        applyStarScaling();
        this.backpack = new Backpack(4, 4, 700, 100, 50);
    }
    
    // New constructor with heroType
    public Hero(int id, String name, String heroType, Color color, int team, int attackRange, int baseMaxHp, int baseDamage) {
        this.id = id;
        this.name = name;
        this.heroType = heroType;
        this.color = color;
        this.team = team;
        this.attackRange = attackRange;
        this.baseMaxHp = baseMaxHp;
        this.baseDamage = baseDamage;
        applyStarScaling();
        this.backpack = new Backpack(4, 4, 700, 100, 50);
    }
    
    /**
     * Áp dụng chỉ số theo cấp sao với công thức nhân dần:
     * Cấp 2 = Cấp1 × 1.5,  Cấp 3 = Cấp2 × 1.4,  Cấp 4 = Cấp3 × 1.3
     * Cấp 5 = Cấp4 × 1.2,  Cấp 6 = Cấp5 × 1.2,  Cấp 7 = Cấp6 × 1.3
     * Kết quả làm tròn lên (Math.ceil).
     */
    public void applyStarScaling() {
        double[] multipliers = { 1.5, 1.4, 1.3, 1.2, 1.2, 1.3 }; // index 0 = lên cấp 2

        double hp   = baseMaxHp;
        double dmg  = baseDamage;
        double mpow = baseMagicPower;
        for (int i = 0; i < starLevel - 1; i++) {
            hp   = Math.ceil(hp   + hp   * multipliers[i]);
            dmg  = Math.ceil(dmg  + dmg  * multipliers[i]);
            mpow = Math.ceil(mpow + mpow * multipliers[i]);
        }
        this.maxHp      = (int) hp;
        this.currentHp  = this.maxHp;
        this.damage     = (int) dmg;
        this.magicPower = (int) mpow;
    }
    
    public void draw(Graphics2D g2, int drawX, int drawY, int size) {
        if (state == State.DEAD) return;

        int w = size * BODY_W; // 1 ô ngang
        int h = size * BODY_H; // 2 ô cao
        int pad = 4;

        // Vẽ vòng tròn trắng dưới chân nếu được Luni buff
        if (hasBuff(BuffType.ABSOLUTE_DEFENSE) || hasBuff(BuffType.MOVE_SPD_BUFF)) {
            g2.setColor(new Color(255, 255, 255, 180));
            java.awt.Stroke oldStroke = g2.getStroke();
            g2.setStroke(new java.awt.BasicStroke(3f));
            // Vị trí chân: hàng cuối cùng
            g2.drawOval(drawX + 4, drawY + h - 16, w - 8, 12);
            g2.setStroke(oldStroke);
        }

        // Bóng đổ nhẹ
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(drawX + pad + 3, drawY + pad + 3, w - pad * 2, h - pad * 2, 12, 12);

        // Hero body (hình chữ nhật bo góc)
        g2.setColor(color);
        g2.fillRoundRect(drawX + pad, drawY + pad, w - pad * 2, h - pad * 2, 12, 12);

        // Hiệu ứng sáng phía trên
        java.awt.GradientPaint gloss = new java.awt.GradientPaint(
            drawX + pad, drawY + pad, new Color(255, 255, 255, 80),
            drawX + pad, drawY + pad + (h - pad * 2) / 2, new Color(255, 255, 255, 0));
        java.awt.Paint oldPaint = g2.getPaint();
        g2.setPaint(gloss);
        g2.fillRoundRect(drawX + pad, drawY + pad, w - pad * 2, (h - pad * 2) / 2, 12, 12);
        g2.setPaint(oldPaint);

        // Viền trắng
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(drawX + pad, drawY + pad, w - pad * 2, h - pad * 2, 12, 12);

        // Hero name (giữa thân)
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        g2.setColor(Color.WHITE);
        String shortName = name.substring(0, Math.min(5, name.length()));
        int nameW = g2.getFontMetrics().stringWidth(shortName);
        g2.drawString(shortName, drawX + w / 2 - nameW / 2, drawY + h / 2);

        // ── Vẽ sao trên đầu hero ────────────────────────────────────
        // Cấp 1-3: sao đồng (số lượng = starLevel)
        // Cấp 4-6: sao bạc (số lượng = starLevel - 3)
        // Cấp 7  : 1 sao kim cương (màu xanh lam sáng)
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        int starCount;
        Color starColor;
        String starSymbol;
        if (starLevel <= 3) {
            starCount  = starLevel;                          // 1, 2, 3
            starColor  = new Color(205, 127, 50);           // đồng
            starSymbol = "★";
        } else if (starLevel <= 6) {
            starCount  = starLevel - 3;                     // 1, 2, 3
            starColor  = new Color(192, 192, 192);          // bạc
            starSymbol = "★";
        } else {                                            // cấp 7
            starCount  = 1;
            starColor  = new Color(130, 220, 255);          // kim cương
            starSymbol = "◆";
        }
        // Vẽ từng ngôi sao (hiển thị trên đầu hero)
        g2.setColor(starColor);
        int starCharW = g2.getFontMetrics().stringWidth(starSymbol);
        int totalStarW = starCount * starCharW + (starCount - 1) * 2;
        int starStartX = drawX + w / 2 - totalStarW / 2;
        int starY = drawY + pad - 1; // ngay trên đầu hero
        for (int i = 0; i < starCount; i++) {
            g2.drawString(starSymbol, starStartX + i * (starCharW + 2), starY);
        }

        // ── HIỆU ỨNG CHOÁNG (STUN VFX OVERHEAD) ──
        if (hasBuff(BuffType.STUN)) {
            java.awt.Stroke oldS = g2.getStroke();
            g2.setStroke(new java.awt.BasicStroke(2.5f));
            g2.setColor(new Color(255, 230, 0)); // Vàng tươi rực rỡ
            
            int spinAngle = (int)((System.currentTimeMillis() / 3) % 360);
            int sRadius = 20;
            int sX = drawX + w / 2 - sRadius / 2;
            int sY = drawY - 16; // Nằm trên ngôi sao một chút
            
            // Vẽ cung hở tạo hiệu ứng xoay liên tục (Loading style)
            g2.drawArc(sX, sY, sRadius, sRadius, spinAngle, 270);
            g2.setStroke(oldS);
        }

        // ── HIỂU ỨNG THANH NIỆM CHÚ (CASTING PROGRESS BAR) ──
        if (state == State.CASTING && actionTimer > 0) {
            int barW = w - 8;
            int barH = 5;
            int bx = drawX + 4;
            int by = drawY - 8; // Dưới Stun icon một chút
            
            // Background
            g2.setColor(new Color(40, 40, 40, 180));
            g2.fillRect(bx, by, barW, barH);
            
            // Fill Bar (Purple for Magic/Buff)
            g2.setColor(new Color(200, 50, 255));
            float ratio = Math.min(1.0f, (float) actionTimer / maxActionTimer);
            int fillW = (int)(barW * ratio);
            g2.fillRect(bx, by, fillW, barH);
            
            // Border
            g2.setColor(Color.WHITE);
            g2.drawRect(bx, by, barW, barH);
        }
    }
    
    public void updateBuffs() {
        if (state == State.DEAD) return;
        activeBuffs.removeIf(b -> {
            b.durationFrames--;
            return b.durationFrames <= 0;
        });
    }

    public void takeDamage(int dmg, Hero attacker) {
        if (state == State.DEAD) return;
        
        if (hasBuff(BuffType.ABSOLUTE_DEFENSE)) {
            removeBuff(BuffType.ABSOLUTE_DEFENSE);
            removeBuff(BuffType.STUN);
            removeBuff(BuffType.ATK_SPD_DEBUFF);
            dmg = 0;
        }
        
        if (hasBuff(BuffType.DAMAGE_REDUCE)) dmg = (int)(dmg * 0.7);
        if (hasBuff(BuffType.DAMAGE_AMP)) dmg = (int)(dmg * 1.3);

        if (hasBuff(BuffType.INVINCIBLE) && currentHp - dmg <= 0) {
            dmg = currentHp - 1; // Khóa máu ở 1
        }

        currentHp -= dmg;
        if (currentHp <= 0) {
            currentHp = 0;
            state = State.DEAD;
        } else if (maxMana > 0 && maxRage == 0 && !hasBuff(BuffType.NO_MANA_REGEN) && state != State.CASTING) {
            // Nhận đòn hồi 5 mana (giảm từ 10 để cân bằng), không hồi khi đang niệm chú (Luni block)
            currentMana = Math.min(maxMana, currentMana + 5);
        }
    }
    
    public void castSkill(List<Hero> heroes, int[][] gridMap) {
        if (state == State.DEAD) return;
        
        // Ghi log để kiểm chứng skill trigger (hoặc hiện VFX text sau này)
        System.out.println(name + " uses Skill " + activeSkillIndex);
        state = State.CASTING;
        actionTimer = 30; 
        maxActionTimer = 30; // Mặc định 0.66s animation cast time

        switch (heroType) {
            case "MARA":
                if (activeSkillIndex == 1) {
                    // Lộ điểm yếu: DMG 1 mục tiêu + debuff nhận thêm 30% sát thương
                    if (target != null) {
                        target.takeDamage(getFinalMagicPower(), this); // Gây đúng Lực phép cơ bản (354 scaled)
                        target.activeBuffs.add(new Buff(BuffType.DAMAGE_AMP, 600, 1.3));
                        // Purple slash for skill usage
                        int tx = 10 + target.combatGridX * 48 + 24;
                        int ty = 25 + target.combatGridY * 48 + 48;
                        VFXManager.add(new VFXManager.SlashEffect(tx, ty, 80, 80, new Color(180, 0, 255), 0, 0));
                    }
                } else {
                    // Phân Thân: Tạo clone 10% stats (chỉ tạo 1 lần để tránh spam quá đà)
                    Hero clone = new Hero(this.id + 9000, this.name + " Clone", this.color, this.team, this.attackRange);
                    clone.heroType = this.heroType;
                    clone.isTank = false;
                    clone.maxHp = Math.max(50, (int)(this.maxHp * 0.1));
                    clone.currentHp = clone.maxHp;
                    clone.damage = (int)(this.damage * 0.1);
                    clone.isOnBench = false;
                    clone.state = State.IDLE;
                    clone.combatGridX = this.combatGridX + 1;
                    clone.combatGridY = this.combatGridY;
                    heroes.add(clone);
                    System.out.println("Mara spawned a clone!");
                }
                break;

            case "LUNI":
                // Sử dụng đồng minh đã liên kết (Nếu chưa có, tìm lại khẩn cấp)
                if (linkedAlly == null || linkedAlly.state == State.DEAD) {
                    int minD = Integer.MAX_VALUE;
                    for (Hero h : heroes) {
                        if (h.team == this.team && h.state != State.DEAD && h != this && !"LUNI".equals(h.heroType)) {
                            int d = getDistance(combatGridX, combatGridY, h.combatGridX, h.combatGridY);
                            if (d < minD) { minD = d; linkedAlly = h; }
                        }
                    }
                }
                Hero finalAlly = (linkedAlly != null && linkedAlly.state != State.DEAD) ? linkedAlly : this;

                // CƠ CHẾ CỘNG CHỈ SỐ (V5): Tăng 90% Lực công và Lực phép cho đồng minh trong 5 giây
                int boostDmg = (int)(getFinalDamage() * 0.9);
                int boostMag = (int)(getFinalMagicPower() * 0.9);
                finalAlly.activeBuffs.add(new Buff(BuffType.FLAT_DAMAGE_BUFF, 225, boostDmg));
                finalAlly.activeBuffs.add(new Buff(BuffType.FLAT_MAGIC_POWER_BUFF, 225, boostMag));

                // Ràng buộc: Luni Niệm chú duy trì trong 5 giây (5 * 45fps = 225 frames)
                actionTimer = 225;
                maxActionTimer = 225;

                if (activeSkillIndex == 1) {
                    // Phước lành Thần tốc: Hồi 40% máu & +20% Tốc chạy
                    finalAlly.currentHp = Math.min(finalAlly.maxHp, finalAlly.currentHp + (int)(finalAlly.maxHp * 0.4));
                    finalAlly.activeBuffs.add(new Buff(BuffType.MOVE_SPD_BUFF, 300, 0.8)); 
                } else {
                    // Trăng hộ vệ: Nhận 2 lớp Absolute Defense
                    finalAlly.activeBuffs.add(new Buff(BuffType.ABSOLUTE_DEFENSE, 600, 1));
                    finalAlly.activeBuffs.add(new Buff(BuffType.ABSOLUTE_DEFENSE, 600, 1));
                }
                break;

            case "RAHAK":
                if (activeSkillIndex == 1) {
                    // Tọa độ Pixel
                    int startX = 10, startY = 25, tileSize = 48;
                    int myX = startX + combatGridX * tileSize + tileSize / 2;
                    int myY = startY + combatGridY * tileSize + tileSize;

                    // Săn kép: Bắn mục tiêu hiện tại và 1 đứa khác bằng Proj
                    if (target != null) {
                        int tx = startX + target.combatGridX * tileSize + tileSize / 2;
                        int ty = startY + target.combatGridY * tileSize + tileSize;
                        VFXManager.add(new VFXManager.ProjectileEffect(myX, myY, tx, ty, damage * 2, this, target));
                    }
                    Hero second = null;
                    for (Hero h : heroes) {
                        if (h.team != this.team && h.state != State.DEAD && h != target) { second = h; break; }
                    }
                    if (second != null) {
                        int sx = startX + second.combatGridX * tileSize + tileSize / 2;
                        int sy = startY + second.combatGridY * tileSize + tileSize;
                        VFXManager.add(new VFXManager.ProjectileEffect(myX, myY, sx, sy, (int)(damage * 1.5), this, second));
                    }
                } else {
                    // Móng vuốt: Gỡ slow + Tăng sức mạnh đáng kể (Damage Amp)
                    removeBuff(BuffType.ATK_SPD_DEBUFF);
                    activeBuffs.add(new Buff(BuffType.DAMAGE_AMP, 360, 2.0)); // +100% Damage
                }
                break;

            case "SARGA":
                if (activeSkillIndex == 1) {
                    // Chấm Dứt: Khóa mana toàn bộ kẻ địch 3 giây (180 frames)
                    for (Hero h : heroes) {
                        if (h.team != this.team && h.state != State.DEAD) {
                            h.activeBuffs.add(new Buff(BuffType.NO_MANA_REGEN, 180, 1));
                        }
                    }
                }
                break;

            case "SONBACH":
                if (activeSkillIndex == 1) {
                    // Bạch Cước Lịch: Giẫm sóng xung kích slow
                    dealDamageArea(heroes, (int)(damage * 1.5), combatGridX, combatGridY, 5, 5);
                    
                    // VFX Tím diện rộng (200x200 pixels ~ 4-5 ô)
                    int sx = 10 + combatGridX * 48 + 24;
                    int sy = 25 + combatGridY * 48 + 48;
                    VFXManager.add(new VFXManager.SlashEffect(sx, sy, 220, 220, new Color(160, 0, 255), 0, 0));

                    for (Hero enemy : getEnemiesInArea(heroes, combatGridX, combatGridY, 5, 5)) {
                        enemy.activeBuffs.add(new Buff(BuffType.ATK_SPD_DEBUFF, 180, 1.5));
                    }
                } else {
                    // Không lùi bước: Bất tử 4 giây (240 frames)
                    activeBuffs.add(new Buff(BuffType.INVINCIBLE, 240, 1));
                }
                break;
            
            case "AGATHE":
                if (activeSkillIndex == 2) {
                    activeBuffs.add(new Buff(BuffType.DAMAGE_REDUCE, 300, 0.5));
                } else {
                    // Skill 1: Hồi máu toàn map 9x9 (phạm vi to)
                    dealDamageArea(heroes, 0, combatGridX, combatGridY, 9, 9); // apply effects
                    for (Hero ally : heroes) {
                        if (ally.team == this.team && ally.state != State.DEAD) {
                            ally.currentHp = Math.min(ally.maxHp, ally.currentHp + (int)(maxHp * 0.5));
                        }
                    }
                }
                break;
        }
        
        // Trừ mana
        currentMana = 0;
    }
    
    public void checkStartOfCombat(List<Hero> allHeroes, int cols, int rows) {
        if ("MARA".equals(heroType)) {
            // Phóng ra sau lưng địch xa nhất
            Hero furthest = null;
            int maxD = -1;
            for (Hero h : allHeroes) {
                if (h.team != this.team && !h.isOnBench) {
                    int d = getDistance(combatGridX, combatGridY, h.combatGridX, h.combatGridY);
                    if (d > maxD) { maxD = d; furthest = h; }
                }
            }
            if (furthest != null) {
                // Đặt tọa độ sau lưng nó (nếu ô đó nằm trên map)
                int backX = furthest.combatGridX;
                int backY = furthest.combatGridY - 1; // Phía "sau" thường là hướng Y âm với enemy trên
                if (backY < 0) backY = furthest.combatGridY + 2; 
                
                if (backX >= 0 && backX < cols && backY >= 0 && backY < rows) {
                    combatGridX = backX; combatGridY = backY;
                }
            }
        }

        if ("LUNI".equals(heroType)) {
            // Thiết lập Liên Kết với đồng minh gần nhất tại thời điểm vào trận
            linkedAlly = null;
            int minD = Integer.MAX_VALUE;
            for (Hero h : allHeroes) {
                if (h != this && h.team == this.team && !h.isOnBench && !"LUNI".equals(h.heroType)) {
                    int d = getDistance(combatGridX, combatGridY, h.combatGridX, h.combatGridY);
                    if (d < minD) {
                        minD = d;
                        linkedAlly = h;
                    }
                }
            }
            
            // Kỹ năng Loại 1: Tự nạp 100% mana ngay lập tức nếu đứng TRƯỚC đồng đội
            if (activeSkillIndex == 1 && linkedAlly != null) {
                // Ở hệ trục dọc: Luni đứng TRƯỚC đồng đội <=> Y của Luni NHỎ HƠN của đồng đội!
                if (this.combatGridY < linkedAlly.combatGridY) {
                    this.currentMana = this.maxMana;
                    System.out.println("[Luni] Nạp Mana đầu trận do đứng trước " + linkedAlly.name);
                }
            }
        }

        // ── BỔ SUNG CƠ CHẾ ĐẦU TRẬN THEO KẾ HOẠCH V4 ──
        if ("SONBACH".equals(heroType) && activeSkillIndex == 2) {
            // Không lùi bước: Giảm 25% Máu tối đa
            this.maxHp = (int)(this.maxHp * 0.75);
            this.currentHp = this.maxHp;
        }

        if ("SARGA".equals(heroType) && activeSkillIndex == 2) {
            // Nghi thức Hư Vô: Hiến tế 2 đồng đội 2 bên cạnh (X-1 và X+1) để lấy Nộ
            for (Hero h : allHeroes) {
                if (h != this && h.team == this.team && !h.isOnBench && h.state != State.DEAD) {
                    if (h.gridY == this.gridY && (h.gridX == this.gridX - 1 || h.gridX == this.gridX + 1)) {
                        h.currentHp = 0;
                        h.state = State.DEAD;
                        this.currentRage = Math.min(this.maxRage, this.currentRage + 90);
                        System.out.println("Sarga sacrificed " + h.name + " for Rage!");
                    }
                }
            }
        }
    }

    public void updateCombat(List<Hero> heroes, int[][] gridMap, int cols, int rows) {
        if (state == State.DEAD) return;
        updateBuffs();
        
        if (hasBuff(BuffType.STUN)) {
            state = State.IDLE;
            return;
        }
        
        // Xử lý cái chết (tự tử / hết hp)
        if (currentHp <= 0) {
            state = State.DEAD;
            
            // Đặc thù Agathe: Hi Sinh kích hoạt hồi máu all 300%
            if ("AGATHE".equals(heroType)) {
                for (Hero ally : heroes) {
                    if (ally.team == this.team && ally.state != State.DEAD) {
                        ally.currentHp = Math.min(ally.maxHp, ally.currentHp + (int)(this.maxHp * 3.0));
                    }
                }
            }

            // Kích hoạt nội tại của Sarga: nạp 45 nộ khi có kẻ địch/đồng đội chết
            for (Hero h : heroes) {
                if (h != this && h.state != State.DEAD && "SARGA".equals(h.heroType) && h.maxRage > 0) {
                    h.currentRage = Math.min(h.maxRage, h.currentRage + 45);
                }
            }
            return;
        }

        actionTimer--;
        if (actionTimer > 0) return;
        
        if (state == State.CASTING) {
            state = State.IDLE; // Reset state after freeze timer
        }

        // --- KIỂM TRA ĐIỀU KIỆN TUNG CHIÊU (Dành cho tướng có Mana) ---
        if (maxMana > 0 && currentMana >= maxMana) {
            castSkill(heroes, gridMap);
            return;
        }

        if (target == null || target.state == State.DEAD) {
            target = findClosestEnemy(heroes);
            if (target == null) {
                state = State.IDLE;
                return;
            }
        }

        int dist = getDistanceToTarget(target); 

        // --- TÍNH TOÁN TỐC ĐÁNH THỰC TẾ (CÓ XÉT BUFF) ---
        int realCooldown = attackCooldown;
        if (hasBuff(BuffType.ATK_SPD_BUFF)) realCooldown = (int)(realCooldown * 0.6); 
        if (hasBuff(BuffType.ATK_SPD_DEBUFF)) realCooldown = (int)(realCooldown * 1.5);
        // GIẢM TỐC ĐỘ COMBAT THÊM NỮA (Tăng frame cooldown 1.5x)
        realCooldown = (int)(realCooldown * 1.5);

        if (dist <= attackRange) {
            state = State.ATTACKING;

            // Tọa độ pixel ảo (phục vụ VFX)
            int startX = 10, startY = 25, tileSize = 48;
            int myX = startX + combatGridX * tileSize + tileSize / 2;
            int myY = startY + combatGridY * tileSize + tileSize;
            int tarX = startX + target.combatGridX * tileSize + tileSize / 2;
            int tarY = startY + target.combatGridY * tileSize + tileSize;
            double angle = Math.atan2(tarY - myY, tarX - myX);
            
            // XỬ LÝ TẤN CÔNG ĐẶC BIỆT: SARGA 
            if ("SARGA".equals(heroType)) {
                attackCount++;
                if (currentRage >= maxRage && attackCount % 3 == 0) {
                    // Hư Vô Hóa: 5x3 to đùng
                    dealDamageArea(heroes, (int)(getFinalDamage() * 2.5), target.combatGridX, target.combatGridY, 5, 3);
                    currentRage = 0; // Dùng sạch nộ
                    currentHp = Math.min(maxHp, currentHp + (int)(maxHp * 0.2));
                    
                    // VFX: Vết chém tím khổng lồ với độ trễ 0.2s
                    VFXManager.add(new VFXManager.SlashEffect(tarX, tarY, 220, 140, new Color(140, 0, 220), angle, 9));
                } else {
                    // Đòn đánh lan 1x3 cơ bản của Sarga
                    dealDamageArea(heroes, getFinalDamage(), target.combatGridX, target.combatGridY, 1, 3);
                    
                    // VFX: Vết chém đỏ lan rộng
                    VFXManager.add(new VFXManager.SlashEffect(tarX, tarY, 80, 120, Color.RED, angle, 0));
                }
            } else {
                // XỬ LÝ TẤN CÔNG THƯỜNG
                if (attackRange > 1) {
                    // Tấn công xa: Bắn ra Quả cầu năng lượng bay đến mục tiêu
                    VFXManager.add(new VFXManager.ProjectileEffect(myX, myY, tarX, tarY, getFinalDamage(), this, target));
                } else {
                    // Cận chiến (Multi-hit loop cho "77x4"): Gây dmg tức thì
                    for (int hCount = 0; hCount < attackHits; hCount++) {
                        target.takeDamage(getFinalDamage(), this);
                    }
                    VFXManager.add(new VFXManager.SlashEffect(tarX, tarY, 60, 60, Color.RED, angle, 0));
                    
                    // Passive Thorns của Agathe (nếu target có buff thorns, attacker nhận dmg)
                    if (target.hasBuff(BuffType.DAMAGE_REDUCE) && "AGATHE".equals(target.heroType)) {
                         this.takeDamage((int)(target.maxHp * 0.01), target);
                    }
                }
            }
            
            // Hồi mana cơ bản khi đánh trúng
            // Hồi mana cơ bản khi đánh trúng (trừ khi bị khóa)
            if (maxMana > 0 && !hasBuff(BuffType.NO_MANA_REGEN)) {
                currentMana = Math.min(maxMana, currentMana + 15);
            }
            
            actionTimer = realCooldown;
        } else {
            state = State.MOVING;
            int bestTX = target.combatGridX, bestTY = target.combatGridY;
            int minD = Integer.MAX_VALUE;
            for (int tDy = 0; tDy < BODY_H; tDy++) {
                int d = getDistance(combatGridX, combatGridY,
                                    target.combatGridX, target.combatGridY + tDy);
                if (d < minD) { minD = d; bestTX = target.combatGridX; bestTY = target.combatGridY + tDy; }
            }
            moveTowards(bestTX, bestTY, gridMap, cols, rows, heroes);
            
            int realMoveCD = moveCooldown;
            if (hasBuff(BuffType.MOVE_SPD_BUFF)) realMoveCD = (int)(realMoveCD * 0.8); // Nhanh hơn 20%
            // GIẢM TỐC ĐỘ MOVE THÊM NỮA (1.5x)
            realMoveCD = (int)(realMoveCD * 1.5);
            actionTimer = realMoveCD;
        }
    }
    
    /**
     * BFS pathfinding: tìm bước đi đầu tiên trên đường đến (tx,ty).
     * Cho phép đi lùi và vòng tránh vật cản hoàn toàn.
     */
    private void moveTowards(int tx, int ty, int[][] gridMap, int cols, int rows, List<Hero> heroes) {
        // Nếu đã kề target thì không cần di chuyển
        if (combatGridX == tx && combatGridY == ty) return;

        // BFS từ (combatGridX, combatGridY) đến (tx, ty)
        int[][] prev = new int[rows][cols]; // encode (px*rows+py)+1, 0=chưa visit
        for (int[] row : prev) java.util.Arrays.fill(row, -1);
        prev[combatGridY][combatGridX] = combatGridY * cols + combatGridX; // bản thân

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{combatGridX, combatGridY});

        int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};
        boolean found = false;

        outer:
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1];
            for (int[] d : dirs) {
                int nx = cx + d[0], ny = cy + d[1];
                if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) continue;
                if (prev[ny][nx] != -1) continue;
                // Ô đích (tx,ty): chỉ chặn nếu có vật cản, bỏ qua hero (có thể là enemy)
                boolean passable = (nx == tx && ny == ty)
                    ? (gridMap[ny][nx] != 1)
                    : canMoveTo(nx, ny, gridMap, cols, rows, heroes);
                if (!passable) continue;
                prev[ny][nx] = cy * cols + cx;
                if (nx == tx && ny == ty) { found = true; break outer; }
                queue.add(new int[]{nx, ny});
            }
        }

        if (!found) {
            // Không tìm thấy đường: fallback greedy
            int dx = Integer.signum(tx - combatGridX);
            int dy = Integer.signum(ty - combatGridY);
            if (canMoveTo(combatGridX + dx, combatGridY, gridMap, cols, rows, heroes))
                combatGridX += dx;
            else if (canMoveTo(combatGridX, combatGridY + dy, gridMap, cols, rows, heroes))
                combatGridY += dy;
            return;
        }

        // Trace back để tìm bước đầu tiên từ vị trí hiện tại
        int stepX = tx, stepY = ty;
        while (true) {
            int enc = prev[stepY][stepX];
            int px = enc % cols, py = enc / cols;
            if (px == combatGridX && py == combatGridY) break;
            stepX = px; stepY = py;
        }
        combatGridX = stepX;
        combatGridY = stepY;
    }
    
    private boolean canMoveTo(int x, int y, int[][] gridMap, int cols, int rows, List<Hero> heroes) {
        // Kiểm tra tất cả BODY_H ô dọc (thân 1x2)
        for (int dy = 0; dy < BODY_H; dy++) {
            int cy = y + dy;
            if (x < 0 || x >= cols || cy < 0 || cy >= rows) return false;
            if (gridMap[cy][x] == 1) return false;
            for (Hero h : heroes) {
                if (h != this && h.state != State.DEAD
                    && h.combatGridX == x && h.combatGridY <= cy && cy < h.combatGridY + BODY_H)
                    return false;
            }
        }
        return true;
    }

    private Hero findClosestEnemy(List<Hero> heroes) {
        Hero closest = null;
        int minDist = Integer.MAX_VALUE;
        boolean closestIsTank = false;

        for (Hero h : heroes) {
            if (h.team != this.team && h.state != State.DEAD && !h.isOnBench) {
                int dist = getDistance(combatGridX, combatGridY, h.combatGridX, h.combatGridY);
                
                boolean better = false;
                if (dist < minDist) {
                    better = true;
                } else if (dist == minDist) {
                    // Equal distance: prefer tank role
                    if (!closestIsTank && h.isTank) better = true;
                }

                if (better) {
                    minDist = dist;
                    closest = h;
                    closestIsTank = h.isTank;
                }
            }
        }
        return closest;
    }
    
    private int getDistance(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    /**
     * Khoảng cách gần nhất giữa thân 1x2 của tôi và thân 1x2 của target.
     * Xét từ cặp (ô tôi) - (ô target) gần nhất.
     */
    private int getDistanceToTarget(Hero target) {
        int minDist = Integer.MAX_VALUE;
        for (int myDy = 0; myDy < BODY_H; myDy++) {
            for (int tDy = 0; tDy < BODY_H; tDy++) {
                int d = getDistance(combatGridX, combatGridY + myDy,
                                    target.combatGridX, target.combatGridY + tDy);
                if (d < minDist) minDist = d;
            }
        }
        return minDist;
    }

    private List<Hero> getEnemiesInArea(List<Hero> allHeroes, int cx, int cy, int w, int h) {
        List<Hero> targets = new java.util.ArrayList<>();
        int halfW = w / 2;
        int halfH = h / 2;
        int minX = cx - halfW;
        int maxX = cx + halfW;
        int minY = cy - halfH;
        int maxY = cy + halfH;

        for (Hero u : allHeroes) {
            if (u.team != this.team && u.state != State.DEAD && !u.isOnBench) {
                // check if hero body intersects area
                boolean intersects = false;
                for (int dy = 0; dy < BODY_H; dy++) {
                    int ux = u.combatGridX;
                    int uy = u.combatGridY + dy;
                    if (ux >= minX && ux <= maxX && uy >= minY && uy <= maxY) intersects = true;
                }
                if (intersects) targets.add(u);
            }
        }
        return targets;
    }
    
    private void dealDamageArea(List<Hero> allHeroes, int dmg, int cx, int cy, int w, int h) {
        for (Hero t : getEnemiesInArea(allHeroes, cx, cy, w, h)) {
            t.takeDamage(dmg, this);
        }
    }
}
