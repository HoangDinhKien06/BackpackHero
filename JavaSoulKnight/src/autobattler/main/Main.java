package autobattler.main;

import javax.swing.JFrame;

public class Main {
    public static JFrame window;

    public static void main(String[] args) {
        // Chặn Java auto-scaling theo DPI/Scale của Windows (giúp khung hình đúng kích thước).
        // Lưu ý: property này có thể chỉ tác dụng trên một số JDK/phiên bản.
        System.setProperty("sun.java2d.uiScale", "1.0");

        window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setTitle("Backpack Hero");

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        window.pack();

        // Đảm bảo "client area" (vùng vẽ) khớp preferred size của GamePanel,
        // tránh việc bị sai lệch do insets/viền cửa sổ.
        java.awt.Insets insets = window.getInsets();
        java.awt.Dimension pref = gamePanel.getPreferredSize();
        window.setSize(
                pref.width + insets.left + insets.right,
                pref.height + insets.top + insets.bottom
        );
        window.validate();

        window.setLocationRelativeTo(null);
        window.setVisible(true);

        gamePanel.startGameThread();
    }
}
