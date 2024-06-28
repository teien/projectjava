package ConnectTCPClient_Server;

import javax.sound.sampled.*;

public class DefaultAudioDevices {
    public static void main(String[] args) {
        try {
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            for (Mixer.Info info : mixerInfos) {
                Mixer mixer = AudioSystem.getMixer(info);
                Line.Info[] targetLineInfos = mixer.getTargetLineInfo();
                for (Line.Info lineInfo : targetLineInfos) {
                    if (lineInfo.getLineClass().equals(TargetDataLine.class)) {
                        System.out.println("Default Microphone: " + info.getName() + " - " + info.getDescription());
                        return;
                    }
                }
            }
            System.out.println("Không tìm thấy default microphone.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
