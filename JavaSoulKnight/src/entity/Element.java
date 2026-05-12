package entity;

public enum Element {
    FIRE("Hỏa"),
    WATER("Nước"),
    ELECTRIC("Điện"),
    EARTH("Đất"),
    PLANT("Cây"),
    WIND("Gió");

    public final String name;

    Element(String name) {
        this.name = name;
    }
}
