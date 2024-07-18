package main;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.GlobalConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemAndProcessCpuMonitor {

    private static final int MAX_PROCESSES_TO_DISPLAY = 5; // Số tiến trình tối đa hiển thị

    private final OperatingSystem os;
    private final CentralProcessor cpu;
    private final Map<Integer, Long> previousTimes = new HashMap<>();
    private final Map<Integer, Long> previousUpTimes = new HashMap<>();
    private final int cpuNumber;
    private double systemCpuLoad;
    private final CentralProcessor processor;

    public SystemAndProcessCpuMonitor() {
        SystemInfo si = new SystemInfo();
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_CPU_UTILITY, true);
        processor = si.getHardware().getProcessor();
        this.os = si.getOperatingSystem();
        this.cpu = si.getHardware().getProcessor();
        this.cpuNumber = cpu.getLogicalProcessorCount();
    }

    public void updateAndPrintInfo() {
        systemCpuLoad = calculateSystemCpuLoad();
        List<OSProcess> processes = getTopProcesses();

        System.out.println("------------------------------------------------------------------------");
        System.out.println("System CPU Load: " + String.format("%.1f", systemCpuLoad * 100) + "%");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("   PID  NAME                CPU%      MEM(MB)");
        System.out.println("------------------------------------------------------------------------");

        for (OSProcess process : processes) {
            if (isSystemIdleProcess(process)) {
                continue;
            }
            double processCpuLoad = calculateProcessCpuLoad(process);
            long memoryUsage = process.getResidentSetSize() / (1024 * 1024); // MB

            System.out.printf("%6d  %-16s  %5.1f  %10d\n",
                    process.getProcessID(),
                    formatProcessName(process.getName()),
                    processCpuLoad,
                    memoryUsage);
        }
    }

    private double calculateSystemCpuLoad() {
        long[] prevTicks = cpu.getSystemCpuLoadTicks();
        try {
            Thread.sleep(1000); // Chờ 1 giây
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return cpu.getSystemCpuLoadBetweenTicks(prevTicks);
    }

    private List<OSProcess> getTopProcesses() {
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_CPU_UTILITY, true);
        return os.getProcesses(OperatingSystem.ProcessFiltering.ALL_PROCESSES,
                OperatingSystem.ProcessSorting.CPU_DESC,
                MAX_PROCESSES_TO_DISPLAY);
    }

    private double calculateProcessCpuLoad(OSProcess process) {
        int pid = process.getProcessID();
        long currentTime = process.getKernelTime() + process.getUserTime();
        long currentUpTime = process.getUpTime();

        long previousTime = previousTimes.getOrDefault(pid, 0L);
        long previousUpTime = previousUpTimes.getOrDefault(pid, 0L);

        double cpuLoad = 0.0;
        if (previousTime > 0 && previousUpTime > 0) {
            cpuLoad = calculateCpuLoad(currentTime, previousTime, currentUpTime, previousUpTime);
        }

        previousTimes.put(pid, currentTime);
        previousUpTimes.put(pid, currentUpTime);

        int usedCores = estimateUsedCores(cpuLoad);
if (cpuLoad > 30 && cpuLoad >systemCpuLoad*100) {
    cpuLoad /= usedCores;
}


        return cpuLoad ;
    }

    private boolean isSystemIdleProcess(OSProcess process) {
        int pid = process.getProcessID();
        String name = process.getName();
        return pid == 0 || "System Idle Process".equals(name) || "Idle".equals(name);
    }

    private double calculateCpuLoad(long currentTime, long previousTime, long currentUpTime, long previousUpTime) {
        long timeDiff = currentTime - previousTime;
        long upTimeDiff = currentUpTime - previousUpTime;
        if (upTimeDiff <= 0) {
            return 0;
        }
        return (100d * timeDiff / upTimeDiff);
    }

    private int estimateUsedCores(double cpuLoad) {
        int estimatedCores = (int) Math.ceil(Math.min(cpuLoad, 100.0) / 100 * cpuNumber);
        return Math.max(1, Math.min(cpuNumber, estimatedCores));
    }

    private String formatProcessName(String processName) {
        if (processName.length() > 16) {
            return processName.substring(0, 16);
        }
        return String.format("%-16s", processName); // Căn trái với độ rộng 16 ký tự
    }
    public static void main(String[] args) {
        SystemAndProcessCpuMonitor monitor = new SystemAndProcessCpuMonitor();
        while (true) {
            monitor.updateAndPrintInfo();
        }
    }
}

