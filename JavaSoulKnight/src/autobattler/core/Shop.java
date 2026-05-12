package autobattler.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shop — Cửa hàng 5 ô tướng.
 * Dùng UnitData để lấy danh sách và UnitFactory để tạo Hero.
 */
public class Shop {
    public Hero[] offers = new Hero[5];

    // Pool: mỗi heroType có N bản sao
    private static final int COPIES_PER_TYPE = 10;

    private List<String> pool = new ArrayList<>();

    public Shop() {
        buildPool();
        reroll();
    }

    private void buildPool() {
        pool.clear();
        for (String heroType : UnitData.getAllTypes()) {
            for (int i = 0; i < COPIES_PER_TYPE; i++) {
                pool.add(heroType);
            }
        }
        Collections.shuffle(pool);
    }

    /** Trả offers hiện tại về pool, xáo ngẫu nhiên 5 offers mới */
    public void reroll() {
        for (int i = 0; i < offers.length; i++) {
            if (offers[i] != null) {
                pool.add(offers[i].heroType);
                offers[i] = null;
            }
        }
        Collections.shuffle(pool);
        for (int i = 0; i < offers.length; i++) {
            if (!pool.isEmpty()) {
                String heroType = pool.remove(pool.size() - 1);
                offers[i] = UnitFactory.createForPlayer(heroType);
            }
        }
    }

    /**
     * Mua tướng ở slot i.
     * @return Hero nếu mua thành công, null nếu thiếu vàng hoặc slot rỗng.
     */
    public Hero buy(int slot, Economy eco) {
        if (slot < 0 || slot >= offers.length || offers[slot] == null) return null;
        int cost = getCost(slot);
        if (eco.gold < cost) return null;
        eco.gold -= cost;
        Hero h = offers[slot];
        offers[slot] = null;
        return h;
    }

    /** Lấy giá của hero ở slot i dựa theo UnitData */
    public int getCost(int slot) {
        if (offers[slot] == null) return 0;
        UnitData.UnitStats stats = UnitData.get(offers[slot].heroType);
        return stats != null ? stats.cost : 3;
    }
}
