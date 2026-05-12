package autobattler.core;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * UnitData — Registry chỉ số toàn bộ tướng.
 *
 * Để thêm tướng mới: gọi register() trong khối static bên dưới.
 * Mọi màn hình UI, chiến đấu sẽ tự động cập nhật.
 *
 * ─── Ghi chú công thức ────────────────────────────────────────────────────
 *  Giáp vật lý / phép :  giảm% = 100 × giáp / (900 + giáp)
 *  Chí mạng            :  sát thương × (1.25 + critDamage/100)
 *  Hút máu             :  HP hồi   = sátThươngGâyRa × lifesteal/100
 *  Xử tử               :  kết liễu ngay khi kẻ địch < executeThreshold% HP
 *  Mana                :  +15 mỗi đòn đánh, +10 khi bị đánh; max = baseMana×2
 */
public class UnitData {

    // =========================================================================
    // Skill — kỹ năng kích hoạt bằng mana
    // =========================================================================
    public static class Skill {
        public final String name;
        public final String description;
        public final int    manaCost;    // mana cần để kích hoạt
        public final int    skillRange;  // phạm vi (ô, cùng quy tắc kim cương)
        public final boolean isPassive;  // true = nội tại (không tốn mana)

        public Skill(String name, String description, int manaCost, int skillRange) {
            this.name        = name;
            this.description = description;
            this.manaCost    = manaCost;
            this.skillRange  = skillRange;
            this.isPassive   = false;
        }

        /** Constructor cho nội tại (không kích hoạt) */
        public Skill(String name, String description) {
            this.name        = name;
            this.description = description;
            this.manaCost    = 0;
            this.skillRange  = 0;
            this.isPassive   = true;
        }
    }

    // =========================================================================
    // UnitStats — toàn bộ chỉ số cơ bản của 1 loại tướng
    // =========================================================================
    public static class UnitStats {

        // ── Thông tin cơ bản ──────────────────────────────────────────────
        public final String name;
        public final String heroType;      // key merge / registry
        public final Color  color;
        public final int    cost;          // giá mua (vàng)
        public final String description;   // mô tả ngắn 1 dòng
        public final boolean isTank;       // true → ưu tiên làm mục tiêu

        // ── Chỉ số chiến đấu cơ bản ───────────────────────────────────────
        public final int    baseHp;             // máu cơ bản
        public final int    baseMana;           // mana tối đa = baseMana*2; bắt đầu = baseMana
        public final int    baseMagicPower;     // lực phép (nhân với hệ số kỹ năng phép)
        public final int    attackDamage;       // lực công mỗi cú (= số trong "77x4")
        public final int    attackHits;         // số cú trong 1 đòn đánh (= "4" trong "77x4", mặc định 1)
        public final int    attackRange;        // phạm vi công (kim cương): 1=cận, 2=gần xa, ...
        public final int    attackCooldown;     // frame giữa các đòn (nhỏ = nhanh)
        public final int    moveCooldown;       // frame giữa mỗi bước di chuyển (nhỏ = nhanh)

        // ── Tấn công nâng cao ─────────────────────────────────────────────
        public final double damageAmp;          // khuếch đại sát thương (%, 0 = không khuếch đại)
        public final int    trueDamage;         // sát thương chuẩn (bỏ qua giáp)
        public final double critRate;           // tỉ lệ chí mạng (%, 0–100)
        public final double critDamage;         // sát thương chí mạng thêm (%, cộng vào 25%)
        public final double lifesteal;          // hút máu (%)
        public final double executeThreshold;   // tỉ lệ xử tử (%, tối đa 80)

        // ── Phòng thủ ─────────────────────────────────────────────────────
        public final int    physArmor;          // lực giáp vật lý
        public final int    magicArmor;         // lực giáp phép
        public final int    damageBlock;        // phòng thủ tuyệt đối (số lần vô hiệu hóa)
        public final double resistance;         // chống chịu (%, âm = yếu hơn, dương = kháng)
        public final double dodgeRate;          // tỉ lệ né (%)
        public final double healingAmp;         // khuếch đại hồi phục (%)
        public final int    startMana;          // mana bắt đầu trận (0 = bắt đầu rỗng)
        public final int    rageMax;            // nộ tối đa (0 = không dùng nộ)

        // ── Kỹ năng & Nội tại ────────────────────────────────────────────
        public final Skill  passive1;           // nội tại 1
        public final Skill  passive2;           // nội tại 2 (có thể null)
        public final Skill  skill1;             // kỹ năng lựa chọn A
        public final Skill  skill2;             // kỹ năng lựa chọn B (có thể null)

        // ── Constructor đầy đủ ────────────────────────────────────────────
        private UnitStats(Builder b) {
            this.name              = b.name;
            this.heroType          = b.heroType;
            this.color             = b.color;
            this.cost              = b.cost;
            this.description       = b.description;
            this.isTank            = b.isTank;
            this.baseHp            = b.baseHp;
            this.baseMana          = b.baseMana;
            this.baseMagicPower    = b.baseMagicPower;
            this.attackDamage      = b.attackDamage;
            this.attackHits        = b.attackHits;
            this.attackRange       = b.attackRange;
            this.attackCooldown    = b.attackCooldown;
            this.moveCooldown      = b.moveCooldown;
            this.damageAmp         = b.damageAmp;
            this.trueDamage        = b.trueDamage;
            this.critRate          = b.critRate;
            this.critDamage        = b.critDamage;
            this.lifesteal         = b.lifesteal;
            this.executeThreshold  = Math.min(80, b.executeThreshold);
            this.physArmor         = b.physArmor;
            this.magicArmor        = b.magicArmor;
            this.damageBlock       = b.damageBlock;
            this.resistance        = b.resistance;
            this.dodgeRate         = b.dodgeRate;
            this.healingAmp        = b.healingAmp;
            this.startMana         = b.startMana;
            this.rageMax           = b.rageMax;
            this.passive1          = b.passive1;
            this.passive2          = b.passive2;
            this.skill1            = b.skill1;
            this.skill2            = b.skill2;
        }

        /** Hiển thị lực công dạng "77x4" hoặc "12" */
        public String attackDisplay() {
            return attackHits > 1 ? attackDamage + "x" + attackHits : String.valueOf(attackDamage);
        }

        /** Tổng sát thương 1 đòn đánh (chưa tính giáp, chí mạng, ...) */
        public int totalHitDamage() {
            return attackDamage * attackHits;
        }
    }

    // =========================================================================
    // Builder — xây dựng UnitStats dễ đọc, giá trị mặc định hợp lý
    // =========================================================================
    public static class Builder {
        String name; String heroType; Color color;
        int cost = 1; String description = ""; boolean isTank = false;
        int baseHp = 100; int baseMana = 60; int baseMagicPower = 0;
        int attackDamage = 10; int attackHits = 1;
        int attackRange = 1; int attackCooldown = 50; int moveCooldown = 30;
        double damageAmp = 0; int trueDamage = 0;
        double critRate = 0; double critDamage = 0;
        double lifesteal = 0; double executeThreshold = 0;
        int physArmor = 0; int magicArmor = 0;
        int damageBlock = 0; double resistance = 0;
        double dodgeRate = 0; double healingAmp = 0;
        int startMana = 0; int rageMax = 0;
        Skill passive1 = null; Skill passive2 = null;
        Skill skill1 = null;   Skill skill2 = null;

        public Builder(String heroType, String name, Color color) {
            this.heroType = heroType; this.name = name; this.color = color;
        }
        public Builder cost(int v)              { cost = v; return this; }
        public Builder desc(String v)           { description = v; return this; }
        public Builder tank()                   { isTank = true; return this; }
        public Builder hp(int v)                { baseHp = v; return this; }
        public Builder mana(int v)              { baseMana = v; return this; }
        public Builder mp(int v)                { baseMagicPower = v; return this; }
        public Builder atk(int dmg)             { attackDamage = dmg; attackHits = 1; return this; }
        public Builder atk(int dmg, int hits)   { attackDamage = dmg; attackHits = hits; return this; }
        public Builder range(int v)             { attackRange = v; return this; }
        public Builder atkSpd(int v)            { attackCooldown = v; return this; }
        public Builder moveSpd(int v)           { moveCooldown = v; return this; }
        public Builder damageAmp(double v)         { damageAmp = v; return this; }
        public Builder damageAmp(int v)             { damageAmp = v; return this; }
        public Builder trueDmg(int v)               { trueDamage = v; return this; }
        public Builder crit(double rate, double dmgBonus) { critRate = rate; critDamage = dmgBonus; return this; }
        public Builder lifesteal(double v)      { lifesteal = v; return this; }
        public Builder execute(double v)        { executeThreshold = v; return this; }
        public Builder armor(int phys, int mag) { physArmor = phys; magicArmor = mag; return this; }
        public Builder block(int v)             { damageBlock = v; return this; }
        public Builder resist(double v)         { resistance = v; return this; }
        public Builder dodge(double v)          { dodgeRate = v; return this; }
        public Builder healingAmp(double v)         { healingAmp = v; return this; }
        public Builder healingAmp(int v)             { healingAmp = v; return this; }
        public Builder startMana(int v)              { startMana = v; return this; }
        public Builder rageMax(int v)                { rageMax = v; return this; }
        public Builder passive1(String n, String d)            { passive1 = new Skill(n, d); return this; }
        public Builder passive2(String n, String d)            { passive2 = new Skill(n, d); return this; }
        public Builder skill1(String n, String d, int mana, int r) { skill1 = new Skill(n, d, mana, r); return this; }
        public Builder skill2(String n, String d, int mana, int r) { skill2 = new Skill(n, d, mana, r); return this; }
        public UnitStats build() { return new UnitStats(this); }
    }

    // =========================================================================
    // Registry
    // =========================================================================
    private static final Map<String, UnitStats> REGISTRY = new LinkedHashMap<>();

    static {
        register(new Builder("MARA", "Mara", new Color(200, 50, 160))
            .hp(826).mana(100).mp(354).startMana(100)
            .atk(77, 4).range(1).atkSpd(50).moveSpd(18)
            .passive1("M\u1ee5c Ti\u00eau Ch\u00e1n \u0110\u1ee9ng", "M\u1ee5c ti\u00eau c\u1ee7a <\u0110\u00e1nh \u00daP Ch\u00ed M\u1ea1nh> ch\u00e1ng trong 1.5 gi\u00e2y.")
            .passive2("Ph\u00f2ng Th\u1ee7 Tuy\u1ec7t \u0110\u1ed1i", "Ph\u00f2ng th\u1ee7 tuy\u1ec7t \u0111\u1ed1i 1 l\u1ea7n sau khi d\u00f9ng <\u0110\u00e1nh \u00daP Ch\u00ed M\u1ea1nh>.")
            .skill1("L\u1ed9 \u0110i\u1ec3m Y\u1ebfu", "L\u00e0m l\u1ed9 \u0111i\u1ec3m y\u1ebfu c\u1ee7a m\u1ee5c ti\u00eau, s\u00e1t th\u01b0\u01a1ng ph\u1ea3i nh\u1eadn +30% trong 10 gi\u00e2y (Boss: gi\u1ea3m m\u1ed9t n\u1eeda).", 100, 1)
            .skill2("Ph\u00e2n Th\u00e2n \u1ea2o \u1ea2nh", "T\u1ea1o ph\u00e2n th\u00e2n c\u00f3 10% l\u1ef1c t\u1ea5n c\u00f4ng/ph\u00e9p/m\u00e1u. T\u1ed5ng m\u00e1u t\u1ed1i \u0111a Mara -20%.", 100, 1)
            .cost(5).desc("Đánh Úp Chí Mạng: Teleport ra sau lưng địch, gây sát thương phép + Stun 1.5s + 1 lần Shield.").build());

        register(new Builder("LUNI", "Luni", new Color(100, 180, 255))
            .hp(885).mana(75).mp(89)
            .atk(89).range(5).atkSpd(50).moveSpd(26)
            .passive1("Khi\u00ean Tuy\u1ec7t \u0110\u1ed1i", "Trao cho \u0111\u1ed1i t\u01b0\u1ee3ng <Ph\u01b0\u1edbc L\u00e0nh Tr\u0103ng Xanh> 1 l\u1ea7n ph\u00f2ng th\u1ee7 tuy\u1ec7t \u0111\u1ed1i.")
            .passive2("K\u00e9o D\u00e0i Buff", "Buff th\u00eam 2 gi\u00e2y sau khi h\u1ebft <Ph\u01b0\u1edbc L\u00e0nh Tr\u0103ng Xanh>.")
            .skill1("Ph\u01b0\u1edbc L\u00e0nh Th\u1ea7n T\u1ed1c", "N\u1ea1p 100% mana n\u1ebfu \u0111\u1ed1i t\u01b0\u1ee3ng \u1edf ph\u00eda sau. T\u1ed1c \u0111\u1ed9 di chuy\u1ec3n \u0111\u1ed1i t\u01b0\u1ee3ng +20%.", 75, 7)
            .skill2("Tr\u0103ng H\u1ed9 V\u1ec7", "Trao th\u00eam 2 l\u1ea7n ph\u00f2ng th\u1ee7 tuy\u1ec7t \u0111\u1ed1i cho \u0111\u1ed1i t\u01b0\u1ee3ng <Ph\u01b0\u1edbc L\u00e0nh Tr\u0103ng Xanh>.", 75, 7)
            .cost(4).desc("Phước Lành Trăng Xanh: Liên kết gần nhất. Buff Khiên và cộng % Atk/Mag của Luni vào chỉ số cơ bản của mục tiêu.").build());

        register(new Builder("RAHAK", "Rahak", new Color(220, 120, 40))
            .hp(944).mana(45).mp(177).startMana(45)
            .atk(89).range(5).atkSpd(40).moveSpd(26)
            .passive1("Th\u1ea7n T\u1ed1c Sau K\u1ef9 N\u0103ng", "T\u1ed1c \u0111\u1ed9 t\u1ea5n c\u00f4ng sau khi d\u00f9ng <L\u1ec7nh \u0110i S\u0103n> +35% (T\u1ed1i \u0111a 3 l\u1ea7n).")
            .passive2("L\u1ed9 \u0110i\u1ec3m Y\u1ebfu", "L\u00e0m l\u1ed9 \u0111i\u1ec3m y\u1ebfu m\u1ee5c ti\u00eau s\u00e1t th\u01b0\u01a1ng ph\u1ea3i nh\u1eadn +10% (T\u1ed1i \u0111a 2 l\u1ea7n. Boss: gi\u1ea3m m\u1ed9t n\u1eeda).")
            .skill1("S\u0103n K\u00e9p", "Khi k\u00edch ho\u1ea1t <L\u1ec7nh \u0110i S\u0103n>, k\u00edch ho\u1ea1t th\u00eam 1 l\u1ea7n g\u00e2y 60% s\u00e1t th\u01b0\u01a1ng cho 1 m\u1ee5c ti\u00eau kh\u00e1c.", 45, 7)
            .skill2("M\u00f3ng Vu\u1ed1t C\u1eeb Kh\u00f4i", "G\u1ee1 b\u1ecf hi\u1ec7u \u1ee9ng gi\u1ea3m t\u1ed1c. T\u1ed5ng s\u00e1t th\u01b0\u01a1ng <L\u1ec7nh \u0110i S\u0103n> +100%.", 45, 7)
            .cost(4).desc("Lệnh Đi Săn: Bắn mục tiêu xa nhất, giảm Atk Speed địch, tăng Atk Speed bản thân.").build());

        register(new Builder("SARGA", "Sarga", new Color(90, 80, 130))
            .hp(1500).mana(0).mp(101).rageMax(360)
            .atk(207).range(1).atkSpd(50).moveSpd(26)
            .passive1("H\u01b0 V\u00f4 H\u00f3a", "H\u1ed3i 100% m\u00e1u t\u1ed1i \u0111a khi [H\u01b0 V\u00f4 H\u00f3a]. M\u1ed7i l\u1ea7n c\u1edd c\u1ee7a \u0111\u1ed3ng \u0111\u1ed9i/k\u1ebb \u0111\u1ecbch n\u1ea1p 45 n\u1ed9.")
            .passive2("C\u1ea7m Mana", "Sau khi [H\u01b0 V\u00f4 H\u00f3a], \u1edf l\u1ea7n t\u1ea5n c\u00f4ng th\u01b0\u1eddng th\u1ee9 3 khi\u1ebfn mana m\u1ee5c ti\u00eau -30.")
            .skill1("Ch\u1ea5m D\u1ee9t", "Khi khai tr\u1eadn, n\u1ebfu Sarga c\u00f3 c\u1ea5p sao cao nh\u1ea5t, ng\u0103n n\u1ea1p mana k\u1ebb \u0111\u1ecbch 3 gi\u00e2y khi [H\u01b0 V\u00f4 H\u00f3a].", 0, 1)
            .skill2("Nghi Th\u1ee9c H\u01b0 V\u00f4", "Khi khai tr\u1eadn, ti\u00eau di\u1ec7t \u0111\u1ed3ng \u0111\u1ed9i 2 b\u00ean c\u00f3 c\u1ea5p * cao nh\u1ea5t. N\u1ed9 +90/105/120/135, h\u00fat m\u00e1u +25% tr\u00ean m\u1ed7i \u0111\u1ed3ng \u0111\u1ed9i b\u1ecb ti\u00eau di\u1ec7t.", 0, 1)
            .cost(5).desc("Hư Vô Hóa: Chỉ hồi nộ khi có người chết. Đầy nộ: Đòn thứ 3 quét 5x3, hồi 100% HP, hút mana địch.").build());

        register(new Builder("SONBACH", "S\u01a1n B\u1ea1ch", new Color(200, 230, 255))
            .hp(1652).mana(60).mp(118)
            .atk(118).range(1).atkSpd(55).moveSpd(26)
            .passive1("Kh\u00f3a Ph\u1ea1m Vi", "Ph\u1ea1m vi t\u1ea5n c\u00f4ng c\u1ee7a \u0111\u1ecbch k\u00edch \u0111\u1ed9ng b\u1ecb gi\u1edbi h\u1ea1n 5x5 \u00f4 trong 4 gi\u00e2y.")
            .passive2("Gi\u1ea3m S\u00e1t Th\u01b0\u01a1ng", "Khi d\u00f9ng <B\u1ea1ch H\u1ed5 C\u01b0\u1edbc>, s\u00e1t th\u01b0\u01a1ng ph\u1ea3i nh\u1eadn -30%.")
            .skill1("B\u1ea1ch C\u01b0\u1edbc L\u1ecbch", "Sau k\u1ef9 n\u0103ng: s\u00f3ng xung k\u00edch g\u00e2y s\u00e1t th\u01b0\u01a1ng ph\u00e9p v\u00e0 gi\u1ea3m 20% t\u1ed1c \u0111\u1ed9 t\u1ea5n c\u00f4ng/di chuy\u1ec3n trong 3 gi\u00e2y.", 60, 7)
            .skill2("Kh\u00f4ng L\u00f9i B\u01b0\u1edbc", "1 l\u1ea7n/tr\u1eadn: khi s\u1eafc h\u1ee7y di\u1ec7t khi \u0111ang d\u00f9ng k\u1ef9 n\u0103ng, tr\u1edf n\u00ean v\u00f4 \u0111\u1ecbch \u0111\u1ebfn cu\u1ed1i k\u1ef9 n\u0103ng. Th\u1eddi gian k\u1ef9 n\u0103ng -1 gi\u00e2y, m\u00e1u t\u1ed1i \u0111a -25%.", 60, 7)
            .cost(5).desc("Bạch Hổ Cước: Kích động địch trong 5x5, giảm sát thương nhận vào, gây sát thương nổ sau 4s.").build());

        register(new Builder("AGATHE", "Agathe", new Color(255, 205, 60))
            .hp(1900).mana(100).mp(48).startMana(0)
            .atk(101).range(2).atkSpd(50).moveSpd(26)
            .tank()
            .passive1("Gi\u1ea3m S\u00e1t Th\u01b0\u01a1ng \u0110\u1ea7u Tr\u1eadn", "Khi khai tr\u1eadn, trong 3 gi\u00e2y s\u00e1t th\u01b0\u01a1ng ph\u1ea3i nh\u1eadn -50%.")
            .passive2("N\u1ea1p Mana Khi Khai Tr\u1eadn", "N\u1ea1p 100% mana khi khai tr\u1eadn.")
            .skill1("Hi Sinh", "Khi t\u1eed tr\u1eadn, ch\u1eefa tr\u1ecb to\u00e0n b\u1ed9 \u0111\u1ed3ng \u0111\u1ed9i trong 9x9 m\u1ed9t l\u01b0\u1ee3ng b\u1eb1ng 300% m\u00e1u t\u1ed1i \u0111a c\u1ee7a Agathe.", 100, 5)
            .skill2("Tr\u1eebng Ph\u1ea1t", "Khi \u0111\u1ed3ng minh trong ph\u1ea1m vi b\u1ecb t\u1ea5n c\u00f4ng, ph\u1ea3n c\u00f4ng m\u1ee5c ti\u00eau s\u00e1t th\u01b0\u01a1ng v\u1eadt l\u00fd = 1% m\u00e1u t\u1ed1i \u0111a Agathe.", 100, 5)
            .cost(5).desc("Hào Quang Hộ Vệ: Nhận sát thương thay đồng đội trong 5x5, Buff giảm 60% DMG nhận vào cho đồng minh.").build());
    }
    // because LinkedHashMap.put() on same key just overwrites.

    /** Đăng ký tướng vào registry */
    public static void register(UnitStats stats) {
        REGISTRY.put(stats.heroType, stats);
    }

    /** Lấy chỉ số tướng theo heroType */
    public static UnitStats get(String heroType) {
        return REGISTRY.get(heroType);
    }

    /** Toàn bộ heroType đã đăng ký (theo thứ tự thêm vào) */
    public static String[] getAllTypes() {
        return REGISTRY.keySet().toArray(new String[0]);
    }

    /** Kiểm tra heroType có tồn tại không */
    public static boolean exists(String heroType) {
        return REGISTRY.containsKey(heroType);
    }

    // ── Tiện ích tính toán công thức ────────────────────────────────────────

    /** Tỉ lệ giảm sát thương từ giáp: 100 × armor / (900 + armor)  (%) */
    public static double armorReduction(int armor) {
        if (armor <= 0) return 0;
        return 100.0 * armor / (900.0 + armor);
    }

    /** Sát thương sau giáp vật lý */
    public static int applyPhysArmor(int rawDamage, int armor) {
        double reduction = armorReduction(armor) / 100.0;
        return (int) Math.round(rawDamage * (1.0 - reduction));
    }

    /** Sát thương sau giáp phép */
    public static int applyMagicArmor(int rawDamage, int armor) {
        return applyPhysArmor(rawDamage, armor); // cùng công thức
    }

    /** Sát thương chí mạng = raw × (1.25 + critDamage/100) */
    public static int applyCrit(int rawDamage, double critDamageBonus) {
        return (int) Math.round(rawDamage * (1.25 + critDamageBonus / 100.0));
    }
}
