package system.ConnectTCPClient_Server;

import javax.sound.sampled.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class AudioHandler {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final AudioFormat format;
    private TargetDataLine microphone;
    private SourceDataLine speaker;
    private volatile boolean running = true;
    private static volatile boolean sending = false;

    public AudioHandler(Socket socket) throws LineUnavailableException, IOException {
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
        this.format = new AudioFormat(48000, 16, 2, true, false);
        initMicrophone();
        initSpeaker();
    }

    private void initMicrophone() throws LineUnavailableException {
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
        microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
        microphone.open(format);
    }

    private void initSpeaker() throws LineUnavailableException {
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
        speaker = (SourceDataLine) AudioSystem.getLine(speakerInfo);
        speaker.open(format);
    }

    public void start() {
        running = true;
        Thread sendThread = new Thread(this::sendAudio);
        Thread receiveThread = new Thread(this::receiveAudio);
        sendThread.start();
        receiveThread.start();
    }

    public static void startSending() {
        sending = true;
    }

    public static void stopSending() {
        sending = false;
    }

    private void sendAudio() {
        try {
            microphone.start();
            byte[] buffer = new byte[4096];
            while (running) {
                if (sending) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    dos.write(buffer, 0, bytesRead);
                    dos.flush();
                    System.out.println("Sent " + bytesRead + " bytes.");
                } else {
                    // Sleep for a short period to reduce CPU usage when not sending
                    Thread.sleep(100);
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error while sending audio: " + e.getMessage());
            stop();
        }
    }

    private void receiveAudio() {
        try {
            speaker.start();
            byte[] buffer = new byte[4096];
            while (running) {
                int bytesRead = dis.read(buffer, 0, buffer.length);
                if (bytesRead == -1) {
                    throw new IOException("End of stream reached");
                }
                speaker.write(buffer, 0, bytesRead);
                System.out.println("Received " + bytesRead + " bytes.");
            }
        } catch (IOException e) {
            System.out.println("Error while receiving audio: " + e.getMessage());
            stop();
        }
    }

    public void stop() {
        running = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        if (speaker != null) {
            speaker.stop();
            speaker.close();
        }
        try {
            dis.close();
            dos.close();
        } catch (IOException e) {
            System.out.println("Error closing streams: " + e.getMessage());
        }
    }
}