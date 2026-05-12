package core;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class InputHandler implements KeyListener, MouseListener, MouseMotionListener {

    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean spacePressed;
    public boolean key1Pressed, key2Pressed, key3Pressed;
    public boolean mousePressed;
    public int mouseX, mouseY; // Tọa độ chuột trên màn hình (dùng cho UI)
    public boolean mouseClickedUI = false;

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W) upPressed = true;
        if (code == KeyEvent.VK_S) downPressed = true;
        if (code == KeyEvent.VK_A) leftPressed = true;
        if (code == KeyEvent.VK_D) rightPressed = true;
        if (code == KeyEvent.VK_SPACE) spacePressed = true;
        
        if (code == KeyEvent.VK_1) key1Pressed = true;
        if (code == KeyEvent.VK_2) key2Pressed = true;
        if (code == KeyEvent.VK_3) key3Pressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W) upPressed = false;
        if (code == KeyEvent.VK_S) downPressed = false;
        if (code == KeyEvent.VK_A) leftPressed = false;
        if (code == KeyEvent.VK_D) rightPressed = false;
        if (code == KeyEvent.VK_SPACE) spacePressed = false;
        
        if (code == KeyEvent.VK_1) key1Pressed = false;
        if (code == KeyEvent.VK_2) key2Pressed = false;
        if (code == KeyEvent.VK_3) key3Pressed = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mousePressed = true;
            mouseClickedUI = true; // Sẽ được reset thành false sau khi các UI xử lý xong
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mousePressed = false;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }
}
