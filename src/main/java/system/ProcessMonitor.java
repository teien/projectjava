package system;

import java.util.List;
import java.util.Map;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import oshi.util.GlobalConfig;

import javax.swing.*;


public class ProcessMonitor {
    private final OperatingSystem os;
    private final Map<Integer, Long> previousTimes = new java.util.HashMap<>();
    private final Map<Integer, Long> previousUpTimes = new java.util.HashMap<>();
    private final int cpuNumber;
    private final JLabel[] processListLabel;
    public ProcessMonitor(OperatingSystem os, int cpuNumber, JLabel[] processListLabel) {
        this.os = os;
        this.cpuNumber = cpuNumber;
        this.processListLabel = processListLabel;

    }

    public void printProcesses() {
        List<OSProcess> processes = os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, 5);
        // bo qua process idle
        processes.removeIf(this::isSystemIdleProcess);
        int labelIndex = 0;
        for (OSProcess process : processes) {
            updateProcessInfo(process, labelIndex);
            labelIndex++;
            if (labelIndex >= processListLabel.length) {
                break;
            }
        }
    }

    private boolean isSystemIdleProcess(OSProcess process) {
        int pid = process.getProcessID();
        String name = process.getName();
        return pid == 0 || "Idle".equals(name); // Combined check for efficiency
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

       // int usedCores = estimateUsedCores(cpuLoad);


        processName = formatProcessName(processName);
        updateLabel(labelIndex, processName, cpuLoad, process.getResidentSetSize());
    }

    private int estimateUsedCores(double cpuLoad) {
        int estimatedCores = (int) Math.ceil(Math.min(cpuLoad, 100.0) / 100 * cpuNumber);
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
        String space = " ";
        return processName + space.repeat(Math.max(0, 9 - processName.length()));
    }

    private void updateLabel(int labelIndex, String processName, double cpuLoad, long residentSetSize) {
        processListLabel[labelIndex].setText(String.format(" %-9s %5.1f %12s", processName, cpuLoad,  FormatUtil.formatBytes(residentSetSize)));
    }
}
