package manager;

import entity.BulletEffect;
import entity.Element;

public class FusionTable {
    public static BulletEffect fuse(Element e1, Element e2) {
        // HỎA
        if ((e1 == Element.FIRE && e2 == Element.FIRE)) return BulletEffect.FIRE_AREA;
        if ((e1 == Element.FIRE && e2 == Element.WATER) || (e2 == Element.FIRE && e1 == Element.WATER)) return BulletEffect.STEAM;
        if ((e1 == Element.FIRE && e2 == Element.ELECTRIC) || (e2 == Element.FIRE && e1 == Element.ELECTRIC)) return BulletEffect.PLASMA;
        if ((e1 == Element.FIRE && e2 == Element.EARTH) || (e2 == Element.FIRE && e1 == Element.EARTH)) return BulletEffect.MAGMA;
        if ((e1 == Element.FIRE && e2 == Element.PLANT) || (e2 == Element.FIRE && e1 == Element.PLANT)) return BulletEffect.ACID;
        if ((e1 == Element.FIRE && e2 == Element.WIND) || (e2 == Element.FIRE && e1 == Element.WIND)) return BulletEffect.BURN;

        // NƯỚC (Loại bỏ các trường hợp đã ghép với Hỏa ở trên)
        if ((e1 == Element.WATER && e2 == Element.WATER)) return BulletEffect.WATER_AREA;
        if ((e1 == Element.WATER && e2 == Element.ELECTRIC) || (e2 == Element.WATER && e1 == Element.ELECTRIC)) return BulletEffect.SPREAD;
        if ((e1 == Element.WATER && e2 == Element.EARTH) || (e2 == Element.WATER && e1 == Element.EARTH)) return BulletEffect.MUD;
        if ((e1 == Element.WATER && e2 == Element.PLANT) || (e2 == Element.WATER && e1 == Element.PLANT)) return BulletEffect.MOSS;
        if ((e1 == Element.WATER && e2 == Element.WIND) || (e2 == Element.WATER && e1 == Element.WIND)) return BulletEffect.TORNADO;

        // ĐIỆN
        if ((e1 == Element.ELECTRIC && e2 == Element.ELECTRIC)) return BulletEffect.SHOCK;
        if ((e1 == Element.ELECTRIC && e2 == Element.EARTH) || (e2 == Element.ELECTRIC && e1 == Element.EARTH)) return BulletEffect.REBIRTH;
        if ((e1 == Element.ELECTRIC && e2 == Element.WIND) || (e2 == Element.ELECTRIC && e1 == Element.WIND)) return BulletEffect.STATIC;
        if ((e1 == Element.ELECTRIC && e2 == Element.PLANT) || (e2 == Element.ELECTRIC && e1 == Element.PLANT)) return BulletEffect.CELL;

        // ĐẤT
        if ((e1 == Element.EARTH && e2 == Element.EARTH)) return BulletEffect.METAL;
        if ((e1 == Element.EARTH && e2 == Element.PLANT) || (e2 == Element.EARTH && e1 == Element.PLANT)) return BulletEffect.SEED;
        if ((e1 == Element.EARTH && e2 == Element.WIND) || (e2 == Element.EARTH && e1 == Element.WIND)) return BulletEffect.SAND;

        // CÂY
        if ((e1 == Element.PLANT && e2 == Element.PLANT)) return BulletEffect.PARASITE;
        if ((e1 == Element.PLANT && e2 == Element.WIND) || (e2 == Element.PLANT && e1 == Element.WIND)) return BulletEffect.POISON_GAS;

        // GIÓ
        if ((e1 == Element.WIND && e2 == Element.WIND)) return BulletEffect.SOUND;

        return BulletEffect.NONE;
    }
}
