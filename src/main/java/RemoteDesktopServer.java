import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import javax.imageio.ImageIO;
import javax.swing.*;

public class RemoteDesktopServer {
    private ServerSocket serverSocket;
    private boolean running = false;
    private Thread serverThread;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RemoteDesktopServer::new);
    }

    public RemoteDesktopServer() {
        JFrame frame = new JFrame("Remote Desktop Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 100);
        frame.setLayout(new FlowLayout());

        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        frame.add(startButton);
        frame.add(stopButton);

        frame.setVisible(true);
    }

    private void startServer() {
        if (running) {
            return;
        }
        running = true;

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(12345);
                System.out.println("Server đang chờ kết nối...");

                while (running) {
                    Socket controlSocket = serverSocket.accept();
                    System.out.println("Client đã kết nối cho điều khiển");

                    Socket imageSocket = serverSocket.accept();
                    System.out.println("Client đã kết nối cho hình ảnh");

                    new ClientHandler(controlSocket, imageSocket).start();
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        });
        serverThread.start();
    }

    private void stopServer() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server đã dừng");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    class ClientHandler extends Thread {
        private Socket controlSocket;
        private Socket imageSocket;
        private Robot robot;
        private Rectangle screenRect;

        public ClientHandler(Socket controlSocket, Socket imageSocket) {
            this.controlSocket = controlSocket;
            this.imageSocket = imageSocket;
            try {
                robot = new Robot();
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                screenRect = new Rectangle(screenSize);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try (DataOutputStream imageDos = new DataOutputStream(imageSocket.getOutputStream());
                 DataInputStream controlDis = new DataInputStream(controlSocket.getInputStream())) {

                // Thread để gửi ảnh màn hình
                new Thread(() -> {
                    try {
                        while (!imageSocket.isClosed()) {
                            BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(screenCapture, "jpg", baos);
                            byte[] imageBytes = baos.toByteArray();

                            imageDos.writeUTF("IMG");
                            imageDos.writeInt(imageBytes.length);
                            imageDos.write(imageBytes);
                            imageDos.flush();

                            Thread.sleep(100); // Đợi 100ms trước khi chụp lại màn hình
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

                // Xử lý lệnh điều khiển từ client
                while (!controlSocket.isClosed()) {
                    try {
                        String type = controlDis.readUTF();
                        if ("CTL".equals(type)) {
                            String action = controlDis.readUTF();
                            int x = controlDis.readInt();
                            int y = controlDis.readInt();
                            int data = controlDis.readInt();

                            handleControlAction(action, x, y, data);
                        }
                    } catch (EOFException e) {
                        System.out.println("Client đã ngắt kết nối.");
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleControlAction(String action, int x, int y, int data) {
            switch (action) {
                case "CLICK":
                    robot.mouseMove(x, y);
                    robot.mousePress(InputEvent.getMaskForButton(data));
                    robot.mouseRelease(InputEvent.getMaskForButton(data));
                    break;
                case "MOUSE_PRESS":
                    robot.mouseMove(x, y);
                    robot.mousePress(InputEvent.getMaskForButton(data));
                    break;
                case "MOUSE_RELEASE":
                    robot.mouseMove(x, y);
                    robot.mouseRelease(InputEvent.getMaskForButton(data));
                    break;
                case "MOVE":
                    robot.mouseMove(x, y);
                    break;
                case "PRESS":
                    robot.keyPress(data);
                    break;
                case "RELEASE":
                    robot.keyRelease(data);
                    break;
            }
        }
    }
}
