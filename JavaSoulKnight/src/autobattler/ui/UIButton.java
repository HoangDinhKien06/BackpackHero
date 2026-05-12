package autobattler.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

public class UIButton {
    public int x, y, width, height;
    public String text;
    public Color baseColor;
    
    public boolean isHovered;
    public boolean isPressed;
    public Runnable action;
    
    public float currentScale = 1.0f;
    public float targetScale = 1.0f;

    public UIButton(int x, int y, int width, int height, String text, Color baseColor, Runnable action) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
        this.baseColor = baseColor;
        this.action = action;
        this.isHovered = false;
        this.isPressed = false;
    }

    public void update(int mouseX, int mouseY, boolean mousePressed, boolean mouseClicked) {
        int scaledWidth = (int)(width * currentScale);
        int scaledHeight = (int)(height * currentScale);
        int scaledX = x - (scaledWidth - width) / 2;
        int scaledY = y - (scaledHeight - height) / 2;
        
        Rectangle bounds = new Rectangle(scaledX, scaledY, scaledWidth, scaledHeight);
        isHovered = bounds.contains(mouseX, mouseY);
        
        if (isHovered && mousePressed) {
            isPressed = true;
        } else {
            isPressed = false;
        }

        // Target scale logic
        if (isPressed) {
            targetScale = 0.95f; // Shrink slightly when clicked
        } else if (isHovered) {
            targetScale = 1.15f; // Grow when hovered
        } else {
            targetScale = 1.0f;
        }
        
        // Lerp scale
        currentScale += (targetScale - currentScale) * 0.2f;

        if (isHovered && mouseClicked) {
            if (action != null) {
                action.run();
            }
        }
    }

    public void draw(Graphics2D g2) {
        AffineTransform oldTx = g2.getTransform();
        
        // Translate to center of button for scaling
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        g2.translate(centerX, centerY);
        g2.scale(currentScale, currentScale);
        
        // Draw Button Background
        if (isPressed) {
            g2.setColor(baseColor.darker().darker());
        } else if (isHovered) {
            g2.setColor(baseColor.brighter());
        } else {
            g2.setColor(baseColor);
        }
        
        int drawX = -width / 2;
        int drawY = -height / 2;
        
        g2.fillRoundRect(drawX, drawY, width, height, 15, 15); // Rounded corners
        
        // Draw Button Border
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(drawX, drawY, width, height, 15, 15);

        // Draw Button Text
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        
        // Center text
        FontMetrics metrics = g2.getFontMetrics();
        int textX = drawX + (width - metrics.stringWidth(text)) / 2;
        int textY = drawY + ((height - metrics.getHeight()) / 2) + metrics.getAscent();
        
        // Shadow for text
        g2.setColor(Color.BLACK);
        g2.drawString(text, textX + 2, textY + 2);
        
        g2.setColor(Color.WHITE);
        g2.drawString(text, textX, textY);
        
        // Restore transform
        g2.setTransform(oldTx);
    }
}
