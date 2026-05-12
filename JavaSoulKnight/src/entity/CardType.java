package entity;

public enum CardType {
    SPREAD_3("Súng 3 Tia", "Bắn ra 3 tia đạn (Giá x4)", 4),
    DOUBLE_FAST("Súng 2 Tia Nhanh", "Bắn 2 tia đạn bay nhanh (Giá x3)", 3),
    BIG_CANNON("Súng Đại Bác", "Đạn siêu to, sát thương lớn, bắn chậm.", 1),
    EFFECT_AREA("Súng Vùng Kích", "Tạo vùng rộng gây liên tục [Hiệu ứng Đạn] cho quái đi vào.", 1),
    HIT_BLACKHOLE("Súng Hố Đen (Trúng)", "Trúng quái tạo Hố Đen (3s). CD 10s.", 1),
    KNOCKBACK("Súng Đẩy Lùi", "Đẩy văng quái ra xa.", 1),
    MULTI_SHOT_ON_HIT("Súng Phân Rã", "Trúng địch đẻ ra 3 viên đạn dí theo nó. CD 7s.", 1),
    BOUNCE_2("Đạn Nảy 2", "Đạn nảy tường 2 lần.", 1),
    HOMING_5S("Đạn Đuổi 5s", "Đạn đuổi theo địch trong 5 giây.", 1),
    PIERCE_2("Đạn Xuyên 2", "Xuyên 2 kẻ địch.", 1),
    SHORT_FAST("Đạn Đoản Mệnh", "Bay cực nhanh nhưng biến mất nhanh.", 1),
    SPAWN_BLACKHOLE("Súng Gọi Hố Đen", "Bắn thẳng ra vùng hút quái.", 1),
    SHIELD_5S("Súng Khiên Chắn", "Tạo khiên 5s chặn quái.", 1),
    DOT_EFFECT("Súng Độc Trú", "Quái bị dính [Hiệu ứng Đạn] liên tục từ giây 1 đến giây 5.", 1),
    SPEED_AREA("Súng Vùng Tăng Tốc", "Tạo vùng tăng tốc cho Player.", 1),
    LIFESTEAL_AREA("Súng Vùng Hút Máu", "Tạo vùng hút máu.", 1),
    SMALL_EFFECT_ON_HIT("Súng Trú (Nhỏ)", "Trúng quái tạo vùng NHỎ gây [Hiệu ứng Đạn] 1 lần.", 1),
    CHARGE_10("Súng Tích Tụ", "Giữ để tích tới 10 đạn bắn cùng lúc.", 1),
    INVINCIBLE("Súng Bất Tử", "Miễn sát thương 10s. CD 20s.", 1),
    BOOMERANG("Súng Boomerang", "Đạn bay đi rồi tự quay lại.", 1);

    public final String name;
    public final String description;
    public final int priceMultiplier;

    CardType(String name, String description, int priceMultiplier) {
        this.name = name;
        this.description = description;
        this.priceMultiplier = priceMultiplier;
    }
}
