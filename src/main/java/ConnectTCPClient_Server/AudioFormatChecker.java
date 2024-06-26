package ConnectTCPClient_Server;

import javax.sound.sampled.*;

public class AudioFormatChecker {
    public static void main(String[] args) {
        try {
            Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
            for (Mixer.Info info : mixerInfo) {
                System.out.println("Mixer: " + info.getName());
                Mixer mixer = AudioSystem.getMixer(info);
                Line.Info[] targetLineInfo = mixer.getTargetLineInfo();
                for (Line.Info lineInfo : targetLineInfo) {
                    if (lineInfo instanceof DataLine.Info) {
                        DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                        System.out.println("  Supported Target Data Lines:");
                        for (AudioFormat format : dataLineInfo.getFormats()) {
                            System.out.println("    " + format);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
