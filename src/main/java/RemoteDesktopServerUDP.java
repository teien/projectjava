import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class RemoteDesktopServerUDP {
    private DatagramSocket socket;
    private boolean isRunning = false;
    private JButton startButton;
    private JButton stopButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RemoteDesktopServerUDP().createAndShowGUI());
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Remote Desktop Server");
        frame.setSize(200, 105);
        frame.setLayout(new FlowLayout());
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer();
            }
        });

        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        frame.add(startButton);
        frame.add(stopButton);
        frame.setVisible(true);
    }

    private void startServer() {
        if (!isRunning) {
            isRunning = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            new Thread(() -> {
                try {
                    socket = new DatagramSocket(12345);
                    System.out.println("Server đang chờ kết nối...");

                    while (isRunning) {
                        byte[] buffer = new byte[65536];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        new ClientHandler(packet).start();
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void stopServer() {
        if (isRunning) {
            isRunning = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Server đã dừng");
        }
    }

    static class ClientHandler extends Thread {
        private final DatagramPacket packet;
        private Robot robot;

        public ClientHandler(DatagramPacket packet) {
            this.packet = packet;
            try {
                robot = new Robot();
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                Rectangle screenRect = new Rectangle(screenSize);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                byte[] data = packet.getData();
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bais);

                String type = dis.readUTF();
                if ("CTL".equals(type)) {
                    String action = dis.readUTF();
                    int x = dis.readInt();
                    int y = dis.readInt();
                    int button = dis.readInt();
                    handleControlAction(action, x, y, button);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleControlAction(String action, int x, int y, int button) {
            switch (action) {
                case "CLICK":
                    robot.mouseMove(x, y);
                    robot.mousePress(InputEvent.getMaskForButton(button));
                    robot.mouseRelease(InputEvent.getMaskForButton(button));
                    break;
                case "MOUSE_PRESS":
                    robot.mouseMove(x, y);
                    robot.mousePress(InputEvent.getMaskForButton(button));
                    break;
                case "MOUSE_RELEASE":
                    robot.mouseMove(x, y);
                    robot.mouseRelease(InputEvent.getMaskForButton(button));
                    break;
                case "MOVE":
                    robot.mouseMove(x, y);
                    break;
                case "PRESS":
                    robot.keyPress(button);
                    break;
                case "RELEASE":
                    robot.keyRelease(button);
                    break;
            }
        }
    }
}
