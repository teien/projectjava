package main;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import oshi.util.Util;

public class HardwareInfoExample {
    public static void main(String[] args) {
        // Tạo đối tượng SystemInfo
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hal = systemInfo.getHardware();

        // Lấy thông tin về CPU
        CentralProcessor processor = hal.getProcessor();
        String cpuName = processor.getProcessorIdentifier().getName();
        System.out.println("CPU Name: " + cpuName);

        // Lấy thông tin về GPU
        for (GraphicsCard gpu : hal.getGraphicsCards()) {
            String gpuName = gpu.getName();
            System.out.println("GPU Name: " + gpuName);
        }

        // Để lấy GPU usage và GPU temperature, bạn có thể cần sử dụng thêm các thư viện khác như NVML (NVIDIA Management Library)
        // hoặc OpenHardwareMonitor (chỉ hỗ trợ trên Windows). OSHI không hỗ trợ trực tiếp việc này.

        // Lấy nhiệt độ CPU (vì nhiệt độ GPU thường không được OSHI hỗ trợ trực tiếp)
        Sensors sensors = hal.getSensors();
        double cpuTemperature = sensors.getCpuTemperature();
        System.out.printf("CPU Temperature: %.1f°C%n", cpuTemperature);

        // Giả sử bạn muốn lấy nhiệt độ GPU bằng cách sử dụng OpenHardwareMonitor trên Windows:
        // Note: Phần này yêu cầu sử dụng JNI hoặc JNA để gọi OpenHardwareMonitor từ Java, không được hỗ trợ trực tiếp bởi OSHI.

        // Sử dụng thư viện khác như nvmlWrapper để lấy GPU usage và GPU temperature (dành cho GPU NVIDIA):
        // Lưu ý: bạn cần thêm thư viện nvmlWrapper vào dự án của mình
        // (https://github.com/JeremyMain/nvmlWrapper)

        // Ví dụ sơ lược (code này không thể chạy trực tiếp mà cần thêm thư viện nvmlWrapper)
        // NvmlWrapper.Nvml nvml = NvmlWrapper.INSTANCE;
        // nvml.NVMLInit();
        // NvmlDevice device = nvml.NVMLDeviceGetHandleByIndex(0); // Lấy GPU đầu tiên

    }
}