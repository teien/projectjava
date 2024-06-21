import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class RemoteDesktopServer {
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private JButton startButton;
    private JButton stopButton;

    public static void main(String[] args) {
       new RemoteDesktopServer().createAndShowGUI();
    }

    void createAndShowGUI() {
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
                    serverSocket = new ServerSocket(12345);
                    System.out.println("Server đang chờ kết nối...");

                    while (isRunning) {
                        try {
                            Socket socket = serverSocket.accept();
                            System.out.println("Client đã kết nối");
                            new ClientHandler(socket).start();
                        } catch (IOException e) {
                            if (isRunning) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void stopServer() {
        if (isRunning) {
            isRunning = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);

            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                System.out.println("Server đã dừng");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ClientHandler extends Thread {
        private final Socket socket;
        private Robot robot;
        private Rectangle screenRect;

        public ClientHandler(Socket socket) {
            this.socket = socket;
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
            try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                 dos.writeInt(screenRect.width);
                 dos.writeInt(screenRect.height);
                 dos.flush();
                // Thread để gửi ảnh màn hình
                new Thread(() -> {
                    try {
                        while (isRunning && !socket.isClosed()) {
                            BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(screenCapture, "jpg", baos);
                            byte[] imageBytes = baos.toByteArray();

                            dos.writeUTF("IMG");
                            dos.writeInt(imageBytes.length);
                            dos.write(imageBytes);
                            dos.flush();

                            Thread.sleep(10); // Đợi 200ms trước khi chụp lại màn hình
                        }
                    } catch (SocketException e) {

                        System.out.println("Client đã ngắt kết nối");

                    } catch (IOException | InterruptedException e) {
                        if (isRunning) {
                            e.printStackTrace();
                        }
                    }
                }).start();

                while (isRunning && !socket.isClosed()) {
                    try {
                        String type = dis.readUTF();
                        if ("CTL".equals(type)) {
                            String action = dis.readUTF();
                            int x = dis.readInt();
                            int y = dis.readInt();
                            int data = dis.readInt();

                            handleControlAction(action, x, y, data);
                        }
                    } catch (EOFException e) {
                        // Client ngắt kết nối
                        System.out.println("Client đã ngắt kết nối");
                        break;
                    } catch (SocketException e) {
                        // Server đã đóng kết nối
                        System.out.println("Server đã đóng kết nối");
                        break;
                    } catch (IOException | InterruptedException e) {
                        if (isRunning) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Client đã ngắt kết nối");
            }
        }

        private void handleControlAction(String action, int x, int y, int data) throws InterruptedException {
            switch (action) {
                /*case "CLICK":
                    robot.mouseMove(x, y);
                    robot.mousePress(InputEvent.getMaskForButton(data));
                    robot.mouseRelease(InputEvent.getMaskForButton(data));
                    break;*/
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
