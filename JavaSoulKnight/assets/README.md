# 📁 Cấu Trúc Thư Mục Assets

```
assets/
└── images/
    ├── maps/           ← Ảnh nền bản đồ (map background)
    │     Ví dụ: forest_map.png, dungeon_map.png
    │
    ├── tiles/          ← Tileset (ô vuông đất, cỏ, đá, vật cản)
    │     Ví dụ: tileset_grass.png, tileset_dungeon.png
    │
    ├── sprites/
    │   ├── heroes/     ← Sprite Sheet của tướng player
    │   │     Ví dụ: warrior_sheet.png, mage_sheet.png
    │   │
    │   └── enemies/    ← Sprite Sheet của quái địch
    │         Ví dụ: orc_sheet.png, goblin_sheet.png
    │
    └── ui/             ← Ảnh giao diện (icon nút, khung, logo)
          Ví dụ: button_frame.png, gold_icon.png, hp_bar.png
```

---

## 📐 Quy Cách Sprite Sheet (Khuyến nghị)

| Loại          | Kích thước mỗi frame | Số cột | Ghi chú                  |
|---------------|----------------------|--------|--------------------------|
| Hero / Enemy  | 48×48 px             | 4-8    | Idle, Walk, Attack, Die  |
| Tile          | 48×48 px             | Nhiều  | Mỗi ô = 1 tile           |
| Map (nền)     | Tùy ý                | 1      | Ảnh nền toàn màn hình    |
| UI Icon       | 32×32 hoặc 64×64     | 1      | Từng icon riêng lẻ       |

---

## ✅ Lưu Ý Quan Trọng

- **Định dạng**: Dùng `.png` (hỗ trợ trong suốt / alpha).
- **Đặt tên file**: `lowercase_underscore.png` — ví dụ: `warrior_idle.png`.
- **Sprite Sheet**: Các frame phải cùng kích thước và đặt theo hàng ngang.
- Khi thêm ảnh mới, đăng ký vào `ResourceManager.java` để load 1 lần duy nhất.
