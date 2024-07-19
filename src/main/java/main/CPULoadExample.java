package main;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.util.GlobalConfig;

public class CPULoadExample {
    public static void main(String[] args) {
        SystemInfo systemInfo = new SystemInfo();
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_CPU_UTILITY, true);
        CentralProcessor processor = systemInfo.getHardware().getProcessor();

        // Lấy các chỉ số hiện tại
        long[] prevTicks = processor.getSystemCpuLoadTicks();
System.out.println("prevTicks: " + prevTicks[0] + " " + prevTicks[1] + " " + prevTicks[2] + " " + prevTicks[3] + " " + prevTicks[4] + " " + prevTicks[5] + " " + prevTicks[6] + " " + prevTicks[7]);
        // Thời gian ngủ giữa hai lần lấy chỉ số để có thể tính toán được thay đổi
        try {
            Thread.sleep(1000);  // ngủ 1 giây
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks);

        // In ra kết quả
        System.out.printf("CPU Load: %.1f%%%n", cpuLoad * 100);
    }
}
