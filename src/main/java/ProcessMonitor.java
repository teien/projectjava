import java.util.List;
import java.util.Map;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import javax.swing.*;

public class ProcessMonitor {
    private final OperatingSystem os;
    private final Map<Integer, Long> previousTimes;
    private final Map<Integer, Long> previousUpTimes;
    private final int cpuNumber;
    private final JLabel[] processListLabel;

    public ProcessMonitor(OperatingSystem os, Map<Integer, Long> previousTimes, Map<Integer, Long> previousUpTimes, int cpuNumber, JLabel[] processListLabel) {
        this.os = os;
        this.previousTimes = previousTimes;
        this.previousUpTimes = previousUpTimes;
        this.cpuNumber = cpuNumber;
        this.processListLabel = processListLabel;
    }

    public void printProcesses() {
        List<OSProcess> processes = os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, 5);
        int labelIndex = 0;
        for (OSProcess process : processes) {
            if (isSystemIdleProcess(process)) {
                continue;
            }
            updateProcessInfo(process, labelIndex);
            labelIndex++;
            if (labelIndex >= processListLabel.length) {
                break;
            }
        }
    }

    private boolean isSystemIdleProcess(OSProcess process) {
        return process.getProcessID() == 0 || "Idle".equals(process.getName());
    }

    private void updateProcessInfo(OSProcess process, int labelIndex) {
        int processId = process.getProcessID();
        String processName = process.getName();
        long currentTime = process.getKernelTime() + process.getUserTime();
        long currentUpTime = process.getUpTime();

        long previousTime = previousTimes.getOrDefault(processId, 0L);
        long previousUpTime = previousUpTimes.getOrDefault(processId, 0L);

        if (previousTime == 0 || previousUpTime == 0) {
            previousTimes.put(processId, currentTime);
            previousUpTimes.put(processId, currentUpTime);
            return;
        }

        double cpuLoad = calculateCpuLoad(currentTime, previousTime, currentUpTime, previousUpTime);

        previousTimes.put(processId, currentTime);
        previousUpTimes.put(processId, currentUpTime);


        int usedCores = estimateUsedCores(cpuLoad);

        cpuLoad /= usedCores;

        processName = formatProcessName(processName);
        updateLabel(labelIndex, processName, cpuLoad, process.getResidentSetSize());
    }

    private int estimateUsedCores(double cpuLoad) {
        int estimatedCores = (int) Math.ceil(cpuLoad / 100 * cpuNumber);
        return Math.max(1, Math.min(cpuNumber, estimatedCores));
    }
    private double calculateCpuLoad(long currentTime, long previousTime, long currentUpTime, long previousUpTime) {
        long timeDifference = currentTime - previousTime;
        long upTimeDifference = currentUpTime - previousUpTime;
        if (upTimeDifference <= 0) {
            return 0;
        }
        return (100d * timeDifference / (double) upTimeDifference);
    }

    private String formatProcessName(String processName) {
        if (processName.length() > 9) {
            return processName.substring(0, 9);
        }
        return processName;
    }

    private void updateLabel(int labelIndex, String processName, double cpuLoad, long residentSetSize) {
        String processNamePadded = "&nbsp;" + processName + "&nbsp;".repeat(9 - processName.length());

        String residentSetSizePadded = "&nbsp;".repeat(9 - FormatUtil.formatBytes(residentSetSize).length())+FormatUtil.formatBytes(residentSetSize);


        String htmlText = "<html>"
                + "<style>"
                + "table { border-spacing: 0; }"
                + "td { padding: 0; padding-right: 14px;}"
                + "</style>"
                + "<table>"
                + "<tr>"
                + "<td style='text-align: right;'>" + processNamePadded + "</td>"
                + "<td style='text-align: center; padding-right: 21px;'>" + String.format("%.1f", cpuLoad) + "</td>"
                + "<td style='text-align: left;'>" + residentSetSizePadded + "</td>"
                + "</tr>"
                + "</table>"
                + "</html>";


        processListLabel[labelIndex].setText(String.format(htmlText));
    }
}
