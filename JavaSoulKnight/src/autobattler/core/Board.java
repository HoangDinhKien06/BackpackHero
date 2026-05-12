package autobattler.core;

import autobattler.input.InputHandler;
import autobattler.main.ResourceManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board {
    public int cols = 14;
    public int rows = 14;
    public int tileSize = 48;
    public int startX = 10;
    public int startY = 25;
    
    // Bench: 2 rows x 8 cols = 16 slots
    public int benchCols = 8;
    public int benchRows = 2;
    public int benchStartX = 10;
    public int benchStartY; // calculated in constructor
    
    // Grids
    public int[][] grid = new int[rows][cols];         // 0=empty, 1=obstacle
    public int[][] obstacleType = new int[rows][cols]; // 0=rock,1=bushe,2=tree,3=stump
    public int[][] obstacleVariant = new int[rows][cols]; // 0-3 (variant 1-4)
    public Hero[][] heroGrid = new Hero[rows][cols];
    public Hero[][] bench = new Hero[benchRows][benchCols];
    
    public List<Hero> heroes = new java.util.concurrent.CopyOnWriteArrayList<>();
    public Hero draggedHero = null;
    public int dragOffsetX, dragOffsetY;
    public int dragSourceRow = -1; // -1 = from board, else bench row
    public int dragSourceCol = -1;
    
    public Hero selectedHero = null;
    
    public boolean isCombatPhase = false;
    public boolean isPaused = false;
    private boolean spaceWasPressed = false;

    // Kết quả chiến đấu: 0=đang chạy, 1=thắng, -1=thua
    public int combatResult = 0;

    // Sell zone: ô bán (hiện thị khi đang kéo tướng)
    public boolean showSellZone = false;
    
    // Player placement zone: rows 7-13 (bottom half)
    public static final int PLAYER_ROW_START = 7;
    
    // Economy reference for max field unit check
    public Economy economy;
    
    /** Chiều cao 1 slot bench tính bằng pixel (= 2 ô để hiển thị hero 1×2 đầy đủ) */
    public int benchSlotH() { return tileSize * Hero.BODY_H; }

    public Board(Economy economy) {
        this.economy = economy;
        benchStartY = startY + rows * tileSize + 15;
        generateObstacles();
    }
    
    private void generateObstacles() {
        Random rand = new Random();
        // ── Vùng địch (rows 0 → PLAYER_ROW_START-1), tránh hàng đầu/cuối zone
        for (int i = 0; i < 10; i++) {
            int r = 1 + rand.nextInt(PLAYER_ROW_START - 2); // tránh hàng viền
            int c = 1 + rand.nextInt(cols - 2);             // tránh cột viền
            grid[r][c] = 1;
            obstacleType[r][c]    = rand.nextInt(4);
            obstacleVariant[r][c] = rand.nextInt(4);
        }
        // ── Vùng người chơi (rows PLAYER_ROW_START → rows-1), tránh hàng viền
        for (int i = 0; i < 6; i++) {
            int r = PLAYER_ROW_START + 1 + rand.nextInt(rows - PLAYER_ROW_START - 2);
            int c = 1 + rand.nextInt(cols - 2);
            grid[r][c] = 1;
            obstacleType[r][c]    = rand.nextInt(4);
            obstacleVariant[r][c] = rand.nextInt(4);
        }
    }

    /** Public để GamePanel gọi khi bắt đầu màn mới */
    public void generateObstaclesPublic() {
        // Xóa vật cản cũ
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r][c] = 0;
        generateObstacles();
    }

    
    public boolean addHeroToBench(Hero hero) {
        for (int r = 0; r < benchRows; r++) {
            for (int c = 0; c < benchCols; c++) {
                if (bench[r][c] == null) {
                    bench[r][c] = hero;
                    hero.isOnBench = true;
                    hero.gridX = c;
                    hero.gridY = r;
                    if (!heroes.contains(hero)) heroes.add(hero);
                    return true;
                }
            }
        }
        return false; // bench full
    }
    
    public int countPlayerOnField() {
        int count = 0;
        for (int r = PLAYER_ROW_START; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Hero h = heroGrid[r][c];
                // grid 1-cell: mỗi hero chỉ có 1 ô chân → đếm trực tiếp
                if (h != null && h.team == 0) count++;
            }
        }
        return count;
    }
    
    public void update(InputHandler input) {
        // Space to toggle combat START, or PAUSE during combat
        if (input.spacePressed && !spaceWasPressed) {
            if (!isCombatPhase) {
                isCombatPhase = true;
                isPaused = false;
                startCombat();
            } else {
                isPaused = !isPaused; // Toggle pause state
            }
        }
        spaceWasPressed = input.spacePressed;
        
        // Cập nhật VFX theo thời gian thực, dừng lại khi Pause
        if (!isPaused) {
            VFXManager.updateAll();
        }

        if (isCombatPhase) {
            if (!isPaused) {
                for (Hero h : heroes) {
                    if (!h.isOnBench) {
                        h.updateCombat(heroes, grid, cols, rows);
                    }
                }
                combatResult = checkCombatResult();
            }
            return; // Vẫn chặn kéo thả trong chế độ chiến đấu
        }
        
        // --- Placement Phase: Drag & Drop ---
        if (input.mousePressed) {
            if (draggedHero == null) {
                // Try pick up from board
                int c = (input.mouseX - startX) / tileSize;
                int r = (input.mouseY - startY) / tileSize;
                if (c >= 0 && c < cols && r >= PLAYER_ROW_START && r < rows
                    && heroGrid[r][c] != null && heroGrid[r][c].team == 0) {
                    draggedHero = heroGrid[r][c];
                    // Chỉ xóa 1 ô chân (grid 1-cell)
                    heroGrid[draggedHero.gridY][draggedHero.gridX] = null;
                    dragSourceRow = draggedHero.gridY;
                    dragSourceCol = draggedHero.gridX;
                    pickupFromBoard(draggedHero);
                    selectedHero = draggedHero;
                }
                
                // Try pick up from bench
                if (draggedHero == null) {
                    for (int br = 0; br < benchRows; br++) {
                        for (int bc = 0; bc < benchCols; bc++) {
                            if (bench[br][bc] != null && isPointInBench(input.mouseX, input.mouseY, br, bc)) {
                                draggedHero = bench[br][bc];
                                bench[br][bc] = null;
                                dragSourceRow = -(br + 1); dragSourceCol = bc; // negative = bench row index
                                pickupFromBench(draggedHero, br, bc);
                                selectedHero = draggedHero;
                                break;
                            }
                        }
                        if (draggedHero != null) break;
                    }
                }
            } else {
                draggedHero.worldX = input.mouseX - dragOffsetX;
                draggedHero.worldY = input.mouseY - dragOffsetY;
            }
        } else {
            if (draggedHero != null) {
                draggedHero.isDragging = false;
                boolean dropped = tryDrop(draggedHero, input);
                if (!dropped) returnToSource(draggedHero);
                draggedHero = null;
                dragSourceRow = -1; dragSourceCol = -1;
            }
        }
        
        VFXManager.updateAll();
    }
    
    private boolean tryDrop(Hero hero, InputHandler input) {
        // Drop target = ô chân (1-cell grid)
        int c = (hero.worldX + tileSize / 2 - startX) / tileSize;
        int r = (hero.worldY + tileSize * Hero.BODY_H - tileSize / 2 - startY) / tileSize;

        if (c >= 0 && c < cols && r >= PLAYER_ROW_START && r < rows) {
            if (heroGrid[r][c] == null && grid[r][c] == 0) {
                // Ô trống: đặt hero
                if (countPlayerOnField() < economy.getMaxFieldUnits()) {
                    heroGrid[r][c] = hero;
                    hero.isOnBench = false;
                    hero.gridX = c; hero.gridY = r;
                    return true;
                }
            } else if (heroGrid[r][c] != null && heroGrid[r][c] != hero) {
                // Merge?
                if (tryMerge(hero, heroGrid[r][c], r, c)) {
                    heroGrid[r][c] = mergedResult;
                    mergedResult.isOnBench = false;
                    mergedResult.gridX = c; mergedResult.gridY = r;
                    return true;
                }
                // Swap
                Hero existing = heroGrid[r][c];
                heroGrid[r][c] = hero;
                hero.isOnBench = false; hero.gridX = c; hero.gridY = r;
                returnToSource(existing);
                return true;
            }
        }

        // Check sell zone drop
        if (isPointInSellZone(hero.worldX + tileSize / 2, hero.worldY + tileSize / 2)) {
            // Bán tướng
            heroes.remove(hero);
            economy.sellHero(hero);
            return true;
        }
        
        // Try drop onto bench
        for (int br = 0; br < benchRows; br++) {
            for (int bc = 0; bc < benchCols; bc++) {
                if (isPointInBench(input.mouseX, input.mouseY, br, bc)) {
                    if (bench[br][bc] == null) {
                        bench[br][bc] = hero;
                        hero.isOnBench = true;
                        hero.gridX = bc; hero.gridY = br;
                        return true;
                    } else if (bench[br][bc] != hero) {
                        // Attempt Merge on bench
                        if (tryMerge(hero, bench[br][bc], br, bc)) {
                            bench[br][bc] = mergedResult; // set merged hero
                            mergedResult.isOnBench = true;
                            mergedResult.gridX = bc; mergedResult.gridY = br;
                            return true;
                        }
                        // Swap
                        Hero existing = bench[br][bc];
                        bench[br][bc] = hero;
                        hero.isOnBench = true; hero.gridX = bc; hero.gridY = br;
                        returnToSource(existing);
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private Hero mergedResult = null;
    
    /** Returns true if merge succeeded, result stored in mergedResult */
    private boolean tryMerge(Hero dragged, Hero target, int targetRow, int targetCol) {
        if (!dragged.heroType.equals(target.heroType)) return false;
        if (dragged.starLevel != target.starLevel) return false;
        if (dragged.starLevel >= 7) return false; // Max star
        
        // Merge!
        Hero merged = UnitFactory.create(dragged.heroType, dragged.starLevel + 1, dragged.team);
        merged.worldX = target.worldX;
        merged.worldY = target.worldY;
        
        // Remove old heroes from lists
        heroes.remove(dragged);
        heroes.remove(target);
        heroes.add(merged);
        
        // Clear target slot
        clearSlot(targetRow, targetCol, target);
        
        mergedResult = merged;
        return true;
    }
    
    /** Xóa 1 ô chân khỏi heroGrid (grid 1-cell). */
    private void clearSlot(int row, int col, Hero hero) {
        if (row >= 0 && row < rows && heroGrid[row][col] == hero) heroGrid[row][col] = null;
    }

    /** Xóa hero khỏi board. r = ô chân (gridY). */
    private void clearHeroFromBoard(int r, int c, Hero hero) {
        if (r >= 0 && r < rows && heroGrid[r][c] == hero) heroGrid[r][c] = null;
    }

    /** Kiểm tra điểm có nằm trong sell zone không */
    public boolean isPointInSellZone(int px, int py) {
        int szX = startX + cols * tileSize + 8;
        int szY = startY + rows * tileSize - tileSize * 2;
        int szW = 80; int szH = tileSize * 2;
        return px >= szX && px < szX + szW && py >= szY && py < szY + szH;
    }
    
    private void returnToSource(Hero hero) {
        if (dragSourceRow >= 0) {
            // Was on board. dragSourceRow = gridY = ô chân.
            int r = dragSourceRow, c = dragSourceCol;
            if (r < rows && heroGrid[r][c] == null) {
                heroGrid[r][c] = hero;
                hero.isOnBench = false;
                hero.gridX = c; hero.gridY = r;
                return;
            }
        } else if (dragSourceRow < 0) {
            // Was on bench
            int br = -(dragSourceRow + 1);
            int bc = dragSourceCol;
            if (br >= 0 && br < benchRows && bc >= 0 && bc < benchCols && bench[br][bc] == null) {
                bench[br][bc] = hero;
                hero.isOnBench = true;
                hero.gridX = bc; hero.gridY = br;
                return;
            }
        }
        // Fallback: find any free bench slot
        addHeroToBench(hero);
    }
    

    private void pickupFromBoard(Hero h) {
        h.isDragging = true;
        // Tính worldX/Y = góc trên-trái của hero (đầu)
        h.worldX = startX + h.gridX * tileSize;
        h.worldY = startY + (h.gridY - (Hero.BODY_H - 1)) * tileSize;
        // dragOffset = khoảng từ góc trên-trái đến TÂM ô CHÂN
        // → khi kéo: worldX = mouseX - offsetX, worldY = mouseY - offsetY
        // → tâm chân = worldX + tileSize/2 = mouseX ✓
        //              worldY + (BODY_H)*tileSize - tileSize/2 = mouseY ✓
        dragOffsetX = tileSize / 2;
        dragOffsetY = Hero.BODY_H * tileSize - tileSize / 2;
    }

    private void pickupFromBench(Hero h, int benchRow, int benchCol) {
        h.isDragging = true;
        h.worldX = benchStartX + benchCol * tileSize;
        h.worldY = benchStartY + benchRow * benchSlotH();
        // Chuột ở tâm ô chân (nửa dưới của bench slot)
        dragOffsetX = tileSize / 2;
        dragOffsetY = Hero.BODY_H * tileSize - tileSize / 2;
    }

    private boolean isPointInBench(int px, int py, int benchRow, int benchCol) {
        int x = benchStartX + benchCol * tileSize;
        int y = benchStartY + benchRow * benchSlotH();
        return px >= x && px < x + tileSize && py >= y && py < y + benchSlotH();
    }

    /**
     * Kiểm tra kết quả combat:
     *  1  = player thắng (địch chết hết)
     * -1  = player thua (hero player chết hết)
     *  0  = đang diễn ra
     */
    public int checkCombatResult() {
        boolean anyEnemy  = false;
        boolean anyPlayer = false;
        for (Hero h : heroes) {
            if (h.isOnBench || h.state == Hero.State.DEAD) continue;
            if (h.team == 0) anyPlayer = true;
            if (h.team != 0) anyEnemy  = true;
        }
        if (!anyEnemy)  return  1; // thắng
        if (!anyPlayer) return -1; // thua
        return 0;
    }

    /**
     * Bán hero tại vị trí (board hoặc bench), trả về true nếu bán thành công.
     * Gold được cộng từ bên ngoài qua Economy.sellHero().
     */
    public Hero removeHeroAt(int mx, int my) {
        // Kiểm tra trên board
        int c = (mx - startX) / tileSize;
        int r = (my - startY) / tileSize;
        if (c >= 0 && c < cols && r >= PLAYER_ROW_START && r < rows && heroGrid[r][c] != null && heroGrid[r][c].team == 0) {
            Hero h = heroGrid[r][c];
            // Xóa cả 2 ô thân 1x2 dọc
            clearHeroFromBoard(h.gridY, h.gridX, h);
            heroes.remove(h);
            return h;
        }
        // Kiểm tra trên bench
        for (int br = 0; br < benchRows; br++) {
            for (int bc = 0; bc < benchCols; bc++) {
                int bx = benchStartX + bc * tileSize;
                int by = benchStartY + br * benchSlotH();
                if (mx >= bx && mx < bx + tileSize && my >= by && my < by + benchSlotH() && bench[br][bc] != null) {
                    Hero h = bench[br][bc];
                    bench[br][bc] = null;
                    heroes.remove(h);
                    return h;
                }
            }
        }
        return null;
    }

    private void startCombat() {
        VFXManager.clear();
        isPaused = false;
        
        // Pass 1: Reset trạng thái và cố định vị trí khởi điểm
        for (Hero h : heroes) {
            h.currentHp = h.maxHp;
            h.state = Hero.State.IDLE;
            h.target = null;
            if (!h.isOnBench) {
                h.combatGridX = h.gridX;
                h.combatGridY = h.gridY - (Hero.BODY_H - 1);
            }
        }

        // Pass 2: Sắp xếp ưu tiên kích hoạt kỹ năng (Tướng đánh nhanh hơn đi trước - Cooldown thấp hơn)
        List<Hero> orderedHeroes = new java.util.ArrayList<>(heroes);
        orderedHeroes.sort((a, b) -> {
            int cmp = Integer.compare(a.attackCooldown, b.attackCooldown);
            if (cmp == 0) return Integer.compare(a.id, b.id); // Tie-break by ID
            return cmp;
        });

        for (Hero h : orderedHeroes) {
            if (!h.isOnBench) {
                h.checkStartOfCombat(heroes, cols, rows);
            }
        }
    }
    
    private void endCombat() {
        for (Hero h : heroes) {
            h.state = Hero.State.IDLE;
        }
    }
    
    public void draw(Graphics2D g2) {
        java.awt.geom.AffineTransform oldCamera = g2.getTransform();
        g2.translate(VFXManager.shakeX, VFXManager.shakeY);
        
        // ── Y-sort rendering: sàn → obstacle → hero (theo từng hàng) ───────────
        // Hero có chân (gridY) ở hàng r được vẽ SAU obstacle hàng r.
        // Điều này làm đầu hero (hàng r-1) tự đè lên obstacle ở hàng trên.
        for (int r = 0; r < rows; r++) {
            // ── 1. Sàn + obstacle của hàng r ────────────────────────────────
            for (int c = 0; c < cols; c++) {
                int x = startX + c * tileSize;
                int y = startY + r * tileSize;

                // Nền đất màu nâu
                g2.setColor(new Color(101, 67, 33));
                g2.fillRect(x, y, tileSize, tileSize);

                // Sàn 9-slice
                String floorKey = (r < PLAYER_ROW_START) ? "floor_enemy" : "floor_player";
                BufferedImage floorSheet = ResourceManager.get(floorKey);
                if (floorSheet != null) {
                    final int TILE = 64;
                    int zoneStartRow = (r < PLAYER_ROW_START) ? 0 : PLAYER_ROW_START;
                    int zoneEndRow   = (r < PLAYER_ROW_START) ? PLAYER_ROW_START - 1 : rows - 1;
                    boolean isTop = (r == zoneStartRow), isBot = (r == zoneEndRow);
                    boolean isLeft = (c == 0),           isRight = (c == cols - 1);
                    int srcX, srcY;
                    if      (isTop && isLeft)   { srcX = 0;   srcY = 0;   }
                    else if (isTop && isRight)  { srcX = 128; srcY = 0;   }
                    else if (isBot && isLeft)   { srcX = 0;   srcY = 128; }
                    else if (isBot && isRight)  { srcX = 128; srcY = 128; }
                    else if (isTop)             { srcX = 64;  srcY = 0;   }
                    else if (isBot)             { srcX = 64;  srcY = 128; }
                    else if (isLeft)            { srcX = 0;   srcY = 64;  }
                    else if (isRight)           { srcX = 128; srcY = 64;  }
                    else                        { srcX = 64;  srcY = 64;  }
                    try {
                        g2.drawImage(floorSheet.getSubimage(srcX, srcY, TILE, TILE),
                                     x, y, tileSize, tileSize, null);
                    } catch (Exception ignored) {}
                    if (r < PLAYER_ROW_START) {
                        g2.setColor(new Color(0, 0, 0, 80));
                        g2.fillRect(x, y, tileSize, tileSize);
                    }
                }

                // Obstacle — vẽ trước hero trong cùng hàng
                if (grid[r][c] == 1) drawObstacle(g2, x, y, obstacleType[r][c], obstacleVariant[r][c]);
            }

            // ── 2. Highlight ô đích (chỉ hàng top của target) ─────────────
            // ── 2. Highlight ô đích (khi kéo hero) ────────────────────────────
            if (!isCombatPhase && draggedHero != null && draggedHero.team == 0) {
                int hc = (draggedHero.worldX + tileSize / 2 - startX) / tileSize;
                int rFoot = (draggedHero.worldY + tileSize * Hero.BODY_H - tileSize / 2 - startY) / tileSize;
                boolean inZone = hc >= 0 && hc < cols && rFoot >= PLAYER_ROW_START && rFoot < rows;
                // Highlight ô chân (1 ô) và ô đầu (visual overhang)
                if (inZone && (r == rFoot || r == rFoot - (Hero.BODY_H - 1))) {
                    boolean canPlace = (heroGrid[rFoot][hc] == null || heroGrid[rFoot][hc] == draggedHero)
                                    && grid[rFoot][hc] == 0;
                    if (canPlace && heroGrid[rFoot][hc] == null
                            && countPlayerOnField() >= economy.getMaxFieldUnits()) canPlace = false;
                    Color fill   = canPlace ? new Color(80, 255, 120, 70)  : new Color(255, 80, 60, 70);
                    Color border = canPlace ? new Color(100, 255, 150, 200) : new Color(255, 100, 80, 200);
                    int hx = startX + hc * tileSize, hy = startY + r * tileSize;
                    g2.setColor(fill);   g2.fillRect(hx, hy, tileSize, tileSize);
                    g2.setColor(border); g2.drawRect(hx, hy, tileSize-1, tileSize-1);
                                         g2.drawRect(hx+1, hy+1, tileSize-3, tileSize-3);
                }
            }

            // ── 3. Placement phase: Y-sort theo gridY (chân) ─────────────────
            // Grid 1-cell: hero tại heroGrid[gridY][c]. Visual 2 ô: vẽ từ gridY-1 lên.
            // Y-sort: hero hàng dưới vẽ SAU → đầu nó đè lên chân hero hàng trên. ✓
            if (!isCombatPhase) {
                for (int c = 0; c < cols; c++) {
                    Hero h = heroGrid[r][c];
                    if (h != null && !h.isDragging && h.gridX == c && h.gridY == r) {
                        int x = startX + c * tileSize;
                        int y = startY + (r - (Hero.BODY_H - 1)) * tileSize;
                        h.draw(g2, x, y, tileSize);
                    }
                }
            }

            // ── 4. Combat phase: Y-sort theo hàng chân (combatGridY+BODY_H-1) ─
            if (isCombatPhase) {
                for (Hero h : heroes) {
                    if (h.isOnBench || h.state == Hero.State.DEAD) continue;
                    int footRow = h.combatGridY + (Hero.BODY_H - 1);
                    if (footRow == r) {
                        int hx = startX + h.combatGridX * tileSize;
                        int hy = startY + h.combatGridY * tileSize;
                        h.draw(g2, hx, hy, tileSize);
                    }
                }
            }

        } // end per-row loop
        
        // ── 5. Setup Phase effects: Luni Link ──────────────────────────
        if (!isCombatPhase) {
            for (Hero h : heroes) {
                if (!h.isOnBench && "LUNI".equals(h.heroType)) {
                    // Get Luni visual location
                    int lX = startX + h.gridX * tileSize + tileSize / 2;
                    int lY = startY + h.gridY * tileSize + tileSize / 2;
                    if (h.isDragging) {
                        lX = h.worldX + tileSize / 2;
                        lY = h.worldY + tileSize * Hero.BODY_H - tileSize / 2;
                    }
                    
                    // Find nearest ally on field
                    Hero nearestAlly = null;
                    int minD = Integer.MAX_VALUE;
                    int nX = 0, nY = 0;
                    
                    for (Hero other : heroes) {
                        if (other != h && !other.isOnBench && other.team == h.team && !"LUNI".equals(other.heroType)) {
                            int oX = startX + other.gridX * tileSize + tileSize / 2;
                            int oY = startY + other.gridY * tileSize + tileSize / 2;
                            if (other.isDragging) {
                                oX = other.worldX + tileSize / 2;
                                oY = other.worldY + tileSize * Hero.BODY_H - tileSize / 2;
                            }
                            int dx = oX - lX, dy = oY - lY;
                            int distSq = dx*dx + dy*dy;
                            if (distSq < minD) {
                                minD = distSq; nearestAlly = other; nX = oX; nY = oY;
                            }
                        }
                    }
                    
                    if (nearestAlly != null) {
                        // Draw Beam (glow effect)
                        java.awt.Stroke oldStroke = g2.getStroke();
                        
                        g2.setColor(new Color(255, 255, 255, 60));
                        g2.setStroke(new java.awt.BasicStroke(6f));
                        g2.drawLine(lX, lY, nX, nY);
                        
                        g2.setColor(new Color(100, 200, 255, 200));
                        g2.setStroke(new java.awt.BasicStroke(2f));
                        g2.drawLine(lX, lY, nX, nY);
                        
                        g2.setStroke(oldStroke);
                        
                        // Draw circular white glow under ally feet
                        g2.setColor(new Color(255, 255, 255, 160));
                        g2.setStroke(new java.awt.BasicStroke(3f));
                        g2.drawOval(nX - tileSize / 2 + 4, nY - 12, tileSize - 8, 16);
                        g2.setStroke(oldStroke);
                    }
                }
            }
        }

        // ── HP bars + attack lines (vẽ sau cùng để luôn nằm trên hero) ────
        if (isCombatPhase) {
            // ── Vẽ Beam Luni trong trận (khi đang CASTING) ──
            for (Hero h : heroes) {
                if ("LUNI".equals(h.heroType) && h.state == Hero.State.CASTING && h.linkedAlly != null && h.linkedAlly.state != Hero.State.DEAD) {
                    int lx = startX + h.combatGridX * tileSize + tileSize / 2;
                    int ly = startY + h.combatGridY * tileSize + tileSize;
                    int nx = startX + h.linkedAlly.combatGridX * tileSize + tileSize / 2;
                    int ny = startY + h.linkedAlly.combatGridY * tileSize + tileSize;
                    
                    java.awt.Stroke oldS = g2.getStroke();
                    g2.setColor(new Color(255, 255, 255, 70));
                    g2.setStroke(new java.awt.BasicStroke(5f));
                    g2.drawLine(lx, ly, nx, ny);
                    g2.setColor(new Color(120, 220, 255, 220));
                    g2.setStroke(new java.awt.BasicStroke(2f));
                    g2.drawLine(lx, ly, nx, ny);
                    g2.setStroke(oldS);
                }
            }
            
            for (Hero h : heroes) {
                if (h.isOnBench || h.state == Hero.State.DEAD) continue;
                int hx = startX + h.combatGridX * tileSize;
                int hy = startY + h.combatGridY * tileSize;
                int barW = tileSize;
                // HP bar (moved up slightly)
                g2.setColor(Color.RED);
                g2.fillRect(hx, hy - 8, barW, 4);
                g2.setColor(Color.GREEN);
                g2.fillRect(hx, hy - 8, (int)((double) h.currentHp / h.maxHp * barW), 4);
                g2.setColor(Color.BLACK);
                g2.drawRect(hx, hy - 8, barW, 4);
                
                // Mana or Rage bar
                if (h.maxMana > 0 || h.maxRage > 0) {
                    g2.setColor(new Color(20, 20, 20, 200));
                    g2.fillRect(hx, hy - 4, barW, 4);
                    
                    g2.setColor(new Color(0, 150, 255)); // Mana/Rage = Blue (như yêu cầu "giống thanh mana")
                    if (h.maxRage > 0) {
                        g2.fillRect(hx, hy - 4, (int)((double) h.currentRage / h.maxRage * barW), 4);
                    } else if (h.maxMana > 0) {
                        g2.fillRect(hx, hy - 4, (int)((double) h.currentMana / h.maxMana * barW), 4);
                    }
                    g2.setColor(Color.BLACK);
                    g2.drawRect(hx, hy - 4, barW, 4);
                }

            }
        }

        
        // Zone labels
        g2.setColor(new Color(255, 100, 100, 120));
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        g2.drawString("VÙNG ĐỊCH", startX + 5, startY + 14);
        g2.setColor(new Color(100, 255, 100, 120));
        g2.drawString("VÙNG CỦA BẠN", startX + 5, startY + PLAYER_ROW_START * tileSize + 14);
        
        // Draw heroes in combat phase — Y-sorted (vẽ trong vòng lặp tile bên trên)
        // → đã tích hợp vào tile loop qua drawCombatHeroesAtRow()

        // ── Sell Zone (hiển thị khi đang kéo tướng) ─────────────────────
        if (draggedHero != null) {
            int szX = startX + cols * tileSize + 8;
            int szY = startY + rows * tileSize - tileSize * 2;
            int szW = 80; int szH = tileSize * 2;
            boolean hoverSell = isPointInSellZone(
                draggedHero.worldX + tileSize / 2, draggedHero.worldY + tileSize / 2);
            // Nền sell zone
            g2.setColor(hoverSell ? new Color(220, 50, 50, 210) : new Color(180, 40, 40, 150));
            g2.fillRoundRect(szX, szY, szW, szH, 10, 10);
            g2.setColor(hoverSell ? new Color(255, 200, 200) : new Color(255, 100, 100));
            g2.drawRoundRect(szX, szY, szW, szH, 10, 10);
            // Icon & text
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            int iconW = g2.getFontMetrics().stringWidth("💰");
            g2.drawString("💰", szX + szW / 2 - iconW / 2, szY + szH / 2 - 4);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            String sellText = "BÁN";
            int stW = g2.getFontMetrics().stringWidth(sellText);
            g2.drawString(sellText, szX + szW / 2 - stW / 2, szY + szH / 2 + 14);
        }
        
        // Draw Bench
        int benchW = benchCols * tileSize;
        int benchH = benchRows * benchSlotH(); // slot cao 2*tileSize

        // Bench background
        g2.setColor(new Color(50, 40, 25));
        g2.fillRect(benchStartX - 2, benchStartY - 2, benchW + 4, benchH + 4);
        
        // Bench label
        g2.setColor(new Color(200, 180, 100));
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.drawString("▼ BĂNG GHẾ DỰ BỊ (16 ô)", benchStartX, benchStartY - 5);
        
        for (int br = 0; br < benchRows; br++) {
            for (int bc = 0; bc < benchCols; bc++) {
                int x = benchStartX + bc * tileSize;
                int y = benchStartY + br * benchSlotH(); // slot cao 2*tileSize
                int slotH = benchSlotH();
                g2.setColor(new Color(70, 55, 30));
                g2.fillRect(x, y, tileSize, slotH);
                g2.setColor(new Color(100, 80, 40));
                g2.drawRect(x, y, tileSize, slotH);

                if (bench[br][bc] != null && !bench[br][bc].isDragging) {
                    bench[br][bc].draw(g2, x, y, tileSize); // hero full 2-tile từ y
                }
            }
        }
        
        // Draw Attack and Skill Range when dragging
        if (draggedHero != null && !isCombatPhase) {
            int hc = (draggedHero.worldX + tileSize / 2 - startX) / tileSize;
            int rFoot = (draggedHero.worldY + tileSize * Hero.BODY_H - tileSize / 2 - startY) / tileSize;
            
            // Lấy config Range từ UnitData
            UnitData.UnitStats stats = UnitData.get(draggedHero.heroType);
            int aRange = draggedHero.attackRange;
            int sRange = stats != null && stats.skill1 != null ? stats.skill1.skillRange : 0;
            if (stats != null && stats.skill2 != null && stats.skill2.skillRange > sRange) sRange = stats.skill2.skillRange;

            // Chỉ vẽ khi hero đang lơ lửng trên vùng hợp lệ của Board
            if (hc >= 0 && hc < cols && rFoot >= 0 && rFoot < rows) {
                // Manhattan distance iteration for overlays
                int maxR = Math.max(aRange, sRange);
                for (int dr = -maxR; dr <= maxR; dr++) {
                    for (int dc = -maxR; dc <= maxR; dc++) {
                        int cr = rFoot + dr;
                        int cc = hc + dc;
                        if (cr < 0 || cr >= rows || cc < 0 || cc >= cols) continue;

                        int manDist = Math.abs(dr) + Math.abs(dc);
                        boolean inSkill = (manDist <= sRange);
                        boolean inAtk = (manDist <= aRange);

                        if (inSkill || inAtk) {
                            int rx = startX + cc * tileSize;
                            int ry = startY + cr * tileSize;
                            if (inAtk) {
                                g2.setColor(new Color(255, 255, 0, 50)); // Yellow transparent for Attack
                                g2.fillRect(rx, ry, tileSize, tileSize);
                            } else if (inSkill) {
                                g2.setColor(new Color(255, 255, 255, 50)); // White transparent for Skill
                                g2.fillRect(rx, ry, tileSize, tileSize);
                            }
                        }
                    }
                }
            }
        }

        // Draw dragged hero
        if (draggedHero != null) {
            draggedHero.draw(g2, draggedHero.worldX, draggedHero.worldY, tileSize);
        }
        
        // Draw selected outline (bao quanh thân 1x2)
        if (selectedHero != null && !selectedHero.isDragging) {
            int sx, sy;
            if (selectedHero.isOnBench) {
                sx = benchStartX + selectedHero.gridX * tileSize;
                sy = benchStartY + selectedHero.gridY * benchSlotH();
            } else {
                // gridY = ô dưới (chân), vẽ outline từ ô trên
                sx = startX + selectedHero.gridX * tileSize;
                sy = startY + (selectedHero.gridY - (Hero.BODY_H - 1)) * tileSize;
            }
            int outW = tileSize * Hero.BODY_W;
            int outH = tileSize * Hero.BODY_H;
            g2.setColor(Color.YELLOW);
            g2.drawRect(sx, sy, outW, outH);
            g2.drawRect(sx + 1, sy + 1, outW - 2, outH - 2);
        }
        
        // Field unit count indicator
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        int onField = countPlayerOnField();
        int maxUnits = economy.getMaxFieldUnits();
        Color countColor = onField >= maxUnits ? Color.RED : Color.GREEN;
        g2.setColor(countColor);
        g2.drawString("Sân: " + onField + "/" + maxUnits + " tướng", startX, startY - 2);
        
        // Draw all active special effects (VFX)
        VFXManager.drawAll(g2);
        
        // Combat phase status — hiển thị INLINE phía trên board (cạnh "Sân:")
        String statusText;
        Color statusColor;
        if (isCombatPhase) {
            statusText = "⚡ ĐANG CHIẾN ĐẤU  [SPACE dừng]";
            statusColor = new Color(255, 100, 100, 230);
        } else {
            statusText = "⚔ SỬA SOẠN TƯỚNG  [SPACE bắt đầu]";
            statusColor = new Color(255, 220, 80, 230);
        }
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.setColor(statusColor);
        int statusW = g2.getFontMetrics().stringWidth(statusText);
        g2.drawString(statusText, startX + cols * tileSize - statusW, startY - 2);
        
        // Khôi phục camera ban đầu
        g2.setTransform(oldCamera);
    }

    /**
     * Vẽ vật cản tại ô (x,y) bằng ảnh thích hợp.
     * type: 0=rock, 1=bushe, 2=tree, 3=stump
     * variant: 0-3 → chọn file Xxx1.png đến Xxx4.png
     */
    private void drawObstacle(Graphics2D g2, int x, int y, int type, int variant) {
        String v = String.valueOf(variant + 1); // "1" to "4"
        BufferedImage img = null;

        switch (type) {
            case 0: // Rock — ảnh đơn 64x64
                img = ResourceManager.get("obs_rock" + v);
                if (img != null) {
                    // Căn giữa trong ô
                    int rw = Math.min(tileSize, img.getWidth());
                    int rh = Math.min(tileSize, img.getHeight());
                    g2.drawImage(img, x + (tileSize - rw)/2, y + (tileSize - rh)/2, rw, rh, null);
                    return;
                }
                break;

            case 1: // Bushe — sprite sheet 1024x128 (8 frame × 128x128)
                img = ResourceManager.get("obs_bushe" + v);
                if (img != null) {
                    int frameW = img.getWidth() / 8; // 128
                    try {
                        BufferedImage frame = img.getSubimage(0, 0, frameW, img.getHeight());
                        g2.drawImage(frame, x, y, tileSize, tileSize, null);
                        return;
                    } catch (Exception ignored) {}
                }
                break;

            case 2: // Tree — sprite sheet 1536x256 (8 frame × 192x256)
                img = ResourceManager.get("obs_tree" + v);
                if (img != null) {
                    int frameW = img.getWidth() / 8; // 192
                    try {
                        BufferedImage frame = img.getSubimage(0, 0, frameW, img.getHeight());
                        // Cây cao hơn 1 ô — vẽ 2 ô chiều cao, căn đáy
                        int drawH = tileSize * 2;
                        int drawY = y - tileSize; // lên 1 ô
                        g2.drawImage(frame, x, drawY, tileSize, drawH, null);
                        return;
                    } catch (Exception ignored) {}
                }
                break;

            case 3: // Stump — ảnh 192x256 (frame đơn, cắt phần có stump)
                img = ResourceManager.get("obs_stump" + v);
                if (img != null) {
                    // Cắt phần stump chính (bỏ khoảng trắng xung quanh)
                    int sw = img.getWidth()  / 3; // ~64
                    int sh = img.getHeight() / 2; // ~128
                    try {
                        BufferedImage frame = img.getSubimage(sw/2, sh/4, sw, sh);
                        g2.drawImage(frame, x, y, tileSize, tileSize, null);
                        return;
                    } catch (Exception ignored) {}
                }
                break;
        }

        // Fallback: vẽ bằng code
        g2.setColor(new Color(80, 70, 60));
        g2.fillRect(x, y, tileSize, tileSize);
        g2.setColor(new Color(50, 40, 30));
        g2.drawRect(x, y, tileSize, tileSize);
        g2.setColor(new Color(120, 100, 80));
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        String[] icons = {"🪨", "🌿", "🌲", "🪵"};
        g2.drawString(icons[type], x + 8, y + tileSize - 8);
    }
}
