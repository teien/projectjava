package system;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import javax.swing.*;

public class ProcessMonitor {
    private final OperatingSystem os;
    private final Map<Integer, Long> previousTimes = new HashMap<>();
    private final Map<Integer, Long> previousUpTimes = new HashMap<>();
    private final int cpuNumber;
    private final JLabel[] processListLabel;
    private final ExecutorService executorService;

    public ProcessMonitor(OperatingSystem os, int cpuNumber, JLabel[] processListLabel) {
        this.os = os;
        this.cpuNumber = cpuNumber;
        this.processListLabel = processListLabel;
        this.executorService = Executors.newFixedThreadPool(processListLabel.length);
    }

    public void printProcesses(Predicate<OSProcess> filter) {
        List<OSProcess> processes = os.getProcesses(filter, OperatingSystem.ProcessSorting.CPU_DESC, processListLabel.length);
        Future<Map.Entry<Integer, String>>[] futures = new Future[processes.size()];
        Map<Integer, String> processInfoMap = new HashMap<>();

        for (int i = 0; i < processes.size() && i < processListLabel.length; i++) {
            final int labelIndex = i;
            futures[i] = executorService.submit(() -> {
                String processInfo = updateProcessInfo(processes.get(labelIndex));
                return Map.entry(labelIndex, processInfo);
            });
        }

        for (Future<Map.Entry<Integer, String>> future : futures) {
            try {
                Map.Entry<Integer, String> entry = future.get();
                processInfoMap.put(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SwingUtilities.invokeLater(() -> {
            for (Map.Entry<Integer, String> entry : processInfoMap.entrySet()) {
                processListLabel[entry.getKey()].setText(entry.getValue());
            }
        });
    }

    private String updateProcessInfo(OSProcess process) {
        int processId = process.getProcessID();
        String processName = process.getName();
        long currentTime = process.getKernelTime() + process.getUserTime();
        long currentUpTime = process.getUpTime();

        long previousTime = previousTimes.getOrDefault(processId, 0L);
        long previousUpTime = previousUpTimes.getOrDefault(processId, 0L);

        long memory = process.getResidentSetSize();
        processName = formatProcessName(processName);

        if (previousTime == 0 || previousUpTime == 0) {
            previousTimes.put(processId, currentTime);
            previousUpTimes.put(processId, currentUpTime);
            return String.format(" %-9s %5.1f %12s", processName, 0.0, FormatUtil.formatBytes(memory));
        }

        double cpuLoad = calculateCpuLoad(currentTime, previousTime, currentUpTime, previousUpTime);
        previousTimes.put(processId, currentTime);
        previousUpTimes.put(processId, currentUpTime);

        return String.format(" %-9s %5.1f %12s", processName, cpuLoad, FormatUtil.formatBytes(memory));
    }

    private double calculateCpuLoad(long currentTime, long previousTime, long currentUpTime, long previousUpTime) {
        long timeDifference = currentTime - previousTime;
        long upTimeDifference = currentUpTime - previousUpTime;
        if (upTimeDifference <= 0) {
            return 0;
        }
        return (100d * timeDifference / (double) upTimeDifference) ;
    }

    private String formatProcessName(String processName) {
        if (processName.length() > 9) {
            return processName.substring(0, 9);
        }
        return String.format("%-9s", processName);
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
