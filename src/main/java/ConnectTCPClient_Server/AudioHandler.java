package ConnectTCPClient_Server;

import javax.sound.sampled.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class AudioHandler extends Thread {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final AudioFormat format;
    private final ConnectionListener listener;
    private TargetDataLine microphone;
    private SourceDataLine speaker;
    private volatile boolean running = true;
    private final Socket socket;

    public AudioHandler(Socket socket, ConnectionListener listener) throws LineUnavailableException, IOException {
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
        this.format = new AudioFormat(48000, 16, 2, true, false);
        this.listener = listener;
        initMicrophone();
        initSpeaker();
        this.socket = socket;
    }

    private void initMicrophone() throws LineUnavailableException {
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
        microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
        microphone.open(format);
    }
    public interface ConnectionListener {
        void onClientDisconnected(String clientAddress);
    }
    private void initSpeaker() throws LineUnavailableException {
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
        speaker = (SourceDataLine) AudioSystem.getLine(speakerInfo);
        speaker.open(format);
    }

    @Override
    public void run() {
        microphone.start();
        speaker.start();
        System.out.println("AudioHandler is running...");
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (running) {
                try {
                    bytesRead = microphone.read(buffer, 0, buffer.length);
                    dos.write(buffer, 0, bytesRead);
                    dos.flush(); // Ensure the data is sent immediately

                    System.out.println(bytesRead);
                    bytesRead = dis.read(buffer, 0, buffer.length);
                    if (bytesRead == -1) {
                        throw new IOException("End of stream reached");
                    }
                    speaker.write(buffer, 0, bytesRead);
                } catch (IOException e) {
                    System.out.println("Client đã ngắt kết nối audio");
                    listener.onClientDisconnected(socket.getInetAddress().getHostAddress());
                    stopAudio();
                }
            }
        } finally {
            stopAudio();
        }
    }

    public void stopAudio() {
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
            System.out.println("Lỗi khi đóng DataInputStream/DataOutputStream: " + e.getMessage());
        }
    }
}
