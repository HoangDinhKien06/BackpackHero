package autobattler.core;

public class Economy {
    public int gold = 40;
    public int level = 1;
    public int exp = 0;
    public int round = 1; // màn hiện tại

    // ── Player HP ──────────────────────────────────────────────
    public int playerHp = 100;

    // ── Win/Lose Streak ────────────────────────────────────────
    public int winStreak  = 0;  // số lần thắng liên tiếp
    public int loseStreak = 0;  // số lần thua liên tiếp

    // EXP needed to reach next level (index = current level)
    private int[] expToNextLevel = { 0, 2, 6, 10, 20, 36, 56, Integer.MAX_VALUE };

    public static final int MAX_LEVEL = 7;
    public static final int ROLL_COST = 2;
    public static final int EXP_COST = 4;
    public static final int EXP_PER_BUY = 4;
    public static final int SELL_REWARD = 2; // bán tướng nhận 2 vàng

    /** Returns max heroes allowed on field based on player level */
    public int getMaxFieldUnits() {
        return Math.min(level + 2, 9); // Level 1 = 3 slots, Level 7 = 9 slots
    }

    public int getExpToNextLevel() {
        if (level >= MAX_LEVEL)
            return Integer.MAX_VALUE;
        return expToNextLevel[level];
    }

    public boolean canAffordRoll() {
        return gold >= ROLL_COST;
    }

    public boolean canAffordLevelUp() {
        return gold >= EXP_COST && level < MAX_LEVEL;
    }

    /** Spend 2 gold for a reroll. Returns true if successful. */
    public boolean roll() {
        if (!canAffordRoll())
            return false;
        gold -= ROLL_COST;
        return true;
    }

    /** Spend 4 gold to buy EXP. Returns true if successful. */
    public boolean buyExp() {
        if (!canAffordLevelUp())
            return false;
        gold -= EXP_COST;
        exp += EXP_PER_BUY;
        checkLevelUp();
        return true;
    }

    private void checkLevelUp() {
        while (level < MAX_LEVEL && exp >= expToNextLevel[level]) {
            exp -= expToNextLevel[level];
            level++;
        }
    }

    /** Give gold (e.g., end of round reward) */
    public void addGold(int amount) {
        gold += amount;
    }

    /** Bán tướng: nhận SELL_REWARD vàng */
    public void sellHero() {
        gold += SELL_REWARD;
    }

    /** Overload: bán tướng (tham số Hero bị bỏ qua, để tương thích Board.java) */
    public void sellHero(Hero hero) {
        gold += SELL_REWARD;
    }

    /** Chuyển sang màn tiếp theo */
    public void nextRound() {
        round++;
    }

    // ══════════════════════════════════════════════════════════
    //  Máu người chơi
    // ══════════════════════════════════════════════════════════

    /**
     * Tính lượng máu mất khi thua.
     * @param aliveEnemies số địch còn sống trên sân khi kết thúc trận
     * @return lượng máu cần trừ
     */
    public int calcHpLoss(int aliveEnemies) {
        int base;
        if (round <= 5)       base = 2;
        else if (round <= 10) base = 5;
        else if (round <= 15) base = 8;
        else                  base = 10;
        return base + aliveEnemies;
    }

    /**
     * Trừ máu người chơi sau khi thua.
     * @return true nếu game over (playerHp <= 0)
     */
    public boolean loseHp(int amount) {
        playerHp -= amount;
        if (playerHp < 0) playerHp = 0;
        return playerHp <= 0;
    }

    // ══════════════════════════════════════════════════════════
    //  Vàng cuối màn
    // ══════════════════════════════════════════════════════════

    /**
     * Tính vàng nhận cuối màn và cộng vào gold.
     * Gọi sau khi round đã tăng (để tính streak đúng).
     * @param isWin  true = thắng, false = thua
     * @return tổng vàng nhận được (để hiển thị)
     */
    public int applyEndRoundGold(boolean isWin) {
        // Cập nhật streak
        if (isWin) {
            winStreak++;
            loseStreak = 0;
        } else {
            loseStreak++;
            winStreak = 0;
        }

        int total = 0;

        // 1. Vàng qua vòng (luôn +3)
        total += 3;

        // 2. Thắng thêm +1
        if (isWin) total += 1;

        // 3. Lợi tức: cứ 10 vàng hiện có = 1 lợi tức, tối đa 5
        int interest = Math.min(gold / 10, 5);
        total += interest;

        // 4. Vàng chuỗi thắng
        if (isWin) {
            if (winStreak >= 6)         total += 2;
            else if (winStreak >= 2)    total += 1;
        }

        // 5. Vàng chuỗi thua
        if (!isWin) {
            if (loseStreak >= 6)        total += 2;
            else if (loseStreak >= 2)   total += 1;
        }

        gold += total;
        return total;
    }

    /** Kiểm tra streak hiện tại để lấy vàng thưởng (chỉ đọc, không thay đổi) */
    public int getStreakBonus() {
        if (winStreak >= 6)       return 2;
        if (winStreak >= 2)       return 1;
        if (loseStreak >= 6)      return 2;
        if (loseStreak >= 2)      return 1;
        return 0;
    }
}
