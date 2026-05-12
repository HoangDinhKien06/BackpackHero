package entity;

public enum BulletEffect {
    NONE("Không có", ""),
    
    // FIRE
    FIRE_AREA("Đạn vùng lửa", "Tạo vùng lửa đốt cháy liên tục."),
    STEAM("Đạn hơi nước", "Gây mù (chậm) và sát thương nhẹ."),
    PLASMA("Đạn plasma", "Sát thương cực lớn, xuyên thấu."),
    MAGMA("Đạn dung nham", "Tạo vũng dung nham nổ và làm chậm."),
    ACID("Đạn axit", "Trừ máu từ từ (ăn mòn giáp)."),
    BURN("Đạn đốt máu", "Rút máu quái rất nhanh nhưng tầm ngắn."),
    
    // WATER
    WATER_AREA("Đạn vùng nước", "Tạo vùng nước làm chậm diện rộng."),
    SPREAD("Đạn lan tỏa", "Phóng điện lan giữa các kẻ địch."),
    MUD("Đạn bùn", "Bắn trúng kẻ địch sẽ bị trói chân (bất động)."),
    MOSS("Đạn rêu", "Ký sinh hút máu truyền về cho người chơi."),
    TORNADO("Đạn bão lốc", "Cuốn kẻ địch lên không trung, vô hiệu hóa."),
    
    // ELECTRIC
    SHOCK("Đạn giật điện", "Tê liệt diện rộng tức thì."),
    REBIRTH("Đạn tái sinh", "Giết quái bằng đạn này có tỉ lệ rớt máu/đạn mạnh."),
    STATIC("Đạn điện tích", "Tích tụ sét, nổ to khi đụng quái khác."),
    CELL("Đạn tế bào", "Đạn phân bào (nhân đôi) mỗi khi nảy/xuyên."),
    
    // EARTH
    METAL("Đạn kim loại", "Sát thương và tốc độ x2 nhưng nặng (rơi nhanh)."),
    SEED("Đạn hạt giống", "Cắm xuống đất mọc ra bục cản đường."),
    SAND("Đạn cát", "Bắn ra mù chùm cát, sát thương theo hình nón."),
    
    // PLANT
    PARASITE("Đạn kí sinh", "Kẻ địch chết sẽ nổ sinh ra dơi đồng minh."),
    POISON_GAS("Đạn khí độc", "Lan tỏa độc diện rộng cực lớn."),
    
    // WIND
    SOUND("Đạn âm thanh", "Xuyên thấu mọi thứ, sát thương dựa trên khoảng cách.");

    public final String name;
    public final String description;

    BulletEffect(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
