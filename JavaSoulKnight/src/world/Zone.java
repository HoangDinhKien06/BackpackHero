package world;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Zone {
    public int id;
    public String name;
    public int width, height;
    public Color bgColor;
    
    public List<Terrain> terrains;
    public List<Portal> portals;
    
    // Loại quái vật được phép sinh ra (0: None, 1: Small, 2: Big, 3: Boss)
    public int[] allowedEnemyTypes;
    public long spawnInterval;

    public Zone(int id, String name, int width, int height, Color bgColor) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.height = height;
        this.bgColor = bgColor;
        this.terrains = new ArrayList<>();
        this.portals = new ArrayList<>();
    }
}
