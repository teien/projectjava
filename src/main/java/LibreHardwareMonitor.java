import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public interface LibreHardwareMonitor extends Library {
    LibreHardwareMonitor INSTANCE = Native.load(
            (Platform.isWindows() ? "LibreHardwareMonitor" : "librehardwaremonitor"),
            LibreHardwareMonitor.class
    );

    void Open();
    void Close();
    String GetHardwareName();
}
