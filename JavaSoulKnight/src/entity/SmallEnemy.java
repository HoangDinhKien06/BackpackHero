package entity;

import math.Vector2D;
import java.awt.Color;
import java.awt.Graphics2D;

public class SmallEnemy extends Enemy {

    public SmallEnemy(double x, double y, Player player) {
        super(x, y, player, 20, 5); // 20 HP, rớt 5 Gold
        this.speed = 3.5;
        this.width = 24;
        this.height = 24;
    }

    @Override
    public void update() {
        updateStatusEffects();
        if (player == null || player.isDead() || freezeTimer > 0) return;

        Vector2D dir = Vector2D.sub(player.getPosition(), this.position);
        if (dir.mag() > 0) {
            dir.normalize();
            dir.mult(speed);
            position.add(dir);
        }
    }

    @Override
    public void draw(Graphics2D g2, double camX, double camY) {
        int drawX = (int)(position.x - camX);
        int finalDrawY = (int)(position.y - camY - z);

        drawShadowAndHealth(g2, camX, camY, drawX, finalDrawY);

        g2.setColor(Color.RED);
        g2.fillRect(drawX - width/2, finalDrawY - height/2, width, height);
        g2.setColor(Color.BLACK);
        g2.drawRect(drawX - width/2, finalDrawY - height/2, width, height);
    }
}
