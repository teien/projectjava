import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ProcessDisplay {

    private static OperatingSystem os;
    private JLabel[] processListUpLabel;

    public ProcessDisplay(OperatingSystem os) {
        this.os = os;
        this.processListUpLabel = new JLabel[5]; // assuming you want to display top 5 processes
    }

    public void printProcesses() {
        List<OSProcess> processes = os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, 5);
        int i = 0; // Chỉ số mảng nên bắt đầu từ 0

        for (OSProcess process : processes) {
            if (process.getProcessID() == 0 || "Idle".equals(process.getName())) {
                continue;
            }

            String processName = process.getName();
            if (processName == null || processName.isEmpty()) {
                processName = "Unknown";
            }

            // Chỉnh sửa tên tiến trình để đảm bảo độ dài cố định


            processListUpLabel[i] = new JLabel();
            processListUpLabel[i].setFont(new Font("Monospaced", Font.PLAIN, 12)); // Sử dụng phông chữ Monospaced
            processListUpLabel[i].setText(String.format(
                    "%-30s %7.1f%% %10s",
                    processName,
                    100d * process.getProcessCpuLoadCumulative(),
                    FormatUtil.formatBytes(process.getResidentSetSize())
            ));
            i++;
        }

        // Tạo JFrame để hiển thị các JLabel
        JFrame frame = new JFrame("Process Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(0, 1)); // Sử dụng GridLayout để hiển thị các JLabel thẳng hàng

        for (JLabel label : processListUpLabel) {
            if (label != null) {
                frame.add(label);
            }
        }

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SystemInfo systemInfo = new SystemInfo();
        os = systemInfo.getOperatingSystem();

        ProcessDisplay display = new ProcessDisplay(os);
        display.printProcesses();
    }
}
