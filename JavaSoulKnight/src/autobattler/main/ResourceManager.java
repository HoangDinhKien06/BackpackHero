package autobattler.main;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * ResourceManager — "Kho vũ khí" quản lý toàn bộ ảnh của game.
 *
 * Chỉ load mỗi file ảnh 1 lần duy nhất vào bộ nhớ (cache).
 * Toàn bộ class khác gọi ResourceManager.get("tên_ảnh") để lấy ảnh.
 *
 * Cách đăng ký ảnh mới:
 *   1. Bỏ file .png vào đúng thư mục trong assets/images/
 *   2. Thêm 1 dòng load() trong khối static bên dưới.
 *   3. Dùng ResourceManager.get("tên_key") ở bất kỳ đâu.
 */
public class ResourceManager {

    // Cache: key -> BufferedImage
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    // ─── Đăng ký ảnh tại đây ─────────────────────────────────────────────
    // Thêm dòng load("key", "đường_dẫn_tương_đối") để load ảnh mới.
    static {
        // ── Arena Floors (sàn đấu) ───────────────────────────────────────
        // Sprite sheet 576x384 — mỗi tile ~96x96 px (6 cột x 4 hàng)
        load("floor_player", "arena/floors/floor_player.png");
        load("floor_enemy",  "arena/floors/floor_enemy.png");
        load("floor_grass",  "arena/floors/floor_grass.png");
        load("floor_stone",  "arena/floors/floor_stone.png");

        // ── Obstacles — Rock (64x64, dùng trực tiếp) ─────────────────────
        load("obs_rock1",  "arena/obstacles/Rock1.png");
        load("obs_rock2",  "arena/obstacles/Rock2.png");
        load("obs_rock3",  "arena/obstacles/Rock3.png");
        load("obs_rock4",  "arena/obstacles/Rock4.png");

        // ── Obstacles — Bushe (sprite sheet 1024x128, 8 frame ngang 128x128)
        load("obs_bushe1", "arena/obstacles/Bushe1.png");
        load("obs_bushe2", "arena/obstacles/Bushe2.png");
        load("obs_bushe3", "arena/obstacles/Bushe3.png");
        load("obs_bushe4", "arena/obstacles/Bushe4.png");

        // ── Obstacles — Tree (sprite sheet 1536x256, 8 frame ngang 192x256)
        load("obs_tree1",  "arena/obstacles/Tree1.png");
        load("obs_tree2",  "arena/obstacles/Tree2.png");
        load("obs_tree3",  "arena/obstacles/Tree3.png");
        load("obs_tree4",  "arena/obstacles/Tree4.png");

        // ── Obstacles — Stump (192x256, dùng frame đầu 64x128)
        load("obs_stump1", "arena/obstacles/Stump 1.png");
        load("obs_stump2", "arena/obstacles/Stump 2.png");
        load("obs_stump3", "arena/obstacles/Stump 3.png");
        load("obs_stump4", "arena/obstacles/Stump 4.png");

        // ── Heroes (uncommment khi có file) ──────────────────────────────
        // load("hero_warrior",  "sprites/heroes/warrior_sheet.png");

        // ── Enemies ───────────────────────────────────────────────────────
        // load("enemy_orc",    "sprites/enemies/orc_sheet.png");

        // ── UI ────────────────────────────────────────────────────────────
        // load("icon_gold",  "ui/gold_icon.png");
    }

    /**
     * Lấy ảnh theo key đã đăng ký.
     * @return BufferedImage, hoặc null nếu key không tồn tại / load lỗi.
     */
    public static BufferedImage get(String key) {
        if (!CACHE.containsKey(key)) {
            System.err.println("[ResourceManager] Không tìm thấy ảnh với key: " + key);
            return null;
        }
        return CACHE.get(key);
    }

    /**
     * Tải toàn bộ ảnh đã đăng ký.
     * Dùng đường dẫn File tương đối từ thư mục project (CWD khi chạy game).
     */
    private static void load(String key, String relativePath) {
        // BASE_PATH = "assets/images/" — tương đối từ project root (CWD)
        String fullPath = "assets/images/" + relativePath;
        try {
            java.io.File file = new java.io.File(fullPath);
            if (!file.exists()) {
                System.err.println("[ResourceManager] Không tìm thấy file: " + file.getAbsolutePath());
                return;
            }
            BufferedImage img = ImageIO.read(file);
            CACHE.put(key, img);
            System.out.println("[ResourceManager] Loaded: " + key + " ← " + fullPath);
        } catch (IOException e) {
            System.err.println("[ResourceManager] Lỗi load ảnh '" + key + "': " + e.getMessage());
        }
    }

    /**
     * Cắt một frame từ Sprite Sheet.
     *
     * @param sheetKey   Key của sprite sheet (đã load bởi ResourceManager)
     * @param col        Cột (0-indexed) trên sprite sheet
     * @param row        Hàng (0-indexed) trên sprite sheet
     * @param frameW     Chiều rộng mỗi frame (pixel)
     * @param frameH     Chiều cao mỗi frame (pixel)
     * @return           BufferedImage của frame đó, hoặc null nếu lỗi
     *
     * Ví dụ: lấy frame thứ 2 hàng 1 của warrior sheet (frame 48x48):
     *   BufferedImage frame = ResourceManager.getFrame("hero_warrior", 1, 0, 48, 48);
     */
    public static BufferedImage getFrame(String sheetKey, int col, int row, int frameW, int frameH) {
        BufferedImage sheet = get(sheetKey);
        if (sheet == null) return null;
        try {
            return sheet.getSubimage(col * frameW, row * frameH, frameW, frameH);
        } catch (Exception e) {
            System.err.println("[ResourceManager] Lỗi cắt frame từ '" + sheetKey + "': " + e.getMessage());
            return null;
        }
    }
}
