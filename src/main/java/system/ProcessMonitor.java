package system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import javax.swing.*;

public class ProcessMonitor {
    private final OperatingSystem os;
    private final Map<Integer, Long> previousTimes = new ConcurrentHashMap<>();
    private final Map<Integer, Long> previousUpTimes = new ConcurrentHashMap<>();

    private final JLabel[] processListLabel;

    public ProcessMonitor(OperatingSystem os, JLabel[] processListLabel) {
        this.os = os;
        this.processListLabel = processListLabel;
    }

    public void printProcesses(Predicate<OSProcess> filter) {
        List<OSProcess> processes = os.getProcesses(filter, OperatingSystem.ProcessSorting.CPU_DESC, processListLabel.length);
        List<CompletableFuture<Map.Entry<Integer, String>>> futures = new ArrayList<>();

        for (int i = 0; i < processes.size() && i < processListLabel.length; i++) {
            final int labelIndex = i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                String processInfo = updateProcessInfo(processes.get(labelIndex));
                return Map.entry(labelIndex, processInfo);
            }));
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.thenAccept(v -> {
            Map<Integer, String> processInfoMap = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            SwingUtilities.invokeLater(() -> {
                for (Map.Entry<Integer, String> entry : processInfoMap.entrySet()) {
                    processListLabel[entry.getKey()].setText(entry.getValue());
                }
            });
        }).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }

    private String updateProcessInfo(OSProcess process) {
        int processId = process.getProcessID();
        String processName = formatProcessName(process.getName());
        long currentTime = process.getKernelTime() + process.getUserTime();
        long currentUpTime = process.getUpTime();

        long previousTime = previousTimes.computeIfAbsent(processId, k -> currentTime);
        long previousUpTime = previousUpTimes.computeIfAbsent(processId, k -> currentUpTime);

        if (previousTime == currentTime || previousUpTime == currentUpTime) {
            return formatProcessInfo(processName, 0.0, process.getResidentSetSize());
        }

        double cpuLoad = calculateCpuLoad(currentTime, previousTime, currentUpTime, previousUpTime);
        previousTimes.put(processId, currentTime);
        previousUpTimes.put(processId, currentUpTime);

        return formatProcessInfo(processName, cpuLoad, process.getResidentSetSize());
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
        return String.format("%-9s", processName);
    }

    private String formatProcessInfo(String processName, double cpuLoad, long memory) {
        return String.format(" %-9s %5.1f %12s", processName, cpuLoad, FormatUtil.formatBytes(memory));
    }

    public void shutdown() {
        // Không cần shutdown ExecutorService vì đã sử dụng CompletableFuture.supplyAsync()
    }
}
