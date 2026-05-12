package autobattler.core;

import autobattler.core.UnitData.UnitStats;

/**
 * UnitFactory — "Nhà máy sản xuất tướng".
 *
 * Cách dùng:
 *   Hero h = UnitFactory.create("MAGE", 2, 0);  // Pháp Sư ★★, team Player
 *   Hero e = UnitFactory.create("TANK", 1, 1);  // Kẻ Thủ ★, team Enemy
 *
 * Để thêm tướng mới, chỉ cần thêm 1 dòng vào UnitData.java.
 */
public class UnitFactory {

    private static int idCounter = 1000;

    // ─── Hệ số nhân theo cấp sao ─────────────────────────────────────────
    // HP scale: mỗi sao HP tăng 80% (nhân 1.8)
    // ATK scale: mỗi sao ATK tăng 100% (nhân 2.0) — giống King God Castle
    private static final double HP_SCALE_PER_STAR  = 1.8;
    private static final double ATK_SCALE_PER_STAR = 2.0;

    /**
     * Tạo một Hero từ heroType + starLevel + team.
     *
     * @param heroType  Loại tướng ("MAGE", "TANK", ...) — xem UnitData.java
     * @param starLevel Cấp sao (1-7)
     * @param team      0 = Player, 1 = Enemy
     * @return Hero đã được khởi tạo đầy đủ, hoặc null nếu heroType không tồn tại
     */
    public static Hero create(String heroType, int starLevel, int team) {
        UnitStats stats = UnitData.get(heroType);
        if (stats == null) {
            System.err.println("[UnitFactory] Không tìm thấy heroType: " + heroType);
            return null;
        }

        int clampedStar = Math.max(1, Math.min(starLevel, 7));

        Hero hero = new Hero(
                idCounter++,
                stats.name,
                stats.heroType,
                stats.color,
                team,
                stats.attackRange,
                stats.baseHp,
                stats.attackDamage
        );

        hero.starLevel = clampedStar;
        hero.baseMagicPower = stats.baseMagicPower;
        hero.applyStarScaling();
        
        hero.attackHits   = stats.attackHits;
        
        hero.maxMana      = stats.baseMana;
        hero.currentMana  = stats.startMana;
        hero.maxRage      = stats.rageMax;
        hero.currentRage  = 0;

        hero.attackCooldown = stats.attackCooldown;
        hero.moveCooldown   = stats.moveCooldown;
        hero.isTank         = stats.isTank;

        return hero;
    }

    /**
     * Shortcut: tạo tướng ★1 cho Player.
     */
    public static Hero createForPlayer(String heroType) {
        return create(heroType, 1, 0);
    }

    /**
     * Shortcut: tạo tướng Enemy với star ngẫu nhiên trong khoảng.
     */
    public static Hero createEnemy(String heroType, int minStar, int maxStar) {
        int star = minStar + (int)(Math.random() * (maxStar - minStar + 1));
        return create(heroType, star, 1);
    }
}
