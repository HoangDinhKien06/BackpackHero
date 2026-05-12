package manager;

import entity.BulletEffect;
import entity.Element;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementManager {
    // Túi chứa Element cơ bản
    public Map<Element, Integer> elementInventory = new HashMap<>();
    
    // Túi chứa Hiệu ứng Đạn đã dung hợp (Không giới hạn)
    public List<BulletEffect> effectInventory = new ArrayList<>();
    
    // 3 Slot trang bị hiệu ứng
    public BulletEffect[] equippedEffects = new BulletEffect[3];
    
    // Chỉ số hiệu ứng đang được dùng (0, 1 hoặc 2)
    public int activeEffectIndex = 0;
    
    // Giá mua Gói Nguyên tố (Tăng gấp đôi sau mỗi lần mua)
    public int elementPackPrice = 100;

    public ElementManager() {
        for (Element e : Element.values()) {
            elementInventory.put(e, 0);
        }
        for (int i = 0; i < 3; i++) {
            equippedEffects[i] = BulletEffect.NONE;
        }
    }

    public void addElement(Element e, int amount) {
        elementInventory.put(e, elementInventory.get(e) + amount);
    }
    
    public boolean hasElements(Element e1, Element e2) {
        if (e1 == e2) {
            return elementInventory.get(e1) >= 2;
        }
        return elementInventory.get(e1) >= 1 && elementInventory.get(e2) >= 1;
    }

    // Thực hiện dung hợp (Nếu thành công sẽ tiêu hao nguyên tố và nhét vào túi Hiệu ứng)
    public boolean fuse(Element e1, Element e2) {
        if (hasElements(e1, e2)) {
            BulletEffect result = FusionTable.fuse(e1, e2);
            if (result != BulletEffect.NONE) {
                // Tiêu hao
                elementInventory.put(e1, elementInventory.get(e1) - 1);
                elementInventory.put(e2, elementInventory.get(e2) - 1);
                
                // Thêm vào túi hiệu ứng
                if (!effectInventory.contains(result)) {
                    effectInventory.add(result);
                }
                return true;
            }
        }
        return false;
    }

    public void equipEffect(int slot, BulletEffect effect) {
        if (slot >= 0 && slot < 3) {
            equippedEffects[slot] = effect;
        }
    }
    
    public void unequipEffect(int slot) {
        if (slot >= 0 && slot < 3) {
            equippedEffects[slot] = BulletEffect.NONE;
        }
    }

    public BulletEffect getActiveEffect() {
        return equippedEffects[activeEffectIndex];
    }
}
