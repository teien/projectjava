package ConnectTCPClient_Server;

import javax.sound.sampled.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class AudioHandler {
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

    private void initSpeaker() throws LineUnavailableException {
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
        speaker = (SourceDataLine) AudioSystem.getLine(speakerInfo);
        speaker.open(format);
    }

    public interface ConnectionListener {
        void onClientDisconnected(String clientAddress);
    }

    public void startAudio() {
        running = true;
        Thread sendThread = new Thread(this::sendAudio);
        Thread receiveThread = new Thread(this::receiveAudio);
        sendThread.start();
        receiveThread.start();
    }

    void sendAudio() {
        try {
            microphone.start();
            byte[] buffer = new byte[4096];
            while (running) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                dos.write(buffer, 0, bytesRead);
                dos.flush();
                System.out.println("Sent " + bytesRead + " bytes.");
            }
        } catch (IOException e) {
            System.out.println("Client đã ngắt kết nối audio");
            listener.onClientDisconnected(socket.getInetAddress().getHostAddress());
            stopAudio();
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
            System.out.println("Client đã ngắt kết nối audio");
            listener.onClientDisconnected(socket.getInetAddress().getHostAddress());
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
            socket.close();
        } catch (IOException e) {
            System.out.println("Lỗi khi đóng DataInputStream/DataOutputStream hoặc socket: " + e.getMessage());
        }
    }
}
