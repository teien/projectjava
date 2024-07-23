package system;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

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

    public synchronized void printProcesses(Predicate<OSProcess> filter) {
        List<OSProcess> processes = os.getProcesses(filter, OperatingSystem.ProcessSorting.CPU_DESC, 4);
        final int[] labelIndex = {0};
        processes.forEach(process -> {
            updateProcessInfo(process, labelIndex[0]);
            labelIndex[0]++;
        });
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
        long memory = 0;
        memory = process.getResidentSetSize();
        double cpuLoad = 0;
        cpuLoad = calculateCpuLoad(currentTime, previousTime, currentUpTime, previousUpTime);
        previousTimes.put(processId, currentTime);
        previousUpTimes.put(processId, currentUpTime);

        processName = formatProcessName(processName);

        updateLabel(labelIndex, processName, cpuLoad, memory);
    }
   /* private int estimateUsedCores(double cpuLoad) {
        int estimatedCores = (int) Math.ceil(Math.min(cpuLoad, 100.0) / 100 * cpuNumber);
        return Math.max(1, Math.min(cpuNumber, estimatedCores));
    }*/
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
