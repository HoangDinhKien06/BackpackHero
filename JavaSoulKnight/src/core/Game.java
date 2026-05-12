package core;

import javax.swing.JFrame;

public class Game {
    public static void main(String[] args) {
        // Chặn Java auto-scaling theo DPI/Scale của Windows (giúp khung hình đúng kích thước).
        System.setProperty("sun.java2d.uiScale", "1.0");

        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setTitle("Java 2D Soul Knight OOP");

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);

        window.pack(); // Đóng gói theo kích thước của GamePanel

        // Đảm bảo client area khớp preferred size của GamePanel.
        java.awt.Insets insets = window.getInsets();
        java.awt.Dimension pref = gamePanel.getPreferredSize();
        window.setSize(
                pref.width + insets.left + insets.right,
                pref.height + insets.top + insets.bottom
        );
        window.validate();

        window.setLocationRelativeTo(null); // Hiển thị ở giữa màn hình
        window.setVisible(true);

        gamePanel.startGameThread();
    }
}
