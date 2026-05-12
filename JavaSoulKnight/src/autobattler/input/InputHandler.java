package autobattler.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import autobattler.main.GamePanel;

public class InputHandler implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
    
    GamePanel gp;
    public boolean mousePressed = false;
    public boolean mouseClicked = false;      // Left click (one frame)
    public boolean mouseRightClicked = false; // Right click (one frame)
    public int mouseX = 0, mouseY = 0;
    public int mouseWheelRot = 0; // +1 down, -1 up
    
    public InputHandler(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = (int)((e.getX() - gp.offsetX) / gp.scale);
        mouseY = (int)((e.getY() - gp.offsetY) / gp.scale);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = (int)((e.getX() - gp.offsetX) / gp.scale);
        mouseY = (int)((e.getY() - gp.offsetY) / gp.scale);
    }

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mousePressed = true;
            mouseClicked = true;
            mouseX = (int)((e.getX() - gp.offsetX) / gp.scale);
            mouseY = (int)((e.getY() - gp.offsetY) / gp.scale);
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            mouseRightClicked = true;
            mouseX = (int)((e.getX() - gp.offsetX) / gp.scale);
            mouseY = (int)((e.getY() - gp.offsetY) / gp.scale);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON1) {
            mousePressed = false;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }

    public boolean spacePressed = false;

    @Override
    public void keyTyped(KeyEvent e) { }
    @Override
    public void keyPressed(KeyEvent e) { 
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            spacePressed = true;
        }
    }
    @Override
    public void keyReleased(KeyEvent e) { 
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            spacePressed = false;
        }
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        mouseWheelRot += e.getWheelRotation();
    }
}
