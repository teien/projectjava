import javax.swing.*;
import java.awt.*;

public class TemperatureDisplay {

    public static void main(String[] args) {
        JFrame frame = new JFrame("GPU Information Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 100);
        frame.setLayout(new BorderLayout());

        double gpuTemperature = 67.5;  // Giả sử đây là nhiệt độ
        double gpuUsage = 75.0;  // Giả sử đây là tỷ lệ sử dụng GPU

        // Tạo JPanel với BorderLayout
        JPanel panel = new JPanel(new BorderLayout());

        // Tạo và cấu hình nhãn "GPU Temperature"
        JLabel labelDescription = new JLabel("GPU Temperature:");
        panel.add(labelDescription, BorderLayout.WEST);

        // Tạo và cấu hình nhãn hiển thị nhiệt độ
        JLabel labelValue = new JLabel(String.format("%.1f°C", gpuTemperature));
        panel.add(labelValue, BorderLayout.EAST);

        // Thêm JPanel vào JFrame
        frame.add(panel, BorderLayout.NORTH);

        // Tạo JPanel thứ hai với BorderLayout cho thông tin GPU Usage
        JPanel panel2 = new JPanel(new BorderLayout());

        // Tạo và cấu hình nhãn "GPU Usage"
        JLabel labelUsageDescription = new JLabel("GPU Usage:");
        panel2.add(labelUsageDescription, BorderLayout.WEST);

        // Tạo và cấu hình nhãn hiển thị tỷ lệ sử dụng GPU
        JLabel labelUsageValue = new JLabel(String.format("%.1f%%", gpuUsage));
        panel2.add(labelUsageValue, BorderLayout.EAST);

        // Thêm JPanel thứ hai vào JFrame
        frame.add(panel2, BorderLayout.SOUTH);

        frame.setVisible(true);
    }
}
