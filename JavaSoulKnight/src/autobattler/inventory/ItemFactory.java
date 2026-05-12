package autobattler.inventory;

import java.awt.Color;

public class ItemFactory {
    // 1. Áo Giáp: 2x2
    public static Item createArmor() {
        return new Item("Áo Giáp", new int[][]{{1,1},{1,1}}, new Color(169, 169, 169)); // Xám
    }
    // 2. Kiếm: 1x3
    public static Item createSword() {
        return new Item("Kiếm", new int[][]{{1},{1},{1}}, new Color(192, 192, 192)); // Bạc
    }
    // 3. Kiếm Hút Máu: 1x2
    public static Item createLifestealSword() {
        return new Item("Kiếm Hút Máu", new int[][]{{1},{1}}, new Color(220, 20, 60)); // Đỏ thẫm
    }
    // 4. Quần Hồi Mana: 2x3
    public static Item createManaPants() {
        return new Item("Quần Hồi Mana", new int[][]{{1,1},{1,1},{1,1}}, new Color(65, 105, 225)); // Xanh dương
    }
    // 5. Giày Tốc Độ: 2x2 (L)
    public static Item createBoots() {
        return new Item("Giày Tốc Độ", new int[][]{{1,0},{1,1}}, new Color(0, 255, 255)); // Cyan
    }
    // 6. Găng Tay: 1x1
    public static Item createGloves() {
        return new Item("Găng Tay", new int[][]{{1}}, new Color(139, 69, 19)); // Nâu
    }
    // 7. Gậy Phép: 3x1
    public static Item createMagicStaff() {
        return new Item("Gậy Phép", new int[][]{{1,1,1}}, new Color(148, 0, 211)); // Tím
    }
    // 8. Cầu Mana (Orb): 1x1
    public static Item createManaOrb() {
        return new Item("Cầu Mana", new int[][]{{1}}, new Color(0, 191, 255)); // Xanh nhạt
    }
    // 9. Đai Giáp Máu: 1x2
    public static Item createHPBelt() {
        return new Item("Đai Giáp Máu", new int[][]{{1,1}}, new Color(34, 139, 34)); // Xanh lá
    }
    // 10. Sách Phép: 2x2
    public static Item createSpellBook() {
        return new Item("Sách Phép", new int[][]{{1,1},{1,1}}, new Color(255, 215, 0)); // Vàng
    }
}
