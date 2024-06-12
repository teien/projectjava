import javax.swing.*;
import java.awt.*;

public class NetworkMonitor extends JFrame {
    public NetworkMonitor() {
        setTitle("Network Monitor");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        NetworkPanel panel = new NetworkPanel();
        add(panel);
        Timer timer = new Timer(1000, e -> panel.updateSpeeds());
        timer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NetworkMonitor monitor = new NetworkMonitor();
            monitor.setVisible(true);
        });
    }
}
